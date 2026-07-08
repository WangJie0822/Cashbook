# 图片存储/备份 backlog 收尾 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 收尾 2026-06-29 图片/备份 backlog 三项——#2 恢复 content:// 分支流式化、M2 抽 `RunStartupMaintenanceUseCase`、M1 级联删除返回实删图片 path 单一真源。

**Architecture:** 三块独立。#2 抽对称流式 helper 消堆内存物化；M2 把 ViewModel init 的启动维护编排搬进 domain UseCase（`onFirstScreenReady` 回调保放行语义）；M1 让 `deleteRecordsBatch` 事务内派生实删记录的图 path 并返回，四条删记录路径透传，消除「取图谓词 vs 删除谓词」两套 WHERE 的注释纪律依赖。

**Tech Stack:** Kotlin, Room（KSP），Hilt，Coroutines/Flow，JUnit4 + Truth + coroutines-test，Roborazzi（不涉本次）。

## Global Constraints

- 金额单位分（Long），本次不涉金额计算。
- 新建 `.kt` 文件须含 Apache 2.0 License Header（spotless 检查，模板见 `spotless/`）。
- Material3 组件禁用规则不涉（无 UI 新增）。
- DAO 新增抽象方法必须同步 `core/data` test 的 `FakeXxxDao`，否则 `:core:data:compileDebugUnitTestKotlin` 报「not abstract」。
- app 模块编译验证用带 flavor 的 `:app:compileOnlineDebugKotlin`（跨模块 Hilt 全图）。
- 本机 gradle：`--no-daemon --console=plain`；首次缺依赖清继承代理 + `-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897` 暖缓存，之后 `--offline`；androidTest 本机受代理 TLS 约束，M1 真机验证靠 PR 的 CI androidTest 兜底。
- SQL 图片列名是 **`image_path`**（`ColumnNames.kt:71`），不是 `path`（后者仅 Kotlin 属性名）。
- 全程在 worktree 内做（`D:\wt\Cashbook\<name>`，base=head 含本地未推送 commit）。

---

## File Structure

- `core/data/.../uitl/impl/BackupRecoveryManagerImpl.kt` — 改（#2：加 `stageInputStreamToCache` 顶层 fun + content:// 分支切换）
- `core/data/src/test/.../uitl/impl/StageInputStreamToCacheTest.kt` — 新建（#2 JVM 单测）
- `core/domain/.../usecase/RunStartupMaintenanceUseCase.kt` — 新建（M2）
- `core/domain/src/test/.../usecase/RunStartupMaintenanceUseCaseTest.kt` — 新建（M2 JVM 单测）
- `feature/records/.../viewmodel/LauncherContentViewModel.kt` — 改（M2：init 退化 + 注入 UseCase）
- `feature/records/src/test/.../viewmodel/LauncherContentViewModelTest.kt` — 改（M2：9 处构造更新 + 编排断言迁移 + 放行测试）
- `core/database/.../dao/TransactionDao.kt` — 改（M1：加 `queryImagePathsByRecordIds`，`deleteRecordsBatch` + 4 路透传改返回 `List<String>`）
- `core/data/src/test/.../testdoubles/FakeTransactionDao.kt` — 改（M1：补 `queryImagePathsByRecordIds` 忠实 override）
- `core/data/src/test/.../testdoubles/TransactionDaoLogicTest.kt` — 改（M1：deleteRecordsBatch 返回 path JVM 用例）
- `core/database/src/androidTest/.../dao/RecordDaoTest.kt` / `TransactionDaoTest.kt` — 改（M1：级联返回 androidTest；删 845/866 两用例、负断言迁入新用例）
- `core/data/.../repository/impl/RecordRepositoryImpl.kt` — 改（M1：删资产/单删用级联返回）
- `core/data/.../repository/impl/BooksRepositoryImpl.kt` — 改（M1：删账本用级联返回）
- `core/database/.../dao/RecordDao.kt` — 改（M1：删 `queryImagePathsByAssetId/ByBookId` 孤儿）
- `core/data/src/test/.../testdoubles/FakeRecordDao.kt` — 改（M1：删两孤儿 override）
- `core/data/src/test/.../repository/`（删资产/删账本 Repository 测试）— 改（M1：级联返回删文件 + F5 负用例）

---

### Task 1: #2 content:// 恢复分支流式化（`stageInputStreamToCache`）

**Files:**
- Create: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/uitl/impl/StageInputStreamToCacheTest.kt`
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/uitl/impl/BackupRecoveryManagerImpl.kt`（新增顶层 `stageInputStreamToCache`；改 `:705-716` content:// 分支）

**Interfaces:**
- Produces: `internal fun stageInputStreamToCache(input: InputStream, cacheDir: File, name: String): File`（流式写入、name 越界抛 `IllegalArgumentException`）

- [ ] **Step 1: 写失败测试**（参照同目录 `StageLocalBackupToCacheTest.kt` 风格）

```kotlin
// StageInputStreamToCacheTest.kt（含 Apache header）
package cn.wj.android.cashbook.core.data.uitl.impl

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File

class StageInputStreamToCacheTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val backupBytes = ByteArray(64 * 1024) { ((it * 31 + 7) % 256).toByte() }

    @Test
    fun stream_copiedIntoCache_bytesIdentical() {
        val cacheDir = tempFolder.newFolder("cache")
        val staged = stageInputStreamToCache(
            ByteArrayInputStream(backupBytes), cacheDir, "Cashbook_Backup_File_x.zip",
        )
        assertThat(staged.absolutePath)
            .isEqualTo(File(cacheDir, "Cashbook_Backup_File_x.zip").absolutePath)
        assertThat(staged.readBytes()).isEqualTo(backupBytes)
    }

    @Test
    fun destAlreadyExists_overwritten() {
        val cacheDir = tempFolder.newFolder("cache")
        File(cacheDir, "Cashbook_Backup_File_x.zip").writeBytes("stale".toByteArray())
        val staged = stageInputStreamToCache(
            ByteArrayInputStream(backupBytes), cacheDir, "Cashbook_Backup_File_x.zip",
        )
        assertThat(staged.readBytes()).isEqualTo(backupBytes)
    }

    @Test
    fun nameWithPathTraversal_rejected() {
        val cacheDir = tempFolder.newFolder("cache")
        try {
            stageInputStreamToCache(
                ByteArrayInputStream(backupBytes), cacheDir, "../escape.zip",
            )
            throw AssertionError("expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // 期望：越出 cacheDir 被拒
        }
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*StageInputStreamToCacheTest" --no-daemon --offline --console=plain`
Expected: 编译失败（`stageInputStreamToCache` 未定义）

- [ ] **Step 3: 实现顶层 fun**（加到 `BackupRecoveryManagerImpl.kt` 末尾、与 `stageLocalBackupToCache` 相邻；`isWithinDir` 已是本模块顶层 fun，直接调）

```kotlin
/**
 * 把 [input] 流式写入恢复缓存目录 [cacheDir] 下名为 [name] 的文件，返回该文件。
 * 流式 copyTo（8KB 缓冲，O(1) 内存），与 [stageLocalBackupToCache] 对称，供 content:// 恢复分支消除
 * 整流 readBytes() 的堆内存物化。[name] 来自 DocumentFile.name（provider 可控），用 [isWithinDir]
 * canonical 校验拒绝路径穿越（本地文件分支因 File.name 剥离分隔符天然安全，此处补齐 content:// 对称）。
 */
internal fun stageInputStreamToCache(input: InputStream, cacheDir: File, name: String): File {
    val dest = File(cacheDir, name)
    require(isWithinDir(dest, cacheDir)) { "staged file escapes cache dir: $name" }
    dest.parentFile?.mkdirs()
    input.use { inS -> FileOutputStream(dest).use { inS.copyTo(it) } }
    return dest
}
```

- [ ] **Step 4: 切换 content:// 分支**（`BackupRecoveryManagerImpl.kt:705-720` 内，把 `createNewFile()` + `writeBytes(it.readBytes())` 改为调 helper）

原 `:709-716`：
```kotlin
if (name.startsWith(BACKUP_FILE_NAME) && name.endsWith(BACKUP_FILE_EXT)) {
    backupZippedCacheFile = File(cacheDir, name)
    if (!backupZippedCacheFile.exists()) {
        backupZippedCacheFile.createNewFile()
    }
    context.contentResolver.openInputStream(localPath.toUri())!!.use {
        backupZippedCacheFile.writeBytes(it.readBytes())
    }
} else {
```
改为：
```kotlin
if (name.startsWith(BACKUP_FILE_NAME) && name.endsWith(BACKUP_FILE_EXT)) {
    val input = context.contentResolver.openInputStream(localPath.toUri())!!
    backupZippedCacheFile = stageInputStreamToCache(input, cacheDir, name)
} else {
```

- [ ] **Step 5: 运行测试确认通过 + 编译**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*StageInputStreamToCacheTest" --no-daemon --offline --console=plain`
Expected: PASS（3 用例）

- [ ] **Step 6: spotless + commit**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
git add core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/uitl/impl/BackupRecoveryManagerImpl.kt core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/uitl/impl/StageInputStreamToCacheTest.kt
git commit -m "[refactor|core:data|备份恢复][公共]#2 content:// 恢复分支流式化 stageInputStreamToCache（消 readBytes 堆物化 + name 路径穿越 isWithinDir 加固）"
```

---

### Task 2: M2 `RunStartupMaintenanceUseCase`（domain）

**Files:**
- Create: `core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/RunStartupMaintenanceUseCase.kt`
- Create: `core/domain/src/test/kotlin/cn/wj/android/cashbook/domain/usecase/RunStartupMaintenanceUseCaseTest.kt`

**Interfaces:**
- Consumes: `RecordRepository`（`migrateAfter9To10`/`recalculateAllFinalAmount`/`backfillImagesToFiles`/`compactDatabaseIfNeeded`/`cleanupOrphanImageFiles`）、`SettingRepository.tempKeysModel`
- Produces: `class RunStartupMaintenanceUseCase @Inject constructor(recordRepository, settingRepository)` 有 `suspend operator fun invoke(onFirstScreenReady: () -> Unit)`

- [ ] **Step 1: 写失败测试**（参照 `GetBudgetProgressUseCaseTest.kt`：`TestDispatcherRule` + `FakeRecordRepository`/`FakeSettingRepository` + `runTest`）

```kotlin
// RunStartupMaintenanceUseCaseTest.kt（含 Apache header）
package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.model.model.TempKeysModel
import cn.wj.android.cashbook.core.testing.repository.FakeRecordRepository
import cn.wj.android.cashbook.core.testing.repository.FakeSettingRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class RunStartupMaintenanceUseCaseTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var recordRepository: FakeRecordRepository
    private lateinit var settingRepository: FakeSettingRepository
    private lateinit var useCase: RunStartupMaintenanceUseCase

    @Before
    fun setup() {
        recordRepository = FakeRecordRepository()
        settingRepository = FakeSettingRepository()
        useCase = RunStartupMaintenanceUseCase(recordRepository, settingRepository)
    }

    @Test
    fun db9To10_not_migrated_then_migrate_then_ready() = runTest {
        settingRepository.setTempKeys(TempKeysModel(db9To10DataMigrated = false, preferenceSplit = false))
        val order = mutableListOf<String>()
        useCase { order.add("ready") }
        // migrate 在放行前调用；此分支不跑后台维护（净自付/backfill/compact）
        assertThat(recordRepository.migrateAfter9To10Count).isEqualTo(1)
        assertThat(order).contains("ready")
        assertThat(recordRepository.backfillImagesToFilesCount).isEqualTo(0)
        // orphanScan 每分支兜底
        assertThat(recordRepository.cleanupOrphanImageFilesCount).isEqualTo(1)
    }

    @Test
    fun db9To10_done_all_flags_pending_then_full_maintenance() = runTest {
        settingRepository.setTempKeys(
            TempKeysModel(
                db9To10DataMigrated = true, preferenceSplit = true,
                finalAmountNetRecalcDone = false, imagesToFilesMigrated = false, dbVacuumDone = false,
            ),
        )
        var ready = false
        useCase { ready = true }
        assertThat(ready).isTrue()
        assertThat(recordRepository.migrateAfter9To10Count).isEqualTo(0)
        assertThat(recordRepository.recalculateAllFinalAmountCount).isEqualTo(1)
        assertThat(recordRepository.backfillImagesToFilesCount).isEqualTo(1)
        // compact gate：本次快照 imagesToFilesMigrated=false → 不跑 compact（顺延下次启动）
        assertThat(recordRepository.compactDatabaseIfNeededCount).isEqualTo(0)
        assertThat(recordRepository.cleanupOrphanImageFilesCount).isEqualTo(1)
    }

    @Test
    fun images_migrated_but_vacuum_pending_then_compact() = runTest {
        settingRepository.setTempKeys(
            TempKeysModel(
                db9To10DataMigrated = true, preferenceSplit = true,
                finalAmountNetRecalcDone = true, imagesToFilesMigrated = true, dbVacuumDone = false,
            ),
        )
        useCase { }
        assertThat(recordRepository.backfillImagesToFilesCount).isEqualTo(0)
        assertThat(recordRepository.compactDatabaseIfNeededCount).isEqualTo(1)
    }

    @Test
    fun all_done_then_only_orphan_scan() = runTest {
        settingRepository.setTempKeys(
            TempKeysModel(
                db9To10DataMigrated = true, preferenceSplit = true,
                finalAmountNetRecalcDone = true, imagesToFilesMigrated = true, dbVacuumDone = true,
            ),
        )
        useCase { }
        assertThat(recordRepository.recalculateAllFinalAmountCount).isEqualTo(0)
        assertThat(recordRepository.backfillImagesToFilesCount).isEqualTo(0)
        assertThat(recordRepository.compactDatabaseIfNeededCount).isEqualTo(0)
        assertThat(recordRepository.cleanupOrphanImageFilesCount).isEqualTo(1)
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :core:domain:testDebugUnitTest --tests "*RunStartupMaintenanceUseCaseTest" --no-daemon --offline --console=plain`
Expected: 编译失败（`RunStartupMaintenanceUseCase` 未定义）

- [ ] **Step 3: 实现 UseCase**（gate 语义逐行搬自 `LauncherContentViewModel.kt:70-128`；`migrateAfter9To10` 分支**不**包 try/catch，保留现有「异常逃逸触发全局 finishAllActivity」语义）

```kotlin
// RunStartupMaintenanceUseCase.kt（含 Apache header）
package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.data.repository.SettingRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * 启动维护编排（从 LauncherContentViewModel.init 收编）：迁移/净自付重算/图片 backfill/DB 压实/孤儿扫描。
 * [onFirstScreenReady] 在正确时机回调决定首屏放行——gate 语义与原 init 逐行等价，逻辑零改动。
 */
class RunStartupMaintenanceUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
    private val settingRepository: SettingRepository,
) {
    suspend operator fun invoke(onFirstScreenReady: () -> Unit) {
        val tempKeys = settingRepository.tempKeysModel.first()
        if (!tempKeys.db9To10DataMigrated) {
            // final_amount 全为 Migration9To10 DEFAULT 0 → 必须先迁移后放行（不包 try/catch：异常逃逸触发全局
            // UncaughtExceptionHandler.finishAllActivity，标志未置位下次幂等重试，与原 init 一致）
            recordRepository.migrateAfter9To10()
            onFirstScreenReady()
        } else {
            onFirstScreenReady()
            if (!tempKeys.finalAmountNetRecalcDone) {
                runCatchingMaintenance("netRecalc") { recordRepository.recalculateAllFinalAmount() }
            }
            if (!tempKeys.imagesToFilesMigrated) {
                runCatchingMaintenance("backfill") { recordRepository.backfillImagesToFiles() }
            }
            if (tempKeys.imagesToFilesMigrated && !tempKeys.dbVacuumDone) {
                runCatchingMaintenance("compact") { recordRepository.compactDatabaseIfNeeded() }
            }
        }
        runCatchingMaintenance("orphanScan") { recordRepository.cleanupOrphanImageFiles() }
    }

    /** 后台维护步骤统一容错：CancellationException 先 rethrow（不吞协程取消），其余记日志不连累后续。 */
    private suspend fun runCatchingMaintenance(tag: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            this.logger().e(t, "startup maintenance <$tag> failed, will retry next launch")
        }
    }
}
```

> 注：`logger()` 扩展在 `core:common`；若 `this.logger()` 在无 tag 类上不可用，用 `cn.wj.android.cashbook.core.common.ext.logger()` 的既有调用式（参照 RecordRepositoryImpl 的 `logger()` 用法）。确认 `core:domain` build.gradle 已依赖 `core:common`（现有 UseCase 已用）。

- [ ] **Step 4: 运行确认通过**

Run: `./gradlew :core:domain:testDebugUnitTest --tests "*RunStartupMaintenanceUseCaseTest" --no-daemon --offline --console=plain`
Expected: PASS（4 用例）

- [ ] **Step 5: spotless + commit**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
git add core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/RunStartupMaintenanceUseCase.kt core/domain/src/test/kotlin/cn/wj/android/cashbook/domain/usecase/RunStartupMaintenanceUseCaseTest.kt
git commit -m "[feat|core:domain|首屏维护][公共]M2 抽 RunStartupMaintenanceUseCase 收编启动维护编排（onFirstScreenReady 回调保 gate 放行语义）+ 4 JVM 用例"
```

---

### Task 3: M2 `LauncherContentViewModel` 接线（feature:records）

**Files:**
- Modify: `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/LauncherContentViewModel.kt`（构造注入 `RunStartupMaintenanceUseCase`；init `:70-130` 退化）
- Modify: `feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/LauncherContentViewModelTest.kt`（9 处构造更新 + 编排断言迁移 + 放行测试）

**Interfaces:**
- Consumes: `RunStartupMaintenanceUseCase`（Task 2）

- [ ] **Step 1: 改 ViewModel 构造 + init**（`@HiltViewModel` 构造新增 `private val runStartupMaintenance: RunStartupMaintenanceUseCase`；init `:70-130` 整段替换）

原 init（`:70-130` 整段）替换为：
```kotlin
init {
    viewModelScope.launch {
        runStartupMaintenance { _migrationCompleted.value = true }
    }
}
```
删除原 init 内 5 段 try/catch（`import` 若 `CancellationException`/`first` 等已无其他引用则清理，避免未用 import lint 警告；`viewModelScope`/`launch` 保留）。

- [ ] **Step 2: 改测试构造 + 迁移编排断言**

（a）`setup()`（`:53-64`）构造 VM 时注入 UseCase：
```kotlin
viewModel = LauncherContentViewModel(
    booksRepository = booksRepository,
    settingRepository = settingRepository,
    recordRepository = recordRepository,
    runStartupMaintenance = RunStartupMaintenanceUseCase(recordRepository, settingRepository),
)
```
（b）其余 8 处内联构造（`:78, 100, 122, 144, 166, 195, 222, 538` 附近）同样补 `runStartupMaintenance = RunStartupMaintenanceUseCase(<该处的 recordRepository>, settingRepository)` 参数（各处用其局部 `freshRecordRepository`）。

（c）**7 处编排 @Test**（`when_db9To10_done_but_net_recalc_not_done`/`when_imagesToFiles_not_migrated`/`when_imagesToFiles_already_migrated`/`launcher_always_runs_orphan_image_cleanup`/`when_net_recalc_running_then_uiState_already_success`/`when_net_recalc_throws_then_uiState_success_and_no_crash`/`when_db9To10_migrating_then_uiState_loading_until_done`）——其编排 count/signal 断言已在 Task 2 的 `RunStartupMaintenanceUseCaseTest` 等价覆盖。删除这 7 个纯编排断言 @Test（它们验证的是已搬走的逻辑）。

（d）**新增 1 个 VM 放行测试（防节点1 F3 盲区：忘调 UseCase 会卡 Loading 却零失败测试）**：

```kotlin
@Test
fun init_invokes_useCase_and_releases_first_screen() = runTest {
    settingRepository.setTempKeys(
        TempKeysModel(
            db9To10DataMigrated = true, preferenceSplit = true,
            finalAmountNetRecalcDone = true, imagesToFilesMigrated = true, dbVacuumDone = true,
        ),
    )
    val fresh = FakeRecordRepository()
    val vm = LauncherContentViewModel(
        booksRepository = booksRepository,
        settingRepository = settingRepository,
        recordRepository = fresh,
        runStartupMaintenance = RunStartupMaintenanceUseCase(fresh, settingRepository),
    )
    // UseCase 确被调用（orphanScan 每分支跑一次）+ 首屏放行（_migrationCompleted 经 uiState 反映）
    assertThat(fresh.cleanupOrphanImageFilesCount).isEqualTo(1)
    // 断言 uiState 已放行（非 Loading）——按现有 uiState 断言方式（参照被删测试里的 uiState 用法）
}
```
> 保留原有非编排测试（如 `when_monthStartDay_configured`、汇总流相关），只补 UseCase 参数。`import` 补 `RunStartupMaintenanceUseCase`。

- [ ] **Step 3: 运行确认通过**

Run: `./gradlew :feature:records:testDebugUnitTest --tests "*LauncherContentViewModelTest" --no-daemon --offline --console=plain`
Expected: PASS（放行测试 + 保留的非编排测试）

- [ ] **Step 4: 编译 app Hilt 全图**（UseCase 注入进 VM，验证 Hilt provide）

Run: `./gradlew :app:compileOnlineDebugKotlin --no-daemon --offline --console=plain`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: spotless + commit**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
git add feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/LauncherContentViewModel.kt feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/LauncherContentViewModelTest.kt
git commit -m "[refactor|feature:records|首屏维护][公共]M2 LauncherContentViewModel init 退化为单行 UseCase 调用（编排断言迁 UseCaseTest + 补放行防盲区测试）"
```

---

### Task 4: M1 DAO 层——`deleteRecordsBatch` 返回实删图 path（core:database + core:data）

**Files:**
- Modify: `core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/dao/TransactionDao.kt`
- Modify: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/testdoubles/FakeTransactionDao.kt`
- Modify: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/testdoubles/TransactionDaoLogicTest.kt`
- Modify: `core/database/src/androidTest/kotlin/cn/wj/android/cashbook/core/database/dao/RecordDaoTest.kt`（或 TransactionDaoTest，按现有级联测试落点）

**Interfaces:**
- Produces: `TransactionDao.queryImagePathsByRecordIds(ids: List<Long>): List<String>`；`deleteRecordsBatch/deleteBookTransaction/deleteAssetRelatedData/deleteRecordTransaction(×2)` 返回 `List<String>`（实删记录的托管图 path）

- [ ] **Step 1: 加投影 DAO + FakeTransactionDao 忠实 override**（先让编译自洽）

`TransactionDao.kt`（紧邻 `:710` `deleteImageRelationsByRecordIds`）新增：
```kotlin
/** 一组记录的图片相对路径（仅 image_path 投影，不物化 BLOB）；deleteRecordsBatch 删前收集删文件用 */
@Query("SELECT image_path FROM db_image_with_related WHERE record_id IN (:ids)")
suspend fun queryImagePathsByRecordIds(ids: List<Long>): List<String>
```
`FakeTransactionDao.kt`（`imageWithRecords` 已建模，`:42`）新增忠实 override：
```kotlin
override suspend fun queryImagePathsByRecordIds(ids: List<Long>): List<String> =
    imageWithRecords.filter { it.recordId in ids }.map { it.path }
```

- [ ] **Step 2: 改 `deleteRecordsBatch` + 4 路透传返回类型（先占位 `emptyList()` 让签名先立）**

`deleteRecordsBatch`（`:728`）签名改 `: List<String>`，方法体末尾先 `return emptyList()`（下一步实现收集）。
`deleteAssetRelatedData`（`:829`）→ `= deleteRecordsBatch(queryRecordsByAssetId(assetId))`（表达式体直接返回）。
`deleteRecordTransaction(record: RecordTable)`（`:647`）→ `= deleteRecordsBatch(listOf(record))`。
`deleteBookTransaction`（`:775`）→ 首步 `val paths = deleteRecordsBatch(queryRecordListByBookId(bookId))`，其余删除语句保留，末尾 `return paths`，签名 `: List<String>`。
`deleteRecordTransaction(recordId: Long?)`（`:576`）→ 签名 `: List<String>`，`null` 分支 `return emptyList()`，末尾 `return deleteRecordTransaction(record)`。

- [ ] **Step 3: 写失败测试**（JVM，`TransactionDaoLogicTest.kt` 新增；FakeTransactionDao 跑真实 `deleteRecordsBatch` 默认体 + 忠实 `queryImagePathsByRecordIds`）

```kotlin
@Test
fun deleteRecordsBatch_returns_managed_image_paths_of_deleted_records() = runTest {
    val dao = FakeTransactionDao()
    // 构造两条记录 + 各自图片（参照本文件既有 createRecord/insertRelatedImages 用法）
    val r1 = createRecord(id = 1L)   // 本文件既有 helper
    val r2 = createRecord(id = 2L)
    dao.insertRecord(r1); dao.insertRecord(r2)   // 用本文件既有插入方式
    dao.insertRelatedImages(listOf(
        imageRow(recordId = 1L, path = "record_images/a.jpg"),
        imageRow(recordId = 2L, path = "record_images/b.jpg"),
    ))

    val paths = dao.deleteRecordsBatch(listOf(r1, r2))

    assertThat(paths).containsExactly("record_images/a.jpg", "record_images/b.jpg")
}

@Test
fun deleteRecordsBatch_no_images_returns_empty() = runTest {
    val dao = FakeTransactionDao()
    val r1 = createRecord(id = 1L)
    dao.insertRecord(r1)
    assertThat(dao.deleteRecordsBatch(listOf(r1))).isEmpty()
}
```
> `imageRow`/`createRecord`/`insertRecord` 用 `TransactionDaoLogicTest.kt`/`FakeTransactionDao.kt` 既有构造式（读文件对齐；`ImageWithRelatedTable` 构造参照 `FakeTransactionDao.insertRelatedImages:97` 处）。

- [ ] **Step 4: 运行确认失败**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*TransactionDaoLogicTest" --no-daemon --offline --console=plain`
Expected: FAIL（返回 `emptyList()`，断言 path 不匹配）

- [ ] **Step 5: 实现收集逻辑**（`deleteRecordsBatch` 内、`:749` 删关联循环**之前**，chunk+flatMap 收集，末尾 return）

在 `:748` `val idList = deletedIds.toList()` 之后、`:749` chunk 删关联之前插入：
```kotlin
// 删关联前捕获实删记录的托管图 path（关联删后无法查）；chunk 防 SQLite 变量上限（节点1 F4）
val imagePaths = idList.chunked(DELETE_IN_CHUNK_SIZE).flatMap { queryImagePathsByRecordIds(it) }
```
方法体末尾 `:766` 存活簇重算后 `return imagePaths`（替换 Step 2 的占位 `return emptyList()`）。

- [ ] **Step 6: 运行确认通过**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*TransactionDaoLogicTest" --no-daemon --offline --console=plain`
Expected: PASS

- [ ] **Step 7: androidTest 级联返回**（`RecordDaoTest`/`TransactionDaoTest` 新增，本机 compile-verified、CI 真机兜底）

新增用例（真实 Room DAO）：
- `deleteRecordsBatch` 返回含 into_asset 转账入账侧记录的图 path；
- 删资产返回 path 与旧 `queryImagePathsByAssetId` 等价、**不含**他资产图（isolation 负断言，承接被删的 `RecordDaoTest:862`）；
- 删账本同理（承接 `:882`）；
- chunk 边界：插 >900 条带图记录，`deleteRecordsBatch` 不抛 `too many SQL variables`、返回全部 path。

- [ ] **Step 8: 编译验证**（Room KSP 验列名 + Fake 抽象成员）

Run: `./gradlew :core:database:compileDebugKotlin :core:data:compileDebugUnitTestKotlin --no-daemon --offline --console=plain`
Expected: BUILD SUCCESSFUL（若报 `no such column: path` → 列名写错，应 `image_path`）

- [ ] **Step 9: spotless + commit**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
git add core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/dao/TransactionDao.kt core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/testdoubles/FakeTransactionDao.kt core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/testdoubles/TransactionDaoLogicTest.kt core/database/src/androidTest/kotlin/cn/wj/android/cashbook/core/database/dao/RecordDaoTest.kt
git commit -m "[feat|core:database|删除图片单一真源][公共]M1 deleteRecordsBatch 事务内 chunk 收集实删记录图 path 并返回，4 路透传（列名 image_path + FakeTransactionDao 忠实 override + androidTest 含 into_asset/isolation/chunk 边界）"
```

---

### Task 5: M1 调用方切换 + 孤儿 DAO 清理（core:data + core:database）

**Files:**
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordRepositoryImpl.kt`（删资产 `:513-522`、单删 `:121-128`）
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/impl/BooksRepositoryImpl.kt`（删账本 `:76`）
- Modify: `core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/dao/RecordDao.kt`（删 `queryImagePathsByAssetId:514`/`ByBookId:526`）
- Modify: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/testdoubles/FakeRecordDao.kt`（删两孤儿 override `:479/:485`）
- Modify: `core/database/src/androidTest/kotlin/cn/wj/android/cashbook/core/database/dao/RecordDaoTest.kt`（删 `:845/:866` 两孤儿用例，负断言已在 Task 4 迁入）
- Modify: 删资产/删账本 Repository JVM 测试（补级联返回删文件 + F5 负用例）

**Interfaces:**
- Consumes: Task 4 的 `deleteRecordsBatch/deleteAssetRelatedData/deleteBookTransaction/deleteRecordTransaction` 返回 `List<String>`

- [ ] **Step 1: 写失败/守护测试**（core:data JVM，参照现有 `RecordRepositoryImpl`/`BooksRepositoryImpl` 测试；断言删资产/删账本/单删后被删记录的托管图文件已删 + F5 恶意 path 不删）

在删资产测试（`RecordRepositoryImplTest` 或对应文件）补：
```kotlin
@Test
fun deleteRecordsWithAsset_deletes_managed_image_files_via_cascade_return() = runTest {
    // FakeTransactionDao 挂图 record_images/a.jpg 属该资产记录；FakeRecordImageFileStorage 记录 delete 调用
    // 删资产后：storage.delete("record_images/a.jpg") 被调用一次
    // 断言参照现有 storage 桩的 deleted 集合
}

@Test
fun cascade_return_with_traversal_path_not_deleted() = runTest {
    // 级联返回含 "record_images/../evil"（模拟恶意备份注入）
    // 经 deleteManagedImageFiles 的 isManaged 过滤 → storage.delete 不含该 path（F5）
}
```
> storage 桩：确认 `core/data` test 是否已有 `FakeRecordImageFileStorage`（记录 `delete` 调用集）；无则新建最小桩（`isManaged` 复刻 `isManagedImagePath`：`startsWith("record_images/") && !contains("..")`）。

- [ ] **Step 2: 实现调用方切换**（安全不变量：级联返回必经 `deleteManagedImageFiles`，禁直接 `storage.delete`）

删资产 `RecordRepositoryImpl.kt:513-522`：
```kotlin
override suspend fun deleteRecordsWithAsset(assetId: Long): Unit =
    withContext(coroutineContext) {
        val imagePaths = transactionDao.deleteAssetRelatedData(assetId)
        deleteManagedImageFiles(imagePaths, recordImageFileStorage)
        recordDataVersion.updateVersion()
    }
```
单删 `RecordRepositoryImpl.kt:121-128`：
```kotlin
override suspend fun deleteRecord(recordId: Long) = withContext(coroutineContext) {
    val imagePaths = transactionDao.deleteRecordTransaction(recordId)
    deleteManagedImageFiles(imagePaths, recordImageFileStorage)
    recordDataVersion.updateVersion()
    assetDataVersion.updateVersion()
}
```
删账本 `BooksRepositoryImpl.kt:76` 附近：把 `val imagePaths = recordDao.queryImagePathsByBookId(id)` + 后续 `transactionDao.deleteBookTransaction(id)` 改为 `val imagePaths = transactionDao.deleteBookTransaction(id)`（保留其后 `deleteManagedImageFiles(imagePaths, ...)`）。

- [ ] **Step 3: 删孤儿 DAO + Fake override + androidTest 用例**

- `RecordDao.kt`：删 `queryImagePathsByAssetId`（`:504-514`）、`queryImagePathsByBookId`（`:516-526`）；`queryImagePathsByRecordId`（`:528-530`）**保留**（编辑路径用）。
- `FakeRecordDao.kt`：删 `:479/:485` 两 override；保留 `queryImagePathsByRecordId`（`:490`）。
- `RecordDaoTest.kt`：删 `when_queryImagePathsByAssetId_...`（`:845`）、`when_queryImagePathsByBookId_...`（`:866`）两用例（负断言已在 Task 4 迁入级联测试）。

- [ ] **Step 4: 运行测试 + 编译验证**

Run:
```bash
./gradlew :core:data:testDebugUnitTest --no-daemon --offline --console=plain
./gradlew :core:database:compileDebugKotlin :app:compileOnlineDebugKotlin --no-daemon --offline --console=plain
```
Expected: PASS + BUILD SUCCESSFUL（孤儿删除后无残留调用；Repository 对外签名不变故上游零影响）

- [ ] **Step 5: spotless + commit**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
git add core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordRepositoryImpl.kt core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/impl/BooksRepositoryImpl.kt core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/dao/RecordDao.kt core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/testdoubles/FakeRecordDao.kt core/database/src/androidTest/kotlin/cn/wj/android/cashbook/core/database/dao/RecordDaoTest.kt core/data/src/test/
git commit -m "[refactor|core:data|删除图片单一真源][公共]M1 删资产/删账本/单删改用级联返回删图 + 删 queryImagePathsByAssetId/ByBookId 孤儿 DAO（安全不变量：经 deleteManagedImageFiles isManaged 守卫 + F5 负用例）"
```

---

## 完整链路验证（全 Task 完成后）

```bash
# 各模块单测
./gradlew :core:data:testDebugUnitTest :core:domain:testDebugUnitTest :feature:records:testDebugUnitTest --no-daemon --offline --console=plain
# app 跨模块 Hilt 全图
./gradlew :app:compileOnlineDebugKotlin --no-daemon --offline --console=plain
# spotless + lint
./gradlew spotlessCheck --init-script gradle/init.gradle.kts --no-configuration-cache
./gradlew :core:data:lintRelease :feature:records:lintRelease --no-daemon --console=plain
```
- androidTest（M1 级联/chunk 真机）本机代理 TLS 允许时可跑 `:core:database:connectedDebugAndroidTest`，否则靠 PR 的 CI androidTest 兜底。
- 全绿后 → 节点2 `comprehensive-review:full-review` → worktree merge 回本地 main（`--ff-only`）→ push → 开 PR（CI 顺带录 backlog #3 缺失截图基线）。

---

## Self-Review

**Spec coverage**：
- #2 流式化 → Task 1 ✓（含 F6 name 净化）
- M2 UseCase → Task 2 ✓；VM 接线 + 测试迁移（F3）→ Task 3 ✓
- M1 级联返回单一真源 → Task 4（DAO 层 + F1 列名 + F4 chunk + F2 Fake override）+ Task 5（调用方 + 孤儿清理 + F5 守卫 + F7 负断言）✓
- 非目标（getWebFile/incremental_vacuum/编辑路径）→ 不在任何 Task，符合 spec YAGNI ✓

**Placeholder scan**：测试代码中 `createRecord`/`imageRow`/`FakeRecordImageFileStorage` 标注为「参照现有构造式/确认是否已有桩」——因这些是模块内既有 helper，implementer 须读对应文件对齐（非臆造签名）；已显式指出落点文件与参照行。androidTest 具体断言（Task 4 Step 7）给了用例语义清单，未逐行展开（本机不跑、CI 兜底，逐行代码留 implementer 按 RecordDaoTest 现有风格写）。

**Type consistency**：`queryImagePathsByRecordIds(ids: List<Long>): List<String>` 在 Task 4 定义、Task 5 不直接调（走 deleteRecordsBatch 返回）；`RunStartupMaintenanceUseCase(recordRepository, settingRepository)` 构造签名 Task 2 定义、Task 3 一致引用；VM 构造新增 `runStartupMaintenance` 参数 Task 3 全处一致。
