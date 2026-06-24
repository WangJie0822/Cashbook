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

package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.model.entity.BudgetStateEnum
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.testing.data.createRecordModel
import cn.wj.android.cashbook.core.testing.data.createRecordTypeModel
import cn.wj.android.cashbook.core.testing.repository.FakeAssetRepository
import cn.wj.android.cashbook.core.testing.repository.FakeBooksRepository
import cn.wj.android.cashbook.core.testing.repository.FakeBudgetRepository
import cn.wj.android.cashbook.core.testing.repository.FakeRecordRepository
import cn.wj.android.cashbook.core.testing.repository.FakeSettingRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTagRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTypeRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * [GetBudgetProgressUseCase] 端到端聚合测试。
 *
 * 默认 monthStartDay=1（FakeSettingRepository）、currentBook.id=1（FakeBooksRepository），
 * 记录 recordTime 默认 2024-01-01，today=2024-01-15 → 周期 [2024-01-01, 2024-02-01)。
 */
class GetBudgetProgressUseCaseTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var recordRepository: FakeRecordRepository
    private lateinit var typeRepository: FakeTypeRepository
    private lateinit var budgetRepository: FakeBudgetRepository
    private lateinit var useCase: GetBudgetProgressUseCase

    private val today = LocalDate.of(2024, 1, 15)

    @Before
    fun setup() {
        recordRepository = FakeRecordRepository()
        typeRepository = FakeTypeRepository()
        budgetRepository = FakeBudgetRepository()
        val transToViews = RecordModelTransToViewsUseCase(
            recordRepository = recordRepository,
            typeRepository = typeRepository,
            assetRepository = FakeAssetRepository(),
            tagRepository = FakeTagRepository(),
            coroutineContext = UnconfinedTestDispatcher(),
        )
        val getRecordViews = GetRecordViewsBetweenDateUseCase(
            recordRepository = recordRepository,
            recordModelTransToViewsUseCase = transToViews,
            coroutineContext = UnconfinedTestDispatcher(),
        )
        val transToPie = TransRecordViewsToAnalyticsPieUseCase(
            typeRepository = typeRepository,
            coroutineContext = UnconfinedTestDispatcher(),
        )
        useCase = GetBudgetProgressUseCase(
            budgetRepository = budgetRepository,
            booksRepository = FakeBooksRepository(),
            settingRepository = FakeSettingRepository(),
            typeRepository = typeRepository,
            getRecordViewsBetweenDateUseCase = getRecordViews,
            transRecordViewsToAnalyticsPieUseCase = transToPie,
            coroutineContext = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun aggregates_overall_and_categories() = runTest {
        typeRepository.addType(createRecordTypeModel(id = 10L, name = "餐饮", typeCategory = RecordTypeCategoryEnum.EXPENDITURE))
        typeRepository.addType(createRecordTypeModel(id = 11L, name = "交通", typeCategory = RecordTypeCategoryEnum.EXPENDITURE))
        // 净自付(EXPENDITURE)=finalAmount：餐饮 700、交通 650
        recordRepository.addRecord(createRecordModel(id = 1L, typeId = 10L, amount = 700L, finalAmount = 700L))
        recordRepository.addRecord(createRecordModel(id = 2L, typeId = 11L, amount = 650L, finalAmount = 650L))
        budgetRepository.upsertBudget(booksId = 1L, typeId = -1L, amount = 2000L)
        budgetRepository.upsertBudget(booksId = 1L, typeId = 10L, amount = 1000L)
        budgetRepository.upsertBudget(booksId = 1L, typeId = 11L, amount = 500L)

        val result = useCase(today = today)

        // 总体 = 直接 Σ EXPENDITURE 净自付
        val overall = result.overall!!
        assertThat(overall.spent).isEqualTo(1350L)
        assertThat(overall.limit).isEqualTo(2000L)
        assertThat(overall.state).isEqualTo(BudgetStateEnum.NORMAL)

        val canting = result.categoryList.first { it.typeId == 10L }
        assertThat(canting.spent).isEqualTo(700L)
        assertThat(canting.state).isEqualTo(BudgetStateEnum.NORMAL) // 70%

        val jiaotong = result.categoryList.first { it.typeId == 11L }
        assertThat(jiaotong.spent).isEqualTo(650L)
        assertThat(jiaotong.state).isEqualTo(BudgetStateEnum.OVER) // 130%
        assertThat(jiaotong.overAmount).isEqualTo(150L)
    }

    @Test
    fun no_overall_budget_returns_null_overall() = runTest {
        typeRepository.addType(createRecordTypeModel(id = 10L, typeCategory = RecordTypeCategoryEnum.EXPENDITURE))
        recordRepository.addRecord(createRecordModel(id = 1L, typeId = 10L, amount = 700L, finalAmount = 700L))
        budgetRepository.upsertBudget(booksId = 1L, typeId = 10L, amount = 1000L) // 仅分类预算

        val result = useCase(today = today)

        assertThat(result.overall).isNull()
        assertThat(result.categoryList).hasSize(1)
    }

    @Test
    fun category_without_record_spent_zero() = runTest {
        typeRepository.addType(createRecordTypeModel(id = 10L, typeCategory = RecordTypeCategoryEnum.EXPENDITURE))
        budgetRepository.upsertBudget(booksId = 1L, typeId = 10L, amount = 1000L) // 设了预算但无记录

        val result = useCase(today = today)

        val canting = result.categoryList.first { it.typeId == 10L }
        assertThat(canting.spent).isEqualTo(0L)
        assertThat(canting.state).isEqualTo(BudgetStateEnum.NORMAL)
    }
}
