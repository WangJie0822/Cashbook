package cn.wj.android.cashbook.ui.record.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.condition
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.tools.maps
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.live.CurrentBooksLiveData
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.store.LocalDataStore

/**
 * 选择关联记录 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/16
 */
class SelectAssociatedRecordViewModel(private val local: LocalDataStore) : BaseViewModel() {

    /** 标记 - 是否是退款 */
    val refund: MutableLiveData<Boolean> = MutableLiveData()

    /** 日期 */
    val dateStr: MutableLiveData<String> = MutableLiveData()

    /** 金额 */
    val amount: MutableLiveData<String> = MutableLiveData()

    /** 金额 */
    val amountStr: LiveData<String> = maps(amount, refund) {
        if (refund.value.condition) {
            R.string.refund_with_colon
        } else {
            R.string.reimburse_with_colon
        }.string + CurrentBooksLiveData.currency.symbol + it
    }

    /** 提示文本 */
    val hintStr:LiveData<String> = refund.map {
        if (it) {
            R.string.refund_select_hint
        } else{
            R.string.reimburse_select_hint
        }.string
    }

    /** 返回按钮点击 */
    val onBackClick: () -> Unit = {
        // 退出当前界面
        uiNavigationData.value = UiNavigationModel.builder {
            close()
        }
    }
}