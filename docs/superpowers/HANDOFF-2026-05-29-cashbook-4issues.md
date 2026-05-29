# 会话交接：Cashbook 四项改进实施（2026-05-29）

> 因上下文占用过高暂停，新会话凭本文件接续。**先读这个，再读 spec + plan。**

## 一句话状态

brainstorming + team-review + writing-plans 全部完成；实施已在 worktree 起步，**Task ②.1 完成并验证通过**，余 **11 个 Task** 待执行。基线已绿、缺失依赖已暖缓存。**下一步：用 Workflow 工具串行编排剩余 11 Task。**

## 工作区 / 分支

- Worktree（必须在此工作，第一步 `pwd` 核对）：
  `D:\Work\Workspace\Owner\Cashbook\.claude\worktrees\feature+cashbook-4issues`
  （bash: `/d/Work/Workspace/Owner/Cashbook/.claude/worktrees/feature+cashbook-4issues`）
- 分支：`worktree-feature+cashbook-4issues`，基于本地 main `93db0941`。
- 进入方式（新会话）：用 `EnterWorktree({path: "<上面绝对路径>"})` 切入已存在的 worktree（不要新建）。

## 关键文档（都在 worktree 内）

- spec：`docs/superpowers/specs/2026-05-29-cashbook-4issues-design.md`（v2，已过四维 team-review，末尾有「评审勘误记录」表）
- plan：`docs/superpowers/plans/2026-05-29-cashbook-4issues.md`（**含每个 Task 的逐字 TDD 代码**，1157 行）

## 提交状态

- `origin/main` 已推送：依赖修复 `1376ea01`（PR #483/#482 合入后修 Canary dependencyGuard 基线）。
- 本地 main `93db0941`（**未推送**）：spec `ec42900c` + plan `93db0941`（纯 doc）。
- worktree 分支已完成：
  - `006587a6` = **Task ②.1**（金额搜索 DAO 加 `amountCent` 匹配 amount/finalAmount）。controller 已核验：diff 逐字符合 plan、`:core:data:testDebugUnitTest` BUILD SUCCESSFUL、工作区干净。
  - ②.1 顺带改了 `RecordRepositoryImpl.kt` 加占位 `amountCent = -1L`（必要编译修复，②.2 替换为真实解析）。

## 剩余 11 个 Task（严格串行，依赖前序 commit）

组1（共享 RecordDao/RecordRepositoryImpl，串行）：**②.2 → ④.1 → ④.2 → ④.3 → ④.4 → ④.5**
组2（共享 MyCategoriesScreen，串行）：**①.1 → ③.1 → ③.2 → ③.3 → ③.4**
两组之间独立，但同一 worktree/分支 → 仍建议整体串行（避免 git 索引 + Gradle file lock 冲突）。

每个 Task 的完整代码/测试/commit 消息见 plan 对应「## Task X.Y」段，逐字可用。

## 下一步执行方式（用户要求用 Workflow 工具）

用 **Workflow 工具**编码串行循环（不要手动逐个派 Agent，也不要 parallel——会撞分支/file lock）：

```
for task in [②.2, ④.1, ④.2, ④.3, ④.4, ④.5, ①.1, ③.1, ③.2, ③.3, ③.4]:
  implementer = await agent(读 plan 的 "## Task <id>" 段，严格 TDD 实现+commit，schema{status,commitSha,buildLine,files,concerns})
  reviewer    = await agent(独立重跑该 Task 测试 + 对照 plan diff + 代码质量，schema{specCompliant,testsPass,issues,verdict})
  if reviewer.verdict == needs_fix: fixer = await agent(按 issues 修+recommit) ; 再 review 一轮
  if 测试仍非绿: break（停链，不在 broken base 上继续）
```
- agent 继承 worktree cwd，但 prompt 必须写死 worktree 绝对路径 + 要求第一步 `pwd` 核对。
- implementer/reviewer 用 sonnet（机械任务）；④.3/④.4/④.5/③.4 是 judgment 重任务可用更强模型。
- 跑完 Workflow 后，**controller 必须 hands-on 终验**（CLAUDE.md 强制：不得直接采纳 subagent「测试通过」强断言；自己 `git show` + 重跑关键测试）。

## 关键环境约束（踩过坑，务必遵守）

1. **代理（强制）**：本机 Maven Central **直连不通**（Connection reset），必须经本地代理 `127.0.0.1:7897`。代理曾间歇性超时——跑 Gradle 前先 `curl -x http://127.0.0.1:7897 --max-time 20 -sI https://repo1.maven.org/...` 探活。
   - Gradle 不读 `HTTP_PROXY` 环境变量，须用系统属性：`-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897`。
   - 所有 gradle 命令前缀清除继承代理：`env -u http_proxy -u https_proxy -u all_proxy -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY`。
2. **缓存已暖**：KSP/Dagger 的 4 个缺失 jar（error_prone_annotations-2.26.1 / google-java-format-1.5 / javac-shaded-9-dev-r4023-3 / checker-compat-qual-2.5.3）已下进 `~/.gradle`。因此 **②.2–③.3 用 `--offline --no-daemon --console=plain`（最快、零网络）**；**仅 ③.4 新增 `sh.calvin.reorderable` 依赖时去掉 --offline、带代理**（须先查 mvnrepository 最新稳定版 + PoC 验证兼容 Compose BOM 2026.05.01，不通则回退方案 B 自实现，见 plan ③.4 Step 1）。
3. **内存（CLAUDE.local.md）**：跑 Gradle 前查内存（可用<1000MB 或 >90% 中止）；`--no-daemon`；TaskStop 不杀 daemon JVM，必要时 `Get-Process java | Stop-Process -Force`。
4. **BUILD 判定**：只信 `grep -E "^BUILD (SUCCESSFUL|FAILED)"`，不看 bash exit code（背景任务 exit 0 是 wrapper 的，gradle 可能 FAILED）。
5. 不用 `git stash`；每 Task 只 stage 相关文件；commit 消息用 plan 里给的。

## 已定决策（勿重新讨论）

- ② 匹配 `amount OR finalAmount`，哨兵用 `keyword.toBigDecimalOrNull()` 判定（**不是** `toAmountCent` 的 0L）。
- ④ 资产余额口径（`verifyAssetBalance:208-238` 规则：源资产 income→+/其余→−，信用卡反向；转入目标→+amount/信用卡−amount；结余=收入−支出=余额净变化）。
- ③ 仅一级分类拖动排序。
- ① 受保护类型点击仍弹菜单、仅渲染「统计数据」项。

## 易错点（来自代码采集，plan 已纳入）

- `FakeRecordDao` 在 `core/data/src/test/.../testdoubles/`（**非** core/testing），已超前实现 amount 子串匹配。
- `RecordModel` 字段是 `charges`（复数）；`RecordTable` 是 `charge`（单数）。
- ③ `TypeDao.queryByLevel` **无 ORDER BY**（隐藏必改点）；`generateSortById` 一级用 count+1 需改 max+1 防撞值；新接口要同步 `FakeTypeDao` + `FakeTypeRepository`。
- ④ 真实记录列表 UI 在 `feature/records/.../screen/AssetInfoContentScreen.kt`（topContent 槽），经 `app/.../ui/MainApp.kt:561` 注入，**不是** AssetInfoScreen.kt；改 UseCase 签名连带 `GetAssetRecordViewsUseCaseTest`/`AssetInfoContentViewModelTest`。
- ④ `RecordViewSummaryModel` 无 assetId/intoAssetId/classification → 资产余额口径需新 query（返回 RecordTable 或带这些字段的 relation）+ domain 计算（plan ④.3 用 GetAssetMonthSummaryUseCase）。

## 全部 Task 完成后

1. controller hands-on 终验所有 commit。
2. 全量回归：`:core:data` `:core:domain` `:feature:types` `:feature:records` `:feature:assets` 单测 + `verifyRoborazziOnlineDebug` + `dependencyGuard` + `:app:assembleOnlineDebug`。
3. **comprehensive-review:full-review**（CLAUDE.md 节点 2 强制）对整个 worktree diff 终审。
4. superpowers:finishing-a-development-branch 决定合入方式（注意本地 main 还有 2 笔未推送 doc 提交）。
