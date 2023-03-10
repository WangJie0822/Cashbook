package cn.wj.android.cashbook.feature.records.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.common.ext.decimalFormat
import cn.wj.android.cashbook.core.common.ext.toBigDecimalOrZero
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.model.RecordViewsModel
import cn.wj.android.cashbook.domain.usecase.GetCurrentMonthRecordViewsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/**
 * 启动页内容 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/15
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LauncherContentViewModel @Inject constructor(
    recordRepository: RecordRepository,
    getCurrentMonthRecordViewsUseCase: GetCurrentMonthRecordViewsUseCase,
) : ViewModel() {

    // TODO
    val bookName: StateFlow<String> = flowOf("默认账本")
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = "默认账本",
        )

    val monthIncome: StateFlow<String> = flowOf("0")
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = "0",
        )

    val monthExpand: StateFlow<String> = flowOf("0")
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = "0",
        )

    val monthBalance: StateFlow<String> = combine(monthIncome, monthExpand) { income, expand ->
        (income.toBigDecimalOrZero() - expand.toBigDecimalOrZero()).toPlainString().decimalFormat()
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = "0",
        )

    val currentMonthRecordListData: StateFlow<Map<String, List<RecordViewsEntity>>> =
        getCurrentMonthRecordViewsUseCase()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = mapOf(),
            )
}