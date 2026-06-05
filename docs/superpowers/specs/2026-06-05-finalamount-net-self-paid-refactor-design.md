# finalAmount 净自付重构 + C 净额展示 设计 (v2)

> 创建于 2026-06-05 · 来源：待办 #4（C 净额展示）+ #5（数据层 finalAmount 吸收模型重构）合并项
> 依据 2026-06-04 核查 spec（`2026-06-04-record-reimburse-refund-display-design.md`）§2/§8 结论
> **v2（2026-06-05）**：过节点 1 team-review（feasibility/security/reverse/impact 四维，0 Critical / 5 High / 12 Medium / 8 Low），全部 finding 已整合，处置见 §13。重大修订：§7 实现路线从「新增独立 worker」改为「改造既有 `migrateAfter9To10` 统一写入器」（H1）；新增饼图净自付改造（H2 用户拍板）；恢复同步重算（H3）。

## 1. 背景与目标

2026-06-04 核查记录显示逻辑时，A/B/D（显示层）已合入 main（`cddfedb1`）；C（净额展示）与 E（数据层 `finalAmount` 吸收模型分项失真）因强相关移出另立项。本 spec 合并实施 C+E：**重构 `finalAmount` 吸收模型为「净自付」语义，使月度分项统计正确、消灭负值，并让 C 净额展示基于正确的数据层语义。**

### 核心痛点（实证算例）

支出 S `amount=¥100,charges=0,concessions=0`（recordAmount=¥100）被报销款 I `amount=¥80` 吸收：
- 现状：`S.finalAmount=0`、`I.finalAmount=80−100=−20`
- 月度本月支出 += 0（漏计全额 ¥100）、本月收入 += −20（负值污染）；净结余 −20 正确但**分项失真**（E）
- 列表 S 显示全额 ¥100 + 整额删除线、不显示净自付 ¥20（C）；详情「实际金额」显失真 finalAmount（S=¥0、I=−¥20，C+M2）

## 2. 现状机制（实证）

> 包名约定（L1 修正）：domain UseCase 实际包为 `cn.wj.android.cashbook.domain.usecase`（**无 `core.` 段**）；`core.data`/`core.database`/`core.model` 类才带 `core.` 段。下文路径以实际包为准。

### 吸收边界（决定净自付模型适用范围）

- **只有报销款(`FIXED_TYPE_ID_REIMBURSE=-2002`)/退款款(`FIXED_TYPE_ID_REFUND=-2001`)能吸收支出**：`TypeRepositoryImpl.needRelated(typeId)` 仅对这两个固定负 ID 返 true（`core/data/.../TypeRepositoryImpl.kt:117-119`）。普通收入不能关联。
- 吸收者必是报销/退款款（INCOME），被吸收必是支出（EXPENDITURE）。关系表 `db_record_with_related`：`record_id`=吸收者收入、`related_record_id`=被吸收支出（`TransactionDao.kt:357-365` 建立方向，**单向**，保证二部图无自关联/无环 — reverse 核验）。
- 关联建立**无金额校验**（`SelectRelatedRecordViewModel.kt:107-115`）→ 允许部分/等额/超额吸收。

### finalAmount 吸收模型（待重构）

- 计算入口纯函数：`recordAmount(category, amount, charges, concessions)`（`core/model/.../model/RecordAmount.kt:29-38`）：INCOME=amount−charges；EXPENDITURE/TRANSFER=amount+charges−concessions。
- insert（`TransactionDao.kt:367-375`）：被吸收支出 `finalAmount=0`；吸收者 `finalAmount = recordAmount − Σ被吸收记录的 finalAmount`（**用 finalAmount**，:370）。
- recalc（`recalculateAbsorberFinalAmount`，`:166-186`）：吸收者 `finalAmount = recordAmount − Σ被吸收记录的 recordAmount`（**用 recordAmount**，:182）。
- **隐患**：insert 用被吸收 `finalAmount`、recalc 用 `recordAmount`，二次吸收边界口径不一致（本次一并修）。
- delete（`deleteRecordTransaction:524-554`）：删支出→重算吸收它的收入；删吸收者→恢复支出全额（无其它吸收者）或重算剩余吸收者。

### finalAmount 写入路径（**两条**，H1 关键修正）

1. **增删改事务**：`insertRecordTransaction`/`deleteRecordTransaction`/`recalculateAbsorberFinalAmount`（上节）。
2. **全量迁移器 `migrateAfter9To10()`**（`core/data/.../RecordRepositoryImpl.kt:499-548`，H1 核验）：db9→10 数据迁移，**本身就是全量重算 finalAmount 的旧吸收语义实现**——转账 `finalAmount=concessions−charge`(:504)、被吸收支出 `finalAmount=0`(:513)、吸收者 `finalAmount=amount−charge−Σ被吸收`(:535，可负)、其它收入 `amount−charge`(:542)。触发方：
   - `LauncherContentViewModel.kt:71`（首页加载，gated `!db9To10DataMigrated`）
   - `BackupAndRecoveryViewModel.kt:247` 的 `refreshDbMigrate`（设置页手动按钮，**无条件**调用）
   - **本次重构必须改造此函数主体为净自付语义**（否则它会覆写净自付，且手动按钮会毁数据）。

### finalAmount 消费方（**6 个透传** + 饼图 + 搜索，L2 修正）

**透传消费方**（读 `RecordModel.finalAmount`，数据层改后自动正确，经 `RecordModelTransToViewsUseCase:76,166` 原样透传）：
1. 日历每日结余 `GetCurrentMonthRecordViewsMapUseCase.kt:57,62`（被 `CalendarViewModel.kt:84` 调用）
2. 统计柱状图 `TransRecordViewsToAnalyticsBarUseCase.kt:107,112`
3. 首页本月汇总 `LauncherContentViewModel.kt:144,148`
4. **首页每日卡片** `LauncherContentViewModel.computeDailySummaries:219,223`（L2 补漏）
5. 列表条目金额 `LauncherContentScreen.kt:731-753`（`RecordListItem`，**被 3 屏共用**，见 §8）
6. 详情「实际金额」`RecordDetailsSheet.kt:179`

**非透传消费方（需主动改）**：
- **Analytics 饼图** `TransRecordViewsToAnalyticsPieUseCase.kt:49,71` + `TransRecordViewsToAnalyticsPieSecondUseCase`（H2 核验）：用 `analyticsPieAmount(原始 amount)`，**不读 finalAmount**。**决策：改为净自付**（见 §3/§8）。
- **搜索按金额** `RecordDao.kt:268`：`OR (... final_amount = :amountCent)`（M2 核验）。重构后 final_amount 变净自付，搜索命中集随之变化（行为变更，需测试守住）。

### 不受 finalAmount 影响（已三方核验，仅需测试守住）

- **资产余额**：`insert`/`delete`/`batchImport` 算 balance 全用 `recordAmount`/`record.amount`（`:290,299,311,324,327,474,483,407,410`），与 finalAmount 解耦。
- **批量导入** `batchImportRecordsTransaction`：无关联，`finalAmount=recordAmount`（`:410`），不进吸收路径。
- `GetAssetMonthSummaryUseCase.kt:65`：用 `recordAmount`（资产流水全额），独立 finalAmount。
- DB_VERSION=12（`ApplicationInfo.kt:29`），`finalAmount` 列已存在（`RecordTable.kt:62`）。

## 3. 需求决策（已与用户确认）

| # | 决策 | 选定 |
|---|---|---|
| 1 | 金额语义方向 | **净自付模型**：本月支出计净自付（100−80=20）；报销/退款款不计入本月收入 |
| 2 | 超额（报销额>支出额）| **下限归 0 + 溢出计收入**：支出净自付下限 0；吸收者 finalAmount=max(0,溢出) 计入收入 |
| 3 | 实现路线 | **存储重算 finalAmount**。排除「聚合时实时算」的理由（L4 精确化）：实时算配下限保护也不会负，但每次查询都要做簇闭包重算，**性能与一致性不如一次性落库**（且跨期报销下聚合范围不含被吸收支出时仍需全局信息）|
| 4 | 列表被吸收支出显示 | **净额优先**：主金额=净自付 ¥20，去删除线（配合已有「已报销(¥80)」标签）|
| 5 | 报销/退款款收入显示 | **显示实收额**：列表主金额=amount(¥80)+性质标签；聚合用 finalAmount=溢出(0) 不虚增收入 |
| 6 | 一对多/多对一分摊 | **顺序贪心填充**（见 §5，整数守恒确定）|
| 7 | **Analytics 饼图口径**（H2）| **改净自付**：饼图分类按净自付统计，与首页/柱状图一致。代价：1对N 跨分类报销时净自付集中在 id 小的分类（罕见，§9 已知局限）|

## 4. finalAmount 新语义（核心不变量）

| 记录 | 重构后 finalAmount |
|---|---|
| 未吸收记录 | recordAmount（不变）|
| 被吸收支出 | 净自付 = recordAmount − 被对冲额（≥0）|
| 报销/退款款（吸收者）| 溢出额 = max(0, recordAmount − 对冲额)（≥0，通常 0）|

**不变量（M11 修正，消费方口径）**：
- ① **`Σ INCOME.finalAmount − Σ EXPENDITURE.finalAmount = Σ recordAmount(INCOME) − Σ recordAmount(EXPENDITURE)`**（= 真实「收入−支出」净结余，offset 在两边抵消故精确守恒）。这是月度结余/柱状图/首页汇总三消费方真正消费的不变量。
  > 旧 v1 写「Σ finalAmount = Σ支出−Σ报销」符号错误（场景5 实测 Σfa=60 但 Σ支出−Σ报销=−60），已废弃。
- ② 所有 `finalAmount ≥ 0`（消灭负值 M2）。

## 5. 分配算法（顺序贪心填充）

**吸收簇定义**：通过 `db_record_with_related` 连通的记录集合（一个支出可被多个吸收者吸、一个吸收者可吸多笔支出，构成二部图的连通分量；单向建立保证无自关联/无环）。分配以「簇」为单位，结果与吸收者插入历史无关、仅依赖 id 顺序，从而**增量重算与全量重算结果一致**。

```
1. 初始化簇内所有记录 finalAmount = recordAmount
2. 吸收者按 id 升序遍历；每个吸收者 I：
     remaining = recordAmount(I)
     I 关联的支出按 id 升序遍历；每个支出 E：
         offset = min(remaining, E.finalAmount)   // 当前剩余净自付
         E.finalAmount -= offset
         remaining     -= offset
     I.finalAmount = remaining                    // 溢出
```

### 守恒验证（全场景，feasibility 逐场景手算复现）

| 场景 | 结果 | 净额校验 |
|---|---|---|
| 1:1 部分（E100,I80）| E.fa=20, I.fa=0 | 本月支出20 ✓ |
| 超额（E100,I120）| E.fa=0, I.fa=20 | 支出0/收入20，净赚20 ✓ |
| 1对N（I80→E100,E50）| E1.fa=20,E2.fa=50,I.fa=0 | 支出70（花150报80）✓ |
| N对1（E100←I30,I40）| E.fa=30,I1.fa=0,I2.fa=0 | 支出30（花100报70）✓ |
| 过度吸收（E100←I80,I80）| E.fa=0,I1.fa=0,I2.fa=60 | 支出0/收入60，净赚60 ✓ |
| **charges≠0**（M9，E `amount100,charge0`；I `amount80,charge5`，recordAmount(I)=75）| E.fa=25,I.fa=0 | 本月支出25（净自付=100−75）✓ |

`offset=min(remaining,E.fa)` 保证 E.fa 与 remaining 均不下穿 0（②非负，无需额外 floor）；纯整数无取整误差。

**id 稳定性不变量（M6）**：算法依赖记录 id 在 update 后不变。现状 `updateRecordTransaction`=delete+insert，`insertRecord(:334)` 沿用原 id（delete 已先清除无主键冲突）→ id 稳定。**此为算法正确性前提，§10 加测试守护**；若未来改 update 为「删旧建新自增 id」会静默破坏一致性。

## 6. 数据层改造（`TransactionDao`）

抽出**单一纯算法函数**「重算一个吸收簇」（按 §5 顺序填充），增删改三处 + 全量重算复用，**消除 insert/recalc 口径不一致隐患**：

- `insertRecordTransaction` 吸收分支（`:367-375`）
- `recalculateAbsorberFinalAmount`（`:166-186`）→ 重定义为净自付分配
- `deleteRecordTransaction`（`:524-554`）

> **增量一致性（正确性关键）**：任何增删改触发时，先从变更记录沿关系表 BFS 发现整个吸收簇（连通分量），将簇内所有记录 finalAmount 重置为 recordAmount，再按 §5 对整簇重算。这样增量结果与全量逐字段一致、与插入历史无关。簇边界发现：从受影响支出反查吸收者（`queryRelatedByRelatedRecordId`）、再从吸收者查被吸收（`queryRelatedByRecordId`），迭代至闭包（双向查询原语 `:119,126` 已存在）。

> **批量删除交互（M4）**：`updateRecordTransaction:236-253`(删插)、`deleteBookTransaction:640-652`、`deleteAssetRelatedData:731-738` 逐条循环 `deleteRecordTransaction`。每条触发整簇重算，对含「即将被删」记录的簇需保证不脏读已删记录（重算时 BFS 须基于当前库状态、排除已删 id —— 复用现有 `excludeAbsorbedId` 机制思路）。§10 加「删账本含吸收簇」用例。

> **性能边界（M8）**：现 `recalculateAbsorberFinalAmount:176-183` 已是逐条 `queryRecordById`(N+1)；BFS 闭包放大。簇规模 = O(簇边数)，单次事务内。plan 评估典型簇大小（现实多为 1:1/1:N 小簇），§10 加大簇压力测试（如 50 笔支出关联 1 报销款）验证事务不超时；若需要，用 `queryRecordByIds` 批量化。

## 7. 历史数据重算（实现路线，H1/H3/H5 重大修订）

> **不新增独立 worker，而是改造既有全量写入器 `migrateAfter9To10`**（H1）。`finalAmount` 列结构不变 → **不升 DB_VERSION、无 Room schema migration**（纯数据值迁移）。

### 7.1 改造 `migrateAfter9To10`（RecordRepositoryImpl:499-548）

- 将其吸收语义段（被吸收支出清零 `:513`、吸收者算差额 `:535`）**替换为 §5 净自付簇算法的全量版**（先全置 recordAmount，再按吸收者 id 升序填充全表关联）。转账段（`:504`）保持。
- 改造后 `refreshDbMigrate` 手动按钮（`BackupAndRecoveryViewModel.kt:247`）自动产出净自付，不再毁数据。
- **触发标记**：老用户 `db9To10DataMigrated` 已为 true，不会重跑。新增独立净自付重算标记 `finalAmountNetRecalcDone`（`core/datastore-proto/.../record_settings.proto` 新增 `bool ... = N;`，proto3 默认 false → 首启触发一次）。

### 7.2 启动触发（InitWorker，M1）

- `InitWorker.doWork()`（`:62-115`）在 `migrateSpecialTypes()` 后，检查 `finalAmountNetRecalcDone`，未完成则调全量净自付重算。
- **注入链**（M1）：`InitWorker` 当前只注入 `SettingRepository`+`TypeRepository`（`:54-55`），需新增 `RecordRepository` 注入（HiltWorker，确认 DI 图可解析）。涉及文件表已列。
- 与 `migrateAfter9To10` 顺序：二者均产出净自付（同算法），先后不再竞态（H1 消解）；标记各自独立幂等。

### 7.3 原子性 + 标记后置（H5）

- 全量净自付重算必须 **`@Transaction` 整体原子**（现有多写操作均 `@Transaction`：`:200,235,267,387,447,640`）。
- 幂等标记**在重算事务成功提交后才置位**（先置标记后崩溃 = 永不重跑的半成品库）。
- 幂等基础：从 amount/charges/concessions（原始值）+ 关系表从头算，连跑结果一致（reverse 核验）。

### 7.4 备份恢复同步重算（H3，三方共指）

- 恢复是 **`CONFLICT_REPLACE` 合并非整库替换**（`DatabaseMigrations.copyData:97-113`）→ 恢复后 finalAmount 是「备份旧语义行 + 当前库新语义行」混合；恢复后**无进程重启**（`BackupRecoveryManagerImpl.startRecovery` 仅 `updateRecoveryState:720`，全代码无 exit/recreate）。
- **修订**：恢复成功分支（`BackupRecoveryManagerImpl.kt:683-697`，copyData 后、置 SUCCESS 前）**同步调用全量净自付重算**（覆盖合并后全表，M12），而非仅清标记等下次启动。
- **接线**：`BackupRecoveryManagerImpl` 未注入 TransactionDao/RecordRepository，plan 须接线（该类已有 `:694-696` 重置 type 迁移标记的同址 idiom，可一并处理）。
- plan 确认 `db_record_with_related` 也走 copyData 合并、主键策略不丢关联（重算依赖关系表正确）。

### 7.5 大数据量性能（L5）

plan 评估全量重算在 InitWorker（启动期，`:121` expedited→non-expedited）对大数据量用户的首启延迟；必要时分批/后台低优先级。

## 8. 显示层

- **列表 `RecordListItem`**（`LauncherContentScreen.kt:731-753`，**M3：被 `SearchScreen.kt:213` + `AssetInfoContentScreen.kt:151` 共 3 屏复用**）：去删除线（`isReimbursed`/`LineThrough` 删除）；`displayAmount` = ①TRANSFER→amount ②INCOME 且 relatedRecord 非空(吸收者)→amount(实收，决策 #5) ③其它含被吸收支出→finalAmount(净自付)。改动同步影响 home/search/asset-info 三屏。
- **详情「实际金额」**（`RecordDetailsSheet.kt:179`）：保持 finalAmount（净额语义）。报销款显 ¥0 有上方「金额 ¥80」行陪衬可解释（决策 #5）。
- **Analytics 饼图改净自付**（决策 #7）：`TransRecordViewsToAnalyticsPieUseCase.kt:49,71` + `...SecondUseCase` 从 `analyticsPieAmount(原始 amount)` 改为按 finalAmount 净自付统计分类。
  > plan 细化：饼图原 `analyticsPieAmount` 口径 TRANSFER 当收入，与 finalAmount 不同；改造需保持饼图「按 typeCategory 分组的分类占比」语义下用各记录 finalAmount（EXPENDITURE 饼图=各支出净自付；INCOME 饼图=各收入 finalAmount 即报销款溢出/普通收入全额）。TRANSFER 在饼图的归类按现状保持。
- 报销/退款款列表关联标签文案（INCOME 吸收者，relatedNature=NONE）：可调为「关联支出(¥X)」，属文案细节，plan 定。

## 9. 已知局限

- `GetAssetMonthSummaryUseCase` 用 recordAmount（资产流水全额），与首页本月汇总（净自付）维度不同——资产流水应显全额交易，**不强行统一**。
- **饼图 1对N 跨分类失真（M7）**：报销款关联多笔不同分类支出时，顺序贪心使净自付集中在 id 小的分类（如餐饮¥100+交通¥50 被报¥80 → 餐饮净20、交通净50），分类占比依录入顺序。1:1（绝大多数）无此问题。与 §5 局限同源，可接受。

## 10. 测试策略

- **DAO 算法**（`TransactionDaoLogicTest` in `core/data` test / `TransactionDaoTest` androidTest）：
  - §5 六场景（含 charges≠0，M9）守恒+非负；多对一/二次吸收/簇闭包。
  - **必改存量断言（M5）**：`TransactionDaoLogicTest.kt:120(==-4000)/141(==-12000)/175(==-2000)` 等旧负值断言 → 改「断言非负 + 净额守恒」；`TransactionDaoTest.kt` 14 处 finalAmount 引用同步。`FakeTransactionDao` 继承 DAO 默认实现，改算法后这些测试真实跑新算法（CLAUDE.md 禁桩：忠实复刻净自付语义）。
  - id 稳定性（M6）：建簇→update 簇内一条→断言其余 finalAmount 与全量重算一致。
  - 批量删除（M4）：删账本/删资产含吸收簇、编辑被吸收支出金额后整簇守恒。
  - 大簇压力（M8）：50 笔支出关联 1 报销款，事务不超时。
- **全量重算**：`migrateAfter9To10` 改造后净自付全场景；幂等（连跑两次一致，H5）；Long.MAX 边界（L7）。
- **资产不变性**（L8）：复用 `verifyAssetBalance` 断言重构前后差值恒 0。
- **update 断关联（H4）**：明确 update 被吸收支出后簇是否保留关联——保留则 insert 侧恢复关联；接受断开则测试断言 + 文档化。
- **搜索（M2）**：`RecordDao.queryRecordByKeyword` 按金额搜在新语义下命中集；`FakeRecordRepository.kt:183` 桩随新语义重新核对忠实度（禁桩）。
- **聚合消费方**：6 个透传消费方（日历/柱状图/首页汇总/首页每日卡片/列表/详情）+ 饼图在净自付下分项正确（`GetCurrentMonthRecordViewsMapUseCaseTest` 等补算例）。
- **显示层截图（M3）**：`feature:records` 截图/ViewModel 测试同步——列表去删除线、报销款显实收、详情实际金额净自付、**`SearchScreenScreenshotTests`/`AssetInfoContentScreenScreenshotTests`**（3 屏共用）；饼图测试守口径。
- `FakeRecordRepository` 新增 `recalculateAllFinalAmount`/改造方法的 override（L6，忠实复刻）。
- 全量（命令示意，plan 精化）：`:core:data:testDebugUnitTest`（FakeTransactionDao 逻辑测试）；`core/database` 真实 DAO 在 androidTest（instrumented，plan 确认运行方式）；`:core:domain:testDebugUnitTest`、`:feature:records:testOnlineDebugUnitTest`。

## 11. 涉及文件（预估，plan 精化）

| 文件 | 改动 |
|---|---|
| `core/database/.../dao/TransactionDao.kt` | §6 统一净自付簇重算函数 + insert/delete/recalc 改造 |
| `core/data/.../repository/impl/RecordRepositoryImpl.kt` | 改造 `migrateAfter9To10` 主体为净自付簇算法（§7.1）|
| `core/data/.../repository/RecordRepository.kt` | 若抽全量重算方法则新增接口 |
| `core/datastore-proto/.../record_settings.proto` | 新增 `finalAmountNetRecalcDone` 标记 |
| `sync/work/.../InitWorker.kt` | 新增 `RecordRepository` 注入 + 接线净自付重算（M1）|
| `core/data/.../uitl/impl/BackupRecoveryManagerImpl.kt` | 恢复成功分支同步重算 + 接线（§7.4）|
| `feature/settings/.../BackupAndRecoveryViewModel.kt` | `refreshDbMigrate` 随 migrateAfter9To10 改造自动一致（核对）|
| `core/domain/.../usecase/TransRecordViewsToAnalyticsPieUseCase.kt` + `...SecondUseCase.kt` | 饼图改净自付（§8，决策 #7）|
| `core/database/.../dao/RecordDao.kt` | 搜索 `final_amount` 行为变更登记（M2，逻辑可不改，测试守住）|
| `feature/records/.../screen/LauncherContentScreen.kt` | 列表 `RecordListItem` 去删除线 + displayAmount（波及 3 屏）|
| `feature/records/.../view/RecordDetailsSheet.kt` | 文案/标签微调（实际金额自动正确）|
| `core/testing/.../FakeRecordRepository.kt` | 新增/改造方法 override（L6）|
| `core/data` test `FakeTransactionDao.kt` | 复刻净自付语义（继承默认实现，主要靠 DAO 改动）|
| 各 DAO/UseCase/ViewModel/截图测试 | M5 断言改写 + 算例 + 3 屏截图同步 |

## 12. 风险与回滚（M10 据实修正）

- 数据层核心改造，回归面：记录增删改 + 6 透传消费方 + 饼图 + 搜索 + 全量重算 + 备份恢复。
- **回滚**：`git revert` 代码后——`migrateAfter9To10` 旧版本随之回到 git 历史中的旧吸收语义，是旧语义的全量重算入口（清 `db9To10DataMigrated` 或点 `refreshDbMigrate` 即按旧语义重算）。但新增的 `finalAmountNetRecalcDone` 标记需一并处理。**不声称「无脑可恢复任一语义」**：回退后用户库是净自付值，需走旧 migrateAfter9To10 重算回旧语义，plan 给回退操作清单。

## 13. team-review finding 处置（节点 1，0 Critical / 5 High / 12 Medium / 8 Low）

- **High 全采纳**：H1 改造 migrateAfter9To10 统一写入器（§2/§7.1）；H2 饼图改净自付（用户拍板，§3#7/§8）；H3 恢复同步重算（§7.4）；H4 update 断关联测试（§10）；H5 重算原子+标记后置（§7.3）。
- **Medium 全采纳**：M1 InitWorker 注入（§7.2）；M2 搜索登记（§2/§10）；M3 RecordListItem 3 屏（§8/§10）；M4 批量删除交互（§6/§10）；M5 测试断言改写（§10）；M6 id 不变量（§5/§10）；M7 饼图 1对N 局限（§9）；M8 簇性能（§6/§10）；M9 charges 守恒（§5/§10）；M10 回滚据实（§12）；M11 不变量措辞（§4）；M12 重算覆盖全表（§7.4）。
- **Low 全采纳**：L1 包名（§2）；L2 消费方清单 6 项+首页每日卡片（§2）；L3 migrate 顺序无强依赖（§7.2）；L4 实时算理由精确化（§3#3）；L5 大数据量性能（§7.5）；L6 FakeRecordRepository override（§10/§11）；L7 Long 边界单测（§10）；L8 verifyAssetBalance 复用（§10）。
- **controller hands-on 核验**：H1(`RecordRepositoryImpl.kt:499-548`)、H2(`...PieUseCase.kt:49,71`)、H3(`BackupRecoveryManagerImpl.kt:720`+`DatabaseMigrations.kt:97-113`)、H4(`TransactionDao.kt:236-253,557,107-112`)、M2(`RecordDao.kt:268`)、M11(场景5 口算) 均自行 Read/演算证实；其余采信 feasibility/impact 的 file:line（评审 hands-on，且多处与 controller 探索一致）。
- feasibility 诚实呈现 15 项「已核验通过」、reverse 5 项「对设计有利」，核心算法（§5/§6/不变量）站得住。
