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

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** 月起始日合法化：仅 1..28 有效，其余（含 0/越界）归 1。与 RecordSettings normalizeMonthStartDay 同语义。 */
private fun normalizeStartDay(day: Int): Int = if (day in 1..28) day else 1

/** 解析信用卡日期字符串为 1..31 的合法日；非法/越界返回 null（跳过该卡）。 */
private fun parseCardDay(raw: String): Int? = raw.trim().toIntOrNull()?.takeIf { it in 1..31 }

/**
 * 给定逻辑日期与数据，计算当日应发的提醒列表（纯函数，无 Android/IO 依赖）。
 *
 * - N1：[creditCardEnable] 时，每张卡 billingDate/repaymentDate 解析为日，命中 [date] 的 day-of-month 即发；
 *   解析失败/越界跳过该卡（不中断整批）；当月无该日（如 "30" 在 2 月）自然不命中（跳过语义）。
 * - N2：[reimbursementEnable] 且 [date] 的 day-of-month == normalizeStartDay([monthStartDay]) 且 [reimbursableCount] > 0 时发。
 */
internal fun computeReminders(
    date: LocalDate,
    creditCardEnable: Boolean,
    reimbursementEnable: Boolean,
    creditCards: List<CreditCardReminderInfo>,
    monthStartDay: Int,
    reimbursableCount: Int,
): List<ReminderItem> {
    val items = mutableListOf<ReminderItem>()
    val today = date.dayOfMonth
    if (creditCardEnable) {
        creditCards.forEach { card ->
            parseCardDay(card.billingDate)?.let {
                if (it == today) items += ReminderItem.CreditCardBilling(card.assetId, card.name)
            }
            parseCardDay(card.repaymentDate)?.let {
                if (it == today) items += ReminderItem.CreditCardRepayment(card.assetId, card.name)
            }
        }
    }
    if (reimbursementEnable && today == normalizeStartDay(monthStartDay) && reimbursableCount > 0) {
        items += ReminderItem.Reimbursement(reimbursableCount)
    }
    return items
}

/**
 * 计算补发应检查的逻辑日期区间（from 到 today，含两端）。
 *
 * - [lastCheckMs] <= 0（首次）：仅 [today]，不补历史。
 * - 否则 from = max(lastCheckDate+1, today-[maxBackfillDays]+1)；若 from > today 返回空（今天已查过）。
 *
 * 上限 [maxBackfillDays] 防设备长期关机后通知轰炸。
 */
internal fun reminderCheckDates(
    lastCheckMs: Long,
    todayMs: Long,
    zoneId: ZoneId,
    maxBackfillDays: Int = 7,
): List<LocalDate> {
    val today = Instant.ofEpochMilli(todayMs).atZone(zoneId).toLocalDate()
    if (lastCheckMs <= 0L) return listOf(today)
    val lastCheck = Instant.ofEpochMilli(lastCheckMs).atZone(zoneId).toLocalDate()
    val earliest = today.minusDays((maxBackfillDays - 1).toLong())
    var from = lastCheck.plusDays(1)
    if (from.isBefore(earliest)) from = earliest
    if (from.isAfter(today)) return emptyList()
    val dates = mutableListOf<LocalDate>()
    var d = from
    while (!d.isAfter(today)) {
        dates += d
        d = d.plusDays(1)
    }
    return dates
}

/**
 * 由提醒类型派生稳定的通知 id（同一卡每类事件唯一、待报销固定）。
 * billing = [baseId] + assetId*2；repayment = +1；reimbursement = [baseId]。
 * 同一提醒重发用同 id 替换而非堆叠。
 */
internal fun reminderNotificationId(baseId: Int, item: ReminderItem): Int = when (item) {
    is ReminderItem.CreditCardBilling -> baseId + item.assetId.toInt() * 2
    is ReminderItem.CreditCardRepayment -> baseId + item.assetId.toInt() * 2 + 1
    is ReminderItem.Reimbursement -> baseId
}
