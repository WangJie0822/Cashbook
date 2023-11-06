package cn.wj.android.cashbook.feature.types.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.feature.types.screen.EditRecordTypeListRoute
import cn.wj.android.cashbook.feature.types.screen.MyCategoriesRoute

private const val ROUTE_MY_CATEGORIES = "type/my_categories"

fun NavController.naviToMyCategories() {
    this.navigate(ROUTE_MY_CATEGORIES)
}

/**
 * 我的分类界面
 *
 * @param onRequestPopBackStack 导航到上一级
 */
fun NavGraphBuilder.myCategoriesScreen(
    onRequestNaviToTypeStatistics: (Long) -> Unit,
    onRequestPopBackStack: () -> Unit,
) {
    composable(ROUTE_MY_CATEGORIES) {
        MyCategoriesRoute(
            onRequestNaviToTypeStatistics = onRequestNaviToTypeStatistics,
            onRequestPopBackStack = onRequestPopBackStack,
        )
    }
}

/**
 * 编辑记录页面标签列表
 *
 * @param typeCategory 记录大类
 * @param defaultTypeId 默认类型 id
 * @param onTypeSelect 类型选中回调
 * @param onRequestNaviToTypeManager 导航到类型管理
 */
@Composable
fun EditRecordTypeListContent(
    typeCategory: RecordTypeCategoryEnum,
    defaultTypeId: Long,
    onTypeSelect: (Long) -> Unit,
    onRequestNaviToTypeManager: () -> Unit,
) {
    EditRecordTypeListRoute(
        typeCategory = typeCategory,
        defaultTypeId = defaultTypeId,
        onTypeSelect = onTypeSelect,
        onRequestNaviToTypeManager = onRequestNaviToTypeManager,
    )
}