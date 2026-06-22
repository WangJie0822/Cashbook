# P-M1 删除路径批量化（关联/记录批量 SQL）设计

> 创建于 2026-06-22。承接 F-1（删账本/删资产去 O(N²)，main `4c0dad0b`）。
> 方案选型：**A2**（批量删关联/记录 + 余额保持逐条），用户 2026-06-22 拍板。
> 节点1 team-review 四维（feasibility/security/reverse/impact）已采纳，修订见各章 ★ 标注与 §9。

## 1. 背景与目标

F-1 已消除「删账本/删资产时每条记录都整簇重算 finalAmount」的 O(N²)，删除走 `deleteRecordsBatch`：逐条 `deleteRecordCore`（余额回退 + 清关联 + 删记录）+ 删后存活簇只重算一次。

**下一个瓶颈**：`deleteRecordCore` 每条记录 ~8 次 per-record DB 往返：resolveType + queryAssetById（主资产）+ updateAsset + queryAssetById（转账对方）+ updateAsset（仅 TRANSFER）+ deleteOldRelatedTags + deleteOldRelatedImages + clearRelatedRecordById + deleteRecord。删 N 条 = N × ~8 单行 DB 操作。

**目标**：把「删关联（标签/图片/关联记录）+ 删记录」从逐条改为**批量 SQL（`WHERE … IN (:ids)`）**，per-record 往返从 ~8 降到 ~2（仅余额回退 query+update）。**余额回退逻辑零改动**（保持逐条，规避方案 A 的信用卡/转账符号聚合数据正确性回归——A2 显式不做）。对外行为逐字段不变。

## 2. 现状关键事实（带出处，行号经节点1 勘误）

- `deleteRecordCore`（`TransactionDao.kt:587-656`，KDoc 587 起 / `suspend fun` 593）：resolveType + calculateRecordAmount（:595-598）+ 余额回退（:599-640）+ `deleteOldRelatedTags`(:643) + `deleteOldRelatedImages`(:646) + `clearRelatedRecordById`(:649) + `deleteRecord`(:652，`@Delete` 返回行数，`<=0` 抛 `DataTransactionException`，**L3**)。
- `deleteRecordsBatch`（`:741-764`）：`deletedIds = records.mapNotNull{it.id}.toSet()`（:743，**带去重**）→ 删前逐条捕获 survivors（:745-755）→ 逐条 `deleteRecordCore` → 存活簇重算（**L6**：必须在所有删除后，否则 BFS 脏读）。
- 单删 `deleteRecordTransaction(record)`(:664) 委托 `deleteRecordsBatch(listOf(record))`；账本 `deleteBookTransaction`(:772) / 资产 `deleteAssetRelatedData`(:858) 走 `deleteRecordsBatch(query…)`。
- **8 个 byBookId/byAssetId 批量删 SQL 是 dead code**（`:722-732`/`:816-851`，仅 DAO 定义 + Fake override，无生产调用；节点1 impact/feasibility 双确认）。
- `updateAsset`(:86)/`queryAssetById`(:93) 均单条，无批量版。
- **`@Query DELETE … : Int` 项目无先例**（所有 @Query DELETE 返回 Unit，唯一 Int 是 `@Delete deleteRecord`）——见 §5 H1。

## 3. 方案 A2 设计

### 3.1 拆 `deleteRecordCore` → `revertRecordBalanceOnly`

把 `deleteRecordCore` 的**余额回退部分**（:595-640，含 resolveType + calculateRecordAmount + 主资产 + 转账对方）抽成私有 `revertRecordBalanceOnly(record)`，**符号逻辑逐字符不变**。删关联/删记录部分不进此函数。`deleteRecordCore` 移除（逻辑分散到 `revertRecordBalanceOnly` + batch 批量删）。

### 3.2 新增 byIds 批量删 SQL（abstract @Query）

```
DELETE FROM db_tag_with_record    WHERE record_id IN (:ids)
DELETE FROM db_image_with_related WHERE record_id IN (:ids)
DELETE FROM db_record_with_related WHERE record_id IN (:ids) OR related_record_id IN (:ids)
DELETE FROM db_record             WHERE id IN (:ids)          -- 返回 Int 删除行数（项目首引，§5 H1 首验）
```
方法名：`deleteTagRelationsByRecordIds` / `deleteImageRelationsByRecordIds` / `deleteRecordRelationsByRecordIds` / `deleteRecordsByIds`。
**`FakeTransactionDao` 须忠实 override**：`deleteRecordRelationsByRecordIds` 必须 `it.recordId in ids || it.relatedRecordId in ids`（双向 IN-OR），禁用宽松 `contains`（CLAUDE.md 测试替身规范，节点1 security F-3/impact）。

### 3.3 改造 `deleteRecordsBatch`（★ 节点1 修订）

```
deleteRecordsBatch(records):
  if empty return
  val deletedIds = records.mapNotNull{ it.id }.toSet()   // ★C1：保留 .toSet() 去重；L3 校验用去重后 size
  // ★H3/L7：survivors 捕获必须在批量删关联之前（删后关联已清无法查），与现逻辑同
  affectedSurvivors = 遍历 records，queryRelatedByRecordId/queryRelatedByRelatedRecordId 捕获两方向不在 deletedIds 的对端
  // 1. 逐条余额回退（零符号改动）
  // ★M2：过滤 id==null，与逐条版 deleteRecordCore 的 `record.id ?: return` 对齐，避免「回退集≠删除集」
  for (record in records.filter { it.id != null }) revertRecordBalanceOnly(record)
  // 2. 批量删关联（chunk 防 IN 上限）
  // ★L8：三类关联删 + 删记录必须遍历【同一完整 deletedIds 全集】，不可拆分到不同分片集合
  deletedIds.chunked(DELETE_IN_CHUNK_SIZE).forEach {
    deleteTagRelationsByRecordIds(it); deleteImageRelationsByRecordIds(it); deleteRecordRelationsByRecordIds(it)
  }
  // 3. 批量删记录 + L3 行数校验（去重 size）
  val deleted = deletedIds.chunked(DELETE_IN_CHUNK_SIZE).sumOf { deleteRecordsByIds(it) }
  if (deleted < deletedIds.size) throw DataTransactionException("Record delete failed!")
  // 4. 存活簇重算（L6：在所有删除之后，保持）
  …discoverClusterIds + recalculateFinalAmountFromCluster…
```

### 3.4 IN chunk

`core/database` 内加 `private const val DELETE_IN_CHUNK_SIZE = 900`（与 core:data 的 `SQL_IN_CHUNK_SIZE` 同值同义；DAO 在 core:database 不能依赖 core:data，独立定义，注释交叉引用。节点1 reverse L2：可选下沉 core:common 共享，本次不做避免扩面）。

### 3.5 dead code 清理（L-clean）

删除 8 个 byBookId/byAssetId dead 批量删 SQL（`:722-732`/`:816-851`）+ `FakeTransactionDao` 对应 override。`deleteBookTransaction`/`deleteAssetRelatedData` 不受影响（已走 `deleteRecordsBatch`）。

## 4. 约束（强制保持）

- **L3（事务原子性）**：`deleteRecordsBatch` 仍 `@Transaction`；批量删记录返回行数 < 去重待删数 → 抛 `DataTransactionException` → 整体回滚。多 chunk DELETE 都在同一 `@Transaction` 默认方法内顺序 suspend 调用，处于同一事务。
- **L6（重算顺序）**：存活簇重算严格在所有删关联+删记录之后（保持），否则 BFS 脏读。
- **L7（★survivors 捕获时序）**：survivors 捕获（读关联表）必须早于批量删关联（删后关联已清无法捕获）。余额回退不碰关联表，插在捕获与批量删之间无害。
- **L8（★删除集完整性）**：三类关联删 + 删记录必须遍历同一完整 `deletedIds` 全集；relation 删的 IN 集与 record 删的 IN 集必须是同一 deletedIds。`deleteRecordRelationsByRecordIds` 双向 IN-OR 保证跨 chunk 边不漏删（每条边任一端落任一 chunk 即删，节点1 reverse H2 核验）。
- **不变量（★余额快照）**：余额回退用调用方传入的 `record` 快照（非库内实时值）；当前三调用方均传新鲜快照（`queryRecordListByBookId`/`queryRecordsByAssetId`/`queryRecordById`），A2 拉长快照与删除间窗口，须保持「调用方传新鲜快照」前置不变量（节点1 reverse H1）。
- **等价性**：余额回退与现逐条 `deleteRecordCore` 余额部分逐字符一致；批量删关联/记录与逐条删终态一致（同组 id，IN 删 = 逐条删集合语义，幂等无序）。
- **金额口径**：不碰 `calculateRecordAmount`/finalAmount 算法。

## 5. 风险

| ID | 风险 | 缓解 |
|---|---|---|
| H1（feasibility）| `@Query DELETE … : Int` 项目首引，L3 校验依赖其返回删除行数 | **实施第一步**先写最小 `@Query DELETE : Int` + androidTest 断言返回行数锁定；Room 官方支持但项目零先例须首验 |
| R1 | 余额符号回归 | **A2 不碰余额符号**（逐条 `revertRecordBalanceOnly` 零改动）；模拟器 androidTest 兜底 |
| C2/F-1（reverse/security，最危险）| A2 把余额回退整体提前到所有删除前，放大「已回退余额 + 删阶段失败」中间窗，正确性**完全依赖 `@Transaction` 回滚**，而 JVM Fake **不建模回滚**（§4-L3 回滚保证仅 androidTest 可验） | 模拟器「批量删 chunk 中途失败 → 回滚已删行」用例列 **blocking 验收**，禁止因本机不便降级（§6） |
| C1（reverse）| 重复/幻影 id 致 `deleted < size` 误抛回滚 | §3.3 保留 `.toSet()` 去重 + L3 用去重 size；当前调用链不产生重复/幻影 id |
| R3 | IN 超 SQLite 变量上限崩溃 | chunk(900) |

## 6. 验证策略

- **JVM `TransactionDaoLogicTest`**（日常回归护栏，本机可跑；注：Fake 跑真实 `deleteRecordsBatch` 默认算法，但**无 `@Transaction` 回滚语义**，失败分支终态断言无效）：
  - 删账本/删资产/单删后余额正确回退（信用卡 + 非信用卡 + 转账对方资产，金丝雀）
  - 关联（标签/图片/关联记录）+ 记录批量删除干净
  - **★存活吸收者→被删支出**：删一批含被吸收支出，断言存活吸收者悬空关联被清 + 存活簇 finalAmount 重算正确（节点1 security F-3/reverse H4，守 survivors 捕获时序 L7）
  - 删除等价性：A2 正常路径终态 == 改造前逐条删终态（同 fixture；**仅正常路径有效**，失败回滚等价性走 androidTest）
- **模拟器 androidTest `TransactionDaoTest`**（android-cli 启 `Medium_Phone`，真实 Room；**blocking**）：
  - 信用卡/转账余额回退符号正确（R1 兜底）
  - IN 批量删真实 SQLite 执行正确
  - **★`@Transaction` 回滚（C2/F-1）**：构造异常发生在**首个 chunk 删除成功之后**（如坏数据使行数校验在累加后触发），断言已删行 + 已回退余额均回滚——A2 新增中间态路径，现有 `when_deleteBookTransaction_with_failure_then_rolled_back` 的 resolveType 注入点在删除前不覆盖此路径
  - `@Query DELETE : Int` 返回行数语义（H1 首验）

## 7. 文件清单

| 文件 | 改动 |
|---|---|
| `core/database/.../dao/TransactionDao.kt` | 拆 `revertRecordBalanceOnly`；改 `deleteRecordsBatch`（L7/L8/M2/C1）；新增 4 个 byIds @Query（含 `deleteRecordsByIds: Int`）+ `DELETE_IN_CHUNK_SIZE`；删 8 个 byBookId/byAssetId dead SQL；移除 `deleteRecordCore` |
| `core/data/.../testdoubles/FakeTransactionDao.kt` | 忠实 override 4 个 byIds（双向 IN-OR）；删 8 个 byBookId/byAssetId override；移除 `deleteRecordCore`（Fake 未 override，无需动） |
| `core/data/.../TransactionDaoLogicTest.kt` | 补/改删除等价 + 余额回退金丝雀 + 批量删干净 + 存活吸收者悬空关联清理用例 |
| `core/database/.../androidTest/.../TransactionDaoTest.kt` | 补真机余额符号 + IN 批量删 + **中途失败回滚（blocking）** + `: Int` 返回行数；订正 :808 注释 `deleteRecordCore`→`revertRecordBalanceOnly` |

## 8. 非目标（YAGNI）

- 余额按资产聚合（方案 A）—— 显式不做，规避符号回归。
- 批量 `updateAssets`（余额逐条 update 保持）。
- 类型批量预取 Map —— resolveType 随 `revertRecordBalanceOnly` 逐条调用、不做批量 Map 预取（余额回退已逐条，无额外收益）。
- 「批量删关联 + 逐条删记录」保守变体（逐条 `@Delete` 返回值做精确 L3、消 C1 行数歧义，节点1 reverse L1）—— 评估后仍取全批量（C1 经 `.toSet()` + 去重 size 已规避，全批量省 1 次/条往返）。

## 9. 节点1 team-review 采纳记录（四维，controller hands-on 核验）

- **reverse C1（Critical→采纳勘误）**：§3.3 漏 `.toSet()`，核验现役 :743 确有去重 → 修。L3 用去重 size。
- **reverse C2 / security F-1（Critical/High→采纳）**：余额回退提前放大中间窗、回滚仅 androidTest 可验 → 模拟器中途失败回滚用例列 blocking（§6）。
- **feasibility H1（High→采纳）**：`@Query DELETE : Int` 项目首引，grep 核验确无先例 → 实施首验（§5）。
- **reverse H1（High→采纳）**：余额回退依赖新鲜快照 → §4 记不变量。
- **reverse H2（High，攻击不成立但采纳约束）**：双向 IN-OR 跨 chunk 不漏删（核验成立）→ §4-L8 写明删除集完整性约束。
- **reverse H3/H4 / security F-3（High/Medium→采纳）**：survivors 捕获时序 L7 + 存活吸收者悬空关联清理用例。
- **reverse M2（Medium→采纳）**：id==null 过滤对齐逐条版（§3.3）。
- **security F-2（Low→采纳）**：§4-L3 标注回滚仅 androidTest 可验、JVM Fake 不建模回滚。
- **impact（总体 LOW，确认）**：三删除链全汇聚 `deleteRecordsBatch`、Fake 同步、dead code 无引用、schema 零变更均经实证成立。
- **feasibility M1/M2（Medium→采纳）**：行号勘误（§2）+ §8 resolveType 措辞消歧义。
