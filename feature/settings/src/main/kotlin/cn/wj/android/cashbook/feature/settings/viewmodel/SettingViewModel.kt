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

    /** 是否需要显示提示 */
    var shouldDisplayBookmark by mutableStateOf("")

    /** 是否允许流量下载 */
    val mobileNetworkDownloadEnable = settingRepository.appDataMode
        .mapLatest { it.mobileNetworkDownloadEnable }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = false,
        )

    /** 启动时是否需要安全验证 */
    val needSecurityVerificationWhenLaunch = settingRepository.appDataMode
        .mapLatest { it.needSecurityVerificationWhenLaunch }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = false,
        )

    /** 安全验证类型 */
    val verificationMode = settingRepository.appDataMode
        .mapLatest { it.verificationModel }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = VerificationModeEnum.WHEN_LAUNCH,
        )

    /** 是否允许指纹认证 */
    val enableFingerprintVerification = settingRepository.appDataMode
        .mapLatest { it.enableFingerprintVerification }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = false,
        )

    /** 密码加密向量信息 */
    private val passwordIv = settingRepository.appDataMode
        .mapLatest { it.passwordIv }

    /** 密码信息 */
    private val passwordInfo = settingRepository.appDataMode
        .mapLatest { it.passwordInfo }

    /** 是否有密码 */
    val hasPassword = passwordInfo
        .mapLatest { it.isNotBlank() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = false,
        )

    /** 黑夜模式 */
    val darkMode = settingRepository.appDataMode
        .mapLatest { it.darkMode }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = DarkModeEnum.FOLLOW_SYSTEM,
        )

    /** 动态配色 */
    val dynamicColor = settingRepository.appDataMode
        .mapLatest { it.dynamicColor }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = false,
        )

    fun onMobileNetworkDownloadEnableChanged(enable: Boolean) {
        viewModelScope.launch {
            settingRepository.updateMobileNetworkDownloadEnable(enable)
        }
    }

    fun onNeedSecurityVerificationWhenLaunchChanged(need: Boolean) {
        viewModelScope.launch {
            if (need && !hasPassword.value) {
                // 开启验证但是没有密码，显示创建密码弹窗
                onPasswordClick()
            } else {
                // 有密码，更新开关
                settingRepository.updateNeedSecurityVerificationWhenLaunch(need)
            }
        }
    }

    fun onEnableFingerprintVerificationChanged(enable: Boolean) {
        viewModelScope.launch {
            if (enable) {
                // 开启指纹识别，需要先认证密码
                dialogState = DialogState.Shown(SettingDialogEnum.VERIFY_PASSWORD)
            } else {
                // 关闭指纹识别
                settingRepository.updateEnableFingerprintVerification(false)
            }
        }
    }

    fun onPasswordClick() {
        dialogState = if (hasPassword.value) {
            // 当前有密码，修改密码
            DialogState.Shown(SettingDialogEnum.MODIFY_PASSWORD)
        } else {
            // 当前没有密码，创建密码
            DialogState.Shown(SettingDialogEnum.CREATE_PASSWORD)
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