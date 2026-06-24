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

import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.testing.data.createRecordTypeModel
import cn.wj.android.cashbook.core.testing.repository.FakeAssetRepository
import cn.wj.android.cashbook.core.testing.repository.FakeBooksRepository
import cn.wj.android.cashbook.core.testing.repository.FakeBudgetRepository
import cn.wj.android.cashbook.core.testing.repository.FakeRecordRepository
import cn.wj.android.cashbook.core.testing.repository.FakeSettingRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTagRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTypeRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import cn.wj.android.cashbook.domain.usecase.GetBudgetProgressUseCase
import cn.wj.android.cashbook.domain.usecase.GetRecordViewsBetweenDateUseCase
import cn.wj.android.cashbook.domain.usecase.RecordModelTransToViewsUseCase
import cn.wj.android.cashbook.domain.usecase.TransRecordViewsToAnalyticsPieUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * [BudgetViewModel] 单元测试。
 */
class BudgetViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var recordRepository: FakeRecordRepository
    private lateinit var typeRepository: FakeTypeRepository
    private lateinit var budgetRepository: FakeBudgetRepository

    private fun buildViewModel(): BudgetViewModel {
        recordRepository = FakeRecordRepository()
        typeRepository = FakeTypeRepository()
        budgetRepository = FakeBudgetRepository()
        val booksRepository = FakeBooksRepository()
        val settingRepository = FakeSettingRepository()
        val transToViews = RecordModelTransToViewsUseCase(
            recordRepository = recordRepository,
            typeRepository = typeRepository,
            assetRepository = FakeAssetRepository(),
            tagRepository = FakeTagRepository(),
            coroutineContext = dispatcherRule.testDispatcher,
        )
        val getRecordViews = GetRecordViewsBetweenDateUseCase(
            recordRepository = recordRepository,
            recordModelTransToViewsUseCase = transToViews,
            coroutineContext = dispatcherRule.testDispatcher,
        )
        val transToPie = TransRecordViewsToAnalyticsPieUseCase(
            typeRepository = typeRepository,
            coroutineContext = dispatcherRule.testDispatcher,
        )
        val getBudgetProgress = GetBudgetProgressUseCase(
            budgetRepository = budgetRepository,
            booksRepository = booksRepository,
            settingRepository = settingRepository,
            typeRepository = typeRepository,
            getRecordViewsBetweenDateUseCase = getRecordViews,
            transRecordViewsToAnalyticsPieUseCase = transToPie,
            coroutineContext = dispatcherRule.testDispatcher,
        )
        return BudgetViewModel(
            getBudgetProgressUseCase = getBudgetProgress,
            budgetRepository = budgetRepository,
            booksRepository = booksRepository,
            settingRepository = settingRepository,
            typeRepository = typeRepository,
        )
    }

    @Test
    fun set_budget_appears_in_state() = runTest {
        val viewModel = buildViewModel()
        typeRepository.addType(createRecordTypeModel(id = 10L, typeCategory = RecordTypeCategoryEnum.EXPENDITURE))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.onSetBudget(typeId = 10L, input = "1000")

        val state = viewModel.uiState.value as BudgetUiState.Success
        assertThat(state.progress.categoryList.any { it.typeId == 10L && it.limit == 100000L }).isTrue()
    }

    @Test
    fun invalid_input_not_persisted() = runTest {
        val viewModel = buildViewModel()
        typeRepository.addType(createRecordTypeModel(id = 10L, typeCategory = RecordTypeCategoryEnum.EXPENDITURE))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.onSetBudget(typeId = 10L, input = "0") // ≤0 非法
        viewModel.onSetBudget(typeId = 10L, input = "abc") // 非数字

        val state = viewModel.uiState.value as BudgetUiState.Success
        assertThat(state.progress.categoryList.any { it.typeId == 10L }).isFalse()
    }

    @Test
    fun addable_excludes_set_and_fixed_types() = runTest {
        val viewModel = buildViewModel()
        // 一级支出：餐饮(10)、交通(11)、平账(-1101 固定负 id)
        typeRepository.addType(createRecordTypeModel(id = 10L, typeCategory = RecordTypeCategoryEnum.EXPENDITURE))
        typeRepository.addType(createRecordTypeModel(id = 11L, typeCategory = RecordTypeCategoryEnum.EXPENDITURE))
        typeRepository.addType(createRecordTypeModel(id = -1101L, typeCategory = RecordTypeCategoryEnum.EXPENDITURE))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.onSetBudget(typeId = 10L, input = "1000") // 已设餐饮

        val state = viewModel.uiState.value as BudgetUiState.Success
        val addableIds = state.addableTypes.map { it.id }
        assertThat(addableIds).containsExactly(11L) // 排除已设(10)+固定负id(-1101)
    }

    @Test
    fun delete_budget_removes_from_state() = runTest {
        val viewModel = buildViewModel()
        typeRepository.addType(createRecordTypeModel(id = 10L, typeCategory = RecordTypeCategoryEnum.EXPENDITURE))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        viewModel.onSetBudget(typeId = 10L, input = "1000")
        assertThat((viewModel.uiState.value as BudgetUiState.Success).progress.categoryList).isNotEmpty()

        viewModel.onDeleteBudget(typeId = 10L)

        val state = viewModel.uiState.value as BudgetUiState.Success
        assertThat(state.progress.categoryList.any { it.typeId == 10L }).isFalse()
    }
}
