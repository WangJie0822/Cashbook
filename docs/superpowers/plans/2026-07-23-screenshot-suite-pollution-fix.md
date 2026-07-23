# 截图套件污染修复 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

> **执行勘误（2026-07-23，随 spec 修订 R1）**：Task 1 实测发现 `--rerun-tasks` 不带 `--continue` 时 FAIL 集是截断值（feature/types 未调度），固化命令补 `--continue`；Task 2（L1）因当前态无 IDENTICAL-yet-FAIL 标本而关闭；Task 3 归因产出「机制 D 漂移 + 机制 P 跨类渲染历史依赖」（qualifier 假设被零 DIMENSION_DIFF 证伪）；Task 5 修复候选 A-D 均未启用（roundtrip 闭环绿证明无须改代码）；Task 6 dynamic 定性并入全量漂移报告一体拍板；终态处置 = 全量重录 531 张基线（用户批准，`51e76163`）。完整证据链见 `docs/testing/reports/2026-07-23-screenshot-suite-pollution-investigation.md`。

**Goal:** 修复本机全量 `verifyRoborazziDebug` 的套件级污染（golden 加载失败型 + 渲染型假 FAIL），并定性 `_dynamic` 噪声，使全量 verify 恢复「FAIL 即真回归」的可信语义。

**Architecture:** 调查驱动（spec 方案 A′）：Phase 0 同源命令固化 FAIL 清单（前置门）→ Phase 1 两线机制归因（L1 golden 加载失败型 / L2 渲染型）→ Phase 2 按证据就地定向修复 → Phase 3 `_dynamic` 定性后用户拍板 → Phase 4 验收（基线零改动 + 连续 2 次全量绿）与文档同步。修复候选代码在本 plan 内给全，但**每个候选都有启用条件（Phase 1 证据）**，无证据不得应用。

**Tech Stack:** Roborazzi 1.59.0 + Robolectric（SDK 36 模拟，Java 21 toolchain）+ Compose test（AndroidComposeTestRule）+ Gradle 9 + Python（py 启动器，XML/PNG 分析脚本）。

**Spec:** `docs/superpowers/specs/2026-07-23-screenshot-suite-pollution-fix-design.md`（含节点 1 四维评审结论与全部护栏，实施前必读）。

## Global Constraints

- **禁改**：`ScreenshotHelper.kt:47` `changeThreshold = 0f`；CI 命令与 `.github/**` 任何文件；生产代码（`src/main`，`core/testing` 例外——它是测试基础设施，其 `src/main` 是允许的修复落点）；基线 PNG（Phase 3 用户拍板裁撤除外）。
- **禁用手段**：重录基线（已证伪）；豁免清单；`build-logic` 全局 `withType<Test>` 配置（修复必须就地定向到截图模块或 core:testing）。
- **9 个截图模块**（`git ls-files '*src/test/screenshots/*.png'` 实测）：`core/design`、`core/ui`、`feature/assets`、`feature/books`、`feature/budget`、`feature/records`、`feature/settings`、`feature/tags`、`feature/types`。
- **Gradle 判定纪律**：结果只信 `grep -E '^BUILD (SUCCESSFUL|FAILED)'`；判测试执行只信 `build/test-results/**/*.xml`（`--console=plain` 不打印 PASSED 行）；`--tests` 过滤器配 `verifyRoborazziDebug` task 名不生效（0 执行假绿），单类必须用 `:module:testDebugUnitTest --tests ... -Proborazzi.test.verify=true`。
- **资源纪律**：每次全量 verify 前跑内存预检（可用 <1000MB 或使用率 >90% 中止询问用户）；gradle 一律 `--offline --no-daemon --console=plain`（offline 缺依赖时清继承代理 + `-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897` 暖缓存一次后回 offline）。
- **插桩纪律**：所有诊断插桩只存在于 worktree，合并前对 `core/testing/.../ScreenshotHelper.kt` 与 `build-logic/.../KotlinAndroid.kt` 逐行 `git diff` 核验零残留；分析脚本放 scratchpad 不入库。
- **瓶颈条款**：任一归因线连续 2 轮假设被推翻仍未定位 → 按全局 CLAUDE.md【排查暂停】固定格式输出，等待用户，不继续抛新猜测。
- **工作目录**：worktree `D:/wt/Cashbook/screenshot-pollution-fix`（短路径避 MAX_PATH）。所有 Read/Edit/Write 用 worktree 绝对路径；scratchpad 记为 `$SCRATCH`（会话 scratchpad 目录）。

---

### Task 0: 建 worktree

**Files:** 无代码改动。

**Interfaces:**
- Produces: worktree `D:/wt/Cashbook/screenshot-pollution-fix`，分支 `worktree-screenshot-pollution-fix`，基于本地 main HEAD。

- [ ] **Step 1: 确认主仓库状态并创建 worktree**

```bash
git -C /d/Work/Workspace/Owner/Cashbook status --porcelain
# 预期：空输出（干净）。有未提交变更 → 停下来问用户。
git -C /d/Work/Workspace/Owner/Cashbook worktree add "D:/wt/Cashbook/screenshot-pollution-fix" -b worktree-screenshot-pollution-fix
```

预期输出含 `Preparing worktree (new branch 'worktree-screenshot-pollution-fix')`。手动 `git worktree add` 默认从当前 HEAD（本地 main）派生，无需 rebase。

- [ ] **Step 2: 核验 worktree 落点与 HEAD 一致**

```bash
git -C "D:/wt/Cashbook/screenshot-pollution-fix" log --oneline -1
git -C /d/Work/Workspace/Owner/Cashbook log --oneline -1
# 预期：两个 hash 相同。
```

- [ ] **Step 3: 补 worktree 本地配置（不入库文件不随 checkout）**

```bash
ls "D:/wt/Cashbook/screenshot-pollution-fix/local.properties" 2>/dev/null || cp /d/Work/Workspace/Owner/Cashbook/local.properties "D:/wt/Cashbook/screenshot-pollution-fix/local.properties" 2>/dev/null; echo done
```

（Cashbook 的 local.properties 若主 checkout 也没有则跳过，`echo done` 只为命令不因 cp 缺源失败。）

---

### Task 1: Phase 0 复现固化（前置门）

**Files:**
- Create: `$SCRATCH/extract_fails.py`（分析脚本，不入库）
- Create: `$SCRATCH/phase0/run1.txt`、`run2.txt`、`serial.txt`（FAIL 清单）

**Interfaces:**
- Produces: 稳定 FAIL 清单（`fail_list.md`：非 dynamic / dynamic 分列 + 两次一致性 + 串行对照结论），供 Task 2/3/5 引用。
- **GATE**：清单稳定 → 继续；当前 HEAD 全绿 → 中止上报用户重议（spec §4 Phase 0 门）。

- [ ] **Step 1: 内存预检**

```powershell
$os=Get-CimInstance Win32_OperatingSystem
"Avail: {0:N0}MB  Used%: {1:N1}" -f ($os.FreePhysicalMemory/1024), ((1-$os.FreePhysicalMemory/$os.TotalVisibleMemorySize)*100)
```

可用 <1000MB 或 >90% → 中止，按 CLAUDE.local.md 建议清理后再跑。

- [ ] **Step 2: 全量 verify 第 1 次（后台跑，判定只看日志）**

```bash
cd "D:/wt/Cashbook/screenshot-pollution-fix" && ./gradlew verifyRoborazziDebug --rerun-tasks --offline --no-daemon --console=plain > "$SCRATCH/phase0/verify1.log" 2>&1
```

`--rerun-tasks` 是必须的：不带它时 PASS 模块 UP-TO-DATE 跳过，两次跑的「一致性」是假的。判定：

```bash
grep -E '^BUILD (SUCCESSFUL|FAILED)' "$SCRATCH/phase0/verify1.log" && tail -1 "$SCRATCH/phase0/verify1.log"
# 预期：BUILD FAILED（有污染时）；末行为 "N actionable tasks: ..."。日志截断停在中途 task = 未完成，勿判定。
```

- [ ] **Step 3: 写 FAIL 提取脚本并跑第 1 次提取**

`$SCRATCH/extract_fails.py`（完整代码）：

```python
import sys, os, io, re, xml.etree.ElementTree as ET

root = sys.argv[1]  # worktree 根
out = []
for dirpath, dirs, files in os.walk(root):
    if os.sep + "build" + os.sep in dirpath and dirpath.endswith(os.path.join("test-results", "testDebugUnitTest")):
        for f in files:
            if not (f.startswith("TEST-") and f.endswith(".xml")):
                continue
            p = os.path.join(dirpath, f)
            try:
                tree = ET.parse(p)
            except ET.ParseError:
                out.append(("PARSE_ERROR", p, "", ""))
                continue
            for case in tree.getroot().iter("testcase"):
                for kind in ("failure", "error"):
                    node = case.find(kind)
                    if node is not None:
                        msg = (node.get("message") or (node.text or "")).strip()
                        # Roborazzi 失败消息含截图文件路径，提取用于 dynamic 分类
                        pngs = re.findall(r"[\w/\\.-]+\.png", msg)
                        out.append((
                            case.get("classname", ""),
                            case.get("name", ""),
                            msg.splitlines()[0][:300] if msg else "",
                            ";".join(pngs)[:500],
                        ))
out.sort()
buf = io.StringIO()
for row in out:
    buf.write("\t".join(row) + "\n")
buf.write(f"# TOTAL_FAILED_CASES: {len(out)}\n")
dyn = sum(1 for r in out if "_dynamic.png" in r[3])
buf.write(f"# CASES_TOUCHING_DYNAMIC_PNG: {dyn}\n")
sys.stdout.write(buf.getvalue())
```

运行：

```bash
py "$SCRATCH/extract_fails.py" "D:/wt/Cashbook/screenshot-pollution-fix" > "$SCRATCH/phase0/run1.txt"
tail -3 "$SCRATCH/phase0/run1.txt"
```

预期：`TOTAL_FAILED_CASES` 约 21+dynamic 若干（2026-07-23 参考值，允许漂移——这正是本 Task 要固化的）。

- [ ] **Step 4: 全量 verify 第 2 次 + 提取**

```bash
cd "D:/wt/Cashbook/screenshot-pollution-fix" && ./gradlew verifyRoborazziDebug --rerun-tasks --offline --no-daemon --console=plain > "$SCRATCH/phase0/verify2.log" 2>&1
grep -E '^BUILD (SUCCESSFUL|FAILED)' "$SCRATCH/phase0/verify2.log"
py "$SCRATCH/extract_fails.py" "D:/wt/Cashbook/screenshot-pollution-fix" > "$SCRATCH/phase0/run2.txt"
diff "$SCRATCH/phase0/run1.txt" "$SCRATCH/phase0/run2.txt" && echo IDENTICAL_FAIL_SET || echo "FAIL_SET_DIFFERS(如上)"
```

记录：两次 FAIL 集是否完全一致；不一致则记录差异子集（「不稳定 FAIL」单列，归因时区别对待）。

- [ ] **Step 5: 串行对照（`--max-workers=1`）**

```bash
cd "D:/wt/Cashbook/screenshot-pollution-fix" && ./gradlew verifyRoborazziDebug --rerun-tasks --max-workers=1 --offline --no-daemon --console=plain > "$SCRATCH/phase0/verify-serial.log" 2>&1
grep -E '^BUILD (SUCCESSFUL|FAILED)' "$SCRATCH/phase0/verify-serial.log"
py "$SCRATCH/extract_fails.py" "D:/wt/Cashbook/screenshot-pollution-fix" > "$SCRATCH/phase0/serial.txt"
diff "$SCRATCH/phase0/run2.txt" "$SCRATCH/phase0/serial.txt" && echo SAME_AS_PARALLEL || echo SERIAL_DIFFERS
```

判读：串行下 FAIL 消失/减少 → 根因含跨模块并发（假设池收窄到并发 I/O/资源竞争）；不变 → 并发排除，聚焦模块内机制。

- [ ] **Step 6: 固化清单 + GATE 判定**

汇总 `$SCRATCH/phase0/fail_list.md`：非 dynamic FAIL 清单（classname+testname+消息首行+涉及 png）/ dynamic FAIL 清单 / 两次一致性 / 串行对照结论。

- FAIL 集稳定（或稳定子集非空）→ 继续 Task 2。
- **全量两次全绿** → 中止：向用户报告「当前 HEAD 症状已消失（依赖态变化），spec Phase 0 门触发，重议范围」。不进入后续 Task。

- [ ] **Step 7: Commit（仅固化报告快照，插桩前基线）**

无代码改动则跳过 commit；把 `fail_list.md` 留 scratchpad（Phase 4 并入调查报告时才入库）。

---

### Task 2: Phase 1-L1 golden 加载失败型归因

**Files:**
- Create: `$SCRATCH/phase1/l1-signature.md`（失败消息签名分析）
- Modify（临时插桩，最终不入库）: 无——L1 优先用零插桩手段

**Interfaces:**
- Consumes: Task 1 `fail_list.md`。
- Produces: L1 机制结论（`$SCRATCH/phase1/l1-conclusion.md`：失败发生在 compare 链的哪个环节 + 一手证据），Task 5 修复的启用依据。

- [ ] **Step 1: 失败消息签名分类（零成本，用 Phase 0 已有 XML）**

对 `fail_list.md` 中每条非 dynamic FAIL 的消息全文（回 XML 取完整 `<failure>` 文本）分类：

```bash
grep -rl "SettingScreenScreenshotTests" "D:/wt/Cashbook/screenshot-pollution-fix/feature/settings/build/test-results/testDebugUnitTest/" | head -3
# 然后对每个 XML 用 Read 工具看 <failure> 完整栈
```

分类维度：
- `AssertionError: Screenshot is changed`（Roborazzi 正常 diff 判定）→ 比对器认为像素变了
- golden 文件不存在/读取异常（`FileNotFoundException`/`IOException`/解码异常栈）→ 加载失败
- `OutOfMemoryError` → 内存
- 其他异常栈 → 单列

记录到 `l1-signature.md`：**IDENTICAL-yet-FAIL 的 10 对（对照 2026-07-23 清单与本轮 Phase 0 清单交集）落在哪一类**。这一步大概率直接锁定环节。

- [ ] **Step 2: 探针自证——人为破坏 golden 取「加载失败」签名**

```bash
cd "D:/wt/Cashbook/screenshot-pollution-fix"
cp feature/settings/src/test/screenshots/SettingScreen/SettingScreen_light_defaultTheme_notDynamic.png "$SCRATCH/phase1/golden-backup.png"
# 截断 golden 制造解码失败
head -c 100 "$SCRATCH/phase1/golden-backup.png" > feature/settings/src/test/screenshots/SettingScreen/SettingScreen_light_defaultTheme_notDynamic.png
./gradlew :feature:settings:testDebugUnitTest --tests "*.SettingScreenScreenshotTests" -Proborazzi.test.verify=true --rerun-tasks --offline --no-daemon --console=plain > "$SCRATCH/phase1/probe-broken-golden.log" 2>&1
grep -E '^BUILD (SUCCESSFUL|FAILED)' "$SCRATCH/phase1/probe-broken-golden.log"
# 取该 run XML 的失败消息 → 这就是「golden 加载失败」的判别签名
# 立即还原：
cp "$SCRATCH/phase1/golden-backup.png" feature/settings/src/test/screenshots/SettingScreen/SettingScreen_light_defaultTheme_notDynamic.png
git -C "D:/wt/Cashbook/screenshot-pollution-fix" status --porcelain feature/settings/src/test/screenshots/
# 预期：空（还原干净）
```

预期：探针 run BUILD FAILED 且失败消息形态可与 Step 1 分类对照——若套件 FAIL 的签名与「broken golden」签名一致 → 证实加载失败型；若都是 `Screenshot is changed` → 加载失败假设被削弱，失败在比对判定环节（转 Step 3）。

- [ ] **Step 3: fork JVM 堆实测（只在 Step 1/2 指向内存/资源时执行）**

```bash
cd "D:/wt/Cashbook/screenshot-pollution-fix" && ./gradlew :feature:settings:testDebugUnitTest -Proborazzi.test.verify=true --rerun-tasks --offline --no-daemon --info 2>&1 | grep -iE "Starting process.*Gradle Test Executor" | head -2 > "$SCRATCH/phase1/fork-cmdline.txt"
cat "$SCRATCH/phase1/fork-cmdline.txt"
# 读 fork 命令行中的 -Xmx 值；无 -Xmx = JVM ergonomic 默认。记录实际值，替换 spec 中【推测】。
```

- [ ] **Step 4: 结论落盘**

`$SCRATCH/phase1/l1-conclusion.md`：机制结论 + 证据（签名对照表、探针 run 引用、堆实测值）。无法定位且已连续 2 轮假设被推翻 → 按【排查暂停】格式输出并停。

---

### Task 3: Phase 1-L2 渲染型归因（qualifier 泄漏假设优先）

**Files:**
- Modify（**临时插桩**，Task 7 前撤除）: `core/testing/src/main/kotlin/cn/wj/android/cashbook/core/testing/util/ScreenshotHelper.kt`
- Create: `$SCRATCH/phase1/l2-conclusion.md`

**Interfaces:**
- Consumes: Task 1 `fail_list.md`（渲染型子集：actual 与 golden 真 diff、缺条目类）。
- Produces: L2 机制结论，Task 5 修复启用依据。

- [ ] **Step 1: 加临时 qualifier 探针（worktree 内，标记 TEMP-PROBE）**

`ScreenshotHelper.kt` 两处 capture 前插入（完整 diff）：

```kotlin
// captureForDevice 内，RuntimeEnvironment.setQualifiers(...) 之前加：
println("TEMP-PROBE captureForDevice BEFORE qualifiers=${RuntimeEnvironment.getQualifiers()}")

// captureMultiTheme 内，this.setContent { 之前加：
println("TEMP-PROBE captureMultiTheme qualifiers=${RuntimeEnvironment.getQualifiers()}")
```

（`RuntimeEnvironment` 已 import；println 走测试 stdout，进 test-results XML 的 system-out。）

- [ ] **Step 2: 单类跑取「健康态」qualifier 基准**

```bash
cd "D:/wt/Cashbook/screenshot-pollution-fix" && ./gradlew :feature:settings:testDebugUnitTest --tests "*.SettingScreenScreenshotTests" -Proborazzi.test.verify=true --rerun-tasks --offline --no-daemon --console=plain > "$SCRATCH/phase1/l2-single.log" 2>&1
grep -E '^BUILD (SUCCESSFUL|FAILED)' "$SCRATCH/phase1/l2-single.log"
grep -rh "TEMP-PROBE" "D:/wt/Cashbook/screenshot-pollution-fix/feature/settings/build/test-results/testDebugUnitTest/"*.xml | sort -u > "$SCRATCH/phase1/l2-qualifiers-single.txt"
cat "$SCRATCH/phase1/l2-qualifiers-single.txt"
```

预期：captureMultiTheme 的 qualifier 为该测试类默认配置值（记录之）。

- [ ] **Step 3: 模块级复现尝试（缩短环路的关键）**

```bash
cd "D:/wt/Cashbook/screenshot-pollution-fix" && ./gradlew :feature:settings:testDebugUnitTest -Proborazzi.test.verify=true --rerun-tasks --offline --no-daemon --console=plain > "$SCRATCH/phase1/l2-module.log" 2>&1
grep -E '^BUILD (SUCCESSFUL|FAILED)' "$SCRATCH/phase1/l2-module.log"
py "$SCRATCH/extract_fails.py" "D:/wt/Cashbook/screenshot-pollution-fix" | grep -i setting
```

判读：
- 模块级复现 FAIL → 迭代环路缩为单模块（分钟级），继续 Step 4。
- 模块级全 PASS → 污染需要跨模块套件才触发；后续对照改跑全量（慢环路），并把「跨模块才触发」记为机制线索（与 Task 1 Step 5 串行对照互证）。

- [ ] **Step 4: 失败态 qualifier 对照**

在能复现 FAIL 的粒度（模块级或全量）跑一次，收全部 TEMP-PROBE 行：

```bash
grep -rh "TEMP-PROBE" "D:/wt/Cashbook/screenshot-pollution-fix"/*/build/test-results/testDebugUnitTest/*.xml "D:/wt/Cashbook/screenshot-pollution-fix"/*/*/build/test-results/testDebugUnitTest/*.xml 2>/dev/null | sort | uniq -c > "$SCRATCH/phase1/l2-qualifiers-suite.txt"
```

判读：
- FAIL 用例的 captureMultiTheme qualifier ≠ 单类基准（如残留 `w1280dp-h800dp` tablet 值）→ **qualifier 泄漏证实**，且解释「缺底部条目」（视口高度变化）。
- qualifier 一致仍 FAIL → 假设证伪，转 Step 5 备选假设。

- [ ] **Step 5: 备选假设（仅 Step 4 证伪后执行）——Compose idle 时序**

在 captureMultiTheme 的 `dynamicThemingValues.forEach` 内、`captureRoboImage` 前临时加 `this.waitForIdle()`，重跑失败粒度：FAIL 消失 → idle 时序证实；不消失 → 按【排查暂停】条款输出（两轮假设已尽）。

- [ ] **Step 6: 结论落盘**

`$SCRATCH/phase1/l2-conclusion.md`：证实/证伪矩阵 + 探针输出引用。**探针本步不撤**（Task 5 修复验证还要用），Task 7 统一撤。

---

### Task 4: CHECKPOINT——归因结论呈报

**Files:** 无。

- [ ] **Step 1: 向用户呈报 L1/L2 机制结论 + 拟启用的修复候选（下 Task 清单中选中项），等确认后进 Task 5**

呈报格式：每条结论附一手证据引用（签名对照/探针输出/对照实验 log 路径）。这是 spec「变更前确认」的强制点：修复方案此刻才与证据绑定。

---

### Task 5: Phase 2 按证据修复（候选池，逐项凭证据启用）

**Files:**
- Modify: `core/testing/src/main/kotlin/cn/wj/android/cashbook/core/testing/util/ScreenshotHelper.kt`（候选 A/B）
- Modify: 9 个截图模块 `*/build.gradle.kts`（候选 C，仅 OOM 证据下）
- Test: 修复自证靠「修前红→修后绿→变异复红」，不新增测试文件

**Interfaces:**
- Consumes: Task 2/3 机制结论 + Task 4 用户确认。
- Produces: 修复 commit（每个候选独立 commit，信息注明证据依据）。

**候选 A：qualifier 快照复位（启用条件：Task 3 Step 4 证实泄漏）**

`captureForDevice` 改为（完整代码，替换 `ScreenshotHelper.kt:66-95` 函数体）：

```kotlin
fun <A : ComponentActivity> AndroidComposeTestRule<ActivityScenarioRule<A>, A>.captureForDevice(
    deviceName: String,
    deviceSpec: String,
    screenshotName: String,
    roborazziOptions: RoborazziOptions = DefaultRoborazziOptions,
    darkMode: Boolean = false,
    body: @Composable () -> Unit,
) {
    val (width, height, dpi) = extractSpecs(deviceSpec)

    // Set qualifiers from specs; snapshot first so suite-mode runs don't leak
    // device qualifiers into later captures (suite pollution root cause, see
    // docs/superpowers/specs/2026-07-23-screenshot-suite-pollution-fix-design.md)
    val originalQualifiers = RuntimeEnvironment.getQualifiers()
    RuntimeEnvironment.setQualifiers("w${width}dp-h${height}dp-${dpi}dpi")

    try {
        this.activity.setContent {
            CompositionLocalProvider(
                LocalInspectionMode provides true,
                LocalDefaultLoadingHint provides "数据加载中",
                LocalDefaultEmptyImagePainter provides ColorPainter(Color.Gray),
            ) {
                TestHarness(darkMode = darkMode) {
                    body()
                }
            }
        }
        this.onRoot()
            .captureRoboImage(
                "src/test/screenshots/${screenshotName}_$deviceName.png",
                roborazziOptions = roborazziOptions,
            )
    } finally {
        RuntimeEnvironment.setQualifiers(originalQualifiers)
    }
}
```

注意：`RuntimeEnvironment.getQualifiers()` 返回的字符串可直接回喂 `setQualifiers`（同 API 对偶）；若实测回喂抛异常（qualifier 串格式不被接受），fallback 为记录并复位到探针实测的类默认值字符串——以 Task 3 实测为准，勿凭假设改。

**候选 B：capture 前显式 idle + 前置自证（启用条件：Task 3 Step 5 证实 idle 时序）**

`captureMultiTheme` 的 `dynamicThemingValues.forEach` 内 `captureRoboImage` 前加：

```kotlin
dynamicTheming = isDynamicTheming
val dynamicThemingDesc = if (isDynamicTheming) "dynamic" else "notDynamic"

// Drive composition to a settled frame before capture; PAUSED-looper suite
// runs may otherwise capture a stale frame (see 2026-07-23 pollution spec)
this.waitForIdle()
```

前置自证：修复 commit 前，用 Task 3 复现粒度验证「不加 waitForIdle 红、加了绿」，即 waitForIdle 生效性由复现对照证明（通用 helper 无法断言具体业务节点，以对照代替内容断言）。

**候选 C：定向堆调整（启用条件：Task 2 实证 OOM/内存签名 + 堆实测值偏低）**

在**证据涉及的截图模块**（而非全部 9 个）`build.gradle.kts` 末尾就地添加（参照 `core/testing/build.gradle.kts` 的 `failOnNoDiscoveredTests` 就地先例）：

```kotlin
// 截图套件 golden 解码内存实证不足（见 2026-07-23 套件污染 spec Task 2 证据），就地提升测试 fork 堆。
// 勿移入 build-logic 全局（会作用于所有模块并传导 CI）。
tasks.withType<Test>().configureEach {
    maxHeapSize = "1g"
}
```

CI 传导核算写入 commit message：ci-gradle.properties `workers.max=2`，并发峰值 daemon 4g + 2×1g = 6g < ubuntu runner 内存。

**候选 D：定向隔离（启用条件：类间泄漏证实且源头复位 A 不可行）**——先在单模块实测 `forkEvery = 1` 时长增量，超过该模块原时长 3 倍则回 Task 4 重议。

- [ ] **Step 1: 按 Task 4 确认清单应用选中候选（每候选独立改动）**

- [ ] **Step 2: 修复自证——复现粒度对照**

```bash
# 修后：Task 3 确定的复现粒度（模块级或全量）跑一次
# 预期：原 FAIL 用例转绿（XML 无 failure）
# 变异验证：git stash 修复（或临时反注释）重跑 → FAIL 复活 → git stash pop
```

变异验证必须真实执行并记录 log 路径；「全绿」不证明修复有效，「撤掉修复复红」才证明。

- [ ] **Step 3: 同构造全扫**

```bash
grep -rln "captureForDevice\|captureMultiDevice\|captureMultiTheme" "D:/wt/Cashbook/screenshot-pollution-fix"/*/src/test "D:/wt/Cashbook/screenshot-pollution-fix"/*/*/src/test 2>/dev/null | wc -l
# 与 54 个截图测试文件对照：候选 A/B 落在共享 helper，天然覆盖全部调用点；
# 若发现测试文件内有绕过 helper 直调 RuntimeEnvironment.setQualifiers / captureRoboImage 的，逐个列出评估是否同病。
grep -rln "RuntimeEnvironment.setQualifiers" "D:/wt/Cashbook/screenshot-pollution-fix"/*/src/test "D:/wt/Cashbook/screenshot-pollution-fix"/*/*/src/test 2>/dev/null
```

- [ ] **Step 4: Commit（每候选一笔）**

```bash
git -C "D:/wt/Cashbook/screenshot-pollution-fix" add core/testing/src/main/kotlin/cn/wj/android/cashbook/core/testing/util/ScreenshotHelper.kt
git -C "D:/wt/Cashbook/screenshot-pollution-fix" commit -m "[fix|test|截图verify][公共]<按实际候选填写：如 captureForDevice qualifier 快照复位消套件泄漏（证据：Task3 探针对照 log）>"
```

commit 前核验 diff 无 TEMP-PROBE 行混入（探针与修复分离——探针 Task 7 才撤，本笔只含修复；用 `git add -p` 或先撤探针再改亦可，取实施时更稳的一种并记录）。

---

### Task 6: Phase 3 `_dynamic` 定性与处置（含 CHECKPOINT）

**Files:**
- Create: `$SCRATCH/phase3/png_diff.py`（纯 Python PNG 像素 diff，不入库）
- Modify（仅用户拍板裁撤/降级分支）: 相关测试文件 + 基线删除

**Interfaces:**
- Consumes: Task 1 dynamic FAIL 清单；Task 5 修复后的套件态（先重跑一次全量，看 dynamic FAIL 是否随污染修复消失——若全消失，本 Task 只剩记录结论）。
- Produces: `_dynamic` 定性结论 + 用户拍板记录 + （若裁撤/降级）实施 commit。

- [ ] **Step 1: 修复后全量重跑，取 dynamic FAIL 残集**

```bash
cd "D:/wt/Cashbook/screenshot-pollution-fix" && ./gradlew verifyRoborazziDebug --rerun-tasks --offline --no-daemon --console=plain > "$SCRATCH/phase3/verify-postfix.log" 2>&1
grep -E '^BUILD (SUCCESSFUL|FAILED)' "$SCRATCH/phase3/verify-postfix.log"
py "$SCRATCH/extract_fails.py" "D:/wt/Cashbook/screenshot-pollution-fix" > "$SCRATCH/phase3/postfix.txt"
```

dynamic FAIL 归零 → 定性「同为套件污染受害者」，跳到 Step 4 记录结论（拍板项退化为确认）。仍有残集 → Step 2。

- [ ] **Step 2: 逐字节/逐像素定性**

`$SCRATCH/phase3/png_diff.py`（完整代码，零依赖）：

```python
import sys, zlib, struct

def read_png(path):
    d = open(path, "rb").read()
    assert d[:8] == b"\x89PNG\r\n\x1a\n", path
    pos, idat, w, h, bd, ct = 8, b"", 0, 0, 0, 0
    while pos < len(d):
        ln, typ = struct.unpack(">I4s", d[pos:pos + 8])
        chunk = d[pos + 8:pos + 8 + ln]
        if typ == b"IHDR":
            w, h, bd, ct = struct.unpack(">IIBB", chunk[:10])
        elif typ == b"IDAT":
            idat += chunk
        pos += 12 + ln
    assert bd == 8, f"bit depth {bd} unsupported"
    ch = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ct]
    raw = zlib.decompress(idat)
    stride = w * ch
    out, prev = bytearray(), bytearray(stride)
    p = 0
    for _ in range(h):
        f = raw[p]; line = bytearray(raw[p + 1:p + 1 + stride]); p += 1 + stride
        if f == 1:
            for i in range(ch, stride): line[i] = (line[i] + line[i - ch]) & 255
        elif f == 2:
            for i in range(stride): line[i] = (line[i] + prev[i]) & 255
        elif f == 3:
            for i in range(stride):
                a = line[i - ch] if i >= ch else 0
                line[i] = (line[i] + ((a + prev[i]) >> 1)) & 255
        elif f == 4:
            for i in range(stride):
                a = line[i - ch] if i >= ch else 0
                b = prev[i]; c = prev[i - ch] if i >= ch else 0
                pp = a + b - c
                pa, pb, pc = abs(pp - a), abs(pp - b), abs(pp - c)
                pred = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[i] = (line[i] + pred) & 255
        out += line; prev = line
    return w, h, ch, bytes(out)

a, b = sys.argv[1], sys.argv[2]
raw_same = open(a, "rb").read() == open(b, "rb").read()
wa, ha, ca, pa_ = read_png(a)
wb, hb, cb, pb_ = read_png(b)
if (wa, ha, ca) != (wb, hb, cb):
    print(f"DIMENSION_DIFF {wa}x{ha}x{ca} vs {wb}x{hb}x{cb}"); sys.exit(0)
diff = sum(1 for i in range(0, len(pa_), ca) if pa_[i:i + ca] != pb_[i:i + ca])
total = wa * ha
print(f"bytes_identical={raw_same} pixels_diff={diff}/{total} ({diff * 100.0 / total:.2f}%)")
```

对残集每对跑 `py png_diff.py <actual> <golden>`，分类：`bytes_identical=True` 仍 FAIL（加载型残留，回 Task 2 线）/ 像素 diff 集中且小比例（局部渲染差）/ 大面积色差（动态取色不确定）。两次跑（Step 1 再重跑一次）diff 是否复现一致。

- [ ] **Step 3: CHECKPOINT——三选一带证据请用户拍板**

按 spec §4 Phase 3 三选项呈报（证据：分类表 + 复现一致性）。等待拍板。

- [ ] **Step 4: 按拍板实施**

- 选项 1（修复保留）：回 Task 5 流程追加对应修复。
- 选项 2（降级验证）：对涉及测试调用点改 `shouldCompareDynamicColor = false`（逐文件列改动清单呈报），同 commit 删除对应 `_dynamic` 基线（护栏 3 流程）。
- 选项 3（裁撤）：
```bash
cd "D:/wt/Cashbook/screenshot-pollution-fix"
git ls-files '*/src/test/screenshots/*.png' | grep -E '_dynamic\.png$' > "$SCRATCH/phase3/to-remove.txt"
wc -l "$SCRATCH/phase3/to-remove.txt"   # 必须 = 260，不等即停
grep -cE '_notDynamic\.png$' "$SCRATCH/phase3/to-remove.txt"   # 必须 = 0
xargs -a "$SCRATCH/phase3/to-remove.txt" git rm --
git status --porcelain | grep -c '^D '   # 必须 = 260
```
测试参数变更（`shouldCompareDynamicColor=false`）与基线删除**同一 commit**。

---

### Task 7: Phase 4 验收与同步

**Files:**
- Modify（撤探针）: `core/testing/.../ScreenshotHelper.kt`
- Create: `docs/testing/reports/2026-07-XX-screenshot-suite-pollution-investigation.md`（XX=实施日）
- Modify: `CLAUDE.md`（3 处）、`docs/superpowers/specs/2026-07-09-ci-feature-core-unit-test-coverage-design.md`（前置条件回填）

**Interfaces:**
- Consumes: 全部前序产物。
- Produces: 验收证据 + 文档同步 commit + 合并就绪的分支。

- [ ] **Step 1: 撤除全部 TEMP-PROBE 插桩 + choke point 零残留核验**

```bash
grep -rn "TEMP-PROBE" "D:/wt/Cashbook/screenshot-pollution-fix/core/testing/" && echo "残留！撤干净再继续" || echo CLEAN
git -C "D:/wt/Cashbook/screenshot-pollution-fix" diff main -- core/testing/src/main/kotlin/cn/wj/android/cashbook/core/testing/util/ScreenshotHelper.kt build-logic/convention/src/main/kotlin/cn/wj/android/cashbook/buildlogic/KotlinAndroid.kt
# 逐行人读：只允许出现 Task 5 已确认的修复行，KotlinAndroid.kt 必须零 diff
```

- [ ] **Step 2: 验收 run×2 + 基线零改动**

```bash
cd "D:/wt/Cashbook/screenshot-pollution-fix"
./gradlew verifyRoborazziDebug --rerun-tasks --offline --no-daemon --console=plain > "$SCRATCH/phase4/accept1.log" 2>&1
grep -E '^BUILD (SUCCESSFUL|FAILED)' "$SCRATCH/phase4/accept1.log"   # 预期 SUCCESSFUL
./gradlew verifyRoborazziDebug --rerun-tasks --offline --no-daemon --console=plain > "$SCRATCH/phase4/accept2.log" 2>&1
grep -E '^BUILD (SUCCESSFUL|FAILED)' "$SCRATCH/phase4/accept2.log"   # 预期 SUCCESSFUL
git status --porcelain -- '*/src/test/screenshots/'   # 预期：空（Phase 3 裁撤分支除外，此时应只有已 commit 的删除、无未跟踪改动）
```

单类抽查一致性：

```bash
./gradlew :feature:settings:testDebugUnitTest --tests "*.SettingScreenScreenshotTests" -Proborazzi.test.verify=true --rerun-tasks --offline --no-daemon --console=plain > "$SCRATCH/phase4/spot.log" 2>&1
grep -E '^BUILD (SUCCESSFUL|FAILED)' "$SCRATCH/phase4/spot.log"   # 预期 SUCCESSFUL，与全量结论一致
```

其他常规回归（修复涉及 core:testing 主源集，须证不破单测编译链）：

```bash
./gradlew testDebugUnitTest :core:model:test -Proborazzi.test.verify=false --offline --no-daemon --console=plain > "$SCRATCH/phase4/unit.log" 2>&1
grep -E '^BUILD (SUCCESSFUL|FAILED)' "$SCRATCH/phase4/unit.log"   # 预期 SUCCESSFUL
./gradlew spotlessCheck --init-script gradle/init.gradle.kts --no-configuration-cache > "$SCRATCH/phase4/spotless.log" 2>&1
grep -E '^BUILD (SUCCESSFUL|FAILED)' "$SCRATCH/phase4/spotless.log"   # 预期 SUCCESSFUL
```

- [ ] **Step 3: 调查报告入库**

`docs/testing/reports/2026-07-XX-screenshot-suite-pollution-investigation.md`：Phase 0 固化清单 → L1/L2 机制结论与证据链 → 修复清单（含变异验证记录）→ dynamic 定性与拍板 → 验收数据。commit：`[test|截图verify][公共]套件污染调查报告`。

- [ ] **Step 4: CLAUDE.md 3 处同步 + 2026-07-09 spec 回填**

1. 「全量 verify 批量 FAIL 判别口径③」条款：改为「套件污染已修复（见 2026-07-23 spec/报告），全量 verify FAIL 即真问题；单类对照手法保留为诊断工具」。
2. 「_dynamic 变体可能报 changed 勿据此判基线错」免责：按 Task 6 拍板结果改写（修复→删免责；降级/裁撤→写明新边界与判据）。
3. 截图测试命令节：补「本机全量 verify 已可信」一句 + 指向报告。
4. `2026-07-09-ci-feature-core-unit-test-coverage-design.md` 前置条件节：标注「套件污染已由 2026-07-23 spec 解决」，其余 H1/H2/M1/M2/M3/L1/L-E 仍待 CI spec。

commit：`[docs|CLAUDE.md|截图verify][公共]套件污染修复后条款同步(判别口径③/dynamic免责/spec前置回填)`。

- [ ] **Step 5: 收尾**

调用 superpowers:finishing-a-development-branch 决定合并方式（预期：worktree 分支合回本地 main + worktree 清理；push 时机用户掌控）。合并前触发节点 2 评审（改动规模预计 <50 行代码 + 文档 → 两维快审档位，由 controller 按「Agent Team 评审」章执行）。

---

## Self-Review 记录

1. **Spec 覆盖**：Phase 0→Task 1；Phase 1 L1→Task 2、L2→Task 3；Phase 2→Task 5（候选 A-D 均给完整代码与启用条件）；Phase 3→Task 6（三分支均有实施步骤）；Phase 4→Task 7（验收三条+同步四项）；护栏 1-8 分布于 Global Constraints 与各 Task 步骤；瓶颈条款、探针自证（Task 2 Step 2）、修复变异验证（Task 5 Step 2）、choke point 零残留（Task 7 Step 1）均落位。
2. **占位符扫描**：Task 5 commit message 含「按实际候选填写」——这是证据绑定型任务的固有形态（修复内容由 Phase 1 证据决定），已用候选池+启用条件闭合，不属 TBD。报告文件名 `2026-07-XX` 由实施日替换。
3. **类型/命名一致性**：`extract_fails.py`/`png_diff.py` 在 Task 1/6 定义、Task 2-7 引用同名；`TEMP-PROBE` 标记贯穿 Task 3/5/7；复现粒度（模块级/全量）在 Task 3 Step 3 确定、Task 5 Step 2 与 Task 6 引用同一定义。
