package cn.wj.android.cashbook.ui.main.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import cn.wj.android.cashbook.base.ext.base.orFalse
import cn.wj.android.cashbook.base.tools.getSharedBoolean
import cn.wj.android.cashbook.base.tools.setSharedBoolean
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.constants.SHARED_KEY_MOBILE_NETWORK_DOWNLOAD_ENABLE
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
    val enableDownloadWithMobileNetwork: MutableLiveData<Boolean> = object : MutableLiveData<Boolean>(getSharedBoolean(SHARED_KEY_MOBILE_NETWORK_DOWNLOAD_ENABLE).orFalse()) {
        override fun setValue(value: Boolean?) {
            super.setValue(value)
            if (null != value) {
                setSharedBoolean(SHARED_KEY_MOBILE_NETWORK_DOWNLOAD_ENABLE, value)
            }
        }
    }

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
}