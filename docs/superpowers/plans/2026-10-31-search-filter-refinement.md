# 搜索组合条件与侧栏、Markdown Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans in this session. Steps use checkbox syntax. 控制器实施搜索任务；parallel-feature-development助手只负责独立Markdown文件，不逐Task派implementer。构建及所有提交由控制器串行进行。

**Goal:** 关键词与已应用筛选相交，标签进入可控制隐藏候选的二级选择页，右侧抽屉与二级页面有完整手势/切换动画，Markdown保留样式并使用jumpBrowser。

**Architecture:** 保留纯关键词旧DAO与纯筛选入口，组合路径在既有fromWhere/绑定数组上加入完整关键词谓词，分页与单SQL统计共享已冻结参数。侧栏使用现有M3DrawerState方式，内容通过带状态快照的页面key切换；Markdown沿用Markwon原生文本渲染。

**Tech Stack:** Kotlin、Room、Paging、Flow、Compose/AnimatedContent、Markwon 4.6.2及AndroidView；无新依赖/schema/CI/权限。

**Spec:** `docs/superpowers/specs/2026-10-31-search-filter-refinement-design.md`（用户明确批准更新后方案、当前会话实施、仅本地提交）。

## Global Constraints

- 分支`codex/search-filter-refinement`，基线为已合入PR550的main；不夹带#417修复，不推送/发布/合并新提交，不写Vault/Memory/pending-docs。
- 关键词原字符串/金额转换/−1哨兵/LIKE不变；转换在IO共享Deferred内，不因预算校验移回UI。
- 同组任一、不同组与关键词相交；当前账本、日期半开区间、父组动态关系、标签EXISTS去重、绑定预算与溢出处理保留。
- 清空关键词保留筛选，重置筛选保留关键词；初始无输入不查全账本。旧SavedState里的两类条件按新组合规则恢复。
- 候选隐藏默认关闭，只改显示不改已选ID；已选隐藏/不可用有摘要且可取消；标签名检索不是账单关键词。
- 子页完成只写父草稿，父完成才查询；取消/关闭丢弃草稿。右侧不改左侧，主抽屉与子页动画独立，Back先子页/弹窗。
- 二级进入向左推进，返回/确认反向，保留父草稿与滚动；快速进退不能残留交互层；减少动态效果下仍可操作。
- Markdown仅通过既有jumpBrowser保持系统分发，不加浏览器白名单/queries/权限；新调用校验网页URL并捕获打开失败，不改变全局helper。
- 方案节点1四维已完成；Task1产出后续接口，命中B，单独一次定向评审。Task2/3不逐Task加评，整批full-review一次，修文案不进复评循环。
- 已有清洁树五模块基线1068项/125XML通过。没有原生Todo工具时以commentary显示Task清单，ledger只保存恢复记录，不能宣称调用了不存在工具。

## Review Focus

1. 筛选+极端数值关键词：预算预检不得触发UI解析，查询异常可恢复且保留筛选——Task1真实VM测试。
2. 对同一条件的迟到失败/另账本旧成功：分页与统计都不能串线——Task1组合态迟到测试。
3. 已选标签后来变隐藏/删除：候选可见集合不是有效ID全集——Task2目录变更与保留/取消测试。
4. AnimatedContent退出页仍需快照，但不能响应点击/Back；主层关闭不能闪回父页——Task2暂停时钟/快速返回和关闭测试。
5. Markdown只有样式但点击链断线、或重组反复设置文本丢滚动：测试必须取真实Screen的ClickableSpan并触发——Task3实际原生View/Intent/重组测试。

## Task 1：组合查询与最小页面接线（controller）

**Files:** data的RecordFilterRepository/impl/RecordFilterSqlBuilder，domain/GetFilteredRecordViewsUseCase，testing/FakeRecordFilterRepository，records/SearchViewModel、SearchScreen、search_filter_strings.xml；对应现有SQL/repository/Room/domain/VM/screen测试。

**Produces:** 保留旧单参方法，新增 `validate(request: RecordFilterRequest, hasKeyword: Boolean): RecordFilterValidation`、`observeRecords(request: RecordFilterRequest, keyword: RecordKeywordQuery): Flow<PagingData<RecordViewsModel>>`、`observeStatistics(request: RecordFilterRequest, keyword: RecordKeywordQuery): Flow<RecordSearchStatistics>`。domain新增两参invoke返回相同实体分页Flow。builder的`build(request, keyword: RecordKeywordQuery? = null): RecordFilterQueries`共享谓词；要求账本相同。Fake新增`combinedRecordRequests`/`combinedStatisticsRequests`（Pair列表），`combinedRecordsProvider: ((RecordFilterRequest, RecordKeywordQuery) -> Flow<PagingData<RecordViewsModel>>)?`、`combinedStatisticsProvider: ((RecordFilterRequest, RecordKeywordQuery) -> Flow<RecordSearchStatistics>)?`；均默认null。

- [ ] 先在SearchFilterStateTest从真实事件构造“筛选标签后输入关键词仍保留request且只计交集”RED，记录实际断言失败，不把缺新API编译失败当此RED。
- [ ] 添加builder/仓库组合绑定、两账本拒绝、预算边界和真实Room旧集合交集断言。新API初次编译RED与行为RED分开记录。
- [ ] 关键词完整括号OR子句通过4个新增绑定接入原predicates；records/summary/statistics都来自同一fromWhere/args。预算hasKeyword预检依据固定谓词形态，执行时按实际argCount复核；用字面量临界fixture核对预检与实际查询一致。
- [ ] VM对Keyword也冻结appliedFilter request；有二者时调用新两参分页/统计，无filter时保留旧纯关键词路径。校验失败不启动金额转换，不同路径使用同一epoch/context隔离。允许Keyword打开抽屉，Apply不清关键词。
- [ ] 新fixture至少含同账本关键词且符合标签、同关键词不符合标签、符合标签不含词、另一账本同词同标签；断言实际ID集合而非只看数量。Room另加61笔跨页、多标签、隐藏、父子/净额及写入失效。
- [ ] 更新旧“筛选暂停/关键词忽略条件”的断言为新行为，保留非数字/空白/数字转换、纯keyword旧DAO、初始、筛选、返回位置、错误分离等守卫。组合前后清空、重试、恢复与切账本均有断言。
- [ ] SearchScreen最小接线：Keyword期间可打开筛选、展示真实已应用条件，不再显示暂停提示；不在本任务做新抽屉结构。截图只在最终统一录受影响基线。
- [ ] 运行model/data/domain/records unit（verify=false），完整读取XML；真实Room留Task4统一设备串行运行。无助手在写入后精确文件本地提交，再钉SHA做一次B评审；最多controller定向修复一次，不扩评审轮数。

## Task 2：标签二级选择、右侧抽屉和子页动画（controller，Consumes Task1）

**Files:** SearchFilterState.kt/SearchFilterEvent.kt、SearchViewModel.kt、SearchFilterDrawer.kt、SearchScreen.kt、新`view/SearchTagPicker.kt`、新`view/SearchFilterSidebar.kt`及records测试/strings/截图。

**Interfaces:** State新增`tagDraft: Set<Long>?`、`tagSearchQuery: String`、`showHiddenTags: Boolean`；事件OpenTags/CancelTags/ConfirmTags/ToggleTagDraft(id)/ClearTagSelection/ChangeTagSearch(keyword)/ShowHiddenTags(show)，旧ToggleTag保留兼容。`SearchTagPicker(state,onEvent,modifier)`只消费tagDraft；`SearchFilterSidebar(state,onEvent,modifier,content)`封装M3右侧容器；SearchFilterDrawer保持原公开签名。

- [ ] 先写标签默认不显示隐藏候选、显示后选中再关闭仍保留ID、二级取消/父提交层级的RED；以及实际SearchRoute右边缘滑动能打开的RED。
- [ ] 每次OpenTags从父草稿复制选择、重置候选名/显示开关；Confirm只写父草稿。候选按全量目录过滤显示，已选清单与missing判定用全量目录；名称检索不改全局关键词或业务请求数。父摘要有界，候选与已选区惰性布局。
- [ ] Sidebar只在容器使用方向适配，正文/内容保持原方向；关闭态保留可测量内容。状态与实际DrawerState同步，按钮/手势/遮罩/Back均可关闭且丢草稿；已应用过滤生效时不误丢关键词。
- [ ] 先用实际M3组件验证右侧方向/横向子列表仲裁，不能只测私有helper；方向不成立停止回报，不改用户选择或另造手势框架。
- [ ] AnimatedContent以页面类型为稳定contentKey；退出内容保留不可变显示快照，不因VM清空草稿立即消失。当前页才可派事件与暴露可操作语义，Back统一看当前目标；减少动态效果和整层关闭不叠加错乱。
- [ ] 暂停Compose时钟检查进入/返回/完成的相反位移，过程中快速进退、点击旧层与Back不应用旧数据；父滚动保留。关闭整层和重新打开不闪错子页；分类已选LazyRow/标签已选区横滑不误关，键盘/系统栏下完成仍可用。
- [ ] 运行records完整unit，确认其它页面/查询不回归；Task4统一录/验基线与真实入口。原子本地提交，不逐Task再派review。

## Task 3：Markdown样式与jumpBrowser（独立settings助手，可与Task1并行准备）

**Files owned only by Markdown worker:** `feature/settings/src/main/kotlin/cn/wj/android/cashbook/feature/settings/screen/MarkdownScreen.kt`、新同模块`view/MarkdownContent.kt`、新`view/MarkdownLinkHandler.kt`、`feature/settings/src/main/res/values/markdown_strings.xml`、对应Markdown行为/截图测试及本模块受影响PNG。禁止修改common Intent helper、core/data、records、Gradle或Manifest。

**Produces:** `@Composable internal fun MarkdownContent(markdownData: String, onLinkFailure: () -> Unit, modifier: Modifier = Modifier)`，用Markwon/原生文本及有界滚动显示。`internal fun openMarkdownLink(context: Context, url: String): Boolean`只校验http/https有效主机、调用既有jumpBrowser并捕获可恢复的启动异常；false交Screen可见反馈。Screen旧调用签名保留。

- [ ] 先从现有MarkdownScreen真实原生View树断言含链接的Spanned/ClickableSpan；旧版应因没有原生可点击Span而真实失败。Root安排独占Gradle时隙并保存RED，助手再写产品代码。
- [ ] 缓存Markwon及解析结果，source/context/theme变化才更新；原生控件映射主题/字号/行距及点击，避免无限高度或重复setText丢滚动。空态、Scaffold返回与两种资产Markdown入口不变。
- [ ] LinkResolver使用最新回调；有效URL调用jumpBrowser，拒绝其它scheme/相对/无主机且不发Intent，启动失败显示反馈。保持编码/查询/片段，不新增浏览器名单或包可见性。
- [ ] 取实际Screen的ClickableSpan点击并断言chooser内ACTION_VIEW+URI；用会抛异常的Context测试反馈与内容保留；验证标题/强调/列表/代码Span、重组同文本身份/滚动、长文和空态。
- [ ] 助手不自行跑Gradle、stage/commit；先RED授权后实现，完成即停止写入并回报精确文件。controller在稳定时隙执行settings完整unit和截图校验，独立回读再原子本地提交。

## Task 4：组合验收、变异与整批评审（controller）

- [ ] 枚举实际测试源/报告，执行主工程Online/Offline+库/JVM全套、build-logic check、DevDebug、Spotless/DependencyGuard/三渠道Lint/自定义Lint，核对真实BUILD成功与XML计数，不用缓存绿替代实际运行。
- [ ] 只录变更影响的records/settings截图类（包括新TagPicker及Markdown格式/链接），人工查看主题、手机/平板/折叠及键盘边界，再全仓verify；不录无关模块。
- [ ] 专用合成AVD显式serial/AVD名保护，运行data全部Room及真实Search+Filter+Tags+Markdown入口；记录组合交集、清空/改条件、隐藏选择、手势/子页动画/Back与实际外部链接。只操作合成数据，保留原证据。
- [ ] 独立变异副本：回退互斥模式、AND改OR/漏条件、错账本/预算漏4绑定、旧结果覆盖、显示开关清ID/以可见目录判missing、子草稿直接生效、动画/返回接线断开、Markdown回调断线/放行scheme/吞失败等；先基线绿、逐个超时/具名失败/原因与rc/XML交叉核对；不在实施树就地变异。
- [ ] 不改变源冻结后跑一次整批full-review，base/head两端SHA，保留Phase2→3用户检查点；修复finding只定向复核，不重复整批层。最后文档按实际运行状态写，本地原子提交，不推送；知识未归档照用户选择报告。

## Pre-flight与执行选择

Task1产生组合重载和context，Task2消费；Task3不共享产品文件，仅构建依赖需等Task1稳定。所有模块Gradle串行，助手返回前不提交任何文件。用户已经明确批准完整设计与当前会话执行；计划只是已批准范围的细化，不重复索取执行方式/范围确认。实现中如需新增依赖、迁移、改变右侧/隐藏条件/外部链接契约则停报；其余低风险细节以spec为准记ledger。
