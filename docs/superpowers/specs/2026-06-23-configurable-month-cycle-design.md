# 设计：全应用可配置月周期（月起始日）

> 创建于 2026-06-23 · superpowers brainstorming 产出
> 状态：待用户评审
> 本 spec 是「预算管理」需求拆分后的**前置子项目 1**。预算管理（消费本周期能力）为独立 spec2，其已收齐的决策见文末附录。

## 1. 背景与目标

当前「本月统计」一律按**自然月**（1 号 00:00 至次月 1 号 00:00，半开区间）。用户需求：新增**可配置月起始日 D**（默认自然月），使「本月」= `[本月 D 日, 次月 D 日)`，以支持「按发薪日/账单日为周期」的统计习惯。

- **D 取值 1–28**（避开 29/30/31——`YearMonth.atDay(29..31)` 在短月抛 `DateTimeException`）。
- **默认 D=1 即现状自然月**，老用户无感知、零数据迁移、可逐字节回归验证。
- 口径不变：仍为净自付（`finalAmount` / `analyticsPieNetAmount`），本子项目**只改"月的起止边界"，不碰任何金额计算**。

### 1.1 作用范围（用户决策）

| 范围 | 决策 |
|---|---|
| 作用面 | **全应用统一可配置**（首页 / 数据分析 / 分类·标签统计 / 资产详情 + 预算[spec2]） |
| 日历屏 | **保持自然月不变**（网格与月汇总同源、该屏自洽，不引入周期） |
| 周期单位 | 月（与首页本月/分析口径一致） |
| 超出范围 | 预算管理（spec2）；备份恢复纳入该设置（backlog）；DST/跨时区严谨处理（现状即有，不在本期） |

### 1.2 节点1 team-review 结论（已纳入本设计）

四维 ad-hoc 评审（team-review 编排设施主 controller 不可用，降级 `Agent`+`agent-teams:team-reviewer` 四维并行 + controller 手动去重/校准 + hands-on 核验）揭示**月区间在仓库有四套独立实现**，初版"只改 `toDateRange()`+4 VM"覆盖不全。两个 Critical 已 controller hands-on 读码证实，本设计已扩大范围覆盖：

| 路径 | 用途 | 处置 |
|---|---|---|
| `DateSelectionEntity.toDateRange()` ByMonth | 首页/资产/标签汇总/标签列表 | 加 `monthStartDay` 参 |
| `RecordRepositoryImpl.queryPagingRecordListByTypeIdBetweenDate` 字符串解析（`:167-173`） | **分类·类型记录列表分页** | **C1：改走 `toDateRange` 毫秒区间，消除字符串解析器** |
| `TransRecordViewsToAnalyticsBarUseCase` 建桶（`:69-76`） | **数据分析柱状图** | **C2：按真实周期区间建日桶** |
| `RecordRepositoryImpl.queryRecordByYearMonth`（`:337-361`） | 日历 | 保持自然月，不改 |

## 2. 现状事实（已 hands-on 核验，带出处）

- `DateSelectionEntity.toDateRange()`（`core/model/.../entity/DateSelectionEntity.kt:52-83`）：`ByMonth` 分支（`:61-65`）`yearMonth.atDay(1)` → `plusMonths(1).atDay(1)`，半开区间，基于 `ZoneId.systemDefault()` 的 `atStartOfDay`。
- `toDateRange()` 全部 7 处调用方：`GetRecordViewsBetweenDateUseCase.kt:42`（唯一生产调用方 = `AnalyticsViewModel.kt:89`）、`AssetInfoContentViewModel.kt:75,106`、`LauncherContentViewModel.kt:117,132`、`TypedAnalyticsViewModel.kt:150,247`。
- 首页 `LauncherContentViewModel`（`:110-133`）经 `toDateRange()`，默认 `_dateSelection = ByMonth(YearMonth.now())`（`:110-112`），已注入 `SettingRepository`（`:61`）。
- 分类列表分页 `TypeRecordPagingSource.load`（`TypedAnalyticsViewModel.kt:221`）走 `selection.getDisplayText()` →（"YYYY-MM"）→ `GetTypeRecordViewsUseCase`（`GetTypeRecordViewsUseCase.kt:49-56`）→ `queryPagingRecordListByTypeIdBetweenDate`（`RecordRepositoryImpl.kt:148-201`），内部对 "YYYY-MM" 自构造 `[atDay(1), +1 month)` **自然月**，**不经 `toDateRange()`**。标签列表 `TagRecordPagingSource`（`:247`）则走 `toDateRange()`。
- 柱状图 `TransRecordViewsToAnalyticsBarUseCase`（`core/domain/.../TransRecordViewsToAnalyticsBarUseCase.kt`）ByMonth 分支（`:69-76`）按 `ym.atEndOfMonth().dayOfMonth` 建**自然月**日桶；总额对桶求和（`AnalyticsViewModel` 侧）。
- 日历 `CalendarViewModel`（`:72-99`）→ `GetCurrentMonthRecordViewsUseCase`（`:37`）→ `queryRecordByYearMonth`（`RecordRepositoryImpl.kt:337-361`，字符串自构造自然月），与 `toDateRange()` 完全隔离。
- DAO 区间 SQL 已是半开：`queryByBooksIdBetweenDate` / `queryRecordByTypeIdBetween` 用 `record_time<:endDate`（`RecordDao.kt:64,206`），无闭区间双计风险。
- 设置存储 `record_settings.proto`（per-app，现有字段号 1–7）；`RecordSettingsModel`（`:24-39`，7 字段无默认值 data class）；映射在 `CombineProtoDataSource`；序列化 `RecordSettingsSerializer`（`getDefaultInstance()`，proto3 标量缺省 = 0）。
- 本改动**无 Room schema 变更**，不需要 DB migration（monthStartDay 存于 Proto DataStore）。

## 3. 设计

### 3.1 配置存储与默认值

- `record_settings.proto` 新增 `int32 monthStartDay = 8;`（字段号 8 未占用，proto3 增量向后兼容）。
- 链路：`RecordSettingsModel` 加 `val monthStartDay: Int = 1`（**给默认值**以降低构造点连带破坏）→ `CombineProtoDataSource` 映射 → `SettingRepository` 暴露读流 + `updateMonthStartDay`。
- **消费端唯一收口 clamp（H1，强制）**：映射处 `monthStartDay = it.monthStartDay.let { d -> if (d in 1..28) d else 1 }`（proto 默认 0 / 越界 / 篡改 → 1），且 `toDateRange(monthStartDay)` / `currentMonthPeriod` 入参再 `coerceIn(1,28)` 防御性兜底。**不依赖设置 UI 的 1–28 单点限制**——否则全体存量用户升级首读 `monthStartDay=0` → `atDay(0)` 抛 `DateTimeException` 致首屏崩溃。

### 3.2 统一月区间工具（core:model 纯函数）

- `DateSelectionEntity.toDateRange(monthStartDay: Int = 1)`：仅 `ByMonth` 分支改为 `[yearMonth.atDay(D), yearMonth.plusMonths(1).atDay(D))`（D = `monthStartDay.coerceIn(1,28)`）。`ByDay/ByYear/DateRange/All` 不变。**D=1 与现状逐字节等价**（金丝雀守护）。默认参 → 7 处现有调用源码兼容。
- 新增 `DateSelectionEntity.Companion.currentMonthPeriod(today: LocalDate, monthStartDay: Int): ByMonth`：`today.dayOfMonth >= D → ByMonth(YearMonth.from(today))`，否则 `→ ByMonth(YearMonth.from(today).minusMonths(1))`（跨年由 `YearMonth.minusMonths` 处理）。D=1 时恒等于 `ByMonth(YearMonth.now())`。
- 月 label 策略：`getDisplayText()` 保持 `"YYYY-MM"`，**约定其语义为"以该月 D 日为起点的周期"**（M1：D≠1 时标签不显式表达区间，v1 接受；范围提示列 backlog）。上/下月切换仍 `YearMonth ±1` 作用于 `ByMonth` label，周期整体平移。饼图下钻经 `getDisplayText()`↔`fromDisplayTextOrNull` round-trip 保留 `YearMonth`，两端各套**同一全局 D** → 周期自洽，不丢 D。

### 3.3 改造调用点（走周期；D=1 全部等价现状）

**A. 经 `toDateRange()` 的路径** —— 传入配置 D：
- 首页 `LauncherContentViewModel`（`:117,132`）、`AnalyticsViewModel`（经 `GetRecordViewsBetweenDateUseCase`，给其 `toDateRange()` 传 D 或加 D 参）、`TypedAnalyticsViewModel`（`:150` summary、`:247` 标签列表）、`AssetInfoContentViewModel`（`:75,106`）。

**B. C1 分类·类型列表（消除字符串解析器）**：
- `TypeRecordPagingSource`（`TypedAnalyticsViewModel.kt:221`）改为与 `TagRecordPagingSource` 对齐：`val (start,end) = selection.toDateRange(D)` → 走毫秒区间的类型分页查询（`GetTypeRecordViewsUseCase` 改/加毫秒区间签名 → `RecordRepository` → `queryRecordByTypeIdBetween`/`queryRecordByTypeIdExactBetween`，二者已是毫秒半开）。`All` 态 → `(0, Long.MAX_VALUE)`。废弃/不再走 `queryPagingRecordListByTypeIdBetweenDate` 的 "YYYY-MM" 自然月分支（兑现项目既定"消除第三份 dateStr 解析"方向）。

**C. C2 数据分析柱状图（按真实周期建桶）**：
- `TransRecordViewsToAnalyticsBarUseCase` 加 `monthStartDay` 参；`ByMonth` 分支日桶从 `ym.atDay(D)` 迭代到 `ym.plusMonths(1).atDay(D)` 前一天（含跨月的"次月 1..D-1"），替代 `repeat(ym.atEndOfMonth().dayOfMonth)`。其余分支不变。

**D. H2 响应式 D 注入**：
- `AnalyticsViewModel` / `TypedAnalyticsViewModel` / `AssetInfoContentViewModel` **新增注入 `SettingRepository`**（`LauncherContentViewModel` 已有）。
- 各统计/列表流 `combine` 进 `settingRepository.recordSettingsModel.map { it.monthStartDay }.distinctUntilChanged()`，使**改 D 后存活 VM 自动重算**（settings 是 cold flow，必须订阅进流，不能 `.first()` 取一次）。
- 初始/当前周期：`init` 中读到 D 后将 `_dateSelection` 设为 `currentMonthPeriod(today, D)`（D=1 为 no-op，无闪烁/无截图回归；D≠1 首帧由 now-月切到周期-月，可接受或加首帧 gate）。

**不改（保持自然月）**：日历 `CalendarViewModel`/`queryRecordByYearMonth` 原样；其余 `toDateRange()` 调用方（如 `GetRecordViewsBetweenDateUseCase` 若服务非周期场景）用默认 D=1。`GetRecordViewsBetweenDateUseCase` 唯一生产调用方是 `AnalyticsViewModel`（应走周期），需确保 D 真正生效（不可只改 VM 不改 UseCase 内 `toDateRange()`）。

### 3.4 设置 UI

- 设置页（`feature:settings`）新增「月起始日」项：1–28 选择器 + 说明"影响本月统计的起止（如 15 表示每月 15 日至次月 14 日）"。改设置经 settings 流驱动所有周期流自动重算。

## 4. 影响面 / 必须同步修改清单（impact 维度产出，防编译失败/回归）

- `core/datastore-proto/.../record_settings.proto`（加 `monthStartDay=8`）
- `core/model/.../model/RecordSettingsModel.kt`（加字段，带默认 `=1`）
- `core/datastore/.../CombineProtoDataSource.kt`（映射 + 0/越界→1 clamp；`updateMonthStartDay`）
- `core/datastore/.../RecordSettingsSerializer.kt`（确认默认实例行为）
- `core/data/.../SettingRepository.kt` + `SettingRepositoryImpl.kt`（`monthStartDay` 读流 + 写方法）
- `core/model/.../entity/DateSelectionEntity.kt`（`toDateRange(monthStartDay)` + `currentMonthPeriod`）
- `core/domain/.../TransRecordViewsToAnalyticsBarUseCase.kt`（C2 建桶 + D 参）
- `core/domain/.../GetTypeRecordViewsUseCase.kt`（C1 毫秒区间签名）
- `core/data/.../RecordRepository.kt` + `RecordRepositoryImpl.kt`（C1 类型分页毫秒区间）
- `core/domain/.../GetRecordViewsBetweenDateUseCase.kt`（D 生效）
- `feature/records/.../viewmodel/{LauncherContentViewModel,AnalyticsViewModel,TypedAnalyticsViewModel,AssetInfoContentViewModel}.kt`（走周期 + 3 处加 `SettingRepository` 注入）
- `feature/settings/...`（月起始日设置 UI + 入口）
- **测试同步**（项目强约定，漏改则模块 `testDebugUnitTest` 整体编译失败）：
  - `core/testing/.../FakeSettingRepository.kt`（补 `monthStartDay`，若接口加方法须实现）
  - `core/data/src/test/.../FakeCombineProtoDataSource.kt`（补字段）
  - `core/testing/.../FakeRecordRepository.kt`（C1 类型分页毫秒区间忠实桩）
  - `feature/records/src/test/.../{Analytics,TypedAnalytics,AssetInfoContent}ViewModelTest.kt`（构造补 `FakeSettingRepository`）
  - 相关 UseCase 测试（`GetTypeRecordViewsUseCaseTest` / `GetRecordViewsBetweenDateUseCaseTest` / 新增 Bar UseCase 周期测试）
- **无 Room migration**（仅 Proto DataStore 增字段，向后兼容靠映射层默认）。
- **截图测试**：4 个 `*ScreenScreenshotTests` 不直接构造 VM（用 Composable 预览数据），不因 VM 构造签名变更而编译失败；但 Preview 勿引入 `now()`（固定日期，防跨月脆弱）。设置页新增项需补/更新截图基线。

## 5. 测试策略

- **纯函数（core:model JVM `:core:model:test`）**：
  - `toDateRange(monthStartDay)`：D=1 金丝雀对所有分支与无参 `toDateRange()` epochMilli 相等；D=15 跨月 `[1/15,2/15)`；边界 day==D；12 月 +D 跨年；D∈{1,28} 边界；非法 D（0/负/29/31/超大）经 coerce 不抛。
  - `currentMonthPeriod(today, D)`：day≥D→本月、day<D→上月、D=1→恒本月、1 月跨年（today=1/5,D=15→上年 12 月）。
- **配置链路**：proto→model 映射 clamp/0→1；`SettingRepository` 读写往返。
- **C1 金丝雀**：D≠1 时分类·类型统计屏「列表区间 == 汇总区间」（消除分裂）。
- **C2**：Bar UseCase ByMonth + D≠1，跨月记录进对应日桶、Σ桶 == 周期总额。
- **VM**：初始周期推导（currentMonthPeriod）、monthStartDay 变化触发重算（响应式）、D=1 等价既有测试。
- **回归金丝雀**：D=1 全应用逐字节等价现状；D≠1 时日历屏仍自然月（不受 monthStartDay 影响）。

## 6. 未决 / Backlog（非本期阻塞）

- M1：D≠1 月 label 仅 "YYYY-MM"，不显式展示区间（可加副标题"D日~次月D-1日"）。
- M2：日历屏（自然月）与首页（周期）在 D≠1 数字不同——可加"本页按自然月统计"提示。
- M3：DST / 跨时区边界（现状即有，中国无 DST，概率低）；如需严谨须固定记账时区。
- 备份恢复纳入 `monthStartDay`（现有 RecordSettings 偏好本就不入备份，行为一致；如需跨设备迁移另扩备份 schema）。
- 首页 `currentMonthPeriod(today,...)` 的 `today` 在 VM 长生命周期跨天不自动刷新（现状自然月亦有此性质，D≠1 边界更隐蔽）。

---

## 附录：预算管理（spec2）已收齐决策（避免丢失，供后续 brainstorming/writing-plans 复用）

> 预算管理在本次 brainstorming 中已问全决策，因拆分为 spec2 暂不实施；其依赖本子项目的「可配置月周期」能力。

- **作用域**：每个账本独立预算（预算表带 `books_id`；分类 `TypeTable` 全局无 books_id，但记录/支出按账本）。
- **周期**：每月固定循环限额（表无月份维度，最简）；「本月」沿用本子项目的可配置周期。
- **维度**：总体预算 + 各「一级支出分类」预算（EXPENDITURE first-level；收入/转账不设预算）。
- **口径**：净自付 `finalAmount`（沿用全应用约定）；「各分类已花」可复用 `TransRecordViewsToAnalyticsPieUseCase`（已按一级分类滚动汇总净自付）；总体已花 = Σ EXPENDITURE 净自付（= 同屏各分类之和，内部一致，不含转账手续费）。
- **入口与展示**：左抽屉新增「预算管理」入口（聚合到现有 `LauncherDrawerActions` 第 8 回调）→ 独立预算屏（顶部总体进度条 + 各分类进度列表 + 设/改限额）；暂不改首页。
- **超支表现**：仅视觉提示（进度条变色 + 已花/限额/超支额），无通知/无后台任务/零额外权限。
- **模块**：新建 `feature:budget` 模块（聚合 usecase 在 core:domain，复用无损）。
- **存储**：新建 Room 表 `db_budget`（id, books_id, type_id[NULL=总体], amount 分；(books_id,type_id) 唯一）；Migration12To13（DB v12→13，`CREATE TABLE` + 索引）。
- **带入 backlog（spec2 搭车）**：① F3 在 Migration12To13 搭车 `DROP TABLE IF EXISTS db_record_temp`（清理历史 migrate6_7 泄漏、防污染备份恢复）；② 新增 `DatabaseTest.migrate12_13` + `validateDroppedTables=true` 守护。
