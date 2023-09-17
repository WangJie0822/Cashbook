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
import cn.wj.android.cashbook.feature.types.navigation.myCategoriesScreen
import cn.wj.android.cashbook.feature.types.navigation.naviToMyCategories

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
    NavHost(
        navController = navController,
        startDestination = START_DESTINATION,
        modifier = modifier,
    ) {

        // 启动页
        settingsLauncherScreen(
            onRequestNaviToMyAsset = navController::naviToMyAsset,
            onRequestNaviToMyBooks = navController::naviToMyBooks,
            onRequestNaviToMyCategory = navController::naviToMyCategories,
            onRequestNaviToMyTags = navController::naviToMyTags,
            onRequestNaviToSetting = navController::naviToSetting,
            onRequestNaviToAboutUs = navController::naviToAboutUs,
            onRequestNaviToPrivacyPolicy = { navController.naviToMarkdown(MarkdownTypeEnum.PRIVACY_POLICY) },
            onShowSnackbar = onShowSnackbar,
            content = { onRequestOpenDrawer ->
                LauncherContent(
                    recordDetailSheetContent = { recordEntity, onRequestDismissSheet ->
                        RecordDetailSheetContent(
                            recordEntity = recordEntity,
                            onRequestNaviToEditRecord = navController::naviToEditRecord,
                            onRequestDismissSheet = onRequestDismissSheet,
                        )
                    },
                    onRequestOpenDrawer = onRequestOpenDrawer,
                    onRequestNaviToEditRecord = navController::naviToEditRecord,
                    onRequestNaviToSearch = { /* TODO */ },
                    onRequestNaviToCalendar = navController::naviToCalendar,
                    onRequestNaviToMyAsset = navController::naviToMyAsset,
                    onShowSnackbar = onShowSnackbar,
                )
            },
        )
        // 关于我们
        aboutUsScreen(
            onShowSnackbar = onShowSnackbar,
            onRequestNaviToChangelog = {
                // 版本信息
                navController.naviToMarkdown(MarkdownTypeEnum.CHANGELOG)
            },
            onRequestNaviToPrivacyPolicy = {
                // 用户协议及隐私政策
                navController.naviToMarkdown(MarkdownTypeEnum.PRIVACY_POLICY)
            },
            onRequestPopBackStack = navController::popBackStack,
        )
        // 设置
        settingScreen(
            onRequestNaviToBackupAndRecovery = navController::naviToBackupAndRecovery,
            onRequestPopBackStack = navController::popBackStack,
            onShowSnackbar = onShowSnackbar,
        )
        // Markdown 显示界面
        markdownScreen(
            onRequestPopBackStack = navController::popBackStack,
        )
        // 备份与恢复
        backupAndRecoveryScreen(
            onRequestPopBackStack = navController::popBackStack,
            onShowSnackbar = onShowSnackbar,
        )

        // 我的标签
        myTagsScreen(
            onRequestNaviToTagStatistic = { /* TODO */ },
            onRequestPopBackStack = navController::popBackStack,
        )

        // 编辑记录
        editRecordScreen(
            typeListContent = { typeCategory, selectedTypeId, onTypeSelect, headerContent, footerContent ->
                EditRecordTypeListContent(
                    typeCategory = typeCategory,
                    selectedTypeId = selectedTypeId,
                    onTypeSelect = onTypeSelect,
                    onRequestNaviToTypeManager = navController::naviToMyCategories,
                    headerContent = headerContent,
                    footerContent = footerContent,
                )
            },
            assetBottomSheetContent = { currentTypeId, isRelated, onAssetChange ->
                EditRecordSelectAssetBottomSheetContent(
                    currentTypeId = currentTypeId,
                    isRelated = isRelated,
                    onAssetChange = onAssetChange,
                    onRequestNaviToEditAsset = navController::naviToEditAsset,
                )
            },
            tagBottomSheetContent = { selectedTagIdList, onTagIdListChange ->
                EditRecordSelectTagBottomSheetContent(
                    selectedTagIdList = selectedTagIdList,
                    onTagIdListChange = onTagIdListChange,
                )
            },
            onRequestPopBackStack = navController::popBackStack,
        )
        // 记录日历
        calendarScreen(
            recordDetailSheetContent = { recordEntity, onRequestDismissSheet ->
                RecordDetailSheetContent(
                    recordEntity = recordEntity,
                    onRequestNaviToEditRecord = navController::naviToEditRecord,
                    onRequestDismissSheet = onRequestDismissSheet,
                )
            },
            onRequestPopBackStack = navController::popBackStack,
            onShowSnackbar = onShowSnackbar,
        )

        // 我的资产
        myAssetScreen(
            onRequestNaviToAssetInfo = navController::naviToAssetInfo,
            onRequestNaviToAddAsset = navController::naviToEditAsset,
            onRequestNaviToInvisibleAsset = {/*TODO*/ },
            onRequestPopBackStack = navController::popBackStack,
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
            recordDetailSheetContent = { recordEntity, onRequestDismissSheet ->
                RecordDetailSheetContent(
                    recordEntity = recordEntity,
                    onRequestNaviToEditRecord = navController::naviToEditRecord,
                    onRequestDismissSheet = onRequestDismissSheet,
                )
            },
            onRequestNaviToEditAsset = navController::naviToEditAsset,
            onRequestPopBackStack = navController::popBackStack,
        )
        // 编辑资产
        editAssetScreen(
            onRequestPopBackStack = navController::popBackStack,
        )

        // 我的账本
        myBooksScreen(
            onRequestNaviToEditBook = navController::naviToEditBook,
            onRequestPopBackStack = navController::popBackStack,
        )
        // 编辑账本
        editBookScreen(
            onRequestPopBackStack = navController::popBackStack,
        )

        // 我的分类
        myCategoriesScreen(
            onRequestNaviToEdiType = { id, parentId -> /*TODO*/ },
            onRequestNaviToTypeStatistics = {  /*TODO*/ },
            onRequestPopBackStack = navController::popBackStack,
        )
    }
}