package cn.wj.android.cashbook.core.ui.expand

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import cn.wj.android.cashbook.core.model.enums.AnalyticsBarTypeEnum
import cn.wj.android.cashbook.core.ui.R

val AnalyticsBarTypeEnum.text:String
    @Composable get()= stringResource(
        id = when (this) {
            AnalyticsBarTypeEnum.EXPENDITURE -> R.string.expend
            AnalyticsBarTypeEnum.INCOME -> R.string.income
            AnalyticsBarTypeEnum.BALANCE -> R.string.surplus
            AnalyticsBarTypeEnum.ALL -> R.string.all
        }
    )