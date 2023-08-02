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
import cn.wj.android.cashbook.core.design.security.shaEncode
import cn.wj.android.cashbook.core.model.enums.VerificationModeEnum
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.domain.usecase.GetCurrentBookUseCase
import cn.wj.android.cashbook.feature.settings.enums.LauncherBookmarkEnum
import cn.wj.android.cashbook.feature.settings.enums.SettingPasswordStateEnum
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.crypto.Cipher
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 首页 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/26
 */
@HiltViewModel
class LauncherViewModel @Inject constructor(
    private val settingRepository: SettingRepository,
    getCurrentBookUseCase: GetCurrentBookUseCase,
) : ViewModel() {

    private val verified = MutableStateFlow(false)

    var firstOpen by mutableStateOf(true)
        private set

    /** 是否显示抽屉菜单 */
    var shouldDisplayDrawerSheet by mutableStateOf(false)
        private set

    var shouldDisplayBookmark by mutableStateOf(LauncherBookmarkEnum.NONE)
        private set

    var dialogState by mutableStateOf<DialogState>(DialogState.Dismiss)
        private set

    val uiState = combine(settingRepository.appDataMode, verified) { appData, verified ->
        LauncherUiState.Success(
            needRequestProtocol = !appData.agreedProtocol,
            needVerity = appData.needSecurityVerificationWhenLaunch && !verified,
            supportFingerprint = appData.enableFingerprintVerification,
            currentBookName = getCurrentBookUseCase().first().name
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = LauncherUiState.Loading,
        )

    /** 密码加密向量信息 */
    private val passwordIv = settingRepository.appDataMode
        .mapLatest { it.passwordIv }

    /** 密码信息 */
    private val passwordInfo = settingRepository.appDataMode
        .mapLatest { it.passwordInfo }

    /** 密码加密向量信息 */
    private val fingerprintIv = settingRepository.appDataMode
        .mapLatest { it.fingerprintIv }

    /** 密码信息 */
    private val fingerprintPasswordInfo = settingRepository.appDataMode
        .mapLatest { it.fingerprintPasswordInfo }

    /** 验证模式 */
    private val verificationMode = settingRepository.appDataMode
        .mapLatest { it.verificationModel }

    fun onVerityConfirm(pwd: String, callback: (SettingPasswordStateEnum) -> Unit) {
        viewModelScope.launch {
            // 使用 AndroidKeyStore 解密密码信息
            val passwordIv = passwordIv.first()
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

            // 密码正确，进入首页
            verified.tryEmit(true)
        }
    }

    fun onFingerprintClick() {
        firstOpen = false
        viewModelScope.launch {
            val bytes = fingerprintIv.first().hexToBytes()
            if (null == bytes) {
                shouldDisplayBookmark = LauncherBookmarkEnum.PASSWORD_DECODE_FAILED
                return@launch
            }
            // 指纹认证
            dialogState = DialogState.Shown(loadDecryptCipher(KEY_ALIAS_FINGERPRINT, bytes))
        }
    }

    fun onFingerprintVerifySuccess(cipher: Cipher) {
        viewModelScope.launch {
            dismissDialog()

            // 获取密码信息
            val passwordIv = passwordIv.first()
            val bytes = passwordIv.hexToBytes()
            if (null == bytes) {
                shouldDisplayBookmark = LauncherBookmarkEnum.PASSWORD_DECODE_FAILED
                return@launch
            }
            val pwdCipher = loadDecryptCipher(KEY_ALIAS_PASSWORD, bytes)
            val pwdSha = pwdCipher.doFinal(passwordInfo.first().hexToBytes()).decodeToString()
            // 获取指纹密码信息
            val fingerprintPwdSha =
                cipher.doFinal(fingerprintPasswordInfo.first().hexToBytes()).decodeToString()

            if (pwdSha != fingerprintPwdSha) {
                // 密码错误
                shouldDisplayBookmark = LauncherBookmarkEnum.PASSWORD_WRONG
                return@launch
            }

            // 密码正确，进入首页
            verified.tryEmit(true)
        }
    }

    var errorText = ""
        private set

    fun onFingerprintVerifyError(code: Int, msg: String) {
        logger().i("onFingerprintVerifyError(code = <$code>, msg = <$msg>)")
        dismissDialog()
        errorText = msg
        shouldDisplayBookmark = LauncherBookmarkEnum.ERROR
    }

    fun onActivityStop() {
        viewModelScope.launch {
            if (verificationMode.first() == VerificationModeEnum.WHEN_FOREGROUND) {
                verified.tryEmit(false)
                firstOpen = true
            }
        }
    }

    fun agreeProtocol() {
        viewModelScope.launch {
            settingRepository.updateAgreedProtocol(true)
        }
    }

    fun displayDrawerSheet() {
        shouldDisplayDrawerSheet = true
    }

    fun dismissDrawerSheet() {
        shouldDisplayDrawerSheet = false
    }

    fun dismissBookmark() {
        shouldDisplayBookmark = LauncherBookmarkEnum.NONE
    }

    private fun dismissDialog() {
        dialogState = DialogState.Dismiss
    }
}

sealed interface LauncherUiState {
    object Loading : LauncherUiState
    data class Success(
        val needRequestProtocol: Boolean,
        val needVerity: Boolean,
        val supportFingerprint: Boolean,
        val currentBookName: String,
    ) : LauncherUiState
}