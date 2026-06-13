# Cashbook 模拟器全量功能测试设计（方案 D）

> 方案 D：**journey 先探索 + 按需补 instrumented**。经节点1 四维 team-review（feasibility/security/reverse/impact）后定稿，所有修正均经 controller hands-on 核验。
> 日期：2026-06-13

## 0. 评审驱动的方向修正（为什么是方案 D）

初稿方案 C（先建确定性 instrumented 守金额）经评审发现前提缺陷，均已 hands-on 核验：
- **金额算法已被现有测试穷尽**：JVM 侧 `RecordAmountTest`/`MoneyTest`(261行)/`SaveRecordUseCaseTest`/`RecordRepositoryImplTest` + DAO 侧 `TransactionDaoTest.kt`（**1009 行**，finalAmount/簇重算/余额联动覆盖于 :508/:568/:611）。新增 on-emulator instrumented 仅多覆盖"UI 接线层"，非算法。
- **finalAmount 不在记账编辑界面渲染**：`EditRecordScreen.kt` 内 `finalAmount` 命中 0；实际在 `LauncherContentScreen.kt:734-740`。初稿断言落点无锚。
- **Compose UI instrumented 在本仓绿地、成本/风险高**：`EditRecordViewModel` 注入 5 Repository+2 UseCase（:78-85）；本仓唯一 Compose instrumented 先例 `core/design/ThemeTest` 用 `createComposeRule` 且不带 Hilt；`HiltConventionPlugin` 未配 androidTest 侧 Hilt 依赖。
- **降级退路与现有 DAO 测试重复**：方案 C 的"PoC 失败降级 DAO 测试"≈ 重测 1009 行已覆盖逻辑，净增量≈0。

**结论**：先用低成本 journey 广覆盖探索找 bug，金额确定性依赖现有测试，**仅当 journey 暴露 UI 层金额/状态异常且现有测试无法复现时，才针对该界面按需补一个确定性测试**。

## 1. 目标与交付物

1. 建立并真跑 `android-cli` journey 套件，对 Cashbook 全功能面做广覆盖探索，产出 PASS/FAIL + bug 清单报告。
2. 金额算法确定性**依赖现有 JVM 单测 + `core/database` DAO androidTest**（本轮跑一次确认绿，作为基线证据纳入报告，不重复造）。
3. **按需 instrumented**（条件触发，非默认产出）：仅当 journey 暴露疑似 UI 层金额/状态异常、且现有测试无法复现时，才在**实际渲染该值的界面**补一个最小确定性测试。

## 2. 构建与运行环境

- **APK**：`:app:assembleOfflineDebug`（OfflineDebug——无网络/无 WebDAV/无更新检查）。注意 `app/build/outputs/apk` 当前仅 Online 产物，需完整构建 Offline。
- **构建顺序（内存约束，CLAUDE.local.md）**：构建前查内存（已确认 14.8GB 可用/53.9%）→ `--offline --no-daemon --console=plain`（缺依赖时带本地代理 `-Dhttp(s).proxyHost=127.0.0.1 -Dhttp(s).proxyPort=7897` 暖一次）→ 只信 `grep -E "^BUILD (SUCCESSFUL|FAILED)"` 判定 → 构建后 `Stop-Process` 残留 Gradle daemon JVM → 再起模拟器。
- **模拟器**：复用 `Medium_Phone` AVD（`android emulator list` 实测存在），`android emulator start`。
- **金额基线测试**：`:core:database:connectedDebugAndroidTest`（task 名**不带 flavor**——library 模块无 flavor 维度）+ 相关 JVM 单测。

## 3. journey 套件（核心交付）

- **位置**：`docs/testing/journeys/NN-*.xml`，序号控制执行顺序。
- **属性声明（评审 L3）**：`android --help` 无 journey 子命令；本套件实为 controller 逐条读 XML + `adb shell input`/`android layout`/`android screen` 手工驱动、LLM 评估，**非 CLI 托管可重放套件、不可 CI 化**。报告与文件均标此属性，不过度承诺。

### 3.1 seed journey（`00-seed.xml`）
1. **首启协议 gate（评审 H2，必做）**：第 0 步点隐私协议同意按钮（`testTag=LAUNCHER_PROTOCOL_CONFIRM`，文本"确定"）；否则 `MainAppViewModel.kt:138 needRequestProtocol=!agreedProtocol`（默认 false）必弹框阻塞全链。
2. **建基线虚构数据**：2 账本、各类资产（现金/储蓄卡/信用卡）、各类型记录（支出/收入/转账）若干、1 组报销对冲。
3. **semantics 盘点（评审 M4，前置产出）**：对每个目标 Screen 用 `android layout` 记录可命中元素清单。实证：`feature/*/src/main` 全量 `testTag` 仅 1 个（`LauncherContentScreen` LAUNCHER_TITLE），`EditRecordScreen=0` → 涉钱界面命中靠 text + `screen --annotate` 视觉兜底。缺口清单写入报告（也是未来加 testTag / 按需 instrumented 的输入）。
4. **seed 成功校验（评审 H6）**：seed 末尾逐项 verify 基线数据已建成；**任一缺失即整体标 seed FAILED、后续全 SKIP**（不在残缺基线上跑，防部分成功假绿）。

### 3.2 功能 journey 清单（源自实际 `*Screen.kt` 枚举）

| 序号 | journey | 覆盖 Screen | 备注 |
|---|---|---|---|
| 00 | seed 建基线 | EditRecord/MyBooks/EditAsset | 含协议 gate + 校验 |
| 01 | 记账增删改（支出/收入/转账 + 手续费/优惠/标签/关联资产） | EditRecord/SelectRelatedRecord | |
| 02 | 记录查看（列表/详情/日历/搜索） | LauncherContent/Calendar/Search | finalAmount 回显在此界面 |
| 03 | 标签管理（增删改/筛选） | MyTags | |
| 04 | 类型管理（一级/二级增删改） | MyCategories/EditRecordTypeList | |
| 05 | 资产管理（各类新建/编辑/详情/隐藏-显示） | MyAsset/EditAsset/AssetInfo/InvisibleAsset | 隐藏操作在**同 journey 内自恢复显示**，避免污染后续 |
| 06 | 统计图表（饼图/折线/分类/切月） | Analytics/TypedAnalytics | |
| 07 | 报销管理（待报销抽屉/报销款对冲/退款） | Reimbursement | |
| 08 | 设置（主题/安全入口/关于/协议） | Setting/AboutUs/Markdown | |
| 09 | 账单导入（虚构样例文件，无则 SKIP） | RecordImport | |
| 10 | 账本管理（新建/编辑/切换/**删除置最后**） | MyBooks/EditBook | **删除破坏状态，全套末尾** |

- **状态污染控制（评审 H6）**：不可逆破坏性 journey（10 删账本）排在全套**最末**；可逆状态变更（05 隐藏资产）在**同 journey 内自恢复**；每个 journey 开头记录前置状态假设。单模拟器串行跑**真实 OfflineDebug DB**，无回滚——删账本后不再依赖其结果。
- **超范围（不计 PASS/FAIL）**：WebDAV 备份恢复（`BackupAndRecoveryScreen`，OfflineDebug 下 `OfflineWebDAVHandler` 全 no-op）→ SKIP；Online 更新检查 → N/A。

### 3.3 假阳性兜底（评审 H5，强制）
journey 只验"可见/不崩/流程走通"，但 LLM 判读有假阳性风险（残留 UI 当新结果 / 错屏当对屏 / 点到无语义按钮 no-op）。每个 verify 步骤强制：
1. 操作后用 `android layout --diff` 确认 UI **确实变化**（排除残留/no-op）。
2. verify 锚定**唯一标识**（具体文本/testTag），禁止"页面看起来像就算过"。
3. 关键流程后 `android screen capture` 存证，报告附图。
4. **保守判定**：失败或不确定一律标 FAILED，不标 PASS（避免虚假信心）。

## 4. 金额确定性（依赖现有测试，本轮跑一次确认）

- 不新增金额逻辑测试。本轮执行并把结果纳入报告作基线证据：
  - JVM：`:core:model` / `:core:domain` / `:core:data` / `:feature:records` 相关金额单测。
  - DAO androidTest：`:core:database:connectedDebugAndroidTest`（需模拟器）。
- 报告列出现有覆盖清单，证明"核心金额正确性已被守护"。

## 5. 按需 instrumented（条件触发预案，本轮可能为空）

- **触发条件**：某 journey 暴露疑似 UI 层金额/状态异常，且现有 JVM/DAO 测试无法复现该路径。
- **落点**：在**实际渲染该值的界面**写测试（如 finalAmount → `LauncherContentScreen`；输入回显 → `EditRecordScreen`），不落错界面（评审 C2）。
- **前置步骤（评审 C1/H1/H3/L4）**：
  1. 建 `feature/<x>/src/androidTest/kotlin/...` 目录（否则 `disableUnnecessaryAndroidTests` gate 关闭 variant）。
  2. 补 androidTest 依赖：`androidTestImplementation`(hilt-android-testing, compose ui-test-junit4, **ui-test-manifest**) + `kspAndroidTest`(hilt-compiler)。
  3. **先 PoC**：先验通"`HiltAndroidRule.inject()` + `createAndroidComposeRule` 能渲染目标 Screen"，再写断言。
  4. 运行 task = `:feature:<x>:connectedDebugAndroidTest`（**不带 flavor**）。
- **若不触发，本轮不写任何 instrumented**（YAGNI）。

## 6. 数据与脱敏（评审 M3/L1）

- 测试一律用 seed 生成的**虚构数据**，不接真机/真实账户；journey 09 导入用虚构样例或 SKIP。
- **不在任何界面填入真实 WebDAV 账号/域名**（`CombineProtoDataSource.kt:320-321` 明文存 DataStore）。
- 报告 md 入库（相对路径，符合 CLAUDE.md 脱敏规范）；**截图/layout 证据放 `docs/testing/reports/evidence/` 并在 `.gitignore` 排除**（仓库当前 `.gitignore` 不覆盖 `docs/`，需加一条），或内联进 md。

## 7. 报告产物

`docs/testing/reports/2026-06-13-emulator-full-test.md`：
- 每 journey 的 PASSED/FAILED/SKIPPED JSON 汇总 + bug 清单（含 layout/截图证据引用）。
- semantics 缺口清单（M4 前置产出）。
- 金额基线测试结果（§4）。
- 超范围项（WebDAV 等）显式列出，不计 PASS/FAIL。
- （若触发）按需 instrumented 结果。
- 显式标注 journey 套件"手动驱动、非自动重放、不可 CI 化"属性。

## 8. 风险与回退

| 风险 | 应对 |
|---|---|
| 会话中断半成品 + 模拟器/JVM 进程泄漏（评审 M5） | journey 串行、**每条结果即时落盘报告文件**（断点可续）；中断后显式 `Stop-Process` 模拟器 + daemon JVM（CLAUDE.local.md） |
| 构建 + 模拟器内存叠加 OOM | 顺序执行；构建后杀 daemon；每重步骤前查内存 |
| journey LLM 判读假阳性（H5） | §3.3 保守判定 + `layout --diff` 证据 + 截图 |
| seed 单点/部分失败级联（H6） | §3.1 seed 校验 + 失败即 SKIP 后续；破坏性 journey 置后 |
| 涉钱界面 testTag≈0、命中不稳（M4） | `screen --annotate` 兜底 + semantics 缺口记报告；精确金额不靠 journey |
| 按需 instrumented PoC 跑不通（H3） | 先最小 PoC 验通再写；跑不通则该疑点改用现有 JVM/DAO 测试复现、不强搭绿地基建 |

## 9. 不做（YAGNI）

- 不预先建 instrumented 基建（仅按需触发）。
- 不搭 WebDAV 服务器测备份恢复。
- 不重复造金额逻辑测试（现有 JVM+DAO 已覆盖）。
- 不接 CI（journey 本就不可确定性重放）。
