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

## Phase 2 完成（commit on upgrade-agp9）+ 执行期发现
- **T2.1 Gradle 9.5.0 中间过渡**：`312aee0a`
- **T2.3 全量切 AGP9.2.1/Kotlin2.3.20/KSP2.3.9/Hilt2.59.2/Gradle9.6.0/内置Kotlin/Java17**：`de9be59a`（17 文件）
- **T2.4 纯验证（无新 pin）**：lint module(R6) `:lint:test` 绿 + 全量单测 online+offline 绿（Roborazzi/Robolectric 无需改版）
- **T3.1 workflow build-tools 36.0.0**：`5737ab7a`

### 执行期关键发现（实测，AGP9 迁移）
- **确切版本**：AGP **9.2.1**（最新稳定，9.3+仅 alpha/rc；android-tools/lint 32.2.1 自然对齐无需改 = 验证 spec M10）；Kotlin **2.3.20**（KSP 2.3.9 内嵌 kotlin-stdlib 2.3.20，精确配对）；KSP **2.3.9**（新版纯 patch 号，无 `-x.y.z` 后缀；skill 要求 ≥2.3.6）；Hilt **2.59.2**。
- **CommonExtension 去泛型**：AGP9 `CommonExtension` 移除 6 个类型参数，4 文件 `<*,*,*,*,*,*>`→`CommonExtension`（KotlinAndroid/AndroidCompose/GradleManagedDevices/Variants）。
- **CommonExtension block 方法移除**：`defaultConfig{}`/`compileOptions{}`/`buildFeatures{}`/`testOptions{}`/`productFlavors{}` 等改属性 `.apply{}`（block 方法迁到 ApplicationExtension/LibraryExtension 等具体类型）。
- **旧 DSL 类型移除**：`com.android.build.gradle.{Library,Test}Extension`→`com.android.build.api.dsl.*`（AndroidLibrary/Test/Feature/LibraryCompose 4 plugin，runtime cast 失败）。`HiltConventionPlugin` 移除 `com.android.build.gradle.api.AndroidBasePlugin` import（仅 KDoc 引用）。
- **library targetSdk**：AGP9 库 `defaultConfig.targetSdk` 移除 → `testOptions.targetSdk`（Application/Test 的 defaultConfig.targetSdk 仍在）。
- **compose BOM 必须 api**：Gradle9 约束传播更严，`implementation(platform(bom))` 不再泄漏给消费方 → core:data 经 core:ui 解析无版本 compose 失败；改 `api(platform(bom))`（AndroidCompose.kt）。
- **baselineprofile 仅 alpha 兼容 AGP9**：稳定版 1.4.1 报 "Module :app is not a supported android module"；pin **1.5.0-alpha06**（技术债，待稳定回退）。per-buildType `baselineProfile.automaticGenerationDuringBuild` 移除 → 顶层 consumer DSL（producer 的 managedDevices/useConnectedDevices 仍兼容）。
- **protobuf 0.10.0**（已最新）：经新 `AndroidComponentsExtension` 自动注册 proto 源 → 移除项目手动 `androidComponents.beforeVariants{ android.sourceSets...srcDir }`（旧 source set API cast 失败）。
- **enableJetifier 保留**：运行时仍含 `com.android.support:support-annotations:27.1.0`（jetifier 替换中），按 Phase0 准则非空保留。
- **本机 wrapper 分发下载**：`GRADLE_OPTS="-Dhttp.proxyHost=... -Dhttps.proxyHost=..."`（命令行 -D 不进 wrapper JVM）；代理 TLS 传输间歇不稳（CONNECT 200 但隧道内 reset），用重试循环暖缓存收敛。

## T3.2 全链路验收门 完成（全 8 步通过，实测）
- **① 全 flavor×buildType `:app:assemble`**：BUILD SUCCESSFUL 6m49s（1175 task，4g heap+--max-workers=1 串行）；`outputs/apk/Cashbook_*.apk` 重命名产物齐全（Outputs.kt AGP9 迁移工作）。
- **② spotless + lint + lint module**：spotlessCheck 绿（无格式违规）；`:app:lintOnlineRelease/lintOfflineRelease/:lint:lint` 绿（84 warning+2 hint 无 error，Design detector Material3 ban 通过）；lint-gradle 从 google() 经代理拉。
- **③ dependencyGuardBaseline + 人工 diff 审查**：`926019ec`；变更仅 com.google.dagger:*(Hilt 2.56.2→2.59.2)+org.jetbrains.kotlin:kotlin-stdlib*(2.2→2.3.20)，全官方坐标，无供应链异常。dependency-guard 0.5.0 兼容 Gradle9。
- **④ verifyRoborazziDevDebug**：BUILD SUCCESSFUL，无 compare 图（无像素 diff）→ AGP9/Kotlin2.3 未致截图移位，无需重生基线。
- **⑤ connectedDebugAndroidTest（Room 迁移）**：硬证 `<testsuite DatabaseTest tests=15 failures=0 errors=0>`，Medium_Phone(API30) 模拟器全过。
- **⑥ baseline profile 生成（H7）**：GMD pixel6Api34 在本机 boot 挂死（0 CPU，env 问题非升级）；改手动启 bp_api34(API34) 连接设备生成 → 硬证 `BaselineProfileGenerator tests=1 failures=0` + profile 非空 **26852 行**（有效 ART 格式）→ baselineprofile 1.5.0-alpha06 端到端在 API34 工作。CI 用 GMD（已还原 useConnectedDevices=false）。
- **⑦ apksigner verify**：release APK "Verifies" v2 scheme（本地 debug 签名回退，minSdk24 兼容；CI release keystore 签 v2/v3）。
- **⑧ commit 基线**：dg `926019ec`；roborazzi 无变化。

### T3.2 执行期发现/踩坑
- **本机内存吃紧**：32GB 机 baseline 占用高（Studio+Edge ~25GB），`:app:assemble` 默认 `org.gradle.jvmargs=-Xmx6g`+`org.gradle.parallel=true` 并行 R8 撑到 97.9% → 临时降 heap 4g/3g + `--max-workers=1` 串行（验收后已还原 6g）。所有重 build 均 --max-workers=1。
- **GMD pixel6Api34 boot 挂死**：AGP GMD 自带启动参数在本机 headless boot 挂死（0 CPU、无 adb 设备）；手动 `emulator -avd X -no-window -gpu swiftshader_indirect` 正常 boot（Medium_Phone/bp_api34 均 ~45s 起）→ 绕过 GMD 用手动连接设备。`collect<Flavor>BaselineProfile` 同时依赖 connected+managedDevices，managedDevices 留着会再触发 GMD 挂死。
- **baseline profile 采集需 API 33+**：连接设备 API30 报 "Baseline Profile collection requires API 33+, or rooted device API 28+"（production build 不可 root）→ 建 bp_api34(API34) AVD。
- **代理对 SDK 下载不稳**：sdkmanager 多 manifest 批量拉取撞代理 down 期反复 "IO exception while downloading manifest/Failed to find package"；改 `curl -C -` 续传直下 `x86_64-34_r14.zip`（1.56GB，4 次续传累积）+ 手动解压到 system-images + 补 `Pkg.Path` → sdkmanager --list_installed 识别。
- **代理 TLS 传输间歇不稳**贯穿 Phase2/3：CONNECT 200 但隧道内 reset（~40-60% 丢包），所有下载用重试循环暖缓存收敛（Gradle 部分成功累积缓存）。

## T3.3 节点2 full-review 完成（Phase1-2，用户 checkpoint 收口）
- 四维（code-quality/architecture/security/performance）独立评审全 diff，controller 对所有强断言 hands-on 核验。
- **抓出并修复 1 个 High 回归（full-review 核心价值）**：`app/build.gradle.kts` 两个 `baselineProfile{}` 块矛盾——T2.3 移 per-buildType `automaticGenerationDuringBuild` 时新增顶层块(=true)，漏看既存顶层块(=false,mergeIntoMain=true)，last-write-wins → 净 false → release 基线 profile 自动生成被静默关闭（比 main 经 per-buildType=true 更糟）。字节码证 setter 无 merge=last-write-wins，controller 字节码+main 对比核验属实。**修复 commit `4a7b6caa`**（合并单块 =true+mergeIntoMain=true，`:app:help` 配置通过）。T3.2⑥ 用手动 collect 未走此 build 钩子故验收不可见——节点2 不可替代价值。
- 其余非阻塞：Medium（Outputs configureEach / configuration-cache=false 提速机会 / CI GMD 生成路径+镜像源+heap 未端到端覆盖 / proto 冷缓存）+ Low/pre-existing（PLUGIN_KOTLIN_ANDROID 死常量 / GenerateFlavorTask 输入指纹 / F-2 CI action pin tag / F-3 mvnrepository.com 源 后两者 pre-existing 非本升级引入）。
- 供应链独立核验全干净（sha256 两源逐字节一致 / dependency-guard 全官方坐标 / alpha 仅构建期不入 APK / targetSdk36 edge-to-edge 已实现）。
- **结论：0 未修复 Critical/High，交付就绪**。报告 `.full-review/05-final-report.md`（评审 scratch，不入项目 git）。

## PR #491 CI 迭代（推 PR 跑真实 CI，本机未覆盖的 CI 路径）
PR https://github.com/WangJie0822/Cashbook/pull/491（base main，20 commit=3 doc+17 升级）。CI 暴露 2 个本机遗漏（本机只跑 compileKotlin/testXxxUnitTest，未跑 CI 精确命令）：
- **CI-1 `check -p build-logic` validatePlugins 失败**（`4c02dcc1`）：`enableStricterValidation` 要求每 Task 类型标缓存注解，`GenerateFlavorTask` 缺 → 加 `@DisableCachingByDefault`。本机 compileKotlin 不查、`check` 才查。
- **CI-2 Robolectric API36 ⊥ JDK17**（`3fa6fad0`）：Robolectric 4.16.1 DefaultSdkProvider 字节码证 API36 requires Java21（API34/35=Java17），CI Java17 跑 Robolectric API36（targetSdk 默认）抛 UOE "Android SDK 36 requires Java 21 (have Java 17)"。androidTest **真机 instrumented 部分本就 ✓**（CI "Build...instrumentation tests" pass），仅 Robolectric coverage 步骤受影响。**用户拍板 CI JDK 17→21**（plan"JDK17 保持"假设被证伪；JDK21 仍满足"≥17"、AGP9/Gradle9 支持、Robolectric 跑实际 targetSdk36 与本机/基线一致）。4 处 setup-java 全 bump。
- 本机已复现并验证两修复（`check -p build-logic` 绿 + `testOnlineDebug testOfflineDebug :lint:test` 绿）。

## 合入决策（待人工拍板）
- `finishing-a-development-branch`：全绿 + 节点2 通过 → 由用户拍板 upgrade-agp9 合入 main。
- 合入前建议：推 PR / 打 `v*_pre` tag 实跑一次真实 CI，覆盖本机未端到端验证的 CI 路径（GMD 基线生成+镜像源、proto 冷缓存、CI heap/parallel、签名链 build-tools 36）。
- 独立 follow-up（pre-existing/技术债，非本升级 blocker）：CI action SHA pin（F-2 android-sign 优先）+ 移除 mvnrepository.com 源 + verification-metadata.xml（F-3）；configuration-cache 评估开启；baselineprofile 1.5.0-alpha06 待稳定版回退；`KotlinAndroid.kt:80` provideDelegate Gradle9.6 弃用 warning（warningsAsErrors 关，非阻塞）。

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
