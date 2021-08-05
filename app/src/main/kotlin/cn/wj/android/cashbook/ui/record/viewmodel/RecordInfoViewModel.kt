package cn.wj.android.cashbook.ui.record.viewmodel

import androidx.core.os.bundleOf
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.constants.ACTION_RECORD
import cn.wj.android.cashbook.data.constants.EVENT_RECORD_CHANGE
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_RECORD_EDIT
import cn.wj.android.cashbook.data.entity.RecordEntity
import cn.wj.android.cashbook.data.event.LifecycleEvent
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.repository.record.RecordRepository
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.launch

/**
 * 记录信息 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/11
 */
class RecordInfoViewModel(private val repository: RecordRepository) : BaseViewModel() {

    /** 当前记录数据 */
    lateinit var record: RecordEntity

    /** 显示关联记录信息弹窗事件 */
    val showAssociatedRecordInfoEvent: LifecycleEvent<RecordEntity> = LifecycleEvent()

    /** 显示删除确认弹窗事件 */
    val showDeleteConfirmEvent: LifecycleEvent<Int> = LifecycleEvent()

    /** 修改点击 */
    val onEditClick: () -> Unit = {
        uiNavigationEvent.value = UiNavigationModel.builder {
            jump(
                ROUTE_PATH_RECORD_EDIT, bundleOf(
                    ACTION_RECORD to record
                )
            )
            close()
        }
    }

    /** 删除点击 */
    val onDeleteClick: () -> Unit = {
        showDeleteConfirmEvent.value = 0
    }

    /** 关联的记录点击 */
    val onAssociatedRecordClick: () -> Unit = {
        record.record?.let {
            showOtherInfo(it.id)
        }
    }

    /** 被关联记录点击 */
    val onBeAssociateRecordClick: () -> Unit = {
        record.beAssociated?.let {
            showOtherInfo(it.id)
        }
    }

    /** 显示指定 [id] 的其它记录信息 */
    private fun showOtherInfo(id: Long) {
        viewModelScope.launch {
            try {
                showAssociatedRecordInfoEvent.value = repository.getRecordById(id)
            } catch (throwable: Throwable) {
                logger().e(throwable, "showOtherInfo")
            }
        }
    }

    /** 删除当前记录 [record] */
    fun deleteRecord() {
        viewModelScope.launch {
            try {
                repository.deleteRecord(record)
                // 通知记录变化
                LiveEventBus.get(EVENT_RECORD_CHANGE).post(0)
                // 删除成功，关闭当前弹窗
                uiNavigationEvent.value = UiNavigationModel.builder {
                    close()
                }
            } catch (throwable: Throwable) {
                logger().e(throwable, "deleteRecord")
            }
        }
    }
}