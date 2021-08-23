package cn.wj.android.cashbook.ui.main.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import cn.wj.android.cashbook.base.tools.mutableLiveDataOf
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.config.AppConfigs
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_BACKUP
import cn.wj.android.cashbook.data.event.LifecycleEvent
import cn.wj.android.cashbook.data.live.CurrentThemeLiveData
import cn.wj.android.cashbook.data.model.UiNavigationModel

/**
 * 设置 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/22
 */
class SettingViewModel : BaseViewModel() {

    /** 显示选择主题事件 */
    val showSelectThemeDialogEvent: LifecycleEvent<Int> = LifecycleEvent()

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

    /** 当前主题显示文本 */
    val currentThemeStrResId: LiveData<Int> = CurrentThemeLiveData.map {
        it.typeStrResId
    }

    /** 返回点击 */
    val onBackClick: () -> Unit = {
        uiNavigationEvent.value = UiNavigationModel.builder {
            close()
        }
    }

    /** 主题点击 */
    val onThemeClick: () -> Unit = {
        showSelectThemeDialogEvent.value = 0
    }

    /** 备份与恢复点击 */
    val onBackupAndRecoveryClick: () -> Unit = {
        uiNavigationEvent.value = UiNavigationModel.builder {
            jump(ROUTE_PATH_BACKUP)
        }
    }
}