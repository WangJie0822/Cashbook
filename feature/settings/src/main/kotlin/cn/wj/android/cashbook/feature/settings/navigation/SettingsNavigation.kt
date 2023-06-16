@file:OptIn(ExperimentalAnimationApi::class)

package cn.wj.android.cashbook.feature.settings.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import cn.wj.android.cashbook.core.model.enums.LauncherMenuAction
import cn.wj.android.cashbook.feature.settings.screen.AboutUsRoute
import cn.wj.android.cashbook.feature.settings.screen.LauncherRoute
import com.google.accompanist.navigation.animation.composable

/** 设置 - 启动页路由 */
const val ROUTE_SETTINGS_LAUNCHER = "settings/launcher"

/** 设置 - 关于我们路由 */
const val ROUTE_SETTINGS_ABOUT_US = "settings/about_us"

/** 跳转到关于我们 */
fun NavController.naviToAboutUs() {
    this.navigate(ROUTE_SETTINGS_ABOUT_US)
}

/**
 * 首页显示
 *
 * @param onMenuClick 菜单点击回调
 * @param content 内容区
 */
fun NavGraphBuilder.settingsLauncherScreen(
    onMenuClick: (LauncherMenuAction) -> Unit,
    content: @Composable (() -> Unit) -> Unit,
) {
    composable(route = ROUTE_SETTINGS_LAUNCHER) {
        LauncherRoute(
            onMenuClick = onMenuClick,
            content = content,
        )
    }
}

/**
 * 关于我们
 */
fun NavGraphBuilder.aboutUsScreen(
    onBackClick: () -> Unit,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,
    onVersionInfoClick: () -> Unit,
    onUserAgreementAndPrivacyPolicyClick: () -> Unit,
) {
    composable(route = ROUTE_SETTINGS_ABOUT_US) {
        AboutUsRoute(
            onBackClick = onBackClick,
            onShowSnackbar = onShowSnackbar,
            onVersionInfoClick = onVersionInfoClick,
            onUserAgreementAndPrivacyPolicyClick = onUserAgreementAndPrivacyPolicyClick,
        )
    }
}