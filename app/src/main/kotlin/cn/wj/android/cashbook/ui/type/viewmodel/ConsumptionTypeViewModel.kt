package cn.wj.android.cashbook.ui.type.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.entity.TypeEntity
import cn.wj.android.cashbook.data.enums.RecordTypeEnum
import cn.wj.android.cashbook.data.store.LocalDataStore
import kotlinx.coroutines.launch

/**
 * 消费类型 ViewModel
 */
class ConsumptionTypeViewModel(private val local: LocalDataStore) : BaseViewModel() {

    /** 记录类型数据 */
    val typeData: MutableLiveData<RecordTypeEnum> = MutableLiveData()

    /** 类型列表数据 */
    val typeListData: LiveData<List<TypeEntity>> = typeData.switchMap {
        loadType(it)
    }

    /** 根据记录类型 [recordType] 加载记录分类 */
    private fun loadType(recordType: RecordTypeEnum): LiveData<List<TypeEntity>> {
        val result = MutableLiveData<List<TypeEntity>>()
        viewModelScope.launch {
            try {
                result.value = local.getTypeListByType(recordType)
            } catch (throwable: Throwable) {
                logger().e(throwable, "loadType")
            }
        }
        return result
    }
}