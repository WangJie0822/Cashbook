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
import cn.wj.android.cashbook.core.common.ApplicationInfo
import cn.wj.android.cashbook.core.common.PASSWORD_REGEX
import cn.wj.android.cashbook.core.common.tools.isMatch
import cn.wj.android.cashbook.core.design.component.CashbookScaffold
import cn.wj.android.cashbook.core.design.component.CashbookTopAppBar
import cn.wj.android.cashbook.core.design.component.CompatPasswordTextField
import cn.wj.android.cashbook.core.design.component.TextFieldState
import cn.wj.android.cashbook.core.design.component.TransparentListItem
import cn.wj.android.cashbook.core.design.icon.CashbookIcons
import cn.wj.android.cashbook.core.design.security.biometric.BiometricAuthenticate
import cn.wj.android.cashbook.core.design.security.biometric.BiometricAuthenticateHintData
import cn.wj.android.cashbook.core.design.security.biometric.HW_AVAILABLE
import cn.wj.android.cashbook.core.design.security.biometric.ProvideBiometricAuthenticateHintData
import cn.wj.android.cashbook.core.design.security.biometric.checkBiometric
import cn.wj.android.cashbook.core.design.theme.supportsDynamicTheming
import cn.wj.android.cashbook.core.model.enums.DarkModeEnum
import cn.wj.android.cashbook.core.model.enums.VerificationModeEnum
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
 * @param onRequestNaviToBackupAndRecovery 导航到备份与恢复
 * @param onRequestPopBackStack 导航到上一级
 * @param onShowSnackbar 显示 [androidx.compose.material3.Snackbar]，参数：(显示文本，action文本) -> [SnackbarResult]
 * @param supportFingerprint 是否支持指纹识别
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/19
 */
@Composable
internal fun SettingRoute(
    onRequestNaviToBackupAndRecovery: () -> Unit,
    onRequestPopBackStack: () -> Unit,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,
    modifier: Modifier = Modifier,
    supportFingerprint: Boolean = checkBiometric() == HW_AVAILABLE,
    viewModel: SettingViewModel = hiltViewModel()
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingScreen(
        supportFingerprint = supportFingerprint,
        uiState = uiState,
        shouldDisplayBookmark = viewModel.shouldDisplayBookmark,
        onRequestDismissBookmark = viewModel::dismissBookmark,
        dialogState = viewModel.dialogState,
        onRequestDismissDialog = viewModel::dismissDialog,
        onMobileNetworkDownloadEnableChanged = viewModel::onMobileNetworkDownloadEnableChanged,
        onNeedSecurityVerificationWhenLaunchChanged = viewModel::onNeedSecurityVerificationWhenLaunchChanged,
        onEnableFingerprintVerificationChanged = viewModel::onEnableFingerprintVerificationChanged,
        onPasswordClick = viewModel::onPasswordClick,
        onClearPasswordClick = viewModel::onClearPasswordClick,
        onDarkModeClick = viewModel::onDarkModeClick,
        onDynamicColorClick = viewModel::onDynamicColorClick,
        onVerificationModeClick = viewModel::onVerificationModeClick,
        onCreateConfirmClick = viewModel::onCreateConfirm,
        onModifyConfirmClick = viewModel::onModifyConfirm,
        onVerifyConfirmClick = viewModel::onVerityConfirm,
        onClearConfirmClick = viewModel::onClearConfirm,
        onFingerprintVerifySuccess = viewModel::onFingerprintVerifySuccess,
        onFingerprintVerifyError = viewModel::onFingerprintVerifyError,
        onDarkModeSelected = viewModel::onDarkModeSelected,
        onDynamicColorSelected = viewModel::onDynamicColorSelected,
        onVerificationModeSelected = viewModel::onVerificationModeSelected,
        onBackupAndRecoveryClick = onRequestNaviToBackupAndRecovery,
        onBackClick = onRequestPopBackStack,
        onShowSnackbar = onShowSnackbar,
        modifier = modifier,
    )
}

/**
 * 设置界面
 *
 * @param supportFingerprint 是否支持指纹
 * @param uiState 界面 UI 状态
 * @param shouldDisplayBookmark 是否显示提示
 * @param onRequestDismissBookmark 隐藏提示
 * @param dialogState 弹窗状态
 * @param onRequestDismissDialog 隐藏弹窗
 * @param onMobileNetworkDownloadEnableChanged 允许移动流量开关切换
 * @param onNeedSecurityVerificationWhenLaunchChanged 启动时需要安全验证开关切换
 * @param onVerificationModeClick 认证模式点击回调
 * @param onEnableFingerprintVerificationChanged 允许指纹认证开关切换
 * @param onPasswordClick 密码点击回调
 * @param onClearPasswordClick 清除密码点击回调
 * @param onCreateConfirmClick 创建密码确认点击回调，参数：(密码) -> 创建结果
 * @param onModifyConfirmClick 修改密码确认点击回调，参数：(旧密码，新密码，结果回调(结果[SettingPasswordStateEnum]) -> [Unit]) -> [Unit]
 * @param onClearConfirmClick 清除密码确认点击回调，参数：(密码, 认证回调(认证结果[SettingPasswordStateEnum] -> [Unit])) -> [Unit]
 * @param onVerifyConfirmClick 安全认证确认点击回调，参数：(密码, 认证回调(认证结果[SettingPasswordStateEnum] -> [Unit])) -> [Unit]
 * @param onFingerprintVerifySuccess 指纹认证成功回调，参数：(数据解码对象) -> [Unit]
 * @param onFingerprintVerifyError 指纹认证失败回调，参数：(错误码, 提示文本) -> [Unit]
 * @param onVerificationModeSelected 安全认证方式切换回调
 * @param onDarkModeClick 黑夜模式点击回调
 * @param onDarkModeSelected 黑夜模式切换回调
 * @param onDynamicColorClick 动态配色点击回调
 * @param onDynamicColorSelected 动态配置模式切换回调
 * @param onBackupAndRecoveryClick 备份与恢复点击回调
 * @param onBackClick 返回点击回调
 * @param onShowSnackbar 显示 [androidx.compose.material3.Snackbar]，参数：(显示文本，action文本) -> [SnackbarResult]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingScreen(
    supportFingerprint: Boolean,
    uiState: SettingUiState,
    shouldDisplayBookmark: String,
    onRequestDismissBookmark: () -> Unit,
    dialogState: DialogState,
    onRequestDismissDialog: () -> Unit,
    onMobileNetworkDownloadEnableChanged: (Boolean) -> Unit,
    onNeedSecurityVerificationWhenLaunchChanged: (Boolean) -> Unit,
    onVerificationModeClick: () -> Unit,
    onEnableFingerprintVerificationChanged: (Boolean) -> Unit,
    onPasswordClick: () -> Unit,
    onClearPasswordClick: () -> Unit,
    onCreateConfirmClick: (String) -> SettingPasswordStateEnum,
    onModifyConfirmClick: (String, String, (SettingPasswordStateEnum) -> Unit) -> Unit,
    onClearConfirmClick: (String, (SettingPasswordStateEnum) -> Unit) -> Unit,
    onVerifyConfirmClick: (String, (SettingPasswordStateEnum) -> Unit) -> Unit,
    onFingerprintVerifySuccess: (Cipher) -> Unit,
    onFingerprintVerifyError: (Int, String) -> Unit,
    onVerificationModeSelected: (VerificationModeEnum) -> Unit,
    onDarkModeClick: () -> Unit,
    onDarkModeSelected: (DarkModeEnum) -> Unit,
    onDynamicColorClick: () -> Unit,
    onDynamicColorSelected: (Boolean) -> Unit,
    onBackupAndRecoveryClick: () -> Unit,
    onBackClick: () -> Unit,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,
    modifier: Modifier = Modifier,
) {
    // 显示提示
    LaunchedEffect(shouldDisplayBookmark) {
        if (shouldDisplayBookmark.isNotBlank()) {
            val showSnackbarResult = onShowSnackbar(shouldDisplayBookmark, null)
            if (SnackbarResult.Dismissed == showSnackbarResult) {
                onRequestDismissBookmark.invoke()
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
            supportFingerprint = supportFingerprint,
            uiState = uiState,
            dialogState = dialogState,
            onRequestDismissDialog = onRequestDismissDialog,
            onCreateConfirmClick = onCreateConfirmClick,
            onModifyConfirmClick = onModifyConfirmClick,
            onClearConfirmClick = onClearConfirmClick,
            onVerifyConfirmClick = onVerifyConfirmClick,
            onFingerprintVerifySuccess = onFingerprintVerifySuccess,
            onFingerprintVerifyError = onFingerprintVerifyError,
            onMobileNetworkDownloadEnableChanged = onMobileNetworkDownloadEnableChanged,
            onNeedSecurityVerificationWhenLaunchChanged = onNeedSecurityVerificationWhenLaunchChanged,
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

/**
 * 设置界面内容
 *
 * @param supportFingerprint 是否支持指纹
 * @param uiState 界面 UI 状态
 * @param dialogState 弹窗状态
 * @param onRequestDismissDialog 隐藏弹窗
 * @param onMobileNetworkDownloadEnableChanged 允许移动流量开关切换
 * @param onNeedSecurityVerificationWhenLaunchChanged 启动时需要安全验证开关切换
 * @param onVerificationModeClick 认证模式点击回调
 * @param onEnableFingerprintVerificationChanged 允许指纹认证开关切换
 * @param onPasswordClick 密码点击回调
 * @param onClearPasswordClick 清除密码点击回调
 * @param onCreateConfirmClick 创建密码确认点击回调，参数：(密码) -> 创建结果
 * @param onModifyConfirmClick 修改密码确认点击回调，参数：(旧密码，新密码，结果回调(结果[SettingPasswordStateEnum]) -> [Unit]) -> [Unit]
 * @param onClearConfirmClick 清除密码确认点击回调，参数：(密码, 认证回调(认证结果[SettingPasswordStateEnum] -> [Unit])) -> [Unit]
 * @param onVerifyConfirmClick 安全认证确认点击回调，参数：(密码, 认证回调(认证结果[SettingPasswordStateEnum] -> [Unit])) -> [Unit]
 * @param onFingerprintVerifySuccess 指纹认证成功回调，参数：(数据解码对象) -> [Unit]
 * @param onFingerprintVerifyError 指纹认证失败回调，参数：(错误码, 提示文本) -> [Unit]
 * @param onVerificationModeSelected 安全认证方式切换回调
 * @param onDarkModeClick 黑夜模式点击回调
 * @param onDarkModeSelected 黑夜模式切换回调
 * @param onDynamicColorClick 动态配色点击回调
 * @param onDynamicColorSelected 动态配置模式切换回调
 * @param onBackupAndRecoveryClick 备份与恢复点击回调
 */
@Composable
internal fun SettingContent(
    supportFingerprint: Boolean,
    uiState: SettingUiState,
    dialogState: DialogState,
    onRequestDismissDialog: () -> Unit,
    onMobileNetworkDownloadEnableChanged: (Boolean) -> Unit,
    onNeedSecurityVerificationWhenLaunchChanged: (Boolean) -> Unit,
    onVerificationModeClick: () -> Unit,
    onEnableFingerprintVerificationChanged: (Boolean) -> Unit,
    onPasswordClick: () -> Unit,
    onClearPasswordClick: () -> Unit,
    onCreateConfirmClick: (String) -> SettingPasswordStateEnum,
    onModifyConfirmClick: (String, String, (SettingPasswordStateEnum) -> Unit) -> Unit,
    onClearConfirmClick: (String, (SettingPasswordStateEnum) -> Unit) -> Unit,
    onVerifyConfirmClick: (String, (SettingPasswordStateEnum) -> Unit) -> Unit,
    onFingerprintVerifySuccess: (Cipher) -> Unit,
    onFingerprintVerifyError: (Int, String) -> Unit,
    onVerificationModeSelected: (VerificationModeEnum) -> Unit,
    onDarkModeClick: () -> Unit,
    onDarkModeSelected: (DarkModeEnum) -> Unit,
    onDynamicColorClick: () -> Unit,
    onDynamicColorSelected: (Boolean) -> Unit,
    onBackupAndRecoveryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
    ) {
        DialogContent(
            dialogState = dialogState,
            onRequestDismissDialog = onRequestDismissDialog,
            verificationMode = uiState.verificationMode,
            darkMode = uiState.darkMode,
            dynamicColor = uiState.dynamicColor,
            onCreateConfirmClick = onCreateConfirmClick,
            onModifyConfirmClick = onModifyConfirmClick,
            onVerifyConfirmClick = onVerifyConfirmClick,
            onClearConfirmClick = onClearConfirmClick,
            onFingerprintVerifySuccess = onFingerprintVerifySuccess,
            onFingerprintVerifyError = onFingerprintVerifyError,
            onDarkModeSelected = onDarkModeSelected,
            onDynamicColorSelected = onDynamicColorSelected,
            onVerificationModeSelected = onVerificationModeSelected,
        )

        LazyColumn {
            if (!ApplicationInfo.isOffline) {
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
                                    text = uiState.verificationMode.text,
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
                if (uiState.needSecurityVerificationWhenLaunch && supportFingerprint) {
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
                                text = uiState.darkMode.text,
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

/**
 * 弹窗内容
 *
 *
 *
 * @param dialogState 弹窗状态
 * @param onRequestDismissDialog 隐藏弹窗
 * @param verificationMode 安全验证模式
 * @param darkMode 黑夜模式
 * @param dynamicColor 动态配色模式
 * @param onCreateConfirmClick 创建密码确认点击回调，参数：(密码) -> 创建结果
 * @param onModifyConfirmClick 修改密码确认点击回调，参数：(旧密码，新密码，结果回调(结果[SettingPasswordStateEnum]) -> [Unit]) -> [Unit]
 * @param onClearConfirmClick 清除密码确认点击回调，参数：(密码, 认证回调(认证结果[SettingPasswordStateEnum] -> [Unit])) -> [Unit]
 * @param onVerifyConfirmClick 安全认证确认点击回调，参数：(密码, 认证回调(认证结果[SettingPasswordStateEnum] -> [Unit])) -> [Unit]
 * @param onFingerprintVerifySuccess 指纹认证成功回调，参数：(数据解码对象) -> [Unit]
 * @param onFingerprintVerifyError 指纹认证失败回调，参数：(错误码, 提示文本) -> [Unit]
 * @param onVerificationModeSelected 安全认证方式切换回调
 * @param onDarkModeSelected 黑夜模式切换回调
 * @param onDynamicColorSelected 动态配置模式切换回调
 */
@Composable
internal fun DialogContent(
    dialogState: DialogState,
    onRequestDismissDialog: () -> Unit,
    verificationMode: VerificationModeEnum,
    darkMode: DarkModeEnum,
    dynamicColor: Boolean,
    onCreateConfirmClick: (String) -> SettingPasswordStateEnum,
    onModifyConfirmClick: (String, String, (SettingPasswordStateEnum) -> Unit) -> Unit,
    onVerifyConfirmClick: (String, (SettingPasswordStateEnum) -> Unit) -> Unit,
    onClearConfirmClick: (String, (SettingPasswordStateEnum) -> Unit) -> Unit,
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
                    onConfirmClick = onCreateConfirmClick,
                    onDismissClick = onRequestDismissDialog,
                )
            }

            SettingDialogEnum.MODIFY_PASSWORD -> {
                // 修改密码
                ModifyPasswordDialog(
                    onConfirmClick = onModifyConfirmClick,
                    onDismissClick = onRequestDismissDialog,
                )
            }

            SettingDialogEnum.VERIFY_PASSWORD -> {
                // 验证密码
                VerityPasswordDialog(
                    onConfirmClick = onVerifyConfirmClick,
                    onDismissClick = onRequestDismissDialog,
                )
            }

            SettingDialogEnum.CLEAR_PASSWORD -> {
                // 清除密码
                ClearPasswordDialog(
                    onConfirmClick = onClearConfirmClick,
                    onDismissClick = onRequestDismissDialog,
                )
            }

            SettingDialogEnum.DARK_MODE -> {
                // 黑夜模式
                DarkModeDialog(
                    darkMode = darkMode,
                    onDarkModeSelected = onDarkModeSelected,
                    onDismissClick = onRequestDismissDialog,
                )
            }

            SettingDialogEnum.DYNAMIC_COLOR -> {
                // 动态配色
                DynamicColorDialog(
                    dynamicColor = dynamicColor,
                    onDynamicColorSelected = onDynamicColorSelected,
                    onDismissClick = onRequestDismissDialog,
                )
            }

            SettingDialogEnum.VERIFICATION_MODE -> {
                // 验证模式
                VerificationModeDialog(
                    verificationMode = verificationMode,
                    onVerificationModeSelected = onVerificationModeSelected,
                    onDismissClick = onRequestDismissDialog,
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

/**
 * 创建密码弹窗
 *
 * @param onConfirmClick 确认点击回调
 * @param onDismissClick 隐藏弹窗
 */
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

    var pwdState by remember {
        mutableStateOf(SettingPasswordStateEnum.SUCCESS)
    }

    val password = remember {
        TextFieldState(
            validator = { it.isMatch(PASSWORD_REGEX) },
            errorFor = {
                if (it.isBlank()) {
                    passwordMustNotBeBlankText
                } else {
                    passwordFormatErrorText
                }
            }
        )
    }

    val passwordAgain = remember {
        TextFieldState(
            validator = { it.isMatch(PASSWORD_REGEX) && it == password.text && pwdState != SettingPasswordStateEnum.PASSWORD_ENCODE_FAILED },
            errorFor = {
                when {
                    it.isEmpty() -> passwordMustNotBeBlankText
                    !it.isMatch(PASSWORD_REGEX) -> passwordFormatErrorText
                    it != password.text -> passwordConfirmFailedText
                    pwdState == SettingPasswordStateEnum.PASSWORD_ENCODE_FAILED -> passwordEncodeFailedText
                    else -> ""
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismissClick,
        title = { Text(text = stringResource(id = R.string.create_password)) },
        text = {
            Column {
                CompatPasswordTextField(
                    textFieldState = password,
                    label = { Text(text = stringResource(id = R.string.new_password)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                )
                CompatPasswordTextField(
                    textFieldState = passwordAgain,
                    label = { Text(text = stringResource(id = R.string.confirm_password)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (password.isValid && passwordAgain.isValid) {
                    pwdState = onConfirmClick(password.text)
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

/**
 * 修改密码弹窗
 *
 * @param onConfirmClick 确认点击回调
 * @param onDismissClick 隐藏弹窗
 */
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

    var pwdState by remember {
        mutableStateOf(SettingPasswordStateEnum.SUCCESS)
    }

    val passwordOld = remember {
        TextFieldState(
            validator = {
                it.isMatch(PASSWORD_REGEX)
                        && pwdState != SettingPasswordStateEnum.PASSWORD_WRONG
                        && pwdState != SettingPasswordStateEnum.PASSWORD_DECODE_FAILED
            },
            errorFor = {
                when {
                    it.isBlank() -> passwordMustNotBeBlankText
                    !it.isMatch(PASSWORD_REGEX) -> passwordFormatErrorText
                    pwdState == SettingPasswordStateEnum.PASSWORD_DECODE_FAILED -> passwordDecodeFailedText
                    pwdState == SettingPasswordStateEnum.PASSWORD_WRONG -> passwordWrongText
                    else -> ""
                }
            }
        )
    }

    val passwordNew = remember {
        TextFieldState(
            validator = { it.isMatch(PASSWORD_REGEX) && it != passwordOld.text },
            errorFor = {
                when {
                    it.isEmpty() -> passwordEncodeFailedText
                    !it.isMatch(PASSWORD_REGEX) -> passwordFormatErrorText
                    it == passwordOld.text -> passwordCannotBeSameText
                    pwdState == SettingPasswordStateEnum.PASSWORD_ENCODE_FAILED -> passwordEncodeFailedText
                    else -> ""
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismissClick,
        title = { Text(text = stringResource(id = R.string.modify_password)) },
        text = {
            Column {
                CompatPasswordTextField(
                    textFieldState = passwordOld,
                    label = { Text(text = stringResource(id = R.string.old_password)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                )
                CompatPasswordTextField(
                    textFieldState = passwordNew,
                    label = { Text(text = stringResource(id = R.string.new_password)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (passwordOld.isValid && passwordNew.isValid) {
                    onConfirmClick.invoke(passwordOld.text, passwordNew.text) { result ->
                        pwdState = result
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

/**
 * 验证密码弹窗
 *
 * @param onConfirmClick 确认点击回调
 * @param onDismissClick 隐藏弹窗
 */
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

    var pwdState by remember {
        mutableStateOf(SettingPasswordStateEnum.SUCCESS)
    }

    val password = remember {
        TextFieldState(
            validator = {
                it.isMatch(PASSWORD_REGEX)
                        && pwdState != SettingPasswordStateEnum.PASSWORD_WRONG
                        && pwdState != SettingPasswordStateEnum.PASSWORD_DECODE_FAILED
            },
            errorFor = {
                when {
                    it.isBlank() -> passwordMustNotBeBlankText
                    !it.isMatch(PASSWORD_REGEX) -> passwordFormatErrorText
                    pwdState == SettingPasswordStateEnum.PASSWORD_DECODE_FAILED -> passwordDecodeFailedText
                    pwdState == SettingPasswordStateEnum.PASSWORD_WRONG -> passwordWrongText
                    else -> ""
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismissClick,
        title = { Text(text = stringResource(id = R.string.verity_password)) },
        text = {
            Column {
                CompatPasswordTextField(
                    textFieldState = password,
                    label = { Text(text = stringResource(id = R.string.please_enter_password)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (password.isValid) {
                    onConfirmClick(password.text) { result ->
                        pwdState = result
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

/**
 * 清除密码弹窗
 *
 * @param onConfirmClick 确认点击回调
 * @param onDismissClick 隐藏弹窗
 */
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

    var pwdState by remember {
        mutableStateOf(SettingPasswordStateEnum.SUCCESS)
    }

    val password = remember {
        TextFieldState(
            validator = {
                it.isMatch(PASSWORD_REGEX)
                        && pwdState != SettingPasswordStateEnum.PASSWORD_WRONG
                        && pwdState != SettingPasswordStateEnum.PASSWORD_DECODE_FAILED
            },
            errorFor = {
                when {
                    it.isBlank() -> passwordMustNotBeBlankText
                    !it.isMatch(PASSWORD_REGEX) -> passwordFormatErrorText
                    pwdState == SettingPasswordStateEnum.PASSWORD_DECODE_FAILED -> passwordDecodeFailedText
                    pwdState == SettingPasswordStateEnum.PASSWORD_WRONG -> passwordWrongText
                    else -> ""
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismissClick,
        title = { Text(text = stringResource(id = R.string.clear_password)) },
        text = {
            Column {
                CompatPasswordTextField(
                    textFieldState = password,
                    label = { Text(text = stringResource(id = R.string.please_enter_password)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (password.isValid) {
                    onConfirmClick(password.text) { result ->
                        pwdState = result
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

/**
 * 安全验证模式选择弹窗
 *
 * @param verificationMode 当前选择模式
 * @param onVerificationModeSelected 模式选择回调
 * @param onDismissClick 隐藏弹窗
 */
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
                VerificationModeEnum.entries.forEach { enum ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .selectable(
                                selected = (enum == verificationMode),
                                onClick = { onVerificationModeSelected.invoke(enum) },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = enum == verificationMode, onClick = null)
                        Text(
                            text = enum.text,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
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

/**
 * 黑夜模式选择弹窗
 *
 * @param darkMode 当前选择模式
 * @param onDarkModeSelected 模式选择回调
 * @param onDismissClick 隐藏弹窗
 */
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
                DarkModeEnum.entries.forEach { enum ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .selectable(
                                selected = (enum == darkMode),
                                onClick = { onDarkModeSelected.invoke(enum) },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = enum == darkMode, onClick = null)
                        Text(
                            text = enum.text,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
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

/**
 * 动态配色模式选择弹窗
 *
 * @param dynamicColor 当前选择模式
 * @param onDynamicColorSelected 模式选择回调
 * @param onDismissClick 隐藏弹窗
 */
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
                            selected = dynamicColor,
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
                            selected = !dynamicColor,
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

/** 枚举对应文本 */
internal val DarkModeEnum.text: String
    @Composable get() = stringResource(
        id = when (this) {
            DarkModeEnum.FOLLOW_SYSTEM -> R.string.follow_system
            DarkModeEnum.LIGHT -> R.string.light_mode
            DarkModeEnum.DARK -> R.string.dark_mode
        }
    )

/** 枚举对应文本 */
internal val VerificationModeEnum.text: String
    @Composable get() = stringResource(
        id = when (this) {
            VerificationModeEnum.WHEN_LAUNCH -> R.string.each_launch
            VerificationModeEnum.WHEN_FOREGROUND -> R.string.each_foreground
        }
    )