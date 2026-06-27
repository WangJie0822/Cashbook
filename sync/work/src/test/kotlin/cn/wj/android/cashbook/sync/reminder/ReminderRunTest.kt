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

package cn.wj.android.cashbook.sync.reminder

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** [reminderRun] 编排纯函数单测（纯 JVM）。 */
class ReminderRunTest {

    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")

    private fun startOfDayMs(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atStartOfDay(zone).toInstant().toEpochMilli()

    /** 当日 10:00 的毫秒（模拟 todayMs）。 */
    private fun atTenMs(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atTime(10, 0).atZone(zone).toInstant().toEpochMilli()

    private fun card(billing: String, repay: String) =
        CreditCardReminderInfo(1L, "招行", billing, repay)

    @Test
    fun bothDisabled_returnsEmptyAndNoCheckpoint() {
        val run = reminderRun(
            lastReminderCheckMs = 0L,
            todayMs = atTenMs(2024, 1, 15),
            zone = zone,
            creditCardEnable = false,
            reimbursementEnable = false,
            creditCards = listOf(card("15", "5")),
            monthStartDay = 1,
            reimbursableCount = 3,
        )
        assertThat(run.items).isEmpty()
        assertThat(run.newLastCheckMs).isNull()
    }

    @Test
    fun datesEmpty_returnsEmptyAndNoCheckpoint() {
        // lastCheck == 今天 → reminderCheckDates 返回空
        val run = reminderRun(
            lastReminderCheckMs = startOfDayMs(2024, 1, 15),
            todayMs = atTenMs(2024, 1, 15),
            zone = zone,
            creditCardEnable = true,
            reimbursementEnable = false,
            creditCards = listOf(card("15", "5")),
            monthStartDay = 1,
            reimbursableCount = 0,
        )
        assertThat(run.items).isEmpty()
        assertThat(run.newLastCheckMs).isNull()
    }

    @Test
    fun firstRun_billingMatch_emitsItemAndAdvancesCheckpoint() {
        val run = reminderRun(
            lastReminderCheckMs = 0L, // 首次 → dates = [today]
            todayMs = atTenMs(2024, 1, 15),
            zone = zone,
            creditCardEnable = true,
            reimbursementEnable = false,
            creditCards = listOf(card("15", "5")),
            monthStartDay = 1,
            reimbursableCount = 0,
        )
        assertThat(run.items).containsExactly(ReminderItem.CreditCardBilling(1L, "招行"))
        assertThat(run.newLastCheckMs).isEqualTo(startOfDayMs(2024, 1, 15))
    }

    @Test
    fun backfill_multiDay_onlyMatchingDayEmits() {
        // lastCheck = Jan10 → dates = [Jan11, Jan12, Jan13]; billing "12" 仅 Jan12 命中
        val run = reminderRun(
            lastReminderCheckMs = startOfDayMs(2024, 1, 10),
            todayMs = atTenMs(2024, 1, 13),
            zone = zone,
            creditCardEnable = true,
            reimbursementEnable = false,
            creditCards = listOf(card("12", "28")),
            monthStartDay = 1,
            reimbursableCount = 0,
        )
        assertThat(run.items).containsExactly(ReminderItem.CreditCardBilling(1L, "招行"))
        assertThat(run.newLastCheckMs).isEqualTo(startOfDayMs(2024, 1, 13))
    }

    @Test
    fun datesNonEmpty_noMatch_advancesCheckpointWithEmptyItems() {
        val run = reminderRun(
            lastReminderCheckMs = 0L, // dates = [today=Jan15]
            todayMs = atTenMs(2024, 1, 15),
            zone = zone,
            creditCardEnable = true,
            reimbursementEnable = false,
            creditCards = listOf(card("20", "25")), // 当日无命中
            monthStartDay = 1,
            reimbursableCount = 0,
        )
        assertThat(run.items).isEmpty()
        assertThat(run.newLastCheckMs).isEqualTo(startOfDayMs(2024, 1, 15))
    }
}
