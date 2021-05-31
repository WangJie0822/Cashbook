package cn.wj.android.cashbook.ui.viewmodel

import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.color
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.store.LocalDataStore
import cn.wj.android.cashbook.data.transform.toSnackbarModel

/**
 * 编辑记录 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/28
 */
class EditRecordViewModel(private val local: LocalDataStore) : BaseViewModel() {

    /** 当前下标 */
    val currentItem: MutableLiveData<Int> = MutableLiveData(0)

    /** 计算结果显示 */
    val calculatorStr: ObservableField<String> = ObservableField()

    /** 等号背景颜色 */
    val equalsBackgroundTint: LiveData<Int> = currentItem.map {
        when (it) {
            0 -> {
                // 支出
                R.color.color_spending
            }
            1 -> {
                // 收入
                R.color.color_income
            }
            else -> {
                // 转账
                R.color.color_secondary
            }
        }.color
    }

    /** 返回按钮点击 */
    val onBackClick: () -> Unit = {
        // 退出当前界面
        uiNavigationData.value = UiNavigationModel.builder {
            close()
        }
    }

    /** 确认点击 */
    val onConfirmClick: () -> Unit = {
        snackbarData.value = "确认保存".toSnackbarModel()
    }
}