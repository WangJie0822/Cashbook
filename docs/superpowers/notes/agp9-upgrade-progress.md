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

## 已完成（commit on upgrade-agp9）
- Phase 0：`7d58ee9b`（查证+基线）
- T1.1 core:data libraryVariants → Copy task：`b5283780`（验证 assembleDebug 绿 + assets 生成）
- T1.2 Outputs.kt APK 命名 → onVariants/SingleArtifact：`c4200504`（验证 assembleOnlineRelease 绿 + `outputs/apk/Cashbook_v1.2.0_26062517_online.apk`）

## 下一步（按 plan 顺序）
- **T1.3 Variants.kt configureGenerateFlavors 渠道枚举源码生成迁移（R2b，复杂）**：当前 `configureGenerateFlavors<LibraryExtension>()`（`AndroidGenerateFlavorsConventionPlugin.kt:29`）经 `generateBuildConfigProvider.doLast` 把 `CashbookFlavor.java`（javapoet，包 `<namespace>.buildlogic`）写入 BuildConfig sourceOutputDir。迁移目标：自定义 `DefaultTask`（@OutputDirectory + @Input packageName）调 `generateFlavor(pkg, outDir)`，经 `variant.sources.java?.addGeneratedSourceDirectory(taskProvider){it.outputDir}` 注入；`when(this){BaseAppModuleExtension/LibraryExtension}` 改新 DSL。先确认哪些模块 apply `cashbook.android.flavors.generate` + 引用 `.buildlogic.CashbookFlavor`（grep `buildlogic.CashbookFlavor`）。验证：`:app:assembleOnlineDebug` 绿 + 枚举类生成 + `BuildConfig.FLAVOR` 引用编译通过。
- **T1.4 app configGenerateReleaseFile 迁移（C1）**：`app/build.gradle.kts:133,249-250`（`applicationVariants` + `mergeAssetsProvider`）。RELEASE.md 生成不依赖变体数据（只读 CHANGELOG + env），优先改普通 task。移除 `import ...api.ApplicationVariant`(:23) + 视情移 `@file:Suppress("DEPRECATION")`(:16)。验证：`BUILD_TAG_NAME=v1.2.0_26062514 :app:assembleOnlineRelease` 生成 RELEASE.md。
- **T1.5 internal DSL 迁移（H1/H2）**：`AndroidApplicationConventionPlugin.kt:24,63`（BaseExtension）、`Badging.kt:22,111`（BaseExtension，确认 `:126,128` sdkDirectory/buildToolsVersion 在新类型可用）、`AndroidApplicationJacocoConventionPlugin.kt:20,30`（BaseAppModuleExtension）→ ApplicationExtension/CommonExtension。还有 `Variants.kt:25,52` 的 BaseAppModuleExtension（随 T1.3 一并处理）。验证 build-logic 编译 + assembleOnlineDebug。
- **T1.6 targetSdk 35→36**（`ProjectSetting.kt:44`）+ API36 行为审查（见 plan §6）。
- **Phase 2（风险核心，建议 fresh 会话专做）**：T2.1 Gradle 9.6.0+sha256（AGP 仍 8.12 验）→ T2.2 内置Kotlin+Hilt2.59.2+KSP2 PoC（core:data，无 opt-out 退路的最高危点）→ T2.3 全量切 AGP9.2/Kotlin2.3/KSP/Hilt+移除4处kotlin.android（含 `baselineProfile/build.gradle.kts:23`）+KotlinAndroid.kt 内置Kotlin适配+Java17+gradle.properties → T2.4 第三方 pin + lint module 验证。
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
