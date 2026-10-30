# 搜索筛选验收记录

日期：2026-10-30。范围：当前账本的日期、收支类型、分类、标签多选筛选与全量净额汇总；关键字搜索独立。实现契约见 [设计](../../superpowers/specs/2026-10-29-search-filter-design.md)，任务见 [计划](../../superpowers/plans/2026-10-30-search-filter.md)。

## 构建与回归

Windows、JDK 21.0.11、Gradle 9.6.1，使用项目 wrapper。下列日志均含真实 `BUILD SUCCESSFUL`；构建与全套单测强制重跑。其余命令保留缓存计数，不把缓存任务写为实际执行。

| 验证 | 命令 | 实际结果 | 本地完整日志 |
| --- | --- | --- | --- |
| Dev APK | `./gradlew :app:assembleDevDebug --rerun-tasks --console=plain` | 470/470 tasks executed | `.superpowers/sdd/2026-10-30-search-filter/task-6-assemble.log` |
| 完整单测 | `./gradlew testOnlineDebug testOfflineDebug testDebugUnitTest :core:model:test :lint:test -Proborazzi.test.verify=false --rerun-tasks --console=plain` | 736/736 tasks executed；1501 tests，0 failures/errors/skips | `task-6-full-tests.log`（同目录） |
| 格式 | `./gradlew spotlessCheck --init-script gradle/init.gradle.kts --no-configuration-cache --console=plain` | 106 executed，33 from cache，90 up-to-date | `task-6-spotless.log` |
| Release lint | `./gradlew :app:lintOnlineRelease :app:lintOfflineRelease :app:lintDevRelease :lint:lint -Dlint.baselines.continue=true --console=plain` | 471 executed，164 from cache，11 up-to-date | `task-6-lint.log` |
| 全部截图 | `./gradlew verifyRoborazziDebug --console=plain` | 18 executed，466 up-to-date；9 个截图模块的测试任务实际执行 | `task-6-all-screenshots.log` |
| 真实 Room | `./gradlew :core:data:connectedDebugAndroidTest --rerun -Pandroid.testInstrumentationRunnerArguments.class=cn.wj.android.cashbook.core.data.repository.impl.RecordFilterRepositoryRoomTest --console=plain` | 实际退出 0；14 tests，0 failures/errors/skips | `task-6-room-final.log`；补充收入断言后的重跑见 `task-6-room-income-final.log`（同时执行 `:core:data:spotlessKotlinCheck --init-script gradle/init.gradle.kts --no-configuration-cache`） |

完整单测的 **207 份报告、214 个显式测试源码路径**及逐套件计数保存在 [测试清单](2026-10-30-search-filter-tests.json)。该清单定义本次统计集合；不以数量替代行为覆盖。截图 reference 共 713 张，以 `git ls-files '*src/test/screenshots/*.png'` 枚举；未将其它模块的图片变化混入本次基线。

前五项的旧 PowerShell 外层会话未取得最终退出码，结论依据是已保存的 Gradle 终态行、实际任务记录及 XML，不补称观察到外层退出 0。后续命令使用无 profile shell，Room 已取得最终退出码。

## 关键行为覆盖

| 测试源码（模块内 `src/test/kotlin`；Room 为 `src/androidTest/kotlin`） | 验证对象 |
| --- | --- |
| `core/model/.../model/RecordFilterTest.kt` | 同组任一、跨组交集、动态整组/排除项、日期边界和类型联动 |
| `core/model/.../model/RecordFilterSummaryTest.kt` | 净额、转账费用、平账、行级金额说明 |
| `core/data/.../repository/impl/RecordFilterSqlBuilderTest.kt` | 绑定参数预算、共享谓词、无字符串注入 |
| `core/data/.../repository/impl/RecordFilterRepositoryTest.kt` | 分页与全量汇总、验证失败时不执行查询 |
| `core/data/.../repository/impl/RecordFilterRepositoryRoomTest.kt` | 真实 SQLite 账本隔离、标签去重、日期上界、动态/删除分类标签、61 笔分页、跨期报销、转账优惠、元数据独立写入后的汇总/分页失效 |
| `core/domain/.../GetFilteredRecordViewsUseCaseTest.kt` | 真实领域映射入口和已完成加载状态 |
| `feature/records/.../viewmodel/SearchFilterStateTest.kt` | 关键字独立、草稿取消、目录更新、过期请求/同条件重试、状态恢复、删除条件不放宽 |
| `feature/records/.../view/SearchFilterDrawerTest.kt` | 真实日期控件、隐藏标签、重置/取消/参数错误 |
| `feature/records/.../view/SearchCategoryPickerTest.kt` | 分类多选/搜索/路径、短横屏布局、跨类型浏览与分类草稿 |
| `feature/records/.../screen/SearchFilterScreenTest.kt` | 11 个真实 SearchRoute/Compose 用例：模式切换、金额附注、错误重试、NavHost 返回与重新进入、滚动恢复、迟到旧上下文、条件不可用提示 |

省略段的完整源码路径在测试清单；Room 全名见上方复跑命令。现有关键字查询用例随完整单测执行。数据库 schema/version 未变；`TypeRepository.allTypeListData` 的真实与 Fake 实现均随 app 构建和完整套件检查。

## 真实 APK 操作回放

使用独立新建的 Android 11 AVD，目标 `emulator-5582`，只安装本次 DevDebug APK。实际 applicationId 为 `cn.wj.android.cashbook`，因此未覆盖已有模拟器上的同名应用。用户账本未被读取或清空。

1. 初次进入搜索，显示输入/筛选提示，未自动查询整个账本。
2. 打开筛选和分类页，真实默认分类目录按收支类型、父类与子类显示；选择餐饮整组再排除三餐，完成后显示零笔与零额汇总。证据：`ui-categories-settled.png`、`ui-filter-empty-real.png`。
3. 从真实新增记账入口创建 `filterqa` 标签，保存夜宵支出 120 元、薪资收入 200 元；首页显示收入 200、支出 120、结余 80。证据：`ui-expense-saved.json`、`ui-income-saved.json`。
4. 再进入搜索，选择该标签及收入、支出，结果 2 笔，收入 200、支出 120、结余 80。证据：`ui-tag-two-types-result.json`/`.png`。
5. 改为仅收入，结果 1 笔 200 元；提交关键字 `120` 并退出搜索展开层后显示夜宵支出 120 元，无筛选汇总，提示筛选暂不生效。清空关键字恢复仅收入 200 元。证据：`ui-income-filter-result.json`、`ui-keyword-results-visible.json`/`.png`、`ui-filter-restored.json`/`.png`。

上述证据在 `.superpowers/sdd/2026-10-30-search-filter/`，均为任务合成数据。截图已逐张查看原图。键盘提交后的展开层通过返回关闭，本次未将该展开层交互单独判为回归。曾将缩放预览中的 Checkbox 误判为窄框；原图复核后撤回，没有因此改产品代码。

操作回放不冒充覆盖全部复杂边界：跨账本、跨期报销、多页、故障注入与生命周期的详细组合分别由真实 Room、状态与 Compose 测试覆盖；尚未在物理手机上执行。

## 变异、评审与交付状态

原定 13 个变异已完成：`mutations-v2/results.json` 状态 PASS，13/13 由指定守卫捕获，四组基线及还原复跑全绿，实际进程退出 0；源码哈希未变，验证副本已按原字节还原。工具为 `tools/testing/verify_search_filter_mutations.py`：独立同提交 worktree、具名守卫、每个子进程硬超时；编译错误和超时不算 KILLED。解析器阳性/阴性对照已通过。

首次运行保存在 `mutations/`，因 Android UTP 的 Truth 失败文本不含 `AssertionError` 类名而被工具判 ERROR；未把该轮计为通过。原始 XML 和实际断言差异已保留，源码哈希及副本还原均为 true。识别器现要求明确的异常类型，或限定的 Android `expected/but was` 形态、对应具名用例的第一帧、以及回读该行确为 `assertThat` 的集合/相等断言。错误类型、错误用例和非断言语句均有反向对照；每轮原始 XML 另存以免被下一轮覆盖。修正后的完整运行使用新目录 `mutations-v2/`。

独立回读 `controller-room-check-v2.json`（XML DOM 路径）确认了七个 Room 变异的实际断言差异，包括 61 笔被错误截为 20 笔，以及转账本金误计。补充收入月的真实事务断言后，三个额外变异也完成：`mutations-net-income-v2/results.json` 为 PASS，3/3 被捕获，Room/界面基线与还原复跑全绿，进程退出 0，源码及副本还原核对为 true。独立 XML DOM 回读 `controller-assertion-check.json` 确认：支出应为 20000 分却变为 100000 分，报销收入应为 0 却变为 80000 分，以及“统计计入：收入 ¥0.00”节点消失。

合计 **16/16**，原定 13 个与补充 3 个分别在同提交的独立副本执行；两次运行的四个被变异运行时源文件哈希完全相同，并与当前源码相同。后一次只增强测试，没有修改运行时行为。逐项具名失败、断言依据、所有失败用例名、XML 相对路径、基线/还原计数和哈希见 [变异清单](2026-10-30-search-filter-mutations.json)，原始运行与首次失败仍保留在本地证据目录。

复跑工具使用 `python -B tools/testing/verify_search_filter_mutations.py --source <源检出> --worktree <同提交的干净独占副本> --output <新证据目录> --device <模拟器序列号>`；可重复传 `--only <变异ID>` 限定范围，`--list` 查看全部 ID，`--self-test` 运行解析器对照。工具在 Windows Python 3.14 实跑；Mac/Linux 的路径与子进程分支未实跑，不冒称跨平台验证通过。

### 未定位的仪器测试超时

补充运行 `mutations-net-income/` 在绿测基线阶段退出 2：14 条中 `typeOnlyWrite_invalidatesLivePaging` 等待 20 秒超时；三个额外变异尚未执行。该轮原始 XML、Gradle 日志和错误结果完整保留，不计通过。不能据此认定是产品失效或测试自身故障。

仅在独立副本的测试中记录阶段、页快照及独立汇总，进行一次 Gradle 单例回放、30 次有界单例回放和 5 次整类 14 用例回放，均未复现超时。明细位于 `paging-timeout-probe/run-1.log`、`repeated/results.json`、`repeated-class/results.json`。诊断输出显示写入后空页与零汇总，**这不构成原超时已修复的证明**。诊断改动已从字节基线精确还原；产品代码未增加规避逻辑。

恢复原测试后的补充运行已在 `mutations-net-income-v2/` 结束并通过。**原超时根因仍未定位**，作为最终评审及交付的已知限制保留，不把后来通过改写为全程无失败。

本记录冻结时，最终 full-review 尚未开始；后续固定范围、原始报告和状态分别保存在 `.full-review/00-scope.md`、`.full-review/raw/`、`.full-review/state.json`。尚未合入主分支、推送或创建 PR。知识文档登记 pending-docs 不代表已归档到知识库。
