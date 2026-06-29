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

import cn.wj.android.cashbook.core.model.enums.AssetClassificationEnum
import cn.wj.android.cashbook.core.model.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.core.model.model.AssetModel
import cn.wj.android.cashbook.sync.workers.selectReminderCreditCards
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * [selectReminderCreditCards] 纯函数单测：N1 信用卡提醒取数过滤判据。
 *
 * 核心：判据为 type==CREDIT_CARD_ACCOUNT（与 EditAsset UI 显示账单/还款日字段一致），
 * 是旧判据 classification.isCreditCard 的严格超集——额外纳入银行品牌信用卡。
 */
class SelectReminderCreditCardsTest {

    private fun asset(
        id: Long,
        type: ClassificationTypeEnum,
        classification: AssetClassificationEnum,
    ): AssetModel = AssetModel(
        id = id,
        booksId = 1L,
        name = "card$id",
        iconResId = 0,
        totalAmount = 0L,
        billingDate = "15",
        repaymentDate = "5",
        type = type,
        classification = classification,
        invisible = false,
        openBank = "",
        cardNo = "",
        remark = "",
        sort = 0,
        modifyTime = 0L,
        balance = 0L,
    )

    @Test
    fun brandCreditCard_included() {
        val list = selectReminderCreditCards(
            listOf(asset(1L, ClassificationTypeEnum.CREDIT_CARD_ACCOUNT, AssetClassificationEnum.BANK_CARD_ZS)),
        )
        assertThat(list.map { it.assetId }).containsExactly(1L)
    }

    @Test
    fun brandDebitCard_excluded() {
        val list = selectReminderCreditCards(
            listOf(asset(2L, ClassificationTypeEnum.CAPITAL_ACCOUNT, AssetClassificationEnum.BANK_CARD_ZS)),
        )
        assertThat(list).isEmpty()
    }

    @Test
    fun nativeCreditCard_included() {
        val list = selectReminderCreditCards(
            listOf(asset(3L, ClassificationTypeEnum.CREDIT_CARD_ACCOUNT, AssetClassificationEnum.OTHER_CREDIT_CARD)),
        )
        assertThat(list.map { it.assetId }).containsExactly(3L)
    }

    @Test
    fun ordinaryAsset_excluded() {
        val list = selectReminderCreditCards(
            listOf(asset(4L, ClassificationTypeEnum.CAPITAL_ACCOUNT, AssetClassificationEnum.CASH)),
        )
        assertThat(list).isEmpty()
    }
}
