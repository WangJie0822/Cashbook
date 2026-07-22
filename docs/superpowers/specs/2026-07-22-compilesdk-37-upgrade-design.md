# 升 compileSdk 37 专项设计

> 2026-07-22。目标：compileSdk 36→37，解锁 3 个被 checkAarMetadata 阻塞的 dependabot PR（#501 androidx-hilt 1.4.0 / #498 core-ktx 1.19.0 / #497 lifecycle 2.11.0）。
> targetSdk 保持 36（行为变更审查留独立专项）；经节点 1 四维评审（feasibility/security/reverse subagent + impact 降级 controller hands-on 补验），无 Critical，2 High 全采纳。

## 1. 背景与目标

- 2026-07-22 dependabot 11 PR 批量处置中，#501/#498/#497 因 AAR metadata 要求消费方 compileSdk≥37 被排除（#501 checkAarMetadata 实锤）。
- 本专项：升 compileSdk 至 37 → 串行合入 3 个 PR → 收尾验证。
- **不做**：targetSdk 升级（Robolectric 模拟版本/运行时行为面完全不同，独立专项）。

## 2. 事实基线（均有硬证据）

| 事实 | 证据 |
|---|---|
| 要求值是整数 37（非 36.1） | #501 分支 CI run 29886847612 原文：`requires ... version 37 or later`、`:app is currently compiled against android-36` |
| AGP 支持 compileSdk 37 | AGP 9.2.1 jar 反编译 `ToolsRevisionUtils.MAX_RECOMMENDED_COMPILE_SDK_VERSION = AndroidVersion(37,0)`（字节码 `bipush 37`）；当前 AGP 9.3.0 ≥ 此值（版本上限单调不降；暖缓存后对 9.3.0 jar 补同法一手实证） |
| build-tools 无需动 | 同 jar `MIN_BUILD_TOOLS_REV = 36.0.0`；全仓无 `buildToolsVersion` 显式 pin；Release.yaml `BUILD_TOOLS_VERSION: "36.0.0"` 是 apksigner 工具版本，与 compileSdk 解耦 |
| compileSdk 唯一配置点 | `ProjectSetting.kt:38` 定义、`KotlinAndroid.kt:43` 唯一消费；`configureKotlinAndroid` 被 Application/Library/Test 三 convention 插件统一调用，覆盖 app/app-catalog/全库模块/baselineProfile |
| Robolectric 不受影响 | 全仓测试无 `@Config(sdk=)` pin；模拟版本由 `testOptions.targetSdk=36`（AndroidLibraryConventionPlugin.kt:46）决定，本专项不动 → `TEST_JVM_VERSION=21` 不动 |
| 本机环境缺口 | platforms 仅 26/33/34/36/36.1（无 37）；gradle 缓存 AGP 最高 9.2.1、lint-gradle 最高 32.2.1（#508 合入后本机从未构建） |
| CI 无需改动 | Build.yaml 无 SDK platform 安装步骤/版本引用；既往 compileSdk 35→36 同 workflow 下 CI 持续绿（platform 走 runner 预装/AGP 自动下载） |
| badging 无回归资产 | badging golden 文件不存在（find 零命中）；CheckBadging CI 已注释禁用 |
| 发版链路无涉 | `Outputs.kt` renamer 只消费 versionName/flavorName；versionCode yyMMddHH 约束与 SDK 无关 |

## 3. 变更点

1. `build-logic/convention/src/main/kotlin/cn/wj/android/cashbook/buildlogic/ProjectSetting.kt:38`：`COMPILE_SDK = 36` → `37`（KDoc 注释同步）。
2. 项目 `CLAUDE.md` 文档同步：dependabot compileSdk 兼容条款中「现 36」→「现 37」；「SDK 37 依赖群累积 3 个保持 open」改写为解锁记录。
3. 如本机 lint 全集跑出 baseline 外新增结论：更新对应 `lint-baseline.xml` 随同一 PR 提交（见 §5）。
4. 无其他代码/依赖/CI 变更；无 DB/proto 变更；无新增测试（构建配置变更，无新逻辑，回归由既有全量测试承担）。

## 4. 环境前置（实施第一步）

1. 装 platform：`sdkmanager "platforms;android-37"`（内嵌 JVM 经 `_JAVA_OPTIONS` 走本地代理）；装后抽验退出码 0 + `platforms/android-37/source.properties` 的 `AndroidVersion.ApiLevel=37`。
2. 暖缓存：清继承代理 + `-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897`，先 `curl -x http://127.0.0.1:7897` 探活 dl.google.com/repo1 返 200 再跑；拉 AGP 9.3.0 全家桶（一次 `help` 或编译）+ lint-gradle 32.3.0（一次 lint task）。暖完增量 `--offline`。
3. 对暖缓存拉到的 AGP 9.3.0 jar 补反编译实证 `MAX_RECOMMENDED_COMPILE_SDK_VERSION ≥ 37`（javap 一条命令，把 §2 推断升级为一手证据）。
4. 内存预检（32GB 机器规约）：可用 <1000MB 或使用率 >90% 中止询问。

## 5. 本机验证矩阵（CI 盲区优先，worktree 内执行）

> 排序原则（reverse H-2）：CI 盲区（截图 verify、app-catalog lint）只有本机能兜，优先级最高；CI 已覆盖面（单测/spotless）按「完整链路验证」规范保留。

| # | 命令 | 目的 | 判定 |
|---|---|---|---|
| 1 | `./gradlew check -p build-logic` | 改动模块自身校验 + buildReleaseApkName 单测 | BUILD SUCCESSFUL |
| 2 | `:app:assembleOnlineDebug`（先行冒烟）→ `:app:assemble` 全 8 变体 + `:app-catalog:assembleRelease` + baselineProfile 编译 | aapt2 按 android-37 资源链接/manifest merge/checkAarMetadata/nullability 编译错误全暴露 | BUILD SUCCESSFUL（只信 `^BUILD (SUCCESSFUL|FAILED)` grep） |
| 3 | `testDebugUnitTest :core:model:test testOnlineDebugUnitTest testOfflineDebugUnitTest`（app 用带 flavor task） | 全库回归 | 0 FAILED |
| 4 | `verifyRoborazziDebug` | **676 张截图基线唯一防线（CI 不 verify）**；compileSdk 换 android.jar 后资源重编译的渲染漂移检查 | 0 diff（`_dynamic` 变体既有噪声按 CLAUDE.md 条款甄别） |
| 5 | `:app:lintOnlineRelease :app:lintOfflineRelease :app:lintDevRelease :lint:lint -Dlint.baselines.continue=true` + **`:app-catalog:lintRelease`** | lint 32.3.0 + API 37 新检查；app-catalog baseline 979 条为最大漂移面且 CI 不跑 | 无 baseline 外新增 error；有则更新 baseline 入 PR |
| 6 | `spotlessCheck` | 格式门禁 | 通过 |
| 7 | `dependencyGuard` | 实证 compileSdk 不动 runtime classpath（预期 baseline 0 变化，有变化即异常信号） | 0 diff |
| 8 | badging diff：升级前后各 `aapt2 dump badging`（Online release APK），diff 忽略 versionCode/versionName 行 | manifest 合并面检查（badging 校验 CI 已禁用、无 golden 在守） | 无新增 uses-permission/组件/queries；产物存 scratchpad 不入库 |

## 6. PR 编排（严格串行）

```
compileSdk-37 PR（worktree 分支 → CI 绿 → --admin merge）
  → @dependabot recreate #498 core-ktx → CI 绿 → 审查 → merge
  → @dependabot recreate #497 lifecycle → CI 绿 → 审查 → merge
  → @dependabot recreate #501 hilt     → CI 绿 → 审查 → merge
```

- 顺序按依赖方向 core-ktx → lifecycle → hilt（hilt 依赖 lifecycle、lifecycle 依赖 core；reverse 实测 baseline 中 androidx 传递约束互相 bump，第一个 PR 可能传递拉入后续版本——属预期，审 diff 确认即可）。
- **merge 一个才 recreate 下一个**（各 PR 的 CI auto-baseline commit 在 `dependencies/*.txt` 有交叉冲突面，批量 recreate 会连环过期）。
- 每个依赖 PR 合并前 checklist（security M-3/L-5）：
  1. CI 结论自查（分支保护 required_status_checks=null，CI 非门禁；auto-baseline 二次 commit run 卡 `action_required` 时 `gh api .../approve`）；
  2. **人工审 baseline diff**：`gh pr diff <n>` 中 `dependencies/*.txt` 逐行确认新增/变更仅为预期工件及其已知传递闭包，陌生 group/artifact 即停（dependencyGuard 对 PR 是自愈不阻断，人工审查是唯一控制点）；
  3. 核对 `libs.versions.toml` 版本号是 PR 唯一源码变更（hilt-compiler 是 KSP processor、不在 guard 范围）；
  4. badging diff（同 §5-8 方法）。

## 7. 回滚预案（reverse H-1）

- **依赖 PR 未合入时**：单 PR revert compileSdk commit 即可（无 baseline 变化）。
- **任一依赖 PR 已合入后**：compileSdk 回退会被已合入依赖的 checkAarMetadata 反向阻断（双向锁）——必须「依赖版本 + compileSdk」捆绑在**同一个 revert PR**（CI auto-baseline 自动回填 dependencies baseline），合入前自查 CI 结论；随后对被回退的 bump 逐个 `@dependabot ignore this minor version` 评论，防 dependabot 立即重开。
- 禁止直接 push main 回退（依赖变更必须走 PR 让 CI 回填 baseline，既有契约）。

## 8. 收尾门

- 三 PR 全合入后：`verifyRoborazziDebug` 复跑 + 模拟器冒烟 journey（记账/备份/提醒通知各一条，兜 85 处 `collectAsStateWithLifecycle` 与 hilt-work Worker 的运行时面）。
- **通过前冻结发版 tag**（避免 compileSdk 37 + 旧依赖的中间态正式包）。
- 节点 2 full-review：预计源码 diff <50 行且无接口/安全面变更，按规约降级两维快审（code-reviewer + architect）。
- 项目 CLAUDE.md 同步（§3-2）随 compileSdk PR 提交。

## 9. 节点 1 评审 finding 处置记录

- 四维：feasibility/security/reverse 为 subagent 评审；impact 因 API 522 三次失败降级「三维交叉覆盖 + controller hands-on 补验」（badging golden 不存在 / 发版链路无涉 / DB-proto 不触碰，三缺口已亲验）。
- 无 Critical。High×2（回滚锁死→§7；验证清单错配 CI 盲区→§5 重排）全采纳。
- Medium×7 全采纳：暖缓存前置（§4-2）/ 矩阵补 check-p-build-logic·assemble·dependencyGuard（§5）/ baseline diff 人工审（§6）/ badging diff（§5-8）/ 传递依赖与合入顺序（§6）/ lint 新检查预跑（§5-5）/ 运行时面收尾门（§8）。
- Low×6 全采纳：aliyun 与 CI 不同源以 CI 绿为合入判据 / SDK 装后抽验（§4-1）/ hilt-compiler 核对（§6-3）/ recreate 串行语义（§6）/ CLAUDE.md 文档同步（§3-2）/ AGP 9.3.0 jar 补实证（§4-3）。
- 已证伪质疑：要求值 36.1 之说——CI 原文铁证为整数 37（§2）。
