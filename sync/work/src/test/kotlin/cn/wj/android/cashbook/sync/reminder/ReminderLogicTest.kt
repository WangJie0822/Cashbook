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

/**
 * [computeReminders] 与 [reminderCheckDates] 纯函数单测（纯 JVM，无 Android 依赖）
 */
class ReminderLogicTest {

    private fun card(id: Long, name: String, billing: String, repay: String) =
        CreditCardReminderInfo(id, name, billing, repay)

    @Test
    fun billingDay_match_emitsBilling() {
        val result = computeReminders(
            date = LocalDate.of(2024, 1, 15),
            creditCardEnable = true,
            reimbursementEnable = false,
            creditCards = listOf(card(1L, "招行", "15", "5")),
            monthStartDay = 1,
            reimbursableCount = 0,
        )
        assertThat(result).containsExactly(ReminderItem.CreditCardBilling(1L, "招行"))
    }

    @Test
    fun repaymentDay_match_emitsRepayment() {
        val result = computeReminders(
            LocalDate.of(2024, 1, 5),
            true,
            false,
            listOf(card(1L, "招行", "15", "5")),
            1,
            0,
        )
        assertThat(result).containsExactly(ReminderItem.CreditCardRepayment(1L, "招行"))
    }

    @Test
    fun creditCardDisabled_emitsNothing() {
        val result = computeReminders(
            LocalDate.of(2024, 1, 15),
            false,
            false,
            listOf(card(1L, "招行", "15", "5")),
            1,
            0,
        )
        assertThat(result).isEmpty()
    }

    @Test
    fun monthEnd_dayNotExist_skips() {
        // 2 月无 30 号，billingDate="30" 在 2024-02 任何一天都不命中
        val days = (1..29).map { LocalDate.of(2024, 2, it) }
        days.forEach { d ->
            val result = computeReminders(d, true, false, listOf(card(1L, "卡", "30", "30")), 1, 0)
            assertThat(result).isEmpty()
        }
    }

    @Test
    fun dirtyDate_invalidOrOutOfRange_skipsThatCard() {
        val result = computeReminders(
            LocalDate.of(2024, 1, 15),
            true,
            false,
            creditCards = listOf(
                card(1L, "脏卡", "abc", ""), // 非数字
                card(2L, "越界卡", "0", "99"), // 越界
                card(3L, "正常卡", "15", "5"), // 命中账单
            ),
            monthStartDay = 1,
            reimbursableCount = 0,
        )
        assertThat(result).containsExactly(ReminderItem.CreditCardBilling(3L, "正常卡"))
    }

    @Test
    fun multipleCards_sameBillingDay_emitsEach() {
        val result = computeReminders(
            LocalDate.of(2024, 1, 15),
            true,
            false,
            listOf(card(1L, "A", "15", "1"), card(2L, "B", "15", "2")),
            1,
            0,
        )
        assertThat(result).containsExactly(
            ReminderItem.CreditCardBilling(1L, "A"),
            ReminderItem.CreditCardBilling(2L, "B"),
        )
    }

    @Test
    fun reimbursement_onMonthStartDay_withCount_emits() {
        val result = computeReminders(
            LocalDate.of(2024, 1, 15),
            false,
            true,
            emptyList(),
            monthStartDay = 15,
            reimbursableCount = 3,
        )
        assertThat(result).containsExactly(ReminderItem.Reimbursement(3))
    }

    @Test
    fun reimbursement_zeroCount_emitsNothing() {
        val result = computeReminders(
            LocalDate.of(2024, 1, 15),
            false,
            true,
            emptyList(),
            monthStartDay = 15,
            reimbursableCount = 0,
        )
        assertThat(result).isEmpty()
    }

    @Test
    fun reimbursement_notMonthStartDay_emitsNothing() {
        val result = computeReminders(
            LocalDate.of(2024, 1, 14),
            false,
            true,
            emptyList(),
            monthStartDay = 15,
            reimbursableCount = 3,
        )
        assertThat(result).isEmpty()
    }

    @Test
    fun monthStartDay_zeroNormalizedToOne() {
        // monthStartDay=0 视为 1（自然月），1 号命中
        val result = computeReminders(
            LocalDate.of(2024, 1, 1),
            false,
            true,
            emptyList(),
            monthStartDay = 0,
            reimbursableCount = 2,
        )
        assertThat(result).containsExactly(ReminderItem.Reimbursement(2))
    }

    // ========== reminderCheckDates 补发区间 ==========

    private val zone = java.time.ZoneId.of("UTC")
    private fun ms(d: LocalDate) = d.atStartOfDay(zone).toInstant().toEpochMilli()

    @Test
    fun checkDates_firstRun_onlyToday() {
        val today = LocalDate.of(2024, 1, 10)
        val result = reminderCheckDates(0L, ms(today), zone)
        assertThat(result).containsExactly(today)
    }

    @Test
    fun checkDates_yesterday_coversToday() {
        val today = LocalDate.of(2024, 1, 10)
        // lastCheck=昨天 → 补查从 lastCheck+1=今天 起，即 [今天]
        val result = reminderCheckDates(ms(LocalDate.of(2024, 1, 9)), ms(today), zone)
        assertThat(result).containsExactly(LocalDate.of(2024, 1, 10)).inOrder()
    }

    @Test
    fun checkDates_gap5days_coversGap() {
        val today = LocalDate.of(2024, 1, 10)
        // lastCheck+1=1/6 .. 1/10
        val result = reminderCheckDates(ms(LocalDate.of(2024, 1, 5)), ms(today), zone)
        assertThat(result).containsExactly(
            LocalDate.of(2024, 1, 6),
            LocalDate.of(2024, 1, 7),
            LocalDate.of(2024, 1, 8),
            LocalDate.of(2024, 1, 9),
            LocalDate.of(2024, 1, 10),
        ).inOrder()
    }

    @Test
    fun checkDates_longGap_cappedAt7days() {
        val today = LocalDate.of(2024, 1, 20)
        // 上限 7 天：[1/14 .. 1/20]
        val result = reminderCheckDates(ms(LocalDate.of(2024, 1, 1)), ms(today), zone)
        assertThat(result).hasSize(7)
        assertThat(result.first()).isEqualTo(LocalDate.of(2024, 1, 14))
        assertThat(result.last()).isEqualTo(today)
    }

    @Test
    fun checkDates_alreadyCheckedToday_empty() {
        val today = LocalDate.of(2024, 1, 10)
        val result = reminderCheckDates(ms(today), ms(today), zone)
        assertThat(result).isEmpty()
    }
}
