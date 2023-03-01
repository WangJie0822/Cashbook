package cn.wj.android.cashbook.feature.assets.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.ui.DevicePreviews
import cn.wj.android.cashbook.feature.assets.R
import cn.wj.android.cashbook.feature.assets.enums.BottomAssetListTypeEnum
import cn.wj.android.cashbook.feature.assets.screen.AssetItem
import cn.wj.android.cashbook.feature.assets.screen.SelectAssetBottomSheet

@DevicePreviews
@Composable
internal fun SelectAssetBottomSheetPreview() {
    CashbookTheme {
        Surface {
            SelectAssetBottomSheet(onAddAssetClick = { }, onAssetItemClick = {})
        }
    }
}

@DevicePreviews
@Composable
internal fun AssetItemPreview() {
    CashbookTheme {
        Surface {
            Column {
                AssetItem(
                    type = BottomAssetListTypeEnum.NO_SELECT,
                    name = "不选择资产",
                    iconResId = R.drawable.vector_baseline_not_select_24,
                    balance = "",
                    totalAmount = "",
                    onAssetItemClick = {},
                )
                AssetItem(
                    type = BottomAssetListTypeEnum.CREDIT_CARD,
                    name = "信用卡",
                    iconResId = R.drawable.vector_type_credit_card_payment_24,
                    balance = "5656",
                    totalAmount = "30000",
                    onAssetItemClick = {},
                )
                AssetItem(
                    type = BottomAssetListTypeEnum.CAPITAL,
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