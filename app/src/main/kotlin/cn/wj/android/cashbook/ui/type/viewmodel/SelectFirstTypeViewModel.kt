package cn.wj.android.cashbook.ui.type.viewmodel

import androidx.core.os.bundleOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.constants.ACTION_SELECTED
import cn.wj.android.cashbook.data.constants.ACTIVITY_RESULT_OK
import cn.wj.android.cashbook.data.entity.TypeEntity
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.repository.type.TypeRepository
import kotlinx.coroutines.launch

/**
 * 选择一级分类 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/8/3
 */
class SelectFirstTypeViewModel(private val repository: TypeRepository) : BaseViewModel() {

    /** 目标分类 */
    var targetTypeData: MutableLiveData<TypeEntity> = MutableLiveData()

    /** 分类列表数据 */
    val listData: LiveData<List<TypeEntity>> = targetTypeData.switchMap {
        val result = MutableLiveData<List<TypeEntity>>()
        viewModelScope.launch {
            try {
                result.value = repository.getFirstTypeListWithOutType(it)
            } catch (throwable: Throwable) {
                logger().e(throwable, "getFirstTypeListWithOutType")
            }
        }
        result
    }

    /** 返回点击 */
    val onBackClick: () -> Unit = {
        uiNavigationEvent.value = UiNavigationModel.builder {
            close()
        }
    }

    /** 分类列表 item 点击 */
    val onTypeItemClick: (TypeEntity) -> Unit = { item ->
        uiNavigationEvent.value = UiNavigationModel.builder {
            close(
                ACTIVITY_RESULT_OK, bundleOf(
                    ACTION_SELECTED to item
                )
            )
        }
    }
}