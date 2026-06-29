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

package cn.wj.android.cashbook.core.data.repository.impl

import cn.wj.android.cashbook.core.data.uitl.DatabaseCompactor
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

/** [runDbCompactIfNeeded] C-robust live VACUUM 编排逻辑单测。 */
class CompactDatabaseIfNeededTest {

    private class FakeDatabaseCompactor(
        private val size: Long,
        private val free: Long,
        private val vacuumResult: Boolean,
    ) : DatabaseCompactor {
        var vacuumCalls = 0
            private set

        override suspend fun databaseSizeBytes(): Long = size
        override suspend fun freeSpaceBytes(): Long = free
        override suspend fun vacuum(): Boolean {
            vacuumCalls++
            return vacuumResult
        }
    }

    @Test
    fun alreadyDone_skips() = runTest {
        val compactor = FakeDatabaseCompactor(size = 100, free = 1000, vacuumResult = true)
        var doneSet = false
        runDbCompactIfNeeded(alreadyDone = true, compactor = compactor) { doneSet = true }
        assertThat(compactor.vacuumCalls).isEqualTo(0) // 已完成不重跑
        assertThat(doneSet).isFalse()
    }

    @Test
    fun insufficientSpace_skips_noFlag() = runTest {
        // free 落在 1×~2× DB 之间：VACUUM 需 ~2×，应跳过（旧 1× 预检会误放行）
        val compactor = FakeDatabaseCompactor(size = 1000, free = 1500, vacuumResult = true)
        var doneSet = false
        runDbCompactIfNeeded(alreadyDone = false, compactor = compactor) { doneSet = true }
        assertThat(compactor.vacuumCalls).isEqualTo(0) // 空间不足跳过、不置位（下次重试）
        assertThat(doneSet).isFalse()
    }

    @Test
    fun success_setsFlag() = runTest {
        val compactor = FakeDatabaseCompactor(size = 100, free = 1000, vacuumResult = true)
        var doneSet = false
        runDbCompactIfNeeded(alreadyDone = false, compactor = compactor) { doneSet = true }
        assertThat(compactor.vacuumCalls).isEqualTo(1)
        assertThat(doneSet).isTrue() // 真成功才置位
    }

    @Test
    fun vacuumFails_noFlag() = runTest {
        val compactor = FakeDatabaseCompactor(size = 100, free = 1000, vacuumResult = false)
        var doneSet = false
        runDbCompactIfNeeded(alreadyDone = false, compactor = compactor) { doneSet = true }
        assertThat(compactor.vacuumCalls).isEqualTo(1)
        assertThat(doneSet).isFalse() // VACUUM 失败不置位（下次启动重试）
    }
}
