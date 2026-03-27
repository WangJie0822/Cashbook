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

package cn.wj.android.cashbook.core.data.helper

import cn.wj.android.cashbook.core.model.enums.AssetClassificationEnum
import cn.wj.android.cashbook.core.model.model.AssetModel
import cn.wj.android.cashbook.core.model.model.PaymentMethodMapping

/**
 * 支付方式自动匹配器
 *
 * 将微信账单中的支付方式文本（如"民生银行储蓄卡(1420)"、"零钱"）
 * 匹配到 Cashbook 中已有的资产账户。
 *
 * 匹配策略按优先级：
 * 1. 精确匹配资产名称
 * 2. 关键词匹配银行卡（识别银行名称 + 卡号后四位）
 * 3. 匹配 classification 类型（如"零钱" → WECHAT）
 * 4. 未匹配
 */
object BillPaymentMatcher {

    /** 银行关键词 → AssetClassificationEnum 映射 */
    private val BANK_KEYWORD_MAP: Map<String, AssetClassificationEnum> = mapOf(
        "中国银行" to AssetClassificationEnum.BANK_CARD_ZG,
        "招商" to AssetClassificationEnum.BANK_CARD_ZS,
        "工商" to AssetClassificationEnum.BANK_CARD_GS,
        "农业" to AssetClassificationEnum.BANK_CARD_NY,
        "建设" to AssetClassificationEnum.BANK_CARD_JS,
        "交通银行" to AssetClassificationEnum.BANK_CARD_JT,
        "邮政" to AssetClassificationEnum.BANK_CARD_YZ,
        "邮储" to AssetClassificationEnum.BANK_CARD_YZ,
        "华夏" to AssetClassificationEnum.BANK_CARD_HX,
        "北京" to AssetClassificationEnum.BANK_CARD_BJ,
        "民生" to AssetClassificationEnum.BANK_CARD_MS,
        "光大" to AssetClassificationEnum.BANK_CARD_GD,
        "中信" to AssetClassificationEnum.BANK_CARD_ZX,
        "广发" to AssetClassificationEnum.BANK_CARD_GF,
        "浦发" to AssetClassificationEnum.BANK_CARD_PF,
        "兴业" to AssetClassificationEnum.BANK_CARD_XY,
    )

    /** 特殊支付方式关键词 → classification 映射 */
    private val SPECIAL_PAYMENT_MAP: Map<String, AssetClassificationEnum> = mapOf(
        "零钱" to AssetClassificationEnum.WECHAT,
        "微信" to AssetClassificationEnum.WECHAT,
        "花呗" to AssetClassificationEnum.ANT_CREDIT_PAY,
        "支付宝" to AssetClassificationEnum.ALIPAY,
        "京东白条" to AssetClassificationEnum.JD_IOUS,
    )

    /**
     * 批量匹配所有支付方式
     *
     * @param paymentMethods 去重后的支付方式原始文本列表
     * @param assets 当前账本下的所有资产
     * @return 映射结果列表
     */
    fun matchAll(
        paymentMethods: List<String>,
        assets: List<AssetModel>,
    ): List<PaymentMethodMapping> {
        return paymentMethods.map { method ->
            matchSingle(method, assets)
        }
    }

    /**
     * 匹配单个支付方式
     */
    private fun matchSingle(
        paymentMethod: String,
        assets: List<AssetModel>,
    ): PaymentMethodMapping {
        if (paymentMethod.isBlank()) {
            return PaymentMethodMapping(
                originalName = paymentMethod,
                matchedAssetId = -1L,
                matchedAssetName = "",
            )
        }

        // 策略1：精确匹配资产名称
        assets.find { it.name == paymentMethod }?.let { asset ->
            return PaymentMethodMapping(
                originalName = paymentMethod,
                matchedAssetId = asset.id,
                matchedAssetName = asset.name,
            )
        }

        // 策略2：银行关键词匹配（含卡号后四位）
        val cardSuffix = extractCardSuffix(paymentMethod)
        for ((keyword, classification) in BANK_KEYWORD_MAP) {
            if (paymentMethod.contains(keyword)) {
                val matched = if (cardSuffix != null) {
                    // 优先匹配银行类型 + 卡号
                    assets.find {
                        it.classification == classification && it.cardNo.endsWith(cardSuffix)
                    } ?: assets.find { it.classification == classification }
                } else {
                    assets.find { it.classification == classification }
                }
                if (matched != null) {
                    return PaymentMethodMapping(
                        originalName = paymentMethod,
                        matchedAssetId = matched.id,
                        matchedAssetName = matched.name,
                    )
                }
            }
        }

        // 策略3：特殊支付方式关键词匹配
        for ((keyword, classification) in SPECIAL_PAYMENT_MAP) {
            if (paymentMethod.contains(keyword)) {
                assets.find { it.classification == classification }?.let { asset ->
                    return PaymentMethodMapping(
                        originalName = paymentMethod,
                        matchedAssetId = asset.id,
                        matchedAssetName = asset.name,
                    )
                }
            }
        }

        // 策略4：未匹配
        return PaymentMethodMapping(
            originalName = paymentMethod,
            matchedAssetId = -1L,
            matchedAssetName = "",
        )
    }

    /**
     * 从支付方式文本中提取卡号后四位
     *
     * 示例："民生银行储蓄卡(1420)" → "1420"
     */
    private fun extractCardSuffix(text: String): String? {
        val regex = Regex("""\((\d{4})\)""")
        return regex.find(text)?.groupValues?.getOrNull(1)
    }
}
