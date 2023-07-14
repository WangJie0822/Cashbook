package cn.wj.android.cashbook.feature.settings.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.common.KEY_ALIAS_FINGERPRINT
import cn.wj.android.cashbook.core.common.KEY_ALIAS_PASSWORD
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.data.repository.SettingRepository
import cn.wj.android.cashbook.core.design.security.hexToBytes
import cn.wj.android.cashbook.core.design.security.loadDecryptCipher
import cn.wj.android.cashbook.core.design.security.loadEncryptCipher
import cn.wj.android.cashbook.core.design.security.shaEncode
import cn.wj.android.cashbook.core.design.security.toHexString
import cn.wj.android.cashbook.core.model.enums.DarkModeEnum
import cn.wj.android.cashbook.core.model.enums.VerificationModeEnum
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.feature.settings.enums.SettingDialogEnum
import cn.wj.android.cashbook.feature.settings.enums.SettingPasswordStateEnum
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.crypto.Cipher
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 设置 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/19
 */
@HiltViewModel
class SettingViewModel @Inject constructor(
    private val settingRepository: SettingRepository,
) : ViewModel() {

    /** 弹窗状态 */
    var dialogState by mutableStateOf<DialogState>(DialogState.Dismiss)
        private set

    /** 是否需要显示提示 */
    var shouldDisplayBookmark by mutableStateOf("")
        private set

    val uiState = settingRepository.appDataMode
        .mapLatest {
            SettingUiState.Success(
                mobileNetworkDownloadEnable = it.mobileNetworkDownloadEnable,
                needSecurityVerificationWhenLaunch = it.needSecurityVerificationWhenLaunch,
                verificationMode = it.verificationModel,
                enableFingerprintVerification = it.enableFingerprintVerification,
                hasPassword = it.passwordInfo.isNotBlank(),
                darkMode = it.darkMode,
                dynamicColor = it.dynamicColor,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = SettingUiState.Loading,
        )

    /** 密码加密向量信息 */
    private val passwordIv = settingRepository.appDataMode
        .mapLatest { it.passwordIv }

    /** 密码信息 */
    private val passwordInfo = settingRepository.appDataMode
        .mapLatest { it.passwordInfo }

    /** 是否有密码 */
    private val hasPassword = passwordInfo
        .mapLatest { it.isNotBlank() }

    fun onMobileNetworkDownloadEnableChanged(enable: Boolean) {
        viewModelScope.launch {
            settingRepository.updateMobileNetworkDownloadEnable(enable)
        }
    }

    fun onNeedSecurityVerificationWhenLaunchChanged(need: Boolean) {
        viewModelScope.launch {
            if (!need) {
                // 关闭验证开关，同时关闭指纹验证开关
                settingRepository.apply {
                    updateNeedSecurityVerificationWhenLaunch(false)
                    updateEnableFingerprintVerification(false)
                }
            } else if (hasPassword.first()) {
                // 有密码，直接开启开关
                settingRepository.updateNeedSecurityVerificationWhenLaunch(true)
            } else {
                // 没有密码，显示创建密码弹窗
                onPasswordClick()
            }
        }
    }

    fun onEnableFingerprintVerificationChanged(enable: Boolean) {
        viewModelScope.launch {
            settingRepository.updateEnableFingerprintVerification(enable)
        }
    }

    fun onPasswordClick() {
        viewModelScope.launch {
            dialogState = if (hasPassword.first()) {
                // 当前有密码，修改密码
                DialogState.Shown(SettingDialogEnum.MODIFY_PASSWORD)
            } else {
                // 当前没有密码，创建密码
                DialogState.Shown(SettingDialogEnum.CREATE_PASSWORD)
            }
        }
    }

    fun onCreateConfirm(pwd: String): SettingPasswordStateEnum {
        // 使用 AndroidKeyStore 进行保存
        val cipher = loadEncryptCipher(KEY_ALIAS_PASSWORD)
        val passwordInfo = cipher.doFinal(pwd.shaEncode().toByteArray()).toHexString()
        val passwordIv = cipher.iv.toHexString()
        logger().i("onCreateConfirm(pwd), passwordInfo = <$passwordInfo>, passwordIv = <$passwordIv>")
        viewModelScope.launch {
            // 保存密码信息
            settingRepository.updatePasswordInfo(passwordInfo)
            // 保存密码向量信息
            settingRepository.updatePasswordIv(passwordIv)
            // 隐藏弹窗
            dismissDialog()
        }
        return SettingPasswordStateEnum.SUCCESS
    }

    fun onModifyConfirm(
        oldPwd: String,
        newPwd: String,
        callback: (SettingPasswordStateEnum) -> Unit
    ) {
        viewModelScope.launch {
            // 使用 AndroidKeyStore 解密密码信息
            val passwordIv = passwordIv.first()
            this@SettingViewModel.logger()
                .i("onModifyConfirm(oldPwd = <$oldPwd>, newPwd = <$newPwd>), passwordIv = <$passwordIv>")
            val bytes = passwordIv.hexToBytes()
            if (null == bytes) {
                callback.invoke(SettingPasswordStateEnum.PASSWORD_DECODE_FAILED)
                return@launch
            }
            val cipher = loadDecryptCipher(KEY_ALIAS_PASSWORD, bytes)
            val pwdSha = cipher.doFinal(passwordInfo.first().hexToBytes()).decodeToString()
            if (oldPwd.shaEncode() == pwdSha) {
                // 密码正确，保存新密码
                val stateEnum = onCreateConfirm(newPwd)
                if (stateEnum == SettingPasswordStateEnum.SUCCESS) {
                    // 保存成功，清除指纹信息
                    settingRepository.updateFingerprintIv("")
                    settingRepository.updateFingerprintPasswordInfo("")
                    settingRepository.updateEnableFingerprintVerification(false)
                }
                callback.invoke(stateEnum)
            } else {
                // 密码错误，提示
                callback.invoke(SettingPasswordStateEnum.PASSWORD_WRONG)
            }
        }
    }

    /** 指纹验证密码信息 */
    private var printFingerprintPwdSha = ""

    fun onVerityConfirm(pwd: String, callback: (SettingPasswordStateEnum) -> Unit) {
        viewModelScope.launch {
            // 使用 AndroidKeyStore 解密密码信息
            val passwordIv = passwordIv.first()
            this@SettingViewModel.logger()
                .i("onVerityConfirm(pwd = <$pwd>), passwordIv = <$passwordIv>")
            val bytes = passwordIv.hexToBytes()
            if (null == bytes) {
                callback.invoke(SettingPasswordStateEnum.PASSWORD_DECODE_FAILED)
                return@launch
            }
            val cipher = loadDecryptCipher(KEY_ALIAS_PASSWORD, bytes)
            val pwdSha = cipher.doFinal(passwordInfo.first().hexToBytes()).decodeToString()
            if (pwd.shaEncode() != pwdSha) {
                // 密码错误，提示
                callback.invoke(SettingPasswordStateEnum.PASSWORD_WRONG)
                return@launch
            }

            // 密码正确，隐藏弹窗
            printFingerprintPwdSha = pwdSha
            dismissDialog()
            // 调用指纹加密
            dialogState = DialogState.Shown(loadEncryptCipher(KEY_ALIAS_FINGERPRINT))
        }
    }

    fun onClearConfirm(pwd: String, callback: (SettingPasswordStateEnum) -> Unit) {
        viewModelScope.launch {
            // 使用 AndroidKeyStore 解密密码信息
            val passwordIv = passwordIv.first()
            this@SettingViewModel.logger()
                .i("onClearConfirm(pwd = <$pwd>), passwordIv = <$passwordIv>")
            val bytes = passwordIv.hexToBytes()
            if (null == bytes) {
                callback.invoke(SettingPasswordStateEnum.PASSWORD_DECODE_FAILED)
                return@launch
            }
            val cipher = loadDecryptCipher(KEY_ALIAS_PASSWORD, bytes)
            val pwdSha = cipher.doFinal(passwordInfo.first().hexToBytes()).decodeToString()
            if (pwd.shaEncode() != pwdSha) {
                // 密码错误，提示
                callback.invoke(SettingPasswordStateEnum.PASSWORD_WRONG)
                return@launch
            }

            // 密码正确，清除密码
            settingRepository.updatePasswordInfo("")
            settingRepository.updatePasswordIv("")
            // 关闭安全验证开关
            settingRepository.updateNeedSecurityVerificationWhenLaunch(false)
            // 关闭指纹验证
            settingRepository.updateEnableFingerprintVerification(false)
            settingRepository.updateFingerprintPasswordInfo("")
            settingRepository.updateFingerprintIv("")
            // 隐藏弹窗
            dismissDialog()
            callback.invoke(SettingPasswordStateEnum.SUCCESS)
        }
    }

    fun onFingerprintVerifySuccess(cipher: Cipher) {
        dismissDialog()
        val fingerprintPasswordInfo =
            cipher.doFinal(printFingerprintPwdSha.toByteArray()).toHexString()
        val fingerprintPasswordIv = cipher.iv.toHexString()
        logger().i("onFingerprintVerifySuccess(cipher), fingerprintPasswordInfo = <$fingerprintPasswordInfo>, fingerprintPasswordIv = <$fingerprintPasswordIv>")
        viewModelScope.launch {
            // 保存密码信息
            settingRepository.updateFingerprintPasswordInfo(fingerprintPasswordInfo)
            // 保存密码向量信息
            settingRepository.updateFingerprintIv(fingerprintPasswordIv)
            // 打开开关
            settingRepository.updateEnableFingerprintVerification(true)
            printFingerprintPwdSha = ""
        }
    }

    fun onFingerprintVerifyError(code: Int, msg: String) {
        logger().i("onFingerprintVerifyError(code = <$code>, msg = <$msg>)")
        dismissDialog()
        printFingerprintPwdSha = ""
        shouldDisplayBookmark = msg
    }

    fun onDarkModeSelected(darkMode: DarkModeEnum) {
        viewModelScope.launch {
            settingRepository.updateDarkMode(darkMode)
        }
    }

    fun onDynamicColorSelected(dynamicColor: Boolean) {
        viewModelScope.launch {
            settingRepository.updateDynamicColor(dynamicColor)
        }
    }

    fun onVerificationModeSelected(verificationMode: VerificationModeEnum) {
        viewModelScope.launch {
            settingRepository.updateVerificationMode(verificationMode)
        }
    }

    fun onClearPasswordClick() {
        dialogState = DialogState.Shown(SettingDialogEnum.CLEAR_PASSWORD)
    }

    fun onDarkModeClick() {
        dialogState = DialogState.Shown(SettingDialogEnum.DARK_MODE)
    }

    fun onDynamicColorClick() {
        dialogState = DialogState.Shown(SettingDialogEnum.DYNAMIC_COLOR)
    }

    fun onVerificationModeClick() {
        dialogState = DialogState.Shown(SettingDialogEnum.VERIFICATION_MODE)
    }

    fun dismissDialog() {
        dialogState = DialogState.Dismiss
    }

    fun dismissBookmark() {
        shouldDisplayBookmark = ""
    }
}

sealed class SettingUiState(
    open val mobileNetworkDownloadEnable: Boolean = false,
    open val needSecurityVerificationWhenLaunch: Boolean = false,
    open val verificationMode: VerificationModeEnum = VerificationModeEnum.WHEN_LAUNCH,
    open val enableFingerprintVerification: Boolean = false,
    open val hasPassword: Boolean = false,
    open val darkMode: DarkModeEnum = DarkModeEnum.FOLLOW_SYSTEM,
    open val dynamicColor: Boolean = false,
) {
    object Loading : SettingUiState()

    data class Success(
        override val mobileNetworkDownloadEnable: Boolean,
        override val needSecurityVerificationWhenLaunch: Boolean,
        override val verificationMode: VerificationModeEnum,
        override val enableFingerprintVerification: Boolean,
        override val hasPassword: Boolean,
        override val darkMode: DarkModeEnum,
        override val dynamicColor: Boolean,
    ) : SettingUiState()
}