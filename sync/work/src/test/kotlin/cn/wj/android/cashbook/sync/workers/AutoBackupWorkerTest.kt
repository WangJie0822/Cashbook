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

package cn.wj.android.cashbook.sync.workers

import androidx.work.ListenableWorker
import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryState
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * [mapBackupStateToResult] 备份终态 → WorkManager Result 映射测试（纯 JVM，无需 Robolectric）。
 */
class AutoBackupWorkerTest {

    @Test
    fun success_state_maps_to_success() {
        val result = mapBackupStateToResult(
            BackupRecoveryState.Success(BackupRecoveryState.SUCCESS_BACKUP),
        )
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
    }

    @Test
    fun webdav_failure_maps_to_retry() {
        val result = mapBackupStateToResult(
            BackupRecoveryState.Failed(BackupRecoveryState.FAILED_BACKUP_WEBDAV),
        )
        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
    }

    @Test
    fun blank_path_failure_maps_to_failure() {
        val result = mapBackupStateToResult(
            BackupRecoveryState.Failed(BackupRecoveryState.FAILED_BLANK_BACKUP_PATH),
        )
        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
    }

    @Test
    fun unauthorized_failure_maps_to_failure() {
        val result = mapBackupStateToResult(
            BackupRecoveryState.Failed(BackupRecoveryState.FAILED_BACKUP_PATH_UNAUTHORIZED),
        )
        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
    }
}
