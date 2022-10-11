package cn.wj.android.cashbook.data.live

import androidx.lifecycle.MutableLiveData
import cn.wj.android.cashbook.data.config.AppConfigs

/**
 * 密码数据
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2022/10/10
 */
object PasswordLiveData : MutableLiveData<String>() {

    override fun onActive() {
        super.onActive()

        if (null == value) {
            value = AppConfigs.password
        }
    }

    override fun setValue(value: String?) {
        super.setValue(value)

        AppConfigs.password = value.orEmpty()
    }
}