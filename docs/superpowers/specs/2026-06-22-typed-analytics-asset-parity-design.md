# 分类/标签统计界面对齐资产优化 设计文档

> 创建于 2026-06-22。参照资产详情页（commit `ab97811f`「资产详情按月视图 UI：月份切换器+收支结余统计卡」）对分类统计、标签统计界面执行相同优化。

## 1. 背景与现状（已核实事实）

### 1.1 资产优化指什么
commit `ab97811f` 对资产详情 `AssetInfoContentScreen` 加了三样能力：
- 月份切换器（`AssetMonthHeader`，`feature/records/.../screen/AssetInfoContentScreen.kt:210`：上一月/下一月按钮 + 当前月文字）
- 收入/支出/结余汇总卡（3 列 `SummaryColumn`，数据来自 `GetAssetMonthSummaryUseCase`）
- 按日分组（资产页本就有：`LauncherListItem.DayHeader` + `recordDaySeparator`，见 `AssetInfoContentViewModel.kt:96`）

### 1.2 目标界面
「分类统计」与「标签统计」是**同一个界面** `TypedAnalyticsScreen`（`feature/records/.../screen/TypedAnalyticsScreen.kt`）：
- `typeId` 有值 → 分类统计
- `tagId` 有值 → 标签统计

现状只有：标题（name + 日期 subTitle）+ 平铺记录列表，**无**月份切换器、**无**收支结余卡、**无**按日分组。代码里留有 TODO（`TypedAnalyticsScreen.kt:55` "待完善，按日拆分，饼状图，日期"）。

### 1.3 关键不对称性（决定方案规模）
- **分类路径**（`GetTypeRecordViewsUseCase`）已支持按日期范围查询（`queryPagingRecordListByTypeIdBetweenDate` → `RecordDao.queryRecordByTypeIdBetween`，`RecordDao.kt:212`）。
- **标签路径**（`GetTagRecordViewsUseCase`）**完全没有日期参数**，只有 `queryPagingRecordListByTagId(tagId, page, pageSize)` → `RecordDao.queryRecordByTagId`（`RecordDao.kt:255`，走 `db_tag_with_record` 关联表 IN 子查询），返回该标签全部记录。
- 入口现状：
  - 标签统计：`MainApp.kt:479` `onRequestNaviToTagStatistic = { naviToTypedAnalytics(tagId = it) }` —— **不传日期**
  - 分类统计（我的分类）：`MainApp.kt:616` `onRequestNaviToTypeStatistics = { naviToTypedAnalytics(typeId = it) }` —— **不传日期**
  - 分类统计（数据分析饼图下钻）：`MainApp.kt:549` 传 `date`（取 `AnalyticsUiState.Success.titleText` = 当前周期显示文本，可为月 `YYYY-MM`/年 `YYYY`/区间/全部）

## 2. 已确认决策（brainstorming 收集）

| 决策项 | 选择 |
|---|---|
| 标签是否做月份切换 | **是，完全对齐资产**（需为标签路径补齐日期范围查询全链路 DAO+SQL+Repository+UseCase） |
| 汇总卡内容 | **照搬资产：收入/支出/结余 3 列**（结余=收入−支出；单向分类某列显 0） |
| 金额口径 | **净自付口径**（`analyticsPieNetAmount`，与数据分析饼图一致；被报销/退款吸收的支出按净自付计入） |
| 周期模式 | **保留入口周期**（带具体周期进来显示该周期；无日期入口默认当月 + 月份切换器） |
| 按日分组 | **加**（复用 `recordDaySeparator` / `LauncherListItem`） |

## 3. 周期模式详细规则（"保留入口周期"）

ViewModel 维护 `period` 状态，由入口 `date` 字符串解析为四态：

| period 态 | 来源 | UI 表现 |
|---|---|---|
| **Month**（`YearMonth`） | 无日期入口默认 `YearMonth.now()`；或入口 `YYYY-MM` | `[<] 2024-01 [>]` 切换器，可前后翻月 |
| **Year**（`YYYY`） | 入口纯年份 | 固定周期文字 `2024`，**无箭头** |
| **Range**（`from~to`） | 入口含 `~` | 固定周期文字 `2024-01-01~2024-03-31`，**无箭头** |
| **All** | 入口空/无法解析 | 固定文字（或"全部"），**无箭头**，汇总+列表覆盖全部记录 |

判据：`monthSwitchable = (period is Month)`。月份模式下前后翻月只改 `period`，重新驱动列表分页与汇总。

入口映射：
- 我的标签 / 我的分类（不带日期）→ Month(now()) + 切换器
- 饼图按月下钻 → Month(该月) + 切换器
- 饼图按年/区间/全部下钻 → 对应固定态、无切换器

## 4. 数据层改动

### 4.1 RecordDao（`core/database`）
新增标签按日期范围分页查询（在现有 tag SQL 加 `record_time` 半开区间）：
```sql
SELECT * FROM db_record
WHERE books_id=:booksId
AND record_time>=:startDate AND record_time<:endDate
AND id IN (SELECT record_id FROM db_tag_with_record WHERE tag_id=:tagId)
ORDER BY record_time DESC LIMIT :pageSize OFFSET :pageNum
```
方法名 `queryRecordByTagIdBetween(booksId, tagId, startDate, endDate, pageNum, pageSize)`。
汇总用的"全量"查询复用分页方法 `pageSize=Int.MAX_VALUE, pageNum=0`（标签/类型月度记录量级有限，与资产 `queryAssetRecordsBetweenDateFlow` 同策略）。

> 不涉及 DB schema 变更 / migration（只加只读 `@Query`）。

### 4.2 RecordRepository（`core/data`）
- 新增 `queryPagingRecordListByTagIdBetweenDate(tagId, dateRange, page, pageSize)`（dateRange 空 → 走旧 `queryPagingRecordListByTagId` 兼容）
- 新增供汇总用的"按周期取全部记录"方法（类型 / 标签各一），返回 `List<RecordModel>`

### 4.3 日期范围解析去重
`RecordRepositoryImpl.kt:155`（`queryPagingRecordListByTypeIdBetweenDate` 内解析 `~`/`-`/年）与 `TypedAnalyticsViewModel` 的 `DateData` 各有一份 `dateStr→(start,end)` 逻辑。抽为**共享纯函数** `parseDateRangeToMillis(dateStr): Pair<Long, Long>?`（放 `core/common` 或 `core/data` 合适位置，top-level `internal fun` 便于单测），三处复用。

### 4.4 UseCase（`core/domain`）
- `GetTagRecordViewsUseCase`：加 `dateRange: String = ""` 参数（空 → 旧全量路径；非空 → 新日期范围路径），保持向后兼容。
- **新增 `GetTypedMonthSummaryUseCase`**（类型/标签通用，仿 `GetAssetMonthSummaryUseCase`）：
  - 入参 `(isType: Boolean, id: Long, startDate: Long, endDate: Long, includeChildTypes: Boolean)`
  - 查周期内该类型/标签全部记录 → 批量解析 type category（`TypeRepository.getRecordTypeCategories`，消 N+1）→ 逐条 `analyticsPieNetAmount(category, finalAmount, amount, charges, concessions)` 求和
  - INCOME → 收入；EXPENDITURE → 支出；**结余 = 收入 − 支出**
  - **TRANSFER → 不计入收支卡**（转账非分类收支语义；纯转账类型页显 0/0/0，可接受退化态，与饼图把转账单列一致）
  - 返回复用 `AssetMonthSummaryModel(income, expenditure, balance)`（字段语义通用，无需新建 model）

## 5. ViewModel / UI 改动（`feature/records`）

### 5.1 TypedAnalyticsViewModel
采用资产页的"多独立 StateFlow"模式（`AssetInfoContentViewModel` 已是 `dateSelection`/`summary`/`recordList` 三条独立流），避免 summary 塞进 uiState 致频繁重算：
- `period: StateFlow<...>` + `updateMonth(YearMonth)` 翻月方法（仿 `AssetInfoContentViewModel.updateMonth`）；`period` 携带 `periodText` 与 `monthSwitchable` 派生信息（或 Screen 侧从 period 态派生）
- `recordList` 类型由 `LazyPagingItems<RecordViewsEntity>` 改为 `LazyPagingItems<LauncherListItem>`，分页流末尾 `insertSeparators { before, after -> recordDaySeparator(before, after) }`（仿 `AssetInfoContentViewModel.kt:96`）
- `summary: StateFlow<AssetMonthSummaryModel>` 独立流，由 `combine(tagId/typeId, period, includeChildTypes, recordDataVersion)` → `GetTypedMonthSummaryUseCase` 驱动
- 标签路径分页改走 `GetTagRecordViewsUseCase(..., dateRange)`（period 转 dateStr 传入）
- `uiState`（`TypedAnalyticsUiState.Success`）维持只承载名称解析（`isType` / `titleText`），**不加 summary 字段**；去掉 `subTitleText`（周期移到 header 卡）

### 5.2 TypedAnalyticsScreen
- Screen 签名新增（仿 `AssetInfoContentScreen`）：`period`/`periodText`、`monthSwitchable`、`summary`、`onPreviousMonth`、`onNextMonth`
- 列表头（`LazyColumn` 首个 item）加 `RecordMonthSummaryHeader`（切换器/固定周期 + 3 列汇总）
- 按日分组：`when (item) { DayHeader -> ...; Record -> RecordListItem(...) }`（仿 `AssetInfoContentScreen`）
- 标题栏只留类型/标签名

## 6. 共享组件抽取（targeted 改进）

资产页私有的 `AssetMonthHeader` / `SummaryColumn` / `AssetRecordDayHeader` 抽到 `feature:records` 的共享 internal 组件文件（如 `view/RecordMonthSummaryHeader.kt`），两屏共用：
- `RecordMonthSummaryHeader`：支持两种模式——月份模式（带 `[<] text [>]` 箭头 + 回调）/ 固定周期模式（仅居中 text，无箭头）
- `SummaryColumn`、`RecordDayHeader` 直接共享
- `AssetInfoContentScreen` 改为调用共享组件（行为等价，靠 Roborazzi 0 diff 保证）

## 7. 测试

| 测试 | 类型 | 要点 |
|---|---|---|
| `GetTypedMonthSummaryUseCaseTest` | JVM (core:domain) | 净自付口径 / TRANSFER 排除 / includeChildTypes 聚合 / 收入类 / 支出类 / 标签混合收支 |
| 日期解析纯函数单测 | JVM | 月 `YYYY-MM` / 年 `YYYY` / 区间 `~` / 空 / 非法 → null |
| `TypedAnalyticsViewModelTest` 更新 | JVM (feature:records) | 周期状态机 / 月份翻页 / 汇总 / 固定周期态 |
| `GetTagRecordViewsUseCase` date 参数 | JVM | 空走全量、非空走日期范围（FakeRecordDao 忠实桩复刻 record_time 半开区间过滤） |
| `TypedAnalyticsScreenScreenshotTests` 更新 | Roborazzi (feature:records) | 签名同步 + 补"固定周期无切换器""月份模式有切换器"两态 |
| `AssetInfoContentScreen` 截图 | Roborazzi | 共享组件抽取后 0 diff 等价校验 |
| `queryRecordByTagIdBetween` | androidTest (core:database，设备门控) | 半开区间 + 关联表 JOIN 正确性 |

## 8. 影响面 / 风险评审

- **`GetTagRecordViewsUseCase` 签名变更**：调用处仅 `TagRecordPagingSource`（feature:records 内）；加默认空参 → 向后兼容，低风险
- **标签统计行为变化**：从"看全部记录"变为"默认当月"（已确认要对齐资产）
- **`TypedAnalyticsScreen`/`uiState` 签名变更**：必须同步更新该模块截图测试 + ViewModelTest（项目强制约定，否则整模块 `testDebugUnitTest` 编译失败）
- **共享组件抽取**：改动 `AssetInfoContentScreen` 这一既有工作界面，靠 Roborazzi 0 diff 守等价
- **金额口径**：选用 `analyticsPieNetAmount`（净自付），与饼图一致；CLAUDE.md 警示三口径不可混用——本设计明确只用净自付口径，TRANSFER 委托其内部 `analyticsPieAmount`（仅在被排除前不影响收支卡）
- **不涉及** DB schema / migration / 跨模块公共接口（除上述两个 feature:records 内签名）

## 9. 不做（YAGNI）

- 不在本界面加饼状图（TODO 提及，但超出"对齐资产优化"范围）
- 不为类型/标签新建 summary model（复用 `AssetMonthSummaryModel`）
- 不改数据分析主界面（AnalyticsScreen）的日期选择能力
- 不为固定周期态（年/区间）补月份切换（按"保留入口周期"决策）
