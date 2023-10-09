package cn.wj.android.cashbook.feature.assets.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.feature.assets.screen.AssetInfoRoute
import cn.wj.android.cashbook.feature.assets.screen.EditAssetRoute
import cn.wj.android.cashbook.feature.assets.screen.EditRecordSelectAssetBottomSheetRoute
import cn.wj.android.cashbook.feature.assets.screen.MyAssetRoute

private const val ROUTE_KEY_ASSET_ID = "assetId"

/** 我的资产 */
private const val ROUTE_MY_ASSET = "asset/my"

/** 资产信息 */
private const val ROUTE_ASSET_INFO = "asset/info?$ROUTE_KEY_ASSET_ID={$ROUTE_KEY_ASSET_ID}"

/** 编辑资产 */
private const val ROUTE_EDIT_ASSET =
    "asset/edit_asset?$ROUTE_KEY_ASSET_ID={$ROUTE_KEY_ASSET_ID}"

fun NavController.naviToMyAsset() {
    this.navigate(ROUTE_MY_ASSET)
}

fun NavController.naviToAssetInfo(assetId: Long) {
    this.navigate(ROUTE_ASSET_INFO.replace("{$ROUTE_KEY_ASSET_ID}", assetId.toString()))
}

fun NavController.naviToEditAsset(assetId: Long = -1L) {
    this.navigate(ROUTE_EDIT_ASSET.replace("{$ROUTE_KEY_ASSET_ID}", assetId.toString()))
}

/**
 * 编辑记录界面选择资产抽屉
 *
 * @param currentTypeId 当前选择的类型 id
 * @param selectedAssetId 已选择资产 id
 * @param isRelated 是否是关联资产
 * @param onAssetChange 资产变化回调
 * @param onRequestNaviToEditAsset 导航到编辑资产
 */
@Composable
fun EditRecordSelectAssetBottomSheetContent(
    currentTypeId: Long,
    selectedAssetId: Long,
    isRelated: Boolean,
    onAssetChange: (Long) -> Unit,
    onRequestNaviToEditAsset: () -> Unit,
) {
    EditRecordSelectAssetBottomSheetRoute(
        currentTypeId = currentTypeId,
        selectedAssetId = selectedAssetId,
        isRelated = isRelated,
        onAssetChange = onAssetChange,
        onRequestNaviToEditAsset = onRequestNaviToEditAsset,
    )
}

/**
 * 我的资产界面
 * 
 * @param onRequestNaviToAssetInfo 导航到资产信息
 * @param onRequestNaviToAddAsset 导航到添加资产
 * @param onRequestNaviToInvisibleAsset 导航到隐藏资产
 * @param onRequestPopBackStack 导航到上一级
 */
fun NavGraphBuilder.myAssetScreen(
    onRequestNaviToAssetInfo: (Long) -> Unit,
    onRequestNaviToAddAsset: () -> Unit,
    onRequestNaviToInvisibleAsset: () -> Unit,
    onRequestPopBackStack: () -> Unit,
) {
    composable(route = ROUTE_MY_ASSET) {
        MyAssetRoute(
            onRequestNaviToAssetInfo = onRequestNaviToAssetInfo,
            onRequestNaviToAddAsset = onRequestNaviToAddAsset,
            onRequestNaviToInvisibleAsset = onRequestNaviToInvisibleAsset,
            onRequestPopBackStack = onRequestPopBackStack,
        )
    }
}

/**
 * 资产信息界面
 *
 * @param assetRecordListContent 资产记录列表，参数：(资产id, 列表头布局, 列表item点击回调) -> [Unit]
 * @param recordDetailSheetContent 记录详情 sheet，参数：(记录数据，隐藏sheet回调) -> [Unit]
 * @param onRequestNaviToEditAsset 导航到编辑资产
 * @param onRequestPopBackStack 导航到上一级
 */
fun NavGraphBuilder.assetInfoScreen(
    assetRecordListContent: @Composable (Long, @Composable () -> Unit, (RecordViewsEntity) -> Unit) -> Unit,
    recordDetailSheetContent: @Composable (RecordViewsEntity?, () -> Unit) -> Unit,
    onRequestNaviToEditAsset: (Long) -> Unit,
    onRequestPopBackStack: () -> Unit,
) {
    composable(
        route = ROUTE_ASSET_INFO,
        arguments = listOf(navArgument(ROUTE_KEY_ASSET_ID) {
            type = NavType.LongType
            defaultValue = -1L
        })
    ) {
        val assetId = it.arguments?.getLong(ROUTE_KEY_ASSET_ID) ?: -1L
        AssetInfoRoute(
            assetId = assetId,
            assetRecordListContent = { topContent, onRecordItemClick ->
                assetRecordListContent.invoke(
                    assetId,
                    topContent,
                    onRecordItemClick,
                )
            },
            recordDetailSheetContent = recordDetailSheetContent,
            onRequestNaviToEditAsset = { onRequestNaviToEditAsset.invoke(assetId) },
            onRequestPopBackStack = onRequestPopBackStack,
        )
    }
}

/**
 * 编辑资产界面
 *
 * @param onRequestPopBackStack 导航到上一级
 */
fun NavGraphBuilder.editAssetScreen(
    onRequestPopBackStack: () -> Unit,
) {
    composable(
        route = ROUTE_EDIT_ASSET,
        arguments = listOf(navArgument(ROUTE_KEY_ASSET_ID) {
            type = NavType.LongType
            defaultValue = -1L
        })
    ) {
        EditAssetRoute(
            assetId = it.arguments?.getLong(ROUTE_KEY_ASSET_ID) ?: -1L,
            onRequestPopBackStack = onRequestPopBackStack,
        )
    }
}