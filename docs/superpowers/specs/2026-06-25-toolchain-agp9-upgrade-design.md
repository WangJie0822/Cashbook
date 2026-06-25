# 工具链升级设计：AGP 9 / Gradle 9 / JDK 17 + Workflow 同步

> 创建日期：2026-06-25
> 类型：基础设施升级（大版本升级专项）
> 目标：将 AGP、Gradle、JDK（及强制连带的 Kotlin/KSP/Hilt）升级到最新支持版本，同步更新 GitHub workflow 脚本。

## 1. 背景与目标

当前项目工具链落后于最新生态。本次升级把构建工具链拉到最新主版本，并同步 CI workflow 使其在新工具链下可用。

结合工具：实施期使用专属 skill `agp-9-upgrade` 承接 AGP 9 的 DSL/API 自动迁移，使用 `android-cli` 完成 SDK 组件安装（build-tools/platform/NDK）与模拟器 instrumented 测试。

### 当前工具链现状（实证）

| 项 | 当前版本 | 出处 |
|---|---|---|
| Gradle | 8.13 | `gradle/wrapper/gradle-wrapper.properties:3` |
| AGP | 8.12.0 | `gradle/libs.versions.toml:60` |
| Kotlin / KSP | 2.2.0 / 2.2.0-2.0.2 | `gradle/libs.versions.toml:11,29` |
| Hilt (Dagger) | 2.56.2 | `gradle/libs.versions.toml:39` |
| 源码 JDK（source/target compat + jvmTarget） | Java 11 | `ProjectSetting.kt:72`；`JavaVersion.target` 枚举到 17（`:175-181`） |
| compileSdk / targetSdk / minSdk | 36 / 35 / 24 | `ProjectSetting.kt:38,44,41` |
| CI 运行 JDK | 17 (zulu) | `Build.yaml` / `Release.yaml` / `PreRelease.yaml` |
| android-tools / android-lint | 32.2.1 | `gradle/libs.versions.toml:61,65` |

## 2. 已锁定决策（brainstorming 经 AskUserQuestion 收口）

1. **升级边界**：AGP 9.x + Gradle 9.x（最新主版本）。
2. **JDK 目标**：源码语言级别 Java 11→17；CI/运行 JDK 保持 ≥17。
3. **SDK 范围**：targetSdk 35→36；compileSdk 仍 36；minSdk 仍 24。
4. **分支策略**：独立升级分支 + worktree，不污染 main，人工拍板合入。
5. **Kotlin 版本**：升到最新 2.3.0（连带 Hilt ≥2.58、Kotlin 2.3 语言变更）。
6. **执行方案**：方案 A —— P-A「改造先于升级」分步 + 完全拥抱新 DSL/内置 Kotlin，`agp-9-upgrade` skill 辅助；内置 Kotlin 与插件栈不兼容时回退 `android.builtInKotlin=false` opt-out 作 fallback。

## 3. 已核验的目标版本矩阵

来源：[AGP about](https://developer.android.com/build/releases/about-agp)、[AGP 9.0 release notes](https://developer.android.com/build/releases/agp-9-0-0-release-notes)、[Gradle releases](https://docs.gradle.org/current/release-notes.html)、[JetBrains AGP9 迁移](https://blog.jetbrains.com/kotlin/2026/01/update-your-projects-for-agp9/)、[google/ksp releases](https://github.com/google/ksp/releases)、[Dagger releases](https://github.com/google/dagger/releases)。

| 项 | 当前 | 目标 | 状态/出处 |
|---|---|---|---|
| Gradle | 8.13 | **9.6.0** | 已核验最新（2026-06-20） |
| AGP | 8.12.0 | **9.2.0** | 已核验最新（2026-04）；要求 Gradle ≥9.4.1 ✓ |
| Kotlin (KGP) | 2.2.0 | **2.3.0** | 已核验最新 |
| KSP | 2.2.0-2.0.2 | **与最终 Kotlin 2.3.0 精确配对的 KSP（版本号前缀=Kotlin 版本，实施期按 google/ksp release pin）** | 见下 KSP 口径统一说明 |
| Hilt (Dagger) | 2.56.2 | **≥2.58（取最新，实施期 pin 时贴 Dagger release note 原文）** | 【实施期 PoC 确认】"2.58 修 Kotlin 2.3 metadata" 需贴 changelog 原文 |

> **KSP 口径统一**（修节点1 feasibility 指出的三处表述混乱）：AGP 9 最低要求 KSP ≥ 2.2.10-2.0.2，但本项目最终用 Kotlin 2.3.0，故 KSP 须取与 Kotlin 2.3.0 配对的那一版（KSP 版本号前缀=Kotlin 版本），实施期按 [google/ksp releases](https://github.com/google/ksp/releases) 对应 Kotlin 2.3.0 的版本 pin，不在 spec 内先写死具体数字。
>
> **版本号"已核验最新"的限制**（修节点1 feasibility/M9）：上表"最新"断言来自 2026-06 的 web 核验，属随时间漂移的外部事实，且本机知识截止 2026-01 无法独立确证 2026-04/06 发布的 AGP 9.2.0 / Gradle 9.6.0。**实施期 Phase 0 必须实拉确认**（依赖解析 / `sdkmanager --list` 命令输出）替代"已核验最新"的肯定语气。
| 源码 Java | 11 | **17** | 已定 |
| CI/运行 JDK | 17 | **17**（≥17 满足，保持） | — |
| compileSdk / minSdk | 36 / 24 | **36 / 24**（不动） | — |
| targetSdk | 35 | **36** | 已定 |
| CI build-tools | 34.0.0 | **36.0.0** | AGP 9 需 build-tools 36 |
| NDK | (默认) | **28.2.13676358** | AGP 9 默认 |

**实施期需 pin / PoC 验证兼容的第三方**（受 Gradle9/AGP9/Kotlin2.3 影响，不凭记忆先断版本）：
Roborazzi 1.59.0、Robolectric 4.16.1、protobuf 插件 0.10.0 + google-protobuf 4.34.1、dependency-guard 0.5.0、androidx-baselineprofile 1.4.1 + benchmark、以及 `android-tools`/`android-lint` 32.2.1。

> **Phase 0 纯查证前移**（修节点1 M7）：protobuf 插件 / dependency-guard / Roborazzi 是否已有 Gradle 9 兼容版本，是"现在就能查 release notes 得到确定答案"的事实，Phase 0 先纯查证（零改造成本），避免推到 Phase 2 才暴露阻塞、沉没成本高。
>
> **`android-tools`/`android-lint` 32.2.1 现状辨析**（修节点1 M10）：当前 32.2.1 与 AGP 8.12.0 按 +23 偏移并不对应（8.12 常配 31.12.x），32.2.1 反而接近 AGP 9.2 对应版本——**升 AGP 9.2 后可能自然对齐、无需改这两行**，plan 阶段须实测核实而非默认要改。lint-api 是版本最敏感 API（每 AGP 大版本常有破坏性变更），故 `:lint:` module 自身编译 + `DesignDetectorTest`/`TestMethodNameDetectorTest` 全绿列为 Phase 2 验收硬项（见 §8 R6）。
>
> **Google Maven 可达性单列探活**（修节点1 feasibility/M9）：AGP 9.2 插件本体、`lint-gradle`、build-tools/platform/NDK 在 **Google Maven（`dl.google.com`/`google()`）非 Maven Central**（项目 CLAUDE.md 已记 lint-gradle 在 google()）。Phase 0 须把 google() 探活与 Maven Central 探活并列（`curl -x 代理 -sI https://dl.google.com/dl/android/maven2/...`）。android-cli 联网装 SDK 需设 `_JAVA_OPTIONS` 代理（内嵌 JVM 不读 HTTP_PROXY，CLAUDE.local.md 记）。

### AGP 9 破坏性变更（命中本项目栈）

1. 内置 Kotlin（`android.builtInKotlin=true` 默认）：`org.jetbrains.kotlin.android` 插件与新 DSL 不兼容。
2. 变体 API 移除：`applicationVariants` / `libraryVariants` / `testVariants` / `unitTestVariants`。
3. 旧 DSL 类型 `BaseExtension` 等移除（`newDsl=true` 默认）。
4. KSP 须 ≥ 2.2.10-2.0.2（KSP2，KSP1 不兼容 Kotlin 2.3 / AGP 9）。
5. 默认翻转：`useAndroidx`/`uniquePackageNames`/`enableAppCompileTimeRClass`/`defaultTargetSdkToCompileSdkIfUnset`/`onlyEnableUnitTestForTheTestedBuildType`。
6. `buildFeatures.resValues`/`shaders` 默认关闭。
7. opt-out 通道（`newDsl=false`/`builtInKotlin=false`）将在 AGP 10 移除——故本设计选择拥抱、不依赖 opt-out。

## 4. Phase 划分（worktree 内，每 Phase 一组原子 commit）

### Phase 0｜准备与基线
- 从 main 拉 worktree + `upgrade-agp9` 分支；`git rebase` 跟上本地 HEAD。
- **纯查证前移（修节点1 M7/M9）**：查 protobuf 插件 / dependency-guard / Roborazzi 有无 Gradle9 兼容版本；实拉确认 AGP 9.2.0 / Gradle 9.6.0 / Kotlin 2.3.0 / 配对 KSP / Hilt 最新具体版本（替"已核验最新"）；Google Maven（`google()`）+ Maven Central 双探活。
- `android-cli` 装齐 SDK 依赖（内嵌 JVM 设 `_JAVA_OPTIONS` 代理）：build-tools 36.0.0、platform 36、NDK 28.2.13676358。
- 审计 `enableJetifier`：`./gradlew :app:dependencies | grep com.android.support` 据实测决定移除/保留（源码已 grep 0 命中）。
- 跑现状全绿基线（build + test + lint + roborazzi verify）存档作回归对照。

### Phase 1｜AGP 8.12 下的平滑改造（每步可编译）
- 审计并迁移**全部**将被移除的变体 API（§5.B B3：build-logic + `app/build.gradle.kts:133` + `core/data/build.gradle.kts:25`）→ `androidComponents{onVariants{}}`。
- 确认各 module `namespace` 唯一。
- `resValue` 全仓 0 命中（已核验）→ **无需开启 `buildFeatures.resValues`**（去除原悬念，修节点1 M2）。
- targetSdk 35→36（**有 API 36 运行时行为后果，非纯构建项**，行为审查见 §6 验收门，修节点1 M8）。
- 这些改动在 AGP 8.12 下逐 commit 验证通过。

### Phase 2｜切版本（拆细为可 commit 子步，修节点1 M1：避免"一次切 6 维仅全绿才 commit"的不可回退巨型原子操作）

按"能独立验证就独立 commit"拆为子步，每子步全绿即 commit；只有真正强耦合的 AGP+内置Kotlin+KSP+Hilt 才放同一子步：

- **2a Gradle-only**：仅 wrapper 8.13→9.6.0（AGP 仍 8.12）。验证现有构建在 Gradle 9.6 下仍绿（AGP 8.12 支持 Gradle 9.x），独立 commit。同步 pin `distributionSha256Sum`（见 §5.A）。
- **2b 变体 API / internal DSL 收尾**：若 Phase 1 未尽，在此把 §5.B 全部 `applicationVariants`/`libraryVariants`/`BaseAppModuleExtension` 迁完（仍 AGP 8.12 下可编译验证），独立 commit。
- **2c 内置 Kotlin PoC（含 Hilt+KSP2，最脆弱环节）**：单 module 验证移除 `kotlin.android` 后与 `kotlin.plugin.compose`、`kotlin.plugin.serialization`、KSP2、Hilt、Room 共存编译。**Hilt+KSP2+内置 Kotlin metadata 是无 opt-out 退路的最高危点**（opt-out 只救内置 Kotlin 冲突，救不了 Hilt metadata），PoC 必须显式覆盖并通过。
- **2d AGP+Kotlin+KSP+Hilt 切换（强耦合，同子步）**：AGP 8.12→9.2.0、Kotlin 2.2.0→2.3.0（含 compose/serialization 插件）、KSP→配对版、Hilt→≥2.58；convention plugin 移除 `kotlin.android`（3 处）+ baselineProfile 模块第 4 处（§5.B）改内置 Kotlin；源码级别 11→17；按 AGP 9 默认翻转梳理 `gradle.properties`。本子步内可能瞬时不可编译，仅全绿时 commit。
- **回退边界**（修节点1 M1）：opt-out（`android.builtInKotlin=false`）**仅覆盖内置 Kotlin 冲突一类**，对 Gradle9 API 移除 / KSP2 不兼容 / Hilt metadata / 变体 API 迁移失败**全部无效**，且 AGP 10 移除 opt-out（属推迟非根治）。故 2a/2b 先落 commit 缩小 2d 的不可回退面；2c PoC 是 2d 的前置闸门。

### Phase 3｜workflow 同步 + 全链路验证
- 改 **3 个 yaml**（含 Build.yaml，§5.D：签名 apksigner build-tools/GMD/JDK/Gradle 兼容）。
- 本机 + android-cli 模拟器跑通 build/test/lint/roborazzi/androidTest；验证 Build.yaml androidTest 矩阵 API 26 在 AGP 9 下可运行（R8）。
- baseline profile 端到端核验（生成且非空）+ 签名 `apksigner verify`（§6 验收门 H5/H7）。
- dependency-guard 基线重生后**人工 diff 审查依赖来源**（H8）；roborazzi 基线按需重生。

## 5. 改动文件清单（实证 file:line）

> 注（修节点1 L1）：build-logic 下文件实际在 `build-logic/convention/src/main/kotlin/cn/wj/android/cashbook/buildlogic/` 子包；下方为简写，implementer 须按完整子包路径定位。

### A. 版本声明
- `gradle/wrapper/gradle-wrapper.properties:3` — Gradle 8.13→9.6.0；**同步 pin `distributionSha256Sum`**（修节点1 H4：从 https://gradle.org/release-checksums/ 取 9.6.0 的 `-all.zip` SHA-256 写入，与 CI `wrapper-validation-action` 形成 jar+distribution 双层校验）
- `gradle/libs.versions.toml` — AGP `:60`、kotlin `:11`、ksp `:29`、hilt `:39`、`android-tools`/`android-lint` `:61,65`（后两者升后或自然对齐，plan 核实）；第三方按 PoC pin
- `cn/wj/android/cashbook/buildlogic/ProjectSetting.kt:38,44,72` — targetSdk 35→36、`javaVersion` 11→17（compileSdk 36 不动；`JavaVersion.target` 的 `:179 VERSION_17` 分支已存在，改 17 不需扩枚举）

### B. AGP 9 破坏性 API 迁移（核心，范围 = build-logic + **业务模块**，全部已 grep 核验）

> 节点1 三维独立收敛：原 spec 只列 build-logic 3 文件，**系统性遗漏业务模块自身的变体 API**。下为全量清单。

**B1 移除 `kotlin.android` apply（共 4 处，非 3 处）**：
- convention：`AndroidApplicationConventionPlugin.kt:41`、`AndroidLibraryConventionPlugin.kt:41`、`AndroidTestConventionPlugin.kt:35`
- **业务模块（修节点1 C3）**：`baselineProfile/build.gradle.kts:23` 直接 `alias(libs.plugins.kotlin.android)`（该模块走 `cashbook.android.test`→上面第 3 处已 apply，此为重复 apply，须连带移除）
- 注：根 `build.gradle.kts:8` 是 `apply false`（仅声明不 apply），无需改

**B2 旧 DSL 类型迁移**：
- `BaseExtension`→`ApplicationExtension`/`CommonExtension`：`AndroidApplicationConventionPlugin.kt:24,63`、`Badging.kt:22,111`（迁 CommonExtension 时确认 `Badging.kt:126,128` 的 `sdkDirectory`/`buildToolsVersion` 属性在新类型可用——修节点1 L2）
- **`BaseAppModuleExtension`（internal DSL，AGP9 移除，修节点1 H1/H2）**：`AndroidApplicationJacocoConventionPlugin.kt:20,30`、`Variants.kt:25,52,71`、`Outputs.kt:19,50`

**B3 变体 API `applicationVariants`/`libraryVariants` → `androidComponents{onVariants{}}`**（全部 grep 核验）：
- build-logic：`Variants.kt:71-72`、`Outputs.kt:57`
- **业务模块（修节点1 C1/C2）**：`app/build.gradle.kts:23`(import ApplicationVariant)`,:133,:249-250`（`configGenerateReleaseFile` 生成 CI RELEASE.md，依赖 `mergeAssetsProvider`，`:16 @file:Suppress("DEPRECATION")` 即为压制此处弃用）；`core/data/build.gradle.kts:25`（`libraryVariants.all` 拷隐私政策/CHANGELOG 到 assets，影响"关于"页内容）
- **两个非平凡重写难点（升级为同级风险，见 §8 R2/R2b）**：`Outputs.kt:57` APK 输出文件名定制；`Variants.kt:67-86 configureGenerateFlavors` 经 `generateBuildConfigProvider.sourceOutputDir` 生成渠道枚举类 `CashbookFlavor.java`——新 API 不暴露 `generateBuildConfigProvider`，源码注入须改 `variant.sources.java.addGeneratedSourceDirectory(...)` 范式；迁移失败则全渠道枚举不生成、全模块编译失败

**B4 内置 Kotlin 迁移核心点（修节点1 M4）**：`KotlinAndroid.kt:34,52` 经 `configureKotlin<KotlinAndroidProjectExtension>()` 配置 Kotlin 编译选项——内置 Kotlin（builtInKotlin=true）后 `KotlinAndroidProjectExtension` 的获取方式是否仍可用是 PoC 首要验证对象；`KotlinAndroid.kt:47,48,67,68,86` 消费 `javaVersion`，改常量自动跟随。**JVM 库（core:model 等，`JvmLibraryConventionPlugin` 用 `kotlin.jvm`）不受内置 Kotlin 影响**，但受 Kotlin 2.3 影响，PoC 勿误纳（修节点1 L4）。

**B5 protobuf 源集注入（修节点1 M3）**：`core/datastore-proto/build.gradle.kts:44-50` `androidComponents.beforeVariants{ ...sourceSets...srcDir }`——AGP9 newDsl 下 `beforeVariants` 内访问 `android.sourceSets` 并 srcDir 注入的时机/合法性待验，纳入 R3。

**已用新 API 无需改**：`Badging.kt:115`、`Jacoco.kt:59`、`PrintTestApks.kt:41`（均 `onVariants`）。

### C. gradle.properties（AGP 9 默认翻转梳理）
- `gradle.properties:41-42` `resvalues=false`/`shaders=false` 已显式设（与 AGP 9 默认一致）。**`resValue` 全仓 grep 0 命中（已核验，修节点1 M2）→ 该翻转项无回归面，无需任何 module 显式开启 `buildFeatures.resValues`**（去除原"实施期审计"悬念，减负）
- `gradle.properties:32` `android.enableJetifier=true`（修节点1 M6）：源码 `com.android.support` grep 0 命中，倾向可移除；**Phase 0 审计运行时依赖（`./gradlew :app:dependencies | grep com.android.support`）据实测决定移除/保留**——AGP9 下该 flag 可能从"告警"升级为"硬失败"，不留到 Phase 2 含糊"复核"。移除可消字节码自动改写面（安全收益 + 提速）
- 确认 AGP 9 flag 预期行为：`builtInKotlin`（拥抱内置不 opt-out）、`uniquePackageNames`、`enableAppCompileTimeRClass`、`onlyEnableUnitTestForTheTestedBuildType`（与本项目已有自定义 gate `AndroidInstrumentedTests.kt:25 disableUnnecessaryAndroidTests` 的交互须核，修节点1 M5）
- `defaultTargetSdkToCompileSdkIfUnset`（修节点1 安全 Low）：本项目 targetSdk 在 `ProjectSetting.kt:44` **显式声明**，该 flag 仅 targetSdk 未设时生效→对本项目无实际影响，仍以显式改 36 为准

### D. GitHub Workflow（范围 = 3 文件，修节点1 H3 补 Build.yaml）
- **签名 apksigner build-tools（修节点1 H5，单列）**：`Release.yaml:59`、`PreRelease.yaml:58` `BUILD_TOOLS_VERSION "34.0.0"`→`"36.0.0"` 实为 `Tlaster/android-sign@v1.2.2` 的签名 apksigner（非编译 build-tools）。须 ① 确认 v1.2.2 兼容 build-tools 36（否则升 action）② 验收加 `apksigner verify --verbose <apk>` 确认 v2/v3 scheme 完好、兼容 minSdk 24
- GMD 镜像：`Release.yaml:41`、`PreRelease.yaml:40` `system-images;android-34;...` — 复核是否需提到 36（失败才提）
- **`Build.yaml`（修节点1 H3，原 spec 漏）**：含 `test_and_apk` + `androidTest` 两 job；`androidTest` 矩阵 `api-level: [26, 30]`（`Build.yaml:145`）跑 `connectedDevDebugAndroidTest`——**AGP 9 是否仍支持 API 26 GMD/emulator 列为待验证假设**（不支持则主 CI androidTest 红、PR 无法合并）
- JDK：**全仓共 4 处** `setup-java@v5 java-version: 17`（`Build.yaml:33-37`+`:172-176` 两 job、`Release.yaml:28-32`、`PreRelease.yaml:27-31`）——AGP 9 需 ≥17，**保持 17 即可**，不动（原"三处"按文件数易漏 androidTest job）
- Gradle 兼容：`setup-gradle@v6` + `wrapper-validation-action@v3`（`Build.yaml:28`）——升后跑 PR 确认 `Validate Gradle Wrapper` 仍 pass（9.6 wrapper jar 须在 action known-good 列表）
- `.github/ci-gradle.properties`：复核 Gradle 9 废弃属性

**明确不动**（避免范围蔓延）：`create-release@v1`/`upload-release-assets@v1`/`publish-release` 等既有 release-body 老问题与工具链升级无关，本次不改；签名 secrets（`SIGNING_KEY`/`ALIAS`/`KEY_STORE_PASSWORD`/`KEY_PASSWORD`）注入方式与 keystore 来源不变（修节点1 L3）。

## 6. 验证、回退与测试

### 验证（每 Phase 完整链路，本机 + android-cli）
- Phase 0 基线：现状全绿存档作回归对照。
- Phase 2/3 验收门：
  - `:app:assemble` 全 flavor×buildType（含 `app-catalog`/`ui-test-hilt-manifest` 全模块 badging/lint baseline）
  - `testOnlineDebugUnitTest` / `testOfflineDebugUnitTest` / `:lint:test`
  - **`:lint:` module 自身编译 + `DesignDetectorTest`/`TestMethodNameDetectorTest` 全绿**（修节点1 R6：lint-api 版本敏感）
  - spotless check
  - lint（`:app:lintOnlineRelease` 等）
  - `dependencyGuardBaseline` 重生后**必须人工 `git diff **/dependencies/*.txt` 逐项核对新增依赖来源**（确认均来自 Google Maven/Maven Central 官方坐标、无非预期 group），确认后才 commit（修节点1 H8，供应链准入控制不可机械跳过）
  - roborazzi verify（工具链导致像素位移时，按「截图判失败方法论」甄别后再 record，不掩盖回归）
  - android-cli 模拟器 `connectedDebugAndroidTest`（Room 迁移等）
  - **baseline profile 生成核验（修节点1 H7）**：Release 构建后核验 `baseline-prof.txt` 生成且非空 + benchmark 跑通（`app/build.gradle.kts:90 automaticGenerationDuringBuild=true`，否则性能优化静默丢失、既有测试抓不到）
  - **签名验收（修节点1 H5）**：`apksigner verify --verbose` 确认 Release/PreRelease 签名 scheme 完好
  - **targetSdk 36 行为审查（修节点1 M8）**：逐条核对 API 36 相对 35 的 behavior-changes（前台服务类型/PendingIntent mutability/广播注册/存储访问/edge-to-edge/WebDAV 后台同步 WorkManager），命中项在模拟器 journey 覆盖，不命中显式记"已核对不命中"
- 本机约束：构建前查内存阈值；经代理拉 Maven Central + **Google Maven** + offline 暖缓存；`grep "^BUILD (SUCCESSFUL|FAILED)"` 判结果不信 exit code；中止后显式杀 Gradle daemon JVM。

### 回退/错误处理
- 逐 Phase 原子 commit，可 reset/revert；Phase 2 已拆细子步（2a/2b 先落 commit 缩小 2d 不可回退面，见 §4）。
- 内置 Kotlin PoC 不通 → 回退 `android.builtInKotlin=false` opt-out（**仅救内置 Kotlin 冲突一类，对 Gradle9 API/KSP2/Hilt metadata/变体 API 失败无效**，修节点1 M1）。
- worktree 善后用 Windows 长路径删除（`\\?\` 前缀）+ `git worktree prune`。

### 测试说明
本次是基础设施升级、**无新增运行时功能代码**，故不新增功能单测；但 **targetSdk 36 是有运行时行为后果的**（见上验收门行为审查），不属"纯构建"。验收门 = 既有全套测试 + 迁移测试 + lint module 测试 + baseline profile 非空 + 签名 verify + targetSdk 行为审查全绿 + dependency-guard/roborazzi 基线按需更新（人工 diff 审查），这本身即测试。

## 7. 评审门（CLAUDE.md 强制）
- 本设计文档写完 → **节点 1 四维评审**（feasibility/security/reverse/impact）→ 用户评审 spec → writing-plans。
- 实施全部完成 → **节点 2 full-review**（comprehensive-review:full-review）。

## 8. 风险登记
- **R1 内置 Kotlin + Hilt + KSP2 三方共存**（外部不确定性，最高危，修节点1 H6）：Hilt 用 KSP 处理注解，KSP2+Kotlin2.3+Dagger metadata 是社区已知高频踩坑点，**且 Hilt 无 opt-out 退路**（不像内置 Kotlin 有 `builtInKotlin=false`）。Phase 2c PoC 必须把 Hilt+KSP2 纳入同一 module 验证；pin Hilt 时贴 Dagger release note 原文证明修 Kotlin 2.3 metadata。
- **R2 `Outputs.kt` APK 输出文件名重写**：迁新 Variant artifacts API 是已知痛点，重点验证全 flavor 产物命名不变。
- **R2b `Variants.kt:67-86 configureGenerateFlavors` 渠道枚举源码生成迁移**（修节点1 H2，与 R2 同级）：依赖旧 `applicationVariants.all` 时机 hook `generateBuildConfig` task 向 `sourceOutputDir` 注入 `CashbookFlavor.java`；新 API 须改 `variant.sources.java.addGeneratedSourceDirectory`，迁移失败则全渠道枚举不生成、全模块编译失败。Phase 2c 单独 PoC。
- **R3 第三方插件 Gradle 9 兼容**：Roborazzi/protobuf 插件/dependency-guard + `core/datastore-proto` 的 `beforeVariants` 源集注入，**Phase 0 纯查证有无 Gradle9 兼容版本**（前移）+ 实施期 PoC，必要时各自升版。
- **R4 本机环境**：代理拉 Maven Central/Google Maven 的 TLS 稳定性可能阻断 instrumented 测试（属环境非代码）；构建内存阈值。
- **R5 Kotlin 2.3 新 deprecation**：`allWarningsAsErrors` 默认关，不阻断；如本机或 CI 开启需处理。
- **R6 lint-api 破坏性变更**（修节点1 M10/H2）：`lint/build.gradle.kts:29 compileOnly(libs.android.lint.api)`，lint-api 每 AGP 大版本常有破坏性变更，`DesignDetector` 用到的 API 可能被移除/改签名→`:lint:` module 编译失败→Material3 ban（CLAUDE.md 强制）失效。Phase 2 验收硬项含 lint module 编译 + 检测器测试。
- **R7 baseline profile 在 AGP 9 的端到端兼容**（修节点1 feasibility/H7）：`baselineProfile` 模块（`com.android.test`）+ benchmark + GMD 是 AGP 大版本升级高危兼容点，Release/PreRelease CI 跑它；验收须核验 profile 实际生成且非空。
- **R8 Build.yaml androidTest API 26 兼容**（修节点1 H3）：AGP 9 可能抬高 instrumented test 最低运行 API，API 26 模拟器跑不起来则主 CI androidTest 红、PR 无法合并。Phase 3 待验证。
- **R9 targetSdk 36 运行时行为变更**（修节点1 M8）：非 AGP 9 强制连带（compileSdk 已 36，targetSdk 保持 35 不阻塞 AGP 9），用户主动选择顺带升。若 Phase 验收暴露 API 36 行为回归且难快速消解，可从本次升级拆出单独处理。
