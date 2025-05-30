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
 * 记录设置数据
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2025/4/26
 */
data class RecordSettingsModel(
    /** 当前账本 id */
    val currentBookId: Long,
    /** 默认类型 id */
    val defaultTypeId: Long,
    /** 上次记录资产 id */
    val lastAssetId: Long,
    /** 退款类型 id */
    val refundTypeId: Long,
    /** 报销类型 id */
    val reimburseTypeId: Long,
    /** 信用卡还款类型 id */
    val creditCardPaymentTypeId: Long,
    /** 充值账号计入总资产 */
    val topUpInTotal: Boolean,
)
