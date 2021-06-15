package cn.wj.android.cashbook.ui.record.viewmodel

import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.constants.ACTION_RECORD
import cn.wj.android.cashbook.data.constants.EVENT_RECORD_CHANGE
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_EDIT_RECORD
import cn.wj.android.cashbook.data.entity.RecordEntity
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.store.LocalDataStore
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.launch

/**
 * 记录信息 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/11
 */
class RecordInfoViewModel(private val local: LocalDataStore) : BaseViewModel() {

    lateinit var record: RecordEntity

    /** 显示删除确认弹窗数据 */
    val showDeleteConfirmData: MutableLiveData<Int> = MutableLiveData()

    /** 修改点击 */
    val onEditClick: () -> Unit = {
        uiNavigationData.value = UiNavigationModel.builder {
            jump(
                ROUTE_PATH_EDIT_RECORD, bundleOf(
                    ACTION_RECORD to record
                )
            )
            close()
        }
    }

    /** 删除点击 */
    val onDeleteClick: () -> Unit = {
        showDeleteConfirmData.value = 0
    }

    fun deleteRecord() {
        viewModelScope.launch {
            try {
                local.deleteRecord(record)
                // 通知记录变化
                LiveEventBus.get(EVENT_RECORD_CHANGE).post(0)
                // 删除成功，关闭当前弹窗
                uiNavigationData.value = UiNavigationModel.builder {
                    close()
                }
            } catch (throwable: Throwable) {
                logger().e(throwable, "deleteRecord")
            }
        }
    }
}