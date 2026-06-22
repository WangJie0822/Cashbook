# 分类/标签统计界面对齐资产优化 设计文档

> 创建于 2026-06-22。参照资产详情页（commit `ab97811f`「资产详情按月视图 UI：月份切换器+收支结余统计卡」）对分类统计、标签统计界面执行相同优化。
> 已过节点 1 四维评审（feasibility/security/reverse/impact，ad-hoc 降级并行），采纳项见各节「【节点1】」标注与第 10 节。

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
  - 分类统计（数据分析饼图下钻）：`MainApp.kt:549` 传 `date`（取 `AnalyticsUiState.Success.titleText` = 当前周期 `getDisplayText()`，可为月 `YYYY-MM`/年 `YYYY`/区间/全部）
- **转账类型可达本页**（【节点1】reverse 核验）：MyCategories 为全部三大类（含转账）建 tab（`MyCategoriesScreen.kt:915`），转账类型的「统计数据」菜单在 protected 判断之外恒显（`MyCategoriesScreen.kt:732`），故转账类型 → 本页是真实可达路径。

## 2. 已确认决策（brainstorming + 节点1）

| 决策项 | 选择 |
|---|---|
| 标签是否做月份切换 | **是，完全对齐资产**（需为标签路径补齐日期范围查询全链路 DAO+SQL+Repository+UseCase） |
| 汇总卡内容 | **照搬资产：收入/支出/结余 3 列**，3 列恒显。**逐条按各记录自身 category 归列**（INCOME→收入，EXPENDITURE→支出），故收入列与支出列**可同时非零**（标签混合，或导入/恢复造成的混类型树）；不存在「页面级单一大类决定唯一列」的简化 |
| 金额口径 | **净自付口径**（`analyticsPieNetAmount`，与数据分析饼图一致；被报销/退款吸收的支出按净自付计入）。**刻意区别于资产页的 recordAmount 口径**，详见第 8 节 |
| 周期模式 | **保留入口周期**（带具体周期进来显示该周期；无日期入口默认当月 + 月份切换器） |
| 周期模型 | **复用既有 `DateSelectionEntity`**（【节点1】），不另造 period 类型，详见第 3 节 |
| 按日分组 | **加**（复用 `recordDaySeparator` / `LauncherListItem`） |
| 转账类型/标签汇总 | **显提示文案「转账不计入收支统计」**（【节点1】），代替误导性的 0/0/0，详见第 4.4 节 |

## 3. 周期模式详细规则（复用 `DateSelectionEntity`）

**【节点1·采纳】** 直接复用 `core/model/.../entity/DateSelectionEntity.kt`（已是 sealed class：`ByDay`/`ByMonth`/`ByYear`/`DateRange`/`All`，自带 `toDateRange()` 返回半开区间毫秒 `Pair<Long,Long>` + `getDisplayText()`，资产页 `AssetInfoContentViewModel` 已用它）。**不另造 period 四态机 + `parseDateRangeToMillis`**——避免产生第三份「dateStr→区间」解析与现有 Calendar 版在月末/DST 边界错位。

ViewModel 维护 `dateSelection: StateFlow<DateSelectionEntity>`，由入口 `date` 字符串解析得到（见下）：

| 态 | 来源 | UI 表现 |
|---|---|---|
| `ByMonth` | 无日期入口默认 `ByMonth(YearMonth.now())`；或入口 `YYYY-MM` | `[<] 2024-01 [>]` 切换器，可前后翻月 |
| `ByYear` | 入口纯年份 `YYYY` | 固定文字 `2024`，**无箭头** |
| `DateRange` | 入口含 `~` | 固定文字 `2024-01-01~2024-03-31`，**无箭头** |
| `All` | 入口空/`全部`/无法解析 | 固定文字「全部」，**无箭头**，覆盖全部记录（`toDateRange()` = `0..Long.MAX_VALUE`） |

- 判据：`monthSwitchable = (dateSelection is ByMonth)`；翻月即 `updateMonth(yearMonth)`，重新驱动列表分页与汇总。
- 周期显示文字直接用 `dateSelection.getDisplayText()`。
- 列表区间 + 汇总区间**统一**由 `dateSelection.toDateRange()` 得到，二者必然一致（消除区间错位风险）。
- **入口字符串解析**：新增 `DateSelectionEntity.fromDisplayTextOrNull(s): DateSelectionEntity?`（解析 `getDisplayText()` 的逆向：`YYYY-MM`→ByMonth、`YYYY`→ByYear、含 `~`→DateRange、`全部`/空/非法→All 或 null）。放 `core/model`（与实体同处）。空入口（我的标签/我的分类）→ 默认 `ByMonth(now())`。

入口映射：
- 我的标签 / 我的分类（不带日期）→ `ByMonth(now())` + 切换器
- 饼图按月下钻 → `ByMonth(该月)` + 切换器
- 饼图按年/区间/全部下钻 → 对应固定态、无切换器

## 4. 数据层改动

### 4.1 RecordDao（`core/database`）
新增标签按日期范围分页查询（在现有 tag SQL 加 `record_time` 半开区间）。**用 Room `@Query` + `:param` 命名参数绑定，禁止字符串拼接**（与现有 tag/type 查询一致，无注入面）：
```sql
SELECT * FROM db_record
WHERE books_id=:booksId
AND record_time>=:startDate AND record_time<:endDate
AND id IN (SELECT record_id FROM db_tag_with_record WHERE tag_id=:tagId)
ORDER BY record_time DESC LIMIT :pageSize OFFSET :pageNum
```
方法名 `queryRecordByTagIdBetween(booksId, tagId, startDate, endDate, pageNum, pageSize)`。

> 不涉及 DB schema 变更 / migration（只加只读 `@Query`）。

### 4.2 RecordRepository（`core/data`）
- 新增 `queryPagingRecordListByTagIdBetweenDate(tagId, startDate, endDate, page, pageSize)`（接收毫秒区间，由 ViewModel 的 `toDateRange()` 产出；区间 = `0..Long.MAX_VALUE` 即全量，无需另留兼容分支）。
- **汇总用「按周期取全部记录」方法（类型 / 标签各一）**（【节点1】）：**独立非分页全量查询**（直接 `pageNum=0, pageSize=Int.MAX_VALUE`，仿 `queryAssetRecordsBetweenDateFlow`），**不复用分页签名**（规避 `page*pageSize` 的 Int 溢出陷阱）。返回 `List<RecordModel>`。booksId 统一取 `combineProtoDataSource.recordSettingsData.first().currentBookId`（务必注入，防跨账本泄漏）。
- 类型路径：`queryPagingRecordListByTypeIdBetweenDate` 现状用 dateStr 解析；本次让 ViewModel 改传毫秒区间后，类型分页也改走毫秒区间重载（或内部统一），与汇总同源。

### 4.3 日期解析去重（随 `DateSelectionEntity` 复用消解）
**【节点1·采纳】** 原计划抽 `parseDateRangeToMillis` 取消。改为：ViewModel 持有 `DateSelectionEntity`，统一用 `toDateRange()`（无字符串解析、无 `Time.kt` 解析失败静默兜底当前时间的隐患）。`RecordRepositoryImpl.kt:155-182` 的 `~`/`-`/年 字符串解析在「类型分页改走毫秒区间」后不再被 ViewModel 触发；旧 `TypedAnalyticsViewModel.DateData` 删除。如类型分页仍保留 dateStr 入口（兼容饼图旧链），保留旧解析但不新增第三份。

### 4.4 UseCase（`core/domain`）
- `GetTagRecordViewsUseCase`：加日期区间参数（毫秒，默认全量），非空区间走 `queryPagingRecordListByTagIdBetweenDate`，保持向后兼容（调用处仅 `TagRecordPagingSource`）。
- **新增 `GetTypedMonthSummaryUseCase`**（类型/标签通用）：
  - 入参 `(isType, id, startDate, endDate, includeChildTypes)`
  - 查周期内该类型/标签全部记录 → 批量解析 type category（`TypeRepository.getRecordTypeCategories`，消 N+1）→ 逐条 `analyticsPieNetAmount(category, finalAmount, amount, charges, concessions)` 求和
  - **逐条按各记录自身 category 归列**：INCOME → 收入；EXPENDITURE → 支出；**结余 = 收入 − 支出**（收入/支出列可同时非零，见第 2 节）
  - **TRANSFER → `continue` 不计入收支卡**（必须在进入 `analyticsPieNetAmount` 的 TRANSFER 分支求和前 continue；CLAUDE.md 金丝雀，选错静默算错）
  - **平账合成类型**（-1101/-1102）：按 type/tag 入口进入的记录不含平账合成类型（平账类型不在 db_type、不可下钻），故**省略** `GetAssetMonthSummaryUseCase` 的 `balanceCategoryOrNull` 兜底；getRecordTypeCategories 未命中的记录直接跳过。
  - **口径刻意区别于 `GetAssetMonthSummaryUseCase`**：后者用 `recordAmount`（资产余额口径），本 UseCase 用 `analyticsPieNetAmount`（净自付，对齐饼图）。**不要照抄 recordAmount**。
  - 返回复用 `AssetMonthSummaryModel(income, expenditure, balance)`（字段语义通用，无需新建 model）

> 转账提示文案不在 UseCase 层处理；UseCase 对转账类型返回 0/0/0，由 UI 层据「类型是否转账大类」决定显提示文案（见 5.2）。

## 5. ViewModel / UI 改动（`feature/records`）

### 5.1 TypedAnalyticsViewModel
采用资产页的"多独立 StateFlow"模式（`AssetInfoContentViewModel` 已是 `dateSelection`/`summary`/`recordList` 三条独立流），避免 summary 塞进 uiState 致频繁重算：
- `dateSelection: StateFlow<DateSelectionEntity>` + `updateMonth(YearMonth)` 翻月方法（仿 `AssetInfoContentViewModel.updateMonth`）；初值由入口字符串经 `fromDisplayTextOrNull` 解析，空 → `ByMonth(now())`
- `recordList` 类型由 `LazyPagingItems<RecordViewsEntity>` 改为 `LazyPagingItems<LauncherListItem>`，分页流末尾 `insertSeparators { before, after -> recordDaySeparator(before, after) }`（仿 `AssetInfoContentViewModel.kt:96`）；分页用 `dateSelection.toDateRange()` 毫秒区间
- `summary: StateFlow<AssetMonthSummaryModel>` 独立流，由 `combine(tagId/typeId, dateSelection, includeChildTypes, recordDataVersion)` → `GetTypedMonthSummaryUseCase`（区间用 `toDateRange()`）驱动
- `uiState`（`TypedAnalyticsUiState.Success`）维持名称解析（`isType` / `titleText`），**新增 `isTransferType: Boolean`**（类型且其 category==TRANSFER；供 UI 决定显提示文案）；**去掉 `subTitleText`**（周期移到 header 卡）

### 5.2 TypedAnalyticsScreen
- Screen 签名新增（仿 `AssetInfoContentScreen`）：`dateSelection`、`monthSwitchable`、`summary`、`isTransferType`、`onPreviousMonth`、`onNextMonth`
- 列表头（`LazyColumn` 首个 item）加 `RecordMonthSummaryHeader`：
  - 切换器/固定周期文字（按 `monthSwitchable` 决定有无箭头，文字 = `dateSelection.getDisplayText()`）
  - `isTransferType == true` → 汇总区显提示文案「转账不计入收支统计」，**不显 3 列**；否则显收入/支出/结余 3 列
- 按日分组：`when (item) { DayHeader -> RecordDayHeader(...); Record -> RecordListItem(item.entity, onClick=showRecordDetailsSheet(item.entity)) }`（仿 `AssetInfoContentScreen.kt:137-166`，key 用 `"record_${id}"`/`"header_${dateStr}"`）
- 空态文案（【节点1】）：列表为空时区分「本月无该标签/分类记录」与全部为空，沿用 `Empty` 组件 + 合适 hint
- 标题栏只留类型/标签名

## 6. 共享组件抽取（targeted 改进）

资产页私有的 `AssetMonthHeader` / `SummaryColumn` / `AssetRecordDayHeader` 抽到 `feature:records` 的共享 internal 组件文件（如 `view/RecordMonthSummaryHeader.kt`），两屏共用：
- `RecordMonthSummaryHeader`：支持月份模式（带 `[<] text [>]` 箭头 + 回调）/ 固定周期模式（仅居中 text，无箭头）/ 转账提示模式（显提示文案代替 3 列）
- `SummaryColumn`、`RecordDayHeader` 直接共享
- `AssetInfoContentScreen` 改为调用共享组件，**资产页恒走「月份模式」分支**，传参路径与现状逐一等价

**【节点1·采纳】抽取作为独立 commit 先行**（抽取 + 资产页切换 + `verifyRoborazzi` 0 diff 验证），再实现类型/标签页，使两部分可独立 revert。**Roborazzi 0 diff 只覆盖渲染、不覆盖回调连线**——资产页翻月回调由既有 `AssetInfoContentViewModelTest`（测 `updateMonth`）+ 保持 Screen→Route 回调传参等价共同守护；抽取时务必核对 `onPreviousMonth`/`onNextMonth` 仍接到 `updateMonth`。

## 7. 测试

### 7.1 新增 / 更新测试
| 测试 | 类型 | 要点 |
|---|---|---|
| `GetTypedMonthSummaryUseCaseTest` | JVM (core:domain) | 净自付口径 / **TRANSFER continue 排除断言** / includeChildTypes 聚合 / 收入类 / 支出类 / 标签混合收支两列同时非零 / **一笔被吸收支出在本 UseCase（净自付）与 GetAssetMonthSummaryUseCase（recordAmount）结果不同**（锁口径差异） |
| `DateSelectionEntity.fromDisplayTextOrNull` 单测 | JVM (core:model) | `YYYY-MM`→ByMonth / `YYYY`→ByYear / `~` 区间→DateRange / `全部`/空/非法→All 或 null |
| `TypedAnalyticsViewModelTest` 更新 | JVM (feature:records) | 周期状态机 / 月份翻页 / 汇总 / 固定周期态 / isTransferType |
| `GetTagRecordViewsUseCase` 区间参数 | JVM | 全量 vs 区间两路 |
| `TypedAnalyticsScreenScreenshotTests` 更新 | Roborazzi (feature:records) | 签名同步 + 补「月份模式有切换器」「固定周期无切换器」「转账提示」三态 |
| `AssetInfoContentScreen` 截图 | Roborazzi | 共享组件抽取后 0 diff 等价校验 |
| `queryRecordByTagIdBetween` | androidTest (core:database，设备门控) | 半开区间 + 关联表 JOIN + booksId 隔离正确性 |

### 7.2 测试替身（【节点1】必改，否则编译失败/假阳性）
- `FakeRecordRepository`：① 既有 `queryPagingRecordListByTagId`（`core/testing/.../FakeRecordRepository.kt:168`）是**宽松桩**（裸 `drop/take` 不按 tagId 过滤，假阳性）——本次顺带修为忠实按 tagId 过滤；② 新增的 tag 日期区间方法 + 两个汇总全量方法必须 `override`（interface 新增 abstract 方法，不补则 `core:testing` 编译失败）；③ 新桩忠实复刻 `record_time in start until end` 半开区间 + tag/type 过滤 + booksId。
- `FakeRecordDao`（`core/data` test）：若 DAO 直测，补 `queryRecordByTagIdBetween` 忠实桩。

### 7.3 强制必改清单（impact 维度核验，编译破坏性）
- `TypedAnalyticsViewModelTest.kt`（subTitleText 3 处断言 :201/:231/:261 必删改 + UseCase 构造）
- `TypedAnalyticsScreenScreenshotTests.kt`（successUiState 含 subTitleText + 4 处 Screen 旧签名调用必改）
- `recordList` 类型变更 → Screen item/key/点击三处重写
- 重录 `TypedAnalyticsScreen*.png`（14 张，UI 结构变）；verify `AssetInfoContentScreen*.png`（14 张应 0 diff，非 0 需人工判定）

## 8. 影响面 / 风险评审（含节点1）

- **`GetTagRecordViewsUseCase` 签名变更**：调用处仅 `TagRecordPagingSource`（feature:records 内）；加默认全量参 → 向后兼容，低风险
- **标签统计行为变化（双重收窄）**：从"看全部记录"变为"当前账本 + 默认当月"。账本隔离是既有行为（`RecordDao.kt:245` `books_id=:booksId`），当月是本次新增；二者叠加，老用户从「全部」变「当月」。已确认要对齐资产；测试断言「标签跨账本只返当前账本」。
- **`TypedAnalyticsScreen`/`uiState` 签名变更**：必须同步更新该模块截图测试 + ViewModelTest（见 7.3），否则整模块 `testDebugUnitTest` 编译失败
- **共享组件抽取**：改动 `AssetInfoContentScreen` 既有工作界面；独立 commit + Roborazzi 0 diff + 既有 ViewModelTest 守回调（见第 6 节）
- **金额口径**：`analyticsPieNetAmount`（净自付）与饼图一致，**与紧邻抄袭的资产页 recordAmount 口径不同**（刻意为之）；同一笔被吸收支出在两屏数值可能不同，测试锁定预期。CLAUDE.md 警示三口径不可混用——本设计明确净自付口径 + TRANSFER continue。
- **转账退化**：转账类型可从「我的分类→转账 tab」进入本页（已核验），显提示文案而非 0/0/0（决策）；标签若含转账记录，转账记录静默排除、3 列仍显非转账收支（与饼图把转账单列一致）。
- **固定周期态无切换器**：从数据分析按年/区间下钻进来无月份切换器（YAGNI，第 9 节），已知交互不一致，权衡后接受。
- **不涉及** DB schema / migration / 跨模块公共接口（除上述 feature:records 内签名 + `DateSelectionEntity.fromDisplayTextOrNull` 新增 public 函数）

## 9. 不做（YAGNI）

- 不在本界面加饼状图（TODO 提及，但超出"对齐资产优化"范围）
- 不为类型/标签新建 summary model（复用 `AssetMonthSummaryModel`）
- 不改数据分析主界面（AnalyticsScreen）的日期选择能力
- 不为固定周期态（年/区间）补月份切换（按"保留入口周期"决策）
- 不隐藏转账类型的统计入口（按节点1 决策用提示文案，不改 MyCategories）

## 10. 节点 1 评审采纳汇总

四维 ad-hoc 并行评审（【team-review 不可用，降级 ad-hoc】），controller 对关键强断言已 hands-on 核验。结论 feasibility/security 0C/0H，reverse/impact 抛出 finding 处置：
- **复用 `DateSelectionEntity`** 替代自造 period + parseDateRangeToMillis（reverse High，核验属实，已采纳，消除第三份日期解析与 Time.kt 静默兜底）
- **汇总逐条归列、3 列恒显、两列可同时非零**（reverse High→校准 Medium，删「单列显 0」措辞）
- **转账显提示文案**（reverse High，用户决策）
- **汇总用独立非分页全量查询**（reverse Medium，规避 page*pageSize 溢出）
- **修 FakeRecordRepository 宽松 tag 桩 + 新桩忠实**（impact Medium）
- **强制必改测试清单 + 重录基线**（impact High×3，升格为 7.3）
- **共享组件独立 commit + 回调守护**（reverse Critical→校准 High）
- **口径差异显式声明 + 测试锁定**（reverse/impact Medium）
- **booksId 双重收窄声明 + 空态文案**（reverse Medium）
- Room 绑定声明 / 平账兜底省略声明 / 固定周期交互割裂接受（feasibility/security Low）
