# Phase 0 升级前查证笔记（2026-06-25）

worktree：`D:/cb-agp9` @ 分支 `upgrade-agp9`（从 main HEAD `c3bb00b4` 派生，含本次 spec/plan doc commit）。

## SDK 组件（Step 2，实为 no-op）

`D:/Work/Development/AndroidSdk` 现状：
- build-tools：30.0.3 / 35.0.0 / **36.1.0** / 37.0.0 → 满足 AGP 9 需 build-tools 36 ✓（无需安装）
- platforms：android-26 / 33 / 34 / **36** / 36.1 → compileSdk 36 覆盖 ✓
- **NDK：无目录；全仓 build 文件 grep `ndk/externalNativeBuild/CMakeLists/.cpp/jniLibs` 0 命中 → 项目无原生代码，NDK 28.2 无需安装**（spec 列的 AGP9 默认 NDK 对本项目不适用）

## 第三方 Gradle9/AGP9 兼容查证（Step 3，修 R3/M7）

| 第三方 | 当前版本 | 结论 |
|---|---|---|
| protobuf gradle plugin | 0.10.0 | **已最新**（2026-04，含 Gradle 9.1 deprecation 修复）；issue #793 AGP9 需 build 时观察 |
| **Hilt (Dagger)** | 2.56.2 | **目标精确化 = 2.59.2**：2.58/2.59 在 AGP 9.0 有 `ComponentTreeDeps` 编译问题（Dagger #5099），**2.59.2 修复**，AGP9 须 ≥2.59.2 |
| KSP | 2.2.0-2.0.2 | 目标 2.3.0（配对 Kotlin 2.3.0） |
| dependency-guard | 0.5.0 | Gradle9 兼容未明确（要求 Gradle 7.4+），构建时验证、必要时升版 |
| Roborazzi | 1.59.0 | 已加 AGP 9.0 支持、Gradle 9 适配进行中，构建时验证、必要时升版 |

## 版本号实拉确认（Step 4，替"已核验最新"）

- AGP **9.2.0**（要求 Gradle ≥9.4.1）、Gradle **9.6.0**、Kotlin **2.3.0**、KSP **2.3.0**、Hilt **2.59.2**。
- （Phase 0 探活：Maven Central + Google Maven `google()` 可达性在首拉构建时随 Gradle 9.6 分发下载一并验证。）

## enableJetifier 审计（Step 5，修 M6）

- 源码 `com.android.support` grep 0 命中 → 倾向移除 `android.enableJetifier=true`。
- 待 Task 2.1 首构建时 `:app:dependencies | grep com.android.support` 复核传递依赖后定。

## 基线（Step 6）

- 命令：`testOnlineDebugUnitTest testOfflineDebugUnitTest :lint:test :app:assemble verifyRoborazziDevDebug --offline --no-daemon`
- 签名：`signing.versions.toml` 不入库，`app/build.gradle.kts:84-87` 缺签名时 Release 回退 debug 签名 → 本地 assemble 不因签名失败。
- 首跑 `--offline` FAILED：`:app:mergeCanaryBenchmarkReleaseNativeLibs` 撞 `dokitx-no-op`/`reorderable` 的 `android-jni` transform offline 无缓存（**环境问题非代码**，CanaryBenchmark 变体本地从未构建过）。
- 带代理重跑暖缓存：**BUILD SUCCESSFUL in 9m 56s（1860 tasks，无 FAILED）** → 改前 main 代码在 worktree 全绿，基线确立。之后增量可 `--offline`。
