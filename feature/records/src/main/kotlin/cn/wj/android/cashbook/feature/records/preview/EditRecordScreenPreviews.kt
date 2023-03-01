@file:OptIn(ExperimentalMaterialApi::class)

package cn.wj.android.cashbook.feature.records.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.design.theme.LocalExtendedColors
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.ui.DevicePreviews
import cn.wj.android.cashbook.feature.records.R
import cn.wj.android.cashbook.feature.records.screen.Amount
import cn.wj.android.cashbook.feature.records.screen.EditRecordScreen
import cn.wj.android.cashbook.feature.records.screen.EditRecordTopBar
import cn.wj.android.cashbook.feature.records.screen.TypeItem

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
internal fun CategoryItemPreview() {
    CashbookTheme {
        Row {
            Column(modifier = Modifier.padding(8.dp)) {
                TypeItem(
                    first = true,
                    shapeType = 0,
                    iconResId = R.drawable.vector_type_books_education_24,
                    showMore = true,
                    title = "标题",
                    selected = true,
                    onTypeClick = {},
                )
                TypeItem(
                    first = true,
                    shapeType = 0,
                    iconResId = R.drawable.vector_type_books_education_24,
                    showMore = true,
                    title = "标题",
                    selected = false,
                    onTypeClick = {},
                )
            }
            Column(modifier = Modifier.padding(8.dp)) {
                TypeItem(
                    first = true,
                    shapeType = 0,
                    iconResId = R.drawable.vector_type_books_education_24,
                    showMore = false,
                    title = "标题",
                    selected = true,
                    onTypeClick = {},
                )
                TypeItem(
                    first = true,
                    shapeType = 0,
                    iconResId = R.drawable.vector_type_books_education_24,
                    showMore = false,
                    title = "标题",
                    selected = false,
                    onTypeClick = {},
                )
            }
            Column(modifier = Modifier.padding(8.dp)) {
                TypeItem(
                    first = false,
                    shapeType = 0,
                    iconResId = R.drawable.vector_type_books_education_24,
                    showMore = false,
                    title = "标题",
                    selected = true,
                    onTypeClick = {},
                )
                TypeItem(
                    first = false,
                    shapeType = 0,
                    iconResId = R.drawable.vector_type_books_education_24,
                    showMore = false,
                    title = "标题",
                    selected = false,
                    onTypeClick = {},
                )
            }
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
        EditRecordScreen(
            onBackClick = {},
            selectAssetBottomSheet = {},
        )
    }
}