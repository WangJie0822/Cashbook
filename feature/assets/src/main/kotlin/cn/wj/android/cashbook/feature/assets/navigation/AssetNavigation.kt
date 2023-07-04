package cn.wj.android.cashbook.feature.assets.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import cn.wj.android.cashbook.core.model.entity.AssetEntity
import cn.wj.android.cashbook.core.model.entity.RecordTypeEntity
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.model.ResultModel
import cn.wj.android.cashbook.feature.assets.screen.AssetInfoRoute
import cn.wj.android.cashbook.feature.assets.screen.EditAssetRoute
import cn.wj.android.cashbook.feature.assets.screen.MyAssetRoute
import cn.wj.android.cashbook.feature.assets.screen.SelectAssetBottomSheetScreen

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

fun NavGraphBuilder.assetInfoScreen(
    assetRecordListContent: @Composable (Long, @Composable () -> Unit, (RecordViewsEntity) -> Unit) -> Unit,
    recordDetailSheetContent: @Composable (recordInfo: RecordViewsEntity?, onRecordDeleteClick: (Long) -> Unit, dismissBottomSheet: () -> Unit) -> Unit,
    confirmDeleteRecordDialogContent: @Composable (recordId: Long, onResult: (ResultModel) -> Unit, onDialogDismiss: () -> Unit) -> Unit,
    onEditAssetClick: (Long) -> Unit,
    onBackClick: () -> Unit,
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
            recordDetailSheetContent = { recordInfo, onRecordDeleteClick, dismissBottomSheet ->
                recordDetailSheetContent(
                    recordInfo = recordInfo,
                    onRecordDeleteClick = onRecordDeleteClick,
                    dismissBottomSheet = dismissBottomSheet,
                )

            },
            confirmDeleteRecordDialogContent = confirmDeleteRecordDialogContent,
            onEditAssetClick = { onEditAssetClick.invoke(assetId) },
            onBackClick = onBackClick,
        )
    }
}

fun NavGraphBuilder.editAssetScreen(
    onBackClick: () -> Unit,
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
            onBackClick = onBackClick,
        )
    }
}