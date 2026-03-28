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

package cn.wj.android.cashbook.feature.assets.viewmodel

import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.enums.AssetClassificationEnum
import cn.wj.android.cashbook.core.model.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.AssetModel
import cn.wj.android.cashbook.core.testing.repository.FakeAssetRepository
import cn.wj.android.cashbook.core.testing.repository.FakeRecordRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTagRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.core.ui.ProgressDialogController
import cn.wj.android.cashbook.core.ui.ProgressDialogState
import cn.wj.android.cashbook.feature.assets.enums.AssetInfoBookmarkEnum
import cn.wj.android.cashbook.feature.assets.enums.AssetInfoDialogEnum
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AssetInfoViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var assetRepository: FakeAssetRepository
    private lateinit var recordRepository: FakeRecordRepository
    private lateinit var tagRepository: FakeTagRepository
    private lateinit var viewModel: AssetInfoViewModel
    private lateinit var fakeController: FakeProgressDialogController

    @Before
    fun setup() {
        assetRepository = FakeAssetRepository()
        recordRepository = FakeRecordRepository()
        tagRepository = FakeTagRepository()
        viewModel = AssetInfoViewModel(
            assetRepository = assetRepository,
            recordRepository = recordRepository,
            tagRepository = tagRepository,
        )
        fakeController = FakeProgressDialogController()
    }

    private class FakeProgressDialogController : ProgressDialogController {
        override var dialogState: DialogState = DialogState.Dismiss
        override fun show(hint: String?, cancelable: Boolean, onDismiss: () -> Unit) {
            dialogState = DialogState.Shown(ProgressDialogState(hint, cancelable, onDismiss))
        }
        override fun dismiss() {
            dialogState = DialogState.Dismiss
        }
    }

    @Test
    fun when_initial_state_then_loading() {
        // 初始 UI 状态应为 Loading
        assertThat(viewModel.uiState.value).isEqualTo(AssetInfoUiState.Loading)
    }

    @Test
    fun when_update_asset_id_then_ui_state_success() = runTest {
        // 订阅 uiState 以激活 WhileSubscribed
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        // 添加一个资产
        val asset = createAssetModel(
            id = 1L,
            name = "测试现金",
            balance = 100000L,
            openBank = "中国银行",
            cardNo = "1234",
            remark = "测试备注",
        )
        assetRepository.addAsset(asset)

        // 更新 assetId 触发加载
        viewModel.updateAssetId(1L)

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(AssetInfoUiState.Success::class.java)
        val success = state as AssetInfoUiState.Success
        assertThat(success.title).isEqualTo("测试现金")
        assertThat(success.balance).isEqualTo("1000.00")
        assertThat(success.openBank).isEqualTo("中国银行")
        assertThat(success.cardNo).isEqualTo("1234")
        assertThat(success.remark).isEqualTo("测试备注")
    }

    @Test
    fun when_update_asset_id_with_credit_card_then_is_credit_card_true() = runTest {
        // 订阅 uiState 以激活 WhileSubscribed
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        // 信用卡资产
        val creditCard = createAssetModel(
            id = 2L,
            type = ClassificationTypeEnum.CREDIT_CARD_ACCOUNT,
            classification = AssetClassificationEnum.CREDIT_CARD,
            totalAmount = 5000000L,
            billingDate = "15",
            repaymentDate = "5",
        )
        assetRepository.addAsset(creditCard)

        viewModel.updateAssetId(2L)

        val state = viewModel.uiState.value as AssetInfoUiState.Success
        assertThat(state.isCreditCard).isTrue()
        assertThat(state.totalAmount).isEqualTo("50000.00")
        assertThat(state.billingDate).isEqualTo("15")
        assertThat(state.repaymentDate).isEqualTo("5")
    }

    @Test
    fun when_display_more_dialog_then_dialog_state_shown() {
        viewModel.displayMoreDialog()

        val state = viewModel.dialogState
        assertThat(state).isInstanceOf(DialogState.Shown::class.java)
        assertThat((state as DialogState.Shown<*>).data).isEqualTo(AssetInfoDialogEnum.MORE_INFO)
    }

    @Test
    fun when_dismiss_dialog_then_dialog_state_dismiss() {
        // 先显示弹窗
        viewModel.displayMoreDialog()
        assertThat(viewModel.dialogState).isInstanceOf(DialogState.Shown::class.java)

        // 隐藏弹窗
        viewModel.dismissDialog()

        assertThat(viewModel.dialogState).isEqualTo(DialogState.Dismiss)
    }

    @Test
    fun when_show_delete_confirm_dialog_then_dialog_state_shown() {
        viewModel.showDeleteConfirmDialog()

        val state = viewModel.dialogState
        assertThat(state).isInstanceOf(DialogState.Shown::class.java)
        assertThat((state as DialogState.Shown<*>).data).isEqualTo(AssetInfoDialogEnum.DELETE_ASSET)
    }

    @Test
    fun when_display_bookmark_then_bookmark_copied() {
        // 初始为 DISMISS
        assertThat(viewModel.bookmark).isEqualTo(AssetInfoBookmarkEnum.DISMISS)

        viewModel.displayBookmark()

        assertThat(viewModel.bookmark).isEqualTo(AssetInfoBookmarkEnum.COPIED_TO_CLIPBOARD)
    }

    @Test
    fun when_dismiss_bookmark_then_bookmark_dismiss() {
        viewModel.displayBookmark()
        assertThat(viewModel.bookmark).isEqualTo(AssetInfoBookmarkEnum.COPIED_TO_CLIPBOARD)

        viewModel.dismissBookmark()

        assertThat(viewModel.bookmark).isEqualTo(AssetInfoBookmarkEnum.DISMISS)
    }

    @Test
    fun when_record_item_click_then_view_record_data_set() {
        val record = createRecordViewsEntity(id = 1L)

        viewModel.onRecordItemClick(record)

        assertThat(viewModel.viewRecordData).isEqualTo(record)
    }

    @Test
    fun when_dismiss_record_detail_sheet_then_view_record_data_null() {
        val record = createRecordViewsEntity(id = 1L)
        viewModel.onRecordItemClick(record)
        assertThat(viewModel.viewRecordData).isNotNull()

        viewModel.dismissRecordDetailSheet()

        assertThat(viewModel.viewRecordData).isNull()
    }

    @Test
    fun when_delete_asset_then_data_removed() {
        // 准备资产
        val asset = createAssetModel(id = 1L)
        assetRepository.addAsset(asset)
        viewModel.updateAssetId(1L)
        viewModel.setProgressDialogHintText("删除中...")

        var successCalled = false
        viewModel.deleteAsset(fakeController) { successCalled = true }

        assertThat(successCalled).isTrue()
    }

    @Test
    fun when_asset_not_found_then_ui_state_success_with_empty() = runTest {
        // 订阅 uiState 以激活 WhileSubscribed
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        // 更新一个不存在的资产 id
        viewModel.updateAssetId(999L)

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(AssetInfoUiState.Success::class.java)
        // assetInfo 为 null 时使用 orEmpty() 等默认值
        val success = state as AssetInfoUiState.Success
        assertThat(success.title).isEmpty()
        assertThat(success.balance).isEqualTo("0.00")
    }

    /**
     * 创建测试用的 AssetModel
     */
    private fun createAssetModel(
        id: Long = 1L,
        name: String = "测试资产",
        balance: Long = 0L,
        type: ClassificationTypeEnum = ClassificationTypeEnum.CAPITAL_ACCOUNT,
        classification: AssetClassificationEnum = AssetClassificationEnum.CASH,
        totalAmount: Long = 0L,
        billingDate: String = "",
        repaymentDate: String = "",
        openBank: String = "",
        cardNo: String = "",
        remark: String = "",
    ): AssetModel = AssetModel(
        id = id,
        booksId = 1L,
        name = name,
        iconResId = 0,
        totalAmount = totalAmount,
        billingDate = billingDate,
        repaymentDate = repaymentDate,
        type = type,
        classification = classification,
        invisible = false,
        openBank = openBank,
        cardNo = cardNo,
        remark = remark,
        sort = 0,
        modifyTime = 1704067200000L,
        balance = balance,
    )

    /**
     * 创建测试用的 RecordViewsEntity
     */
    private fun createRecordViewsEntity(id: Long = 1L): RecordViewsEntity = RecordViewsEntity(
        id = id,
        typeId = 1L,
        typeCategory = RecordTypeCategoryEnum.EXPENDITURE,
        typeName = "餐饮",
        typeIconResName = "ic_type_food",
        assetId = 1L,
        assetName = "现金",
        assetIconResId = 0,
        relatedAssetId = null,
        relatedAssetName = null,
        relatedAssetIconResId = null,
        amount = 10000L,
        finalAmount = 10000L,
        charges = 0L,
        concessions = 0L,
        remark = "",
        reimbursable = false,
        relatedTags = emptyList(),
        relatedImage = emptyList(),
        relatedRecord = emptyList(),
        relatedAmount = 0L,
        recordTime = 1704110400000L,
    )
}
