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

import cn.wj.android.cashbook.core.common.FIXED_TYPE_ID_REFUND
import cn.wj.android.cashbook.core.common.FIXED_TYPE_ID_REIMBURSE
import cn.wj.android.cashbook.core.model.enums.RecordRelatedNatureEnum
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.RECORD_TYPE_BALANCE_EXPENDITURE
import cn.wj.android.cashbook.core.model.model.RECORD_TYPE_BALANCE_INCOME
import cn.wj.android.cashbook.core.testing.data.createAssetModel
import cn.wj.android.cashbook.core.testing.data.createImageModel
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

    /** 构造一组覆盖多种类型 / 资产 / 关联 / 标签 / 图片的混合数据，供批量等价性测试复用 */
    private fun setupMixedRecords(): List<cn.wj.android.cashbook.core.model.model.RecordModel> {
        // 类型：1=普通支出，2=普通收入，3=收入类型（带关联），4=支出类型（带关联）
        typeRepository.addType(
            createRecordTypeModel(id = 1L, name = "餐饮", typeCategory = RecordTypeCategoryEnum.EXPENDITURE),
        )
        typeRepository.addType(
            createRecordTypeModel(id = 2L, name = "工资", typeCategory = RecordTypeCategoryEnum.INCOME),
        )
        typeRepository.addType(
            createRecordTypeModel(id = 3L, name = "报销", typeCategory = RecordTypeCategoryEnum.INCOME),
        )
        typeRepository.addType(
            createRecordTypeModel(id = 4L, name = "购物", typeCategory = RecordTypeCategoryEnum.EXPENDITURE),
        )

        // 资产
        assetRepository.addAsset(createAssetModel(id = 10L, name = "支付宝"))
        assetRepository.addAsset(createAssetModel(id = 11L, name = "银行卡"))

        // 关联记录（被引用，类型 1 普通支出）
        val relatedExpenditure = createRecordModel(
            id = 100L,
            typeId = 1L,
            amount = 3000L,
            charges = 200L,
            concessions = 100L,
        )
        // 关联记录（被引用，类型 2 普通收入）
        val relatedIncome = createRecordModel(id = 101L, typeId = 2L, amount = 5000L)

        // 主记录们
        val r1 = createRecordModel(id = 1L, typeId = 1L, assetId = 10L, amount = 1000L) // 普通支出，无关联
        val r2 = createRecordModel(id = 2L, typeId = 1L, assetId = 10L, amount = 2000L) // 与 r1 共享类型/资产
        val r3 = createRecordModel(id = 3L, typeId = 3L, assetId = 11L, amount = 8000L) // 收入类型，关联支出 100
        val r4 = createRecordModel(
            id = 4L,
            typeId = 4L,
            assetId = 11L,
            relatedAssetId = 10L,
            amount = 9000L,
        ) // 支出类型，关联收入 101
        val r5 = createRecordModel(
            id = 5L,
            typeId = RECORD_TYPE_BALANCE_INCOME.id,
            amount = 500L,
        ) // 平账收入合成类型

        recordRepository.addRecord(relatedExpenditure)
        recordRepository.addRecord(relatedIncome)
        recordRepository.addRecord(r1)
        recordRepository.addRecord(r2)
        recordRepository.addRecord(r3)
        recordRepository.addRecord(r4)
        recordRepository.addRecord(r5)

        // 收入类型 r3 关联支出 100
        recordRepository.setRelatedIds(3L, listOf(100L))
        // 支出类型 r4 关联收入 101
        recordRepository.setRelatedFromIds(4L, listOf(101L))

        // 标签：仅 r1 有标签
        tagRepository.setRelatedTags(1L, listOf(createTagModel(id = 1L, name = "日常")))
        // 图片：仅 r3 有图片
        recordRepository.setImages(3L, listOf(createImageModel(id = 1L, recordId = 3L, path = "/p")))

        return listOf(r1, r2, r3, r4, r5)
    }

    @Test
    fun batch_produces_field_equivalent_result_to_single() = runTest {
        val records = setupMixedRecords()

        // 单条逐个转换作为黄金基准
        val expected = records.map { useCase(it) }
        // 批量转换
        val actual = useCase(records)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun batch_does_not_query_per_record_but_uses_batch_apis() = runTest {
        val records = setupMixedRecords()

        // 重置计数（setupMixedRecords 不调用查询接口，此处保险起见）
        val imagesSingleBefore = recordRepository.queryImagesByRecordIdCount
        val byIdsBefore = recordRepository.queryByIdsCount
        val imagesBatchBefore = recordRepository.queryImagesByRecordIdsCount

        useCase(records)

        // 批量路径不应逐条调用 queryImagesByRecordId
        assertThat(recordRepository.queryImagesByRecordIdCount).isEqualTo(imagesSingleBefore)
        // 批量路径应调用批量图片查询恰好一次
        assertThat(recordRepository.queryImagesByRecordIdsCount).isEqualTo(imagesBatchBefore + 1)
        // 批量路径应调用批量记录查询恰好一次（解析关联记录）
        assertThat(recordRepository.queryByIdsCount).isEqualTo(byIdsBefore + 1)
    }

    @Test
    fun batch_empty_input_returns_empty() = runTest {
        val result = useCase(emptyList())
        assertThat(result).isEmpty()
    }

    @Test
    fun given_expenditure_type_when_related_income_has_charges_then_related_amount_excludes_charges() = runTest {
        val type = createRecordTypeModel(id = 1L, typeCategory = RecordTypeCategoryEnum.EXPENDITURE)
        typeRepository.addType(type)
        val record = createRecordModel(id = 1L, typeId = 1L, amount = 20000L)
        recordRepository.addRecord(record)
        // 关联的收入记录（报销款）带手续费 500
        val relatedRecord = createRecordModel(id = 2L, typeId = 2L, amount = 8000L, charges = 500L)
        recordRepository.addRecord(relatedRecord)
        recordRepository.setRelatedFromIds(1L, listOf(2L))

        val result = useCase(record)

        // A 修复后：支出主记录关联收入 = recordAmount(INCOME) = amount - charges = 8000 - 500 = 7500
        assertThat(result.relatedAmount).isEqualTo(7500L)
    }

    @Test
    fun given_expenditure_related_all_reimburse_then_nature_reimbursed() = runTest {
        val type = createRecordTypeModel(id = 1L, typeCategory = RecordTypeCategoryEnum.EXPENDITURE)
        typeRepository.addType(type)
        val record = createRecordModel(id = 1L, typeId = 1L, amount = 10000L)
        recordRepository.addRecord(record)
        // 关联收入为报销类型（固定负 ID）
        recordRepository.addRecord(createRecordModel(id = 2L, typeId = FIXED_TYPE_ID_REIMBURSE, amount = 8000L))
        recordRepository.setRelatedFromIds(1L, listOf(2L))

        assertThat(useCase(record).relatedNature).isEqualTo(RecordRelatedNatureEnum.REIMBURSED)
    }

    @Test
    fun given_expenditure_related_all_refund_then_nature_refunded() = runTest {
        val type = createRecordTypeModel(id = 1L, typeCategory = RecordTypeCategoryEnum.EXPENDITURE)
        typeRepository.addType(type)
        val record = createRecordModel(id = 1L, typeId = 1L, amount = 10000L)
        recordRepository.addRecord(record)
        recordRepository.addRecord(createRecordModel(id = 2L, typeId = FIXED_TYPE_ID_REFUND, amount = 8000L))
        recordRepository.setRelatedFromIds(1L, listOf(2L))

        assertThat(useCase(record).relatedNature).isEqualTo(RecordRelatedNatureEnum.REFUNDED)
    }

    @Test
    fun given_expenditure_related_mixed_then_nature_mixed() = runTest {
        val type = createRecordTypeModel(id = 1L, typeCategory = RecordTypeCategoryEnum.EXPENDITURE)
        typeRepository.addType(type)
        val record = createRecordModel(id = 1L, typeId = 1L, amount = 10000L)
        recordRepository.addRecord(record)
        recordRepository.addRecord(createRecordModel(id = 2L, typeId = FIXED_TYPE_ID_REIMBURSE, amount = 4000L))
        recordRepository.addRecord(createRecordModel(id = 3L, typeId = FIXED_TYPE_ID_REFUND, amount = 3000L))
        recordRepository.setRelatedFromIds(1L, listOf(2L, 3L))

        assertThat(useCase(record).relatedNature).isEqualTo(RecordRelatedNatureEnum.MIXED)
    }

    @Test
    fun given_expenditure_no_related_then_nature_none() = runTest {
        val type = createRecordTypeModel(id = 1L, typeCategory = RecordTypeCategoryEnum.EXPENDITURE)
        typeRepository.addType(type)
        val record = createRecordModel(id = 1L, typeId = 1L, amount = 10000L)
        recordRepository.addRecord(record)

        assertThat(useCase(record).relatedNature).isEqualTo(RecordRelatedNatureEnum.NONE)
    }

    @Test
    fun given_income_absorber_with_related_then_nature_none() = runTest {
        // 收入吸收者（报销款主记录）relatedNature 恒 NONE（仅被吸收支出有性质）
        val type = createRecordTypeModel(id = 1L, typeCategory = RecordTypeCategoryEnum.INCOME)
        typeRepository.addType(type)
        val record = createRecordModel(id = 1L, typeId = 1L, amount = 8000L)
        recordRepository.addRecord(record)
        recordRepository.addRecord(createRecordModel(id = 2L, typeId = 5L, amount = 10000L))
        recordRepository.setRelatedIds(1L, listOf(2L))

        assertThat(useCase(record).relatedNature).isEqualTo(RecordRelatedNatureEnum.NONE)
    }
}
