package cn.wj.android.cashbook.ui.main.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.condition
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.ext.shaEncode
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.constants.PASSWORD_MIN_LENGTH
import cn.wj.android.cashbook.data.live.PasswordLiveData
import cn.wj.android.cashbook.data.model.SnackbarModel
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.transform.toSnackbarModel

/**
 * 编辑密码 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2022/10/10
 */
class EditPasswordViewModel : BaseViewModel() {

    /** 是否是修改密码 */
    val modifyPassword: LiveData<Boolean> = PasswordLiveData.map {
        it.isNotBlank()
    }

    /** 标题文本 */
    val titleStr: LiveData<String> = modifyPassword.map {
        if (it) {
            R.string.modify_password
        } else {
            R.string.create_password
        }.string
    }

    /** 旧密码 */
    val oldPassword: MutableLiveData<String> = MutableLiveData()

    /** 第一个输入框密码 */
    val firstPassword: MutableLiveData<String> = MutableLiveData()

    /** 第二个输入框密码 */
    val secondPassword: MutableLiveData<String> = MutableLiveData()

    /** 确认点击 */
    val onConfirmClick: () -> Unit = fun() {
        val first = firstPassword.value.orEmpty()
        val second = secondPassword.value.orEmpty()
        if (modifyPassword.value.condition) {
            // 修改密码
            val old = oldPassword.value.orEmpty()
            if (old.shaEncode() != PasswordLiveData.value) {
                // 旧密码校验错误
                snackbarEvent.value =
                    R.string.please_enter_right_old_password.string.toSnackbarModel()
                return
            }
            if (first != second) {
                // 两次输入不同
                snackbarEvent.value = SnackbarModel(R.string.please_enter_same_password)
                return
            }
            if (first.length < PASSWORD_MIN_LENGTH) {
                // 密码长度低于6
                snackbarEvent.value = SnackbarModel(R.string.password_length_too_short)
                return
            }
        } else {
            // 创建密码
            if (first != second) {
                // 两次输入不同
                snackbarEvent.value = SnackbarModel(R.string.please_enter_same_password)
                return
            }
            if (first.length < PASSWORD_MIN_LENGTH) {
                // 密码长度低于6
                snackbarEvent.value = SnackbarModel(R.string.password_length_too_short)
                return
            }
        }
        // 保存密码
        PasswordLiveData.value = first.shaEncode()
        // 关闭弹窗
        uiNavigationEvent.value = UiNavigationModel.builder {
            close()
        }
    }

    /** 取消点击 */
    val onCancelClick: () -> Unit = {
        uiNavigationEvent.value = UiNavigationModel.builder {
            close()
        }
    }
}