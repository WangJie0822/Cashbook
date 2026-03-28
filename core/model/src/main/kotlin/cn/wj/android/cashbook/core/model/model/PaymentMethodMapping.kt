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
 * 支付方式到资产的映射
 *
 * @param originalName 原始文本，如"民生银行储蓄卡(1420)"
 * @param matchedAssetId 匹配到的资产 ID，-1 表示未匹配
 * @param matchedAssetName 资产名称（用于展示）
 */
data class PaymentMethodMapping(
    val originalName: String,
    val matchedAssetId: Long,
    val matchedAssetName: String,
)
