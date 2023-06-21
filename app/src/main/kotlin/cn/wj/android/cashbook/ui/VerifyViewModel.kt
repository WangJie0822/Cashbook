package cn.wj.android.cashbook.ui

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
import cn.wj.android.cashbook.enums.MainBookmarkEnum
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
 * 验证 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/20
 */
@HiltViewModel
class VerifyViewModel @Inject constructor(
    settingRepository: SettingRepository,
) : ViewModel() {

    private val veritied = MutableStateFlow(false)

    var firstOpen by mutableStateOf(true)

    var shouldDisplayBookmark by mutableStateOf(MainBookmarkEnum.NONE)

    var dialogState by mutableStateOf<DialogState>(DialogState.Dismiss)

    val needVerity = combine(settingRepository.appDataMode, veritied) { appData, veritied ->
        appData.needSecurityVerificationWhenLaunch && !veritied
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = true,
        )

    val supportFingerprint = settingRepository.appDataMode
        .mapLatest {
            it.enableFingerprintVerification
        }
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
            this@VerifyViewModel.logger()
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

            // 密码正确，进入首页
            veritied.tryEmit(true)
        }
    }

    fun onFingerprintClick() {
        firstOpen = false
        viewModelScope.launch {
            val bytes = fingerprintIv.first().hexToBytes()
            if (null == bytes) {
                shouldDisplayBookmark = MainBookmarkEnum.PASSWORD_DECODE_FAILED
                return@launch
            }
            // 指纹认证
            dialogState = DialogState.Shown(loadDecryptCipher(KEY_ALIAS_FINGERPRINT, bytes))
        }
    }

    fun onFingerprintVerifySuccess(cipher: Cipher) {
        viewModelScope.launch {
            dismissDialog()

            this@VerifyViewModel.logger().i("onFingerprintVerifySuccess(cipher)")

            // 获取密码信息
            val passwordIv = passwordIv.first()
            val bytes = passwordIv.hexToBytes()
            if (null == bytes) {
                shouldDisplayBookmark = MainBookmarkEnum.PASSWORD_DECODE_FAILED
                return@launch
            }
            val pwdCipher = loadDecryptCipher(KEY_ALIAS_PASSWORD, bytes)
            val pwdSha = pwdCipher.doFinal(passwordInfo.first().hexToBytes()).decodeToString()
            // 获取指纹密码信息
            val fingerprintPwdSha =
                cipher.doFinal(fingerprintPasswordInfo.first().hexToBytes()).decodeToString()

            if (pwdSha != fingerprintPwdSha) {
                // 密码错误
                shouldDisplayBookmark = MainBookmarkEnum.PASSWORD_WRONG
                return@launch
            }

            // 密码正确，进入首页
            veritied.tryEmit(true)
        }
    }

    var errorText = ""

    fun onFingerprintVerifyError(code: Int, msg: String) {
        logger().i("onFingerprintVerifyError(code = <$code>, msg = <$msg>)")
        dismissDialog()
        errorText = msg
        shouldDisplayBookmark = MainBookmarkEnum.ERROR
    }

    fun onBookmarkDismiss() {
        shouldDisplayBookmark = MainBookmarkEnum.NONE
    }

    fun onActivityStop() {
        viewModelScope.launch {
            if (verificationMode.first() == VerificationModeEnum.WHEN_FOREGROUND) {
                veritied.tryEmit(false)
                firstOpen = true
            }
        }
    }

    private fun dismissDialog() {
        dialogState = DialogState.Dismiss
    }
}