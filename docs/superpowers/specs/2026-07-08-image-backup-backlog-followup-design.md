# 图片存储/备份 backlog 收尾设计（#2 恢复流式化 + M2 抽 UseCase + M1 级联单一真源）

> 创建于 2026-07-08。消化 2026-06-29 信用卡/图片备份 backlog 清理（main `b1e7725f`）遗留的 full-review defer backlog。

## 背景与目标

三项独立 backlog，同属「图片存储/备份」主题，合一个 spec / 一个 worktree / 一个 PR 交付：

- **#2 content:// 恢复分支流式化**：恢复流的 content:// 分支把整个备份 zip 读入堆内存，与已修的本地分支（流式）不对称，大备份有 OOM 风险。
- **M2 抽 `RunStartupMaintenanceUseCase`**：启动维护逻辑（迁移/重算/backfill/VACUUM/孤儿扫描）全堆在 ViewModel init，收编为 domain UseCase。
- **M1 级联删除图片谓词单一真源**：删记录场景的「取图谓词」与「删除谓词」是两套 WHERE，易漂移致孤儿图/漏删；让级联删除返回实删记录的图片 path 作单一真源。

**不在本次范围（YAGNI）**：

- `getWebFile`（`core/data/.../impl/BackupRecoveryManagerImpl.kt:237`）的 `writeBytes(bytes)`——`bytes` 来自 `WebDAVHandler.get()` 返回 `ByteArray`（网络层接口），改流式需改网络接口，超出 #2 范围。
- incremental_vacuum（`auto_vacuum=INCREMENTAL`）——改 DB pragma，需 migration + 真机验证，正确性风险最高，独立评估。
- M1 不纳入编辑记录路径（`updateRecord`），见块三「非目标」。

## 块一：#2 content:// 恢复分支流式化

### 现状

`core/data/.../impl/BackupRecoveryManagerImpl.kt:714-716` content:// 恢复分支：

```kotlin
context.contentResolver.openInputStream(localPath.toUri())!!.use {
    backupZippedCacheFile.writeBytes(it.readBytes())   // 整个 zip（含图片 BLOB，可数十 MB）读入堆
}
```

本地路径分支已是流式 `stageLocalBackupToCache`（`:983`，`copyTo(overwrite=true)`）。两分支内存行为不对称。

### 方案

抽对称的顶层 `internal fun stageInputStreamToCache(input: InputStream, cacheDir: File, name: String): File`：

- 内部 `input.use { inS -> FileOutputStream(dest).use { inS.copyTo(it) } }`（`copyTo` 默认 8KB 缓冲，O(1) 内存）。
- content:// 分支去掉 `createNewFile()` + `readBytes()`，改调 `stageInputStreamToCache(inputStream, cacheDir, name)`。
- `stageLocalBackupToCache` 保持不变（本地文件已能 `copyTo` 流式，无需经 InputStream 中转）。

### 测试

顶层 internal fun，`core/data` test 源集 JVM 单测：构造 `ByteArrayInputStream` → 调用 → 断言目标文件字节与输入一致；覆盖目标已存在时覆盖写。与既有 `stageLocalBackupToCache`/`writeStoredZipEntry` 单测同文件风格。

## 块二：M2 抽 `RunStartupMaintenanceUseCase`

### 现状

`feature/records/.../viewmodel/LauncherContentViewModel.kt:70-130` init 块内联 5 个 try/catch：

1. `db9To10DataMigrated` gate：false → `migrateAfter9To10()` 后 `_migrationCompleted=true`；true → 立即 `_migrationCompleted=true`。
2. else 分支后台：`finalAmountNetRecalcDone` → `recalculateAllFinalAmount()`；`imagesToFilesMigrated` → `backfillImagesToFiles()`；`imagesToFilesMigrated && !dbVacuumDone` → `compactDatabaseIfNeeded()`。
3. 每次启动兜底：`cleanupOrphanImageFiles()`。

### 方案

新建 `core/domain/.../usecase/RunStartupMaintenanceUseCase.kt`，收编全部维护步骤：

```kotlin
class RunStartupMaintenanceUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
    private val settingRepository: SettingRepository,   // 读 tempKeysModel
) {
    suspend operator fun invoke(onFirstScreenReady: () -> Unit) {
        val tempKeys = settingRepository.tempKeysModel.first()
        // gate 语义 100% 原样搬入：仅决定何时回调 onFirstScreenReady，逻辑零改动
        if (!tempKeys.db9To10DataMigrated) {
            recordRepository.migrateAfter9To10()
            onFirstScreenReady()
        } else {
            onFirstScreenReady()
            if (!tempKeys.finalAmountNetRecalcDone) runCatchingMaintenance("netRecalc") { recordRepository.recalculateAllFinalAmount() }
            if (!tempKeys.imagesToFilesMigrated) runCatchingMaintenance("backfill") { recordRepository.backfillImagesToFiles() }
            if (tempKeys.imagesToFilesMigrated && !tempKeys.dbVacuumDone) runCatchingMaintenance("compact") { recordRepository.compactDatabaseIfNeeded() }
        }
        runCatchingMaintenance("orphanScan") { recordRepository.cleanupOrphanImageFiles() }
    }
}
```

- `runCatchingMaintenance` 私有 helper：`try { block() } catch (e: CancellationException) { throw e } catch (t: Throwable) { logger().e(...) }`——保留「CancellationException 先 rethrow」不变量（与既有 init 一致）。
- **注意** db9To10=false 分支的 `migrateAfter9To10` 现有代码**不带 try/catch**（异常逃逸触发全局 UncaughtExceptionHandler 的 finishAllActivity，幂等下次重试）——UseCase 须保留此差异（此分支不包 runCatchingMaintenance），与 `LauncherContentViewModel.kt:74-76` 现状一致。
- ViewModel init 退化为：
  ```kotlin
  init { viewModelScope.launch { runStartupMaintenance { _migrationCompleted.value = true } } }
  ```

### 决策点 1（已定）：gate 用 `onFirstScreenReady` 回调彻底收编

理由：① 放行时机语义（`LauncherContentViewModel.kt:74-79`）只需原样搬进 UseCase、逻辑零改动，回归风险最低；② UseCase 落 domain 不依赖 UI 类型，`FakeRecordRepository:122/130` 已备 `backfillImagesToFilesCount`/`cleanupOrphanImageFilesCount` 计数桩，编排顺序可直接 JVM 断言，无需 Robolectric。

### 测试

`core/domain` test（用 `core:testing` 的 `FakeRecordRepository` + Fake SettingRepository）JVM 单测：

- db9To10=false → `migrateAfter9To10` 调用 + onReady 在其后调用（顺序）。
- db9To10=true + 各标志组合 → onReady 立即调用 + 对应维护方法按标志触发/跳过（用 count 断言）。
- 各标志已完成 → 对应维护跳过。
- orphanScan 每分支都调用。

ViewModel 行为保持：`LauncherContentViewModel` 现有测试（若有）验证 `_migrationCompleted` 仍正确置位。

## 块三：M1 级联删除图片谓词单一真源

### 现状（两套 WHERE 漂移风险）

删记录场景全部经 `TransactionDao.deleteRecordsBatch(records)`（`core/database/.../dao/TransactionDao.kt:728`）：

- 删资产 `RecordRepositoryImpl.kt:513-522`：先 `queryImagePathsByAssetId(assetId)`（谓词 A：`asset_id OR into_asset_id`），再 `deleteAssetRelatedData(assetId)`→`deleteRecordsBatch(queryRecordsByAssetId(assetId))`（谓词 B）。**A/B 两套 WHERE 须手工保持一致。**
- 删账本 `BooksRepositoryImpl.kt:76`：`queryImagePathsByBookId(id)` vs `deleteBookTransaction`→`deleteRecordsBatch(queryRecordListByBookId(bookId))`。同样漂移风险。
- 单删 `RecordRepositoryImpl.kt:121-128`：`queryImagesByRecordId(recordId).map{path}` vs `deleteRecordTransaction(recordId)`→`deleteRecordsBatch(listOf(record))`（`TransactionDao.kt:647-648`）。谓词都是单 record id、漂移低，但仍是两次独立查询。

### 方案：`deleteRecordsBatch` 事务内返回实删托管图 path

`deleteRecordsBatch` 内部才持有实删的 `deletedIds`（`TransactionDao.kt:730`）——这是唯一真源。改造：

1. 新增 DAO 投影 `@Query("SELECT path FROM db_image_with_related WHERE record_id IN (:ids)") suspend fun queryImagePathsByRecordIds(ids: List<Long>): List<String>`（只投影 path、**不物化 BLOB**，遵循 full-review P1 教训；chunk 复用 `DELETE_IN_CHUNK_SIZE`）。
2. `deleteRecordsBatch` **删关联/记录之前**（关联未清时）`queryImagePathsByRecordIds(idList)` 收集 path，方法**改返回 `List<String>`**。
3. `deleteBookTransaction`/`deleteAssetRelatedData`/`deleteRecordTransaction(recordId)`/`deleteRecordTransaction(record)` **改返回 `List<String>`**，透传 `deleteRecordsBatch` 的返回（`deleteBookTransaction` 内 `deleteRecordsBatch(...)` 是首步，其返回即账本全部记录的图 path）。
4. 调用方改造：
   - 删资产 `RecordRepositoryImpl.kt:513-522`：删 `queryImagePathsByAssetId` 前置查询，改 `val imagePaths = transactionDao.deleteAssetRelatedData(assetId)` 拿返回值删文件。
   - 删账本 `BooksRepositoryImpl.kt:76`：删 `queryImagePathsByBookId`，改用 `deleteBookTransaction(id)` 返回值。
   - 单删 `RecordRepositoryImpl.kt:121-128`：删 `queryImagesByRecordId(recordId).map{path}`，改用 `deleteRecordTransaction(recordId)` 返回值。
5. 清理孤儿：`queryImagePathsByAssetId`（`RecordDao.kt:514`）/`queryImagePathsByBookId`（`:526`）生产调用方仅上述两处，删除后成孤儿 → 删 DAO 声明 + `FakeRecordDao:479/485` override + androidTest `RecordDaoTest:845/866` 两用例（其守护职责由新的按 record id 路径 + `deleteRecordsBatch` androidTest 承接）。`queryImagePathsByRecordId`（`:530`）**保留**（编辑路径 `updateRecord:100` 仍用）。

### 决策点 2（已定）：编辑路径不纳入 M1

理由：编辑 `updateRecord`（`RecordRepositoryImpl.kt:100-116`）走 `queryImagePathsByRecordId` + `managedImagesToDelete(oldManagedPaths, persistedImages, ...)` **diff** 新旧图片集、只删被移除的、保留记录本身，与「删整条记录」语义不同；强纳入会把两种不同语义混进 `deleteRecordsBatch` 的级联返回，制造新耦合。

### 影响面与回归风险

- 5 个 DAO 方法签名变更（`Unit`→`List<String>`）：`deleteRecordsBatch`、`deleteBookTransaction`、`deleteAssetRelatedData`、两个 `deleteRecordTransaction` 重载（`recordId=null` 分支返回 `emptyList()`）。
- 同步 `core/data` test 的 `FakeTransactionDao`（若有对应 override）+ `FakeRecordDao`（删两个孤儿 override）。
- **级联删除语义变更须 androidTest 真机验证**——本次靠 PR 的 CI androidTest 兜底（本机模拟器可选补跑）。

### 测试

- `core/database` androidTest `RecordDaoTest`/`TransactionDaoTest`：
  - `deleteRecordsBatch` 返回实删记录的全部托管图 path（含 into_asset 场景、多记录、无图记录返回空、平账 type 记录）。
  - 删资产/删账本经级联返回的 path 与旧 `queryImagePathsByAssetId/ByBookId` 等价（迁移前后一致性）。
- `core/data` JVM（`FakeTransactionDao`/`FakeRecordDao` 忠实桩）：删资产/删账本/单删的 Repository 层用级联返回删文件、不再前置查询。

## 测试策略汇总

- #2：JVM 单测（顶层 fun）。
- M2：`core/domain` JVM 单测（Fake 计数桩验编排顺序）。
- M1：`core/database` androidTest（真 DAO 级联返回）+ `core/data` JVM（Fake 忠实桩验调用方接线）。
- 完整链路：`:app:compileOnlineDebugKotlin`（跨模块 Hilt 全图）+ 相关模块 `testDebugUnitTest` + `spotlessCheck` + `feature:records`/`core:data` lint。

## 验收标准

1. #2：content:// 恢复分支流式化、单测通过；恢复功能行为不变。
2. M2：ViewModel init 退化为单行 UseCase 调用；UseCase 编排顺序 + gate 放行语义单测通过；`LauncherContentViewModel` 行为保持。
3. M1：4 条删记录路径经级联返回删图、无独立取图查询；两孤儿 DAO 删除；androidTest 级联返回正确。
4. 全链路验证绿。节点1 四维评审 + 节点2 full-review 无未决 Critical/High。

## 整合与交付

- 全程在 worktree（`D:\wt\Cashbook\<name>`，base=head 含本地未推送 commit）内 TDD。
- 完成 → merge 回本地 main（`--ff-only`）→ push origin → 开 PR 到 main。
- **PR 的 CI 顺带解决 backlog #3 截图基线**：`Build.yaml:91-103` 在 PR 事件 + `verifyRoborazziDevDebug` 失败时 `recordRoborazziDevDebug` 并 auto-commit 回 PR 分支 → 自动录制 `RecordDetailsSheet_withImage` 等缺失基线；CI androidTest 兜底 M1 级联真机验证。
