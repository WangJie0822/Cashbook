package cn.wj.android.cashbook.ui.type.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.constants.EVENT_RECORD_CHANGE
import cn.wj.android.cashbook.data.entity.TypeEntity
import cn.wj.android.cashbook.data.entity.TypeIconEntity
import cn.wj.android.cashbook.data.entity.TypeIconGroupEntity
import cn.wj.android.cashbook.data.enums.TypeEnum
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.repository.type.TypeRepository
import cn.wj.android.cashbook.data.transform.toSnackbarModel
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.launch

/**
 * 编辑分类
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/30
 */
class EditTypeViewModel(private val repository: TypeRepository) : BaseViewModel() {

    /** 当前分类信息 */
    val typeData: MutableLiveData<TypeEntity> = object : MutableLiveData<TypeEntity>(null) {
        override fun setValue(value: TypeEntity?) {
            super.setValue(value)
            typeName.value = value?.name
            iconResStr.value = value?.iconResName
        }
    }

    /** 标题文本 */
    val titleStr: LiveData<String> = typeData.map {
        if (null == it || it.id == -1L) {
            // 新建
            R.string.new_type
        } else {
            // 编辑
            R.string.edit_type
        }.string
    }

    /** 是否显示父类型 */
    val showParent: LiveData<Boolean> = typeData.map {
        null != it && it.type == TypeEnum.SECOND && null != it.parent
    }

    /** 父类型名称 */
    val parentName: LiveData<String> = typeData.map {
        it?.parent?.name.orEmpty()
    }

    /** 类型提示 */
    val typeHint: LiveData<String> = typeData.map {
        if (it?.type == TypeEnum.FIRST) {
            // 一级分类
            R.string.first_type
        } else {
            R.string.second_type
        }.string
    }

    /** 分类名称 */
    val typeName: MutableLiveData<String> = MutableLiveData()

    /** 分类图标资源 */
    val iconResStr: MutableLiveData<String> = MutableLiveData()

    /** 分组列表数据 */
    val groupListData: MutableLiveData<List<TypeIconGroupEntity>> = object : MutableLiveData<List<TypeIconGroupEntity>>() {
        override fun onActive() {
            // 获取数据
            loadTypeIconData()
        }
    }

    /** 分组图标数据 */
    val iconListData: MutableLiveData<List<TypeIconEntity>> = MutableLiveData()

    /** 返回按钮点击 */
    val onBackClick: () -> Unit = {
        // 退出当前界面
        uiNavigationEvent.value = UiNavigationModel.builder {
            close()
        }
    }

    /** 保存点击 */
    val onSaveClick: () -> Unit = {
        checkToSave()
    }

    /** 分组 item 点击 */
    val onGroupItemClick: (TypeIconGroupEntity) -> Unit = { item ->
        groupListData.value?.forEach {
            if (it.name == item.name) {
                it.selected.set(true)
                // 更新图标列表
                iconListData.value = it.icons
            } else {
                it.selected.set(false)
            }
        }
    }

    /** 图标 item 点击 */
    val onIconItemClick: (TypeIconEntity) -> Unit = { item ->
        typeName.value = item.name
        iconResStr.value = item.iconResIdStr
    }

    /** 获取类型图标数据 */
    private fun loadTypeIconData() {
        viewModelScope.launch {
            try {
                val list = repository.getTypeIconData()
                groupListData.value = list
                // 默认选中第一条
                onGroupItemClick.invoke(list.first())
            } catch (throwable: Throwable) {
                logger().e(throwable, "loadTypeIconData")
            }
        }
    }

    /** 检查并保存修改 */
    private fun checkToSave() {
        val typeEntity = typeData.value ?: return
        val name = typeName.value.orEmpty()
        if (name.isBlank()) {
            snackbarEvent.value = R.string.type_name_cannot_be_blank.string.toSnackbarModel()
            return
        }
        val iconResIdStr = iconResStr.value.orEmpty()
        if (iconResIdStr.isBlank()) {
            snackbarEvent.value = R.string.please_select_type_icon.string.toSnackbarModel()
            return
        }
        viewModelScope.launch {
            try {
                if (repository.getTypeCountByName(name) > 0L) {
                    // 已有相同名称
                    snackbarEvent.value = R.string.same_name_type_exist_already.string.toSnackbarModel()
                    return@launch
                }
                if (typeEntity.id == -1L) {
                    // 新建分类
                    repository.insertType(typeEntity.copy(name = name, iconResName = iconResIdStr, sort = repository.getTypeCount().toInt()))
                } else {
                    // 更新分类
                    repository.updateType(typeEntity.copy(name = name, iconResName = iconResIdStr))
                }
                LiveEventBus.get(EVENT_RECORD_CHANGE).post(0)
                uiNavigationEvent.value = UiNavigationModel.builder {
                    close()
                }
            } catch (throwable: Throwable) {
                logger().e(throwable, "checkToSave")
            }
        }
    }
}