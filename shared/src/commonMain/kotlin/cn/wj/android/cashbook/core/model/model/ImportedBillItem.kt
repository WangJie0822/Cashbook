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

package cn.wj.android.cashbook.core.model.model

/**
 * 导入的账单条目（格式无关的中间模型）
 *
 * @param transactionTime 交易时间戳
 * @param transactionType 原始交易类型（如"商户消费"）
 * @param counterparty 交易对方
 * @param description 商品/描述
 * @param direction 收入/支出方向
 * @param amount 金额
 * @param paymentMethod 支付方式原始文本
 * @param status 当前状态
 * @param transactionId 交易单号（用于去重）
 * @param merchantId 商户单号
 * @param remark 备注
 */
data class ImportedBillItem(
    val transactionTime: Long,
    val transactionType: String,
    val counterparty: String,
    val description: String,
    val direction: BillDirection,
    val amount: Double,
    val paymentMethod: String,
    val status: String,
    val transactionId: String,
    val merchantId: String,
    val remark: String,
)

/** 账单收支方向 */
enum class BillDirection {
    /** 收入 */
    INCOME,

    /** 支出 */
    EXPENDITURE,
}
