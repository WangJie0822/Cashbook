/*
 * Copyright 2021 The Cashbook Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.wj.android.cashbook.feature.records.navigation

import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.feature.records.screen.AnalyticsRoute
import cn.wj.android.cashbook.feature.records.screen.AssetInfoContentRoute
import cn.wj.android.cashbook.feature.records.screen.CalendarRoute
import cn.wj.android.cashbook.feature.records.screen.EditRecordRoute
import cn.wj.android.cashbook.feature.records.screen.LauncherContentRoute
import cn.wj.android.cashbook.feature.records.screen.SearchRoute
import cn.wj.android.cashbook.feature.records.screen.SelectRelatedRecordRoute
import cn.wj.android.cashbook.feature.records.screen.TypedAnalyticsRoute
import cn.wj.android.cashbook.feature.records.view.RecordDetailsSheet
import kotlinx.serialization.Serializable

/** 路由 - 编辑记录 */
@Serializable
data class EditRecord(
    val recordId: Long = -1L,
    val assetId: Long = -1L,
)

/** 路由 - 数据分析 */
@Serializable
object Analytics

/** 路由 - 分类数据分析 */
@Serializable
data class TypedAnalytics(
    val tagId: Long = -1L,
    val typeId: Long = -1L,
    val date: String = "",
)

/** 路由 - 选择关联记录 */
@Serializable
object SelectRelatedRecord

/** 路由 - 日历 */
@Serializable
object RecordCalendar

/** 路由 - 搜索 */
@Serializable
object RecordSearch

fun NavController.naviToAnalytics() {
    this.navigate(Analytics)
}

fun NavController.naviToEditRecord(recordId: Long = -1L, assetId: Long = -1L) {
    this.navigate(EditRecord(recordId = recordId, assetId = assetId))
}

fun NavController.naviToTypedAnalytics(
    tagId: Long = -1L,
    typeId: Long = -1L,
    date: String? = null,
    includeChildTypes: Boolean = true,
) {
    this.navigate(
        TypedAnalytics(
            tagId = tagId,
            typeId = typeId,
            date = date.orEmpty(),
        ),
    )
}

fun NavController.naviToSelectRelatedRecord() {
    this.navigate(SelectRelatedRecord)
}

fun NavController.naviToCalendar() {
    this.navigate(RecordCalendar)
}

fun NavController.naviToSearch() {
    this.navigate(RecordSearch)
}

/**
 * 编辑记录
 *
 * @param typeListContent 类型列表布局，参数：(类型大类, 默认类型 id, 类型选择回调) -> [Unit]
 * @param assetBottomSheetContent 选择资产抽屉布局，参数：(已选择类型id, 已选择资产id, 是否是关联资产, 资产选择回调) -> [Unit]
 * @param tagBottomSheetContent 选择标签抽屉布局，参数：(已选择标签id列表, 标签id列表变化回调) -> [Unit]
 * @param onRequestPopBackStack 导航到上一级
 */
fun NavGraphBuilder.editRecordScreen(
    typeListContent: @Composable (RecordTypeCategoryEnum, Long, (Long) -> Unit) -> Unit,
    assetBottomSheetContent: @Composable (Long, Long, Boolean, (Long) -> Unit) -> Unit,
    tagBottomSheetContent: @Composable (List<Long>, (List<Long>) -> Unit, () -> Unit) -> Unit,
    onRequestNaviToSelectRelatedRecord: () -> Unit,
    onRequestPopBackStack: () -> Unit,
) {
    composable<EditRecord> { backStackEntry ->
        val route = backStackEntry.toRoute<EditRecord>()
        EditRecordRoute(
            recordId = route.recordId,
            assetId = route.assetId,
            typeListContent = typeListContent,
            assetBottomSheetContent = assetBottomSheetContent,
            tagBottomSheetContent = tagBottomSheetContent,
            onRequestNaviToSelectRelatedRecord = onRequestNaviToSelectRelatedRecord,
            onRequestPopBackStack = onRequestPopBackStack,
        )
    }
}

fun NavGraphBuilder.analyticsScreen(
    onRequestNaviToTypeAnalytics: (Long, String?, Boolean) -> Unit,
    onRequestPopBackStack: () -> Unit,
) {
    composable<Analytics> {
        AnalyticsRoute(
            onRequestNaviToTypeAnalytics = onRequestNaviToTypeAnalytics,
            onRequestPopBackStack = onRequestPopBackStack,
        )
    }
}

fun NavGraphBuilder.typedAnalyticsScreen(
    onRequestNaviToEditRecord: (Long) -> Unit,
    onRequestNaviToAssetInfo: (Long) -> Unit,
    onRequestPopBackStack: () -> Unit,
) {
    composable<TypedAnalytics> { backStackEntry ->
        val route = backStackEntry.toRoute<TypedAnalytics>()
        TypedAnalyticsRoute(
            typeId = route.typeId,
            tagId = route.tagId,
            date = route.date,
            onRequestNaviToEditRecord = onRequestNaviToEditRecord,
            onRequestNaviToAssetInfo = onRequestNaviToAssetInfo,
            onRequestPopBackStack = onRequestPopBackStack,
        )
    }
}

fun NavGraphBuilder.selectRelatedRecordScreen(
    onRequestPopBackStack: () -> Unit,
) {
    composable<SelectRelatedRecord> {
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
    composable<RecordCalendar> {
        CalendarRoute(
            recordDetailSheetContent = recordDetailSheetContent,
            onRequestPopBackStack = onRequestPopBackStack,
            onShowSnackbar = onShowSnackbar,
        )
    }
}

fun NavGraphBuilder.searchScreen(
    onRequestNaviToEditRecord: (Long) -> Unit,
    onRequestNaviToAssetInfo: (Long) -> Unit,
    onRequestPopBackStack: () -> Unit,
) {
    composable<RecordSearch> {
        SearchRoute(
            onRequestNaviToEditRecord = onRequestNaviToEditRecord,
            onRequestNaviToAssetInfo = onRequestNaviToAssetInfo,
            onRequestPopBackStack = onRequestPopBackStack,
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
 * @param onRequestNaviToAnalytics 导航到数据分析
 * @param onShowSnackbar 显示 [androidx.compose.material3.Snackbar]，参数：(显示文本，action文本) -> [SnackbarResult]
 */
@Composable
fun LauncherContent(
    recordDetailSheetContent: @Composable (RecordViewsEntity?, () -> Unit) -> Unit,
    onRequestOpenDrawer: () -> Unit,
    onRequestNaviToEditRecord: (Long) -> Unit,
    onRequestNaviToSearch: () -> Unit,
    onRequestNaviToCalendar: () -> Unit,
    onRequestNaviToAnalytics: () -> Unit,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,
) {
    LauncherContentRoute(
        recordDetailSheetContent = recordDetailSheetContent,
        onRequestOpenDrawer = onRequestOpenDrawer,
        onRequestNaviToEditRecord = onRequestNaviToEditRecord,
        onRequestNaviToSearch = onRequestNaviToSearch,
        onRequestNaviToCalendar = onRequestNaviToCalendar,
        onRequestNaviToAnalytics = onRequestNaviToAnalytics,
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
 * @param onRequestNaviToAssetInfo 导航到资产信息
 * @param onRequestDismissSheet 隐藏 sheet
 */
@Composable
fun RecordDetailSheetContent(
    recordEntity: RecordViewsEntity?,
    onRequestNaviToEditRecord: (Long) -> Unit,
    onRequestNaviToAssetInfo: (Long) -> Unit,
    onRequestDismissSheet: () -> Unit,
) {
    RecordDetailsSheet(
        recordData = recordEntity,
        onRequestNaviToEditRecord = onRequestNaviToEditRecord,
        onRequestNaviToAssetInfo = onRequestNaviToAssetInfo,
        onRequestDismissSheet = onRequestDismissSheet,
    )
}
