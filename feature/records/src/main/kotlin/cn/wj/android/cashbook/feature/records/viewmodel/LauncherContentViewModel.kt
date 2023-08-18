package cn.wj.android.cashbook.feature.records.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.common.ext.decimalFormat
import cn.wj.android.cashbook.core.common.ext.toBigDecimalOrZero
import cn.wj.android.cashbook.core.model.entity.RecordDayEntity
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.domain.usecase.GetCurrentMonthRecordViewsMapUseCase
import cn.wj.android.cashbook.domain.usecase.GetCurrentMonthRecordViewsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class LauncherContentViewModel @Inject constructor(
    getCurrentMonthRecordViewsUseCase: GetCurrentMonthRecordViewsUseCase,
    getCurrentMonthRecordViewsMapUseCase: GetCurrentMonthRecordViewsMapUseCase,
) : ViewModel() {

    /** 删除记录失败错误信息 */
    var shouldDisplayDeleteFailedBookmark by mutableStateOf(0)
        private set

    /** 弹窗状态 */
    var dialogState by mutableStateOf<DialogState>(DialogState.Dismiss)
        private set

    /** 记录详情数据 */
    var viewRecord by mutableStateOf<RecordViewsEntity?>(null)
        private set

    /** 当前选择时间 */
    private val _dateData = MutableStateFlow(YearMonth.now())
    val dateData: StateFlow<YearMonth> = _dateData

    /** 当前月记录数据 */
    private val _currentMonthRecordListData = _dateData.flatMapLatest { date ->
        getCurrentMonthRecordViewsUseCase(date.year.toString(), date.monthValue.toString())
    }

    /** 界面 UI 状态 */
    val uiState = _currentMonthRecordListData
        .mapLatest { list ->
            val recordMap = getCurrentMonthRecordViewsMapUseCase(list)
            var totalIncome = BigDecimal.ZERO
            var totalExpenditure = BigDecimal.ZERO
            recordMap.keys.forEach {
                totalIncome += it.dayIncome.toBigDecimalOrZero()
                totalExpenditure += it.dayExpand.toBigDecimalOrZero()
            }
            LauncherContentUiState.Success(
                monthIncome = totalIncome.decimalFormat(),
                monthExpand = totalExpenditure.decimalFormat(),
                monthBalance = (totalIncome - totalExpenditure).decimalFormat(),
                recordMap = recordMap,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LauncherContentUiState.Loading,
        )

    /** 隐藏提示 */
    fun dismissBookmark() {
        shouldDisplayDeleteFailedBookmark = 0
    }

    /** 显示记录 [record] 详情 sheet */
    fun displayRecordDetailsSheet(record: RecordViewsEntity) {
        viewRecord = record
    }

    /** 隐藏 sheet */
    fun dismissSheet() {
        viewRecord = null
    }

    /** 显示日期选择弹窗 */
    fun displayDateSelectDialog() {
        viewModelScope.launch {
            dialogState = DialogState.Shown(_dateData.first())
        }
    }

    /** 刷新已选择日期 */
    fun refreshSelectedDate(date: YearMonth) {
        dismissDialog()
        _dateData.tryEmit(date)
    }

    /** 隐藏弹窗 */
    fun dismissDialog() {
        dialogState = DialogState.Dismiss
    }
}

/**
 * 界面 UI 状态
 */
sealed interface LauncherContentUiState {
    /** 加载中 */
    data object Loading : LauncherContentUiState

    /**
     * 加载完成
     *
     * @param monthIncome 月收入
     * @param monthExpand 月支出
     * @param monthBalance 月结余
     * @param recordMap 记录列表数据
     */
    data class Success(
        val monthIncome: String,
        val monthExpand: String,
        val monthBalance: String,
        val recordMap: Map<RecordDayEntity, List<RecordViewsEntity>>,
    ) : LauncherContentUiState
}