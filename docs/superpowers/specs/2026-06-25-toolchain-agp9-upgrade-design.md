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
| KSP | 2.2.0-2.0.2 | **2.3.0** | 已核验最新（兼容 Kotlin 2.2.*+） |
| Hilt (Dagger) | 2.56.2 | **≥2.58（取最新，实施期 pin）** | Hilt 2.58 修 Kotlin 2.3 metadata |
| 源码 Java | 11 | **17** | 已定 |
| CI/运行 JDK | 17 | **17**（≥17 满足，保持） | — |
| compileSdk / minSdk | 36 / 24 | **36 / 24**（不动） | — |
| targetSdk | 35 | **36** | 已定 |
| CI build-tools | 34.0.0 | **36.0.0** | AGP 9 需 build-tools 36 |
| NDK | (默认) | **28.2.13676358** | AGP 9 默认 |

**实施期需 pin / PoC 验证兼容的第三方**（受 Gradle9/AGP9/Kotlin2.3 影响，不凭记忆先断版本）：
Roborazzi 1.59.0、Robolectric 4.16.1、protobuf 插件 0.10.0 + google-protobuf 4.34.1、dependency-guard 0.5.0、androidx-baselineprofile 1.4.1 + benchmark、以及 `android-tools`/`android-lint` 32.2.1（须按 AGP↔tools 偏移对齐 AGP 9.2 对应版本，实施期 pin）。

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
- `android-cli` 装齐 SDK 依赖：build-tools 36.0.0、platform 36、NDK 28.2.13676358。
- 跑现状全绿基线（build + test + lint + roborazzi verify）存档作回归对照。

### Phase 1｜AGP 8.12 下的平滑改造（每步可编译）
- 审计 build-logic 将被移除的变体 API → 迁 `androidComponents{onVariants{}}`。
- 确认各 module `namespace` 唯一。
- 显式开启用到 `resValue` 的 module 的 `buildFeatures.resValues`。
- targetSdk 35→36。
- 这些改动在 AGP 8.12 下逐 commit 验证通过。

### Phase 2｜一次性切版本（本 Phase 内可能瞬时不可编译，仅全绿时 commit）
- **先做内置 Kotlin PoC**：单 module 验证移除 `kotlin.android` 后与 `kotlin.plugin.compose`、`kotlin.plugin.serialization`、KSP、Hilt、Room 共存编译。
- 版本切换：Gradle 8.13→9.6.0、AGP 8.12.0→9.2.0、Kotlin 2.2.0→2.3.0（含 compose/serialization 插件）、KSP→2.3.0、Hilt→≥2.58。
- convention plugin：移除 `kotlin.android` apply 改用内置 Kotlin；源码级别 11→17。
- 按 AGP 9 默认翻转梳理 `gradle.properties`。
- PoC 不通 → 回退 `android.builtInKotlin=false` opt-out，其余照旧。

### Phase 3｜workflow 同步 + 全链路验证
- 改 3 个 yaml（build-tools/GMD/JDK/Gradle 兼容）。
- 本机 + android-cli 模拟器跑通 build/test/lint/roborazzi/androidTest。
- dependency-guard、roborazzi 基线按需重生。

## 5. 改动文件清单（实证 file:line）

### A. 版本声明
- `gradle/wrapper/gradle-wrapper.properties:3` — Gradle 8.13→9.6.0
- `gradle/libs.versions.toml` — AGP `:60`、kotlin `:11`、ksp `:29`、hilt `:39`、`android-tools`/`android-lint` `:61,65`；第三方按 PoC pin
- `build-logic/.../ProjectSetting.kt:38,44,72` — targetSdk 35→36、`javaVersion` 11→17（compileSdk 36 不动）

### B. build-logic AGP 9 破坏性 API 迁移（核心）
- 移除 `kotlin.android` 插件 apply：`AndroidApplicationConventionPlugin.kt:41`、`AndroidLibraryConventionPlugin.kt:41`、`AndroidTestConventionPlugin.kt:35`
- `BaseExtension` → `ApplicationExtension`/`CommonExtension`：`AndroidApplicationConventionPlugin.kt:24,63`、`Badging.kt:22,111`
- 变体 API `applicationVariants`/`libraryVariants` → `androidComponents{onVariants{}}`：`Variants.kt:71-72`、`Outputs.kt:57`（APK 输出文件名定制是迁移难点，需用新 Variant artifacts API 重写）
- 已用新 API 无需改：`Badging.kt:115`、`Jacoco.kt:59`、`PrintTestApks.kt:41`

### C. gradle.properties（AGP 9 默认翻转梳理）
- `gradle.properties:41-42` `resvalues=false`/`shaders=false` 已显式设（与 AGP 9 默认一致）；用到 `resValue` 的 module 实施期审计并显式开启
- `gradle.properties:32` `android.enableJetifier=true` — AGP 9 下复核是否仍需（Jetifier 渐废，可能告警）
- 确认 AGP 9 flag：`builtInKotlin`（拥抱内置不 opt-out）、`uniquePackageNames`、`enableAppCompileTimeRClass`、`defaultTargetSdkToCompileSdkIfUnset`、`onlyEnableUnitTestForTheTestedBuildType` 的预期行为

### D. GitHub Workflow（限定工具链必需项）
- build-tools：`Release.yaml:59`、`PreRelease.yaml:58` `"34.0.0"`→`"36.0.0"`
- GMD 镜像：`Release.yaml:41`、`PreRelease.yaml:40` `system-images;android-34;...` — 复核是否需提到 36（失败才提）
- JDK：三处 `setup-java@v5 java-version: 17` 保持不动（与源码 Java 17 一致）
- Gradle 兼容：`setup-gradle@v6` + `wrapper-validation-action@v3` 对 Gradle 9.6 的校验确认
- `.github/ci-gradle.properties`：复核 Gradle 9 废弃属性

**明确不动**（避免范围蔓延）：`create-release@v1`/`upload-release-assets@v1`/`publish-release` 等既有 release-body 老问题与工具链升级无关，本次不改。

## 6. 验证、回退与测试

### 验证（每 Phase 完整链路，本机 + android-cli）
- Phase 0 基线：现状全绿存档作回归对照。
- Phase 2/3 验收门：
  - `:app:assemble` 全 flavor×buildType
  - `testOnlineDebugUnitTest` / `testOfflineDebugUnitTest` / `:lint:test`
  - spotless check
  - lint（`:app:lintOnlineRelease` 等）
  - `dependencyGuardBaseline` 重生（依赖变 baseline 必变）
  - roborazzi verify（工具链导致像素位移时，按「截图判失败方法论」甄别后再 record，不掩盖回归）
  - android-cli 模拟器 `connectedDebugAndroidTest`（Room 迁移等）
- 本机约束：构建前查内存阈值；经代理拉 Maven Central + offline 暖缓存；`grep "^BUILD (SUCCESSFUL|FAILED)"` 判结果不信 exit code；中止后显式杀 Gradle daemon JVM。

### 回退/错误处理
- 逐 Phase 原子 commit，可 reset/revert。
- 内置 Kotlin PoC 不通 → 回退 `android.builtInKotlin=false` opt-out。
- worktree 善后用 Windows 长路径删除（`\\?\` 前缀）+ `git worktree prune`。

### 测试说明
本次是基础设施升级、**无新增运行时功能**，故不新增单测；验收门 = 既有全套测试 + 迁移测试全绿 + dependency-guard/roborazzi 基线按需更新，这本身即测试。

## 7. 评审门（CLAUDE.md 强制）
- 本设计文档写完 → **节点 1 四维评审**（feasibility/security/reverse/impact）→ 用户评审 spec → writing-plans。
- 实施全部完成 → **节点 2 full-review**（comprehensive-review:full-review）。

## 8. 风险登记
- **R1 内置 Kotlin 与插件栈共存**（外部不确定性）：Phase 2 先 PoC，不通回退 opt-out。
- **R2 变体 API 输出文件名重写**：`Outputs.kt` 的 APK 命名定制迁到新 Variant artifacts API 是已知迁移痛点，需重点验证全 flavor 产物命名不变。
- **R3 第三方插件 Gradle 9 兼容**：Roborazzi/protobuf/dependency-guard 等实施期 PoC 验证，必要时各自升版。
- **R4 本机环境**：代理拉 Maven Central 的 TLS 稳定性可能阻断 instrumented 测试（属环境非代码）；构建内存阈值。
- **R5 Kotlin 2.3 新 deprecation**：`allWarningsAsErrors` 默认关，不阻断；如本机或 CI 开启需处理。
