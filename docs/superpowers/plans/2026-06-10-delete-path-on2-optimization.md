# 删除路径去 O(N²) + 死代码清理 (F-1 + L1) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 消除 `deleteBookTransaction`/`deleteAssetRelatedData` 删除时「每条记录都整簇重算 finalAmount」的 O(N²)，并清理 L1 死代码，行为对外不变。

**Architecture:** 方案 A——把单条删除核心抽成 `deleteRecordCore`（余额回退+清关联+删记录，无重算），删账本/资产改为「逐条 core 删 + 删完后对存活簇只重算一次」；`recalculateFinalAmountForCluster` 拆为 `discoverClusterIds`（返簇成员+outEdges 缓存）+ `recalculateFinalAmountFromCluster`，避免重复 BFS 与 N+1 倒退。

**Tech Stack:** Kotlin + Room（`@Transaction` 默认方法）+ JUnit/Truth（JVM 单测，`FakeTransactionDao` 内存替身跑真实默认方法）。

**设计依据:** `docs/superpowers/specs/2026-06-10-delete-path-on2-optimization-design.md`（已过节点1 team-review 四维，吸收 16 finding）。

---

## 环境/命令约定（每个 Task 复用）

- JVM 单测（核心）：
  ```bash
  ./gradlew :core:data:testDebugUnitTest --offline --no-daemon --console=plain
  ```
  单测过滤：追加 `--tests "*.TransactionDaoLogicTest.<方法名>"`。
- androidTest 仅编译核验（本机无设备）：
  ```bash
  ./gradlew :core:database:compileDebugAndroidTestKotlin --offline --no-daemon --console=plain
  ```
- 判定只信 `grep -E '^BUILD (SUCCESSFUL|FAILED)'`（bash exit code 不可信）。
- 跑 Gradle 前先看内存（CLAUDE.local.md 阈值）；首次缺依赖才带本地代理 `127.0.0.1:7897`，否则一律 `--offline`。
- 测试 task 名是 `testDebugUnitTest`（`core:data` 是 android library，**不是** `test`/`testOnlineDebugUnitTest`）。

## 文件结构（改动清单）

| 文件 | 责任 | 改动 |
|---|---|---|
| `core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/dao/TransactionDao.kt` | DAO 事务逻辑 | 拆 `discoverClusterIds`+`recalculateFinalAmountFromCluster`；抽 `deleteRecordCore`；重写 `deleteRecordTransaction(record)`、`deleteBookTransaction`、`deleteAssetRelatedData`；新增 `deleteRecordsBatch`、`ClusterDiscovery` |
| `core/database/src/main/kotlin/.../dao/RecordDao.kt` | DAO 查询 | 删 `queryRelatedRecord()`（L1） |
| `core/data/src/test/kotlin/.../testdoubles/FakeTransactionDao.kt` | 测试替身 | 删两个简化覆盖；`queryRecordByIds` 加计数器 |
| `core/data/src/test/kotlin/.../testdoubles/FakeRecordDao.kt` | 测试替身 | 删 `queryRelatedRecord` override（L1） |
| `core/data/src/test/kotlin/.../testdoubles/TransactionDaoLogicTest.kt` | JVM 单测 | 既有 4 删除测试补 type；新增区分力/守卫测试 |
| `core/data/src/test/kotlin/.../repository/impl/BooksRepositoryImplTest.kt` | JVM 单测 | `deleteBookTransaction` 测试补 type |
| `core/database/src/androidTest/kotlin/.../dao/RecordDaoTest.kt` | androidTest | 删 793 测试、952 改用 `queryAllRelatedRecords`（L1，compile-only） |
| `core/database/src/androidTest/kotlin/.../dao/TransactionDaoTest.kt` | androidTest | 补回滚/跨账本用例（compile-only） |

---

## Task 1: 拆 `recalculateFinalAmountForCluster` → `discoverClusterIds` + `recalculateFinalAmountFromCluster`（H2/H3）

**Files:**
- Modify: `core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/dao/TransactionDao.kt`（现 164-242）
- Test: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/testdoubles/TransactionDaoLogicTest.kt`

- [ ] **Step 1: 写失败测试（锁定 discoverClusterIds 返回 outEdges，防回退到 Set<Long>）**

在 `TransactionDaoLogicTest.kt` 的 `// ========== recalculateAllFinalAmount 全量测试 ==========` 区块前（约 337 行）插入：

```kotlin
// ========== discoverClusterIds 测试 ==========

@Test
fun when_discoverClusterIds_then_returns_cluster_and_outEdges() = runTest {
    setupTypesForAbsorption()
    // E1(1),E2(2) 被 I(3) 吸收
    insertRecord(id = 1L, typeId = EXPENDITURE_TYPE_ID, amount = 10000L)
    insertRecord(id = 2L, typeId = EXPENDITURE_TYPE_ID, amount = 5000L)
    insertRecord(id = 3L, typeId = INCOME_TYPE_ID, amount = 8000L)
    dao.relatedRecords.add(RecordWithRelatedTable(id = 1L, recordId = 3L, relatedRecordId = 1L))
    dao.relatedRecords.add(RecordWithRelatedTable(id = 2L, recordId = 3L, relatedRecordId = 2L))

    val result = dao.discoverClusterIds(3L)

    assertThat(result.clusterIds).containsExactly(1L, 2L, 3L)
    // outEdges 缓存：吸收者 I(3) 的出边是其吸收的支出 [1,2]
    assertThat(result.outEdges[3L]).containsExactly(1L, 2L)
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*.TransactionDaoLogicTest.when_discoverClusterIds_then_returns_cluster_and_outEdges" --offline --no-daemon --console=plain`
Expected: 编译失败 `unresolved reference: discoverClusterIds`（方法尚不存在）。

- [ ] **Step 3: 实现拆分**

在 `TransactionDao.kt` 中，把现有 `recalculateFinalAmountForCluster`（174-242）整体替换为下面三段（`ClusterDiscovery` 放在 `interface TransactionDao { ... }` 块**外**的文件顶层，紧邻 import 之后或文件末尾均可）：

```kotlin
/**
 * 簇 BFS 发现结果：簇成员 id 集 + 每个节点的出边缓存（record_id → 其吸收的支出 id 列表）。
 * outEdges 供净自付重算 step3 复用，避免对每个吸收者重新 queryRelatedByRecordId（消 N+1）。
 */
data class ClusterDiscovery(
    val clusterIds: Set<Long>,
    val outEdges: Map<Long, List<Long>>,
)
```

```kotlin
    /**
     * 从 [seedRecordId] 沿关系表 BFS 发现连通簇（被吸收支出↔吸收者收入二部图连通分量）。
     * 返回簇成员 + outEdges 缓存（record_id 侧被吸收支出），供重算复用。
     *
     * @param excludeRecordIds 需排除的记录 id（删除场景：记录即将被删但关联尚未清除）
     */
    @Transaction
    suspend fun discoverClusterIds(
        seedRecordId: Long,
        excludeRecordIds: Set<Long> = emptySet(),
    ): ClusterDiscovery {
        val clusterIds = LinkedHashSet<Long>()
        val outEdges = HashMap<Long, List<Long>>() // record_id -> 其吸收的支出 id 列表
        val queue = ArrayDeque<Long>()
        if (seedRecordId !in excludeRecordIds) {
            clusterIds.add(seedRecordId)
            queue.add(seedRecordId)
        }
        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            val absorbed = queryRelatedByRecordId(cur).map { it.relatedRecordId }
            outEdges[cur] = absorbed
            val neighbors = absorbed + queryRelatedByRelatedRecordId(cur).map { it.recordId }
            for (n in neighbors) {
                if (n !in excludeRecordIds && clusterIds.add(n)) {
                    queue.add(n)
                }
            }
        }
        return ClusterDiscovery(clusterIds, outEdges)
    }

    /**
     * 对已发现的簇 [clusterIds]（含 [outEdges] 缓存）执行净自付重算（§5 顺序贪心填充）。
     * 复用 outEdges 零额外查询；只写变化项。与全量版 [recalculateAllFinalAmount] 同口径。
     */
    @Transaction
    suspend fun recalculateFinalAmountFromCluster(
        clusterIds: Set<Long>,
        outEdges: Map<Long, List<Long>>,
    ) {
        if (clusterIds.isEmpty()) return

        // 1. 批量取簇内记录，初始化 finalAmount（转账=concessions-charge，收支=recordAmount）
        val records = queryRecordByIds(clusterIds.toList()).associateBy { it.id }
        val finalAmounts = HashMap<Long, Long>(clusterIds.size)
        for (id in clusterIds) {
            val record = records[id] ?: continue
            val type = resolveType(record.typeId) ?: continue
            val category = RecordTypeCategoryEnum.ordinalOf(type.typeCategory)
            finalAmounts[id] = if (category == RecordTypeCategoryEnum.TRANSFER) {
                record.concessions - record.charge
            } else {
                calculateRecordAmount(record, category)
            }
        }

        // 2. 吸收者（record_id 侧有被吸收支出）按 id 升序顺序贪心填充（用缓存 outEdges）
        val absorbers = clusterIds.filter { id ->
            (outEdges[id] ?: emptyList()).any { it in clusterIds }
        }.sorted()
        for (absorberId in absorbers) {
            var remaining = finalAmounts[absorberId] ?: continue
            val absorbedIds = (outEdges[absorberId] ?: emptyList())
                .filter { it in clusterIds }
                .sorted()
            for (expenseId in absorbedIds) {
                val current = finalAmounts[expenseId] ?: continue
                val offset = minOf(remaining, current)
                finalAmounts[expenseId] = current - offset
                remaining -= offset
            }
            finalAmounts[absorberId] = remaining
        }

        // 3. 落库（仅写变化项）
        for ((id, finalAmount) in finalAmounts) {
            if (records[id]?.finalAmount != finalAmount) {
                updateRecordFinalAmountById(id, finalAmount)
            }
        }
    }

    /**
     * 重算一个吸收簇的 finalAmount（净自付语义）。从 [seedRecordId] BFS 发现整簇再重算。
     * 内部拆为 [discoverClusterIds] + [recalculateFinalAmountFromCluster]，对外签名行为不变。
     */
    @Transaction
    suspend fun recalculateFinalAmountForCluster(
        seedRecordId: Long,
        excludeRecordIds: Set<Long> = emptySet(),
    ) {
        val (clusterIds, outEdges) = discoverClusterIds(seedRecordId, excludeRecordIds)
        recalculateFinalAmountFromCluster(clusterIds, outEdges)
    }
```

> 说明：`recalculateAbsorberFinalAmount`（现 299-308，委托 `recalculateFinalAmountForCluster`）保持不变。

- [ ] **Step 4: 运行确认通过（新测试 + 既有重算/删除测试不回归）**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*.TransactionDaoLogicTest" --offline --no-daemon --console=plain`
Expected: `BUILD SUCCESSFUL`，含 `when_discoverClusterIds_*`、既有 `when_recalcAll_*`、`when_delete_*` 全 PASS。

- [ ] **Step 5: 提交**

```bash
git add core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/dao/TransactionDao.kt core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/testdoubles/TransactionDaoLogicTest.kt
git commit -m "[refactor|core|删除路径去O2][公共]拆 discoverClusterIds(返簇+outEdges)+recalculateFinalAmountFromCluster,recalculateFinalAmountForCluster 委托(H2/H3 防 N+1 与 2x BFS)"
```

---

## Task 2: 抽 `deleteRecordCore` + 重写单删 `deleteRecordTransaction(record)`（行为等价）

**Files:**
- Modify: `TransactionDao.kt`（现 573-665 的 `deleteRecordTransaction(record)`）

- [ ] **Step 1: 确认守护测试已存在（无需新写，TDD 由既有覆盖驱动）**

既有 `TransactionDaoLogicTest` 的 `when_delete_absorber_income_single`(264)、`when_delete_one_of_two_absorbers`(279)、`when_delete_shared_expense`(298) 已覆盖单删的存活簇重算语义，作为本 Task 的回归守护（它们已 `setupTypesForAbsorption`，跑真实默认方法）。

- [ ] **Step 2: 运行确认当前绿（基线）**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*.TransactionDaoLogicTest.when_delete_*" --offline --no-daemon --console=plain`
Expected: `BUILD SUCCESSFUL`（重写前基线）。

- [ ] **Step 3: 抽取 + 重写**

在 `TransactionDao.kt` 中，把现有 `deleteRecordTransaction(record: RecordTable)`（582-665，含 @Throws/@Transaction）整体替换为下面两段：

```kotlin
    /**
     * 删除单条记录的核心：余额回退 + 删标签/图片关联 + 清关联 + 删记录，**不含** finalAmount 重算。
     * 供单删与批量删（账本/资产）复用——批量删时由调用方在所有 core 完成后统一对存活簇重算。
     */
    @Throws(DataTransactionException::class)
    @Transaction
    suspend fun deleteRecordCore(record: RecordTable) {
        val recordId = record.id ?: return
        val type = resolveType(record.typeId)
            ?: throw DataTransactionException("Type must not be null")
        val category = RecordTypeCategoryEnum.ordinalOf(type.typeCategory)
        val oldRecordAmount = calculateRecordAmount(record, category)
        // 更新资产余额
        queryAssetById(record.assetId)?.let { asset ->
            val balance =
                if (ClassificationTypeEnum.ordinalOf(asset.type) == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT) {
                    if (category == RecordTypeCategoryEnum.INCOME) {
                        asset.balance + oldRecordAmount
                    } else {
                        asset.balance - oldRecordAmount
                    }
                } else {
                    if (category == RecordTypeCategoryEnum.INCOME) {
                        asset.balance - oldRecordAmount
                    } else {
                        asset.balance + oldRecordAmount
                    }
                }
            updateAsset(asset.copy(balance = balance))
        }
        if (category == RecordTypeCategoryEnum.TRANSFER) {
            queryAssetById(record.intoAssetId)?.let { asset ->
                val balance =
                    if (ClassificationTypeEnum.ordinalOf(asset.type) == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT) {
                        asset.balance + record.amount
                    } else {
                        asset.balance - record.amount
                    }
                updateAsset(asset.copy(balance = balance))
            }
        }
        // 移除关联标签/照片
        deleteOldRelatedTags(recordId)
        deleteOldRelatedImages(recordId)
        // 移除关联记录
        clearRelatedRecordById(recordId)
        // 删除当前记录
        val result = deleteRecord(record)
        if (result <= 0) {
            throw DataTransactionException("Record delete failed!")
        }
    }

    /**
     * 删除已有记录（单删，UI 路径）。
     * 先捕获与本记录关联的对端记录（删除会清关联），删除核心后对存活簇逐簇重算一次。
     */
    @Throws(DataTransactionException::class)
    @Transaction
    suspend fun deleteRecordTransaction(record: RecordTable) {
        val recordId = record.id ?: return
        // 捕获两个方向上的对端记录 id（排除自身），删除前先捕获
        val survivors = (
            queryRelatedByRecordId(recordId).map { it.relatedRecordId } +
                queryRelatedByRelatedRecordId(recordId).map { it.recordId }
            ).filter { it != recordId }.toSet()
        deleteRecordCore(record)
        // 对存活簇逐簇重算一次（去重；删后关联已清、记录已删，BFS 见裁剪簇）
        val visited = HashSet<Long>()
        for (sid in survivors) {
            if (sid in visited) continue
            val (clusterIds, outEdges) = discoverClusterIds(sid)
            visited += clusterIds
            recalculateFinalAmountFromCluster(clusterIds, outEdges)
        }
    }
```

> 注：`deleteRecordTransaction(recordId: Long?)` 重载（561-571）不变，仍委托本方法。

- [ ] **Step 4: 运行确认通过**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*.TransactionDaoLogicTest.when_delete_*" --offline --no-daemon --console=plain`
Expected: `BUILD SUCCESSFUL`，3 个删除测试仍 PASS（行为等价）。

- [ ] **Step 5: 提交**

```bash
git add core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/dao/TransactionDao.kt
git commit -m "[refactor|core|删除路径去O2][公共]抽 deleteRecordCore(余额回退+清关联+删,无重算)+单删 deleteRecordTransaction 改捕获 survivors 后逐簇重算(行为等价)"
```

---

## Task 3: 既有删除测试补 Type（H1 前置，去 Fake 覆盖前必做）

**Files:**
- Modify: `TransactionDaoLogicTest.kt`（387/404/420/435 四个测试）
- Modify: `BooksRepositoryImplTest.kt`（191 测试 + import）

> 背景：去掉 Fake 简化覆盖后，真实 `deleteRecordTransaction` 首行 `resolveType(typeId)` 取不到 Type 即抛 `DataTransactionException`。这 5 个测试用 `createRecordTable`（typeId=1L）但未注册 type，必须先补。此时 Fake 仍覆盖（简化版不调 resolveType），补 type 无害、为 Task 4 预置。

- [ ] **Step 1: TransactionDaoLogicTest 四个删除测试补 setupTypesForAbsorption()**

在以下 4 个测试方法体**第一行**插入 `setupTypesForAbsorption()`：
- `when_deleteBookTransaction_then_assets_also_deleted`（387，紧接 `val bookId = 1L` 前）
- `when_deleteBookTransaction_then_only_book_records_deleted`（404）
- `when_deleteAssetRelatedData_then_all_related_records_deleted`（420）
- `when_deleteAssetRelatedData_then_tag_and_image_relations_cleaned`（435）

示例（387）：
```kotlin
@Test
fun when_deleteBookTransaction_then_assets_also_deleted() = runTest {
    setupTypesForAbsorption()
    val bookId = 1L
    dao.books.add(createBooksTable(id = bookId))
    // ...原样不变
```

- [ ] **Step 2: BooksRepositoryImplTest:191 补 type + import**

在 `BooksRepositoryImplTest.kt` import 区加：
```kotlin
import cn.wj.android.cashbook.core.database.table.TypeTable
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
```

在 `when_deleteBookTransaction_then_book_and_records_deleted`（191）的 `// 准备数据` 后、第一条 `transactionDao.records.add` 前插入：
```kotlin
        transactionDao.types.add(
            TypeTable(
                id = 1L,
                parentId = -1L,
                name = "支出",
                iconName = "icon",
                typeLevel = 0,
                typeCategory = RecordTypeCategoryEnum.EXPENDITURE.ordinal,
                protected = 0,
                sort = 0,
            ),
        )
```

- [ ] **Step 3: 运行确认仍绿（Fake 仍覆盖，行为不变）**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*.TransactionDaoLogicTest" --tests "*.BooksRepositoryImplTest" --offline --no-daemon --console=plain`
Expected: `BUILD SUCCESSFUL`（补 type 无害）。

- [ ] **Step 4: 提交**

```bash
git add core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/testdoubles/TransactionDaoLogicTest.kt core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/repository/impl/BooksRepositoryImplTest.kt
git commit -m "[test|core|删除路径去O2][公共]H1 前置:5 个删除测试补 TypeTable 注册(去 Fake 覆盖后真实 resolveType 需 Type,否则抛异常)"
```

---

## Task 4: 去 `FakeTransactionDao` 简化覆盖 + 加 `queryRecordByIds` 计数器

**Files:**
- Modify: `FakeTransactionDao.kt`（删 244-262 两 override；改 171-173 加计数；删 316-319 过期注释）

- [ ] **Step 1: 删两个简化覆盖 + 加计数器**

删除 `FakeTransactionDao.kt` 中 `deleteBookTransaction`（244-254）与 `deleteAssetRelatedData`（256-262）两个 override 整段，以及 316-319 的过期注释段。

把 `queryRecordByIds`（171-173）替换为带计数版：
```kotlin
    /** queryRecordByIds 调用计数（区分力测试用：净自付重算每簇调一次，删除路径据此判重算次数）。可手动重置。 */
    var queryRecordByIdsCallCount = 0

    override suspend fun queryRecordByIds(ids: List<Long>): List<RecordTable> {
        queryRecordByIdsCallCount++
        return records.filter { it.id in ids }
    }
```

> 现在 `deleteBookTransaction`/`deleteAssetRelatedData` 由 Fake 继承真实默认方法（当前仍是 Task 2 后的「单删循环」版，下个 Task 才优化）。

- [ ] **Step 2: 运行确认全绿（真实逻辑 + Task 3 的 type 兜住）**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*.TransactionDaoLogicTest" --tests "*.BooksRepositoryImplTest" --offline --no-daemon --console=plain`
Expected: `BUILD SUCCESSFUL`。4 个删除测试现跑真实默认方法（逐条删，余额回退因多数无 asset 而 `?.let` 跳过），断言不变仍 PASS；`deleteBookCalled` 经真实 `deleteBookById` 置位。

- [ ] **Step 3: 提交**

```bash
git add core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/testdoubles/FakeTransactionDao.kt
git commit -m "[test|core|删除路径去O2][公共]去 FakeTransactionDao deleteBook/deleteAsset 简化覆盖(继承真实算法)+queryRecordByIds 计数器(区分力)"
```

---

## Task 5: 区分力测试驱动批量重写（消 O(N²)）+ 存活簇/转账守卫

**Files:**
- Modify: `TransactionDaoLogicTest.kt`（新增测试）
- Modify: `TransactionDao.kt`（重写 `deleteBookTransaction`/`deleteAssetRelatedData` + 新增 `deleteRecordsBatch`）

- [ ] **Step 1: 写失败/守卫测试**

在 `TransactionDaoLogicTest.kt` 的 `// ========== deleteBookTransaction 测试 ==========`（384）下追加：

```kotlin
@Test
fun when_deleteBook_all_in_book_cluster_then_no_survivor_recalc() = runTest {
    // 区分力：全簇随账本同删 → 无存活簇 → 重算 0 次（旧逐条路径会 >0）
    setupTypesForAbsorption()
    val bookId = 1L
    dao.books.add(createBooksTable(id = bookId))
    insertRecord(id = 1L, typeId = EXPENDITURE_TYPE_ID, amount = 10000L, booksId = bookId)
    insertRecord(id = 2L, typeId = INCOME_TYPE_ID, amount = 8000L, booksId = bookId)
    dao.relatedRecords.add(RecordWithRelatedTable(id = 1L, recordId = 2L, relatedRecordId = 1L))
    dao.recalculateFinalAmountForCluster(2L)
    dao.queryRecordByIdsCallCount = 0 // 重置，只测删除阶段

    dao.deleteBookTransaction(bookId)

    assertThat(dao.queryRecordByIdsCallCount).isEqualTo(0)
    assertThat(dao.records).isEmpty()
}

@Test
fun when_deleteAsset_clears_absorbed_expense_then_surviving_absorber_recalced() = runTest {
    // 存活簇正向守卫（L2）：删资产清掉被吸收支出 → 存活吸收者恢复 recordAmount
    setupTypesForAbsorption()
    dao.assets.add(createAssetTable(id = 10L, balance = 0L))
    dao.assets.add(createAssetTable(id = 20L, balance = 0L))
    insertRecord(id = 1L, typeId = EXPENDITURE_TYPE_ID, amount = 10000L, assetId = 10L)
    insertRecord(id = 2L, typeId = INCOME_TYPE_ID, amount = 8000L, assetId = 20L)
    dao.relatedRecords.add(RecordWithRelatedTable(id = 1L, recordId = 2L, relatedRecordId = 1L))
    dao.recalculateFinalAmountForCluster(2L) // E.fa=2000, I.fa=0

    dao.deleteAssetRelatedData(10L) // 删 E（asset=10）

    assertThat(dao.queryRecordById(1L)).isNull()
    assertThat(dao.queryRecordById(2L)!!.finalAmount).isEqualTo(8000L) // 恢复 recordAmount
}

@Test
fun when_deleteAsset_with_transfer_then_surviving_counterpart_balance_reverted() = runTest {
    // AUDIT-2/2b：删资产 A(10)，转账 B(20)→A，源资产 B 存活余额回退；A 行存活余额回退
    setupTypesForAbsorption()
    // 转账后状态：B=80000(被扣 20000), A=20000(被加 20000)
    dao.assets.add(createAssetTable(id = 10L, balance = 20000L))
    dao.assets.add(createAssetTable(id = 20L, balance = 80000L))
    insertRecord(id = 1L, typeId = TRANSFER_TYPE_ID, amount = 20000L, assetId = 20L, intoAssetId = 10L)

    dao.deleteAssetRelatedData(10L)

    assertThat(dao.queryRecordById(1L)).isNull()
    assertThat(dao.queryAssetById(20L)!!.balance).isEqualTo(100000L) // 源 B 回退 +20000
    assertThat(dao.queryAssetById(10L)).isNotNull() // A 行存活(AUDIT-2)
    assertThat(dao.queryAssetById(10L)!!.balance).isEqualTo(0L) // A 回退 -20000
}
```

- [ ] **Step 2: 运行确认区分力测试失败**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*.TransactionDaoLogicTest.when_deleteBook_all_in_book_cluster_then_no_survivor_recalc" --offline --no-daemon --console=plain`
Expected: FAIL —— 当前「逐条删」路径删每条都对未删的同书记录重算 → `queryRecordByIdsCallCount` > 0，断言 `isEqualTo(0)` 失败。
（另两个守卫测试此时应 PASS，因逐条路径对存活簇/转账也正确。）

- [ ] **Step 3: 重写批量删除**

在 `TransactionDao.kt` 中，把现有 `deleteBookTransaction`（733-753 含 KDoc）与 `deleteAssetRelatedData`（826-839 含 KDoc）整体替换为下面三段：

```kotlin
    /**
     * 批量删除一组记录：逐条余额回退+清关联+删记录（无逐条重算），
     * 删完后对「存活簇」（不在待删集、与待删记录关联的记录所在簇）只重算一次。
     * 消除「每条都整簇重算」的 O(N²)；存活引用为备份恢复/导入异常的安全网（正常数据 survivors 为空）。
     */
    @Throws(DataTransactionException::class)
    @Transaction
    suspend fun deleteRecordsBatch(records: List<RecordTable>) {
        if (records.isEmpty()) return
        val deletedIds = records.mapNotNull { it.id }.toSet()
        val affectedSurvivors = LinkedHashSet<Long>()
        for (record in records) {
            val recordId = record.id ?: continue
            // 删除会清关联，先捕获两个方向上不在待删集的对端 id
            queryRelatedByRecordId(recordId).forEach {
                if (it.relatedRecordId !in deletedIds) affectedSurvivors.add(it.relatedRecordId)
            }
            queryRelatedByRelatedRecordId(recordId).forEach {
                if (it.recordId !in deletedIds) affectedSurvivors.add(it.recordId)
            }
            deleteRecordCore(record)
        }
        // 收尾：存活簇逐簇重算一次（必须在所有 deleteRecordCore 之后，否则 BFS 脏读未删记录）
        val visited = HashSet<Long>()
        for (sid in affectedSurvivors) {
            if (sid in visited) continue
            val (clusterIds, outEdges) = discoverClusterIds(sid)
            visited += clusterIds
            recalculateFinalAmountFromCluster(clusterIds, outEdges)
        }
    }

    /**
     * 事务化删除账本及其所有关联数据。
     * 逐条删记录正确回退资产余额（含跨账本转账对方资产），删后统一对存活簇重算（去 O(N²)）。
     */
    @Throws(DataTransactionException::class)
    @Transaction
    suspend fun deleteBookTransaction(bookId: Long) {
        deleteRecordsBatch(queryRecordListByBookId(bookId))
        deleteTagsByBookId(bookId)
        deleteAssetsByBookId(bookId)
        deleteBookById(bookId)
    }

    /**
     * 事务化删除资产关联的所有数据（不删资产行本身，守 AUDIT-2 契约：目标资产存活+余额回退）。
     * 逐条删记录正确回退对方资产余额（转账场景），删后统一对存活簇重算（去 O(N²)）。
     */
    @Transaction
    suspend fun deleteAssetRelatedData(assetId: Long) {
        deleteRecordsBatch(queryRecordsByAssetId(assetId))
    }
```

> 删除原 `deleteBookTransaction` 上方的旧 KDoc（733-740）与 `deleteAssetRelatedData` 上方旧 KDoc（826-831）一并替换。原 `deleteRecordsByBookId` 等批量 SQL 查询保留不删（其它处可能引用）。

- [ ] **Step 4: 运行确认全绿**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*.TransactionDaoLogicTest" --tests "*.BooksRepositoryImplTest" --offline --no-daemon --console=plain`
Expected: `BUILD SUCCESSFUL` —— 区分力测试 `...no_survivor_recalc` 转 PASS（重算 0 次）；3 个守卫测试 PASS；既有 4 删除测试 + 单删测试 + 全量测试全 PASS。

- [ ] **Step 5: 提交**

```bash
git add core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/dao/TransactionDao.kt core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/testdoubles/TransactionDaoLogicTest.kt
git commit -m "[perf|core|删除路径去O2][公共]deleteBook/deleteAsset 抽 deleteRecordsBatch:逐条 core 删+删后存活簇单次重算,消 O(N²)+区分力(重算0次)/存活簇/转账余额守卫测试"
```

---

## Task 6: L1 死代码清理

**Files:**
- Modify: `core/database/src/main/kotlin/.../dao/RecordDao.kt`（删 305）
- Modify: `FakeRecordDao.kt`（删 236-238 override）
- Modify: `RecordDaoTest.kt`（删 777-795 测试、952 改 swap、775 注释）

- [ ] **Step 1: 删生产死代码 + Fake override**

`RecordDao.kt` 删除（304-305）：
```kotlin
    @Query("SELECT * FROM db_record_with_related")
    suspend fun queryRelatedRecord(): List<RecordWithRelatedTable>
```

`FakeRecordDao.kt` 删除（236-238）：
```kotlin
    override suspend fun queryRelatedRecord(): List<RecordWithRelatedTable> {
        return relatedRecords.toList()
    }
```

- [ ] **Step 2: androidTest 收尾（compile-only）**

`RecordDaoTest.kt`：
1. 删除整个测试 `when_queryRelatedRecord_then_returnsAllRelations`（777-795）——其唯一目的是测已删的 `queryRelatedRecord()`。
2. 把 775 region 注释 `// region 补充测试：queryRelatedRecord, ...` 改为 `// region 补充测试：queryRelatedRecordCountByID, getRelatedIdListById, getRecordIdListFromRelatedId`。
3. `when_deleteRelatedWithAsset` 中 952 行 `val allRelated = recordDao.queryRelatedRecord()` 改为：
   ```kotlin
        val allRelated = transactionDao.queryAllRelatedRecords()
   ```
   （该测试 786 行已用 `transactionDao`，在 scope 内。）

- [ ] **Step 3: 编译核验（JVM 单测 + androidTest 编译）**

Run:
```bash
./gradlew :core:data:testDebugUnitTest --offline --no-daemon --console=plain
./gradlew :core:database:compileDebugAndroidTestKotlin --offline --no-daemon --console=plain
```
Expected: 两条均 `BUILD SUCCESSFUL`（`FakeRecordDao` 不再 override 不存在的方法；androidTest 编译通过）。

- [ ] **Step 4: 提交**

```bash
git add core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/dao/RecordDao.kt core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/testdoubles/FakeRecordDao.kt core/database/src/androidTest/kotlin/cn/wj/android/cashbook/core/database/dao/RecordDaoTest.kt
git commit -m "[refactor|core|删除路径去O2][公共]L1 删 RecordDao.queryRelatedRecord 死代码+FakeRecordDao override,androidTest 793 整测删除/952 改 queryAllRelatedRecords"
```

---

## Task 7: androidTest 补回滚/真机用例（compile-only）+ 全量回归

**Files:**
- Modify: `core/database/src/androidTest/kotlin/.../dao/TransactionDaoTest.kt`（新增用例）

> @Transaction 回滚行为 Fake 不建模（内存直改无回滚），故 L3 回滚守卫放真实 Room androidTest（本机只编译，有设备时跑）。

- [ ] **Step 1: 加回滚 androidTest（compile-only）**

在 `TransactionDaoTest.kt` 的 `// region 5. deleteBookTransaction 测试`（746）内追加（仿照既有用例的 insertBook/insertType/insertAsset helper）：

```kotlin
@Test
fun when_deleteBookTransaction_with_failure_then_rolled_back() = runTest {
    // L3：批量删中途异常 → 整事务回滚（删一条引用了不存在 type 的记录触发 DataTransactionException）
    val bookId = insertBook(name = "回滚账本")
    val typeId = insertType(createExpenditureType())
    insertAsset(createNormalAsset(id = 1L, booksId = bookId, balance = 100000L))
    transactionDao.insertRecordTransaction(
        record = createRecord(typeId = typeId, assetId = 1L, booksId = bookId, amount = 5000L),
        tagIdList = emptyList(),
        needRelated = false,
        relatedRecordIdList = emptyList(),
        relatedImageList = emptyList(),
    )
    // 直插一条 type_id 指向不存在类型的记录，使 deleteRecordCore 的 resolveType 抛异常
    transactionDao.insertRecord(
        createRecord(typeId = 999999L, assetId = 1L, booksId = bookId, amount = 1000L),
    )
    val before = transactionDao.queryRecordListByBookId(bookId).size

    runCatching { transactionDao.deleteBookTransaction(bookId) }

    // 事务回滚：记录与账本应原样保留
    assertThat(transactionDao.queryRecordListByBookId(bookId)).hasSize(before)
    assertThat(database.booksDao().queryAll().any { it.id == bookId }).isTrue()
}
```

> 若 `createRecord`/helper 签名与上不符，按该文件既有 helper 实际签名对齐（仅本机编译核验，不 run）。

- [ ] **Step 2: 编译核验 androidTest**

Run: `./gradlew :core:database:compileDebugAndroidTestKotlin --offline --no-daemon --console=plain`
Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 3: 全量回归 + spotless**

Run:
```bash
./gradlew :core:data:testDebugUnitTest :core:database:testDebugUnitTest --offline --no-daemon --console=plain
./gradlew spotlessCheck --init-script gradle/init.gradle.kts --no-configuration-cache --offline --no-daemon --console=plain
```
Expected: 测试 `BUILD SUCCESSFUL`；spotless 不通过则 `./gradlew spotlessApply ...` 修复后重跑。

- [ ] **Step 4: 提交**

```bash
git add core/database/src/androidTest/kotlin/cn/wj/android/cashbook/core/database/dao/TransactionDaoTest.kt
git commit -m "[test|core|删除路径去O2][公共]L3 删账本中途异常整事务回滚 androidTest(compile-verified,本机无设备未 run)"
```

---

## Self-Review（写计划后自检结果）

**1. Spec 覆盖：**
- H1 → Task 3（5 测试补 type）+ Task 4（去覆盖）；H2/H3 → Task 1（discoverClusterIds 返 outEdges + recalculateFinalAmountFromCluster 单次 BFS）；M1/AUDIT-2b → Task 5 转账守卫测试；M3/AUDIT-2 → Task 5 `..._counterpart_balance_reverted` 断言 A 行存活；M4 → §4.1 union（Task 2 实现）；M5 → 计划未引入 RecordRepositoryImplTest（已删该目标）；M6 → spec §6 已记录、Task 6 实现；M7 → Task 6（793 整删、952 swap）；L1 → Task 5 计数指标改 queryRecordByIds；L2 → Task 5 存活簇正向测试；L3 → deleteRecordCore 保留抛异常（Task 2）+ Task 7 回滚 androidTest；L5/L6 → Task 2/Task 5 注释 + deleteRecordsBatch 收尾顺序。**无遗漏。**
- §8 成功标准 1（重算次数=存活簇数）→ Task 5 区分力测试；2（行为等价）→ Task 2/5 守卫；3（不丢 outEdges）→ Task 1 测试；4（AUDIT-2/2b）→ Task 5；5（L1+编译）→ Task 6/7；6（评审）→ 节点1 已过、节点2 待 full-review。

**2. 占位符扫描：** 无 TBD/TODO；所有代码步骤含完整代码。Task 7 androidTest 标注「按既有 helper 实际签名对齐」是因 androidTest helper 未逐行读，属本机不可 run 的合理留白（compile-verified 即可）。

**3. 类型一致性：** `discoverClusterIds(seed, exclude): ClusterDiscovery`、`recalculateFinalAmountFromCluster(clusterIds, outEdges)`、`deleteRecordCore(record)`、`deleteRecordsBatch(records)`、`queryRecordByIdsCallCount` 全计划一致；解构 `val (clusterIds, outEdges) = discoverClusterIds(...)` 依赖 `ClusterDiscovery` data class（Task 1 定义）。

---

## Execution Handoff

**Plan complete and saved to `docs/superpowers/plans/2026-06-10-delete-path-on2-optimization.md`. Two execution options:**

**1. Subagent-Driven (recommended by skill)** - 每 Task 派新 subagent，Task 间评审。

**2. Inline Execution (本机推荐)** - 本会话内 controller 亲自串行执行（executing-plans），每 Task 读 plan→TDD→跑测试→commit→自核验。CLAUDE.md 实证：本机后台 Workflow 易受会话中断 + 本地 Gradle 资源累积，多 Task 含本地构建优先 controller 串行。

**Which approach?**
