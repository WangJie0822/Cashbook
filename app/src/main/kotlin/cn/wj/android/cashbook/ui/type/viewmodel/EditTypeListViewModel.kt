package cn.wj.android.cashbook.ui.type.viewmodel

import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import cn.wj.android.cashbook.base.ext.base.orElse
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.constants.ACTION_SELECTED
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_TYPE_EDIT
import cn.wj.android.cashbook.data.entity.TypeEntity
import cn.wj.android.cashbook.data.enums.RecordTypeEnum
import cn.wj.android.cashbook.data.model.UiNavigationModel

/**
 * 编辑分类列表
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/29
 */
class EditTypeListViewModel : BaseViewModel() {

    /** 当前界面下标 */
    val currentItem: MutableLiveData<Int> = MutableLiveData(RecordTypeEnum.EXPENDITURE.position)

    /** 返回按钮点击 */
    val onBackClick: () -> Unit = {
        // 退出当前界面
        uiNavigationEvent.value = UiNavigationModel.builder {
            close()
        }
    }

    /** 添加大类点击 */
    val onAddFirstTypeClick: () -> Unit = {
        uiNavigationEvent.value = UiNavigationModel.builder {
            jump(
                ROUTE_PATH_TYPE_EDIT, bundleOf(
                    ACTION_SELECTED to TypeEntity.empty()
                        .copy(recordType = RecordTypeEnum.fromPosition(currentItem.value.orElse(RecordTypeEnum.EXPENDITURE.position)).orElse(RecordTypeEnum.EXPENDITURE))
                )
            )
        }
    }
}