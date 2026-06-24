/*
 * Copyright 2021 The Cashbook Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.wj.android.cashbook.feature.budget.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.common.model.recordDataVersion
import cn.wj.android.cashbook.core.data.repository.BooksRepository
import cn.wj.android.cashbook.core.data.repository.BudgetRepository
import cn.wj.android.cashbook.core.data.repository.SettingRepository
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.model.entity.BudgetProgressEntity
import cn.wj.android.cashbook.core.model.model.RecordTypeModel
import cn.wj.android.cashbook.core.model.model.parseBudgetAmountCent
import cn.wj.android.cashbook.domain.usecase.GetBudgetProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 预算管理 ViewModel。
 *
 * 以当前账本流为外层 [flatMapLatest]，combine(记账版本 / 月起始日 / 预算流 / 一级支出分类)
 * → 调 [GetBudgetProgressUseCase] 聚合本周期预算进度；切账本/记账/改限额/改月起始日均自动重算。
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2026/6/23
 */
@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val getBudgetProgressUseCase: GetBudgetProgressUseCase,
    private val budgetRepository: BudgetRepository,
    private val booksRepository: BooksRepository,
    private val settingRepository: SettingRepository,
    private val typeRepository: TypeRepository,
) : ViewModel() {

    val uiState: StateFlow<BudgetUiState> =
        booksRepository.currentBook.flatMapLatest { book ->
            combine(
                recordDataVersion,
                settingRepository.recordSettingsModel.map { it.monthStartDay }.distinctUntilChanged(),
                budgetRepository.getBudgetsByBooksFlow(book.id),
                typeRepository.firstExpenditureTypeListData,
            ) { _, _, budgets, firstExpenditureTypes ->
                val progress = getBudgetProgressUseCase()
                val setTypeIds = budgets.map { it.typeId }.toSet()
                // 排除固定类型(负 id，如平账 -1101)+已设预算的分类
                val addable = firstExpenditureTypes.filter { it.id > 0 && it.id !in setTypeIds }
                BudgetUiState.Success(progress = progress, addableTypes = addable)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = BudgetUiState.Loading,
        )

    /** 设置/更新限额；非法输入（≤0/超上界/非数字）静默忽略（UI 层校验提示） */
    fun onSetBudget(typeId: Long, input: String) {
        val cent = parseBudgetAmountCent(input) ?: return
        viewModelScope.launch {
            val booksId = booksRepository.currentBook.first().id
            budgetRepository.upsertBudget(booksId, typeId, cent)
        }
    }

    /** 删除单项预算 */
    fun onDeleteBudget(typeId: Long) {
        viewModelScope.launch {
            val booksId = booksRepository.currentBook.first().id
            budgetRepository.deleteBudget(booksId, typeId)
        }
    }
}

/** 预算管理界面状态 */
sealed interface BudgetUiState {

    /** 加载中 */
    data object Loading : BudgetUiState

    /**
     * 加载完成
     *
     * @param progress 本周期预算进度（总体 + 各分类）
     * @param addableTypes 可添加预算的一级支出分类（已排除固定类型与已设预算）
     */
    data class Success(
        val progress: BudgetProgressEntity,
        val addableTypes: List<RecordTypeModel>,
    ) : BudgetUiState
}
