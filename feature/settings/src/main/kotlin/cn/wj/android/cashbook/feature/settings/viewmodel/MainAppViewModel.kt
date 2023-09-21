package cn.wj.android.cashbook.feature.settings.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.common.KEY_ALIAS_FINGERPRINT
import cn.wj.android.cashbook.core.common.KEY_ALIAS_PASSWORD
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.data.repository.BooksRepository
import cn.wj.android.cashbook.core.data.repository.SettingRepository
import cn.wj.android.cashbook.core.design.security.hexToBytes
import cn.wj.android.cashbook.core.design.security.loadDecryptCipher
import cn.wj.android.cashbook.core.design.security.shaEncode
import cn.wj.android.cashbook.core.model.enums.VerificationModeEnum
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.feature.settings.enums.MainAppBookmarkEnum
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
 * @param settingRepository 设置相关数据仓库
 * @param booksRepository 账本相关数据仓库
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/26
 */
@HiltViewModel
class MainAppViewModel @Inject constructor(
    private val settingRepository: SettingRepository,
    booksRepository: BooksRepository,
) : ViewModel() {

    /** 标记 - 是否已认证 */
    private val _verified = MutableStateFlow(false)

    var firstOpen by mutableStateOf(true)
        private set

    /** 是否显示提示 */
    var shouldDisplayBookmark by mutableStateOf(MainAppBookmarkEnum.NONE)
        private set

    /** 弹窗状态 */
    var dialogState by mutableStateOf<DialogState>(DialogState.Dismiss)
        private set

    /** 界面 UI 状态 */
    val uiState = combine(settingRepository.appDataMode, _verified) { appData, verified ->
        MainAppUiState.Success(
            needRequestProtocol = !appData.agreedProtocol,
            needVerity = appData.needSecurityVerificationWhenLaunch && !verified,
            supportFingerprint = appData.enableFingerprintVerification,
            currentBookName = booksRepository.currentBook.first().name
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = MainAppUiState.Loading,
        )

    /** 密码加密向量信息 */
    private val _passwordIv = settingRepository.appDataMode
        .mapLatest { it.passwordIv }

    /** 密码信息 */
    private val _passwordInfo = settingRepository.appDataMode
        .mapLatest { it.passwordInfo }

    /** 密码加密向量信息 */
    private val _fingerprintIv = settingRepository.appDataMode
        .mapLatest { it.fingerprintIv }

    /** 密码信息 */
    private val _fingerprintPasswordInfo = settingRepository.appDataMode
        .mapLatest { it.fingerprintPasswordInfo }

    /** 验证模式 */
    private val _verificationMode = settingRepository.appDataMode
        .mapLatest { it.verificationModel }

    /** 确认认证，使用 [pwd] 进行认证并将认证结果回调 [callback] */
    fun onVerityConfirm(pwd: String, callback: (SettingPasswordStateEnum) -> Unit) {
        viewModelScope.launch {
            // 使用 AndroidKeyStore 解密密码信息
            val passwordIv = _passwordIv.first()
            val bytes = passwordIv.hexToBytes()
            if (null == bytes) {
                callback.invoke(SettingPasswordStateEnum.PASSWORD_DECODE_FAILED)
                return@launch
            }
            val cipher = loadDecryptCipher(KEY_ALIAS_PASSWORD, bytes)
            val pwdSha = cipher.doFinal(_passwordInfo.first().hexToBytes()).decodeToString()
            if (pwd.shaEncode() != pwdSha) {
                // 密码错误，提示
                callback.invoke(SettingPasswordStateEnum.PASSWORD_WRONG)
                return@launch
            }

            // 密码正确，进入首页
            _verified.tryEmit(true)
        }
    }

    /** 显示指纹认证 */
    fun showFingerprintVerify() {
        firstOpen = false
        viewModelScope.launch {
            val bytes = _fingerprintIv.first().hexToBytes()
            if (null == bytes) {
                shouldDisplayBookmark = MainAppBookmarkEnum.PASSWORD_DECODE_FAILED
                return@launch
            }
            // 指纹认证
            dialogState = DialogState.Shown(loadDecryptCipher(KEY_ALIAS_FINGERPRINT, bytes))
        }
    }

    /** 指纹认证成功，使用 [cipher] 进行解码认证 */
    fun onFingerprintVerifySuccess(cipher: Cipher) {
        viewModelScope.launch {
            dismissDialog()

            // 获取密码信息
            val passwordIv = _passwordIv.first()
            val bytes = passwordIv.hexToBytes()
            if (null == bytes) {
                shouldDisplayBookmark = MainAppBookmarkEnum.PASSWORD_DECODE_FAILED
                return@launch
            }
            val pwdCipher = loadDecryptCipher(KEY_ALIAS_PASSWORD, bytes)
            val pwdSha = pwdCipher.doFinal(_passwordInfo.first().hexToBytes()).decodeToString()
            // 获取指纹密码信息
            val fingerprintPwdSha =
                cipher.doFinal(_fingerprintPasswordInfo.first().hexToBytes()).decodeToString()

            if (pwdSha != fingerprintPwdSha) {
                // 密码错误
                shouldDisplayBookmark = MainAppBookmarkEnum.PASSWORD_WRONG
                return@launch
            }

            // 密码正确，进入首页
            _verified.tryEmit(true)
        }
    }

    /** 错误文本，指纹认证失败时使用 */
    var errorText = ""
        private set

    /** 指纹认证失败，使用错误码 [code]、错误信息 [msg] 处理错误提示 */
    fun onFingerprintVerifyError(code: Int, msg: String) {
        logger().i("onFingerprintVerifyError(code = <$code>, msg = <$msg>)")
        dismissDialog()
        errorText = msg
        shouldDisplayBookmark = MainAppBookmarkEnum.ERROR
    }

    /** 界面不可见时刷新安全认证状态 */
    fun refreshVerifyState() {
        viewModelScope.launch {
            if (_verificationMode.first() == VerificationModeEnum.WHEN_FOREGROUND) {
                _verified.tryEmit(false)
                firstOpen = true
            }
        }
    }

    /** 同意用户隐私协议 */
    fun agreeProtocol() {
        viewModelScope.launch {
            settingRepository.updateAgreedProtocol(true)
        }
    }

    /** 隐藏提示 */
    fun dismissBookmark() {
        shouldDisplayBookmark = MainAppBookmarkEnum.NONE
    }

    /** 隐藏弹窗 */
    private fun dismissDialog() {
        dialogState = DialogState.Dismiss
    }
}

/**
 * 界面 UI 状态
 */
sealed interface MainAppUiState {
    /** 加载中 */
    data object Loading : MainAppUiState

    /**
     * 加载完成
     *
     * @param needRequestProtocol 需要显示用户隐私协议确认
     * @param needVerity 需要安全认证
     * @param supportFingerprint 是否支持指纹识别
     * @param currentBookName 当前账本名称
     */
    data class Success(
        val needRequestProtocol: Boolean,
        val needVerity: Boolean,
        val supportFingerprint: Boolean,
        val currentBookName: String,
    ) : MainAppUiState
}