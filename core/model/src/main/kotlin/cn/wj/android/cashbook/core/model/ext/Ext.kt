package cn.wj.android.cashbook.core.model.ext

import cn.wj.android.cashbook.core.model.entity.AssetEntity
import cn.wj.android.cashbook.core.model.enums.AssetClassificationEnum
import cn.wj.android.cashbook.core.model.enums.ClassificationTypeEnum

/** 是否需要银行信息，债务、基金投资及其他非银行卡资产不需要 */
val AssetEntity.hasBankInfo: Boolean
    get() = type != ClassificationTypeEnum.DEBT_ACCOUNT
            && type != ClassificationTypeEnum.INVESTMENT_FINANCIAL_ACCOUNT
            && classification !in arrayOf(
        AssetClassificationEnum.ALIPAY,
        AssetClassificationEnum.CASH,
        AssetClassificationEnum.WECHAT,
        AssetClassificationEnum.JD_IOUS,
        AssetClassificationEnum.ANT_CREDIT_PAY,
    )

val AssetEntity.isCreditCard: Boolean
    get() = type == ClassificationTypeEnum.CREDIT_CARD_ACCOUNT

val AssetEntity.isDebt: Boolean
    get() = type == ClassificationTypeEnum.DEBT_ACCOUNT
