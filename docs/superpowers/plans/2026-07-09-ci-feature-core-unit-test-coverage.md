# CI 覆盖 feature/core 单元测试 + CLAUDE.md 截图认知纠正 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans（本任务 controller 亲自 inline 串行）。Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 CI 执行 feature/core 模块 JVM 单测（当前只 app），并纠正项目 CLAUDE.md 中关于 CI 管理截图基线的 4 处错误认知；截图 verify 上 CI 解耦为独立后续 spec。

**Architecture:** 改 `.github/workflows/Build.yaml` 的 `Run local tests` step，加无前缀聚合 task `testDebugUnitTest`（21 库模块）+ `:core:model:test`（JVM 库）+ `-Proborazzi.test.verify=false`（截图模块只跑逻辑不 capture/verify）。库单测首次在 UTC CI 执行前，本机以 UTC/en 环境预筛暴露并修时区/locale 脆弱测试。CLAUDE.md 4 处截图条款改为实情。

**Tech Stack:** GitHub Actions（ubuntu-latest）、Gradle（AGP9/Gradle9/JDK17 源码、CI JDK21）、Roborazzi 1.59.0、JUnit4 + Truth、Robolectric。

## Global Constraints

- 本轮**只做单测覆盖上 CI + CLAUDE.md 4 处纠正**；截图 verify 上 CI **不做**（解耦独立后续 spec，前置加固条件见 spec「截图 verify 独立 spec 前置条件」）。
- 单测命令必须带 `-Proborazzi.test.verify=false`（M4：避免 9 截图模块冗余 capture + 与后续截图 verify step 双跑）。
- 项目 CLAUDE.md 是团队分发文档，**不写 vault `[[...]]` 引用**。
- 本机 Gradle：`--no-daemon --offline --console=plain`；跑前查内存（可用 <1000MB 或使用率 >90% 中止）；判 BUILD 只信 `grep -E '^BUILD (SUCCESSFUL|FAILED)'`。
- spotless：`--init-script gradle/init.gradle.kts --no-configuration-cache`。
- app 整体编译验证用 `:app:compileOnlineDebugKotlin`（有 flavor，`compileDebugKotlin` 歧义）。
- 工作目录：worktree `D:/wt/Cashbook/ci-coverage-fix`（分支 worktree-ci-coverage-fix，base main @ 90534855）。controller Read/Edit/Write 用 worktree 绝对路径。

---

## Task 1: M5 时区/locale 预筛（暴露脆弱测试）

**Files:**
- 只读跑测试，无文件改动（本 task 产出为脆弱测试清单）

**目标**：确定本机可靠的 UTC/en 注入方式，跑全库单测，输出「哪些测试在 UTC/en 下失败」清单，驱动 Task 2。

- [ ] **Step 1: 查内存**

Run: `powershell.exe -Command "$os=Get-CimInstance Win32_OperatingSystem; 'Avail: {0:N0}MB Used%: {1:N1}' -f ($os.FreePhysicalMemory/1024), ((1-$os.FreePhysicalMemory/$os.TotalVisibleMemorySize)*100)"`
Expected: 可用 >1000MB 且使用率 <90%，否则中止先腾内存。

- [ ] **Step 2: 验证 TZ 环境变量能否注入 test fork JVM**

先小范围验证注入生效（core:model 是 JVM 库、快）：
Run: `cd D:/wt/Cashbook/ci-coverage-fix && TZ=UTC LANG=en_US.UTF-8 ./gradlew :core:model:test --no-daemon --offline --console=plain 2>&1 | grep -E '^BUILD (SUCCESSFUL|FAILED)'`
Expected: BUILD SUCCESSFUL（core:model 无时区依赖应过）。
若要确认 TZ 真作用于 test fork：临时在任一 test 里 `println(java.util.TimeZone.getDefault().id)` 不可行（改代码）；改为信任 Java 读 `TZ` 环境变量的标准行为（POSIX + Windows JVM 均读），并以 Step 3 全库跑的失败模式反推是否生效——若某时区敏感测试在 `TZ=UTC` 下失败而默认（CST）下通过，即证明注入生效。

- [ ] **Step 3: UTC/en 预筛全库单测**

Run:
```
cd D:/wt/Cashbook/ci-coverage-fix && TZ=UTC LANG=en_US.UTF-8 ./gradlew testDebugUnitTest :core:model:test --no-daemon --offline --console=plain -Proborazzi.test.verify=false 2>&1 | tee /d/Temp/claude/utc-prescreen.log | grep -E '^BUILD (SUCCESSFUL|FAILED)|FAILED$|> Task .*FAILED'
```
Expected: 记录所有 `FAILED` 的测试类/方法。BUILD SUCCESSFUL = 无脆弱测试（Task 2 空转）；BUILD FAILED = 收集失败清单。

- [ ] **Step 4: 对照默认时区确认失败是时区/locale 导致（非其他）**

对 Step 3 失败的模块，去掉 TZ/LANG 再跑确认默认（CST/zh）下通过：
Run: `cd D:/wt/Cashbook/ci-coverage-fix && ./gradlew :<失败模块>:testDebugUnitTest --no-daemon --offline --console=plain -Proborazzi.test.verify=false 2>&1 | grep -E '^BUILD (SUCCESSFUL|FAILED)'`
Expected: 默认下 SUCCESSFUL（证明失败纯由时区/locale） → 归入 Task 2 修复清单；默认下也 FAILED → 是 pre-existing 真 bug，单独记录不混入本轮。

- [ ] **Step 5: 输出脆弱清单（无 commit，本 task 无代码改动）**

在本会话记录：`<模块>::<测试类>::<方法>` 逐条 + 失败断言摘要。此清单为 Task 2 输入。若清单为空 → 跳过 Task 2，直接 Task 3。

---

## Task 2: 修脆弱测试为 zone/locale-robust（数据驱动，输入=Task 1 清单）

**Files:**
- Modify: Task 1 清单列出的测试文件（`<module>/src/test/.../XxxTest.kt`），逐个修

**Interfaces:**
- Consumes: Task 1 输出的脆弱测试清单（`<模块>::<类>::<方法>` + 失败断言）
- Produces: 全库单测在 UTC/en 与默认时区**均通过**

**修复模式**（参考既有 robust 写法 `feature/records/.../CalendarViewModelTest.kt`：固定 epoch + `systemDefault` 派生 + 注释「跨时区成立」）：

- 时区敏感（从固定 epoch 反推日期/月份）：期望值也由**同一 tz 派生**，而非硬编码某时区结果。
  ```kotlin
  // 脆弱：硬编码期望
  val ts = 1718200000000L
  assertThat(result.date).isEqualTo("2024-06-12") // 仅在 CST 成立，UTC 是 06-12 15:06 也是 12 号——需按实际断言

  // robust：期望由被测同一 zone 规则派生
  val ts = 1718200000000L
  val expected = Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate()
  assertThat(result.date).isEqualTo(expected)
  ```
- locale 敏感（断言格式化字符串 `toMoneyCNY` 等）：固定断言的期望格式，或对被测函数注入固定 Locale。
- **修复原则**：改为**任意时区/locale 都成立**，不引入全局 tz 固定护栏（用户已定「先预筛实测再定」，规模小则逐个 robust）。若 Task 1 清单很大（>5 文件）→ 停下来报用户，重新评估是否改为加全局护栏。

- [ ] **Step 1: 逐个修 Task 1 清单文件**（每文件一改，按上述模式）

- [ ] **Step 2: 复验该文件 UTC/en 通过**

Run: `cd D:/wt/Cashbook/ci-coverage-fix && TZ=UTC LANG=en_US.UTF-8 ./gradlew :<module>:testDebugUnitTest --tests '<修复的类>' --no-daemon --offline --console=plain -Proborazzi.test.verify=false 2>&1 | grep -E '^BUILD (SUCCESSFUL|FAILED)'`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 复验该文件默认时区仍通过（防回归）**

Run: `cd D:/wt/Cashbook/ci-coverage-fix && ./gradlew :<module>:testDebugUnitTest --tests '<修复的类>' --no-daemon --offline --console=plain -Proborazzi.test.verify=false 2>&1 | grep -E '^BUILD (SUCCESSFUL|FAILED)'`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: spotless + commit**

Run: `cd D:/wt/Cashbook/ci-coverage-fix && ./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache --no-daemon 2>&1 | grep -E '^BUILD'`
```bash
git -C D:/wt/Cashbook/ci-coverage-fix add <修复文件>
git -C D:/wt/Cashbook/ci-coverage-fix commit -m "[test|<模块>|时区健壮性][公共]修 UTC/en 下脆弱单测为 zone/locale-robust（CI 首次跑 feature/core 单测前置）"
```

---

## Task 3: Build.yaml 加库模块单测到 CI（组件 1）

**Files:**
- Modify: `.github/workflows/Build.yaml`（`Run local tests` step，现 `:106-108`）

**Interfaces:**
- Consumes: Task 2 完成（脆弱测试已修，CI 首跑不会红）
- Produces: CI Local job 执行 feature/core `testDebugUnitTest` + `:core:model:test`

- [ ] **Step 1: 改 Build.yaml**

将 `Run local tests` step 的 run 命令：
```yaml
        run: ./gradlew testOnlineDebug testOfflineDebug :lint:test
```
改为：
```yaml
        run: ./gradlew testOnlineDebug testOfflineDebug testDebugUnitTest :core:model:test :lint:test -Proborazzi.test.verify=false
```
（保持 step name `Run local tests`、`if: always()` 不变。）

- [ ] **Step 2: 本机自验——库单测执行 + 截图模块不 capture**

Run:
```
cd D:/wt/Cashbook/ci-coverage-fix && ./gradlew testDebugUnitTest :core:model:test --no-daemon --offline --console=plain -Proborazzi.test.verify=false 2>&1 | grep -E '^BUILD (SUCCESSFUL|FAILED)'
```
Expected: BUILD SUCCESSFUL。
再验截图模块未被 capture 污染工作区：
Run: `git -C D:/wt/Cashbook/ci-coverage-fix status --short | grep -c 'src/test/screenshots'`
Expected: `0`（`-Proborazzi.test.verify=false` 使截图模块不做 capture/verify，无基线覆写）。

- [ ] **Step 3: commit**

```bash
git -C D:/wt/Cashbook/ci-coverage-fix add .github/workflows/Build.yaml
git -C D:/wt/Cashbook/ci-coverage-fix commit -m "[ci|build|CI覆盖feature-core单测][公共]Run local tests 加 testDebugUnitTest :core:model:test -Proborazzi.test.verify=false，让 CI 跑库模块 JVM 单测（此前 app-flavor task 只覆盖 app，feature/core UnitTest 执行=0）"
```

---

## Task 4: CLAUDE.md 4 处截图认知纠正（组件 3）

**Files:**
- Modify: `CLAUDE.md`（项目根，:57-59 / :182 / :189 / :193）

**Interfaces:**
- Consumes: 无
- Produces: CLAUDE.md 截图条款与实情一致（feature/core 截图基线本地 record 维护、本轮起单测上 CI、截图 verify 待独立 spec）

- [ ] **Step 1: 改 `:57-59`「截图测试」命令**

`verifyRoborazziOnlineDebug`/`recordRoborazziOnlineDebug`（Online flavor，对库截图验不到、app 无截图）→ 改为库模块无 flavor 变体：
```bash
# 校验（feature/core 库模块，无 flavor）
./gradlew verifyRoborazziDebug   # 校验全部截图模块
./gradlew recordRoborazziDebug   # 生成/更新基准截图
```
并加注：「app 无截图测试；截图测试全在 feature/core 库模块（无 flavor，用 Debug 变体）。本机 record 即权威 reference，CI 当前不 verify feature/core 截图（见下）。」

- [ ] **Step 2: 改 `:182`**

`recordRoborazziDevDebug` + 硬编码行号 `（51-103 行）` → 库模块 `recordRoborazziDebug` + 按 step name 引用：「Dependency Guard 机制仅在 `event_name == 'pull_request'` 时自动 `dependencyGuardBaseline` 并 auto-commit（Build.yaml `Push new Dependency Guard baselines` step）。」删除截图相关的错误暗示（截图 record 当前不覆盖 feature，见 :193 纠正）。

- [ ] **Step 3: 改 `:189`**

「（本地 verify 必失败、Reference 空白；CI 走 record 模式 auto-commit 不暴露）」删除「CI 走 record 模式 auto-commit 不暴露」——改为：「（本地 verify 必失败、Reference 空白；**CI 当前不 verify/record feature/core 截图，缺失基线不会被 CI 暴露也不会被 CI 补录，须本地首录**）」。

- [ ] **Step 4: 改 `:193`（核心错误）**

「**Roborazzi 截图基线由 CI 管理，本机不录/不判**」整条改为实情：
```
- **Roborazzi 截图基线本地 record 维护，CI 当前不 verify/不 record feature/core 截图**：CI 的 `verifyRoborazziDevDebug`/`recordRoborazziDevDebug` 是 app flavor task，只匹配 app（app 无截图，空转）；feature/core 是库模块（无 flavor，仅 Debug 变体），CI 不覆盖其截图。故 feature/core 的 728 张基线全靠**本地 `recordRoborazziDebug` 录入库维护，本机 record 即权威 reference**。本机 `verifyRoborazzi` 对 `_dynamic`（Material You）变体报 changed 属本机渲染差异，但因 CI 不 verify feature 截图，本地录的即最终版（无 CI 重录覆盖）。**本轮起 feature/core 单测已上 CI（`testDebugUnitTest`），但截图 verify 仍待独立 spec 加固后引入**（前置：像素容差/PR-only 门控/独立录基线 PR，见 docs/superpowers/specs 截图 verify spec）。实证：run 28991717231 feature/core UnitTest 执行=0、Roborazzi task 全 `:app:`。
```

- [ ] **Step 5: 核对无遗漏 + commit**

Run: `grep -nE '[Rr]oborazzi' D:/wt/Cashbook/ci-coverage-fix/CLAUDE.md`（确认 4 处均已改、无残留 DevDebug/OnlineDebug 错误命名指向 feature 截图）
```bash
git -C D:/wt/Cashbook/ci-coverage-fix add CLAUDE.md
git -C D:/wt/Cashbook/ci-coverage-fix commit -m "[docs|公共|CLAUDE.md截图认知纠正][公共]4 处纠正 CI 管理截图基线的错误认知（CI 不 verify/record feature/core 截图，728 基线本地 record 维护；本轮起单测上 CI，截图 verify 待独立 spec）"
```

---

## Task 5: 完整链路验证 + 截图 verify backlog 记录 + 节点2 + PR

**Files:**
- 无代码改动（验证 + backlog 记录 + 交付）

- [ ] **Step 1: 完整链路本机验证**

```
cd D:/wt/Cashbook/ci-coverage-fix
# 库单测 UTC/en（模拟 CI）全绿
TZ=UTC LANG=en_US.UTF-8 ./gradlew testDebugUnitTest :core:model:test --no-daemon --offline --console=plain -Proborazzi.test.verify=false 2>&1 | grep -E '^BUILD (SUCCESSFUL|FAILED)'
# spotless
./gradlew spotlessCheck --init-script gradle/init.gradle.kts --no-configuration-cache --no-daemon 2>&1 | grep -E '^BUILD'
# app 跨模块 Hilt 全图
./gradlew :app:compileOnlineDebugKotlin --no-daemon --offline --console=plain 2>&1 | grep -E '^BUILD (SUCCESSFUL|FAILED)'
```
Expected: 三条均 BUILD SUCCESSFUL。

- [ ] **Step 2: 截图 verify 独立 spec 记 backlog**

更新 memory `cashbook-pending-todos.md`：新增「截图 verify 上 CI（独立 spec 待写）」条目，含前置加固条件 H1/H2/M1/M2/M3/L1（引 spec）。

- [ ] **Step 3: 节点2 full-review**

`comprehensive-review:full-review`，评审目标 = 本 worktree git diff（Build.yaml + CLAUDE.md + 脆弱测试修复）。Build.yaml 逻辑改动触发；CLAUDE.md 纯文档纠正走文档领域豁免（维度收敛）。修复其 Critical/High。

- [ ] **Step 4: push + 开 PR + gh workflow scope**

改 Build.yaml 需 workflow scope：
```bash
gh auth refresh -h github.com -s workflow   # 若 push/PR 报 workflow scope 缺失
```
push worktree 分支 + 开 PR（base main）。**CI 实证验收**：PR CI Local job 日志应出现 `:feature:*:testDebugUnitTest` / `:core:*:testDebugUnitTest` 执行（feature/core UnitTest 执行计数 0→>0），且无 screenshots auto-commit。CI 绿后 merge。

---

## Self-Review

**1. Spec coverage:**
- 组件1（单测覆盖）→ Task 3 ✅
- 组件2（M5 tz 预筛+修）→ Task 1 + Task 2 ✅
- 组件3（CLAUDE.md 4 处）→ Task 4 ✅
- 组件4（截图 verify 独立 spec backlog）→ Task 5 Step 2 ✅
- 验证 + 节点2 → Task 5 ✅
- 无遗漏。

**2. Placeholder scan:** Task 2 是数据驱动（输入=Task 1 清单），非 placeholder——给了修复模式、参考文件、复验命令、大清单（>5 文件）停下来报用户的退路。Task 1 清单为空则跳过 Task 2（明确分支）。

**3. Type consistency:** 命令参数一致（`-Proborazzi.test.verify=false` 全程一致；`testDebugUnitTest :core:model:test` 一致）；CLAUDE.md 4 处改法各自独立无交叉引用错误。

**关键依赖顺序**：Task 2（修脆弱测试）必须先于 Task 3（Build.yaml 上 CI），否则 CI 首跑库单测在 UTC 下红。Task 1→2→3→4→5 严格串行。
