package cn.wj.android.cashbook.ui.main.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.base.ext.base.orElse
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.config.AppConfigs
import cn.wj.android.cashbook.data.entity.BackupEntity
import cn.wj.android.cashbook.data.entity.RESULT_CODE_RECOVERY_CHANNEL_ERROR
import cn.wj.android.cashbook.data.entity.RESULT_CODE_RECOVERY_PATH_ERROR
import cn.wj.android.cashbook.data.entity.RESULT_CODE_RECOVERY_UNKNOWN_FILE
import cn.wj.android.cashbook.data.entity.RESULT_CODE_SUCCESS
import cn.wj.android.cashbook.data.entity.RESULT_CODE_WEBDAV_FAILED
import cn.wj.android.cashbook.data.enums.AutoBackupEnum
import cn.wj.android.cashbook.data.event.LifecycleEvent
import cn.wj.android.cashbook.data.model.ProgressModel
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.repository.main.MainRepository
import cn.wj.android.cashbook.data.transform.toSnackbarModel
import cn.wj.android.cashbook.manager.WebDAVManager
import kotlinx.coroutines.launch

/**
 * 备份相关 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/8/6
 */
class BackupViewModel(private val repository: MainRepository) : BaseViewModel() {

    /** 选择备份路径事件 */
    val selectBackupPathEvent: LifecycleEvent<Boolean> = LifecycleEvent()

    /** 检查备份路径 */
    val checkBackupPathEvent: LifecycleEvent<String> = LifecycleEvent()

    /** 选择恢复文件路径 */
    val selectRecoveryPathEvent: LifecycleEvent<Int> = LifecycleEvent()

    /** 选择自动备份方案 */
    val selectAutoBackup: LifecycleEvent<Int> = LifecycleEvent()

    /** 选择备份列表弹窗 */
    val showSelectBackupListDialogEvent: LifecycleEvent<List<BackupEntity>> = LifecycleEvent()

    /** WebDAV 服务器地址 */
    val webDAVWebUrl: MutableLiveData<String> = object : MutableLiveData<String>(AppConfigs.webDAVWebUrl) {

        override fun setValue(value: String?) {
            super.setValue(value)
            AppConfigs.webDAVWebUrl = value.orEmpty()
        }
    }

    /** WebDAV 账户 */
    val webDAVAccount: MutableLiveData<String> = object : MutableLiveData<String>(AppConfigs.webDAVAccount) {

        override fun setValue(value: String?) {
            super.setValue(value)
            AppConfigs.webDAVAccount = value.orEmpty()
        }
    }

    /** WebDAV 密码 */
    val webDAVPassword: MutableLiveData<String> = object : MutableLiveData<String>(AppConfigs.webDAVPassword) {

        override fun setValue(value: String?) {
            super.setValue(value)
            AppConfigs.webDAVPassword = value.orEmpty()
        }
    }

    /** 自动备份配置 */
    val autoBackup: MutableLiveData<AutoBackupEnum> = object : MutableLiveData<AutoBackupEnum>(AutoBackupEnum.fromValue(AppConfigs.autoBackup)) {

        override fun setValue(value: AutoBackupEnum?) {
            super.setValue(value)
            AppConfigs.autoBackup = value.orElse(AutoBackupEnum.CLOSED).value
        }
    }

    /** 自动备份文本 */
    val autoBackupTextResId: LiveData<Int> = autoBackup.map {
        it.textResId
    }

    /** 备份路径 */
    val backupPathData: MutableLiveData<String> = object : MutableLiveData<String>() {

        override fun onActive() {
            value = AppConfigs.backupPath
        }

        override fun setValue(value: String?) {
            super.setValue(value)
            AppConfigs.backupPath = value.orEmpty()
        }
    }

    /** 返回点击 */
    val onBackClick: () -> Unit = {
        uiNavigationEvent.value = UiNavigationModel.builder {
            close()
        }
    }

    /** 备份路径点击 */
    val onBackupPathClick: () -> Unit = {
        selectBackupPathEvent.value = false
    }

    /** 备份点击 */
    val onBackupClick: () -> Unit = {
        val path = backupPathData.value.orEmpty()
        if (path.isBlank()) {
            // 未选择备份路径，请求备份路径
            selectBackupPathEvent.value = true
        } else {
            // 开始备份
            checkBackupPathEvent.value = path
        }
    }

    /** 恢复点击 */
    val onRecoveryClick: () -> Unit = {
        tryRecovery()
    }

    /** 恢复长按 */
    val onRecoveryLongClick: () -> Boolean = {
        tryRecovery(true)
        true
    }

    /** 自动备份点击 */
    val onAutoBackupClick: () -> Unit = {
        selectAutoBackup.value = 0
    }

    /** 尝试备份 */
    fun tryBackup() {
        viewModelScope.launch {
            try {
                progressEvent.value = ProgressModel()
                snackbarEvent.value = when (repository.backup().code) {
                    RESULT_CODE_SUCCESS -> {
                        // 备份成功
                        R.string.backup_success
                    }
                    RESULT_CODE_WEBDAV_FAILED -> {
                        // 仅本地备份成功
                        R.string.backup_success_without_webdav
                    }
                    else -> {
                        // 备份失败
                        R.string.backup_failed
                    }
                }.string.toSnackbarModel()
            } catch (throwable: Throwable) {
                logger().e(throwable, "tryBackup")
                snackbarEvent.value = R.string.backup_failed.string.toSnackbarModel()
            } finally {
                progressEvent.value = null
            }
        }
    }

    /** 尝试恢复备份，[onlyLocal] 是仅从本地恢复 */
    private fun tryRecovery(onlyLocal: Boolean = false) {
        viewModelScope.launch {
            try {
                progressEvent.value = ProgressModel()
                if (!onlyLocal && WebDAVManager.available()) {
                    // WebDAV 配置可用，查询云端备份列表
                    val webList = getWebBackupList()
                    if (webList.isNotEmpty()) {
                        // 有数据，显示选择弹窗
                        showSelectBackupListDialogEvent.value = webList
                        return@launch
                    }
                    snackbarEvent.value = R.string.get_webdav_backup_failed.string.toSnackbarModel()
                }
                // WebDAV 不可用或无云端备份，获取本地备份列表
                selectRecoveryPathEvent.value = 0
            } catch (throwable: Throwable) {
                logger().e(throwable, "tryRecovery")
            } finally {
                progressEvent.value = null
            }
        }
    }

    /** 获取云端备份列表 */
    private suspend fun getWebBackupList(): List<BackupEntity> {
        return try {
            progressEvent.value = ProgressModel()
            repository.getWebBackupList()
        } catch (throwable: Throwable) {
            logger().e(throwable, "tryRecovery")
            arrayListOf()
        } finally {
            progressEvent.value = null
        }
    }

    /** 查询 [path] 路径下的备份列表 */
    fun queryLocalBackupList(path: String) {
        viewModelScope.launch {
            try {
                progressEvent.value = ProgressModel()
                val list = repository.getLocalBackupList(path)
                if (list.isEmpty()) {
                    snackbarEvent.value = R.string.no_backup_files.string.toSnackbarModel()
                } else {
                    showSelectBackupListDialogEvent.value = list
                }
            } catch (throwable: Throwable) {
                logger().e(throwable, "queryLocalBackupList")
            } finally {
                progressEvent.value = null
            }
        }
    }

    /** 根据 [backup] 恢复备份 */
    fun recovery(backup: BackupEntity) {
        viewModelScope.launch {
            try {
                progressEvent.value = ProgressModel()
                snackbarEvent.value = when (if (backup.webDAV) {
                    // 云端备份
                    repository.recoveryWeb(backup.path)
                } else {
                    // 本地备份
                    repository.recoveryLocal(backup.path)
                }.code) {
                    RESULT_CODE_SUCCESS -> {
                        // 恢复成功
                        R.string.recovery_success
                    }
                    RESULT_CODE_RECOVERY_UNKNOWN_FILE -> {
                        // 未知文件
                        R.string.recovery_failed_unkonwn_file
                    }
                    RESULT_CODE_RECOVERY_CHANNEL_ERROR -> {
                        // 渠道异常
                        R.string.recovery_failed_channel_error
                    }
                    RESULT_CODE_RECOVERY_PATH_ERROR -> {
                        // 路径异常
                        R.string.recovery_failed_path_error
                    }
                    else -> {
                        R.string.recovery_failed
                    }
                }.string.toSnackbarModel()
            } catch (throwable: Throwable) {
                logger().e(throwable, "recovery")
                snackbarEvent.value = R.string.recovery_failed.string.toSnackbarModel()
            } finally {
                progressEvent.value = null
            }
        }
    }
}