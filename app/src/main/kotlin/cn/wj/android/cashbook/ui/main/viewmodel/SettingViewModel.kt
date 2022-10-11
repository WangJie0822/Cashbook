package cn.wj.android.cashbook.ui.main.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.condition
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.tools.mutableLiveDataOf
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.config.AppConfigs
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_BACKUP
import cn.wj.android.cashbook.data.event.LifecycleEvent
import cn.wj.android.cashbook.data.live.CurrentDayNightLiveData
import cn.wj.android.cashbook.data.live.PasswordLiveData
import cn.wj.android.cashbook.data.model.UiNavigationModel

/**
 * 设置 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/22
 */
class SettingViewModel : BaseViewModel() {

    /** 显示选择白天黑夜模式事件 */
    val showSelectDayNightDialogEvent: LifecycleEvent<Int> = LifecycleEvent()

    /** 显示编辑密码弹窗事件 */
    val showEditPasswordDialogEvent: LifecycleEvent<Int> = LifecycleEvent()

    /** 显示清除密码弹窗事件 */
    val showClearPasswordDialogEvent: LifecycleEvent<Int> = LifecycleEvent()

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

    /** 标记 - 是否有密码 */
    val hasPassword: LiveData<Boolean> = PasswordLiveData.map {
        it.isNotBlank()
    }

    /** 密码选项文本 */
    val passwordItemStr: LiveData<String> = hasPassword.map {
        if (it) {
            R.string.modify_password
        } else {
            R.string.create_password
        }.string
    }

    /** 是否开启安全校验 */
    val enableVerifyWhenOpen: MutableLiveData<Boolean> = mutableLiveDataOf(
        onActive = {
            if (null == value) {
                value = AppConfigs.needVerifyWhenOpen
            }
        }, onSet = {
            AppConfigs.needVerifyWhenOpen = value.condition
        }
    )

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

    /** 开启验证开关 */
    val onVerifyWhenOpenChange: (Boolean) -> Unit = {
        if (it && !hasPassword.value.condition) {
            // 没有设置密码，需先设置密码
            showEditPasswordDialogEvent.value = 0
            enableVerifyWhenOpen.value = false
        } else {
            // 同步状态
            enableVerifyWhenOpen.value = it
        }
    }

    /** 密码点击 */
    val onPasswordClick: () -> Unit = {
        showEditPasswordDialogEvent.value = 0
    }

    /** 清空密码点击 */
    val onClearPasswordClick: () -> Unit = {
        showClearPasswordDialogEvent.value = 0
    }
}