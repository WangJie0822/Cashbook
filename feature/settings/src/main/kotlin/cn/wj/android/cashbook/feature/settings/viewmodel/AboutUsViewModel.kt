package cn.wj.android.cashbook.feature.settings.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.common.ApplicationInfo
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.data.repository.SettingRepository
import cn.wj.android.cashbook.core.data.uitl.AppUpgradeManager
import cn.wj.android.cashbook.core.data.uitl.NetworkMonitor
import cn.wj.android.cashbook.core.model.entity.UpgradeInfoEntity
import cn.wj.android.cashbook.feature.settings.enums.AboutUsBookmarkEnum
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 关于我们
 *
 * @param settingRepository 设置相关数据仓库
 * @param networkMonitor 网络监控
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/14
 */
@HiltViewModel
class AboutUsViewModel @Inject constructor(
    private val settingRepository: SettingRepository,
    networkMonitor: NetworkMonitor,
    private val appUpgradeManager: AppUpgradeManager,
) : ViewModel() {

    private val _isUpgradeDownloading = appUpgradeManager.isDownloading

    /** 是否需要显示提示 */
    var shouldDisplayBookmark by mutableStateOf(AboutUsBookmarkEnum.NONE)
        private set

    /** 标记 - 是否正在获取更新数据 */
    var inRequestUpdateData by mutableStateOf(false)
        private set

    /** 更新数据 */
    private val _updateInfoData = MutableStateFlow<UpgradeInfoEntity?>(null)
    val updateInfoData: StateFlow<UpgradeInfoEntity?> = _updateInfoData

    /** 下载确认数据 */
    private val _confirmUpdateInfoData = MutableStateFlow<UpgradeInfoEntity?>(null)
    val confirmUpdateInfoData: StateFlow<UpgradeInfoEntity?> = _confirmUpdateInfoData

    /** 界面 UI 状态 */
    val uiState =
        combine(settingRepository.appDataMode, _updateInfoData) { appDataModel, updateInfoEntity ->
            AboutUsUiState.Success(
                useGitee = !appDataModel.useGithub,
                autoCheckUpdate = appDataModel.autoCheckUpdate,
                ignoreUpdateVersion = appDataModel.ignoreUpdateVersion.isNotBlank() && appDataModel.ignoreUpdateVersion == updateInfoEntity?.versionName
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = AboutUsUiState.Loading
            )

    /**
     * 是否允许下载
     * - WiFi或用户允许流量下载
     */
    private val _allowDownload =
        combine(settingRepository.appDataMode, networkMonitor.isWifi) { appDataModel, isWifi ->
            isWifi || appDataModel.mobileNetworkDownloadEnable
        }

    /** 更新是否使用 Gitee 源 */
    fun updateUseGitee(useGitee: Boolean) {
        viewModelScope.launch {
            settingRepository.updateUseGithub(!useGitee)
        }
    }

    /** 更新自动检查更新开关 */
    fun updateAutoCheckUpdate(autoCheckUpdate: Boolean) {
        viewModelScope.launch {
            settingRepository.updateAutoCheckUpdate(autoCheckUpdate)
        }
    }

    /** 检查更新 */
    fun checkUpdate() {
        viewModelScope.launch {
            try {
                if (_isUpgradeDownloading.first()) {
                    shouldDisplayBookmark = AboutUsBookmarkEnum.UPDATE_DOWNLOADING
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
                            shouldDisplayBookmark = AboutUsBookmarkEnum.NO_NEED_UPDATE
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
                shouldDisplayBookmark = AboutUsBookmarkEnum.START_DOWNLOAD
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
            shouldDisplayBookmark = AboutUsBookmarkEnum.START_DOWNLOAD
        }
    }

    /** 隐藏无 WiFi 升级提示弹窗 */
    fun dismissNoWifiUpdateDialog() {
        _confirmUpdateInfoData.tryEmit(null)
    }

    /** 隐藏提示 */
    fun dismissBookmark() {
        shouldDisplayBookmark = AboutUsBookmarkEnum.NONE
    }

    private fun checkUpgradeFromInfo(
        info: UpgradeInfoEntity,
        need: () -> Unit,
        noNeed: () -> Unit
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
        val splits = versionName.split("_")
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
 *
 * @param useGitee 是否使用 gitee 源
 * @param autoCheckUpdate 是否自动检查更新
 * @param ignoreUpdateVersion 是否跳过此版本
 */
sealed class AboutUsUiState(
    open val useGitee: Boolean = false,
    open val autoCheckUpdate: Boolean = false,
    open val ignoreUpdateVersion: Boolean = false,
) {
    /** 加载中 */
    data object Loading : AboutUsUiState()

    /** 加载完成 */
    data class Success(
        override val useGitee: Boolean,
        override val autoCheckUpdate: Boolean,
        override val ignoreUpdateVersion: Boolean,
    ) : AboutUsUiState()
}