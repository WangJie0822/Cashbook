package cn.wj.android.cashbook.ui.type.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.entity.TypeEntity
import cn.wj.android.cashbook.data.enums.RecordTypeEnum
import cn.wj.android.cashbook.data.enums.TypeEnum
import cn.wj.android.cashbook.data.store.LocalDataStore

/**
 * 消费类型 ViewModel
 */
class ConsumptionTypeViewModel(local: LocalDataStore) : BaseViewModel() {

    /** 记录类型数据 */
    val typeData: MutableLiveData<RecordTypeEnum> = MutableLiveData()

    /** 类型列表数据 */
    val typeListData: LiveData<List<TypeEntity>> = typeData.switchMap {
        loadType(it)
    }

    /** 根据记录类型 [recordType] 加载记录分类 */
    private fun loadType(recordType: RecordTypeEnum): LiveData<List<TypeEntity>> {
        val result = MutableLiveData<List<TypeEntity>>()
// TODO
        result.value = arrayListOf<TypeEntity>().apply {
            for (i in 0..10) {
                add(
                    TypeEntity(
                        id = -1L,
                        parentId = -1L,
                        name = recordType.name + i,
                        iconResName = "@drawable/vector_dining_24",
                        type = TypeEnum.FIRST,
                        childList = arrayListOf()
                    )
                )
            }
        }
        return result
    }
}