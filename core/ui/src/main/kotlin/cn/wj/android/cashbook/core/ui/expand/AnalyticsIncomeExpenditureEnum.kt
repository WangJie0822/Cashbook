package cn.wj.android.cashbook.core.ui.expand

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import cn.wj.android.cashbook.core.model.enums.AnalyticsIncomeExpenditureEnum
import cn.wj.android.cashbook.core.ui.R

val AnalyticsIncomeExpenditureEnum.text:String
    @Composable get()= stringResource(
        id = when (this) {
            AnalyticsIncomeExpenditureEnum.EXPENDITURE -> R.string.expend
            AnalyticsIncomeExpenditureEnum.INCOME -> R.string.income
            AnalyticsIncomeExpenditureEnum.ALL -> R.string.all
        }
    )