# 测试套件全量评估 审计报告

> 日期：2026-06-11 ｜ 分支：main ｜ 性质：纯审计（未改任何代码）
> 方案：[审计设计](2026-06-11-test-suite-audit-design.md) ｜ 计划：[执行计划](../plans/2026-06-11-test-suite-audit.md)
> 审计方式：T1 金额核心 controller 亲审 ｜ T2/T3 Workflow 只读 fan-out + controller 核验
> **报告完成** ｜ **结论速览见文末 [Phase 4 执行摘要](#phase-4--综合报告执行摘要)**：0 Critical / 15 High / 39 Medium / 16 Low，两个系统性结构问题（androidTest 设备门控 + 假阳性/弱断言测试）。

## Finding 格式与严重度

`[严重度] 维度 | 模块 | 源 file:line ↔ 测试 file:line | 问题 | 修复建议`

- **Critical**：假阳性覆盖致真实 bug 可漏网
- **High**：关键业务路径完全无覆盖 / 弱断言掩盖逻辑错误
- **Medium**：反向/边界/异常分支缺失
- **Low**：次要工具/边角缺口
- 维度：①覆盖广度 ②逻辑正确性 ③反向/边界/异常

---

# Phase 1 — T1 金额核心（Controller 亲审）

## T1 core/model

### 可测单元分类（50 源文件）

- **有逻辑需测**：
  - `model/RecordAmount.kt` — 三口径金额函数（`recordAmount` / `analyticsPieAmount` / `analyticsPieNetAmount`）
  - `transfer/ModelTransfer.kt` — 4 个 Model↔Entity 映射函数
  - enum companion 映射（`RecordTypeCategoryEnum` 等约 10 个 enum 的 `fun`/`when`）+ `RecordViewsModel`/`ResultModel`/`RecordTypeModel` 少量计算属性 —— 低风险，集中观察
- **数据类豁免**（无逻辑分支，不计缺口）：其余 ~37 个 data class / sealed / 纯 entity（如 `AssetModel`、`BooksModel`、`RecordModel`、各 `entity/*`、`Selectable`/`Expandable` 接口等）
- **空文件**：`ext/Ext.kt`（仅 license + package，无任何声明）—— 非缺口

### Findings

**[Low] ②+① | core/model | `model/RecordAmount.kt:66-76`（`analyticsPieNetAmount`）↔（无直接单测，但 usecase 层判别性覆盖）**
- 三口径中 `analyticsPieNetAmount` 是唯一**无专属 model 级单测**的（另两口径各有 `RecordAmountTest.kt:38-42`/`AnalyticsPieAmountTest.kt:38-42` 判别性金丝雀）。
- **Task 4 已核实间接覆盖判别性成立**（原拟 Medium 降为 Low）：`TransRecordViewsToAnalyticsPieUseCaseTest` 对该函数三分支均有判别性用例——`:111` EXPENDITURE finalAmount=2000≠amount 10000 断言 2000、`:131` INCOME 断言 finalAmount 480000≠amount-charges 490000、`:150` TRANSFER 断言 9800=amount-charges≠finalAmount（#10b 金丝雀，误简化为"总返回 finalAmount"会被此用例抓到）。
- 仅剩 model 级定位性缺口（unit 级金丝雀更靠近函数本身、跑得快）。
- **修复建议（可选）**：补 `AnalyticsPieNetAmountTest` 把 #10b 金丝雀下沉到 model 单测层（locality + 速度），非必须。

**[Low] ① | core/model | `transfer/ModelTransfer.kt:26-103` ↔ （无直接测试）**
- 4 个映射函数（`RecordModel.asEntity` / `RecordEntity.asModel` / `RecordTypeModel.asEntity` / `RecordViewsModel.asEntity`）无直接单测。机械字段拷贝，风险低，但字段漂移（漏拷 / 错位 / 新增字段忘同步）无测试守护——`RecordViewsModel.asEntity`（77-103 行）拷贝 20+ 字段尤甚。
- **修复建议**：补往返映射等值测试（`model→entity→model` 全字段相等）。注：可能被 core/data 的 `MappingTest.kt` 间接覆盖，Task 2/3 交叉确认。

### 小结
core/model 金额三口径核心逻辑覆盖良好（2/3 有 model 级判别性金丝雀单测，第 3 个 `analyticsPieNetAmount` 由 usecase 层判别性覆盖——Task 4 已证实）。无 Critical/High，仅 2 个 Low（locality + ModelTransfer 映射）。

## T1 core/data（假阳性桩 + 事务逻辑）

### 桩忠实度核查结论（重点）

**优点（非缺口，明确记录）**：`FakeTransactionDao`（`testdoubles/FakeTransactionDao.kt:300-302`）**不 override 任何 `@Transaction` 默认方法**，继承真实默认实现（簇 BFS `discoverClusterIds`、顺序贪心 `recalculateFinalAmountFromCluster`、`deleteRecordsBatch`、`insertRecordTransaction` 等）跑在忠实建模的基础 CRUD 上。逐一核对基础方法忠实：`queryRelatedByRecordId`=filter recordId（↔ 真实 `WHERE record_id=:id`）、`queryRelatedByRelatedRecordId`、`queryRecordsByAssetId`=assetId||intoAssetId（↔ `WHERE asset_id OR into_asset_id`）、`clearRelatedRecordById`、`updateRecordFinalAmountById` 均对齐。故 `TransactionDaoLogicTest` 的 24 个用例（簇部分吸收/超额溢出/1toN/Nto1/charges/exclude、删除三场景含判别性、recalcAll 幂等+等价增量含转账 -300 负值、删账本/删资产 AUDIT-2 余额回退、计数器区分力）**跑真实算法、非假阳性，覆盖强**。

### Findings

**[High] ① | core/data | `TransactionDao.kt:516-573`（`batchImportRecordsTransaction`）↔（无任何测试）**
- 微信账单批量导入的核心事务，含 per-asset 余额聚合（按资产分组累计 income/expenditure，信用卡 vs 非信用卡分支 `:561-567`）。生产经 `RecordRepositoryImpl.kt:575` 调用（微信导入路径）。
- 全仓 grep 确认**零测试覆盖**：不在 `TransactionDaoLogicTest`、不在 androidTest `TransactionDaoTest`、`RecordRepositoryImplTest` 也无导入测试（其只测查询/搜索/历史）。`FakeTransactionDao` 继承真实默认实现可直接 JVM 测，但无人写。
- 风险：批量导入后资产余额错算（聚合分支 bug）无测试守护。
- **修复建议**：补 `TransactionDaoLogicTest` 用例，setup 资产 + 多条混合收支记录批量导入，断言各资产余额聚合正确（含信用卡分支）+ 返回 id 列表 + finalAmount=recordAmount（无关联）。

**[Medium] ②(潜在)+① | core/data | `FakeRecordDao.kt:407` ↔ `RecordDao.kt:437`（`queryByWechatTransactionId`）**
- 真实 SQL `remark LIKE '%[微信单号:' || :transactionId || ']%'`（方括号定界）；`FakeRecordDao` 实现为裸 `it.remark.contains(transactionId)`——**偏离方括号定界语义**（remark="转账1234元"、id="1234" 时 Fake 匹配但真实 SQL 不匹配）。
- **孪生 Fake 不一致**：`core/testing/FakeRecordRepository.kt:389` 已于 2026-06-01 修复为忠实 `[微信单号:$transactionId]` marker 并被 `RecordImportViewModelTest.kt:222-242` EXACT 路径覆盖；但 `core/data` 的 `FakeRecordDao` 同名方法被遗漏未同步。
- 当前无 active 假阳性（`RecordRepositoryImpl.queryByWechatTransactionId` `:556-560` 本身无 repository 级测试，故 FakeRecordDao 该方法实际未被任何测试调用）——属**潜在 landmine**：未来若加 RecordRepositoryImplTest 用此 Fake 会得到比生产更松的匹配。
- **修复建议**：① `FakeRecordDao.queryByWechatTransactionId` 改用 `"[微信单号:$transactionId]"` marker contains 对齐真实 SQL + FakeRecordRepository；② 补 `RecordRepositoryImpl.queryByWechatTransactionId` repository 级测试（含方括号定界判别性：bare id 不匹配 / bracketed 匹配）。

**[Medium] ①(结构) | core/data ↔ core/database | `TransactionDao.kt:402-468`（insertRecordTransaction 余额）/`:336-368`（verifyAssetBalance）**
- insert/转账的资产余额更新（信用卡/非信用卡/转账目标资产）+ `verifyAssetBalance` 的余额一致性校验，**仅在设备门控的 androidTest `TransactionDaoTest.kt`（余额断言 `:391/414/443/659/856`、verifyAssetBalance `:956-1005`）覆盖**——本机无设备不跑、CI 通常也不跑 instrumented test。
- JVM 可跑的 `TransactionDaoLogicTest` 只覆盖**删除侧**余额回退（`:441-455` 转账对方资产），**insert 侧余额完全未在 JVM 触达**（insert 用例 `:244` 未 setup 资产，`queryAssetById` 返 null 跳过余额分支）。
- `FakeTransactionDao` 已忠实建模 assets 且删除侧余额已在 JVM 跑通——insert 侧余额 + verifyAssetBalance 完全可迁入 JVM 套件获得快速回归保护，却未做。
- **修复建议**：在 `TransactionDaoLogicTest` 补 insert 侧余额用例（setup 资产 → insertRecordTransaction → 断言余额；覆盖信用卡/非信用卡/转账目标三分支）+ verifyAssetBalance 用例。

**[Low] ③ | core/data | `TransactionDao.kt:411-425`（insertRecordTransaction 防御性 throw）/ `:653`（deleteRecordCore 删除失败 throw）↔（JVM 无反向测试）**
- 落库守卫（type 为 null / asset 未找到 / 转账目标资产未找到 → `DataTransactionException`）与删除失败 throw 的反向路径，JVM 套件无覆盖（androidTest 亦未见对应 throw 用例）。
- **修复建议**：补 `assertThrows<DataTransactionException>` 用例覆盖三类守卫 + 删除不存在记录。

**[Low] ②(潜在) | core/data | `FakeRecordDao.kt:430,438`（queryExportRecords/countExportRecords）/ `FakeTransactionDao.kt:240-242`（deleteTagsByBookId no-op）**
- `queryExportRecords` 返 `emptyList()`、`countExportRecords` 返 `0`（注释"测试中不依赖"）；`deleteTagsByBookId` 为 no-op（"Fake 中标签不追踪 booksId"）。均为有意简化，当前无测试依赖→无 active 假阳性，但属潜在 landmine（导出/删账本标签清理若日后用这些 Fake 验证会假绿）。
- **修复建议**：保持现状即可，但在 Fake 方法上补 `// 若需测试导出/删账本标签清理，必须先忠实建模` 警示注释，防误用。

### 小结
core/data 桩设计整体优秀（继承真实 @Transaction 默认实现是正确模式，簇/重算/删除算法真实覆盖强）。主缺口：①`batchImportRecordsTransaction` 零覆盖（High）；②insert 侧余额/verifyAssetBalance 仅 androidTest 设备门控（Medium）；③`FakeRecordDao` wechat 单号桩遗漏未同步孪生修复（Medium 潜在）。无 Critical（无 active 假阳性致 bug 当下漏网）。

## T1 core/data（Repository + helper）

### 优点（明确记录）
- **helper 反向覆盖良好**：`BillCategoryMatcherTest`（4 测含 2 反向 null：无关键词命中 / 命中但类型缺失）、`BillPaymentMatcherTest`（5 测含 strategy4 unmatched 返负 id）、`WechatBillParserTest`（6 测含 invalid/neutral-direction/too-few-columns 三反向 null）、`DailyAccountExporterTest`（9 测含空列表/CSV 转义/引号转义/UTF8 BOM 反向边界）。
- **MappingTest（Table↔Model）覆盖好**：null id→-1、reimbursable on/off、invisible、protected 等字段/边界判别性覆盖。
- **RecordRepositoryImplTest 查询/搜索覆盖好**：queryById/asModel、queryByKeyword（remark + amount + amount sentinel -1 反向）、search history（blank 忽略 / 去重 / >10 截断反向）等。

### Findings

**[Medium] ①+③ | core/data | `uitl/impl/BackupRecoveryManagerImpl.kt`（备份恢复编排，~750 行）↔ 仅 `BackupRecoveryManagerSchemeTest`（覆盖 `normalizeWebDAVScheme`）**
- 唯一测试 `BackupRecoveryManagerSchemeTest` 仅覆盖纯函数 `normalizeWebDAVScheme`（`:748`，9 用例：dav/davs→https、https 保持、http/blank/无 scheme/不支持 scheme/仅 scheme 无 host 反向 reject、whitespace trim——反向覆盖好）。
- 但 `startBackup`/`startRecovery`/`createPreRestoreBackup`/`getRecoveryList`/`getLocalBackupList`/`upload`/`getWebFile`/`refreshConnectedStatus` 等备份恢复编排核心（整库备份/恢复 + WebDAV IO + 版本 bump）**全部无测试**。数据丢失风险（恢复 bug 覆盖用户数据 / preRestore 备份失败）核心路径无回归守护。
- IO 重难纯 JVM 测，但备份文件名/元数据解析（`getRecoveryList`/`getLocalBackupList`）可抽纯函数后测，`createPreRestoreBackup` 失败回滚反向路径可用 Fake 文件系统覆盖。
- **修复建议**：抽备份文件名/元数据解析为纯函数补测 + preRestore 失败反向用例。

**[Low] ① | core/model | `transfer/ModelTransfer.kt:77-103`（`RecordViewsModel.asEntity` 等 Model↔Entity 映射）↔（无测试，确认 Task 1 M-2）**
- 交叉核实：`MappingTest.kt` 覆盖的是 **core/data 的 Table↔Model**（`asTable`/`asModel`），与 core/model 的 **Model↔Entity（`asEntity`）是不同层**，不构成 ModelTransfer 覆盖。`RecordViewsModel.asEntity`（拷贝 20+ 字段）字段漂移无守护。
- **修复建议**：补 Model↔Entity 往返/全字段等值测试。

### 豁免
- `helper/AssetHelper.kt`（资产类型→`R.string`/`R.drawable` 资源 ID 映射）：返回 Android 资源 ID，需 Android 运行时不可纯 JVM 测，豁免（非缺口）。

### 小结
core/data helper 与 Repository 查询层覆盖良好且反向充分；主缺口是 BackupRecoveryManagerImpl 备份恢复编排核心仅覆盖一个纯函数（Medium，数据丢失风险路径）。

## T1 core/domain

### 覆盖结论：最强模块
- **广度满覆盖**：25 个 UseCase 全部有对应 `*Test`（25/25），另有 2 个 Repository 层测试（`AssetRecordBetweenDateRepositoryTest`、`TypeFirstSortRepositoryTest`）。
- **金额口径判别性覆盖（核心）**：两个 analytics pie usecase 测试均对 `analyticsPieNetAmount` 三分支判别性覆盖：
  - `TransRecordViewsToAnalyticsPieUseCaseTest`：`:111` EXPENDITURE finalAmount=2000≠amount 10000→断言 2000；`:131` INCOME 断言 480000≠490000；`:150` TRANSFER 断言 9800=amount-charges≠10150（**#10b 金丝雀**）。
  - `TransRecordViewsToAnalyticsPieSecondUseCaseTest`：`:104` drilldown finalAmount 判别、`:129` TRANSFER 9800 金丝雀，对称覆盖。
- **反向覆盖**：empty records→empty、平账记录（BALANCE_EXPENDITURE/INCOME）被排除出饼图、二级类型聚合到父级等边界。
- `GetAssetMonthSummaryUseCaseTest` 用 `recordAmount` 口径 + `balanceCategoryOrNull` 兜底解析（`:100` 注释证实结余口径与 verifyAssetBalance 一致性守卫）。

### Findings
无 Critical/High/Medium。core/domain 是本次审计覆盖质量最高的模块（满广度 + 判别性金额口径 + 反向边界）。

### 小结
core/domain 无缺口。金额三口径在此层（连同 core/model）形成完整判别性防护网，#10b TRANSFER 金丝雀双 usecase 守护。

## T1 core/database（静态审计，androidTest 未实跑）

> **审计方式声明**：本机无设备，`core/database` 全部测试为 androidTest（instrumented，需真实 SQLite），**未实跑，仅静态审源码覆盖**。下列覆盖结论基于测试源码静态分析。

### 覆盖结论：覆盖充分（但执行环境受限）
- **Migration 覆盖优秀（无缺口）**：`DatabaseTest.kt` 13 @Test——11 个 Migration（1→2…11→12）**每个有专属测试**，含 schema 验证 + 数据转换断言。重点：`migrate11_12`（元→分 INTEGER 重建）断言 `12.34→1234`/`88.88→8888`/边界 1.005 非负/14 索引/3 固定类型；`migrate6_7` 断言字段裁剪 + tag_ids 拆分 + protected 计算 + 余额回算；`migrateAll` 全链路 1→12 + Room schema 校验；`recovery_from_database` 备份恢复。
- **DAO 覆盖充分**：6 个 DAO 全有 `*DaoTest`（@Test 数：RecordDao 31、TransactionDao 26、TypeDao 16、AssetDao 7、TagDao 6、BooksDao 4，共 90 @Test）。`TransactionDaoTest`（1009 行）覆盖 insert/transfer 余额三分支、migrateTypeRecords、verifyAssetBalance、deleteTag。

### Findings

**[Medium] ①(结构/CI) | core/database（全 90 androidTest + DatabaseTest 13）↔ 设备门控不跑**
- core/database 全部 103 个测试为 androidTest（instrumented），**本机无设备不跑，CI 通常也不跑 instrumented（需 emulator）**。结果：这批覆盖充分、质量高的数据层测试（含 DAO SQL/Migration/余额）在快速本地/CI JVM 路径**零回归保护**——与 T2「insert 余额仅 androidTest」同根问题，影响面更大。
- 算法层有 JVM Fake 测试（`TransactionDaoLogicTest`）兜底簇/重算/删除，但 **DAO SQL 本身**（`@Query` 真实语义）+ Migration 仅靠 androidTest，平时不执行。
- 注：Room Migration 测试需 `MigrationTestHelper` + 真实 SQLite，确实难纯 JVM；DAO `@Query` 测试理论可 Robolectric + in-memory Room 跑（部分项目这么做）。
- **修复建议**：① CI 增加 emulator/instrumented test job（最直接，让 103 个测试真正护栏化）；或 ② 关键 DAO 查询语义用 Robolectric + in-memory Room 迁入 JVM 套件；至少在 README/CI 文档显式声明「数据层回归依赖 instrumented test，必须在合并前真机/emulator 跑过」。

### 小结
core/database 测试**写得好、覆盖全**（Migration 无缺口、DAO 充分），但全 androidTest 设备门控是本次审计最大的**系统性结构问题**：大量优质数据层覆盖在日常 JVM/CI 路径不执行。属执行环境问题非测试缺失。

---

## Phase 1（T1 金额核心）阶段小结

| 模块 | Critical | High | Medium | Low | 总评 |
|---|---|---|---|---|---|
| core/model | 0 | 0 | 0 | 2 | 金额三口径判别性覆盖完整 |
| core/data | 0 | 1 | 2 | 2 | 桩设计优秀；batchImport 零覆盖 + 余额仅 androidTest |
| core/domain | 0 | 0 | 0 | 0 | 最强模块，无缺口 |
| core/database | 0 | 0 | 1 | 0 | 覆盖全但全 androidTest 设备门控 |

**T1 关键结论**：
1. **金额核心逻辑（三口径 + 簇重算 + 删除/余额回退）覆盖优秀且判别性强**，#10b TRANSFER 金丝雀多层守护，无 Critical/无 active 假阳性。
2. **唯一 High**：`batchImportRecordsTransaction`（微信批量导入余额聚合）零覆盖。
3. **系统性结构问题（贯穿 core/data + core/database）**：大量数据层覆盖（insert 余额、verifyAssetBalance、全部 DAO SQL、全部 Migration）仅在设备门控 androidTest，日常 JVM/CI 不执行 → 实际回归保护远低于「覆盖率」表象。这是 T1 最值得决策的发现。

---

# Phase 2 — T2 深审（Workflow 只读 fan-out + Controller 核验）

> **核验声明**：以下 finding 由 4 个只读 agent fan-out 产出，**每条强断言已由 controller hands-on 核验**（Read 标的 file:line / grep 确认无测试）。agent 报告**无虚报**（x==x 恒真、复制版方法、零测试均经核实属实）。controller 对部分 agent 严重度做了**爆炸半径校准**（见各条「校准」注记）——金融/数据完整性路径维持高，纯展示/周期性/平凡函数下调。

## T2 sync/work

**优点（核实）**：`AutoBackupWorker.mapBackupStateToResult`（`AutoBackupWorker.kt:94-100`，`@VisibleForTesting` 抽为顶层纯函数脱离 Worker/Context 依赖）被 `AutoBackupWorkerTest` 4 用例覆盖全 3 个 when 分支，else 用两个不同 Failed code 验证（避免单 code 假阳性），isEqualTo 强断言。**可测性设计典范**。

**[Medium] coverage | `SyncWorker.kt:58-76` ↔ none**（agent 报 High，controller 核验属实→校准 Medium）
- 核实（Read SyncWorker.kt:53,67-68）：`retryCount` 实例变量，doWork 内 `retryCount++; if (retryCount >= 5) Result.failure() else Result.retry()`——真实重试阈值分支，零测试。与同模块已抽纯函数+测全分支的 `mapBackupStateToResult` 形成反差。
- 校准理由：周期性版本同步重试（非金融、低爆炸半径，且 WorkManager 自身有 runAttemptCount 重试）→ Medium 而非 High。
- 修复建议：把重试决策抽为纯函数 `nextSyncResult(success, retryCount)` + 5 提为常量，测成功/retryCount<4→retry/达 4→failure 边界。

**[Low] reverse | `AutoBackupWorker.kt:95-100` ↔ `AutoBackupWorkerTest.kt:45-59`**：else 分支未对 `BackupRecoveryState.None/InProgress` 与其它 Failed code 钉死契约用例（行为正确但无守护）。建议补 None/InProgress→failure 反向用例。

## T2 core/common

**优点（核实）**：`ext/Money.kt`/`ext/Number.kt`/`ext/String.kt` 覆盖优秀且反向充分——Money 的 HALF_UP 舍入边界精确钉死（`MoneyTest.kt:178-187` 19.995→2000/19.994→1999）、往返一致性（`:238-260`）、Long.MIN_VALUE 不崩溃、空串/非数字归零；Number 六方法 null/空/非法/有效四类全覆盖。

**[Medium] coverage | `util/LunarUtils.kt:199`（`getLunarTextWithFestival`）↔ none**（agent 报 High→校准 Medium）
- 核实（Read LunarUtils.kt:199-203 + grep 全仓无 LunarUtils 测试）：真实公历转农历算法（solarToLunar 位运算解码 + 闰月判定 + 节日优先级），零测试，算法表/位移写错会静默输出错误农历。
- 校准理由：纯展示（CalendarView 显示农历/节日），无金融/数据影响 → Medium。但算法复杂易错难察觉，建议补已知公历-农历对照测试（春节/中秋/闰月/母亲节）。

**[Medium] coverage | `tools/Time.kt:119`（时间解析/格式化方法群）↔ none**（agent 报 High→校准 Medium）
- 核实（grep 无 Time 测试）：toLongTime/toLocalDate/toMs/toDateString 等纯 JVM 时间转换零测试；agent 指出 `toMs` 用 ZoneOffset 而 `toLocalDate` 用 ZoneId（:124,128）+ `paresDate`/`parseDate` 拼写不一致（:77,108）易误用。
- 修复建议：固定时区下测往返一致 + 非法串 parseDate→null + parseDateLong 回退。

**[Medium] coverage | `ext/Any.kt:53`（`moneyFormat`）↔ none**：另一金额显示口径（与 Money.kt 并存），null/空/有效三分支 + 两位小数无守护。建议补 AnyExtTest。**注**：项目存在 ≥2 套金额格式化口径（Money.kt + Any.moneyFormat），后者无测试，潜在不一致风险。

**[Medium] reverse | `enums/MimeType.kt:56`（`parse`）↔ none**：真实解析逻辑（null/size!=2/未知 type 三反向分支 + 自定义 equals），非纯 data class，无测试。建议测 `parse("image/png")`/null/`"a/b/c"`/未知。

**[Medium] reverse | `tools/Regex.kt:30`（`isMatch`）↔ none**：密码/金额校验底层（null/blank 短路 + Pattern.matches），配合 `Patterns.PASSWORD_REGEX`/`PATTERN_SIGN_MONEY`，无测试。建议测 null/空白→false + 合法/非法密码与金额串。

**[Low] coverage**：`ext/Flow.kt:22 tryEmitNoRepeat`（去重语义可 runTest 测）、`ApplicationInfo.kt:71 setFlavor`（非法名 fallback Dev 无守护）——均纯 JVM 可测但无测试。

## T2 core/network

**优点（核实）**：`GitReleaseEntitySerializationTest` 反向充分（@SerialName 映射/未知字段忽略/全 null/空 assets）；`LoggerInterceptorTest` 用真实 MockWebServer 端到端 + 凭据脱敏强断言（Authorization/Cookie/Proxy-Authorization 三头验证「原始凭据不出现 + ██ 占位」，守护 DataSourceModule 脱敏）。

**[High] coverage | `util/OkHttpWebDAVHandler.kt:44-227` ↔ none**（核实属实，维持 High）
- 核实（grep 无 OkHttpWebDAVHandler 测试）：备份/恢复的 WebDAV 网络底座零测试——含 url.isBlank() 短路守卫、HTTP isSuccessful 映射、`list()` 的 PROPFIND 207 XML 解析（Jsoup 取 d:href、剔目录、仅留 .zip 构造 BackupModel）、`get()` !isSuccessful→null。
- 维持 High 理由：① 备份数据完整性（数据丢失风险，呼应 T1 BackupRecoveryManager 缺口）② callFactory 构造注入 + 同模块 LoggerInterceptorTest 已证 MockWebServer 纯 JVM 可跑 + Jsoup 纯 JVM → **完全可测却裸奔**。
- 修复建议：MockWebServer 注入，测 list() 喂 PROPFIND XML 断言只解析 .zip、get() 200→bytes/404→null、exists/put 2xx→true/5xx→false/空 url→false。

**[Medium] logic | `datasource/NetworkDataSource.kt:54-67` ↔ `NetworkDataSourceTest.kt:37-42`**（agent 报 High→校准 Medium）
- 核实（Read 两文件）：`NetworkDataSourceTest` 测的是测试内 `private fun filterRelease`（**复制版**，docstring 明说"复现 checkUpdate 筛选逻辑"），5 用例全调复制版**从不调真实 `checkUpdate()`**；真实方法的 useGitee 分支（gitee vs github）完全未触达。属确认的假阳性（复制版与源码并行，源码筛选改坏测试仍绿）。
- 校准理由：筛选逻辑仅 3 行 firstOrNull + 测试作者已文档化限制（完整 URL 致 MockWebServer 无法拦截）+ 复制版自身反向覆盖好 → Medium。
- 修复建议：注入 Fake Call.Factory 返回预置 JSON 让真实 checkUpdate 跑过 Retrofit 解码+筛选，useGitee true/false 各测。

**[Medium] reverse | `entity/GitContentsEntity.kt:26-32` ↔ none**：与已测 GitReleaseEntity 不同，4 字段全非空无默认值，ignoreUnknownKeys 但无 coerceInputValues 下缺字段会抛异常，无序列化测试。建议比照 GitReleaseEntitySerializationTest 补合法+缺必填字段抛异常用例。

**[Medium] coverage | `okhttp/LoggerInterceptor.kt:219-237` ↔ `LoggerInterceptorTest.kt:99-117`**：LEVEL_BODY 的 JSON 格式化 + >200 字符截断分支从未触达（现有 body 测试故意用 plain-text 规避 JVM 无 org.json）；chain.proceed 抛异常的 HTTP FAILED 日志+rethrow 也未覆盖。建议 Robolectric（模块已 isIncludeAndroidResources）或提供 org.json 测格式化/截断 + MockWebServer 触发 IOException 测 rethrow。

## T2 core/datastore

**优点（核实）**：`CombineProtoDataSourceTest` 对 `encryptWebDAVPassword`/`decryptWebDAVPassword` 的反向短路（空串、不含冒号旧明文向后兼容）**忠实直调真实静态方法**（`CombineProtoDataSource.kt:415/431` 短路分支），isEqualTo 强断言；测试头注释自觉划界 AndroidKeyStore round-trip 需真机不测，未用假桩冒充。

**[High] logic（假阳性）| `CombineProtoDataSource.kt:368-370`（`needRelated`）↔ `CombineProtoDataSourceTest.kt:38-60`**（agent 报 Critical→校准 High）
- 核实（Read 测试 + 源）**完全属实**：3 个 needRelated 测试**从不调真实 `suspend fun needRelated`**，而是测试体内内联复刻表达式；更甚 `CombineProtoDataSourceTest.kt:41` 写成 `FIXED_TYPE_ID_REFUND == FIXED_TYPE_ID_REFUND`（**x==x 恒真同义反复**）。即使把源码 `||` 改 `&&`/比错常量/返反值，3 测仍全绿——教科书级假阳性，比 CLAUDE.md 历史案例更恶劣（含字面 x==x）。
- 校准理由：维持「确认的假阳性」定性，但 needRelated 仅 2 行平凡比较 + 生产吸收边界实走 `TypeRepositoryImpl.needRelated`（CLAUDE.md 记载）+ 此 CombineProtoDataSource.needRelated 可能是次要路径 → 爆炸半径 High 而非 Critical。**但该测试必须删除/重写**（x==x 是硬伤）。
- 修复建议：构造 CombineProtoDataSource 实例（注入 Fake DataStore）runTest 实调 `needRelated(FIXED_TYPE_ID_REFUND/REIMBURSE/1L)`；或重构为顶层纯函数直调。禁止内联复刻 + x==x。

**[Medium] reverse | `serializer/AppSettingsSerializer.kt:36-40`（6 个 Serializer 同结构）↔ none**（agent 报 High→校准 Medium）
- 核实（grep 无 Serializer 测试）：6 个 Serializer（AppPreferences/AppSettings/GitInfos/RecordSettings/SearchHistory/TempKeys）readFrom 对损坏 proto 捕获 InvalidProtocolBufferException 抛 CorruptionException 的反向路径（DataStore 自愈契约入口）零测试；纯 JVM 可测（无 Android 依赖）。
- 校准理由：设置数据损坏（可恢复，非金融记录）+ 易测 → Medium。
- 修复建议：参数化测 6 个 Serializer：非法字节→assertThrows<CorruptionException>、空流→defaultValue、writeTo 往返相等。

### T2 小结
core/common 的金额/数字/字符串 ext 与 core/network 的序列化/拦截器脱敏覆盖优秀。**核心缺口**：①`OkHttpWebDAVHandler` 备份网络底座零覆盖（High，呼应 T1 备份恢复缺口）②`needRelated` 测试 x==x 假阳性（High，必须重写）③多个纯 JVM 可测逻辑（LunarUtils/Time/MimeType/Regex/Serializer 损坏路径）裸奔（Medium）。无 active 致命 bug，但假阳性测试 + 备份层裸奔是要点。

---

# Phase 3 — T3 广度扫描（Workflow 只读 fan-out + Controller 核验）

> **核验声明**：11 模块只读 fan-out（广度模式：只查覆盖缺口 + 明显反向，截图不深审渲染）。controller 抽样 hands-on 核验代表性 High + 连接 T1 的桩：`CalculatorUtils`/`DesignDetector` grep 确认无测试 ✓、`EditAssetViewModelTest` 中 `.save(`/`lastUpdatedAsset`=0 次 ✓、`FakeRecordRepository.batchImportRecords:407-409` 确为 `emptyList()` 空桩 ✓。agent 报告**无虚报**。

## T3 跨模块共性主题（最值得 action 的发现）

T3 的 ViewModel/UI 缺口高度集中在 4 个**系统性模式**，比单条 finding 更值得优先处理：

1. **「写入/成功路径」零覆盖，「事件处理/早退守卫」却覆盖好**：多个 ViewModel 的核心写入方法无测试触达——`EditAssetViewModel.save`（核实 0 次调用）、`EditBookViewModel.onSaveClick` 成功路径、`RecordImportViewModel.confirmImport` 成功路径、`feature/settings exportRecords`、`CalendarViewModel.uiState` 月度聚合、`AnalyticsViewModel.showSheet`。测试多停在初始态/dismiss/早退守卫。
2. **「名实不符」弱断言（假阳性风险）**：测试方法名声称 `then_type_created`/`data_removed`/`type_to_second`，但断言只检查 `dialogState==Dismiss` 或 `successCalled==true`，**从不断言真实状态变更**。集中在 feature/types（3 条）、feature/assets（2 条：`when_delete_asset_then_data_removed` 只断言 successCalled 不断言 getAssetById==null）。
3. **失败/反向分支因 Fake 永不抛异常而零覆盖**：删除/保存失败的错误回调（bookmark/ResultModel.failure）路径在 ConfirmDeleteRecordDialog/DeleteTagDialog/EditTagDialog/AssetInfo/MyBooks 等普遍未测——根因是 Fake（FakeRecordRepository/FakeTagRepository 等）的 delete/update 是 no-op 永不抛。**统一修法：给 Fake 加可注入异常钩子**（参照已有 `recalcThrowable`）。
4. **设计/UI 纯逻辑仅截图覆盖、行为零单测**：`CalculatorUtils`（计算器引擎！）、`runCatchWithProgress`、`DefaultProgressDialogController`、`formatLargeValue`、`Cipher` hex 工具、`PieChart` 命中测试、`TextFieldState` 错误态机、`popBackStackSafety`——均纯 JVM 可测却只有截图测试（截图不验逻辑/计算正确性）。

## T3 各模块 finding（紧凑）

### feature/records（强覆盖，少量缺口）
优点：EditRecordViewModelTest（trySave 三校验 + 口径强制规则）、LauncherContentViewModelTest（首屏 gate 区分力 + totals 三口径）覆盖极强。
- **[High] coverage** `CalendarViewModel.kt:76-104`：uiState 月度 totalIncome/Expenditure/balance + selectedDay 过滤 + schemas 映射零覆盖（仅测事件处理器，未订阅 uiState）。与 LauncherContent 同构但后者全测。
- **[High] coverage** `AnalyticsViewModel.kt:153-168`：`showSheet`（下钻二级饼图，三分支）整方法零覆盖。
- **[Medium] reverse** `ConfirmDeleteRecordDialogViewModel.kt:46-52`：删除失败回调（ResultModel.failure）零覆盖（FakeRecordRepository.deleteRecord 永不抛）。
- **[Low] reverse** `TypedAnalyticsViewModel.kt:129-134`：日期区间 `~` 分支 + tagId==typeId→Loading 未覆盖。

### feature/settings（ViewModel 充分，更新/导出链路缺口）
优点：6 ViewModel 全有测试；BackupAndRecoveryDomainValidationTest 拒明文 http 安全约束反向充分。
- **[High] coverage** `BackupAndRecoveryViewModel.kt:287-309`：`exportRecords` 导出状态机（Exporting→Done/catch→Error）零 ViewModel 测试（截图只静态传 ExportState.Idle）。
- **[High] coverage** `MainAppViewModel.kt:414-442`：`needUpdate` 版本号数值化比较（防 '1.10'<'1.9' 字典序陷阱）零覆盖——FakeSettingRepository.getLatestUpdateInfo 硬编码 versionName="" 使比较算法从未执行。建议抽纯函数。
- **[Medium] coverage** `MainAppViewModel.kt:338-376`：confirmUpdate/confirmDownload happy path 未覆盖（Fake 空版本致结构性不可达）。
- **[Medium] reverse** `BackupAndRecoveryViewModel.kt:142-148`：saveWebDAV 非法地址拒绝（明文 http→不落库 + Failed）集成层未覆盖。
- **[Low] reverse** `MainAppViewModel.kt:358-363`：dismissUpdateDialog ignore=true（持久化忽略版本）分支未覆盖。

### feature/assets（ViewModel 齐全，save + 弱断言）
优点：MyAsset 资产/负债分类反向好；InvisibleAsset/EditRecordSelectAsset 反向到位。
- **[High] coverage** `EditAssetViewModel.kt:187`：`save()`（保存/新建资产核心写入）**零测试触达**（核实 0 次调用）——doSaving 重入守卫/无变化跳过/落库/onSuccess/catch 恢复全未测。
- **[Medium] logic** `AssetInfoViewModel.kt:129`：`when_delete_asset_then_data_removed` 只断言 successCalled，不断言 getAssetById==null（名实不符弱断言）。
- **[Medium] logic** `EditAssetViewModel.kt:127`：updateClassification 非银行卡只断言 sheet dismiss，不断言资产名/分类写入 uiState。
- **[Medium] reverse** `AssetInfoViewModel.kt:144`：deleteAsset 失败路径（ASSET_DELETE_FAILED bookmark）零覆盖（Fake no-op）。
- **[Medium] reverse** `MyAssetViewModel.kt:59`：资产+负债**并存**混合用例缺失（现均单一类型，netAsset 相减逻辑未实质验证）。

### feature/types（覆盖充分，3 条名实不符弱断言）
优点：MyCategoriesViewModel 21 测覆盖增删改 + 受保护反向；FakeTypeRepository 忠实复刻 DAO。
- **[Medium] logic** `MyCategoriesViewModel.kt:233-264`：`when_save_new_record_type_then_type_created` 只断言 Dismiss，不断言新类型创建（typeLevel/typeCategory/sort 派生未验证）。
- **[Medium] logic** `MyCategoriesViewModel.kt:135-140`：`when_change_type_to_second` 只断言 Dismiss，不断言 typeLevel==SECOND/parentId==2L。
- **[Medium] reverse** `MyCategoriesViewModel.kt:99-107`：onMoveFirstType 越界保护分支零覆盖。
- **[Medium] coverage** `EditRecordTypeListViewModel.kt:58-80`：INCOME/TRANSFER typeListData 填充路径未覆盖（仅 EXPENDITURE）。
- **[Low] coverage** `MyCategoriesViewModel.kt:222-231`：requestEditType 新增路径（type/parentType 均 null）未覆盖。

### feature/tags（核心选择逻辑缺口 + 潜在 bug）
优点：MyTags/EditTagDialog 重名校验正反向；FakeTagRepository 忠实复刻。
- **[High] reverse** `EditRecordSelectTagBottomSheetViewModel.kt:77-96`：核心标签选择逻辑（updateSelectedTags firstSet guard/toggle/隐藏标签过滤）零覆盖（仅测 dialog/初始态）。
- **[Medium] reverse** `DeleteTagDialogViewModel.kt:65-69` / `EditTagDialogViewModel.kt:71-75`：delete/save 失败分支零覆盖（Fake 永不抛）。
- **[Medium] reverse（潜在 bug）** `EditTagDialogViewModel.kt:59`：编辑已有标签不改名时 `countTagByName(name)>0` 含自身→误报 NAME_EXIST 拦截保存。现有测试改名规避了该路径，既是覆盖缺口也可能掩盖去重逻辑 bug（应 countByNameExcludingId）。**建议与开发确认预期语义**。

### feature/books（写入成功路径缺口）
优点：EditBook NAME_DUPLICATED 反向 + updateBookId 状态流转；FakeBooksRepository 忠实复刻 isDuplicated。
- **[Medium] reverse** `MyBooksViewModel.kt:70`：confirmDeleteBook 失败分支（返 false→dialog 保持 Shown）零覆盖。
- **[Medium] coverage** `EditBookViewModel.kt:111`：onSaveClick 成功路径（updateBook+onSuccess）零覆盖。
- **[Medium] reverse** `EditBookViewModel.kt:103`：背景图 BG_IMG_TYPE_ERROR/BG_IMG_SAVE_FAILED 两失败分支零覆盖。
- **[Low] logic** `FakeBooksRepository.kt:66`：deleteBook 删当前账本未 re-select（与真实 BooksRepositoryImpl re-select list.first() 不一致，潜在保真隐患）。

### feature/record-import（导入链路缺口，连接 T1）
优点：checkDuplicate 三路径（EXACT/POSSIBLE/NONE）断言强 + 元/分单位错配回归 + FakeRecordRepository wechat 桩忠实（方括号定界）。
- **[High] coverage** `RecordImportViewModel.kt:265-316`：`confirmImport` 成功路径（Ready→Importing→Done）零覆盖——remark 拼接 `[微信单号]`、toCent、finalAmount、skipped 计算全未触达。**T1 batchImport 缺口的 UI 层同源**。
- **[High] coverage** `RecordImportViewModel.kt:142-262`：整个 Ready 状态及交互（buildPreviewItems/selectBook/updatePaymentMapping/toggle）零覆盖——**缺 .xlsx fixture**（WechatBillParser 可测但 test/resources 无账单）。
- **[Medium] reverse** `RecordImportViewModel.kt:176-187`：单号非空但库无匹配→回落模糊匹配分支未覆盖。
- **[Medium] logic** `FakeRecordRepository.kt:407-409`：`batchImportRecords` 空桩（`emptyList()`，核实属实）——补 Done 路径测试时会致 Done.imported 恒 0 假阳性。**需先改忠实桩**（存入 records + 返递增 id）。

### core/ui（进度弹窗逻辑零覆盖）
优点：3 截图测试齐全（DateSelectionPopup/SelectDateDialog/TypeIcon）。
- **[High] coverage** `DialogState.kt:110-137`：`runCatchWithProgress`（minInterval 补偿延迟 + timeout + Result 包装）纯 suspend 逻辑零单测（UX 关键，写错→弹窗闪烁/永不超时）。
- **[High] coverage** `DialogState.kt:83-104`：`DefaultProgressDialogController`（show/dismiss 状态机）纯 JVM 零单测。
- **[Medium] coverage** `Navigation.kt:42-48`：popBackStackSafety（防首页弹空）两分支零覆盖。
- **[Medium] coverage** `WindowAdaptiveInfo.kt:27-40`：bookImageRatio 三档 when 映射零覆盖。
- **[Low]** Color.colorInt 舍入 + DateSelectionPopup 区间约束（内联 @Composable，建议抽纯函数）。

### core/design（计算器引擎零覆盖 — 注意）
优点：theme token（Motion/Shape/Haptic）纯 JVM 单测；LineChart 截图含 empty/负值反向。
- **[High] coverage** `util/CalculatorUtils.kt:30`：**计算器引擎**（表达式解析 + 运算符优先级 + BigDecimal 精度 + 括号配对态机）**零单测**（核实无测试）——截图只用静态 defaultText 从不点按钮。算错金额输入静默无网。**建议补 CalculatorUtilsTest（纯 junit，internal 同模块可测）**。
- **[Medium] coverage** `LineChart.kt:343 formatLargeValue`（M/K 后缀阈值边界）纯函数零断言。
- **[Medium] coverage** `Cipher.kt:159` toHexString/hexToBytes/shaEncode（纯 JVM，往返一致 + WebDAV 兼容契约）零覆盖。
- **[Medium] coverage** `TextFieldState.kt:36`：错误展示态机（被 9 个源文件使用，「未聚焦不显示错误」契约）零行为单测。
- **[Medium] reverse** `PieChart.kt:150`：命中测试几何（atan2 角度规范化/孔洞返 -1）内联 Composable 未抽函数、零测试（建议抽 hitTestSlice 纯函数）。
- **[Low] reverse** `CalendarView.kt:186`：月历网格 weekStart 偏移/跨月 off-by-one（内联，建议抽 buildMonthGrid）。

### app（基本充分）
优点：MainViewModel/ActivityUiState 状态流转 + dark/dynamic 分支覆盖好；FakeSettingRepository 忠实。
- **[Low] logic** `ActivityUiState.kt:72`：shouldUseDarkTheme 的 setDefaultNightMode 副作用未纳入断言（只断布尔）。
- **[Low] coverage** `ExampleUnitTest.kt:27-32`：模板空占位测试（2+2=4），建议删除清理。

### lint（load-bearing 门禁零覆盖 — 注意）
优点：TestMethodNameDetectorTest 对 detectPrefix/detectFormat 覆盖强（行号 + quickfix diff）。
- **[High] coverage** `design/DesignDetector.kt:35-117`：**DesignDetector 零测试**（核实无 DesignDetectorTest）——这是 CLAUDE.md 声明的硬门禁（app/feature/core:ui 误用 Material3 触发 `Design` ERROR 中止构建），最 load-bearing 的自定义规则，两扫描分支（METHOD_NAMES 18 映射 + RECEIVER_NAMES Icons）失效则静默停止拦截。**建议补 DesignDetectorTest（复用 TestLintTask 范式）**。
- **[Medium] reverse** `TestMethodNameDetector.kt:83`：detectFormat 的 `isAndroidTest()` gate 无反向覆盖（无非-androidTest 路径用例断言 0 告警，gate 破坏会让 JVM 单测反引号命名误报爆炸）。
- **[Low] reverse** detectFormat 正则 `{1,2}` 上界边界差一断言。

### T3 小结
ViewModel/UI 层覆盖整体不错（事件处理 + 状态流转多有覆盖），但**写入成功路径 + 失败反向分支 + 设计层纯逻辑**三类系统性裸奔。**最该优先**：①`CalculatorUtils` 计算器引擎零测试（High，影响金额输入正确性）②`DesignDetector` load-bearing 门禁零测试（High，门禁静默失效风险）③`confirmImport`/Ready + batchImportRecords 空桩（High/Medium，连接 T1 导入缺口）。无 Critical（UI 层无 active 致命 bug）。

---

# Phase 4 — 综合报告（执行摘要）

## 1. 总体结论

- **测试套件总量 157 文件**，覆盖 21 模块。整体质量**中上**：金额核心链路（三口径 + 簇重算 + 删除/余额）覆盖**优秀且判别性强**，无 Critical、无 active 致命 bug。
- **finding 总计 70 条**：0 Critical / 15 High / 39 Medium / 16 Low。
- **两个系统性结构问题比单条 finding 更值得决策**（见 §4）。
- 全部 finding 带 `file:line` 证据；fan-out agent 强断言均经 controller hands-on 核验，无虚报。

## 2. 严重度总表

| 严重度 | 数量 | 含义 |
|---|---|---|
| Critical | 0 | 无假阳性致 active bug 当下漏网 |
| High | 15 | 关键路径零覆盖 / 假阳性 / load-bearing 逻辑裸奔 |
| Medium | 39 | 反向边界缺失 / 弱断言 / 纯 JVM 逻辑裸奔 |
| Low | 16 | 边角缺口 / locality / 模板清理 |

## 3. 覆盖矩阵（按模块）

| 模块 | 覆盖评级 | 关键缺口 |
|---|---|---|
| core/domain | ★★★★★ | 无（满广度 + 判别性金额口径 + 反向） |
| core/model | ★★★★☆ | analyticsPieNetAmount model 级 locality（已由 usecase 判别性覆盖） |
| core/data | ★★★★☆ | batchImport 零覆盖(H)；insert 余额仅 androidTest |
| core/database | ★★★★☆* | *覆盖全但**全 androidTest 设备门控**（结构问题） |
| core/common | ★★★☆☆ | Money/Number/String 优秀；LunarUtils/Time/Regex/MimeType 裸奔 |
| core/network | ★★★☆☆ | 序列化/脱敏好；OkHttpWebDAVHandler 零覆盖(H)；filterRelease 测复制版 |
| core/datastore | ★★☆☆☆ | needRelated x==x 假阳性(H)；6 Serializer 损坏路径裸奔 |
| sync/work | ★★★☆☆ | AutoBackup 典范；SyncWorker 重试逻辑裸奔 |
| feature/records | ★★★★☆ | Calendar/Analytics.showSheet 零覆盖(H) |
| feature/settings | ★★★★☆ | exportRecords/needUpdate 零覆盖(H) |
| feature/assets | ★★★☆☆ | save() 零覆盖(H)；2 条名实不符弱断言 |
| feature/types | ★★★★☆ | 3 条名实不符弱断言（只断 Dismiss） |
| feature/tags | ★★★☆☆ | 选择逻辑零覆盖(H)；潜在去重 bug |
| feature/books | ★★★☆☆ | onSaveClick 成功路径零覆盖 |
| feature/record-import | ★★☆☆☆ | confirmImport/Ready 零覆盖(H)；batchImport 空桩 |
| core/ui | ★★★☆☆ | 进度弹窗逻辑零覆盖(H×2) |
| core/design | ★★★☆☆ | **CalculatorUtils 计算器引擎零覆盖(H)** |
| app | ★★★★☆ | 基本充分；ExampleUnitTest 模板噪声 |
| lint | ★★★☆☆ | **DesignDetector load-bearing 门禁零覆盖(H)** |

## 4. 两个系统性结构问题（最值得决策）

### 结构问题 A：大量数据层覆盖仅在设备门控 androidTest
core/database 全部 103 个测试（全部 DAO SQL + 12 个 Migration）+ TransactionDao 的 insert/transfer 余额 + verifyAssetBalance 均为 androidTest，**本机无设备不跑、CI 通常也不跑 instrumented**。这批覆盖充分、质量高的数据层测试在日常 JVM/CI 路径**零回归保护**——「覆盖率」表象远高于实际护栏。
- 建议（择一/组合）：① CI 增 emulator/instrumented job；② 关键 DAO `@Query` 语义用 Robolectric + in-memory Room 迁入 JVM；③ 至少 CI 文档显式声明数据层回归依赖 instrumented，合并前须真机/emulator 跑过。

### 结构问题 B：假阳性 / 弱断言测试（测试存在但不验证真实行为）
本次审计抓出 ≥6 处「测试绿但不验证 SUT」：
- `CombineProtoDataSourceTest` needRelated **x==x 恒真**（最恶劣）
- `NetworkDataSourceTest` 测 `filterRelease` **复制版**而非真实 checkUpdate
- feature/types 3 条 + feature/assets 2 条**名实不符**（只断 dialogState==Dismiss / successCalled，不断真实状态变更）
- 多处失败反向分支因 **Fake 永不抛异常**而零覆盖（需给 Fake 加可注入异常钩子）
- `FakeRecordRepository.batchImportRecords` **emptyList 空桩**（补 Done 测试时会假绿）
- 建议：建立「测试必须调用真实 SUT + 断言关键输出」的评审 checklist；统一给 core/testing 的 Fake 增加异常注入能力。

## 5. 15 个 High 的建议补齐顺序（仅建议，不实施）

> 按「风险 × 可测性」排序，分两档。**用户已确认先报告后分批补**，以下为优先级参考。

**第一档（数据完整性 / load-bearing，强烈建议优先）**
1. `core/datastore` **needRelated x==x 假阳性**（删除/重写该测试，成本极低）
2. `core/design` **CalculatorUtils 计算器引擎**（纯 junit 可测，影响金额输入正确性）
3. `lint` **DesignDetector**（load-bearing 设计门禁，TestLintTask 范式可测）
4. `core/data` **batchImportRecordsTransaction**（微信导入余额聚合，Fake 继承真实事务可 JVM 测）
5. `core/network` **OkHttpWebDAVHandler**（备份网络底座，MockWebServer 可测，呼应备份恢复缺口）

**第二档（ViewModel 写入/核心路径覆盖）**
6. `feature/record-import` confirmImport 成功路径 + Ready 状态（先补 batchImportRecords 忠实桩 + .xlsx fixture）
7. `feature/assets` EditAssetViewModel.save()
8. `feature/settings` exportRecords 导出状态机 + needUpdate 版本比较（建议抽纯函数）
9. `feature/records` CalendarViewModel uiState + AnalyticsViewModel.showSheet
10. `feature/tags` EditRecordSelectTagBottomSheetViewModel 选择逻辑
11. `core/ui` runCatchWithProgress + DefaultProgressDialogController（纯 JVM 可测）

## 6. 边界与免责

- **androidTest 未实跑**：core/database 覆盖结论基于测试源码静态分析（本机无设备）。
- 截图测试（Roborazzi）按广度模式只确认存在性，未深审渲染像素正确性。
- 本审计**未改任何代码**；以上修复建议待用户分批决策。

---

*报告完成。审计方式：Phase 1 controller 亲审（T1 金额核心）+ Phase 2/3 Workflow 只读 fan-out + controller 逐条核验（T2/T3）+ Phase 4 综合。*
