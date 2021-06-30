package cn.wj.android.cashbook.ui.type.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.base.ext.base.orElse
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.ext.base.toNewList
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.entity.TypeEntity
import cn.wj.android.cashbook.data.enums.RecordTypeEnum
import cn.wj.android.cashbook.data.enums.TypeEnum
import cn.wj.android.cashbook.data.event.LifecycleEvent
import cn.wj.android.cashbook.data.store.LocalDataStore
import cn.wj.android.cashbook.data.transform.toSnackbarModel
import kotlinx.coroutines.launch

/**
 * 分类列表
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/29
 */
class TypListViewModel(private val local: LocalDataStore) : BaseViewModel() {

    /** 显示编辑类型菜单弹窗事件 */
    val showEditTypeMenuEvent: LifecycleEvent<TypeEntity> = LifecycleEvent()

    /** 保存点击事件 */
    val saveEvent: LifecycleEvent<Int> = LifecycleEvent()

    /** 标记 - 是否是编辑状态 */
    val edit: MutableLiveData<Boolean> = MutableLiveData(false)

    /** 分类大类 */
    val typeData: MutableLiveData<RecordTypeEnum> = object : MutableLiveData<RecordTypeEnum>() {
        override fun setValue(value: RecordTypeEnum?) {
            super.setValue(value)
            loadTypeList()
        }
    }

    /** 类型数据 */
    val listData: MutableLiveData<List<TypeEntity>> = MutableLiveData()

    /** 取消点击 */
    val onCancelClick: () -> Unit = {
        // 恢复排序
        listData.value = listData.value.toNewList()
        // 退出编辑
        edit.value = false
    }

    /** 保存点击 */
    val onSaveClick: () -> Unit = {
        saveEvent.value = 0
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
        showEditTypeMenuEvent.value = item
    }

    /** 添加二级分类点击 */
    val onAddSecondTypeClick: (TypeEntity) -> Unit = { first ->
        snackbarEvent.value = "添加子类${first.name}".toSnackbarModel()
    }

    private fun loadTypeList() {
        viewModelScope.launch {
            try {
                listData.value = local.getTypeListByType(typeData.value.orElse(RecordTypeEnum.EXPENDITURE))
            } catch (throwable: Throwable) {
                logger().e(throwable, "getTypeListByType")
            }
        }
    }

    /** 更新分类信息 */
    fun updateType(types: List<TypeEntity>) {
        viewModelScope.launch {
            try {
                val ls = arrayListOf<TypeEntity>()
                types.forEachIndexed { index, first ->
                    ls.add(first.copy(sort = index))
                    first.childList.forEachIndexed { i, second ->
                        ls.add(second.copy(sort = i))
                    }
                }
                local.updateTypes(ls)
                // 更新成功，刷新
                loadTypeList()
                // 退出编辑
                edit.value = false
            } catch (throwable: Throwable) {
                logger().e(throwable, "deleteType")
            }
        }
    }

    /** 删除类型 [type] */
    fun deleteType(type: TypeEntity) {
        if (type.system) {
            // 系统类型不支持删除
            snackbarEvent.value = R.string.system_type_does_not_support_deletion.string.toSnackbarModel()
            return
        }
        if (type.type == TypeEnum.FIRST && type.childList.isNotEmpty()) {
            // 一级分类且子分类不为空
            snackbarEvent.value = R.string.has_second_type_can_not_delete.string.toSnackbarModel()
            return
        }
        viewModelScope.launch {
            try {
                local.deleteType(type)
                // 删除成功，刷新列表
                val ls = listData.value.toNewList()
                if (type.type == TypeEnum.FIRST) {
                    // 一级分类，直接移除
                    val index = ls.indexOfFirst { it.id == type.id }
                    if (index >= 0) {
                        ls.removeAt(index)
                    }
                } else {
                    // 二级分类
                    val index = ls.indexOfFirst { it.id == type.parent?.id }
                    if (index >= 0) {
                        val parent = ls[index]
                        val childLs = parent.childList.toNewList()
                        val i = childLs.indexOfFirst { it.id == type.id }
                        if (i >= 0) {
                            childLs.removeAt(i)
                        }
                        ls[index] = parent.copy(childList = childLs)
                    }
                }
                listData.value = ls
            } catch (throwable: Throwable) {
                logger().e(throwable, "deleteType")
            }
        }
    }
}