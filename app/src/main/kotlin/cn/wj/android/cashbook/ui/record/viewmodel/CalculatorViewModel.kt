package cn.wj.android.cashbook.ui.record.viewmodel

import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import cn.wj.android.cashbook.base.ui.BaseViewModel

/**
 * 计算器弹窗 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/9
 */
class CalculatorViewModel : BaseViewModel() {

    /** 确认点击数据 */
    val confirmClickData: MutableLiveData<Int> = MutableLiveData()

    /** 计算结果显示 */
    val calculatorStr: ObservableField<String> = ObservableField()

    /** 确认点击 */
    val onConfirmClick: () -> Unit = {
        confirmClickData.value = 0
    }
}