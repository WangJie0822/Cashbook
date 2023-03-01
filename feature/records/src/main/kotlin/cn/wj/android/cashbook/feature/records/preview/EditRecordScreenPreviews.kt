@file:OptIn(ExperimentalMaterialApi::class)

package cn.wj.android.cashbook.feature.records.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.design.theme.LocalExtendedColors
import cn.wj.android.cashbook.core.model.entity.RecordTypeEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.ui.DevicePreviews
import cn.wj.android.cashbook.feature.records.R
import cn.wj.android.cashbook.feature.records.enums.AssetListTypeEnum
import cn.wj.android.cashbook.feature.records.enums.BottomSheetEnum
import cn.wj.android.cashbook.feature.records.screen.Amount
import cn.wj.android.cashbook.feature.records.screen.AssetItem
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
        val typeList = listOf(
            RecordTypeEntity(
                -1L,
                -1L,
                "记录类型",
                R.drawable.vector_type_books_education_24,
                0,
                listOf(),
                false,
                0
            ),
            RecordTypeEntity(
                -1L,
                -1L,
                "记录类型",
                R.drawable.vector_type_electrical_appliance_24,
                0,
                listOf(),
                false,
                0
            ),
            RecordTypeEntity(
                -1L,
                -1L,
                "记录类型",
                R.drawable.vector_type_bag_24,
                0,
                listOf(),
                true,
                0
            ),
            RecordTypeEntity(
                -1L,
                1L,
                "记录类型",
                R.drawable.vector_type_bicycle_24,
                0,
                listOf(),
                true,
                -1
            ),
            RecordTypeEntity(
                -1L,
                1L,
                "记录类型",
                R.drawable.vector_type_at_24,
                0,
                listOf(),
                false,
                0
            ),
            RecordTypeEntity(
                -1L,
                1L,
                "记录类型",
                R.drawable.vector_type_call_charge_24,
                0,
                listOf(),
                false,
                0
            ),
            RecordTypeEntity(
                -1L,
                1L,
                "记录类型",
                R.drawable.vector_type_borrow_24,
                0,
                listOf(),
                false,
                1
            ),
        )
        EditRecordScreen(
            onBackClick = {},
            selectedTypeCategory = RecordTypeCategoryEnum.EXPENDITURE,
            onTabSelected = {},
            amount = "1000",
            onAmountClick = {},
            typeList = typeList,
            onTypeClick = {},
            onTypeSettingClick = {},
            remark = "备注文本",
            onRemarkTextChanged = {},
            assetName = "中国银行",
            onAssetClick = {},
            relatedAssetName = "",
            onRelatedAssetClick = {},
            dateTime = "2023-02-21 20:20",
            onDateTimeClick = {},
            tags = "标签1, 标签2",
            onTagsClick = {},
            charges = "",
            onChargesClick = {},
            concessions = "",
            onConcessionsClick = {},
            reimbursable = false,
            onReimbursableClick = {},
            bottomSheetEnum = BottomSheetEnum.NONE,
            assetList = listOf(),
            onAssetItemClick = {},
            onAddAssetClick = {},
            onSaveClick = {},
        )
    }
}

@DevicePreviews
@Composable
internal fun AssetItemPreview() {
    CashbookTheme {
        Surface {
            Column {
                AssetItem(
                    type = AssetListTypeEnum.NO_SELECT,
                    name = "不选择资产",
                    iconResId = R.drawable.vector_baseline_not_select_24,
                    balance = "",
                    totalAmount = "",
                    onAssetItemClick = {},
                )
                AssetItem(
                    type = AssetListTypeEnum.CREDIT_CARD,
                    name = "信用卡",
                    iconResId = R.drawable.vector_type_credit_card_payment_24,
                    balance = "5656",
                    totalAmount = "30000",
                    onAssetItemClick = {},
                )
                AssetItem(
                    type = AssetListTypeEnum.CAPITAL,
                    name = "银行卡",
                    iconResId = R.drawable.vector_type_credit_card_payment_24,
                    balance = "5656",
                    totalAmount = "",
                    onAssetItemClick = {},
                )
            }
        }
    }
}

@DevicePreviews
@Composable
internal fun AssetBottomSheetPreview() {
    CashbookTheme {
        Surface {
//            AssetBottomSheet()
        }
    }
}