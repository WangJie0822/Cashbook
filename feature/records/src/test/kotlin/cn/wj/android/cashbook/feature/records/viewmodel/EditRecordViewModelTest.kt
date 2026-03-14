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

package cn.wj.android.cashbook.feature.records.viewmodel

import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.testing.data.createAssetModel
import cn.wj.android.cashbook.core.testing.data.createRecordModel
import cn.wj.android.cashbook.core.testing.data.createRecordTypeModel
import cn.wj.android.cashbook.core.testing.data.createTagModel
import cn.wj.android.cashbook.core.testing.repository.FakeAssetRepository
import cn.wj.android.cashbook.core.testing.repository.FakeRecordRepository
import cn.wj.android.cashbook.core.testing.repository.FakeSettingRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTagRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTypeRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.domain.usecase.GetDefaultRecordUseCase
import cn.wj.android.cashbook.domain.usecase.SaveRecordUseCase
import cn.wj.android.cashbook.feature.records.enums.EditRecordBookmarkEnum
import cn.wj.android.cashbook.feature.records.enums.EditRecordBottomSheetEnum
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * EditRecordViewModel 的单元测试
 *
 * 使用 Robolectric 提供 Android 环境（ImageViewModel 依赖 android.graphics.Bitmap）。
 */
@RunWith(RobolectricTestRunner::class)
class EditRecordViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var typeRepository: FakeTypeRepository
    private lateinit var assetRepository: FakeAssetRepository
    private lateinit var tagRepository: FakeTagRepository
    private lateinit var recordRepository: FakeRecordRepository
    private lateinit var settingRepository: FakeSettingRepository
    private lateinit var viewModel: EditRecordViewModel

    @Before
    fun setup() {
        typeRepository = FakeTypeRepository()
        assetRepository = FakeAssetRepository()
        tagRepository = FakeTagRepository()
        recordRepository = FakeRecordRepository()
        settingRepository = FakeSettingRepository()

        // 添加默认支出类型
        typeRepository.addType(
            createRecordTypeModel(
                id = 1L,
                name = "餐饮",
                typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
            ),
        )
        // 添加收入类型
        typeRepository.addType(
            createRecordTypeModel(
                id = 2L,
                name = "工资",
                typeCategory = RecordTypeCategoryEnum.INCOME,
            ),
        )
        // 添加转账类型
        typeRepository.addType(
            createRecordTypeModel(
                id = 3L,
                name = "转账",
                typeCategory = RecordTypeCategoryEnum.TRANSFER,
            ),
        )

        val getDefaultRecordUseCase = GetDefaultRecordUseCase(
            recordRepository = recordRepository,
            typeRepository = typeRepository,
            coroutineContext = dispatcherRule.testDispatcher,
        )
        val saveRecordUseCase = SaveRecordUseCase(
            recordRepository = recordRepository,
            typeRepository = typeRepository,
            coroutineContext = dispatcherRule.testDispatcher,
        )

        viewModel = EditRecordViewModel(
            typeRepository = typeRepository,
            assetRepository = assetRepository,
            tagRepository = tagRepository,
            recordRepository = recordRepository,
            settingRepository = settingRepository,
            getDefaultRecordUseCase = getDefaultRecordUseCase,
            saveRecordUseCase = saveRecordUseCase,
        )
    }

    // region 初始状态

    @Test
    fun when_initialized_then_uiState_is_loading() {
        assertThat(viewModel.uiState.value).isEqualTo(EditRecordUiState.Loading)
    }

    @Test
    fun when_initialized_then_bottomSheetType_is_none() {
        assertThat(viewModel.bottomSheetType).isEqualTo(EditRecordBottomSheetEnum.NONE)
    }

    @Test
    fun when_initialized_then_shouldDisplayBookmark_is_none() {
        assertThat(viewModel.shouldDisplayBookmark).isEqualTo(EditRecordBookmarkEnum.NONE)
    }

    @Test
    fun when_initialized_then_dialogState_is_dismiss() {
        assertThat(viewModel.dialogState).isEqualTo(DialogState.Dismiss)
    }

    @Test
    fun when_initialized_then_selectedTypeCategoryData_is_expenditure() {
        assertThat(viewModel.selectedTypeCategoryData.value)
            .isEqualTo(RecordTypeCategoryEnum.EXPENDITURE)
    }

    @Test
    fun when_initRecordId_with_new_record_then_uiState_becomes_success() = runTest {
        // 收集 uiState 以激活 WhileSubscribed 上游
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.initRecordId(-1L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(EditRecordUiState.Success::class.java)
        val success = state as EditRecordUiState.Success
        // 新建记录金额为 0
        assertThat(success.amountText).isEqualTo("0")
    }

    // endregion

    // region 底部 Sheet 状态管理

    @Test
    fun when_displayAmountSheet_then_bottomSheetType_is_amount() {
        viewModel.displayAmountSheet()
        assertThat(viewModel.bottomSheetType).isEqualTo(EditRecordBottomSheetEnum.AMOUNT)
    }

    @Test
    fun when_displayChargesSheet_then_bottomSheetType_is_charges() {
        viewModel.displayChargesSheet()
        assertThat(viewModel.bottomSheetType).isEqualTo(EditRecordBottomSheetEnum.CHARGES)
    }

    @Test
    fun when_displayConcessions_then_bottomSheetType_is_concessions() {
        viewModel.displayConcessions()
        assertThat(viewModel.bottomSheetType).isEqualTo(EditRecordBottomSheetEnum.CONCESSIONS)
    }

    @Test
    fun when_displayAssetSheet_then_bottomSheetType_is_assets() {
        viewModel.displayAssetSheet()
        assertThat(viewModel.bottomSheetType).isEqualTo(EditRecordBottomSheetEnum.ASSETS)
    }

    @Test
    fun when_displayRelatedAssetSheet_then_bottomSheetType_is_related_assets() {
        viewModel.displayRelatedAssetSheet()
        assertThat(viewModel.bottomSheetType).isEqualTo(EditRecordBottomSheetEnum.RELATED_ASSETS)
    }

    @Test
    fun when_displayTagSheet_then_bottomSheetType_is_tags() {
        viewModel.displayTagSheet()
        assertThat(viewModel.bottomSheetType).isEqualTo(EditRecordBottomSheetEnum.TAGS)
    }

    @Test
    fun when_displayImageSheet_then_bottomSheetType_is_images() {
        viewModel.displayImageSheet()
        assertThat(viewModel.bottomSheetType).isEqualTo(EditRecordBottomSheetEnum.IMAGES)
    }

    @Test
    fun when_dismissBottomSheet_then_bottomSheetType_is_none() {
        viewModel.displayAmountSheet()
        assertThat(viewModel.bottomSheetType).isEqualTo(EditRecordBottomSheetEnum.AMOUNT)

        viewModel.dismissBottomSheet()
        assertThat(viewModel.bottomSheetType).isEqualTo(EditRecordBottomSheetEnum.NONE)
    }

    // endregion

    // region 金额修改

    @Test
    fun when_updateAmount_then_uiState_reflects_new_amount() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.initRecordId(-1L)
        advanceUntilIdle()

        viewModel.updateAmount("19.99")
        advanceUntilIdle()

        val success = viewModel.uiState.value as EditRecordUiState.Success
        assertThat(success.amountText).isEqualTo("19.99")
    }

    @Test
    fun when_updateAmount_then_bottomSheet_dismissed() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.initRecordId(-1L)
        advanceUntilIdle()

        viewModel.displayAmountSheet()
        assertThat(viewModel.bottomSheetType).isEqualTo(EditRecordBottomSheetEnum.AMOUNT)

        viewModel.updateAmount("10")
        advanceUntilIdle()

        assertThat(viewModel.bottomSheetType).isEqualTo(EditRecordBottomSheetEnum.NONE)
    }

    @Test
    fun when_updateCharge_then_uiState_reflects_charges() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.initRecordId(-1L)
        advanceUntilIdle()

        viewModel.updateCharge("5.50")
        advanceUntilIdle()

        val success = viewModel.uiState.value as EditRecordUiState.Success
        assertThat(success.chargesText).isEqualTo("5.5")
    }

    @Test
    fun when_updateConcessions_then_uiState_reflects_concessions() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.initRecordId(-1L)
        advanceUntilIdle()

        viewModel.updateConcessions("3")
        advanceUntilIdle()

        val success = viewModel.uiState.value as EditRecordUiState.Success
        assertThat(success.concessionsText).isEqualTo("3")
    }

    // endregion

    // region 类型切换

    @Test
    fun when_updateTypeCategory_then_selectedTypeCategoryData_updated() = runTest {
        // 收集 selectedTypeCategoryData 以激活 WhileSubscribed 上游
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.selectedTypeCategoryData.collect {}
        }

        viewModel.initRecordId(-1L)
        advanceUntilIdle()

        viewModel.updateTypeCategory(RecordTypeCategoryEnum.INCOME)
        advanceUntilIdle()

        assertThat(viewModel.selectedTypeCategoryData.value)
            .isEqualTo(RecordTypeCategoryEnum.INCOME)
    }

    @Test
    fun when_updateType_with_valid_id_then_uiState_reflects_type() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.initRecordId(-1L)
        advanceUntilIdle()

        viewModel.updateType(2L)
        advanceUntilIdle()

        val success = viewModel.uiState.value as EditRecordUiState.Success
        assertThat(success.selectedTypeId).isEqualTo(2L)
    }

    @Test
    fun when_updateType_with_invalid_id_then_type_not_changed() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.initRecordId(-1L)
        advanceUntilIdle()

        val typeIdBefore = (viewModel.uiState.value as EditRecordUiState.Success).selectedTypeId

        viewModel.updateType(-1L)
        advanceUntilIdle()

        val typeIdAfter = (viewModel.uiState.value as EditRecordUiState.Success).selectedTypeId
        assertThat(typeIdAfter).isEqualTo(typeIdBefore)
    }

    // endregion

    // region 资产切换

    @Test
    fun when_updateAsset_then_uiState_reflects_asset() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val asset = createAssetModel(id = 10L, name = "现金", balance = 100000L)
        assetRepository.addAsset(asset)

        viewModel.initRecordId(-1L)
        advanceUntilIdle()

        viewModel.updateAsset(10L)
        advanceUntilIdle()

        val success = viewModel.uiState.value as EditRecordUiState.Success
        assertThat(success.selectedAssetId).isEqualTo(10L)
        assertThat(success.assetText).contains("现金")
    }

    @Test
    fun when_updateAsset_then_bottomSheet_dismissed() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.initRecordId(-1L)
        advanceUntilIdle()

        viewModel.displayAssetSheet()
        viewModel.updateAsset(10L)
        advanceUntilIdle()

        assertThat(viewModel.bottomSheetType).isEqualTo(EditRecordBottomSheetEnum.NONE)
    }

    @Test
    fun when_updateRelatedAsset_then_bottomSheet_dismissed() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.initRecordId(-1L)
        advanceUntilIdle()

        viewModel.displayRelatedAssetSheet()
        viewModel.updateRelatedAsset(10L)
        advanceUntilIdle()

        assertThat(viewModel.bottomSheetType).isEqualTo(EditRecordBottomSheetEnum.NONE)
    }

    @Test
    fun when_initAssetId_with_valid_id_then_asset_set() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val asset = createAssetModel(id = 20L, name = "银行卡", balance = 500000L)
        assetRepository.addAsset(asset)

        viewModel.initRecordId(-1L)
        advanceUntilIdle()

        viewModel.initAssetId(20L)
        advanceUntilIdle()

        val success = viewModel.uiState.value as EditRecordUiState.Success
        assertThat(success.selectedAssetId).isEqualTo(20L)
    }

    @Test
    fun when_initAssetId_with_negative_one_then_asset_not_changed() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.initRecordId(-1L)
        advanceUntilIdle()

        val assetIdBefore =
            (viewModel.uiState.value as EditRecordUiState.Success).selectedAssetId

        viewModel.initAssetId(-1L)
        advanceUntilIdle()

        val assetIdAfter =
            (viewModel.uiState.value as EditRecordUiState.Success).selectedAssetId
        assertThat(assetIdAfter).isEqualTo(assetIdBefore)
    }

    // endregion

    // region 备注修改

    @Test
    fun when_updateRemark_then_uiState_reflects_remark() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.initRecordId(-1L)
        advanceUntilIdle()

        viewModel.updateRemark("午餐费用")
        advanceUntilIdle()

        val success = viewModel.uiState.value as EditRecordUiState.Success
        assertThat(success.remarkText).isEqualTo("午餐费用")
    }

    // endregion

    // region 可报销状态

    @Test
    fun when_switchReimbursable_then_reimbursable_toggled() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.initRecordId(-1L)
        advanceUntilIdle()

        // 默认不可报销
        assertThat((viewModel.uiState.value as EditRecordUiState.Success).reimbursable)
            .isFalse()

        // 切换为可报销
        viewModel.switchReimbursable()
        advanceUntilIdle()

        assertThat((viewModel.uiState.value as EditRecordUiState.Success).reimbursable)
            .isTrue()

        // 再次切换回不可报销
        viewModel.switchReimbursable()
        advanceUntilIdle()

        assertThat((viewModel.uiState.value as EditRecordUiState.Success).reimbursable)
            .isFalse()
    }

    // endregion

    // region 标签关联

    @Test
    fun when_updateTag_then_displayTagIdListData_updated() = runTest {
        // 收集标签相关流以激活上游
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.displayTagIdListData.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val tag1 = createTagModel(id = 1L, name = "旅行")
        val tag2 = createTagModel(id = 2L, name = "聚餐")
        tagRepository.addTag(tag1)
        tagRepository.addTag(tag2)

        viewModel.initRecordId(-1L)
        advanceUntilIdle()

        viewModel.updateTag(listOf(1L, 2L))
        advanceUntilIdle()

        assertThat(viewModel.displayTagIdListData.value).containsExactly(1L, 2L)
    }

    @Test
    fun when_updateTag_then_tagTextData_updated() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.tagTextData.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val tag1 = createTagModel(id = 1L, name = "旅行")
        val tag2 = createTagModel(id = 2L, name = "聚餐")
        tagRepository.addTag(tag1)
        tagRepository.addTag(tag2)

        viewModel.initRecordId(-1L)
        advanceUntilIdle()

        viewModel.updateTag(listOf(1L, 2L))
        advanceUntilIdle()

        assertThat(viewModel.tagTextData.value).isEqualTo("旅行,聚餐")
    }

    @Test
    fun when_initialized_then_tagTextData_is_empty() {
        assertThat(viewModel.tagTextData.value).isEmpty()
    }

    // endregion

    // region 关联记录

    @Test
    fun when_updateRelatedRecord_then_currentRelatedRecord_updated() = runTest {
        viewModel.initRecordId(-1L)
        advanceUntilIdle()

        viewModel.updateRelatedRecord(listOf(100L, 200L))
        advanceUntilIdle()

        val relatedIds = viewModel.currentRelatedRecord().first()
        assertThat(relatedIds).containsExactly(100L, 200L)
    }

    // endregion

    // region 弹窗状态

    @Test
    fun when_dismissDialog_then_dialogState_is_dismiss() {
        viewModel.dismissDialog()
        assertThat(viewModel.dialogState).isEqualTo(DialogState.Dismiss)
    }

    @Test
    fun when_dismissBookmark_then_bookmark_is_none() {
        viewModel.dismissBookmark()
        assertThat(viewModel.shouldDisplayBookmark).isEqualTo(EditRecordBookmarkEnum.NONE)
    }

    // endregion

    // region 保存记录

    @Test
    fun when_trySave_with_zero_amount_then_bookmark_amount_must_not_be_zero() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.selectedTypeCategoryData.collect {}
        }

        viewModel.initRecordId(-1L)
        advanceUntilIdle()

        // 金额为 0 不修改
        var successCalled = false
        viewModel.trySave("保存中") { successCalled = true }
        advanceUntilIdle()

        assertThat(viewModel.shouldDisplayBookmark)
            .isEqualTo(EditRecordBookmarkEnum.AMOUNT_MUST_NOT_BE_ZERO)
        assertThat(successCalled).isFalse()
    }

    @Test
    fun when_trySave_with_type_category_mismatch_then_bookmark_type_not_match() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.selectedTypeCategoryData.collect {}
        }

        viewModel.initRecordId(-1L)
        advanceUntilIdle()

        // 设置金额
        viewModel.updateAmount("10")
        advanceUntilIdle()

        // 类型是支出（typeId=1），但切换分类为收入
        viewModel.updateTypeCategory(RecordTypeCategoryEnum.INCOME)
        advanceUntilIdle()

        var successCalled = false
        viewModel.trySave("保存中") { successCalled = true }
        advanceUntilIdle()

        assertThat(viewModel.shouldDisplayBookmark)
            .isEqualTo(EditRecordBookmarkEnum.TYPE_NOT_MATCH_CATEGORY)
        assertThat(successCalled).isFalse()
    }

    @Test
    fun when_trySave_with_valid_data_then_onSuccess_called() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.selectedTypeCategoryData.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.displayTagIdListData.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.displayImageData.collect {}
        }

        viewModel.initRecordId(-1L)
        advanceUntilIdle()

        // 设置金额
        viewModel.updateAmount("50")
        advanceUntilIdle()

        var successCalled = false
        viewModel.trySave("保存中") { successCalled = true }
        advanceUntilIdle()

        assertThat(successCalled).isTrue()
        // 验证记录已保存到 repository
        assertThat(recordRepository.lastUpdatedRecord).isNotNull()
        assertThat(recordRepository.lastUpdatedRecord!!.amount).isEqualTo(5000L)
    }

    @Test
    fun when_trySave_expenditure_then_reimbursable_preserved() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.selectedTypeCategoryData.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.displayTagIdListData.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.displayImageData.collect {}
        }

        viewModel.initRecordId(-1L)
        advanceUntilIdle()

        viewModel.updateAmount("100")
        viewModel.switchReimbursable()
        advanceUntilIdle()

        viewModel.trySave("保存中") {}
        advanceUntilIdle()

        // 支出类型应保留 reimbursable
        assertThat(recordRepository.lastUpdatedRecord).isNotNull()
        assertThat(recordRepository.lastUpdatedRecord!!.reimbursable).isTrue()
    }

    @Test
    fun when_trySave_income_then_reimbursable_forced_false() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.selectedTypeCategoryData.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.displayTagIdListData.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.displayImageData.collect {}
        }

        viewModel.initRecordId(-1L)
        advanceUntilIdle()

        // 先设置为收入类型
        viewModel.updateType(2L)
        viewModel.updateTypeCategory(RecordTypeCategoryEnum.INCOME)
        viewModel.updateAmount("100")
        viewModel.switchReimbursable()
        advanceUntilIdle()

        viewModel.trySave("保存中") {}
        advanceUntilIdle()

        // 收入类型 reimbursable 强制为 false
        assertThat(recordRepository.lastUpdatedRecord).isNotNull()
        assertThat(recordRepository.lastUpdatedRecord!!.reimbursable).isFalse()
    }

    @Test
    fun when_trySave_income_then_concessions_forced_zero() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.selectedTypeCategoryData.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.displayTagIdListData.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.displayImageData.collect {}
        }

        viewModel.initRecordId(-1L)
        advanceUntilIdle()

        viewModel.updateType(2L)
        viewModel.updateTypeCategory(RecordTypeCategoryEnum.INCOME)
        viewModel.updateAmount("100")
        viewModel.updateConcessions("5")
        advanceUntilIdle()

        viewModel.trySave("保存中") {}
        advanceUntilIdle()

        // 收入类型优惠强制为 0
        assertThat(recordRepository.lastUpdatedRecord).isNotNull()
        assertThat(recordRepository.lastUpdatedRecord!!.concessions).isEqualTo(0L)
    }

    @Test
    fun when_trySave_non_transfer_then_relatedAssetId_forced_negative_one() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.selectedTypeCategoryData.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.displayTagIdListData.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.displayImageData.collect {}
        }

        viewModel.initRecordId(-1L)
        advanceUntilIdle()

        viewModel.updateAmount("100")
        viewModel.updateRelatedAsset(10L)
        advanceUntilIdle()

        viewModel.trySave("保存中") {}
        advanceUntilIdle()

        // 非转账类型关联资产强制为 -1
        assertThat(recordRepository.lastUpdatedRecord).isNotNull()
        assertThat(recordRepository.lastUpdatedRecord!!.relatedAssetId).isEqualTo(-1L)
    }

    // endregion

    // region 编辑已有记录

    @Test
    fun given_existing_record_when_initRecordId_then_uiState_reflects_record() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val existingRecord = createRecordModel(
            id = 100L,
            typeId = 1L,
            amount = 9999L,
            remark = "已有记录",
        )
        recordRepository.addRecord(existingRecord)

        viewModel.initRecordId(100L)
        advanceUntilIdle()

        val success = viewModel.uiState.value as EditRecordUiState.Success
        assertThat(success.amountText).isEqualTo("99.99")
        assertThat(success.remarkText).isEqualTo("已有记录")
    }

    @Test
    fun given_existing_record_with_tags_when_init_then_tags_displayed() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.tagTextData.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.displayTagIdListData.collect {}
        }

        val existingRecord = createRecordModel(id = 100L, typeId = 1L, amount = 1000L)
        recordRepository.addRecord(existingRecord)

        val tag = createTagModel(id = 5L, name = "出差")
        tagRepository.addTag(tag)
        tagRepository.setRelatedTags(100L, listOf(tag))

        viewModel.initRecordId(100L)
        advanceUntilIdle()

        assertThat(viewModel.tagTextData.value).isEqualTo("出差")
        assertThat(viewModel.displayTagIdListData.value).containsExactly(5L)
    }

    // endregion

    // region initRecordId 幂等性

    @Test
    fun when_initRecordId_called_twice_then_second_call_ignored() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.initRecordId(-1L)
        advanceUntilIdle()

        val existingRecord = createRecordModel(id = 200L, typeId = 1L, amount = 5000L)
        recordRepository.addRecord(existingRecord)

        // 第二次调用应被忽略
        viewModel.initRecordId(200L)
        advanceUntilIdle()

        val success = viewModel.uiState.value as EditRecordUiState.Success
        // 仍然是新建记录的金额 0，而非 200L 记录的 5000
        assertThat(success.amountText).isEqualTo("0")
    }

    @Test
    fun when_initAssetId_called_twice_then_second_call_ignored() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val asset1 = createAssetModel(id = 10L, name = "现金", balance = 100000L)
        val asset2 = createAssetModel(id = 20L, name = "银行卡", balance = 500000L)
        assetRepository.addAsset(asset1)
        assetRepository.addAsset(asset2)

        viewModel.initRecordId(-1L)
        advanceUntilIdle()

        viewModel.initAssetId(10L)
        advanceUntilIdle()

        // 第二次调用应被忽略
        viewModel.initAssetId(20L)
        advanceUntilIdle()

        val success = viewModel.uiState.value as EditRecordUiState.Success
        assertThat(success.selectedAssetId).isEqualTo(10L)
    }

    // endregion

    // region needRelated

    @Test
    fun given_type_needs_related_when_init_then_needRelated_is_true() = runTest {
        typeRepository.setNeedRelated(1L)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.initRecordId(-1L)
        advanceUntilIdle()

        val success = viewModel.uiState.value as EditRecordUiState.Success
        assertThat(success.needRelated).isTrue()
    }

    @Test
    fun given_type_not_needs_related_when_init_then_needRelated_is_false() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.initRecordId(-1L)
        advanceUntilIdle()

        val success = viewModel.uiState.value as EditRecordUiState.Success
        assertThat(success.needRelated).isFalse()
    }

    // endregion

    // region defaultTypeIdData

    @Test
    fun when_initRecordId_for_new_record_then_defaultTypeIdData_is_default_type() = runTest {
        // 收集 defaultTypeIdData 以激活 WhileSubscribed 上游
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.defaultTypeIdData.collect {}
        }

        viewModel.initRecordId(-1L)
        advanceUntilIdle()

        // 默认类型是 typeRepository 中第一个支出类型的 id
        assertThat(viewModel.defaultTypeIdData.value).isEqualTo(1L)
    }

    // endregion

    // region currentRecord

    @Test
    fun when_updateAmount_then_currentRecord_reflects_change() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.initRecordId(-1L)
        advanceUntilIdle()

        viewModel.updateAmount("25.5")
        advanceUntilIdle()

        val record = viewModel.currentRecord().first()
        assertThat(record.amount).isEqualTo(2550L)
    }

    // endregion
}
