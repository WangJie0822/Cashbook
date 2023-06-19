@file:OptIn(ExperimentalAnimationApi::class)

package cn.wj.android.cashbook.feature.settings.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import cn.wj.android.cashbook.feature.settings.screen.AboutUsRoute
import cn.wj.android.cashbook.feature.settings.screen.LauncherRoute
import cn.wj.android.cashbook.feature.settings.screen.SettingRoute
import com.google.accompanist.navigation.animation.composable

/** 设置 - 启动页路由 */
const val ROUTE_SETTINGS_LAUNCHER = "settings/launcher"

/** 设置 - 关于我们路由 */
const val ROUTE_SETTINGS_ABOUT_US = "settings/about_us"

/** 设置 - 设置路由 */
const val ROUTE_SETTINGS_SETTING = "settings/setting"

/** 跳转到关于我们 */
fun NavController.naviToAboutUs() {
    this.navigate(ROUTE_SETTINGS_ABOUT_US)
}

/** 跳转到设置 */
fun NavController.naviToSetting() {
    this.navigate(ROUTE_SETTINGS_SETTING)
}

/**
 * 首页显示
 *
 * @param content 内容区
 */
fun NavGraphBuilder.settingsLauncherScreen(
    onMyAssetClick: () -> Unit,
    onMyBookClick: () -> Unit,
    onMyCategoryClick: () -> Unit,
    onMyTagClick: () -> Unit,
    onSettingClick: () -> Unit,
    onAboutUsClick: () -> Unit,
    content: @Composable (() -> Unit) -> Unit,
) {
    composable(route = ROUTE_SETTINGS_LAUNCHER) {
        LauncherRoute(
            onMyAssetClick = onMyAssetClick,
            onMyBookClick = onMyBookClick,
            onMyCategoryClick = onMyCategoryClick,
            onMyTagClick = onMyTagClick,
            onSettingClick = onSettingClick,
            onAboutUsClick = onAboutUsClick,
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

fun NavGraphBuilder.settingScreen(
    onBackClick: () -> Unit,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,

) {
    composable(route = ROUTE_SETTINGS_SETTING) {
        SettingRoute(
            onBackClick = onBackClick,
            onShowSnackbar = onShowSnackbar,
        )
    }
}