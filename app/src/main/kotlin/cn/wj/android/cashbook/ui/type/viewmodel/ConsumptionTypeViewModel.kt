package cn.wj.android.cashbook.ui.type.viewmodel

import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.base.ext.base.orElse
import cn.wj.android.cashbook.base.tools.mutableLiveDataOf
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.constants.ACTION_SELECTED
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_TYPE_LIST_EDIT
import cn.wj.android.cashbook.data.entity.TypeEntity
import cn.wj.android.cashbook.data.enums.RecordTypeEnum
import cn.wj.android.cashbook.data.enums.TypeEnum
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.repository.type.TypeRepository
import kotlinx.coroutines.launch

/**
 * 消费类型 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/28
 */
class ConsumptionTypeViewModel(private val repository: TypeRepository) : BaseViewModel() {

    /** 更新二级类型列表数据 */
    val secondTypeData: MutableLiveData<TypeEntity?> = MutableLiveData()

    /** 选中类型数据 */
    val selectTypeData: MutableLiveData<TypeEntity> = MutableLiveData()

    /** 记录类型数据 */
    val typeData: MutableLiveData<RecordTypeEnum> = mutableLiveDataOf {
        if (null != value) {
            loadType()
        }
    }

    /** 类型列表数据 */
    val typeListData: MutableLiveData<List<TypeEntity>?> = MutableLiveData()

    /** 类型 item 点击 */
    val onTypeItemClick: (TypeEntity) -> Unit = { item ->
        // 更新选中状态
        // 标记 - 是否是取消二级分类
        var selectParent: TypeEntity? = null
        if (item.type == TypeEnum.SECOND) {
            // 二级类型，更新选中状态
            typeListData.value?.firstOrNull {
                it.id == item.parent?.id
            }?.let { parent ->
                if (parent.selected.get() && parent.expand.get()) {
                    // 已选中且已展开，更新选中状态
                    parent.childList.forEach {
                        if (it.id == item.id) {
                            // 点击的分类
                            if (it.selected.get()) {
                                // 取消选中
                                it.selected.set(false)
                                selectParent = parent
                            } else {
                                it.selected.set(true)
                            }
                        } else {
                            // 取消选中其他
                            it.selected.set(false)
                        }
                    }
                } else {
                    // 更新选中状态及折叠状态
                    typeListData.value?.forEach {
                        val selected = it.id == parent.id
                        it.selected.set(selected)
                        it.expand.set(selected)
                    }
                    parent.childList.forEach {
                        if (it.id == item.id) {
                            // 点击的分类
                            if (it.selected.get()) {
                                // 取消选中
                                it.selected.set(false)
                                selectParent = parent
                            } else {
                                it.selected.set(true)
                            }
                        } else {
                            // 取消选中其他
                            it.selected.set(false)
                        }
                    }
                    secondTypeData.value = parent
                }
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
            secondTypeData.value = typeListData.value?.firstOrNull { it.id == item.id }
        }

        // selectParent 不为空代表取消二级分类选择，使用父类型
        selectTypeData.value = selectParent ?: item
    }

    /** 类型设置点击 */
    val onTypeSettingClick: () -> Unit = {
        // 跳转类型设置
        uiNavigationEvent.value = UiNavigationModel.builder {
            jump(
                ROUTE_PATH_TYPE_LIST_EDIT, bundleOf(
                    ACTION_SELECTED to typeData.value?.position.orElse(RecordTypeEnum.EXPENDITURE.position)
                )
            )
        }
    }

    /** 加载记录分类数据 */
    fun loadType() {
        val recordType = typeData.value ?: return
        viewModelScope.launch {
            try {
                typeListData.value = repository.getTypeListByType(recordType)
            } catch (throwable: Throwable) {
                logger().e(throwable, "loadType")
            }
        }
    }
}