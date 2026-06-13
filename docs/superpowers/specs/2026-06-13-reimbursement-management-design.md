# 待报销管理界面 设计文档

> 创建于 2026-06-13。对应待办 #1「添加报销管理界面」。
> 流程：superpowers brainstorming → 本 spec →（节点1 team-review 四维已通过，finding 已并入）→ writing-plans → TDD 实施。
> **修订 v2（2026-06-13）**：已采纳节点1 team-review 四维 finding（C1 数据链路、H1/H2 抽屉接线与测试影响、M1-M5、L1-L2），下文为修正后版本。

## 1. 背景与目标

Cashbook 记账时可对支出记录勾选「能否报销」（`RecordTable.reimbursable`，0/1）。「报销」一笔支出在数据模型里 = 新建一条报销款收入（类型 `-2002`）并通过 `db_record_with_related` 关联该支出（吸收者=收入、被吸收=支出）。

当前没有任何独立入口让用户浏览「已勾可报销、但还没报」的记录。本功能新增**待报销管理界面**：列出当前账本所有「标记可报销且尚未关联任何报销/退款款」的支出记录，并在顶部展示笔数与合计金额，方便用户掌握待报销规模、逐条处理。

数据层基础已部分具备（`reimbursable` 字段、`db_record_with_related` 关联表、`queryRelatedRecordCountByID`），但现有查询（`getExpenditureReimburseRecordListAfterTime`，RecordDao.kt:334-346）限定「最近三个月 + LIMIT 50」，为「编辑报销款时选关联支出」场景设计，不满足「全部待报销」诉求。

## 2. 已锁定决策

| 决策点 | 结论 | 依据 |
|---|---|---|
| 展示内容 | 仅未报销：`reimbursable=1` 且未关联任何报销/退款款 | 用户选定 |
| 账本范围 | 仅当前账本（`booksId = currentBookId`） | 与全应用列表/统计口径一致 |
| 时间范围 | 全部时间（无三个月限制、无 LIMIT） | #1 核心诉求 |
| 入口 | 左侧抽屉菜单项（「我的标签」项之后、分隔线 `CbHorizontalDivider` 之前，归入「账本/资产/分类/标签」业务管理组） | 低频管理类功能（L1 finding 校正位置） |
| 点击行为 | 打开 `RecordDetailsSheet`（复用） | 与主列表行为一致 |
| 顶部汇总 | 笔数 + 合计金额 | 用户选定 |
| 搜索 | 不做（YAGNI） | 待报销笔数通常有限 |
| 查询方式 | 方案 A：纯 SQL `NOT EXISTS` 一次过滤，DAO 返回 `List<RecordTable>` | 全部时间数据量大，避免现有逐条 filter 的 N+1（2026-06-01-...-leftover-design.md:51,130） |
| 排序 | `record_time DESC`（最新在上） | 与全应用列表一致 |
| 金额口径 | 汇总与列表项统一取 `finalAmount` | 见 §3.7 |
| **「待报销」语义边界（M1）** | 被**任何**报销款(-2002)**或**退款款(-2001)关联即视为已处理、排除出待报销列表 | 与主列表标签口径一致（被关联显「已退款/已报销」标签、不显「待报销」，LauncherContentScreen.kt:709/722）；退款通常意味款已退回、无需再报 |

## 3. 详细设计

分层改动面：`core:database`(DAO) → `core:data`(Repository) → `core:domain`(UseCase) → `feature:records`(Screen+VM+route) → `feature:settings`(抽屉项) → `app`(接线) → `core:ui`(字符串) → `core:design`(图标)。**除三处既有签名扩展外（§5：抽屉回调链、`RecordRepository` 接口加方法、`LauncherScreen` 系列加回调），其余全为纯新增。**

### 3.1 数据层 —— `core:database`

`RecordDao` 新增方法 `queryReimbursableUnrelated`，**返回 `List<RecordTable>`**（C1 finding：不返回 `RecordViewsRelation`——它无 `asModel()` 且缺 `booksId/assetId/relatedAssetId` 无法转 `RecordModel`）。SQL 仿 `getExpenditureRecordListAfterTime`（RecordDao.kt:319-332）的 `type_id IN (SELECT ...)` 子查询写法：

```sql
SELECT * FROM db_record
WHERE books_id = :booksId
  AND reimbursable = $SWITCH_INT_ON
  AND type_id IN (SELECT id FROM db_type WHERE type_category = :expenditureCategory)
  AND NOT EXISTS (
      SELECT 1 FROM db_record_with_related r
      WHERE r.record_id = db_record.id OR r.related_record_id = db_record.id
  )
ORDER BY record_time DESC
```

```kotlin
suspend fun queryReimbursableUnrelated(
    booksId: Long,
    expenditureCategory: Int = RecordTypeCategoryEnum.EXPENDITURE.ordinal,
): List<RecordTable>
```

- `type_id IN (SELECT id FROM db_type WHERE type_category=EXPENDITURE)` 与 `RecordListItem` 的「待报销」判定（LauncherContentScreen.kt:722 `EXPENDITURE && reimbursable`）保持一致。
- `NOT EXISTS` 子查询同时排除「作为吸收者」(`record_id`) 与「作为被吸收支出」(`related_record_id`) 的记录，等价于现有 `queryRelatedRecordCountByID(id) <= 0`（RecordDao.kt:304-305）的 SQL 内联版；**这是方案 A 在 SQL 内一次消除 N+1 的关键**（替代现有逐条 `queryRelatedRecordCountById` 过滤）。
- 此 `NOT EXISTS` 双向排除即实现 §2 的 M1 语义边界（被报销款 OR 退款款关联都算已处理）。
- 无 `record_time` 下限、无 `LIMIT`。
- **安全守门（L3 finding）**：仅 `:booksId`/`:expenditureCategory` 参数绑定 + `$SWITCH_INT_ON` 编译期常量插值，禁止任何运行时变量拼进 SQL 字符串。

### 3.2 数据层 —— `core:data`

`RecordRepository` 接口新增：
```kotlin
suspend fun getReimbursableUnrelatedRecordList(): List<RecordModel>
```
`RecordRepositoryImpl` 实现（仿 `getLastThreeMonthReimbursableRecordList`，RecordRepositoryImpl.kt:415-424）：
```kotlin
override suspend fun getReimbursableUnrelatedRecordList(): List<RecordModel> =
    withContext(coroutineContext) {
        val booksId = combineProtoDataSource.recordSettingsData.first().currentBookId
        recordDao.queryReimbursableUnrelated(booksId).map { it.asModel() }
    }
```
`RecordTable.asModel()`（RecordRepository.kt:198-213）字段完整，链路成立（C1 修正后）。

**测试替身（M3 finding，两个 Fake 职责分清）**：
- `FakeRecordDao`（**`core:data` test 源集，不依赖 `core:testing`**）新增 `queryReimbursableUnrelated` **忠实桩**：过滤 `reimbursable==SWITCH_INT_ON && type_category==EXPENDITURE && 未关联`。**勿照抄** `FakeRecordDao.query` 的 `emptyList()` 空桩坏范例（FakeRecordDao.kt:88-90，已是 CLAUDE.md 点名的假阳性）。
- `FakeRecordRepository`（`core:testing`）新增 `getReimbursableUnrelatedRecordList` 忠实桩：`reimbursable && typeCategory==EXPENDITURE && relatedMap[id].isNullOrEmpty() && relatedFromMap[id].isNullOrEmpty()`（**双向都空**才算未关联——FakeRecordRepository 关联分 `relatedMap` 吸收者侧 + `relatedFromMap` 被吸收侧两 map，仅查一个会漏，已核验 FakeRecordRepository.kt:35-36）。

### 3.3 领域层 —— `core:domain`

新增 `GetReimbursableUnrelatedRecordViewsUseCase`：
```kotlin
class GetReimbursableUnrelatedRecordViewsUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
    private val recordModelTransToViewsUseCase: RecordModelTransToViewsUseCase,
    @Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext,
) {
    suspend operator fun invoke(): ReimbursementListData = withContext(coroutineContext) {
        val records = recordRepository.getReimbursableUnrelatedRecordList()  // 已在 SQL NOT EXISTS 过滤未关联，无需再 filter
            .let { recordModelTransToViewsUseCase(it) }   // 批量转换（IN 批量查 type/asset/related/image，非 N+1）
            .map { it.asEntity() }
        ReimbursementListData(
            records = records,
            count = records.size,
            totalAmount = records.sumOf { it.finalAmount },
        )
    }
}

data class ReimbursementListData(
    val records: List<RecordViewsEntity>,
    val count: Int,
    val totalAmount: Long,   // 单位：分
)
```

> `recordModelTransToViewsUseCase` 接受 `List` 批量转换（GetRelatedRecordViewsUseCase.kt:66 同款用法），其 `transBatch` 对 type/asset/related/image 用 IN 批量、不引入逐条 N+1；**唯标签为接口约束的 1-per-record**（M5，见 §7 前提）。**与 `GetRelatedRecordViewsUseCase.kt:53-66` 逐字同构，唯一差异是数据来源已在 SQL 内过滤未关联、不再逐条 filter。**

### 3.4 UI 层 —— `feature:records`

**`ReimbursementViewModel`**（`@HiltViewModel`）：
- `uiState: StateFlow<ReimbursementUiState>`：`combine(recordDataVersion) { ... }.mapLatest { useCase() }`（**M4 finding**：`recordDataVersion` 是 `core:common` 顶层全局 `StateFlow<Int>`，DataVersion.kt:36，**非** Repository Flow；ViewModel 直接 `import` 并 combine，照 `SearchViewModel.kt:71` / `TypedAnalyticsViewModel.kt:32,93` 模式）。删/改/新建关联记录均 bump version（`updateRecord` RecordRepositoryImpl.kt:95 / `deleteRecord` :103），保证详情弹窗内操作后列表自动刷新。`stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Loading)`。
- `viewRecord: RecordViewsEntity?`（`mutableStateOf`）+ `showRecordDetailsSheet(record)` / `dismissSheet()`（仿 `TypedAnalyticsViewModel`）。
- 状态定义：
  ```kotlin
  sealed interface ReimbursementUiState {
      data object Loading : ReimbursementUiState
      data class Success(
          val records: List<RecordViewsEntity>,
          val count: Int,
          val totalAmount: Long,
      ) : ReimbursementUiState
  }
  ```

**`ReimbursementScreen`**（无状态，参数化）：
- `CbScaffold` + `CbTopAppBar`（标题 `pending_reimbursement`="待报销"、返回键 `onRequestPopBackStack`）
- 顶部汇总条：`Success` 时显示「共 N 笔，合计 ¥X」（`totalAmount.toMoneyCNY()`）；`Loading` 显示 `Loading()`
- `LazyColumn { items(records) { RecordListItem(item, modifier=clickable{ showRecordDetailsSheet(it) }) } }`；空列表用 `Empty(hintText=...)`（仿 SelectRelatedRecordScreen.kt:177-180）
- `viewRecord != null` 时 `CbModalBottomSheet` 渲染 `recordDetailSheetContent(viewRecord)`（照搬 LauncherContentScreen.kt:234-250）
- Route 组件 `ReimbursementRoute(recordDetailSheetContent, onRequestPopBackStack, viewModel=hiltViewModel())`：`collectAsStateWithLifecycle()` 观察 uiState，传递回调

### 3.5 导航与入口接线

**`feature:records` — `RecordNavigation.kt`**：
- `@Serializable object Reimbursement`（Grep 确认全库无同名 route 冲突）
- `fun NavController.naviToReimbursement() { navigate(Reimbursement) }`
- `fun NavGraphBuilder.reimbursementScreen(recordDetailSheetContent: @Composable (RecordViewsEntity?, () -> Unit) -> Unit, onRequestPopBackStack: () -> Unit)`（仿 `calendarScreen`，RecordNavigation.kt:183-195；无 `onShowSnackbar`——待报销无 snackbar 需求）

**`feature:settings` — 抽屉回调 4 层透传（H1 finding 校正：实际链路 4 层，导航回调走函数参数、ViewModel 名为 `LauncherViewModel` 且构造不变）**：
1. `settingsLauncherScreen`（NavGraphBuilder，**`SettingsNavigation.kt:73-89`**）加 `onRequestNaviToReimbursement: () -> Unit` 参数，透传给 `LauncherRoute`
2. `LauncherRoute`（`LauncherScreen.kt:63-69`，持 6 个 `onRequestNaviTo*`）加 `onRequestNaviToReimbursement`，映射为 `onReimbursementClick = { onRequestNaviToReimbursement.invoke() }`
3. `LauncherScreen`（`LauncherScreen.kt:129-138`）加 `onReimbursementClick: () -> Unit`，透传给 `LauncherSheet`
4. `LauncherSheet`（`LauncherScreen.kt:210-216`）加 `onReimbursementClick`，在「我的标签」`NavigationDrawerItem`(:258-263) **之后、`CbHorizontalDivider`(:265) 之前**插入「待报销」`NavigationDrawerItem`（L1 finding）
> 导航回调**不进** `LauncherViewModel` 构造（其构造仅 `settingRepository, booksRepository`，LauncherViewModelTest.kt:45-47）→ `LauncherViewModel`/`LauncherViewModelTest` 零影响。

**`app` — `MainApp.kt`**：
- `settingsLauncherScreen(...)`（MainApp.kt:412）传 `onRequestNaviToReimbursement = navController::naviToReimbursement`
- `CashbookNavHost` 注册 `reimbursementScreen(recordDetailSheetContent = { recordEntity, onRequestDismissSheet -> RecordDetailSheetContent(recordEntity, navController::naviToEditRecord, navController::naviToAssetInfo, onRequestDismissSheet) }, onRequestPopBackStack = navController::popBackStackSafety)`（照搬 calendarScreen 接线 MainApp.kt:511-522）

### 3.6 字符串 —— `core:ui`

`core/ui/src/main/res/values/strings_records.xml`：
- 标题复用已有 `pending_reimbursement`="待报销"（line 79）
- 新增汇总格式串 `reimbursement_summary_format`="共 %1$d 笔，合计 %2$s"
- 空态可复用已有 `no_record_data`="无记录数据"

### 3.7 金额口径（确定结论）

- **列表项单条**：`RecordListItem` 对未关联 EXPENDITURE 走 `else` 分支显示 `item.finalAmount`（LauncherContentScreen.kt:730-741，核验确认）。
- **待报销记录必然未关联**（`NOT EXISTS` 保证）→ 对**已正确重算 finalAmount 的**未关联记录，`finalAmount = recordAmount`（CLAUDE.md：未吸收记录 finalAmount = recordAmount = amount + charges − concessions）（L2 finding：弱化为「已正确重算」前提——理论上 finalAmount 重构 main `7114045e` 前的历史数据若迁移遗漏未重算会偏离，但迁移 gate `recalculateAllFinalAmount` 应已覆盖，属低概率边界）。
- **结论**：列表项显示 = `finalAmount`，与用户指定的 `recordAmount` 口径在「已重算未关联记录」上一致。
- **汇总** `totalAmount = Σ records.finalAmount`，与列表项口径天然一致，用持久化权威值，不重复实现 recordAmount（不违反 CLAUDE.md「禁止自行用 BigDecimal/Double 重算 recordAmount」）。

## 4. 测试策略

| 模块 | 测试 | 任务名 | 备注 |
|---|---|---|---|
| `core:database` | `RecordDaoTest`：`queryReimbursableUnrelated` 命中 reimbursable+未关联 / 排除「作为吸收者」与「作为被吸收支出」双向 / 排除非 EXPENDITURE / 排除他账本 / 排序 DESC | androidTest（`:core:database:connectedCheck`） | 本机无设备，compile-verified + 真机补跑 |
| `core:data` | `RecordRepositoryImplTest` 用 `FakeRecordDao`（忠实桩）验委托 currentBookId | `:core:data:testDebugUnitTest` | JVM；`FakeRecordDao` 忠实桩，勿空桩（M3） |
| `core:domain` | `GetReimbursableUnrelatedRecordViewsUseCaseTest`：列表转换 + 汇总（空列表→count0/total0、多笔聚合 count 与 ΣfinalAmount） | `:core:domain:testDebugUnitTest` | 用 `FakeRecordRepository` 双向忠实桩（M3） |
| `feature:records` | `ReimbursementViewModelTest`：uiState 三态、recordDataVersion 触发重查、showSheet/dismissSheet | `:feature:records:testDebugUnitTest` | |
| `feature:records` | `ReimbursementScreenScreenshotTests`：Loading / 空态 / 有数据（含汇总条） | `:feature:records:testDebugUnitTest`（Roborazzi） | 仿现有截图测试 |
| `feature:settings` | **`LauncherScreenScreenshotTests` 的 4 个 @Test（:50/69/89/110）同步加 `onReimbursementClick = {}`**（H2 finding）+ 抽屉截图基准重录 | `:feature:settings:testDebugUnitTest` | 否则整模块测试源集编译失败 |

## 5. 影响评估（现有功能回归）

- **纯新增**：新 DAO 方法 / Repository 方法 / UseCase / Screen / route，不改现有签名 → 现有调用方零影响。
- `RecordRepository` 接口加方法 → `RecordRepositoryImpl`（Grep 确认单实现类）+ `FakeRecordRepository`（`core:testing`）必须同步实现，否则**所有依赖 core:testing 的 test 模块**（core:domain + 全 feature）编译失败（CLAUDE.md 已警示）。
- **抽屉回调 4 层签名扩展（H1）**：`settingsLauncherScreen`(SettingsNavigation.kt:73) / `LauncherRoute` / `LauncherScreen` / `LauncherSheet` 各加回调参数；调用方 `MainApp.kt:412`（Grep 确认唯一调用处）同步。
- **测试影响（H2 校正）**：`LauncherViewModel` 构造不变 → `LauncherViewModelTest` **零影响**（原 spec 误列已删）；真正受影响是 `LauncherScreenScreenshotTests` 的 **4 个 @Test** 需加 `onReimbursementClick = {}` + 截图基准重录。
- **`core:design` 改动（M2 finding）**：`CbIcons` 确无 `ReceiptLong`/`Payments`（Grep + CbIcons.kt 核验），需新增 1 个图标 val（如 `val ReceiptLong = Icons.Filled.ReceiptLong`，`material-icons-extended` 已 implementation）。**纯新增 val、零签名变更、零回归**，但因 `core:design` 是公共模块，按 CLAUDE.md「公共模块变更须列入影响评估」显式记此项。
- `core:data` test 源集新增 `FakeRecordDao.queryReimbursableUnrelated` 桩。
- DAO 新查询为只读，**无 migration、无 schema 变更**（不触发 Room schema version bump / androidTest schema 校验）。
- 新增 route `Reimbursement` 命名零冲突（Grep 确认）。

## 6. 实施阶段划分（产物 + 依赖，不含工时）

> 自底向上，每阶段产物可独立编译 + 测试通过，原子提交。

- **Phase 1 — 数据层**：`core:database` DAO `queryReimbursableUnrelated`（返回 `List<RecordTable>`）+ androidTest（compile-verified）；`core:data` Repository 接口+实现+`FakeRecordDao` 忠实桩+`RecordRepositoryImplTest`；`core:testing` `FakeRecordRepository` 双向忠实桩。产物：数据层可查询 + JVM 测试绿。
- **Phase 2 — 领域层**：`core:domain` `GetReimbursableUnrelatedRecordViewsUseCase` + `ReimbursementListData` + 单测（汇总/空列表/多笔）。依赖 Phase 1。
- **Phase 3 — UI 层**：`feature:records` ViewModel + UiState + Screen + Route + ViewModelTest + ScreenshotTests。依赖 Phase 2。
- **Phase 4 — 导航与入口接线**：`core:design` CbIcons 图标；`RecordNavigation` route；`feature:settings` 抽屉回调 4 层 + 抽屉项；`app` MainApp 接线；同步 `LauncherScreenScreenshotTests` 4 处 + 截图基准。依赖 Phase 3。
- **Phase 5 — 字符串 + 端到端校验**：`core:ui` 字符串；全链路自检（抽屉进入 → 列表 → 汇总 → 点击详情 → 编辑/删除回列表刷新）；模块测试全绿 + spotless。

## 7. 性能前提与非目标

**性能前提（M5 finding，显式声明）**：待报销列表 + 汇总在内存一次性物化（无分页），假设待报销笔数有限（YAGNI，与「不做搜索」取向一致）；`RecordModelTransToViewsUseCase.transBatch` 对 type/asset/related/image 批量 IN、唯标签为接口约束的 1-per-record，故笔数大时标签查询为 O(N)。若未来待报销笔数极大需优化，可将汇总（count + Σ）改走轻量 `COUNT(*) + SUM(final_amount)` SQL 与列表分离、列表分页——本次不做。

**非目标（YAGNI，本次不做）**：
- 搜索/筛选框；跨账本聚合；列表分页
- 从待报销列表「直接发起报销」动作（仍走现有「新建报销款 → 选关联支出」路径）
- 待报销提醒/角标/通知
- 已报销记录的浏览（本界面只列未报销）
