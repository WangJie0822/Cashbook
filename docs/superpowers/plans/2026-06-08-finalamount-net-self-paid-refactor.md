# finalAmount 净自付重构 + C 净额展示 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `finalAmount` 吸收模型重构为「净自付」语义（被报销/退款的支出存净自付额、报销款不虚增收入、消灭负值），使月度分项统计正确，并让列表 C 净额展示基于正确数据层。

**Architecture:** 在 `TransactionDao` 抽出单一「吸收簇净自付重算」纯算法（顺序贪心填充），增删改三处 + 全量重算复用，消除现状 insert/recalc 口径不一致。全量重算改造既有 `migrateAfter9To10`（不升 DB_VERSION），老用户经 `LauncherContentViewModel` 新 gate（`finalAmountNetRecalcDone` 标记）触发一次，备份恢复后同步重算。饼图与列表显示层切到 net self-paid。

**Tech Stack:** Kotlin · Room · Proto DataStore · Hilt · JUnit4 + Truth + coroutines-test · Roborazzi 截图测试

---

## 与 spec v2 的事实校正（实施前必读）

撰写本 plan 时对 spec v2（`2026-06-05-finalamount-net-self-paid-refactor-design.md`）引用的代码做了 hands-on 核验，发现 3 处偏差，已在本 plan 校正：

1. **迁移标记 proto 位置**：spec §7.1/§11 写 `record_settings.proto`，**实际**既有迁移标记 `db9To10DataMigrated` 在 **`temp_keys.proto`**（`TempKeysModel` + `CombineProtoDataSource.tempKeysData`）。新标记 `finalAmountNetRecalcDone` 加到 `temp_keys.proto`（与既有同类标记同址）。
2. **触发入口**：spec §7.2 选 `InitWorker`（需 M1 给 HiltWorker 注入 `RecordRepository`）。**用户 2026-06-08 拍板改为 `LauncherContentViewModel`**——既有 `migrateAfter9To10` 触发就在此处（`LauncherContentViewModel.kt:67-75` init 协程，gated `!db9To10DataMigrated`），新 gate 与之同位、顺序确定、免 InitWorker DI 改动。InitWorker（`sync/work`）本 plan **不改**。
3. **`RecordListItem` 复用屏数**：spec §8/M3 说 3 屏。**实际**是单一 composable（定义在 `LauncherContentScreen.kt:~660`），被 6 屏 7 处调用（`LauncherContentScreen`/`SearchScreen`/`AssetInfoContentScreen`/`CalendarScreen`/`SelectRelatedRecordScreen`×2/`TypedAnalyticsScreen`）。改一处影响全部，截图回归面更广。

另核验确认（无需改动，仅测试守住）：
- **饼图 TRANSFER 不入饼图**：`TransRecordViewsToAnalyticsPieUseCase.kt:43` 用 `it.type.typeCategory == typeCategory`（typeCategory 仅 EXPENDITURE/INCOME），转账被过滤。spec §8 对饼图 TRANSFER 口径的顾虑不触发，饼图改造只需把 `analyticsPieAmount(...)` 换成 `record.finalAmount`。
- **资产余额与 finalAmount 解耦**：insert/delete/batchImport/verifyAssetBalance 全用 `calculateRecordAmount`/`record.amount`，本重构对资产余额零影响（Task 12 加不变性测试守住）。
- **androidTest `when_insertRecordWithRelated`（`TransactionDaoTest.kt:507-546`）**：income(20000) 吸收 E1(5000)+E2(3000)，income > 总支出 → 净自付下 E1=0,E2=0,income=12000，**与旧断言一致无需改**。需改写的是 income < 支出的场景（`TransactionDaoLogicTest` 三处负值断言）。
- **转账 finalAmount 既有不一致（不在本次范围）**：insert 路径转账存 `recordAmount(TRANSFER)=amount+charge-concessions`（`TransactionDao.kt:334`），migrate 路径存 `concessions-charge`（`RecordRepositoryImpl.kt:504`）。本 plan 全量重算**保持 migrate 的 `concessions-charge`**（与既有 migrate 行为一致，不引入新回归），不修 insert 路径转账（YAGNI，转账不参与吸收）。

## File Structure（改动文件总览）

| 文件 | 改动 | Task |
|---|---|---|
| `core/datastore-proto/src/main/proto/temp_keys.proto` | 新增 `finalAmountNetRecalcDone` 字段 3 | 1 |
| `core/model/.../model/TempKeysModel.kt` | 新增字段 | 1 |
| `core/datastore/.../datasource/CombineProtoDataSource.kt` | tempKeysData 映射 + `updateFinalAmountNetRecalcDone` | 1 |
| `core/data/test/.../testdoubles/FakeCombineProtoDataSource.kt` | 镜像字段 + 更新方法 | 1 |
| `core/testing/.../repository/FakeSettingRepository.kt` | 镜像字段 | 1 |
| `core/database/.../dao/TransactionDao.kt` | 净自付簇算法 + 全量重算 + insert/delete/recalc 改造 + 2 查询 | 2-6 |
| `core/data/test/.../testdoubles/FakeTransactionDao.kt` | 新增 2 查询 override | 2 |
| `core/data/test/.../testdoubles/TransactionDaoLogicTest.kt` | M5 断言改写 + 新算法测试 | 2,3,5,6 |
| `core/data/.../repository/RecordRepository.kt` | 新增 `recalculateAllFinalAmount()` | 7 |
| `core/data/.../repository/impl/RecordRepositoryImpl.kt` | 改造 `migrateAfter9To10` + 实现 `recalculateAllFinalAmount` | 7 |
| `core/testing/.../repository/FakeRecordRepository.kt` | `recalculateAllFinalAmount` override | 7 |
| `feature/records/.../viewmodel/LauncherContentViewModel.kt` | 新增 `finalAmountNetRecalcDone` gate | 8 |
| `core/data/.../uitl/impl/BackupRecoveryManagerImpl.kt` | 恢复成功后同步全量重算 | 9 |
| `core/domain/.../usecase/TransRecordViewsToAnalyticsPieUseCase.kt` | 改 `record.finalAmount` | 10 |
| `core/domain/.../usecase/TransRecordViewsToAnalyticsPieSecondUseCase.kt` | 改 `record.finalAmount` | 10 |
| `feature/records/.../screen/LauncherContentScreen.kt` | `RecordListItem` displayAmount + 去删除线 | 11 |
| `core/database/androidTest/.../dao/TransactionDaoTest.kt` | M5 断言 + 新净自付 instrumented 测试 | 12 |
| 各截图/ViewModel 测试 | 签名/算例同步 | 11,12 |

---

## Task 1: 新增 `finalAmountNetRecalcDone` 迁移标记（datastore 管道）

**Files:**
- Modify: `core/datastore-proto/src/main/proto/temp_keys.proto`
- Modify: `core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/model/TempKeysModel.kt:24-29`
- Modify: `core/datastore/src/main/kotlin/cn/wj/android/cashbook/core/datastore/datasource/CombineProtoDataSource.kt:132-138,191-193`
- Modify: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/testdoubles/FakeCombineProtoDataSource.kt:99-104,274-276`
- Modify: `core/testing/src/main/kotlin/cn/wj/android/cashbook/core/testing/repository/FakeSettingRepository.kt:87-90`
- Test: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/repository/impl/SettingRepositoryImplTest.kt`（已有 `db9To10DataMigrated` 测试块，仿写）

- [ ] **Step 1: 写失败测试**（在 `SettingRepositoryImplTest.kt` 的 `db9To10DataMigrated 测试` 块附近新增）

```kotlin
// ========== finalAmountNetRecalcDone 测试 ==========
@Test
fun when_finalAmountNetRecalcDone_default_then_false() = runTest {
    val tempKeys = settingRepository.tempKeysModel.first()
    assertThat(tempKeys.finalAmountNetRecalcDone).isFalse()
}

@Test
fun when_updateFinalAmountNetRecalcDone_true_then_tempKeys_updated() = runTest {
    fakeDataSource.updateFinalAmountNetRecalcDone(true)
    val tempKeys = settingRepository.tempKeysModel.first()
    assertThat(tempKeys.finalAmountNetRecalcDone).isTrue()
}
```

- [ ] **Step 2: 运行测试确认编译失败**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*.SettingRepositoryImplTest" --offline --no-daemon --console=plain`
Expected: 编译失败 `unresolved reference: finalAmountNetRecalcDone` / `updateFinalAmountNetRecalcDone`

- [ ] **Step 3: proto 加字段**（`temp_keys.proto`）

```proto
/* 临时开关配置 */
message TempKeys {
  bool db9To10dataMigrated = 1; // 数据库版本9升级到10之后是否进行数据迁移
  bool preferenceSplit = 2; // app_preferences 是否已拆分
  bool finalAmountNetRecalcDone = 3; // finalAmount 净自付重算是否已执行（proto3 默认 false，首启触发一次）
}
```

- [ ] **Step 4: 模型 + 映射 + 更新方法 + 镜像构造点**

`TempKeysModel.kt`（新增字段，**放末尾保持其他构造点位置参数兼容**）：
```kotlin
data class TempKeysModel(
    /** 数据库数据是否执行版本9到版本10迁移 */
    val db9To10DataMigrated: Boolean,
    /** proto app_preferences 是否拆分 */
    val preferenceSplit: Boolean,
    /** finalAmount 净自付重算是否已执行 */
    val finalAmountNetRecalcDone: Boolean = false,
)
```

`CombineProtoDataSource.kt:134-138` 映射 + `:191-193` 后新增更新方法：
```kotlin
// tempKeysData map（加一行）
TempKeysModel(
    db9To10DataMigrated = it.db9To10DataMigrated,
    preferenceSplit = it.preferenceSplit,
    finalAmountNetRecalcDone = it.finalAmountNetRecalcDone,
)
```
```kotlin
suspend fun updateFinalAmountNetRecalcDone(done: Boolean) {
    tempKeys.updateData { it.copy { this.finalAmountNetRecalcDone = done } }
}
```

`FakeCombineProtoDataSource.kt:100-103` 构造 + `:274-276` 后新增方法：
```kotlin
TempKeysModel(
    db9To10DataMigrated = false,
    preferenceSplit = false,
    finalAmountNetRecalcDone = false,
)
```
```kotlin
suspend fun updateFinalAmountNetRecalcDone(done: Boolean) {
    _tempKeys.update { it.copy(finalAmountNetRecalcDone = done) }
}
```

`FakeSettingRepository.kt:87-89` 构造（加一行 `finalAmountNetRecalcDone = false`，因字段有默认值此处可不改，但显式写出更清晰）。

- [ ] **Step 5: 运行测试确认通过**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*.SettingRepositoryImplTest" --offline --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL（断言 `grep -E "^BUILD (SUCCESSFUL|FAILED)"`）

- [ ] **Step 6: 提交**

```bash
git add core/datastore-proto core/model core/datastore core/data core/testing
git commit -m "[feat|core|finalAmount净额][公共]新增 finalAmountNetRecalcDone 迁移标记(temp_keys.proto)"
```

---

## Task 2: 净自付簇算法核心 + 全量重算骨架（TransactionDao）

> 本 Task 是重构核心。`recalculateFinalAmountForCluster` = 增量入口（BFS 发现连通簇 → 初始化 recordAmount → 吸收者 id 升序顺序贪心填充）；`recalculateAllFinalAmount` = 全量入口（同算法全表版，转账保持 concessions-charge）。二者按 §5 同序，增量与全量结果逐字段一致。

**Files:**
- Modify: `core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/dao/TransactionDao.kt`（新增 2 查询 + 2 算法函数；改造 `recalculateAbsorberFinalAmount`）
- Modify: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/testdoubles/FakeTransactionDao.kt`（新增 2 查询 override）
- Test: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/testdoubles/TransactionDaoLogicTest.kt`

- [ ] **Step 1: 写失败测试**（§5 六场景守恒+非负，新增到 `TransactionDaoLogicTest.kt`）

```kotlin
// ========== 净自付簇算法 recalculateFinalAmountForCluster 测试 ==========

@Test
fun when_cluster_1to1_partial_then_expense_net_income_zero() = runTest {
    // E(100) 被 I(80) 部分吸收 → E.fa=20, I.fa=0
    setupTypesForAbsorption()
    insertRecord(id = 1L, typeId = EXPENDITURE_TYPE_ID, amount = 10000L)
    insertRecord(id = 2L, typeId = INCOME_TYPE_ID, amount = 8000L)
    dao.relatedRecords.add(RecordWithRelatedTable(id = 1L, recordId = 2L, relatedRecordId = 1L))

    dao.recalculateFinalAmountForCluster(2L)

    assertThat(dao.queryRecordById(1L)!!.finalAmount).isEqualTo(2000L)  // 净自付
    assertThat(dao.queryRecordById(2L)!!.finalAmount).isEqualTo(0L)     // 溢出
}

@Test
fun when_cluster_over_reimburse_then_floor_zero_and_overflow_to_income() = runTest {
    // E(100) 被 I(120) 超额吸收 → E.fa=0, I.fa=20
    setupTypesForAbsorption()
    insertRecord(id = 1L, typeId = EXPENDITURE_TYPE_ID, amount = 10000L)
    insertRecord(id = 2L, typeId = INCOME_TYPE_ID, amount = 12000L)
    dao.relatedRecords.add(RecordWithRelatedTable(id = 1L, recordId = 2L, relatedRecordId = 1L))

    dao.recalculateFinalAmountForCluster(2L)

    assertThat(dao.queryRecordById(1L)!!.finalAmount).isEqualTo(0L)
    assertThat(dao.queryRecordById(2L)!!.finalAmount).isEqualTo(2000L)
}

@Test
fun when_cluster_1toN_then_greedy_fill_by_id_asc() = runTest {
    // I(80) 吸收 E1(100),E2(50) → E1.fa=20,E2.fa=50,I.fa=0
    setupTypesForAbsorption()
    insertRecord(id = 1L, typeId = EXPENDITURE_TYPE_ID, amount = 10000L)
    insertRecord(id = 2L, typeId = EXPENDITURE_TYPE_ID, amount = 5000L)
    insertRecord(id = 3L, typeId = INCOME_TYPE_ID, amount = 8000L)
    dao.relatedRecords.add(RecordWithRelatedTable(id = 1L, recordId = 3L, relatedRecordId = 1L))
    dao.relatedRecords.add(RecordWithRelatedTable(id = 2L, recordId = 3L, relatedRecordId = 2L))

    dao.recalculateFinalAmountForCluster(3L)

    assertThat(dao.queryRecordById(1L)!!.finalAmount).isEqualTo(2000L)  // 80 先填 E1 → 100-80
    assertThat(dao.queryRecordById(2L)!!.finalAmount).isEqualTo(5000L)  // 无剩余，E2 全额自付
    assertThat(dao.queryRecordById(3L)!!.finalAmount).isEqualTo(0L)
}

@Test
fun when_cluster_Nto1_then_expense_offset_by_sum() = runTest {
    // E(100) 被 I1(30),I2(40) 吸收 → E.fa=30,I1.fa=0,I2.fa=0
    setupTypesForAbsorption()
    insertRecord(id = 1L, typeId = EXPENDITURE_TYPE_ID, amount = 10000L)
    insertRecord(id = 2L, typeId = INCOME_TYPE_ID, amount = 3000L)
    insertRecord(id = 3L, typeId = INCOME_TYPE_ID, amount = 4000L)
    dao.relatedRecords.add(RecordWithRelatedTable(id = 1L, recordId = 2L, relatedRecordId = 1L))
    dao.relatedRecords.add(RecordWithRelatedTable(id = 2L, recordId = 3L, relatedRecordId = 1L))

    dao.recalculateFinalAmountForCluster(1L)

    assertThat(dao.queryRecordById(1L)!!.finalAmount).isEqualTo(3000L)  // 100-30-40
    assertThat(dao.queryRecordById(2L)!!.finalAmount).isEqualTo(0L)
    assertThat(dao.queryRecordById(3L)!!.finalAmount).isEqualTo(0L)
}

@Test
fun when_cluster_charges_nonzero_then_net_self_paid_uses_recordAmount() = runTest {
    // M9: E(amount100,charge0)=100；I(amount80,charge5)→recordAmount(I)=75 → E.fa=25,I.fa=0
    setupTypesForAbsorption()
    insertRecord(id = 1L, typeId = EXPENDITURE_TYPE_ID, amount = 10000L)
    insertRecord(id = 2L, typeId = INCOME_TYPE_ID, amount = 8000L, charge = 500L)
    dao.relatedRecords.add(RecordWithRelatedTable(id = 1L, recordId = 2L, relatedRecordId = 1L))

    dao.recalculateFinalAmountForCluster(2L)

    assertThat(dao.queryRecordById(1L)!!.finalAmount).isEqualTo(2500L)  // 100-75
    assertThat(dao.queryRecordById(2L)!!.finalAmount).isEqualTo(0L)
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*.TransactionDaoLogicTest" --offline --no-daemon --console=plain`
Expected: 编译失败 `unresolved reference: recalculateFinalAmountForCluster`

- [ ] **Step 3: 实现簇算法 + 2 查询（`TransactionDao.kt`）**

在 `queryRecordByIds`（`:105`）附近新增 2 查询：
```kotlin
@Query(value = "SELECT * FROM db_record")
suspend fun queryAllRecords(): List<RecordTable>

@Query(value = "SELECT * FROM db_record_with_related")
suspend fun queryAllRelatedRecords(): List<RecordWithRelatedTable>
```

在 `recalculateAbsorberFinalAmount`（`:166-186`）**之前**新增核心簇算法（导入 `kotlin.collections.ArrayDeque` 无需额外 import）：
```kotlin
/**
 * 重算一个吸收簇的 finalAmount（净自付语义，§5 顺序贪心填充）。
 *
 * 从 [seedRecordId] 沿关系表 BFS 发现整个连通簇（被吸收支出↔吸收者收入构成二部图连通分量），
 * 将簇内所有记录 finalAmount 重置为 recordAmount，再按吸收者 id 升序、每个吸收者按被吸收支出 id 升序
 * 顺序贪心填充。结果只依赖 id 顺序、与插入历史无关，故增量与全量逐字段一致。
 *
 * @param seedRecordId 簇内任一记录 id（受影响的吸收者或被吸收支出）
 * @param excludeRecordIds 需从簇中排除的记录 id（删除场景：记录即将被删但关联尚未清除）
 */
suspend fun recalculateFinalAmountForCluster(
    seedRecordId: Long,
    excludeRecordIds: Set<Long> = emptySet(),
) {
    // 1. BFS 发现连通簇（排除 excludeRecordIds，排除节点不入簇也不被遍历）
    val clusterIds = LinkedHashSet<Long>()
    val queue = ArrayDeque<Long>()
    if (seedRecordId !in excludeRecordIds) {
        clusterIds.add(seedRecordId)
        queue.add(seedRecordId)
    }
    while (queue.isNotEmpty()) {
        val cur = queue.removeFirst()
        val neighbors = queryRelatedByRecordId(cur).map { it.relatedRecordId } +
            queryRelatedByRelatedRecordId(cur).map { it.recordId }
        for (n in neighbors) {
            if (n !in excludeRecordIds && clusterIds.add(n)) {
                queue.add(n)
            }
        }
    }
    if (clusterIds.isEmpty()) return

    // 2. 初始化簇内 finalAmount = recordAmount（簇内仅 income/expenditure，转账不参与吸收）
    val finalAmounts = HashMap<Long, Long>(clusterIds.size)
    for (id in clusterIds) {
        val record = queryRecordById(id) ?: continue
        val type = resolveType(record.typeId) ?: continue
        val category = RecordTypeCategoryEnum.ordinalOf(type.typeCategory)
        finalAmounts[id] = calculateRecordAmount(record, category)
    }

    // 3. 簇内吸收者（record_id 侧，有被吸收支出）按 id 升序，顺序贪心填充
    val absorbers = clusterIds.filter { id ->
        queryRelatedByRecordId(id).any { it.relatedRecordId in clusterIds }
    }.sorted()
    for (absorberId in absorbers) {
        var remaining = finalAmounts[absorberId] ?: continue
        val absorbedIds = queryRelatedByRecordId(absorberId)
            .map { it.relatedRecordId }
            .filter { it in clusterIds }
            .sorted()
        for (expenseId in absorbedIds) {
            val current = finalAmounts[expenseId] ?: continue
            val offset = minOf(remaining, current)  // current/remaining 均不下穿 0，保证非负
            finalAmounts[expenseId] = current - offset
            remaining -= offset
        }
        finalAmounts[absorberId] = remaining  // 溢出（通常 0）
    }

    // 4. 落库
    for ((id, finalAmount) in finalAmounts) {
        updateRecordFinalAmountById(id, finalAmount)
    }
}
```

- [ ] **Step 4: FakeTransactionDao 新增 2 查询 override**（`FakeTransactionDao.kt`，在 `queryRecordByIds`（`:171`）附近）

```kotlin
override suspend fun queryAllRecords(): List<RecordTable> {
    return records.toList()
}

override suspend fun queryAllRelatedRecords(): List<RecordWithRelatedTable> {
    return relatedRecords.toList()
}
```

> `recalculateFinalAmountForCluster` 是接口默认方法，FakeTransactionDao 直接继承——测试真实跑算法（CLAUDE.md 禁桩，忠实复刻净自付语义）。

- [ ] **Step 5: 运行测试确认通过**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*.TransactionDaoLogicTest" --offline --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL，5 个新测试 PASS

- [ ] **Step 6: 提交**

```bash
git add core/database core/data
git commit -m "[feat|core|finalAmount净额][公共]TransactionDao 净自付吸收簇算法 recalculateFinalAmountForCluster(§5 顺序贪心)"
```

---

## Task 3: 改造 `recalculateAbsorberFinalAmount` 委托簇算法 + M5 旧负值断言改写

**Files:**
- Modify: `core/database/.../dao/TransactionDao.kt:166-186`
- Modify: `core/data/test/.../TransactionDaoLogicTest.kt:106-176`（M5：旧 `recalculateAbsorberFinalAmount` 负值断言改写为净自付）

- [ ] **Step 1: 改写旧断言为失败测试**（替换 `TransactionDaoLogicTest.kt` 的三个旧测试 `:106-176`）

```kotlin
// ========== recalculateAbsorberFinalAmount 测试（净自付：委托簇重算）==========

@Test
fun when_absorber_recalc_single_absorbed_then_cluster_net_self_paid() = runTest {
    // I(60) 吸收 E(100) → 净自付：E.fa=40, I.fa=0（旧吸收模型曾为 I.fa=-40）
    setupTypesForAbsorption()
    insertRecord(id = 1L, typeId = EXPENDITURE_TYPE_ID, amount = 10000L)
    insertRecord(id = 2L, typeId = INCOME_TYPE_ID, amount = 6000L)
    dao.relatedRecords.add(RecordWithRelatedTable(id = 1L, recordId = 2L, relatedRecordId = 1L))

    dao.recalculateAbsorberFinalAmount(2L)

    assertThat(dao.queryRecordById(1L)!!.finalAmount).isEqualTo(4000L)  // 净自付，非负
    assertThat(dao.queryRecordById(2L)!!.finalAmount).isEqualTo(0L)
}

@Test
fun when_absorber_recalc_multiple_absorbed_then_greedy_by_id() = runTest {
    // I(60) 吸收 E1(100),E2(80)：I 先填 E1 → E1.fa=40,E2.fa=80,I.fa=0
    setupTypesForAbsorption()
    insertRecord(id = 1L, typeId = EXPENDITURE_TYPE_ID, amount = 10000L)
    insertRecord(id = 2L, typeId = EXPENDITURE_TYPE_ID, amount = 8000L)
    insertRecord(id = 3L, typeId = INCOME_TYPE_ID, amount = 6000L)
    dao.relatedRecords.add(RecordWithRelatedTable(id = 1L, recordId = 3L, relatedRecordId = 1L))
    dao.relatedRecords.add(RecordWithRelatedTable(id = 2L, recordId = 3L, relatedRecordId = 2L))

    dao.recalculateAbsorberFinalAmount(3L)

    assertThat(dao.queryRecordById(1L)!!.finalAmount).isEqualTo(4000L)
    assertThat(dao.queryRecordById(2L)!!.finalAmount).isEqualTo(8000L)
    assertThat(dao.queryRecordById(3L)!!.finalAmount).isEqualTo(0L)
}

@Test
fun when_absorber_recalc_no_absorbed_then_finalAmount_is_recordAmount() = runTest {
    setupTypesForAbsorption()
    insertRecord(id = 1L, typeId = INCOME_TYPE_ID, amount = 6000L)

    dao.recalculateAbsorberFinalAmount(1L)

    assertThat(dao.queryRecordById(1L)!!.finalAmount).isEqualTo(6000L)
}

@Test
fun when_absorber_recalc_excludes_specific_absorbed_then_excluded_not_in_cluster() = runTest {
    // I(60) 吸收 E1(100),E2(80)，排除 E1 → 簇={I,E2}：E2.fa=20,I.fa=0
    setupTypesForAbsorption()
    insertRecord(id = 1L, typeId = EXPENDITURE_TYPE_ID, amount = 10000L)
    insertRecord(id = 2L, typeId = EXPENDITURE_TYPE_ID, amount = 8000L)
    insertRecord(id = 3L, typeId = INCOME_TYPE_ID, amount = 6000L)
    dao.relatedRecords.add(RecordWithRelatedTable(id = 1L, recordId = 3L, relatedRecordId = 1L))
    dao.relatedRecords.add(RecordWithRelatedTable(id = 2L, recordId = 3L, relatedRecordId = 2L))

    dao.recalculateAbsorberFinalAmount(3L, excludeAbsorbedId = 1L)

    assertThat(dao.queryRecordById(2L)!!.finalAmount).isEqualTo(2000L)  // 6000 填 E2 → 8000-6000
    assertThat(dao.queryRecordById(3L)!!.finalAmount).isEqualTo(0L)
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*.TransactionDaoLogicTest" --offline --no-daemon --console=plain`
Expected: FAIL（旧 `recalculateAbsorberFinalAmount` 仍输出旧吸收模型负值 -4000，断言期望 4000）

- [ ] **Step 3: 改造 `recalculateAbsorberFinalAmount` 委托簇算法**（`TransactionDao.kt:166-186` 整体替换）

```kotlin
/**
 * 重算受影响吸收簇的 finalAmount（净自付语义）。
 *
 * 兼容旧签名：从 [absorberId] 所在簇重算，可排除即将删除的被吸收记录 [excludeAbsorbedId]。
 * 实际委托 [recalculateFinalAmountForCluster]——不再是「仅算吸收者」，而是整簇净自付重算。
 */
suspend fun recalculateAbsorberFinalAmount(
    absorberId: Long,
    excludeAbsorbedId: Long = -1L,
) {
    recalculateFinalAmountForCluster(
        seedRecordId = absorberId,
        excludeRecordIds = if (excludeAbsorbedId != -1L) setOf(excludeAbsorbedId) else emptySet(),
    )
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*.TransactionDaoLogicTest" --offline --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 提交**

```bash
git add core/database core/data
git commit -m "[refactor|core|finalAmount净额][公共]recalculateAbsorberFinalAmount 委托净自付簇算法 + M5 旧负值断言改写"
```

---

## Task 4: insert 吸收分支改造 → 簇算法

**Files:**
- Modify: `core/database/.../dao/TransactionDao.kt:367-376`（insert 的 `needRelated` 分支）
- Test: `core/data/test/.../TransactionDaoLogicTest.kt`（新增 insert-with-related 净自付测试）

- [ ] **Step 1: 写失败测试**

```kotlin
// ========== insertRecordTransaction 净自付测试 ==========
@Test
fun when_insert_income_absorber_then_cluster_net_self_paid() = runTest {
    // 先插入支出 E(100)，再插入收入 I(80) 关联 E → E.fa=20, I.fa=0
    setupTypesForAbsorption()
    val expense = createRecordTable(id = null, typeId = EXPENDITURE_TYPE_ID, amount = 10000L)
    val expenseId = dao.insertRecordTransaction(
        record = expense, tagIdList = emptyList(), needRelated = false,
        relatedRecordIdList = emptyList(), relatedImageList = emptyList(),
    ).let { dao.records.first { r -> r.amount == 10000L }.id!! }

    val income = createRecordTable(id = null, typeId = INCOME_TYPE_ID, amount = 8000L)
    dao.insertRecordTransaction(
        record = income, tagIdList = emptyList(), needRelated = true,
        relatedRecordIdList = listOf(expenseId), relatedImageList = emptyList(),
    )
    val incomeId = dao.records.first { it.amount == 8000L }.id!!

    assertThat(dao.queryRecordById(expenseId)!!.finalAmount).isEqualTo(2000L)
    assertThat(dao.queryRecordById(incomeId)!!.finalAmount).isEqualTo(0L)
}
```

> 注：`insertRecordTransaction` 是 `@Transaction` 默认方法，FakeTransactionDao 继承（不 override），会真实跑 insert 逻辑（含资产校验——本测试 assetId 默认 -1L=NO_ASSET_ID 跳过资产分支）。返回值忽略，用 records 列表反查 id。

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*.TransactionDaoLogicTest" --offline --no-daemon --console=plain`
Expected: FAIL（旧 insert 用 `recordAmount - Σ被吸收 finalAmount`，E.fa 被设 0、I.fa=8000-? ≠ 净自付）

- [ ] **Step 3: 改造 insert 吸收分支**（`TransactionDao.kt:367-376` 替换 `// 更新关联记录的金额` 段）

```kotlin
        if (needRelated && relatedRecordIdList.isNotEmpty()) {
            // 更新关联记录
            insertRelatedRecord(
                relatedRecordIdList.map { relatedRecordId ->
                    RecordWithRelatedTable(
                        id = null,
                        recordId = recordId,
                        relatedRecordId = relatedRecordId,
                    )
                },
            )
            // 净自付：对新吸收者所在簇整体重算（替换旧的 recordAmount - Σ finalAmount 口径）
            recalculateFinalAmountForCluster(recordId)
        }
```

- [ ] **Step 4: 运行测试确认通过**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*.TransactionDaoLogicTest" --offline --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 提交**

```bash
git add core/database core/data
git commit -m "[refactor|core|finalAmount净额][公共]insertRecordTransaction 吸收分支改净自付簇重算"
```

---

## Task 5: delete 分支改造 → 簇算法（含恢复全额/剩余吸收者）

**Files:**
- Modify: `core/database/.../dao/TransactionDao.kt:524-554`（delete 的 EXPENDITURE/INCOME 分支）
- Test: `core/data/test/.../TransactionDaoLogicTest.kt`（删支出后吸收者恢复 / 删吸收者后支出恢复全额）

- [ ] **Step 1: 写失败测试**

```kotlin
// ========== deleteRecordTransaction 净自付测试 ==========
@Test
fun when_delete_absorber_income_then_expense_restores_full() = runTest {
    // I(80) 吸收 E(100)：删 I → E 恢复全额 100
    setupTypesForAbsorption()
    insertRecord(id = 1L, typeId = EXPENDITURE_TYPE_ID, amount = 10000L)
    val income = insertRecord(id = 2L, typeId = INCOME_TYPE_ID, amount = 8000L)
    dao.relatedRecords.add(RecordWithRelatedTable(id = 1L, recordId = 2L, relatedRecordId = 1L))
    dao.recalculateFinalAmountForCluster(2L)  // 先建立净自付：E.fa=20

    dao.deleteRecordTransaction(income)

    assertThat(dao.queryRecordById(1L)!!.finalAmount).isEqualTo(10000L)  // 恢复全额
    assertThat(dao.queryRecordById(2L)).isNull()
}

@Test
fun when_delete_expense_then_remaining_absorber_recalc() = runTest {
    // E(100) 被 I1(30),I2(40)：删 E → I1,I2 恢复各自 recordAmount
    setupTypesForAbsorption()
    val expense = insertRecord(id = 1L, typeId = EXPENDITURE_TYPE_ID, amount = 10000L)
    insertRecord(id = 2L, typeId = INCOME_TYPE_ID, amount = 3000L)
    insertRecord(id = 3L, typeId = INCOME_TYPE_ID, amount = 4000L)
    dao.relatedRecords.add(RecordWithRelatedTable(id = 1L, recordId = 2L, relatedRecordId = 1L))
    dao.relatedRecords.add(RecordWithRelatedTable(id = 2L, recordId = 3L, relatedRecordId = 1L))
    dao.recalculateFinalAmountForCluster(1L)  // E.fa=30,I1.fa=0,I2.fa=0

    dao.deleteRecordTransaction(expense)

    assertThat(dao.queryRecordById(1L)).isNull()
    assertThat(dao.queryRecordById(2L)!!.finalAmount).isEqualTo(3000L)  // 恢复 recordAmount
    assertThat(dao.queryRecordById(3L)!!.finalAmount).isEqualTo(4000L)
}
```

> 注：`deleteRecordTransaction(record)` 是 `@Transaction` 默认方法，Fake 继承（**未** override，区别于 `deleteBookTransaction`/`deleteAssetRelatedData` 的简化 override）。assetId=-1L 跳过资产分支。

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*.TransactionDaoLogicTest" --offline --no-daemon --console=plain`
Expected: 删支出场景：旧 INCOME 删分支逻辑与簇算法在「剩余吸收者」口径不一致，断言失败

- [ ] **Step 3: 改造 delete INCOME 分支**（`TransactionDao.kt:531-553`，整体替换 `else if (category == ... INCOME)` 块）

```kotlin
        } else if (category == RecordTypeCategoryEnum.INCOME) {
            // 删除收入（吸收者）：对其曾吸收的每个支出所在簇重算（排除即将删除的吸收者）
            val absorbedRelations = queryRelatedByRecordId(recordId)
            for (relation in absorbedRelations) {
                recalculateFinalAmountForCluster(
                    seedRecordId = relation.relatedRecordId,
                    excludeRecordIds = setOf(recordId),
                )
            }
        }
```

> EXPENDITURE 分支（`:525-530`）**无需改**——它已调 `recalculateAbsorberFinalAmount(absorber, excludeAbsorbedId=recordId)`，Task 3 后该方法委托簇算法，自动正确。

- [ ] **Step 4: 补 H4 测试（update 断关联，spec §10/H4）**

`updateRecordTransaction` = `deleteRecordTransaction`(默认) + `insertRecordTransaction`(默认)，Fake 均继承。编辑被吸收支出时 delete 会 `clearRelatedRecordById` 清掉「吸收者→该支出」关联、insert 重建的是支出本身（无关联），故吸收链断开、两者各自恢复 recordAmount。本测试固化该行为：

```kotlin
@Test
fun when_update_absorbed_expense_then_relation_broken_both_standalone() = runTest {
    // I(80) 吸收 E(100)（净自付 E.fa=20）；编辑 E 金额为 120 → 关联断开，E 独立 120，I 独立 80
    setupTypesForAbsorption()
    insertRecord(id = 1L, typeId = EXPENDITURE_TYPE_ID, amount = 10000L)
    insertRecord(id = 2L, typeId = INCOME_TYPE_ID, amount = 8000L)
    dao.relatedRecords.add(RecordWithRelatedTable(id = 1L, recordId = 2L, relatedRecordId = 1L))
    dao.recalculateFinalAmountForCluster(2L)

    // 编辑支出（delete+insert，沿用 id=1）
    dao.updateRecordTransaction(
        record = createRecordTable(id = 1L, typeId = EXPENDITURE_TYPE_ID, amount = 12000L),
        tagIdList = emptyList(), needRelated = false,
        relatedRecordIdList = emptyList(), relatedImageList = emptyList(),
    )

    // 关联断开（清除），E 独立 recordAmount=120，I 恢复独立 recordAmount=80
    assertThat(dao.relatedRecords).isEmpty()
    assertThat(dao.queryRecordById(1L)!!.finalAmount).isEqualTo(12000L)
    assertThat(dao.queryRecordById(2L)!!.finalAmount).isEqualTo(8000L)
}
```

- [ ] **Step 5: 运行测试确认通过**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*.TransactionDaoLogicTest" --offline --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: 提交**

```bash
git add core/database core/data
git commit -m "[refactor|core|finalAmount净额][公共]deleteRecordTransaction INCOME 分支改净自付簇重算 + H4 update 断关联测试"
```

---

## Task 6: 全量重算 `recalculateAllFinalAmount`（全表净自付 + 转账保持）

**Files:**
- Modify: `core/database/.../dao/TransactionDao.kt`（新增 `recalculateAllFinalAmount`）
- Test: `core/data/test/.../TransactionDaoLogicTest.kt`（全量 + 幂等 + 增量一致）

- [ ] **Step 1: 写失败测试**

```kotlin
// ========== recalculateAllFinalAmount 全量测试 ==========
@Test
fun when_recalcAll_then_all_clusters_net_self_paid_and_idempotent() = runTest {
    setupTypesForAbsorption()
    // 簇A：E1(100) 被 I1(80)；簇B：独立支出 E2(50)；转账 T(amount200,charge5,concession2)
    insertRecord(id = 1L, typeId = EXPENDITURE_TYPE_ID, amount = 10000L)
    insertRecord(id = 2L, typeId = INCOME_TYPE_ID, amount = 8000L)
    insertRecord(id = 3L, typeId = EXPENDITURE_TYPE_ID, amount = 5000L)
    insertRecord(id = 4L, typeId = TRANSFER_TYPE_ID, amount = 20000L, charge = 500L, concessions = 200L)
    dao.relatedRecords.add(RecordWithRelatedTable(id = 1L, recordId = 2L, relatedRecordId = 1L))

    dao.recalculateAllFinalAmount()

    assertThat(dao.queryRecordById(1L)!!.finalAmount).isEqualTo(2000L)   // 净自付 100-80
    assertThat(dao.queryRecordById(2L)!!.finalAmount).isEqualTo(0L)      // 溢出 0
    assertThat(dao.queryRecordById(3L)!!.finalAmount).isEqualTo(5000L)   // 独立支出全额
    assertThat(dao.queryRecordById(4L)!!.finalAmount).isEqualTo(-300L)   // 转账 concessions-charge=200-500

    // 幂等：连跑两次结果一致
    dao.recalculateAllFinalAmount()
    assertThat(dao.queryRecordById(1L)!!.finalAmount).isEqualTo(2000L)
    assertThat(dao.queryRecordById(2L)!!.finalAmount).isEqualTo(0L)
}

@Test
fun when_recalcAll_equals_incremental_cluster_result() = runTest {
    // M6/§5：全量与增量逐字段一致
    setupTypesForAbsorption()
    insertRecord(id = 1L, typeId = EXPENDITURE_TYPE_ID, amount = 10000L)
    insertRecord(id = 2L, typeId = EXPENDITURE_TYPE_ID, amount = 5000L)
    insertRecord(id = 3L, typeId = INCOME_TYPE_ID, amount = 8000L)
    dao.relatedRecords.add(RecordWithRelatedTable(id = 1L, recordId = 3L, relatedRecordId = 1L))
    dao.relatedRecords.add(RecordWithRelatedTable(id = 2L, recordId = 3L, relatedRecordId = 2L))

    dao.recalculateAllFinalAmount()
    val fullA = dao.queryRecordById(1L)!!.finalAmount
    val fullB = dao.queryRecordById(2L)!!.finalAmount
    val fullC = dao.queryRecordById(3L)!!.finalAmount

    // 增量重算同簇
    dao.recalculateFinalAmountForCluster(3L)
    assertThat(dao.queryRecordById(1L)!!.finalAmount).isEqualTo(fullA)
    assertThat(dao.queryRecordById(2L)!!.finalAmount).isEqualTo(fullB)
    assertThat(dao.queryRecordById(3L)!!.finalAmount).isEqualTo(fullC)
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*.TransactionDaoLogicTest" --offline --no-daemon --console=plain`
Expected: 编译失败 `unresolved reference: recalculateAllFinalAmount`

- [ ] **Step 3: 实现 `recalculateAllFinalAmount`**（`TransactionDao.kt`，加 `@Transaction`，放 `recalculateFinalAmountForCluster` 之后）

```kotlin
/**
 * 全表净自付重算（迁移 / 启动 gate / 备份恢复后复用）。
 *
 * - 转账：finalAmount = concessions - charge（保持既有 migrate 语义，转账不参与吸收）
 * - 收入/支出：先初始化 recordAmount，再按全表吸收者 id 升序顺序贪心填充（§5）
 *
 * 与 [recalculateFinalAmountForCluster] 同算法同序，全量与增量结果一致；幂等（连跑结果一致）。
 */
@Transaction
suspend fun recalculateAllFinalAmount() {
    val allRecords = queryAllRecords()
    val allRelations = queryAllRelatedRecords()

    // 1. 初始化 finalAmount
    val finalAmounts = HashMap<Long, Long>(allRecords.size)
    for (record in allRecords) {
        val id = record.id ?: continue
        val type = resolveType(record.typeId) ?: continue
        val category = RecordTypeCategoryEnum.ordinalOf(type.typeCategory)
        finalAmounts[id] = if (category == RecordTypeCategoryEnum.TRANSFER) {
            record.concessions - record.charge
        } else {
            calculateRecordAmount(record, category)
        }
    }

    // 2. 吸收者 id 升序，被吸收支出 id 升序，顺序贪心填充
    val absorbedByAbsorber = allRelations.groupBy({ it.recordId }, { it.relatedRecordId })
    for (absorberId in absorbedByAbsorber.keys.sorted()) {
        var remaining = finalAmounts[absorberId] ?: continue
        for (expenseId in (absorbedByAbsorber[absorberId] ?: emptyList()).sorted()) {
            val current = finalAmounts[expenseId] ?: continue
            val offset = minOf(remaining, current)
            finalAmounts[expenseId] = current - offset
            remaining -= offset
        }
        finalAmounts[absorberId] = remaining
    }

    // 3. 落库（仅写变化项）
    for (record in allRecords) {
        val id = record.id ?: continue
        val finalAmount = finalAmounts[id] ?: continue
        if (record.finalAmount != finalAmount) {
            updateRecordFinalAmountById(id, finalAmount)
        }
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*.TransactionDaoLogicTest" --offline --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 提交**

```bash
git add core/database core/data
git commit -m "[feat|core|finalAmount净额][公共]TransactionDao 全表净自付重算 recalculateAllFinalAmount(转账保持 concessions-charge)"
```

---

## Task 7: 改造 `migrateAfter9To10` + 新增 `recalculateAllFinalAmount` 仓库方法

**Files:**
- Modify: `core/data/.../repository/RecordRepository.kt:136`（接口新增方法）
- Modify: `core/data/.../repository/impl/RecordRepositoryImpl.kt:499-549`（改造 migrate + 实现新方法）
- Modify: `core/testing/.../repository/FakeRecordRepository.kt:297-299`（override 新方法）
- Test: `core/data` 现有 `RecordRepositoryImplTest`（若有）或新增针对 migrate 改造的测试

- [ ] **Step 1: 接口新增方法**（`RecordRepository.kt:136` 附近）

```kotlin
    suspend fun migrateAfter9To10()

    /** 全表净自付重算（迁移 gate / 备份恢复复用），完成后置 finalAmountNetRecalcDone 标记 */
    suspend fun recalculateAllFinalAmount()
```

- [ ] **Step 2: 写失败测试**（若 `RecordRepositoryImplTest` 不存在则新建，验证改造后 migrate 产出净自付 + 置标记）

```kotlin
@Test
fun when_recalculateAllFinalAmount_then_dao_recalc_and_marker_set() = runTest {
    // 经真实 RecordRepositoryImpl + FakeTransactionDao + FakeCombineProtoDataSource 验证净自付 + 标记
    // 簇：E(100) 被 I(80)
    fakeTransactionDao.types.add(createType(EXPENDITURE_TYPE_ID, RecordTypeCategoryEnum.EXPENDITURE))
    fakeTransactionDao.types.add(createType(INCOME_TYPE_ID, RecordTypeCategoryEnum.INCOME))
    fakeTransactionDao.records.add(createRecord(1L, EXPENDITURE_TYPE_ID, 10000L))
    fakeTransactionDao.records.add(createRecord(2L, INCOME_TYPE_ID, 8000L))
    fakeTransactionDao.relatedRecords.add(RecordWithRelatedTable(1L, 2L, 1L))

    repository.recalculateAllFinalAmount()

    assertThat(fakeTransactionDao.queryRecordById(1L)!!.finalAmount).isEqualTo(2000L)
    assertThat(fakeCombineProtoDataSource.tempKeysData.first().finalAmountNetRecalcDone).isTrue()
}
```

> 若 `core/data` 无现成 `RecordRepositoryImpl` 单测脚手架（实证：`RecordRepositoryImpl` 依赖 RecordDao/TransactionDao/CombineProtoDataSource/CoroutineContext），用 `FakeTransactionDao` + `FakeCombineProtoDataSource` + `RecordDao` 的最小 fake 构造。如脚手架成本过高，本 Task 的测试可降级为「DAO 层 Task 6 已覆盖算法 + 仅手验标记置位」，并在 plan 勘误说明。

- [ ] **Step 3: 改造 `migrateAfter9To10` + 实现新方法**（`RecordRepositoryImpl.kt:499-549` 整段替换）

```kotlin
    override suspend fun migrateAfter9To10() = withContext(coroutineContext) {
        // 改造（H1）：统一走净自付全量重算，替换旧吸收语义逐字段计算。
        // 转账由 recalculateAllFinalAmount 内部保持 concessions-charge。
        transactionDao.recalculateAllFinalAmount()
        this@RecordRepositoryImpl.logger().i("migrateAfter9To10(), net self-paid recalc done")
        // 标记两个迁移标志（净自付重算已含旧 db9To10 全量重算职责，避免首启重复跑）
        combineProtoDataSource.updateDb9To10DataMigrated(true)
        combineProtoDataSource.updateFinalAmountNetRecalcDone(true)
        recordDataVersion.updateVersion()
    }

    override suspend fun recalculateAllFinalAmount() = withContext(coroutineContext) {
        transactionDao.recalculateAllFinalAmount()
        combineProtoDataSource.updateFinalAmountNetRecalcDone(true)
        recordDataVersion.updateVersion()
    }
```

> 删除旧 migrate 体内对 `recordDao.queryByTypeCategory`/`queryRelatedRecord`/`updateRecord(list)` 的全部引用（净自付重算由 TransactionDao 内聚完成）。检查这些 RecordDao 方法是否仍被他处引用——若仅 migrate 用且无其他引用，保留定义不删（避免误删波及，YAGNI）。

- [ ] **Step 4: FakeRecordRepository override**（`FakeRecordRepository.kt:297-299`）

```kotlin
    override suspend fun migrateAfter9To10() {
        // no-op
    }

    override suspend fun recalculateAllFinalAmount() {
        // no-op（feature/domain 层测试不触发真实重算）
    }
```

- [ ] **Step 5: 运行测试确认通过**

Run: `./gradlew :core:data:testDebugUnitTest --offline --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL（含 core:testing 编译——FakeRecordRepository 新 override）

- [ ] **Step 6: 提交**

```bash
git add core/data core/testing
git commit -m "[refactor|core|finalAmount净额][公共]migrateAfter9To10 改净自付全量重算 + 新增 recalculateAllFinalAmount 仓库方法"
```

---

## Task 8: `LauncherContentViewModel` 新增 `finalAmountNetRecalcDone` gate

**Files:**
- Modify: `feature/records/.../viewmodel/LauncherContentViewModel.kt:67-75`
- Test: `feature/records` 现有 `LauncherContentViewModelTest`（若有则补；ViewModel init 协程触发）

- [ ] **Step 1: 写失败测试**（若 `LauncherContentViewModelTest` 存在，补一例；用 FakeSettingRepository + FakeRecordRepository 验证 gate 调用）

```kotlin
@Test
fun when_net_recalc_not_done_then_recalculateAllFinalAmount_called() = runTest {
    // FakeSettingRepository.tempKeysModel 默认 finalAmountNetRecalcDone=false
    // FakeRecordRepository 记录 recalculateAllFinalAmount 是否被调用
    val recordRepository = RecordingFakeRecordRepository()  // 计数 override
    val vm = LauncherContentViewModel(booksRepository, settingRepository, recordRepository, useCase)
    advanceUntilIdle()
    assertThat(recordRepository.recalculateAllFinalAmountCount).isEqualTo(1)
}
```

> 若 feature:records 无 ViewModel 测试脚手架/init 协程难测（实证踩坑：Robolectric+Compose 交互限制），本 Task 测试可降级为编译通过 + 人工核验 gate 逻辑，并在 plan 标注。gate 逻辑本身极简（一个 if），回归风险低。

- [ ] **Step 2: 改造 VM init gate**（`LauncherContentViewModel.kt:67-75`）

```kotlin
    init {
        viewModelScope.launch {
            val tempKeys = settingRepository.tempKeysModel.first()
            if (!tempKeys.db9To10DataMigrated) {
                // 改造后 migrateAfter9To10 内部已置 finalAmountNetRecalcDone，新用户不重复跑
                recordRepository.migrateAfter9To10()
            } else if (!tempKeys.finalAmountNetRecalcDone) {
                // 老用户（已迁移 db9To10、但未做净自付重算）：触发一次净自付全量重算
                recordRepository.recalculateAllFinalAmount()
            }
            _migrationCompleted.value = true
        }
    }
```

> `else if`：新用户走 migrate（已含净自付 + 置两标记）→ 不重复；老用户走净自付重算。两分支互斥、顺序确定。

- [ ] **Step 3: 运行测试确认通过**

Run: `./gradlew :feature:records:testOnlineDebugUnitTest --offline --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 提交**

```bash
git add feature/records
git commit -m "[feat|feature|finalAmount净额][公共]LauncherContentViewModel 新增净自付重算 gate(老用户触发一次)"
```

---

## Task 9: 备份恢复后同步净自付重算（H3）

**Files:**
- Modify: `core/data/.../uitl/impl/BackupRecoveryManagerImpl.kt:682-697`（恢复成功分支，置 SUCCESS 前）
- Test: 该类有 androidTest/集成测试则补；否则代码评审 + 手验

- [ ] **Step 1: 在恢复成功分支同步重算**（`BackupRecoveryManagerImpl.kt:693` 三个 `updateXxxTypeId(0L)` 之后、`BackupRecoveryState.SUCCESS_RECOVERY` 之前插入）

```kotlin
                // 重置迁移标志，下次启动时自动触发应用层迁移
                combineProtoDataSource.updateRefundTypeId(0L)
                combineProtoDataSource.updateReimburseTypeId(0L)
                combineProtoDataSource.updateCreditCardPaymentTypeId(0L)
                // 净自付（H3）：恢复是 CONFLICT_REPLACE 合并、恢复后无进程重启，
                // 合并后 finalAmount 为「备份旧语义行 + 当前库新语义行」混合，须同步对全表重算覆盖
                database.transactionDao().recalculateAllFinalAmount()
                BackupRecoveryState.SUCCESS_RECOVERY
```

> `database: CashbookDatabase` 已注入（`:87`），`transactionDao()` 已暴露（`CashbookDatabase.kt:73`），无需新增注入。`recalculateAllFinalAmount` 在 recoveryFromDb 合并完成后对同一 Room 库执行（合并经 raw SQLite，Room DAO 读同底层库）。
> 标记 `finalAmountNetRecalcDone` 此处**不重置**——同步重算后数据已正确，标记保持 true，下次启动 gate 跳过（无冗余重跑）。

- [ ] **Step 2: 编译验证 + 现有备份恢复测试不回归**

Run: `./gradlew :core:data:testDebugUnitTest --offline --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add core/data
git commit -m "[fix|core|finalAmount净额][公共]备份恢复成功后同步全表净自付重算(H3 合并后混合语义覆盖)"
```

---

## Task 10: Analytics 饼图改净自付（决策 #7 / H2）

**Files:**
- Modify: `core/domain/.../usecase/TransRecordViewsToAnalyticsPieUseCase.kt:49,71`
- Modify: `core/domain/.../usecase/TransRecordViewsToAnalyticsPieSecondUseCase.kt:48,62`
- Test: `core/domain` 现有 Pie usecase 测试（补净自付算例）

- [ ] **Step 1: 写失败测试**（在现有 Pie usecase 测试补一例：被吸收支出按净自付计入分类占比）

```kotlin
@Test
fun when_pie_with_absorbed_expense_then_uses_net_self_paid_finalAmount() = runTest {
    // 餐饮支出 E(100) 净自付 20（finalAmount=2000），饼图餐饮分类应计 20 而非 100
    val records = listOf(
        recordViews(typeId = 1L, typeCategory = EXPENDITURE, amount = 10000L, finalAmount = 2000L),
    )
    val result = useCase(RecordTypeCategoryEnum.EXPENDITURE, records)
    assertThat(result.first().totalAmount).isEqualTo(2000L)
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew :core:domain:testDebugUnitTest --tests "*Pie*" --offline --no-daemon --console=plain`
Expected: FAIL（旧 `analyticsPieAmount(原始 amount)` 算 10000，断言期望 2000）

- [ ] **Step 3: 两个 usecase 改用 `record.finalAmount`**

`TransRecordViewsToAnalyticsPieUseCase.kt:49`：
```kotlin
            total += record.finalAmount
```
`:71`：
```kotlin
                        typeTotal += it.finalAmount
```
`TransRecordViewsToAnalyticsPieSecondUseCase.kt:48`：
```kotlin
            total += record.finalAmount
```
`:62`：
```kotlin
                        typeTotal += it.finalAmount
```

> 两文件删除 `import ...analyticsPieAmount`（改后两文件无其他引用）。TRANSFER 被 `it.type.typeCategory == typeCategory` 过滤不入饼图，无需特殊处理。
> **已 grep 确认**：`analyticsPieAmount` 全仓仅这 2 个 usecase 引用 + `RecordAmount.kt` 定义。本 Task 改后它成 dead code。**保留定义不删**（YAGNI 避免 churn；`RecordAmount.kt` 注释已说明两口径区别，且其单测若存在一并保留）。

- [ ] **Step 4: 运行测试确认通过**

Run: `./gradlew :core:domain:testDebugUnitTest --tests "*Pie*" --offline --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 提交**

```bash
git add core/domain
git commit -m "[refactor|core|finalAmount净额][公共]Analytics 饼图两 usecase 改净自付(record.finalAmount)"
```

---

## Task 11: 列表 C 净额展示 + 去删除线（`RecordListItem`）

> 单一 composable，影响 6 屏 7 处。改：被吸收支出主金额显净自付 finalAmount、去整额删除线；INCOME 吸收者显实收 amount（决策 #5）。

**Files:**
- Modify: `feature/records/.../screen/LauncherContentScreen.kt:731-753`
- Test: `feature/records` 截图测试（`LauncherContentScreenScreenshotTests`/`SearchScreenScreenshotTests`/`AssetInfoContentScreenScreenshotTests` 等）+ 录制基准

- [ ] **Step 1: 改 displayAmount + 去删除线**（`LauncherContentScreen.kt:731-753` 替换）

```kotlin
                    val displayAmount = when {
                        item.typeCategory == RecordTypeCategoryEnum.TRANSFER -> {
                            item.amount
                        }
                        // INCOME 吸收者（报销/退款款）：显实收额 amount（聚合用 finalAmount 溢出不虚增）
                        item.typeCategory == RecordTypeCategoryEnum.INCOME &&
                            item.relatedRecord.isNotEmpty() -> {
                            item.amount
                        }
                        // 其它（含被吸收支出）：显净自付 finalAmount
                        else -> item.finalAmount
                    }
                    Text(
                        text = displayAmount.toMoneyCNY(),
                        color = item.typeCategory.typeColor,
                        style = MaterialTheme.typography.labelLarge,
                    )
```

> 删除 `isReimbursed` 变量与 `textDecoration = if (isReimbursed) LineThrough else None`——净自付下不再整额划删除线（已有「已报销(¥80)」标签表达，`:710-722` 保留不动）。

- [ ] **Step 2: 检查截图/ViewModel 测试编译**

Run: `./gradlew :feature:records:testOnlineDebugUnitTest --offline --no-daemon --console=plain`
Expected: 截图测试因像素变化 FAIL（删除线消失 + 金额变化），但**编译通过**。若编译失败（签名漂移）先修编译。

- [ ] **Step 3: 录制新截图基准**

Run: `./gradlew :feature:records:recordRoborazziOnlineDebug --offline --no-daemon --console=plain`
（涉及 6 屏共用 composable，确认 home/search/asset-info/calendar/select-related/typed-analytics 截图按净额更新）
Expected: 基准图重生成

- [ ] **Step 4: 校验截图测试通过**

Run: `./gradlew :feature:records:verifyRoborazziOnlineDebug --offline --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 提交**

```bash
git add feature/records
git commit -m "[feat|feature|finalAmount净额][公共]列表 C 净额:被吸收支出显净自付去删除线 + 吸收者显实收(6 屏共用) + 截图基准"
```

---

## Task 12: androidTest 真实 Room 净自付 + 资产不变性（M5/L8）

> `core/database` androidTest 为 instrumented（需模拟器/真机）。真实 Room DAO 净自付端到端校验 + verifyAssetBalance 重构前后不变。

**Files:**
- Modify: `core/database/androidTest/.../dao/TransactionDaoTest.kt:507-546`（核验 `when_insertRecordWithRelated` 是否需调整——income>支出场景不变；新增 income<支出场景）
- Test: 同文件新增净自付 instrumented 用例 + verifyAssetBalance 不变性

- [ ] **Step 1: 新增 instrumented 测试**（`TransactionDaoTest.kt`，复用本文件既有 helper：`insertBook`/`insertType`/`createExpenditureType`/`createIncomeType`/`createNormalAsset`/`insertAsset`/`createRecord`）

```kotlin
@Test
fun when_insertRecordWithRelated_partial_then_net_self_paid() = runTest {
    // E(100) 被 I(80) 部分吸收 → E.finalAmount=20, I.finalAmount=0（真实 Room）
    insertBook()
    val expTypeId = insertType(createExpenditureType())
    val incTypeId = insertType(createIncomeType())
    val expenseId = transactionDao.insertRecord(createRecord(typeId = expTypeId, amount = 10000L))
    transactionDao.insertRecordTransaction(
        record = createRecord(typeId = incTypeId, amount = 8000L),
        tagIdList = emptyList(), needRelated = true,
        relatedRecordIdList = listOf(expenseId), relatedImageList = emptyList(),
    )
    assertThat(transactionDao.queryRecordById(expenseId)!!.finalAmount).isEqualTo(2000L)
}

@Test
fun when_recalculateAllFinalAmount_then_asset_balance_unchanged() = runTest {
    // L8：recalcAll 只改 finalAmount，不动 asset.balance（余额全程用 recordAmount 口径，与 finalAmount 解耦）
    insertBook()
    val expTypeId = insertType(createExpenditureType())
    val incTypeId = insertType(createIncomeType())
    insertAsset(createNormalAsset(id = 10L, balance = 100000L))
    val expenseId = transactionDao.insertRecord(createRecord(typeId = expTypeId, assetId = 10L, amount = 10000L))
    transactionDao.insertRecordTransaction(
        record = createRecord(typeId = incTypeId, assetId = 10L, amount = 8000L),
        tagIdList = emptyList(), needRelated = true,
        relatedRecordIdList = listOf(expenseId), relatedImageList = emptyList(),
    )
    val balanceBefore = transactionDao.queryAssetById(10L)!!.balance

    transactionDao.recalculateAllFinalAmount()

    assertThat(transactionDao.queryAssetById(10L)!!.balance).isEqualTo(balanceBefore)
}

@Test
fun when_large_cluster_50_expenses_one_absorber_then_completes() = runTest {
    // M8：50 笔支出关联 1 报销款，recalculateFinalAmountForCluster 单事务内完成、结果守恒
    insertBook()
    val expTypeId = insertType(createExpenditureType())
    val incTypeId = insertType(createIncomeType())
    val expenseIds = (1..50).map {
        transactionDao.insertRecord(createRecord(typeId = expTypeId, amount = 1000L))
    }
    val absorberId = transactionDao.insertRecord(createRecord(typeId = incTypeId, amount = 100000L))
    transactionDao.insertRelatedRecord(
        expenseIds.map { RecordWithRelatedTable(id = null, recordId = absorberId, relatedRecordId = it) },
    )

    transactionDao.recalculateFinalAmountForCluster(absorberId)

    // 50×10=500 元 < 吸收者 1000 元：全部支出净自付归 0，吸收者溢出 50000 分
    assertThat(expenseIds.all { transactionDao.queryRecordById(it)!!.finalAmount == 0L }).isTrue()
    assertThat(transactionDao.queryRecordById(absorberId)!!.finalAmount).isEqualTo(50000L)
}

@Test
fun when_delete_book_with_absorption_cluster_then_records_cleared_others_intact() = runTest {
    // M4：删账本逐条删记录走真实 deleteRecordTransaction，含吸收簇整簇重算不脏读
    val bookA = insertBook(id = 1L, name = "A")
    val bookB = insertBook(id = 2L, name = "B")
    val expTypeId = insertType(createExpenditureType())
    val incTypeId = insertType(createIncomeType())
    // bookA：E 被 I 吸收
    val expenseId = transactionDao.insertRecord(createRecord(typeId = expTypeId, booksId = bookA, amount = 10000L))
    transactionDao.insertRecordTransaction(
        record = createRecord(typeId = incTypeId, booksId = bookA, amount = 8000L),
        tagIdList = emptyList(), needRelated = true,
        relatedRecordIdList = listOf(expenseId), relatedImageList = emptyList(),
    )
    // bookB：独立支出
    val bRecordId = transactionDao.insertRecord(createRecord(typeId = expTypeId, booksId = bookB, amount = 5000L).copy(finalAmount = 5000L))

    transactionDao.deleteBookTransaction(bookA)

    assertThat(transactionDao.queryRecordListByBookId(bookA)).isEmpty()
    assertThat(transactionDao.queryRecordById(bRecordId)!!.finalAmount).isEqualTo(5000L)  // 其它账本不受影响
}
```

> `when_insertRecordWithRelated`（`:507-546`，income 20000 吸收 5000+3000）净自付下结果 E1=0,E2=0,income=12000 **与旧断言一致**，保留不改。

- [ ] **Step 2: 运行 instrumented 测试**（需连接模拟器/真机）

Run: `./gradlew :core:database:connectedDebugAndroidTest --offline --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL（无设备则跳过本 Task instrumented 部分，依赖 Task 2-6 的 JVM 逻辑测试覆盖算法；plan 标注 androidTest 待有设备时补跑）

- [ ] **Step 3: 提交**

```bash
git add core/database
git commit -m "[test|core|finalAmount净额][公共]TransactionDao androidTest 净自付端到端 + 资产余额不变性(L8)"
```

---

## Task 13: 全量回归 + 详情/搜索守住 + 全链路验证

**Files:**
- 核对: `feature/records/.../view/RecordDetailsSheet.kt:179`（详情「实际金额」已显 finalAmount，数据层改后自动净自付，**无需改值**；文案如需调在此）
- 核对: `core/database/.../dao/RecordDao.kt:268`（按金额搜命中 final_amount，净自付后命中集变化——测试守住，逻辑不改）

- [ ] **Step 1: 详情页核对**——`RecordDetailsSheet.kt:179` `recordData.finalAmount.toMoneyCNY()` 在净自付下自动显示净额（报销款显溢出 0，有上方「金额」行陪衬，决策 #5）。仅在文案不清时微调，否则不动。

- [ ] **Step 2: 搜索测试守住**——确认 `feature/records` 或 `core/data` 中按金额搜索测试在新 final_amount 语义下命中集符合预期（`FakeRecordRepository` 按金额查的桩忠实复刻 `amount=:x OR final_amount=:x`，CLAUDE.md 禁桩）。

- [ ] **Step 3: 全模块回归（完整链路验证）**

Run（逐模块，先暖缓存再 offline）：
```bash
./gradlew :core:data:testDebugUnitTest :core:domain:testDebugUnitTest :feature:records:testOnlineDebugUnitTest --offline --no-daemon --console=plain
```
Expected: 全部 `BUILD SUCCESSFUL`（`grep -E "^BUILD (SUCCESSFUL|FAILED)"` 确认）

- [ ] **Step 4: Spotless 格式**

Run: `./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache --offline --no-daemon`
Expected: 格式化无残留

- [ ] **Step 5: 提交（如有格式/文案变更）**

```bash
git add -A
git commit -m "[test|feature|finalAmount净额][公共]详情/搜索净自付守住 + 全模块回归 + spotless"
```

---

## 测试命令速查（CLAUDE.md 校准）

| 模块 | 任务名 | 备注 |
|---|---|---|
| `core:data`（含 TransactionDaoLogicTest/SettingRepositoryImplTest） | `:core:data:testDebugUnitTest` | Android 库 |
| `core:domain`（Pie usecase） | `:core:domain:testDebugUnitTest` | Android 库 |
| `feature:records`（VM/截图） | `:feature:records:testOnlineDebugUnitTest` | 有 flavor |
| `core:database`（真实 Room） | `:core:database:connectedDebugAndroidTest` | **instrumented 需设备** |
| 截图录制/校验 | `:feature:records:recordRoborazziOnlineDebug` / `verifyRoborazziOnlineDebug` | Roborazzi |

> 本机经代理拉 Maven Central：缺依赖时带 `-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897` 暖缓存一次，之后 `--offline`。后台 Gradle `exit 0` ≠ SUCCESSFUL，只信 `grep -E "^BUILD (SUCCESSFUL|FAILED)"`。

## 风险与回滚（spec §12）

- 数据层核心改造，回归面：增删改 + 6 透传消费方 + 饼图 + 搜索 + 全量重算 + 备份恢复 + 资产余额不变性。
- **回滚**：`git revert` 各 Task commit。回退后用户库为净自付值，需走旧 `migrateAfter9To10`（git 历史中旧吸收语义版）重算回旧语义——清 `db9To10DataMigrated` 或点设置页「数据迁移」按钮触发；`finalAmountNetRecalcDone` 标记需一并处理（手动清或随 proto 字段回退）。**不声称无脑可恢复任一语义**。

## 节点 2 评审（开发完成后，交付前）

全部 Task 完成 + 完整链路验证通过后，按 CLAUDE.md「Agent Team 评审」节点 2 调用 `comprehensive-review:full-review` 对本次 git diff 做多维终审（架构/安全/性能/测试/best-practices），Critical/High 修复后再交付。
