package cn.wj.android.cashbook.ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import cn.wj.android.cashbook.core.design.component.CashbookBackground
import cn.wj.android.cashbook.core.ui.LocalNavController
import cn.wj.android.cashbook.feature.assets.navigation.SelectAssetBottomSheet
import cn.wj.android.cashbook.feature.assets.navigation.editAssetScreen
import cn.wj.android.cashbook.feature.assets.navigation.naviToEditAsset
import cn.wj.android.cashbook.feature.records.navigation.LauncherContent
import cn.wj.android.cashbook.feature.records.navigation.editRecordScreen
import cn.wj.android.cashbook.feature.records.navigation.naviToEditRecord
import cn.wj.android.cashbook.feature.records.navigation.naviToSelectRelatedRecord
import cn.wj.android.cashbook.feature.records.navigation.selectRelatedRecordScreen
import cn.wj.android.cashbook.feature.settings.navigation.ROUTE_SETTINGS_LAUNCHER
import cn.wj.android.cashbook.feature.settings.navigation.aboutUsScreen
import cn.wj.android.cashbook.feature.settings.navigation.naviToAboutUs
import cn.wj.android.cashbook.feature.settings.navigation.settingsLauncherScreen
import cn.wj.android.cashbook.feature.tags.navigation.SelectTagsBottomSheet
import cn.wj.android.cashbook.feature.tags.navigation.myTagsScreen
import cn.wj.android.cashbook.feature.tags.navigation.naviToMyTags
import cn.wj.android.cashbook.feature.types.navigation.SelectRecordTypeList
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.rememberAnimatedNavController

/** 开始默认显示路径 */
private const val START_DESTINATION = ROUTE_SETTINGS_LAUNCHER

/** 应用入口 */
@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalAnimationApi::class
)
@Composable
fun MainApp() {
    CashbookBackground {
        val navController = rememberAnimatedNavController()
        val snackbarHostState = remember { SnackbarHostState() }
        CompositionLocalProvider(LocalNavController provides navController) {
            Scaffold(containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
                CashbookNavHost(
                    navController = navController,
                    onShowSnackbar = { message, action ->
                        snackbarHostState.showSnackbar(
                            message = message,
                            actionLabel = action,
                            duration = SnackbarDuration.Short,
                        )
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .consumeWindowInsets(paddingValues),
                )
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CashbookNavHost(
    navController: NavHostController,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,
    modifier: Modifier,
) {
    AnimatedNavHost(
        navController = navController,
        startDestination = START_DESTINATION,
        modifier = modifier,
    ) {

        // 启动页
        settingsLauncherScreen(
            onMyAssetClick = { /* TODO */ },
            onMyBookClick = {/* TODO */ },
            onMyCategoryClick = {/* TODO */ },
            onMyTagClick = navController::naviToMyTags,
            onSettingClick = {/* TODO */ },
            onAboutUsClick = navController::naviToAboutUs,
            content = { openDrawer ->
                LauncherContent(
                    onMenuClick = openDrawer,
                    onAddClick = navController::naviToEditRecord,
                    onSearchClick = { /* TODO */ },
                    onCalendarClick = { /* TODO */ },
                    onMyAssetClick = { /* TODO */ },
                    onRecordItemEditClick = navController::naviToEditRecord,
                )
            },
        )
        // 关于我们
        aboutUsScreen(
            onBackClick = navController::popBackStack,
            onShowSnackbar = onShowSnackbar,
            onVersionInfoClick = {
                // TODO 版本信息
            },
            onUserAgreementAndPrivacyPolicyClick = {
                // TODO 用户协议及隐私政策
            },
        )

        // 我的标签
        myTagsScreen(
            onBackClick = navController::popBackStack,
            onTagStatisticClick = { /* TODO */ },
        )
        // 编辑记录
        editRecordScreen(
            onBackClick = navController::popBackStack,
            onSelectRelatedRecordClick = navController::naviToSelectRelatedRecord,
            selectTypeList = { typeCategory, selectedType, overTypeList, underTypeList, onTypeSelected ->
                SelectRecordTypeList(
                    typeCategory = typeCategory,
                    selectedType = selectedType,
                    overTypeList = overTypeList,
                    underTypeList = underTypeList,
                    onTypeSelected = onTypeSelected,
                    onTypeSettingClick = { /*TODO*/ },
                )
            },
            selectAssetBottomSheet = { selectedType, related, onAssetItemClick ->
                SelectAssetBottomSheet(
                    selectedType = selectedType,
                    related = related,
                    onAddAssetClick = navController::naviToEditAsset,
                    onAssetItemClick = onAssetItemClick
                )
            },
            selectTagBottomSheet = { list, onTagItemClick ->
                SelectTagsBottomSheet(
                    selectedTagIds = list, onTagItemClick = onTagItemClick
                )
            },
        )
        // 选择关联记录
        selectRelatedRecordScreen(
            onBackClick = navController::popBackStack,
        )
        // 编辑资产
        editAssetScreen(
            onBackClick = navController::popBackStack,
        )
    }
}