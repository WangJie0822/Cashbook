# 截图套件「污染」调查报告：机制归因与基线重录（2026-07-23）

> ⚠️ **2026-07-24 更正（§9）**：本报告归因的「机制 P（跨类渲染历史效应）」经次日根查实证**不存在**——其唯一立案证据（§3「MyTags 单类 verify 全过」）是参照系错误的实验缺陷（record2 已把基线覆盖为套件产物、verify 前未还原）。118 FAIL 全部由机制 D 单独解释；渲染与执行轨迹无关。§3/§7 受影响条目已就地标注，完整证据链见 §9。基线重录决策（`51e76163`）不受影响。

- spec：`docs/superpowers/specs/2026-07-23-screenshot-suite-pollution-fix-design.md`（修订 R1 即依据本报告实证）
- 基线：main `48546e66`（= `805fb101` 三依赖 PR 终态 + docs），worktree `worktree-screenshot-pollution-fix`
- 结论速览：**「套件级测试污染」的立项框架被推翻**。全量 verify 的 118 个 FAIL = 依赖升级渲染漂移（机制 D）~~+ 跨类渲染历史效应（机制 P）叠加~~【§9 更正：机制 P 不存在，全部为机制 D】，无一例代码回归、无比对器故障；出路 = 全量重录 531 张基线（用户拍板批准，commit `51e76163`）。

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
| MyTags 单类 fresh-JVM verify | **全过**（~~即自身渲染与 2026-06-25 旧 golden 逐字节一致，零漂移~~）【§9 更正：该 run（20:43）前 1 分钟 record2（20:42）已把工作区基线覆盖为套件 record 产物且未还原，PASS 实为「单类渲染 == 套件产物」；「与旧 golden 一致」从未被字节比对（verify PASS 不产 actual 文件），系参照系错误推断】 | ~~其套件 FAIL（3.48%）100% 来自执行上下文 → 机制 P~~【§9 更正：同参照系（旧 golden）下单类与套件失败集逐用例一致，无单类/套件分歧，机制 P 不成立】 |
| `:feature:tags:recordRoborazziDebug` ×2 | 两次产物 23/23 逐字节相同 | 渲染 = 确定性函数(执行轨迹) |
| MyTags dark_dynamic 三方比对 | record 套件 actual == verify 套件 actual（逐字节）；二者 ≠ 旧 golden（3.48%，diff 集中文本/芯片区 bbox(24,33)-(448,167)）；~~旧 golden == 今日单类渲染~~【§9 更正：第三方样本来自上一行的 PASS 推断，参照系错误；实际单类渲染 == record 套件 actual（2026-07-24 exp4 字节级实证）】 | ~~污染变量 = 同 JVM 内前序渲染内容~~【§9 更正：不存在该污染变量；非 qualifier 的结论（零 DIMENSION_DIFF）仍成立】 |
| **Roundtrip**：suite record 产物作基线 → suite verify | **28/28 全绿** | 「suite record → suite verify」闭环稳定；verify/record 轨迹分歧仅在已有失败（早抛跳帧）时级联 |

~~机制串联【推测——已实证的是「前序渲染内容影响后续渲染」这一现象本身（三方比对）；具体因果链与缓存类型待 §8 根查证实】：EditRecord 类渲染因依赖升级变更（fresh-JVM 首帧即 mismatch）→ 其在 JVM 中留下的内容依赖状态改变 → 后续 MyTags 的「带历史渲染」随之改变（尽管 MyTags 自身 fresh 渲染零漂移）。~~

【§9 更正：上述机制串联整段作废——「前序渲染内容影响后续渲染」的现象本身即由参照系错误虚构，§8 根查（2026-07-24 执行）已证伪。MyTags 的 3.48% diff 与 EditRecordSelectTag 的 diff 同为机制 D（依赖升级直接改变各自渲染）。】

## 4. 漂移面全量分级（record 全项目 vs HEAD 基线，531/676 张改写、145 张逐字节复现）

- 模块分布：records 156 / design 138 / assets 72 / ui 44 / settings 40 / books 28 / tags 23 / types 21 / budget 9。
- 幅度分级（逐像素 + 最大通道差）：

| 级别 | 张数 | 定性（抽检实证） |
|---|---|---|
| <1% 像素 | 314 | AA/文本微差，亚感知 |
| 1-10% | 176 | 同上为主 |
| ≥10% 且 delta≤2 | 12 | 大面积表面色整体偏移 ±2 色阶，亚感知（MyCategories 设备图 33.11% 属此类，bbox 即分类网格区） |
| ≥10% 且 delta 26-255 | 29 | **可见变化，全部为 `_dark_*_dynamic` 变体**：抽检（Fab 目检：浅蓝底深"+"→深蓝底浅"+"）指向 Material3 动态暗色 container 色调映射上游变更~~，或叠加机制 P~~【§9 更正：机制 P 不存在，归因收敛为纯上游变更】——非本项目代码回归。【推测，未本轮实测】真机 Android 12+ 动态取色应同步呈现新样式 |

- ≥10% 桶 41 张的 max_channel_delta 实测呈**双峰**（1-2 与 26-255 两簇，3-25 区间零样本），非分级遗漏。
- 佐证：2026-07-09 会话即有「record 531/676 字节不复现」记录（当时按「入库版权威」discard、未做像素定性）——漂移面早于 2026-07-22 依赖潮已部分存在，本轮首次完成像素级定性。
- **结论：无一例指向 Cashbook 代码回归。**

## 5. 处置（用户拍板）

- **重录 531 张入库**（commit `51e76163`；「重录禁令」经 spec 修订 R1 解除——原禁令依据「7/13 逐字节相同」在本态不成立）。
- 机制 P 根查（定位并复位具体泄漏状态）记 backlog【§9 更正：根查已于 2026-07-24 完成，结论=机制 P 不存在】；roundtrip 闭环绿证明其不阻塞**稳定绿**（稳定性证据；护栏敏感性由 §6 变异实证补证）。
- 验收：连续 2 次全量 `verifyRoborazziDebug --rerun-tasks --continue` BUILD SUCCESSFUL（见 §6）。

## 6. 验收记录（已实跑确认）

- accept1：`verifyRoborazziDebug --rerun-tasks --continue` **BUILD SUCCESSFUL in 4m 18s**，480/480 executed，extract_fails=0
- accept2：同命令 **BUILD SUCCESSFUL in 4m 28s**，480/480 executed，extract_fails=0
- 基线零残留：验收后 `git status` 无任何 PNG 改动（record 产物与入库基线逐字节一致）
- **敏感性变异实证**：`MyTagsScreen.kt` padding `top = 8.dp → 9.dp` 单处变异 → `:feature:tags` MyTags 类 verify **BUILD FAILED（4/4 用例红）** → `git checkout` 还原（grep 复核 8.dp 回位）→ 复跑 **BUILD SUCCESSFUL**。roundtrip 绿证稳定性、变异红证敏感性，护栏双向自证。

## 7. 新判读口径（已同步 CLAUDE.md）

> 【§9 更正：口径 1/2 因机制 P 被证伪而**作废**，CLAUDE.md 已于 2026-07-24 改为更正版口径（单类红即真信号、增删测试不影响其他测试渲染）；口径 3/4 仍有效。】

1. ~~**基线只对「完整模块套件轨迹」有效**：单类/`--tests` 过滤 verify 可能因缺少前序渲染历史而假红（MyTags 标本反例：套件绿的态下单类也可能红，反之亦然）——**单类假红 ≠ 基线错**；判真回归以全量（或整模块）verify 为准。~~【作废】
2. ~~**增删/重命名截图测试会改变模块内渲染轨迹**：须重录该模块基线并随代码同 commit。~~【作废】
3. **依赖升级（尤其 compose-bom/AGP/Robolectric）后全量 verify 大面积红为预期**：按本报告方法（record ×2 定确定性 → 逐像素分级 → 通道差幅度 → 目检可见级样本）定性后重录，禁止逐张豁免或按旧「套件污染」口径免责。
4. 固化 FAIL 集必须 `--rerun-tasks --continue` 并核对各模块 XML mtime。

## 8. Backlog

- ~~机制 P 根查~~ ✅ **已完成（2026-07-24，见 §9）**：结论 = 机制 P 不存在，无泄漏状态可定位；单类诊断本就可用、基线从不绑定套件轨迹。
- CI 截图 verify spec（2026-07-09 前置条件）：~~本报告的 roundtrip 协议与「轨迹绑定」限制是其新增输入——CI verify 必须整套件跑且基线由同轨迹 record 产出~~【§9 更正：轨迹绑定约束解除，CI verify 可按任意粒度（单类/单模块/分片）执行；roundtrip 协议（record 后即刻 verify 自洽）仍推荐保留】。

## 9. 更正：机制 P 根查结论（2026-07-24）

### 9.1 缺陷定位（昨日实验时序考古，源自会话 transcript 命令原文 + daemon 日志 + 产物 mtime）

| 本地时间 | 动作 | 基线状态 |
|---|---|---|
| 20:35:39 | record1（`:feature:tags:recordRoborazziDebug`）| 覆盖为套件产物 B |
| 20:39:27 | 快照 record1 产物 + `git checkout -- feature/tags/src/test/screenshots/` | **还原为旧 golden** ✓ |
| 20:39:42–20:42:33 | record2（与 record1 逐字节互比） | **再次覆盖为 B，此后无还原** |
| 20:43:39–20:47:08 | single-mytags 单类 verify（daemon-27036 启动时刻 20:43:42 吻合） | **仍为 B** ← 参照系错误 |

- §3「MyTags 单类 verify 全过」的参照不是旧 golden 而是 B → PASS 实为「单类渲染 == B（套件产物）」。
- 「与旧 golden 逐字节一致」从未被真实字节比对：verify PASS 时 Roborazzi 不产出 `_actual.png`，该断言只能来自 PASS 推断，而推断前提（基线=旧 golden）不成立。

### 9.2 证伪实验（2026-07-24，五组独立对照）

| # | 实验 | 结果 | 判定 |
|---|---|---|---|
| 1 | 主 checkout 单类 MyTags verify（基线=新基线 B） | 4/4 绿（XML 实证） | 单类渲染 == B |
| 2 | 主 checkout 整模块 tags 套件 verify | 30/30 绿 | 套件渲染 == B |
| 3 | 主 checkout 单类 **record** 全帧落盘 | `git status` 0 变化（12 张逐字节同 B） | 单类渲染 == B（字节级，不受 verify 首帧抛断言跳帧影响） |
| 4 | **fresh worktree @`48546e66`（基线=旧 golden）** 单类 verify | 2/4 红（dark_dynamic 帧），actual 与 B **字节 IDENTICAL**、与旧 golden DIFFERENT | 基线未被污染时单类**本该红**——昨日若参照真是旧 golden 不可能 PASS |
| 5 | 昨日 `suite-oldgolden.log`（还原旧 golden 后整模块套件 verify，21:01） | 6 失败 = EditRecordSelectTag 4 用例 + MyTags 恰 2 个 multipleThemes 用例（multipleDevices 绿） | **与实验 4 单类失败模式逐用例一致** → 同参照系下单类 == 套件，无分歧 |

- 辅助实证：像素级比对（stdlib PNG 解码）旧 golden vs 新基线——设备帧仅 47–50 px 差（0.00–0.01%，Roborazzi 比较器吸收故 verify 绿）、dark_dynamic 帧 3.48% 超阈红；即「verify 绿 ≠ 字节级一致」，实验 4 的 2 红 2 绿完全由 diff 幅度解释，无需轨迹因素。
- 环境变量排除：两日 daemon JVM opts 逐项相同（daemon 日志）；nativeruntime 字体目录 per-run 随机新建无共享；worktree/主 checkout、fresh/增量构建、single-use/常驻 daemon 各维对照渲染逐字节一致。

### 9.3 更正后结论

1. **机制 P（跨类渲染历史依赖）不存在**。其唯一立案证据是 §9.1 的参照系错误；同参照系下单类与套件失败集逐用例一致。
2. **渲染 = 确定性函数(代码 + 依赖)，与执行轨迹无关**——比原「确定性函数(执行轨迹)」更强：单类/套件/record/verify/worktree/主 checkout/daemon 形态均不改变渲染字节。
3. 118 FAIL 与 531 张漂移**全部由机制 D（依赖升级渲染漂移）单独解释**；29 张 dark_dynamic 可见变化归因收敛为 Material3 动态暗色上游变更。
4. **基线重录决策（`51e76163`）不受影响**：B 态即当前代码+依赖的真实渲染，且五组实验证明与轨迹无关、稳定可复现。
5. 对 CI 截图 verify spec 的输入更新：**轨迹绑定约束解除**——CI 可单类/单模块/分片 verify，基线录制无须「同轨迹」；roundtrip（record 后即刻 verify）仍作为录制自洽性检查保留。

### 9.4 流程教训

- **record 会直接覆盖 `src/test/screenshots/` 工作区基线**：record 之后、verify 之前必须 `git status` 确认基线状态并显式还原，否则 verify 的参照系静默漂移——本案缺陷即 record2 与单类 verify 之间缺一次还原（20:39:27 对 record1 做过还原，record2 后漏做）。
- **verify PASS 只证明「渲染 == 当前工作区基线」**，不证明与任何 git 版本一致；「与旧 golden 一致」类断言必须落到显式字节比对（PASS 无 actual 产物，无从比对）。
- 推翻昨日结论依据的是**当日命令原文 + daemon 日志 + 产物 mtime 三源时序交叉**——会话 scratchpad 与 transcript 是可考古的一手证据，定性冲突时优先回查原始命令而非重做昨日实验的复述版。
