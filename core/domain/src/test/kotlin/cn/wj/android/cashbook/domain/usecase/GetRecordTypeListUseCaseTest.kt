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
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.enums.TypeLevelEnum
import cn.wj.android.cashbook.core.testing.data.createRecordTypeModel
import cn.wj.android.cashbook.core.testing.repository.FakeTypeRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class GetRecordTypeListUseCaseTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var typeRepository: FakeTypeRepository
    private lateinit var useCase: GetRecordTypeListUseCase

    @Before
    fun setup() {
        typeRepository = FakeTypeRepository()
        useCase = GetRecordTypeListUseCase(
            typeRepository = typeRepository,
            ioCoroutineContext = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun when_expenditure_category_then_returns_expenditure_types() = runTest {
        typeRepository.addType(
            createRecordTypeModel(
                id = 1L,
                name = "餐饮",
                typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
            ),
        )
        typeRepository.addType(
            createRecordTypeModel(
                id = 2L,
                name = "工资",
                typeCategory = RecordTypeCategoryEnum.INCOME,
            ),
        )

        val result = useCase(RecordTypeCategoryEnum.EXPENDITURE, -1L)

        // 至少包含餐饮类型和末尾设置项
        assertThat(result.any { it.name == "餐饮" }).isTrue()
        assertThat(result.none { it.name == "工资" }).isTrue()
    }

    @Test
    fun when_has_second_level_types_then_included_after_selected_parent() = runTest {
        typeRepository.addType(
            createRecordTypeModel(
                id = 1L,
                name = "餐饮",
                typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
            ),
        )
        typeRepository.addType(
            createRecordTypeModel(
                id = 10L,
                parentId = 1L,
                name = "午餐",
                typeLevel = TypeLevelEnum.SECOND,
                typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
            ),
        )

        val result = useCase(RecordTypeCategoryEnum.EXPENDITURE, 1L)

        // 餐饮被选中后，其二级分类应出现在结果中
        assertThat(result.any { it.name == "午餐" }).isTrue()
    }

    @Test
    fun when_selected_type_exists_then_marked_as_selected() = runTest {
        typeRepository.addType(
            createRecordTypeModel(
                id = 1L,
                name = "餐饮",
                typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
            ),
        )
        typeRepository.addType(
            createRecordTypeModel(
                id = 2L,
                name = "交通",
                typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
            ),
        )

        val result = useCase(RecordTypeCategoryEnum.EXPENDITURE, 2L)

        val transportType = result.first { it.name == "交通" }
        assertThat(transportType.selected).isTrue()
    }

    @Test
    fun when_last_item_then_is_settings_entity() = runTest {
        typeRepository.addType(
            createRecordTypeModel(
                id = 1L,
                name = "餐饮",
                typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
            ),
        )

        val result = useCase(RecordTypeCategoryEnum.EXPENDITURE, 1L)

        // 最后一项应该是设置项（id = -1001L）
        assertThat(result.last().id).isEqualTo(-1001L)
    }

    @Test
    fun given_income_category_when_has_need_related_types_then_marked() = runTest {
        // 使用固定退款类型 ID，自动判断为 needRelated
        typeRepository.addType(
            createRecordTypeModel(
                id = FIXED_TYPE_ID_REFUND,
                name = "退款",
                typeCategory = RecordTypeCategoryEnum.INCOME,
            ),
        )

        val result = useCase(RecordTypeCategoryEnum.INCOME, FIXED_TYPE_ID_REFUND)

        val refundType = result.firstOrNull { it.name == "退款" }
        assertThat(refundType).isNotNull()
        assertThat(refundType!!.needRelated).isTrue()
    }
}
