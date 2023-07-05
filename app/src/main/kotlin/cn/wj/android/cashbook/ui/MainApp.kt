package cn.wj.android.cashbook.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import cn.wj.android.cashbook.core.common.PASSWORD_REGEX
import cn.wj.android.cashbook.core.common.tools.isMatch
import cn.wj.android.cashbook.core.design.component.CashbookGradientBackground
import cn.wj.android.cashbook.core.design.component.CashbookScaffold
import cn.wj.android.cashbook.core.design.component.PasswordTextField
import cn.wj.android.cashbook.core.design.security.biometric.BiometricAuthenticate
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.core.ui.LocalNavController
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.enums.MainBookmarkEnum
import cn.wj.android.cashbook.feature.assets.navigation.EditRecordSelectAssetBottomSheetContent
import cn.wj.android.cashbook.feature.assets.navigation.assetInfoScreen
import cn.wj.android.cashbook.feature.assets.navigation.editAssetScreen
import cn.wj.android.cashbook.feature.assets.navigation.myAssetScreen
import cn.wj.android.cashbook.feature.assets.navigation.naviToAssetInfo
import cn.wj.android.cashbook.feature.assets.navigation.naviToEditAsset
import cn.wj.android.cashbook.feature.assets.navigation.naviToMyAsset
import cn.wj.android.cashbook.feature.records.navigation.AssetInfoContent
import cn.wj.android.cashbook.feature.records.navigation.ConfirmDeleteRecordDialogContent
import cn.wj.android.cashbook.feature.records.navigation.LauncherContent
import cn.wj.android.cashbook.feature.records.navigation.RecordDetailSheetContent
import cn.wj.android.cashbook.feature.records.navigation.editRecordScreen
import cn.wj.android.cashbook.feature.records.navigation.naviToEditRecord
import cn.wj.android.cashbook.feature.records.navigation.selectRelatedRecordScreen
import cn.wj.android.cashbook.feature.settings.enums.SettingPasswordStateEnum
import cn.wj.android.cashbook.feature.settings.navigation.ROUTE_SETTINGS_LAUNCHER
import cn.wj.android.cashbook.feature.settings.navigation.aboutUsScreen
import cn.wj.android.cashbook.feature.settings.navigation.naviToAboutUs
import cn.wj.android.cashbook.feature.settings.navigation.naviToSetting
import cn.wj.android.cashbook.feature.settings.navigation.settingScreen
import cn.wj.android.cashbook.feature.settings.navigation.settingsLauncherScreen
import cn.wj.android.cashbook.feature.tags.navigation.EditRecordSelectTagBottomSheetContent
import cn.wj.android.cashbook.feature.tags.navigation.myTagsScreen
import cn.wj.android.cashbook.feature.tags.navigation.naviToMyTags
import cn.wj.android.cashbook.feature.types.navigation.EditRecordTypeListContent
import javax.crypto.Cipher

/** 开始默认显示路径 */
private const val START_DESTINATION = ROUTE_SETTINGS_LAUNCHER

/** 应用入口 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainApp(
    viewModel: VerifyViewModel = viewModel()
) {

    val needVerity by viewModel.needVerity.collectAsStateWithLifecycle()
    val supportFingerprint by viewModel.supportFingerprint.collectAsStateWithLifecycle()
    val shouldDisplayBookmark = viewModel.shouldDisplayBookmark

    CashbookGradientBackground {
        val navController = rememberNavController()
        val snackbarHostState = remember { SnackbarHostState() }

        // 提示文本
        val passwordDecodeFailedText = stringResource(id = R.string.password_decode_failed)
        val passwordWrongText = stringResource(id = R.string.password_wrong)

        LaunchedEffect(shouldDisplayBookmark) {
            if (shouldDisplayBookmark != MainBookmarkEnum.NONE) {
                val tipText = when (shouldDisplayBookmark) {
                    MainBookmarkEnum.PASSWORD_DECODE_FAILED -> passwordDecodeFailedText
                    MainBookmarkEnum.PASSWORD_WRONG -> passwordWrongText
                    MainBookmarkEnum.ERROR -> viewModel.errorText
                    else -> ""
                }
                val showSnackbarResult = snackbarHostState.showSnackbar(tipText)
                if (SnackbarResult.Dismissed == showSnackbarResult) {
                    viewModel.onBookmarkDismiss()
                }
            }
        }

        CompositionLocalProvider(LocalNavController provides navController) {
            CashbookScaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(snackbarHostState) },
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .consumeWindowInsets(paddingValues),
                ) {
                    if (needVerity) {
                        Verification(
                            firstOpen = viewModel.firstOpen,
                            supportFingerprint = supportFingerprint,
                            dialogState = viewModel.dialogState,
                            onConfirmClick = viewModel::onVerityConfirm,
                            onFingerprintClick = viewModel::onFingerprintClick,
                            onFingerprintVerifySuccess = viewModel::onFingerprintVerifySuccess,
                            onFingerprintVerifyError = viewModel::onFingerprintVerifyError,
                        )
                    } else {
                        CashbookNavHost(
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
    }
}

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
                IconButton(onClick = { onFingerprintClick.invoke() }) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = null,
                    )
                }
                if (firstOpen) {
                    onFingerprintClick.invoke()
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
    // FIXME 使用 AnimatedNavHost 从二级界面返回时快速点击左上角菜单会导致 Navigation 不显示，后续添加界面动画时需修复此问题
    NavHost(
        navController = navController,
        startDestination = START_DESTINATION,
        modifier = modifier,
    ) {

        // 启动页
        settingsLauncherScreen(
            onMyAssetClick = navController::naviToMyAsset,
            onMyBookClick = {/* TODO */ },
            onMyCategoryClick = {/* TODO */ },
            onMyTagClick = navController::naviToMyTags,
            onSettingClick = navController::naviToSetting,
            onAboutUsClick = navController::naviToAboutUs,
            content = { openDrawer ->
                LauncherContent(
                    onEditRecordClick = navController::naviToEditRecord,
                    onMenuClick = openDrawer,
                    onSearchClick = { /* TODO */ },
                    onCalendarClick = { /* TODO */ },
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
                // TODO 版本信息
            },
            onUserAgreementAndPrivacyPolicyClick = {
                // TODO 用户协议及隐私政策
            },
        )
        // 设置
        settingScreen(
            onBackClick = navController::popBackStack,
            onBackupAndRecoveryClick = {/*TODO*/ },
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
    }
}