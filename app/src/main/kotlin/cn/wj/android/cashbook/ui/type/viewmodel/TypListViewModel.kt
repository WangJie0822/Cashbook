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
import cn.wj.android.cashbook.data.transform.toSnackbarModel
import kotlinx.coroutines.launch

/**
 * 分类列表
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/29
 */
class TypListViewModel(private val local: LocalDataStore) : BaseViewModel() {

    /** 分类大类 */
    val typeData: MutableLiveData<RecordTypeEnum> = MutableLiveData()

    /** 类型数据 */
    val listData: LiveData<List<TypeEntity>> = typeData.switchMap {
        val result = MutableLiveData<List<TypeEntity>>()
        viewModelScope.launch {
            try {
                result.value = local.getTypeListByType(it)
            } catch (throwable: Throwable) {
                logger().e(throwable, "getTypeListByType")
            }
        }
        result
    }

    /** 一级分类点击 */
    val onFirstTypeItemClick: (TypeEntity) -> Unit = { item ->
        listData.value?.forEach {
            if (it.id == item.id && it.showMore) {
                it.expand.set(!it.expand.get())
            } else {
                it.expand.set(false)
            }
        }
    }

    /** 分类菜单点击 */
    val onTypeMenuClick: (TypeEntity) -> Unit = { item ->
        snackbarEvent.value = item.name.toSnackbarModel()
    }

    /** 添加二级分类点击 */
    val onAddSecondTypeClick: (TypeEntity) -> Unit = { first->
        snackbarEvent.value = "添加子类${first.name}".toSnackbarModel()
    }
}