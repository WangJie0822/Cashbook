package cn.wj.android.cashbook.feature.assets.navigation

import androidx.compose.runtime.Composable
import cn.wj.android.cashbook.core.model.entity.AssetEntity
import cn.wj.android.cashbook.feature.assets.screen.SelectAssetBottomSheet

@Composable
fun selectAssetBottomSheet(
    onAddAssetClick: () -> Unit,
    onAssetItemClick: (AssetEntity?) -> Unit,
) {
    SelectAssetBottomSheet(
        onAddAssetClick = onAddAssetClick,
        onAssetItemClick = onAssetItemClick,
    )
}