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
import cn.wj.android.cashbook.core.testing.data.createRecordModel
import cn.wj.android.cashbook.core.testing.data.createRecordTypeModel
import cn.wj.android.cashbook.core.testing.repository.FakeRecordRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTypeRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.feature.types.enums.MyCategoriesBookmarkEnum
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MyCategoriesViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var typeRepository: FakeTypeRepository
    private lateinit var recordRepository: FakeRecordRepository
    private lateinit var viewModel: MyCategoriesViewModel

    @Before
    fun setup() {
        typeRepository = FakeTypeRepository()
        recordRepository = FakeRecordRepository()
        viewModel = MyCategoriesViewModel(
            typeRepository = typeRepository,
            recordRepository = recordRepository,
        )
    }

    @Test
    fun when_initial_state_then_loading() {
        assertThat(viewModel.uiState.value).isEqualTo(MyCategoriesUiState.Loading)
    }

    @Test
    fun when_type_list_emitted_then_state_is_success() = runTest {
        // 添加支出类型
        typeRepository.addType(
            createRecordTypeModel(
                id = 1L,
                name = "餐饮",
                typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
            ),
        )

        // 收集状态以触发 Flow
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect()
        }

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(MyCategoriesUiState.Success::class.java)
        val success = state as MyCategoriesUiState.Success
        assertThat(success.selectedTab).isEqualTo(RecordTypeCategoryEnum.EXPENDITURE)
        assertThat(success.typeList).hasSize(1)
        assertThat(success.typeList.first().data.name).isEqualTo("餐饮")

        collectJob.cancel()
    }

    @Test
    fun when_select_income_category_then_tab_changes() = runTest {
        // 准备收入类型
        typeRepository.addType(
            createRecordTypeModel(
                id = 1L,
                name = "工资",
                typeCategory = RecordTypeCategoryEnum.INCOME,
            ),
        )

        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect()
        }

        // 切换到收入分类
        viewModel.selectTypeCategory(RecordTypeCategoryEnum.INCOME)

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(MyCategoriesUiState.Success::class.java)
        val success = state as MyCategoriesUiState.Success
        assertThat(success.selectedTab).isEqualTo(RecordTypeCategoryEnum.INCOME)
        assertThat(success.typeList).hasSize(1)
        assertThat(success.typeList.first().data.name).isEqualTo("工资")

        collectJob.cancel()
    }

    @Test
    fun when_dismiss_bookmark_then_state_is_dismiss() {
        // 先设置一个提示状态
        viewModel.shouldDisplayBookmark = MyCategoriesBookmarkEnum.PROTECTED_TYPE

        viewModel.dismissBookmark()

        assertThat(viewModel.shouldDisplayBookmark).isEqualTo(MyCategoriesBookmarkEnum.DISMISS)
    }

    @Test
    fun when_dismiss_dialog_then_state_is_dismiss() {
        viewModel.dialogState = DialogState.Shown("test")

        viewModel.dismissDialog()

        assertThat(viewModel.dialogState).isEqualTo(DialogState.Dismiss)
    }

    @Test
    fun when_request_change_first_type_has_children_then_show_bookmark() = runTest {
        // 一级分类
        typeRepository.addType(
            createRecordTypeModel(
                id = 1L,
                name = "餐饮",
                typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
            ),
        )
        // 二级分类
        typeRepository.addType(
            createRecordTypeModel(
                id = 10L,
                parentId = 1L,
                name = "午餐",
                typeLevel = TypeLevelEnum.SECOND,
                typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
            ),
        )

        viewModel.requestChangeFirstTypeToSecond(1L)

        assertThat(viewModel.shouldDisplayBookmark)
            .isEqualTo(MyCategoriesBookmarkEnum.CHANGE_FIRST_TYPE_HAS_CHILD)
    }

    @Test
    fun when_request_change_first_type_no_children_then_show_select_dialog() = runTest {
        // 两个一级分类，互无子分类
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
            viewModel.uiState.collect()
        }

        viewModel.requestChangeFirstTypeToSecond(1L)

        assertThat(viewModel.dialogState).isInstanceOf(DialogState.Shown::class.java)
        val data = (viewModel.dialogState as DialogState.Shown<*>).data
        assertThat(data).isInstanceOf(MyCategoriesDialogData.SelectFirstType::class.java)
        val selectData = data as MyCategoriesDialogData.SelectFirstType
        assertThat(selectData.id).isEqualTo(1L)
        // 排除当前分类，只剩另一个
        assertThat(selectData.typeList).hasSize(1)
        assertThat(selectData.typeList.first().id).isEqualTo(2L)

        collectJob.cancel()
    }

    @Test
    fun when_change_type_to_second_then_dialog_dismissed() = runTest {
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

        viewModel.dialogState = DialogState.Shown("test")
        viewModel.changeTypeToSecond(id = 1L, parentId = 2L)

        assertThat(viewModel.dialogState).isEqualTo(DialogState.Dismiss)
    }

    @Test
    fun when_change_second_type_to_first_then_type_updated() = runTest {
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

        viewModel.changeSecondTypeToFirst(10L)

        // 验证类型已变更（通过 FakeTypeRepository 的实现确认）
        val type = typeRepository.getRecordTypeById(10L)
        assertThat(type).isNotNull()
        assertThat(type!!.typeLevel).isEqualTo(TypeLevelEnum.FIRST)
        assertThat(type.parentId).isEqualTo(-1L)
    }

    @Test
    fun when_request_delete_protected_type_then_show_protected_bookmark() = runTest {
        typeRepository.addType(
            createRecordTypeModel(
                id = 1L,
                name = "餐饮",
                protected = true,
                typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
            ),
        )

        viewModel.requestDeleteType(1L)

        assertThat(viewModel.shouldDisplayBookmark)
            .isEqualTo(MyCategoriesBookmarkEnum.PROTECTED_TYPE)
    }

    @Test
    fun when_request_delete_first_type_with_children_then_show_has_child_bookmark() = runTest {
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

        viewModel.requestDeleteType(1L)

        assertThat(viewModel.shouldDisplayBookmark)
            .isEqualTo(MyCategoriesBookmarkEnum.DELETE_FIRST_TYPE_HAS_CHILD)
    }

    @Test
    fun when_request_delete_type_no_records_then_type_deleted() = runTest {
        typeRepository.addType(
            createRecordTypeModel(
                id = 1L,
                name = "餐饮",
                typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
            ),
        )

        viewModel.requestDeleteType(1L)

        // 该类型没有关联记录，应被直接删除
        val type = typeRepository.getRecordTypeById(1L)
        assertThat(type).isNull()
    }

    @Test
    fun when_request_delete_type_with_records_then_show_delete_dialog() = runTest {
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
        // 添加关联记录
        recordRepository.addRecord(createRecordModel(id = 100L, typeId = 1L))

        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect()
        }

        viewModel.requestDeleteType(1L)

        assertThat(viewModel.dialogState).isInstanceOf(DialogState.Shown::class.java)
        val data = (viewModel.dialogState as DialogState.Shown<*>).data
        assertThat(data).isInstanceOf(MyCategoriesDialogData.DeleteType::class.java)
        val deleteData = data as MyCategoriesDialogData.DeleteType
        assertThat(deleteData.id).isEqualTo(1L)
        assertThat(deleteData.recordSize).isEqualTo(1)

        collectJob.cancel()
    }

    @Test
    fun when_change_record_type_before_delete_then_type_deleted_and_dialog_dismissed() = runTest {
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
        recordRepository.addRecord(createRecordModel(id = 100L, typeId = 1L))

        viewModel.dialogState = DialogState.Shown("test")
        viewModel.changeRecordTypeBeforeDeleteType(id = 1L, toId = 2L)

        // 类型已删除
        assertThat(typeRepository.getRecordTypeById(1L)).isNull()
        // 弹窗已关闭
        assertThat(viewModel.dialogState).isEqualTo(DialogState.Dismiss)
    }

    @Test
    fun when_request_edit_type_then_show_edit_dialog() = runTest {
        typeRepository.addType(
            createRecordTypeModel(
                id = 10L,
                parentId = 1L,
                name = "午餐",
                typeLevel = TypeLevelEnum.SECOND,
                typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
            ),
        )
        typeRepository.addType(
            createRecordTypeModel(
                id = 1L,
                name = "餐饮",
                typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
            ),
        )

        viewModel.requestEditType(id = 10L, parentId = 1L)

        assertThat(viewModel.dialogState).isInstanceOf(DialogState.Shown::class.java)
        val data = (viewModel.dialogState as DialogState.Shown<*>).data
        assertThat(data).isInstanceOf(MyCategoriesDialogData.EditType::class.java)
        val editData = data as MyCategoriesDialogData.EditType
        assertThat(editData.type).isNotNull()
        assertThat(editData.type!!.id).isEqualTo(10L)
        assertThat(editData.parentType).isNotNull()
        assertThat(editData.parentType!!.id).isEqualTo(1L)
    }

    @Test
    fun when_save_record_type_with_duplicate_name_then_show_bookmark() = runTest {
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

        // 尝试将 id=2 的类型改名为已存在的 "餐饮"
        viewModel.saveRecordType(id = 2L, parentId = -1L, name = "餐饮", iconName = "icon")

        assertThat(viewModel.shouldDisplayBookmark)
            .isEqualTo(MyCategoriesBookmarkEnum.DUPLICATE_TYPE_NAME)
    }

    @Test
    fun when_save_record_type_with_same_name_as_self_then_success() = runTest {
        typeRepository.addType(
            createRecordTypeModel(
                id = 1L,
                name = "餐饮",
                typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
            ),
        )

        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect()
        }

        // 保存同名类型（修改图标）
        viewModel.saveRecordType(id = 1L, parentId = -1L, name = "餐饮", iconName = "new_icon")

        // 不应显示重复名称提示
        assertThat(viewModel.shouldDisplayBookmark)
            .isNotEqualTo(MyCategoriesBookmarkEnum.DUPLICATE_TYPE_NAME)
        // 弹窗应关闭
        assertThat(viewModel.dialogState).isEqualTo(DialogState.Dismiss)

        collectJob.cancel()
    }

    @Test
    fun when_save_new_record_type_then_type_created() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect()
        }

        // 保存一个不存在的新类型（id=-1L 表示新建）
        viewModel.saveRecordType(id = -1L, parentId = -1L, name = "新分类", iconName = "new_icon")

        // 弹窗应关闭
        assertThat(viewModel.dialogState).isEqualTo(DialogState.Dismiss)

        collectJob.cancel()
    }

    @Test
    fun when_set_reimburse_type_then_type_updated() = runTest {
        typeRepository.addType(
            createRecordTypeModel(
                id = 1L,
                name = "报销",
                typeCategory = RecordTypeCategoryEnum.INCOME,
            ),
        )

        viewModel.setReimburseType(1L)

        assertThat(typeRepository.isReimburseType(1L)).isTrue()
    }

    @Test
    fun when_set_refund_type_then_type_updated() = runTest {
        typeRepository.addType(
            createRecordTypeModel(
                id = 1L,
                name = "退款",
                typeCategory = RecordTypeCategoryEnum.INCOME,
            ),
        )

        viewModel.setRefundType(1L)

        assertThat(typeRepository.isRefundType(1L)).isTrue()
    }

    @Test
    fun when_set_credit_card_payment_type_then_type_updated() = runTest {
        typeRepository.addType(
            createRecordTypeModel(
                id = 1L,
                name = "还款",
                typeCategory = RecordTypeCategoryEnum.TRANSFER,
            ),
        )

        viewModel.setCreditCardPaymentType(1L)

        assertThat(typeRepository.isCreditPaymentType(1L)).isTrue()
    }

    @Test
    fun when_request_move_second_type_then_show_select_dialog() = runTest {
        // 准备两个一级分类
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
            viewModel.uiState.collect()
        }

        // 请求移动二级分类到其他一级分类
        viewModel.requestMoveSecondTypeToAnother(id = 10L, parentId = 1L)

        assertThat(viewModel.dialogState).isInstanceOf(DialogState.Shown::class.java)
        val data = (viewModel.dialogState as DialogState.Shown<*>).data
        assertThat(data).isInstanceOf(MyCategoriesDialogData.SelectFirstType::class.java)
        val selectData = data as MyCategoriesDialogData.SelectFirstType
        assertThat(selectData.id).isEqualTo(10L)
        // 排除当前父分类，只剩另一个
        assertThat(selectData.typeList).hasSize(1)
        assertThat(selectData.typeList.first().id).isEqualTo(2L)

        collectJob.cancel()
    }
}
