@file:OptIn(ExperimentalAnimationApi::class, ExperimentalAnimationApi::class)

package cn.wj.android.cashbook.feature.tags.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import cn.wj.android.cashbook.core.model.entity.TagEntity
import cn.wj.android.cashbook.core.ui.controller
import cn.wj.android.cashbook.feature.tags.screen.MyTagsRoute
import com.google.accompanist.navigation.animation.composable

private const val ROUTE_MY_TAGS = "tag/my_tag"

fun NavController.naviToMyTags() {
    this.navigate(ROUTE_MY_TAGS)
}

fun NavGraphBuilder.myTagsScreen(
    onTagStatisticClick: (TagEntity) -> Unit,
) {
    composable(ROUTE_MY_TAGS) {
        MyTagsRoute(
            onBackClick = { controller?.popBackStack() },
            onTagStatisticClick = onTagStatisticClick,
        )
    }
}