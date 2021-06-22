package cn.wj.android.cashbook.ui.record.viewmodel

import androidx.core.os.bundleOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.condition
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.tools.maps
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.constants.ACTION_RECORD
import cn.wj.android.cashbook.data.constants.ACTIVITY_RESULT_OK
import cn.wj.android.cashbook.data.entity.RecordEntity
import cn.wj.android.cashbook.data.live.CurrentBooksLiveData
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.store.LocalDataStore
import cn.wj.android.cashbook.interfaces.RecordListClickListener
import kotlinx.coroutines.launch

/**
 * 选择关联记录 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/16
 */
class SelectAssociatedRecordViewModel(private val local: LocalDataStore) : BaseViewModel(), RecordListClickListener {

    /** 标记 - 是否是退款 */
    val refund: MutableLiveData<Boolean> = MutableLiveData()

    /** 默认展示记录列表 */
    private val defaultListData: LiveData<List<RecordEntity>> = refund.switchMap {
        getDefaultDataList(it)
    }

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
        }.string + CurrentBooksLiveData.currency.symbol + amount.value
    }

    /** 搜索框文本 */
    val searchText: MutableLiveData<String> = MutableLiveData("")

    /** 搜索结果数据 */
    private val searchListData: LiveData<List<RecordEntity>> = searchText.switchMap {
        searchDataListByRemarkOrAmount(it)
    }

    /** 显示列表数据 */
    val listData: LiveData<List<RecordEntity>> = maps(searchListData, defaultListData) {
        if (searchListData.value?.isEmpty().condition) {
            defaultListData.value
        } else {
            searchListData.value
        }.orEmpty()
    }

    /** 是否显示提示 */
    val showHint: LiveData<Boolean> = searchListData.map {
        it.isEmpty()
    }

    /** 提示文本 */
    val hintStr: LiveData<String> = refund.map {
        if (it) {
            R.string.refund_select_hint
        } else {
            R.string.reimburse_select_hint
        }.string
    }

    /** 返回按钮点击 */
    val onBackClick: () -> Unit = {
        // 退出当前界面
        uiNavigationEvent.value = UiNavigationModel.builder {
            close()
        }
    }

    /** 记录 item 点击 */
    override val onRecordItemClick: (RecordEntity) -> Unit = { item ->
        // 设置返回数据并结束当前界面
        uiNavigationEvent.value = UiNavigationModel.builder {
            close(
                ACTIVITY_RESULT_OK, bundleOf(
                    ACTION_RECORD to item
                )
            )
        }
    }

    /** 根据是否是退款 [refund] 获取默认列表数据 */
    private fun getDefaultDataList(refund: Boolean): LiveData<List<RecordEntity>> {
        val result = MutableLiveData<List<RecordEntity>>()
        viewModelScope.launch {
            try {
                result.value = if (refund) {
                    // 退款
                    local.getLastThreeMonthExpenditureRecordLargerThanAmount(amount.value.orEmpty())
                } else {
                    // 报销
                    local.getLastThreeMonthReimburseExpenditureRecord()
                }
            } catch (throwable: Throwable) {
                logger().e(throwable, "getDefaultDataList")
            }
        }
        return result
    }

    /** 根据关键字 [keywords] 搜索备注或金额 */
    private fun searchDataListByRemarkOrAmount(keywords: String): LiveData<List<RecordEntity>> {
        val result = MutableLiveData<List<RecordEntity>>()
        viewModelScope.launch {
            try {
                result.value = when {
                    keywords.isBlank() -> {
                        arrayListOf()
                    }
                    refund.value.condition -> {
                        // 退款
                        local.getExpenditureRecordByRemarkOrAmount(keywords)
                    }
                    else -> {
                        // 报销
                        local.getReimburseExpenditureRecordByRemarkOrAmount(keywords)
                    }
                }
            } catch (throwable: Throwable) {
                logger().e(throwable, "searchDataListByRemarkOrAmount")
            }
        }
        return result
    }
}