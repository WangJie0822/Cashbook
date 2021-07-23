package cn.wj.android.cashbook.ui.record.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.paging.PagingData
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.entity.RecordEntity
import cn.wj.android.cashbook.data.event.LifecycleEvent
import cn.wj.android.cashbook.data.model.NoDataModel
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.store.LocalDataStore
import cn.wj.android.cashbook.interfaces.RecordListClickListener

/**
 * 搜索账单 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/7/23
 */
class SearchRecordViewModel(private val local: LocalDataStore) : BaseViewModel(), RecordListClickListener {

    /** 显示记录详情弹窗事件 */
    val showRecordDetailsDialogEvent: LifecycleEvent<RecordEntity> = LifecycleEvent()

    /** 无数据界面 Model */
    val noDataModel = NoDataModel(R.string.no_data_search)

    /** 搜索框文本 */
    val searchText: MutableLiveData<String> = MutableLiveData("")

    /** 标记 - 是否正在刷新 */
    val refreshing: MutableLiveData<Boolean> = MutableLiveData<Boolean>(true)


    /** 当前资产记录列表 */
    val listData: LiveData<PagingData<RecordEntity>> = searchText.switchMap {
        local.getRecordListByKeywordsPagerData(it)
    }

    /** 返回点击 */
    val onBackClick: () -> Unit = {
        uiNavigationEvent.value = UiNavigationModel.builder {
            close()
        }
    }

    /** 记录 item 点击 */
    override val onRecordItemClick: (RecordEntity) -> Unit = { item ->
        showRecordDetailsDialogEvent.value = item
    }

}