package cn.wj.android.cashbook.feature.settings.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.common.PASSWORD_REGEX
import cn.wj.android.cashbook.core.common.tools.isMatch
import cn.wj.android.cashbook.core.design.component.CashbookScaffold
import cn.wj.android.cashbook.core.design.component.CashbookTopAppBar
import cn.wj.android.cashbook.core.design.component.PasswordTextField
import cn.wj.android.cashbook.core.design.component.TransparentListItem
import cn.wj.android.cashbook.core.design.icon.CashbookIcons
import cn.wj.android.cashbook.core.design.security.biometric.BiometricAuthenticate
import cn.wj.android.cashbook.core.design.security.biometric.BiometricAuthenticateHintData
import cn.wj.android.cashbook.core.design.security.biometric.HW_AVAILABLE
import cn.wj.android.cashbook.core.design.security.biometric.ProvideBiometricAuthenticateHintData
import cn.wj.android.cashbook.core.design.security.biometric.checkBiometric
import cn.wj.android.cashbook.core.design.theme.PreviewTheme
import cn.wj.android.cashbook.core.design.theme.supportsDynamicTheming
import cn.wj.android.cashbook.core.model.enums.DarkModeEnum
import cn.wj.android.cashbook.core.model.enums.VerificationModeEnum
import cn.wj.android.cashbook.core.ui.DevicePreviews
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.settings.enums.SettingDialogEnum
import cn.wj.android.cashbook.feature.settings.enums.SettingPasswordStateEnum
import cn.wj.android.cashbook.feature.settings.viewmodel.SettingUiState
import cn.wj.android.cashbook.feature.settings.viewmodel.SettingViewModel
import javax.crypto.Cipher

/**
 * 设置页路由
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/19
 */
@Composable
internal fun SettingRoute(
    onBackClick: () -> Unit,
    onBackupAndRecoveryClick: () -> Unit,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,
    modifier: Modifier = Modifier,
    hasFingerprint: Boolean = checkBiometric() == HW_AVAILABLE,
    viewModel: SettingViewModel = hiltViewModel()
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingScreen(
        dialogState = viewModel.dialogState,
        shouldDisplayBookmark = viewModel.shouldDisplayBookmark,
        uiState = uiState,
        hasFingerprint = hasFingerprint,
        onMobileNetworkDownloadEnableChanged = viewModel::onMobileNetworkDownloadEnableChanged,
        onNeedSecurityVerificationWhenLaunchChanged = viewModel::onNeedSecurityVerificationWhenLaunchChanged,
        onEnableFingerprintVerificationChanged = viewModel::onEnableFingerprintVerificationChanged,
        onPasswordClick = viewModel::onPasswordClick,
        onClearPasswordClick = viewModel::onClearPasswordClick,
        onDarkModeClick = viewModel::onDarkModeClick,
        onDynamicColorClick = viewModel::onDynamicColorClick,
        onVerificationModeClick = viewModel::onVerificationModeClick,
        onCreateConfirm = viewModel::onCreateConfirm,
        onModifyConfirm = viewModel::onModifyConfirm,
        onVerityConfirm = viewModel::onVerityConfirm,
        onClearConfirm = viewModel::onClearConfirm,
        onFingerprintVerifySuccess = viewModel::onFingerprintVerifySuccess,
        onFingerprintVerifyError = viewModel::onFingerprintVerifyError,
        onDarkModeSelected = viewModel::onDarkModeSelected,
        onDynamicColorSelected = viewModel::onDynamicColorSelected,
        onVerificationModeSelected = viewModel::onVerificationModeSelected,
        onDialogDismiss = viewModel::dismissDialog,
        onBookmarkDismiss = viewModel::dismissBookmark,
        onBackClick = onBackClick,
        onBackupAndRecoveryClick = onBackupAndRecoveryClick,
        onShowSnackbar = onShowSnackbar,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingScreen(
    dialogState: DialogState,
    shouldDisplayBookmark: String,
    uiState: SettingUiState,
    hasFingerprint: Boolean,
    onMobileNetworkDownloadEnableChanged: (Boolean) -> Unit,
    onNeedSecurityVerificationWhenLaunchChanged: (Boolean) -> Unit,
    onEnableFingerprintVerificationChanged: (Boolean) -> Unit,
    onPasswordClick: () -> Unit,
    onClearPasswordClick: () -> Unit,
    onDarkModeClick: () -> Unit,
    onDynamicColorClick: () -> Unit,
    onVerificationModeClick: () -> Unit,
    onCreateConfirm: (String) -> SettingPasswordStateEnum,
    onModifyConfirm: (String, String, (SettingPasswordStateEnum) -> Unit) -> Unit,
    onVerityConfirm: (String, (SettingPasswordStateEnum) -> Unit) -> Unit,
    onClearConfirm: (String, (SettingPasswordStateEnum) -> Unit) -> Unit,
    onFingerprintVerifySuccess: (Cipher) -> Unit,
    onFingerprintVerifyError: (Int, String) -> Unit,
    onDialogDismiss: () -> Unit,
    onBookmarkDismiss: () -> Unit,
    onDarkModeSelected: (DarkModeEnum) -> Unit,
    onDynamicColorSelected: (Boolean) -> Unit,
    onVerificationModeSelected: (VerificationModeEnum) -> Unit,
    onBackClick: () -> Unit,
    onBackupAndRecoveryClick: () -> Unit,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(shouldDisplayBookmark) {
        if (shouldDisplayBookmark.isNotBlank()) {
            val showSnackbarResult = onShowSnackbar(shouldDisplayBookmark, null)
            if (SnackbarResult.Dismissed == showSnackbarResult) {
                onBookmarkDismiss.invoke()
            }
        }
    }

    CashbookScaffold(
        topBar = {
            CashbookTopAppBar(
                onBackClick = onBackClick,
                title = { Text(text = stringResource(id = R.string.settings)) },
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        SettingContent(
            modifier = Modifier.padding(paddingValues),
            dialogState = dialogState,
            onCreateConfirm = onCreateConfirm,
            onDialogDismiss = onDialogDismiss,
            onModifyConfirm = onModifyConfirm,
            onVerityConfirm = onVerityConfirm,
            onClearConfirm = onClearConfirm,
            uiState = uiState,
            onFingerprintVerifySuccess = onFingerprintVerifySuccess,
            onFingerprintVerifyError = onFingerprintVerifyError,
            onMobileNetworkDownloadEnableChanged = onMobileNetworkDownloadEnableChanged,
            onNeedSecurityVerificationWhenLaunchChanged = onNeedSecurityVerificationWhenLaunchChanged,
            hasFingerprint = hasFingerprint,
            onEnableFingerprintVerificationChanged = onEnableFingerprintVerificationChanged,
            onPasswordClick = onPasswordClick,
            onDarkModeClick = onDarkModeClick,
            onDynamicColorClick = onDynamicColorClick,
            onVerificationModeClick = onVerificationModeClick,
            onClearPasswordClick = onClearPasswordClick,
            onDarkModeSelected = onDarkModeSelected,
            onDynamicColorSelected = onDynamicColorSelected,
            onVerificationModeSelected = onVerificationModeSelected,
            onBackupAndRecoveryClick = onBackupAndRecoveryClick,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun SettingContent(
    dialogState: DialogState,
    hasFingerprint: Boolean,
    uiState: SettingUiState,
    onMobileNetworkDownloadEnableChanged: (Boolean) -> Unit,
    onEnableFingerprintVerificationChanged: (Boolean) -> Unit,
    onNeedSecurityVerificationWhenLaunchChanged: (Boolean) -> Unit,
    onPasswordClick: () -> Unit,
    onClearPasswordClick: () -> Unit,
    onDarkModeClick: () -> Unit,
    onDynamicColorClick: () -> Unit,
    onVerificationModeClick: () -> Unit,
    onCreateConfirm: (String) -> SettingPasswordStateEnum,
    onModifyConfirm: (String, String, (SettingPasswordStateEnum) -> Unit) -> Unit,
    onVerityConfirm: (String, (SettingPasswordStateEnum) -> Unit) -> Unit,
    onClearConfirm: (String, (SettingPasswordStateEnum) -> Unit) -> Unit,
    onFingerprintVerifySuccess: (Cipher) -> Unit,
    onFingerprintVerifyError: (Int, String) -> Unit,
    onDarkModeSelected: (DarkModeEnum) -> Unit,
    onDynamicColorSelected: (Boolean) -> Unit,
    onVerificationModeSelected: (VerificationModeEnum) -> Unit,
    onDialogDismiss: () -> Unit,
    onBackupAndRecoveryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
    ) {

        DialogContent(
            dialogState = dialogState,
            verificationMode = uiState.verificationMode,
            darkMode = uiState.darkMode,
            dynamicColor = uiState.dynamicColor,
            onCreateConfirm = onCreateConfirm,
            onDialogDismiss = onDialogDismiss,
            onModifyConfirm = onModifyConfirm,
            onVerityConfirm = onVerityConfirm,
            onClearConfirm = onClearConfirm,
            onFingerprintVerifySuccess = onFingerprintVerifySuccess,
            onFingerprintVerifyError = onFingerprintVerifyError,
            onDarkModeSelected = onDarkModeSelected,
            onDynamicColorSelected = onDynamicColorSelected,
            onVerificationModeSelected = onVerificationModeSelected,
        )

        LazyColumn {
            item {
                TransparentListItem(
                    modifier = Modifier.padding(top = 16.dp),
                    headlineContent = { Text(text = stringResource(id = R.string.mobile_network_download_enable)) },
                    trailingContent = {
                        Switch(
                            checked = uiState.mobileNetworkDownloadEnable,
                            onCheckedChange = onMobileNetworkDownloadEnableChanged,
                        )
                    },
                )
                Divider(modifier = Modifier.padding(horizontal = 8.dp))
            }
            item {
                TransparentListItem(
                    modifier = Modifier.padding(top = 16.dp),
                    headlineContent = { Text(text = stringResource(id = R.string.need_security_verification_when_launch)) },
                    trailingContent = {
                        Switch(
                            checked = uiState.needSecurityVerificationWhenLaunch,
                            onCheckedChange = onNeedSecurityVerificationWhenLaunchChanged,
                        )
                    },
                )
                if (uiState.needSecurityVerificationWhenLaunch) {
                    val verificationModeText = stringResource(
                        id = when (uiState.verificationMode) {
                            VerificationModeEnum.WHEN_LAUNCH -> R.string.each_launch
                            VerificationModeEnum.WHEN_FOREGROUND -> R.string.each_foreground
                        }
                    )
                    TransparentListItem(
                        modifier = Modifier
                            .clickable { onVerificationModeClick.invoke() },
                        headlineContent = { Text(text = stringResource(id = R.string.verification_mode)) },
                        trailingContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    modifier = Modifier.padding(end = 8.dp),
                                    text = verificationModeText,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Icon(
                                    imageVector = CashbookIcons.KeyboardArrowRight,
                                    contentDescription = null,
                                )
                            }
                        },
                    )
                }
                if (uiState.needSecurityVerificationWhenLaunch && hasFingerprint) {
                    TransparentListItem(
                        headlineContent = { Text(text = stringResource(id = R.string.enable_fingerprint_verification)) },
                        trailingContent = {
                            Switch(
                                checked = uiState.enableFingerprintVerification,
                                onCheckedChange = onEnableFingerprintVerificationChanged,
                            )
                        },
                    )
                }
                TransparentListItem(
                    modifier = Modifier.clickable { onPasswordClick.invoke() },
                    headlineContent = {
                        if (uiState.hasPassword) {
                            Text(text = stringResource(id = R.string.modify_password))
                        } else {
                            Text(text = stringResource(id = R.string.create_password))
                        }
                    },
                )
                if (uiState.hasPassword) {
                    TransparentListItem(
                        modifier = Modifier.clickable { onClearPasswordClick.invoke() },
                        headlineContent = { Text(text = stringResource(id = R.string.clear_password)) },
                    )
                }
                Divider(modifier = Modifier.padding(horizontal = 8.dp))
            }

            item {
                val darkModeText = stringResource(
                    id = when (uiState.darkMode) {
                        DarkModeEnum.FOLLOW_SYSTEM -> R.string.follow_system
                        DarkModeEnum.LIGHT -> R.string.light_mode
                        DarkModeEnum.DARK -> R.string.dark_mode
                    }
                )
                TransparentListItem(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .clickable { onDarkModeClick.invoke() },
                    headlineContent = { Text(text = stringResource(id = R.string.dark_mode)) },
                    trailingContent = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                modifier = Modifier.padding(end = 8.dp),
                                text = darkModeText,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Icon(
                                imageVector = CashbookIcons.KeyboardArrowRight,
                                contentDescription = null,
                            )
                        }
                    },
                )
                if (supportsDynamicTheming()) {
                    TransparentListItem(
                        modifier = Modifier.clickable { onDynamicColorClick.invoke() },
                        headlineContent = { Text(text = stringResource(id = R.string.dynamic_color)) },
                        supportingContent = { Text(text = stringResource(id = R.string.dynamic_color_hint)) },
                        trailingContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    modifier = Modifier.padding(end = 8.dp),
                                    text = stringResource(id = if (uiState.dynamicColor) R.string.switch_on else R.string.switch_off),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Icon(
                                    imageVector = CashbookIcons.KeyboardArrowRight,
                                    contentDescription = null,
                                )
                            }
                        },
                    )
                }
                Divider(modifier = Modifier.padding(horizontal = 8.dp))
            }

            item {
                TransparentListItem(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .clickable { onBackupAndRecoveryClick.invoke() },
                    headlineContent = { Text(text = stringResource(id = R.string.backup_and_recovery)) },
                    trailingContent = {
                        Icon(
                            imageVector = CashbookIcons.KeyboardArrowRight,
                            contentDescription = null,
                        )
                    },
                )
            }
        }
    }
}

@Composable
internal fun DialogContent(
    dialogState: DialogState,
    verificationMode: VerificationModeEnum,
    darkMode: DarkModeEnum,
    dynamicColor: Boolean,
    onCreateConfirm: (String) -> SettingPasswordStateEnum,
    onModifyConfirm: (String, String, (SettingPasswordStateEnum) -> Unit) -> Unit,
    onVerityConfirm: (String, (SettingPasswordStateEnum) -> Unit) -> Unit,
    onClearConfirm: (String, (SettingPasswordStateEnum) -> Unit) -> Unit,
    onDialogDismiss: () -> Unit,
    onFingerprintVerifySuccess: (Cipher) -> Unit,
    onFingerprintVerifyError: (Int, String) -> Unit,
    onDarkModeSelected: (DarkModeEnum) -> Unit,
    onDynamicColorSelected: (Boolean) -> Unit,
    onVerificationModeSelected: (VerificationModeEnum) -> Unit,
) {
    (dialogState as? DialogState.Shown<*>)?.let {
        when (it.data) {
            SettingDialogEnum.CREATE_PASSWORD -> {
                // 创建密码
                CreatePasswordDialog(
                    onConfirmClick = onCreateConfirm,
                    onDismissClick = onDialogDismiss,
                )
            }

            SettingDialogEnum.MODIFY_PASSWORD -> {
                // 修改密码
                ModifyPasswordDialog(
                    onConfirmClick = onModifyConfirm,
                    onDismissClick = onDialogDismiss,
                )
            }

            SettingDialogEnum.VERIFY_PASSWORD -> {
                // 验证密码
                VerityPasswordDialog(
                    onConfirmClick = onVerityConfirm,
                    onDismissClick = onDialogDismiss,
                )
            }

            SettingDialogEnum.CLEAR_PASSWORD -> {
                // 清除密码
                ClearPasswordDialog(
                    onConfirmClick = onClearConfirm,
                    onDismissClick = onDialogDismiss,
                )
            }

            SettingDialogEnum.DARK_MODE -> {
                // 黑夜模式
                DarkModeDialog(
                    darkMode = darkMode,
                    onDarkModeSelected = onDarkModeSelected,
                    onDismissClick = onDialogDismiss,
                )
            }

            SettingDialogEnum.DYNAMIC_COLOR -> {
                // 动态配色
                DynamicColorDialog(
                    dynamicColor = dynamicColor,
                    onDynamicColorSelected = onDynamicColorSelected,
                    onDismissClick = onDialogDismiss,
                )
            }

            SettingDialogEnum.VERIFICATION_MODE -> {
                // 验证模式
                VerificationModeDialog(
                    verificationMode = verificationMode,
                    onVerificationModeSelected = onVerificationModeSelected,
                    onDismissClick = onDialogDismiss,
                )
            }

            else -> {
                val data = it.data
                if (data is Cipher) {
                    // 指纹认证
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
                            subTitle = stringResource(id = R.string.verity_fingerprint_to_open_fingerprint_login),
                            hint = stringResource(id = R.string.press_sensing_to_verity_fingerprint),
                            cryptoCipher = data,
                            onSuccess = onFingerprintVerifySuccess,
                            onError = onFingerprintVerifyError,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun CreatePasswordDialog(
    onConfirmClick: (String) -> SettingPasswordStateEnum,
    onDismissClick: () -> Unit,
) {
    // 提示文本
    val passwordMustNotBeBlankText = stringResource(id = R.string.password_must_not_be_blank)
    val passwordConfirmFailedText = stringResource(id = R.string.password_confirm_failed)
    val passwordFormatErrorText = stringResource(id = R.string.password_format_error)
    val passwordEncodeFailedText = stringResource(id = R.string.password_encode_failed)

    var pwd1 by remember {
        mutableStateOf("")
    }
    var pwd1Error by remember {
        mutableStateOf(false)
    }
    var pwd1SupportText by remember {
        mutableStateOf("")
    }
    var pwd2 by remember {
        mutableStateOf("")
    }
    var pwd2Error by remember {
        mutableStateOf(false)
    }
    var pwd2SupportText by remember {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = onDismissClick,
        title = { Text(text = stringResource(id = R.string.create_password)) },
        text = {
            Column {
                PasswordTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    initializedText = pwd1,
                    label = stringResource(id = R.string.new_password),
                    isError = pwd1Error,
                    supportingText = pwd1SupportText,
                    onValueChange = { pwd1 = it },
                )
                PasswordTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    initializedText = pwd2,
                    label = stringResource(id = R.string.confirm_password),
                    isError = pwd2Error,
                    supportingText = pwd2SupportText,
                    onValueChange = { pwd2 = it },
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (pwd1.isBlank()) {
                    pwd1Error = true
                    pwd1SupportText = passwordMustNotBeBlankText
                } else if (!pwd1.isMatch(PASSWORD_REGEX)) {
                    pwd1Error = true
                    pwd1SupportText = passwordFormatErrorText
                } else {
                    pwd1Error = false
                    pwd1SupportText = ""
                    when {
                        pwd2.isBlank() -> {
                            pwd2Error = true
                            pwd2SupportText = passwordMustNotBeBlankText
                        }

                        !pwd2.isMatch(PASSWORD_REGEX) -> {
                            pwd2Error = true
                            pwd2SupportText = passwordFormatErrorText
                        }

                        pwd1 != pwd2 -> {
                            pwd2Error = true
                            pwd2SupportText = passwordConfirmFailedText
                        }

                        else -> {
                            val result = onConfirmClick.invoke(pwd1)
                            if (result == SettingPasswordStateEnum.PASSWORD_ENCODE_FAILED) {
                                pwd2Error = true
                                pwd2SupportText = passwordEncodeFailedText
                            }
                        }
                    }
                }
            }) {
                Text(text = stringResource(id = R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissClick) {
                Text(text = stringResource(id = R.string.cancel))
            }
        },
    )
}


@Composable
internal fun ModifyPasswordDialog(
    onConfirmClick: (String, String, (SettingPasswordStateEnum) -> Unit) -> Unit,
    onDismissClick: () -> Unit,
) {
    // 提示文本
    val passwordMustNotBeBlankText = stringResource(id = R.string.password_must_not_be_blank)
    val passwordCannotBeSameText = stringResource(id = R.string.password_cannot_be_same)
    val passwordFormatErrorText = stringResource(id = R.string.password_format_error)
    val passwordEncodeFailedText = stringResource(id = R.string.password_encode_failed)
    val passwordDecodeFailedText = stringResource(id = R.string.password_decode_failed)
    val passwordWrongText = stringResource(id = R.string.password_wrong)

    var pwd1 by remember {
        mutableStateOf("")
    }
    var pwd1Error by remember {
        mutableStateOf(false)
    }
    var pwd1SupportText by remember {
        mutableStateOf("")
    }
    var pwd2 by remember {
        mutableStateOf("")
    }
    var pwd2Error by remember {
        mutableStateOf(false)
    }
    var pwd2SupportText by remember {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = onDismissClick,
        title = { Text(text = stringResource(id = R.string.modify_password)) },
        text = {
            Column {
                PasswordTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    initializedText = pwd1,
                    label = stringResource(id = R.string.old_password),
                    isError = pwd1Error,
                    supportingText = pwd1SupportText,
                    onValueChange = { pwd1 = it },
                )
                PasswordTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    initializedText = pwd2,
                    label = stringResource(id = R.string.new_password),
                    isError = pwd2Error,
                    supportingText = pwd2SupportText,
                    onValueChange = { pwd2 = it },
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (pwd1.isBlank()) {
                    pwd1Error = true
                    pwd1SupportText = passwordMustNotBeBlankText
                } else if (!pwd1.isMatch(PASSWORD_REGEX)) {
                    pwd1Error = true
                    pwd1SupportText = passwordFormatErrorText
                } else {
                    pwd1Error = false
                    pwd1SupportText = ""
                    when {
                        pwd2.isBlank() -> {
                            pwd2Error = true
                            pwd2SupportText = passwordMustNotBeBlankText
                        }

                        !pwd2.isMatch(PASSWORD_REGEX) -> {
                            pwd2Error = true
                            pwd2SupportText = passwordFormatErrorText
                        }

                        pwd1 == pwd2 -> {
                            pwd2Error = true
                            pwd2SupportText = passwordCannotBeSameText
                        }

                        else -> {
                            onConfirmClick.invoke(pwd1, pwd2) { result ->
                                when (result) {
                                    SettingPasswordStateEnum.PASSWORD_ENCODE_FAILED -> {
                                        pwd2Error = true
                                        pwd2SupportText = passwordEncodeFailedText
                                    }

                                    SettingPasswordStateEnum.PASSWORD_DECODE_FAILED -> {
                                        pwd2Error = false
                                        pwd2SupportText = ""
                                        pwd1Error = true
                                        pwd1SupportText = passwordDecodeFailedText
                                    }

                                    SettingPasswordStateEnum.PASSWORD_WRONG -> {
                                        pwd2Error = false
                                        pwd2SupportText = ""
                                        pwd1Error = true
                                        pwd1SupportText = passwordWrongText
                                    }

                                    else -> {
                                        pwd1Error = false
                                        pwd1SupportText = ""
                                        pwd2Error = false
                                        pwd2SupportText = ""
                                    }
                                }
                            }
                        }
                    }
                }
            }) {
                Text(text = stringResource(id = R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissClick) {
                Text(text = stringResource(id = R.string.cancel))
            }
        },
    )
}

@Composable
internal fun VerityPasswordDialog(
    onConfirmClick: (String, (SettingPasswordStateEnum) -> Unit) -> Unit,
    onDismissClick: () -> Unit,
) {
    // 提示文本
    val passwordMustNotBeBlankText = stringResource(id = R.string.password_must_not_be_blank)
    val passwordWrongText = stringResource(id = R.string.password_wrong)
    val passwordFormatErrorText = stringResource(id = R.string.password_format_error)
    val passwordDecodeFailedText = stringResource(id = R.string.password_decode_failed)

    var pwd by remember {
        mutableStateOf("")
    }
    var pwdError by remember {
        mutableStateOf(false)
    }
    var pwdSupportText by remember {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = onDismissClick,
        title = { Text(text = stringResource(id = R.string.verity_password)) },
        text = {
            Column {
                PasswordTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    initializedText = pwd,
                    label = stringResource(id = R.string.please_enter_password),
                    isError = pwdError,
                    supportingText = pwdSupportText,
                    onValueChange = { pwd = it },
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = {
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
            }) {
                Text(text = stringResource(id = R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissClick) {
                Text(text = stringResource(id = R.string.cancel))
            }
        },
    )
}

@Composable
internal fun ClearPasswordDialog(
    onConfirmClick: (String, (SettingPasswordStateEnum) -> Unit) -> Unit,
    onDismissClick: () -> Unit,
) {
    // 提示文本
    val passwordMustNotBeBlankText = stringResource(id = R.string.password_must_not_be_blank)
    val passwordWrongText = stringResource(id = R.string.password_wrong)
    val passwordFormatErrorText = stringResource(id = R.string.password_format_error)
    val passwordDecodeFailedText = stringResource(id = R.string.password_decode_failed)

    var pwd by remember {
        mutableStateOf("")
    }
    var pwdError by remember {
        mutableStateOf(false)
    }
    var pwdSupportText by remember {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = onDismissClick,
        title = { Text(text = stringResource(id = R.string.clear_password)) },
        text = {
            Column {
                PasswordTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    initializedText = pwd,
                    label = stringResource(id = R.string.please_enter_password),
                    isError = pwdError,
                    supportingText = pwdSupportText,
                    onValueChange = { pwd = it },
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = {
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
            }) {
                Text(text = stringResource(id = R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissClick) {
                Text(text = stringResource(id = R.string.cancel))
            }
        },
    )
}

@Composable
internal fun VerificationModeDialog(
    verificationMode: VerificationModeEnum,
    onVerificationModeSelected: (VerificationModeEnum) -> Unit,
    onDismissClick: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissClick,
        title = { Text(text = stringResource(id = R.string.verification_mode)) },
        text = {
            Column(
                modifier = Modifier.selectableGroup(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .selectable(
                            selected = (VerificationModeEnum.WHEN_LAUNCH == verificationMode),
                            onClick = { onVerificationModeSelected.invoke(VerificationModeEnum.WHEN_LAUNCH) },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = VerificationModeEnum.WHEN_LAUNCH == verificationMode,
                        onClick = null
                    )
                    Text(
                        text = stringResource(id = R.string.each_launch),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .selectable(
                            selected = (VerificationModeEnum.WHEN_FOREGROUND == verificationMode),
                            onClick = { onVerificationModeSelected.invoke(VerificationModeEnum.WHEN_FOREGROUND) },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = VerificationModeEnum.WHEN_FOREGROUND == verificationMode,
                        onClick = null
                    )
                    Text(
                        text = stringResource(id = R.string.each_foreground),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissClick) {
                Text(text = stringResource(id = R.string.close))
            }
        },
    )
}

@Composable
internal fun DarkModeDialog(
    darkMode: DarkModeEnum,
    onDarkModeSelected: (DarkModeEnum) -> Unit,
    onDismissClick: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissClick,
        title = { Text(text = stringResource(id = R.string.dark_mode)) },
        text = {
            Column(
                modifier = Modifier.selectableGroup(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .selectable(
                            selected = (DarkModeEnum.FOLLOW_SYSTEM == darkMode),
                            onClick = { onDarkModeSelected.invoke(DarkModeEnum.FOLLOW_SYSTEM) },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = DarkModeEnum.FOLLOW_SYSTEM == darkMode, onClick = null)
                    Text(
                        text = stringResource(id = R.string.follow_system),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .selectable(
                            selected = (DarkModeEnum.LIGHT == darkMode),
                            onClick = { onDarkModeSelected.invoke(DarkModeEnum.LIGHT) },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = DarkModeEnum.LIGHT == darkMode, onClick = null)
                    Text(
                        text = stringResource(id = R.string.light_mode),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .selectable(
                            selected = (DarkModeEnum.DARK == darkMode),
                            onClick = { onDarkModeSelected.invoke(DarkModeEnum.DARK) },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = DarkModeEnum.DARK == darkMode, onClick = null)
                    Text(
                        text = stringResource(id = R.string.dark_mode),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissClick) {
                Text(text = stringResource(id = R.string.close))
            }
        },
    )
}

@Composable
internal fun DynamicColorDialog(
    dynamicColor: Boolean,
    onDynamicColorSelected: (Boolean) -> Unit,
    onDismissClick: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissClick,
        title = { Text(text = stringResource(id = R.string.dynamic_color)) },
        text = {
            Column(
                modifier = Modifier.selectableGroup(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .selectable(
                            selected = (dynamicColor),
                            onClick = { onDynamicColorSelected.invoke(true) },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = dynamicColor, onClick = null)
                    Text(
                        text = stringResource(id = R.string.switch_on),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .selectable(
                            selected = (!dynamicColor),
                            onClick = { onDynamicColorSelected.invoke(false) },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = !dynamicColor, onClick = null)
                    Text(
                        text = stringResource(id = R.string.switch_off),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissClick) {
                Text(text = stringResource(id = R.string.close))
            }
        },
    )
}

@DevicePreviews
@Composable
private fun SettingScreenPreview() {
    PreviewTheme {
        SettingScreen(
            dialogState = DialogState.Dismiss,
            shouldDisplayBookmark = "",
            uiState = SettingUiState.Loading,
            hasFingerprint = true,
            onMobileNetworkDownloadEnableChanged = {},
            onNeedSecurityVerificationWhenLaunchChanged = {},
            onEnableFingerprintVerificationChanged = {},
            onPasswordClick = {},
            onClearPasswordClick = {},
            onDarkModeClick = {},
            onDynamicColorClick = {},
            onVerificationModeClick = {},
            onCreateConfirm = { SettingPasswordStateEnum.SUCCESS },
            onModifyConfirm = { _, _, _ -> },
            onVerityConfirm = { _, _ -> },
            onClearConfirm = { _, _ -> },
            onFingerprintVerifySuccess = {},
            onFingerprintVerifyError = { _, _ -> },
            onDialogDismiss = {},
            onBookmarkDismiss = {},
            onDarkModeSelected = {},
            onDynamicColorSelected = {},
            onVerificationModeSelected = {},
            onBackClick = {},
            onBackupAndRecoveryClick = {},
            onShowSnackbar = { _, _ -> SnackbarResult.Dismissed },
        )
    }
}