@file:OptIn(ExperimentalAnimationApi::class)

package cn.wj.android.cashbook.feature.settings.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import cn.wj.android.cashbook.core.model.enums.LauncherMenuAction
import cn.wj.android.cashbook.feature.settings.screen.LauncherRoute
import com.google.accompanist.navigation.animation.composable

const val ROUTE_SETTINGS_LAUNCHER = "settings/launcher"

fun NavController.naviToSettingsLauncher() {
    this.navigate(ROUTE_SETTINGS_LAUNCHER)
}

/**
 * 首页显示
 *
 * @param onMenuClick 菜单点击回调
 * @param pinnedTitle 固定标题
 * @param collapsedTitle 可折叠标题
 * @param content 内容区
 */
fun NavGraphBuilder.settingsLauncherScreen(
    onMenuClick: (LauncherMenuAction) -> Unit,
    pinnedTitle: @Composable () -> Unit,
    collapsedTitle: @Composable () -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    composable(route = ROUTE_SETTINGS_LAUNCHER) {
        LauncherRoute(
            onMenuClick = onMenuClick,
            pinnedTitle = pinnedTitle,
            collapsedTitle = collapsedTitle,
            content = content,
        )
    }
}