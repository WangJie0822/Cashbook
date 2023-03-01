package cn.wj.android.cashbook.feature.records.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.common.ext.decimalFormat
import cn.wj.android.cashbook.core.common.ext.toBigDecimalOrZero
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 启动页内容 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/15
 */
@HiltViewModel
class LauncherContentViewModel @Inject constructor() : ViewModel() {

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

}