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

@file:Suppress("unused")

package cn.wj.android.cashbook.core.model.enums

/**
 * 资产分类枚举
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/2
 */
enum class AssetClassificationEnum(val code: Int) {

    // 资金账户
    CASH(0),
    WECHAT(1),
    ALIPAY(2),
    DOUYIN(3),
    BANK_CARD(4),
    OTHER_CAPITAL(5),

    // 信用卡账户
    CREDIT_CARD(6),
    ANT_CREDIT_PAY(7),
    JD_IOUS(8),
    DOUYIN_MONTH(9),
    OTHER_CREDIT_CARD(10),

    // 充值账户
    PHONE_CHARGE(11),
    BUS_CARD(12),
    MEAL_CARD(13),
    MEMBER_CARD(14),
    DEPOSIT(15),
    OTHER_TOP_UP(16),

    // 投资理财账户
    STOCK(17),
    FUND(18),
    OTHER_INVESTMENT_FINANCIAL(19),

    // 债务
    BORROW(20),
    LEND(21),
    DEBT(22),

    // 银行卡
    BANK_CARD_ZG(23),
    BANK_CARD_ZS(24),
    BANK_CARD_GS(25),
    BANK_CARD_NY(26),
    BANK_CARD_JS(27),
    BANK_CARD_JT(28),
    BANK_CARD_YZ(29),
    BANK_CARD_HX(30),
    BANK_CARD_BJ(31),
    BANK_CARD_MS(32),
    BANK_CARD_GD(33),
    BANK_CARD_ZX(34),
    BANK_CARD_GF(35),
    BANK_CARD_PF(36),
    BANK_CARD_XY(37),

    ;

    val isDebt: Boolean
        get() = this in ClassificationTypeEnum.DEBT_ACCOUNT.array

    val isCreditCard: Boolean
        get() = this in ClassificationTypeEnum.CREDIT_CARD_ACCOUNT.array

    val isInvestmentFinancialAccount: Boolean
        get() = this in ClassificationTypeEnum.INVESTMENT_FINANCIAL_ACCOUNT.array

    val hasBankInfo: Boolean
        get() = !isDebt && !isInvestmentFinancialAccount && this !in arrayOf(
            ALIPAY,
            CASH,
            WECHAT,
            JD_IOUS,
            ANT_CREDIT_PAY,
        )

    val isBankCard: Boolean
        get() = this == BANK_CARD || this == CREDIT_CARD

    companion object {
        /** 通过持久化的 code 值查找枚举，与 ordinal 解耦 */
        fun codeOf(code: Int): AssetClassificationEnum {
            return entries.first { it.code == code }
        }

        /** @see codeOf */
        fun ordinalOf(ordinal: Int): AssetClassificationEnum = codeOf(ordinal)
    }
}
