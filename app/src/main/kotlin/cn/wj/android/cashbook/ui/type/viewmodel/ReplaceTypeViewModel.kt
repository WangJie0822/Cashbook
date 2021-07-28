package cn.wj.android.cashbook.ui.type.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.constants.EVENT_TYPE_CHANGE
import cn.wj.android.cashbook.data.entity.TypeEntity
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.repository.type.TypeRepository
import cn.wj.android.cashbook.data.transform.toSnackbarModel
import com.google.android.material.snackbar.Snackbar
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.launch

/**
 * 分类替换 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/7/2
 */
class ReplaceTypeViewModel(private val repository: TypeRepository) : BaseViewModel() {

    /** 分类数据 */
    val typeData: MutableLiveData<TypeEntity> = MutableLiveData()

    /** 列表数据 */
    val listData: LiveData<List<TypeEntity>> = typeData.switchMap {
        val result = MutableLiveData<List<TypeEntity>>()
        viewModelScope.launch {
            try {
                result.value = repository.getReplaceTypeListByType(it)
            } catch (throwable: Throwable) {
                logger().e(throwable, "getRecordCountByType")
            }
        }
        result
    }

    /** 提示文本 */
    val hintStr: LiveData<String> = typeData.switchMap {
        val result = MutableLiveData<String>()
        viewModelScope.launch {
            try {
                result.value = R.string.replace_type_hint_format.string.format(repository.getRecordCountByType(it))
            } catch (throwable: Throwable) {
                logger().e(throwable, "getRecordCountByType")
            }
        }
        result
    }

    /** 当前分类图标 */
    val oldTypeIconResStr: LiveData<String> = typeData.map {
        it.iconResName
    }

    /** 当前分类名称 */
    val oldTypeName: LiveData<String> = typeData.map {
        it.name
    }

    /** 目标分类数据 */
    private val newTypeData: MutableLiveData<TypeEntity> = MutableLiveData()

    /** 当前分类图标 */
    val newTypeIconResStr: LiveData<String> = newTypeData.map {
        it.iconResName
    }

    /** 当前分类名称 */
    val newTypeName: LiveData<String> = newTypeData.map {
        it.name
    }

    /** 返回点击 */
    val onBackClick: () -> Unit = {
        uiNavigationEvent.value = UiNavigationModel.builder {
            close()
        }
    }

    /** 保存点击 */
    val onSaveClick: () -> Unit = {
        checkToSave()
    }

    /** 列表 item 点击 */
    val onTypeItemClick: (TypeEntity) -> Unit = { item ->
        newTypeData.value = item
        listData.value?.forEach {
            it.selected.set(it.id == item.id)
        }
    }

    /** 检查并保存 */
    private fun checkToSave() {
        val old = typeData.value ?: return
        val new = newTypeData.value
        if (null == new) {
            snackbarEvent.value = R.string.please_select_new_type.string.toSnackbarModel()
            return
        }
        viewModelScope.launch {
            try {
                // 更新记录数据
                repository.updateRecordTypes(old, new)
                // 删除分类
                repository.deleteType(old)
                // 修改成功
                LiveEventBus.get(EVENT_TYPE_CHANGE).post(0)
                snackbarEvent.value = R.string.modify_success.string.toSnackbarModel(onCallback = object : Snackbar.Callback() {
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        uiNavigationEvent.value = UiNavigationModel.builder {
                            close()
                        }
                    }
                })
            } catch (throwable: Throwable) {
                logger().e(throwable, "checkToSave")
            }
        }
    }
}