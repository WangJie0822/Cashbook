# 搜索结果收支汇总与分类饼图实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans in this session. Steps use checkbox syntax for tracking. 本次由控制器实施关键词/状态/页面，独立助手负责明确划定的聚合与模型文件；构建与提交由控制器串行执行。

**Goal:** 关键词与筛选均有全量净收支汇总及一张分类净额分布图，保留既有匹配和互斥。

**Architecture:** 单条只读分组 SQL 返回每组笔数、收入、支出及错误计数，同一快照在内存安全加总为总汇总；数据库分组到有效一级分类。关键词分页复用原 DAO，但固定查询上下文中的账本与金额参数。UI 消费原子统计结果，不从分页计算。

**Tech Stack:** Kotlin、Room、Paging、Flow、Compose、现有 CbPieChart；无新增依赖或 schema 迁移。

**Spec:** `docs/superpowers/specs/2026-10-30-search-result-statistics-design.md`

## Global Constraints

- 分支 codex/search-result-statistics，worktree D:/wt/search-result-statistics/Cashbook；base 为远端 main 的内容，不夹带分类修复。
- 当前会话执行；仅本地提交、不推送、不发布、不归档 Vault/Memory。
- 金额 Long 分；先分类有符号求和后 abs；占比基数 sum(abs(分类净额))，不等于结余。转账只有 charge−concessions；平账计笔数不计金额。
- 父级有效须存在、为一级且同收支类型；否则以自身分组；同名不同 ID 不合并。
- 旧关键词 remark LIKE/数字转换/-1 哨兵/record_time DESC 保持；查询上下文固定 bookId、keyword、amountCent。
- 所有读值绑定；筛选仍用 EXISTS 标签去重。预算覆盖实际统计与分页参数。
- 仅统计失败时保留可用列表；Initial 不查全账本；迟到工作不覆盖当前查询。
- 设计节点1四维已完成，依本机 AGENTS.md 收敛：Task1 为后续设计前提，命中 B，单独做一次定向代码评审；Task2 不逐任务评审，整批交付前仅跑一次 full-review。文案不进入复评循环。
- 当前未提供原生 Todo 工具，下面任务清单同步以 commentary 展示，执行细节另存本地 ledger，不以 ledger 冒充工具状态。

## Review Focus

1. 同一分类正负相抵必须先汇总再 abs，不能用逐笔绝对值：Task1 纯函数、Room 和变异覆盖。
2. 旧关键词 fake 的 contains 不等价于 SQL LIKE：Task1 真实旧 DAO 对照 `%`、`_`、引号、中文、数字、空白和两个账本。
3. 一页以外仍计入；汇总/分布不能跨版本：Task1 单 SQL、多页 fixture、写入失效与迟到结果测试。
4. 未知类型/失效父级不能丢笔数或猜分类：Task1 Room 与分类分组测试。
5. 小扇区、负数独占、零贡献及统计错误的 UI 不能误报空列表：Task2 行为/截图与 Task3 设备回放。

## 文件与接口约定

以下 main/test 文件遵守模块原有 Kotlin 包路径，新增文件在同包明确列出，不迁移既有文件。

- `core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/model/RecordSearchStatistics.kt`（新增）：`RecordKeywordQuery(bookId: Long, keyword: String, amountCent: Long)`；`RecordCategoryNet(typeId: Long, typeCategory: RecordTypeCategoryEnum, typeName: String, amount: Long)`；`RecordSearchStatistics(summary: RecordFilterSummary, categories: List<RecordCategoryNet>)`，派生 `magnitude: Long`，检查 abs、基数和结余溢出。
- `core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/relation/RecordSearchStatisticsRelation.kt`（新增）：单 SQL 组行 typeId/name/category、excluded、totalCount、income、expenditure、invalidAmounts，所有金额 Long，未知类型可空。
- `RecordFilterDao.kt`（现有 dao 包）：新增 `observeStatistics(SupportSQLiteQuery): Flow<List<RecordSearchStatisticsRelation>>`，观察 Record/Type/Tag/TagWithRecord。
- `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordSearchStatisticsSqlBuilder.kt`（新增）：`build(fromWhere: String, args: List<Any>): SupportSQLiteQuery`、`keyword(query: RecordKeywordQuery): SupportSQLiteQuery`。
- 同目录 `RecordFilterSqlBuilder.kt`：RecordFilterQueries 新增 statistics 查询，从同一 fromWhere/args 构建，预算纳入新查询。
- `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/RecordFilterRepository.kt` 与 impl：新增 `observeStatistics(RecordFilterRequest)`、`observeKeywordStatistics(RecordKeywordQuery)`，均返回 Flow<RecordSearchStatistics>；原 observeSummary 保留兼容。
- RecordRepository 接口、真实实现与 FakeRecordRepository：保留旧三参数入口，新增 `queryPagingRecordListByKeyword(query: RecordKeywordQuery, page: Int, pageSize: Int)` 重载；GetSearchRecordViewsUseCase 同样新增 query 重载，调用真实旧 DAO，并提供 `suspend createQuery(bookId: Long, keyword: String): RecordKeywordQuery` 在已注入 IO 上转换。VM 用同一 Deferred 供分页/统计 await，转换失败进入各自错误边界，不逃逸 UI 事件。
- `core/testing/.../repository/FakeRecordFilterRepository.kt`：实现新统计契约，保留 summaryProvider 的旧错误/迟到测试接线；新增 keywordRequests 与 keywordStatisticsProvider。SQL 正确性由真实 Room 测试承担。
- `feature/records/.../model/SearchFilterState.kt`：Ready 保留 context/summary 构造兼容，新增 categories 默认空列表。
- `feature/records/.../viewmodel/SearchViewModel.kt`：两种查询模式均启动统计；关键词分页携带不可变 RecordKeywordQuery。
- `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/view/SearchResultDistribution.kt`（新增）：SearchDistributionState/rememberSearchDistributionState管理可保存纯UI状态；LazyListScope.searchResultDistribution将首卡和展开余项接入主列表，消费RecordSearchStatistics，点击不修改查询。
- SearchScreen、search_filter_strings.xml、现有 SearchFilterScreenTest/SearchScreenScreenshotTests：接入两种模式统计、独立统计加载/错误、关键词贡献说明与新截图。

## Task 1：查询与原子统计链路

**Produces:** 上述模型、两种 observeStatistics Flow、固定关键词分页、Ready.categories。UI 不参与任务1。

- [x] 在现有 SearchFilterStateTest 写关键词汇总真实旧行为红灯，断言 Keyword 模式 summary 为 Ready；先运行目标用例并保留断言失败 XML。
- [x] 模型测试：+5/−3 同组=+2；−3 独占 magnitude=3；同组相抵为零；同名不同 ID；Long.MIN abs、绝对值和/结余溢出明确抛出异常。
- [x] 聚合 SQL 以有效父级 CASE 分组；组行总计兼顾平账及未知类型，不展开关系。转账差值用 typeof 检测整数溢出，坏差值贡献暂置0且标 invalidAmounts；仓库收到标志明确失败，不能把它当0。SQL SUM 自身溢出和 Kotlin addExact/subtractExact 均进入统计失败。
- [x] 真实 Room 测试放 `core/data/src/androidTest/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordSearchStatisticsRoomTest.kt`：对照旧 DAO，构造超过20条匹配、两个账本、重复标签、父子混记、跨类型同名、失效父级、零/负贡献、无匹配；记录及分类修改后统计失效；验证同一发射内汇总与分布守恒。
- [x] 增加/适配 data 的 SQL/仓库测试以及全部 RecordFilterDao fake；不能删去已有筛选预算/非法条件用例。
- [x] 关键词 query 由已有数值转换构造一次；分页传入同一个 query；初始不查询，关键词不带 appliedFilter。
- [x] 适配 FakeRecordRepository 的新重载与明确账本，保留旧入口兼容；FakeFilter 的真实类型/组关系计算与测试输入一致，不用全量空桩冒充统计。
- [x] VM 测试：Keyword/Filter 都 Ready；恢复筛选、切账本、关键词空白/初始、加载/失败/重试、旧响应迟到、编辑与分类改名刷新；旧断言只在确属本需求改变时更新，保留互斥/历史语义。
- [x] 执行 `./gradlew :core:model:test :core:data:testDebugUnitTest :core:domain:testDebugUnitTest :feature:records:testDebugUnitTest -Proborazzi.test.verify=false --console=plain`，检查真实 XML；仪器测试用 `:core:data:connectedDebugAndroidTest` 目标新类。
- [x] 无助手在写入时才提交精确文件清单，中文原子提交。任务1是后续接口前提，钉定 SHA 后派一名 reviewer；可测 finding 最多一轮定向复核，文案自行订正留账。

## Task 2：搜索页图表与状态

**Consumes:** Task1 的统计结果与 Ready.categories。

- [x] 先写 UI 行为断言：关键词统计可见；统计错误而列表仍在；正负/零/无匹配；图例超过5行可展开；折叠不改变查询；点图例选中/取消；小比例仍列出。
- [x] 新组件复用 CbPieChart，sliceSpaceDegrees=0、showLeaderLines=false；扇区为全部非零组，图例含零组。默认展开、默认前5，圆心用占比基数或选中组有符号金额；比例为0显示0%，极小非零显示小于显示精度。
- [x] 搜索页从整体 failed/pending 拆出统计状态，保留分页错误路径；仅统计失败不隐藏列表，统计加载不混入旧查询数据。两种模式均用贡献解释，不改共享 RecordListItem 默认值。
- [x] 新文案放现有 search_filter_strings.xml；沿用设计系统与主题，长中文、手机/平板/折叠屏布局可读且整页滚动。
- [x] 补 screenshot 用例；只录本次受影响的 records 截图，核对 Git 改动与可见图片，再验证完整截图基线。
- [x] 本地测试通过后原子提交本任务。此任务不含逐字产品实现代码，也不作为后续产品任务设计前提，不额外派 task review。

## Task 3：组合验收与交付

- [ ] 枚举本次 unit/androidTest 用例；全套执行 `./gradlew testOnlineDebug testOfflineDebug testDebugUnitTest :core:model:test :lint:test -Proborazzi.test.verify=false --rerun-tasks --console=plain`，逐 XML 复算总数。
- [ ] 构建 DevDebug、Spotless、Dependency Guard、Online/Offline/Dev Release Lint、自定义 Lint；执行 `verifyRoborazziDebug`，不重录无关模块。
- [ ] 变异独立副本：关键词统计接线回退、丢关键词账本、错用转账本金/截负值、先逐笔abs、只取第一页、漏标签去重/小分类、迟到结果覆盖、统计错误隐藏列表。每轮硬超时、基线先绿；记录具名失败和原因，变异源不碰实施树。
- [ ] 专用模拟器与合成数据，从真实搜索提交/筛选入口验证汇总、图例、折叠/选中、编辑后刷新；截图和账单数据留证；不操作用户账本。
- [ ] 全部开发结束后固定两个 SHA 做一次 full-review，完整保留发现及执行事件；按本机 AGENTS.md 的检查点处理阶段推进，不伪造批准。
- [ ] 文档按实际运行写完成态，复核影响范围；仅本地提交、保留 worktree，交付逐项报告构建、守卫、变异、提交、知识归档（本次不归档）。

## 自检

设计各节均由 Task1/2 兑现，Task3验证。模型/接口在各任务间同名同型；同快照选择单 SQL 已明确，避免独立 Flow 拼接。后续发现无法维持该只读方案或需要改 schema/金额写入算法时停止说明，不自动扩大范围。没有以工时估算方案。
