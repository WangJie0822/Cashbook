@file:Suppress("unused")

package cn.wj.android.cashbook.data.enums

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import cn.wj.android.cashbook.R

/**
 * 资产分类枚举
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/2
 */
enum class AssetClassificationEnum(
    @DrawableRes val logoResId: Int,
    @StringRes val nameResId: Int
) {

    // 资金账户
    CASH(R.drawable.vector_cash_circle_24, R.string.asset_classifications_cash),
    WECHAT(R.drawable.vector_wechat_circle_24, R.string.asset_classifications_wechat),
    ALIPAY(R.drawable.vector_alipay_circle_24, R.string.asset_classifications_alipay),
    BANK_CARD(R.drawable.vector_bank_card_circle_24, R.string.asset_classifications_bank_card),
    OTHER_CAPITAL(R.drawable.vector_asset_circle_24, R.string.asset_classifications_other_capital),

    // 信用卡账户
    CREDIT_CARD(R.drawable.vector_credit_card_circle_24, R.string.asset_classifications_credit_card),
    ANT_CREDIT_PAY(R.drawable.vector_ant_credit_pay_circle_24, R.string.asset_classifications_ant_credit_pay),
    JD_IOUS(R.drawable.vector_jingdong_circle_24, R.string.asset_classifications_jd_ious),
    OTHER_CREDIT_CARD(R.drawable.vector_credit_card_other_circle_24, R.string.asset_classifications_other_credit_card),

    // 充值账户
    PHONE_CHARGE(R.drawable.vector_phone_charge_circle_24, R.string.asset_classifications_phone_charge),
    BUS_CARD(R.drawable.vector_bus_card_circle_24, R.string.asset_classifications_bus_card),
    MEAL_CARD(R.drawable.vector_meal_card_circle_24, R.string.asset_classifications_meal_card),
    MEMBER_CARD(R.drawable.vector_member_card_circle_24, R.string.asset_classifications_member_card),
    DEPOSIT(R.drawable.vector_deposit_circle_24, R.string.asset_classifications_deposit),
    OTHER_TOP_UP(R.drawable.vector_rechargeable_card_circle_24, R.string.asset_classifications_other_top_up),

    // 投资理财账户
    STOCK(R.drawable.vector_stock_circle_24, R.string.asset_classifications_stock),
    FUND(R.drawable.vector_fund_circle_24, R.string.asset_classifications_fund),
    OTHER_INVESTMENT_FINANCIAL(R.drawable.vector_financial_circle_24, R.string.asset_classifications_other_investment_financial),

    // 债务
    BORROW(R.drawable.vector_borrow_circle_24, R.string.asset_classifications_borrow),
    LEND(R.drawable.vector_lend_circle_24, R.string.asset_classifications_lend),

    // 银行卡
    BANK_CARD_ZG(R.drawable.vector_bank_zg_24, R.string.asset_classifications_bank_zg),
    BANK_CARD_ZS(R.drawable.vector_bank_zs_24, R.string.asset_classifications_bank_zs),
    BANK_CARD_GS(R.drawable.vector_bank_gs_24, R.string.asset_classifications_bank_gs),
    BANK_CARD_NY(R.drawable.vector_bank_ny_24, R.string.asset_classifications_bank_ny),
    BANK_CARD_JS(R.drawable.vector_bank_js_24, R.string.asset_classifications_bank_js),
    BANK_CARD_JT(R.drawable.vector_bank_jt_24, R.string.asset_classifications_bank_jt),
    BANK_CARD_YZ(R.drawable.vector_bank_yz_24, R.string.asset_classifications_bank_yz),
    BANK_CARD_HX(R.drawable.vector_bank_hx_24, R.string.asset_classifications_bank_hx),
    BANK_CARD_BJ(R.drawable.vector_bank_bj_24, R.string.asset_classifications_bank_bj),
    BANK_CARD_MS(R.drawable.vector_bank_ms_24, R.string.asset_classifications_bank_ms),
    BANK_CARD_GD(R.drawable.vector_bank_gd_24, R.string.asset_classifications_bank_gd),
    BANK_CARD_ZX(R.drawable.vector_bank_zx_24, R.string.asset_classifications_bank_zx),
    BANK_CARD_GF(R.drawable.vector_bank_gf_24, R.string.asset_classifications_bank_gf),
    BANK_CARD_PF(R.drawable.vector_bank_pf_24, R.string.asset_classifications_bank_pf),
    BANK_CARD_XY(R.drawable.vector_bank_xy_24, R.string.asset_classifications_bank_xy);

    /** 是否需要选择银行 */
    val needSelectBank: Boolean
        get() = this == BANK_CARD || this == CREDIT_CARD

    /** 分类对应大类 */
    val parentType: ClassificationTypeEnum?
        get() = when (this) {
            in CAPITAL_ACCOUNT -> ClassificationTypeEnum.CAPITAL_ACCOUNT
            in CREDIT_CARD_ACCOUNT -> ClassificationTypeEnum.CREDIT_CARD_ACCOUNT
            in TOP_UP_ACCOUNT -> ClassificationTypeEnum.TOP_UP_ACCOUNT
            in INVESTMENT_FINANCIAL_ACCOUNT -> ClassificationTypeEnum.INVESTMENT_FINANCIAL_ACCOUNT
            in DEBT_ACCOUNT -> ClassificationTypeEnum.DEBT_ACCOUNT
            else -> null
        }

    companion object {

        /** 资金账户 */
        val CAPITAL_ACCOUNT = arrayOf(CASH, WECHAT, ALIPAY, BANK_CARD, OTHER_CAPITAL)

        /** 信用卡账户 */
        val CREDIT_CARD_ACCOUNT = arrayOf(CREDIT_CARD, ANT_CREDIT_PAY, JD_IOUS, OTHER_CREDIT_CARD)

        /** 充值账户 */
        val TOP_UP_ACCOUNT = arrayOf(PHONE_CHARGE, BUS_CARD, MEAL_CARD, MEMBER_CARD, DEPOSIT, OTHER_TOP_UP)

        /** 投资理财账户 */
        val INVESTMENT_FINANCIAL_ACCOUNT = arrayOf(STOCK, FUND, OTHER_INVESTMENT_FINANCIAL)

        /** 债务 */
        val DEBT_ACCOUNT = arrayOf(BORROW, LEND)

        /** 银行列表 */
        val BANK_LIST = arrayOf(
            BANK_CARD_ZG,
            BANK_CARD_ZS,
            BANK_CARD_GS,
            BANK_CARD_NY,
            BANK_CARD_JS,
            BANK_CARD_JT,
            BANK_CARD_YZ,
            BANK_CARD_HX,
            BANK_CARD_BJ,
            BANK_CARD_MS,
            BANK_CARD_GD,
            BANK_CARD_ZX,
            BANK_CARD_GF,
            BANK_CARD_PF,
            BANK_CARD_XY
        )

        fun fromName(name: String?): AssetClassificationEnum? {
            return values().firstOrNull { classification -> classification.name == name }
        }
    }
}