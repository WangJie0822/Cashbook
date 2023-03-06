package cn.wj.android.cashbook.feature.records.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import cn.wj.android.cashbook.core.model.entity.AssetEntity
import cn.wj.android.cashbook.core.model.entity.RecordTypeEntity
import cn.wj.android.cashbook.core.model.entity.TagEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.ui.controller
import cn.wj.android.cashbook.feature.records.screen.EditRecordRoute
import cn.wj.android.cashbook.feature.records.screen.LauncherCollapsedTitleScreen
import cn.wj.android.cashbook.feature.records.screen.LauncherContentScreen
import cn.wj.android.cashbook.feature.records.screen.LauncherPinnedTitleScreen
import com.google.accompanist.navigation.animation.composable

private const val ROUTE_EDIT_RECORD_KEY = "recordId"
private const val ROUTE_EDIT_RECORD =
    "record/edit_record?$ROUTE_EDIT_RECORD_KEY={$ROUTE_EDIT_RECORD_KEY}"

fun NavController.naviToEditRecord(recordId: Long = -1L) {
    this.navigate(ROUTE_EDIT_RECORD.replace("{$ROUTE_EDIT_RECORD_KEY}", recordId.toString()))
}

/**
 * 编辑记录
 */
@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.editRecordScreen(
    selectTypeList: @Composable (RecordTypeCategoryEnum, RecordTypeEntity?, @Composable LazyGridItemScope.() -> Unit, @Composable LazyGridItemScope.() -> Unit, (RecordTypeEntity?) -> Unit) -> Unit,
    selectAssetBottomSheet: @Composable (RecordTypeEntity?, Boolean, (AssetEntity?) -> Unit) -> Unit,
    selectTagBottomSheet: @Composable (List<Long>, (TagEntity) -> Unit) -> Unit,
) {
    composable(
        route = ROUTE_EDIT_RECORD,
        arguments = listOf(navArgument(ROUTE_EDIT_RECORD_KEY) {
            type = NavType.LongType
            defaultValue = -1L
        }),
    ) {
        EditRecordRoute(
            recordId = it.arguments?.getLong(ROUTE_EDIT_RECORD_KEY) ?: -1L,
            onBackClick = { controller?.popBackStack() },
            selectTypeList = selectTypeList,
            selectAssetBottomSheet = selectAssetBottomSheet,
            selectTagBottomSheet = selectTagBottomSheet,
        )
    }
}

@Composable
fun LauncherPinnedTitleContent() {
    LauncherPinnedTitleScreen()
}

@Composable
fun LauncherCollapsedTitleContent() {
    LauncherCollapsedTitleScreen()
}

@Composable
fun LauncherContent(modifier: Modifier) {
    LauncherContentScreen(modifier)
}