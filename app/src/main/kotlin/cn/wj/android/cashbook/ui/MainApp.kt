package cn.wj.android.cashbook.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import cn.wj.android.cashbook.core.common.tools.isMatch
import cn.wj.android.cashbook.core.design.component.CashbookGradientBackground
import cn.wj.android.cashbook.core.design.component.CashbookScaffold
import cn.wj.android.cashbook.core.design.component.Loading
import cn.wj.android.cashbook.core.design.component.PasswordTextField
import cn.wj.android.cashbook.core.design.component.rememberLifecycleObserver
import cn.wj.android.cashbook.core.design.icon.CashbookIcons
import cn.wj.android.cashbook.core.design.security.biometric.BiometricAuthenticate
import cn.wj.android.cashbook.core.design.security.biometric.BiometricAuthenticateHintData
import cn.wj.android.cashbook.core.design.security.biometric.ProvideBiometricAuthenticateHintData
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
import javax.crypto.Cipher

/** 开始默认显示路径 */
private const val START_DESTINATION = ROUTE_SETTINGS_LAUNCHER

/** 应用入口 */
@Composable
fun MainApp(
    viewModel: MainAppViewModel = viewModel(),
) {

    CashbookGradientBackground {
        val navController = rememberNavController()
        val snackbarHostState = remember { SnackbarHostState() }

        val onShowSnackbar: suspend (String, String?) -> SnackbarResult = { message, action ->
            snackbarHostState.showSnackbar(
                message = message,
                actionLabel = action,
            )
        }

        CompositionLocalProvider(LocalNavController provides navController) {
            CashbookScaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(snackbarHostState) },
            ) { paddingValues ->

                // 监听生命周期，在界面 onStop 时刷新安全认证状态
                rememberLifecycleObserver {
                    onStop {
                        viewModel.refreshVerifyState()
                    }
                }

                // 提示文本
                val passwordDecodeFailedText = stringResource(id = R.string.password_decode_failed)
                val passwordWrongText = stringResource(id = R.string.password_wrong)
                // 显示及隐藏提示
                LaunchedEffect(viewModel.shouldDisplayBookmark) {
                    if (viewModel.shouldDisplayBookmark != MainAppBookmarkEnum.NONE) {
                        val tipText = when (viewModel.shouldDisplayBookmark) {
                            MainAppBookmarkEnum.PASSWORD_DECODE_FAILED -> passwordDecodeFailedText
                            MainAppBookmarkEnum.PASSWORD_WRONG -> passwordWrongText
                            MainAppBookmarkEnum.ERROR -> viewModel.errorText
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

                val context = LocalContext.current

                MainAppScreen(
                    uiState = uiState,
                    onAgreeProtocolClick = viewModel::agreeProtocol,
                    firstOpen = viewModel.firstOpen,
                    dialogState = viewModel.dialogState,
                    onVerifyConfirmClick = viewModel::onVerityConfirm,
                    onFingerprintClick = viewModel::showFingerprintVerify,
                    onFingerprintVerifySuccess = viewModel::onFingerprintVerifySuccess,
                    onFingerprintVerifyError = viewModel::onFingerprintVerifyError,
                    onPrivacyPolicyClick = {
                        MarkdownActivity.actionStart(
                            context,
                            MarkdownTypeEnum.PRIVACY_POLICY
                        )
                    },
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
    dialogState: DialogState,
    onVerifyConfirmClick: (String, (SettingPasswordStateEnum) -> Unit) -> Unit,
    onFingerprintClick: () -> Unit,
    onFingerprintVerifySuccess: (Cipher) -> Unit,
    onFingerprintVerifyError: (Int, String) -> Unit,
    onPrivacyPolicyClick: () -> Unit,
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
                        AlertDialog(
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
                                                offset
                                            )
                                        annotations.firstOrNull()?.let {
                                            if (it.item == PRIVACY_POLICY_FILE_PATH) {
                                                onPrivacyPolicyClick()
                                            }
                                        }
                                    }
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = onAgreeProtocolClick) {
                                    Text(text = stringResource(id = R.string.confirm))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { currentActivity?.finish() }) {
                                    Text(text = stringResource(id = R.string.cancel))
                                }
                            },
                        )
                    }
                    if (needVerity) {
                        // 显示安全认证
                        Verification(
                            firstOpen = firstOpen,
                            supportFingerprint = supportFingerprint,
                            dialogState = dialogState,
                            onConfirmClick = onVerifyConfirmClick,
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
            onShowSnackbar = onShowSnackbar,
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
            tagBottomSheetContent = { selectedTagIdList, onTagIdListChange ->
                EditRecordSelectTagBottomSheetContent(
                    selectedTagIdList = selectedTagIdList,
                    onTagIdListChange = onTagIdListChange,
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
 * @param onConfirmClick 安全认证确认点击回调，参数：(密码, 认证回调(认证结果[SettingPasswordStateEnum] -> [Unit])) -> [Unit]
 * @param onFingerprintClick 指纹识别点击回调
 * @param onFingerprintVerifySuccess 指纹认证成功回调，参数：(数据解码对象) -> [Unit]
 * @param onFingerprintVerifyError 指纹认证失败回调，参数：(错误码, 提示文本) -> [Unit]
 */
@Composable
internal fun Verification(
    firstOpen: Boolean,
    supportFingerprint: Boolean,
    dialogState: DialogState,
    onConfirmClick: (String, (SettingPasswordStateEnum) -> Unit) -> Unit,
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
                    )
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

                var pwd by remember {
                    mutableStateOf("")
                }
                var pwdError by remember {
                    mutableStateOf(false)
                }
                var pwdSupportText by remember {
                    mutableStateOf("")
                }

                PasswordTextField(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .weight(1f),
                    initializedText = pwd,
                    label = stringResource(id = R.string.please_enter_password),
                    isError = pwdError,
                    supportingText = pwdSupportText,
                    onValueChange = { pwd = it },
                )

                TextButton(
                    onClick = {
                        when {
                            pwd.isBlank() -> {
                                pwdError = true
                                pwdSupportText = passwordMustNotBeBlankText
                            }

                            !pwd.isMatch(PASSWORD_REGEX) -> {
                                pwdError = true
                                pwdSupportText = passwordFormatErrorText
                            }

                            else -> {
                                onConfirmClick.invoke(pwd) { result ->
                                    if (result == SettingPasswordStateEnum.PASSWORD_WRONG) {
                                        // 密码错误
                                        pwdError = true
                                        pwdSupportText = passwordWrongText
                                    } else if (result == SettingPasswordStateEnum.PASSWORD_DECODE_FAILED) {
                                        pwdError = true
                                        pwdSupportText = passwordDecodeFailedText
                                    }
                                }
                            }
                        }
                    },
                    content = {
                        Text(text = stringResource(id = R.string.confirm))
                    },
                )
            }
            if (supportFingerprint) {
                IconButton(onClick = { onFingerprintClick() }) {
                    Icon(
                        imageVector = CashbookIcons.Fingerprint,
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