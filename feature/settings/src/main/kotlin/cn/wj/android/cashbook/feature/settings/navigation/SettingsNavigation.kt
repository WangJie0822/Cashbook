package cn.wj.android.cashbook.feature.settings.navigation

import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import cn.wj.android.cashbook.feature.settings.screen.LauncherRoute

const val ROUTE_SETTINGS_LAUNCHER = "settings/launcher"

fun NavController.naviToSettingsLauncher() {
    this.navigate(ROUTE_SETTINGS_LAUNCHER)
}

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.settingsLauncherScreen(content: @Composable (Modifier, DrawerState) -> Unit) {
    composable(route = ROUTE_SETTINGS_LAUNCHER) {
        LauncherRoute(
            content = content
        )
    }
}