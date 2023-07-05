package cn.wj.android.cashbook.feature.records.navigation

import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.ResultModel
import cn.wj.android.cashbook.core.ui.LocalNavController
import cn.wj.android.cashbook.feature.records.dialog.ConfirmDeleteRecordDialogRoute
import cn.wj.android.cashbook.feature.records.screen.AssetInfoContentRoute
import cn.wj.android.cashbook.feature.records.screen.EditRecordRoute
import cn.wj.android.cashbook.feature.records.screen.LauncherContentRoute
import cn.wj.android.cashbook.feature.records.screen.RecordDetailsSheet
import cn.wj.android.cashbook.feature.records.screen.SelectRelatedRecordRoute
import cn.wj.android.cashbook.feature.records.viewmodel.EditRecordViewModel

private const val ROUTE_EDIT_RECORD_KEY_RECORD_ID = "recordId"
private const val ROUTE_EDIT_RECORD_KEY_TYPE_ID = "typeId"
private const val ROUTE_EDIT_RECORD =
    "record/edit_record?$ROUTE_EDIT_RECORD_KEY_RECORD_ID={$ROUTE_EDIT_RECORD_KEY_RECORD_ID}&$ROUTE_EDIT_RECORD_KEY_TYPE_ID={$ROUTE_EDIT_RECORD_KEY_TYPE_ID}"

private const val ROUTE_SELECT_RELATED_RECORD = "record/select_related_record"

fun NavController.naviToEditRecord(recordId: Long = -1L, typeId: Long = -1L) {
    this.navigate(
        ROUTE_EDIT_RECORD
            .replace(
                oldValue = "{$ROUTE_EDIT_RECORD_KEY_RECORD_ID}",
                newValue = recordId.toString()
            )
            .replace(
                oldValue = "{$ROUTE_EDIT_RECORD_KEY_TYPE_ID}",
                newValue = typeId.toString()
            )
    )
}

fun NavController.naviToSelectRelatedRecord() {
    this.navigate(ROUTE_SELECT_RELATED_RECORD)
}

/**
 * 编辑记录
 */
fun NavGraphBuilder.editRecordScreen(
    typeListContent: @Composable (
        modifier: Modifier,
        typeCategory: RecordTypeCategoryEnum,
        selectedTypeId: Long,
        onTypeSelect: (Long) -> Unit,
        headerContent: @Composable (modifier: Modifier) -> Unit,
        footerContent: @Composable (modifier: Modifier) -> Unit,
    ) -> Unit,
    selectAssetBottomSheetContent: @Composable (
        currentTypeId: Long,
        isRelated: Boolean,
        onAssetChange: (Long) -> Unit,
    ) -> Unit,
    selectTagBottomSheetContent: @Composable (
        selectedTagIdList: List<Long>,
        onTagIdListChange: (List<Long>) -> Unit,
    ) -> Unit,
    onBackClick: () -> Unit,
) {
    composable(
        route = ROUTE_EDIT_RECORD,
        arguments = listOf(
            navArgument(ROUTE_EDIT_RECORD_KEY_RECORD_ID) {
                type = NavType.LongType
                defaultValue = -1L
            },
            navArgument(ROUTE_EDIT_RECORD_KEY_TYPE_ID) {
                type = NavType.LongType
                defaultValue = -1L
            },
        ),
    ) {
        EditRecordRoute(
            recordId = it.arguments?.getLong(ROUTE_EDIT_RECORD_KEY_RECORD_ID) ?: -1L,
            typeId = it.arguments?.getLong(ROUTE_EDIT_RECORD_KEY_TYPE_ID) ?: -1L,
            typeListContent = typeListContent,
            selectAssetBottomSheetContent = selectAssetBottomSheetContent,
            selectTagBottomSheetContent = selectTagBottomSheetContent,
            onBackClick = onBackClick,
        )
    }
}

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

@Composable
fun LauncherContent(
    onEditRecordClick: (Long) -> Unit,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onMyAssetClick: () -> Unit,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,
) {
    LauncherContentRoute(
        onEditRecordClick = onEditRecordClick,
        onMenuClick = onMenuClick,
        onSearchClick = onSearchClick,
        onCalendarClick = onCalendarClick,
        onMyAssetClick = onMyAssetClick,
        onShowSnackbar = onShowSnackbar,
    )
}

@Composable
fun AssetInfoContent(
    assetId: Long,
    topContent: @Composable () -> Unit,
    onRecordItemClick: (RecordViewsEntity) -> Unit,
) {
    AssetInfoContentRoute(
        assetId = assetId,
        topContent = topContent,
        onRecordItemClick = onRecordItemClick,
    )
}

@Composable
fun RecordDetailSheetContent(
    recordEntity: RecordViewsEntity?,
    onRecordItemEditClick: (Long) -> Unit,
    onRecordItemDeleteClick: (Long) -> Unit,
) {
    RecordDetailsSheet(
        recordEntity = recordEntity,
        onRecordItemEditClick = onRecordItemEditClick,
        onRecordItemDeleteClick = onRecordItemDeleteClick,
    )
}

@Composable
fun ConfirmDeleteRecordDialogContent(
    recordId: Long,
    onResult: (ResultModel) -> Unit,
    onDialogDismiss: () -> Unit,
) {
    ConfirmDeleteRecordDialogRoute(
        recordId = recordId,
        onResult = onResult,
        onDialogDismiss = onDialogDismiss,
    )
}