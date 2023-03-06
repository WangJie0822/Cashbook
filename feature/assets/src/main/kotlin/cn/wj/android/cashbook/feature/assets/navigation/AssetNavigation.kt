package cn.wj.android.cashbook.feature.assets.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import cn.wj.android.cashbook.core.model.entity.AssetEntity
import cn.wj.android.cashbook.core.model.entity.RecordTypeEntity
import cn.wj.android.cashbook.core.ui.controller
import cn.wj.android.cashbook.feature.assets.screen.EditAssetRoute
import cn.wj.android.cashbook.feature.assets.screen.SelectAssetBottomSheetScreen
import com.google.accompanist.navigation.animation.composable

@Composable
fun SelectAssetBottomSheet(
    selectedType: RecordTypeEntity?, // TODO 根据当前类型显示资产列表
    related: Boolean,
    onAddAssetClick: () -> Unit,
    onAssetItemClick: (AssetEntity?) -> Unit,
) {
    SelectAssetBottomSheetScreen(
        onAddAssetClick = onAddAssetClick,
        onAssetItemClick = onAssetItemClick,
    )
}

private const val ROUTE_EDIT_ASSET_KEY = "assetId"
private const val ROUTE_EDIT_ASSET =
    "asset/edit_asset?$ROUTE_EDIT_ASSET_KEY={$ROUTE_EDIT_ASSET_KEY}"

fun NavController.naviToEditAsset(assetId: Long = -1L) {
    this.navigate(ROUTE_EDIT_ASSET.replace("{$ROUTE_EDIT_ASSET_KEY}", assetId.toString()))
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.editAssetScreen(
) {
    composable(
        route = ROUTE_EDIT_ASSET,
        arguments = listOf(navArgument(ROUTE_EDIT_ASSET_KEY) {
            type = NavType.LongType
            defaultValue = -1L
        })
    ) {
        EditAssetRoute(
            onBackClick = { controller?.popBackStack() },
            assetId = it.arguments?.getLong(ROUTE_EDIT_ASSET_KEY) ?: -1L
        )
    }
}