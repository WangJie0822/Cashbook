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
import cn.wj.android.cashbook.core.model.model.RECORD_TYPE_BALANCE_EXPENDITURE
import cn.wj.android.cashbook.core.model.model.RECORD_TYPE_BALANCE_INCOME
import cn.wj.android.cashbook.core.testing.data.createAssetModel
import cn.wj.android.cashbook.core.testing.data.createRecordModel
import cn.wj.android.cashbook.core.testing.data.createRecordTypeModel
import cn.wj.android.cashbook.core.testing.data.createTagModel
import cn.wj.android.cashbook.core.testing.repository.FakeAssetRepository
import cn.wj.android.cashbook.core.testing.repository.FakeRecordRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTagRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTypeRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class RecordModelTransToViewsUseCaseTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var recordRepository: FakeRecordRepository
    private lateinit var typeRepository: FakeTypeRepository
    private lateinit var assetRepository: FakeAssetRepository
    private lateinit var tagRepository: FakeTagRepository
    private lateinit var useCase: RecordModelTransToViewsUseCase

    @Before
    fun setup() {
        recordRepository = FakeRecordRepository()
        typeRepository = FakeTypeRepository()
        assetRepository = FakeAssetRepository()
        tagRepository = FakeTagRepository()
        useCase = RecordModelTransToViewsUseCase(
            recordRepository = recordRepository,
            typeRepository = typeRepository,
            assetRepository = assetRepository,
            tagRepository = tagRepository,
            coroutineContext = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun when_balance_income_type_then_uses_built_in_type() = runTest {
        val record = createRecordModel(
            id = 1L,
            typeId = RECORD_TYPE_BALANCE_INCOME.id,
            amount = 10000L,
        )
        recordRepository.addRecord(record)

        val result = useCase(record)

        assertThat(result.type).isEqualTo(RECORD_TYPE_BALANCE_INCOME)
    }

    @Test
    fun when_balance_expenditure_type_then_uses_built_in_type() = runTest {
        val record = createRecordModel(
            id = 1L,
            typeId = RECORD_TYPE_BALANCE_EXPENDITURE.id,
            amount = 10000L,
        )
        recordRepository.addRecord(record)

        val result = useCase(record)

        assertThat(result.type).isEqualTo(RECORD_TYPE_BALANCE_EXPENDITURE)
    }

    @Test
    fun when_normal_type_then_fetches_from_repository() = runTest {
        val type = createRecordTypeModel(id = 1L, name = "餐饮")
        typeRepository.addType(type)
        val record = createRecordModel(id = 1L, typeId = 1L, amount = 5000L)
        recordRepository.addRecord(record)

        val result = useCase(record)

        assertThat(result.type.name).isEqualTo("餐饮")
    }

    @Test
    fun when_has_asset_then_includes_asset_in_result() = runTest {
        val type = createRecordTypeModel(id = 1L)
        typeRepository.addType(type)
        val asset = createAssetModel(id = 10L, name = "支付宝")
        assetRepository.addAsset(asset)
        val record = createRecordModel(id = 1L, typeId = 1L, assetId = 10L)
        recordRepository.addRecord(record)

        val result = useCase(record)

        assertThat(result.asset).isNotNull()
        assertThat(result.asset!!.name).isEqualTo("支付宝")
    }

    @Test
    fun when_has_related_tags_then_includes_tags() = runTest {
        val type = createRecordTypeModel(id = 1L)
        typeRepository.addType(type)
        val tag = createTagModel(id = 1L, name = "日常")
        tagRepository.setRelatedTags(1L, listOf(tag))
        val record = createRecordModel(id = 1L, typeId = 1L)
        recordRepository.addRecord(record)

        val result = useCase(record)

        assertThat(result.relatedTags).hasSize(1)
        assertThat(result.relatedTags.first().name).isEqualTo("日常")
    }

    @Test
    fun given_income_type_when_has_related_records_then_calculates_related_amount() = runTest {
        val type = createRecordTypeModel(
            id = 1L,
            typeCategory = RecordTypeCategoryEnum.INCOME,
        )
        typeRepository.addType(type)
        val record = createRecordModel(id = 1L, typeId = 1L, amount = 20000L)
        recordRepository.addRecord(record)
        // 关联的支出记录
        val relatedRecord = createRecordModel(
            id = 2L,
            typeId = 2L,
            amount = 10000L,
            charges = 500L,
            concessions = 0L,
        )
        recordRepository.addRecord(relatedRecord)
        // 收入类型使用 getRelatedIdListById
        recordRepository.setRelatedIds(1L, listOf(2L))

        val result = useCase(record)

        // 收入类型关联支出: amount + charges - concessions = 10000 + 500 - 0 = 10500
        assertThat(result.relatedAmount).isEqualTo(10500L)
    }

    @Test
    fun given_expenditure_type_when_has_related_records_then_calculates_related_amount() = runTest {
        val type = createRecordTypeModel(
            id = 1L,
            typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
        )
        typeRepository.addType(type)
        val record = createRecordModel(id = 1L, typeId = 1L, amount = 20000L)
        recordRepository.addRecord(record)
        // 关联的收入记录
        val relatedRecord = createRecordModel(id = 2L, typeId = 2L, amount = 8000L)
        recordRepository.addRecord(relatedRecord)
        // 支出类型使用 getRecordIdListFromRelatedId
        recordRepository.setRelatedFromIds(1L, listOf(2L))

        val result = useCase(record)

        // 支出类型关联收入: amount = 8000
        assertThat(result.relatedAmount).isEqualTo(8000L)
    }
}
