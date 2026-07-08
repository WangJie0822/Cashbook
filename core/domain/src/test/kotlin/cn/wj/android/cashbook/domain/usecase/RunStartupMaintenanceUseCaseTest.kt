/*
 * Copyright 2021 The Cashbook Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

/**
 * [RunStartupMaintenanceUseCase] 编排测试：验证 gate 放行时机与各维护步骤按 tempKeys 标志触发/跳过。
 *
 * 编排逻辑从 LauncherContentViewModel.init 收编而来，此处以 UseCase 单元覆盖（原 VM 编排断言迁入本类）。
 */
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
        settingRepository.setTempKeys(
            TempKeysModel(db9To10DataMigrated = false, preferenceSplit = false),
        )
        var ready = false

        useCase { ready = true }

        // migrate 在放行前调用；此分支不跑后台维护（净自付/backfill/compact）
        assertThat(recordRepository.migrateAfter9To10Count).isEqualTo(1)
        assertThat(ready).isTrue()
        assertThat(recordRepository.recalculateAllFinalAmountCount).isEqualTo(0)
        assertThat(recordRepository.backfillImagesToFilesCount).isEqualTo(0)
        // orphanScan 每分支兜底
        assertThat(recordRepository.cleanupOrphanImageFilesCount).isEqualTo(1)
    }

    @Test
    fun db9To10_done_all_flags_pending_then_full_maintenance() = runTest {
        settingRepository.setTempKeys(
            TempKeysModel(
                db9To10DataMigrated = true,
                preferenceSplit = true,
                finalAmountNetRecalcDone = false,
                imagesToFilesMigrated = false,
                dbVacuumDone = false,
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
                db9To10DataMigrated = true,
                preferenceSplit = true,
                finalAmountNetRecalcDone = true,
                imagesToFilesMigrated = true,
                dbVacuumDone = false,
            ),
        )

        useCase { }

        assertThat(recordRepository.recalculateAllFinalAmountCount).isEqualTo(0)
        assertThat(recordRepository.backfillImagesToFilesCount).isEqualTo(0)
        assertThat(recordRepository.compactDatabaseIfNeededCount).isEqualTo(1)
        assertThat(recordRepository.cleanupOrphanImageFilesCount).isEqualTo(1)
    }

    @Test
    fun all_done_then_only_orphan_scan() = runTest {
        settingRepository.setTempKeys(
            TempKeysModel(
                db9To10DataMigrated = true,
                preferenceSplit = true,
                finalAmountNetRecalcDone = true,
                imagesToFilesMigrated = true,
                dbVacuumDone = true,
            ),
        )

        useCase { }

        assertThat(recordRepository.recalculateAllFinalAmountCount).isEqualTo(0)
        assertThat(recordRepository.backfillImagesToFilesCount).isEqualTo(0)
        assertThat(recordRepository.compactDatabaseIfNeededCount).isEqualTo(0)
        assertThat(recordRepository.cleanupOrphanImageFilesCount).isEqualTo(1)
    }
}
