/*
 * Copyright 2021 The Cashbook Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.wj.android.cashbook.ui

import android.app.Activity
import android.text.Spanned
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import cn.wj.android.cashbook.core.common.PASSWORD_REGEX
import cn.wj.android.cashbook.core.common.PRIVACY_POLICY_FILE_PATH
import cn.wj.android.cashbook.core.common.TestTag
import cn.wj.android.cashbook.core.common.tools.isMatch
import cn.wj.android.cashbook.core.design.component.CashbookGradientBackground
import cn.wj.android.cashbook.core.design.component.CbAlertDialog
import cn.wj.android.cashbook.core.design.component.CbIconButton
import cn.wj.android.cashbook.core.design.component.CbPasswordTextField
import cn.wj.android.cashbook.core.design.component.CbScaffold
import cn.wj.android.cashbook.core.design.component.CbTextButton
import cn.wj.android.cashbook.core.design.component.Loading
import cn.wj.android.cashbook.core.design.component.TextFieldState
import cn.wj.android.cashbook.core.design.component.rememberLifecycleObserver
import cn.wj.android.cashbook.core.design.component.rememberSnackbarHostState
import cn.wj.android.cashbook.core.design.icon.CbIcons
import cn.wj.android.cashbook.core.design.security.biometric.BiometricAuthenticate
import cn.wj.android.cashbook.core.design.security.biometric.BiometricAuthenticateHintData
import cn.wj.android.cashbook.core.design.security.biometric.ProvideBiometricAuthenticateHintData
import cn.wj.android.cashbook.core.model.entity.UpgradeInfoEntity
import cn.wj.android.cashbook.core.model.enums.MarkdownTypeEnum
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.core.ui.LocalNavController
import cn.wj.android.cashbook.core.ui.ProgressDialog
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.core.ui.popBackStackSafety
import cn.wj.android.cashbook.feature.assets.navigation.EditRecordSelectAssetBottomSheetContent
import cn.wj.android.cashbook.feature.assets.navigation.assetInfoScreen
import cn.wj.android.cashbook.feature.assets.navigation.editAssetScreen
import cn.wj.android.cashbook.feature.assets.navigation.invisibleAssetScreen
import cn.wj.android.cashbook.feature.assets.navigation.myAssetScreen
import cn.wj.android.cashbook.feature.assets.navigation.naviToAssetInfo
import cn.wj.android.cashbook.feature.assets.navigation.naviToEditAsset
import cn.wj.android.cashbook.feature.assets.navigation.naviToInvisibleAsset
import cn.wj.android.cashbook.feature.assets.navigation.naviToMyAsset
import cn.wj.android.cashbook.feature.books.navigation.editBookScreen
import cn.wj.android.cashbook.feature.books.navigation.myBooksScreen
import cn.wj.android.cashbook.feature.books.navigation.naviToEditBook
import cn.wj.android.cashbook.feature.books.navigation.naviToMyBooks
import cn.wj.android.cashbook.feature.records.navigation.AssetInfoContent
import cn.wj.android.cashbook.feature.records.navigation.LauncherContent
import cn.wj.android.cashbook.feature.records.navigation.RecordDetailSheetContent
import cn.wj.android.cashbook.feature.records.navigation.analyticsScreen
import cn.wj.android.cashbook.feature.records.navigation.calendarScreen
import cn.wj.android.cashbook.feature.records.navigation.editRecordScreen
import cn.wj.android.cashbook.feature.records.navigation.naviToAnalytics
import cn.wj.android.cashbook.feature.records.navigation.naviToCalendar
import cn.wj.android.cashbook.feature.records.navigation.naviToEditRecord
import cn.wj.android.cashbook.feature.records.navigation.naviToSearch
import cn.wj.android.cashbook.feature.records.navigation.naviToSelectRelatedRecord
import cn.wj.android.cashbook.feature.records.navigation.naviToTypedAnalytics
import cn.wj.android.cashbook.feature.records.navigation.searchScreen
import cn.wj.android.cashbook.feature.records.navigation.selectRelatedRecordScreen
import cn.wj.android.cashbook.feature.records.navigation.typedAnalyticsScreen
import cn.wj.android.cashbook.feature.settings.enums.MainAppBookmarkEnum
import cn.wj.android.cashbook.feature.settings.enums.SettingPasswordStateEnum
import cn.wj.android.cashbook.feature.settings.navigation.ROUTE_SETTINGS_LAUNCHER
import cn.wj.android.cashbook.feature.settings.navigation.aboutUsScreen
import cn.wj.android.cashbook.feature.settings.navigation.backupAndRecoveryScreen
import cn.wj.android.cashbook.feature.settings.navigation.naviToAboutUs
import cn.wj.android.cashbook.feature.settings.navigation.naviToBackupAndRecovery
import cn.wj.android.cashbook.feature.settings.navigation.naviToSetting
import cn.wj.android.cashbook.feature.settings.navigation.settingScreen
import cn.wj.android.cashbook.feature.settings.navigation.settingsLauncherScreen
import cn.wj.android.cashbook.feature.settings.viewmodel.MainAppUiState
import cn.wj.android.cashbook.feature.settings.viewmodel.MainAppViewModel
import cn.wj.android.cashbook.feature.tags.navigation.EditRecordSelectTagBottomSheetContent
import cn.wj.android.cashbook.feature.tags.navigation.myTagsScreen
import cn.wj.android.cashbook.feature.tags.navigation.naviToMyTags
import cn.wj.android.cashbook.feature.types.navigation.EditRecordTypeListContent
import cn.wj.android.cashbook.feature.types.navigation.myCategoriesScreen
import cn.wj.android.cashbook.feature.types.navigation.naviToMyCategories
import io.noties.markwon.Markwon
import javax.crypto.Cipher

/** 开始默认显示路径 */
private const val START_DESTINATION = ROUTE_SETTINGS_LAUNCHER

/** 应用入口 */
@Composable
fun MainApp(
    viewModel: MainAppViewModel = viewModel(),
) {
    // 监听生命周期
    rememberLifecycleObserver {
        onCreate {
            // 在界面创建时自动检查更新
            viewModel.checkUpdateAuto()
        }

        onStop {
            // 在界面 onStop 时刷新安全认证状态
            viewModel.refreshVerifyState()
        }
    }

    CashbookGradientBackground {
        val navController = rememberNavController()
        val snackbarHostState = rememberSnackbarHostState()

        val onShowSnackbar: suspend (String, String?) -> SnackbarResult = { message, action ->
            snackbarHostState.showSnackbar(
                message = message,
                actionLabel = action,
            )
        }

        CompositionLocalProvider(LocalNavController provides navController) {
            CbScaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(snackbarHostState) },
            ) { paddingValues ->

                // 提示文本
                val passwordDecodeFailedText = stringResource(id = R.string.password_decode_failed)
                val passwordWrongText = stringResource(id = R.string.password_wrong)
                val isLatestVersionText = stringResource(id = R.string.it_is_the_latest_version)
                val downloadingText = stringResource(id = R.string.update_downloading_hint)
                val startDownloadText = stringResource(id = R.string.start_background_download)
                val downloadFailedText = stringResource(id = R.string.download_failed)
                val installFailedText = stringResource(id = R.string.install_failed)
                // 显示及隐藏提示
                LaunchedEffect(viewModel.shouldDisplayBookmark) {
                    if (viewModel.shouldDisplayBookmark != MainAppBookmarkEnum.NONE) {
                        val tipText = when (viewModel.shouldDisplayBookmark) {
                            MainAppBookmarkEnum.PASSWORD_DECODE_FAILED -> passwordDecodeFailedText
                            MainAppBookmarkEnum.PASSWORD_WRONG -> passwordWrongText
                            MainAppBookmarkEnum.ERROR -> viewModel.errorText
                            MainAppBookmarkEnum.NO_NEED_UPDATE -> isLatestVersionText
                            MainAppBookmarkEnum.UPDATE_DOWNLOADING -> downloadingText
                            MainAppBookmarkEnum.START_DOWNLOAD -> startDownloadText
                            MainAppBookmarkEnum.DOWNLOAD_FAILED -> downloadFailedText
                            MainAppBookmarkEnum.INSTALL_FAILED -> installFailedText
                            else -> ""
                        }
                        if (tipText.isNotBlank()) {
                            val showSnackbarResult = onShowSnackbar(tipText, null)
                            if (SnackbarResult.Dismissed == showSnackbarResult) {
                                viewModel.dismissBookmark()
                            }
                        } else {
                            viewModel.dismissBookmark()
                        }
                    }
                }

                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val shouldDisplayUpdateDialog by viewModel.updateInfoData.collectAsStateWithLifecycle()
                val shouldDisplayNoWifiUpdateDialog by viewModel.confirmUpdateInfoData.collectAsStateWithLifecycle()
                val ignoreUpdateVersion by viewModel.ignoreUpdateVersionData.collectAsStateWithLifecycle()

                val context = LocalContext.current

                MainAppScreen(
                    uiState = uiState,
                    onAgreeProtocolClick = viewModel::agreeProtocol,
                    firstOpen = viewModel.firstOpen,
                    verifyState = viewModel.verifyState,
                    dialogState = viewModel.dialogState,
                    onRequestVerifyPassword = viewModel::onVerityConfirm,
                    onFingerprintClick = viewModel::showFingerprintVerify,
                    onFingerprintVerifySuccess = viewModel::onFingerprintVerifySuccess,
                    onFingerprintVerifyError = viewModel::onFingerprintVerifyError,
                    onPrivacyPolicyClick = {
                        MarkdownActivity.actionStart(
                            context = context,
                            type = MarkdownTypeEnum.PRIVACY_POLICY,
                        )
                    },
                    ignoreUpdateVersion = ignoreUpdateVersion,
                    shouldDisplayUpdateDialog = shouldDisplayUpdateDialog,
                    onConfirmUpdateClick = viewModel::confirmUpdate,
                    onRequestDismissUpdateDialog = viewModel::dismissUpdateDialog,
                    shouldDisplayNoWifiUpdateDialog = shouldDisplayNoWifiUpdateDialog,
                    onConfirmDownloadClick = viewModel::confirmDownload,
                    onRequestDismissNoWifiUpdateDialog = viewModel::dismissNoWifiUpdateDialog,
                    navController = navController,
                    onShowSnackbar = onShowSnackbar,
                    modifier = Modifier.padding(paddingValues),
                )
            }
        }
    }
}

@Composable
private fun MainAppScreen(
    uiState: MainAppUiState,
    onAgreeProtocolClick: () -> Unit,
    firstOpen: Boolean,
    verifyState: SettingPasswordStateEnum,
    dialogState: DialogState,
    onRequestVerifyPassword: (String) -> Unit,
    onFingerprintClick: () -> Unit,
    onFingerprintVerifySuccess: (Cipher) -> Unit,
    onFingerprintVerifyError: (Int, String) -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    ignoreUpdateVersion: Boolean,
    shouldDisplayUpdateDialog: UpgradeInfoEntity?,
    onConfirmUpdateClick: () -> Unit,
    onRequestDismissUpdateDialog: (Boolean) -> Unit,
    shouldDisplayNoWifiUpdateDialog: UpgradeInfoEntity?,
    onConfirmDownloadClick: (Boolean) -> Unit,
    onRequestDismissNoWifiUpdateDialog: () -> Unit,
    navController: NavHostController,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // 全局进度弹窗
        ProgressDialog()

        // 显示 UI
        when (uiState) {
            MainAppUiState.Loading -> {
                Loading(modifier = Modifier.align(Alignment.Center))
            }

            is MainAppUiState.Success -> {
                with(uiState) {
                    if (needRequestProtocol) {
                        // 显示用户隐私协议
                        val currentActivity = LocalContext.current as? Activity
                        CbAlertDialog(
                            onDismissRequest = { currentActivity?.finish() },
                            title = { Text(text = stringResource(id = R.string.user_agreement_and_privacy_policy)) },
                            text = {
                                val tag = "TAG_URL"
                                val annotatedString = buildAnnotatedString {
                                    append(stringResource(id = R.string.user_agreement_and_privacy_policy_hint_start))
                                    pushStringAnnotation(tag, PRIVACY_POLICY_FILE_PATH)
                                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                        append(stringResource(id = R.string.user_agreement_and_privacy_policy_with_chevron))
                                    }
                                    pop()
                                    append(stringResource(id = R.string.user_agreement_and_privacy_policy_hint_end))
                                }
                                ClickableText(
                                    text = annotatedString,
                                    onClick = { offset ->
                                        val annotations =
                                            annotatedString.getStringAnnotations(
                                                tag,
                                                offset,
                                                offset,
                                            )
                                        annotations.firstOrNull()?.let {
                                            if (it.item == PRIVACY_POLICY_FILE_PATH) {
                                                onPrivacyPolicyClick()
                                            }
                                        }
                                    },
                                )
                            },
                            confirmButton = {
                                CbTextButton(
                                    onClick = onAgreeProtocolClick,
                                    modifier = Modifier.testTag(TestTag.Launcher.LAUNCHER_PROTOCOL_CONFIRM),
                                ) {
                                    Text(text = stringResource(id = R.string.confirm))
                                }
                            },
                            dismissButton = {
                                CbTextButton(onClick = { currentActivity?.finish() }) {
                                    Text(text = stringResource(id = R.string.cancel))
                                }
                            },
                        )
                    }
                    // 升级提示弹窗
                    if (null != shouldDisplayUpdateDialog) {
                        val content = Markwon.create(LocalContext.current)
                            .toMarkdown(shouldDisplayUpdateDialog.displayVersionInfo)
                        UpdateHintDialog(
                            content = content,
                            ignoreUpdateVersion = ignoreUpdateVersion,
                            onConfirmClick = onConfirmUpdateClick,
                            onDismissClick = onRequestDismissUpdateDialog,
                        )
                    }
                    // 非WiFi下载提示弹窗
                    if (null != shouldDisplayNoWifiUpdateDialog) {
                        NoWifiUpdateHintDialog(
                            onConfirmClick = onConfirmDownloadClick,
                            onDismissClick = onRequestDismissNoWifiUpdateDialog,
                        )
                    }
                    if (needVerity) {
                        // 显示安全认证
                        Verification(
                            firstOpen = firstOpen,
                            supportFingerprint = supportFingerprint,
                            verifyState = verifyState,
                            dialogState = dialogState,
                            onRequestVerifyPassword = onRequestVerifyPassword,
                            onFingerprintClick = onFingerprintClick,
                            onFingerprintVerifySuccess = onFingerprintVerifySuccess,
                            onFingerprintVerifyError = onFingerprintVerifyError,
                        )
                    } else {
                        // 显示实际内容
                        CashbookNavHost(
                            navController = navController,
                            onShowSnackbar = onShowSnackbar,
                        )
                    }
                }
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
    val context = LocalContext.current
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
                    onRequestNaviToSearch = navController::naviToSearch,
                    onRequestNaviToCalendar = navController::naviToCalendar,
                    onRequestNaviToAnalytics = navController::naviToAnalytics,
                    onShowSnackbar = onShowSnackbar,
                )
            },
        )
        // 关于我们
        aboutUsScreen(
            onRequestNaviToChangelog = {
                // 版本信息
                MarkdownActivity.actionStart(context, MarkdownTypeEnum.CHANGELOG)
            },
            onRequestNaviToPrivacyPolicy = {
                // 用户协议及隐私政策
                MarkdownActivity.actionStart(context, MarkdownTypeEnum.PRIVACY_POLICY)
            },
            onRequestPopBackStack = navController::popBackStackSafety,
        )
        // 设置
        settingScreen(
            onRequestNaviToBackupAndRecovery = navController::naviToBackupAndRecovery,
            onRequestPopBackStack = navController::popBackStackSafety,
            onShowSnackbar = onShowSnackbar,
        )
        // 备份与恢复
        backupAndRecoveryScreen(
            onRequestPopBackStack = navController::popBackStackSafety,
            onShowSnackbar = onShowSnackbar,
        )

        // 我的标签
        myTagsScreen(
            onRequestNaviToTagStatistic = { navController.naviToTypedAnalytics(tagId = it) },
            onRequestPopBackStack = navController::popBackStackSafety,
        )

        // 编辑记录
        editRecordScreen(
            typeListContent = { typeCategory, defaultTypeId, onTypeSelect ->
                EditRecordTypeListContent(
                    typeCategory = typeCategory,
                    defaultTypeId = defaultTypeId,
                    onTypeSelect = onTypeSelect,
                    onRequestNaviToTypeManager = navController::naviToMyCategories,
                )
            },
            assetBottomSheetContent = { currentTypeId, selectedAssetId, isRelated, onAssetChange ->
                EditRecordSelectAssetBottomSheetContent(
                    currentTypeId = currentTypeId,
                    selectedAssetId = selectedAssetId,
                    isRelated = isRelated,
                    onAssetChange = onAssetChange,
                    onRequestNaviToEditAsset = navController::naviToEditAsset,
                )
            },
            tagBottomSheetContent = { selectedTagIdList, onTagIdListChange, onRequestDismissSheet ->
                EditRecordSelectTagBottomSheetContent(
                    selectedTagIdList = selectedTagIdList,
                    onTagIdListChange = onTagIdListChange,
                    onRequestDismissSheet = onRequestDismissSheet,
                )
            },
            onRequestNaviToSelectRelatedRecord = navController::naviToSelectRelatedRecord,
            onRequestPopBackStack = navController::popBackStackSafety,
        )
        // 选择关联记录
        selectRelatedRecordScreen(
            onRequestPopBackStack = navController::popBackStackSafety,
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
            onRequestPopBackStack = navController::popBackStackSafety,
            onShowSnackbar = onShowSnackbar,
        )
        // 搜索记录
        searchScreen(
            onRequestNaviToEditRecord = navController::naviToEditRecord,
            onRequestPopBackStack = navController::popBackStackSafety,
        )
        // 数据分析
        analyticsScreen(
            onRequestNaviToTypeAnalytics = { navController.naviToTypedAnalytics(typeId = it) },
            onRequestPopBackStack = navController::popBackStackSafety,
        )
        // 分类数据分析
        typedAnalyticsScreen(
            onRequestNaviToEditRecord = navController::naviToEditRecord,
            onRequestPopBackStack = navController::popBackStackSafety,
        )

        // 我的资产
        myAssetScreen(
            onRequestNaviToAssetInfo = navController::naviToAssetInfo,
            onRequestNaviToAddAsset = navController::naviToEditAsset,
            onRequestNaviToInvisibleAsset = navController::naviToInvisibleAsset,
            onRequestPopBackStack = navController::popBackStackSafety,
        )
        // 不可见资产
        invisibleAssetScreen(
            onRequestNaviToAssetInfo = navController::naviToAssetInfo,
            onRequestPopBackStack = navController::popBackStackSafety,
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
            onRequestNaviToAddRecord = { assetId -> navController.naviToEditRecord(assetId = assetId) },
            onRequestPopBackStack = navController::popBackStackSafety,
        )
        // 编辑资产
        editAssetScreen(
            onRequestPopBackStack = navController::popBackStackSafety,
        )

        // 我的账本
        myBooksScreen(
            onRequestNaviToEditBook = navController::naviToEditBook,
            onRequestPopBackStack = navController::popBackStackSafety,
        )
        // 编辑账本
        editBookScreen(
            onRequestPopBackStack = navController::popBackStackSafety,
        )

        // 我的分类
        myCategoriesScreen(
            onRequestNaviToTypeStatistics = { navController.naviToTypedAnalytics(typeId = it) },
            onRequestPopBackStack = navController::popBackStackSafety,
        )
    }
}

/**
 * 安全认证界面
 *
 * @param firstOpen 是否是第一打开，用于支持指纹识别时自动拉起指纹认证
 * @param supportFingerprint 是否支持指纹识别
 * @param dialogState 弹窗状态
 * @param onRequestVerifyPassword 安全认证确认点击回调，参数：(密码, 认证回调(认证结果[SettingPasswordStateEnum] -> [Unit])) -> [Unit]
 * @param onFingerprintClick 指纹识别点击回调
 * @param onFingerprintVerifySuccess 指纹认证成功回调，参数：(数据解码对象) -> [Unit]
 * @param onFingerprintVerifyError 指纹认证失败回调，参数：(错误码, 提示文本) -> [Unit]
 */
@Composable
internal fun Verification(
    firstOpen: Boolean,
    supportFingerprint: Boolean,
    verifyState: SettingPasswordStateEnum,
    dialogState: DialogState,
    onRequestVerifyPassword: (String) -> Unit,
    onFingerprintClick: () -> Unit,
    onFingerprintVerifySuccess: (Cipher) -> Unit,
    onFingerprintVerifyError: (Int, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        (dialogState as? DialogState.Shown<*>)?.let {
            if (it.data is Cipher) {
                val data = it.data as Cipher
                ProvideBiometricAuthenticateHintData(
                    hintData = BiometricAuthenticateHintData(
                        unSupportHint = stringResource(id = R.string.device_not_support_fingerprint),
                        noFingerprintHint = stringResource(id = R.string.please_add_at_least_one_fingerprint),
                        noDeviceCredentialHint = stringResource(id = R.string.please_set_device_credential_first),
                        cancelHint = stringResource(id = R.string.cancel),
                        userCancelHint = stringResource(id = R.string.user_cancel),
                        verificationFailedHint = stringResource(id = R.string.fingerprint_verification_failed),
                    ),
                ) {
                    BiometricAuthenticate(
                        title = stringResource(id = R.string.verity_fingerprint),
                        subTitle = stringResource(id = R.string.verity_fingerprint_to_use),
                        hint = stringResource(id = R.string.press_sensing_to_verity_fingerprint),
                        cryptoCipher = data,
                        onSuccess = onFingerprintVerifySuccess,
                        onError = onFingerprintVerifyError,
                    )
                }
            }
        }

        Text(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp),
            text = stringResource(id = R.string.launch_verity_hint),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 提示文本
                val passwordMustNotBeBlankText =
                    stringResource(id = R.string.password_must_not_be_blank)
                val passwordWrongText = stringResource(id = R.string.password_wrong)
                val passwordFormatErrorText =
                    stringResource(id = R.string.password_format_error)
                val passwordDecodeFailedText =
                    stringResource(id = R.string.password_decode_failed)

                val pwdTextFieldState = remember {
                    TextFieldState(
                        validator = {
                            it.isNotBlank() && it.isMatch(PASSWORD_REGEX)
                        },
                        errorFor = {
                            when {
                                it.isBlank() -> passwordMustNotBeBlankText
                                !it.isMatch(PASSWORD_REGEX) -> passwordFormatErrorText
                                verifyState == SettingPasswordStateEnum.PASSWORD_WRONG -> passwordWrongText
                                verifyState == SettingPasswordStateEnum.PASSWORD_DECODE_FAILED -> passwordDecodeFailedText
                                else -> ""
                            }
                        },
                    )
                }

                CbPasswordTextField(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .weight(1f),
                    textFieldState = pwdTextFieldState,
                    label = { Text(text = stringResource(id = R.string.please_enter_password)) },
                )

                CbTextButton(
                    onClick = {
                        if (pwdTextFieldState.isValid) {
                            onRequestVerifyPassword(pwdTextFieldState.text)
                        }
                    },
                    content = {
                        Text(text = stringResource(id = R.string.confirm))
                    },
                )
            }
            if (supportFingerprint) {
                CbIconButton(onClick = { onFingerprintClick() }) {
                    Icon(
                        imageVector = CbIcons.Fingerprint,
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = null,
                    )
                }
                if (firstOpen) {
                    onFingerprintClick()
                }
            }
        }
    }
}

/**
 * 更新提示弹窗
 *
 * @param content 更新内容
 * @param ignoreUpdateVersion 是否跳过此版本
 * @param onConfirmClick 确认点击回调
 * @param onDismissClick 取消点击回调，参数：(是否跳过此版本) -> [Unit]
 */
@Composable
internal fun UpdateHintDialog(
    content: Spanned,
    ignoreUpdateVersion: Boolean,
    onConfirmClick: () -> Unit,
    onDismissClick: (Boolean) -> Unit,
) {
    var ignore by remember {
        mutableStateOf(ignoreUpdateVersion)
    }
    CbAlertDialog(
        onDismissRequest = { onDismissClick.invoke(false) },
        text = {
            Column {
                Text(
                    text = buildAnnotatedString {
                        append(content)
                    },
                )
                Row(
                    modifier = Modifier
                        .clickable { ignore = !ignore }
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = ignore, onCheckedChange = { ignore = it })
                    Text(text = stringResource(id = R.string.ignore_this_version))
                }
            }
        },
        confirmButton = {
            CbTextButton(onClick = onConfirmClick) {
                Text(text = stringResource(id = R.string.update))
            }
        },
        dismissButton = {
            CbTextButton(onClick = { onDismissClick.invoke(ignore) }) {
                Text(text = stringResource(id = R.string.cancel))
            }
        },
    )
}

/**
 * 未连接 WiFi 更新提示弹窗
 *
 * @param onConfirmClick 确认点击回调，参数：(是否不再提示) -> [Unit]
 * @param onDismissClick 取消点击回调
 */
@Composable
internal fun NoWifiUpdateHintDialog(
    onConfirmClick: (Boolean) -> Unit,
    onDismissClick: () -> Unit,
) {
    var noMOrePrompt by remember {
        mutableStateOf(false)
    }
    CbAlertDialog(
        onDismissRequest = onDismissClick,
        text = {
            Column {
                Text(
                    text = stringResource(id = R.string.no_wifi_download_available_hint),
                )
                Row(
                    modifier = Modifier
                        .clickable { noMOrePrompt = !noMOrePrompt }
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = noMOrePrompt, onCheckedChange = { noMOrePrompt = it })
                    Text(text = stringResource(id = R.string.no_more_prompt))
                }
            }
        },
        confirmButton = {
            CbTextButton(onClick = { onConfirmClick.invoke(noMOrePrompt) }) {
                Text(text = stringResource(id = R.string.update))
            }
        },
        dismissButton = {
            CbTextButton(onClick = onDismissClick) {
                Text(text = stringResource(id = R.string.cancel))
            }
        },
    )
}
