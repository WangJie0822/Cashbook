# 搜索组合条件、二级标签选择与Markdown链接设计

状态：待用户批准完整描述，尚未修改产品代码。旧搜索统计6笔已通过[PR #550](https://github.com/WangJie0822/Cashbook/pull/550)合入main，本设计是后续独立范围，替代旧版“关键词与筛选互斥”的约定，不回写历史验收结论。

## 意图与已确认边界

- 用户要求关键词在筛选结果内继续搜索；不输入关键词时仅按筛选，未应用筛选时继续普通关键词搜索。
- 标签入口像分类一样收在二级选择页，支持多选，避免主侧栏列出大量标签。
- 默认隐藏候选中的已隐藏标签，勾选“显示隐藏标签”后列出并标记。用户明确：关闭显示开关不取消已选条件，只改变候选可见性。
- 筛选侧栏借鉴首页的抽屉机制，用户明确保持从右侧滑入。
- Markdown保留样式，链接可点击，交由外部浏览器打开。
- 用户选择先合入旧6笔、再做本优化；现在旧PR已合入。新范围默认当前会话实施、仅本地提交，不含新推送、release或知识归档授权。

## 现状实证

- `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/SearchViewModel.kt` 的 `onFilterEvent(OpenDrawer)` 在Keyword模式拒绝打开；`startQuery` 的关键词统计仅调用 `observeKeywordStatistics(keywordQuery)`。`SearchScreen` 也禁用关键词期间的筛选按钮。这些需要按新规则改变。
- `SearchFilterDrawer#SearchFilterDrawer` 当前直接列出全部 `state.tags`，只在行上标记invisible；`SearchCategoryPicker#SearchCategoryPicker` 已有独立页、名称检索、候选懒加载、已选清单与取消/完成语义。
- `feature/settings/src/main/kotlin/cn/wj/android/cashbook/feature/settings/screen/LauncherScreen.kt` 的 `LauncherScreen` 已使用 `ModalNavigationDrawer`、`ModalDrawerSheet`、`DrawerState`、协程开关及BackHandler；搜索页当前为右侧Box与自制遮罩。
- `MarkdownScreen#MarkdownScreen` 将 `Markwon.toMarkdown()` 的结果append到Compose AnnotatedString，没有链接注解或打开浏览器事件；`SettingRepositoryImpl#getContentByMarkdownType` 读取打包的CHANGELOG/PRIVACY_POLICY。`core/common/src/main/kotlin/cn/wj/android/cashbook/core/common/tools/Intent.kt#jumpBrowser` 已提供ACTION_VIEW选择器入口。
- 新工作区基线五模块（model/data/domain/records/settings）已实际BUILD SUCCESSFUL，125 XML / 1068项、失败/错误/跳过均0；仅为旧代码基线，不是本设计验收。

## 关键词与筛选共同生效

结果集合为当前账本内：时间、收支类型、分类、标签条件相交，再与原关键词备注LIKE/金额匹配集合相交。同一组多选继续为任一匹配，不改分类父组/排除项和标签EXISTS去重。

- 原关键词金额解析、舍入、−1哨兵、空白处理、LIKE通配符含义保留，不trim业务关键词；不更改现有触发/历史记录节奏。
- 输入关键词时筛选按钮仍可用；应用筛选不清空关键词。清空关键词保留已应用筛选，重置筛选不清空关键词。首次无输入且未应用筛选仍不自动查询全账本；明确应用“不限”条件的既有行为保留。
- 所有列表、笔数、收支及分类图都对应同一组组合条件；不得在已加载分页结果上本地过滤，分页外匹配也参与全量统计。
- 冻结上下文同时包含账本、原关键词/金额参数及已解析日期/ID条件；同一次查询由分页和聚合共享。金额转换继续在注入IO内只做一次，失败进入原有错误边界，不能回退到UI事件同步换算。
- 参数预算纳入关键词实际附加绑定；日期/条件无效或超预算时不应用草稿，不静默丢条件。预检查不能为了预算同步解析极端金额，执行入口仍复核实际argCount。
- 旧结果、旧失败及不可协作的迟到任务不能覆盖新组合；切账本、改日期、修改分类/标签/账单及重试都覆盖。仅统计失败时保留同一查询仍可用的列表，不能显示旧查询金额或伪装成0。
- 净额/饼图保持已验收口径：收入/支出finalAmount，转账charge−concessions；分类先有符号加总后abs，金额符号与零组保留，平账仅计数。

### 数据实现路径

候选A：保留现有纯关键词和纯筛选入口，新增组合查询。优点：普通关键词继续走已对照的旧DAO分页；组合条件复用筛选SQL谓词和绑定数组。缺点：查询上下文多一种组合分支，必须单独覆盖取消、刷新、预算与错误隔离。

候选B：把全部搜索改为统一RawQuery/Pager。优点：所有输入共享一套入口；缺点：普通关键词的分页实现也改变，需要额外核对现有排序/刷新/匹配契约，不能只验证组合条件。依据是当前两条入口分别为 `RecordRepositoryImpl#queryPagingRecordListByKeyword` 与 `RecordFilterRepositoryImpl#observeRecords`，并非两条同形实现。

本设计以A拟定，理由是本次新增组合语义，纯关键词已有真实旧DAO对照，无须同时迁移其分页实现。组合路径扩展 `RecordFilterSqlBuilder` 的WHERE/参数构建，输出分页和单查询聚合；不复制一份独立统计筛选条件。旧公开入口保留，扩展方式/内部类型名由实施计划固定并检查全部调用方。相同记录时间的组合结果沿用筛选列表现有ID次序；纯关键词旧排序不变。

## 二级标签选择

- 主侧栏只显示“标签”入口及有界摘要：已选数量、最多前两个名称、隐藏数量/省略提示；不把整份标签列表或无限名称重新展开到主侧栏。
- 进入二级页复制父草稿中的标签ID；包含名称检索、候选懒加载、多选、已选摘要/清单、清空选择、取消与完成。标签仍是单层数据，不新增标签层级或父子关系。
- “显示隐藏标签”进入选择页时默认关闭；当前页配置变更可恢复，关闭后再次进入按默认关闭，不新增全局偏好。它不进入业务查询上下文，也不触发账单查询。
- 已选隐藏标签在已选清单/摘要中保持可见并标记，可直接取消；候选列表隐藏时不清空ID、不改变匹配结果。比较“已删除/不可用”必须对照全量目录，不能把未显示的隐藏候选误判为丢失。
- 候选名搜索只影响候选，不影响业务关键词或已选ID；清空选择表示不限标签，不新增“全选所有ID”导致超预算。大量候选与已选清单按可见范围组合，主侧栏摘要不随总数失控。
- 二级“完成”仅更新父草稿，父侧栏“完成”才应用条件；二级取消恢复父草稿，整层取消/手势关闭放弃未应用修改。日期/类型/分类草稿仍按既有规则工作。

## 右侧抽屉

复用首页已有ModalNavigationDrawer/DrawerState控制方式，增加局部方向适配保持右侧。AndroidX官方源码的抽屉手势显式依据 `LocalLayoutDirection` 设置 `reverseDirection`；该源码支持拟定方向适配，不是本项目右侧手势已实测的声明。

- 只在抽屉容器隔离方向，内容与正文保留应用原方向；不把中文文本、图标或全页切为RTL。抽屉关闭时仍须提供可测量的内容与锚点，不能因草稿为空让边缘手势无内容可打开。
- 按钮、边缘手势、拖动关闭、点击遮罩、系统返回与无障碍关闭统一同步实际DrawerState和VM意图；避免false→false不触发LaunchedEffect造成关不掉。
- 返回顺序：日期弹窗/二级选择页先返回上层，再关闭主侧栏，最后才离开搜索页。关闭整层不误应用草稿。
- 分类页已有横向已选LazyRow，新增标签已选区也不能被外层抽屉吞掉横向操作；纵向候选滚动不关闭侧栏。手机/平板/折叠屏、键盘与系统栏下的完成/取消均可操作。
- 实施须先用实际组件的行为用例验证局部方向与手势隔离；若适配不成立，停止说明，不擅自改成左侧或替换成新手写手势系统。

## Markdown与外部链接

保留已引入的Markwon，不新建WebView或新解析库。拟通过AndroidView中的原生文本控件承载Markwon Spanned，保留标题、段落、强调、列表、引用及代码样式；使用项目主题文本/链接颜色、字号缩放和长内容滚动。

- [Markwon官方入门](https://noties.io/Markwon/docs/v4/core/getting-started.html)列出 `setMarkdown(TextView,...)` / `setParsedMarkdown`；[配置文档](https://noties.io/Markwon/docs/v4/core/configuration.html)明确LinkResolver及默认LinkMovementMethod。此为所用路径的API依据，不是本项目新交互已完成。
- 缓存解析器及处理解析/重组更新，不能每次重组创建Markwon；上下文和主题变化仍正确更新显示，回调读取最新状态。
- 标准绝对网页链接（http/https且有效主机）通过自定义LinkResolver调用项目已有 `jumpBrowser`，保持系统选择器/关联处理器的现有分发语义；不改变全局helper的其他调用。
- 方案评审指出通用ACTION_VIEW不能保证只去浏览器；用户随后明确“不用，使用jumpBrowser就行”，因此不实现只限浏览器处理器的适配层，不新增包查询、manifest queries或权限。系统可选择浏览器或已关联应用，不把这种现有分发宣称为仅浏览器保证。
- 无可用处理器或启动失败给简短反馈，不闪退、不内嵌WebView、不改系统默认浏览器、不增加逐链接确认。
- 相对地址、空/无主机地址或其他scheme不猜测来源、不启动任意Intent；拒绝路径不得降级成无过滤的全局helper。保留有效URL的大小写/编码/查询与片段语义。
- [Android官方URL分发说明](https://developer.android.com/training/package-visibility/use-cases#open-urls)作为现有helper行为边界的依据；实际链接点击接线、URL与错误反馈仍需实施验收。
- 版本更新和隐私协议两入口一致；空内容/返回行为保留。不扩大到远程图片、表格/公式/代码高亮插件或内嵌页面导航。
- 原生文本互操作是本设计拟定路径；另一候选是逐类Span转换为Compose样式/链接注解，优点是保留纯Compose文本节点，缺点是要明确映射Markwon生成的各类Span。这里不把未做PoC的转换覆盖或维护成本当既定事实；本设计选择已有官方TextView路径，避免新增转换协议。

## 影响面、排除项与可回退性

涉及records搜索VM/状态/持久化恢复/SQL及测试替身、records的筛选/标签页与截图、settings Markdown界面与测试。旧公共关键词入口、全局浏览器helper、查询参数预算及日期边界保持兼容；实际扩展的全部调用处须编译与回归。

不改schema、金额写入/退款报销算法、首页菜单本身、Analytics/Budget口径、分类编辑#417修复、CI/依赖/版本号。旧SavedState里的关键词和筛选按新组合规则恢复；标签显示偏好不丢筛选条件。独立分支原子本地提交，保留主工作区未提交改动；不推送、不发布、不写Vault/Memory/pending-docs，除非用户后续更改授权。

## 验收要求（待实施，不是通过声明）

1. 真实Room用同一夹具对照旧关键词DAO集合与旧筛选集合的交集，并独立核对金额/分类/笔数；超过一页、两账本、时间边界、多类型/同名/父子/多标签/隐藏标签、无匹配、正负零与溢出均有实际断言。
2. VM覆盖关键词后加/改筛选、筛选后输入/清空关键词、无筛选旧入口、清空筛选保留关键词、恢复/切账本/刷新、极端金额IO失败及恢复、不可协作迟到成功/失败，不能把新旧条件拼错。
3. UI覆盖二级取消/完成/重置、隐藏默认关闭与已选保留/取消、候选搜索不触发业务查询、目录变化/长名称/大量项、右侧拖动与返回层级、分类和已选横向滚动不误关闭；保留统计错误不遮列表及图表交互不改查询守卫。
4. Markdown测试实际样式与URL Span、主题/长文滚动/字号、调用既有jumpBrowser的URL和Intent、特殊字符URL、无处理程序/不支持scheme反馈、空内容及导航；真实入口用合成文档点击链接，不能只测一个未接上的helper。用户已选择保留系统分发语义，不要求浏览器处理器白名单或新增包查询。
5. 守卫先RED后GREEN；独立副本做精确旧逻辑回退、丢筛选/关键词、OR错接AND、预算漏参、隐藏可见性误清ID、草稿直接应用/返回关闭错误、右侧/横向手势、链接回调断线等删除/取反/移位变异。新边界fixture不从被测常量反算。
6. 最终完整unit、Room、DevDebug、格式/依赖/lint、受影响截图录制/目检和全仓verify；仅合成数据专用模拟器，不操作真实账本。固定SHA整批full-review一次，照实逐项报告未执行环境及知识归档选择。

## 留给实施者的低风险选择

内部值对象/重载命名、UI事件聚合与文件拆分、主题参数和测试夹具组织由实施计划细化；不得据此改变上述业务集合、显示开关语义、草稿提交层级、右侧方向或浏览器边界。若现有依赖不能满足方案或需新增依赖/迁移/改变交互，先回报，不自行扩大范围。
