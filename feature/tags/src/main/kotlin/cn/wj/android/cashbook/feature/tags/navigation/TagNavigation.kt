package cn.wj.android.cashbook.feature.tags.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import cn.wj.android.cashbook.feature.tags.screen.EditRecordSelectTagBottomSheetRoute
import cn.wj.android.cashbook.feature.tags.screen.MyTagsRoute

private const val ROUTE_MY_TAGS = "tag/my_tag"

fun NavController.naviToMyTags() {
    this.navigate(ROUTE_MY_TAGS)
}

/**
 * 我的标签
 *
 * @param onRequestNaviToTagStatistic 导航到标签数据分析
 * @param onRequestPopBackStack 导航到上一级
 */
fun NavGraphBuilder.myTagsScreen(
    onRequestNaviToTagStatistic: (Long) -> Unit,
    onRequestPopBackStack: () -> Unit,
) {
    composable(ROUTE_MY_TAGS) {
        MyTagsRoute(
            onRequestPopBackStack = onRequestPopBackStack,
            onRequestNaviToTagStatistic = onRequestNaviToTagStatistic,
        )
    }
}

/**
 * 编辑记录界面选择标签抽屉
 *
 * @param selectedTagIdList 已选择标签id列表
 * @param onTagIdListChange 已选择标签id列表变化回调
 */
@Composable
fun EditRecordSelectTagBottomSheetContent(
    selectedTagIdList: List<Long>,
    onTagIdListChange: (List<Long>) -> Unit,
) {
    EditRecordSelectTagBottomSheetRoute(
        selectedTagIdList = selectedTagIdList,
        onTagIdListChange = onTagIdListChange,
    )
}