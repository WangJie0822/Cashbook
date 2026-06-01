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
import cn.wj.android.cashbook.core.model.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.core.model.model.AssetModel
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * [BillPaymentMatcher] 支付方式匹配测试（4 阶策略）。
 *
 * 全部脱敏：资产名为占位，卡号后四位用 0000。
 */
class BillPaymentMatcherTest {

    @Test
    fun strategy1_matches_exact_asset_name() {
        val assets = listOf(createAsset(id = 1L, name = "我的零钱"))
        val result = BillPaymentMatcher.matchAll(listOf("我的零钱"), assets)
        assertThat(result.first().matchedAssetId).isEqualTo(1L)
    }

    @Test
    fun strategy2_matches_bank_by_keyword_and_card_suffix() {
        val assets = listOf(
            createAsset(
                id = 5L,
                name = "民生储蓄卡",
                classification = AssetClassificationEnum.BANK_CARD_MS,
                cardNo = "0000",
            ),
        )
        val result = BillPaymentMatcher.matchAll(listOf("民生银行储蓄卡(0000)"), assets)
        assertThat(result.first().matchedAssetId).isEqualTo(5L)
    }

    @Test
    fun strategy3_matches_special_payment_wechat() {
        val assets = listOf(
            createAsset(id = 3L, name = "微信钱包", classification = AssetClassificationEnum.WECHAT),
        )
        val result = BillPaymentMatcher.matchAll(listOf("零钱"), assets)
        assertThat(result.first().matchedAssetId).isEqualTo(3L)
    }

    @Test
    fun strategy4_unmatched_returns_negative_id() {
        val result = BillPaymentMatcher.matchAll(listOf("未知支付方式"), emptyList())
        assertThat(result.first().matchedAssetId).isEqualTo(-1L)
    }

    @Test
    fun matchAll_returns_one_mapping_per_input() {
        val result = BillPaymentMatcher.matchAll(listOf("零钱", "未知"), emptyList())
        assertThat(result).hasSize(2)
    }

    private fun createAsset(
        id: Long,
        name: String,
        classification: AssetClassificationEnum = AssetClassificationEnum.CASH,
        cardNo: String = "",
    ) = AssetModel(
        id = id,
        booksId = 1L,
        name = name,
        iconResId = 0,
        totalAmount = 0L,
        billingDate = "",
        repaymentDate = "",
        type = ClassificationTypeEnum.CAPITAL_ACCOUNT,
        classification = classification,
        invisible = false,
        openBank = "",
        cardNo = cardNo,
        remark = "",
        sort = 0,
        modifyTime = 1704067200000L,
        balance = 0L,
    )
}
