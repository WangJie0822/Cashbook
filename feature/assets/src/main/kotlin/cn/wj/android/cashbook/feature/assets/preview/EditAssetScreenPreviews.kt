package cn.wj.android.cashbook.feature.assets.preview

import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.ui.DevicePreviews
import cn.wj.android.cashbook.feature.assets.screen.SelectAssetClassificationSheet
import cn.wj.android.cashbook.feature.assets.screen.SelectAssetClassificationTypeSheet

@DevicePreviews
@Composable
fun SelectAssetClassificationTypeSheetPreview() {
    CashbookTheme {
        Surface {
            SelectAssetClassificationTypeSheet(
                onItemClick = { _, _ -> }
            )
        }
    }
}

@DevicePreviews
@Composable
fun SelectAssetClassificationSheetPreview() {
    CashbookTheme {
        Surface {
            SelectAssetClassificationSheet(
                onItemClick = {}
            )
        }
    }
}