package cn.wj.android.cashbook.feature.settings.screen

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.common.PASSWORD_REGEX
import cn.wj.android.cashbook.core.common.PRIVACY_POLICY_FILE_PATH
import cn.wj.android.cashbook.core.common.tools.isMatch
import cn.wj.android.cashbook.core.design.component.Loading
import cn.wj.android.cashbook.core.design.component.PasswordTextField
import cn.wj.android.cashbook.core.design.component.rememberLifecycleObserver
import cn.wj.android.cashbook.core.design.icon.CashbookIcons
import cn.wj.android.cashbook.core.design.security.biometric.BiometricAuthenticate
import cn.wj.android.cashbook.core.design.security.biometric.BiometricAuthenticateHintData
import cn.wj.android.cashbook.core.design.security.biometric.ProvideBiometricAuthenticateHintData
import cn.wj.android.cashbook.core.design.theme.PreviewTheme
import cn.wj.android.cashbook.core.ui.BackPressHandler
import cn.wj.android.cashbook.core.ui.DevicePreviews
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.settings.enums.LauncherBookmarkEnum
import cn.wj.android.cashbook.feature.settings.enums.SettingPasswordStateEnum
import cn.wj.android.cashbook.feature.settings.viewmodel.LauncherUiState
import cn.wj.android.cashbook.feature.settings.viewmodel.LauncherViewModel
import javax.crypto.Cipher

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
 * @param onRequestNaviToPrivacyPolicy 导航到用户隐私协议
 * @param onShowSnackbar 显示 [androidx.compose.material3.Snackbar]，参数：(显示文本，action文本) -> [SnackbarResult]
 * @param content 显示内容，参数 (打开抽屉) -> [Unit]
 */
@Composable
internal fun LauncherRoute(
    modifier: Modifier = Modifier,
    onRequestNaviToMyAsset: () -> Unit = {},
    onRequestNaviToMyBooks: () -> Unit = {},
    onRequestNaviToMyCategory: () -> Unit = {},
    onRequestNaviToMyTags: () -> Unit = {},
    onRequestNaviToSetting: () -> Unit = {},
    onRequestNaviToAboutUs: () -> Unit = {},
    onRequestNaviToPrivacyPolicy: () -> Unit = {},
    onShowSnackbar: suspend (String, String?) -> SnackbarResult = { _, _ -> SnackbarResult.Dismissed },
    viewModel: LauncherViewModel = hiltViewModel(),
    content: @Composable (() -> Unit) -> Unit,
) {

    // 界面 UI 状态数据
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LauncherScreen(
        shouldDisplayDrawerSheet = viewModel.shouldDisplayDrawerSheet,
        onRequestDisplayDrawerSheet = viewModel::displayDrawerSheet,
        onRequestDismissDrawerSheet = viewModel::dismissDrawerSheet,
        shouldDisplayBookmark = viewModel.shouldDisplayBookmark,
        errorText = viewModel.errorText,
        onRequestDismissBookmark = viewModel::dismissBookmark,
        uiState = uiState,
        onAgreeProtocolClick = viewModel::agreeProtocol,
        firstOpen = viewModel.firstOpen,
        dialogState = viewModel.dialogState,
        onVerifyConfirmClick = viewModel::onVerityConfirm,
        onFingerprintClick = viewModel::showFingerprintVerify,
        onFingerprintVerifySuccess = viewModel::onFingerprintVerifySuccess,
        onFingerprintVerifyError = viewModel::onFingerprintVerifyError,
        onRequestRefreshVerifyState = viewModel::refreshVerifyState,
        onMyAssetClick = {
            onRequestNaviToMyAsset.invoke()
            viewModel.dismissDrawerSheet()
        },
        onMyBookClick = {
            onRequestNaviToMyBooks.invoke()
            viewModel.dismissDrawerSheet()
        },
        onMyCategoryClick = {
            onRequestNaviToMyCategory.invoke()
            viewModel.dismissDrawerSheet()
        },
        onMyTagClick = {
            onRequestNaviToMyTags.invoke()
            viewModel.dismissDrawerSheet()
        },
        onSettingClick = {
            onRequestNaviToSetting.invoke()
            viewModel.dismissDrawerSheet()
        },
        onAboutUsClick = {
            onRequestNaviToAboutUs.invoke()
            viewModel.dismissDrawerSheet()
        },
        onPrivacyPolicyClick = onRequestNaviToPrivacyPolicy,
        onShowSnackbar = onShowSnackbar,
        content = { content { viewModel.displayDrawerSheet() } },
        modifier = modifier,
    )
}

/**
 * 首页显示
 * - 首页显示主体，提供左侧抽屉菜单、用户隐私协议弹窗、安全校验功能，具体内容显示通过 [content] 参数提供
 *
 * @param shouldDisplayDrawerSheet 是否显示抽屉菜单
 * @param onRequestDisplayDrawerSheet 显示抽屉菜单
 * @param onRequestDismissDrawerSheet 隐藏抽屉菜单
 * @param shouldDisplayBookmark 是否显示 [androidx.compose.material3.Snackbar]，取值 [LauncherBookmarkEnum]
 * @param errorText 错误提示文本
 * @param onRequestDismissBookmark 隐藏 [androidx.compose.material3.Snackbar]
 * @param uiState UI 显示数据
 * @param onAgreeProtocolClick 同意隐私协议点击
 * @param firstOpen 是否是第一打开，用于支持指纹识别时自动拉起指纹认证
 * @param dialogState 弹窗状态
 * @param onVerifyConfirmClick 安全认证确认点击回调，参数：(密码, 认证回调(认证结果[SettingPasswordStateEnum] -> [Unit])) -> [Unit]
 * @param onFingerprintClick 指纹识别点击回调
 * @param onFingerprintVerifySuccess 指纹认证成功回调，参数：(数据解码对象) -> [Unit]
 * @param onFingerprintVerifyError 指纹认证失败回调，参数：(错误码, 提示文本) -> [Unit]
 * @param onRequestRefreshVerifyState 刷新安全认证状态
 * @param onMyAssetClick 我的资产点击回调
 * @param onMyBookClick 我的账本点击回调
 * @param onMyCategoryClick 我的分类点击回调
 * @param onMyTagClick 我的标签点击回调
 * @param onSettingClick 设置点击回调
 * @param onAboutUsClick 关于我们点击回调
 * @param onPrivacyPolicyClick 用户隐私协议点击回调
 * @param onShowSnackbar 显示 [androidx.compose.material3.Snackbar]，参数：(显示文本，action文本) -> [SnackbarResult]
 * @param content 显示内容，参数 (打开抽屉) -> [Unit]
 * @param drawerState 抽屉状态，默认关闭，状态变化时在回调中更新数据状态
 */
@Composable
internal fun LauncherScreen(
    shouldDisplayDrawerSheet: Boolean,
    onRequestDisplayDrawerSheet: () -> Unit,
    onRequestDismissDrawerSheet: () -> Unit,
    shouldDisplayBookmark: LauncherBookmarkEnum,
    errorText: String,
    onRequestDismissBookmark: () -> Unit,
    uiState: LauncherUiState,
    onAgreeProtocolClick: () -> Unit,
    firstOpen: Boolean,
    dialogState: DialogState,
    onVerifyConfirmClick: (String, (SettingPasswordStateEnum) -> Unit) -> Unit,
    onFingerprintClick: () -> Unit,
    onFingerprintVerifySuccess: (Cipher) -> Unit,
    onFingerprintVerifyError: (Int, String) -> Unit,
    onRequestRefreshVerifyState: () -> Unit,
    onMyAssetClick: () -> Unit,
    onMyBookClick: () -> Unit,
    onMyCategoryClick: () -> Unit,
    onMyTagClick: () -> Unit,
    onSettingClick: () -> Unit,
    onAboutUsClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    drawerState: DrawerState = rememberDrawerState(
        initialValue = DrawerValue.Closed,
        confirmStateChange = {
            if (it == DrawerValue.Closed) {
                onRequestDismissDrawerSheet()
            } else {
                onRequestDisplayDrawerSheet()
            }
            true
        },
    ),
) {

    // 监听生命周期，在界面 onStop 时刷新安全认证状态
    rememberLifecycleObserver {
        onStop {
            onRequestRefreshVerifyState()
        }
    }

    // 提示文本
    val passwordDecodeFailedText = stringResource(id = R.string.password_decode_failed)
    val passwordWrongText = stringResource(id = R.string.password_wrong)
    // 显示及隐藏提示
    LaunchedEffect(shouldDisplayBookmark) {
        if (shouldDisplayBookmark != LauncherBookmarkEnum.NONE) {
            val tipText = when (shouldDisplayBookmark) {
                LauncherBookmarkEnum.PASSWORD_DECODE_FAILED -> passwordDecodeFailedText
                LauncherBookmarkEnum.PASSWORD_WRONG -> passwordWrongText
                LauncherBookmarkEnum.ERROR -> errorText
                else -> ""
            }
            if (tipText.isNotBlank()) {
                val showSnackbarResult = onShowSnackbar(tipText, null)
                if (SnackbarResult.Dismissed == showSnackbarResult) {
                    onRequestDismissBookmark()
                }
            } else {
                onRequestDismissBookmark()
            }
        }
    }

    // 控制抽屉菜单显示隐藏
    LaunchedEffect(shouldDisplayDrawerSheet) {
        if (shouldDisplayDrawerSheet) {
            drawerState.open()
        } else {
            drawerState.close()
        }
    }

    // 抽屉显示时，返回关闭抽屉
    if (drawerState.isOpen) {
        BackPressHandler {
            onRequestDismissDrawerSheet()
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        when (uiState) {
            LauncherUiState.Loading -> {
                Loading(modifier = Modifier.align(Alignment.Center))
            }

            is LauncherUiState.Success -> {
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
                    } else if (needVerity) {
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
                        ModalNavigationDrawer(
                            modifier = modifier,
                            drawerState = drawerState,
                            drawerContent = {
                                LauncherSheet(
                                    currentBookName = uiState.currentBookName,
                                    onMyAssetClick = onMyAssetClick,
                                    onMyBookClick = onMyBookClick,
                                    onMyCategoryClick = onMyCategoryClick,
                                    onMyTagClick = onMyTagClick,
                                    onSettingClick = onSettingClick,
                                    onAboutUsClick = onAboutUsClick,
                                )
                            },
                            content = content,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 首页抽屉菜单
 *
 * @param currentBookName 当前账本名
 * @param onMyAssetClick 我的资产点击回调
 * @param onMyBookClick 我的账本点击回调
 * @param onMyCategoryClick 我的分类点击回调
 * @param onMyTagClick 我的标签点击回调
 * @param onSettingClick 设置点击回调
 * @param onAboutUsClick 关于我们点击回调
 */
@Composable
internal fun LauncherSheet(
    currentBookName: String,
    onMyAssetClick: () -> Unit,
    onMyBookClick: () -> Unit,
    onMyCategoryClick: () -> Unit,
    onMyTagClick: () -> Unit,
    onSettingClick: () -> Unit,
    onAboutUsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalDrawerSheet(
        modifier = modifier,
    ) {
        Text(
            text = stringResource(id = R.string.sheet_title),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(24.dp)
        )
        NavigationDrawerItem(
            label = { Text(text = stringResource(id = R.string.my_books)) },
            icon = { Icon(imageVector = CashbookIcons.LibraryBooks, contentDescription = null) },
            badge = {
                Text(
                    text = currentBookName,
                    style = MaterialTheme.typography.labelMedium,
                    color = LocalContentColor.current.copy(alpha = 0.5f),
                )
            },
            selected = false,
            onClick = onMyBookClick,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
        NavigationDrawerItem(
            label = { Text(text = stringResource(id = R.string.my_assets)) },
            icon = { Icon(imageVector = CashbookIcons.WebAsset, contentDescription = null) },
            selected = false,
            onClick = onMyAssetClick,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
        NavigationDrawerItem(
            label = { Text(text = stringResource(id = R.string.my_categories)) },
            icon = { Icon(imageVector = CashbookIcons.Category, contentDescription = null) },
            selected = false,
            onClick = onMyCategoryClick,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
        NavigationDrawerItem(
            label = { Text(text = stringResource(id = R.string.my_tags)) },
            icon = { Icon(imageVector = CashbookIcons.Layers, contentDescription = null) },
            selected = false,
            onClick = onMyTagClick,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
        Divider(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .height(1.dp)
        )
        NavigationDrawerItem(
            label = { Text(text = stringResource(id = R.string.settings)) },
            icon = { Icon(imageVector = CashbookIcons.Settings, contentDescription = null) },
            selected = false,
            onClick = onSettingClick,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
        NavigationDrawerItem(
            label = { Text(text = stringResource(id = R.string.about_us)) },
            icon = { Icon(imageVector = CashbookIcons.Info, contentDescription = null) },
            selected = false,
            onClick = onAboutUsClick,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
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
                IconButton(onClick = { onFingerprintClick.invoke() }) {
                    Icon(
                        imageVector = CashbookIcons.Fingerprint,
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

@DevicePreviews
@Composable
private fun LauncherScreenPreview() {
    PreviewTheme {
        LauncherRoute(
            content = {},
        )
    }
}