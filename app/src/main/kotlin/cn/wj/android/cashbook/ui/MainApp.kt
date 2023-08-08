package cn.wj.android.cashbook.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import cn.wj.android.cashbook.core.design.component.CashbookGradientBackground
import cn.wj.android.cashbook.core.design.component.CashbookScaffold
import cn.wj.android.cashbook.core.model.enums.MarkdownTypeEnum
import cn.wj.android.cashbook.core.ui.LocalNavController
import cn.wj.android.cashbook.feature.assets.navigation.EditRecordSelectAssetBottomSheetContent
import cn.wj.android.cashbook.feature.assets.navigation.assetInfoScreen
import cn.wj.android.cashbook.feature.assets.navigation.editAssetScreen
import cn.wj.android.cashbook.feature.assets.navigation.myAssetScreen
import cn.wj.android.cashbook.feature.assets.navigation.naviToAssetInfo
import cn.wj.android.cashbook.feature.assets.navigation.naviToEditAsset
import cn.wj.android.cashbook.feature.assets.navigation.naviToMyAsset
import cn.wj.android.cashbook.feature.books.navigation.editBookScreen
import cn.wj.android.cashbook.feature.books.navigation.myBooksScreen
import cn.wj.android.cashbook.feature.books.navigation.naviToEditBook
import cn.wj.android.cashbook.feature.books.navigation.naviToMyBooks
import cn.wj.android.cashbook.feature.records.navigation.AssetInfoContent
import cn.wj.android.cashbook.feature.records.navigation.ConfirmDeleteRecordDialogContent
import cn.wj.android.cashbook.feature.records.navigation.LauncherContent
import cn.wj.android.cashbook.feature.records.navigation.RecordDetailSheetContent
import cn.wj.android.cashbook.feature.records.navigation.calendarScreen
import cn.wj.android.cashbook.feature.records.navigation.editRecordScreen
import cn.wj.android.cashbook.feature.records.navigation.naviToCalendar
import cn.wj.android.cashbook.feature.records.navigation.naviToEditRecord
import cn.wj.android.cashbook.feature.settings.navigation.ROUTE_SETTINGS_LAUNCHER
import cn.wj.android.cashbook.feature.settings.navigation.aboutUsScreen
import cn.wj.android.cashbook.feature.settings.navigation.backupAndRecoveryScreen
import cn.wj.android.cashbook.feature.settings.navigation.markdownScreen
import cn.wj.android.cashbook.feature.settings.navigation.naviToAboutUs
import cn.wj.android.cashbook.feature.settings.navigation.naviToBackupAndRecovery
import cn.wj.android.cashbook.feature.settings.navigation.naviToMarkdown
import cn.wj.android.cashbook.feature.settings.navigation.naviToSetting
import cn.wj.android.cashbook.feature.settings.navigation.settingScreen
import cn.wj.android.cashbook.feature.settings.navigation.settingsLauncherScreen
import cn.wj.android.cashbook.feature.tags.navigation.EditRecordSelectTagBottomSheetContent
import cn.wj.android.cashbook.feature.tags.navigation.myTagsScreen
import cn.wj.android.cashbook.feature.tags.navigation.naviToMyTags
import cn.wj.android.cashbook.feature.types.navigation.EditRecordTypeListContent

/** 开始默认显示路径 */
private const val START_DESTINATION = ROUTE_SETTINGS_LAUNCHER

/** 应用入口 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {

    CashbookGradientBackground {
        val navController = rememberNavController()
        val snackbarHostState = remember { SnackbarHostState() }

        CompositionLocalProvider(LocalNavController provides navController) {
            CashbookScaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(snackbarHostState) },
            ) { paddingValues ->
                CashbookNavHost(
                    modifier = Modifier.padding(paddingValues),
                    navController = navController,
                    onShowSnackbar = { message, action ->
                        snackbarHostState.showSnackbar(
                            message = message,
                            actionLabel = action,
                            duration = SnackbarDuration.Short,
                        )
                    },
                )
            }
        }
    }
}

@Composable
fun CashbookNavHost(
    navController: NavHostController,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,
    modifier: Modifier = Modifier,
) {
    // FIXME 使用 AnimatedNavHost 从二级界面返回时快速点击左上角菜单会导致 Navigation 不显示，后续添加界面动画时需修复此问题
    NavHost(
        navController = navController,
        startDestination = START_DESTINATION,
        modifier = modifier,
    ) {

        // 启动页
        settingsLauncherScreen(
            onMyAssetClick = navController::naviToMyAsset,
            onMyBookClick = navController::naviToMyBooks,
            onMyCategoryClick = {/* TODO */ },
            onMyTagClick = navController::naviToMyTags,
            onSettingClick = navController::naviToSetting,
            onAboutUsClick = navController::naviToAboutUs,
            onPrivacyPolicyClick = { navController.naviToMarkdown(MarkdownTypeEnum.PRIVACY_POLICY) },
            onShowSnackbar = onShowSnackbar,
            content = { openDrawer ->
                LauncherContent(
                    onEditRecordClick = navController::naviToEditRecord,
                    onMenuClick = openDrawer,
                    onSearchClick = { /* TODO */ },
                    onCalendarClick = navController::naviToCalendar,
                    onMyAssetClick = navController::naviToMyAsset,
                    onShowSnackbar = onShowSnackbar,
                )
            },
        )
        // 关于我们
        aboutUsScreen(
            onBackClick = navController::popBackStack,
            onShowSnackbar = onShowSnackbar,
            onVersionInfoClick = {
                // 版本信息
                navController.naviToMarkdown(MarkdownTypeEnum.CHANGELOG)
            },
            onUserAgreementAndPrivacyPolicyClick = {
                // 用户协议及隐私政策
                navController.naviToMarkdown(MarkdownTypeEnum.PRIVACY_POLICY)
            },
        )
        // 设置
        settingScreen(
            onBackClick = navController::popBackStack,
            onBackupAndRecoveryClick = navController::naviToBackupAndRecovery,
            onShowSnackbar = onShowSnackbar,
        )
        // markdown
        markdownScreen(
            onBackClick = navController::popBackStack,
        )
        // 备份与恢复
        backupAndRecoveryScreen(
            onBackClick = navController::popBackStack,
            onShowSnackbar = onShowSnackbar,
        )

        // 我的标签
        myTagsScreen(
            onBackClick = navController::popBackStack,
            onTagStatisticClick = { /* TODO */ },
        )
        // 编辑记录
        editRecordScreen(
            typeListContent = { modifier, typeCategory, selectedTypeId, onTypeSelect, headerContent, footerContent ->
                EditRecordTypeListContent(
                    typeCategory = typeCategory,
                    selectedTypeId = selectedTypeId,
                    onTypeSelect = onTypeSelect,
                    headerContent = headerContent,
                    footerContent = footerContent,
                    onTypeSettingClick = { /*TODO*/ },
                    modifier = modifier,
                )
            },
            selectAssetBottomSheetContent = { currentTypeId, isRelated, onAssetChange ->
                EditRecordSelectAssetBottomSheetContent(
                    currentTypeId = currentTypeId,
                    isRelated = isRelated,
                    onAssetChange = onAssetChange,
                    onAddAssetClick = navController::naviToEditAsset,
                )
            },
            selectTagBottomSheetContent = { selectedTagIdList, onTagIdListChange ->
                EditRecordSelectTagBottomSheetContent(
                    selectedTagIdList = selectedTagIdList,
                    onTagIdListChange = onTagIdListChange,
                )
            },
            onBackClick = navController::popBackStack,
        )
        // TODO 选择关联记录
//        selectRelatedRecordScreen(
//            onBackClick = navController::popBackStack,
//        )
        // 日历
        calendarScreen(
            onRecordItemEditClick = navController::naviToEditRecord,
            onBackClick = navController::popBackStack,
            onShowSnackbar = onShowSnackbar,
        )

        // 我的资产
        myAssetScreen(
            onAssetItemClick = navController::naviToAssetInfo,
            onAddAssetClick = navController::naviToEditAsset,
            onInvisibleAssetClick = {/*TODO*/ },
            onBackClick = navController::popBackStack,
        )
        // 资产信息
        assetInfoScreen(
            assetRecordListContent = { assetId, topContent, onRecordItemClick ->
                AssetInfoContent(
                    assetId = assetId,
                    topContent = topContent,
                    onRecordItemClick = onRecordItemClick,
                )
            },
            recordDetailSheetContent = { recordEntity, onRecordItemDeleteClick, dismissBottomSheet ->
                RecordDetailSheetContent(
                    recordEntity = recordEntity,
                    onRecordItemEditClick = { recordId ->
                        navController.naviToEditRecord(recordId)
                        dismissBottomSheet()
                    },
                    onRecordItemDeleteClick = onRecordItemDeleteClick,
                )
            },
            confirmDeleteRecordDialogContent = { recordId, onResult, onDialogDismiss ->
                ConfirmDeleteRecordDialogContent(
                    recordId = recordId,
                    onResult = onResult,
                    onDialogDismiss = onDialogDismiss,
                )
            },
            onEditAssetClick = navController::naviToEditAsset,
            onBackClick = navController::popBackStack,
        )
        // 编辑资产
        editAssetScreen(
            onBackClick = navController::popBackStack,
        )

        // 我的账本
        myBooksScreen(
            onEditBookClick = navController::naviToEditBook,
            onBackClick = navController::popBackStack,
        )
        // 编辑账本
        editBookScreen(
            onBackClick = navController::popBackStack,
        )
    }
}