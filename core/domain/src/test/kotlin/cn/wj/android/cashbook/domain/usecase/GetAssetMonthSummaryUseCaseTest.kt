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

import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.testing.data.createRecordModel
import cn.wj.android.cashbook.core.testing.data.createRecordTypeModel
import cn.wj.android.cashbook.core.testing.repository.FakeRecordRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTypeRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class GetAssetMonthSummaryUseCaseTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var recordRepository: FakeRecordRepository
    private lateinit var typeRepository: FakeTypeRepository
    private lateinit var useCase: GetAssetMonthSummaryUseCase

    @Before
    fun setup() {
        recordRepository = FakeRecordRepository()
        typeRepository = FakeTypeRepository()
        useCase = GetAssetMonthSummaryUseCase(
            recordRepository = recordRepository,
            typeRepository = typeRepository,
            coroutineContext = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun normal_asset_income_and_expense() = runTest {
        typeRepository.addType(createRecordTypeModel(id = 1L, typeCategory = RecordTypeCategoryEnum.INCOME))
        typeRepository.addType(createRecordTypeModel(id = 2L, typeCategory = RecordTypeCategoryEnum.EXPENDITURE))
        recordRepository.addRecord(createRecordModel(id = 1L, typeId = 1L, assetId = 5L, amount = 10000L, charges = 0L)) // 收入 +10000
        recordRepository.addRecord(createRecordModel(id = 2L, typeId = 2L, assetId = 5L, amount = 3000L, charges = 0L)) // 支出 -3000

        val result = useCase(assetId = 5L, isCreditCard = false, startDate = 0L, endDate = Long.MAX_VALUE)

        assertThat(result.income).isEqualTo(10000L)
        assertThat(result.expenditure).isEqualTo(3000L)
        assertThat(result.balance).isEqualTo(7000L)
    }

    @Test
    fun transfer_in_counts_as_income() = runTest {
        typeRepository.addType(createRecordTypeModel(id = 3L, typeCategory = RecordTypeCategoryEnum.TRANSFER))
        // 转账：从资产 9 转入资产 5，本金 5000，手续费 100
        recordRepository.addRecord(
            createRecordModel(id = 1L, typeId = 3L, assetId = 9L, relatedAssetId = 5L, amount = 5000L, charges = 100L),
        )

        val result = useCase(assetId = 5L, isCreditCard = false, startDate = 0L, endDate = Long.MAX_VALUE)

        // 资产 5 为转入目标：+amount(5000)，不承担手续费
        assertThat(result.income).isEqualTo(5000L)
        assertThat(result.expenditure).isEqualTo(0L)
        assertThat(result.balance).isEqualTo(5000L)
    }

    @Test
    fun credit_card_expense_direction_reversed() = runTest {
        typeRepository.addType(createRecordTypeModel(id = 2L, typeCategory = RecordTypeCategoryEnum.EXPENDITURE))
        recordRepository.addRecord(createRecordModel(id = 1L, typeId = 2L, assetId = 5L, amount = 3000L, charges = 0L))

        val result = useCase(assetId = 5L, isCreditCard = true, startDate = 0L, endDate = Long.MAX_VALUE)

        // 信用卡支出 delta = +recordAmount → 记为收入侧（余额数值增大方向）
        assertThat(result.balance).isEqualTo(3000L)
        assertThat(result.income).isEqualTo(3000L)
        assertThat(result.expenditure).isEqualTo(0L)
    }
}
