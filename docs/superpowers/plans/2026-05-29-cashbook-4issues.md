# Cashbook 分类/搜索/资产 四项体验改进 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复「受保护类型无统计弹窗」「金额搜不到」两个 Bug，并新增「一级分类拖动排序」「资产详情按月+收支结余」两个功能。

**Architecture:** 沿用 app→feature/*→core/* 分层与 Cb* 设计系统。金额 `Long`（分）。改动按文件共享分两组串行：**组 1 = ②金额搜索 → ④资产按月**（共享 `RecordDao`/`RecordRepositoryImpl`）；**组 2 = ①受保护弹窗 → ③一级拖动排序**（共享 `MyCategoriesScreen.kt`，③ 在 ① 之后）。两组之间独立。

**Tech Stack:** Kotlin 2.2.0、AGP 8.12.0、Compose BOM 2026.05.01、Room、Hilt、Paging3、Coroutines/Flow；测试 JUnit4 + Truth + `kotlinx-coroutines-test` + `TestDispatcherRule`（`core/testing`）+ Roborazzi/Robolectric（截图）。

**口径决策（来自 spec，已用户确认）：** ② 匹配 `amount OR finalAmount`，哨兵用 `toBigDecimalOrNull()` 判定（非 `toAmountCent` 的 0L）；④ 资产余额口径（`verifyAssetBalance` 规则：源资产 income→+ / 其余→−，信用卡反向；转入目标→+amount/信用卡−amount；结余=收入−支出=余额净变化）。

---

## 分支策略

执行前用 **superpowers:using-git-worktrees** 建独立 worktree（分支名 `feature/cashbook-4issues`），从本地 `main`（含已合入的依赖修复 `1376ea01` + 本 spec 提交 `ec42900c`）派生并 `git rebase main`。**禁止直接在主工作区 main 上实现。** 每个 Task 一笔原子 commit（消息格式 `[fix|...|...][公共]...` / `[feat|...|...][公共]...`）。

## 通用执行约束（每个 Task 通用）

- 改 `RecordDao`/`TypeDao` 等 DAO 接口签名后，必须同步对应 Fake（编译依赖），否则单测模块编译失败。
- 写操作后 `TypeRepository`/`RecordRepository` 的实现必须沿用 `withContext(coroutineContext)` + 末尾 `typeDataVersion.updateVersion()` / `recordDataVersion` 驱动刷新。
- 每个 Task 末尾跑该模块单测 + `spotlessApply`；新文件带 Apache License Header（拷贝任一现有文件 1-15 行）。
- 跑 Gradle 前按 `CLAUDE.local.md` 查内存（可用<1000MB 或 使用率>90% 中止）；用 JDK 21；默认串行不加 `--parallel`。
- 常用命令：
  - 格式：`./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache`
  - core 模块单测：`./gradlew :core:data:testDebugUnitTest` / `:core:domain:testDebugUnitTest`
  - feature 模块单测：`./gradlew :feature:types:testOnlineDebugUnitTest` / `:feature:records:testOnlineDebugUnitTest`
  - 截图基准：`./gradlew recordRoborazziOnlineDebug`；校验：`./gradlew verifyRoborazziOnlineDebug`
  - 依赖基线（仅 ③ 引库后）：`./gradlew dependencyGuardBaseline`

---

## 文件结构总览

| 区域 | 文件 | 责任 | 涉及 Task |
|---|---|---|---|
| ② | `core/database/.../dao/RecordDao.kt` | `queryRecordByKeyword` 加 `amountCent` 参数 + 金额条件 | ②.1 |
| ② | `core/data/.../testdoubles/FakeRecordDao.kt` | 对齐新签名与精确语义 | ②.1 |
| ② | `core/data/.../impl/RecordRepositoryImpl.kt` | keyword→amountCent 合法判定后传 DAO | ②.2 |
| ② | `core/testing/.../repository/FakeRecordRepository.kt` | 复制 remark OR amount/finalAmount 语义 | ②.2 |
| ② | `core/data/.../RecordRepositoryImplTest.kt`、`core/domain/.../GetSearchRecordViewsUseCaseTest.kt` | 金额搜索测试 | ②.1/②.2 |
| ④ | `core/database/.../dao/RecordDao.kt` | 新增按 assetId+日期范围 分页查询 + 资产汇总视图查询 | ④.1 |
| ④ | `core/model/.../model/AssetMonthSummaryModel.kt`(新) | 资产月度收支结余结果模型 | ④.2 |
| ④ | `core/data/.../RecordRepository.kt`(+Impl+Fake) | 按 assetId+月份 分页 + 汇总 Flow | ④.2 |
| ④ | `core/domain/.../usecase/GetAssetMonthSummaryUseCase.kt`(新) | 资产余额口径计算收入/支出/结余 | ④.3 |
| ④ | `core/domain/.../usecase/GetAssetRecordViewsUseCase.kt` | invoke 加月份范围参数 | ④.4 |
| ④ | `feature/records/.../viewmodel/AssetInfoContentViewModel.kt` | 月份状态 + 按月分页 + 按日分组 + 汇总流 | ④.4 |
| ④ | `feature/records/.../screen/AssetInfoContentScreen.kt` | 月份切换器 + 统计卡 + 按日分组渲染 | ④.5 |
| ④ | `feature/assets/.../screen/AssetInfoScreen.kt`、`app/.../ui/MainApp.kt` | 跨模块槽适配 | ④.5 |
| ① | `feature/types/.../screen/MyCategoriesScreen.kt` | First/SecondTypeItem 始终弹菜单 + protected 仅渲染统计 | ①.1 |
| ① | `feature/types/.../screen/MyCategoriesScreenProtectedMenuTest.kt`(新) | protected 菜单 Compose 交互测试 | ①.1 |
| ③ | `core/database/.../dao/TypeDao.kt`(+FakeTypeDao) | `updateSortById` + `queryByLevel` 加 `ORDER BY sort` | ③.1 |
| ③ | `core/data/.../TypeRepository.kt`(+Impl+Fake) | `updateFirstTypeSort(sortedIds)` | ③.2 |
| ③ | `feature/types/.../viewmodel/MyCategoriesViewModel.kt` | `onMoveFirstType` + 一级 `.sortedBy{sort}` | ③.3 |
| ③ | `feature/types/.../screen/MyCategoriesScreen.kt` | `ExpandableTypeList` 改 keyed `items()` + 拖动 | ③.4 |
| ③ | `gradle/libs.versions.toml`、`feature/types/build.gradle.kts`、`app/dependencies/*.txt` | reorderable 依赖 + 基线 | ③.4 |

---

# 组 1：② 金额搜索 → ④ 资产按月

## Task ②.1：RecordDao.queryRecordByKeyword 扩展金额匹配 + Fake/DAO 测试

**Files:**
- Modify: `core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/dao/RecordDao.kt`（`queryRecordByKeyword` 242-257）
- Modify: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/testdoubles/FakeRecordDao.kt`（`queryRecordByKeyword` 187-204）
- Test: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordRepositoryImplTest.kt`

> 说明：真实 Room `@Query` 只能在 androidTest 验证；JVM 单测通过 `FakeRecordDao` 验证语义。DAO 签名新增 `amountCent: Long` 参数（非数字时上游传 `-1L` 哨兵）。

- [ ] **Step 1: 改 FakeRecordDao 为新签名 + 精确语义（先让旧测试编译失败，再补金额用例）**

`FakeRecordDao.queryRecordByKeyword` 改为：

```kotlin
override suspend fun queryRecordByKeyword(
    booksId: Long,
    keyword: String,
    amountCent: Long,
    pageNum: Int,
    pageSize: Int,
): List<RecordTable> {
    return records.filter {
        it.booksId == booksId && (
            it.remark.contains(keyword) ||
                (amountCent != -1L && (it.amount == amountCent || it.finalAmount == amountCent))
            )
    }
        .sortedByDescending { it.recordTime }
        .drop(pageNum)
        .take(pageSize)
}
```

- [ ] **Step 2: 在 RecordRepositoryImplTest 写失败测试（金额命中）**

把现有 `when_queryRecordByKeyword_then_matches_remark` 的调用补 `amountCent = -1L`，并新增：

```kotlin
@Test
fun when_queryRecordByKeyword_with_amount_then_matches_amount() = runTest {
    recordDao.addRecord(createRecord(id = 1L, booksId = 1L, amount = 10000L, remark = "午餐"))
    recordDao.addRecord(createRecord(id = 2L, booksId = 1L, amount = 5000L, remark = "晚餐"))

    val results = recordDao.queryRecordByKeyword(
        booksId = 1L,
        keyword = "100",
        amountCent = 10000L,
        pageNum = 0,
        pageSize = 10,
    )
    assertThat(results).hasSize(1)
    assertThat(results.first().id).isEqualTo(1L)
}

@Test
fun when_queryRecordByKeyword_amount_sentinel_minus_one_then_no_amount_match() = runTest {
    recordDao.addRecord(createRecord(id = 1L, booksId = 1L, amount = 0L, remark = "记账"))

    val results = recordDao.queryRecordByKeyword(
        booksId = 1L,
        keyword = "不存在",
        amountCent = -1L,
        pageNum = 0,
        pageSize = 10,
    )
    assertThat(results).isEmpty()
}
```

- [ ] **Step 3: 运行测试，确认失败**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*RecordRepositoryImplTest*"`
Expected: 编译失败（真实 `RecordDao.queryRecordByKeyword` 仍是旧签名，无 `amountCent`）。

- [ ] **Step 4: 改真实 RecordDao.queryRecordByKeyword 签名 + SQL**

```kotlin
@Query(
    value = """
    SELECT * FROM db_record
    WHERE books_id=:booksId
    AND (
        remark LIKE '%'||:keyword||'%'
        OR (:amountCent != -1 AND (amount = :amountCent OR final_amount = :amountCent))
    )
    ORDER BY record_time
    DESC LIMIT :pageSize
    OFFSET :pageNum
""",
)
suspend fun queryRecordByKeyword(
    booksId: Long,
    keyword: String,
    amountCent: Long,
    pageNum: Int,
    pageSize: Int,
): List<RecordTable>
```

- [ ] **Step 5: 运行测试，确认通过**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*RecordRepositoryImplTest*"`
Expected: PASS（含 remark/amount/哨兵三类用例）。

- [ ] **Step 6: 格式化 + 提交**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
git add core/database/.../RecordDao.kt core/data/.../FakeRecordDao.kt core/data/.../RecordRepositoryImplTest.kt
git commit -m "[fix|core|search][公共]金额搜索 DAO 加 amountCent 匹配 amount/finalAmount"
```

## Task ②.2：Repository 解析 keyword→amountCent + UseCase 金额测试

**Files:**
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordRepositoryImpl.kt`（`queryPagingRecordListByKeyword` 238-251）
- Modify: `core/testing/src/main/kotlin/cn/wj/android/cashbook/core/testing/repository/FakeRecordRepository.kt`（146-154）
- Test: `core/domain/src/test/kotlin/cn/wj/android/cashbook/domain/usecase/GetSearchRecordViewsUseCaseTest.kt`

- [ ] **Step 1: FakeRecordRepository 复制新语义（含哨兵判定）**

```kotlin
override suspend fun queryPagingRecordListByKeyword(
    keyword: String,
    page: Int,
    pageSize: Int,
): List<RecordModel> {
    val amountCent = if (keyword.toBigDecimalOrNull() != null) keyword.toAmountCent() else -1L
    return records.filter {
        it.remark.contains(keyword) ||
            (amountCent != -1L && (it.amount == amountCent || it.finalAmount == amountCent))
    }
        .drop(page * pageSize)
        .take(pageSize)
}
```
（顶部 import `cn.wj.android.cashbook.core.common.ext.toAmountCent`。`RecordModel` 金额字段：`amount`/`finalAmount` 均 Long。）

- [ ] **Step 2: 在 GetSearchRecordViewsUseCaseTest 写失败测试**

```kotlin
@Test
fun when_search_by_amount_then_matches_record() = runTest {
    recordRepository.addRecord(createRecordModel(id = 1L, typeId = 1L, amount = 10000L, finalAmount = 10000L, remark = "午餐"))
    recordRepository.addRecord(createRecordModel(id = 2L, typeId = 1L, amount = 5000L, finalAmount = 5000L, remark = "晚餐"))

    val result = useCase("100", 0, 10)

    assertThat(result).hasSize(1)
}

@Test
fun when_search_non_numeric_then_not_match_zero_amount_record() = runTest {
    recordRepository.addRecord(createRecordModel(id = 1L, typeId = 1L, amount = 0L, finalAmount = 0L, remark = "记账"))

    val result = useCase("早餐", 0, 10)

    assertThat(result).isEmpty()
}

@Test
fun when_search_by_final_amount_with_fee_then_matches() = runTest {
    // amount 9000 含手续费后 finalAmount 10000，用户按所见 100 元搜应命中
    recordRepository.addRecord(createRecordModel(id = 1L, typeId = 1L, amount = 9000L, finalAmount = 10000L, remark = "转账"))

    val result = useCase("100", 0, 10)

    assertThat(result).hasSize(1)
}
```

- [ ] **Step 3: 运行测试，确认失败**

Run: `./gradlew :core:domain:testDebugUnitTest --tests "*GetSearchRecordViewsUseCaseTest*"`
Expected: 新增三例 FAIL（`when_search_non_numeric_then_not_match_zero_amount_record` 当前 Fake 只 remark 匹配会先意外通过；`when_search_by_amount`/`by_final_amount` FAIL）。

- [ ] **Step 4: 改真实 RecordRepositoryImpl 传 amountCent**

```kotlin
override suspend fun queryPagingRecordListByKeyword(
    keyword: String,
    page: Int,
    pageSize: Int,
): List<RecordModel> = withContext(coroutineContext) {
    val amountCent = if (keyword.toBigDecimalOrNull() != null) keyword.toAmountCent() else -1L
    recordDao.queryRecordByKeyword(
        booksId = combineProtoDataSource.recordSettingsData.first().currentBookId,
        keyword = keyword,
        amountCent = amountCent,
        pageNum = page * pageSize,
        pageSize = pageSize,
    ).map {
        it.asModel()
    }
}
```
（顶部 import `cn.wj.android.cashbook.core.common.ext.toAmountCent`。）

- [ ] **Step 5: 运行测试，确认通过**

Run: `./gradlew :core:domain:testDebugUnitTest --tests "*GetSearchRecordViewsUseCaseTest*"` 与 `./gradlew :core:data:testDebugUnitTest`
Expected: PASS。

- [ ] **Step 6: 格式化 + 提交**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
git add core/data/.../RecordRepositoryImpl.kt core/testing/.../FakeRecordRepository.kt core/domain/.../GetSearchRecordViewsUseCaseTest.kt
git commit -m "[fix|core|search][公共]搜索关键词按 toBigDecimalOrNull 判定金额并匹配 amount/finalAmount"
```

---

## Task ④.1：RecordDao 新增按 assetId+日期范围 分页查询 + 资产汇总视图查询

**Files:**
- Modify: `core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/dao/RecordDao.kt`
- Modify: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/testdoubles/FakeRecordDao.kt`
- Test: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordRepositoryImplTest.kt`

> 资产余额口径需要每条记录的 `assetId`/`intoAssetId`/`typeCategory`/`amount`/`charge`/`concessions`。分页列表用 `queryRecordByAssetIdBetween` 返回 `RecordTable`（已含上述字段，typeCategory 由上层 transToViews 解析）。汇总用返回完整 `RecordTable` 列表即可（上层 UseCase 拿 RecordModel + 类型）；本 Task 只加分页查询；汇总走已有按月范围查询全量记录再在 domain 过滤本资产（见 ④.3）。

- [ ] **Step 1: FakeRecordDao 加新方法**

```kotlin
override suspend fun queryRecordByAssetIdBetween(
    booksId: Long,
    assetId: Long,
    startDate: Long,
    endDate: Long,
    pageNum: Int,
    pageSize: Int,
): List<RecordTable> {
    return records.filter {
        it.booksId == booksId &&
            (it.assetId == assetId || it.intoAssetId == assetId) &&
            it.recordTime >= startDate && it.recordTime < endDate
    }
        .sortedByDescending { it.recordTime }
        .drop(pageNum)
        .take(pageSize)
}
```

- [ ] **Step 2: RecordRepositoryImplTest 写失败测试**

```kotlin
@Test
fun when_queryRecordByAssetIdBetween_then_filters_asset_and_date() = runTest {
    recordDao.addRecord(createRecord(id = 1L, booksId = 1L, assetId = 5L, recordTime = 1500L))
    recordDao.addRecord(createRecord(id = 2L, booksId = 1L, assetId = 5L, recordTime = 3000L)) // 区间外
    recordDao.addRecord(createRecord(id = 3L, booksId = 1L, assetId = 9L, recordTime = 1500L)) // 别的资产

    val results = recordDao.queryRecordByAssetIdBetween(
        booksId = 1L, assetId = 5L, startDate = 1000L, endDate = 2000L, pageNum = 0, pageSize = 10,
    )
    assertThat(results).hasSize(1)
    assertThat(results.first().id).isEqualTo(1L)
}
```

- [ ] **Step 3: 运行，确认编译失败**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*RecordRepositoryImplTest*"`
Expected: 编译失败（真实 DAO 无 `queryRecordByAssetIdBetween`）。

- [ ] **Step 4: 真实 RecordDao 加 @Query（镜像 queryRecordByTypeIdBetween 模板）**

```kotlin
/** 资产 id 为 [assetId]（含转入资产）在 [startDate,endDate) 的第 [pageNum] 页 [pageSize] 条记录 */
@Query(
    """
    SELECT * FROM db_record
    WHERE books_id=:booksId
    AND (asset_id=:assetId OR into_asset_id=:assetId)
    AND record_time>=:startDate
    AND record_time<:endDate
    ORDER BY record_time DESC LIMIT :pageSize OFFSET :pageNum
""",
)
suspend fun queryRecordByAssetIdBetween(
    booksId: Long,
    assetId: Long,
    startDate: Long,
    endDate: Long,
    pageNum: Int,
    pageSize: Int,
): List<RecordTable>
```

- [ ] **Step 5: 运行，确认通过**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*RecordRepositoryImplTest*"`
Expected: PASS。

- [ ] **Step 6: 格式化 + 提交**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
git add core/database/.../RecordDao.kt core/data/.../FakeRecordDao.kt core/data/.../RecordRepositoryImplTest.kt
git commit -m "[feat|core|asset][公共]RecordDao 新增按资产+日期范围分页查询"
```

## Task ④.2：Repository 新增按 assetId+月份 分页 + 月度记录 Flow（含 Fake）

**Files:**
- Modify: `core/data/.../repository/RecordRepository.kt`（接口）
- Modify: `core/data/.../repository/impl/RecordRepositoryImpl.kt`
- Modify: `core/testing/.../repository/FakeRecordRepository.kt`
- Test: `core/data/.../RecordRepositoryImplTest.kt`

- [ ] **Step 1: 接口加两个方法（RecordRepository.kt）**

```kotlin
/** 资产 [assetId] 在 [startDate,endDate) 的分页记录（含转入） */
suspend fun queryPagingRecordListByAssetIdBetweenDate(
    assetId: Long,
    startDate: Long,
    endDate: Long,
    page: Int,
    pageSize: Int,
): List<RecordModel>

/** 资产 [assetId] 在 [startDate,endDate) 的全量记录（用于余额口径汇总），响应式 */
fun queryAssetRecordsBetweenDateFlow(
    assetId: Long,
    startDate: Long,
    endDate: Long,
): Flow<List<RecordModel>>
```

- [ ] **Step 2: FakeRecordRepository 实现两方法 + 写失败测试**

FakeRecordRepository（in-memory）：
```kotlin
override suspend fun queryPagingRecordListByAssetIdBetweenDate(
    assetId: Long, startDate: Long, endDate: Long, page: Int, pageSize: Int,
): List<RecordModel> =
    records.filter {
        (it.assetId == assetId || it.intoAssetId == assetId) &&
            it.recordTime >= startDate && it.recordTime < endDate
    }.sortedByDescending { it.recordTime }.drop(page * pageSize).take(pageSize)

override fun queryAssetRecordsBetweenDateFlow(
    assetId: Long, startDate: Long, endDate: Long,
): Flow<List<RecordModel>> = MutableStateFlow(
    records.filter {
        (it.assetId == assetId || it.intoAssetId == assetId) &&
            it.recordTime >= startDate && it.recordTime < endDate
    },
)
```
（注意 `RecordModel` 是否含 `intoAssetId`：若字段名为 `relatedAssetId`，按其实际命名改；以 `RecordModel` 实际定义为准——实现期 Read `core/model/.../RecordModel.kt` 确认转入资产字段名。）

`RecordRepositoryImplTest` 新增：
```kotlin
@Test
fun when_queryPagingRecordListByAssetIdBetweenDate_then_filters() = runTest {
    recordDao.addRecord(createRecord(id = 1L, booksId = 1L, assetId = 5L, recordTime = 1500L))
    recordDao.addRecord(createRecord(id = 2L, booksId = 1L, assetId = 5L, recordTime = 9999L))
    val repository = RecordRepositoryImpl(recordDao, /* 其余构造参数见现有 setup */)
    // ... 见现有 RecordRepositoryImpl 构造；若现有测试未实例化 Impl，则此处仅验证 FakeRecordDao 已在 ④.1 覆盖，本步可只测 Fake 行为
}
```
> 若 `RecordRepositoryImplTest` 现有结构未实例化 `RecordRepositoryImpl`（仅测 FakeRecordDao），则本 Task 的 Impl 行为由 ④.1 的 DAO 测试 + ④.4 的 ViewModel/UseCase 测试间接覆盖；此处只需保证 Fake + 接口 + Impl 编译通过。实现期按现有测试组织方式取舍，不强行新建 Impl 实例。

- [ ] **Step 3: 运行确认失败（编译）**

Run: `./gradlew :core:data:testDebugUnitTest`
Expected: 编译失败（接口新方法未实现 / Impl 未实现）。

- [ ] **Step 4: RecordRepositoryImpl 实现两方法**

```kotlin
override suspend fun queryPagingRecordListByAssetIdBetweenDate(
    assetId: Long, startDate: Long, endDate: Long, page: Int, pageSize: Int,
): List<RecordModel> = withContext(coroutineContext) {
    recordDao.queryRecordByAssetIdBetween(
        booksId = combineProtoDataSource.recordSettingsData.first().currentBookId,
        assetId = assetId,
        startDate = startDate,
        endDate = endDate,
        pageNum = page * pageSize,
        pageSize = pageSize,
    ).map { it.asModel() }
}

override fun queryAssetRecordsBetweenDateFlow(
    assetId: Long, startDate: Long, endDate: Long,
): Flow<List<RecordModel>> = combine(
    recordDataVersion,
    combineProtoDataSource.recordSettingsData,
) { _, settings ->
    // 全量（pageSize 取足够大）；资产月度记录量级有限
    recordDao.queryRecordByAssetIdBetween(
        booksId = settings.currentBookId,
        assetId = assetId,
        startDate = startDate,
        endDate = endDate,
        pageNum = 0,
        pageSize = Int.MAX_VALUE,
    ).map { it.asModel() }
}
```

- [ ] **Step 5: 运行确认通过**

Run: `./gradlew :core:data:testDebugUnitTest`
Expected: PASS。

- [ ] **Step 6: 提交**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
git add core/data/.../RecordRepository.kt core/data/.../RecordRepositoryImpl.kt core/testing/.../FakeRecordRepository.kt core/data/.../RecordRepositoryImplTest.kt
git commit -m "[feat|core|asset][公共]Repository 新增按资产+月份分页与月度记录 Flow"
```

## Task ④.3：GetAssetMonthSummaryUseCase（资产余额口径收入/支出/结余）

**Files:**
- Create: `core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/model/AssetMonthSummaryModel.kt`
- Create: `core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/GetAssetMonthSummaryUseCase.kt`
- Test: `core/domain/src/test/kotlin/cn/wj/android/cashbook/domain/usecase/GetAssetMonthSummaryUseCaseTest.kt`（新建）

> 复用 `calculateRecordAmount` 同一公式（收入=amount−charge；其余=amount+charge−concessions），按 `verifyAssetBalance` 方向规则逐记录算 delta。isCreditCard 由 ViewModel 传入（已在 `AssetInfoUiState.Success.isCreditCard` 暴露，避免 UseCase 反查 AssetRepository）。

- [ ] **Step 1: 新建结果模型**

`AssetMonthSummaryModel.kt`（带 License Header）：
```kotlin
package cn.wj.android.cashbook.core.model.model

/** 资产月度收支结余（单位：分）。结余=收入−支出=该资产当月余额净变化。 */
data class AssetMonthSummaryModel(
    val income: Long,
    val expenditure: Long,
    val balance: Long,
)
```

- [ ] **Step 2: 写失败测试 GetAssetMonthSummaryUseCaseTest**

```kotlin
class GetAssetMonthSummaryUseCaseTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var recordRepository: FakeRecordRepository
    private lateinit var typeRepository: FakeTypeRepository
    private lateinit var useCase: GetAssetMonthSummaryUseCase

    @Before
    fun setup() {
        recordRepository = FakeRecordRepository()
        typeRepository = FakeTypeRepository()
        useCase = GetAssetMonthSummaryUseCase(
            recordRepository = recordRepository,
            typeRepository = typeRepository,
            coroutineContext = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun normal_asset_income_and_expense() = runTest {
        typeRepository.addType(createRecordTypeModel(id = 1L, typeCategory = RecordTypeCategoryEnum.INCOME))
        typeRepository.addType(createRecordTypeModel(id = 2L, typeCategory = RecordTypeCategoryEnum.EXPENDITURE))
        recordRepository.addRecord(createRecordModel(id = 1L, typeId = 1L, assetId = 5L, amount = 10000L, charges = 0L))   // 收入 +10000
        recordRepository.addRecord(createRecordModel(id = 2L, typeId = 2L, assetId = 5L, amount = 3000L, charges = 0L))    // 支出 -3000

        val result = useCase(assetId = 5L, isCreditCard = false, startDate = 0L, endDate = Long.MAX_VALUE)

        assertThat(result.income).isEqualTo(10000L)
        assertThat(result.expenditure).isEqualTo(3000L)
        assertThat(result.balance).isEqualTo(7000L)
    }

    @Test
    fun transfer_in_counts_as_income() = runTest {
        typeRepository.addType(createRecordTypeModel(id = 3L, typeCategory = RecordTypeCategoryEnum.TRANSFER))
        // 转账：从资产 9 转入资产 5，本金 5000，手续费 100
        recordRepository.addRecord(
            createRecordModel(id = 1L, typeId = 3L, assetId = 9L, relatedAssetId = 5L, amount = 5000L, charges = 100L),
        )

        val result = useCase(assetId = 5L, isCreditCard = false, startDate = 0L, endDate = Long.MAX_VALUE)

        // 资产 5 为转入目标：+amount(5000)，不承担手续费
        assertThat(result.income).isEqualTo(5000L)
        assertThat(result.expenditure).isEqualTo(0L)
        assertThat(result.balance).isEqualTo(5000L)
    }

    @Test
    fun credit_card_expense_direction_reversed() = runTest {
        typeRepository.addType(createRecordTypeModel(id = 2L, typeCategory = RecordTypeCategoryEnum.EXPENDITURE))
        recordRepository.addRecord(createRecordModel(id = 1L, typeId = 2L, assetId = 5L, amount = 3000L, charges = 0L))

        val result = useCase(assetId = 5L, isCreditCard = true, startDate = 0L, endDate = Long.MAX_VALUE)

        // 信用卡支出 delta = +recordAmount → 记为收入侧（余额数值增大方向）
        assertThat(result.balance).isEqualTo(3000L)
        assertThat(result.income).isEqualTo(3000L)
        assertThat(result.expenditure).isEqualTo(0L)
    }
}
```
> `createRecordModel` 形参为 `charges`（复数）、`relatedAssetId`（转入资产）；以 `core/testing/.../TestDataFactory.kt` 实际签名为准（实现期确认 `relatedAssetId` 即 `intoAssetId` 语义）。

- [ ] **Step 3: 运行确认失败**

Run: `./gradlew :core:domain:testDebugUnitTest --tests "*GetAssetMonthSummaryUseCaseTest*"`
Expected: 编译失败（UseCase 不存在）。

- [ ] **Step 4: 实现 UseCase**

`GetAssetMonthSummaryUseCase.kt`（带 License Header）：
```kotlin
package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.AssetMonthSummaryModel
import cn.wj.android.cashbook.core.model.model.RecordModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * 计算某资产在指定月份范围的收支结余（资产余额口径）。
 * 结余 = 收入 − 支出 = 该资产当月余额净变化（对齐 TransactionDao.verifyAssetBalance 方向规则）。
 */
class GetAssetMonthSummaryUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
    private val typeRepository: TypeRepository,
    @Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext,
) {

    suspend operator fun invoke(
        assetId: Long,
        isCreditCard: Boolean,
        startDate: Long,
        endDate: Long,
    ): AssetMonthSummaryModel = withContext(coroutineContext) {
        if (assetId == -1L) return@withContext AssetMonthSummaryModel(0L, 0L, 0L)
        val records = recordRepository.queryAssetRecordsBetweenDateFlow(assetId, startDate, endDate).first()
        var income = 0L
        var expenditure = 0L
        for (record in records) {
            val category = typeRepository.getRecordTypeById(record.typeId)?.typeCategory ?: continue
            // 复用 calculateRecordAmount 公式：收入=amount-charge；其余=amount+charge-concessions
            val recordAmount = if (category == RecordTypeCategoryEnum.INCOME) {
                record.amount - record.charges
            } else {
                record.amount + record.charges - record.concessions
            }
            var delta = 0L
            // 本资产作为源资产
            if (record.assetId == assetId) {
                delta += if (isCreditCard) {
                    if (category == RecordTypeCategoryEnum.INCOME) -recordAmount else recordAmount
                } else {
                    if (category == RecordTypeCategoryEnum.INCOME) recordAmount else -recordAmount
                }
            }
            // 本资产作为转账目标
            if (record.relatedAssetId == assetId && category == RecordTypeCategoryEnum.TRANSFER) {
                delta += if (isCreditCard) -record.amount else record.amount
            }
            if (delta >= 0) income += delta else expenditure += -delta
        }
        AssetMonthSummaryModel(income = income, expenditure = expenditure, balance = income - expenditure)
    }
}
```
> 字段名以 `RecordModel` 实际定义为准（`charges` vs `charge`、`relatedAssetId` vs `intoAssetId`）——实现期 Read 确认并对齐。`getRecordTypeById` 返回的 `typeCategory` 为 `RecordTypeCategoryEnum`（domain model），无需 `ordinalOf`。

- [ ] **Step 5: 运行确认通过**

Run: `./gradlew :core:domain:testDebugUnitTest --tests "*GetAssetMonthSummaryUseCaseTest*"`
Expected: PASS（普通资产/转入/信用卡反向三例）。

- [ ] **Step 6: 提交**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
git add core/model/.../AssetMonthSummaryModel.kt core/domain/.../GetAssetMonthSummaryUseCase.kt core/domain/.../GetAssetMonthSummaryUseCaseTest.kt
git commit -m "[feat|core|asset][公共]新增资产月度余额口径收支结余 UseCase"
```

## Task ④.4：GetAssetRecordViewsUseCase 加月份范围 + AssetInfoContentViewModel 月份状态/分组/汇总

**Files:**
- Modify: `core/domain/.../usecase/GetAssetRecordViewsUseCase.kt`
- Modify: `feature/records/.../viewmodel/AssetInfoContentViewModel.kt`
- Test: `core/domain/.../GetAssetRecordViewsUseCaseTest.kt`、`feature/records/.../AssetInfoContentViewModelTest.kt`

- [ ] **Step 1: UseCase invoke 加日期范围参数（写失败测试先）**

`GetAssetRecordViewsUseCaseTest` 现有 `when_asset_id_valid_then_returns_asset_records` 改调用为带范围；新增按月过滤用例：
```kotlin
@Test
fun when_between_date_then_filters_by_time() = runTest {
    recordRepository.addRecord(createRecordModel(id = 1L, typeId = 1L, assetId = 5L, recordTime = 1500L))
    recordRepository.addRecord(createRecordModel(id = 2L, typeId = 1L, assetId = 5L, recordTime = 5000L))

    val result = useCase(assetId = 5L, startDate = 1000L, endDate = 2000L, pageNum = 0, pageSize = 10)

    assertThat(result).hasSize(1)
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :core:domain:testDebugUnitTest --tests "*GetAssetRecordViewsUseCaseTest*"`
Expected: 编译失败（invoke 无 startDate/endDate）。

- [ ] **Step 3: 改 UseCase invoke 签名**

```kotlin
suspend operator fun invoke(
    assetId: Long,
    startDate: Long,
    endDate: Long,
    pageNum: Int,
    pageSize: Int,
): List<RecordViewsEntity> = withContext(coroutineContext) {
    if (assetId == -1L) return@withContext emptyList()
    recordRepository.queryPagingRecordListByAssetIdBetweenDate(assetId, startDate, endDate, pageNum, pageSize)
        .sortedByDescending { it.recordTime }
        .map { recordModelTransToViewsUseCase(it).asEntity() }
}
```

- [ ] **Step 4: AssetInfoContentViewModel 加月份状态 + 按日分组分页 + 汇总流**

参照 `LauncherContentViewModel` 模式重写（关键片段）：
```kotlin
private val _assetIdData = MutableStateFlow(-1L)
private val _isCreditCard = MutableStateFlow(false)
private val _dateSelection = MutableStateFlow<DateSelectionEntity>(DateSelectionEntity.ByMonth(YearMonth.now()))
val dateSelection: StateFlow<DateSelectionEntity> = _dateSelection

fun updateAssetId(id: Long) { _assetIdData.tryEmit(id) }
fun updateIsCreditCard(value: Boolean) { _isCreditCard.tryEmit(value) }
fun updateMonth(yearMonth: YearMonth) { _dateSelection.tryEmit(DateSelectionEntity.ByMonth(yearMonth)) }

val recordList = combine(_assetIdData, _dateSelection, recordDataVersion) { assetId, selection, _ ->
    assetId to selection.toDateRange()
}.flatMapLatest { (assetId, range) ->
    Pager(
        config = PagingConfig(pageSize = DEFAULT_PAGE_SIZE, initialLoadSize = DEFAULT_PAGE_SIZE),
        pagingSourceFactory = { AssetRecordPagingSource(assetId, range.first, range.second, getAssetRecordViewsUseCase) },
    ).flow.map { pagingData ->
        pagingData.map { LauncherListItem.Record(it) as LauncherListItem } // 复用 LauncherListItem? 或新建 AssetListItem
    }
}.cachedIn(viewModelScope)

val summary: StateFlow<AssetMonthSummaryModel> = combine(_assetIdData, _isCreditCard, _dateSelection, recordDataVersion) { id, isCc, selection, _ ->
    val (start, end) = selection.toDateRange()
    getAssetMonthSummaryUseCase(id, isCc, start, end)
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AssetMonthSummaryModel(0, 0, 0))
```
PagingSource 改造传日期范围给新 invoke：
```kotlin
private class AssetRecordPagingSource(
    private val assetId: Long,
    private val startDate: Long,
    private val endDate: Long,
    private val getAssetRecordViewsUseCase: GetAssetRecordViewsUseCase,
) : PagingSource<Int, RecordViewsEntity>() {
    override fun getRefreshKey(state: PagingState<Int, RecordViewsEntity>): Int? = null
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, RecordViewsEntity> = runCatching {
        val page = params.key ?: 0
        val items = getAssetRecordViewsUseCase(assetId, startDate, endDate, page, params.loadSize)
        LoadResult.Page(items, if (page > 0) page - 1 else null, if (items.isNotEmpty()) page + 1 else null)
    }.getOrElse { LoadResult.Error(it) }
}
```
> 按日分组：沿用 `LauncherContentViewModel.recordPagingData` 的 `insertSeparators` + `DayHeader` 模式（复用 `LauncherListItem` 或在 feature/records 内新建等价 sealed）。实现期决定复用还是新建 list item 类型；若新建需带 License Header。

- [ ] **Step 5: AssetInfoContentViewModelTest 适配 + 新增**

更新 `buildFakeUseCase` 注入 `GetAssetMonthSummaryUseCase`（Fake 仓库）；`TestPagingSource` 镜像新 3 参 load；新增 `updateMonth` 不崩溃用例。运行：
Run: `./gradlew :feature:records:testOnlineDebugUnitTest --tests "*AssetInfoContentViewModelTest*"` 与 `:core:domain:testDebugUnitTest`
Expected: PASS。

- [ ] **Step 6: 提交**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
git add core/domain/.../GetAssetRecordViewsUseCase.kt feature/records/.../AssetInfoContentViewModel.kt core/domain/.../GetAssetRecordViewsUseCaseTest.kt feature/records/.../AssetInfoContentViewModelTest.kt
git commit -m "[feat|feature|asset][公共]资产记录按月分页+按日分组+月度汇总 ViewModel"
```

## Task ④.5：UI — 月份切换器 + 统计卡 + 跨模块槽适配

**Files:**
- Modify: `feature/records/.../screen/AssetInfoContentScreen.kt`
- Modify: `feature/assets/.../screen/AssetInfoScreen.kt`
- Modify: `app/.../ui/MainApp.kt`
- Test: `feature/records/.../AssetInfoContentScreenshotTests.kt`（截图，可选但推荐）

- [ ] **Step 1: AssetInfoContentScreen 渲染按日分组 + 汇总（topContent 内嵌月份切换器/统计卡）**

`AssetInfoContentRoute` 收集 `viewModel.dateSelection` / `viewModel.summary`，把「月份切换器 + 统计卡」组合进传入的 `topContent` 之上或之内；按日分组遍历 `recordList`（DayHeader + Record）。所有新 UI 用 `Cb*` 组件（`CbTextButton`/`CbCard`/`CbIconButton` 等），禁裸 Material3。

- [ ] **Step 2: 跨模块槽参数适配（三处同步）**

`AssetInfoContentRoute` 若需 `isCreditCard`/月份回调，扩参；同步改 `AssetInfoScreen.kt` 的 `assetRecordListContent` 槽签名与 `AssetInfoRoute`，并改 `MainApp.kt:561` 注入处传 `isCreditCard`（来自 `AssetInfoUiState.Success.isCreditCard`，经 `viewModel.updateIsCreditCard`）。**变更影响评审：三处签名必须一致，否则编译失败。**

- [ ] **Step 3: 编译 + 录制截图基准**

Run: `./gradlew :app:assembleOnlineDebug`（确认全链路编译）
Run: `./gradlew recordRoborazziOnlineDebug`（如新增/改截图）
Expected: BUILD SUCCESSFUL（用 `grep -E '^BUILD (SUCCESSFUL|FAILED)'` 判定）。

- [ ] **Step 4: 跑相关单测回归**

Run: `./gradlew :feature:records:testOnlineDebugUnitTest :feature:assets:testOnlineDebugUnitTest`
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
git add feature/records/.../AssetInfoContentScreen.kt feature/assets/.../AssetInfoScreen.kt app/.../MainApp.kt feature/records/.../*ScreenshotTests.kt
git commit -m "[feat|feature|asset][公共]资产详情按月视图 UI：月份切换器+收支结余统计卡"
```

---

# 组 2：① 受保护类型统计弹窗 → ③ 一级拖动排序

## Task ①.1：受保护类型点击始终弹菜单，仅渲染「统计数据」

**Files:**
- Modify: `feature/types/.../screen/MyCategoriesScreen.kt`（`FirstTypeItem` 615-704、`SecondTypeItem` 747-845）
- Test: `feature/types/src/test/kotlin/cn/wj/android/cashbook/feature/types/screen/MyCategoriesScreenProtectedMenuTest.kt`（新建，Robolectric Compose 交互测试）

- [ ] **Step 1: 写失败的 Compose 交互测试**

新建测试（镜像 `MyCategoriesScreenScreenshotTests` 的 Robolectric 配置 + `createAndroidComposeRule`）：
```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, qualifiers = "480dpi")
@LooperMode(LooperMode.Mode.PAUSED)
class MyCategoriesScreenProtectedMenuTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun protectedType_click_showsOnlyStatisticMenu() {
        val protectedType = ExpandableRecordTypeModel(
            data = createRecordTypeModel(id = 1L, name = "报销", protected = true),
            list = emptyList(),
        )
        composeTestRule.setContent {
            MyCategoriesScreen(
                shouldDisplayBookmark = MyCategoriesBookmarkEnum.DISMISS,
                onRequestDismissBookmark = {},
                dialogState = DialogState.Dismiss,
                onRequestDismissDialog = {},
                uiState = MyCategoriesUiState.Success(
                    selectedTab = RecordTypeCategoryEnum.EXPENDITURE,
                    typeList = listOf(protectedType),
                ),
                onRequestSelectTypeCategory = {},
                onRequestEditType = {},
                onRequestChangeFirstTypeToSecond = {},
                onRequestAddFirstType = {},
                onRequestAddSecondType = {},
                changeFirstTypeToSecond = { _, _ -> },
                onRequestChangeSecondTypeToFirst = {},
                onRequestMoveSecondTypeToAnother = { _, _ -> },
                onRequestNaviToTypeStatistics = {},
                onRequestDeleteType = {},
                changeRecordTypeBeforeDelete = { _, _ -> },
                onRequestSaveRecordType = { _, _, _, _ -> },
                onRequestPopBackStack = {},
            )
        }
        val ctx = composeTestRule.activity
        composeTestRule.onNodeWithText("报销").performClick()
        composeTestRule.onNodeWithText(ctx.getString(R.string.statistic_data)).assertExists()
        composeTestRule.onNodeWithText(ctx.getString(R.string.edit)).assertDoesNotExist()
        composeTestRule.onNodeWithText(ctx.getString(R.string.delete)).assertDoesNotExist()
    }
}
```
> 回调参数列表与名称以 `MyCategoriesScreen` 实际 internal 签名为准（见采集签名表）。`R` 为 `core.ui.R`（菜单文案资源所在；实现期确认 `statistic_data`/`edit`/`delete` 的 R 归属）。

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :feature:types:testOnlineDebugUnitTest --tests "*MyCategoriesScreenProtectedMenuTest*"`
Expected: FAIL（当前 protected 点击不弹菜单，`statistic_data` 节点不存在）。

- [ ] **Step 3: 改 FirstTypeItem — 始终弹菜单，protected 仅渲染统计项**

把点击处的 `if (!first.data.protected) { expandedMenu = true }` 改为 `expandedMenu = true`；`DropdownMenu` 内非统计的 4 项用 `if (!first.data.protected) { ... }` 包裹：
```kotlin
modifier = Modifier.clickable(
    onClick = rememberHapticOnClick { expandedMenu = true },
),
// ...
DropdownMenu(expanded = expandedMenu, onDismissRequest = { expandedMenu = false }) {
    if (!first.data.protected) {
        DropdownMenuItem(text = { Text(text = stringResource(id = R.string.edit)) }, onClick = { expandedMenu = false; onRequestEditType(first.data.id) })
        DropdownMenuItem(text = { Text(text = stringResource(id = R.string.delete)) }, onClick = { expandedMenu = false; onRequestDeleteType(first.data.id) })
        DropdownMenuItem(text = { Text(text = stringResource(id = R.string.change_to_second_type)) }, onClick = { expandedMenu = false; onRequestChangeFirstTypeToSecond(first.data.id) })
        DropdownMenuItem(text = { Text(text = stringResource(id = R.string.add_second_type)) }, onClick = { expandedMenu = false; onRequestAddSecondType(first.data.id) })
    }
    DropdownMenuItem(text = { Text(text = stringResource(id = R.string.statistic_data)) }, onClick = { expandedMenu = false; onRequestNaviToTypeStatistics(first.data.id) })
}
```
（保持原有多行格式风格，勿压成一行；上面为示意。）

- [ ] **Step 4: 同样改 SecondTypeItem**

点击处 `if (!second.data.protected) { expandedMenu = true }` → `expandedMenu = true`；`DropdownMenu` 内 编辑/删除/转为一级/移动到其它一级 用 `if (!second.data.protected) { ... }` 包裹，保留「统计数据」始终渲染。

- [ ] **Step 5: 运行确认通过**

Run: `./gradlew :feature:types:testOnlineDebugUnitTest --tests "*MyCategoriesScreenProtectedMenuTest*"`
Expected: PASS。

- [ ] **Step 6: 格式化 + 提交**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
git add feature/types/.../MyCategoriesScreen.kt feature/types/.../MyCategoriesScreenProtectedMenuTest.kt
git commit -m "[fix|feature|types][公共]受保护类型点击弹菜单仅保留统计数据项"
```

## Task ③.1：TypeDao 加 updateSortById + queryByLevel 按 sort 排序

**Files:**
- Modify: `core/database/.../dao/TypeDao.kt`
- Modify: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/testdoubles/FakeTypeDao.kt`
- Test: `core/data/.../TypeRepositoryImplTest.kt`（用 FakeTypeDao）

- [ ] **Step 1: FakeTypeDao 加 updateSortById + queryByLevel 排序 + 写失败测试**

FakeTypeDao：
```kotlin
override suspend fun updateSortById(id: Long, sort: Int) {
    val mutable = typesFlow.value.toMutableList()
    val index = mutable.indexOfFirst { it.id == id }
    if (index >= 0) {
        mutable[index] = mutable[index].copy(sort = sort)
        typesFlow.value = mutable
    }
}

override suspend fun queryByLevel(typeLevel: Int): List<TypeTable> {
    return typesFlow.value.filter { it.typeLevel == typeLevel }.sortedBy { it.sort }
}
```
`TypeRepositoryImplTest` 新增：
```kotlin
@Test
fun when_updateSortById_then_queryByLevel_ordered_by_sort() = runTest {
    val a = typeDao.insertType(createTypeTable(name = "A", typeLevel = TypeLevelEnum.FIRST.ordinal, sort = 3))
    val b = typeDao.insertType(createTypeTable(name = "B", typeLevel = TypeLevelEnum.FIRST.ordinal, sort = 1))
    typeDao.updateSortById(a, 0)
    typeDao.updateSortById(b, 1)
    val list = typeDao.queryByLevel(TypeLevelEnum.FIRST.ordinal)
    assertThat(list.map { it.name }).containsExactly("A", "B").inOrder()
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*TypeRepositoryImplTest*"`
Expected: 编译失败（真实/Fake TypeDao 无 `updateSortById`）。

- [ ] **Step 3: 真实 TypeDao 加方法 + queryByLevel 加 ORDER BY sort**

```kotlin
@Query("SELECT * FROM db_type WHERE type_level=:typeLevel ORDER BY sort ASC")
suspend fun queryByLevel(typeLevel: Int): List<TypeTable>

@Query("UPDATE db_type SET sort=:sort WHERE id=:id")
suspend fun updateSortById(id: Long, sort: Int)
```

- [ ] **Step 4: 运行确认通过**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*TypeRepositoryImplTest*"`
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
git add core/database/.../TypeDao.kt core/data/.../FakeTypeDao.kt core/data/.../TypeRepositoryImplTest.kt
git commit -m "[feat|core|types][公共]TypeDao 新增 updateSortById 并按 sort 排序查询一级类型"
```

## Task ③.2：TypeRepository.updateFirstTypeSort（接口+Impl+Fake）+ generateSortById 改 max+1

**Files:**
- Modify: `core/data/.../repository/TypeRepository.kt`、`impl/TypeRepositoryImpl.kt`
- Modify: `core/testing/.../repository/FakeTypeRepository.kt`
- Test: `core/data/.../TypeRepositoryImplTest.kt`

- [ ] **Step 1: 接口加方法**

```kotlin
/** 按给定一级分类 id 顺序写入连续 sort（0,1,2...） */
suspend fun updateFirstTypeSort(sortedIds: List<Long>)
```

- [ ] **Step 2: Fake 实现 + 写失败测试**

FakeTypeRepository：
```kotlin
override suspend fun updateFirstTypeSort(sortedIds: List<Long>) {
    sortedIds.forEachIndexed { index, id ->
        val i = types.indexOfFirst { it.id == id }
        if (i >= 0) types[i] = types[i].copy(sort = index)
    }
    updateFlows()
}
```
（`updateFlows()` 排序：把 `_firstXxxTypeListData` 的赋值改为 `.sortedBy { it.sort }`，使 Fake 也反映顺序。）

测试（用 `TypeRepositoryImpl` + `FakeTypeDao`，见采集范式 new Impl）：
```kotlin
@Test
fun when_updateFirstTypeSort_then_sort_written_continuously() = runTest {
    val repo = TypeRepositoryImpl(typeDao, transactionDao, fakeDataSource, UnconfinedTestDispatcher())
    val a = typeDao.insertType(createTypeTable(name = "A", typeLevel = TypeLevelEnum.FIRST.ordinal, sort = 5))
    val b = typeDao.insertType(createTypeTable(name = "B", typeLevel = TypeLevelEnum.FIRST.ordinal, sort = 9))
    repo.updateFirstTypeSort(listOf(b, a))
    assertThat(typeDao.queryById(b)!!.sort).isEqualTo(0)
    assertThat(typeDao.queryById(a)!!.sort).isEqualTo(1)
}
```
> `transactionDao` 用 `FakeTransactionDao`（若存在）或现有测试已有的获取方式；以 `TypeRepositoryImplTest` 现有可用 Fake 为准。

- [ ] **Step 3: 运行确认失败**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*TypeRepositoryImplTest*"`
Expected: 编译失败（接口/Impl 未实现）。

- [ ] **Step 4: Impl 实现 + generateSortById 改 max+1**

```kotlin
override suspend fun updateFirstTypeSort(sortedIds: List<Long>): Unit = withContext(coroutineContext) {
    sortedIds.forEachIndexed { index, id -> typeDao.updateSortById(id, index) }
    typeDataVersion.updateVersion()
}
```
`generateSortById` 一级分支 `typeDao.countByLevel(FIRST) + 1` → 改为基于现有最大 sort：新增 DAO `@Query("SELECT MAX(sort) FROM db_type WHERE type_level=:level") suspend fun maxSortByLevel(level: Int): Int?`（同步 FakeTypeDao），一级 `sort = (typeDao.maxSortByLevel(FIRST.ordinal) ?: 0) + 1`。
> 此子改动连带新增一个 DAO 方法 + Fake，归入本 Task；对应补一条 `generateSortById` 不撞值的测试。

- [ ] **Step 5: 运行确认通过**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*TypeRepositoryImplTest*"`
Expected: PASS。

- [ ] **Step 6: 提交**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
git add core/data/.../TypeRepository.kt core/data/.../TypeRepositoryImpl.kt core/testing/.../FakeTypeRepository.kt core/database/.../TypeDao.kt core/data/.../FakeTypeDao.kt core/data/.../TypeRepositoryImplTest.kt
git commit -m "[feat|core|types][公共]新增 updateFirstTypeSort 连续重写 sort，generateSortById 改 max+1 防撞值"
```

## Task ③.3：MyCategoriesViewModel.onMoveFirstType + 一级 sortedBy

**Files:**
- Modify: `feature/types/.../viewmodel/MyCategoriesViewModel.kt`
- Test: `feature/types/src/test/kotlin/cn/wj/android/cashbook/feature/types/viewmodel/MyCategoriesViewModelTest.kt`（新建或扩展）

- [ ] **Step 1: 写失败测试（拖动重排持久化顺序）**

```kotlin
@Test
fun when_move_first_type_then_order_persisted() = runTest {
    typeRepository.addType(createRecordTypeModel(id = 1L, name = "A", typeCategory = RecordTypeCategoryEnum.EXPENDITURE))
    typeRepository.addType(createRecordTypeModel(id = 2L, name = "B", typeCategory = RecordTypeCategoryEnum.EXPENDITURE))
    val collectJob = launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect() }

    viewModel.onMoveFirstType(fromIndex = 0, toIndex = 1)

    val state = viewModel.uiState.value as MyCategoriesUiState.Success
    assertThat(state.typeList.map { it.data.name }).containsExactly("B", "A").inOrder()
    collectJob.cancel()
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :feature:types:testOnlineDebugUnitTest --tests "*MyCategoriesViewModelTest*"`
Expected: 编译失败（无 `onMoveFirstType`）。

- [ ] **Step 3: ViewModel 加方法 + 一级排序兜底**

```kotlin
fun onMoveFirstType(fromIndex: Int, toIndex: Int) {
    viewModelScope.launch {
        val current = (uiState.value as? MyCategoriesUiState.Success)?.typeList ?: return@launch
        val ids = current.map { it.data.id }.toMutableList()
        if (fromIndex !in ids.indices || toIndex !in ids.indices) return@launch
        ids.add(toIndex, ids.removeAt(fromIndex))
        typeRepository.updateFirstTypeSort(ids)
    }
}
```
`uiState` 的 `_currentTypeList` 经 Repository（已在 ③.1 让 `queryByLevel ORDER BY sort` 生效）→ 真实链路顺序正确；Fake 经 ③.2 `updateFlows().sortedBy{sort}` 顺序正确。若需双保险，可在 `uiState` map 内对一级 `typeList` 追加 `.sortedBy { it.data.sort }`。

- [ ] **Step 4: 运行确认通过**

Run: `./gradlew :feature:types:testOnlineDebugUnitTest --tests "*MyCategoriesViewModelTest*"`
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
git add feature/types/.../MyCategoriesViewModel.kt feature/types/.../MyCategoriesViewModelTest.kt
git commit -m "[feat|feature|types][公共]MyCategoriesViewModel 新增一级分类拖动重排持久化"
```

## Task ③.4：reorderable 依赖 + ExpandableTypeList 改 keyed items 拖动 + 依赖基线

**Files:**
- Modify: `gradle/libs.versions.toml`、`feature/types/build.gradle.kts`
- Modify: `feature/types/.../screen/MyCategoriesScreen.kt`（`ExpandableTypeList` 473-514，接 `onMoveFirstType`）
- Modify: `app/dependencies/{Online,Offline,Canary}ReleaseRuntimeClasspath.txt` + `app-catalog/dependencies/releaseRuntimeClasspath.txt`（基线重生成）

- [ ] **Step 1: PoC 验证 reorderable 兼容性（前置，必做）**

到 mvnrepository / GitHub `Calvin-LL/Reorderable` releases 查 **当前最新稳定版本号**（禁止凭记忆写死）。临时在 `feature/types/build.gradle.kts` 加 `implementation("sh.calvin.reorderable:reorderable:<查到的版本>")`，写一个最小 `ReorderableItem` + `rememberReorderableLazyListState` 的 Composable，跑 `./gradlew :feature:types:compileOnlineDebugKotlin`。
- 编译通过 → 采用方案 A，继续 Step 2。
- 编译失败/不兼容 Compose BOM 2026.05.01 → **改走方案 B**（`Modifier.pointerInput` + `detectDragGesturesAfterLongPress` + `LazyListState` 自算 reorder，无新依赖，跳过 Step 2 的 libs/基线改动），并在本 plan 末尾「实施记录」标注切换原因。

- [ ] **Step 2: 正式声明依赖（方案 A）**

`gradle/libs.versions.toml`：`[versions]` 加 `calvin-reorderable = "<版本>"`；`[libraries]` 加 `calvin-reorderable = { group = "sh.calvin.reorderable", name = "reorderable", version.ref = "calvin-reorderable" }`。
`feature/types/build.gradle.kts` dependencies 块加 `implementation(libs.calvin.reorderable)`。

- [ ] **Step 3: 改 ExpandableTypeList 为 keyed items + 拖动**

把 `typeList.forEach { item { ... } }` 改为 `items(items = typeList, key = { it.data.id })`，外层 `LazyColumn` 用 `rememberLazyListState()` + `rememberReorderableLazyListState(...)`（方案 A）；每个一级项包 `ReorderableItem(state, key = first.data.id)`，长按拖动手柄触发，`onMove`/拖动结束回调里算 from/to index → 调上层透传的 `onMoveFirstType(from, to)`。二级 `SecondTypeList` 仍在同一 item 内 `if (first.expanded && hasChild)`；`Footer` 作为独立非拖动尾项（`item {}`）。给 `ExpandableTypeList` 新增形参 `onMoveFirstType: (Int, Int) -> Unit` 并由 `MyCategoriesScreen` 透传到 ViewModel。**勿动** `DialogExpandableTypeList`/`SelectFirstTypeDialog`。

- [ ] **Step 4: 编译 + 录制截图 + 重生成依赖基线**

```bash
./gradlew :feature:types:assembleOnlineDebug
./gradlew recordRoborazziOnlineDebug
./gradlew dependencyGuardBaseline   # 方案 A 必跑，重生成 4 份基线
```
确认 `git status` 显示 `app/dependencies/*.txt`（3 份）+ `app-catalog/dependencies/releaseRuntimeClasspath.txt` 有改动（新增 reorderable 传递依赖）。

- [ ] **Step 5: 跑全套相关测试 + dependencyGuard 校验**

```bash
./gradlew :feature:types:testOnlineDebugUnitTest
./gradlew dependencyGuard   # 校验基线一致（应 BUILD SUCCESSFUL）
```
Expected: 均 PASS（用 `grep -E '^BUILD (SUCCESSFUL|FAILED)'` 判定）。

- [ ] **Step 6: 提交**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
git add gradle/libs.versions.toml feature/types/build.gradle.kts feature/types/.../MyCategoriesScreen.kt app/dependencies/ app-catalog/dependencies/
git commit -m "[feat|feature|types][公共]一级分类长按拖动排序（reorderable）+ 重生成依赖基线"
```

---

## 完成后整体验收

- [ ] 全量单测：`./gradlew :core:data:testDebugUnitTest :core:domain:testDebugUnitTest :feature:types:testOnlineDebugUnitTest :feature:records:testOnlineDebugUnitTest :feature:assets:testOnlineDebugUnitTest`
- [ ] 截图校验：`./gradlew verifyRoborazziOnlineDebug`
- [ ] 依赖基线：`./gradlew dependencyGuard`
- [ ] Lint：`./gradlew :app:lintOnlineRelease`
- [ ] 全链路编译：`./gradlew :app:assembleOnlineDebug`（`grep -E '^BUILD (SUCCESSFUL|FAILED)'`）
- [ ] 交付前走 **comprehensive-review:full-review**（CLAUDE.md 节点 2 强制）对本分支 `git diff` 做最终多维评审。

## 实施记录（执行时回填偏离）

- （reorderable 方案 A/B 实际选择与原因）
- （RecordModel 转入资产/手续费字段实际命名）
- （④ 按日分组 list item 复用 LauncherListItem 还是新建）
