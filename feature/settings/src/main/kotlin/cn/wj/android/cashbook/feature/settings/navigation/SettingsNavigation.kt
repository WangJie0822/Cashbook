package cn.wj.android.cashbook.feature.settings.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import cn.wj.android.cashbook.core.model.enums.LauncherMenuAction
import cn.wj.android.cashbook.feature.settings.screen.LauncherRoute

const val ROUTE_SETTINGS_LAUNCHER = "settings/launcher"

fun NavController.naviToSettingsLauncher() {
    this.navigate(ROUTE_SETTINGS_LAUNCHER)
}

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