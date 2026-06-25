# AGP9 升级执行进度与续做指引（2026-06-25）

> 在 fresh 会话续做：`cd D:/cb-agp9`（worktree，分支 `upgrade-agp9`），读 plan `docs/superpowers/plans/2026-06-25-toolchain-agp9-upgrade.md` + spec + 本笔记 + `phase0-precheck.md`，从下方「下一步」继续。

## 环境（已就绪）
- worktree：`D:/cb-agp9` @ 分支 `upgrade-agp9`（从 main HEAD 派生）。主仓库 `D:/Work/Workspace/Owner/Cashbook` 在 main。
- 构建命令规约见 plan Global Constraints：暖缓存后 `--offline --no-daemon --console=plain`；首拉/缺依赖去 offline 加 `-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897`；判定只信 `grep -E "^BUILD (SUCCESSFUL|FAILED)"`；中止杀 daemon。
- SDK 已满足（build-tools 36.1.0 / platform-36）；**NDK 无需**（项目无原生码）。
- 基线（改前 main 代码）带代理 BUILD SUCCESSFUL，全绿。

## 关键决策/发现（执行期核验）
- **Hilt 目标 = 2.59.2**（非泛 ≥2.58）：2.58/2.59 在 AGP 9.0 有 `ComponentTreeDeps` 编译 bug（Dagger #5099），2.59.2 修复。
- protobuf 插件 0.10.0 已最新（含 Gradle 9.1 deprecation 修复），项目已在此版。
- dependency-guard 0.5.0 / Roborazzi 1.59.0：Gradle9 兼容未明确，Phase 2 构建时验证、必要时升版。
- enableJetifier：源码 `com.android.support` 0 命中，倾向移除（Task 2.1 首构建 `:app:dependencies` 复核传递依赖后定）。
- 迁移模式（已验证）：变体 API → `androidComponents.onVariants`；APK 命名 → `SingleArtifact.APK` + Copy task + `tasks.configureEach{if(name=="assemble$cap")finalizedBy(copy)}`（onVariants 时 assemble 任务未建，用 configureEach 懒挂）；不依赖变体的逻辑 → 普通 task + `preBuild.dependsOn`。
- **KDoc 坑**：注释里 `**/*.apk` 的 `*/` 会提前闭合 `/** */`，示例里避开。
- **Robolectric × targetSdk 36（重要环境项）**：targetSdk 36 → Robolectric 4.16.1 按 targetSdk 选 API 36 模拟，需 `org.robolectric:android-all-instrumented:16-robolectric-13921718-i7`（缓存 `~/.m2/repository/org/robolectric/android-all-instrumented/`）。Robolectric **测试 fork JVM 不继承 Gradle `-Dhttp.proxyHost`**，本机直连 Maven 不通 → offline/无代理下 `MavenArtifactFetcher` SocketException、`classMethod` FAILED（非代码回归、非 Robolectric 不兼容——它认识 API 36 坐标）。**CI 有直连网络自动下载无碍**。本机解法：经代理手动下该 jar+pom+sha1 到 ~/.m2（已做），之后 offline 命中。Robolectric 4.16.1 **支持 API 36**，无需升版。

## 已完成（commit on upgrade-agp9）
- Phase 0：`7d58ee9b`（查证+基线）
- T1.1 core:data libraryVariants → Copy task：`b5283780`
- T1.2 Outputs.kt APK 命名 → onVariants/SingleArtifact：`c4200504`
- T1.3 Variants.kt 渠道枚举生成 → Variant sources API：`5b765165`
- T1.4 app configGenerateReleaseFile 迁出变体 API：`914f3232`
- T1.5 internal DSL（BaseExtension/BaseAppModuleExtension）迁新 DSL：`5cb81f40`
- T1.6 targetSdk 35→36 + API36 行为审查：`3afccbd3`
- **Phase 1 里程碑全绿**：`bd061468`（全 flavor assemble + 双 flavor 单测 + lint:test + roborazzi verify）
- **T2.1 Gradle 8.13→9.5.0（中间过渡）+ pin sha256**：见下决策

## Phase 2 关键决策（2026-06-25 执行期实测）
- **AGP 8.12 ⊥ Gradle 9.6.0（实测，t2.1.log:40）**：Gradle 9.6.0 移除内部 API `org.gradle.api.problems.internal.InternalProblems`，AGP 8.12 的 `com.android.internal.application` 仍依赖它 → 配置阶段 `Could not create service of type InternalProblems` FAILED；官方提示 "or use Gradle 9.5"。
- **决策（用户拍板，Option A）**：T2.1 先到 **Gradle 9.5.0**（AGP 8.12 下验证全绿，隔离 Gradle 核心迁移风险），T2.3 再把 Gradle 9.5.0→**9.6.0** 与 AGP 9.2/Kotlin2.3/KSP/Hilt 一起切。最终目标仍 Gradle 9.6.0。
  - Gradle 9.5.0 `-all.zip` sha256 = `a3c4ba4aca8f0075688b9c5b18939fd28e8cb4357c227da5c1d9f38343791439`
  - Gradle 9.6.0 `-all.zip` sha256 = `87a2216cc1f9122192d4e0fe905ffdf1b4c72cff797e9f733b174e157cadd396`（T2.3 用）
  - AGP 9.2.0 要求 Gradle ≥9.4.1，9.6.0 满足。
- **本机 wrapper 分发下载经代理**：Gradle wrapper 的分发下载**不读**命令行 `-Dhttps.proxyHost`（那是 GradleWrapperMain 程序参数非 JVM 系统属性）；`-all.zip` 经 301 重定向到 CDN 主机本机直连超时。解法：`GRADLE_OPTS="-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897"` 环境变量（进 wrapper JVM，分发下载与 --no-daemon 构建均生效）。
- **KSP 配对版 = `2.3.0`**（新版 KSP 已对齐 Kotlin 版本号、无 `-x.y.z` 后缀；旧格式 `2.2.0-2.0.2`）。
- T2.1 实测 `BUILD SUCCESSFUL in 11m 53s`（AGP 8.12 + Gradle 9.5.0，assembleOnlineDebug）。残留告警：`KotlinAndroid.kt:80` `Project.provideDelegate` 在 Gradle 9.6 弃用（warning 非 error，warningsAsErrors 关，记后续清理）；core:design/core:ui 既有 Compose API 弃用（与升级无关）。

## 下一步（按 plan 顺序）
- **T2.2 内置Kotlin+Hilt2.59.2+KSP2 PoC（core:data，无 opt-out 退路的最高危点）**：切 AGP9.2/Kotlin2.3/KSP2.3/Hilt2.59.2/Gradle9.6.0 + 移 kotlin.android + 内置Kotlin，先 compile core:data 验证（PoC 改动并入 T2.3 commit）。
- **T2.3 全量切**：AGP9.2/Kotlin2.3/KSP2.3/Hilt2.59.2 + Gradle 9.5.0→9.6.0（用上方 sha256）+ 移除 4 处 kotlin.android（含 `baselineProfile/build.gradle.kts:23`）+ KotlinAndroid.kt 内置Kotlin适配 + ProjectSetting.kt:72 javaVersion 11→17 + gradle.properties（enableJetifier 复核）。借 agp-9-upgrade skill。
- **T2.4 第三方 pin + lint module 验证**。
- **Phase 3**：T3.1 workflow（Release/PreRelease build-tools 36 + Build.yaml 复核）→ T3.2 全链路验收门 → T3.3 节点2 full-review + finishing-a-development-branch 人工合入。

## T1.6 API 36 行为审查（M8，基于官方 behavior-changes-16）
targetSdk 35→36 = Android 16 行为变更生效。对 Cashbook 适用性：
- **edge-to-edge 强制（`windowOptOutEdgeToEdgeEnforcement` 弃用、不可 opt-out）** ★相关：Compose 应用，Phase 3 journey 验 UI 不被状态/导航栏裁切。
- **predictive back 默认开（onBackPressed 不调、KEYCODE_BACK 不派发）** ★相关：Navigation Compose，Phase 3 验返回导航/拦截逻辑。
- 大屏(smallestWidth≥600dp)忽略 screenOrientation/resizableActivity/aspectRatio：手机应用，低优先（平板/折叠才显）。
- elegant font（elegantTextHeight 忽略）：中文 UI 轻微排版，低风险。
- Local Network Permission（**opt-in、非 targetSdk 默认强制**）：WebDAV 若指 LAN 地址留意，当前无默认影响。
- scheduleAtFixedRate 只补 1 次：Cashbook 用 WorkManager，N/A。
- 蓝牙 bond / 健康权限 / GPU syscall / MediaStore.getVersion / Safer Intents(opt-in) / App-owned photos：Cashbook 不涉及或 opt-in，N/A。
- **结论**：仅 edge-to-edge + predictive back 需 Phase 3 真机 journey 运行时验证；其余 N/A 或低风险。已显式核对，无遗漏阻断项。

## 合入
全绿 + 节点2 full-review 通过后，由用户拍板 worktree `upgrade-agp9` 合入 main（`finishing-a-development-branch`）。
