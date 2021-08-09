package cn.wj.android.cashbook.ui.main.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.config.AppConfigs
import cn.wj.android.cashbook.data.event.LifecycleEvent
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.repository.main.MainRepository
import cn.wj.android.cashbook.data.transform.toSnackbarModel
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
        selectRecoveryPathEvent.value = 0
    }

    /** 尝试备份到备份路径 */
    fun tryBackup() {
        viewModelScope.launch {
            try {
                repository.backup()
                snackbarEvent.value = R.string.backup_success.string.toSnackbarModel()
            } catch (throwable: Throwable) {
                logger().e(throwable, "tryBackup")
            }
        }
    }

    /** 尝试从 [path] 开始恢复 */
    fun tryRecovery(path: String) {
        viewModelScope.launch {
            try {
                if (repository.recovery(path)) {
                    snackbarEvent.value = R.string.recovery_success.string.toSnackbarModel()
                } else {
                    snackbarEvent.value = R.string.recovery_failed.string.toSnackbarModel()
                }
            } catch (throwable: Throwable) {
                logger().e(throwable, "tryRecovery")
            }
        }
    }
}