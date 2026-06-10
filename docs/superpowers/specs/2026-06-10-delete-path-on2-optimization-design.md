# 删除路径去 O(N²) + 死代码清理 设计文档（F-1 + L1）v2

> 日期：2026-06-10　分支：main　模块：`core/database`（TransactionDao / RecordDao）+ 测试
> 来源：Cashbook 待办 backlog —— F-1（删账本/删资产 O(N²) 性能）+ L1（`RecordDao.queryRelatedRecord` 死代码）
> 用户决策：①范围＝两个删除函数都改 + 一并审计删除路径潜在正确性（不保证行为逐字不变）；②算法＝方案 A「跳过无用功」。
> v2：吸收节点1 team-review 四维 finding（High 3 / Medium 7 / Low 6，无 Critical，无算法硬 bug）。修订点见末节「节点1 team-review 吸收记录」。

## 1. 问题与现状（实证）

### 1.1 F-1：删除路径 O(N²)

`TransactionDao`（`core/database/src/main/kotlin/.../dao/TransactionDao.kt`）：

- `deleteBookTransaction(bookId)`（741-753）：`queryRecordListByBookId` 后**逐条** `deleteRecordTransaction(record)`。调用方 `BooksRepositoryImpl.kt:71`（`deleteBook`）。
- `deleteAssetRelatedData(assetId)`（832-839）：`queryRecordsByAssetId` 后**逐条** `deleteRecordTransaction(record)`。调用方 `RecordRepositoryImpl.kt:472`（`deleteRecordsWithAsset`），其生产入口 `feature/assets/.../AssetInfoViewModel.kt:140`。
- `deleteRecordTransaction(record)`（584-665）每条做：
  1. 回退源资产 + 转账对方资产余额（591-631，含信用卡/收支/转账三向符号）；
  2. 删标签/图片关联（634-637）；
  3. **按关联整簇重算 finalAmount**（640-655）：EXPENDITURE 走 `queryRelatedByRelatedRecordId` → 每个吸收者 `recalculateAbsorberFinalAmount`；INCOME 走 `queryRelatedByRecordId` → 每个被吸收支出 `recalculateFinalAmountForCluster`；
  4. 清关联（658）+ 删记录（661，失败抛 `DataTransactionException`）。

**O(N²) 真凶**（feasibility ④ 实证通过）：第 3 步每条都做整簇 BFS 重算（`recalculateFinalAmountForCluster` 内 BFS + 批量查询，TransactionDao.kt:174-242），删整账本/整资产时**同一簇被重算 N 次**，且**重算的对象本身就要被删**（纯浪费）。第 1/2/4 步是 O(N) 线性、廉价（余额回退每条 ≤2×queryAssetById+2×updateAsset=O(1)）。

note 估算：删大报销簇账本 ~1-2s，低频操作。

### 1.2 L1：死生产代码

- `RecordDao.queryRelatedRecord()`（RecordDao.kt:305，`SELECT * FROM db_record_with_related`）与 `TransactionDao.queryAllRelatedRecords()`（110-111）SQL **逐字相同**。
- 无生产调用方（旧 `migrateAfter9To10` 唯一消费者已删）。仅被 androidTest `RecordDaoTest:793,952` + `FakeRecordDao:236` 当验证 helper 用。

## 2. 删除路径正确性审计（用户要求一并查）

实证结论：

1. **资产 book 维度**：`AssetRepositoryImpl:115-130` 用 `getVisibleAssetsByBookId(currentBookId)`，新建资产绑定 `currentBookId`。转账源/目标都从当前账本资产列表选 → **正常流程转账不跨账本**。
2. **吸收关系 book 维度**：可选报销关联记录由 `RecordDao.queryReimburseByBooksIdAfterDate(booksId, dateTime)`（116-119）按 `books_id=:booksId` 过滤 → **吸收关系限当前账本**。
3. **当前逐条路径对存活引用是正确的，只是浪费**：删支出时 `recalculateAbsorberFinalAmount(absorber, excludeAbsorbedId)` / 删收入时 `recalculateFinalAmountForCluster(seed, exclude={income})` 都正确更新存活簇成员；跨账本转账对方资产（若存在）也会被逐条回退。删除对象自身的 finalAmount/余额回退是浪费（随后即删）。
4. **结论**：正常数据下删账本/删资产时，簇与转账两端**随账本/资产同删** → 既无存活簇成员、也无存活对方资产 → finalAmount 重算与对方余额回退**理论上 0 次**。存活引用仅可能由备份恢复/导入/历史脏数据产生。

**审计结论项（team-review 复查后定稿）**：

- **AUDIT-1（既有缺口，非本次引入，不在本次修复范围）**：删账本 B 时，若其它账本 A 的转账记录的对方资产在 B 内（跨账本转账，正常流程不产生），该记录不在 `queryRecordListByBookId(B)` → 转账记录存活但对方资产被 `deleteAssetsByBookId(B)` 删 → 悬挂引用 + A 账本余额未回退。本设计不主动制造、也不在删账本侧主动修复跨账本悬挂（属另一独立缺陷域）。
- **AUDIT-2（本设计须守住的契约，措辞已订正）**：`deleteAssetRelatedData` **只删记录、不删资产行**，**被操作资产自身行存活 + 余额回退到记录前**（androidTest `TransactionDaoTest.kt:826-830` 断言 `deleteAssetRelatedData(1L)` 后 `queryAsset(1L)!!.balance==100000L` 且行仍在）。注意：此处「资产」指**被删操作的目标资产本身**，不是转账对方 intoAsset。删 asset 行由上层 `AssetInfoViewModel:138-141` 另行 `assetRepository.deleteById` 完成。
- **AUDIT-2b（转账对方资产，并入 §5.2 守卫）**：删资产删转账记录时，**存活的源/目标对方资产余额须正确回退**——此场景现有测试零覆盖（见 §5.2#3）。
- **AUDIT-3（测试迁移根因，已订正，见 §5.1/§5.3）**：去掉 Fake 简化覆盖后，受影响既有测试的真实失败原因是 `resolveType` 取不到 Type 抛 `DataTransactionException`（**非**「余额回退不一致」），须先补 TypeTable。

## 3. 方案对比（输出全部方案）

性能瓶颈拆解：①finalAmount 重算 = O(N²) 真凶；②逐条余额回退 = O(N) 线性廉价、非瓶颈。

### 方案 A —「跳过无用功，保留逐条余额」【已选 · 推荐】

- 删除前捕获「与待删集 D 关联的**存活**记录 id」；逐条余额回退 + 清关联 + 删记录，**跳过逐条 finalAmount 重算**；删完后对每个**存活簇只重算一次**（去重）。
- 优点：精准消灭 O(N²)；余额符号逻辑**零改动**（风险最低）；常见「全簇在账本内」场景 finalAmount 工作量降到 0。
- 缺点：逐条删/清关联仍在（线性，可接受）。

### 方案 A+C —「A 叠加批量删除」
- A 基础上用现成批量 SQL 替逐条删关联/标签/图片/记录。优点：进一步砍 churn；缺点：改动略大、收益中等。

### 方案 B —「全聚合批量」
- 按存活资产聚合余额回退 + 批量 SQL 删 + 存活簇重算。优点：DB 写最少；缺点：**重新推导余额符号数学**（删除路径最易错处），正确性风险最高，相对 A 增量收益有限。

**推荐 A**（feasibility ④ 实证：O(N²) 唯一来源是重复整簇重算，A 精准消除而不碰余额符号逻辑；prior review `.full-review/01-quality-architecture.md:25` 已提「同簇重复重算可用已处理簇集合去重」，与本设计 visited-set 方向一致）。

## 4. 方案 A 详细设计

### 4.1 抽取 `deleteRecordCore`

把现 `deleteRecordTransaction(record)` 的核心（余额回退 591-631 + 删标签/图片 634-637 + 清关联 658 + 删记录 661-664）抽成私有 `deleteRecordCore(record)`，**不含** finalAmount 重算（原 640-655）。

> **L3 约束**：`deleteRecordCore` 必须保留删记录失败抛异常（现 661-664 `if (result <= 0) throw DataTransactionException`），抽取后该检查仍在 core 内、仍在 `@Transaction` 内。

feasibility ②a 已核验抽取边界干净：recalc 块（640-655）唯一副作用是 `updateRecordFinalAmountById`，无 balance/relation/record 变更，可安全后移。

`deleteRecordTransaction(record)`（单删，UI 路径）重写为：
```
val survivors = (queryRelatedByRecordId(id) 的 relatedRecordId) + (queryRelatedByRelatedRecordId(id) 的 recordId)，排除 id 自身
deleteRecordCore(record)
survivors 去重后逐簇重算（用 §4.3 单次 BFS 入口）
```

**等价性论证**：现状「recalc(exclude=id) → 清关联 → 删记录」，终态 = 存活簇按裁剪后净自付。重构「捕获 survivors → deleteRecordCore(清关联+删) → recalc(survivors)」，recalc 时关联已清、记录已删，BFS 见裁剪簇 → 终态逐字段一致。
- **多吸收者场景（L5 补强）**：被吸收支出同时被多个 income 吸收时，删一个吸收者后另一存活吸收者物理仍在、仍被 BFS 纳入簇 → exclude 版与删后版 BFS 同形 → 仍等价。
- **脏数据前提（M4/I-4 补强）**：上述等价性以「二部图不变式成立」（EXPENDITURE 仅作 related_record_id、INCOME 仅作 record_id）为前提。现状 `deleteRecordTransaction`（640-655）按 category 单分支取关联；本设计 survivors 改为**两方向 union**——正常二部图下 union = 单分支等价，对脏数据（一条记录两方向都有关联）是**有意放宽**（更鲁棒，多重算一个方向的簇，结果仍正确）。

### 4.2 批量删除（两函数同模式）

```
deleteBookTransaction(bookId) / deleteAssetRelatedData(assetId):
  val records = 待删集 D（queryRecordListByBookId / queryRecordsByAssetId）
  val deletedIds = D 的 id 集
  val affectedSurvivors = LinkedHashSet<Long>()
  for (record in records):
      // 删除会清关联，先捕获两个方向上不在 D 的对端 id
      affectedSurvivors += queryRelatedByRecordId(id).map{relatedRecordId}.filter{!in deletedIds}
      affectedSurvivors += queryRelatedByRelatedRecordId(id).map{recordId}.filter{!in deletedIds}
      deleteRecordCore(record)          // 余额回退 + 清关联 + 删记录, 无重算
  // deleteBook 收尾: deleteTagsByBookId + deleteAssetsByBookId + deleteBookById
  // deleteAsset 收尾: 无(不删资产行, 守 AUDIT-2 契约)
  // 存活簇逐簇重算一次(单次 BFS, 见 §4.3)
  val visited = HashSet<Long>()
  for (sid in affectedSurvivors):
      if (sid in visited) continue
      val (clusterIds, outEdges) = discoverClusterIds(sid)
      visited += clusterIds
      recalculateFinalAmountFromCluster(clusterIds, outEdges)
```

> **L6 硬顺序约束**：批量收尾重算**必须严格在所有 `deleteRecordCore` 完成之后**（全部待删关联已清、记录已删）。**禁止并入删除循环**（边删边算），否则 BFS 会脏读尚未删除的记录、把已删记录纳入簇 → finalAmount 算错。

### 4.3 `discoverClusterIds` / `recalculateFinalAmountFromCluster` 提取（H2/H3 修订）

把现 `recalculateFinalAmountForCluster`（174-242）拆为两个内部步骤，供单删/批量/内部三处复用：

- `discoverClusterIds(seed, exclude): ClusterDiscovery` —— 抽 step1 BFS（182-200），**同时返回 `clusterIds` 与 `outEdges` 缓存**（富类型，如 data class `ClusterDiscovery(clusterIds: Set<Long>, outEdges: Map<Long, List<Long>>)`）。**关键（H2/F-1/I-2）**：BFS 期间构建的 `outEdges`（183/192）被 step3（219-225）消费以找吸收者+被吸收列表；若只返 `Set<Long>` 会丢缓存 → 内部 recalc 须每节点重 `queryRelatedByRecordId` → 重新引入 commit `7114045e` 刚消除的 N+1。故必须随簇带出 `outEdges`。
- `recalculateFinalAmountFromCluster(clusterIds, outEdges)` —— step2/3/4（205-241），直接消费已发现的簇 + 缓存，**不再自己 BFS**。
- `recalculateFinalAmountForCluster(seed, exclude)`（对外签名不变）= `discoverClusterIds(seed, exclude)` → `recalculateFinalAmountFromCluster(...)`。其调用方 `insertRecordTransaction(490)`、`recalculateAbsorberFinalAmount(304)`、单删（§4.1）行为不变。

**H3（2x BFS）一并消除**：批量收尾用 `discoverClusterIds(sid)` 拿到簇 + outEdges 后**直接** `recalculateFinalAmountFromCluster`，每存活簇只 BFS 一次（不再「discover 标 visited + recalc 内部再 BFS」两遍）。

### 4.4 原子性与事务边界（M2/L3 补强）

- 两 DAO 函数保持 `@Transaction`，异常整体回滚。**L3**：批量循环中任一 `deleteRecordCore` 抛 `DataTransactionException` → 外层 `@Transaction` 整体回滚（余额/记录/关联均不变）。
- **M2 事务边界声明（防回归误判，本次不修）**：asset 删除在生产是**跨 4 次独立 repository 调用**（`AssetInfoViewModel.kt:138-141`：deleteRelatedWithAsset → deleteRecordRelatedWithAsset → deleteRecordsWithAsset → `assetRepository.deleteById`），各自独立 `withContext`、**无外层事务**。仅 `deleteAssetRelatedData`（TransactionDao.kt:832 `@Transaction`）内部原子（记录删除+余额回退）。即「记录回退+删」与「删 asset 行」分属两个事务；若 `deleteById` 失败，记录已删但 asset 残留。此为既有缺口、非本设计引入，仅文档化以防后续误判整链路原子。

## 5. 测试策略

### 5.1 JVM 测真实算法（核心）

- **删 `FakeTransactionDao` 对两函数的简化覆盖**（FakeTransactionDao.kt:244-262）→ 继承真实默认方法。Fake 已忠实建模余额（`updateAsset`/`queryAssetById` 对 `assets` 列表，125-134）、关联、finalAmount、类型（feasibility ②c 已核验所有 base 依赖齐全：queryRecordListByBookId/queryRecordsByAssetId/deleteTagsByBookId(no-op)/deleteAssetsByBookId/deleteBookById/queryTypeById/updateAsset/queryAssetById/clearRelatedRecordById/deleteRecord）。
- **前置硬约束（AUDIT-3 / H1 / F-2 / R-1 / I-1）**：去覆盖后真实 `deleteRecordTransaction` 首行 `resolveType(typeId)`（586）取不到 Type 即抛 `DataTransactionException`（587）。受影响既有测试**均未注册 TypeTable**，去覆盖瞬间会**直接崩溃**（非「调断言」）。故**必须先给受影响测试补 `setupTypesForAbsorption()`/注册对应 TypeTable**（要验余额回退的再注册 AssetTable），再删覆盖。见 §5.3 清单。
- `deleteBookCalled` 经真实 `deleteBookById`（Fake:204-207 置位）仍置位（前提：先修好 type 不抛异常，否则事务回滚标志状态不可靠）。

### 5.2 新增 JVM 区分力测试（`TransactionDaoLogicTest`）

1. 删账本/资产后**存活其他账本记录 finalAmount 不变**；
2. **重算次数守卫（L1/R-4/F-6 订正指标）**：`updateRecordFinalAmountById` 计数被「仅写变化项」（238）短路，不能跨场景区分新旧实现（旧路径若 finalAmount 未变也 0 写）。**改用 Fake 可观测的 `queryRecordByIds` 调用次数作区分力指标**——`recalculateFinalAmountFromCluster` 每簇调一次 `queryRecordByIds`（205），旧逐条路径删 N 条 = N 次、新路径 = 0~存活簇数。在 `FakeTransactionDao` 加 `queryRecordByIdsCallCount` 计数字段（当前 Fake 无任何 int 计数器）。断言「全簇在账本内删光」场景该计数为 0。
3. **删资产转账对方资产余额回退（AUDIT-2b / M1 / R-2）**：建 assets + types，构造「删资产 A，A 是某转账 intoAsset、源资产 B 存活」场景，断言 B 余额正确回退（与 deleteBook 守卫对称）。**注**：现有 `when_deleteAssetRelatedData_then_all_related_records_deleted`（420-432）`dao.assets` 为空 → 真实路径 `queryAssetById` 返 null → 余额回退 `?.let` 跳过 = 假阳性，须补带 assets 的新用例。
4. 删资产后**目标资产存活 + 余额回退**（守 AUDIT-2）。
5. **存活簇成员正向断言（L2，无 FK 兜底下的回归网）**：schema 零外键（全靠应用层维护一致性），须断言「删后存活簇成员 finalAmount 被裁剪到正确值」（跨账本吸收场景）。
6. **中途失败回滚（L3）**：批量删中途模拟 `deleteRecord` 失败，断言全部回滚（余额/记录/关联均未变）。

### 5.3 既有测试回归审计（AUDIT-3 / H1，清单订正）

去覆盖后真实路径需 `resolveType` 成功。**必改测试清单**（补 TypeTable，验余额的补 AssetTable，断言改用真实回退新值——L4）：
- `TransactionDaoLogicTest.kt`：`when_deleteBookTransaction_then_assets_also_deleted`(387)、`_only_book_records_deleted`(404)、`when_deleteAssetRelatedData_then_all_related_records_deleted`(420)、`_tag_and_image_relations_cleaned`(435) —— 4 个，全用 `createRecordTable` typeId=1L 默认（515）、无 `setupTypesForAbsorption`。
- `BooksRepositoryImplTest.kt:191`（`when_deleteBookTransaction_then_book_and_records_deleted`）—— typeId=1L、无 TypeTable，断言 `deleteBookCalled`（205）前即崩。
- **M5/F-3 订正**：**删除**原稿列的「RecordRepositoryImplTest 相关用例」目标——全仓无 JVM 测试调 `deleteRecordsWithAsset`/`deleteAssetRelatedData`（除 LogicTest 直调 dao），asset 删除回归只在 `TransactionDaoLogicTest:420-458`。

### 5.4 androidTest（compile-verified，有设备时跑）

`TransactionDaoTest` 补跨账本/重算用例；本机无设备只编译核验，run 验证由 JVM `TransactionDaoLogicTest` 承担。

## 6. L1 死代码清理

- **决策反转说明（M6/F-4）**：2026-06-08 prior full-review（`.full-review/05-final-report.md:27`）曾将 L1 microadjust 为 defer（理由「跨 DAO 切换 + androidTest 无法 run 验证 + churn > Low 价值」）。本次纳入的依据：与 F-1 同批做（已在 `core/database` 删除路径上下文中）、跨 DAO 切换已验 compile-safe（`RecordDaoTest:786` 确已持有 `transactionDao`）；androidTest 无法本机 run 的顾虑通过 compile-verified + JVM 侧 `queryAllRelatedRecords` 已有覆盖缓解。
- 删 `RecordDao.queryRelatedRecord()`（305）+ `FakeRecordDao` override（236，必须同步删，否则 override 不存在方法编译错）。
- androidTest `RecordDaoTest`（M7/F-5/I-5 订正）：
  - **793 所在整个测试** `when_queryRelatedRecord_then_returnsAllRelations`（777-795）**删除**——其唯一目的是测 `queryRelatedRecord()`，改 swap 只会变成 `queryAllRelatedRecords` 的冗余隐式重复测试；连带清理 775 region 注释中的 `queryRelatedRecord` 字样。
  - **952**（`when_deleteRelatedWithAsset` 中当验证 helper 用）改用 `transactionDao.queryAllRelatedRecords()`（SQL 逐字相同，该测试已持有 transactionDao）。
- 已确认无生产调用方（grep 全仓仅上述测试 + 定义）。compile-verified。

## 7. 影响面

- 公共行为：`deleteBook` / `deleteRecordsWithAsset` 对外结果不变（存活资产余额 + 存活记录 finalAmount 与现状一致），仅更快。
- `deleteRecordTransaction`（单删）签名不变、行为等价（§4.1 论证）。
- `recalculateFinalAmountForCluster` 对外签名不变（内部拆 `discoverClusterIds` + `recalculateFinalAmountFromCluster`，**保留 outEdges 缓存不丢**）。
- **备份/迁移/启动 gate 零影响（impact I-7 核验）**：`recalculateAllFinalAmount`（253-291）是独立实现，不调 `recalculateFinalAmountForCluster`/`discoverClusterIds`；其消费者 `BackupRecoveryManagerImpl.kt:702`、`RecordRepositoryImpl.kt:503/512`、`LauncherContentViewModel.kt:85` 均不经删除路径。
- 数据迁移：无。Schema：无变更（本次纯逻辑重构，无 `@Entity`/迁移改动）。

## 8. 成功标准

1. 删账本/删资产消除 O(N²)：`queryRecordByIds`（recalc 入口）调用次数 = 存活簇数（正常数据 0），不随删除记录数线性增长；
2. 存活资产余额 + 存活记录 finalAmount 与现状逐字段一致（JVM 测断言）；
3. `discoverClusterIds` 抽取**不丢 outEdges**、不引入 N+1，每存活簇只 BFS 一次；
4. AUDIT-2 契约守住（删资产目标资产存活+余额回退）+ AUDIT-2b 转账对方资产余额回退有守卫；
5. L1 死代码移除、编译通过、androidTest compile-verified；既有 5 个删除测试补 type 后全绿；
6. 节点1 team-review 四维（本节）+ 节点2 full-review 通过。

## 9. 节点1 team-review 吸收记录（v2）

四维（feasibility/security/reverse/impact）评审，controller 已 hands-on 核验全部强断言。High 3 / Medium 7 / Low 6，无 Critical、无算法终态正确性硬 bug。全部吸收，无驳回。

| 编号 | 维度来源 | 严重度 | 落实位置 |
|---|---|---|---|
| H1 | reverse R-1 + impact I-1 + feasibility F-2 | High | §5.1 前置约束 + §5.3 清单（根因改为 resolveType 抛异常缺 Type，列全 5 测试） |
| H2 | impact I-2 + feasibility F-1 | High | §4.3 discoverClusterIds 返富类型带 outEdges |
| H3 | impact I-3 | High（实质 Medium） | §4.3 recalculateFinalAmountFromCluster 入口，单次 BFS（与 H2 同处修复） |
| M1 | reverse R-2 | Medium | §5.2#3 删资产转账对方资产余额回退测试（带 assets+types） |
| M2 | security | Medium | §4.4 asset 删除跨 4 调用非单事务边界声明 |
| M3 | reverse R-3 | Medium | §2 AUDIT-2 措辞订正 + AUDIT-2b 单列 |
| M4 | impact I-4 | Medium | §4.1 union vs category 单分支等价前提/有意放宽说明 |
| M5 | feasibility F-3 | Medium | §5.3 删除不存在的 RecordRepositoryImplTest 目标 |
| M6 | feasibility F-4 | Medium | §6 L1 defer 决策反转理由 |
| M7 | feasibility F-5 + impact I-5 | Medium | §6 RecordDaoTest:793 整测删除、仅 952 swap |
| L1 | reverse R-4 + feasibility F-6 | Low | §5.2#2 区分力指标改计 queryRecordByIds 次数 |
| L2 | security | Low | §5.2#5 无 FK 下存活簇正向断言 |
| L3 | security | Low | §4.1/§4.4 deleteRecordCore 保留抛异常 + §5.2#6 回滚测试 |
| L4 | security | Low | §5.3 断言用真实回退新值 |
| L5 | reverse R-5 | Low | §4.1 多吸收者等价性补强 |
| L6 | reverse R-6 | Low | §4.2 批量收尾硬顺序约束 |

确认项（无需动作）：SQL 注入面 CLEAN；备份/迁移/启动 gate 零影响（I-7）；L1 swap 无遗漏生产引用（I-6/feasibility）；deleteRecordCore 抽取边界干净 + Fake base 方法齐全（feasibility ②a/②c）；O(N²) 真凶定位与余额线性判断成立（feasibility ④）。
