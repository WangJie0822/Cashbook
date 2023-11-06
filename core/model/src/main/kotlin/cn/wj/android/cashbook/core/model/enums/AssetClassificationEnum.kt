@file:Suppress("unused")

package cn.wj.android.cashbook.core.model.enums

/**
 * 资产分类枚举
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/2
 */
enum class AssetClassificationEnum {

    // 资金账户
    CASH,
    WECHAT,
    ALIPAY,
    DOUYIN,
    BANK_CARD,
    OTHER_CAPITAL,

    // 信用卡账户
    CREDIT_CARD,
    ANT_CREDIT_PAY,
    JD_IOUS,
    DOUYIN_MONTH,
    OTHER_CREDIT_CARD,

    // 充值账户
    PHONE_CHARGE,
    BUS_CARD,
    MEAL_CARD,
    MEMBER_CARD,
    DEPOSIT,
    OTHER_TOP_UP,

    // 投资理财账户
    STOCK,
    FUND,
    OTHER_INVESTMENT_FINANCIAL,

    // 债务
    BORROW,
    LEND,
    DEBT,

    // 银行卡
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
    BANK_CARD_XY,

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
        fun ordinalOf(ordinal: Int): AssetClassificationEnum {
            return entries.first { it.ordinal == ordinal }
        }
    }
}