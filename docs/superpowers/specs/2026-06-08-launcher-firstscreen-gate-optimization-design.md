# 首屏 gate 优化（M-1 净自付重算去 gate）+ 备份恢复刷新一致性（F2）设计

> 创建于 2026-06-08。来源：finalAmount 净自付重构 full-review 授权 defer 的 backlog（M-1/F2）。
> 优先级依据：用户 `M-1（最值得修、最低成本）> F-1 删账本 > L1`，本次只做 M-1 + F2。

## 1. 背景与问题

净自付重构（main `7114045e`）后，老用户首次启动新版本时需要做一次全表 `finalAmount` 净自付重算。当前实现把该重算放在 `LauncherContentViewModel.init` 的 gate 里，阻塞首屏直到完成：

- **M-1（性能）**：`recalculateAllFinalAmount` 全量重算在 `LauncherContentViewModel.init` gate 内执行，`uiState` combine `_migrationCompleted`（`!completed → Loading`）。老用户数万条记录重算耗时数秒，期间首页卡 Loading。
- **F2（架构/一致性）**：`BackupRecoveryManagerImpl` 恢复成功后直调 `database.transactionDao().recalculateAllFinalAmount()`，**绕过 Repository**——不置 `finalAmountNetRecalcDone` 标记、不 bump `recordDataVersion`。后果：①恢复后下次启动可能冗余再跑（幂等无错）；②恢复后若不重启停前台，列表/饼图可能显示重算前缓存值。

## 2. 范围

**本次做：**
- 改动 1（M-1）：`LauncherContentViewModel.init` 首屏 gate 分离——netRecalc 分支去 gate（后台静默重算），db9To10 迁移分支保留 gate。
- 改动 2（F2）：`BackupRecoveryManagerImpl` 恢复后改走 `recordRepository.recalculateAllFinalAmount()`，统一副作用（置标记 + bump version）。
- 对应测试。

**本次不做（非目标）：**
- ~~改动 3：给 `getRecordPagingData` 加 `recordDataVersion` 双保险~~ —— **已剔除**。核实后该改动冗余且引入回归，理由见 §6。
- F-1（删账本 O(N²)）、L1（`RecordDao.queryRelatedRecord` 死代码）—— 留 backlog，按用户优先级在后续会话单独立项。

## 3. 已核实事实（代码引用）

| 事实 | 出处 |
|---|---|
| init gate：两分支末尾才置 `_migrationCompleted=true` | `LauncherContentViewModel.kt:67-79` |
| `uiState` combine `_migrationCompleted`，`!completed → Loading` | `LauncherContentViewModel.kt:132-172` |
| `recalculateAllFinalAmount()` 已 bump `recordDataVersion` + 置 `finalAmountNetRecalcDone` | `RecordRepositoryImpl.kt:509-513` |
| 汇总流 `queryRecordViewSummariesFlow` **订阅** `recordDataVersion` → bump 后自动刷新 | `RecordRepositoryImpl.kt:351-365` |
| 分页流 `getRecordPagingData` **未订阅** version；`pagingQueryByBooksIdBetweenDate` 是 `@Query` 返回 `PagingSource`（Room 生成，对 `db_record` UPDATE 自动 invalidate，确定行为） | `RecordRepositoryImpl.kt:330-349`、`RecordDao.kt:84-88` |
| `Migration9To10` 加 `final_amount` 列为 `REAL DEFAULT 0 NOT NULL`（`Migration11To12` 后转 INTEGER 分单位）——未跑应用层填充前全为 0（两种类型下"0"均成立，本方案不改列类型） | `Migration9To10.kt:44`、`Migration11To12.kt:55,69` |
| F2 现状：恢复后直调 DAO，绕过 repository | `BackupRecoveryManagerImpl.kt:699` |
| F2 可行性：`RecordRepositoryImpl` 只依赖 `recordDao`/`transactionDao`/`combineProtoDataSource`/`coroutineContext`，**不依赖 BackupRecoveryManager** → 注入无循环依赖；同一 Hilt 单例 `database` 下同库操作 | `RecordRepositoryImpl.kt:54-59`、`BackupRecoveryManagerImpl.kt:83-91` |
| F2 事务/线程边界（controller hands-on 核验）：恢复段无外层显式 SQLite 事务（`:684-692` 裸 `execSQL` autocommit，`recoveryFromDb` 返回后其内部事务已结束）；`ioCoroutineContext` 与 `RecordRepositoryImpl.coroutineContext` 同为 `@Dispatcher(IO)` → `withContext` 切换 no-op，Room suspend `@Transaction` 用自身 executor。F2 改走 repository 切 dispatcher 安全 | `BackupRecoveryManagerImpl.kt:681-699,90`、`RecordRepositoryImpl.kt:58,509` |

**两分支差异的事实依据**：`!db9To10DataMigrated` 用户（新装/从 v9 升级）在 `migrateAfter9To10()` 跑完前 `final_amount` 全为 `Migration9To10` 的 DEFAULT **0**，首屏会显示全 `0.00` → 必须 gate；netRecalc 老用户（`db9To10DataMigrated=true && finalAmountNetRecalcDone=false`）的 finalAmount 是**旧吸收模型值**（净自付重构前 `migrateAfter9To10` 写入：被吸收支出=0、吸收者=`amount−charge−Σ被吸收`**可负**），**非净自付正确值**——不 gate 时首屏短暂显示该旧值（被吸收报销支出显 0、报销月份分项偏差），重算后刷新到净自付值，相比卡 Loading 数秒可接受。

## 4. 设计

### 改动 1（M-1）：`LauncherContentViewModel.init` gate 分离

把 netRecalc 分支的 `_migrationCompleted=true` 提到 `recalculateAllFinalAmount()` 之前；db9To10 分支保持"先跑后置位"。

```kotlin
init {
    viewModelScope.launch {
        val tempKeys = settingRepository.tempKeysModel.first()
        if (!tempKeys.db9To10DataMigrated) {
            // final_amount 全为 Migration9To10 DEFAULT 0，首屏会显全 0 → 必须 gate 等填充完成
            recordRepository.migrateAfter9To10()
            _migrationCompleted.value = true
        } else {
            // 已迁移：立即放行首屏（finalAmount 为旧吸收模型值——被吸收支出=0/吸收者可负，首屏短暂显旧值）
            _migrationCompleted.value = true
            if (!tempKeys.finalAmountNetRecalcDone) {
                // 净自付重算后台静默跑；完成后 recalculateAllFinalAmount 内部 bump recordDataVersion，
                // 汇总流（订阅 version）+ 列表（Room PagingSource 对 db_record UPDATE 自动 invalidate）自动刷新到净自付值
                recordRepository.recalculateAllFinalAmount()
            }
        }
    }
}
```

要点：同一 `viewModelScope.launch` 协程内，`recalculateAllFinalAmount()` 是 suspend，置位后继续在后台执行；但 `_migrationCompleted` 已为 true，`uiState` 不再 Loading，首屏立即可见。

### 改动 2（F2）：`BackupRecoveryManagerImpl` 恢复后走 Repository

- 构造注入新增 `private val recordRepository: RecordRepository`（已验证无循环依赖）。
- `BackupRecoveryManagerImpl.kt:699` 由 `database.transactionDao().recalculateAllFinalAmount()` 改为 `recordRepository.recalculateAllFinalAmount()`。

副作用统一：重算 + 置 `finalAmountNetRecalcDone=true` + bump `recordDataVersion`。
- 语义正确性：恢复后确已对合并后的全表重算，置标记 true 表示"当前库已是净自付语义"，下次启动 netRecalc 分支不重复跑（即便跑也幂等无错）。
- bump version：恢复后无重启停前台时，汇总流自动刷新；列表经 Room invalidate 刷新。

## 5. 测试策略

### `LauncherContentViewModelTest`（feature:records，FakeRecordRepository + UnconfinedTestDispatcher）

- **保留**：`when_db9To10_done_but_net_recalc_not_done_then_recalculateAllFinalAmount_called`（netRecalc 仍被调一次）。
- **新增（去 gate 区分力，对应 H-1/M-D）**：netRecalc 分支首屏立即 `Success`（不卡 Loading）。要点：
  - Fake 的 `recalculateAllFinalAmount` 需可控挂起（`CompletableDeferred`），**且暴露"已进入重算"信号**（如 `recalcStarted: CompletableDeferred` 或计数）供测试确认协程确实停在挂起点。
  - 断言时机：在"已进入重算、未放行"窗口读 `uiState == Success`（此时 `_migrationCompleted` 已 true），再 `deferred.complete()` 放行确认无异常。
  - **区分力验证（关键）**：该用例对**旧顺序**（先 recalc 后置位）必须 FAIL（挂起中 `_migrationCompleted=false` → Loading），切到**新顺序**才 PASS。plan 实施时先证旧顺序红、再证新顺序绿，否则 Fake no-op 下改前改后都绿、测不出去 gate 退化。
- **新增（可见性回归，对应 H-1）**：重算"前"列表/汇总用旧 finalAmount、放行重算"后"刷新到净自付新值——证明刷新链（version + Room invalidate）真生效，而非首屏值本就对。
- **新增（db9To10 gate）**：`!db9To10DataMigrated` 时 `migrateAfter9To10` 未完成 → `uiState` 为 `Loading`，完成后转 `Success`。
- **测试替身改造**：`FakeRecordRepository` 给 `recalculateAllFinalAmount` **与** `migrateAfter9To10` 都加 invocationCount + 可控挂起开关（两者对称，对应 F-feas-3）。**开关默认 = 立即返回不挂起**（M-F 硬约束）——该 Fake 被 **32 个测试文件**复用，默认不挂起才不破坏现有测试。implementer 改完须跑全部受影响模块 `testDebugUnitTest`（core:domain、feature:assets、feature:record-import、feature:records、feature:settings、feature:types），**不能只跑 feature:records**。

### `BackupRecoveryManagerImpl`（F2）

- 先核实现有测试覆盖（writing-plans 阶段确认；已知 `BackupRecoveryManagerSchemeTest` 只测 Companion 纯函数 `normalizeWebDAVScheme`、不实例化 Manager）。
- 验证恢复成功路径调用 `recordRepository.recalculateAllFinalAmount()`（而非直调 DAO）。Manager 构造依赖 Context/WebDAV/database 实例化成本高，若确无单测则**降级为编译期签名保证 + 人工核验调用点**，并在交付说明**显式标注**"F2 调用点替换无自动化测试覆盖，靠编译期签名 + 人工 diff 核验"（对应 F-feas-4）——repository 已有的 `recalculateAllFinalAmount` 单测（`TransactionDaoLogicTest`）只证该方法本身正确，**不证** `:699` 真调的是 repository，不得用"复用已有单测"措辞掩盖替换点本身未覆盖。

## 6. 方案选型记录

### netRecalc 去 gate 实现方式

| 方案 | 说明 | 取舍 |
|---|---|---|
| **A（采纳）** | 分支内把 `_migrationCompleted=true` 提到重算前，同协程后台续跑 | 最小改动、复用现有刷新链、db9To10 分支零改动 |
| B | netRecalc 拆独立 `launch` 协程 | "后台任务"语义更显式，但多一协程、跨协程读 tempKeys 反绕 |
| C | 上移 WorkManager/Application | 单次迁移严重 over-engineering，违反 YAGNI |

采纳 A，理由：M-1 标注"最低成本"，A 改动面最小且复用已就位的 `recordDataVersion` + Room invalidate 刷新机制。

### 改动 3（分页流双保险）剔除理由

最初设想给 `getRecordPagingData` 加 `recordDataVersion` 双保险，"不依赖 Room 自动 invalidate 推测"。核实后剔除：

1. **Room 自动 invalidate 是确定行为非推测**：`pagingQueryByBooksIdBetweenDate`（`RecordDao.kt:84-88`）是 `@Query` 返回 `PagingSource`，Room 生成，自动注册 InvalidationTracker 监听 `db_record`；netRecalc 重算的 UPDATE 必触发 invalidate，列表自动刷新且保留滚动位置。
2. **加 version 引入回归**：普通增删改记录都 bump `recordDataVersion`（`RecordRepositoryImpl` 7 处）。若 `combine(recordDataVersion).flatMapLatest { Pager(...) }`，则**每次增删改**都重建整个 Pager → 列表回顶 / 丢失滚动位置 + 整页重载，比 Room 增量 invalidate 重。

收益冗余 + 回归代价 → 不做。

## 7. 影响评估 / 回归分析

- **首屏数据短暂不一致（netRecalc 老用户）**：去 gate 后首屏先用吸收模型旧 finalAmount 渲染，重算 UPDATE 后由 Room invalidate（列表）+ version（汇总）刷新到净自付值。差异仅限被吸收报销支出（旧值 0 → 净自付）与吸收者，多数记录不变，且短暂。这是 M-1 的预期取舍（相比卡 Loading 数秒更优）。
- **db9To10 分支体验不变**：保留 gate，首屏 Loading 直到 finalAmount 填充完成，避免显示全 0。⚠️ implementer 必须保持该分支"先 `migrateAfter9To10()` 后置 `_migrationCompleted=true`"，不可顺手把两分支都改成"先置位"（对应 impact L-1）。
- **F2 副作用变更**：恢复后新增置 `finalAmountNetRecalcDone=true` + bump version。需确认恢复流程其它地方不依赖"该标记在恢复后保持原值"——当前恢复流程已重置多个迁移标志（`BackupRecoveryManagerImpl.kt:694-696` 重置 refund/reimburse/creditCard typeId 为 0 触发下次启动应用层迁移），置 netRecalc 标记 true 与该模式一致（恢复后数据已是新语义，无需再迁移）。
- **DI 变更**：`BackupRecoveryManagerImpl` 构造新增 `RecordRepository` 参数，无循环依赖（已验证）。若有该 Manager 的测试/Fake 需同步构造。
- **现有测试兼容**：`FakeRecordRepository` 加挂起开关默认立即返回，现有 `LauncherContentViewModelTest` 其余用例不受影响。
- **并发安全性（对应 reverse R-3）**：去 gate 后台重算与用户增删改记录经 Room **单写连接串行**执行（suspend `@Transaction` 共享单写连接，不真正并发 SQL）；二者均维护吸收簇 finalAmount，无论增删改排在全量重算前/后都基于当时已提交状态 → 最终一致。**前提不变量**：不引入第二个写连接 / 不开 multi-instance invalidation。
- **半完成态幂等兜底（对应 reverse R-2）**：`recalculateAllFinalAmount` 三步（DAO `@Transaction` 重算 → 置 `finalAmountNetRecalcDone` → bump version）中后两步在 DAO 事务**之外**；DAO 提交后、置 flag 前进程被杀 → "finalAmount 已净自付但 flag=false"，下次启动 netRecalc 分支重跑。重跑无害因 `recalculateAllFinalAmount` 幂等（`TransactionDao.kt:287` 仅写变化项，二次全表零 UPDATE）。**前提不变量**：该幂等性是去 gate 后台化的正确性依赖，禁止改为非幂等 / 带 flag 短路。

## 8. 非目标

- 不改 `recalculateAllFinalAmount` / `migrateAfter9To10` 的算法（净自付簇 BFS + 顺序贪心填充保持不变）。
- 不动 F-1（删账本 O(N²)）、L1（死代码）。
- 不改饼图/列表显示层口径。
