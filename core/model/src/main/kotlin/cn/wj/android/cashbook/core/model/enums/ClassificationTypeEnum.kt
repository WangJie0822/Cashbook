@file:Suppress("unused")

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
            AssetClassificationEnum.OTHER_CAPITAL
        )
    ),

    /** 信用卡账户 */
    CREDIT_CARD_ACCOUNT(
        arrayOf(
            AssetClassificationEnum.CREDIT_CARD,
            AssetClassificationEnum.ANT_CREDIT_PAY,
            AssetClassificationEnum.JD_IOUS,
            AssetClassificationEnum.DOUYIN_MONTH,
            AssetClassificationEnum.OTHER_CREDIT_CARD
        )
    ),

    /** 充值账户 */
    TOP_UP_ACCOUNT(
        arrayOf(
            AssetClassificationEnum.PHONE_CHARGE,
            AssetClassificationEnum.BUS_CARD,
            AssetClassificationEnum.MEAL_CARD,
            AssetClassificationEnum.MEMBER_CARD,
            AssetClassificationEnum.DEPOSIT,
            AssetClassificationEnum.OTHER_TOP_UP
        )
    ),

    /** 投资理财账户 */
    INVESTMENT_FINANCIAL_ACCOUNT(
        arrayOf(
            AssetClassificationEnum.STOCK,
            AssetClassificationEnum.FUND,
            AssetClassificationEnum.OTHER_INVESTMENT_FINANCIAL
        )
    ),

    /** 债务 */
    DEBT_ACCOUNT(
        arrayOf(
            AssetClassificationEnum.BORROW,
            AssetClassificationEnum.LEND
        )
    );

    fun isCreditCard(): Boolean {
        return this == CREDIT_CARD_ACCOUNT
    }

    fun isDebt(): Boolean {
        return this == DEBT_ACCOUNT
    }

    companion object {
        fun ordinalOf(ordinal: Int): ClassificationTypeEnum {
            return entries.first { it.ordinal == ordinal }
        }
    }
}