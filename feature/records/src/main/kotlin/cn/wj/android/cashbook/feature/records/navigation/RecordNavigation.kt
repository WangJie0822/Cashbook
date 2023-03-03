@file:OptIn(ExperimentalAnimationApi::class)

package cn.wj.android.cashbook.feature.records.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import cn.wj.android.cashbook.core.model.entity.AssetEntity
import cn.wj.android.cashbook.core.model.entity.RecordTypeEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.ui.controller
import cn.wj.android.cashbook.feature.records.screen.EditRecordRoute
import cn.wj.android.cashbook.feature.records.screen.LauncherCollapsedTitleScreen
import cn.wj.android.cashbook.feature.records.screen.LauncherContentScreen
import cn.wj.android.cashbook.feature.records.screen.LauncherPinnedTitleScreen
import com.google.accompanist.navigation.animation.composable

private const val ROUTE_EDIT_RECORD = "record/edit_record"

fun NavController.naviToEditRecord() {
    this.navigate(ROUTE_EDIT_RECORD)
}

/**
 * 编辑记录
 */
fun NavGraphBuilder.editRecordScreen(
    selectTypeList: @Composable (Modifier, RecordTypeCategoryEnum, RecordTypeEntity?, @Composable LazyGridItemScope.() -> Unit, @Composable LazyGridItemScope.() -> Unit, (RecordTypeEntity?) -> Unit) -> Unit,
    selectAssetBottomSheet: @Composable (RecordTypeEntity?, Boolean, (AssetEntity?) -> Unit) -> Unit,
) {
    composable(ROUTE_EDIT_RECORD) {
        EditRecordRoute(
            onBackClick = { controller?.popBackStack() },
            selectTypeList = selectTypeList,
            selectAssetBottomSheet = selectAssetBottomSheet,
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