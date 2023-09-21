package cn.wj.android.cashbook.feature.records.navigation

import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.feature.records.screen.AssetInfoContentRoute
import cn.wj.android.cashbook.feature.records.screen.CalendarRoute
import cn.wj.android.cashbook.feature.records.screen.EditRecordRoute
import cn.wj.android.cashbook.feature.records.screen.LauncherContentRoute
import cn.wj.android.cashbook.feature.records.screen.SelectRelatedRecordRoute
import cn.wj.android.cashbook.feature.records.view.RecordDetailsSheet

private const val ROUTE_EDIT_RECORD_KEY_RECORD_ID = "recordId"
private const val ROUTE_EDIT_RECORD_KEY_TYPE_ID = "typeId"

/** 路由 - 编辑记录 */
internal const val ROUTE_EDIT_RECORD =
    "record/edit_record?$ROUTE_EDIT_RECORD_KEY_RECORD_ID={$ROUTE_EDIT_RECORD_KEY_RECORD_ID}&$ROUTE_EDIT_RECORD_KEY_TYPE_ID={$ROUTE_EDIT_RECORD_KEY_TYPE_ID}"

/** 路由 - 选择关联记录 */
private const val ROUTE_SELECT_RELATED_RECORD = "record/select_related_record"

/** 路由 - 日历 */
private const val ROUTE_RECORD_CALENDAR = "record/calendar"

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

fun NavController.naviToCalendar() {
    this.navigate(ROUTE_RECORD_CALENDAR)
}

/**
 * 编辑记录
 *
 * @param typeListContent 类型列表布局，参数：(类型大类, 已选择类型id, 类型选择回调, 头布局, 脚布局) -> [Unit]
 * @param assetBottomSheetContent 选择资产抽屉布局，参数：(已选择类型id, 是否是关联资产, 资产选择回调) -> [Unit]
 * @param tagBottomSheetContent 选择标签抽屉布局，参数：(已选择标签id列表, 标签id列表变化回调) -> [Unit]
 * @param onRequestPopBackStack 导航到上一级
 */
fun NavGraphBuilder.editRecordScreen(
    typeListContent: @Composable (
        RecordTypeCategoryEnum, Long, (Long) -> Unit,
        @Composable (modifier: Modifier) -> Unit,
        @Composable (modifier: Modifier) -> Unit
    ) -> Unit,
    assetBottomSheetContent: @Composable (Long, Boolean, (Long) -> Unit) -> Unit,
    tagBottomSheetContent: @Composable (List<Long>, (List<Long>) -> Unit) -> Unit,
    onRequestNaviToSelectRelatedRecord: () -> Unit,
    onRequestPopBackStack: () -> Unit,
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
            assetBottomSheetContent = assetBottomSheetContent,
            tagBottomSheetContent = tagBottomSheetContent,
            onRequestNaviToSelectRelatedRecord = onRequestNaviToSelectRelatedRecord,
            onRequestPopBackStack = onRequestPopBackStack,
        )
    }
}

fun NavGraphBuilder.selectRelatedRecordScreen(
    onRequestPopBackStack: () -> Unit,
) {
    composable(
        route = ROUTE_SELECT_RELATED_RECORD,
    ) {
        SelectRelatedRecordRoute(
            onRequestPopBackStack = onRequestPopBackStack,
        )
    }
}

/**
 * 记录日历界面
 *
 * @param recordDetailSheetContent 记录详情 sheet，参数：(记录数据，隐藏sheet回调) -> [Unit]
 * @param onRequestPopBackStack 导航到上一级
 * @param onShowSnackbar 显示 [androidx.compose.material3.Snackbar]，参数：(显示文本，action文本) -> [SnackbarResult]
 */
fun NavGraphBuilder.calendarScreen(
    recordDetailSheetContent: @Composable (RecordViewsEntity?, () -> Unit) -> Unit,
    onRequestPopBackStack: () -> Unit,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,
) {
    composable(route = ROUTE_RECORD_CALENDAR) {
        CalendarRoute(
            recordDetailSheetContent = recordDetailSheetContent,
            onRequestPopBackStack = onRequestPopBackStack,
            onShowSnackbar = onShowSnackbar,
        )
    }
}

/**
 * 首页具体显示内容
 *
 * @param recordDetailSheetContent 记录详情 sheet，参数：(记录数据，隐藏sheet回调) -> [Unit]
 * @param onRequestOpenDrawer 打开抽屉菜单
 * @param onRequestNaviToEditRecord 导航到编辑记录
 * @param onRequestNaviToSearch 导航到搜索
 * @param onRequestNaviToCalendar 导航到日历
 * @param onRequestNaviToMyAsset 导航到我的资产
 * @param onShowSnackbar 显示 [androidx.compose.material3.Snackbar]，参数：(显示文本，action文本) -> [SnackbarResult]
 */
@Composable
fun LauncherContent(
    recordDetailSheetContent: @Composable (RecordViewsEntity?, () -> Unit) -> Unit,
    onRequestOpenDrawer: () -> Unit,
    onRequestNaviToEditRecord: (Long) -> Unit,
    onRequestNaviToSearch: () -> Unit,
    onRequestNaviToCalendar: () -> Unit,
    onRequestNaviToMyAsset: () -> Unit,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,
) {
    LauncherContentRoute(
        recordDetailSheetContent = recordDetailSheetContent,
        onRequestOpenDrawer = onRequestOpenDrawer,
        onRequestNaviToEditRecord = onRequestNaviToEditRecord,
        onRequestNaviToSearch = onRequestNaviToSearch,
        onRequestNaviToCalendar = onRequestNaviToCalendar,
        onRequestNaviToMyAsset = onRequestNaviToMyAsset,
        onShowSnackbar = onShowSnackbar,
    )
}

/**
 * 资产信息界面记录列表
 *
 * @param assetId 资产 id
 * @param topContent 列表头布局
 * @param onRecordItemClick 记录列表 item 点击回调
 */
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

/**
 * 记录详情 sheet 内容
 *
 * @param recordEntity 显示的记录数据
 * @param onRequestNaviToEditRecord 导航到编辑记录
 * @param onRequestDismissSheet 隐藏 sheet
 */
@Composable
fun RecordDetailSheetContent(
    recordEntity: RecordViewsEntity?,
    onRequestNaviToEditRecord: (Long) -> Unit,
    onRequestDismissSheet: () -> Unit,
) {
    RecordDetailsSheet(
        recordData = recordEntity,
        onRequestNaviToEditRecord = onRequestNaviToEditRecord,
        onRequestDismissSheet = onRequestDismissSheet,
    )
}