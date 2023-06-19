package cn.wj.android.cashbook.feature.records.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.common.ext.decimalFormat
import cn.wj.android.cashbook.core.common.ext.toBigDecimalOrZero
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.ResultModel
import cn.wj.android.cashbook.domain.usecase.DeleteRecordUseCase
import cn.wj.android.cashbook.domain.usecase.GetCurrentBooksUseCase
import cn.wj.android.cashbook.domain.usecase.GetCurrentMonthRecordViewsUseCase
import cn.wj.android.cashbook.feature.records.model.RecordDialogState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

/**
 * 启动页内容 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/15
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LauncherContentViewModel @Inject constructor(
    getCurrentBooksUseCase: GetCurrentBooksUseCase,
    getCurrentMonthRecordViewsUseCase: GetCurrentMonthRecordViewsUseCase,
    private val deleteRecordUseCase: DeleteRecordUseCase,
) : ViewModel() {

    /** 弹窗状态数据 */
    val dialogState: MutableStateFlow<RecordDialogState> =
        MutableStateFlow(RecordDialogState.Dismiss)

    val bookName: StateFlow<String> = getCurrentBooksUseCase()
        .mapLatest { it.name }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = "",
        )

    val currentMonthRecordListMapData: StateFlow<Map<String, List<RecordViewsEntity>>> =
        getCurrentMonthRecordViewsUseCase()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = mapOf(),
            )

    private val currentMonthRecordListData: Flow<List<RecordViewsEntity>> =
        currentMonthRecordListMapData
            .mapLatest { map ->
                val result = arrayListOf<RecordViewsEntity>()
                map.forEach {
                    result.addAll(it.value)
                }
                result
            }

    val monthIncome: StateFlow<String> = currentMonthRecordListData
        .mapLatest { recordList ->
            var totalIncome = BigDecimal.ZERO
            recordList.forEach { record ->
                when (record.typeCategory) {
                    RecordTypeCategoryEnum.EXPENDITURE -> {
                        // 支出
                    }

                    RecordTypeCategoryEnum.INCOME -> {
                        // 收入
                        totalIncome += (record.amount.toBigDecimalOrZero() - record.charges.toBigDecimalOrZero())
                    }

                    RecordTypeCategoryEnum.TRANSFER -> {
                        // 转账
                        totalIncome += record.concessions.toBigDecimalOrZero()
                    }
                }
            }
            totalIncome.decimalFormat()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = "0",
        )

    val monthExpand: StateFlow<String> = currentMonthRecordListData
        .mapLatest { recordList ->
            var totalExpenditure = BigDecimal.ZERO
            recordList.forEach { record ->
                when (record.typeCategory) {
                    RecordTypeCategoryEnum.EXPENDITURE -> {
                        // 支出
                        totalExpenditure += (record.amount.toBigDecimalOrZero() + record.charges.toBigDecimalOrZero() - record.concessions.toBigDecimalOrZero())
                    }

                    RecordTypeCategoryEnum.INCOME -> {
                        // 收入
                    }

                    RecordTypeCategoryEnum.TRANSFER -> {
                        // 转账
                        totalExpenditure += record.charges.toBigDecimalOrZero()
                    }
                }
            }
            totalExpenditure.decimalFormat()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = "0",
        )

    val monthBalance: StateFlow<String> =
        combine(monthIncome, monthExpand) { income, expand ->
            (income.toBigDecimalOrZero() - expand.toBigDecimalOrZero()).decimalFormat()
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = "0",
            )

    /** 选中的记录数据 */
    val selectedRecordData: MutableStateFlow<RecordViewsEntity?> = MutableStateFlow(null)

    fun onRecordItemClick(recordViewsEntity: RecordViewsEntity) {
        selectedRecordData.value = recordViewsEntity
    }

    fun onRecordDeleteClick(recordId: Long) {
        dialogState.value = RecordDialogState.Show(recordId)
    }

    fun onDismiss() {
        dialogState.value = RecordDialogState.Dismiss
    }

    suspend fun tryDeleteRecord(recordId: Long): ResultModel {
        return try {
            deleteRecordUseCase(recordId)
            ResultModel.success()
        } catch (throwable: Throwable) {
            ResultModel.failure(throwable)
        }
    }
}