# 搜索页组合筛选 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在当前账本搜索页增加不与关键字叠加的组合筛选，支持动态分类整组选取、标签任意匹配、净额收支汇总与计入额说明。

**Architecture:** 保留旧关键字 DAO 与原有账单主金额。筛选另设不可变条件模型、独立仓库及共享 SQL 谓词，列表和汇总在数据库内使用同一条件；整组选择保存父 ID 与排除 ID。页面用独立草稿和查询上下文管理模式、取消、返回恢复与加载状态。

**Tech Stack:** 项目现有 Kotlin、Compose、Hilt、Flow、Paging 3.5.0、Room 2.7.1；金额 Long 分；本地单测 JDK 21。版本依据 `gradle/libs.versions.toml`，本计划不升级依赖。

**Spec:** `docs/superpowers/specs/2026-10-29-search-filter-design.md`，2026-10-30 grilling 最终确认后的版本；必须与本文一起阅读。

## Global Constraints

- 当前账本、一段含首尾日期的区间；组内取任一、跨组取交集；隐藏标签全部可选且带标记。
- 关键字独立，空关键字恢复此前筛选；初次进入仍为未搜索状态，显式应用不限条件才能查全账本。
- 金额统一 Long 分；普通收支使用 finalAmount；转账支出贡献为 charges − concessions；平账仅参与明细和笔数。
- 净额允许随后续跨期关联报销／退款变化；不按筛选集重算吸收关系。
- 分类整组为动态父组减排除项，禁止压平为当前子类快照；类型变化只清除不兼容分类并提示。
- 查询不改 schema、不加迁移、不升级依赖、不改 CI、不改首页或其它列表默认显示；测试只使用项目已有依赖目录中的库。
- 只做独占 worktree 的原子本地提交；不合入共享 main、不推送、不创建 PR。
- 执行前建立 UI task list 与恢复 ledger；两者不能相互替代。本计划不是测试通过或实施完成记录。
- 本机 AGENTS 覆盖执行 skill 的评审状态机：默认不逐 Task 派 reviewer；仅 A（brief 含逐字实现代码）或 B（后续任务设计前提）命中时派，修订复核最多一轮且须有客观验收命令。文档措辞由 controller 自改；整批产品 full-review 只跑一次，范围两端固定 SHA。

## Review Focus

1. 先选整组再新增／移动子类，排除项仍有效：Task 1 的纯语义测试及 Task 2 的真实 Room 插入、移动测试。
2. 同标签跨账本、已选标签隐藏／删除、父组删除：Task 2 与 Task 3，限制不能静默变成不限。
3. 汇总先到／分页先到、旧请求迟到、同条件数据刷新：Task 3；只发布匹配上下文，取消不当成业务错误。
4. 跨期报销、转账优惠大于手续费、平账、关联收入主金额与计入额不同：Task 2、Task 5。
5. 大量分类与标签组合、分页初次加载大小不同、同时间戳、多标签重复命中：Task 2、Task 6；参数超限明确拒绝且不放宽条件。

## 文件与接口边界

所有路径相对仓库根。新符号是本计划要求创建的接口，不声称现有代码已经定义。已有引用用文件与符号定位，不写会漂移的行号。

| Task | 主要产物 | 后续依赖 |
| --- | --- | --- |
| 1 | 筛选条件、动态分类选择、统计贡献的纯模型 | 2–5 |
| 2 | Room 筛选 DAO、真实查询与独立仓库 | 3、6 |
| 3 | 模式、草稿、类型联动、恢复和查询状态 | 4、5 |
| 4 | 侧栏与分层分类选择组件 | 5 |
| 5 | 搜索页接线、统计卡、行级说明与截图 | 6 |
| 6 | 整体验收、变异证据、整批评审与交付记录 | 最终交付 |

### 查询实现约定

采用 `@RawQuery`、`SupportSQLiteQuery` 与 Room 生成的 PagingSource；独立筛选仓库绕开现有 RecordRepositoryImpl 的 final CombineProtoDataSource 测试构造限制，不重构旧仓库。

依据：[RawQuery 官方文档](https://developer.android.google.cn/reference/androidx/room/RawQuery) 明确接受 SupportSQLiteQuery、支持关联返回对象，并要求 Observable Queries 设置 observedEntities；[AndroidX 官方 Paging 测试变更](https://android.googlesource.com/platform/frameworks/support/+/6101de80533a09757061d6c7e258f4082b6cff31%5E1..6101de80533a09757061d6c7e258f4082b6cff31/) 包含 `loadItemsRaw(query: SupportSQLiteQuery): PagingSource<Int, PagingEntity>`。原文短引：“RawQuery return types can also be objects with Relations”。这是 API 可行性依据，当前项目的组合编译和执行仍由 Task 2 证明。

- 一个 `RecordFilterSqlBuilder` 构造共享 FROM/WHERE 与对应绑定参数，再生成有序列表查询和 COUNT/SUM 查询；动态值全部绑定，不拼名称、关键字或所选 ID。
- 账本与日期始终在顶层 AND；标签用 EXISTS 或记录 ID 子查询，避免 JOIN 放大。分类为精确 ID 或仍存在的父组自身／子项，再排除例外 ID；失效选择不剥离成不限。
- 普通类型与合成平账类型使用统一分类表达式；LEFT JOIN 不能让平账丢失。列表复用已有 LauncherRecordViewRelation 与 RecordViewsRelationMapper，不逐条 N+1 补关系。
- 列表固定 `record_time DESC, id DESC`；分页 LIMIT/OFFSET 由 Room PagingSource 管理，不改旧关键字的页码规则。
- 两条 SQL 分别计算实际参数数量，并为 Room 分页附加的 LIMIT/OFFSET 保留 2 个参数。数据层按现有 SQL_IN_CHUNK_SIZE=900 的保守项目预算校验，不宣称它是设备硬上限。整组不枚举所有子 ID；超预算明确拒绝应用，不截断条件、不分块局部分页后拼接。
- RawQuery 的 observedEntities 覆盖 RecordTable、TypeTable、TagTable、TagWithRecordTable 及列表关系涉及的 AssetTable、ImageWithRelatedTable、RecordWithRelatedTable。摘要至少观察影响其匹配及金额的前四张表。

### 开始执行前的门禁

- 在独占 worktree 核对分支、干净状态、`CLAUDE.md` 与本计划；确认 JDK 21、SDK 和测试依赖实际可用，不根据本次只读探测就宣称能构建。
- 建立六项 UI task list 和恢复 ledger；锁定实施基线 SHA。执行与最终相同范围的完整单测基线命令（见 Task 6 Step 2），保留失败集合；基线失败不能直接归因为本功能，也不能顺手修无关模块。
- 所有长 Gradle 调用保留可见输出或持续可读日志，每次以真实 BUILD SUCCESSFUL/FAILED、目标任务状态及测试报告判定；按本机规则监控，不能用日志暂时静默认定挂死。
- 本文任务步骤在测试、实现、提交均未执行前保持未勾选；本轮文档检查不能代填产品验收。

## Task 1：筛选语义与统计贡献模型

**Files — Create:**
- `core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/model/RecordFilter.kt`
- `core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/model/RecordFilterSummary.kt`
- `core/model/src/test/kotlin/cn/wj/android/cashbook/core/model/model/RecordFilterTest.kt`
- `core/model/src/test/kotlin/cn/wj/android/cashbook/core/model/model/RecordFilterSummaryTest.kt`

**Interfaces — Produces:**
- `CategoryFilterSelection(exactTypeIds: Set<Long>, wholeGroupIds: Set<Long>, excludedTypeIds: Set<Long>)`，空集合默认；`matches(typeId: Long, parentId: Long): Boolean` 仅判选择表达式，不把缺失数据库分类视为有效。
- `RecordFilter(dateSelection: DateSelectionEntity = All, typeCategories: Set<RecordTypeCategoryEnum>, categories: CategoryFilterSelection, tagIds: Set<Long>)`，集合默认空表示不限。
- `RecordFilterRequest(bookId: Long, fromInclusive: Long, toExclusive: Long, typeCategories: Set<RecordTypeCategoryEnum>, categories: CategoryFilterSelection, tagIds: Set<Long>)`；`RecordFilter.toRequest(bookId: Long, monthStartDay: Int): RecordFilterRequest` 统一日期转换。
- `selectCategoryGroup(selection: CategoryFilterSelection, parentId: Long, selected: Boolean, catalog: List<RecordTypeModel>): CategoryFilterSelection`。
- `selectCategoryItem(selection: CategoryFilterSelection, type: RecordTypeModel, selected: Boolean): CategoryFilterSelection`。
- `RecordFilterTypeUpdate(filter: RecordFilter, removedTypeIds: Set<Long>)`；`changeFilterTypes(filter: RecordFilter, allowedTypes: Set<RecordTypeCategoryEnum>, catalog: List<RecordTypeModel>): RecordFilterTypeUpdate`，未知失效 ID 不静默删除；空 allowedTypes 表示全部，不清除兼容项。
- `RecordFilterSummary(totalCount: Long, income: Long, expenditure: Long)`，派生 `balance: Long`。
- `RecordFilterContribution(income: Long, expenditure: Long)`；`recordFilterContribution(record: RecordViewsEntity): RecordFilterContribution`；`recordFilterDisplayAmount(record: RecordViewsEntity): Long` 与现有 RecordListItem 的主金额规则一致；`shouldShowFilterContribution(record: RecordViewsEntity): Boolean`。

- [ ] **Step 1 — 写红测。** `wholeGroup_keepsFutureChildAndExclusions` 固定以下断言；另测父项直接记账、精确选择跨组、显式全组取消、空类型为全部、类型联动移除及失效 ID 不退化。

```kotlin
val selection = CategoryFilterSelection(wholeGroupIds = setOf(10L), excludedTypeIds = setOf(11L))
assertThat(selection.matches(typeId = 12L, parentId = 10L)).isTrue()
assertThat(selection.matches(typeId = 11L, parentId = 10L)).isFalse()
assertThat(selection.matches(typeId = 13L, parentId = 20L)).isFalse()
```

- [ ] **Step 2 — 红测记录。** `./gradlew :core:model:test --tests '*RecordFilter*Test' --console=plain`；新增符号缺失先表现为编译红，补接口后必须看到目标断言红，不把编译失败当守卫判别力。
- [ ] **Step 3 — 最小实现。** 保留父组与排除集合；不展平未来成员。贡献测试固定 Long 分：转账 amount=500000、charge=200、优惠=0 → 支出=200；优惠=300 → 支出=-100；完全对冲关联收入 amount=80000/finalAmount=0 → 主金额=80000、收入=0；平账贡献为零。
- [ ] **Step 4 — 绿测。** 同 Step 2，并运行 `./gradlew :core:model:test --console=plain`；检查实际测试用例数与报告，无新增恒 skip。
- [ ] **Step 5 — 原子提交。** 仅本 Task 的四个文件，提交说明围绕动态筛选模型与金额贡献。

**逐 Task 评审判据：B。** 该接口是后续 SQL、状态与 UI 的设计前提；仅 SDD 执行时派该 Task reviewer，Native 不人为添加实施子代理。

## Task 2：真实数据库组合查询与独立仓库

**Files — Create:**
- `core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/dao/RecordFilterDao.kt`
- `core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/relation/RecordFilterSummaryRelation.kt`
- `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/RecordFilterRepository.kt`
- `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordFilterSqlBuilder.kt`
- `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordFilterRepositoryImpl.kt`
- `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordFilterSqlBuilderTest.kt`
- `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordFilterRepositoryTest.kt`
- `core/data/src/androidTest/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordFilterRepositoryRoomTest.kt`

**Files — Modify:** `core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/CashbookDatabase.kt`；`core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/di/DatabaseModule.kt`；`core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/di/DataModule.kt`；`core/data/build.gradle.kts` 仅按既有 core/database 模板配置 androidTest runner 及已有 catalog 的测试依赖。

**Interfaces — Consumes:** Task 1 的 RecordFilterRequest、RecordFilterSummary；已有 LauncherRecordViewRelation、`toRecordViewsModel()`。

**Interfaces — Produces:**
- DAO：`pagingRecords(query: SupportSQLiteQuery): PagingSource<Int, LauncherRecordViewRelation>`（Transaction）；`observeSummary(query: SupportSQLiteQuery): Flow<RecordFilterSummaryRelation>`。
- `RecordFilterSummaryRelation(totalCount: Long, income: Long, expenditure: Long)` 对应查询别名。
- `RecordFilterRepository.validate(request: RecordFilterRequest): RecordFilterValidation`，在同一新仓库接口文件定义 sealed interface RecordFilterValidation：Valid、TooManyParameters(required: Int, limit: Int)、InvalidRange。
- `RecordFilterRepository.observeRecords(request: RecordFilterRequest): Flow<PagingData<RecordViewsModel>>`；`observeSummary(request: RecordFilterRequest): Flow<RecordFilterSummary>`。
- `RecordFilterSqlBuilder.build(request: RecordFilterRequest): RecordFilterQueries`；RecordFilterQueries 定义在同一 builder 文件，携带 `records: SupportSQLiteQuery`、`summary: SupportSQLiteQuery` 与包含分页保留位的 `requiredBindCount: Int`；预算拒绝在调用 DAO 前完成。

- [ ] **Step 1 — 写红测。** Builder 用真实生成的参数和 SQL 测约束及预算；Room 测试从真实 repository 入口查询，不手写另一份“等价 SQL”。数据明确含两个账本、两标签重复关联、父子类型、合成平账、同时间戳及 61 笔以上记录。
- [ ] **Step 2 — 首先编译组合。** `./gradlew :core:database:compileDebugKotlin :core:data:compileDebugKotlin :core:data:compileDebugAndroidTestKotlin --console=plain`，确认当前 Room 2.7.1 生成 RawQuery + PagingSource + Relation 代码。失败先回源诊断，不能为了绿灯退回内存分页或改依赖版本。
- [ ] **Step 3 — 实现共享查询与仓库。** 按“查询实现约定”；在现有 DatabaseModule 增加 DAO provider、DataModule 增加筛选仓库绑定。数据库版本仍为既有值，schema 与 migrations 不变。仓库构造仅依赖 DAO 和现有 dispatcher，单测可实例化真实 Impl，不复制现有 final 数据源限制下的手工镜像测试。
- [ ] **Step 4 — 跑查询红绿与真实 Room。** 本地 `./gradlew :core:data:testDebugUnitTest --tests '*RecordFilter*Test' --console=plain`；设备可用后 `./gradlew :core:data:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=cn.wj.android.cashbook.core.data.repository.impl.RecordFilterRepositoryRoomTest --console=plain`。实际门禁为 BUILD SUCCESSFUL 与目标用例执行，不以“已编译”替代 Room 行为。
- [ ] **Step 5 — 补齐承重断言。** `tagsAny_deduplicatesAcrossPages`、`wholeGroup_newChildAndMovedChild_followCurrentTree`、`deletedSelections_neverBecomeUnrestricted`、`syntheticBalance_countedButNotSummed`、`aggregate_coversAllPages`、`sameTimestamp_stableOrder`；分类与标签组合跨 900 预算、保留 2 个分页参数，超限 DAO 调用次数为零。`outsideRangeReimbursement_updatesHistoricNet` 必须通过真实 TransactionDao 保存 10 月支出 100000 分、11 月关联报销 80000 分，再查询 10 月验证支出 20000 分，不直接手填理想 finalAmount。普通收入／支出 sum finalAmount、转账 sum charge-concessions；COALESCE 保证空集为零。
- [ ] **Step 6 — 原子提交。** 明确列出本 Task 所有文件；不带 schema、版本文件或现有关键字 DAO 改动。

**逐 Task 评审判据：B。** 该仓库与查询语义是后续状态与页面的前提。

## Task 3：查询模式、草稿、类型联动与状态恢复

**Files — Create:**
- `core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/GetFilteredRecordViewsUseCase.kt`
- `core/domain/src/test/kotlin/cn/wj/android/cashbook/domain/usecase/GetFilteredRecordViewsUseCaseTest.kt`
- `core/testing/src/main/kotlin/cn/wj/android/cashbook/core/testing/repository/FakeRecordFilterRepository.kt`
- `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/model/SearchFilterState.kt`
- `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/model/SearchFilterEvent.kt`
- `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/SearchFilterStateCodec.kt`
- `feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/SearchFilterStateTest.kt`

**Files — Modify:** `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/SearchViewModel.kt`；`feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/SearchViewModelTest.kt`。

**Interfaces — Consumes:** Task 1 条件与联动函数、Task 2 筛选仓库；已有 BooksRepository.currentBook、TypeRepository、TagRepository、SettingRepository、各数据版本信号。

**Interfaces — Produces:**
- `GetFilteredRecordViewsUseCase.invoke(request: RecordFilterRequest): Flow<PagingData<RecordViewsEntity>>`，复用已有 `RecordViewsModel.asEntity()`。
- `SearchMode` = Initial / Keyword / Filter；`SearchFilterState` 在同名文件定义，默认构造为 Initial：保存 `queryDraft: String`、`submittedKeyword: String`、`appliedFilter: RecordFilter?`、`drawerDraft: RecordFilter?`、`categoryDraft: CategoryFilterSelection?`，以及 `catalog: List<RecordTypeModel>`、`tags: List<TagModel>`、`categoryBrowseType: RecordTypeCategoryEnum`、`categoryBrowseParentId: Long?`、`categorySearchQuery: String` 与可访问提示；三层草稿不能共享可变集合，完整目录数据不写入 SavedStateHandle。
- 同文件定义 `SearchFilterSummaryState`：Hidden、Loading(context: SearchQueryContext)、Ready(context: SearchQueryContext, summary: RecordFilterSummary)、Failed(context: SearchQueryContext, message: String)。
- `SearchQueryContext(criteriaRevision: Long, bookId: Long, mode: SearchMode, request: RecordFilterRequest?, keyword: String)`；数据刷新 revision 与 criteriaRevision 分开，避免编辑回页被无条件滚到顶部。
- ViewModel：`filterState: StateFlow<SearchFilterState>`、`filterSummary: StateFlow<SearchFilterSummaryState>`、保持现有 `recordListData` 对外类型；`onFilterEvent(event: SearchFilterEvent): Unit`。
- Event 必须覆盖 Open/Cancel/Reset/ApplyDrawer、UpdateDate、UpdateTypes、ToggleTag、Open/Cancel/ConfirmCategories、ToggleCategoryGroup、ToggleCategoryItem、ChangeCategoryBrowser、ChangeCategorySearch、RemoveCategory、UpdateQueryDraft、ClearSubmittedKeyword、Retry。保留 `onKeywordChange` 作为提交关键字入口；历史点击发送 UpdateQueryDraft，仍不执行搜索。
- `SearchFilterStateCodec` 只向 SavedStateHandle 写 Bundle 支持的字符串、数字、LongArray／IntArray 等原生值；不把普通 Kotlin data class 直接作为可保存对象、不增加序列化依赖或 DataStore 字段。

- [ ] **Step 1 — 写红测。** `initial_doesNotQueryAll`、`typing_doesNotSubmit`、`keyword_ignoresPreviouslyAppliedFilter`、`clearKeyword_restoresFilter`、`cancelDraft_doesNotChangeResults`、`resetThenApply_unrestrictedIsExplicit`、`nestedCategoryCancel_keepsDrawerDraft`；对仓库调用参数及次数作行为断言。
- [ ] **Step 2 — 编译并确认红。** `./gradlew :core:domain:testDebugUnitTest --tests '*GetFilteredRecordViewsUseCaseTest' :feature:records:testDebugUnitTest --tests '*Search*Test' --console=plain`；每个模块仅用匹配本模块的精确任务，禁止无匹配模块假红。
- [ ] **Step 3 — 实现状态与流接线。** Keyword 完整沿用旧 Pager/匹配/历史规则；Filter 调用新 use case 和 summary；Initial 不发全量请求。应用前一次性 validate，失败保持原 criteria 与结果。新条件或账本取消旧流、校验上下文；汇总与页面就绪状态按同一 criteria 合并，旧条件金额不能短暂显示为新条件金额。
- [ ] **Step 4 — 保存与刷新。** 返回同一 back stack entry 保留 appliedFilter 与位置；真正退出后重建为 Initial。分别处理 book、record、type、tag 信号；全局标签不因切账本清空，隐藏不是删除。动态整组在分类变化后仍按父组与排除项重新查询。取消异常必须继续传播。
- [ ] **Step 5 — 绿测与竞态。** 补 `incomeSelection_prunedWhenTypesBecomeExpenditure`、`allTypes_doesNotRemoveCompatibleSelection`、`hiddenTags_remainSelectableAndApplied`、`bookSwitch_preservesFilterButScopesRecords`、`oldSummaryOrPage_cannotOverwriteNewCriteria`、`independentTypeVersion_refreshesTreeAndQuery`、`codec_roundTripKeepsAppliedMode`。用可控 Deferred/Flow 造两个返回次序及取消，不用真实 sleep；记录与摘要错误均能重试，不能伪装零。
- [ ] **Step 6 — 原子提交。** 同步所有新构造参数调用；FakeRecordFilterRepository 使用与 SQL 相同的账本、日期、层级和标签语义，真实 SQL 正确性仍以 Task 2 为准。

**逐 Task 评审判据：B。** UI 消费此状态和事件契约；SDD 下该边界须在后续任务前核验。

## Task 4：筛选侧栏与分层分类选择

**Files — Create:**
- `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/view/SearchFilterDrawer.kt`
- `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/view/SearchCategoryPicker.kt`
- `feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/view/SearchFilterDrawerTest.kt`
- `feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/view/SearchCategoryPickerTest.kt`
- `feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/view/SearchFilterDrawerScreenshotTests.kt`

**Files — Modify:** `core/ui/src/main/res/values/strings.xml` 增加用户可见文案，不写技术实现提示。

**Interfaces — Consumes:** SearchFilterState、SearchFilterEvent 及真实分类树、全量 TagModel 列表。

**Interfaces — Produces:** `SearchFilterDrawer(state: SearchFilterState, onEvent: (SearchFilterEvent) -> Unit, modifier: Modifier)`；`SearchCategoryPicker(state: SearchFilterState, onEvent: (SearchFilterEvent) -> Unit, modifier: Modifier)`。事件作为单入口传递，不新增十几个跨层独立回调。

- [ ] **Step 1 — 写交互红测。** 使用真实组件测试树：父组全选／排除／部分选中、只选父项、跨允许类型浏览保留、分类名搜索保留选择、底部移除、已隐藏标记、不可用选项标记、分类页返回与侧栏遮罩取消分别发对应事件。
- [ ] **Step 2 — 红测运行。** `./gradlew :feature:records:testDebugUnitTest --tests '*SearchFilterDrawerTest' --tests '*SearchCategoryPickerTest' --console=plain`，确保断言命中实际显示节点与事件，不能仅检查文本存在。
- [ ] **Step 3 — 实现。** 顶部类型页签由允许范围控制；父级导航与子项区域各自可滚动，已选区与完成按钮不被长列表挤出。使用设计系统组件及现有日期转换，支持配置的月周期；从真实日期选择入口点击起止日后断言草稿日期，不能只直接发送理想 UpdateDate 事件而漏掉日期控件的时区／初值转换。父组摘要明确“整组／排除 N 项”，不把动态组伪装成固定 N 个 ID。
- [ ] **Step 4 — 绿测与截图。** 模块测试通过后，本地截图覆盖空、较多分类、长名称、隐藏标签、部分整组与联动提示，采用项目 captureMultiTheme/captureMultiDevice；检查手机、平板、折叠屏可见性及触控目标。基线在 Task 5 接入后统一正式录制。
- [ ] **Step 5 — 原子提交。** 只提交本组件、其测试及对应文案。

**逐 Task 评审判据：B。** Task 5 使用本 Task 的组件接口和取消事件语义；仅在 SDD 执行时做该边界的独立核验，不能借接口偏离重开反复措辞评审。

## Task 5：搜索入口接线、收支卡与行级计入额

**Files — Create:**
- `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/view/SearchFilterSummary.kt`
- `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/view/SearchFilterRecordItem.kt`
- `feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/screen/SearchFilterScreenTest.kt`

**Files — Modify:** `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/screen/SearchScreen.kt`；`feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/screen/SearchScreenScreenshotTests.kt`；`core/ui/src/main/res/values/strings.xml`；`feature/records/src/test/screenshots/` 中实际受影响及新增基线。

**Interfaces — Consumes:** Tasks 1–4；现有 RecordListItem 与详情／编辑回调。

**Interfaces — Produces:** `SearchFilterSummary(summary: RecordFilterSummary, modifier: Modifier)`；`SearchFilterRecordItem(record: RecordViewsEntity, onClick: () -> Unit, modifier: Modifier)` 内部复用 RecordListItem，附注只存在于该筛选包装，不改变共享 RecordListItem 默认行为。SearchRoute 采集状态并向 SearchScreen 增加必需参数 `filterState: SearchFilterState`、`summaryState: SearchFilterSummaryState`、`onFilterEvent: (SearchFilterEvent) -> Unit`；旧详情、导航与关键字提交参数保持，所有截图调用同步提供明确状态与事件接收器。

- [ ] **Step 1 — 写真实接线红测。** 从搜索栏筛选按钮打开侧栏、选择标签及分类、完成后核对条件摘要、记录 ID 和汇总；有关键字时筛选不参与，清空后恢复；详情／编辑返回保持条件与位置；真正退出搜索入口后重进为空初始态。
- [ ] **Step 2 — 运行红测。** `./gradlew :feature:records:testDebugUnitTest --tests '*SearchFilterScreenTest' --console=plain`，不能仅直接渲染孤立 Summary 替代入口测试。
- [ ] **Step 3 — 接线。** 使用 rememberSaveable 的 LazyListState 及明确条件键；切换查询条件回顶部，编辑造成的数据刷新不无条件重置位置。加载、空、错误分别展示；摘要覆盖全部结果，不读 LazyPagingItems 当前加载页相加。
- [ ] **Step 4 — 金额说明。** 断言转账主金额 ¥5,000.00／统计支出 ¥2.00；完全对冲的报销收入主金额 ¥800.00／统计收入 ¥0.00；正常净额相同记录不重复加注。顶部注明净额、转账仅计费用优惠、平账不计收支。Keyword 及其它列表不出现本次新附注。
- [ ] **Step 5 — 回归与本地基线。** 先跑 Search* 测试及旧 GetSearchRecordViewsUseCaseTest，再 `./gradlew :feature:records:recordRoborazziDebug --console=plain`。目检实际变化、新增状态并检查 git diff，只接受本次 UI 影响的基线；然后 `./gradlew :feature:records:verifyRoborazziDebug --console=plain`。不得靠大面积重录掩盖其它屏变化，不依赖 CI 自动重录。
- [ ] **Step 6 — 原子提交。** 产品接线与对应截图一同提交，主界面、旧搜索和其它共享列表的回归结果写入 Task 6 记录。

## Task 6：整体验收、变异与整批交付

**Files — Create:** `docs/testing/reports/2026-10-30-search-filter-validation.md`；需要自动化变异时新建 `tools/testing/verify_search_filter_mutations.py`，仅作用任务独占副本、跨平台、子进程硬超时，严禁就地破坏源文件。

**Interfaces — Consumes:** Tasks 1–5 的实际实现与测试入口；不创建另一套手写查询充当预言机。

- [ ] **Step 1 — 前置实证。** 在实际 worktree 核对 git status、JDK 21、SDK、设备；按需读取本机存在的长构建／测试规则。目录有未提交无关改动先处理归属；不复制主检出 CLAUDE.md 的用户改动。
- [ ] **Step 2 — 构建与完整单测。** 保存命令和完整日志，运行以下独立命令；实际目标没有执行、只缓存命中或环境失败必须记录，不宣称已通过。

```bash
./gradlew :app:assembleDevDebug --rerun-tasks --console=plain
./gradlew testOnlineDebug testOfflineDebug testDebugUnitTest :core:model:test :lint:test -Proborazzi.test.verify=false --rerun-tasks --console=plain
./gradlew spotlessCheck --init-script gradle/init.gradle.kts --no-configuration-cache --console=plain
./gradlew :app:lintOnlineRelease :app:lintOfflineRelease :app:lintDevRelease :lint:lint -Dlint.baselines.continue=true --console=plain
./gradlew verifyRoborazziDebug --console=plain
```

- [ ] **Step 3 — 真数据库和入口回放。** 跑 Task 2 的完整 Room 测试类；并回放当前账本、跨账本重名标签、日期首尾、动态子分类插入、组减例外、类型清理提示、隐藏标签、跨期报销、转账优惠大于手续费、61 笔以上分页及错误重试。安装测试产物前核对目标设备与包，避免覆盖用户有价值数据；用模拟器或任务数据集，不直接清用户数据。
- [ ] **Step 4 — 有界变异。** 基线必须实际执行目标用例。逐个独立改变账本 AND、标签 EXISTS 去重、日期 `< end`、父组动态匹配、排除项、全量汇总、转账费用、Keyword 独立分支、版本接线；顺序约束增加移位型变异。每次核对失败用例与 AssertionError 原因，编译错误不算杀死；还原后再跑基线并比较源文件 hash 不变。
- [ ] **Step 5 — 整批评审。** 所有实现与本地链路验证通过后，将基线和最终提交都解析成 SHA，只发起一次 full-review；按其流程处理唯一 Phase 2→3 checkpoint。Critical/High 修复后 controller 定向复验，不把文档措辞送回循环；未放行阻塞项不能宣告交付。
- [ ] **Step 6 — 交付记录与知识。** 记录真实测试命令、显式文件集合、结果、未执行项及原因，逐项报告构建、守卫、变异、提交、知识归档五项状态。`/summarize-session` 只有实际落到 vault 才算归档，pending-docs 不能代替。验证记录单独本地提交，随后停等用户对合入／推送的明确指令。

## 自审与执行交接

- 规格覆盖映射：数据范围／日期／统计 → Tasks 1–2；模式／取消／恢复／联动 → Task 3；分类布局／隐藏标签 → Task 4；入口／附注／截图 → Task 5；完整链路／变异／回归与交付 → Task 6。
- Review Focus 五项均已放入相应 Task 的具名测试；后续新增边界必须归入所属 Task，不另开无边界评审层。
- 新接口统一使用 `RecordFilterRequest`、`CategoryFilterSelection`、`RecordFilterSummary`；DAO Relation 只在 database/data 内部，页面消费实体与界面状态，不把数据库表透传 UI。
- 本文仅创建计划，复查过现有源路径和依赖版本；RawQuery 的当前项目编译、SQL 行为、设备与性能仍是实施门禁，未提前记为成功。
- 执行方式尚未选择。Native 的优点是六个任务的接口和上下文可在本会话连续保持，缺点是实施阶段没有逐 Task 独立视角；SDD 的优点是基础接口可按 B 判据独立验收，缺点是需给每个实现／评审代理重复装载上下文。两者最终都须整批 full-review。
- 本计划倾向 Native，理由：Tasks 1→2→3→4→5 的依赖紧密，且最终已有整批评审门禁；这不是用户已经作出的选择。交接时请用户评审计划并选择 Native 或 SDD，选择前不开始产品代码。
