# 设计：预算管理（按账本 + 一级支出分类，可配置月周期）

> 创建于 2026-06-23 · superpowers brainstorming 产出
> 状态：待用户评审（v2：已纳入节点1 四维评审[feasibility/security/reverse/impact，team-review 设施缺失降级 ad-hoc]修订；reverse Critical 经 PoC 实测排除）
> 本 spec 是「预算管理」需求（用户 2026-06-03 登记 #3，Cashbook 唯一未实施功能需求）的实施设计，**消费**前置子项目 [可配置月周期](./2026-06-23-configurable-month-cycle-design.md) 的能力。
> 本 spec 范围（用户决策）= **预算管理 + F3 兜底 migration 搭车 + Low backlog ③（固定周期态月标签语义）**。journey 黑盒剩余路径不在本 spec（独立测试活动）。

## 1. 背景与目标

为 Cashbook 增加**预算管理**：用户可为「当前账本」设置**总体月度预算**与**各一级支出分类月度预算**，在独立预算屏查看「本周期已花 / 限额 / 进度」，超支时给视觉提示。

- **「本月」沿用可配置月周期**（`DateSelectionEntity.currentMonthPeriod(today, monthStartDay)`，前置子项目产物），月起始日 D≠1 时预算周期 = `[本月 D 日, 次月 D 日)`。
- **口径为净自付**（`analyticsPieNetAmount` / `finalAmount`，全应用约定），与数据分析饼图逐分一致。
- **仅视觉提示**：进度条变色 + 显示已花/限额/超支额，无通知、无后台任务、零额外权限。

## 2. 已收齐决策（继承附录 + 本次 brainstorming 新增）

| 维度 | 决策 | 来源 |
|---|---|---|
| 作用域 | 每个账本独立预算（预算表带 `books_id`） | 附录 |
| 周期 | 每月固定循环限额（表无月份维度），「本月」走可配置周期 | 附录 |
| 维度 | 总体预算 + 各「一级支出分类」预算（仅 EXPENDITURE；收入/转账不设） | 附录 |
| 口径 | 净自付 `analyticsPieNetAmount`，复用 `TransRecordViewsToAnalyticsPieUseCase` | 附录 + 本次方案 A |
| 入口/展示 | 左抽屉新增「预算管理」入口（`LauncherDrawerActions` 第 8 回调）→ 独立预算屏 | 附录 |
| 超支表现 | 仅视觉（进度条变色 + 已花/限额/超支额） | 附录 |
| 模块 | 新建 `feature:budget` 模块（聚合 UseCase 在 core:domain） | 附录 |
| 存储 | 新建 Room 表 `db_budget`；Migration12To13（DB v12→13） | 附录 |
| **分类列表展示** | **仅展示已设预算的分类**，顶部「+ 添加分类预算」入口弹未设分类选择器 | 本次 |
| **超支视觉档位** | **两级**：<80% 正常 / 80–100% 接近(橙) / >100% 超支(红+超支额) | 本次 |
| **「已花」计算** | **方案 A**：查本周期 recordViews → `TransRecordViewsToAnalyticsPieUseCase` 聚合 | 本次 |
| **总体预算角色** | 可选、与分类预算各自独立设限额（无「分类之和 ≤ 总体」约束）；总体已花 = 本周期 Σ EXPENDITURE 净自付（直接求和，数值上等于无孤儿时各分类之和；见 §4.2） | 本次 |

### 2.1 方案选型记录：「已花」金额计算 = 方案 A（复用 Pie UseCase）

- **方案 A（采纳）**：预算屏 VM 查本周期 EXPENDITURE recordViews → 喂 `TransRecordViewsToAnalyticsPieUseCase`（`core/domain/.../TransRecordViewsToAnalyticsPieUseCase.kt:48-80`，输出 `List<AnalyticsRecordPieEntity>`，按一级分类滚动汇总 `analyticsPieNetAmount` 净自付）→ 各分类已花；总体已花另**直接对本周期 EXPENDITURE 求 Σ `analyticsPieNetAmount`**（不靠 Σ pieList，规避孤儿少算，见 §4.2 修订）。
  - 采纳依据：① 复用既有净自付聚合，与数据分析饼图口径同源、逐分一致；② 不在 SQL 重写 `finalAmount` 净自付口径，守「禁止自行用 BigDecimal/Double 重实现金额计算」约定，规避口径漂移；③ 月度记录量有限，全量查内存再聚合开销可接受（报销管理 `GetReimbursableUnrelatedRecordViewsUseCase` 已用同类「查 recordViews 再聚合」模式无性能问题）。
- **方案 B（否决）**：新建 SQL 聚合 UseCase——净自付口径 SQL 重写复杂、易与 Pie 口径漂移、违背禁止重实现约定。
- **方案 C（否决）**：总体 SQL + 分类 Pie 混合——两套口径来源，一致性风险。

## 3. 数据模型与存储

### 3.1 `db_budget` 表（core:database）

```kotlin
@Entity(
    tableName = "db_budget",
    indices = [Index(value = ["books_id", "type_id"], unique = true)],
)
data class BudgetTable(
    @PrimaryKey(autoGenerate = true) val id: Long?,
    @ColumnInfo(name = "books_id") val booksId: Long,  // 所属账本
    @ColumnInfo(name = "type_id")  val typeId: Long,   // 一级支出分类 id；-1L = 总体预算
    @ColumnInfo(name = "amount")   val amount: Long,   // 限额，单位：分（守金额约定）
)
```

**对附录的修正（feasibility，重要）**：附录原写「`type_id NULL=总体`」。但 **SQLite 的 UNIQUE 索引中 NULL 值互不相等**——同账本可插入多条 `type_id=NULL` 的总体预算，唯一约束对总体失效。故改用**哨兵 `type_id = -1L` 表总体**：-1 是非 NULL 值，正常参与唯一约束 → 保证每账本最多一条总体预算 + 每个一级分类最多一条预算。

**哨兵安全性依据（节点1 feasibility/security 修正）**：附录原写「真实 type id ≥1」**事实错误**——项目存在负数固定 type id：报销/退款/信用卡还款（`FIXED_TYPE_ID_REFUND=-2001`/`REIMBURSE=-2002`/`CREDIT_CARD_PAYMENT=-2003`，`core/common/.../Constants.kt:83-89`）、平账（`TYPE_TABLE_BALANCE_EXPENDITURE.id=-1101`/`BALANCE_INCOME=-1102`，`TypeTable.kt:66+`）。`-1L` 哨兵仍安全的**真实依据**是：-1L 不与任何现存 type id 冲突（负数固定类型 -2001~-2003/-1101/-1102 + autoGenerate ≥1 的自定义分类均≠-1L）；且 -1L 不属 EXPENDITURE 一级分类集，§4.2 总体 spent 走独立 Σ 分支、`pieList.find{typeId==-1}` 永不命中，逻辑自洽。

> `TypeTable.parentId = -1L` 表「无父」（`TypeTable.kt:69` 等），本表 `type_id = -1L` 表「总体」，二者在各自表语义独立、不交叉，无歧义。

### 3.2 Migration12To13（DB v12→13）

- **纯新增表，不涉及表重建** → 直接 `CREATE TABLE db_budget(...)` + `CREATE UNIQUE INDEX index_db_budget_books_id_type_id`，**不套 `_new` 四步模式**（`_new` 模式仅用于改列/裁字段的表重建，见 CLAUDE.md）。
- **F3 兜底搭车**：同一 migration 内追加 `DROP TABLE IF EXISTS db_record_temp`——清理历史 `Migration6To7.migrateRecord` 遗漏的 `db_record_temp` 泄漏（已 main `f4cc7514` 修今后升级路径，此处清存量已残留库），防其污染备份恢复（`DatabaseMigrations.copyData` 遍历 `db_` 前缀表搬运）。
- `DB_VERSION` 从 12 → 13（`ApplicationInfo.kt:29`）。
- `DatabaseTest.migrate12_13` 守护：`runMigrationsAndValidate(13, validateDroppedTables = true)` —— 校验 `db_record_temp` 已清除 + `db_budget` 建成 + 唯一索引存在。

## 4. 模块结构与数据流

### 4.1 分层（遵循 app→feature→core）

| 模块 | 新增内容 |
|---|---|
| `core:database` | `BudgetTable` + `BudgetDao` + `Migration12To13`（注册进 `DatabaseMigrations`） |
| `core:model` | `BudgetModel`（领域模型）+ `BudgetProgressEntity`（总体 + 各分类的 限额/已花/进度/状态，供 UI 消费）+ `BudgetStateEnum`(NORMAL/NEAR/OVER) |
| `core:data` | `BudgetRepository` + `BudgetRepositoryImpl`（CRUD + `budgetsFlow`） |
| `core:domain` | `GetBudgetProgressUseCase`（聚合，复用 `TransRecordViewsToAnalyticsPieUseCase`） |
| `feature:budget`（**新模块**） | `BudgetRoute` / `BudgetScreen` / `BudgetViewModel` + 设·改限额对话框 + navigation |
| `feature:settings` | `LauncherDrawerActions` 加第 8 回调 `onBudgetClick`（**public**） |
| `app` | `MainApp` 接线 budget 导航 + 构造回调；`settings.gradle.kts` include |

`feature:budget` 的 `build.gradle.kts` 照抄 `feature/tags/build.gradle.kts`（`cashbook.android.library.feature` + `.compose` + `.jacoco` + roborazzi）。

### 4.2 `GetBudgetProgressUseCase` 数据流（方案 A）

```
输入：monthStartDay D（SettingRepository）
1. period   = DateSelectionEntity.currentMonthPeriod(today, D)             // 可配置周期「本月」
2. records  = getRecordViewsBetweenDateUseCase(period, D)                  // 本周期全部 recordViews
3. pieList  = transRecordViewsToAnalyticsPieUseCase(EXPENDITURE, records)  // 各一级分类已花(净自付)
4. budgets  = budgetRepository.getBudgetsByBooks(currentBooksId)           // 已设限额(总体 type_id=-1L + 各分类)
5. 组装 BudgetProgressEntity：
   · 总体     : limit = budgets[-1L]?.amount
                spent = Σ analyticsPieNetAmount(EXPENDITURE, records 各条)   // 直接对本周期 EXPENDITURE 求和(见下"总体 spent")
   · 各分类   : 仅遍历"已设预算"的 budget(type_id≠-1L)，spent = pieList.find{ typeId==type_id }?.totalAmount ?: 0
   · 每项     : progress = if(limit>0) (spent.toFloat()/limit).coerceAtMost(1f) else null
                state    = NORMAL(<80%)/NEAR(80–100%)/OVER(>100%, overAmount=spent−limit)
输出：BudgetProgressEntity(overall: BudgetItem?, categoryList: List<BudgetItem>)
```

- **总体 spent 不靠「Σ 各分类 pieList」**（节点1 reverse High 修正）：`TransRecordViewsToAnalyticsPieUseCase` 只返回 `List<AnalyticsRecordPieEntity>`、**不返回 total**；备份恢复合并致「记录引用已删 type」的孤儿场景下 `Σ pieList < 真实总额`。故总体 spent **直接对本周期 EXPENDITURE recordViews 求 Σ `analyticsPieNetAmount`**（与 Pie UseCase 内部 total 同算法、与各分类口径一致），与「仅展示已设预算分类」解耦。
- **已花自动限当前账本**（节点1 feasibility）：`getRecordViewsBetweenDateUseCase` 内部走 `queryByBooksIdBetweenDate(currentBookId,...)`（`RecordRepositoryImpl.kt:322-326`）已按当前账本过滤；该 `currentBookId` 与 step4 `budgetRepository` 的 `currentBooksId` 同源（均取 `recordSettingsData.currentBookId`）→ 已花与 budgets 同账本，无需手动过滤 records。
- **当前账本** `currentBooksId` 经 `BooksRepository.currentBook.map{ it.id }`（`BooksRepository.kt:27` 接口属性，与报销管理同源）。
- **响应式刷新 + 账本竞态规避**（节点1 reverse Low）：`BudgetViewModel` 以**当前账本流为外层** `flatMapLatest`——`booksRepository.currentBook.flatMapLatest { book -> combine(recordDataVersion, settingRepository.recordSettingsModel.map{monthStartDay}.distinctUntilChanged(), budgetRepository.queryByBooksFlow(book.id)) { ... } }`，确保 booksId、budgets、records 三者由同一账本派生，避免快速切账本时「A 账本预算 + B 账本已花」错配帧。settings 是 cold flow，必须订阅进流（不能 `.first()` 取一次）。

### 4.3 `BudgetDao` 关键方法

- `queryByBooksFlow(booksId): Flow<List<BudgetTable>>`（驱动 budgetsFlow）
- `queryByBooks(booksId): List<BudgetTable>`（UseCase 一次性取）
- `queryByBooksAndType(booksId, typeId): BudgetTable?`
- `upsert(budget)`（限额 upsert，**用 Room `@Upsert` 或 `@Insert(onConflict=REPLACE)` 依赖 `(books_id,type_id)` 唯一索引原子处理**，节点1 security Low：避免 Repository 层「先查后写」的并发竞态）
- `deleteByBooksAndType(booksId, typeId)`（删单项预算）
- `deleteByBooksId(booksId)`（删账本级联）
- `deleteByTypeId(typeId)`（删一级分类级联；对任意 typeId 幂等，命不中即 no-op）

## 5. 级联删除边界（数据一致性 impact，androidTest 守护）

- **删账本**：在 `TransactionDao.deleteBookTransaction(id)` 的 `@Transaction` 内增删 `db_budget WHERE books_id=:id`——与项目现有跨表级联（记录/关联/资产）同事务同模式，保原子性、避免删一半残留孤儿。
- **删一级支出分类**：`TypeRepositoryImpl.deleteById(id)`（`:148`）内增删该 `type_id` 的预算（`budgetRepository.deleteByTypeId(id)` 或 DAO 层）。一级分类删除频率低、数据量小，Repository 顺序删可接受；是否收进单一 `@Transaction` 由 plan 阶段定。
- androidTest：删账本 → 该账本全部预算清空；删一级支出分类 → 该分类预算清空、总体预算与其他分类不受影响。

## 6. UI 设计（feature:budget，全用 core:design 封装）

### 6.1 预算屏布局

```
┌─ 预算管理（CbScaffold + CbTopAppBar）──────┐
│ 总体预算                                   │
│ ████████░░░░  ¥3200 / ¥5000   (64%)       │  NORMAL(主题色)
│                                           │
│ 分类预算                     [+ 添加分类预算] │
│ 🍔 餐饮  ███████████░ ¥900 / ¥1000         │  NEAR(橙)
│ 🚗 交通  ██████████████ ¥650/¥500 超¥150    │  OVER(红)
│ 🛒 购物  ████░░░░░░░░ ¥300 / ¥1000         │  NORMAL
└───────────────────────────────────────────┘
```

- **总体行**：未设总体预算时显「设置总体预算」引导；已设则显进度条。
- **点总体/分类行** → 设·改限额对话框（`CbAlertDialog` + 金额输入）。**金额校验（节点1 security Med）**：经 `String.toAmountCent()` 转分后必须校验 `> 0`（拒绝 ≤0，`toAmountCent` 对负/非数字返 0、对超大输入静默低 64 位回绕）+ 设合理上界（如 ≤ `999999_00` 分=999 万元防溢出/误输入）；`KeyboardType` 限数字、过滤负号。校验不过给提示、不落库。对话框内含「删除」按钮删该项预算。
- **「+ 添加分类预算」** → 弹**未设预算的一级支出分类**选择器：数据源 `TypeRepository.firstExpenditureTypeListData`（对外 Flow，已 `filter{ EXPENDITURE }`；节点1 feasibility：原 spec 引的 `getFirstRecordTypeList()` 是 private 不可调）→ **排除固定类型**（`it.id > 0`，剔除平账 -1101 等负 id 固定一级支出类型，避免「平账预算」无意义项；节点1 feasibility/security Med）→ 再减去已设 type_id → 选后弹限额对话框。
- **空态**：未设任何预算（总体 + 分类皆无）→ 引导文案 + 「+ 添加预算」。
- 金额显示用 `Long.toMoneyCNY()` / `toMoneyString()`（守金额约定）。

### 6.2 抽屉入口

`LauncherDrawerActions` 第 8 回调 `onBudgetClick`，置于「待报销」之后（财务管理类入口聚集）。跨 5 层透传由现有 `actions` 聚合承载（无需逐参）。新增 `LauncherDrawerActions` 字段须保持 **public**（`settingsLauncherScreen` 是 public 跨模块 API，否则 `compileDebugKotlin` 报 expose internal）。

### 6.3 超支视觉（两级）

`BudgetStateEnum`：`NORMAL`(<80%, 主题色) / `NEAR`(80–100%, 橙) / `OVER`(>100%, 红 + 显示超支额)。橙/红配色用 core:design 主题色扩展（plan 阶段定具体 token）。

## 7. Low backlog ③：固定周期态月标签语义（独立小改，本 spec 含）

- **现状**：`RecordMonthSummaryHeader.SummaryColumn`（`feature/records/.../view/RecordMonthSummaryHeader.kt:122,127,132`）硬编码 `month_income`/`month_expend`/`month_balance`（月收入/支出/结余），不随周期态变。固定周期态（年/全部/区间，`monthSwitchable=false`）时语义不对。
- **处理**：该 header 已有 `monthSwitchable: Boolean` 参数（`:56`）。`monthSwitchable=true` → "月收入/月支出/月结余"；`=false` → 中性"收入/支出/结余"。**新增中性 string（如 `summary_income/expend/balance`），不得删/改现有 `month_income/expend/balance`**（节点1 impact Low：`CalendarScreen.kt:287,292,297` 与 `LauncherContentScreen.kt:411` 仍消费旧 `month_*`，误删会破坏日历/首页月份态）。
- **影响**：`TypedAnalyticsScreen` / `AssetInfoContentScreen` 固定态截图基线需 re-record。
- **不在此项**：首页 `LauncherContentScreen.kt:411` 已按 `DateSelectionTypeEnum` 派生标签。

## 8. 测试策略（守「测试替身忠实复刻 DAO/SQL 语义」强约定）

| 层 | 测试 |
|---|---|
| 纯函数(JVM) | `BudgetProgressEntity` 组装抽 top-level `internal fun`：progress/state(NORMAL/NEAR/OVER/limit=0→null/无总体预算/spent=0/spent 远超 limit 时 progress coerceAtMost 1f) + mutation 验证判别力 |
| 校验(JVM) | 限额输入校验：负/0/非数字/超大(回绕)输入被拒、合法值通过、上界拒绝（节点1 security Med） |
| Repository | `BudgetRepositoryImpl` CRUD + budgetsFlow（`FakeBudgetDao` 忠实复刻 upsert/(books_id,type_id) 唯一约束/级联删除语义，禁空桩） |
| UseCase | `GetBudgetProgressUseCase` 端到端（Fake Record/Budget/Setting Repo）：净自付已花口径、**总体 spent = 直接 Σ EXPENDITURE（非 Σ pieList）**、可配置周期 D 生效、仅 EXPENDITURE、含负 id 固定类型(平账)被排除出选择器 |
| ViewModel | `BudgetViewModel` 响应式（记账/改限额/改 D/切账本→重算、flatMapLatest 切账本无错配帧）、空态、设·改·删限额、添加分类选择器排除已设 |
| androidTest(DAO) | `migrate12_13`(validateDroppedTables=true: db_record_temp 清 + db_budget 建 + 唯一索引) + 唯一约束冲突(同账本同 type_id) + upsert 二次调用为 update 非重复插入 + 级联删除(删账本/删一级分类→预算清) + **v12 备份 `recoveryFromDb`→v13 含空 db_budget 返 true**(防漏注册 migration 回归) |
| 截图 | 预算屏四态(NORMAL/NEAR/OVER/空态)基线；`LauncherScreen` success 两态 re-record(新增抽屉项致 UI 变化、非 0 diff)；Low backlog③ 受影响屏(TypedAnalytics/AssetInfoContent 固定态) re-record |

## 9. 影响面 / 必须同步修改清单（impact，防编译失败/回归）

- `settings.gradle.kts`（include `:feature:budget`）
- **`app/build.gradle.kts`（加 `implementation(projects.feature.budget)`）**（节点1 impact High：现 `:152-158` 无 budget，MainApp 调 `budgetScreen` 必须先有此依赖，否则 `:app:compileDebugKotlin` unresolved）
- `core/common/.../ApplicationInfo.kt`（`DB_VERSION` 12→13）
- `core/database/`：`BudgetTable` + `BudgetDao` + `Migration12To13` + `DatabaseMigrations`(注册进 `MIGRATION_LIST`) + `CashbookDatabase`(加 entity/dao) + **`di/DatabaseModule.kt` 加 `providesBudgetDao`**（节点1 impact Med：每个 DAO 均在此显式 provide，缺则 Hilt KSP 报 missing binding） + schema json
- `core/database/.../TransactionDao.kt`（`deleteBookTransaction` 加删 budget）
- `core/model/`：`BudgetModel` + `BudgetProgressEntity` + `BudgetStateEnum`（plan 阶段坐实是否需 `compose_compiler_config.conf` 稳定性标注）
- `core/data/`：`BudgetRepository` + `BudgetRepositoryImpl` + Hilt 绑定 module；`TypeRepositoryImpl.deleteById` 加级联（构造增 `budgetRepository`/`budgetDao` 依赖；不破坏 `TypeRepositoryImplTest`——其不实例化 Impl）
- `core/domain/.../GetBudgetProgressUseCase.kt`（包名 `cn.wj.android.cashbook.domain.usecase`，非 `core.domain`）
- `feature/budget/`（全新模块：build.gradle.kts 照抄 feature/tags + Route/Screen/ViewModel/navigation/dialog）
- `feature/settings/.../navigation/LauncherDrawerActions.kt`（加 `onBudgetClick`，public）
- `feature/records/.../view/RecordMonthSummaryHeader.kt`（Low backlog③）
- `app/.../MainApp.kt`（`:416` 构造 `LauncherDrawerActions` 补 `onBudgetClick` + budget 导航接线）
- **CI/baseline**：新增模块改 classpath → `app/build.gradle.kts:240` `dependencyGuard` baseline 须**走 PR 由 CI 自动回填**（节点1 impact Med：CLAUDE.md 既有约定，不可直接 push main）
- **测试同步**（漏改则模块 `testDebugUnitTest` 整体编译失败）：
  - `core/testing/`：`FakeBudgetRepository`（若新增）
  - `core/data/src/test/`：`FakeBudgetDao`（忠实桩）
  - **全部 4 个 `LauncherDrawerActions` 构造点补 `onBudgetClick`**（节点1 impact High：`MainApp.kt:416` + **`LauncherScreen.kt:79` Route wrap**[原漏列] + `LauncherScreenScreenshotTests.kt` ×?；实施时 Grep `LauncherDrawerActions(` 确认全覆盖）
  - `RecordMonthSummaryHeader` 相关截图测试

## 10. 未决 / Backlog（非本期阻塞）

- 多账本预算复制/模板（新账本继承上一账本预算）——本期不做。
- 预算历史回看（往月预算执行情况）——表无月份维度，本期仅「本周期」。
- 预算到期/超支通知（WorkManager + 权限）——附录已定仅视觉，不做。
- 总体预算与分类预算的「分类之和 vs 总体」一致性校验/提示——本期各自独立，不做。
- `monthSwitchable=false` 中性标签若需区分「年/区间/全部」更精确文案——本期统一中性「收入/支出/结余」，精确化列 backlog。

### 10.1 节点1 评审衍生的显式记录（设计决策，非阻塞）

- **「已花」用净自付而非毛支出（语义选择，节点1 reverse Med）**：已花口径为 `analyticsPieNetAmount`（净自付），与全应用/数据分析饼图一致。**反直觉表现需知晓**：① 本月某支出被当月报销吸收，「已花」按净额计（毛 ¥1500、被报销 ¥800 → 计 ¥700），与「我刷了 1500」的预算心智不同；② 报销款滞后录入（本月刷、下月录报销）时，因 `finalAmount` 重算，本月「已花」会在用户无新支出时自行回落。**本期保持净自付**（统一性优先），显式记录此语义选择，避免上线被当 bug；如用户反馈更需「毛支出预算」，可加口径开关（backlog）。
- **DB v13 不支持降级（项目既有约束，非本 spec 新增，节点1 reverse Low）**：`DatabaseModule.kt:52-58` Room builder 未配 `fallbackToDestructiveMigrationOnDowngrade`，装 v13 后回退旧版 App 启动崩溃——所有 DB 版本升级共有，前 11 次 migration 同此。记录提示，不在本期改变此既有策略。
- **备份恢复合并语义（节点1 reverse Critical 实测排除）**：db_budget 的 `(books_id,type_id)` 唯一索引是全库首个业务唯一约束。**PoC 实测（sqlite 3.50.4）**：`copyData` 用的 `INSERT OR REPLACE`(`CONFLICT_REPLACE`) 对唯一索引冲突**不抛异常**——删冲突行再插入、备份覆盖当前（符合 `copyData` KDoc `DatabaseMigrations.kt:87-94` 既定「备份优先合并」语义）。故 autoGenerate id + 唯一索引在备份恢复下工作正常，**无需改复合主键**。
- **模块结构权衡（节点1 reverse Low）**：选「新建 `feature:budget` 独立模块」而非「挂靠 feature:settings/books 子屏」。代价是整套 build.gradle/截图/jacoco/navigation 样板 + `onBudgetClick` 须 public 跨模块暴露；收益是隔离、编译并行、与现有 feature 模块组织一致。本期取独立模块，记录此权衡。

---

## 附：本 spec 不含（范围外，独立处理）

- journey 黑盒剩余路径（编辑/删除/隐藏资产/删切账本/搜索/导入回归验证）——测试验证活动，独立排期。
- Low backlog ②（`FakeRecordRepository.queryRecordsByTypeIdInRange` 不建模 `includeChildTypes`）——测试桩小项，靠 DAO androidTest 覆盖，可随本期 Fake 改造顺带。
- Low backlog ④（标签区间 androidTest）——spec1 已真机跑 `connectedDebugAndroidTest` 109 green 覆盖。
