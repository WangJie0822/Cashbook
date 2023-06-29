package cn.wj.android.cashbook.feature.assets.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import cn.wj.android.cashbook.core.model.entity.AssetEntity
import cn.wj.android.cashbook.core.model.entity.RecordTypeEntity
import cn.wj.android.cashbook.feature.assets.screen.EditAssetRoute
import cn.wj.android.cashbook.feature.assets.screen.MyAssetRoute
import cn.wj.android.cashbook.feature.assets.screen.SelectAssetBottomSheetScreen

/** 我的资产 */
private const val ROUTE_MY_ASSET = "asset/my"

/** 编辑资产 */
private const val ROUTE_EDIT_ASSET_KEY = "assetId"
private const val ROUTE_EDIT_ASSET =
    "asset/edit_asset?$ROUTE_EDIT_ASSET_KEY={$ROUTE_EDIT_ASSET_KEY}"

fun NavController.naviToMyAsset() {
    this.navigate(ROUTE_MY_ASSET)
}

fun NavController.naviToEditAsset(assetId: Long = -1L) {
    this.navigate(ROUTE_EDIT_ASSET.replace("{$ROUTE_EDIT_ASSET_KEY}", assetId.toString()))
}

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

fun NavGraphBuilder.myAssetScreen(
    onAssetItemClick: (Long) -> Unit,
    onAddAssetClick: () -> Unit,
    onInvisibleAssetClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    composable(route = ROUTE_MY_ASSET) {
        MyAssetRoute(
            onAssetItemClick = onAssetItemClick,
            onAddAssetClick = onAddAssetClick,
            onInvisibleAssetClick = onInvisibleAssetClick,
            onBackClick = onBackClick,
        )
    }
}

fun NavGraphBuilder.editAssetScreen(
    onBackClick: () -> Unit,
) {
    composable(
        route = ROUTE_EDIT_ASSET,
        arguments = listOf(navArgument(ROUTE_EDIT_ASSET_KEY) {
            type = NavType.LongType
            defaultValue = -1L
        })
    ) {
        EditAssetRoute(
            onBackClick = onBackClick,
            assetId = it.arguments?.getLong(ROUTE_EDIT_ASSET_KEY) ?: -1L
        )
    }
}