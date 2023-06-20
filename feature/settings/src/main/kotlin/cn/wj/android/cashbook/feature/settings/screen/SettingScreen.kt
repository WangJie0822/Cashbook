package cn.wj.android.cashbook.feature.settings.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.common.PASSWORD_REGEX
import cn.wj.android.cashbook.core.common.tools.isMatch
import cn.wj.android.cashbook.core.design.component.CommonTopBar
import cn.wj.android.cashbook.core.design.component.CompatTextField
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.settings.enums.SettingBookmarkEnum
import cn.wj.android.cashbook.feature.settings.enums.SettingDialogEnum
import cn.wj.android.cashbook.feature.settings.viewmodel.SettingViewModel

/**
 * 设置页路由
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/19
 */
@Composable
internal fun SettingRoute(
    onBackClick: () -> Unit,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,
    modifier: Modifier = Modifier,
    hasFingerprint: Boolean = false,// TODO
    viewModel: SettingViewModel = hiltViewModel()
) {

    val mobileNetworkDownloadEnable by viewModel.mobileNetworkDownloadEnable.collectAsStateWithLifecycle()
    val needSecurityVerificationWhenLaunch by viewModel.needSecurityVerificationWhenLaunch.collectAsStateWithLifecycle()
    val enableFingerprintVerification by viewModel.enableFingerprintVerification.collectAsStateWithLifecycle()
    val hasPassword by viewModel.hasPassword.collectAsStateWithLifecycle()

    SettingScreen(
        dialogState = viewModel.dialogState,
        shouldDisplayBookmark = viewModel.shouldDisplayBookmark,
        mobileNetworkDownloadEnable = mobileNetworkDownloadEnable,
        needSecurityVerificationWhenLaunch = needSecurityVerificationWhenLaunch,
        enableFingerprintVerification = enableFingerprintVerification,
        hasPassword = hasPassword,
        hasFingerprint = hasFingerprint,
        onMobileNetworkDownloadEnableChanged = viewModel::onMobileNetworkDownloadEnableChanged,
        onNeedSecurityVerificationWhenLaunchChanged = viewModel::onNeedSecurityVerificationWhenLaunchChanged,
        onEnableFingerprintVerificationChanged = viewModel::onEnableFingerprintVerificationChanged,
        onPasswordClick = viewModel::onPasswordClick,
        onClearPasswordClick = viewModel::onClearPasswordClick,
        onCreateConfirm = viewModel::onCreateConfirm,
        onModifyConfirm = viewModel::onModifyConfirm,
        onClearConfirm = viewModel::onClearConfirm,
        onDialogDismiss = viewModel::dismissDialog,
        onBookmarkDismiss = viewModel::dismissBookmark,
        onBackClick = onBackClick,
        onShowSnackbar = onShowSnackbar,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingScreen(
    dialogState: DialogState,
    shouldDisplayBookmark: SettingBookmarkEnum,
    mobileNetworkDownloadEnable: Boolean,
    needSecurityVerificationWhenLaunch: Boolean,
    enableFingerprintVerification: Boolean,
    hasPassword: Boolean,
    hasFingerprint: Boolean,
    onMobileNetworkDownloadEnableChanged: (Boolean) -> Unit,
    onNeedSecurityVerificationWhenLaunchChanged: (Boolean) -> Unit,
    onEnableFingerprintVerificationChanged: (Boolean) -> Unit,
    onPasswordClick: () -> Unit,
    onClearPasswordClick: () -> Unit,
    onCreateConfirm: (String) -> SettingBookmarkEnum,
    onModifyConfirm: (String, String, (SettingBookmarkEnum) -> Unit) -> Unit,
    onClearConfirm: (String, (SettingBookmarkEnum) -> Unit) -> Unit,
    onDialogDismiss: () -> Unit,
    onBookmarkDismiss: () -> Unit,
    onBackClick: () -> Unit,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,
    modifier: Modifier = Modifier,
) {
    // 提示文本
    val passwordMustNotBeBlankText = stringResource(id = R.string.password_must_not_be_blank)
    val passwordConfirmFailedText = stringResource(id = R.string.password_confirm_failed)
    val passwordFormatErrorText = stringResource(id = R.string.password_format_error)
    val passwordEncodeFailedText = stringResource(id = R.string.password_encode_failed)
    LaunchedEffect(shouldDisplayBookmark) {
        if (shouldDisplayBookmark != SettingBookmarkEnum.NONE) {
            val tipsText = when (shouldDisplayBookmark) {
                SettingBookmarkEnum.PASSWORD_MUST_NOT_BLANK -> passwordMustNotBeBlankText
                SettingBookmarkEnum.PASSWORD_CONFIRM_FAILED -> passwordConfirmFailedText
                SettingBookmarkEnum.PASSWORD_FORMAT_ERROR -> passwordFormatErrorText
                SettingBookmarkEnum.PASSWORD_ENCODE_FAILED -> passwordEncodeFailedText
                else -> ""
            }
            val showSnackbarResult = onShowSnackbar(tipsText, null)
            if (SnackbarResult.Dismissed == showSnackbarResult) {
                onBookmarkDismiss.invoke()
            }
        }
    }

    Scaffold(
        topBar = {
            CommonTopBar(
                text = stringResource(id = R.string.settings),
                onBackClick = onBackClick,
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        Box(
            modifier = Modifier.padding(paddingValues),
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

                    SettingDialogEnum.CLEAR_PASSWORD -> {
                        // 清除密码
                        ClearPasswordDialog(
                            onConfirmClick = onClearConfirm,
                            onDismissClick = onDialogDismiss,
                        )
                    }
                }
            }
            LazyColumn {
                item {
                    ListItem(
                        modifier = Modifier.padding(top = 16.dp),
                        headlineText = { Text(text = stringResource(id = R.string.mobile_network_download_enable)) },
                        trailingContent = {
                            Switch(
                                checked = mobileNetworkDownloadEnable,
                                onCheckedChange = onMobileNetworkDownloadEnableChanged,
                            )
                        },
                    )
                    Divider(modifier = Modifier.padding(horizontal = 8.dp))
                }
                item {
                    ListItem(
                        modifier = Modifier.padding(top = 16.dp),
                        headlineText = { Text(text = stringResource(id = R.string.need_security_verification_when_launch)) },
                        trailingContent = {
                            Switch(
                                checked = needSecurityVerificationWhenLaunch,
                                onCheckedChange = onNeedSecurityVerificationWhenLaunchChanged,
                            )
                        },
                    )
                    if (hasFingerprint) {
                        ListItem(
                            headlineText = { Text(text = stringResource(id = R.string.enable_fingerprint_verification)) },
                            trailingContent = {
                                Switch(
                                    checked = enableFingerprintVerification,
                                    onCheckedChange = onEnableFingerprintVerificationChanged,
                                )
                            },
                        )
                    }
                    ListItem(
                        modifier = Modifier.clickable { onPasswordClick.invoke() },
                        headlineText = {
                            if (hasPassword) {
                                Text(text = stringResource(id = R.string.modify_password))
                            } else {
                                Text(text = stringResource(id = R.string.create_password))
                            }
                        },
                    )
                    if (hasPassword) {
                        ListItem(
                            modifier = Modifier.clickable { onClearPasswordClick.invoke() },
                            headlineText = { Text(text = stringResource(id = R.string.clear_password)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun CreatePasswordDialog(
    onConfirmClick: (String) -> SettingBookmarkEnum,
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
                            if (result == SettingBookmarkEnum.PASSWORD_ENCODE_FAILED) {
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
    onConfirmClick: (String, String, (SettingBookmarkEnum) -> Unit) -> Unit,
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
                                if (result == SettingBookmarkEnum.PASSWORD_ENCODE_FAILED) {
                                    pwd2Error = true
                                    pwd2SupportText = passwordEncodeFailedText
                                } else if (result == SettingBookmarkEnum.PASSWORD_DECODE_FAILED) {
                                    pwd2Error = false
                                    pwd2SupportText = ""
                                    pwd1Error = true
                                    pwd1SupportText = passwordDecodeFailedText
                                } else if (result == SettingBookmarkEnum.PASSWORD_WRONG) {
                                    pwd2Error = false
                                    pwd2SupportText = ""
                                    pwd1Error = true
                                    pwd1SupportText = passwordWrongText
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
internal fun ClearPasswordDialog(
    onConfirmClick: (String, (SettingBookmarkEnum) -> Unit) -> Unit,
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
                            if (result == SettingBookmarkEnum.PASSWORD_WRONG) {
                                // 密码错误
                                pwdError = true
                                pwdSupportText = passwordWrongText
                            } else if (result == SettingBookmarkEnum.PASSWORD_DECODE_FAILED) {
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
internal fun PasswordTextField(
    modifier: Modifier = Modifier,
    initializedText: String,
    label: String,
    placeholder: String? = null,
    supportingText: String? = null,
    onValueChange: (String) -> Unit,
    onValueVerify: ((String) -> Boolean)? = null,
    isError: Boolean = false,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    var visible by remember {
        mutableStateOf(false)
    }

    CompatTextField(
        modifier = modifier,
        initializedText = initializedText,
        label = label,
        placeholder = placeholder,
        supportingText = supportingText,
        onValueChange = onValueChange,
        onValueVerify = onValueVerify,
        isError = isError,
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = null
                )
            }
        },
        visualTransformation = if (!visible) PasswordVisualTransformation() else VisualTransformation.None,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (!visible) KeyboardType.Password else KeyboardType.Text),
        keyboardActions = keyboardActions,
    )
}