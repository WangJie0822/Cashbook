@file:Suppress("unused")

package cn.wj.android.cashbook.core.model.enums

/**
 * 资产分类大类枚举
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/2
 */
enum class ClassificationTypeEnum {

    /** 资金账户 */
    CAPITAL_ACCOUNT/*(R.string.asset_classifications_capital_account)*/,

    /** 信用卡账户 */
    CREDIT_CARD_ACCOUNT/*(R.string.asset_classifications_credit_card_account)*/,

    /** 充值账户 */
    TOP_UP_ACCOUNT/*(R.string.asset_classifications_top_up_account)*/,

    /** 投资理财账户 */
    INVESTMENT_FINANCIAL_ACCOUNT/*(R.string.asset_classifications_investment_financial_account)*/,

    /** 债务 */
    DEBT_ACCOUNT/*(R.string.asset_classifications_debt_account)*/;
}