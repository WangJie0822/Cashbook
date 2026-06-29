# 信用卡品牌卡 N1 提醒修复 + 图片备份 backlog 清理 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复银行品牌信用卡收不到 N1 提醒的 bug，并清理图片迁文件系统/备份恢复的 full-review Low backlog（删文件对称、孤儿扫描节流、live VACUUM、STORED、integrity gate、白名单、catalog）+ 补恢复端到端 androidTest。

**Architecture:** 单 worktree 隔离、controller 串行 TDD。每项原子 commit。纯逻辑抽 top-level `internal fun` JVM 单测；DB/文件真实语义落 androidTest；跨模块 Hilt 由 `:app:compileOnlineDebugKotlin` 验。

**Tech Stack:** Kotlin / Room / Proto DataStore / Hilt / JUnit + Truth / Robolectric / androidTest(instrumented)。

## Global Constraints

- 金额单位分（本批不涉金额，不触碰金额口径）。
- 本期**不删 `db_image_with_related.image_bytes` BLOB 列、不改 Room schema、DB_VERSION 不变（14）**。
- DAO 新增抽象方法必须同步唯一 `FakeRecordDao`（`core/data/src/test/.../testdoubles/FakeRecordDao.kt`），否则 `:core:data:compileDebugUnitTestKotlin` 失败。
- 测试替身忠实复刻真实 DAO/SQL 匹配语义，禁 `emptyList()`/宽松桩。
- feature/core 模块测试任务 `:<mod>:testDebugUnitTest`；JVM 库（core:model）`:core:model:test`；app 带 flavor `:app:testOnlineDebugUnitTest` / `:app:compileOnlineDebugKotlin`。
- 截图基线本机不录（CI 管理）。
- 提交信息格式 `[类型|模块|功能][公共]说明`，结尾 `Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>`。
- 构建命令离线优先：`--offline --no-daemon --console=plain`；判 BUILD 结果只信 `grep -E '^BUILD (SUCCESSFUL|FAILED)'`。

---

## Task 概览与依赖

- T1 信用卡 N1 过滤修复（`sync:work`，独立）
- T2 proto 基础设施：`lastOrphanScanMs` + `dbVacuumDone`（B/C 前置）
- T3 DAO 路径投影查询 + FakeRecordDao + androidTest（A 前置）
- T4 A：删资产/删账本删图片文件（依赖 T3）
- T5 A：编辑路径删被移除图片（依赖 T3）
- T6 B：孤儿扫描节流 + 恢复复位（依赖 T2）
- T7 C：DatabaseCompactor 健壮版 live VACUUM（依赖 T2）
- T8 (a) 备份图片 entry STORED
- T9 (b) integrity_check gate
- T10 (c) importSettings 不 apply ignoreUpdateVersion
- T11 (d) org.json 走 version catalog
- T12 D：恢复端到端 androidTest
- 验证收尾：模拟器跑 androidTest + 手动黑盒往返 + 截图基线 CI 说明

---

### Task 1: 信用卡 N1 过滤改用 type==CREDIT_CARD_ACCOUNT

**Files:**
- Modify: `sync/work/src/main/kotlin/cn/wj/android/cashbook/sync/workers/DailyReminderWorker.kt:88-94`
- Create: `sync/work/src/test/kotlin/cn/wj/android/cashbook/sync/reminder/SelectReminderCreditCardsTest.kt`

**Interfaces:**
- Produces: `internal fun selectReminderCreditCards(assets: List<AssetModel>): List<CreditCardReminderInfo>`（top-level，`sync/work` 同包；过滤 `type == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT` + map 到 `CreditCardReminderInfo(id, name, billingDate, repaymentDate)`）。

- [ ] **Step 1: 写失败测试**（先读 `CreditCardReminderInfo` 定义与 `AssetModel` 构造，用 `core:testing` 的 `createAssetModel` 或直接构造确认字段）

```kotlin
// SelectReminderCreditCardsTest.kt（package cn.wj.android.cashbook.sync.reminder）
class SelectReminderCreditCardsTest {
    private fun asset(id: Long, type: ClassificationTypeEnum, cls: AssetClassificationEnum) =
        AssetModel(/* 按实际 AssetModel 必填字段构造，type=type, classification=cls,
                       billingDate="15", repaymentDate="5", invisible=false, … */)

    @Test fun brandCreditCard_included() {
        val list = selectReminderCreditCards(
            listOf(asset(1, ClassificationTypeEnum.CREDIT_CARD_ACCOUNT, AssetClassificationEnum.BANK_CARD_ZS)),
        )
        assertThat(list.map { it.id }).containsExactly(1L)
    }
    @Test fun brandDebitCard_excluded() {
        val list = selectReminderCreditCards(
            listOf(asset(2, ClassificationTypeEnum.CAPITAL_ACCOUNT, AssetClassificationEnum.BANK_CARD_ZS)),
        )
        assertThat(list).isEmpty()
    }
    @Test fun nativeCreditCard_included() {
        val list = selectReminderCreditCards(
            listOf(asset(3, ClassificationTypeEnum.CREDIT_CARD_ACCOUNT, AssetClassificationEnum.OTHER_CREDIT_CARD)),
        )
        assertThat(list.map { it.id }).containsExactly(3L)
    }
    @Test fun ordinaryAsset_excluded() {
        val list = selectReminderCreditCards(
            listOf(asset(4, ClassificationTypeEnum.CAPITAL_ACCOUNT, AssetClassificationEnum.CASH)),
        )
        assertThat(list).isEmpty()
    }
}
```

- [ ] **Step 2: 跑测试确认失败** — `./gradlew :sync:work:testDebugUnitTest --tests "*SelectReminderCreditCardsTest" --offline --no-daemon --console=plain`，Expected: FAIL（`selectReminderCreditCards` 未定义）。
- [ ] **Step 3: 实现纯函数 + 接线 worker**

```kotlin
// DailyReminderWorker.kt 顶层（文件末或同文件 top-level）
/** N1 取数边界：仅当前账本【可见】信用卡大类（type==CREDIT_CARD_ACCOUNT，含银行品牌信用卡）。
 *  隐藏信用卡（invisible）刻意不提醒，与既有 currentVisibleAssetListData 取数一致。 */
internal fun selectReminderCreditCards(assets: List<AssetModel>): List<CreditCardReminderInfo> =
    assets.filter { it.type == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT }
        .map { CreditCardReminderInfo(it.id, it.name, it.billingDate, it.repaymentDate) }
```
```kotlin
// DailyReminderWorker.kt:88-94 改为
val creditCards = if (settings.creditCardReminderEnable) {
    selectReminderCreditCards(assetRepository.currentVisibleAssetListData.first())
} else {
    emptyList()
}
```

- [ ] **Step 4: 跑测试确认通过** + `./gradlew :sync:work:compileDebugKotlin --offline --no-daemon --console=plain`。Expected: PASS / BUILD SUCCESSFUL。
- [ ] **Step 5: spotless + commit**

```bash
./gradlew :sync:work:spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache --offline
git add sync/work/src/main/kotlin/cn/wj/android/cashbook/sync/workers/DailyReminderWorker.kt sync/work/src/test/kotlin/cn/wj/android/cashbook/sync/reminder/SelectReminderCreditCardsTest.kt
git commit -m "[fix|sync:work|信用卡提醒][公共]N1 信用卡过滤改用 type==CREDIT_CARD_ACCOUNT（含银行品牌信用卡，与 EditAsset UI 判据一致）+ selectReminderCreditCards 纯函数 4 用例"
```

---

### Task 2: proto 基础设施 lastOrphanScanMs + dbVacuumDone

**Files:**
- Modify: `core/datastore-proto/src/main/proto/temp_keys.proto`
- Modify: `core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/model/TempKeysModel.kt`
- Modify: `core/datastore/src/main/kotlin/cn/wj/android/cashbook/core/datastore/datasource/CombineProtoDataSource.kt:136-208`
- Modify: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/testdoubles/FakeCombineProtoDataSource.kt`

**Interfaces:**
- Produces: `TempKeysModel.lastOrphanScanMs: Long = 0L`、`TempKeysModel.dbVacuumDone: Boolean = false`；`CombineProtoDataSource.updateLastOrphanScanMs(ms: Long)`、`CombineProtoDataSource.updateDbVacuumDone(done: Boolean)`。

- [ ] **Step 1: 改 proto**（追加末位 field，proto3 默认 0/false 向后兼容）

```protobuf
message TempKeys {
  bool db9To10dataMigrated = 1;
  bool preferenceSplit = 2;
  bool finalAmountNetRecalcDone = 3;
  bool imagesToFilesMigrated = 4;
  int64 lastOrphanScanMs = 5; // 上次孤儿图片扫描时间戳（节流，默认 0=从未扫）
  bool dbVacuumDone = 6;      // backfill 后 live DB VACUUM 是否已成功（仅真成功才置位）
}
```

- [ ] **Step 2: 改 TempKeysModel**（末位带默认值，不破坏现有具名构造点）

```kotlin
data class TempKeysModel(
    val db9To10DataMigrated: Boolean,
    val preferenceSplit: Boolean,
    val finalAmountNetRecalcDone: Boolean = false,
    val imagesToFilesMigrated: Boolean = false,
    val lastOrphanScanMs: Long = 0L,
    val dbVacuumDone: Boolean = false,
)
```

- [ ] **Step 3: 改 CombineProtoDataSource**（`tempKeysData` map 加两字段；加两 setter，仿 `updateImagesToFilesMigrated:206-208`）

```kotlin
// tempKeysData map 内追加
lastOrphanScanMs = it.lastOrphanScanMs,
dbVacuumDone = it.dbVacuumDone,
```
```kotlin
suspend fun updateLastOrphanScanMs(ms: Long) {
    tempKeys.updateData { it.copy { this.lastOrphanScanMs = ms } }
}
suspend fun updateDbVacuumDone(done: Boolean) {
    tempKeys.updateData { it.copy { this.dbVacuumDone = done } }
}
```

- [ ] **Step 4: 同步 FakeCombineProtoDataSource**（读 `core/data/src/test/.../testdoubles/FakeCombineProtoDataSource.kt:290-300` 现有 `updateImagesToFilesMigrated` 桩，补两个对称 setter，作用于 `_tempKeys` MutableStateFlow）

```kotlin
override suspend fun updateLastOrphanScanMs(ms: Long) {
    _tempKeys.update { it.copy(lastOrphanScanMs = ms) }
}
override suspend fun updateDbVacuumDone(done: Boolean) {
    _tempKeys.update { it.copy(dbVacuumDone = done) }
}
```
> 注：若 `FakeCombineProtoDataSource` 不是 override 真类而是独立桩，按其现有模式加方法即可。先读该文件确认 `CombineProtoDataSource` 是 open class 还是被继承。

- [ ] **Step 5: 编译验证**

```bash
./gradlew :core:datastore-proto:compileDebugKotlin :core:model:compileKotlin :core:datastore:compileDebugKotlin :core:data:compileDebugUnitTestKotlin --offline --no-daemon --console=plain
```
Expected: BUILD SUCCESSFUL（无 “not implement abstract member”）。

- [ ] **Step 6: spotless + commit**

```bash
./gradlew :core:datastore-proto:spotlessApply :core:model:spotlessApply :core:datastore:spotlessApply :core:data:spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache --offline
git add core/datastore-proto/src/main/proto/temp_keys.proto core/model/.../TempKeysModel.kt core/datastore/.../CombineProtoDataSource.kt core/data/src/test/.../FakeCombineProtoDataSource.kt
git commit -m "[feat|core:datastore|图片备份][公共]temp_keys 加 lastOrphanScanMs/dbVacuumDone 字段（孤儿扫描节流 + live VACUUM 成功门）+ 全链镜像 setter + Fake 同步"
```

---

### Task 3: DAO 路径投影查询 + FakeRecordDao 忠实复刻 + androidTest

**Files:**
- Modify: `core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/dao/RecordDao.kt`（仿 `queryAllImagePaths:478-480`）
- Modify: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/testdoubles/FakeRecordDao.kt`（仿 `queryAllImagePaths` 实现）
- Modify: `core/database/src/androidTest/kotlin/cn/wj/android/cashbook/core/database/dao/RecordDaoTest.kt`

**Interfaces:**
- Produces: `RecordDao.queryImagePathsByAssetId(assetId: Long): List<String>`、`RecordDao.queryImagePathsByBookId(bookId: Long): List<String>`（均返 `image_path` 投影）。

- [ ] **Step 1: 先读真实删除谓词**：`queryRecordsByAssetId`（`deleteAssetRelatedData` 消费）与 `queryRecordListByBookId`（`deleteBookTransaction` 消费）的 `@Query`，确认 asset 侧含 `asset_id=:assetId OR into_asset_id=:assetId`、book 侧含 `books_id=:bookId`。新查询谓词**逐字镜像**之。

- [ ] **Step 2: 写 androidTest 失败用例**（含负向断言：他 asset/book 图片不返回）

```kotlin
// RecordDaoTest.kt 新增 region（仿现有 21. 图片 backfill 真 SQL）
@Test fun queryImagePathsByAssetId_returnsOnlyThatAssetImages() = runTest {
    // 插入资产A记录(含图 path "record_images/a.jpg") + 资产B记录(含图 "record_images/b.jpg")
    // + 一条 into_asset_id=A 的转账记录(含图 "record_images/t.jpg")
    val paths = recordDao.queryImagePathsByAssetId(assetIdA)
    assertThat(paths).containsExactly("record_images/a.jpg", "record_images/t.jpg")  // 含 into_asset_id 侧
    assertThat(paths).doesNotContain("record_images/b.jpg")                          // 他资产不返回
}
@Test fun queryImagePathsByBookId_returnsOnlyThatBookImages() = runTest {
    val paths = recordDao.queryImagePathsByBookId(bookId1)
    assertThat(paths).containsExactly("record_images/x.jpg")
    assertThat(paths).doesNotContain("record_images/y.jpg")  // 他账本不返回
}
```
> androidTest 本机无设备时此步先写不跑，T12 收尾统一在模拟器跑。

- [ ] **Step 3: 实现 DAO 查询**（mirror 删除谓词，投影 `image_path`）

```kotlin
/** 按资产取图片相对路径（mirror deleteAssetRelatedData→queryRecordsByAssetId 谓词，含 into_asset_id 侧），删资产删文件用 */
@Query("""
    SELECT image_path FROM db_image_with_related
    WHERE record_id IN (SELECT id FROM db_record WHERE asset_id=:assetId OR into_asset_id=:assetId)
""")
suspend fun queryImagePathsByAssetId(assetId: Long): List<String>

/** 按账本取图片相对路径（mirror deleteBookTransaction→queryRecordListByBookId 谓词），删账本删文件用 */
@Query("""
    SELECT image_path FROM db_image_with_related
    WHERE record_id IN (SELECT id FROM db_record WHERE books_id=:bookId)
""")
suspend fun queryImagePathsByBookId(bookId: Long): List<String>
```

- [ ] **Step 4: 同步 FakeRecordDao 忠实复刻**（持有 `records` + `images` 列表，按谓词筛 id 再投影；先读 FakeRecordDao 字段名）

```kotlin
override suspend fun queryImagePathsByAssetId(assetId: Long): List<String> {
    val ids = records.filter { it.assetId == assetId || it.intoAssetId == assetId }.map { it.id }.toSet()
    return images.filter { it.recordId in ids }.map { it.path }
}
override suspend fun queryImagePathsByBookId(bookId: Long): List<String> {
    val ids = records.filter { it.booksId == bookId }.map { it.id }.toSet()
    return images.filter { it.recordId in ids }.map { it.path }
}
```
> 字段名以 FakeRecordDao/RecordTable 实际为准（assetId/intoAssetId/booksId）。

- [ ] **Step 5: 编译验证** — `./gradlew :core:database:compileDebugKotlin :core:data:compileDebugUnitTestKotlin --offline --no-daemon --console=plain`。Expected: BUILD SUCCESSFUL。androidTest 编译：`./gradlew :core:database:compileDebugAndroidTestKotlin --offline --no-daemon --console=plain`。
- [ ] **Step 6: spotless + commit**

```bash
git add core/database/.../RecordDao.kt core/data/src/test/.../FakeRecordDao.kt core/database/src/androidTest/.../RecordDaoTest.kt
git commit -m "[feat|core:database|图片备份][公共]新增 queryImagePathsByAssetId/ByBookId 路径投影（mirror 删除谓词含 into_asset_id）+ FakeRecordDao 忠实复刻 + androidTest 负向断言"
```

---

### Task 4: A — 删资产/删账本同步删图片文件

**Files:**
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordRepositoryImpl.kt:498-503`
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/impl/BooksRepositoryImpl.kt:39-74`
- Test: A 的文件删除行为 JVM 难直测（RecordRepositoryImpl/BooksRepositoryImpl 不实例化）；真实语义靠 T12 androidTest + `deleteManagedImageFiles` 既有单测（已存在 `DeleteManagedImageFilesTest`，若无则补）。

**Interfaces:**
- Consumes: `RecordDao.queryImagePathsByAssetId/ByBookId`（T3）、top-level `deleteManagedImageFiles`（`RecordRepositoryImpl.kt:699`，core:data internal）。

- [ ] **Step 1: 改 deleteRecordsWithAsset**（删前捕获→删 DB→删文件）

```kotlin
override suspend fun deleteRecordsWithAsset(assetId: Long): Unit =
    withContext(coroutineContext) {
        // 删前捕获图片相对路径（删后关联已清无法查）
        val imagePaths = recordDao.queryImagePathsByAssetId(assetId)
        transactionDao.deleteAssetRelatedData(assetId)
        deleteManagedImageFiles(imagePaths, recordImageFileStorage)  // 删 DB 后 best-effort 删文件
        recordDataVersion.updateVersion()
    }
```

- [ ] **Step 2: 改 BooksRepositoryImpl.deleteBook**（注入 RecordDao + RecordImageFileStorage；先读其构造与 import）

```kotlin
class BooksRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    // …现有依赖…
    private val recordDao: RecordDao,
    private val recordImageFileStorage: RecordImageFileStorage,
) : BooksRepository {
    override suspend fun deleteBook(id: Long): Boolean = withContext(coroutineContext) {
        try {
            val imagePaths = recordDao.queryImagePathsByBookId(id)   // 删前捕获
            transactionDao.deleteBookTransaction(id)
            deleteManagedImageFiles(imagePaths, recordImageFileStorage)  // 复用 core:data internal top-level fn
            true
        } catch (throwable: Throwable) {
            this@BooksRepositoryImpl.logger().e(throwable, "deleteBook()")
            false
        }
    }
}
```
> `deleteManagedImageFiles` 是 `...repository.impl` 包顶层 internal，BooksRepositoryImpl 同包可直接调用。

- [ ] **Step 3: 跨模块编译 + Hilt 图验证**

```bash
./gradlew :core:data:compileDebugKotlin :core:data:testDebugUnitTest --offline --no-daemon --console=plain
./gradlew :app:compileOnlineDebugKotlin --offline --no-daemon --console=plain
```
Expected: BUILD SUCCESSFUL（Hilt 已提供 RecordDao/RecordImageFileStorage；无测试直接实例化 BooksRepositoryImpl）。

- [ ] **Step 4: spotless + commit**

```bash
git add core/data/.../RecordRepositoryImpl.kt core/data/.../BooksRepositoryImpl.kt
git commit -m "[fix|core:data|图片备份][公共]删资产/删账本批量删时同步删图片文件（删前路径投影捕获→删 DB→deleteManagedImageFiles），补齐与单删的对称"
```

---

### Task 5: A — 编辑路径删被移除图片

**Files:**
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordRepositoryImpl.kt:86-105`（updateRecord）

**Interfaces:**
- Consumes: `recordDao.queryImagesByRecordId(recordId)`（既有，返 `ImageWithRelatedTable`，取 `.image_path`/`.path`）、`persistNewImages`、`deleteManagedImageFiles`、`storage.isManaged`。

- [ ] **Step 1: 改 updateRecord**（捕获旧托管图 → 持久化 → diff 删被移除）

```kotlin
override suspend fun updateRecord(
    record: RecordModel, tagIdList: List<Long>, needRelated: Boolean,
    relatedRecordIdList: List<Long>, relatedImageList: List<ImageModel>,
) = withContext(coroutineContext) {
    // 编辑前旧托管图路径（用于 diff 删被移除图，补齐编辑路径删文件对称）
    val oldManagedPaths = recordDao.queryImagesByRecordId(record.id)
        .map { it.path }.filter { recordImageFileStorage.isManaged(it) }.toSet()
    val persistedImages = persistNewImages(relatedImageList, recordImageFileStorage)
    transactionDao.updateRecordTransaction(
        record = record.asTable(), tagIdList = tagIdList, needRelated = needRelated,
        relatedRecordIdList = relatedRecordIdList, relatedImageList = persistedImages,
    )
    // 保留集 = 持久化后仍引用的托管图；被移除 = 旧 − 保留 → 删文件（保留图 path 不变，diff 精确）
    val keptPaths = persistedImages.map { it.path }.filter { recordImageFileStorage.isManaged(it) }.toSet()
    deleteManagedImageFiles((oldManagedPaths - keptPaths).toList(), recordImageFileStorage)
    recordDataVersion.updateVersion()
    assetDataVersion.updateVersion()
}
```
> 字段名 `.path` 以 `ImageWithRelatedTable`/`ImageModel` 实际为准（DAO 列 `image_path`，model 多为 `path`）。

- [ ] **Step 2: 编译 + 现有测试不回归**

```bash
./gradlew :core:data:compileDebugKotlin :core:data:testDebugUnitTest --offline --no-daemon --console=plain
```
Expected: BUILD SUCCESSFUL（既有 updateRecord 相关测试不破）。

- [ ] **Step 3: spotless + commit**

```bash
git add core/data/.../RecordRepositoryImpl.kt
git commit -m "[fix|core:data|图片备份][公共]编辑记录时删被移除的托管图片文件（旧托管图 diff 保留集），补齐编辑路径删文件对称（节点1 reverse R1）"
```

---

### Task 6: B — 孤儿扫描节流 + 恢复复位

**Files:**
- Create: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/uitl/OrphanScanThrottle.kt`（或置 `RecordImageFileStorage.kt` 同文件 top-level）
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordRepositoryImpl.kt:649-660`（cleanupOrphanImageFiles）
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/uitl/impl/BackupRecoveryManagerImpl.kt:807`（恢复复位）
- Test: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/uitl/OrphanScanThrottleTest.kt`

**Interfaces:**
- Consumes: `combineProtoDataSource.tempKeysData`、`combineProtoDataSource.updateLastOrphanScanMs`（T2）。
- Produces: `internal fun shouldRunOrphanScan(lastScanMs: Long, nowMs: Long, throttleMs: Long): Boolean`；常量 `ORPHAN_SCAN_THROTTLE_MS = 7L * 24 * 60 * 60 * 1000`。

- [ ] **Step 1: 写纯函数失败测试**

```kotlin
class OrphanScanThrottleTest {
    private val week = 7L * 24 * 60 * 60 * 1000
    @Test fun firstEver_runs() { assertThat(shouldRunOrphanScan(0L, 1_000L, week)).isTrue() }
    @Test fun withinWindow_skips() { assertThat(shouldRunOrphanScan(1_000L, 1_000L + week - 1, week)).isFalse() }
    @Test fun overWindow_runs() { assertThat(shouldRunOrphanScan(1_000L, 1_000L + week, week)).isTrue() }
}
```

- [ ] **Step 2: 跑确认失败** — `./gradlew :core:data:testDebugUnitTest --tests "*OrphanScanThrottleTest" --offline --no-daemon --console=plain`。
- [ ] **Step 3: 实现纯函数**

```kotlin
const val ORPHAN_SCAN_THROTTLE_MS: Long = 7L * 24 * 60 * 60 * 1000
internal fun shouldRunOrphanScan(lastScanMs: Long, nowMs: Long, throttleMs: Long): Boolean =
    nowMs - lastScanMs >= throttleMs
```

- [ ] **Step 4: 节流接入 cleanupOrphanImageFiles**（gate 下沉 repo，VM 不变）

```kotlin
override suspend fun cleanupOrphanImageFiles(graceWindowMs: Long) = withContext(coroutineContext) {
    val now = System.currentTimeMillis()
    val lastScan = combineProtoDataSource.tempKeysData.first().lastOrphanScanMs
    if (!shouldRunOrphanScan(lastScan, now, ORPHAN_SCAN_THROTTLE_MS)) {
        return@withContext  // 7 天内已扫，跳过全目录 listFiles()
    }
    val referenced = recordDao.queryAllImagePaths()
        .filter { recordImageFileStorage.isManaged(it) }.map { it.substringAfterLast('/') }.toSet()
    val children = recordImageFileStorage.baseDir().listFiles()?.toList()
    if (children != null) {
        computeOrphanFiles(referenced, children, now, graceWindowMs).forEach { it.delete() }
    }
    combineProtoDataSource.updateLastOrphanScanMs(now)  // 扫后回写
}
```

- [ ] **Step 5: 恢复复位**（`BackupRecoveryManagerImpl.kt:807` `updateImagesToFilesMigrated(false)` 之后追加）

```kotlin
// 恢复合并重新引入孤儿 → 复位扫描时间戳，强制下次启动扫一次（节点1 reverse R4）
combineProtoDataSource.updateLastOrphanScanMs(0L)
```

- [ ] **Step 6: 跑测试 + 编译** — `./gradlew :core:data:testDebugUnitTest :core:data:compileDebugKotlin --offline --no-daemon --console=plain`。Expected: PASS / SUCCESSFUL。
- [ ] **Step 7: spotless + commit**

```bash
git add core/data/.../OrphanScanThrottle.kt core/data/.../RecordRepositoryImpl.kt core/data/.../BackupRecoveryManagerImpl.kt core/data/src/test/.../OrphanScanThrottleTest.kt
git commit -m "[perf|core:data|图片备份][公共]孤儿图片扫描节流（7天窗口，shouldRunOrphanScan 纯函数+gate 下沉 repo，VM 不动）+ 恢复后复位时间戳强制下次扫"
```

---

### Task 7: C — DatabaseCompactor 健壮版 live VACUUM

**Files:**
- Create: `core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/util/DatabaseCompactor.kt`（接口 + impl）
- Modify: `core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/di/DatabaseModule.kt`（@Provides DatabaseCompactor）
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/RecordRepository.kt`（+ `suspend fun compactDatabaseIfNeeded()`）
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordRepositoryImpl.kt`（注入 DatabaseCompactor + 实现）
- Modify: `core/testing/src/main/kotlin/cn/wj/android/cashbook/core/testing/repository/FakeRecordRepository.kt`（+ no-op override 或计数）
- Modify: `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/LauncherContentViewModel.kt:96-108`（gate 后调用）
- Test: `core/data/src/test/.../CompactDatabaseIfNeededTest.kt`（FakeDatabaseCompactor + 可控空间）

**Interfaces:**
- Produces: `interface DatabaseCompactor { suspend fun freeSpaceBytes(): Long; suspend fun databaseSizeBytes(): Long; suspend fun vacuum(): Boolean }`；`RecordRepository.compactDatabaseIfNeeded()`。

- [ ] **Step 1: 定义 DatabaseCompactor 接口 + impl**（impl 持 `CashbookDatabase`；vacuum 走 SupportSQLiteDatabase execSQL("VACUUM")，真成功 true、异常 false；StatFs 查可用空间；DB 大小取 db 文件 length）

```kotlin
interface DatabaseCompactor {
    /** 数据库主文件当前字节大小 */ suspend fun databaseSizeBytes(): Long
    /** 数据库所在分区可用字节 */ suspend fun freeSpaceBytes(): Long
    /** 执行 VACUUM，真成功返 true，异常/锁/ENOSPC 返 false */ suspend fun vacuum(): Boolean
}

class DatabaseCompactorImpl @Inject constructor(
    private val database: CashbookDatabase,
    @ApplicationContext private val context: Context,
) : DatabaseCompactor {
    override suspend fun databaseSizeBytes(): Long =
        context.getDatabasePath(DB_FILE_NAME).let { if (it.exists()) it.length() else 0L }
    override suspend fun freeSpaceBytes(): Long =
        StatFs(context.getDatabasePath(DB_FILE_NAME).parentFile!!.absolutePath).availableBytes
    override suspend fun vacuum(): Boolean = try {
        database.openHelper.writableDatabase.execSQL("VACUUM"); true
    } catch (t: Throwable) { false }
}
```
> `DB_FILE_NAME` 与 BackupRecoveryManagerImpl 用的常量对齐（数据库文件名）；放 `core/database` 内常量。

- [ ] **Step 2: Hilt 提供**（DatabaseModule 加 @Provides/@Binds，DatabaseCompactorImpl 走 @Inject 构造，加 `@Binds abstract fun bindDatabaseCompactor(impl): DatabaseCompactor` 或 @Provides）。

- [ ] **Step 3: 写 compactDatabaseIfNeeded 失败测试**（FakeDatabaseCompactor 计数 vacuum 调用 + 控 free/size + Fake tempKeys dbVacuumDone）

```kotlin
class CompactDatabaseIfNeededTest {
    // 用 FakeRecordDao/FakeCombineProtoDataSource + FakeDatabaseCompactor 直接测纯编排逻辑
    @Test fun alreadyDone_skips() { /* dbVacuumDone=true → 不调 vacuum */ }
    @Test fun insufficientSpace_skips_noFlag() { /* free < size → 不调 vacuum、dbVacuumDone 仍 false */ }
    @Test fun success_setsFlag() { /* free>=size, vacuum()→true → updateDbVacuumDone(true) */ }
    @Test fun vacuumFails_noFlag() { /* vacuum()→false → 不置位（下次重试） */ }
}
```
> 若 RecordRepositoryImpl 不可 JVM 实例化（CombineProtoDataSource final），把编排逻辑抽 top-level `internal suspend fun runDbCompactIfNeeded(compactor, tempKeysRead, setDone)` 便于直测，Impl 薄委托。

- [ ] **Step 4: 实现 compactDatabaseIfNeeded**

```kotlin
override suspend fun compactDatabaseIfNeeded() = withContext(coroutineContext) {
    if (combineProtoDataSource.tempKeysData.first().dbVacuumDone) return@withContext
    val size = databaseCompactor.databaseSizeBytes()
    if (databaseCompactor.freeSpaceBytes() < size) return@withContext  // StatFs 预检：空间不足留待下次
    if (databaseCompactor.vacuum()) {
        combineProtoDataSource.updateDbVacuumDone(true)  // 仅真成功才置位，否则下次启动重试
    }
}
```

- [ ] **Step 5: VM 接线**（`LauncherContentViewModel.kt` backfill gate 之后；backfill 完成 imagesToFilesMigrated=true 且 dbVacuumDone=false 时后台跑）

```kotlin
// 在 imagesToFilesMigrated 块之后追加（同 else 分支内、孤儿扫描之前）
if (tempKeys.imagesToFilesMigrated && !tempKeys.dbVacuumDone) {
    try { recordRepository.compactDatabaseIfNeeded() }
    catch (e: CancellationException) { throw e }
    catch (e: Throwable) { this@LauncherContentViewModel.logger().e(e, "db compact failed, retry next launch") }
}
```
> 注：`tempKeys` 是 init 起始读的快照；本次 backfill 刚置 imagesToFilesMigrated 的场景下，VACUUM 顺延下次启动（快照仍 false）——可接受（C 是一次性回收，迟一次启动无碍）。如需当次跑，可在 backfill 成功后重读标志，权衡复杂度，默认顺延。

- [ ] **Step 6: 同步 FakeRecordRepository**（`compactDatabaseIfNeeded` no-op 或计数）+ 编译全链

```bash
./gradlew :core:database:compileDebugKotlin :core:data:testDebugUnitTest :feature:records:testDebugUnitTest --offline --no-daemon --console=plain
./gradlew :app:compileOnlineDebugKotlin --offline --no-daemon --console=plain
```

- [ ] **Step 7: spotless + commit**

```bash
git add core/database/.../DatabaseCompactor.kt core/database/.../DatabaseModule.kt core/data/.../RecordRepository.kt core/data/.../RecordRepositoryImpl.kt core/testing/.../FakeRecordRepository.kt feature/records/.../LauncherContentViewModel.kt core/data/src/test/.../CompactDatabaseIfNeededTest.kt
git commit -m "[feat|core:data|图片备份][公共]C-robust live DB VACUUM（DatabaseCompactor 接缝+StatFs 空间预检+dbVacuumDone 仅成功置位+失败下次重试），backfill 后一次性回收空闲页"
```

---

### Task 8: (a) 备份图片 entry 改 STORED（两遍流式）

**Files:**
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/uitl/impl/BackupRecoveryManagerImpl.kt:444-445,550-556`
- Test: `core/data/src/test/.../StoredZipEntryTest.kt`（CRC32/size 计算）

**Interfaces:**
- Produces: `private fun putZipStoredFileEntry(zos, file, entryName)`（method=STORED + 两遍流式算 CRC32/size）。

- [ ] **Step 1: 写 CRC/size 计算纯函数测试**（喂已知字节，断言 CRC32 与 size）

```kotlin
class StoredZipEntryTest {
    @Test fun crcAndSize_match_knownBytes() {
        val bytes = "hello".toByteArray()
        val crc = java.util.zip.CRC32().apply { update(bytes) }.value
        // 抽 computeCrcAndSize(InputStream): Pair<Long,Long> 流式实现，断言 == (crc, 5)
        assertThat(computeCrcAndSize(bytes.inputStream())).isEqualTo(crc to 5L)
    }
}
```

- [ ] **Step 2: 实现两遍流式 STORED 写入**

```kotlin
/** 流式算 (crc32, size)，O(1) 内存 */
internal fun computeCrcAndSize(input: InputStream): Pair<Long, Long> {
    val crc = java.util.zip.CRC32(); var size = 0L; val buf = ByteArray(8 * 1024)
    input.use { while (true) { val n = it.read(buf); if (n < 0) break; crc.update(buf, 0, n); size += n } }
    return crc.value to size
}
private fun putZipStoredFileEntry(zos: ZipOutputStream, file: File, entryName: String) {
    val (crc, size) = computeCrcAndSize(FileInputStream(file).buffered())  // 第一遍
    val entry = ZipEntry(entryName).apply { method = ZipEntry.STORED; this.size = size; compressedSize = size; this.crc = crc }
    zos.putNextEntry(entry)
    FileInputStream(file).buffered().use { it.copyTo(zos) }  // 第二遍流写
    zos.closeEntry()
}
```
> 注意：STORED entry 不受 `zos.setLevel(BEST_COMPRESSION)` 影响（level 仅作用于 DEFLATED）。

- [ ] **Step 3: 图片打包改用 STORED**（`:444-445`）

```kotlin
imagesDir.listFiles()?.filter { it.isFile }?.forEach { img ->
    putZipStoredFileEntry(zos, img, RECORD_IMAGES_ENTRY_PREFIX + img.name)  // JPEG 已压，STORED 省 CPU
}
```

- [ ] **Step 4: 跑测试 + 编译** — `./gradlew :core:data:testDebugUnitTest --tests "*StoredZipEntryTest" :core:data:compileDebugKotlin --offline --no-daemon --console=plain`。
- [ ] **Step 5: spotless + commit**

```bash
git add core/data/.../BackupRecoveryManagerImpl.kt core/data/src/test/.../StoredZipEntryTest.kt
git commit -m "[perf|core:data|图片备份][公共]备份 record_images entry 改 ZipEntry.STORED（JPEG 已压缩，两遍流式算 CRC32/size 保持 O(1) 内存）"
```

---

### Task 9: (b) integrity_check gate 回退

**Files:**
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/uitl/impl/BackupRecoveryManagerImpl.kt:404-434`

- [ ] **Step 1: 重构 VACUUM 块**（捕获 integrity 为 val → 出 `.use{}` → 不 ok 则重 checkpoint + 回退覆盖）

```kotlin
databaseFile.copyTo(databaseCacheFile)  // 已存在 :415
var integrityOk = true
runCatching {
    context.openOrCreateDatabase(databaseCacheFile.absolutePath, Context.MODE_PRIVATE, null).use { db ->
        db.execSQL("VACUUM")
        db.rawQuery("PRAGMA integrity_check", null).use { c ->
            if (c.moveToFirst() && !"ok".equals(c.getString(0), ignoreCase = true)) integrityOk = false
        }
    }
}.onFailure { integrityOk = false; logger().w("startBackup(), VACUUM failed: ${it.message}") }
if (!integrityOk) {
    // 句柄已随 .use{} 关闭；回退前重 checkpoint（回退点晚于首次 checkpoint，期间写入可能落 -wal）
    database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
    databaseFile.copyTo(databaseCacheFile, overwrite = true)
    logger().w("startBackup(), integrity_check not ok, fallback to checkpointed copy")
}
```

- [ ] **Step 2: 编译 + 现有备份测试不回归** — `./gradlew :core:data:compileDebugKotlin :core:data:testDebugUnitTest --offline --no-daemon --console=plain`。
- [ ] **Step 3: spotless + commit**

```bash
git add core/data/.../BackupRecoveryManagerImpl.kt
git commit -m "[fix|core:data|图片备份][公共]备份 VACUUM integrity_check 失败时回退未 VACUUM 副本（关句柄后回退+重 checkpoint 防丢最近写入）"
```

---

### Task 10: (c) importSettings 不再 apply ignoreUpdateVersion

**Files:**
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/impl/SettingRepositoryImpl.kt:297`
- Modify: `core/testing/src/main/kotlin/cn/wj/android/cashbook/core/testing/repository/FakeSettingRepository.kt:142`
- Test: `core/data/src/test/.../repository/impl/SettingRepositoryImplTest.kt`（若可）或 FakeSettingRepository 行为测试

**Interfaces:** 字段保留于 `SettingsBackup`/codec（JSON 形状不变，前后向兼容），仅 import 不 apply。

- [ ] **Step 1: 写/改测试**：import 后本地 `ignoreUpdateVersion` 未被备份值覆盖

```kotlin
@Test fun importSettings_doesNotOverwrite_ignoreUpdateVersion() = runTest {
    // 设本地 ignoreUpdateVersion="local-v" → importSettings(json with ignoreUpdateVersion="backup-v")
    // → 断言读回仍 "local-v"
}
```

- [ ] **Step 2: 改 SettingRepositoryImpl.importSettings**（删 `updateIgnoreUpdateVersion(backup.ignoreUpdateVersion)` 行，加注释）

```kotlin
// 不恢复 ignoreUpdateVersion：设备本地「跳过此更新版本」状态不随备份转移（节点1 reverse R2）
// 字段仍保留于 SettingsBackup/codec（JSON 形状不变，旧 app 解析兼容）
```

- [ ] **Step 3: 同步 FakeSettingRepository.importSettings**（同样不 apply）。
- [ ] **Step 4: 跑测试 + 编译** — `./gradlew :core:data:testDebugUnitTest :core:testing:compileDebugKotlin --offline --no-daemon --console=plain`。
- [ ] **Step 5: spotless + commit**

```bash
git add core/data/.../SettingRepositoryImpl.kt core/testing/.../FakeSettingRepository.kt core/data/src/test/.../SettingRepositoryImplTest.kt
git commit -m "[fix|core:data|图片备份][公共]恢复设置不再 apply ignoreUpdateVersion（设备本地跳过版本不随备份转移；字段保留于 codec 保前后向兼容，规避旧 app 恢复丢全部设置）"
```

---

### Task 11: (d) org.json 走 version catalog

**Files:**
- Modify: `gradle/libs.versions.toml`（加 version + library）
- Modify: `core/data/build.gradle.kts:60-62`

- [ ] **Step 1: libs.versions.toml 加条目**（先读现有 `[versions]`/`[libraries]` 格式）

```toml
# [versions]
orgJson = "20180813"
# [libraries]
org-json = { group = "org.json", name = "json", version.ref = "orgJson" }
```

- [ ] **Step 2: build.gradle.kts 改引用**

```kotlin
// 原 testImplementation("org.json:json:20180813") 改为
testImplementation(libs.org.json)
```

- [ ] **Step 3: 验证测试仍绿**（org.json 解析正常） — `./gradlew :core:data:testDebugUnitTest --offline --no-daemon --console=plain`。Expected: BUILD SUCCESSFUL（离线 20180813 有 metadata）。
- [ ] **Step 4: commit**

```bash
git add gradle/libs.versions.toml core/data/build.gradle.kts
git commit -m "[build|core:data|图片备份][公共]org.json 测试依赖迁入 version catalog（libs.org.json，版本固定 20180813 离线缓存有 metadata）"
```

---

### Task 12: D — 恢复端到端 androidTest

**Files:**
- Create: `core/data/src/androidTest/kotlin/cn/wj/android/cashbook/core/data/BackupRecoveryEndToEndTest.kt`（或置 `core/database` androidTest，按可注入真实 DB+文件的位置）

- [ ] **Step 1: 写端到端 androidTest**（构造含图记录 DB + record_images 文件 + settings → startBackup 打包 → 清空 → recovery → 校验 DB 行/图片文件/settings 还原、STORED 图片 entry、白名单/manifest 版本戳）

```kotlin
@Test fun backup_then_recovery_roundtrip_restoresDbAndImages() = runTest {
    // 1. 真实 Room DB 插入 1 条带图记录（record_images/x.jpg 落 filesDir）+ settings
    // 2. backupRecoveryManager.startBackup(...) → 得 zip
    // 3. 校验 zip：record_images entry method==STORED；含 db/settings.json/manifest.json
    // 4. 清空 DB + filesDir/record_images → startRecovery(zip)
    // 5. 断言：记录回来、图片文件 record_images/x.jpg 存在且字节一致、settings 还原、
    //          ZipSlip(../) entry 被白名单拒、manifest formatVersion 读取正确
}
```
> 端到端依赖真实 `BackupRecoveryManagerImpl`（注入真实 database/context/storage/settingRepository），需 Hilt test 或手工组装。先读现有 androidTest 基建（`CashbookTestRunner`、`@HiltAndroidTest` 用例）选最简组装路径。

- [ ] **Step 2: 编译 androidTest** — `./gradlew :core:data:compileDebugAndroidTestKotlin --offline --no-daemon --console=plain`。Expected: BUILD SUCCESSFUL（本机无设备先不跑，T 收尾跑）。
- [ ] **Step 3: commit**

```bash
git add core/data/src/androidTest/.../BackupRecoveryEndToEndTest.kt
git commit -m "[test|core:data|图片备份][公共]新增备份→恢复端到端 androidTest（含图记录 round-trip：DB/图片文件/settings 还原 + STORED entry + 白名单 + manifest 版本戳）"
```

---

### 验证收尾（非代码 commit）

- [ ] **全量编译 + JVM 测试**：`./gradlew :app:compileOnlineDebugKotlin :app:testOnlineDebugUnitTest :core:data:testDebugUnitTest :sync:work:testDebugUnitTest :feature:records:testDebugUnitTest --offline --no-daemon --console=plain`，全 BUILD SUCCESSFUL。
- [ ] **spotlessCheck**：`./gradlew spotlessCheck --init-script gradle/init.gradle.kts --no-configuration-cache --offline`。
- [ ] **lint**（涉及模块）：`./gradlew :core:data:lintRelease :sync:work:lintRelease --offline`（首次缺 lint-gradle 去 --offline 经代理拉）。
- [ ] **androidTest 模拟器跑**（启 Medium_Phone，关 Studio 腾内存，代理实下载验 UTP）：`:core:database:connectedDebugAndroidTest`（T3 路径查询 + 既有 DAO）+ `:core:data:connectedDebugAndroidTest`（T12 端到端 + C VACUUM 真实回收）。判定看 XML `<testsuite ... failures="0">`。
- [ ] **手动黑盒往返**：模拟器「建带图记录→备份→pm clear/卸载重装→恢复→验图片回来 + 数据完整 + 信用卡品牌卡设日期后触发 N1 收到提醒」。android-cli + `android layout` JSON dump（截图全白）。报告写 `docs/testing/reports/2026-06-29-creditcard-image-backup-journey.md`。
- [ ] **截图基线**：本批无常驻 UI 改动；若 T 引入新截图用例则留 CI 录制（本机不录）。
- [ ] **节点2 full-review**：对全 diff 跑 `comprehensive-review:full-review`；blocking（Critical/High）交付前修复。
- [ ] **finishing**：worktree 内验证通过 → 主仓库 `--ff-only` 合入 main（using-git-worktrees / finishing-a-development-branch）。

---

## Self-Review

**1. Spec coverage（逐项对照）：**
- 信用卡修复 → T1 ✓
- A 删文件对称（资产/账本/编辑路径）→ T3(DAO) + T4(资产/账本) + T5(编辑) ✓
- B 孤儿节流 + 恢复复位 → T2(proto) + T6 ✓
- C live VACUUM（C-robust）→ T2(dbVacuumDone) + T7 ✓
- (a) STORED → T8 ✓；(b) integrity gate → T9 ✓；(c) ignoreUpdateVersion → T10 ✓；(d) org.json catalog → T11 ✓
- D 恢复端到端 androidTest → T12 ✓
- E 截图基线（CI 管理）→ 验证收尾说明 ✓
- 手动黑盒往返 → 验证收尾 ✓

**2. Placeholder scan：** 每 Task 含具体测试码 + 实现码 + 精确命令；字段名/谓词处标注"以实际为准、实现时先读"，非 TBD。

**3. Type consistency：** `selectReminderCreditCards`/`shouldRunOrphanScan`/`compactDatabaseIfNeeded`/`queryImagePathsByAssetId|ByBookId`/`updateLastOrphanScanMs`/`updateDbVacuumDone`/`DatabaseCompactor` 在定义 Task 与消费 Task 中名称一致。

**4. fan-out 完整性：** proto 2 字段贯穿 TempKeysModel/CombineProtoDataSource/FakeCombineProtoDataSource（T2）；DAO 2 方法同步 FakeRecordDao（T3）；(c) 同步 SettingRepositoryImpl+FakeSettingRepository（T10）；DatabaseCompactor Hilt @Binds（T7）——均在对应 Task Files/Steps 列出。
