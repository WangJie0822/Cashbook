package cn.wj.android.cashbook.core.ui.expand

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import cn.wj.android.cashbook.core.design.theme.LocalExtendedColors
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum

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