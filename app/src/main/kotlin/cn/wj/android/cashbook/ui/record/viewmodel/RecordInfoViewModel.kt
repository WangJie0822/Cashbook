package cn.wj.android.cashbook.ui.record.viewmodel

import androidx.core.os.bundleOf
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.constants.ACTION_RECORD
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_EDIT_RECORD
import cn.wj.android.cashbook.data.entity.RecordEntity
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.store.LocalDataStore

/**
 * 记录信息 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/11
 */
class RecordInfoViewModel(private val local: LocalDataStore) : BaseViewModel() {

    lateinit var record: RecordEntity

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

    }
}