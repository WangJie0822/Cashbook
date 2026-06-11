# 测试套件全量评估 审计报告

> 日期：2026-06-11 ｜ 分支：main ｜ 性质：纯审计（未改任何代码）
> 方案：[审计设计](2026-06-11-test-suite-audit-design.md) ｜ 计划：[执行计划](../plans/2026-06-11-test-suite-audit.md)
> 审计方式：T1 金额核心 controller 亲审 ｜ T2/T3 Workflow 只读 fan-out + controller 核验
> **报告生成中**——各 Phase 完成后增量追加 section，末尾 Phase 4 出总表/矩阵/统计。

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
