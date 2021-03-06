package cn.wj.android.cashbook.ui.type.viewmodel

import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.base.ext.base.orElse
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.ext.base.toNewList
import cn.wj.android.cashbook.base.tools.mutableLiveDataOf
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.constants.*
import cn.wj.android.cashbook.data.entity.TypeEntity
import cn.wj.android.cashbook.data.enums.RecordTypeEnum
import cn.wj.android.cashbook.data.enums.TypeEnum
import cn.wj.android.cashbook.data.event.LifecycleEvent
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.repository.type.TypeRepository
import cn.wj.android.cashbook.data.transform.toSnackbarModel
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.launch

/**
 * 分类列表
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/29
 */
class TypeListViewModel(private val repository: TypeRepository) : BaseViewModel() {

    /** 记录展开类型的 id */
    private var expandTypeId = -1L

    /** 显示编辑类型菜单弹窗事件 */
    val showEditTypeMenuEvent: LifecycleEvent<TypeEntity> = LifecycleEvent()

    /** 保存点击事件 */
    val saveEvent: LifecycleEvent<Int> = LifecycleEvent()

    /** 标记 - 是否是编辑状态 */
    val edit: MutableLiveData<Boolean> = MutableLiveData(false)

    /** 分类大类 */
    val typeData: MutableLiveData<RecordTypeEnum> = mutableLiveDataOf {
        loadTypeList()
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
            if (it.expand.get()) {
                expandTypeId = it.id
            }
        }
    }

    /** 分类菜单点击 */
    val onTypeMenuClick: (TypeEntity) -> Unit = { item ->
        showEditTypeMenuEvent.value = item
    }

    /** 添加二级分类点击 */
    val onAddSecondTypeClick: (TypeEntity) -> Unit = { first ->
        uiNavigationEvent.value = UiNavigationModel.builder {
            jump(
                ROUTE_PATH_TYPE_EDIT, bundleOf(
                    ACTION_SELECTED to TypeEntity.empty()
                        .copy(parent = first, type = TypeEnum.SECOND, recordType = typeData.value.orElse(RecordTypeEnum.EXPENDITURE))
                )
            )
        }
    }

    /** 加载分类列表数据 */
    fun loadTypeList() {
        viewModelScope.launch {
            try {
                val typeList = repository.getTypeListByType(typeData.value.orElse(RecordTypeEnum.EXPENDITURE))
                // 修正已展开的数据
                typeList.firstOrNull {
                    it.id == expandTypeId
                }?.expand?.set(true)
                listData.value = typeList
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
                repository.updateTypes(ls)
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
        if (type.refund || type.reimburse) {
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
                // 检查是否存在记录
                if (repository.getRecordCountByType(type) > 0) {
                    // 跳转替换分类
                    uiNavigationEvent.value = UiNavigationModel.builder {
                        jump(
                            ROUTE_PATH_TYPE_REPLACE, bundleOf(
                                ACTION_SELECTED to type
                            )
                        )
                    }
                    return@launch
                }
                repository.deleteType(type)
                // 删除成功，刷新列表
                LiveEventBus.get<Int>(EVENT_RECORD_CHANGE).post(0)
                snackbarEvent.value = R.string.delete_success.string.toSnackbarModel()
            } catch (throwable: Throwable) {
                logger().e(throwable, "deleteType")
            }
        }
    }

    /** 将  [type] 修改为一级分类 */
    fun changeToFirstType(type: TypeEntity) {
        viewModelScope.launch {
            try {
                val changed = type.copy(parent = null, type = TypeEnum.FIRST, sort = repository.getTypeCount().toInt())
                repository.updateType(changed)
                // 修改成功，刷新列表
                LiveEventBus.get<Int>(EVENT_RECORD_CHANGE).post(0)
                snackbarEvent.value = R.string.update_success.string.toSnackbarModel()
            } catch (throwable: Throwable) {
                logger().e(throwable, "changeToFirstType")
            }
        }
    }

    /** 处理选择一级分类返回 */
    fun disposeForResult(target: TypeEntity, first: TypeEntity) {
        viewModelScope.launch {
            try {
                val changed = if (target.first) {
                    // 一级分类，修改为二级分类
                    target.copy(parent = first, type = TypeEnum.SECOND, sort = repository.getTypeCount().toInt())
                } else {
                    // 二级分类，移动到其它一级分类
                    target.copy(parent = first, sort = repository.getTypeCount().toInt())
                }
                repository.updateType(changed)
                // 修改成功，刷新列表
                LiveEventBus.get<Int>(EVENT_RECORD_CHANGE).post(0)
                snackbarEvent.value = R.string.update_success.string.toSnackbarModel()
            } catch (throwable: Throwable) {
                logger().e(throwable, "changeToFirstType")
            }
        }
    }
}