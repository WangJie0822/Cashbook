package cn.wj.android.cashbook.core.ui.expand

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import cn.wj.android.cashbook.core.design.theme.LocalExtendedColors
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.ui.R

val RecordTypeCategoryEnum.typeColor: Color
    @Composable get() = when (this) {
        RecordTypeCategoryEnum.EXPENDITURE -> LocalExtendedColors.current.expenditure
        RecordTypeCategoryEnum.INCOME -> LocalExtendedColors.current.income
        RecordTypeCategoryEnum.TRANSFER -> LocalExtendedColors.current.transfer
    }
val RecordTypeCategoryEnum.onTypeColor: Color
    @Composable get() = when (this) {
        RecordTypeCategoryEnum.EXPENDITURE -> LocalExtendedColors.current.onExpenditure
        RecordTypeCategoryEnum.INCOME -> LocalExtendedColors.current.onIncome
        RecordTypeCategoryEnum.TRANSFER -> LocalExtendedColors.current.onTransfer
    }

val RecordTypeCategoryEnum.text: String
    @Composable get() = stringResource(
        id = when (this) {
            RecordTypeCategoryEnum.EXPENDITURE -> R.string.expend
            RecordTypeCategoryEnum.INCOME -> R.string.income
            RecordTypeCategoryEnum.TRANSFER -> R.string.transfer
        }
    )

val RecordTypeCategoryEnum.percentText: String
    @Composable get() = stringResource(
        id = when (this) {
            RecordTypeCategoryEnum.EXPENDITURE -> R.string.expenditure_percent
            RecordTypeCategoryEnum.INCOME -> R.string.income_percent
            RecordTypeCategoryEnum.TRANSFER -> R.string.transfer_percent
        }
    )