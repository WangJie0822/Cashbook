# AGP9/Gradle9/JDK17 工具链升级 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans（**本机推荐 controller 亲自串行**，见下方执行说明）to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 Cashbook 构建工具链升级到最新主版本（AGP 9.2 / Gradle 9.6 / Kotlin 2.3 / 源码 Java 17 / targetSdk 36），并同步 3 个 GitHub workflow。

**Architecture:** 方案 A「P-A 改造先于升级」——先在 AGP 8.12 下完成可平滑迁移的变体 API / internal DSL 改造（每步可编译可 commit），再分子步切版本（Gradle-only → 内置 Kotlin PoC → AGP+Kotlin+KSP+Hilt 同步），最后改 workflow + 全链路验证。全程在 worktree `upgrade-agp9` 分支，不污染 main，人工拍板合入。

**Tech Stack:** Gradle 9.6.0、AGP 9.2.0、Kotlin 2.3.0（内置 Kotlin）、KSP2、Hilt ≥2.58、Room、Compose、Hilt、自定义 convention plugins、Roborazzi、baseline profile。

**实施载体**：复杂 AGP 9 DSL/API 迁移用专属 skill `agp-9-upgrade`；SDK 组件安装用 `android-cli`。

> **关于"确切代码"的取舍（事实优先）**：标 **[机械]** 的任务给出确切前后代码（已核验）。标 **[迁移]** 的任务给出已核验的当前代码 + 精确迁移目标 API + 编译验证门；其确切新 API 替换代码在执行期借 `agp-9-upgrade` skill + [AGP 9 迁移文档](https://developer.android.com/build/releases/agp-9-0-0-release-notes) 导出，**以编译通过为闸门**——不在 plan 内预写未经构建验证的 onVariants/artifacts 代码（避免凭空捏造）。

---

## Global Constraints

> 每个任务的要求隐含包含本节。所有值从 spec 逐字复制。

- **版本目标**：Gradle `9.6.0`、AGP `9.2.0`、Kotlin `2.3.0`、KSP `与 Kotlin 2.3.0 配对版（执行期 google/ksp release pin）`、Hilt `≥2.58（取最新）`、源码 Java `17`、targetSdk `36`、compileSdk `36`（不动）、minSdk `24`（不动）、CI/运行 JDK `17`（保持）、CI 签名 build-tools `36.0.0`、NDK `28.2.13676358`。
- **分支**：所有改动在 worktree `upgrade-agp9` 分支，不污染 main；逐任务原子 commit；全绿后人工合入。
- **构建命令规约（本机，CLAUDE.local.md）**：
  - 暖缓存后增量（默认）：`env -u http_proxy -u https_proxy -u all_proxy -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY ./gradlew <task> --offline --no-daemon --console=plain 2>&1 | tee <log>`
  - 首拉/缺依赖（含 Google Maven `google()`）：去 `--offline`，加 `-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897`
  - **结果判定只信** `grep -E "^BUILD (SUCCESSFUL|FAILED)" <log>`（不信 bash exit code）；中止后 `Get-Process java -EA SilentlyContinue | Stop-Process -Force` 杀残留 daemon。
  - 构建前查内存（可用 <1000MB 或使用率 >90% 中止问用户）。
  - 同一 worktree 内 gradle 被 file lock 串行，**不并行多路 gradle**。
  - spotless/lint 需 `--init-script gradle/init.gradle.kts --no-configuration-cache`。
  - app 模块编译/测试 task 必须带 flavor（`:app:compileOnlineDebugKotlin` / `:app:testOnlineDebugUnitTest`，裸 `:app:compileDebugKotlin` ambiguous）。
  - android-cli 联网装 SDK 需 `_JAVA_OPTIONS="-Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897 -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897"`。
- **回退边界**：opt-out（`android.builtInKotlin=false`）**仅救内置 Kotlin 冲突一类**，对 Gradle9 API/KSP2/Hilt metadata/变体 API 失败无效。
- **build-logic 文件实际子包**：`build-logic/convention/src/main/kotlin/cn/wj/android/cashbook/buildlogic/`（下方简写需补全）。

---

## Phase 0：准备与基线

### Task 0.1：worktree + SDK + 第三方 Gradle9 兼容查证 + 基线 [机械/查证]

**Files:**
- 无源码改动（环境准备 + 查证报告）

**Interfaces:**
- Produces：worktree `upgrade-agp9`（绝对路径含 `.claude/worktrees/` 或项目内 worktree 路径）；基线全绿记录；第三方 Gradle9 兼容结论；enableJetifier 去留结论。

- [ ] **Step 1：确认 main 干净并拉 worktree**

Run（确认干净）：`git -C "D:/Work/Workspace/Owner/Cashbook" status --short`
Expected：空输出（干净）。非空则停下问用户。
然后用 `EnterWorktree` 工具或 `git worktree add` 从 main 拉 `upgrade-agp9` 分支，并 `git rebase main` 跟上本地 HEAD。

- [ ] **Step 2：android-cli 装齐 SDK 组件**

Run（设代理）：
```
android sdk install "build-tools;36.0.0"
android sdk install "platforms;android-36"
android sdk install "ndk;28.2.13676358"
```
（内嵌 JVM 设 `_JAVA_OPTIONS` 代理见 Global Constraints）
Expected：三组件 installed。

- [ ] **Step 3：第三方 Gradle9 兼容纯查证（前移，修 R3/M7）**

对以下逐一查 release notes 确认存在 Gradle 9 兼容版本，记录到本 task 的查证笔记：
- protobuf gradle plugin（当前 `libs.versions.toml:33` = 0.10.0）
- dependency-guard（`:54` = 0.5.0）
- Roborazzi（`:46` = 1.59.0）、Robolectric（`:50` = 4.16.1）
- androidx-baselineprofile（`:62` = 1.4.1）

Expected：每项给出"已有 Gradle9 兼容版本 = X"或"最新版尚不兼容 → 阻塞，需停下问用户"。

- [ ] **Step 4：探活 + 实拉确认最新版本号（替"已核验最新"）**

Run：`curl -x http://127.0.0.1:7897 --max-time 20 -sI https://repo1.maven.org/maven2/` 与 `... -sI https://dl.google.com/dl/android/maven2/` 各确认 200/Connection established。
确认 AGP 9.2.0 / Gradle 9.6.0 / Kotlin 2.3.0 / 配对 KSP / Hilt 最新具体版本号（官方页），记录到查证笔记替代 spec 的"已核验最新"。

- [ ] **Step 5：enableJetifier 审计（修 M6）**

Run：`env -u http_proxy ... ./gradlew :app:dependencies --configuration onlineReleaseRuntimeClasspath 2>&1 | grep -i "com.android.support" | head`
Expected：空 → 可移除 `enableJetifier`（记结论，Task 2.3 执行移除）；非空 → 保留并记原因。

- [ ] **Step 6：跑现状全绿基线存档**

Run（AGP 8.12 现状）：
```
env -u http_proxy ... ./gradlew testOnlineDebugUnitTest testOfflineDebugUnitTest :lint:test :app:assemble verifyRoborazziDevDebug --no-daemon --console=plain 2>&1 | tee phase0-baseline.log
```
Expected：`grep -E "^BUILD (SUCCESSFUL|FAILED)" phase0-baseline.log` = SUCCESSFUL。存档 phase0-baseline.log 作回归对照。

- [ ] **Step 7：Commit（仅查证笔记，若有）**

```bash
git add docs/superpowers/notes/phase0-precheck.md 2>/dev/null; git commit -m "[chore|build|工具链][公共]Phase0 升级前查证：SDK/第三方Gradle9兼容/版本号/jetifier审计/基线" || echo "无文件改动，跳过 commit"
```

---

## Phase 1：AGP 8.12 下平滑改造（每任务可编译可 commit）

> 这些迁移在 AGP 8.12 下进行——旧变体 API 仍可用、新 `androidComponents`/Variant API 也已存在，故可逐 commit 验证。**新 API 替换代码借 agp-9-upgrade skill + AGP 文档导出，以编译为闸门。**

### Task 1.1：迁移 `core/data` libraryVariants → 新 API [迁移]

**Files:**
- Modify：`core/data/build.gradle.kts:25-36`

**当前代码（已核验）**：
```kotlin
libraryVariants.all {
    preBuildProvider.get().doFirst {
        val intoDir = File(projectDir, "/src/main/assets")
        delete(intoDir)
        copy {
            from(rootDir)
            into(intoDir)
            include("PRIVACY_POLICY.md", "CHANGELOG.md")
        }
    }
}
```
**迁移目标**：`libraryVariants` 在 AGP 9 移除。此逻辑是"构建前把 PRIVACY_POLICY.md/CHANGELOG.md 拷进 assets"，**不依赖 variant 信息**——优先改为普通 `tasks.register<Copy>` + `preBuild.dependsOn(...)`，或 `androidComponents.onVariants`/`beforeVariants` 内 wiring。确切实现借 agp-9-upgrade skill 导出。

- [ ] **Step 1：用 agp-9-upgrade skill / AGP 文档导出替换代码并编辑**（不依赖 variant 的 Copy task 优先）
- [ ] **Step 2：编译验证（AGP 8.12 下）**

Run：`env -u http_proxy ... ./gradlew :core:data:assembleDebug --offline --no-daemon --console=plain 2>&1 | tee t1.1.log`
Expected：`grep -E "^BUILD (SUCCESSFUL|FAILED)" t1.1.log` = SUCCESSFUL；且 `core/data/src/main/assets/PRIVACY_POLICY.md`、`CHANGELOG.md` 确实生成。

- [ ] **Step 3：Commit**

```bash
git add core/data/build.gradle.kts && git commit -m "[refactor|build|工具链][公共]core:data 资产拷贝迁出 libraryVariants（AGP9 预改造）"
```

### Task 1.2：迁移 `Outputs.kt` 变体 API + internal DSL（APK 命名，R2）[迁移]

**Files:**
- Modify：`.../buildlogic/Outputs.kt`（全文，`:19` import BaseAppModuleExtension、`:50` 接收者类型、`:57` applicationVariants.all）

**当前代码（已核验，见 spec §5.B B3）**：`fun BaseAppModuleExtension.configureOutputs(...)` 内 `applicationVariants.all { ... packageApplicationProvider/assembleProvider ... }` 把 release APK 重命名复制到 `outputs/app`。
**迁移目标**：`BaseAppModuleExtension`（internal DSL）+ `applicationVariants` 均 AGP 9 移除。接收者改 `ApplicationExtension`，变体遍历改 `androidComponents.onVariants{}`，APK 产物路径用新 `variant.artifacts.get(SingleArtifact.APK)` 范式 + Copy task。确切实现借 agp-9-upgrade skill 导出。

- [ ] **Step 1：导出替换代码并编辑 Outputs.kt（含 `app/build.gradle.kts:136` 调用处签名适配）**
- [ ] **Step 2：编译验证（build-logic）**

Run：`env -u http_proxy ... ./gradlew :build-logic:convention:compileKotlin --offline --no-daemon --console=plain 2>&1 | tee t1.2a.log`
Expected：BUILD SUCCESSFUL。

- [ ] **Step 3：产物命名回归验证（全 flavor，关键 R2）**

Run：`env -u http_proxy ... ./gradlew :app:assembleOnlineRelease --offline --no-daemon --console=plain 2>&1 | tee t1.2b.log`
Expected：BUILD SUCCESSFUL 且 `outputs/app/Cashbook_*.apk` 命名与基线一致（对照 phase0 产物命名）。

- [ ] **Step 4：Commit**

```bash
git add build-logic/convention/src/main/kotlin/cn/wj/android/cashbook/buildlogic/Outputs.kt app/build.gradle.kts && git commit -m "[refactor|build|工具链][公共]Outputs APK 命名迁 androidComponents/artifacts（AGP9 预改造）"
```

### Task 1.3：迁移 `Variants.kt` configureGenerateFlavors 渠道枚举生成（R2b）[迁移]

**Files:**
- Modify：`.../buildlogic/Variants.kt`（`:25` import、`:52` `is BaseAppModuleExtension`、`:67-86` configureGenerateFlavors）

**当前代码（已核验）**：`configureGenerateFlavors` 经 `applicationVariants/libraryVariants .all{}` 取 `generateBuildConfigProvider.get()`，在其 `doLast` 把 `CashbookFlavor.java` 枚举用 javapoet 写到 `sourceOutputDir`（BuildConfig 源码目录）。
**迁移目标**：`applicationVariants`/`libraryVariants`/`BaseAppModuleExtension` 移除；源码生成改 `variant.sources.java.addGeneratedSourceDirectory(taskProvider, ...)` 范式（新 Variant sources API），javapoet 生成逻辑（`generateFlavor`）复用。确切实现借 agp-9-upgrade skill 导出。`Variants.kt:52` 的 `BaseAppModuleExtension` 判断改新 DSL 类型判定。

- [ ] **Step 1：导出替换代码并编辑 Variants.kt**
- [ ] **Step 2：编译 build-logic**

Run：`env -u http_proxy ... ./gradlew :build-logic:convention:compileKotlin --offline --no-daemon --console=plain 2>&1 | tee t1.3a.log`
Expected：BUILD SUCCESSFUL。

- [ ] **Step 3：渠道枚举生成回归（关键）**

Run：`env -u http_proxy ... ./gradlew :app:assembleOnlineDebug --offline --no-daemon --console=plain 2>&1 | tee t1.3b.log`
Expected：BUILD SUCCESSFUL 且生成的 `CashbookFlavor` 枚举类存在（构建产物 BuildConfig 路径下含 `buildlogic/CashbookFlavor`）、`BuildConfig.FLAVOR` 引用编译通过。

- [ ] **Step 4：Commit**

```bash
git add build-logic/convention/src/main/kotlin/cn/wj/android/cashbook/buildlogic/Variants.kt && git commit -m "[refactor|build|工具链][公共]渠道枚举生成迁 Variant sources API（AGP9 预改造）"
```

### Task 1.4：迁移 `app` configGenerateReleaseFile 变体 API（C1）[迁移]

**Files:**
- Modify：`app/build.gradle.kts:23`（import ApplicationVariant）、`:133`（调用）、`:249-250`（函数签名 + applicationVariants.all）、`:16`（视情况移除 `@file:Suppress("DEPRECATION")`）

**当前代码（已核验）**：`configGenerateReleaseFile(applicationVariants)` → `fun Project.configGenerateReleaseFile(applicationVariants: DomainObjectSet<ApplicationVariant>) { applicationVariants.all { mergeAssetsProvider.get().doFirst { ...生成 RELEASE.md... } } }`。
**迁移目标**：`applicationVariants` + `com.android.build.gradle.api.ApplicationVariant` + `mergeAssetsProvider` 均 AGP 9 移除。RELEASE.md 生成不依赖 variant 数据（只读 CHANGELOG.md + env BUILD_TAG_NAME），优先改为普通 task 挂 `mergeAssets`/`preBuild` 之前，或 `androidComponents.onVariants`。确切实现借 agp-9-upgrade skill 导出。

- [ ] **Step 1：导出替换代码并编辑 app/build.gradle.kts（含移除不再需要的 import 与 `@file:Suppress("DEPRECATION")`）**
- [ ] **Step 2：编译验证**

Run：`env -u http_proxy ... ./gradlew :app:compileOnlineReleaseKotlin --offline --no-daemon --console=plain 2>&1 | tee t1.4a.log`
Expected：BUILD SUCCESSFUL（配置阶段通过、无 DEPRECATION 残留警告致错）。

- [ ] **Step 3：RELEASE.md 生成回归（模拟 CI）**

Run：`env -u http_proxy ... BUILD_TAG_NAME=v1.2.0_26062514 ./gradlew :app:assembleOnlineRelease --offline --no-daemon --console=plain 2>&1 | tee t1.4b.log`
Expected：BUILD SUCCESSFUL 且生成 RELEASE.md（CHANGELOG 提取逻辑不变）。

- [ ] **Step 4：Commit**

```bash
git add app/build.gradle.kts && git commit -m "[refactor|build|工具链][公共]configGenerateReleaseFile 迁出变体 API（AGP9 预改造 C1）"
```

### Task 1.5：迁移 internal DSL 类型（Badging / Jacoco / AppConvention，H1/H2/B2）[迁移]

**Files:**
- Modify：`AndroidApplicationConventionPlugin.kt:24,63`（BaseExtension→ApplicationExtension/CommonExtension）
- Modify：`.../buildlogic/Badging.kt:22,111`（BaseExtension；确认 `:126,128` sdkDirectory/buildToolsVersion 在新类型可用，L2）
- Modify：`AndroidApplicationJacocoConventionPlugin.kt:20,30`（BaseAppModuleExtension→ApplicationExtension）

**迁移目标**：`BaseExtension` / `BaseAppModuleExtension`（internal/旧 DSL）→ 公共新 DSL（`ApplicationExtension`/`CommonExtension`）。确切实现借 agp-9-upgrade skill 导出；`sdkDirectory`/`buildToolsVersion` 若新类型无，则改从 `androidComponents`/SDK 组件 provider 取。

- [ ] **Step 1：导出替换代码并编辑 3 文件**
- [ ] **Step 2：编译 build-logic**

Run：`env -u http_proxy ... ./gradlew :build-logic:convention:compileKotlin --offline --no-daemon --console=plain 2>&1 | tee t1.5a.log`
Expected：BUILD SUCCESSFUL。

- [ ] **Step 3：badging/jacoco 任务回归**

Run：`env -u http_proxy ... ./gradlew :app:assembleOnlineDebug --offline --no-daemon --console=plain 2>&1 | tee t1.5b.log`
Expected：BUILD SUCCESSFUL。

- [ ] **Step 4：Commit**

```bash
git add build-logic/convention/src/main/kotlin/ && git commit -m "[refactor|build|工具链][公共]internal DSL（BaseExtension/BaseAppModuleExtension）迁新 DSL（AGP9 预改造 H1/H2）"
```

### Task 1.6：targetSdk 35→36 + 行为审查（M8）[机械 + 审查]

**Files:**
- Modify：`.../buildlogic/ProjectSetting.kt:44`

- [ ] **Step 1：改常量**

`ProjectSetting.kt:44`：`const val TARGET_SDK = 35` → `const val TARGET_SDK = 36`

- [ ] **Step 2：编译 + 单测回归**

Run：`env -u http_proxy ... ./gradlew :app:assembleOnlineDebug testOnlineDebugUnitTest --offline --no-daemon --console=plain 2>&1 | tee t1.6.log`
Expected：BUILD SUCCESSFUL。

- [ ] **Step 3：API 36 行为审查（记录到笔记，不命中也显式记）**

逐条核对 API 36 相对 35 的 behavior-changes（前台服务类型/PendingIntent mutability/广播注册/存储访问/edge-to-edge/WebDAV 后台同步 WorkManager），命中项标记到 Phase 3 journey 验证。

- [ ] **Step 4：Commit**

```bash
git add build-logic/convention/src/main/kotlin/cn/wj/android/cashbook/buildlogic/ProjectSetting.kt && git commit -m "[feat|build|工具链][公共]targetSdk 35→36（含 API36 行为审查）"
```

---

## Phase 2：切版本（拆细可 commit 子步，修 M1）

### Task 2.1（2a）：Gradle 8.13→9.6.0 + pin sha256（AGP 仍 8.12，H4）[机械]

**Files:**
- Modify：`gradle/wrapper/gradle-wrapper.properties:3`（+ 新增 `distributionSha256Sum`）

- [ ] **Step 1：改 distributionUrl + pin sha256**

`:3` `gradle-8.13-all.zip` → `gradle-9.6.0-all.zip`；新增一行 `distributionSha256Sum=<从 https://gradle.org/release-checksums/ 取 9.6.0 -all.zip SHA-256>`。

- [ ] **Step 2：验证 Gradle 9.6 下现有构建仍绿（AGP 8.12 支持 Gradle 9.x）**

Run（首拉 Gradle 9.6 分发，带代理）：`env -u http_proxy ... ./gradlew :app:assembleOnlineDebug -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897 --no-daemon --console=plain 2>&1 | tee t2.1.log`
Expected：BUILD SUCCESSFUL（wrapper 用 9.6.0，sha256 校验通过）。若 AGP 8.12 与 Gradle 9.6 不兼容报错 → 回退到本任务前、记录后停下问用户（可能需先 2c/2d）。

- [ ] **Step 3：Commit**

```bash
git add gradle/wrapper/gradle-wrapper.properties && git commit -m "[build|build|工具链][公共]Gradle 8.13→9.6.0 + pin distributionSha256Sum（2a）"
```

### Task 2.2（2c）：内置 Kotlin + Hilt + KSP2 PoC（最脆弱，R1）[迁移/验证]

**Files:**
- 临时改动（PoC，验证后并入 2d 或回退）

**目标**：在一个代表性 Android module（建议 `core:data`，含 Hilt+KSP）验证：移除 `kotlin.android` apply + 内置 Kotlin + Kotlin 2.3 + KSP2 + Hilt ≥2.58 共存编译。**Hilt+KSP2+内置 Kotlin metadata 是无 opt-out 退路的最高危点。**

- [ ] **Step 1：在 PoC module 上切 Kotlin 2.3 + KSP 配对版 + Hilt ≥2.58 + 移除 kotlin.android + 内置 Kotlin（借 agp-9-upgrade skill）**
- [ ] **Step 2：编译验证 Hilt/KSP 生成**

Run：`env -u http_proxy ... ./gradlew :core:data:compileDebugKotlin -D...proxy --no-daemon --console=plain 2>&1 | tee t2.2.log`
Expected：BUILD SUCCESSFUL，无 Dagger/Hilt metadata version 错误、无 KSP2 不兼容。

- [ ] **Step 3：判定**

PoC 通过 → 进 Task 2.3 全量切；不通且为内置 Kotlin 冲突 → 记录 `android.builtInKotlin=false` opt-out fallback；不通且为 Hilt metadata/KSP2 → **停下问用户**（无 opt-out 退路，需调 Hilt/KSP 版本或方案）。

- [ ] **Step 4：Commit（PoC 结论笔记，PoC 代码改动并入 2d 不单独 commit）**

```bash
git add docs/superpowers/notes/phase2-builtin-kotlin-poc.md 2>/dev/null && git commit -m "[chore|build|工具链][公共]内置Kotlin+Hilt+KSP2 PoC 结论（2c）" || echo "无笔记，跳过"
```

### Task 2.3（2d）：AGP+Kotlin+KSP+Hilt 全量切换 + 内置 Kotlin + Java17 + gradle.properties [机械 + 迁移]

**Files:**
- Modify：`gradle/libs.versions.toml`（AGP `:60`→9.2.0、kotlin `:11`→2.3.0、ksp `:29`→配对版、hilt `:39`→≥2.58、android-tools `:61`/android-lint `:65` 按 plan 核实结果、第三方按 Task 0.1 查证 pin）
- Modify：移除 `kotlin.android` apply 共 4 处：`AndroidApplicationConventionPlugin.kt:41`、`AndroidLibraryConventionPlugin.kt:41`、`AndroidTestConventionPlugin.kt:35`、`baselineProfile/build.gradle.kts:23`
- Modify：`.../buildlogic/KotlinAndroid.kt:34,52`（内置 Kotlin 下 `KotlinAndroidProjectExtension` 获取方式，B4）、`:47,48,67,68,86`（Java 17 自动跟随常量）
- Modify：`.../buildlogic/ProjectSetting.kt:72`（`javaVersion = JavaVersion.VERSION_17`）
- Modify：`gradle.properties`（AGP 9 flags + enableJetifier 按 Task 0.1 结论）

- [ ] **Step 1：改版本号（libs.versions.toml，机械，用 Task 0.1 实拉的确切版本）**
- [ ] **Step 2：移除 4 处 kotlin.android apply + KotlinAndroid.kt 内置 Kotlin 适配（借 agp-9-upgrade skill）**
- [ ] **Step 3：ProjectSetting.kt:72 javaVersion 11→17（机械）**

`val javaVersion = JavaVersion.VERSION_11` → `JavaVersion.VERSION_17`

- [ ] **Step 4：gradle.properties AGP 9 flags 梳理（机械）**

按 Task 0.1 结论处理 `android.enableJetifier`（移除或保留）；确认 AGP 9 默认翻转无需额外设（`resValues`/`shaders` 已显式、resValue 0 命中；targetSdk 显式声明，`defaultTargetSdkToCompileSdkIfUnset` 无影响）。

- [ ] **Step 5：build-logic 编译**

Run：`env -u http_proxy ... ./gradlew :build-logic:convention:compileKotlin -D...proxy --no-daemon --console=plain 2>&1 | tee t2.3a.log`
Expected：BUILD SUCCESSFUL。

- [ ] **Step 6：全量编译（跨模块 Hilt 图，带 flavor）**

Run：`env -u http_proxy ... ./gradlew :app:compileOnlineDebugKotlin -D...proxy --no-daemon --console=plain 2>&1 | tee t2.3b.log`
Expected：BUILD SUCCESSFUL。失败按 systematic-debugging 定位；内置 Kotlin 冲突类可用 `android.builtInKotlin=false` opt-out（记技术债）。

- [ ] **Step 7：Commit**

```bash
git add gradle/libs.versions.toml gradle.properties build-logic/ baselineProfile/build.gradle.kts && git commit -m "[build|build|工具链][公共]切 AGP9.2/Kotlin2.3/KSP/Hilt + 内置Kotlin + Java17（2d）"
```

### Task 2.4：第三方插件 pin 收尾（按构建报错，R3/R6）[机械]

**Files:**
- Modify：`gradle/libs.versions.toml`（Roborazzi/Robolectric/protobuf/dependency-guard/baselineprofile/lint-tools 按需）

- [ ] **Step 1：按 Task 0.1 查证 + 构建报错 pin 各第三方到 Gradle9/AGP9 兼容版**
- [ ] **Step 2：lint module 编译 + 检测器测试（R6 硬项）**

Run：`env -u http_proxy ... ./gradlew :lint:compileKotlin :lint:test --no-daemon --console=plain 2>&1 | tee t2.4a.log`
Expected：BUILD SUCCESSFUL（`DesignDetectorTest`/`TestMethodNameDetectorTest` 通过，lint-api 无破坏性变更或已适配）。

- [ ] **Step 3：全模块编译 + 单测**

Run：`env -u http_proxy ... ./gradlew testOnlineDebugUnitTest testOfflineDebugUnitTest --no-daemon --console=plain 2>&1 | tee t2.4b.log`
Expected：BUILD SUCCESSFUL。

- [ ] **Step 4：Commit**

```bash
git add gradle/libs.versions.toml && git commit -m "[build|build|工具链][公共]第三方插件 pin Gradle9/AGP9 兼容版 + lint module 验证（R3/R6）"
```

---

## Phase 3：workflow 同步 + 全链路验证

### Task 3.1：GitHub workflow 同步（3 文件，H3/H5）[机械]

**Files:**
- Modify：`.github/workflows/Release.yaml:59`、`.github/workflows/PreRelease.yaml:58`（`BUILD_TOOLS_VERSION "34.0.0"`→`"36.0.0"`）
- Modify（按需）：`Release.yaml:41`/`PreRelease.yaml:40` GMD 镜像（仅构建失败才提到 36）
- Verify（不一定改）：`Build.yaml` androidTest 矩阵 `api-level:[26,30]`、4 处 setup-java（保持 17）、`ci-gradle.properties`

- [ ] **Step 1：改 Release/PreRelease 的 BUILD_TOOLS_VERSION → 36.0.0**
- [ ] **Step 2：确认 `Tlaster/android-sign@v1.2.2` 兼容 build-tools 36**

查 action release/issues；不兼容则升 action 版本。

- [ ] **Step 3：复核 Build.yaml/ci-gradle.properties（Gradle9 废弃属性、4 处 setup-java 保持 17）**
- [ ] **Step 4：Commit**

```bash
git add .github/workflows/ && git commit -m "[build|build|工具链][公共]workflow 同步 build-tools 36 + Gradle9 兼容（H3/H5）"
```

### Task 3.2：全链路验收门 [验证]

**Files:** 无源码改动（验证 + 基线重生）

- [ ] **Step 1：全 flavor×buildType assemble**

Run：`env -u http_proxy ... ./gradlew :app:assemble --no-daemon --console=plain 2>&1 | tee t3.2-assemble.log`
Expected：BUILD SUCCESSFUL（含 app-catalog/ui-test-hilt-manifest 全模块）。

- [ ] **Step 2：spotless + lint + lint module**

Run：`env -u http_proxy ... ./gradlew spotlessCheck --init-script gradle/init.gradle.kts --no-configuration-cache && ./gradlew :app:lintOnlineRelease :app:lintOfflineRelease :lint:lint :lint:test --no-daemon --console=plain 2>&1 | tee t3.2-lint.log`
Expected：BUILD SUCCESSFUL。

- [ ] **Step 3：dependency-guard 重生 + 人工 diff 审查（H8）**

Run：`env -u http_proxy ... ./gradlew dependencyGuardBaseline --no-daemon --console=plain 2>&1 | tee t3.2-dg.log`
然后 `git diff **/dependencies/*.txt` 逐项核对新增依赖来源（均官方坐标、无非预期 group），确认后才纳入 commit。

- [ ] **Step 4：roborazzi verify（截图）**

Run：`env -u http_proxy ... ./gradlew verifyRoborazziDevDebug --no-daemon --console=plain 2>&1 | tee t3.2-robo.log`
Expected：BUILD SUCCESSFUL。工具链导致像素位移时按「截图判失败方法论」甄别后 record（只信本次 run FAILED + compare 图 mtime，不被 stale 图误导），不掩盖回归。

- [ ] **Step 5：android-cli 模拟器 connectedAndroidTest（Room 迁移）**

Run：`android emulator start <device>`（阻塞至就绪）→ `env -u http_proxy ... ./gradlew :core:database:connectedDebugAndroidTest -D...proxy --no-daemon --console=plain 2>&1 | tee t3.2-android.log` → `android emulator stop`
Expected：硬证读 `core/database/build/outputs/androidTest-results/connected/**/TEST-*.xml` 的 `<testsuite tests=".." failures="0" errors="0">`。

- [ ] **Step 6：baseline profile 生成核验（H7）**

Run：`env -u http_proxy ... BUILD_TAG_NAME=v0.0.0_26010100 ./gradlew :app:assembleOnlineRelease -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=BaselineProfile ... --no-daemon --console=plain 2>&1 | tee t3.2-bp.log`（或本机用 connected device）
Expected：`baseline-prof.txt` 生成且非空。

- [ ] **Step 7：签名验收（H5，可选本机模拟）**

对 Release APK：`apksigner verify --verbose <apk>` 确认 v2/v3 scheme 完好、兼容 minSdk 24。

- [ ] **Step 8：Commit 基线**

```bash
git add **/dependencies/*.txt **/screenshots/ 2>/dev/null && git commit -m "[test|build|工具链][公共]升级后 dependency-guard/roborazzi 基线重生（已人工 diff 审查）" || echo "无基线变化"
```

### Task 3.3：节点 2 full-review + 合入决策（流程门）

- [ ] **Step 1：节点 2 `comprehensive-review:full-review`**

对本次 worktree 全 diff 跑 full-review（跨模块 + 构建 + workflow，跑满 5 Phase）；blocking（Critical/High）交付前修复或列出 + 用户授权放行。

- [ ] **Step 2：CI 验证（可选）**

推 `upgrade-agp9` 到远端开 PR，确认 Build.yaml（含 androidTest API 26/30）CI 全绿（验 R8）。

- [ ] **Step 3：合入决策（人工拍板）**

走 `superpowers:finishing-a-development-branch`：全绿 + full-review 通过 → 由用户拍板合入 main；失败 → 留 worktree 迭代。

---

## Self-Review（写完对照 spec）

**1. Spec 覆盖**：§3 版本矩阵→Task 2.1/2.3/2.4；§4 Phase 0-3→Phase 0-3 任务；§5.A 版本声明→T2.1/2.3；§5.B 变体API/internal DSL/kotlin.android/KotlinAndroid→T1.1-1.5/T2.3；§5.C gradle.properties→T2.3 Step4；§5.D workflow→T3.1；§6 验收门→T3.2（assemble/test/lint/lint-module/dg-diff/roborazzi/connectedTest/baseline-prof/apksigner/targetSdk行为）；§7 节点2→T3.3；§8 风险 R1-R9→对应任务（R1→T2.2、R2→T1.2、R2b→T1.3、R3→T0.1/T2.4、R6→T2.4、R7→T3.2、R8→T3.3、R9→T1.6）。无 spec 要求无对应任务。

**2. 占位符扫描**：[迁移] 任务的"借 agp-9-upgrade skill 导出确切代码"非占位符，是对"未经构建验证的 AGP9 API 代码不预写"的事实优先处置，以编译门为交付闸门（plan 头已声明）。机械任务均有确切前后值。

**3. 类型/命名一致**：`configGenerateReleaseFile`/`configureOutputs`/`configureGenerateFlavors`/`generateFlavor`/`CashbookFlavor`/`KotlinAndroidProjectExtension` 等跨任务命名与源码核验一致。

---

## 执行说明（本机节奏，CLAUDE.local.md）

本升级**大量本地 Gradle 构建 + 会话可能中断**：后台 workflow + worktree 在会话 interrupt 时会留半成品、daemon 累积 OOM；同 worktree 多路 gradle 被 file lock 串行不加速。故**推荐 controller 亲自串行 inline 执行**（每任务：读 plan→改→跑验证命令→grep BUILD→commit→自核验），commit 落 git 后会话 resume 能续。**不建议后台 Workflow / 多路并行 subagent worktree build。**
