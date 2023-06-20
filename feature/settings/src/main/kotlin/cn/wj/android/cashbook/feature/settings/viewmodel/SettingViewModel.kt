package cn.wj.android.cashbook.feature.settings.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.common.KEY_ALIAS_PASSWORD
import cn.wj.android.cashbook.core.common.ext.logger
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
    var shouldDisplayBookmark by mutableStateOf(SettingBookmarkEnum.NONE)

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

    fun onCreateConfirm(pwd: String): SettingBookmarkEnum {
        // 使用 AndroidKeyStore 进行保存
        val cipher = loadEncryptCipher(KEY_ALIAS_PASSWORD)
            ?: return SettingBookmarkEnum.PASSWORD_ENCODE_FAILED
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
        return SettingBookmarkEnum.NONE
    }

    fun onModifyConfirm(oldPwd: String, newPwd: String, callback: (SettingBookmarkEnum) -> Unit) {
        viewModelScope.launch {
            // 使用 AndroidKeyStore 解密密码信息
            val passwordIv = passwordIv.first()
            this@SettingViewModel.logger()
                .i("onModifyConfirm(oldPwd = <$oldPwd>, newPwd = <$newPwd>), passwordIv = <$passwordIv>")
            val cipher = loadDecryptCipher(KEY_ALIAS_PASSWORD, passwordIv.hexToBytes())
            if (null == cipher) {
                callback.invoke(SettingBookmarkEnum.PASSWORD_DECODE_FAILED)
                return@launch
            }
            val pwdSha = cipher.doFinal(passwordInfo.first().hexToBytes()).decodeToString()
            if (oldPwd.shaEncode() == pwdSha) {
                // 密码正确，保存新密码
                callback.invoke(onCreateConfirm(newPwd))
            } else {
                // 密码错误，提示
                callback.invoke(SettingBookmarkEnum.PASSWORD_WRONG)
            }
        }
    }

    fun onClearConfirm(pwd: String, callback: (SettingBookmarkEnum) -> Unit) {
        viewModelScope.launch {
            // 使用 AndroidKeyStore 解密密码信息
            val passwordIv = passwordIv.first()
            this@SettingViewModel.logger()
                .i("onClearConfirm(pwd = <$pwd>), passwordIv = <$passwordIv>")
            val cipher = loadDecryptCipher(KEY_ALIAS_PASSWORD, passwordIv.hexToBytes())
            if (null == cipher) {
                callback.invoke(SettingBookmarkEnum.PASSWORD_DECODE_FAILED)
                return@launch
            }
            val pwdSha = cipher.doFinal(passwordInfo.first().hexToBytes()).decodeToString()
            if (pwd.shaEncode() == pwdSha) {
                // 密码正确，清除密码
                settingRepository.updatePasswordInfo("")
                // 隐藏弹窗
                dismissDialog()
                callback.invoke(SettingBookmarkEnum.NONE)
            } else {
                // 密码错误，提示
                callback.invoke(SettingBookmarkEnum.PASSWORD_WRONG)
            }
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