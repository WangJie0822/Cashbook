package cn.wj.android.cashbook.ui.type.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.entity.TypeEntity
import cn.wj.android.cashbook.data.enums.RecordTypeEnum
import cn.wj.android.cashbook.data.enums.TypeEnum
import cn.wj.android.cashbook.data.store.LocalDataStore
import cn.wj.android.cashbook.data.transform.toSnackbarModel
import kotlinx.coroutines.launch

/**
 * 消费类型 ViewModel
 */
class ConsumptionTypeViewModel(private val local: LocalDataStore) : BaseViewModel() {

    /** 更新二级类型列表数据 */
    val secondTypeData: MutableLiveData<TypeEntity> = MutableLiveData()

    /** 记录类型数据 */
    val typeData: MutableLiveData<RecordTypeEnum> = MutableLiveData()

    /** 类型列表数据 */
    val typeListData: LiveData<List<TypeEntity>> = typeData.switchMap {
        loadType(it)
    }

    /** 类型 item 点击 */
    val onTypeItemClick: (TypeEntity) -> Unit = { item ->
        // 更新选中状态
        if (item.type == TypeEnum.SECOND) {
            // 二级类型，更新选中状态
            typeListData.value?.firstOrNull {
                it.id == item.parentId
            }?.childList?.forEach { it ->
                it.selected.set(it.id == item.id)
            }
        } else {
            // 一级类型
            if (item.selected.get()) {
                // 已选中，更新折叠状态
                item.expand.set(!item.expand.get())
            } else {
                // 未选中，更新选中状态及折叠状态
                typeListData.value?.forEach {
                    val selected = it.id == item.id
                    it.selected.set(selected)
                    it.expand.set(selected)
                }
            }
            // 更新二级列表
            secondTypeData.value = item
        }
    }

    /** TODO 类型设置点击 */
    val onTypeSettingClick: () -> Unit = {
        snackbarData.value = "类型设置".toSnackbarModel()
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