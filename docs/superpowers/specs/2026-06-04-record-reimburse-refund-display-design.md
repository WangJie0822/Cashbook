# 记录报销/退款显示逻辑核查与显示层修复 设计

> 创建于 2026-06-04 · 来源：待办 #2「核查记录列表显示逻辑」
> 范围：**显示层修复 A/B/D（关联金额口径统一 + 报销/退款区分 + 待报销标识）**；C（净额/删除线/实际金额脱钩）与数据层 `finalAmount` 吸收模型重构**均另立项**
> 已过节点 1 team-review（四维 feasibility/security/reverse/impact），finding 处置见 §10

## 1. 背景与目标

核查「记录列表在记录关联了退款、报销时的显示逻辑是否合理」。核查覆盖显示层（首页列表条目、记录详情）与数据层（`finalAmount` 吸收模型 + 月度结余/统计消费）。结论：**存在系统性的金额口径不一致、状态语义歧义、信息缺失，以及数据层分项统计失真**。本次实施低风险显示层修复 A/B/D，净额展示与数据层重构隔离另立项。

### 现状机制（实证）

- `RecordTable.reimbursable: Int`（`core/database/.../table/RecordTable.kt:66`）仅是支出的「能否报销」标志位；报销/退款均通过 `relatedRecord` 关联记录实现。
- 报销 = 报销类型收入关联可报销支出；退款 = 退款类型收入关联任意支出（含可报销支出，无 `reimbursable` 过滤，`core/database/.../dao/RecordDao.kt:322-335`）。
- **报销/退款记录的 `type_id` 是固定负 ID**（核验，见 §10 reverse#1 驳回）：`FIXED_TYPE_ID_REFUND=-2001`、`FIXED_TYPE_ID_REIMBURSE=-2002`（`core/common/.../Constants.kt:83,86`）。`migrateSpecialTypes`（`InitWorker.kt:65` 启动调用，幂等）经 `updateRecordTypeId`（`TransactionDao.kt:654` `UPDATE db_record SET type_id=:newTypeId WHERE type_id=:oldTypeId`）把存量记录改写为固定负 ID；db_type 有固定行（`TypeTable.kt:94,107`）。`isReimburseType/isRefundType`（`TypeRepositoryImpl.kt:121-126`）即比对这两个常量。
- **吸收模型**（`TransactionDao.kt:166-186,368-375`）：收入（报销款/退款款）为「吸收者」，被吸收支出 `finalAmount=0`，吸收者 `finalAmount = 自身recordAmount − Σ被吸收支出金额`。
- 关联建立**无金额校验**（`SelectRelatedRecordViewModel.kt:107-115`）→ 允许「¥80 报销款关联 ¥100 支出」的部分/不等额吸收。
- 金额纯函数（`core/model/.../model/RecordAmount.kt:29-38`）：`recordAmount(INCOME)=amount−charges`；`recordAmount(EXPENDITURE/TRANSFER)=amount+charges−concessions`。

## 2. 核查结论

贯穿算例：支出 **S** `amount=¥100, charges=0, concessions=0`（`fullAmount=recordAmount=¥100`）；报销/退款款收入 **I** `amount=¥80`。I 吸收 S → `S.finalAmount=0`、`I.finalAmount=80−100=−¥20`。

### 显示层问题

| 编号 | 问题 | 证据 | 本次 |
|---|---|---|---|
| **A** | 同一被吸收支出，列表 vs 详情「关联金额」公式不同 | 列表 `Σ关联收入.amount`（`RecordModelTransToViewsUseCase.kt:189`）vs 详情 `Σ(amount−charges)`（`RecordDetailsSheet.kt:290`）；差额=关联收入手续费，收入可填手续费（`EditRecordScreen.kt:663-669`） | ✅ 修 |
| **B** | 可报销支出被「退款」时详情误标「已报销」 | 详情仅凭 `reimbursable` 标志判定（`RecordDetailsSheet.kt:192-200`），无法区分关联的是报销还是退款；列表用中性「已退\|报」（`LauncherContentScreen.kt:713`） | ✅ 修 |
| **C** | 列表整额划删除线、不显示净额；详情「实际金额」=失真 finalAmount | `LauncherContentScreen.kt:733-744`；`RecordDetailsSheet.kt:174-183` | ⏸ 另立项 |
| **D** | 列表对「可报销未报销」支出无任何标识 | 列表条目对 `reimbursable && relatedRecord空` 无标签 | ✅ 修 |

### 数据层问题（E，另立项）

- `finalAmount` 驱动月度结余（`GetCurrentMonthRecordViewsMapUseCase.kt:57,62`）、首页本月汇总（`LauncherContentViewModel.kt:144,148`）、统计柱状图（`TransRecordViewsToAnalyticsBarUseCase.kt:107,112`）。
- 算例下：本月支出漏计 ¥100、本月收入被扣 ¥20。**净结余正确，分项失真**。
- 另：`insert` 吸收用被吸收记录 `finalAmount`（`TransactionDao.kt:370`）、`recalc` 用 `recordAmount`（`:182`），二次吸收边界可能不等（潜在隐患）。

## 3. 范围与边界

- **本次（显示层 A/B/D）**：仅改 `feature/records` 列表前缀/状态标注/待报销标签 + `core/domain` 关联金额口径与关联性质计算 + `core/model` 实体字段 + 文案。**不改主金额/「实际金额」语义、不脱钩 `finalAmount`、不动删除线逻辑、不改数据库 schema/migration/聚合算法。**
- **另立项①（C 净额展示）**：列表净自付 + 部分报销删除线改进 + 详情「实际金额」直觉化——因在数据层不动 `finalAmount` 前提下必造成「显示层自算值 vs 月度汇总/柱状图/编辑界面」单点不一致（team-review M3 实证四处矛盾），且未解决收入吸收者负 `finalAmount` 显示（M2）。与数据层重构一并评估。
- **另立项②（数据层 E）**：`finalAmount` 吸收模型重构，解决分项失真、负值；涉及 Room migration + 历史重算 + 月度结余/资产余额回归。

## 4. 设计（A/B/D）

### 4.1 核心原则
`relatedAmount` 作为列表与详情「关联金额」的**单一数据源**，统一 `recordAmount` 口径。**不触碰主金额、实际金额、删除线、月度汇总**——这些维持现状，与全 App 其它消费方保持同源自洽，避免引入单点不一致。

### 4.2 关联性质判定（新增）
在 `RecordModelTransToViewsUseCase` 计算「关联性质」写入 `RecordViewsModel/Entity`。新增枚举 `core/model/.../enums/RecordRelatedNatureEnum { NONE, REIMBURSED, REFUNDED, MIXED }`。

判定逻辑（仅对 **EXPENDITURE 主记录**——其 `relatedRecord` 是吸收它的收入）：
- `relatedRecord` 空 → `NONE`
- 全部 `typeId == FIXED_TYPE_ID_REIMBURSE` → `REIMBURSED`
- 全部 `typeId == FIXED_TYPE_ID_REFUND` → `REFUNDED`
- 其它（混合 / 非上述）→ `MIXED`

约束：
- **判定依据已迁移保证**（§1）：报销/退款记录 `type_id` 恒为固定负 ID，判定可行（team-review reverse#1 驳回）。
- **零额外查询**（H2/L3）：`relatedRecord` 已物化为 `List<RecordModel>`（单条 `:56-62`、批量 `transBatch :135-152`），`typeId` 随之加载；判定与 `sumRelatedAmount` 同一次遍历完成，**禁止 per-record 查库**（勿破坏 N+1 批量化优化 commit `e836d060`）。
- **INCOME 主记录 / TRANSFER / 平账记录**：`relatedNature = NONE`，沿用现有「已关联」中性语义，不进入报销/退款判定。

### 4.3 A — 统一关联金额口径
`RecordModelTransToViewsUseCase.sumRelatedAmount`（`:181-196`）两分支统一为 **`Σ recordAmount(关联记录)`**，复用 `RecordAmount.kt` 纯函数（禁重实现）。
- **关联 category 由主 category 取反推断**（H2，零查库）：EXPENDITURE 主记录 → 关联记录按 `recordAmount(INCOME)=amount−charges`；INCOME 主记录 → 关联记录按 `recordAmount(EXPENDITURE)=amount+charges−concessions`（= 现状 INCOME 分支，逐字段不变）。仅 EXPENDITURE 分支从 `Σamount` 改为 `Σ(amount−charges)`。
- 详情页**两处**内联重算均删除、改用 `entity.relatedAmount`（M7）：
  - 「关联记录行」`RecordDetailsSheet.kt:276-295`（现状 INCOME 侧 `Σ(amount+charges−concessions)`、EXPENDITURE 侧 `Σ(amount−charges)`）
  - 确认无第三处（详情「实际金额」`:174-183` 显示 finalAmount，不属关联金额，本次不动）
- 列表 `LauncherContentScreen.kt:717` 已用 `item.relatedAmount`，随口径修正自动生效。

### 4.4 B — 区分报销/退款
- 列表前缀（`LauncherContentScreen.kt:709-722`）：按 `relatedNature` 显示 `已报销`/`已退款`/`已退\|报`（MIXED）+ `(relatedAmount)`。
- 详情金额行标注（`RecordDetailsSheet.kt:192-206`）：按 `relatedNature` + `reimbursable` 显示——`REIMBURSED`→已报销、`REFUNDED`→已退款、`MIXED`→已退\|报、`NONE && reimbursable`→待报销。替代只认 `reimbursable` 二态的现状。**列表前缀（§4.4）、详情标注、待报销标签（§4.5）三处术语统一**（已报销/已退款/已退\|报/待报销）。

### 4.5 D — 待报销标识
列表条目对 `reimbursable && relatedNature==NONE` 显示 `[待报销]` 标签（trailing 区，`LauncherContentScreen.kt`）。新增文案 `strings_records.xml`：`pending_reimbursement`=「待报销」。

> 列表主金额、删除线（`:733-744`）、详情「实际金额」**保持现状不动**（净额改进属另立项①）。

## 5. 涉及文件

| 文件 | 改动 |
|---|---|
| `core/model/.../enums/RecordRelatedNatureEnum.kt` | 新增枚举（放 `core.model.enums` 包，自动命中 Compose 稳定性 wildcard `compose_compiler_config.conf`） |
| `core/model/.../entity/RecordViewsEntity.kt` + `model/RecordViewsModel.kt` | 增 `relatedNature` 字段，**给默认值 `= RecordRelatedNatureEnum.NONE`**（H1：收敛构造点编译破坏面为 0） |
| `core/model/.../transfer/ModelTransfer.kt:78` | Model→Entity 映射**真实传递** `relatedNature`（H1：防默认值掩盖未赋值） |
| `core/domain/.../usecase/RecordModelTransToViewsUseCase.kt` | A 口径统一（`sumRelatedAmount` 单函数即覆盖单条+批量）+ relatedNature 计算并**真实赋值**（`:65-82`、`:154-171`） |
| `feature/records/.../screen/LauncherContentScreen.kt` | B 前缀 / D 待报销标签 |
| `feature/records/.../view/RecordDetailsSheet.kt` | A 删两处内联重算用 relatedAmount / B 状态标注 |
| `core/ui/.../res/values/strings_records.xml` | 待报销 / 已退款 等文案 |

**H1 构造点全清单**（新增字段虽有默认值不破坏编译，但需知悉范围；relatedNature 真值仅 domain 计算写入，其余构造点保持 NONE 默认即可）：`ModelTransfer.kt:78`；`RecordDetailsSheetPreviewParameterProvider.kt`（8 处）；`feature/records` 截图测试 `AssetInfoContentScreenScreenshotTests/CalendarScreenScreenshotTests/SearchScreenScreenshotTests/SelectRelatedRecordScreenScreenshotTests/TypedAnalyticsScreenScreenshotTests`、ViewModelTest `AssetInfoContentViewModelTest/CalendarViewModelTest/LauncherContentViewModelTest/RecordDayGroupingTest/SearchViewModelTest/TypedAnalyticsViewModelTest`；跨模块 `feature/assets/.../AssetInfoViewModelTest.kt:281`、`core/domain/.../GetCurrentMonthRecordViewsMapUseCaseTest.kt:182`、`core/testing/.../TestDataFactory.kt`（`RecordViewsModel` 工厂）。

## 6. 测试策略

- `RecordModelTransToViewsUseCase` 单测：
  - `sumRelatedAmount` 统一口径（**含关联收入 charges≠0 算例**，验证 EXPENDITURE 主分支从 Σamount→Σ(amount−charges) 的修正；INCOME 主分支不变回归）。
  - `relatedNature` 判定：**关联记录 fixture 必须用 `typeId = FIXED_TYPE_ID_REIMBURSE(-2002)/FIXED_TYPE_ID_REFUND(-2001)`**（M5：否则全判 MIXED，假阳性）；覆盖 REIMBURSED/REFUNDED/MIXED/NONE 四态 + 取反推断（H2）。
  - 批量 `transBatch` 与单条逐字段等价（含 relatedNature）。
- `feature/records` 截图测试：`RecordDetailsSheetPreviewParameterProvider` 补「已报销/已退款/待报销」样例（关联记录 typeId 用固定负 ID）；改 Composable/ViewModel 签名时同步 `*ScreenshotTests`/`*ViewModelTest`（模块测试源集整体编译）。
- 全量：`./gradlew :core:domain:testDebugUnitTest :core:model:testDebugUnitTest :feature:records:testOnlineDebugUnitTest :feature:assets:testOnlineDebugUnitTest`（注意 H1 跨 `feature:assets`/`core:domain` 构造点）。

## 7. 已知局限

A/B/D 不触碰主金额/实际金额/汇总，**与全 App 其它消费方同源自洽，无新增不一致**。列表净自付、删除线改进、详情实际金额直觉化、月度分项失真——均留另立项①②，本次不解决。

## 8. 另立项登记

1. **C 净额展示**：列表净自付 + 部分报销删除线 + 详情「实际金额」直觉化（依赖数据层 finalAmount 语义）。
2. **数据层 finalAmount 吸收模型重构**：重定义分配使月度分项正确；含 Room migration、历史重算、月度结余/资产余额/收入吸收者负值全回归。

## 9. 风险与回滚

- 纯显示层 + domain 口径 + entity 追加字段（默认值），无 schema/migration；回归面限于 `feature:records` 显示 + 关联金额口径消费方（已核 `relatedAmount` 消费方仅列表/详情两处，team-review impact① PASS）。
- 回滚：纯代码改动，`git revert`。

## 10. team-review finding 处置（节点 1）

- **驳回**：reverse [High]「B 判定完全失效（typeId 永不等固定 ID）」——hands-on 核验 `updateRecordTypeId`(`TransactionDao.kt:654`)+`InitWorker.kt:65`+`TypeTable.kt:94,107` 证明报销/退款记录 type_id 恒为固定负 ID，**该强断言与实证相反，撤回**。
- **采纳**：H1（relatedNature 默认值 + 真实赋值 + 构造点全清单，§5）；H2（关联 category 取反推断零查库，§4.3）；M5（fixture 用固定负 ID，§6）；M7（详情两处内联重算改 relatedAmount，§4.3）；L3（relatedNature 计算无 N+1，§4.2）。
- **随 C 收敛而消解**：M1（净额基线）、M2（收入吸收者负值）、M3（脱钩四处矛盾）、M4（净自付二分边界）、M6（净自付纯函数）——C 移交另立项后均不在本次范围。
- security 维度无 finding。
