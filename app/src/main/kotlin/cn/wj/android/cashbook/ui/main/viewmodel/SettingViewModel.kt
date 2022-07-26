package cn.wj.android.cashbook.ui.main.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import cn.wj.android.cashbook.base.tools.mutableLiveDataOf
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.config.AppConfigs
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_BACKUP
import cn.wj.android.cashbook.data.event.LifecycleEvent
import cn.wj.android.cashbook.data.live.CurrentDayNightLiveData
import cn.wj.android.cashbook.data.model.UiNavigationModel

/**
 * 设置 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/22
 */
class SettingViewModel : BaseViewModel() {

    /** 显示选择白天黑夜模式事件 */
    val showSelectDayNightDialogEvent: LifecycleEvent<Int> = LifecycleEvent()

    /** 是否允许流量下载 */
    val enableDownloadWithMobileNetwork: MutableLiveData<Boolean> = mutableLiveDataOf(
        onActive = {
            if (null == value) {
                value = AppConfigs.mobileNetworkDownloadEnable
            }
        }, onSet = {
            value?.let { value ->
                AppConfigs.mobileNetworkDownloadEnable = value
            }
        }
    )

    /** 当前白天黑夜模式显示文本 */
    val currentDayNightStrResId: LiveData<Int> = CurrentDayNightLiveData.map {
        it.typeStrResId
    }

    /** 返回点击 */
    val onBackClick: () -> Unit = {
        uiNavigationEvent.value = UiNavigationModel.builder {
            close()
        }
    }

    /** 白天黑夜模式点击 */
    val onDayNightClick: () -> Unit = {
        showSelectDayNightDialogEvent.value = 0
    }

    /** 备份与恢复点击 */
    val onBackupAndRecoveryClick: () -> Unit = {
        uiNavigationEvent.value = UiNavigationModel.builder {
            jump(ROUTE_PATH_BACKUP)
        }
    }
}