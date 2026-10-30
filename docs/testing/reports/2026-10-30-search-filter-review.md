# 搜索筛选评审与收尾记录

日期：2026-10-30。设计契约见 [规格](../../superpowers/specs/2026-10-29-search-filter-design.md)，完整命令与首轮限制见 [验收记录](2026-10-30-search-filter-validation.md)。

## 评审范围与结果

本批执行一次 full-review，五阶段覆盖质量/架构、安全/性能、测试/文档、Kotlin/Compose/Room/Paging、CI/CD，再由控制器汇总。原始冻结范围、完整 finding、错误/恢复历史与报告哈希保留在本地 `.full-review/`，最终状态为 complete。Phase 2 后遵守用户批准点，未跳阶段；中途只读工具错误的 failed 原文仍保留。

去重结果：Critical 0、High 0、Medium 2、Low 0。两项 Medium 分别为 SF-01、SF-02，下面记录修复与客观验收。修复后只有控制器定向复核，没有再发起第二层整批评审；后续产物不冒充旧冻结版本已覆盖。

## SF-01：Keyword 模式筛选入口

`SearchScreen#SearchScreen` 的筛选按钮与 `SearchViewModel#onFilterEvent` 的 OpenDrawer 原来只检查 metadataReady，允许 Keyword 模式编辑暂停中的条件，与“清空关键字后筛选”契约不符。该问题没有关键字叠加、金额错误或越账本证据，定为 Medium。

修复按**已提交的 mode**判断：Keyword 禁用入口，按钮提供 stateDescription“清空关键字后筛选”，事件层同样拒绝打开。仅输入草稿时仍可筛选；清空关键字后恢复已应用条件。原关键字匹配、500ms 历史写入、SQL/统计和共享主金额未调整。

先新增用例并在旧实现取得两个真实 AssertionError：UI 的 `keyword_disablesFilterEntryAndExplainsHowToRestoreIt` 报按钮仍 enabled；状态的 `keyword_blocksDrawerButDraftAndClearKeepFilteringAvailable` 报本应为 null 的 drawerDraft 非空。修复后 UI13/State23 通过；分别撤销按钮禁用、说明和事件 Guard 三个独立变异，具名断言全部失败，证明没有被另一道保护掩盖。

7 张 Keyword 结果页的主题/设备截图重录并查看代表性明暗图，随后 feature 和最终全模块 verify 均通过；其余基准未伪造差异。

## SF-02：筛选资源归属与截图门禁

原新增筛选字符串置于 core/ui，而截图变化属于 feature/records。现有 `.github/scripts/screenshot_freshness.sh` 按模块自身资源与基准变化判断，实际旧冻结范围执行返回 `MISS core/ui`、exit1。

逐个枚举新增资源及 Kotlin/XML 调用，55 个资源只有 feature/records 的 5 个生产文件消费，共 72 处引用。因此仅把它们迁入 `feature/records/src/main/res/values/search_filter_strings.xml`，调用显式使用该模块 R 的 FilterR 别名；原通用 core/ui 资源恢复到原基线的 Git blob。Windows 工作区依 core.autocrlf 保持 CRLF，Git 对比没有通用资源差异。

没有更改 CI、添加豁免标签、伪改 core/ui 图片、改公共仓库接口或迁移数据库。资源整理后相同现有门禁实际返回 `OK: all screenshot modules fresh (or untouched).`、exit0；完整编译、单测、格式、lint、截图和最终 APK 原生回放验证消费者。资源值、格式占位符与既有通用字符串保持原语义。

代码定位可复算：

```bash
git log --fixed-strings --grep='禁用关键字模式筛选入口并补恢复守卫' -1 --format=%H
git log --fixed-strings --grep='将筛选文案归入功能模块并恢复共享资源' -1 --format=%H
```

## 恢复候选与尚未定位记录

R-01 没有被当作已确认产品 Bug。新增 `newViewModel_delayedBookRestoresAppliedCriteriaAndScroll` 从真实 SearchRoute、真实 VM/SavedStateHandle 出发：80 条记录，两次应用形成较高 revision，滚到 record50；建立全新 VM，延迟账本 Flow 首发，再模拟保存状态重建。等待真实结果列表就绪后，record50 仍可见、原标签约束及当前账本恢复、新 revision 小于旧值。该场景通过，未修改恢复产品代码。将实际条件键回退为含 revision 的写法，新增用例以“record50 未显示”的 AssertionError 失败，另一个既有恢复用例也失败。不是以超时/编译错误算作杀死。

R-02 保留原始一次 Room 基线 20 秒超时及当轮未执行变异的 ERROR 记录；后来诊断、原测试还原及补充变异通过，**首次超时根因仍未定位**。原始 XML 没有给出具体等待点，不把它改写为产品/环境/测试故障已修复。完整限制见首轮验收记录。

## 最终运行记录

Windows、JDK 21.0.11、Gradle 9.6.1。下列命令实际退出 0；长日志在本地任务目录，不作为 Git 跟踪交付文件。

```bash
./gradlew testOnlineDebug testOfflineDebug testDebugUnitTest :core:model:test :lint:test -Proborazzi.test.verify=false --rerun-tasks --console=plain
./gradlew :app:assembleDevDebug spotlessCheck :app:lintOnlineRelease :app:lintOfflineRelease :app:lintDevRelease :lint:lint --init-script gradle/init.gradle.kts --no-configuration-cache -Dlint.baselines.continue=true --console=plain
./gradlew verifyRoborazziDebug --console=plain
python -B tools/testing/verify_search_filter_mutations.py --self-test
```

- 完整单测：1504，0 failures/errors/skips，732/732 tasks executed；207 份报告及 214 条显式源码路径见 [测试清单](2026-10-30-search-filter-tests.json)。从实际执行 task 枚举 XML，复算表头/用例数、唯一性、时间与既有套件不丢失；没有 XML 的已执行空测试任务单独列出，不算成测试通过条数。
- 构建/格式/lint：1332 tasks，114 executed、10 from cache、1208 up-to-date；Dev APK 实际安装启动。
- 截图：9 个模块测试实际执行，713 张跟踪基准图；19 executed、465 up-to-date。
- 首轮 16 项实际证据仍在 [原变异清单](2026-10-30-search-filter-mutations.json)；新 4 项和 UI13/State23 的基线/还原、5 目标文件哈希及独立 XML 核验见 [收尾清单](2026-10-30-search-filter-post-review-mutations.json)。两个时期分别保存实际源版本，没有声称全部 20 项在同一个源版本重跑。

收尾 4 项可在相同源提交的干净独占副本运行，路径参数按实际工作区填入；每个子进程有 600 秒硬上限：

```bash
python -B tools/testing/verify_search_filter_mutations.py --source "<source>" --worktree "<mutation-worktree>" --output "<new-output-directory>" --only keyword_filter_button_reenabled --only keyword_filter_hint_removed --only keyword_drawer_guard_removed --only restored_scroll_uses_revision_counter --timeout 600
```

最终 APK 在任务专用只读 Android 11 AVD 中回放，用户账本和原有 AVD 未使用。原生 XML 断言的是“筛选”文字的**按钮父节点** enabled=false，不把仍 enabled 的 TextView 子节点误当按钮；点击禁用入口未打开抽屉，清空后父节点 enabled=true 且原已应用零笔筛选恢复。4 项断言和 APK SHA256 见收尾清单。截图已查看，模拟器已按精确 AVD 名关闭；未删 AVD 或 worktree。APK 先从暂存的资源整理构建、随后提交该整理，生产源文件等同清单所记版本；构建元数据在该次提交前求值，清单分别记录二者。

## 交付边界

无已知 Critical/High 遗留；两项 Medium 已按上述证据定向处理。没有运行远程 CI、物理手机、Mac/Linux 执行或 TalkBack 实际朗读测试，也未做性能测量、升级或部署；不把配置检查/辅助语义测试写成这些动作已通过。

用户明确选择“本次不归档，交付时注明”，本次不执行 Vault 归档。保留独占本地分支、两个 worktree 和可打开产物，未执行主分支合入、推送或 PR/标签写操作。
