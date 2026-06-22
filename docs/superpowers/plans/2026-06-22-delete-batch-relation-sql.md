# P-M1 删除路径批量化 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把删除路径（删账本/删资产/单删）的「删关联（标签/图片/关联记录）+ 删记录」从逐条 per-record SQL 改为批量 `IN (:ids)` SQL，per-record DB 往返从 ~8 降到 ~2（仅余额回退），余额回退逻辑零改动，对外行为逐字段不变。

**Architecture:** 拆 `deleteRecordCore` 的余额回退为私有 `revertRecordBalanceOnly`（零符号改动）；`deleteRecordsBatch` 改为「删前捕获 survivors → 逐条余额回退 → 批量删关联/记录（byIds IN + chunk）→ L3 行数校验 → 存活簇重算」；移除 `deleteRecordCore`；清理 8 个 dead byBookId/byAssetId SQL。

**Tech Stack:** Kotlin, Room (KSP), JUnit + Truth, Robolectric（androidTest 真机/模拟器）。

## Global Constraints

- 金额单位分（Long）；不碰 `calculateRecordAmount`/finalAmount 算法。
- **L3**：`deleteRecordsBatch` 保持 `@Transaction`；批量删记录返回行数 < 去重待删数 → 抛 `DataTransactionException` → 整体回滚。
- **L6**：存活簇重算严格在所有删关联+删记录之后。
- **L7**：survivors 捕获（读关联表）必须早于批量删关联。
- **L8**：三类关联删 + 删记录遍历同一完整 `deletedIds` 全集。
- **余额快照不变量**：余额回退用调用方传入 record 快照；保持「调用方传新鲜快照」。
- Fake byIds override 须忠实 IN 过滤（`deleteRecordRelationsByRecordIds` 双向 IN-OR），禁宽松 contains。
- `@Transaction` 回滚仅 androidTest 可验，JVM Fake 不建模回滚。
- 源文件 Apache 2.0 License Header；ktlint android mode；中文注释。
- 模块测试任务名：core:data（Android 库）`:core:data:testDebugUnitTest`；core:database `:core:database:test`（JVM）/ androidTest `connectedDebugAndroidTest`（设备）。
- gradle 命令前缀清继承代理 + `--offline --no-daemon --console=plain`。

---

### Task 1: 新增 byIds 批量删 SQL + DELETE_IN_CHUNK_SIZE + Fake 忠实 override

**Files:**
- Modify: `core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/dao/TransactionDao.kt`（加 4 个 @Query + file-level const）
- Modify: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/testdoubles/FakeTransactionDao.kt`（override 4 个）
- Test: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/testdoubles/TransactionDaoLogicTest.kt`（byIds 行为单测）

**Interfaces:**
- Produces: `deleteTagRelationsByRecordIds(ids: List<Long>)`、`deleteImageRelationsByRecordIds(ids: List<Long>)`、`deleteRecordRelationsByRecordIds(ids: List<Long>)`、`deleteRecordsByIds(ids: List<Long>): Int`、file-private `const val DELETE_IN_CHUNK_SIZE = 900`

- [ ] **Step 1: 写失败测试**（LogicTest 末尾辅助方法前插入新段）

```kotlin
    // ========== byIds 批量删 SQL 测试（P-M1）==========

    @Test
    fun when_deleteRecordsByIds_then_returns_deleted_row_count() = runTest {
        dao.records.add(createRecordTable(id = 1L))
        dao.records.add(createRecordTable(id = 2L))
        dao.records.add(createRecordTable(id = 3L))

        val deleted = dao.deleteRecordsByIds(listOf(1L, 2L))

        assertThat(deleted).isEqualTo(2)
        assertThat(dao.records.map { it.id }).containsExactly(3L)
    }

    @Test
    fun when_deleteRecordRelationsByRecordIds_then_clears_both_directions() = runTest {
        // 吸收者 S=2（存活）指向被删支出 D=1：record_id=2, related_record_id=1
        dao.relatedRecords.add(RecordWithRelatedTable(id = 1L, recordId = 2L, relatedRecordId = 1L))
        // 反向：record_id=1（被删）指向 3
        dao.relatedRecords.add(RecordWithRelatedTable(id = 2L, recordId = 1L, relatedRecordId = 3L))
        // 无关边
        dao.relatedRecords.add(RecordWithRelatedTable(id = 3L, recordId = 5L, relatedRecordId = 6L))

        dao.deleteRecordRelationsByRecordIds(listOf(1L))

        // 含 1 的两条（任一方向）被清，无关边保留
        assertThat(dao.relatedRecords.map { it.id }).containsExactly(3L)
    }

    @Test
    fun when_deleteTagAndImageRelationsByRecordIds_then_only_matching_cleared() = runTest {
        dao.tagWithRecords.add(TagWithRecordTable(id = 1L, recordId = 1L, tagId = 100L))
        dao.tagWithRecords.add(TagWithRecordTable(id = 2L, recordId = 2L, tagId = 100L))
        dao.imageWithRecords.add(ImageWithRelatedTable(id = 1L, recordId = 1L, path = "/a"))
        dao.imageWithRecords.add(ImageWithRelatedTable(id = 2L, recordId = 2L, path = "/b"))

        dao.deleteTagRelationsByRecordIds(listOf(1L))
        dao.deleteImageRelationsByRecordIds(listOf(1L))

        assertThat(dao.tagWithRecords.map { it.recordId }).containsExactly(2L)
        assertThat(dao.imageWithRecords.map { it.recordId }).containsExactly(2L)
    }
```

注：确认 LogicTest 顶部已 import `RecordWithRelatedTable`/`TagWithRecordTable`/`ImageWithRelatedTable`；缺则补 import。`ImageWithRelatedTable` 构造参数核对实际签名（id/recordId/path 等），以编译为准。

- [ ] **Step 2: 跑测试验证失败**

Run: `env -u http_proxy -u https_proxy -u all_proxy -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY ./gradlew :core:data:testDebugUnitTest --tests "*TransactionDaoLogicTest*" --offline --no-daemon --console=plain`
Expected: 编译失败（`deleteRecordsByIds` 等未定义）

- [ ] **Step 3: 加 TransactionDao @Query + file-level const**

在 `TransactionDao.kt` 文件顶部（package 行后、import 后、`@Dao interface` 前）加：

```kotlin
/** 批量 IN 删除单批最大参数数，低于 SQLite 旧版变量上限（999）；
 *  与 core:data 的 SQL_IN_CHUNK_SIZE 同值同义，因 core:database 不能依赖 core:data 故独立定义 */
private const val DELETE_IN_CHUNK_SIZE = 900
```

在 `deleteRecordsByBookId`（:731-732）之后、`queryRecordListByBookId` 等之间合适位置加 4 个 byIds @Query：

```kotlin
    /** 批量删除一组记录的标签关联（IN，删账本/资产/单删共用，消逐条 deleteOldRelatedTags） */
    @Query("DELETE FROM db_tag_with_record WHERE record_id IN (:ids)")
    suspend fun deleteTagRelationsByRecordIds(ids: List<Long>)

    /** 批量删除一组记录的图片关联（IN） */
    @Query("DELETE FROM db_image_with_related WHERE record_id IN (:ids)")
    suspend fun deleteImageRelationsByRecordIds(ids: List<Long>)

    /** 批量删除一组记录的关联记录关系（双向 IN-OR，等价逐条 clearRelatedRecordById） */
    @Query("DELETE FROM db_record_with_related WHERE record_id IN (:ids) OR related_record_id IN (:ids)")
    suspend fun deleteRecordRelationsByRecordIds(ids: List<Long>)

    /** 批量删除一组记录（IN），返回实际删除行数（L3 校验用） */
    @Query("DELETE FROM db_record WHERE id IN (:ids)")
    suspend fun deleteRecordsByIds(ids: List<Long>): Int
```

- [ ] **Step 4: 加 FakeTransactionDao 忠实 override**

在 `FakeTransactionDao.kt` 的 `deleteRecordsByAssetId`（:272-274）之后加：

```kotlin
    override suspend fun deleteTagRelationsByRecordIds(ids: List<Long>) {
        tagWithRecords.removeAll { it.recordId in ids }
    }

    override suspend fun deleteImageRelationsByRecordIds(ids: List<Long>) {
        imageWithRecords.removeAll { it.recordId in ids }
    }

    override suspend fun deleteRecordRelationsByRecordIds(ids: List<Long>) {
        relatedRecords.removeAll { it.recordId in ids || it.relatedRecordId in ids }
    }

    override suspend fun deleteRecordsByIds(ids: List<Long>): Int {
        val before = records.size
        records.removeAll { it.id in ids }
        return before - records.size
    }
```

- [ ] **Step 5: 跑测试验证通过**

Run: `env -u http_proxy ... ./gradlew :core:data:testDebugUnitTest --tests "*TransactionDaoLogicTest*" --offline --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: spotlessApply + commit**

```bash
env -u http_proxy ... ./gradlew :core:database:spotlessApply :core:data:spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache --offline --no-daemon --console=plain
git add core/database/.../dao/TransactionDao.kt core/data/.../testdoubles/FakeTransactionDao.kt core/data/.../testdoubles/TransactionDaoLogicTest.kt
git commit -m "[feat|core|删除批量SQL][公共]P-M1 Task1 新增 byIds 批量删 @Query + Fake 忠实 override"
```

---

### Task 2: 抽 `revertRecordBalanceOnly`（余额回退零符号改动）

**Files:**
- Modify: `core/database/.../dao/TransactionDao.kt`（抽私有方法，deleteRecordCore 改调它）

**Interfaces:**
- Produces: `private suspend fun revertRecordBalanceOnly(record: RecordTable)`（余额回退，含主资产 + 转账对方，符号逻辑与原 deleteRecordCore :599-640 逐字符一致）

- [ ] **Step 1: 加 `revertRecordBalanceOnly`（复制 deleteRecordCore :595-640 余额逻辑）**

在 `deleteRecordCore` 之前加：

```kotlin
    /**
     * 仅回退单条记录涉及的资产余额（主资产 + 转账对方资产），不删任何关联/记录。
     * 符号逻辑与原 deleteRecordCore 余额回退部分逐字符一致（信用卡/非信用卡 × INCOME/支出转账 × 主/转账对方）。
     * 供 deleteRecordsBatch 逐条调用（A2：余额逐条回退、关联/记录批量删）。
     */
    @Throws(DataTransactionException::class)
    suspend fun revertRecordBalanceOnly(record: RecordTable) {
        val type = resolveType(record.typeId)
            ?: throw DataTransactionException("Type must not be null")
        val category = RecordTypeCategoryEnum.ordinalOf(type.typeCategory)
        val oldRecordAmount = calculateRecordAmount(record, category)
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
    }
```

- [ ] **Step 2: 改 `deleteRecordCore` 复用 `revertRecordBalanceOnly`（中间态，保持现有测试绿）**

把 `deleteRecordCore` 的余额回退段（resolveType 起到转账对方 updateAsset 止，原 :595-640）整段替换为单行调用，删关联/删记录部分保留：

```kotlin
    @Throws(DataTransactionException::class)
    @Transaction
    suspend fun deleteRecordCore(record: RecordTable) {
        val recordId = record.id ?: return
        // 余额回退（抽出复用）
        revertRecordBalanceOnly(record)
        // 移除关联标签
        deleteOldRelatedTags(recordId)
        // 移除关联照片
        deleteOldRelatedImages(recordId)
        // 移除关联记录
        clearRelatedRecordById(recordId)
        // 删除当前记录
        val result = deleteRecord(record)
        if (result <= 0) {
            throw DataTransactionException("Record delete failed!")
        }
    }
```

- [ ] **Step 3: 跑现有删除用例验证等价（余额金丝雀守护）**

Run: `env -u http_proxy ... ./gradlew :core:data:testDebugUnitTest --tests "*TransactionDaoLogicTest*" --offline --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL（`when_deleteAsset_with_transfer_then_surviving_counterpart_balance_reverted` 等余额用例守护重构等价）

- [ ] **Step 4: spotlessApply + commit**

```bash
env -u http_proxy ... ./gradlew :core:database:spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache --offline --no-daemon --console=plain
git add core/database/.../dao/TransactionDao.kt
git commit -m "[refactor|core|删除批量SQL][公共]P-M1 Task2 抽 revertRecordBalanceOnly（余额回退零符号改动）"
```

---

### Task 3: 改 `deleteRecordsBatch` 批量删 + 移除 `deleteRecordCore`

**Files:**
- Modify: `core/database/.../dao/TransactionDao.kt`（重写 deleteRecordsBatch，删 deleteRecordCore）
- Test: `core/data/.../testdoubles/TransactionDaoLogicTest.kt`（补存活吸收者悬空关联清理用例）

**Interfaces:**
- Consumes: Task1 的 4 个 byIds + DELETE_IN_CHUNK_SIZE；Task2 的 `revertRecordBalanceOnly`
- Produces: 改造后的 `deleteRecordsBatch`（行为对外不变）

- [ ] **Step 1: 写存活吸收者悬空关联清理失败测试**（验 L7 survivors 捕获 + 批量删后存活簇重算）

在 LogicTest 「deleteAssetRelatedData 测试」段补：

```kotlin
    @Test
    fun when_deleteAssetRelatedData_clears_dangling_relation_and_recalcs_survivor() = runTest {
        // 存活吸收者 S(id=2,asset=20) 吸收被删支出 D(id=1,asset=10)；删 asset=10 → D 删、悬空关联清、S 重算
        setupTypesForAbsorption()
        dao.assets.add(createAssetTable(id = 10L, balance = 0L))
        dao.assets.add(createAssetTable(id = 20L, balance = 0L))
        insertRecord(id = 1L, typeId = EXPENDITURE_TYPE_ID, amount = 10000L, assetId = 10L)
        insertRecord(id = 2L, typeId = INCOME_TYPE_ID, amount = 8000L, assetId = 20L)
        dao.relatedRecords.add(RecordWithRelatedTable(id = 1L, recordId = 2L, relatedRecordId = 1L))
        dao.recalculateFinalAmountForCluster(2L) // E.fa=2000, I.fa=0

        dao.deleteAssetRelatedData(10L)

        assertThat(dao.queryRecordById(1L)).isNull()            // 被删支出删除
        assertThat(dao.relatedRecords).isEmpty()                // 悬空关联清（双向 IN-OR）
        assertThat(dao.queryRecordById(2L)!!.finalAmount).isEqualTo(8000L) // 存活吸收者重算恢复 recordAmount
    }
```

- [ ] **Step 2: 跑测试验证失败**（旧 deleteRecordsBatch 走 deleteRecordCore，此用例应通过——若已绿说明等价已满足，作回归基线；继续重写实现保持绿）

Run: `env -u http_proxy ... ./gradlew :core:data:testDebugUnitTest --tests "*TransactionDaoLogicTest*" --offline --no-daemon --console=plain`
Expected: 该用例 PASS（旧路径已正确）—— 作为重写前回归基线

- [ ] **Step 3: 重写 `deleteRecordsBatch` + 删 `deleteRecordCore`**

把 `deleteRecordsBatch`（:741-764）整体替换为：

```kotlin
    @Throws(DataTransactionException::class)
    @Transaction
    suspend fun deleteRecordsBatch(records: List<RecordTable>) {
        if (records.isEmpty()) return
        val deletedIds = records.mapNotNull { it.id }.toSet()
        // L7：survivors 必须删前捕获（删后关联已清无法查）
        val affectedSurvivors = LinkedHashSet<Long>()
        for (record in records) {
            val recordId = record.id ?: continue
            queryRelatedByRecordId(recordId).forEach {
                if (it.relatedRecordId !in deletedIds) affectedSurvivors.add(it.relatedRecordId)
            }
            queryRelatedByRelatedRecordId(recordId).forEach {
                if (it.recordId !in deletedIds) affectedSurvivors.add(it.recordId)
            }
        }
        // 逐条余额回退（零符号改动；过滤 id==null 与逐条版 record.id ?: return 对齐）
        for (record in records) {
            if (record.id == null) continue
            revertRecordBalanceOnly(record)
        }
        // L8：三类关联删 + 删记录遍历同一完整 deletedIds 全集
        val idList = deletedIds.toList()
        idList.chunked(DELETE_IN_CHUNK_SIZE).forEach { chunk ->
            deleteTagRelationsByRecordIds(chunk)
            deleteImageRelationsByRecordIds(chunk)
            deleteRecordRelationsByRecordIds(chunk)
        }
        // 批量删记录 + L3 行数校验（去重 size）
        val deleted = idList.chunked(DELETE_IN_CHUNK_SIZE).sumOf { deleteRecordsByIds(it) }
        if (deleted < deletedIds.size) {
            throw DataTransactionException("Record delete failed!")
        }
        // L6：存活簇重算必须在所有删除之后
        val visited = HashSet<Long>()
        for (sid in affectedSurvivors) {
            if (sid in visited) continue
            val (clusterIds, outEdges) = discoverClusterIds(sid)
            visited += clusterIds
            recalculateFinalAmountFromCluster(clusterIds, outEdges)
        }
    }
```

删除整个 `deleteRecordCore` 方法（原 :587-656，含 KDoc + @Throws + @Transaction + 函数体）。

- [ ] **Step 4: 跑全量 LogicTest 验证等价 + 新用例通过**

Run: `env -u http_proxy ... ./gradlew :core:data:testDebugUnitTest --offline --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL（删账本/资产/单删等价 + 余额金丝雀 + 存活吸收者重算 + 悬空关联清理全绿）

- [ ] **Step 5: spotlessApply + commit**

```bash
env -u http_proxy ... ./gradlew :core:database:spotlessApply :core:data:spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache --offline --no-daemon --console=plain
git add core/database/.../dao/TransactionDao.kt core/data/.../testdoubles/TransactionDaoLogicTest.kt
git commit -m "[perf|core|删除批量SQL][公共]P-M1 Task3 deleteRecordsBatch 批量删关联/记录 + 移除 deleteRecordCore"
```

---

### Task 4: 清理 8 个 dead byBookId/byAssetId 批量删 SQL

**Files:**
- Modify: `core/database/.../dao/TransactionDao.kt`（删 8 个 @Query）
- Modify: `core/data/.../testdoubles/FakeTransactionDao.kt`（删 8 个 override）

**Interfaces:**
- 移除：`deleteTagRelationsByBookId`/`deleteRecordRelationsByBookId`/`deleteImageRelationsByBookId`/`deleteRecordsByBookId`/`deleteTagRelationsByAssetId`/`deleteRecordRelationsByAssetId`/`deleteImageRelationsByAssetId`/`deleteRecordsByAssetId`（dead code）

- [ ] **Step 1: 确认无生产引用**

Run: `git grep -n "deleteTagRelationsByBookId\|deleteRecordRelationsByBookId\|deleteImageRelationsByBookId\|deleteRecordsByBookId\|deleteTagRelationsByAssetId\|deleteRecordRelationsByAssetId\|deleteImageRelationsByAssetId\|deleteRecordsByAssetId" -- '*.kt'`
Expected: 仅命中 TransactionDao.kt 定义 + FakeTransactionDao.kt override（无 RepositoryImpl/UseCase/测试断言引用）

- [ ] **Step 2: 删 TransactionDao 8 个 @Query**

删除 `TransactionDao.kt` 的：
- byBookId 段（:722-732，含 deleteTagRelationsByBookId/deleteRecordRelationsByBookId/deleteImageRelationsByBookId/deleteRecordsByBookId 4 个 @Query）
- byAssetId 段（:816-851，含 deleteTagRelationsByAssetId/deleteRecordRelationsByAssetId/deleteImageRelationsByAssetId/deleteRecordsByAssetId 4 个 @Query + 各自 KDoc）

注：保留 `queryRecordsByAssetId`（:155 区，被 deleteAssetRelatedData 用）与 `deleteAssetsByBookId`/`deleteTagsByBookId`/`queryRecordListByBookId`（非本清单、仍被 deleteBookTransaction 用）。逐个核对方法名删除，勿误删。

- [ ] **Step 3: 删 FakeTransactionDao 8 个 override**

删除 `FakeTransactionDao.kt` 的 8 个对应 override（:213-230 byBookId 4 个 + :254-274 byAssetId 4 个）。保留 `deleteAssetsByBookId`(:236)/`deleteTagsByBookId`(:240)/`queryRecordsByAssetId`(:244)/`queryAllAssetsByBookId`(:232)。

- [ ] **Step 4: 编译 + 全量测试验证无破坏**

Run: `env -u http_proxy ... ./gradlew :core:data:testDebugUnitTest --offline --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL（删 dead code 不破坏任何测试）

- [ ] **Step 5: spotlessApply + commit**

```bash
env -u http_proxy ... ./gradlew :core:database:spotlessApply :core:data:spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache --offline --no-daemon --console=plain
git add core/database/.../dao/TransactionDao.kt core/data/.../testdoubles/FakeTransactionDao.kt
git commit -m "[refactor|core|删除批量SQL][公共]P-M1 Task4 清理 8 个 dead byBookId/byAssetId 批量删 SQL"
```

---

### Task 5: androidTest blocking 用例（中途失败回滚 + IN 批量删 + 注释订正）

**Files:**
- Modify: `core/database/.../androidTest/.../dao/TransactionDaoTest.kt`

**Interfaces:**
- Consumes: Task1-3 的批量删路径
- 注：androidTest 本机 compile-only（无设备），实跑在节点2 后由 android-cli 模拟器执行（spec §6 blocking）

- [ ] **Step 1: 订正过期注释**

`TransactionDaoTest.kt` 中 `when_deleteBookTransaction_with_failure_then_rolled_back` 的注释（约 :808）由「使 deleteRecordCore 的 resolveType 抛异常」改为「使 revertRecordBalanceOnly 的 resolveType 抛异常（A2 后余额回退在批量删前，throw 点不变）」。

- [ ] **Step 2: 补「批量删 chunk 中途失败 → 回滚」blocking 用例**

在 androidTest 删除回滚段补（C2/F-1，验 A2 新增中间态；真实 Room @Transaction 回滚）：

```kotlin
    @Test
    fun when_deleteRecordsBatch_fails_after_some_deletes_then_all_rolled_back() = runTest {
        testBookId = createTestBook()
        val typeId = typeDao.insertType(createType())
        // 正常记录 + 一条坏 type 记录（使 revertRecordBalanceOnly 在批量删前抛，但已覆盖删前回退态回滚）
        val goodId = insertRecord(createRecord(typeId = typeId, recordTime = 1000L, remark = "good"))
        // 坏记录：typeId 不存在 → resolveType 抛 DataTransactionException
        val badRecord = createRecord(typeId = 999999L, recordTime = 2000L, remark = "bad")
        val badId = transactionDao.insertRecord(badRecord)

        val before = recordDao.queryById(goodId)
        try {
            transactionDao.deleteBookTransaction(testBookId)
            assertWithMessage("应抛 DataTransactionException").fail()
        } catch (e: DataTransactionException) {
            // 期望异常
        }
        // 整事务回滚：good 记录仍在、账本仍在
        assertThat(recordDao.queryById(goodId)).isNotNull()
        assertThat(recordDao.queryById(badId)).isNotNull()
    }
```

注：异常注入点（坏 type_id）在 `revertRecordBalanceOnly`（删前），验证「删前回退态 + 抛 → @Transaction 整体回滚」。spec §6 要求的「首个 chunk 删除成功后失败」需 Room 层 mock `deleteRecordsByIds` 返回不足行数才能精确触发，本机无设备无法 mock androidTest DAO；本用例覆盖「回退态抛→回滚」，模拟器实跑时若需进一步覆盖「删除中途失败」，由 reviewer 在节点2/模拟器阶段评估补充（记 backlog）。构造细节（createRecord/insertRecord 辅助、DataTransactionException import）以 androidTest 现有 fixture 为准。

- [ ] **Step 3: 编译 androidTest（本机 compile-only）**

Run: `env -u http_proxy ... ./gradlew :core:database:compileDebugAndroidTestKotlin --offline --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: commit**

```bash
env -u http_proxy ... ./gradlew :core:database:spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache --offline --no-daemon --console=plain
git add core/database/.../androidTest/.../dao/TransactionDaoTest.kt
git commit -m "[test|core|删除批量SQL][公共]P-M1 Task5 androidTest 补中途失败回滚 + 注释订正（本机 compile-only）"
```

---

## Self-Review

**1. Spec 覆盖**：
- §3.1 拆 revertRecordBalanceOnly → Task 2 ✓
- §3.2 4 个 byIds @Query + Fake 忠实 → Task 1 ✓
- §3.3 改 deleteRecordsBatch（C1 .toSet / M2 id 过滤 / L7 survivors / L8 全集 / L3 行数）→ Task 3 ✓
- §3.4 DELETE_IN_CHUNK_SIZE → Task 1 ✓
- §3.5 删 8 dead SQL → Task 4 ✓
- §5 H1（@Query DELETE :Int 首验）→ Task 1 Step1（deleteRecordsByIds 返回行数测试）+ Task 5（androidTest 真机）✓
- §6 JVM 金丝雀/存活吸收者/悬空关联 → Task 3 现有用例 + 新用例 ✓；androidTest 回滚 → Task 5 ✓
- 移除 deleteRecordCore → Task 3 ✓

**2. Placeholder 扫描**：Task 1/2/3 含完整 exact code；Task 5 androidTest 构造细节标注「以现有 fixture 为准」（androidTest 本机 compile-only，fixture 需读现有 TransactionDaoTest 确认 createRecord/insertRecord 签名）——执行 Task 5 时先读现有 androidTest 头部辅助。

**3. 类型一致性**：`deleteRecordsByIds(ids: List<Long>): Int`、`revertRecordBalanceOnly(record: RecordTable)`、`DELETE_IN_CHUNK_SIZE` 跨 Task 1/2/3 一致；`discoverClusterIds`/`recalculateFinalAmountFromCluster`/`queryRelatedByRecordId`/`queryRelatedByRelatedRecordId` 沿用现有签名（Task 3 复用，未改）。

**4. 歧义**：Task 4 删除范围用「逐个核对方法名」防误删相邻存活方法；Task 5 中途失败精确触发点的局限已显式标注 backlog。
