# 截图套件污染修复设计（本机全量 verify 可信绿）

- 日期：2026-07-23
- 状态：**已按 Phase 1 归因修订（见文首「修订 R1」节，原§1-§6 保留为立项时假设记录）**；实施计划另见同名 plan
- 前序：`2026-07-09-ci-feature-core-unit-test-coverage-design.md`（截图 verify 独立 spec 前置条件节）、`2026-07-22-compilesdk-37-upgrade-design.md`（Task 9 截图 verify 深挖）

## 修订 R1（2026-07-23，Phase 0/1 实证后，用户批准路径）

Phase 0/1 在当前 main HEAD（`48546e66`）的实证推翻了本 spec 立项时的三个核心前提，路径修订如下。原 §1-§6 保留作立项假设记录，与本节冲突处**以本节为准**。

### R1.1 实证结论（证据见调查报告 `docs/testing/reports/`）

1. **FAIL 集**：全量 `verifyRoborazziDebug --rerun-tasks --continue` 稳定 118 用例（103 涉 `_dynamic`），并行==串行逐字节一致 → 跨模块并发排除、确定性。
2. **「golden 加载失败型（IDENTICAL-yet-FAIL）」在当前态不存在**：118 个失败 PNG 全部为真实像素差（0.01%-99.7%），XML 零异常栈。原 L1 线关闭。
3. **机制 D（依赖漂移）**：部分类（如 EditRecordSelectTagBottomSheet）fresh-JVM 单类跑首帧即 mismatch → 渲染因依赖升级（2026-07-22/23 compose-bom 2026.06.01/lifecycle 2.11/AGP 9.3.0 等）真实变更；record 两次逐字节全同（渲染确定）。
4. **机制 P（跨类渲染历史依赖）**：MyTags dynamic 帧单类 PASS（与 2026-06-25 旧 golden 逐字节一致=自身零漂移）、套件 FAIL（3.48%）→ 同 JVM 内**前序渲染内容**改变后续渲染（diff 集中文本/芯片区，指向 glyph/AA 缓存类内容依赖状态）；且 verify 早抛跳帧会改变下游轨迹（级联效应）。
5. **Roundtrip 闭环绿**：以 suite record 产出为基线，suite verify 全过（tags 模块 28/28 实证）→「suite record → suite verify」是稳定协议；机制 P 不阻塞可信绿。

### R1.2 路径修订

- **重录基线解禁**：原 §2「重录——已证伪」的依据（2026-07-23 早段「7/13 逐字节相同」）在当前态不成立（record 现稳定产出不同像素）。重录在本轮语义 = **依赖升级后的基线更新**，以「全量漂移分级报告 + 视觉抽检 + 用户拍板」为前置门，非「用重录掩盖污染」。
- **机制 P 根修降级为 backlog**：roundtrip 闭环绿证明 P 不阻塞验收目标（稳定性证据；护栏敏感性另由变异实证补证，见调查报告 §6）；根查（定位具体泄漏状态并复位）记 backlog，标本与证据链在调查报告。
- **新增限制条款（写入 CLAUDE.md）**：① 基线仅对「完整模块套件轨迹」有效——**单类/`--tests` 过滤 verify 可能假红**，不得据以判基线错（诊断口径反转：单类假红=正常，套件红=真问题）；② 增删/重命名截图测试会改变模块内渲染轨迹，**须重录该模块基线**随代码同 commit。
- **验收判据不变**：连续 2 次全量 verify 0 FAIL + 单双跑一致；「基线零改动」判据废除，改为「基线重录 diff 经分级报告+抽检+拍板后入库」。
- `_dynamic` 定性并入漂移报告一体拍板（不再单列三选一：污染标本已证 dynamic 帧同受机制 P/D 影响，与非 dynamic 无本质区别）。

## 1. 背景与实证

项目 676 张 Roborazzi 截图基线（9 个 feature/core 库模块）由本机 record 入库为权威 reference，CI 当前不 verify/不 record 库模块截图——全量 `verifyRoborazziDebug` 是这批基线的唯一防线。

2026-07-23 compileSdk 37 专项深挖实证（采集自当时的升级 worktree 态）：

- 全量 `verifyRoborazziDebug` 稳定报 ~21 个非 dynamic FAIL（settings/assets 模块）。
- 其中 **10 对 `_actual.png` 与 golden 逐字节 IDENTICAL 仍 FAIL**（纯 Python PNG 解码器逐像素 diff 实证，mtime 证实 actual 为同轮产物）→ 失败点不在渲染，而在 compare/golden 加载环节。
- SettingScreen actual 缺「月起始日/信用卡还款提醒」两条目（127 行 diff）；真机冒烟该页渲染齐全 → 非产品缺陷。
- 单类跑（`:feature:settings:testDebugUnitTest --tests "...SettingScreenScreenshotTests" -Proborazzi.test.verify=true`）test-results XML 证实 4/4 真执行全 PASS。
- 另有 `_dynamic`（Material You 动态配色）变体 **260/676 张**（`git ls-files` 实测；`_notDynamic` keeper 亦 260 张）本机 verify 既有噪声，根因从未核实。

当前项目 CLAUDE.md 以「全量 verify 批量 FAIL 判别口径③」条款为这批假 FAIL 免责——本 spec 的目标是修复根因后**撤掉免责**，让 FAIL 恢复「即真回归」的语义。

## 2. 范围

**IN**：
1. 套件级污染修复：golden 加载失败型（IDENTICAL-yet-FAIL）+ 渲染型 FAIL 的机制归因与修复，使本机全量 `verifyRoborazziDebug` 恢复 100% 可信绿。
2. `_dynamic` 变体噪声根因定性，按结论处置（修复保留 / 降级验证 / 裁撤，由用户拍板）。
3. CLAUDE.md 相关条款与 2026-07-09 spec 前置条件状态的同步更新。

**OUT（评审钉死的非目标）**：
- CI 截图 verify 上线（独立后续 spec，前置条件 H1/H2/M1/M2/M3/L1/L-E 见 2026-07-09 spec）。
- `ScreenshotHelper.kt:47 changeThreshold=0f` 调整——污染实证是逐字节 IDENTICAL 仍 FAIL，与像素容差无关；容差是后续 CI spec 的 H1 决策，本轮不抢跑（该值被 54 个测试文件、184 处引用）。
- 基线重录——2026-07-23 已证伪（曾重录 13 张，7/13 与 HEAD golden 逐字节相同、6 张仅 1 字节差，golden 本就正确）。
- 豁免清单——FAIL 集随依赖漂移（lifecycle 2.11 曾显性化潜伏脆弱性），豁免清单会失守且掏空门禁。
- 生产代码变更——修复只允许落测试代码 / `core:testing` 捕获层；若调查发现真实生产 bug（如 SettingScreen 内部异步未 settle），单列独立评审，不混入本轮。
- CI 命令/workflow 文件变更。

## 3. 节点 1 四维评审结论（feasibility / security / reverse / impact，已按 file:line 合并去重）

### 3.1 根因分类学修正（High，本 spec 最重要的修正）

- **「异步 uiState 渲染竞态型」标签撤回**：`feature/settings/src/test/.../SettingScreenScreenshotTests.kt:49-61` 的 uiState 为**静态直灌**（完整构造的 `SettingUiState.Success`），走无状态 overload `SettingScreen(uiState=…)`（`SettingScreen.kt:204`），测试路径无 ViewModel、无 Flow、无异步加载。缺失的两条目在 `LazyColumn`（`SettingScreen.kt:375`）内、纯由静态 uiState 字段驱动（`:431`/`:452`）。原定性与代码事实冲突，改为「渲染/重组时序或全局状态泄漏型【待 Phase 1 实证】」。
- **新候选机制【推测，Phase 1 优先验证】**：`core/testing/.../ScreenshotHelper.kt:77` 的 `RuntimeEnvironment.setQualifiers(...)` 是 Robolectric **进程级全局状态**；前序测试类的 `captureMultiDevice`（tablet 1280×800 等 qualifier）改写后未复位，泄漏进后续 `captureMultiTheme` 捕获 → 视口高度变化 → LazyColumn 底部条目落出视口。
- **「单类 PASS = 套件污染」判据非同源**：单类对照用 `testDebugUnitTest --tests ... -Proborazzi.test.verify=true`，全量用 `verifyRoborazziDebug`，两条不同 Gradle 入口对 Test task 的属性注入/输出装配不必然一致。「套件污染」定性降级为【待验证】；Phase 0 必须用与交付判据同源的命令固化。

### 3.2 备选路径裁决

- **方案 B（加固优先：maxHeapSize + Roborazzi 升级）四路一致否决**：IDENTICAL-yet-FAIL 证明渲染正确、失败在 compare/加载环节，加堆仅在「golden 解码期 OOM」这一未测分支下对症（≥3 个互斥候选机制无一有测量）；Roborazzi 升级三重未验证（新版本可达性 / 无上游 issue 佐证 / 零容差下可能触发 676 张重录掩盖真因），且 dependencyGuard baseline 实测不覆盖 test 依赖（`dependencies/*.txt` 零 roborazzi 命中，仅 `*ReleaseRuntimeClasspath`）——升级零自动化门禁，人工审查是唯一控制点。
- **方案 C（结构隔离 forkEvery）降级为候选修复手段**：54 个截图测试类 × Robolectric sandbox 冷启的时长代价；「纯类间污染」假设未验证（`captureMultiTheme` 单方法内多帧 mutate + 全局 qualifier，类内亦可能有竞态）；全局 forkEvery 会殃及所有模块单测；拆独立 test task 破 CI `testDebugUnitTest` 聚合口径。仅当 Phase 1 证据指向类间泄漏且源头复位不可行时，作定向（仅截图模块）手段使用。

### 3.3 共同护栏（写入 plan 的强制约束）

1. **Phase 0 前置门**：现有证据全部采自 compileSdk37 worktree 态，当前 main HEAD（三依赖 PR 合入后）未复验。连续 2 次同源命令固化同一 FAIL 清单后才进入修复；若当前 HEAD 全绿则中止上报重议。
2. **build-logic `withType<Test>`（`KotlinAndroid.kt:89-100`）是全模块 choke point 且传导 CI**（CI `Run local tests` 用聚合 `testDebugUnitTest`）：修复优先就地定向（截图模块 / `core:testing`），不落 build-logic 全局；既有先例为 `core:testing` 的 `failOnNoDiscoveredTests` 就地关闭。表述规范：「不改 CI 命令，build-logic 改动对 CI 的传导已核算」。
3. **`_dynamic` 裁撤的 glob 护栏**：`_dynamic.png$` 260 张 vs `_notDynamic.png$` 260 张，大小写不敏感或 `*dynamic*` 子串匹配会命中 520 张连 keeper 一起删。裁撤必须 end-anchored 大小写敏感生成清单 → 人工核对条数=260 → `git rm`，删除与测试参数变更（`shouldCompareDynamicColor`）同一 commit。
4. **插桩临时化**：调查插桩全程在 worktree 内；提交前对 `KotlinAndroid.kt` 与 `ScreenshotHelper.kt` 两个 choke point 逐行 `git diff` 确认零残留；每阶段结束 `git status` 核验工作区。
5. **探针自证**：golden 加载失败探针先用已知该红的注入（如人为破坏 golden 文件）证明有区分力，再信其报告。
6. **修复自证**：修前可复现红 → 修后绿 → 变异验证（撤掉修复 FAIL 复活）；每修一处全扫 54 个测试文件同构造。
7. **【推测】待实测项**：test fork JVM 默认堆值（不预设 512MB）——实测路径 `./gradlew :feature:settings:testDebugUnitTest --info` 读 fork 命令行，或测试内打印 `Runtime.getRuntime().maxMemory()`。
8. **资源规约**：全量 verify 前查内存（32GB 规约）；gradle 结果只信 `^BUILD (SUCCESSFUL|FAILED)`；后台跑日志末行须为 actionable tasks。

### 3.4 对「投入价值」质疑的回应（reverse R-8）

本轮修复的隔离/复位/机制结论是后续「CI 截图 verify spec」的硬前置——CI 跑的正是全量套件，套件污染不修 CI verify 必然假红。本轮投入直接复用于 CI spec，非一次性本机绿。

## 4. 设计（Phase 0-4）

### Phase 0 复现固化（前置门）

- 当前 main HEAD 全量 `verifyRoborazziDebug` 连跑 2 次，固化 FAIL 清单：非 dynamic / dynamic 分列，记录两次跑的一致性（FAIL 集是否稳定）。
- 加跑一次 `verifyRoborazziDebug --max-workers=1` 对照：FAIL 消失 → 根因含跨模块并发（`org.gradle.parallel=true`），假设池收窄。
- 操作纪律（2026-07-23 已踩坑复用）：`--tests` 过滤器配 `verifyRoborazziDebug` task 名不生效（0 执行假绿）；`--console=plain` 不打印 PASSED 行，判执行只信 `build/test-results/**/*.xml`；判 FAIL 只信本次 run 的 FAILED 标记 + compare 图 mtime，勿被 stale 残留误导。
- **门**：固化出稳定 FAIL 清单 → 进入 Phase 1；当前 HEAD 全绿 → 中止上报（症状已被依赖态变化消除，重议范围）。

### Phase 1 机制归因（systematic-debugging，两线分头）

**L1 golden 加载失败型（IDENTICAL-yet-FAIL）**：
- 插桩 compare 环节异常路径（Roborazzi compare/golden 读取），捕获异常类型（OOM / IOException / decode error / 其他）。
- 候选池：golden 解码期 OOM（配合 fork 堆实测）/ 文件句柄耗尽 / verify 并发文件 I/O 争用 / Roborazzi 内部状态累积。
- 探针先自证区分力（护栏 5）。

**L2 渲染型（标签已归零）**：
- 优先验证 qualifier 泄漏假设：capture 前打印 `RuntimeEnvironment.getQualifiers()` 与实际 density，对比单类跑与套件跑的取值差异。
- 候选池：全局 qualifier 泄漏 / Compose 重组 idle 时序（PAUSED looper 下捕获前无 `waitForIdle`）/ Robolectric looper 状态累积。

- 产物：每类 FAIL 的机制结论，每条带一手证据（插桩输出/对照实验记录）。
- 瓶颈条款：连续 2 轮假设被推翻仍未定位 → 按【排查暂停】固定格式输出，等待用户输入，不继续抛新猜测。

### Phase 2 按证据修复

- 修复原则：就地定向（截图测试 / `core:testing` 捕获层），禁碰生产代码，禁落 build-logic 全局。
- 候选手段池（以 Phase 1 证据为前提启用）：
  - qualifier 泄漏 → `captureForDevice`/`captureMultiTheme` 捕获前后显式复位 qualifiers（源头修复）；
  - idle 时序 → 捕获前显式驱动 looper/Compose idle 至稳定态 + **前置自证断言**（capture 前断言目标节点已渲染，注入没生效先红在前置）；
  - golden 加载 OOM（若实证）→ 定向堆调整（仅截图模块就地配置，核算 CI 传导：ci-gradle.properties workers.max=2 下的并发峰值）；
  - 类间泄漏且源头复位不可行（若实证）→ 定向隔离（仅截图模块，先单模块实测时长增量）。
- 每修一处全扫 54 个测试文件同构造；修复自证按护栏 6。

### Phase 3 `_dynamic` 定性与处置

- 复现：dynamic FAIL 集两次跑一致性 + diff 内容定性（整体色偏 vs 局部 vs 随机）。
- 三选一带证据请用户拍板：
  1. 确定性可修（如同为套件污染的受害者）→ 修复保留 260 张；
  2. 本质非确定（Robolectric 下动态配色取色不可确定化）但仍有验证价值 → 降级验证（如仅 notDynamic 参与像素比对，dynamic 变体退出 verify）；
  3. 本质非确定且无降级价值 → 裁撤，按护栏 3 执行（end-anchored 清单 + 数量核验 + 测试参数与基线删除同 commit），并在 spec/CLAUDE.md 记录 38% 覆盖损失。
- 「保留」与「裁撤」都需机制证据——噪声护栏等于假护栏，不做无证据的二元决策。

### Phase 4 验收与同步

- 验收三条：① 连续 2 次全量 `verifyRoborazziDebug` 0 FAIL（dynamic 按 Phase 3 拍板口径）② 基线 PNG 零改动（`git status` 干净；若某模块修复后基线确实变化，单独判断是「修复暴露错误基线」还是「修复引入副作用」，两者处置相反，不一律重录）③ 单类抽查与全量结论一致。
- 同步更新（漏更会误导后续会话按旧口径放行真回归）：
  1. 项目 CLAUDE.md「全量 verify 批量 FAIL 判别口径③」条款 → 改为指向本 spec + 记录污染已修与修法；
  2. 项目 CLAUDE.md「_dynamic 变体可能报 changed 勿据此判基线错」免责条款 → 按 Phase 3 定性结果更新（修复则删免责，降级/裁撤则明确边界与判据）；
  3. 2026-07-09 spec「截图 verify 独立 spec 前置条件」节 → 回填本轮解决的部分（套件污染）与仍待 CI spec 处理的部分（H1 容差等）。
- 调查报告入 `docs/testing/reports/`（机制结论 + 证据链 + 修复清单）。

## 5. 退路

- 调查不收敛 → 按瓶颈条款暂停等输入；不退向重录/豁免（§2 已钉死）。
- 若 Phase 1 证据指向 Roborazzi 上游 bug 且新版已修 → 升级作为**独立变更**单独评审：钉具体版本（禁 latest）、PR 附 changelog/release 说明核验、显式声明供应链核验（dependencyGuard 不覆盖 test 依赖，人工审查是唯一控制点）、先单模块 dry-run 验证 golden 兼容（确认不触发全量重录）再决定；不与本轮污染修复捆绑。
- 修复手段均为配置/测试代码级，逐项可 revert；无黏性产物（不重录基线、不升级依赖）。

## 6. 与后续「CI 截图 verify spec」的衔接

本轮交付后，CI spec 的剩余前置为：H1 像素容差（跨 OS 渲染差异吸收）、H2 push-to-main 自愈、M1 record 后二次 verify、M2 独立 job、M3 独立录 Linux 基线 PR、L1 file_pattern 收窄、L-E UP-TO-DATE 破缓存，以及「跨 OS 渲染确定性」PoC。本轮的机制结论与隔离/复位手段直接作为该 spec 的输入。
