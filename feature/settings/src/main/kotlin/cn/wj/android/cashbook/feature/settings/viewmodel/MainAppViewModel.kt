/*
 * Copyright 2021 The Cashbook Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.wj.android.cashbook.feature.settings.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.common.ApplicationInfo
import cn.wj.android.cashbook.core.common.KEY_ALIAS_FINGERPRINT
import cn.wj.android.cashbook.core.common.KEY_ALIAS_PASSWORD
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.data.repository.BooksRepository
import cn.wj.android.cashbook.core.data.repository.SettingRepository
import cn.wj.android.cashbook.core.data.uitl.AppUpgradeManager
import cn.wj.android.cashbook.core.data.uitl.NetworkMonitor
import cn.wj.android.cashbook.core.design.security.hexToBytes
import cn.wj.android.cashbook.core.design.security.loadDecryptCipher
import cn.wj.android.cashbook.core.design.security.shaEncode
import cn.wj.android.cashbook.core.model.entity.UpgradeInfoEntity
import cn.wj.android.cashbook.core.model.enums.VerificationModeEnum
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.feature.settings.enums.MainAppBookmarkEnum
import cn.wj.android.cashbook.feature.settings.enums.SettingPasswordStateEnum
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.crypto.Cipher
import javax.inject.Inject

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
    networkMonitor: NetworkMonitor,
    private val appUpgradeManager: AppUpgradeManager,
) : ViewModel() {

    /** 标记 - 是否已认证 */
    private val _verified = MutableStateFlow(false)

    /** 认证结果 */
    var verifyState by mutableStateOf(SettingPasswordStateEnum.SUCCESS)

    var firstOpen by mutableStateOf(true)
        private set

    /** 是否显示提示 */
    var shouldDisplayBookmark by mutableStateOf(MainAppBookmarkEnum.NONE)
        private set

    /** 弹窗状态 */
    var dialogState by mutableStateOf<DialogState>(DialogState.Dismiss)
        private set

    private val _isUpgradeDownloading = appUpgradeManager.isDownloading

    /** 标记 - 是否正在获取更新数据 */
    var inRequestUpdateData by mutableStateOf(false)
        private set

    /** 更新数据 */
    private val _updateInfoData = MutableStateFlow<UpgradeInfoEntity?>(null)
    val updateInfoData: StateFlow<UpgradeInfoEntity?> = _updateInfoData

    /** 下载确认数据 */
    private val _confirmUpdateInfoData = MutableStateFlow<UpgradeInfoEntity?>(null)
    val confirmUpdateInfoData: StateFlow<UpgradeInfoEntity?> = _confirmUpdateInfoData

    val ignoreUpdateVersionData =
        combine(settingRepository.appDataMode, _updateInfoData) { appDataModel, updateInfoEntity ->
            appDataModel.ignoreUpdateVersion.isNotBlank() && appDataModel.ignoreUpdateVersion == updateInfoEntity?.versionName
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = false,
            )

    /**
     * 是否允许下载
     * - WiFi或用户允许流量下载
     */
    private val _allowDownload =
        combine(settingRepository.appDataMode, networkMonitor.isWifi) { appDataModel, isWifi ->
            isWifi || appDataModel.mobileNetworkDownloadEnable
        }

    /** 界面 UI 状态 */
    val uiState = combine(settingRepository.appDataMode, _verified) { appData, verified ->
        val needSecurityVerificationWhenLaunch = appData.needSecurityVerificationWhenLaunch
        if (!needSecurityVerificationWhenLaunch) {
            // 未开启安全认证，启动自动修改认证状态为已认证，防止开启认证开关时立即拉起认证
            _verified.tryEmit(true)
        }
        MainAppUiState.Success(
            needRequestProtocol = !appData.agreedProtocol,
            needVerity = needSecurityVerificationWhenLaunch && !verified,
            supportFingerprint = appData.enableFingerprintVerification,
            currentBookName = booksRepository.currentBook.first().name,
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

    /** 确认认证，使用 [pwd] 进行认证 */
    fun onVerityConfirm(pwd: String) {
        viewModelScope.launch {
            // 使用 AndroidKeyStore 解密密码信息
            val passwordIv = _passwordIv.first()
            val bytes = passwordIv.hexToBytes()
            if (null == bytes) {
                verifyState = SettingPasswordStateEnum.PASSWORD_DECODE_FAILED
                return@launch
            }
            val cipher = loadDecryptCipher(KEY_ALIAS_PASSWORD, bytes)
            val pwdSha = cipher.doFinal(_passwordInfo.first().hexToBytes()).decodeToString()
            if (pwd.shaEncode() != pwdSha) {
                // 密码错误，提示
                verifyState = SettingPasswordStateEnum.PASSWORD_WRONG
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

    /** 检查更新 */
    fun checkUpdateAuto() {
        logger().i("checkUpdateAuto()")
        viewModelScope.launch {
            try {
                val appDataModel = settingRepository.appDataMode.first()
                if (!appDataModel.autoCheckUpdate) {
                    return@launch
                }
                val upgradeInfoEntity = settingRepository.checkUpdate()
                if (upgradeInfoEntity.versionName == appDataModel.ignoreUpdateVersion) {
                    return@launch
                }
                checkUpgradeFromInfo(
                    info = upgradeInfoEntity,
                    need = {
                        _updateInfoData.tryEmit(upgradeInfoEntity)
                    },
                    noNeed = {
                        // empty block
                    },
                )
            } catch (throwable: Throwable) {
                logger().e(throwable, "checkUpdateAuto()")
            }
        }
    }

    /** 检查更新 */
    fun checkUpdate() {
        logger().i("checkUpdate()")
        viewModelScope.launch {
            try {
                if (_isUpgradeDownloading.first()) {
                    shouldDisplayBookmark = MainAppBookmarkEnum.UPDATE_DOWNLOADING
                    return@launch
                }
                inRequestUpdateData = true
                if (settingRepository.syncLatestVersion()) {
                    val upgradeInfoEntity = settingRepository.checkUpdate()
                    checkUpgradeFromInfo(
                        info = upgradeInfoEntity,
                        need = {
                            _updateInfoData.tryEmit(upgradeInfoEntity)
                        },
                        noNeed = {
                            shouldDisplayBookmark = MainAppBookmarkEnum.NO_NEED_UPDATE
                        },
                    )
                }
            } catch (throwable: Throwable) {
                logger().e(throwable, "checkUpdate()")
            } finally {
                inRequestUpdateData = false
            }
        }
    }

    /** 确认升级 */
    fun confirmUpdate() {
        val upgradeInfo = updateInfoData.value ?: return
        _updateInfoData.tryEmit(null)
        viewModelScope.launch {
            if (_allowDownload.first()) {
                // 允许直接下载
                appUpgradeManager.startDownload(upgradeInfo)
                shouldDisplayBookmark = MainAppBookmarkEnum.START_DOWNLOAD
            } else {
                // 未连接WiFi且未允许流量下载，弹窗提示
                _confirmUpdateInfoData.tryEmit(upgradeInfo)
            }
        }
    }

    /** 隐藏升级弹窗 */
    fun dismissUpdateDialog(ignore: Boolean) {
        viewModelScope.launch {
            settingRepository.updateIgnoreUpdateVersion(if (ignore) updateInfoData.value?.versionName.orEmpty() else "")
            _updateInfoData.tryEmit(null)
        }
    }

    /** 确认下载 */
    fun confirmDownload(noMorePrompt: Boolean) {
        val updateInfo = confirmUpdateInfoData.value ?: return
        _confirmUpdateInfoData.tryEmit(null)
        viewModelScope.launch {
            // 更新是否允许流量下载属性
            settingRepository.updateMobileNetworkDownloadEnable(noMorePrompt)
            // 开始下载
            appUpgradeManager.startDownload(updateInfo)
            shouldDisplayBookmark = MainAppBookmarkEnum.START_DOWNLOAD
        }
    }

    /** 隐藏无 WiFi 升级提示弹窗 */
    fun dismissNoWifiUpdateDialog() {
        _confirmUpdateInfoData.tryEmit(null)
    }

    /** 隐藏弹窗 */
    private fun dismissDialog() {
        dialogState = DialogState.Dismiss
    }

    private fun checkUpgradeFromInfo(
        info: UpgradeInfoEntity,
        need: () -> Unit,
        noNeed: () -> Unit,
    ) {
        logger().d("checkFromInfo info: $info")
        if (ApplicationInfo.isDev) {
            // Dev 环境，永远提示更新
            need.invoke()
            return
        }
        if (!needUpdate(info.versionName)) {
            // 已是最新版本
            noNeed.invoke()
            return
        }
        // 不是最新版本
        if (info.downloadUrl.isBlank()) {
            // 没有下载资源
            noNeed.invoke()
            return
        }
        need.invoke()
    }

    /** 根据网络返回的版本信息判断是否需要更新 */
    private fun needUpdate(versionName: String?): Boolean {
        if (versionName.isNullOrBlank()) {
            return false
        }
        val localSplits = ApplicationInfo.versionName.split("_")
        val splits = versionName.replace("Pre Release ", "")
            .replace("Release ", "")
            .split("_")
        val localVersions = localSplits.first().replace("v", "").split(".")
        val versions = splits.first().replace("v", "").split(".")
        if (localSplits.first() == splits.first()) {
            return splits[1].toInt() > localSplits[1].toInt()
        }
        for (i in localVersions.indices) {
            if (versions[i] > localVersions[i]) {
                return true
            }
        }
        return false
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
