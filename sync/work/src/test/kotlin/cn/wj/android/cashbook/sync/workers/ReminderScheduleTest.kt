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

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

/**
 * [initialDelayToNext] 纯函数单测（纯 JVM）
 */
class ReminderScheduleTest {

    private val zone = ZoneId.of("UTC")

    @Test
    fun initialDelay_beforeTargetHour_sameDay() {
        val now = LocalDate.of(2024, 1, 10).atTime(8, 0).atZone(zone).toInstant().toEpochMilli()
        val delay = initialDelayToNext(10, now, zone)
        assertThat(delay).isEqualTo(Duration.ofHours(2).toMillis())
    }

    @Test
    fun initialDelay_afterTargetHour_nextDay() {
        val now = LocalDate.of(2024, 1, 10).atTime(11, 0).atZone(zone).toInstant().toEpochMilli()
        val delay = initialDelayToNext(10, now, zone)
        assertThat(delay).isEqualTo(Duration.ofHours(23).toMillis())
    }
}
