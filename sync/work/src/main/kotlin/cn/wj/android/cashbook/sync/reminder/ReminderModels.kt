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

import androidx.annotation.StringRes

/**
 * 一条待发送的提醒
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2026/6/26
 */
sealed interface ReminderItem {
    /** 信用卡账单日提醒 */
    data class CreditCardBilling(val assetId: Long, val assetName: String) : ReminderItem

    /** 信用卡还款日提醒 */
    data class CreditCardRepayment(val assetId: Long, val assetName: String) : ReminderItem

    /** 待报销提醒 */
    data class Reimbursement(val count: Int) : ReminderItem
}

/**
 * 信用卡提醒所需信息（从 AssetModel 提取的值类型，便于纯函数测试）
 *
 * @param assetId 资产 id
 * @param name 资产名称
 * @param billingDate 账单日（字符串，如 "15"，可能为空/脏值）
 * @param repaymentDate 还款日（字符串，如 "5"，可能为空/脏值）
 */
data class CreditCardReminderInfo(
    val assetId: Long,
    val name: String,
    val billingDate: String,
    val repaymentDate: String,
)

/**
 * 一条通知的展示规格（从 [ReminderItem] 派生的值类型，便于纯函数测试）。
 *
 * @param textRes 文案字符串资源 id
 * @param formatArgs 文案格式化参数（assetName 或 count）
 * @param target 深链目标（REMINDER_TARGET_ASSET / REMINDER_TARGET_REIMBURSEMENT）
 * @param assetId 深链资产 id（非资产类为 -1）
 */
data class NotificationSpec(
    @StringRes val textRes: Int,
    val formatArgs: List<Any>,
    val target: Int,
    val assetId: Long,
)
