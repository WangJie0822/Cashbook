# CI 覆盖 feature/core 单元测试 + CLAUDE.md 截图认知纠正 设计文档

**日期**：2026-07-09
**分支**：`worktree-ci-coverage-fix`（base `main` @ 90534855）

## 目标

让 CI（`.github/workflows/Build.yaml`）真正执行 feature/core 模块的 JVM 单元测试（当前只跑 app 模块），补上库模块单测的回归护栏；同步纠正项目 CLAUDE.md 中关于「CI 管理截图基线 / CI 会重录 / 本机不录」的错误认知条款。

**截图 verify 上 CI 解耦为独立后续 spec**（见「范围外」），本轮不做。

## 背景与根因（实证）

### CI 覆盖盲区（已核验，run 28991717231）

- `Build.yaml:81` CI `./gradlew verifyRoborazziDevDebug` + `:108` `Run local tests` 用 `testOnlineDebug testOfflineDebug :lint:test`，全是 **app flavor task**。
- feature/core 是库模块（`cashbook.android.library` / `cashbook.jvm.library`，**无 Product Flavor**，仅 Debug 变体），Gradle 无 `:` 前缀只匹配到 app。
- CI 实际日志实证：feature/core `UnitTest` 执行计数 **= 0**，Roborazzi task 全 `:app:`；app 有 0 截图测试 / 0 基线（`:app:verifyRoborazziDevDebug` 空转 no-op）。
- **后果**：feature/core 的 JVM 单测 + 676 张截图基线**完全无 CI 护栏**，全靠本地维护。

### 根因

Cashbook 只在 app 配 Product Flavor（Online/Offline/Canary/Dev），feature/core 库模块无 flavor。CI 命令沿用 Now-in-Android 模板的 app-flavor task 名（NiA 所有模块共享 demo/prod flavor，Cashbook 迁移时只 app 有 flavor、CI 命令未相应调整），导致这些 task 不级联到库模块。

### 聚合 task 可行性（dry-run 实证）

- `testDebugUnitTest`（无前缀）聚合 **21 个库模块** Debug 单测（含有测试源集的 android 库；app 不响应，无歧义无双跑）。
- `:core:model:test`：core:model 是 JVM 库（`cashbook.jvm.library`，7 个 test kt），不在 `testDebugUnitTest` 覆盖内，需单列。
- 唯一有测试的 JVM 库即 core:model（core:datastore-proto 0 测试；lint 已由既有 `:lint:test` 覆盖；build-logic 由 `check -p build-logic` 覆盖）。

## 范围（本轮）

**IN（低风险纯收益）**：
1. 单测覆盖上 CI：`Build.yaml` `Run local tests` step 加 `testDebugUnitTest :core:model:test`（配 `-Proborazzi.test.verify=false`）。
2. M5 时区/locale 脆弱测试处理：本机 UTC/en 预筛全库单测，修真正脆弱的为 zone/locale-robust。
3. CLAUDE.md 4 处截图认知错误纠正。

**OUT（解耦为独立后续 spec）**：
- **截图 verify 上 CI**：需一整套加固（见「截图 verify 独立 spec 前置条件」），单独设计。本轮不加 `verifyRoborazziDebug` / `recordRoborazziDebug` 到 CI，不触碰 676 张基线。

## 节点1 四维评审结论（feasibility / security / reverse / impact，controller 已逐条 hands-on 核验）

评审把方案劈成两半：**单测覆盖 = 低风险纯收益；截图 verify = 高复杂度高风险（2 High + 3 Medium）**。故 scope 收窄为只做单测覆盖，截图 verify 解耦。

### 本轮采纳并处理的 finding

- **M4（Medium，reverse+impact）**：裸 `testDebugUnitTest` 未带 `-Proborazzi.test.verify=false` → 9 截图模块在 `Run local tests` 以默认 capture 模式冗余渲染覆写工作区 + 与 verify step 双跑逼近 timeout。**本轮处理**：单测命令加 `-Proborazzi.test.verify=false`（与既有 `:197` coverage 步骤一致），截图模块只跑单测逻辑、不做截图 capture/verify。
- **M5（Medium，reverse）**：feature/core 单测首次在 UTC CI 执行，`ci-gradle.properties` 无 `user.timezone`/`user.language` 固定；库单测含 `.now()`/`ZoneId.systemDefault()`/Locale（11 文件命中），`Run local tests` 无 `continue-on-error` → 脆弱单测直接判 job 红。**本轮处理**：executing 首步本机以 UTC/en 环境预筛全库单测，实测脆弱数后逐个改 robust。
- **M6（Medium，impact）**：CLAUDE.md 实为 **4 处**命名/认知错误（非 3）。**本轮处理**：全 4 处纠正。
- **L2（Low，三路一致，正面）**：coverage 口径 app-only，库单测门禁 pass/fail 不计入 jacoco `min-coverage-*` → **本轮新增库单测对覆盖率阈值零影响、无回归**。扩展 coverage 到库模块是独立后续项，不在本轮。
- **L3（Low，impact）**：改 `.github/workflows/*` 的 PR 需 `gh auth refresh -s workflow`。**本轮处理**：合并前预置。

### 分歧裁决（controller hands-on）

- **GITHUB_TOKEN 是否触发新 run**：`Build.yaml:97-103` git-auto-commit-action **无 `token:`** → 用默认 GITHUB_TOKEN → 官方递归防护**不触发新 run**。feasibility + reverse 对，impact 的「cancel-in-progress 死循环」证伪撤销。（此裁决属截图 verify 域，仅记录，本轮不涉截图。）

### 留给「截图 verify 独立 spec」的 finding（本轮不做）

- **H1（High）**：`ScreenshotHelper.kt:47 changeThreshold=0f`（像素零容差）× 676 本机基线 × ubuntu-latest 滚动镜像 → 首跑必全 mismatch + 后续每次镜像微升成批 mismatch 的常态化噪声。
- **H2（High）**：`on:` 含 push:main；record 分支 PR-only，push-to-main verify 失败 → checkfork `exit 1`，main CI 红无自愈；触发面从 0 放大到 676。
- **M1（Medium）**：GITHUB_TOKEN 不触发新 run + verify `continue-on-error` → 录出的基线本 PR 内从不被二次 verify 就随绿 PR 合入。
- **M2（Medium）**：首跑 verify+record+capture ≈ 3× 渲染逼近 60min timeout；record 无 continue-on-error，Linux 崩溃即 job 红卡死。
- **M3（Medium）**：首个 PR diff 被 676 PNG 淹没无法 review + git 膨胀。
- **L1（Low，security+feasibility+reverse）**：`file_pattern '*/*.png'` 广口径纳入 66 非截图 png，建议收窄 `*/src/test/screenshots/*.png`。（服务截图 record 路径，随截图 spec 一并做。）

## 设计

### 组件 1 — 单测覆盖上 CI

`Build.yaml` 的 `Run local tests` step（现 `:106-108`）：

```yaml
# 现状
- name: Run local tests
  if: always()
  run: ./gradlew testOnlineDebug testOfflineDebug :lint:test

# 改为
- name: Run local tests
  if: always()
  run: ./gradlew testOnlineDebug testOfflineDebug testDebugUnitTest :core:model:test :lint:test -Proborazzi.test.verify=false
```

- `testDebugUnitTest`：21 库模块 Debug 单测。
- `:core:model:test`：JVM 库单测（7 test kt）。
- `-Proborazzi.test.verify=false`（M4）：9 截图模块只跑单测逻辑，不做截图 capture/verify（截图 verify 不在本轮）。
- app 现有 `testOnlineDebug testOfflineDebug`（→ `:app:test{Online,Offline}DebugUnitTest`）与 `testDebugUnitTest`（app 不响应，`:app:testDebugUnitTest` 计数 0）无歧义、无双跑，新旧并存安全。
- `if: always()`（既有）+ **无 `continue-on-error`**：库单测失败会真实判 job 红——正是所需护栏。故 M5 预筛必须在合并前完成。

### 组件 2 — M5 时区/locale 脆弱测试处理（预筛实测再定）

**executing 首步**（在改 Build.yaml 之前）：本机以 UTC/en 环境跑全库单测，实测脆弱数。

- 预筛方式：给 test fork JVM 注入 `user.timezone=UTC` + `user.language=en` + `user.country=US`（经环境变量 `TZ`/`LANG` 或临时 test systemProperty；executing 时确定本机可行方式并记录）。
- 命中文件 11 个（core/data 3、core/domain 2、core/model 1、feature/records 5）为审计起点；reverse 已抽查 `CalendarViewModelTest` 是 zone-robust（固定 epoch + systemDefault 派生），故实际脆弱数需预筛确认。
- **决策延后到实测**：挂少（1-3 个）→ 逐个改 zone/locale-robust（参考 CalendarViewModelTest 既有模式，不改 convention、不变本机行为）；挂多 → 再评估是否加全局 tz/locale 固定护栏。
- 修复原则：脆弱测试改为**任意时区/locale 都成立**（用固定 epoch + 相对断言，或对 locale 相关断言固定期望），而非依赖某一环境。

### 组件 3 — CLAUDE.md 4 处纠正

将项目 CLAUDE.md 的截图相关条款改为实情（feature/core 截图基线本地 record 维护；CI 当前不 verify feature 截图；本轮起 feature/core **单测**上 CI，**截图 verify** 待独立 spec）：

- **`:57-59`「截图测试」命令**：`verifyRoborazziOnlineDebug`/`recordRoborazziOnlineDebug`（Online flavor，对库截图验不到、app 又无截图）→ 改为 `verifyRoborazziDebug`/`recordRoborazziDebug`（库模块无 flavor 变体，正解），并注明本地录/判。
- **`:182`**：`recordRoborazziDevDebug` + 硬编码行号 `（51-103 行）` → 改为库模块 `recordRoborazziDebug` 事实 + 按 step name 引用避免行号漂移。
- **`:189`**：「CI 走 record 模式 auto-commit 不暴露」错误暗示 CI 录 feature 基线 → 改为「feature/core 截图 CI 当前不 verify/不 record，缺失基线须本地首录」。
- **`:193`**：「Roborazzi 截图基线由 CI 管理，本机不录/不判」+「CI 会重录覆盖」核心错误 → 改为「feature/core 截图基线本地 record 维护（本机 record 即权威 reference），CI 当前不纳入护栏；本轮起 feature/core 单测上 CI，截图 verify 待独立 spec 加固后引入」。

（CLAUDE.md 是团队分发文档，不写 vault `[[...]]` 引用。）

### 组件 4 — 截图 verify 独立 spec（记 backlog，本轮不做）

在 `D:/Vault/.meta/pending-docs.json` 与 memory 记录：截图 verify 上 CI 作为独立后续 spec，前置加固条件见「截图 verify 独立 spec 前置条件」。

## 截图 verify 独立 spec 前置条件（供后续参考，本轮不实现）

后续若要让 CI verify feature/core 截图，必须先解决：
1. **H1 像素容差**：`ScreenshotHelper.kt:47 changeThreshold` 从 0f 提到小容差（如 0.01），吸收跨渲染 antialiasing；或截图校验降级为周期性重录。
2. **H2 push-to-main 无自愈**：verify/record/auto-commit 全套限定 `event==pull_request`，push-to-main 跳过截图校验或 `-Proborazzi.test.verify=false`。
3. **M1 二次 verify**：record 后同 job 再跑一次 `verifyRoborazziDebug`（不 continue-on-error）确保录出基线自洽；或首批 Linux 基线人工确认。
4. **M2 独立 job**：截图 verify/record 拆到独立 job（与单测、APK 构建解耦），评估 60min timeout。
5. **M3 独立录基线 PR**：先用独立「CI 录 Linux 基线」PR 一次性重录 676 张并单独评审合入，再翻开常态 verify，避免淹没功能 PR。
6. **L1 收窄 pathspec**：`file_pattern` → `*/src/test/screenshots/*.png`。
7. **L-E 破 UP-TO-DATE 缓存**（节点2 架构评审补）：截图 verify 若在后续 step 以 `-Proborazzi.test.verify=true` 再跑同一 `:feature:*:testDebugUnitTest`，Gradle 会因该 task 已在 `Run local tests`（`verify=false`）执行而判 UP-TO-DATE、**跳过 verify**（`Build.yaml:105` 现有注释「Run local tests after screenshot tests to avoid wrong UP-TO-DATE」正是此类坑）。故截图 verify 须 `--rerun-tasks` 或拆独立 job/checkout 破缓存，不可与本轮的 `verify=false` 单测跑同一 task 图。

## 验证（完整链路）

1. **组件 2 预筛**：本机 UTC/en 跑 `testDebugUnitTest :core:model:test`（21 库 + JVM 库）全绿（修完脆弱测试）。
2. **组件 1 本机自验**：`testDebugUnitTest :core:model:test -Proborazzi.test.verify=false` 本机 BUILD SUCCESSFUL；确认截图模块不做 capture（`git status` 无 screenshots 工作区改动）。
3. spotlessCheck（`--init-script gradle/init.gradle.kts --no-configuration-cache`）。
4. `:app:compileOnlineDebugKotlin`（跨模块 Hilt 全图不回归）。
5. **CI 实证**：改 Build.yaml 后开 PR，CI Local job 日志应出现 `:feature:*:testDebugUnitTest` / `:core:*:testDebugUnitTest` 执行（feature/core UnitTest 执行计数从 0 → >0），且截图模块无 record auto-commit。
6. **节点 2 full-review**：Build.yaml 逻辑改动触发；CLAUDE.md 纯文档纠正走文档领域豁免（维度收敛 feasibility/impact）。

## 风险与缓解

| 风险 | 缓解 |
|---|---|
| M5 脆弱测试首次 UTC CI 判红 | executing 首步本机 UTC/en 预筛 + 修 robust，合并前全绿 |
| `testDebugUnitTest` 含 0 测试模块 no-op | 无害（Gradle SKIPPED） |
| 库单测显著增加 CI 时长 | 单测比截图渲染轻；`-Proborazzi.test.verify=false` 避免冗余 capture；观测首个 PR CI 时长，超时再评估拆 job |
| 改 Build.yaml 需 workflow scope | 合并前 `gh auth refresh -h github.com -s workflow` |
| coverage 口径不含库模块 | L2：不影响阈值、无回归；扩 coverage 是独立后续项 |
