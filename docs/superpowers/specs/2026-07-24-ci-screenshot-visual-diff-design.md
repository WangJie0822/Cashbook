# CI 截图护栏：非阻塞视觉 diff 报告（C'）+ 基线新鲜度 lint（L）设计

- 日期：2026-07-24
- 决策：节点 1 四维评审（feasibility/security/reverse/impact）后用户拍板 C'+L 组合；方案 A（Linux 基线入库）、B（容差放宽）落选，理由见 §7
- 上游输入：`docs/superpowers/specs/2026-07-09-ci-feature-core-unit-test-coverage-design.md`（前置 7 条 H1/H2/M1/M2/M3/L1/L-E）、`docs/testing/reports/2026-07-23-screenshot-suite-pollution-investigation.md`（§9 机制 P 证伪：渲染=确定性函数(代码+依赖)、与执行轨迹无关）

## 1. 背景与问题

- feature/core 的 676 张 Roborazzi 截图基线全靠本机 Windows record 维护，CI 零截图护栏（现 Build.yaml:78-103 的 verify/record/auto-commit 链是 app flavor task，对库模块空转）。
- 实际痛点两个：① **静默陈旧**——改 UI 忘记重录基线，无任何信号（报告 §8 点名）；② **CI 无像素信号**——UI 回归与依赖漂移全靠本地跑 verify 才可见。
- 跨 OS 硬约束（实证 `1152c817`：ubuntu 曾盲录改写 539/676 张；渐变/表面帧最高 99.5% 像素有差但全部 delta≤1[differ 默认 maxDistance 0.007≈1.8/255 可吸收]、**文本/dynamic 帧 2-5% 像素差 delta 至 37[超出吸收，verify 必红]**）：**本机基线在 ubuntu 上 verify 不可行**——吸收跨 OS 文本帧噪声所需的 changeThreshold（≥0.05）严格覆盖真回归信号下限（padding 1dp 变异=3.48%），无缝可穿（feasibility F1 + reverse Critical）。

## 2. 方案总览

把「信息」与「阻塞」拆开：

| 组件 | 定位 | 阻塞力 |
|---|---|---|
| **C' 视觉 diff 报告** | 同 runner 双 record 相对比对，输出「本 PR 改了哪些帧」的 artifact + summary | **永不 fail**（基础设施错误除外） |
| **L 基线新鲜度 lint** | UI 源改动但同 PR 未动该模块基线 → 红 | **fail**（可 label 豁免） |
| 清理空转链 | 删 Build.yaml:78-103 四步死链 | — |

不做：Linux 基线入库、改 `changeThreshold`（本机护栏零容差语义不动）、auto-commit 任何 PNG。

## 3. 组件 C'：视觉 diff 报告 job

### 3.0 Phase 0 gating 实验（前置，不通过则 C' 作废只留 L）

feasibility F2：「ubuntu 上 record ×2 逐字节一致」目前只有 Windows 证据，是 C' 成立的唯一 gating 前提。首个实现 PR 中先加一个临时 job：同 runner 连跑两次 `recordRoborazziDebug --rerun-tasks`，两套产物逐字节比对，**全一致才继续实现比对逻辑**；顺带实测单次全量 record 时长（feasibility F4 要求实测，不外推 Windows 数据）。

### 3.1 job 设计

- 独立 job `screenshot-diff`（impact M2/L-E：不与 `Run local tests` 共 task 图，规避 UP-TO-DATE 假绿与 60min 挤兑），`timeout-minutes` 按 Phase 0 实测数据设定。
- **`permissions: contents: read`**（security：C' 无写仓库需求，不复用 `test_and_apk` 的 `contents: write`）。
- 触发：`pull_request`（**禁 `pull_request_target`**，security High 硬约束）；push:main 不跑（无比对对象）。
- 流程：
  1. `actions/checkout` `fetch-depth: 0`；
  2. `git merge-base $BASE_SHA $HEAD_SHA` 计算比对基点——**SHA 一律取 `github.event.pull_request.base.sha`/`head.sha` 经 `env:` 注入**，禁止分支名/`${{ }}` 直接插值 `run:`（security 注入面）；
  3. checkout merge-base → `./gradlew recordRoborazziDebug` → 产物（`*/src/test/screenshots/**`）复制到 `$RUNNER_TEMP/base-shots/` → **`git checkout -- . && git clean -fd` 还原工作树**（feasibility F3：record 直接改写 git 跟踪文件，不还原则切 HEAD 会冲突）；
  4. checkout PR HEAD → 再 record；
  5. 逐文件字节比对两套产物，按 **changed / added / removed** 三类归集（增删截图测试是合法操作，单独列出、不算异常）；
  6. 输出：`GITHUB_STEP_SUMMARY` 写三类清单（模块分组+计数）；changed 帧的新旧 PNG 对打包 `actions/upload-artifact@v7` 上传（复用现有 action，无新增供应链依赖；`retention-days` 设短值如 14，security Low）。
- **全量跑、不做 changed-paths 过滤**（reverse High：过滤会漏 core/design→全 feature 的跨模块影响；core/design 改动是本项目高频操作，过滤净收益小、脚枪大。时长若 Phase 0 实测超预算，再议「core/** 触碰即全量」的保守过滤，本期不做）。
- dependabot PR 照常跑：531 张级漂移面清单正是依赖升级定性（CLAUDE.md ③⑶）需要的现成材料——**非阻塞定位下这是信号不是噪声**（impact M 的「需旁路」在报告定位下不成立）。

### 3.2 C' 的语义边界（明确不承诺）

- C' 回答「这个 PR 改了哪些帧」，不回答「改得对不对」——判定回归/预期仍靠人看 summary/artifact 与本地零容差 verify。
- 合并后 main 不做渲染验证（reverse High 的组合空窗在「报告」定位下不构成缺口：入库基线的权威性仍由本机零容差 verify + L 的新鲜度约束维护）。

## 4. 组件 L：基线新鲜度 lint

### 4.1 判定规则

对 9 个截图模块（feature/tags、feature/records、feature/assets、feature/books、feature/types、feature/settings、feature/budget、core/design、core/ui）：

```
PR diff 触碰 <module>/src/main/**（*.kt / res/**）
且未触碰 <module>/src/test/screenshots/**
→ 该模块报「疑似基线未重录」，job fail
```

- 仅约束**模块自身**基线：core/design 改动只要求 core/design 自身基线更新，**不强制**下游 8 模块级联重录（级联影响由 C' 报告可视化，是否重录由人按 C' 清单判断——避免「改一个 Cb 组件就要重录 676 张」的过重约束）。
- **豁免机制**：PR 打 label `screenshot-freshness-skip` 即跳过（纯逻辑改动——如 ViewModel/repository 调整——不影响渲染时使用；误报成本=打一个 label）。
- 实现形态：workflow 内独立轻量 job（`git diff --name-only $MERGE_BASE...$HEAD` + 脚本归类），**零 Gradle、零渲染**；SHA 经 env 注入同 §3.1 约束。

### 4.2 为什么 L 拿阻塞力

- 零误报面中最便宜的检查：规则纯路径级、可 label 豁免；
- 直击实证痛点（「CI 未 verify 截图期间忘记重录 = 静默陈旧零信号」，报告 §8 / CLAUDE.md ③②旧文）；
- 本仓 `required_status_checks=null`、单人 owner `--admin` merge 常态（impact M）：任何 job 红都是建议性信号，L 的「红」在 merge 前自查时可见即达到目的。

## 5. 组件 3：清理空转截图链

删除 Build.yaml:78-103 四个 step（`verifyRoborazziDevDebug` no-op verify / checkfork / record / auto-commit）：

- 对库模块本就空转（app flavor task），保留只造成「CI 在管截图」的认知误导（该误导已实际发生过，见 CLAUDE.md 2026-07-09 三处纠正记录）；
- 顺带消掉 security Medium：`file_pattern '*/*.png'` 实测匹配 742 文件（676 截图 + 66 生产资源 png，含 launcher 图标）——链删除后自动提交面归零；
- `test_and_apk` job 的 `permissions: contents: write` 因 Dependency Guard auto-baseline 仍需保留，不动。

## 6. 安全约束固化（security 维 finding 全量落盘）

1. 触发保持 `pull_request`；**任何情况下禁止改为 `pull_request_target`**（fork PR 在可写 token + secrets 上下文执行 = CI 提权，Release.yaml 持有 SIGNING_KEY 等 secrets）。
2. 新增 job 一律最小权限：`screenshot-diff` 与 L job 均 `contents: read`。
3. 未受信输入（分支名等）禁止 `${{ }}` 直接插值 `run:`；一律 API SHA + `env:` 间接 + 引号引用。
4. 新增第三方 action（如未来引入 image-diff/comment bot）必须 SHA-pin（与仓库现规范一致）；本期仅复用现有 `actions/checkout`/`upload-artifact`。
5. 截图 fixture 延续合成/脱敏数据惯例（现状已核验：卡号 `6222 **** **** 1234` 等均合成）；新增涉凭据/路径界面的截图测试时评审 fixture。

## 7. 落选方案存档（含否决证据）

- **B（单基线+容差）**：三维独立否决。核心：跨 OS 文本/dynamic 帧 changed 像素 2-5%（delta 至 37，超 differ 默认 maxDistance 吸收）与真回归信号（padding 1dp=3.48%）**同量级重叠**——`changeThreshold` 放到能过跨 OS（≥0.05）即对 ≤5% 真回归全盲，无可行单值；`maxDistance` 抬到吸收 delta=37（≈0.145）更是放弃一切颜色级检出。且两旋钮均在 9 模块共享的 `DefaultRoborazziOptions`（ScreenshotHelper.kt:45-49，captureMultiTheme 硬编码引用），改动连本机护栏一起钝化（2026-07-23 变异实证的「变异红」自证能力丧失）。
- **A（Linux 基线入库）**：机制可行（路径切换仅 2 处硬编码，feasibility 实证），但 auto-commit 回填是自毁式护栏（verify 拦下的回归被 record 录进基线自动放行；历史实锤 `a5f964ec`→`1152c817` 盲录 539 张）；去 auto-commit 改人工录基线 PR 后，dependabot 每月级漂移都需一轮无本机复现能力的 Linux 基线人工评审，维护成本与「本机 record 即权威」不变量冲突；另有滚动镜像无预警全量红、repo 每次重录 +12MB 永久历史。
- **reverse 提出的替代**：cron 周期 record-diff（离线漂移审查）——被 C' 吸收（C' 即按 PR 粒度的同思路，归因更细）；仅 verify core/design——仍需 Linux 基线，同 A 否决理由。

## 8. 验证计划（完整链路）

1. **Phase 0 gating**：ubuntu 同 runner `recordRoborazziDebug` ×2 逐字节一致 + 单次时长实测（不一致 → C' 作废，L 独立继续）。
2. C' 首验：构造含 UI 改动的测试 PR（如临时 padding 变异）→ summary 出现该模块 changed 清单 + artifact 含新旧对；纯文档 PR → 三类全空。
3. L 变异验证（双向）：改 MyTagsScreen padding 不动基线 → L 红；补上基线改动 → L 绿；打 `screenshot-freshness-skip` label → 跳过；纯 docs PR → 不触发。
4. 空转链删除后：Build.yaml 全 job 绿 + `git log` 确认无 auto-commit 行为残留。
5. workflow 变更 PR 需 gh `workflow` scope（既有契约）。

## 9. 开放决策点（writing-plans 前需确认）

- L 的 `src/main/**` 触发面是否排除 `res/values/**`（纯字符串/尺寸资源改动通常影响渲染，倾向**不排除**——字符串变更本就该重录）；
- C' artifact 是否额外生成像素 diff 可视化图（本期倾向只传新旧原图对，diff 图后续增强）；
- Phase 0 若实测单次 record 超 ~20min，是否接受 C' 只在打 `screenshot-diff` label 的 PR 上按需触发（成本控制备选）。
