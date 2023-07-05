package cn.wj.android.cashbook.feature.tags.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import cn.wj.android.cashbook.core.model.entity.TagEntity
import cn.wj.android.cashbook.feature.tags.screen.EditRecordSelectTagBottomSheetRoute
import cn.wj.android.cashbook.feature.tags.screen.MyTagsRoute

private const val ROUTE_MY_TAGS = "tag/my_tag"

fun NavController.naviToMyTags() {
    this.navigate(ROUTE_MY_TAGS)
}

fun NavGraphBuilder.myTagsScreen(
    onBackClick: () -> Unit,
    onTagStatisticClick: (TagEntity) -> Unit,
) {
    composable(ROUTE_MY_TAGS) {
        MyTagsRoute(
            onBackClick = onBackClick,
            onTagStatisticClick = onTagStatisticClick,
        )
    }
}

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