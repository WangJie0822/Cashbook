package cn.wj.android.cashbook.feature.records.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.design.theme.LocalExtendedColors
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.ui.DevicePreviews
import cn.wj.android.cashbook.feature.records.screen.Amount
import cn.wj.android.cashbook.feature.records.screen.EditRecordTopBar

@DevicePreviews
@Composable
internal fun AmountPreview() {
    CashbookTheme {
        Column {
            Amount(
                amount = "10000",
                primaryColor = LocalExtendedColors.current.expenditure,
                onAmountClick = {},
            )
            Amount(
                amount = "10000",
                primaryColor = LocalExtendedColors.current.income,
                onAmountClick = {},
            )
            Amount(
                amount = "10000",
                primaryColor = LocalExtendedColors.current.transfer,
                onAmountClick = {},
            )
        }
    }
}

@DevicePreviews
@Composable
internal fun EditRecordTopBarPreview() {
    CashbookTheme {
        Column {
            EditRecordTopBar(
                selectedTabIndex = RecordTypeCategoryEnum.EXPENDITURE.position,
                onTabSelected = {},
                onBackClick = {},
            )
            EditRecordTopBar(
                selectedTabIndex = RecordTypeCategoryEnum.INCOME.position,
                onTabSelected = {},
                onBackClick = {},
            )
            EditRecordTopBar(
                selectedTabIndex = RecordTypeCategoryEnum.TRANSFER.position,
                onTabSelected = {},
                onBackClick = {},
            )
        }
    }
}

@DevicePreviews
@Composable
internal fun EditRecordScreenPreview() {
    CashbookTheme {
//   TODO     EditRecordScreen(
//            onBackClick = {},
//            selectAssetBottomSheet = {},
//        )
    }
}