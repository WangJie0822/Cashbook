package cn.wj.android.cashbook.feature.settings.screen

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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import cn.wj.android.cashbook.core.common.manager.AppManager
import cn.wj.android.cashbook.core.common.tools.isMatch
import cn.wj.android.cashbook.core.design.component.Loading
import cn.wj.android.cashbook.core.design.component.PasswordTextField
import cn.wj.android.cashbook.core.design.component.rememberLifecycleObserver
import cn.wj.android.cashbook.core.design.icon.CashbookIcons
import cn.wj.android.cashbook.core.design.security.biometric.BiometricAuthenticate
import cn.wj.android.cashbook.core.design.security.biometric.BiometricAuthenticateHintData
import cn.wj.android.cashbook.core.design.security.biometric.ProvideBiometricAuthenticateHintData
import cn.wj.android.cashbook.core.design.theme.PreviewTheme
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
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LauncherRoute(
    onMyAssetClick: () -> Unit,
    onMyBookClick: () -> Unit,
    onMyCategoryClick: () -> Unit,
    onMyTagClick: () -> Unit,
    onSettingClick: () -> Unit,
    onAboutUsClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,
    content: @Composable (() -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LauncherViewModel = hiltViewModel(),
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LauncherScreen(
        shouldDisplayDrawerSheet = viewModel.shouldDisplayDrawerSheet,
        shouldDisplayBookmark = viewModel.shouldDisplayBookmark,
        errorText = viewModel.errorText,
        dismissBookmark = viewModel::dismissBookmark,
        uiState = uiState,
        agreeProtocol = viewModel::agreeProtocol,
        firstOpen = viewModel.firstOpen,
        dialogState = viewModel.dialogState,
        onConfirmClick = viewModel::onVerityConfirm,
        onFingerprintClick = viewModel::onFingerprintClick,
        onFingerprintVerifySuccess = viewModel::onFingerprintVerifySuccess,
        onFingerprintVerifyError = viewModel::onFingerprintVerifyError,
        onActivityStop = viewModel::onActivityStop,
        onMyAssetClick = {
            onMyAssetClick.invoke()
            viewModel.dismissDrawerSheet()
        },
        onMyBookClick = {
            onMyBookClick.invoke()
            viewModel.dismissDrawerSheet()
        },
        onMyCategoryClick = {
            onMyCategoryClick.invoke()
            viewModel.dismissDrawerSheet()
        },
        onMyTagClick = {
            onMyTagClick.invoke()
            viewModel.dismissDrawerSheet()
        },
        onSettingClick = {
            onSettingClick.invoke()
            viewModel.dismissDrawerSheet()
        },
        onAboutUsClick = {
            onAboutUsClick.invoke()
            viewModel.dismissDrawerSheet()
        },
        onPrivacyPolicyClick = onPrivacyPolicyClick,
        onShowSnackbar = onShowSnackbar,
        content = { content { viewModel.displayDrawerSheet() } },
        modifier = modifier,
        drawerState = rememberDrawerState(
            initialValue = DrawerValue.Closed,
            confirmStateChange = {
                if (it == DrawerValue.Closed) {
                    viewModel.dismissDrawerSheet()
                }
                true
            },
        )
    )
}

/**
 * 首页显示
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LauncherScreen(
    shouldDisplayDrawerSheet: Boolean,
    shouldDisplayBookmark: LauncherBookmarkEnum,
    errorText: String,
    dismissBookmark: () -> Unit,
    uiState: LauncherUiState,
    agreeProtocol: () -> Unit,
    firstOpen: Boolean,
    dialogState: DialogState,
    onConfirmClick: (String, (SettingPasswordStateEnum) -> Unit) -> Unit,
    onFingerprintClick: () -> Unit,
    onFingerprintVerifySuccess: (Cipher) -> Unit,
    onFingerprintVerifyError: (Int, String) -> Unit,
    onActivityStop: () -> Unit,
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
    drawerState: DrawerState = rememberDrawerState(initialValue = DrawerValue.Closed),
) {

    rememberLifecycleObserver {
        onStop {
            onActivityStop()
        }
    }

    // 提示文本
    val passwordDecodeFailedText = stringResource(id = R.string.password_decode_failed)
    val passwordWrongText = stringResource(id = R.string.password_wrong)

    LaunchedEffect(shouldDisplayBookmark) {
        if (shouldDisplayBookmark != LauncherBookmarkEnum.NONE) {
            val tipText = when (shouldDisplayBookmark) {
                LauncherBookmarkEnum.PASSWORD_DECODE_FAILED -> passwordDecodeFailedText
                LauncherBookmarkEnum.PASSWORD_WRONG -> passwordWrongText
                LauncherBookmarkEnum.ERROR -> errorText
                else -> ""
            }
            val showSnackbarResult = onShowSnackbar(tipText, null)
            if (SnackbarResult.Dismissed == showSnackbarResult) {
                dismissBookmark()
            }
        }
    }

    // 抽屉菜单显示状态
    LaunchedEffect(shouldDisplayDrawerSheet) {
        if (shouldDisplayDrawerSheet) {
            drawerState.open()
        } else {
            drawerState.close()
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
                        AlertDialog(
                            onDismissRequest = { AppManager.finishAllActivity() },
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
                                TextButton(onClick = agreeProtocol) {
                                    Text(text = stringResource(id = R.string.confirm))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { AppManager.finishAllActivity() }) {
                                    Text(text = stringResource(id = R.string.cancel))
                                }
                            },
                        )
                    } else if (needVerity) {
                        Verification(
                            firstOpen = firstOpen,
                            supportFingerprint = supportFingerprint,
                            dialogState = dialogState,
                            onConfirmClick = onConfirmClick,
                            onFingerprintClick = onFingerprintClick,
                            onFingerprintVerifySuccess = onFingerprintVerifySuccess,
                            onFingerprintVerifyError = onFingerprintVerifyError,
                        )
                    } else {
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
 */
@OptIn(ExperimentalMaterial3Api::class)
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

@OptIn(ExperimentalMaterial3Api::class)
@DevicePreviews
@Composable
private fun LauncherScreenPreview() {
    PreviewTheme {
        LauncherScreen(
            shouldDisplayDrawerSheet = false,
            shouldDisplayBookmark = LauncherBookmarkEnum.NONE,
            errorText = "",
            dismissBookmark = {},
            uiState = LauncherUiState.Success(
                needRequestProtocol = false,
                needVerity = false,
                supportFingerprint = false,
                currentBookName = "默认账本1",
            ),
            agreeProtocol = {},
            firstOpen = true,
            dialogState = DialogState.Dismiss,
            onConfirmClick = { _, _ -> },
            onFingerprintClick = {},
            onFingerprintVerifySuccess = {},
            onFingerprintVerifyError = { _, _ -> },
            onActivityStop = {},
            onMyAssetClick = {},
            onMyBookClick = {},
            onMyCategoryClick = {},
            onMyTagClick = {},
            onSettingClick = {},
            onAboutUsClick = {},
            onPrivacyPolicyClick = {},
            onShowSnackbar = { _, _ -> SnackbarResult.Dismissed },
            content = { Text(text = "内容部分") },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@DevicePreviews
@Composable
private fun LauncherScreenSheetPreview() {
    PreviewTheme {
        LauncherScreen(
            shouldDisplayDrawerSheet = true,
            shouldDisplayBookmark = LauncherBookmarkEnum.NONE,
            errorText = "",
            dismissBookmark = {},
            uiState = LauncherUiState.Success(
                needRequestProtocol = false,
                needVerity = false,
                supportFingerprint = false,
                currentBookName = "默认账本2",
            ),
            agreeProtocol = {},
            firstOpen = true,
            dialogState = DialogState.Dismiss,
            onConfirmClick = { _, _ -> },
            onFingerprintClick = {},
            onFingerprintVerifySuccess = {},
            onFingerprintVerifyError = { _, _ -> },
            onActivityStop = {},
            onMyAssetClick = {},
            onMyBookClick = {},
            onMyCategoryClick = {},
            onMyTagClick = {},
            onSettingClick = {},
            onAboutUsClick = {},
            onPrivacyPolicyClick = {},
            onShowSnackbar = { _, _ -> SnackbarResult.Dismissed },
            content = { Text(text = "内容部分") },
            drawerState = rememberDrawerState(initialValue = DrawerValue.Open),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@DevicePreviews
@Composable
private fun LauncherScreenVerifyPreview() {
    PreviewTheme {
        LauncherScreen(
            shouldDisplayDrawerSheet = false,
            shouldDisplayBookmark = LauncherBookmarkEnum.PASSWORD_DECODE_FAILED,
            errorText = "",
            dismissBookmark = {},
            uiState = LauncherUiState.Success(
                needRequestProtocol = false,
                needVerity = true,
                supportFingerprint = true,
                currentBookName = "默认账本3",
            ),
            agreeProtocol = {},
            firstOpen = true,
            dialogState = DialogState.Dismiss,
            onConfirmClick = { _, _ -> },
            onFingerprintClick = {},
            onFingerprintVerifySuccess = {},
            onFingerprintVerifyError = { _, _ -> },
            onActivityStop = {},
            onMyAssetClick = {},
            onMyBookClick = {},
            onMyCategoryClick = {},
            onMyTagClick = {},
            onSettingClick = {},
            onAboutUsClick = {},
            onPrivacyPolicyClick = {},
            onShowSnackbar = { _, _ -> SnackbarResult.Dismissed },
            content = { Text(text = "内容部分") },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@DevicePreviews
@Composable
private fun LauncherScreenProtocolPreview() {
    PreviewTheme {
        LauncherScreen(
            shouldDisplayDrawerSheet = false,
            shouldDisplayBookmark = LauncherBookmarkEnum.NONE,
            errorText = "",
            dismissBookmark = {},
            uiState = LauncherUiState.Success(
                needRequestProtocol = true,
                needVerity = false,
                supportFingerprint = false,
                currentBookName = "默认账本4",
            ),
            agreeProtocol = {},
            firstOpen = true,
            dialogState = DialogState.Dismiss,
            onConfirmClick = { _, _ -> },
            onFingerprintClick = {},
            onFingerprintVerifySuccess = {},
            onFingerprintVerifyError = { _, _ -> },
            onActivityStop = {},
            onMyAssetClick = {},
            onMyBookClick = {},
            onMyCategoryClick = {},
            onMyTagClick = {},
            onSettingClick = {},
            onAboutUsClick = {},
            onPrivacyPolicyClick = {},
            onShowSnackbar = { _, _ -> SnackbarResult.Dismissed },
            content = { Text(text = "内容部分") },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@DevicePreviews
@Composable
private fun LauncherScreenLoadingPreview() {
    PreviewTheme {
        LauncherScreen(
            shouldDisplayDrawerSheet = false,
            shouldDisplayBookmark = LauncherBookmarkEnum.NONE,
            errorText = "",
            dismissBookmark = {},
            uiState = LauncherUiState.Loading,
            agreeProtocol = {},
            firstOpen = true,
            dialogState = DialogState.Dismiss,
            onConfirmClick = { _, _ -> },
            onFingerprintClick = {},
            onFingerprintVerifySuccess = {},
            onFingerprintVerifyError = { _, _ -> },
            onActivityStop = {},
            onMyAssetClick = {},
            onMyBookClick = {},
            onMyCategoryClick = {},
            onMyTagClick = {},
            onSettingClick = {},
            onAboutUsClick = {},
            onPrivacyPolicyClick = {},
            onShowSnackbar = { _, _ -> SnackbarResult.Dismissed },
            content = { Text(text = "内容部分") },
        )
    }
}