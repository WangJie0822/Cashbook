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

package cn.wj.android.cashbook.core.model.enums

/**
 * 资产分类大类枚举
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/2
 */
enum class ClassificationTypeEnum(val array: Array<AssetClassificationEnum>) {

    /** 资金账户 */
    CAPITAL_ACCOUNT(
        arrayOf(
            AssetClassificationEnum.CASH,
            AssetClassificationEnum.WECHAT,
            AssetClassificationEnum.ALIPAY,
            AssetClassificationEnum.DOUYIN,
            AssetClassificationEnum.BANK_CARD,
            AssetClassificationEnum.OTHER_CAPITAL,
        ),
    ),

    /** 信用卡账户 */
    CREDIT_CARD_ACCOUNT(
        arrayOf(
            AssetClassificationEnum.CREDIT_CARD,
            AssetClassificationEnum.ANT_CREDIT_PAY,
            AssetClassificationEnum.JD_IOUS,
            AssetClassificationEnum.DOUYIN_MONTH,
            AssetClassificationEnum.OTHER_CREDIT_CARD,
        ),
    ),

    /** 充值账户 */
    TOP_UP_ACCOUNT(
        arrayOf(
            AssetClassificationEnum.PHONE_CHARGE,
            AssetClassificationEnum.BUS_CARD,
            AssetClassificationEnum.MEAL_CARD,
            AssetClassificationEnum.MEMBER_CARD,
            AssetClassificationEnum.DEPOSIT,
            AssetClassificationEnum.OTHER_TOP_UP,
        ),
    ),

    /** 投资理财账户 */
    INVESTMENT_FINANCIAL_ACCOUNT(
        arrayOf(
            AssetClassificationEnum.STOCK,
            AssetClassificationEnum.FUND,
            AssetClassificationEnum.OTHER_INVESTMENT_FINANCIAL,
        ),
    ),

    /** 债务 */
    DEBT_ACCOUNT(
        arrayOf(
            AssetClassificationEnum.BORROW,
            AssetClassificationEnum.LEND,
        ),
    ),

    ;

    val isCreditCard: Boolean
        get() = this == CREDIT_CARD_ACCOUNT

    companion object {
        fun ordinalOf(ordinal: Int): ClassificationTypeEnum {
            return entries.first { it.ordinal == ordinal }
        }
    }
}
