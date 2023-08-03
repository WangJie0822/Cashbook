package cn.wj.android.cashbook.feature.settings.navigation

import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import cn.wj.android.cashbook.core.model.enums.MarkdownTypeEnum
import cn.wj.android.cashbook.feature.settings.screen.AboutUsRoute
import cn.wj.android.cashbook.feature.settings.screen.BackupAndRecoveryRoute
import cn.wj.android.cashbook.feature.settings.screen.LauncherRoute
import cn.wj.android.cashbook.feature.settings.screen.MarkdownRoute
import cn.wj.android.cashbook.feature.settings.screen.SettingRoute

/** 设置 - 启动页路由 */
const val ROUTE_SETTINGS_LAUNCHER = "settings/launcher"

/** 设置 - 关于我们路由 */
private const val ROUTE_SETTINGS_ABOUT_US = "settings/about_us"

/** 设置 - 设置路由 */
private const val ROUTE_SETTINGS_SETTING = "settings/setting"

/** 设置 - markdown 界面 */
private const val ROUTE_SETTINGS_MARKDOWN_KEY_TYPE = "mdType"
private const val ROUTE_SETTINGS_MARKDOWN =
    "settings/markdown?$ROUTE_SETTINGS_MARKDOWN_KEY_TYPE={$ROUTE_SETTINGS_MARKDOWN_KEY_TYPE}"

/** 设置  - 备份与恢复 */
private const val ROUTE_SETTINGS_BACKUP_AND_RECOVERY = "settings/backup_and_recovery"

/** 跳转到关于我们 */
fun NavController.naviToAboutUs() {
    this.navigate(ROUTE_SETTINGS_ABOUT_US)
}

/** 跳转到设置 */
fun NavController.naviToSetting() {
    this.navigate(ROUTE_SETTINGS_SETTING)
}

/** 跳转到 markdown 界面，显示对应类型 [type] 的数据 */
fun NavController.naviToMarkdown(type: MarkdownTypeEnum) {
    this.navigate(
        ROUTE_SETTINGS_MARKDOWN
            .replace(
                oldValue = "{$ROUTE_SETTINGS_MARKDOWN_KEY_TYPE}",
                newValue = type.ordinal.toString()
            )
    )
}

/** 跳转备份恢复界面 */
fun NavController.naviToBackupAndRecovery() {
    this.navigate(ROUTE_SETTINGS_BACKUP_AND_RECOVERY)
}

/**
 * 首页显示
 */
fun NavGraphBuilder.settingsLauncherScreen(
    onMyAssetClick: () -> Unit,
    onMyBookClick: () -> Unit,
    onMyCategoryClick: () -> Unit,
    onMyTagClick: () -> Unit,
    onSettingClick: () -> Unit,
    onAboutUsClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,
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
            onPrivacyPolicyClick = onPrivacyPolicyClick,
            onShowSnackbar = onShowSnackbar,
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

/**
 * 设置
 */
fun NavGraphBuilder.settingScreen(
    onBackClick: () -> Unit,
    onBackupAndRecoveryClick: () -> Unit,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,
) {
    composable(route = ROUTE_SETTINGS_SETTING) {
        SettingRoute(
            onBackClick = onBackClick,
            onBackupAndRecoveryClick = onBackupAndRecoveryClick,
            onShowSnackbar = onShowSnackbar,
        )
    }
}

fun NavGraphBuilder.markdownScreen(
    onBackClick: () -> Unit,
) {
    composable(
        route = ROUTE_SETTINGS_MARKDOWN,
        arguments = listOf(
            navArgument(ROUTE_SETTINGS_MARKDOWN_KEY_TYPE) {
                type = NavType.IntType
                defaultValue = -1
            },
        ),
    ) {
        val mdOrdinal = it.arguments?.getInt(ROUTE_SETTINGS_MARKDOWN_KEY_TYPE) ?: -1
        MarkdownRoute(
            markdownType = MarkdownTypeEnum.ordinalOf(mdOrdinal),
            onBackClick = onBackClick,
        )
    }
}

fun NavGraphBuilder.backupAndRecoveryScreen(
    onBackClick: () -> Unit,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,
) {
    composable(route = ROUTE_SETTINGS_BACKUP_AND_RECOVERY) {
        BackupAndRecoveryRoute(
            onBackClick = onBackClick,
            onShowSnackbar = onShowSnackbar,
        )
    }
}