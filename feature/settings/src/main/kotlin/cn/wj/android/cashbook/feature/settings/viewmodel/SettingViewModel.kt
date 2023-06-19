package cn.wj.android.cashbook.feature.settings.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.common.KEY_ALIAS_PASSWORD
import cn.wj.android.cashbook.core.data.repository.SettingRepository
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.feature.settings.enums.SettingBookmarkEnum
import cn.wj.android.cashbook.feature.settings.enums.SettingDialogEnum
import cn.wj.android.cashbook.feature.settings.security.hexToBytes
import cn.wj.android.cashbook.feature.settings.security.loadDecryptCipher
import cn.wj.android.cashbook.feature.settings.security.loadEncryptCipher
import cn.wj.android.cashbook.feature.settings.security.shaEncode
import cn.wj.android.cashbook.feature.settings.security.toHexString
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
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
    var shouldDisplayBookmark by mutableStateOf(SettingBookmarkEnum.NONE)

    /** 是否显示密码错误 */
    var shouldDisplayPasswordWrong by mutableStateOf(false)

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
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = "",
        )

    /** 密码信息 */
    private val passwordInfo = settingRepository.appDataMode
        .mapLatest { it.passwordInfo }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = "",
        )

    /** 是否有密码 */
    val hasPassword = passwordInfo
        .mapLatest { it.isNotBlank() }
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
            settingRepository.updateNeedSecurityVerificationWhenLaunch(need)
            // TODO
        }
    }

    fun onEnableFingerprintVerificationChanged(enable: Boolean) {
        viewModelScope.launch {
            settingRepository.updateEnableFingerprintVerification(enable)
            // TODO
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

    fun onCreateConfirm(pwd: String) {
        // 使用 AndroidKeyStore 进行保存
        val cipher = loadEncryptCipher(KEY_ALIAS_PASSWORD)
        if (null == cipher) {
            shouldDisplayBookmark = SettingBookmarkEnum.PASSWORD_ENCODE_FAILED
            return
        }
        val passwordInfo = cipher.doFinal(pwd.shaEncode().toByteArray()).toHexString()
        viewModelScope.launch {
            // 保存密码信息
            settingRepository.updatePasswordInfo(passwordInfo)
            // 保存密码向量信息
            settingRepository.updatePasswordIv(cipher.iv.toHexString())
            // 隐藏弹窗
            dismissDialog()
        }
    }

    fun onClearConfirm(pwd: String) {
        shouldDisplayPasswordWrong = false
        // 使用 AndroidKeyStore 解密密码信息
        val value = passwordIv.value
        val cipher = loadDecryptCipher(KEY_ALIAS_PASSWORD, value.hexToBytes())
        if (null == cipher) {
            shouldDisplayBookmark = SettingBookmarkEnum.PASSWORD_ENCODE_FAILED
            return
        }
        val pwdInfo = cipher.doFinal(passwordInfo.value.hexToBytes()).decodeToString()
        if (pwd.shaEncode() == pwdInfo) {
            // 密码正确，清除密码
            viewModelScope.launch {
                settingRepository.updatePasswordInfo("")
                // 隐藏弹窗
                dismissDialog()

            }
        } else {
            // 密码错误，提示
            shouldDisplayPasswordWrong = true
        }
    }

    fun onClearPasswordClick() {
        dialogState = DialogState.Shown(SettingDialogEnum.CLEAR_PASSWORD)
    }

    fun dismissDialog() {
        dialogState = DialogState.Dismiss
    }

    fun dismissBookmark() {
        shouldDisplayBookmark = SettingBookmarkEnum.NONE
    }
}