package cn.wj.android.cashbook.ui.type.viewmodel

import androidx.lifecycle.MutableLiveData
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.enums.RecordTypeEnum
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.transform.toSnackbarModel

/**
 * 编辑分类列表
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/29
 */
class EditTypListViewModel : BaseViewModel() {

    /** 当前界面下标 */
    val currentItem: MutableLiveData<Int> = MutableLiveData(RecordTypeEnum.EXPENDITURE.position)

    /** 返回按钮点击 */
    val onBackClick: () -> Unit = {
        // 退出当前界面
        uiNavigationEvent.value = UiNavigationModel.builder {
            close()
        }
    }

    /** TODO 添加大类点击 */
    val onAddFirstTypeClick: () -> Unit = {
        snackbarEvent.value = "添加大类".toSnackbarModel()
    }
}