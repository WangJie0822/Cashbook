# 会话交接 part2：Cashbook 四项改进（2026-05-30）

> 承接 `HANDOFF-2026-05-29-cashbook-4issues.md`。**新会话先读本文件，再读 spec + plan。**

## 一句话状态

12 个 Task 中 **11 个已完成并 controller hands-on 核验真绿**（git 工作区干净）。**仅剩 ③.4（一级拖动排序 UI）+ 按日分组补全（④.4/④.5 的遗漏）**。

## 工作区 / 分支

- Worktree（必须在此工作，第一步 `pwd` 核对）：
  `D:\Work\Workspace\Owner\Cashbook\.claude\worktrees\feature+cashbook-4issues`
  （bash: `/d/Work/Workspace/Owner/Cashbook/.claude/worktrees/feature+cashbook-4issues`）
- 分支：`worktree-feature+cashbook-4issues`，HEAD `66fbdf53`。
- 进入：`EnterWorktree({path: "<上面绝对路径>"})`（已存在，勿新建）。

## 关键文档

- spec：`docs/superpowers/specs/2026-05-29-cashbook-4issues-design.md`
- plan：`docs/superpowers/plans/2026-05-29-cashbook-4issues.md`（含逐 Task TDD 代码；**③.4 在「## Task ③.4」段**）

## 已完成 commit（main..HEAD，从旧到新）

| commit | Task | 说明 |
|---|---|---|
| `006587a6` | ②.1 | 金额搜索 DAO 加 amountCent（上一会话） |
| `819ee50d` | — | 旧交接文档 |
| `aae10733` | ②.2 | 搜索 keyword→toBigDecimalOrNull 判定金额匹配 amount/finalAmount |
| `034269e6` | ④.1 | RecordDao 按资产+日期范围分页查询 |
| `341688fd` | ④.2 | Repository 按资产+月份分页 + 月度记录 Flow |
| `f0a637e6` | ④.3 | 资产月度余额口径收支结余 UseCase |
| `fb630af7` | ④.4 | 资产记录按月分页 + 月度汇总 ViewModel ⚠️**见下「按日分组」** |
| `ab97811f` | ④.5 | 资产详情按月视图 UI：月份切换器+收支结余统计卡 ⚠️**见下** |
| `6f494284` | ①.1 | 受保护类型点击弹菜单仅保留统计数据项（**测试降级 smoke，见下**） |
| `1e32ebe5` | ③.1 | TypeDao updateSortById + queryByLevel ORDER BY sort |
| `a3c9798d` | ③.2 | Repository updateFirstTypeSort + generateSortById 改 max+1 防撞 |
| `66fbdf53` | ③.3 | MyCategoriesViewModel.onMoveFirstType 拖动重排持久化 |

## 剩余工作 1：③.4 一级分类长按拖动排序（UI）

plan「## Task ③.4」段。**③.1/③.2/③.3 已把底层做完**（TypeDao.updateSortById/maxSortByLevel、Repository.updateFirstTypeSort、ViewModel.onMoveFirstType），③.4 只剩 **UI 拖动入口 + 依赖 + 基线**。

- **reorderable 最新稳定版 = `3.1.0`**（已查 `repo1.maven.org/.../reorderable/maven-metadata.xml`，无 alpha/beta；勿凭记忆，引用此实证）。
- **Step1 PoC（必做）**：临时在 `feature/types/build.gradle.kts` 加 `implementation("sh.calvin.reorderable:reorderable:3.1.0")` + 写最小 `rememberReorderableLazyListState` + `ReorderableItem` 用法，`./gradlew :feature:types:compileOnlineDebugKotlin`（**带代理**，新依赖需下载）验证兼容 Compose BOM 2026.05.01。
  - 兼容 → 方案 A：正式声明（`gradle/libs.versions.toml` + `feature/types/build.gradle.kts`）+ `ExpandableTypeList`（`MyCategoriesScreen.kt:473-514` 附近）改 keyed `items()` + `ReorderableItem` 长按拖动，`onMove`/拖动结束回调透传到已存在的 `viewModel.onMoveFirstType(from, to)`。
  - 不兼容 → 方案 B：`Modifier.pointerInput` + `detectDragGesturesAfterLongPress` 自实现，无新依赖，跳过 libs/基线改动。
- **Step4-6（慢，带代理）**：`:app:assembleOnlineDebug`（验编译）+ `recordRoborazziOnlineDebug`（截图基准）+ `dependencyGuardBaseline`（方案 A 重生成 4 份基线：`app/dependencies/{Online,Offline,Canary}ReleaseRuntimeClasspath.txt` + `app-catalog/dependencies/releaseRuntimeClasspath.txt`）+ `dependencyGuard` 校验。
- commit msg：`[feat|feature|types][公共]一级分类长按拖动排序（reorderable）+ 重生成依赖基线`
- **勿动** `DialogExpandableTypeList`/`SelectFirstTypeDialog`。

## 剩余工作 2：按日分组补全（④.4 + ④.5 的遗漏）

⚠️ **④.4 commit message 写了「按日分组」但实际未实现**（`AssetInfoContentViewModel.recordList` 只加了月份过滤，无 `insertSeparators`/`DayHeader`；`AssetInfoContentScreen` 平铺渲染）。用户已确认：**先 commit ④.5 核心，最后补按日分组**（一个独立 commit）。

- plan ④.4 Step4 + ④.5 Step1 要求：记录列表按日分组（DayHeader + Record），参考首页 `LauncherContentViewModel.recordPagingData` 的 `insertSeparators` + `DayHeader` 模式（复用 `LauncherListItem` 或在 feature/records 内新建等价 sealed）。
- 改：`AssetInfoContentViewModel.recordList`（加 insertSeparators 生成 DayHeader）+ `AssetInfoContentScreen`（渲染 DayHeader）+ 测试。
- commit msg 建议：`[feat|feature|asset][公共]资产详情记录列表按日分组（DayHeader）`

## 全部完成后（controller 终验，强制）

1. controller hands-on 终验所有 commit（`git show` + 重跑关键测试，不直接采纳 subagent 强断言）。
2. 全量回归（带 `-Xmx4g`）：`:core:data:testDebugUnitTest :core:domain:testDebugUnitTest :feature:types:testDebugUnitTest :feature:records:testDebugUnitTest :feature:assets:testDebugUnitTest` + `verifyRoborazziOnlineDebug` + `dependencyGuard` + `:app:assembleOnlineDebug`。
3. **`comprehensive-review:full-review`**（CLAUDE.md 节点 2 强制）对整个 worktree diff 终审。
4. `superpowers:finishing-a-development-branch` 决定合入（注意本地 main 还有 spec/plan 2 笔未推送 doc commit）。

## 环境约束（踩过坑，务必遵守）

1. **gradle 命令必须带 `-Dorg.gradle.jvmargs="-Xmx4g -XX:MaxMetaspaceSize=1g -XX:+UseParallelGC -Dfile.encoding=UTF-8 -Duser.country=CN -Duser.language=zh"`** —— 项目默认 6g 在测试阶段触发 `Gradle build daemon disappeared`（OOM），4g 实测稳定（feature:types 全套 7min 通过）。
2. **禁止 `run_in_background` 跑 gradle** —— 后台 JVM 不回收会累积 → 弹窗 `no enough space for thread` 资源耗尽（本会话踩过，整晚卡死）。前台 `--no-daemon` 跑完即退。
3. **代理 `127.0.0.1:7897`**：Maven Central 必经，跑前 `curl -x http://127.0.0.1:7897 --max-time 20 -sI ... repo1.maven.org` 探活（200 才跑）。命令前缀 `env -u http_proxy -u https_proxy -u all_proxy -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY`。
4. **offline 优先**：core/feature/app 编译链已暖（含 aapt2 + `androidx.hilt:hilt-compiler:1.3.0`）；增量用 `--offline --no-daemon --console=plain`。**仅 ③.4 新依赖 reorderable + roborazzi 录制需带代理**（`-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897`，不加 --offline）。
5. **feature 模块 test task 名是 `testDebugUnitTest`（无 flavor）** —— plan 里写的 `:feature:*:testOnlineDebugUnitTest` 是错的。
6. **BUILD 只信 `grep -E '^BUILD (SUCCESSFUL|FAILED)'`**，不看 bash exit code。
7. JDK 21（默认）；内存：跑前查（可用<1000MB 或 >90% 中止）。

## 关键经验（本会话实证，避免重蹈）

- **Robolectric + Compose BOM 2026.05.01 无法测 DropdownMenu/Popup 菜单交互**：performClick(touch)/sdk=28/performSemanticsAction(OnClick) 四法皆不能使菜单项入语义树（robolectric#9595 onClick regression）。①.1 因此测试降级为渲染 smoke（`MyCategoriesScreenProtectedMenuTest`，详见该文件注释）。③.4 拖动 UI 若要交互测试可能同样受限，优先 ViewModel 层测（onMoveFirstType 已在 ③.3 测）。
- **core:data 测试无法实例化任何 RepositoryImpl**：`FakeCombineProtoDataSource` 不实现 `CombineProtoDataSource` 接口。Repository 新方法测试放 **core:domain** 用 Fake（④.2 `AssetRecordBetweenDateRepositoryTest`、③.2 `TypeFirstSortRepositoryTest` 都这样）。
- **Robolectric 渲染列表项可能在屏幕外**：`assertIsDisplayed` 对屏外节点失败，用 `assertExists`（①.1 smoke 踩过）。
- **跨模块 `core.ui.R` 在 feature Robolectric 测试 ID=0x0**：菜单文案断言用 UI 实际中文文本字面量匹配。
- **`createRecordTypeModel`/`createRecordModel` 在 `core/testing` TestDataFactory**（有 sort/protected/charges/relatedAssetId 参数）；`createTypeTable` 在各测试文件本地定义。
- **字段名**：RecordModel 手续费 `charges`（复数）、转入资产 `relatedAssetId`；RecordTable 是 `charge`/`intoAssetId`。

## 后台 Workflow 教训（勿重蹈）

本任务曾用 `Workflow` 工具后台串行编排，两次受挫：① 无人值守整晚累积 gradle 进程 → 资源耗尽卡死；② 会话中断把后台 Workflow 进程杀掉留半成品。**后台 Workflow 依赖会话存活，不适合本机使用节奏。剩余 ③.4 + 按日分组建议 controller 亲自串行做**（每 Task：读 plan→TDD→跑测试→commit→自核验）。
