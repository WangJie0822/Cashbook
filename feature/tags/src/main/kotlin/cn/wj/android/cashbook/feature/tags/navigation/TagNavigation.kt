@file:OptIn(ExperimentalAnimationApi::class, ExperimentalAnimationApi::class)

package cn.wj.android.cashbook.feature.tags.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import cn.wj.android.cashbook.core.model.entity.TagEntity
import cn.wj.android.cashbook.feature.tags.screen.MyTagsRoute
import cn.wj.android.cashbook.feature.tags.screen.SelectTagBottomSheetScreen
import com.google.accompanist.navigation.animation.composable

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
fun SelectTagsBottomSheet(
    selectedTagIds: List<Long>,
    onTagItemClick: (TagEntity) -> Unit,
) {
    SelectTagBottomSheetScreen(
        selectedTagIds = selectedTagIds,
        onTagItemClick = onTagItemClick,
    )
}