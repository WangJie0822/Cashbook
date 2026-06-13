# 待报销管理界面 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 Cashbook 新增「待报销管理界面」——左抽屉入口，列出当前账本全部「可报销且未关联任何报销/退款款」的支出记录，顶部显示笔数与合计金额，点击打开记录详情弹窗。

**Architecture:** 自底向上分层（database DAO → data Repository → domain UseCase → feature UI → app 接线）。核心查询用纯 SQL `NOT EXISTS` 一次过滤未关联记录（返回 `List<RecordTable>`，复用 `RecordTable.asModel` + `RecordModelTransToViewsUseCase`，与现有 `GetRelatedRecordViewsUseCase` 同构），避免逐条 `queryRelatedRecordCountById` 的 N+1。除三处既有签名扩展（抽屉回调链、`RecordRepository` 接口、`LauncherScreen` 系列），其余纯新增。

**Tech Stack:** Kotlin、Jetpack Compose、Room、Hilt、Navigation Compose、Coroutines/Flow、JUnit4 + Truth、Roborazzi（截图测试）。

**Spec:** `docs/superpowers/specs/2026-06-13-reimbursement-management-design.md`（已过节点1 team-review 四维，finding 已并入）。

**前置事实（已 hands-on 核验，实施时无需重新探索）：**
- `RecordTable.asModel()` 存在且字段完整：`core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/RecordRepository.kt:198-213`
- `RecordViewsModel.asEntity()` 存在：`core/model/.../model/transfer/ModelTransfer.kt:77`
- `recordDataVersion` 是 `core:common` 顶层全局 `StateFlow<Int>`：`core/common/.../model/DataVersion.kt`，import 路径 `cn.wj.android.cashbook.core.common.model.recordDataVersion`
- `SWITCH_INT_ON`=1 / `SWITCH_INT_OFF`=0：`cn.wj.android.cashbook.core.common`
- `RecordTypeCategoryEnum.EXPENDITURE.ordinal` 用于 type_category 过滤
- 测试工厂 `createRecordModel(id, booksId, typeId, ..., finalAmount, ..., reimbursable, recordTime)` / `createRecordTypeModel` / `createRecordViewsModel`：`core/testing/.../data/TestDataFactory.kt`
- 模块测试任务：JVM 库 `core:model` 用 `:test`；Android 库（`core:data`/`core:domain`/`feature:*`/`core:testing` 消费方）用 `:testDebugUnitTest`；`core:database` DAO 测试是 androidTest（本机无设备 → compile-verify + 真机补跑）

**本机执行约束（controller 实施时遵守）：** 跑 Gradle 前先查内存（CLAUDE.local.md「Gradle/JVM 高内存命令审慎执行」）；后台 build 只信 `grep -E '^BUILD (SUCCESSFUL|FAILED)'`；spotless 用 `--init-script gradle/init.gradle.kts --no-configuration-cache`。

---

## Phase 1 — 数据层（database DAO + data Repository + 两个 Fake 忠实桩）

### Task 1.1: RecordDao.queryReimbursableUnrelated（DAO 方法 + androidTest）

**Files:**
- Modify: `core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/dao/RecordDao.kt`（在 `getExpenditureReimburseRecordListAfterTime` 之后，约 line 346 后）
- Test: `core/database/src/androidTest/kotlin/cn/wj/android/cashbook/core/database/dao/RecordDaoTest.kt`（新增 region）

- [ ] **Step 1: 写失败的 androidTest**

在 `RecordDaoTest.kt` 末尾 `queryEarliestRecordTime` region 前插入（复用现有 `createTestBook`/`createType`/`createRecord`/`insertRecord` helper；关联记录用 `transactionDao` 插入——参照本文件 `queryRelatedById` 测试的关联插入方式，若 `transactionDao.insertRecord` 后需建立关联，用 `database.recordDao()` 或 `transactionDao` 的关联插入 API）：

```kotlin
    // region 12. queryReimbursableUnrelated（待报销管理界面）

    @Test
    fun when_queryReimbursableUnrelated_then_returnsReimbursableUnrelatedExpenditureOnly() = runTest {
        testBookId = createTestBook()
        val expTypeId = typeDao.insertType(
            createType(typeCategory = RecordTypeCategoryEnum.EXPENDITURE.ordinal),
        )
        val incomeTypeId = typeDao.insertType(
            createType(name = "工资", typeCategory = RecordTypeCategoryEnum.INCOME.ordinal),
        )

        // 命中：可报销 + 支出 + 未关联
        val hit = insertRecord(
            createRecord(typeId = expTypeId, reimbursable = SWITCH_INT_ON, recordTime = 9000L, remark = "待报销A"),
        )
        // 排除：不可报销
        insertRecord(
            createRecord(typeId = expTypeId, reimbursable = SWITCH_INT_OFF, recordTime = 8000L, remark = "不可报销"),
        )
        // 排除：可报销但是收入类型（reimbursable 误置）
        insertRecord(
            createRecord(typeId = incomeTypeId, reimbursable = SWITCH_INT_ON, recordTime = 7000L, remark = "收入误置"),
        )

        val result = recordDao.queryReimbursableUnrelated(testBookId)

        assertThat(result).hasSize(1)
        assertThat(result[0].id).isEqualTo(hit)
        assertThat(result[0].remark).isEqualTo("待报销A")
    }

    @Test
    fun when_queryReimbursableUnrelated_then_excludesRecordsRelatedEitherDirection() = runTest {
        testBookId = createTestBook()
        val expTypeId = typeDao.insertType(
            createType(typeCategory = RecordTypeCategoryEnum.EXPENDITURE.ordinal),
        )
        val incomeTypeId = typeDao.insertType(
            createType(name = "报销款", typeCategory = RecordTypeCategoryEnum.INCOME.ordinal),
        )

        // 作为被吸收支出（related_record_id）被关联 → 应排除
        val absorbed = insertRecord(
            createRecord(typeId = expTypeId, reimbursable = SWITCH_INT_ON, recordTime = 9000L, remark = "已被报销"),
        )
        val absorber = insertRecord(
            createRecord(typeId = incomeTypeId, reimbursable = SWITCH_INT_ON, recordTime = 9500L, remark = "报销款"),
        )
        // 建立关联：absorber(record_id) -> absorbed(related_record_id)
        transactionDao.insertRelatedRecord(
            RecordWithRelatedTable(id = null, recordId = absorber, relatedRecordId = absorbed),
        )

        // 未关联可报销支出 → 命中
        val free = insertRecord(
            createRecord(typeId = expTypeId, reimbursable = SWITCH_INT_ON, recordTime = 8000L, remark = "未报销"),
        )

        val result = recordDao.queryReimbursableUnrelated(testBookId)

        assertThat(result.map { it.id }).containsExactly(free)
    }

    @Test
    fun when_queryReimbursableUnrelated_then_excludesOtherBooksAndSortsDesc() = runTest {
        testBookId = createTestBook()
        val otherBookId = createTestBook()
        val expTypeId = typeDao.insertType(
            createType(typeCategory = RecordTypeCategoryEnum.EXPENDITURE.ordinal),
        )

        val older = insertRecord(
            createRecord(typeId = expTypeId, reimbursable = SWITCH_INT_ON, recordTime = 1000L, remark = "早"),
        )
        val newer = insertRecord(
            createRecord(typeId = expTypeId, reimbursable = SWITCH_INT_ON, recordTime = 9000L, remark = "晚"),
        )
        // 他账本可报销 → 排除
        insertRecord(
            createRecord(typeId = expTypeId, booksId = otherBookId, reimbursable = SWITCH_INT_ON, recordTime = 5000L, remark = "他账本"),
        )

        val result = recordDao.queryReimbursableUnrelated(testBookId)

        assertThat(result.map { it.id }).containsExactly(newer, older).inOrder()
    }

    // endregion
```

> 注：`transactionDao.insertRelatedRecord(RecordWithRelatedTable(...))` 为关联插入 API。实施时若该方法名与实际不符，用 `TransactionDao` 中实际的关联插入方法（grep `RecordWithRelatedTable` 于 `TransactionDao.kt` 确认，常见名 `insertRelatedRecord`/`insertRecordRelated`）；`RecordWithRelatedTable` import 自 `core.database.table`。

- [ ] **Step 2: 跑测试确认失败（编译失败：方法不存在）**

Run: `./gradlew :core:database:compileDebugAndroidTestKotlin`
Expected: 编译失败 `unresolved reference: queryReimbursableUnrelated`

- [ ] **Step 3: 实现 DAO 方法**

在 `RecordDao.kt` 的 `getExpenditureReimburseRecordListAfterTime`（约 line 343-346）之后插入：

```kotlin
    /** 查询当前账本全部「可报销支出且未关联任何报销/退款款」的记录（待报销管理界面用），按时间倒序、无 LIMIT */
    @Query(
        """
        SELECT * FROM db_record
        WHERE books_id = :booksId
        AND reimbursable = $SWITCH_INT_ON
        AND type_id IN (SELECT id FROM db_type WHERE type_category = :expenditureCategory)
        AND NOT EXISTS (
            SELECT 1 FROM db_record_with_related r
            WHERE r.record_id = db_record.id OR r.related_record_id = db_record.id
        )
        ORDER BY record_time DESC
    """,
    )
    suspend fun queryReimbursableUnrelated(
        booksId: Long,
        expenditureCategory: Int = RecordTypeCategoryEnum.EXPENDITURE.ordinal,
    ): List<RecordTable>
```

> `SWITCH_INT_ON` 已 import（RecordDao.kt:23）；`RecordTypeCategoryEnum` 已 import（:29）。

- [ ] **Step 4: 跑编译验证（本机无设备，compile-verify）**

Run: `./gradlew :core:database:compileDebugAndroidTestKotlin`
Expected: BUILD SUCCESSFUL（androidTest 真机执行留待有设备时补跑，在 commit message 标注）

- [ ] **Step 5: Commit**

```bash
git add core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/dao/RecordDao.kt \
        core/database/src/androidTest/kotlin/cn/wj/android/cashbook/core/database/dao/RecordDaoTest.kt
git commit -F - <<'EOF'
[feat|reimbursement|待报销查询][公共]新增 RecordDao.queryReimbursableUnrelated

待报销管理界面 Phase1。当前账本+reimbursable+EXPENDITURE+NOT EXISTS 双向未关联，时间倒序无 LIMIT。androidTest 3 用例（命中过滤/双向关联排除/他账本+排序），本机无设备 compile-verified，真机补跑。

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
EOF
```

---

### Task 1.2: FakeRecordDao.queryReimbursableUnrelated 忠实桩

**Files:**
- Modify: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/testdoubles/FakeRecordDao.kt`

- [ ] **Step 1: 实现忠实桩（FakeRecordDao 是接口实现，加新方法即编译需要；此 Fake 的验证由 Task 1.3 的 RecordRepositoryImplTest 驱动）**

在 `FakeRecordDao.kt` 的 `getExpenditureReimburseRecordListAfterTime`（约 line 283-292）之后插入忠实桩（双向 `relatedRecords` 排除 + type_category 过滤 + reimbursable，**勿照抄 `query` 的 emptyList 空桩**）：

```kotlin
    override suspend fun queryReimbursableUnrelated(
        booksId: Long,
        expenditureCategory: Int,
    ): List<RecordTable> {
        val expenditureTypeIds = types
            .filter { it.typeCategory == expenditureCategory }
            .mapNotNull { it.id }
        return records.filter { record ->
            record.booksId == booksId &&
                record.reimbursable == SWITCH_INT_ON &&
                record.typeId in expenditureTypeIds &&
                relatedRecords.none { it.recordId == record.id || it.relatedRecordId == record.id }
        }.sortedByDescending { it.recordTime }
    }
```

> `SWITCH_INT_ON` 已 import（FakeRecordDao.kt:21）；`types`（FakeTypeEntry，含 `typeCategory`）、`relatedRecords`（RecordWithRelatedTable）已是字段。

- [ ] **Step 2: 跑编译确认 Fake 接口完整**

Run: `./gradlew :core:data:compileDebugUnitTestKotlin`
Expected: BUILD SUCCESSFUL（接口方法已实现，无 "class must be abstract" 错误）

- [ ] **Step 3: Commit（与 Task 1.3 合并提交，见 Task 1.3 Step 5）**

暂不单独 commit，随 Task 1.3 一起提交（Fake 桩 + 其驱动测试同一原子提交）。

---

### Task 1.3: RecordRepository.getReimbursableUnrelatedRecordList（接口 + 实现 + 测试）

**Files:**
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/RecordRepository.kt`（接口 line 118 区域 + 实现 line 415 区域）
- Test: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordRepositoryImplTest.kt`

> 注：若 `RecordRepositoryImpl` 因依赖多无法直接实例化（既往记录其测试降级），则本 Task 测试改为**直接验证 `FakeRecordDao.queryReimbursableUnrelated` 的过滤语义**（Task 1.2 的桩）。下方 Step 1 给 Fake 直测版本，确定可行。

- [ ] **Step 1: 写失败的测试（验证 FakeRecordDao 忠实桩的过滤语义）**

在 `RecordRepositoryImplTest.kt` 新增（参照本文件 line 405 区域 `queryReimburseByBooksIdAfterDate` 测试的 `createXxx` 局部构造 + `FakeRecordDao` 用法）：

```kotlin
    @Test
    fun when_queryReimbursableUnrelated_then_filters_reimbursable_expenditure_unrelated() = runTest {
        val dao = FakeRecordDao()
        // 类型：1=支出，2=收入
        dao.types.add(FakeRecordDao.FakeTypeEntry(id = 1L, parentId = -1L, typeCategory = RecordTypeCategoryEnum.EXPENDITURE.ordinal))
        dao.types.add(FakeRecordDao.FakeTypeEntry(id = 2L, parentId = -1L, typeCategory = RecordTypeCategoryEnum.INCOME.ordinal))

        val hit = dao.addRecord(createRecordTable(id = null, typeId = 1L, booksId = 1L, reimbursable = SWITCH_INT_ON, recordTime = 9000L))
        dao.addRecord(createRecordTable(id = null, typeId = 1L, booksId = 1L, reimbursable = SWITCH_INT_OFF, recordTime = 8000L)) // 不可报销
        dao.addRecord(createRecordTable(id = null, typeId = 2L, booksId = 1L, reimbursable = SWITCH_INT_ON, recordTime = 7000L)) // 收入
        val absorbed = dao.addRecord(createRecordTable(id = null, typeId = 1L, booksId = 1L, reimbursable = SWITCH_INT_ON, recordTime = 6000L))
        dao.relatedRecords.add(RecordWithRelatedTable(id = 1L, recordId = 99L, relatedRecordId = absorbed.id!!)) // 被吸收 → 排除

        val result = dao.queryReimbursableUnrelated(booksId = 1L)

        assertThat(result.map { it.id }).containsExactly(hit.id)
    }
```

辅助构造（若本文件未有 `createRecordTable`，在文件内新增 private helper，参照 `RecordTable` 12 字段）：

```kotlin
    private fun createRecordTable(
        id: Long? = null,
        typeId: Long = 1L,
        assetId: Long = -1L,
        intoAssetId: Long = -1L,
        booksId: Long = 1L,
        amount: Long = 10000L,
        finalAmount: Long = 10000L,
        concessions: Long = 0L,
        charge: Long = 0L,
        remark: String = "",
        reimbursable: Int = SWITCH_INT_OFF,
        recordTime: Long = 1000L,
    ) = RecordTable(id, typeId, assetId, intoAssetId, booksId, amount, finalAmount, concessions, charge, remark, reimbursable, recordTime)
```

> import：`RecordWithRelatedTable`、`RecordTable` 自 `core.database.table`；`SWITCH_INT_ON`/`SWITCH_INT_OFF` 自 `core.common`；`RecordTypeCategoryEnum` 自 `core.model.enums`。

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*RecordRepositoryImplTest*"`
Expected: 编译失败 `unresolved reference: queryReimbursableUnrelated`（Task 1.2 桩若未提交则此处先补；TDD 顺序：Task 1.2 桩 + 本测试一起红→绿）

- [ ] **Step 3: 加 Repository 接口方法 + 实现**

`RecordRepository.kt` 接口（在 `getLastThreeMonthReimbursableRecordList` 声明附近，约 line 118 后）：

```kotlin
    /** 当前账本全部「可报销且未关联任何报销/退款款」的支出记录（待报销管理界面用） */
    suspend fun getReimbursableUnrelatedRecordList(): List<RecordModel>
```

`RecordRepositoryImpl`（在 `getLastThreeMonthReimbursableRecordList` 实现附近，约 line 424 后）：

```kotlin
    override suspend fun getReimbursableUnrelatedRecordList(): List<RecordModel> =
        withContext(coroutineContext) {
            val appDataModel = combineProtoDataSource.recordSettingsData.first()
            recordDao.queryReimbursableUnrelated(booksId = appDataModel.currentBookId)
                .map { it.asModel() }
        }
```

- [ ] **Step 4: 跑测试确认通过**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*RecordRepositoryImplTest*"`
Expected: BUILD SUCCESSFUL，新测试 PASS

- [ ] **Step 5: Commit（含 Task 1.2 Fake 桩）**

```bash
git add core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/RecordRepository.kt \
        core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/testdoubles/FakeRecordDao.kt \
        core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordRepositoryImplTest.kt
git commit -F - <<'EOF'
[feat|reimbursement|待报销查询][公共]RecordRepository.getReimbursableUnrelatedRecordList + Fake 忠实桩

待报销管理界面 Phase1。接口+impl 委托 currentBookId 调 DAO；FakeRecordDao 忠实桩双向 relatedRecords 排除+type_category+reimbursable；RecordRepositoryImplTest 验过滤语义。

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
EOF
```

---

### Task 1.4: FakeRecordRepository.getReimbursableUnrelatedRecordList 双向忠实桩（core:testing）

**Files:**
- Modify: `core/testing/src/main/kotlin/cn/wj/android/cashbook/core/testing/repository/FakeRecordRepository.kt`

- [ ] **Step 1: 实现双向忠实桩（供 Phase 2 UseCase 测试用；接口加方法后 core:testing 必须实现否则全下游 test 模块编译失败）**

在 `FakeRecordRepository.kt` 的 `getLastThreeMonthReimbursableRecordList`（约 line 253-255）之后插入。**双向都空才算未关联**（`relatedMap` 吸收者侧 + `relatedFromMap` 被吸收侧）；RecordModel 无 typeCategory，故 Fake 层按 reimbursable + 双向未关联过滤（与现有 `getLastThreeMonthReimbursableRecordList` 同口径限制，type_category 由 DAO androidTest 覆盖）：

```kotlin
    override suspend fun getReimbursableUnrelatedRecordList(): List<RecordModel> {
        return records.filter { record ->
            record.reimbursable &&
                relatedMap[record.id].isNullOrEmpty() &&
                relatedFromMap[record.id].isNullOrEmpty()
        }
    }
```

- [ ] **Step 2: 跑编译确认 core:testing 完整 + 下游不破**

Run: `./gradlew :core:testing:compileDebugKotlin :core:domain:testDebugUnitTest`
Expected: BUILD SUCCESSFUL（接口已实现，core:domain 既有测试不受影响）

- [ ] **Step 3: Commit（与 Phase 2 UseCase 一起提交更原子；此处先单独提交 Fake 桩避免悬空接口）**

```bash
git add core/testing/src/main/kotlin/cn/wj/android/cashbook/core/testing/repository/FakeRecordRepository.kt
git commit -F - <<'EOF'
[feat|reimbursement|待报销查询][公共]FakeRecordRepository.getReimbursableUnrelatedRecordList 双向忠实桩

待报销管理界面 Phase1。reimbursable + relatedMap/relatedFromMap 双向都空才算未关联，消假阳性。

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
EOF
```

---

## Phase 2 — 领域层（UseCase + 汇总）

### Task 2.1: GetReimbursableUnrelatedRecordViewsUseCase + ReimbursementListData + 测试

**Files:**
- Create: `core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/GetReimbursableUnrelatedRecordViewsUseCase.kt`
- Test: `core/domain/src/test/kotlin/cn/wj/android/cashbook/domain/usecase/GetReimbursableUnrelatedRecordViewsUseCaseTest.kt`

- [ ] **Step 1: 写失败的测试（参照 GetRelatedRecordViewsUseCaseTest 的 setup）**

```kotlin
package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.testing.data.createRecordModel
import cn.wj.android.cashbook.core.testing.data.createRecordTypeModel
import cn.wj.android.cashbook.core.testing.repository.FakeAssetRepository
import cn.wj.android.cashbook.core.testing.repository.FakeRecordRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTagRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTypeRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class GetReimbursableUnrelatedRecordViewsUseCaseTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var recordRepository: FakeRecordRepository
    private lateinit var typeRepository: FakeTypeRepository
    private lateinit var useCase: GetReimbursableUnrelatedRecordViewsUseCase

    @Before
    fun setup() {
        recordRepository = FakeRecordRepository()
        typeRepository = FakeTypeRepository()
        typeRepository.addType(
            createRecordTypeModel(id = 1L, typeCategory = RecordTypeCategoryEnum.EXPENDITURE),
        )
        val transUseCase = RecordModelTransToViewsUseCase(
            recordRepository = recordRepository,
            typeRepository = typeRepository,
            assetRepository = FakeAssetRepository(),
            tagRepository = FakeTagRepository(),
            coroutineContext = UnconfinedTestDispatcher(),
        )
        useCase = GetReimbursableUnrelatedRecordViewsUseCase(
            recordRepository = recordRepository,
            recordModelTransToViewsUseCase = transUseCase,
            coroutineContext = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun when_empty_then_zero_count_and_total() = runTest {
        val result = useCase()

        assertThat(result.records).isEmpty()
        assertThat(result.count).isEqualTo(0)
        assertThat(result.totalAmount).isEqualTo(0L)
    }

    @Test
    fun when_multiple_reimbursable_unrelated_then_aggregates_count_and_sum_finalAmount() = runTest {
        recordRepository.addRecord(
            createRecordModel(id = 1L, typeId = 1L, reimbursable = true, finalAmount = 10000L),
        )
        recordRepository.addRecord(
            createRecordModel(id = 2L, typeId = 1L, reimbursable = true, finalAmount = 25000L),
        )
        // 不可报销 → 不计入
        recordRepository.addRecord(
            createRecordModel(id = 3L, typeId = 1L, reimbursable = false, finalAmount = 9999L),
        )

        val result = useCase()

        assertThat(result.count).isEqualTo(2)
        assertThat(result.totalAmount).isEqualTo(35000L)
        assertThat(result.records.map { it.id }).containsExactly(1L, 2L)
    }

    @Test
    fun when_record_related_either_direction_then_excluded() = runTest {
        recordRepository.addRecord(createRecordModel(id = 1L, typeId = 1L, reimbursable = true, finalAmount = 10000L))
        recordRepository.addRecord(createRecordModel(id = 2L, typeId = 1L, reimbursable = true, finalAmount = 20000L))
        recordRepository.setRelatedIds(1L, listOf(99L))      // 作为吸收者
        recordRepository.setRelatedFromIds(2L, listOf(88L))  // 作为被吸收支出

        val result = useCase()

        assertThat(result.records).isEmpty()
        assertThat(result.totalAmount).isEqualTo(0L)
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew :core:domain:testDebugUnitTest --tests "*GetReimbursableUnrelatedRecordViewsUseCaseTest*"`
Expected: 编译失败 `unresolved reference: GetReimbursableUnrelatedRecordViewsUseCase`

- [ ] **Step 3: 实现 UseCase**

`GetReimbursableUnrelatedRecordViewsUseCase.kt`（含 Apache License Header，参照同目录其他文件头）：

```kotlin
package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.transfer.asEntity
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * 获取待报销（可报销且未关联任何报销/退款款）记录视图 + 汇总用例
 *
 * 数据已在 [RecordRepository.getReimbursableUnrelatedRecordList] 的 SQL NOT EXISTS 内过滤未关联，
 * 此处仅批量转换 + 聚合，不再逐条 filter。
 */
class GetReimbursableUnrelatedRecordViewsUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
    private val recordModelTransToViewsUseCase: RecordModelTransToViewsUseCase,
    @Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext,
) {

    suspend operator fun invoke(): ReimbursementListData = withContext(coroutineContext) {
        val records: List<RecordViewsEntity> = recordRepository.getReimbursableUnrelatedRecordList()
            .let { recordModelTransToViewsUseCase(it) }
            .map { it.asEntity() }
        ReimbursementListData(
            records = records,
            count = records.size,
            totalAmount = records.sumOf { it.finalAmount },
        )
    }
}

/**
 * 待报销列表数据 + 汇总
 *
 * @param records 待报销记录视图列表（时间倒序）
 * @param count 笔数
 * @param totalAmount 合计金额（单位：分，Σ finalAmount）
 */
data class ReimbursementListData(
    val records: List<RecordViewsEntity>,
    val count: Int,
    val totalAmount: Long,
)
```

- [ ] **Step 4: 跑测试确认通过**

Run: `./gradlew :core:domain:testDebugUnitTest --tests "*GetReimbursableUnrelatedRecordViewsUseCaseTest*"`
Expected: BUILD SUCCESSFUL，3 测试 PASS

- [ ] **Step 5: Commit**

```bash
git add core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/GetReimbursableUnrelatedRecordViewsUseCase.kt \
        core/domain/src/test/kotlin/cn/wj/android/cashbook/domain/usecase/GetReimbursableUnrelatedRecordViewsUseCaseTest.kt
git commit -F - <<'EOF'
[feat|reimbursement|待报销用例][公共]GetReimbursableUnrelatedRecordViewsUseCase + 汇总

待报销管理界面 Phase2。批量转换+聚合 count/ΣfinalAmount，数据已在 SQL 过滤未关联不再逐条 filter。3 单测（空/多笔聚合/双向关联排除）。

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
EOF
```

---

## Phase 3 — UI 层（ViewModel + Screen + 截图测试）

### Task 3.1: ReimbursementUiState + ReimbursementViewModel + 测试

**Files:**
- Create: `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/ReimbursementViewModel.kt`
- Test: `feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/ReimbursementViewModelTest.kt`

- [ ] **Step 1: 写失败的测试**

```kotlin
package cn.wj.android.cashbook.feature.records.viewmodel

import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.testing.data.createRecordModel
import cn.wj.android.cashbook.core.testing.data.createRecordTypeModel
import cn.wj.android.cashbook.core.testing.repository.FakeAssetRepository
import cn.wj.android.cashbook.core.testing.repository.FakeRecordRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTagRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTypeRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.domain.usecase.GetReimbursableUnrelatedRecordViewsUseCase
import cn.wj.android.cashbook.domain.usecase.RecordModelTransToViewsUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ReimbursementViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var recordRepository: FakeRecordRepository
    private lateinit var viewModel: ReimbursementViewModel

    @Before
    fun setup() {
        recordRepository = FakeRecordRepository()
        val typeRepository = FakeTypeRepository()
        typeRepository.addType(createRecordTypeModel(id = 1L, typeCategory = RecordTypeCategoryEnum.EXPENDITURE))
        val transUseCase = RecordModelTransToViewsUseCase(
            recordRepository = recordRepository,
            typeRepository = typeRepository,
            assetRepository = FakeAssetRepository(),
            tagRepository = FakeTagRepository(),
            coroutineContext = UnconfinedTestDispatcher(),
        )
        val useCase = GetReimbursableUnrelatedRecordViewsUseCase(
            recordRepository = recordRepository,
            recordModelTransToViewsUseCase = transUseCase,
            coroutineContext = UnconfinedTestDispatcher(),
        )
        viewModel = ReimbursementViewModel(useCase)
    }

    @Test
    fun uiState_emits_success_with_count_and_total() = runTest {
        recordRepository.addRecord(createRecordModel(id = 1L, typeId = 1L, reimbursable = true, finalAmount = 10000L))
        recordRepository.addRecord(createRecordModel(id = 2L, typeId = 1L, reimbursable = true, finalAmount = 20000L))

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(ReimbursementUiState.Success::class.java)
        state as ReimbursementUiState.Success
        assertThat(state.count).isEqualTo(2)
        assertThat(state.totalAmount).isEqualTo(30000L)
    }

    @Test
    fun showAndDismiss_recordDetailsSheet_updates_viewRecord() {
        val entity = RecordViewsEntity(
            id = 1L, typeId = 1L, typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
            typeName = "餐饮", typeIconResName = "ic_test",
            assetId = null, assetName = null, assetIconResId = null,
            relatedAssetId = null, relatedAssetName = null, relatedAssetIconResId = null,
            amount = 10000L, finalAmount = 10000L, charges = 0L, concessions = 0L,
            remark = "", reimbursable = true, relatedTags = emptyList(),
            relatedImage = emptyList(), relatedRecord = emptyList(), relatedAmount = 0L,
            recordTime = 1704067200000L,
        )

        viewModel.showRecordDetailsSheet(entity)
        assertThat(viewModel.viewRecord).isEqualTo(entity)

        viewModel.dismissRecordDetailSheet()
        assertThat(viewModel.viewRecord).isNull()
    }
}
```

> `RecordViewsEntity` 构造字段以 `core/model/.../entity/RecordViewsEntity.kt:40-70` 为准（已核验全字段）；`RecordRelatedNatureEnum.NONE` 为默认值可省略。

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew :feature:records:testDebugUnitTest --tests "*ReimbursementViewModelTest*"`
Expected: 编译失败 `unresolved reference: ReimbursementViewModel`

- [ ] **Step 3: 实现 ViewModel + UiState**

`ReimbursementViewModel.kt`（含 License Header）：

```kotlin
package cn.wj.android.cashbook.feature.records.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.common.model.recordDataVersion
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.domain.usecase.GetReimbursableUnrelatedRecordViewsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 待报销管理界面 ViewModel
 *
 * 监听全局 [recordDataVersion]，记录增删改后自动重查，保证详情弹窗内操作后列表刷新。
 */
@HiltViewModel
class ReimbursementViewModel @Inject constructor(
    private val getReimbursableUnrelatedRecordViewsUseCase: GetReimbursableUnrelatedRecordViewsUseCase,
) : ViewModel() {

    /** 需显示详情的记录数据 */
    var viewRecord by mutableStateOf<RecordViewsEntity?>(null)
        private set

    val uiState: StateFlow<ReimbursementUiState> = recordDataVersion
        .mapLatest {
            val data = getReimbursableUnrelatedRecordViewsUseCase()
            ReimbursementUiState.Success(
                records = data.records,
                count = data.count,
                totalAmount = data.totalAmount,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = ReimbursementUiState.Loading,
        )

    fun showRecordDetailsSheet(item: RecordViewsEntity) {
        viewRecord = item
    }

    fun dismissRecordDetailSheet() {
        viewRecord = null
    }
}

sealed interface ReimbursementUiState {
    data object Loading : ReimbursementUiState
    data class Success(
        val records: List<RecordViewsEntity>,
        val count: Int,
        val totalAmount: Long,
    ) : ReimbursementUiState
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `./gradlew :feature:records:testDebugUnitTest --tests "*ReimbursementViewModelTest*"`
Expected: BUILD SUCCESSFUL，2 测试 PASS

- [ ] **Step 5: Commit**

```bash
git add feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/ReimbursementViewModel.kt \
        feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/ReimbursementViewModelTest.kt
git commit -F - <<'EOF'
[feat|reimbursement|待报销界面][公共]ReimbursementViewModel + UiState

待报销管理界面 Phase3。combine recordDataVersion mapLatest 重查保证刷新；viewRecord 状态管详情弹窗。2 单测（uiState 汇总/showSheet 切换）。

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
EOF
```

---

### Task 3.2: ReimbursementScreen + Route + 字符串

**Files:**
- Create: `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/screen/ReimbursementScreen.kt`
- Modify: `core/ui/src/main/res/values/strings_records.xml`（新增汇总格式串）

- [ ] **Step 1: 新增字符串**

在 `core/ui/src/main/res/values/strings_records.xml` 内新增（`pending_reimbursement`="待报销" 已存在 line 79，复用作标题/抽屉项；`no_record_data`="无记录数据" 已存在）：

```xml
    <string name="reimbursement_summary_format">共 %1$d 笔，合计 %2$s</string>
```

- [ ] **Step 2: 实现 Screen + Route（参照 SelectRelatedRecordScreen.kt 骨架 + LauncherContentScreen 详情弹窗模式）**

`ReimbursementScreen.kt`（含 License Header）：

```kotlin
package cn.wj.android.cashbook.feature.records.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.common.ext.toMoneyString
import cn.wj.android.cashbook.core.design.component.CbModalBottomSheet
import cn.wj.android.cashbook.core.design.component.CbScaffold
import cn.wj.android.cashbook.core.design.component.CbTopAppBar
import cn.wj.android.cashbook.core.design.component.Empty
import cn.wj.android.cashbook.core.design.component.Loading
import cn.wj.android.cashbook.core.design.theme.rememberHapticOnClick
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.records.viewmodel.ReimbursementUiState
import cn.wj.android.cashbook.feature.records.viewmodel.ReimbursementViewModel

@Composable
internal fun ReimbursementRoute(
    recordDetailSheetContent: @Composable (RecordViewsEntity?, () -> Unit) -> Unit,
    onRequestPopBackStack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReimbursementViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ReimbursementScreen(
        uiState = uiState,
        viewRecord = viewModel.viewRecord,
        recordDetailSheetContent = { record ->
            recordDetailSheetContent(record, viewModel::dismissRecordDetailSheet)
        },
        onRecordItemClick = viewModel::showRecordDetailsSheet,
        onRequestDismissSheet = viewModel::dismissRecordDetailSheet,
        onRequestPopBackStack = onRequestPopBackStack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReimbursementScreen(
    uiState: ReimbursementUiState,
    viewRecord: RecordViewsEntity?,
    recordDetailSheetContent: @Composable (RecordViewsEntity?) -> Unit,
    onRecordItemClick: (RecordViewsEntity) -> Unit,
    onRequestDismissSheet: () -> Unit,
    onRequestPopBackStack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CbScaffold(
        modifier = modifier,
        topBar = {
            CbTopAppBar(
                onBackClick = onRequestPopBackStack,
                title = { Text(text = stringResource(id = R.string.pending_reimbursement)) },
            )
        },
    ) { paddingValues ->
        if (viewRecord != null) {
            CbModalBottomSheet(
                onDismissRequest = onRequestDismissSheet,
                content = { recordDetailSheetContent(viewRecord) },
            )
        }
        Column(modifier = Modifier.padding(paddingValues)) {
            when (uiState) {
                ReimbursementUiState.Loading -> {
                    Loading(modifier = Modifier.fillMaxWidth())
                }
                is ReimbursementUiState.Success -> {
                    Text(
                        text = stringResource(
                            id = R.string.reimbursement_summary_format,
                            uiState.count,
                            uiState.totalAmount.toMoneyString(),
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                    if (uiState.records.isEmpty()) {
                        Empty(
                            hintText = stringResource(id = R.string.no_record_data),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(items = uiState.records, key = { it.id }) { item ->
                                RecordListItem(
                                    item = item,
                                    modifier = Modifier.clickable(
                                        onClick = rememberHapticOnClick { onRecordItemClick(item) },
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
```

> 校验点（实施时 grep 确认精确签名）：①`CbModalBottomSheet` 参数名（LauncherContentScreen.kt:236 用 `onDismissRequest`+`content`，可能还需 `sheetState`，照其调用对齐）；②金额显示函数：`Long.toMoneyString()` 在 `core/common/ext/Money.kt`（CLAUDE.md 载明），若签名不符改 `toMoneyCNY()`；③`Empty`/`Loading`/`CbScaffold`/`CbTopAppBar` 均 `core:design`（SelectRelatedRecordScreen 同 import）。

- [ ] **Step 3: 编译验证**

Run: `./gradlew :feature:records:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/screen/ReimbursementScreen.kt \
        core/ui/src/main/res/values/strings_records.xml
git commit -F - <<'EOF'
[feat|reimbursement|待报销界面][公共]ReimbursementScreen + Route + 汇总字符串

待报销管理界面 Phase3。顶栏+汇总条（共N笔合计¥X）+LazyColumn(RecordListItem)+空态+详情弹窗复用 recordDetailSheetContent，照搬 SelectRelatedRecord/LauncherContent 模式。

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
EOF
```

---

### Task 3.3: ReimbursementScreenScreenshotTests

**Files:**
- Create: `feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/screen/ReimbursementScreenScreenshotTests.kt`

- [ ] **Step 1: 写截图测试（参照同目录 SelectRelatedRecordScreenScreenshotTests.kt 的 captureMultiTheme/captureMultiDevice 模板与 Roborazzi 配置）**

照 `feature/records/src/test/.../screen/SelectRelatedRecordScreenScreenshotTests.kt` 现有模板（同 `@RunWith`/`@Config`/`composeTestRule` rule），新增 3 个用例，分别渲染：
- `ReimbursementScreen(uiState = ReimbursementUiState.Loading, viewRecord = null, recordDetailSheetContent = {}, onRecordItemClick = {}, onRequestDismissSheet = {}, onRequestPopBackStack = {})`
- `ReimbursementUiState.Success(records = emptyList(), count = 0, totalAmount = 0L)`（空态）
- `ReimbursementUiState.Success(records = listOf(<用 createRecordViewsModel(...).asEntity() 或直接构造 RecordViewsEntity>), count = 1, totalAmount = 10000L)`（有数据 + 汇总条）

> 用例骨架逐字复制 SelectRelatedRecordScreenScreenshotTests 的方法结构（仅替换被渲染的 Composable 与状态构造），确保 Roborazzi 注解/捕获 API 与项目一致。RecordViewsEntity 构造字段同 Task 3.1 Step 1。

- [ ] **Step 2: 录制基准 + 跑测试**

Run（录基准）: `./gradlew :feature:records:recordRoborazziOnlineDebug --tests "*ReimbursementScreenScreenshotTests*"`
然后校验: `./gradlew :feature:records:verifyRoborazziOnlineDebug --tests "*ReimbursementScreenScreenshotTests*"`
Expected: BUILD SUCCESSFUL（基准图生成于 `feature/records/src/test/screenshots/`）

- [ ] **Step 3: Commit**

```bash
git add feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/screen/ReimbursementScreenScreenshotTests.kt \
        feature/records/src/test/screenshots/
git commit -F - <<'EOF'
[test|reimbursement|待报销界面][公共]ReimbursementScreen 截图测试

待报销管理界面 Phase3。Loading/空态/有数据（含汇总条）3 用例 + Roborazzi 基准。

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
EOF
```

---

## Phase 4 — 导航与入口接线（CbIcons → RecordNavigation → feature:settings 抽屉 → app）

### Task 4.1: CbIcons.ReceiptLong

**Files:**
- Modify: `core/design/src/main/kotlin/cn/wj/android/cashbook/core/design/icon/CbIcons.kt`

- [ ] **Step 1: 加图标 val + import（material-icons-extended 已依赖，feasibility 核验可用）**

import 区（按字母序插入 `filled.PhotoLibrary` 后附近）：
```kotlin
import androidx.compose.material.icons.filled.ReceiptLong
```
`object CbIcons` 内（`PhotoLibrary` 附近）：
```kotlin
    val ReceiptLong = Icons.Filled.ReceiptLong
```

- [ ] **Step 2: 编译验证**

Run: `./gradlew :core:design:compileDebugKotlin`
Expected: BUILD SUCCESSFUL（若 `ReceiptLong` 在 extended 库不存在，改用 `Payments`/`Receipt` 等已存在的等价图标，grep `androidx.compose.material.icons.filled` 确认）

- [ ] **Step 3: Commit**

```bash
git add core/design/src/main/kotlin/cn/wj/android/cashbook/core/design/icon/CbIcons.kt
git commit -F - <<'EOF'
[feat|reimbursement|图标][公共]CbIcons 新增 ReceiptLong（待报销抽屉项用）

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
EOF
```

---

### Task 4.2: RecordNavigation —— Reimbursement route + nav + screen

**Files:**
- Modify: `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/navigation/RecordNavigation.kt`

- [ ] **Step 1: 加 route 对象 + 导航函数 + NavGraphBuilder（参照同文件 calendarScreen RecordNavigation.kt:183-195）**

route 区（`object RecordSearch` 后）：
```kotlin
/** 路由 - 待报销管理 */
@Serializable
object Reimbursement
```
导航函数区（`naviToSearch` 后）：
```kotlin
fun NavController.naviToReimbursement() {
    this.navigate(Reimbursement)
}
```
NavGraphBuilder 区（`searchScreen` 后）：
```kotlin
/**
 * 待报销管理界面
 *
 * @param recordDetailSheetContent 记录详情 sheet，参数：(记录数据，隐藏sheet回调) -> [Unit]
 * @param onRequestPopBackStack 导航到上一级
 */
fun NavGraphBuilder.reimbursementScreen(
    recordDetailSheetContent: @Composable (RecordViewsEntity?, () -> Unit) -> Unit,
    onRequestPopBackStack: () -> Unit,
) {
    composable<Reimbursement> {
        ReimbursementRoute(
            recordDetailSheetContent = recordDetailSheetContent,
            onRequestPopBackStack = onRequestPopBackStack,
        )
    }
}
```
import 区加：`import cn.wj.android.cashbook.feature.records.screen.ReimbursementRoute`

- [ ] **Step 2: 编译验证**

Run: `./gradlew :feature:records:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/navigation/RecordNavigation.kt
git commit -F - <<'EOF'
[feat|reimbursement|导航][公共]RecordNavigation 新增 Reimbursement route + reimbursementScreen

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
EOF
```

---

### Task 4.3: feature:settings 抽屉 4 层回调 + 抽屉项

**Files:**
- Modify: `feature/settings/src/main/kotlin/cn/wj/android/cashbook/feature/settings/navigation/SettingsNavigation.kt`（settingsLauncherScreen，line 73-93）
- Modify: `feature/settings/src/main/kotlin/cn/wj/android/cashbook/feature/settings/screen/LauncherScreen.kt`（LauncherRoute :63 / LauncherScreen :129 / LauncherSheet :210）

- [ ] **Step 1: SettingsNavigation.settingsLauncherScreen 加回调参数 + 透传**

`SettingsNavigation.kt`：函数签名（line 73-81）加 `onRequestNaviToReimbursement: () -> Unit,`（放 `onRequestNaviToMyTags` 后）；`LauncherRoute(...)` 调用（line 83-91）加 `onRequestNaviToReimbursement = onRequestNaviToReimbursement,`。

- [ ] **Step 2: LauncherRoute 加回调 + 映射 onReimbursementClick**

`LauncherScreen.kt` `LauncherRoute`（line 63-73）签名加 `onRequestNaviToReimbursement: () -> Unit,`（放 `onRequestNaviToMyTags` 后）；`LauncherScreen(...)` 调用（line 77-108）在 `onMyTagClick` 块后加：
```kotlin
        onReimbursementClick = {
            onRequestNaviToReimbursement.invoke()
            viewModel.dismissDrawerSheet()
        },
```

- [ ] **Step 3: LauncherScreen 加回调 + 透传 LauncherSheet**

`LauncherScreen`（line 129-141）签名加 `onReimbursementClick: () -> Unit,`（放 `onMyTagClick` 后）；`LauncherSheet(...)` 调用（line 181-189）加 `onReimbursementClick = onReimbursementClick,`。

- [ ] **Step 4: LauncherSheet 加回调 + 在 my_tags 后、divider 前插入抽屉项**

`LauncherSheet`（line 210-218）签名加 `onReimbursementClick: () -> Unit,`（放 `onMyTagClick` 后）；在「我的标签」`NavigationDrawerItem`（line 258-264，`onClick = onMyTagClick`）**之后**、`CbHorizontalDivider`（line 265）**之前**插入：
```kotlin
        NavigationDrawerItem(
            label = { Text(text = stringResource(id = R.string.pending_reimbursement)) },
            icon = { Icon(imageVector = CbIcons.ReceiptLong, contentDescription = null) },
            selected = false,
            onClick = onReimbursementClick,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
```

> `pending_reimbursement`="待报销" 在 `core:ui` strings_records.xml:79（已存在）；`R` 在 feature:settings 指向 `core.ui.R`（确认 import 与现有抽屉项一致）；`CbIcons.ReceiptLong` 来自 Task 4.1。

- [ ] **Step 5: 编译验证（settings main，截图测试此时会编译失败——Task 4.4 修复）**

Run: `./gradlew :feature:settings:compileDebugKotlin`
Expected: BUILD SUCCESSFUL（main 源集编译通过；`:feature:settings:testDebugUnitTest` 暂会因 LauncherScreenScreenshotTests 缺参失败，Task 4.4 修）

- [ ] **Step 6: Commit（与 Task 4.4 一起，保持 settings 模块测试始终可编译）**

暂不提交，连同 Task 4.4 一起原子提交。

---

### Task 4.4: LauncherScreenScreenshotTests 4 处加参 + 基准

**Files:**
- Modify: `feature/settings/src/test/kotlin/cn/wj/android/cashbook/feature/settings/screen/LauncherScreenScreenshotTests.kt`

- [ ] **Step 1: 4 处 LauncherScreen(...) 调用加 onReimbursementClick = {}**

在 `LauncherScreenScreenshotTests.kt` 的 4 处 `LauncherScreen(...)` 调用（约 line 50/69/89/110，每处与现有 `onMyTagClick = {}` 等并列）各加：
```kotlin
                onReimbursementClick = {},
```

- [ ] **Step 2: 录制基准（抽屉新增项 → 抽屉相关截图变化）+ 验证**

Run: `./gradlew :feature:settings:recordRoborazziOnlineDebug --tests "*LauncherScreenScreenshotTests*"` 然后 `./gradlew :feature:settings:verifyRoborazziOnlineDebug --tests "*LauncherScreenScreenshotTests*"`
Expected: BUILD SUCCESSFUL（基准重录，抽屉含「待报销」项）

- [ ] **Step 3: 跑 settings 全模块测试确认无签名漂移残留**

Run: `./gradlew :feature:settings:testDebugUnitTest`
Expected: BUILD SUCCESSFUL（`LauncherViewModelTest` 零影响 + 截图测试编译通过）

- [ ] **Step 4: Commit（含 Task 4.3）**

```bash
git add feature/settings/src/main/kotlin/cn/wj/android/cashbook/feature/settings/navigation/SettingsNavigation.kt \
        feature/settings/src/main/kotlin/cn/wj/android/cashbook/feature/settings/screen/LauncherScreen.kt \
        feature/settings/src/test/kotlin/cn/wj/android/cashbook/feature/settings/screen/LauncherScreenScreenshotTests.kt \
        feature/settings/src/test/screenshots/
git commit -F - <<'EOF'
[feat|reimbursement|抽屉入口][公共]待报销抽屉项 4 层回调透传 + 截图基准

待报销管理界面 Phase4。settingsLauncherScreen→LauncherRoute→LauncherScreen→LauncherSheet 透传 onRequestNaviToReimbursement/onReimbursementClick，插在我的标签后、分隔线前；LauncherScreenScreenshotTests 4 处加参 + 重录基准。

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
EOF
```

---

### Task 4.5: app MainApp 接线

**Files:**
- Modify: `app/src/main/kotlin/cn/wj/android/cashbook/ui/MainApp.kt`（settingsLauncherScreen 调用 line 412 + NavHost 注册区）

- [ ] **Step 1: settingsLauncherScreen 传 onRequestNaviToReimbursement + 注册 reimbursementScreen**

`MainApp.kt` `settingsLauncherScreen(...)`（line 412-437）加：
```kotlin
            onRequestNaviToReimbursement = navController::naviToReimbursement,
```
（放 `onRequestNaviToMyTags = navController::naviToMyTags,` 后）

在 `selectRelatedRecordScreen(...)` / `calendarScreen(...)` 附近的 NavHost 注册区加（照搬 calendarScreen 接线 line 511-522）：
```kotlin
        // 待报销管理
        reimbursementScreen(
            recordDetailSheetContent = { recordEntity, onRequestDismissSheet ->
                RecordDetailSheetContent(
                    recordEntity = recordEntity,
                    onRequestNaviToEditRecord = navController::naviToEditRecord,
                    onRequestNaviToAssetInfo = navController::naviToAssetInfo,
                    onRequestDismissSheet = onRequestDismissSheet,
                )
            },
            onRequestPopBackStack = navController::popBackStackSafety,
        )
```
import 区加：
```kotlin
import cn.wj.android.cashbook.feature.records.navigation.naviToReimbursement
import cn.wj.android.cashbook.feature.records.navigation.reimbursementScreen
```

- [ ] **Step 2: 编译验证（app + 全量）**

Run: `./gradlew :app:compileOnlineDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/kotlin/cn/wj/android/cashbook/ui/MainApp.kt
git commit -F - <<'EOF'
[feat|reimbursement|接线][公共]MainApp 注册 reimbursementScreen + 抽屉导航接线

待报销管理界面 Phase4。settingsLauncherScreen 传 naviToReimbursement；NavHost 注册 reimbursementScreen 注入 RecordDetailSheetContent（编辑/资产详情/关闭），照搬 calendar 接线。

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
EOF
```

---

## Phase 5 — 端到端校验 + 全量回归 + spotless

### Task 5.1: 全链路验证 + 格式 + 全量测试

**Files:** 无新增（验证 + 修复）

- [ ] **Step 1: spotless 格式修复**

Run: `./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache`
Expected: BUILD SUCCESSFUL（新文件补齐 License Header / ktlint 格式）

- [ ] **Step 2: 跑全部受影响模块 JVM 单测**

Run: `./gradlew :core:data:testDebugUnitTest :core:domain:testDebugUnitTest :feature:records:testDebugUnitTest :feature:settings:testDebugUnitTest`
Expected: BUILD SUCCESSFUL（全绿）

- [ ] **Step 3: 全量编译（app Online Debug）确认接线无断裂**

Run: `./gradlew :app:assembleOnlineDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 端到端自检（真机/模拟器有设备时；本机无设备则记录为待真机验收）**

手动路径验收清单：
1. 打开应用 → 左抽屉 → 「待报销」项可见（在「我的标签」与分隔线之间）
2. 点击进入 → 顶栏标题「待报销」+ 汇总条「共 N 笔，合计 ¥X」
3. 列表只含「可报销 + 未关联任何报销/退款款」的支出，按时间倒序
4. 点击一条 → 弹出记录详情；在详情内编辑/删除 → 返回列表自动刷新（汇总同步更新）
5. 新建一条报销款并关联某待报销记录 → 该记录从列表消失、汇总减少
6. 空账本/无待报销 → 显示空态 + 汇总「共 0 笔，合计 ¥0」

> 本机无设备时：上述 androidTest（Task 1.1）+ 端到端步骤标注为「真机补跑」，在交付消息显式列出未在设备上验证的项。

- [ ] **Step 5: 节点2 full-review（开发完成后强制，见下方 Execution Handoff 之后）**

实施全部完成后，按 CLAUDE.md 节点 2 调用 `comprehensive-review:full-review` 对本次 git diff 做最终审查（架构+安全+性能+测试+best-practices）。

- [ ] **Step 6: 完成（最终合并）**

全部绿 + full-review 通过后，按 `superpowers:finishing-a-development-branch` FF 合入 main。

---

## Self-Review（plan 对 spec 覆盖核查）

- ✅ spec §3.1 DAO `queryReimbursableUnrelated`（返 RecordTable + NOT EXISTS 双向 + type_category）→ Task 1.1
- ✅ spec §3.2 Repository 方法 + 两个 Fake 忠实桩（FakeRecordDao + FakeRecordRepository 双向）→ Task 1.2/1.3/1.4
- ✅ spec §3.3 UseCase + ReimbursementListData + ΣfinalAmount → Task 2.1
- ✅ spec §3.4 ViewModel（viewRecord + recordDataVersion mapLatest）→ Task 3.1
- ✅ spec §3.5 RecordNavigation route + 抽屉 4 层接线 + MainApp → Task 4.2/4.3/4.5
- ✅ spec §3.6 字符串（reimbursement_summary_format + 复用 pending_reimbursement）→ Task 3.2
- ✅ spec §3.7 金额口径 finalAmount（汇总 + 列表项一致）→ Task 2.1 + 3.2
- ✅ spec §4 测试策略全覆盖（DAO androidTest / Repository / UseCase / ViewModel / 两个 ScreenshotTests）→ Task 1.1/1.3/2.1/3.1/3.3/4.4
- ✅ spec §5 影响（CbIcons 新增 / LauncherScreenScreenshotTests 4 处 / LauncherViewModelTest 零影响）→ Task 4.1/4.4
- ✅ spec §7 性能前提（无分页、笔数有限）→ 设计内建，UseCase 一次性物化
- 类型一致性：`ReimbursementListData`(records/count/totalAmount) / `ReimbursementUiState.Success`(records/count/totalAmount) / `queryReimbursableUnrelated`(booksId, expenditureCategory) / `getReimbursableUnrelatedRecordList()` / `showRecordDetailsSheet`/`dismissRecordDetailSheet` 全 plan 一致 ✅
