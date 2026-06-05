# finalAmount 净自付重构 + C 净额展示 设计

> 创建于 2026-06-05 · 来源：待办 #4（C 净额展示）+ #5（数据层 finalAmount 吸收模型重构）合并项
> 依据 2026-06-04 核查 spec（`2026-06-04-record-reimburse-refund-display-design.md`）§2/§8 结论
> 本 spec 走 superpowers 双节点链路：节点 1 team-review（方案）+ 节点 2 full-review（开发完成）

## 1. 背景与目标

2026-06-04 核查记录显示逻辑时，A/B/D（显示层）已合入 main（`cddfedb1`）；C（净额展示）与 E（数据层 `finalAmount` 吸收模型分项失真）因强相关移出另立项。本 spec 合并实施 C+E：**重构 `finalAmount` 吸收模型为「净自付」语义，使月度分项统计正确、消灭负值，并让 C 净额展示基于正确的数据层语义。**

### 核心痛点（实证算例）

支出 S `amount=¥100,charges=0,concessions=0`（recordAmount=¥100）被报销款 I `amount=¥80` 吸收：
- 现状：`S.finalAmount=0`、`I.finalAmount=80−100=−20`
- 月度本月支出 += 0（漏计全额 ¥100）、本月收入 += −20（负值污染）；净结余 −20 正确但**分项失真**（E）
- 列表 S 显示全额 ¥100 + 整额删除线、不显示净自付 ¥20（C）；详情「实际金额」显失真 finalAmount（S=¥0、I=−¥20，C+M2）

## 2. 现状机制（实证）

### 吸收边界（决定净自付模型适用范围）

- **只有报销款(`FIXED_TYPE_ID_REIMBURSE=-2002`)/退款款(`FIXED_TYPE_ID_REFUND=-2001`)能吸收支出**：`TypeRepositoryImpl.needRelated(typeId)` 仅对这两个固定负 ID 返 true（`TypeRepositoryImpl.kt:117-119`）。普通收入不能关联。
- 吸收者必是报销/退款款（INCOME），被吸收必是支出（EXPENDITURE）。关系表 `db_record_with_related`：`record_id`=吸收者收入、`related_record_id`=被吸收支出（`TransactionDao.kt:357-365` 建立方向）。
- 关联建立**无金额校验**（`SelectRelatedRecordViewModel.kt:107-115` addToRelated 无校验）→ 允许部分/等额/超额吸收。

### finalAmount 吸收模型（待重构）

- 计算入口纯函数：`recordAmount(category, amount, charges, concessions)`（`core/model/.../model/RecordAmount.kt:29-38`）：INCOME=amount−charges；EXPENDITURE/TRANSFER=amount+charges−concessions。
- insert（`TransactionDao.kt:367-375`）：被吸收支出 `finalAmount=0`；吸收者 `finalAmount = recordAmount − Σ被吸收记录的 finalAmount`（**用 finalAmount**）。
- recalc（`recalculateAbsorberFinalAmount`，`:166-186`）：吸收者 `finalAmount = recordAmount − Σ被吸收记录的 recordAmount`（**用 recordAmount**）。
- **隐患**：insert 用被吸收 `finalAmount`、recalc 用 `recordAmount`，二次吸收边界口径不一致（spec §2 已记，本次一并修）。
- delete（`deleteRecordTransaction:524-554`）：删支出→重算吸收它的收入；删吸收者→恢复支出全额（无其它吸收者）或重算剩余吸收者。

### finalAmount 消费方（5 处，分项失真来源）

1. 每日卡片结余 `GetCurrentMonthRecordViewsMapUseCase.kt:57,62`（EXP/INC 用 finalAmount，TRANSFER 用 charges−concessions，平账跳过）
2. 统计柱状图 `TransRecordViewsToAnalyticsBarUseCase.kt:107,112`（同上逻辑）
3. 首页本月汇总 `LauncherContentViewModel.kt:144,148`（同上逻辑）
4. 列表条目金额 `LauncherContentScreen.kt:731-753`（被吸收支出用 amount+删除线，其它用 finalAmount）
5. 详情「实际金额」`RecordDetailsSheet.kt:179`（直接显示 finalAmount）

显示层透传：`RecordModelTransToViewsUseCase` 把 `RecordModel.finalAmount` 原样传入 `RecordViewsModel.finalAmount`（`:76,166`）。**故数据层把净自付落到 finalAmount 后，5 消费方自动正确。**

### 不受 finalAmount 影响（已实证，仅需测试守住）

- **资产余额**：`insert`/`delete` 算 balance 用 `recordAmount`（`:290,474`），完全独立于 finalAmount 吸收模型。
- **批量导入** `batchImportRecordsTransaction`：导入记录无关联，`finalAmount=recordAmount`（`:410`），不进吸收路径。
- DB_VERSION=12（`ApplicationInfo.kt:29`），`finalAmount` 列已存在（`RecordTable.kt:62`）。
- 启动 worker 框架：`InitWorker.doWork()`（`:62-65`）每次启动调 `migrateSpecialTypes()`（幂等，`TypeRepositoryImpl.kt:199-223`，靠 DataStore typeId 标记）。

## 3. 需求决策（已与用户确认）

| # | 决策 | 选定 |
|---|---|---|
| 1 | 金额语义方向 | **净自付模型**：本月支出计净自付（100−80=20）；报销/退款款不计入本月收入 |
| 2 | 超额（报销额>支出额，如花100报120）| **下限归 0 + 溢出计收入**：支出净自付下限 0；吸收者 finalAmount=max(0,溢出) 计入收入 |
| 3 | 实现路线 | **存储重算 finalAmount**（跨期报销决定：本月花下月报，聚合层实时算会把本月支出对冲成负）|
| 4 | 列表被吸收支出显示 | **净额优先**：主金额=净自付 ¥20，去删除线（配合已有「已报销(¥80)」标签）|
| 5 | 报销/退款款收入显示 | **显示实收额**：列表主金额=amount(¥80)+性质标签；聚合仍用 finalAmount=溢出(0) 不虚增收入 |
| 6 | 一对多/多对一分摊 | **顺序贪心填充**（见§5，整数守恒确定，非比例分摊）|

## 4. finalAmount 新语义（核心不变量）

| 记录 | 重构后 finalAmount |
|---|---|
| 未吸收记录 | recordAmount（不变）|
| 被吸收支出 | 净自付 = recordAmount − 被对冲额（≥0）|
| 报销/退款款（吸收者）| 溢出额 = max(0, recordAmount − 对冲额)（≥0，通常 0）|

**不变量**：① 一个吸收簇内 `Σ finalAmount = 净额`（Σ支出 − Σ报销退款，下限相关）② 所有 `finalAmount ≥ 0`（消灭 M2 负值）。

## 5. 分配算法（顺序贪心填充）

**吸收簇定义**：通过 `db_record_with_related` 连通的记录集合（一个支出可被多个吸收者吸、一个吸收者可吸多笔支出，构成二部图的连通分量）。分配以「簇」为单位，保证结果与吸收者插入历史无关、仅依赖 id 顺序，从而**增量重算与全量重算结果一致**。

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

### 守恒验证（全场景）

| 场景 | 结果 | 净额校验 |
|---|---|---|
| 1:1 部分（E100,I80）| E.fa=20, I.fa=0 | 本月支出20 ✓ |
| 超额（E100,I120）| E.fa=0, I.fa=20 | 支出0/收入20，净赚20 ✓ |
| 1对N（I80→E100,E50）| E1.fa=20,E2.fa=50,I.fa=0 | 支出70（花150报80）✓ |
| N对1（E100←I30,I40）| E.fa=30,I1.fa=0,I2.fa=0 | 支出30（花100报70）✓ |
| 过度吸收（E100←I80,I80）| E.fa=0,I1.fa=0,I2.fa=60 | 支出0/收入60，净赚60 ✓ |

纯整数无取整误差；依赖 id 顺序，确定可复现。**局限**：1对N 时净自付集中在前几笔支出（单条显示），但簇内总额守恒——属可接受权衡（用户已确认）。

## 6. 数据层改造（`TransactionDao`）

抽出**单一纯算法函数** `recalculateClusterFinalAmount(absorberId)`（或等价"重算一个吸收者+其被吸收支出"），按§5 顺序填充，三处复用，**消除 insert/recalc 口径不一致隐患**：

- `insertRecordTransaction` 吸收分支（`:367-375`）：建关联后调统一重算（替代当前 `Σ finalAmount` 清零逻辑）
- `recalculateAbsorberFinalAmount`（`:166-186`）：重定义为净自付分配（吸收者=溢出、被吸收=净自付），统一用 recordAmount 口径
- `deleteRecordTransaction`（`:524-554`）：删支出/删吸收者后，对受影响吸收者按§5 重算（被吸收支出净自付随之恢复）

> **增量一致性（正确性关键）**：任何增删改触发时，先从变更记录沿关系表 BFS 发现整个吸收簇（连通分量），将簇内所有记录 finalAmount 重置为 recordAmount，再按 §5 对整簇重算。这样增量结果与全量重算逐字段一致、与吸收者插入历史无关——避免"在 E 当前残值上继续填充"导致的顺序依赖错算。plan 阶段实现簇边界发现（从受影响支出反查所有吸收者、再从吸收者查所有被吸收，迭代至闭包）。

## 7. 历史数据重算

- 新增 `recalculateAllFinalAmount()`（Repository/DAO 层）：全量按§5 重算所有记录 finalAmount（先全置 recordAmount，再按吸收者 id 升序填充全表关联）。
- 挂 `InitWorker.doWork()`（`migrateSpecialTypes()` 之后），复用 DataStore 幂等标记模式（新增标记位，如 `finalAmountRecalcDone`/版本号）。
- **不升 DB_VERSION、无 Room schema migration**（finalAmount 列结构不变，纯数据值迁移）。
- **WebDAV 备份恢复后强制重算**：恢复导入的是旧语义 finalAmount，恢复流程须清除幂等标记或直接调 `recalculateAllFinalAmount()`。plan 阶段定位恢复入口（`RecordRepository`/备份恢复 UseCase）并接线。
- 重算幂等：基于 amount/charges/concessions（原始值）+ 关系表从头算，多次跑结果一致。

## 8. 显示层

- **列表**（`LauncherContentScreen.kt:731-753`）：去删除线（`isReimbursed`/`LineThrough` 逻辑删除）；`displayAmount` 改为：
  - TRANSFER → amount（不变）
  - INCOME 且 relatedRecord 非空（吸收者）→ amount（实收，决策 #5）
  - 其它（含被吸收支出）→ finalAmount（净自付）
- **详情「实际金额」**（`RecordDetailsSheet.kt:179`）：保持 finalAmount（净额语义）。报销款显 ¥0 有上方「金额 ¥80」行陪衬可解释（决策 #5 确认详情不像列表显实收）。
- 报销/退款款（INCOME 吸收者）列表关联标签文案：当前 NONE→`R.string.related`「已关联」；可调为「关联支出」（preview 示意），属文案细节，plan 定。

## 9. 已知局限（不在本次范围）

- `GetAssetMonthSummaryUseCase` 用 recordAmount（资产流水全额），与首页本月汇总（净自付）维度不同——资产流水应显全额交易，**不强行统一**。
- 1对N 净自付集中前几笔（§5 局限）。

## 10. 测试策略

- **DAO androidTest**（`TransactionDaoTest`）：insert/delete/update 吸收各场景（§5 五场景）finalAmount 守恒+非负；多对一/二次吸收。
- **`FakeTransactionDao`**（`core/data` test）：忠实复刻净自付语义（CLAUDE.md 强制——禁桩；既往 queryByTimeAndAmount 等假阳性教训）。
- **重算算法单测**：`recalculateAllFinalAmount` 全场景；幂等（连跑两次结果一致）。
- **资产余额不变性**：重构前后资产 balance 不变（验证 §2 独立性）。
- **聚合消费方**：月度结余/柱状图/首页汇总在净自付下分项正确（`GetCurrentMonthRecordViewsMapUseCaseTest` 等补算例）。
- **显示层**：`feature:records` 截图/ViewModel 测试同步（列表去删除线、报销款显实收、详情实际金额净自付）；改 Composable/ViewModel 签名时同步 `*ScreenshotTests`/`*ViewModelTest`（模块测试源集整体编译）。
- 全量（命令示意，具体任务名 plan 精化）：`core/data` 的 `FakeTransactionDao` 逻辑测试走 `:core:data:testDebugUnitTest`；`core/database` 真实 DAO 测试在 `androidTest`（instrumented，需设备/模拟器或 Robolectric，plan 确认运行方式）；`:core:domain:testDebugUnitTest`、`:feature:records:testOnlineDebugUnitTest` 覆盖聚合与显示。

## 11. 涉及文件（预估，plan 阶段精化）

| 文件 | 改动 |
|---|---|
| `core/database/.../dao/TransactionDao.kt` | §6 统一净自付重算函数 + insert/delete/recalc 改造 |
| `core/data/.../repository/RecordRepository(Impl).kt` | 新增 `recalculateAllFinalAmount()` |
| `core/data/.../repository/.../SettingRepository` 或 DataStore | 重算幂等标记 |
| `sync/work/.../InitWorker.kt` | 接线重算（migrateSpecialTypes 后）|
| 备份恢复入口（plan 定位）| 恢复后触发重算 |
| `feature/records/.../screen/LauncherContentScreen.kt` | 列表去删除线 + displayAmount 改 |
| `feature/records/.../view/RecordDetailsSheet.kt` | 文案/标签微调（实际金额已自动正确）|
| `core/data` test `FakeTransactionDao.kt` | 复刻净自付语义 |
| 各 DAO/UseCase/ViewModel/截图测试 | 算例与签名同步 |

## 12. 风险与回滚

- 数据层核心改造，回归面集中在记录增删改 + 5 消费方 + 重算。
- 重算幂等可重跑；纯代码 + 数据值迁移（无 schema），`git revert` 后 `finalAmount` 可由重算恢复任一语义（旧语义需回退代码后重算）。
- 实施走 git worktree 隔离（大修改约束），验收后 FF 合入 main。
