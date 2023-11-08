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
import cn.wj.android.cashbook.core.ui.R

/**
 * 从类型枚举中获取对应信息的工具类
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/3/3
 */
internal object AssetHelper {

    fun getNameResIdByType(type: ClassificationTypeEnum): Int = when (type) {
        ClassificationTypeEnum.CAPITAL_ACCOUNT -> R.string.asset_classifications_capital_account
        ClassificationTypeEnum.CREDIT_CARD_ACCOUNT -> R.string.asset_classifications_credit_card_account
        ClassificationTypeEnum.TOP_UP_ACCOUNT -> R.string.asset_classifications_top_up_account
        ClassificationTypeEnum.INVESTMENT_FINANCIAL_ACCOUNT -> R.string.asset_classifications_investment_financial_account
        ClassificationTypeEnum.DEBT_ACCOUNT -> R.string.asset_classifications_debt_account
    }

    fun getIconResIdByType(type: AssetClassificationEnum): Int = when (type) {
        AssetClassificationEnum.CASH -> R.drawable.vector_cash_circle_24
        AssetClassificationEnum.WECHAT -> R.drawable.vector_wechat_circle_24
        AssetClassificationEnum.ALIPAY -> R.drawable.vector_alipay_circle_24
        AssetClassificationEnum.DOUYIN -> R.drawable.vector_douyin_circle_24
        AssetClassificationEnum.BANK_CARD -> R.drawable.vector_bank_card_circle_24
        AssetClassificationEnum.OTHER_CAPITAL -> R.drawable.vector_asset_circle_24
        AssetClassificationEnum.CREDIT_CARD -> R.drawable.vector_credit_card_circle_24
        AssetClassificationEnum.ANT_CREDIT_PAY -> R.drawable.vector_ant_credit_pay_circle_24
        AssetClassificationEnum.JD_IOUS -> R.drawable.vector_jingdong_circle_24
        AssetClassificationEnum.DOUYIN_MONTH -> R.drawable.vector_douyin_month_circle_24
        AssetClassificationEnum.OTHER_CREDIT_CARD -> R.drawable.vector_credit_card_other_circle_24
        AssetClassificationEnum.PHONE_CHARGE -> R.drawable.vector_phone_charge_circle_24
        AssetClassificationEnum.BUS_CARD -> R.drawable.vector_bus_card_circle_24
        AssetClassificationEnum.MEAL_CARD -> R.drawable.vector_meal_card_circle_24
        AssetClassificationEnum.MEMBER_CARD -> R.drawable.vector_member_card_circle_24
        AssetClassificationEnum.DEPOSIT -> R.drawable.vector_deposit_circle_24
        AssetClassificationEnum.OTHER_TOP_UP -> R.drawable.vector_rechargeable_card_circle_24
        AssetClassificationEnum.STOCK -> R.drawable.vector_stock_circle_24
        AssetClassificationEnum.FUND -> R.drawable.vector_fund_circle_24
        AssetClassificationEnum.OTHER_INVESTMENT_FINANCIAL -> R.drawable.vector_financial_circle_24
        AssetClassificationEnum.BORROW -> R.drawable.vector_borrow_circle_24
        AssetClassificationEnum.LEND -> R.drawable.vector_lend_circle_24
        AssetClassificationEnum.DEBT -> R.drawable.vector_debt_circle_24
        AssetClassificationEnum.BANK_CARD_ZG -> R.drawable.vector_bank_zg_24
        AssetClassificationEnum.BANK_CARD_ZS -> R.drawable.vector_bank_zs_24
        AssetClassificationEnum.BANK_CARD_GS -> R.drawable.vector_bank_gs_24
        AssetClassificationEnum.BANK_CARD_NY -> R.drawable.vector_bank_ny_24
        AssetClassificationEnum.BANK_CARD_JS -> R.drawable.vector_bank_js_24
        AssetClassificationEnum.BANK_CARD_JT -> R.drawable.vector_bank_jt_24
        AssetClassificationEnum.BANK_CARD_YZ -> R.drawable.vector_bank_yz_24
        AssetClassificationEnum.BANK_CARD_HX -> R.drawable.vector_bank_hx_24
        AssetClassificationEnum.BANK_CARD_BJ -> R.drawable.vector_bank_bj_24
        AssetClassificationEnum.BANK_CARD_MS -> R.drawable.vector_bank_ms_24
        AssetClassificationEnum.BANK_CARD_GD -> R.drawable.vector_bank_gd_24
        AssetClassificationEnum.BANK_CARD_ZX -> R.drawable.vector_bank_zx_24
        AssetClassificationEnum.BANK_CARD_GF -> R.drawable.vector_bank_gf_24
        AssetClassificationEnum.BANK_CARD_PF -> R.drawable.vector_bank_pf_24
        AssetClassificationEnum.BANK_CARD_XY -> R.drawable.vector_bank_xy_24
    }

    fun getNameResIdByType(type: AssetClassificationEnum): Int = when (type) {
        AssetClassificationEnum.CASH -> R.string.asset_classifications_cash
        AssetClassificationEnum.WECHAT -> R.string.asset_classifications_wechat
        AssetClassificationEnum.ALIPAY -> R.string.asset_classifications_alipay
        AssetClassificationEnum.DOUYIN -> R.string.asset_classifications_douyin
        AssetClassificationEnum.BANK_CARD -> R.string.asset_classifications_bank_card
        AssetClassificationEnum.OTHER_CAPITAL -> R.string.asset_classifications_other_capital
        AssetClassificationEnum.CREDIT_CARD -> R.string.asset_classifications_credit_card
        AssetClassificationEnum.ANT_CREDIT_PAY -> R.string.asset_classifications_ant_credit_pay
        AssetClassificationEnum.JD_IOUS -> R.string.asset_classifications_jd_ious
        AssetClassificationEnum.DOUYIN_MONTH -> R.string.asset_classifications_douyin_month
        AssetClassificationEnum.OTHER_CREDIT_CARD -> R.string.asset_classifications_other_credit_card
        AssetClassificationEnum.PHONE_CHARGE -> R.string.asset_classifications_phone_charge
        AssetClassificationEnum.BUS_CARD -> R.string.asset_classifications_bus_card
        AssetClassificationEnum.MEAL_CARD -> R.string.asset_classifications_meal_card
        AssetClassificationEnum.MEMBER_CARD -> R.string.asset_classifications_member_card
        AssetClassificationEnum.DEPOSIT -> R.string.asset_classifications_deposit
        AssetClassificationEnum.OTHER_TOP_UP -> R.string.asset_classifications_other_top_up
        AssetClassificationEnum.STOCK -> R.string.asset_classifications_stock
        AssetClassificationEnum.FUND -> R.string.asset_classifications_fund
        AssetClassificationEnum.OTHER_INVESTMENT_FINANCIAL -> R.string.asset_classifications_other_investment_financial
        AssetClassificationEnum.BORROW -> R.string.asset_classifications_borrow
        AssetClassificationEnum.LEND -> R.string.asset_classifications_lend
        AssetClassificationEnum.DEBT -> R.string.asset_classifications_debt
        AssetClassificationEnum.BANK_CARD_ZG -> R.string.asset_classifications_bank_zg
        AssetClassificationEnum.BANK_CARD_ZS -> R.string.asset_classifications_bank_zs
        AssetClassificationEnum.BANK_CARD_GS -> R.string.asset_classifications_bank_gs
        AssetClassificationEnum.BANK_CARD_NY -> R.string.asset_classifications_bank_ny
        AssetClassificationEnum.BANK_CARD_JS -> R.string.asset_classifications_bank_js
        AssetClassificationEnum.BANK_CARD_JT -> R.string.asset_classifications_bank_jt
        AssetClassificationEnum.BANK_CARD_YZ -> R.string.asset_classifications_bank_yz
        AssetClassificationEnum.BANK_CARD_HX -> R.string.asset_classifications_bank_hx
        AssetClassificationEnum.BANK_CARD_BJ -> R.string.asset_classifications_bank_bj
        AssetClassificationEnum.BANK_CARD_MS -> R.string.asset_classifications_bank_ms
        AssetClassificationEnum.BANK_CARD_GD -> R.string.asset_classifications_bank_gd
        AssetClassificationEnum.BANK_CARD_ZX -> R.string.asset_classifications_bank_zx
        AssetClassificationEnum.BANK_CARD_GF -> R.string.asset_classifications_bank_gf
        AssetClassificationEnum.BANK_CARD_PF -> R.string.asset_classifications_bank_pf
        AssetClassificationEnum.BANK_CARD_XY -> R.string.asset_classifications_bank_xy
    }
}

val ClassificationTypeEnum.nameResId: Int
    get() = AssetHelper.getNameResIdByType(this)

val AssetClassificationEnum.iconResId: Int
    get() = AssetHelper.getIconResIdByType(this)

val AssetClassificationEnum.nameResId: Int
    get() = AssetHelper.getNameResIdByType(this)

val assetClassificationEnumBanks: Array<AssetClassificationEnum>
    get() = arrayOf(
        AssetClassificationEnum.BANK_CARD_ZG,
        AssetClassificationEnum.BANK_CARD_ZS,
        AssetClassificationEnum.BANK_CARD_GS,
        AssetClassificationEnum.BANK_CARD_NY,
        AssetClassificationEnum.BANK_CARD_JS,
        AssetClassificationEnum.BANK_CARD_JT,
        AssetClassificationEnum.BANK_CARD_YZ,
        AssetClassificationEnum.BANK_CARD_HX,
        AssetClassificationEnum.BANK_CARD_BJ,
        AssetClassificationEnum.BANK_CARD_MS,
        AssetClassificationEnum.BANK_CARD_GD,
        AssetClassificationEnum.BANK_CARD_ZX,
        AssetClassificationEnum.BANK_CARD_GF,
        AssetClassificationEnum.BANK_CARD_PF,
        AssetClassificationEnum.BANK_CARD_XY,
    )
