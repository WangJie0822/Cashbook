package cn.wj.android.cashbook.feature.records.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import cn.wj.android.cashbook.core.model.entity.AssetEntity
import cn.wj.android.cashbook.core.model.entity.RecordTypeEntity
import cn.wj.android.cashbook.core.model.entity.TagEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.ui.LocalNavController
import cn.wj.android.cashbook.feature.records.screen.EditRecordRoute
import cn.wj.android.cashbook.feature.records.screen.LauncherContentScreen
import cn.wj.android.cashbook.feature.records.screen.SelectRelatedRecordRoute
import cn.wj.android.cashbook.feature.records.viewmodel.EditRecordViewModel
import com.google.accompanist.navigation.animation.composable

private const val ROUTE_EDIT_RECORD_KEY = "recordId"
private const val ROUTE_EDIT_RECORD =
    "record/edit_record?$ROUTE_EDIT_RECORD_KEY={$ROUTE_EDIT_RECORD_KEY}"

private const val ROUTE_SELECT_RELATED_RECORD = "record/select_related_record"

fun NavController.naviToEditRecord(recordId: Long = -1L) {
    this.navigate(ROUTE_EDIT_RECORD.replace("{$ROUTE_EDIT_RECORD_KEY}", recordId.toString()))
}

fun NavController.naviToSelectRelatedRecord() {
    this.navigate(ROUTE_SELECT_RELATED_RECORD)
}

/**
 * 编辑记录
 */
@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.editRecordScreen(
    onBackClick: () -> Unit,
    onSelectRelatedRecordClick: () -> Unit,
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
            onBackClick = onBackClick,
            selectTypeList = selectTypeList,
            selectAssetBottomSheet = selectAssetBottomSheet,
            selectTagBottomSheet = selectTagBottomSheet,
            onSelectRelatedRecordClick = onSelectRelatedRecordClick,
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.selectRelatedRecordScreen(
    onBackClick: () -> Unit,
) {
    composable(
        route = ROUTE_SELECT_RELATED_RECORD,
    ) {
        val navController = LocalNavController.current
        val parentEntry = remember(it) {
            navController.getBackStackEntry(ROUTE_EDIT_RECORD)
        }
        val parentViewModel = hiltViewModel<EditRecordViewModel>(parentEntry)
        SelectRelatedRecordRoute(
            onBackPressed = onBackClick,
            parentViewModel = parentViewModel,
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun LauncherContent(
    onAddClick: () -> Unit,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onMyAssetClick: () -> Unit,
    onRecordItemEditClick: (Long) -> Unit,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,
) {
    LauncherContentScreen(
        onMenuClick = onMenuClick,
        onAddClick = onAddClick,
        onSearchClick = onSearchClick,
        onCalendarClick = onCalendarClick,
        onMyAssetClick = onMyAssetClick,
        onRecordItemEditClick = onRecordItemEditClick,
        onShowSnackbar = onShowSnackbar,
    )
}