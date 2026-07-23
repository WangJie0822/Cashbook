# 截图套件「污染」调查报告：机制归因与基线重录（2026-07-23）

- spec：`docs/superpowers/specs/2026-07-23-screenshot-suite-pollution-fix-design.md`（修订 R1 即依据本报告实证）
- 基线：main `48546e66`（= `805fb101` 三依赖 PR 终态 + docs），worktree `worktree-screenshot-pollution-fix`
- 结论速览：**「套件级测试污染」的立项框架被推翻**。全量 verify 的 118 个 FAIL = 依赖升级渲染漂移（机制 D）+ 跨类渲染历史效应（机制 P）叠加，无一例代码回归、无比对器故障；出路 = 全量重录 531 张基线（用户拍板批准，commit `51e76163`）。

## 1. Phase 0 复现固化

| Run | 命令要点 | 结果 | FAIL 用例 |
|---|---|---|---|
| run1 | `verifyRoborazziDebug --rerun-tasks` | BUILD FAILED 5m20s，474/474 executed | 115（101 涉 dynamic） |
| run2 | 同上 | BUILD FAILED 4m39s | 115，与 run1 **逐字节一致** |
| serial（作废） | `--max-workers=1` 无 `--continue` | 206/471 task 后中止 | 大部分 XML 为 run2 残留（mtime 证伪），判定作废 |
| serial2 | `--max-workers=1 --continue` | BUILD FAILED 12m44s，480/480 | 118 |
| run3 | 并行 `--continue` | BUILD FAILED 5m3s，480/480 | 118，与 serial2 **逐字节一致** |

- **完整 FAIL 集 = 118 用例（103 涉 `_dynamic`）**；run1/run2 的 115 是无 `--continue` 时 feature/types 测试 task 未被调度的「恰好一致的截断值」。
- **跨模块并发排除**：并行 == 串行逐条一致。
- 操作教训：① 无 `--continue` 时单 worker 首败即停、并行时任务已全启故都能跑完——固化对照必须带 `--continue`；② 判「某模块 0 失败」前必须核对该模块 XML mtime 是否落在本次 run 窗口（本轮 serial 初跑即靠 mtime 识破 stale 假象）。

## 2. 失败形态

- 118 条失败消息全部为 `AssertionError: Roborazzi: <png> is changed.`；XML 深层扫描 OOM/IOException/decode **零命中**。
- 118 个失败 PNG 与 `_actual.png` 批量逐字节+逐像素比对：**全部 PIXEL_DIFF（真实像素差），零 BYTES_IDENTICAL** → 2026-07-23 早段的「golden 加载失败型（IDENTICAL-yet-FAIL）」在本态**不存在**，比对器无故障。
- Roborazzi verify 在测试方法内**首个 mismatch 处抛断言、跳过该方法剩余帧**：失败消息只暴露首帧（captureMultiTheme 首帧= dark+dynamic、captureMultiDevice 首帧= phone），实际漂移面远大于 FAIL 计数（118 用例 vs 531 文件）。

## 3. 机制归因（tags 模块标本链）

| 实验 | 结果 | 推论 |
|---|---|---|
| EditRecordSelectTag 单类 fresh-JVM verify | FAIL，与套件同集 4/4 | 与执行环境无关 → **机制 D：依赖漂移** |
| MyTags 单类 fresh-JVM verify | **全过**（即自身渲染与 2026-06-25 旧 golden 逐字节一致，零漂移） | 其套件 FAIL（3.48%）**100% 来自执行上下文** → **机制 P** |
| `:feature:tags:recordRoborazziDebug` ×2 | 两次产物 23/23 逐字节相同 | 渲染 = 确定性函数(执行轨迹) |
| MyTags dark_dynamic 三方比对 | record 套件 actual == verify 套件 actual（逐字节）；二者 ≠ 旧 golden（3.48%，diff 集中文本/芯片区 bbox(24,33)-(448,167)）；旧 golden == 今日单类渲染 | 污染变量 = **同 JVM 内前序渲染内容**（【推测】指向 glyph/AA 缓存类内容依赖状态；非 qualifier——全程零 DIMENSION_DIFF 证伪 qualifier 泄漏假设） |
| **Roundtrip**：suite record 产物作基线 → suite verify | **28/28 全绿** | 「suite record → suite verify」闭环稳定；verify/record 轨迹分歧仅在已有失败（早抛跳帧）时级联 |

机制串联【推测——已实证的是「前序渲染内容影响后续渲染」这一现象本身（三方比对）；具体因果链与缓存类型待 §8 根查证实】：EditRecord 类渲染因依赖升级变更（fresh-JVM 首帧即 mismatch）→ 其在 JVM 中留下的内容依赖状态改变 → 后续 MyTags 的「带历史渲染」随之改变（尽管 MyTags 自身 fresh 渲染零漂移）。

## 4. 漂移面全量分级（record 全项目 vs HEAD 基线，531/676 张改写、145 张逐字节复现）

- 模块分布：records 156 / design 138 / assets 72 / ui 44 / settings 40 / books 28 / tags 23 / types 21 / budget 9。
- 幅度分级（逐像素 + 最大通道差）：

| 级别 | 张数 | 定性（抽检实证） |
|---|---|---|
| <1% 像素 | 314 | AA/文本微差，亚感知 |
| 1-10% | 176 | 同上为主 |
| ≥10% 且 delta≤2 | 12 | 大面积表面色整体偏移 ±2 色阶，亚感知（MyCategories 设备图 33.11% 属此类，bbox 即分类网格区） |
| ≥10% 且 delta 26-255 | 29 | **可见变化，全部为 `_dark_*_dynamic` 变体**：抽检（Fab 目检：浅蓝底深"+"→深蓝底浅"+"）指向 Material3 动态暗色 container 色调映射上游变更，或叠加机制 P——二者均非本项目代码回归。【推测，未本轮实测】真机 Android 12+ 动态取色应同步呈现新样式 |

- ≥10% 桶 41 张的 max_channel_delta 实测呈**双峰**（1-2 与 26-255 两簇，3-25 区间零样本），非分级遗漏。
- 佐证：2026-07-09 会话即有「record 531/676 字节不复现」记录（当时按「入库版权威」discard、未做像素定性）——漂移面早于 2026-07-22 依赖潮已部分存在，本轮首次完成像素级定性。
- **结论：无一例指向 Cashbook 代码回归。**

## 5. 处置（用户拍板）

- **重录 531 张入库**（commit `51e76163`；「重录禁令」经 spec 修订 R1 解除——原禁令依据「7/13 逐字节相同」在本态不成立）。
- 机制 P 根查（定位并复位具体泄漏状态）记 backlog；roundtrip 闭环绿证明其不阻塞**稳定绿**（稳定性证据；护栏敏感性由 §6 变异实证补证）。
- 验收：连续 2 次全量 `verifyRoborazziDebug --rerun-tasks --continue` BUILD SUCCESSFUL（见 §6）。

## 6. 验收记录（已实跑确认）

- accept1：`verifyRoborazziDebug --rerun-tasks --continue` **BUILD SUCCESSFUL in 4m 18s**，480/480 executed，extract_fails=0
- accept2：同命令 **BUILD SUCCESSFUL in 4m 28s**，480/480 executed，extract_fails=0
- 基线零残留：验收后 `git status` 无任何 PNG 改动（record 产物与入库基线逐字节一致）
- **敏感性变异实证**：`MyTagsScreen.kt` padding `top = 8.dp → 9.dp` 单处变异 → `:feature:tags` MyTags 类 verify **BUILD FAILED（4/4 用例红）** → `git checkout` 还原（grep 复核 8.dp 回位）→ 复跑 **BUILD SUCCESSFUL**。roundtrip 绿证稳定性、变异红证敏感性，护栏双向自证。

## 7. 新判读口径（已同步 CLAUDE.md）

1. **基线只对「完整模块套件轨迹」有效**：单类/`--tests` 过滤 verify 可能因缺少前序渲染历史而假红（MyTags 标本反例：套件绿的态下单类也可能红，反之亦然）——**单类假红 ≠ 基线错**；判真回归以全量（或整模块）verify 为准。
2. **增删/重命名截图测试会改变模块内渲染轨迹**：须重录该模块基线并随代码同 commit。
3. **依赖升级（尤其 compose-bom/AGP/Robolectric）后全量 verify 大面积红为预期**：按本报告方法（record ×2 定确定性 → 逐像素分级 → 通道差幅度 → 目检可见级样本）定性后重录，禁止逐张豁免或按旧「套件污染」口径免责。
4. 固化 FAIL 集必须 `--rerun-tasks --continue` 并核对各模块 XML mtime。

## 8. Backlog

- 机制 P 根查：定位内容依赖渲染状态（候选：Compose 文本布局缓存 / skia glyph 缓存）并评估逐类复位可行性——根修后单类诊断恢复可用、基线不再绑定套件轨迹。标本 = feature/tags 的 MyTagsScreenScreenshotTests；最小复现：`./gradlew :feature:tags:testDebugUnitTest --tests "*.MyTagsScreenScreenshotTests" -Proborazzi.test.verify=true --rerun-tasks`（单类）对照 `./gradlew :feature:tags:testDebugUnitTest -Proborazzi.test.verify=true --rerun-tasks`（整模块）——若基线为单类轨迹录制则整模块红、反之亦然。
- CI 截图 verify spec（2026-07-09 前置条件）：本报告的 roundtrip 协议与「轨迹绑定」限制是其新增输入——CI verify 必须整套件跑且基线由同轨迹 record 产出。
