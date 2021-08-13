package cn.wj.android.cashbook.ui.record.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.base.ext.base.completeZero
import cn.wj.android.cashbook.base.ext.base.condition
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.base.ext.base.orElse
import cn.wj.android.cashbook.base.tools.maps
import cn.wj.android.cashbook.base.ui.BaseViewModel
import cn.wj.android.cashbook.data.entity.DateRecordEntity
import cn.wj.android.cashbook.data.entity.RecordEntity
import cn.wj.android.cashbook.data.entity.TypeEntity
import cn.wj.android.cashbook.data.event.LifecycleEvent
import cn.wj.android.cashbook.data.model.UiNavigationModel
import cn.wj.android.cashbook.data.repository.record.RecordRepository
import cn.wj.android.cashbook.interfaces.RecordListClickListener
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * 分类下记录统计 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/8/13
 */
class TypeRecordViewModel(private val repository: RecordRepository) : BaseViewModel(), RecordListClickListener {

    val tabs: List<String> by lazy {
        arrayListOf<String>().apply {
            val calendar = Calendar.getInstance()
            val currentYear = calendar.get(Calendar.YEAR)
            val currentMonth = calendar.get(Calendar.MONTH) + 1
            add(currentYear.toString())
            for (i in currentMonth downTo 1) {
                add("$currentYear-${i.completeZero()}")
            }
            for (i in 1 until 10) {
                val year = currentYear - i
                add(year.toString())
                for (m in 12 downTo 1) {
                    add("$year-${m.completeZero()}")
                }
            }
        }
    }

    /** 显示记录详情弹窗事件 */
    val showRecordDetailsDialogEvent: LifecycleEvent<RecordEntity> = LifecycleEvent()

    /** 类型数据 */
    val typeData: MutableLiveData<TypeEntity> = MutableLiveData()

    /** 选中年份下标 */
    val currentItem: MutableLiveData<Int> = MutableLiveData()

    /** 标题文本 */
    val titleStr: LiveData<String> = maps(typeData, currentItem) {
        "${typeData.value?.name.orEmpty()}(${tabs[currentItem.value.orElse(0)]})"
    }

    /** 列表数据 */
    val listData: LiveData<List<DateRecordEntity>> = titleStr.switchMap {
        val result = MutableLiveData<List<DateRecordEntity>>()
        viewModelScope.launch {
            try {
                val type = typeData.value
                result.value = if (null != type) {
                    repository.getTypeRecordList(type, tabs[currentItem.value.orElse(0)])
                } else {
                    arrayListOf()
                }
            } catch (throwable: Throwable) {
                logger().e(throwable, "getListData")
            } finally {
                refreshing.value = false
            }
        }
        result
    }

    /** 刷新状态 */
    val refreshing: MutableLiveData<Boolean> = object : MutableLiveData<Boolean>(true) {

        override fun setValue(value: Boolean?) {
            super.setValue(value)
            if (value.condition) {
                // 刷新
                currentItem.value = currentItem.value
            }
        }
    }

    /** 返回点击 */
    val onBackClick: () -> Unit = {
        uiNavigationEvent.value = UiNavigationModel.builder {
            close()
        }
    }

    /** 记录点击 */
    override val onRecordItemClick: (RecordEntity) -> Unit = { item ->
        showRecordDetailsDialogEvent.value = item
    }

    override val onRecordItemLongClick: (RecordEntity) -> Unit = {}
}