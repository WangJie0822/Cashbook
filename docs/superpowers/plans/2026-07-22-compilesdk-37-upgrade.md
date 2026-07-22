# 升 compileSdk 37 专项实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** compileSdk 36→37 合入 main，随后串行合入被解锁的 3 个 dependabot PR（#498 core-ktx 1.19.0 → #497 lifecycle 2.11.0 → #501 hilt 1.4.0），全程本机验证 CI 盲区（截图 verify / app-catalog lint）。

**Architecture:** 单常量变更（`ProjectSetting.kt:38`，唯一配置点经三 convention 插件覆盖全模块）+ 文档同步；验证按「CI 盲区优先」矩阵；PR 严格串行（merge 一个才 recreate 下一个）；回滚预案见 spec §7。

**Tech Stack:** AGP 9.3.0 / Gradle 9.6 / Kotlin 2.3.20；spec：`docs/superpowers/specs/2026-07-22-compilesdk-37-upgrade-design.md`。

## Global Constraints

- **构建判定只信** `grep -E '^BUILD (SUCCESSFUL|FAILED)' <log>`；后台 Bash 的 exit 0 与首个完成通知不可信；日志末行须为 `N actionable tasks: ...`。
- **联网 gradle 命令统一前缀**（Git Bash）：`env -u http_proxy -u https_proxy -u all_proxy -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY ./gradlew <task> -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897 --console=plain`；暖缓存后增量改 `--offline --console=plain`。
- **gh/curl 等网络命令先清继承代理**：`env -u http_proxy -u https_proxy -u all_proxy -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY gh ...`（下文简写 `env -u… gh`，执行时写全六个 -u）。
- **每次长构建（>5min）前内存预检**：`powershell -NoProfile -Command "$os=Get-CimInstance Win32_OperatingSystem; '{0:N0}MB {1:N1}%' -f ($os.FreePhysicalMemory/1024), ((1-$os.FreePhysicalMemory/$os.TotalVisibleMemorySize)*100)"`——可用 <1000MB 或 >90% 中止询问用户。默认串行，不加 `--parallel`。
- **worktree 用 D:/wt 短路径**（Android 构建撞 MAX_PATH，CLAUDE.local.md 场景 1）：`D:/wt/Cashbook/compilesdk-37`，分支 `worktree-compilesdk-37`。worktree 内文件工具用 worktree 绝对路径。
- **merge 全部用 `gh pr merge <n> --merge --admin`**，merge 前必自查 CI 结论（分支保护 required_status_checks=null，CI 非门禁）；auto-baseline 二次 commit run 卡 `action_required` 时 `env -u… gh api repos/WangJie0822/Cashbook/actions/runs/<id>/approve -X POST`。
- **专项完成前冻结发版 tag**（不推任何 `v*` tag）。
- **badging 基线链**存 scratchpad：`badging-36.txt → badging-37.txt → badging-498.txt → badging-497.txt → badging-501.txt`，每次与前一个 diff（过滤 versionCode/versionName 行）。aapt2 = `D:/Work/Development/AndroidSdk/build-tools/36.0.0/aapt2`。
- Python 用 `py`（裸 python3 踩 WindowsApps stub）；PowerShell 包装 cmd 内置命令时 `chcp 65001`。

---

### Task 1: 环境前置（SDK 37 安装 + 暖缓存 + AGP 9.3.0 实证）

**Files:** 无代码变更（纯环境）。

**Interfaces:**
- Produces: 本机 `platforms/android-37` 就位；gradle 缓存含 AGP 9.3.0；AGP 9.3.0 `MAX_RECOMMENDED_COMPILE_SDK ≥ 37` 一手证据。

- [ ] **Step 1: 内存预检**（Global Constraints 命令）。低于阈值中止询问。

- [ ] **Step 2: 装 android-37 platform**

```powershell
& { $env:_JAVA_OPTIONS="-Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897 -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897"; 'HTTP_PROXY','HTTPS_PROXY','http_proxy','https_proxy','ALL_PROXY','all_proxy' | % { Remove-Item "Env:$_" -EA SilentlyContinue }; android sdk install "platforms;android-37" }
```

预期：退出码 0。若 `android sdk install` 子命令名不符，先 `android sdk --help` 确认实名（android CLI 已配 `.androidrc` 指向 `D:\Work\Development\AndroidSdk`）；fallback 用 SDK 自带 `cmdline-tools` 的 `sdkmanager.bat "platforms;android-37"`（同样带 `_JAVA_OPTIONS` 代理）。

- [ ] **Step 3: 装后抽验**

```bash
grep -E "AndroidVersion.ApiLevel|Platform.Version" "D:/Work/Development/AndroidSdk/platforms/android-37/source.properties"
```

预期：`AndroidVersion.ApiLevel=37`。

- [ ] **Step 4: 代理探活 + 暖缓存拉 AGP 9.3.0**

```bash
curl -x http://127.0.0.1:7897 --max-time 20 -sI -o /dev/null -w "%{http_code}\n" https://dl.google.com/android/maven2/com/android/tools/build/gradle/9.3.0/gradle-9.3.0.pom
```

预期 `200`（`000` 为代理上游未出网，等恢复）。然后在主 checkout（`D:\Work\Workspace\Owner\Cashbook`）跑：

```bash
env -u http_proxy -u https_proxy -u all_proxy -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY ./gradlew help -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897 --console=plain > /tmp/warm-agp.log 2>&1; grep -E '^BUILD (SUCCESSFUL|FAILED)' /tmp/warm-agp.log
```

预期：`BUILD SUCCESSFUL`（配置阶段解析 buildscript classpath 即拉 AGP 9.3.0）。

- [ ] **Step 5: AGP 9.3.0 jar 补一手实证**

```bash
J=$(find ~/.gradle/caches/modules-2/files-2.1/com.android.tools.build/builder/9.3.0 -name "builder-9.3.0.jar" | head -1); cd "D:/Temp/claude/D--Work-Workspace-Owner-Cashbook/d4754db0-74ef-4069-8249-08ba7bc00efc/scratchpad" && unzip -o -q "$J" "com/android/builder/core/ToolsRevisionUtils.class" && javap -c -p com/android/builder/core/ToolsRevisionUtils.class | grep -A 3 "bipush"
```

预期：`bipush 37`（或更大值）写入 `MAX_RECOMMENDED_COMPILE_SDK_VERSION`。若 jar 未在此路径，`find ~/.gradle/caches -name "builder-9.3.0.jar"` 定位。**若值 <37 → 中止，回报用户（spec §2 推断被推翻）**。

---

### Task 2: worktree 创建 + badging-36 基线采集

**Files:** 无代码变更（worktree + 基线产物在 scratchpad）。

**Interfaces:**
- Consumes: Task 1 的暖缓存。
- Produces: worktree `D:/wt/Cashbook/compilesdk-37`（分支 `worktree-compilesdk-37`，基于本地 main）；`scratchpad/badging-36.txt`；全依赖暖缓存完成（首次 assemble 联网拉全量）。

- [ ] **Step 1: 建 worktree**

```bash
git -C D:/Work/Workspace/Owner/Cashbook worktree add "D:/wt/Cashbook/compilesdk-37" -b worktree-compilesdk-37 && git -C "D:/wt/Cashbook/compilesdk-37" log --oneline -1
```

预期：HEAD 与本地 main 一致（手动 add 基于当前 HEAD，无 EnterWorktree 的 origin 派生滞后问题）。

- [ ] **Step 2: 改动前构建 OnlineDebug（同时完成依赖全量暖缓存）**

内存预检后，worktree 内：

```bash
cd "D:/wt/Cashbook/compilesdk-37" && env -u http_proxy -u https_proxy -u all_proxy -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY ./gradlew :app:assembleOnlineDebug -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897 --console=plain > /tmp/asm36.log 2>&1; grep -E '^BUILD (SUCCESSFUL|FAILED)' /tmp/asm36.log
```

预期：`BUILD SUCCESSFUL`（run_in_background 跑，按 Global Constraints 判定）。

- [ ] **Step 3: 采集 badging-36 基线**

```bash
APK=$(ls "D:/wt/Cashbook/compilesdk-37/app/build/outputs/apk/online/debug/"*.apk | head -1) && "D:/Work/Development/AndroidSdk/build-tools/36.0.0/aapt2" dump badging "$APK" > "D:/Temp/claude/D--Work-Workspace-Owner-Cashbook/d4754db0-74ef-4069-8249-08ba7bc00efc/scratchpad/badging-36.txt" && grep -cE "uses-permission" "D:/Temp/claude/D--Work-Workspace-Owner-Cashbook/d4754db0-74ef-4069-8249-08ba7bc00efc/scratchpad/badging-36.txt"
```

预期：badging-36.txt 生成，权限行数为非 0 整数（记录该数）。

---

### Task 3: 变更实施（常量 + 文档，2 原子 commit）

**Files:**
- Modify: `build-logic/convention/src/main/kotlin/cn/wj/android/cashbook/buildlogic/ProjectSetting.kt:37-38`（worktree 路径：`D:/wt/Cashbook/compilesdk-37/build-logic/...`）
- Modify: `CLAUDE.md:183`（worktree 路径）

**Interfaces:**
- Produces: `COMPILE_SDK = 37`；CLAUDE.md 条款同步。后续 Task 4-5 验证/提 PR 基于这两笔 commit。

- [ ] **Step 1: 改 ProjectSetting.kt（worktree 内）**

```kotlin
        /** SDK 编译版本 */
        const val COMPILE_SDK = 37
```

（替换 `const val COMPILE_SDK = 36`；KDoc 行不变。）

- [ ] **Step 2: commit 1**

```bash
cd "D:/wt/Cashbook/compilesdk-37" && git add build-logic/convention/src/main/kotlin/cn/wj/android/cashbook/buildlogic/ProjectSetting.kt && git diff --cached --stat && git commit -m "[build|compileSdk|SDK37][公共]compileSdk 36→37（解锁 #501 hilt 1.4.0/#498 core-ktx 1.19.0/#497 lifecycle 2.11.0 依赖群；AGP 9.3.0 MAX_RECOMMENDED=37 实证；targetSdk 保持 36）

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

预期：1 file changed, 1 insertion, 1 deletion。

- [ ] **Step 3: 改 CLAUDE.md:183 条款**

将「当前 `ProjectSetting.Config.COMPILE_SDK`（现 36）」改为「（现 37）」；将句尾「2026-07-22 更新：AGP 已升 9.3.0（#508）、SDK 37 依赖群累积 3 个（#501 hilt 1.4.0 / #498 core-ktx 1.19.0 / #497 lifecycle 2.11.0，均保持 open 待升 SDK 37 专项解锁）。」改为「2026-07-22 更新：AGP 已升 9.3.0（#508）；compileSdk 已升 37（本条款实证的 SDK 37 依赖群 #501/#498/#497 由专项解锁合入，见 `docs/superpowers/specs/2026-07-22-compilesdk-37-upgrade-design.md`）。」

- [ ] **Step 4: commit 2**

```bash
cd "D:/wt/Cashbook/compilesdk-37" && git add CLAUDE.md && git diff --cached --stat && git commit -m "[docs|CLAUDE.md|SDK37][公共]同步 compileSdk 37 条款（现 36→现 37；依赖群解锁记录指向专项 spec）

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

预期：1 file changed。

---

### Task 4: 本机验证矩阵（spec §5 全 8 项 + badging-37 diff）

**Files:**
- 可能 Modify: `app/lint-baseline.xml`、`app-catalog/lint-baseline.xml`（仅当 API 37 lint 出新增结论）
- 产物: `scratchpad/badging-37.txt`

**Interfaces:**
- Consumes: Task 3 的 2 commit。
- Produces: 8 项验证全绿记录（回报用户时逐项贴判定行）；可能的 lint baseline commit 3。

> 所有命令在 `D:/wt/Cashbook/compilesdk-37` 执行；暖缓存已全（Task 2），默认 `--offline`；**任何一项 FAILED → 停下按 systematic-debugging 定位，不得跳过**。每条长构建前做内存预检，run_in_background 跑，grep BUILD 行判定。

- [ ] **Step 1: build-logic 自校验**

```bash
env -u http_proxy -u https_proxy -u all_proxy -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY ./gradlew check -p build-logic --offline --console=plain > /tmp/v1-buildlogic.log 2>&1; grep -E '^BUILD (SUCCESSFUL|FAILED)' /tmp/v1-buildlogic.log
```

预期 `BUILD SUCCESSFUL`（含 buildReleaseApkName 7 用例）。

- [ ] **Step 2: OnlineDebug 冒烟（37 下首编）+ badging-37 diff**

```bash
env -u… ./gradlew :app:assembleOnlineDebug --offline --console=plain > /tmp/asm37.log 2>&1; grep -E '^BUILD (SUCCESSFUL|FAILED)' /tmp/asm37.log
```

预期 SUCCESSFUL（android-37 platform 已本地就位，offline 可用；若报 platform 缺失再去 offline 联网）。然后：

```bash
S="D:/Temp/claude/D--Work-Workspace-Owner-Cashbook/d4754db0-74ef-4069-8249-08ba7bc00efc/scratchpad"; APK=$(ls app/build/outputs/apk/online/debug/*.apk | head -1) && "D:/Work/Development/AndroidSdk/build-tools/36.0.0/aapt2" dump badging "$APK" > "$S/badging-37.txt" && diff <(grep -vE "versionCode|versionName|platformBuildVersion|compileSdkVersion" "$S/badging-36.txt") <(grep -vE "versionCode|versionName|platformBuildVersion|compileSdkVersion" "$S/badging-37.txt"); echo "diff-exit=$?"
```

预期 `diff-exit=0`（`platformBuildVersion*`/`compileSdkVersion*` 编译标记行随 compileSdk 必然 36→37，已在过滤器中排除——它们正是本次变更本身；过滤后其余内容零差异；出现新增 uses-permission/组件行 → 停下回报）。

- [ ] **Step 3: 全变体编译**

```bash
env -u… ./gradlew :app:assemble :app-catalog:assembleRelease --offline --console=plain > /tmp/v3-assemble.log 2>&1; grep -E '^BUILD (SUCCESSFUL|FAILED)' /tmp/v3-assemble.log
```

预期 SUCCESSFUL（app 8 变体；release 无签名配置时产 unsigned 不影响编译验证；若 release 变体因缺 `gradle/signing.versions.toml` 配置失败，记录后改跑 `:app:assembleOnlineDebug :app:assembleOfflineDebug :app:assembleCanaryDebug :app:assembleDevDebug :app-catalog:assembleDebug` 覆盖全 flavor 编译面）。再编 baselineProfile 模块：

```bash
env -u… ./gradlew :baselineProfile:assemble --offline --console=plain > /tmp/v3b-bp.log 2>&1; grep -E '^BUILD (SUCCESSFUL|FAILED)' /tmp/v3b-bp.log
```

预期 SUCCESSFUL（`com.android.test` 模块同吃 COMPILE_SDK；若 assemble 无该任务，`./gradlew :baselineProfile:tasks --all | grep -i assemble` 确认实名后跑）。

- [ ] **Step 4: 全库单测**

```bash
env -u… ./gradlew testDebugUnitTest :core:model:test :app:testOnlineDebugUnitTest :app:testOfflineDebugUnitTest --offline --console=plain > /tmp/v4-test.log 2>&1; grep -E '^BUILD (SUCCESSFUL|FAILED)' /tmp/v4-test.log; grep -cE "FAILED" /tmp/v4-test.log
```

预期 `BUILD SUCCESSFUL` 且无测试 FAILED 行。

- [ ] **Step 5: 截图 verify（CI 盲区，唯一防线）**

```bash
env -u… ./gradlew verifyRoborazziDebug --offline --console=plain > /tmp/v5-roborazzi.log 2>&1; grep -E '^BUILD (SUCCESSFUL|FAILED)' /tmp/v5-roborazzi.log; grep -iE "FAILED|comparison" /tmp/v5-roborazzi.log | head -20
```

预期 SUCCESSFUL 0 diff。若个别 `_dynamic`（动态配色）变体 changed：按项目 CLAUDE.md 条款甄别（本机已知噪声、勿据此判基线错）；**非 dynamic 变体出 diff → 停下**，Read compare PNG 分析（compileSdk 理论不影响 Robolectric 渲染[模拟 targetSdk=36]，出 diff 即异常信号）。

- [ ] **Step 6: lint 全集（含 CI 不跑的 Dev 与 app-catalog）**

```bash
env -u… ./gradlew :app:lintOnlineRelease :app:lintOfflineRelease :app:lintDevRelease :app-catalog:lintRelease :lint:lint -Dlint.baselines.continue=true -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897 --console=plain > /tmp/v6-lint.log 2>&1; grep -E '^BUILD (SUCCESSFUL|FAILED)' /tmp/v6-lint.log
```

（首跑带代理不带 offline——lint-gradle 32.3.0 本机缓存无，需从 google() 拉。）预期 SUCCESSFUL。若 FAILED 且是 baseline 外新增结论：核阅每条是否真实问题——真实问题修复；API 37 数据库噪声更新 baseline（删除对应 `lint-baseline.xml` 后重跑 lint 自动重建），commit 3：

```bash
git add app/lint-baseline.xml app-catalog/lint-baseline.xml && git commit -m "[build|lint|SDK37][公共]lint baseline 更新（API 37 lint 数据库新增结论，逐条核阅均为噪声）

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

- [ ] **Step 7: spotlessCheck + dependencyGuard**

```bash
env -u… ./gradlew spotlessCheck --init-script gradle/init.gradle.kts --no-configuration-cache --offline --console=plain > /tmp/v7-spotless.log 2>&1; grep -E '^BUILD (SUCCESSFUL|FAILED)' /tmp/v7-spotless.log
env -u… ./gradlew dependencyGuard --offline --console=plain > /tmp/v8-guard.log 2>&1; grep -E '^BUILD (SUCCESSFUL|FAILED)' /tmp/v8-guard.log; git status --short
```

预期：两者 SUCCESSFUL；`git status` 无 `dependencies/*.txt` 改动（实证 compileSdk 不动 classpath；有改动 → 异常信号，停下分析）。

---

### Task 5: compileSdk PR → CI → merge

**Files:** 无新变更（push + PR + merge）。

**Interfaces:**
- Consumes: Task 3/4 的 2-3 commit。
- Produces: compileSdk 37 落 origin/main；本地 main 同步；worktree 保留（Task 6-8 badging 构建复用）。

- [ ] **Step 1: push 分支 + 建 PR**

```bash
cd "D:/wt/Cashbook/compilesdk-37" && env -u… git push -u origin worktree-compilesdk-37 && env -u… git ls-remote origin refs/heads/worktree-compilesdk-37 | cut -f1
```

预期：ls-remote 返回的 hash = 本地分支 HEAD（push 落地唯一权威）。

```bash
env -u… gh pr create --title "[build|compileSdk|SDK37][公共]compileSdk 36→37 解锁 SDK37 依赖群" --body "compileSdk 36→37（targetSdk 保持 36），解锁 #501/#498/#497。设计 spec：docs/superpowers/specs/2026-07-22-compilesdk-37-upgrade-design.md。本机验证矩阵 8 项全绿（build-logic check/全变体编译/全库单测/verifyRoborazziDebug 0 diff/lint 全集含 app-catalog/spotless/dependencyGuard 0 变化/badging diff 无 manifest 面变化）。

🤖 Generated with [Claude Code](https://claude.com/claude-code)"
```

- [ ] **Step 2: CI 观察至绿**

```bash
env -u… gh pr checks <PR号> --watch --interval 60
```

无 checks 时按 CLAUDE.md 口径：`env -u… gh run list --branch worktree-compilesdk-37` 查 run，`action_required` 则 approve（Global Constraints 命令）。红了 → systematic-debugging（辨环境假失败：aliyun 502/负缓存按 CLAUDE.md ③④ 处置）。

- [ ] **Step 3: merge + 本地同步**

```bash
env -u… gh pr merge <PR号> --merge --admin && cd D:/Work/Workspace/Owner/Cashbook && env -u… git fetch origin && git merge --ff-only origin/main && git log --oneline -2
```

预期：本地 main ff 到含 compileSdk commit 的 origin/main。（worktree 分支暂不删。）

---

### Task 6: 合入 #498 core-ktx 1.18.0→1.19.0

**Files:** 无本地变更（PR 操作 + badging 审查构建）。

**Interfaces:**
- Consumes: Task 5 后的 origin/main；`scratchpad/badging-37.txt`。
- Produces: #498 合入；`scratchpad/badging-498.txt` 作下一基线。

- [ ] **Step 1: recreate**

```bash
env -u… gh pr comment 498 --body "@dependabot recreate"
```

等 dependabot 重建（`env -u… gh pr view 498 --json headRefName,commits --jq '.commits[-1].oid'` 变化即重建完成，2-5min 轮询）。

- [ ] **Step 2: CI 至绿**：同 Task 5 Step 2（分支名从 `gh pr view 498 --json headRefName` 取）。

- [ ] **Step 3: 合并前审查三件套**

```bash
env -u… gh pr diff 498 | grep -E "^(diff|[+-])" | grep -vE "^(\+\+\+|---)" | head -80
```

核对：① `libs.versions.toml` 仅 `androidx-core = ` 版本行变更（catalog 唯一源码变更点）；② `dependencies/*.txt` 新增/变更行仅 `androidx.core:core-ktx:1.19.0` 及其已知传递闭包（androidx.core 组内工件），**陌生 group/artifact → 停下回报**；③ 记录实际解析版本（传递可能拉入 lifecycle/hilt 新版，属预期，记录即可）。

- [ ] **Step 4: badging diff**

```bash
cd "D:/wt/Cashbook/compilesdk-37" && env -u… git fetch origin "$(env -u… gh pr view 498 --json headRefName --jq .headRefName)" && git checkout FETCH_HEAD && env -u… ./gradlew :app:assembleOnlineDebug -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897 --console=plain > /tmp/asm498.log 2>&1; grep -E '^BUILD (SUCCESSFUL|FAILED)' /tmp/asm498.log
S="D:/Temp/claude/D--Work-Workspace-Owner-Cashbook/d4754db0-74ef-4069-8249-08ba7bc00efc/scratchpad"; APK=$(ls app/build/outputs/apk/online/debug/*.apk | head -1) && "D:/Work/Development/AndroidSdk/build-tools/36.0.0/aapt2" dump badging "$APK" > "$S/badging-498.txt" && diff <(grep -vE "versionCode|versionName" "$S/badging-37.txt") <(grep -vE "versionCode|versionName" "$S/badging-498.txt"); echo "diff-exit=$?"
```

预期 `diff-exit=0`（新增 uses-permission/组件 → 停下回报）。

- [ ] **Step 5: merge + 同步**

```bash
env -u… gh pr merge 498 --merge --admin && cd D:/Work/Workspace/Owner/Cashbook && env -u… git fetch origin && git merge --ff-only origin/main
```

---

### Task 7: 合入 #497 lifecycle 2.9.1→2.11.0

**Files/Interfaces:** 同 Task 6 型（Consumes badging-498.txt；Produces badging-497.txt）。

- [ ] **Step 1: recreate**

```bash
env -u… gh pr comment 497 --body "@dependabot recreate"
```

等重建完成（`env -u… gh pr view 497 --json headRefName,commits --jq '.commits[-1].oid'` 变化）。

- [ ] **Step 2: CI 至绿**：同 Task 5 Step 2（分支名 `gh pr view 497 --json headRefName`）。

- [ ] **Step 3: 合并前审查三件套**

```bash
env -u… gh pr diff 497 | grep -E "^(diff|[+-])" | grep -vE "^(\+\+\+|---)" | head -80
```

核对：① catalog 仅 `androidx-lifecycle = "2.11.0"` 行；② baseline 变更仅 `androidx.lifecycle:*` 组及已知传递（注意 JetBrains `org.jetbrains.androidx.lifecycle:*` wrapper 与 androidx 2.11.0 的混合图变化——记录实际解析版本组合）；③ 陌生工件停下。

- [ ] **Step 4: badging diff**

```bash
cd "D:/wt/Cashbook/compilesdk-37" && env -u… git fetch origin "$(env -u… gh pr view 497 --json headRefName --jq .headRefName)" && git checkout FETCH_HEAD && env -u… ./gradlew :app:assembleOnlineDebug -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897 --console=plain > /tmp/asm497.log 2>&1; grep -E '^BUILD (SUCCESSFUL|FAILED)' /tmp/asm497.log
S="D:/Temp/claude/D--Work-Workspace-Owner-Cashbook/d4754db0-74ef-4069-8249-08ba7bc00efc/scratchpad"; APK=$(ls app/build/outputs/apk/online/debug/*.apk | head -1) && "D:/Work/Development/AndroidSdk/build-tools/36.0.0/aapt2" dump badging "$APK" > "$S/badging-497.txt" && diff <(grep -vE "versionCode|versionName" "$S/badging-498.txt") <(grep -vE "versionCode|versionName" "$S/badging-497.txt"); echo "diff-exit=$?"
```

预期 `diff-exit=0`（lifecycle 2.x 历史上有 ProcessLifecycleOwner initializer provider 声明，若 diff 出现 provider/receiver 行 → 停下核对是否 androidx 官方 startup 组件，回报用户定夺）。

- [ ] **Step 5: merge + 同步**

```bash
env -u… gh pr merge 497 --merge --admin && cd D:/Work/Workspace/Owner/Cashbook && env -u… git fetch origin && git merge --ff-only origin/main
```

---

### Task 8: 合入 #501 hilt 1.3.0→1.4.0

**Files/Interfaces:** 同 Task 6 型（Consumes badging-497.txt；Produces badging-501.txt）。

- [ ] **Step 1: recreate**

```bash
env -u… gh pr comment 501 --body "@dependabot recreate"
```

等重建完成（`env -u… gh pr view 501 --json headRefName,commits --jq '.commits[-1].oid'` 变化）。

- [ ] **Step 2: CI 至绿**：同 Task 5 Step 2（分支名 `gh pr view 501 --json headRefName`）。此时 compileSdk 37 已在 main，checkAarMetadata 应不再失败——若仍失败，贴原始错误行分析（可能要求值又升）。

- [ ] **Step 3: 合并前审查三件套**

```bash
env -u… gh pr diff 501 | grep -E "^(diff|[+-])" | grep -vE "^(\+\+\+|---)" | head -80
```

核对：① catalog 仅 `androidx-hilt = "1.4.0"` 行（hilt-compiler 是 KSP processor 不在 guard 内，靠此步核对）；② baseline 变更仅 `androidx.hilt:*`（含 1.4.0 新拆 `hilt-lifecycle-viewmodel*` 工件）及已知传递；③ 陌生工件停下。

- [ ] **Step 4: badging diff**

```bash
cd "D:/wt/Cashbook/compilesdk-37" && env -u… git fetch origin "$(env -u… gh pr view 501 --json headRefName --jq .headRefName)" && git checkout FETCH_HEAD && env -u… ./gradlew :app:assembleOnlineDebug -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897 --console=plain > /tmp/asm501.log 2>&1; grep -E '^BUILD (SUCCESSFUL|FAILED)' /tmp/asm501.log
S="D:/Temp/claude/D--Work-Workspace-Owner-Cashbook/d4754db0-74ef-4069-8249-08ba7bc00efc/scratchpad"; APK=$(ls app/build/outputs/apk/online/debug/*.apk | head -1) && "D:/Work/Development/AndroidSdk/build-tools/36.0.0/aapt2" dump badging "$APK" > "$S/badging-501.txt" && diff <(grep -vE "versionCode|versionName" "$S/badging-497.txt") <(grep -vE "versionCode|versionName" "$S/badging-501.txt"); echo "diff-exit=$?"
```

预期 `diff-exit=0`。

- [ ] **Step 5: merge + 同步**

```bash
env -u… gh pr merge 501 --merge --admin && cd D:/Work/Workspace/Owner/Cashbook && env -u… git fetch origin && git merge --ff-only origin/main && git log --oneline -5
```

---

### Task 9: 收尾门（终态验证 + 冒烟 + 节点 2 + 清理）

**Files:**
- 清理: worktree `D:/wt/Cashbook/compilesdk-37` + 分支
- 更新: memory `cashbook-pending-todos.md`（会话末）

**Interfaces:**
- Consumes: 三 PR 全合入后的 origin/main。
- Produces: 终态验证记录；节点 2 快审结论；发版冻结解除条件达成声明。

- [ ] **Step 1: 主 checkout 终态验证（三依赖合入态）**

内存预检后，主 checkout（联网拉新依赖）：

```bash
cd D:/Work/Workspace/Owner/Cashbook && env -u… ./gradlew testDebugUnitTest :core:model:test :app:testOnlineDebugUnitTest verifyRoborazziDebug -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897 --console=plain > /tmp/final-verify.log 2>&1; grep -E '^BUILD (SUCCESSFUL|FAILED)' /tmp/final-verify.log; grep -cE "FAILED" /tmp/final-verify.log
```

预期 SUCCESSFUL、0 测试 FAILED、截图 0 diff（dynamic 噪声按 CLAUDE.md 甄别）。

- [ ] **Step 2: 模拟器冒烟 journey（3 条）**

启动 Medium_Phone 模拟器（`android` CLI），安装终态 OnlineDebug APK（主 checkout `:app:assembleOnlineDebug` 产物）。黑盒驱动（`android layout` JSON dump，Compose 截图全白；DoKit 浮窗拦菜单先拖离）：
1. **记账**：打开 app → 记一笔支出 ¥10 → 首页列表出现该记录（验 lifecycle 2.11 下 collectAsStateWithLifecycle 订阅链 + hilt VM 注入）。
2. **备份**：设置 → 备份与恢复 → 本地备份 → 报成功（验 hilt-work Worker 与文件链路）。
3. **提醒调度**：logcat 过滤 `reminder`，重启 app 观察 InitWorker `_OneTime` 补查 work 正常运行（验 hilt-work HiltWorkerFactory 实例化 DailyReminderWorker 不崩）。

任一步崩溃/无响应 → systematic-debugging + 按 spec §7 回滚预案评估。

- [ ] **Step 3: 节点 2 两维快审**

源码 diff 预计 <50 行（常量 + CLAUDE.md + 可能 lint baseline；dependabot PR 是版本号），按规约降级：并行派 `comprehensive-review:comprehensive-review-code-reviewer` + `comprehensive-review:comprehensive-review-architect-review` 两 subagent 审 `git diff <专项前 main>..HEAD`（prompt 明令只读、禁改工作区）。Critical/High → 修复后再交付；无 → 通过。

- [ ] **Step 4: worktree 清理**

```bash
cd D:/Work/Workspace/Owner/Cashbook && git worktree remove "D:/wt/Cashbook/compilesdk-37" && git worktree prune && git branch -d worktree-compilesdk-37 && env -u… git push origin --delete worktree-compilesdk-37
```

撞 `Filename too long` 时：`powershell -NoProfile -Command "Remove-Item -LiteralPath '\\?\D:\wt\Cashbook\compilesdk-37' -Recurse -Force"` 后 `git worktree prune`。删前确认无 Gradle daemon 占用（`Get-Process java` 按 JDK 路径辨认，`Stop-Process` 停本项目 daemon）。

- [ ] **Step 5: 交付声明 + 收尾**

- 回报用户：三 PR 合入 + 终态验证 + 冒烟结果 + 节点 2 结论；声明**发版冻结解除**（发版时机用户掌控）。
- 更新 memory `cashbook-pending-todos.md`（compileSdk 37 专项完成条目）；提醒 `/summarize-session`。
