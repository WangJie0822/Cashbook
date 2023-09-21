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
 * - 首页显示主体，提供左侧抽屉菜单、用户隐私协议弹窗、安全校验功能，具体内容显示通过 [content] 参数提供
 *
 * @param onRequestNaviToMyAsset 导航到我的资产
 * @param onRequestNaviToMyBooks 导航到我的账本
 * @param onRequestNaviToMyCategory 导航到我的分类
 * @param onRequestNaviToMyTags 导航到我的标签
 * @param onRequestNaviToSetting 导航到设置
 * @param onRequestNaviToAboutUs 导航到关于我们
 * @param content 显示内容，参数 (打开抽屉) -> [Unit]
 */
fun NavGraphBuilder.settingsLauncherScreen(
    onRequestNaviToMyAsset: () -> Unit,
    onRequestNaviToMyBooks: () -> Unit,
    onRequestNaviToMyCategory: () -> Unit,
    onRequestNaviToMyTags: () -> Unit,
    onRequestNaviToSetting: () -> Unit,
    onRequestNaviToAboutUs: () -> Unit,
    content: @Composable (() -> Unit) -> Unit,
) {
    composable(route = ROUTE_SETTINGS_LAUNCHER) {
        LauncherRoute(
            onRequestNaviToMyAsset = onRequestNaviToMyAsset,
            onRequestNaviToMyBooks = onRequestNaviToMyBooks,
            onRequestNaviToMyCategory = onRequestNaviToMyCategory,
            onRequestNaviToMyTags = onRequestNaviToMyTags,
            onRequestNaviToSetting = onRequestNaviToSetting,
            onRequestNaviToAboutUs = onRequestNaviToAboutUs,
            content = content,
        )
    }
}

/**
 * 关于我们
 *
 * @param onRequestNaviToChangelog 导航到修改日志
 * @param onRequestNaviToPrivacyPolicy 导航到用户隐私协议
 * @param onRequestPopBackStack 导航到上一级
 * @param onShowSnackbar 显示 [androidx.compose.material3.Snackbar]，参数：(显示文本，action文本) -> [SnackbarResult]
 */
fun NavGraphBuilder.aboutUsScreen(
    onRequestNaviToChangelog: () -> Unit,
    onRequestNaviToPrivacyPolicy: () -> Unit,
    onRequestPopBackStack: () -> Unit,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,
) {
    composable(route = ROUTE_SETTINGS_ABOUT_US) {
        AboutUsRoute(
            onRequestNaviToChangelog = onRequestNaviToChangelog,
            onRequestNaviToPrivacyPolicy = onRequestNaviToPrivacyPolicy,
            onRequestPopBackStack = onRequestPopBackStack,
            onShowSnackbar = onShowSnackbar,
        )
    }
}

/**
 * 设置
 *
 * @param onRequestNaviToBackupAndRecovery 导航到备份与恢复
 * @param onRequestPopBackStack 导航到上一级
 * @param onShowSnackbar 显示 [androidx.compose.material3.Snackbar]，参数：(显示文本，action文本) -> [SnackbarResult]
 */
fun NavGraphBuilder.settingScreen(
    onRequestNaviToBackupAndRecovery: () -> Unit,
    onRequestPopBackStack: () -> Unit,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,
) {
    composable(route = ROUTE_SETTINGS_SETTING) {
        SettingRoute(
            onRequestPopBackStack = onRequestPopBackStack,
            onRequestNaviToBackupAndRecovery = onRequestNaviToBackupAndRecovery,
            onShowSnackbar = onShowSnackbar,
        )
    }
}

/**
 * Markdown 显示界面
 *
 * @param onRequestPopBackStack 导航到上一级
 */
fun NavGraphBuilder.markdownScreen(
    onRequestPopBackStack: () -> Unit,
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
            onRequestPopBackStack = onRequestPopBackStack,
        )
    }
}

/**
 * 备份恢复界面
 *
 * @param onRequestPopBackStack 导航到上一级
 * @param onShowSnackbar 显示 [androidx.compose.material3.Snackbar]，参数：(显示文本，action文本) -> [SnackbarResult]
 */
fun NavGraphBuilder.backupAndRecoveryScreen(
    onRequestPopBackStack: () -> Unit,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,
) {
    composable(route = ROUTE_SETTINGS_BACKUP_AND_RECOVERY) {
        BackupAndRecoveryRoute(
            onRequestPopBackStack = onRequestPopBackStack,
            onShowSnackbar = onShowSnackbar,
        )
    }
}