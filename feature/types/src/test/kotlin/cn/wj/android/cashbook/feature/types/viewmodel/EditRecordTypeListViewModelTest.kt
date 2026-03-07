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

package cn.wj.android.cashbook.feature.types.viewmodel

import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.enums.TypeLevelEnum
import cn.wj.android.cashbook.core.testing.data.createRecordTypeModel
import cn.wj.android.cashbook.core.testing.repository.FakeTypeRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import cn.wj.android.cashbook.domain.usecase.GetRecordTypeListUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class EditRecordTypeListViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var typeRepository: FakeTypeRepository
    private lateinit var getRecordTypeListUseCase: GetRecordTypeListUseCase
    private lateinit var viewModel: EditRecordTypeListViewModel

    @Before
    fun setup() {
        typeRepository = FakeTypeRepository()
        getRecordTypeListUseCase = GetRecordTypeListUseCase(
            typeRepository = typeRepository,
            ioCoroutineContext = UnconfinedTestDispatcher(),
        )
        viewModel = EditRecordTypeListViewModel(
            typeRepository = typeRepository,
            getRecordTypeListUseCase = getRecordTypeListUseCase,
        )
    }

    @Test
    fun when_initial_state_then_type_category_is_expenditure() {
        assertThat(viewModel.currentTypeCategoryData.value)
            .isEqualTo(RecordTypeCategoryEnum.EXPENDITURE)
    }

    @Test
    fun when_initial_state_then_type_list_is_empty() {
        assertThat(viewModel.typeListData.value).isEmpty()
    }

    @Test
    fun when_initial_state_then_selected_type_id_is_negative_one() {
        assertThat(viewModel.currentSelectedTypeId.value).isEqualTo(-1L)
    }

    @Test
    fun when_update_with_expenditure_category_then_category_changes() = runTest {
        typeRepository.addType(
            createRecordTypeModel(
                id = 1L,
                name = "餐饮",
                typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
            ),
        )

        viewModel.update(RecordTypeCategoryEnum.EXPENDITURE, 1L)

        assertThat(viewModel.currentTypeCategoryData.value)
            .isEqualTo(RecordTypeCategoryEnum.EXPENDITURE)
    }

    @Test
    fun when_update_with_income_category_then_category_changes() = runTest {
        typeRepository.addType(
            createRecordTypeModel(
                id = 5L,
                name = "工资",
                typeCategory = RecordTypeCategoryEnum.INCOME,
            ),
        )

        viewModel.update(RecordTypeCategoryEnum.INCOME, 5L)

        assertThat(viewModel.currentTypeCategoryData.value)
            .isEqualTo(RecordTypeCategoryEnum.INCOME)
    }

    @Test
    fun when_update_with_default_type_id_then_selected_id_updated() = runTest {
        typeRepository.addType(
            createRecordTypeModel(
                id = 3L,
                name = "餐饮",
                typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
            ),
        )

        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.currentSelectedTypeId.collect()
        }

        viewModel.update(RecordTypeCategoryEnum.EXPENDITURE, 3L)

        assertThat(viewModel.currentSelectedTypeId.value).isEqualTo(3L)

        collectJob.cancel()
    }

    @Test
    fun when_update_called_twice_then_default_type_id_only_set_once() = runTest {
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

        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.currentSelectedTypeId.collect()
        }

        // 第一次调用设置默认类型
        viewModel.update(RecordTypeCategoryEnum.EXPENDITURE, 1L)
        assertThat(viewModel.currentSelectedTypeId.value).isEqualTo(1L)

        // 第二次调用不应再更改默认类型
        viewModel.update(RecordTypeCategoryEnum.EXPENDITURE, 2L)
        assertThat(viewModel.currentSelectedTypeId.value).isEqualTo(1L)

        collectJob.cancel()
    }

    @Test
    fun when_update_type_id_then_selected_id_changes() = runTest {
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

        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.currentSelectedTypeId.collect()
        }

        viewModel.update(RecordTypeCategoryEnum.EXPENDITURE, 1L)
        assertThat(viewModel.currentSelectedTypeId.value).isEqualTo(1L)

        // 手动更新选中类型 ID
        viewModel.updateTypeId(2L)
        assertThat(viewModel.currentSelectedTypeId.value).isEqualTo(2L)

        collectJob.cancel()
    }

    @Test
    fun when_update_with_type_data_then_type_list_populated() = runTest {
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

        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.typeListData.collect()
        }

        viewModel.update(RecordTypeCategoryEnum.EXPENDITURE, 1L)

        // 类型列表不为空，包含一级分类、二级分类和设置项
        assertThat(viewModel.typeListData.value).isNotEmpty()

        collectJob.cancel()
    }

    @Test
    fun when_update_to_transfer_category_then_category_is_transfer() = runTest {
        typeRepository.addType(
            createRecordTypeModel(
                id = 1L,
                name = "转账",
                typeCategory = RecordTypeCategoryEnum.TRANSFER,
            ),
        )

        viewModel.update(RecordTypeCategoryEnum.TRANSFER, 1L)

        assertThat(viewModel.currentTypeCategoryData.value)
            .isEqualTo(RecordTypeCategoryEnum.TRANSFER)
    }
}
