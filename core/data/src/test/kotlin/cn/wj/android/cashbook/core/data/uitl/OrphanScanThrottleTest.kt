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

package cn.wj.android.cashbook.core.data.uitl

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** [shouldRunOrphanScan] 孤儿图片扫描节流判定纯函数单测。 */
class OrphanScanThrottleTest {

    private val week = ORPHAN_SCAN_THROTTLE_MS

    @Test
    fun firstEver_runs() {
        // lastScanMs=0（从未扫）→ 立即扫
        assertThat(shouldRunOrphanScan(lastScanMs = 0L, nowMs = 1_000L, throttleMs = week)).isTrue()
    }

    @Test
    fun withinWindow_skips() {
        // 距上次不足窗口 → 跳过
        assertThat(shouldRunOrphanScan(lastScanMs = 1_000L, nowMs = 1_000L + week - 1, throttleMs = week)).isFalse()
    }

    @Test
    fun overWindow_runs() {
        // 距上次达到窗口 → 扫
        assertThat(shouldRunOrphanScan(lastScanMs = 1_000L, nowMs = 1_000L + week, throttleMs = week)).isTrue()
    }
}
