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

import androidx.test.core.app.ApplicationProvider
import cn.wj.android.cashbook.core.data.repository.AssetRepository
import cn.wj.android.cashbook.core.model.enums.AssetClassificationEnum
import cn.wj.android.cashbook.core.model.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.core.model.model.AssetModel
import cn.wj.android.cashbook.core.testing.repository.FakeAssetRepository
import cn.wj.android.cashbook.core.testing.repository.FakeRecordRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.domain.usecase.GetDefaultAssetUseCase
import cn.wj.android.cashbook.domain.usecase.SaveAssetUseCase
import cn.wj.android.cashbook.feature.assets.enums.EditAssetBottomSheetEnum
import cn.wj.android.cashbook.feature.assets.enums.SelectDayEnum
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
class EditAssetViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var assetRepository: FakeAssetRepository
    private lateinit var recordRepository: FakeRecordRepository
    private lateinit var viewModel: EditAssetViewModel

    @Before
    fun setup() {
        assetRepository = FakeAssetRepository()
        recordRepository = FakeRecordRepository()
        val saveAssetUseCase = SaveAssetUseCase(
            recordRepository = recordRepository,
            assetRepository = assetRepository,
            coroutineContext = UnconfinedTestDispatcher(),
        )
        val getDefaultAssetUseCase = GetDefaultAssetUseCase(
            context = ApplicationProvider.getApplicationContext(),
            assetRepository = assetRepository,
        )
        viewModel = EditAssetViewModel(
            assetRepository = assetRepository,
            saveAssetUseCase = saveAssetUseCase,
            getDefaultAssetUseCase = getDefaultAssetUseCase,
        )
    }

    @Test
    fun when_initial_state_then_bottom_sheet_dismiss() {
        // 初始底部 Sheet 应为 DISMISS
        assertThat(viewModel.bottomSheetData).isEqualTo(EditAssetBottomSheetEnum.DISMISS)
    }

    @Test
    fun when_initial_state_then_dialog_dismiss() {
        // 初始弹窗状态应为 Dismiss
        assertThat(viewModel.dialogState).isEqualTo(DialogState.Dismiss)
    }

    @Test
    fun when_show_select_classification_sheet_then_classification_type() {
        viewModel.showSelectClassificationSheet()

        assertThat(viewModel.bottomSheetData).isEqualTo(EditAssetBottomSheetEnum.CLASSIFICATION_TYPE)
    }

    @Test
    fun when_dismiss_bottom_sheet_then_dismiss() {
        viewModel.showSelectClassificationSheet()
        assertThat(viewModel.bottomSheetData).isEqualTo(EditAssetBottomSheetEnum.CLASSIFICATION_TYPE)

        viewModel.dismissBottomSheet()

        assertThat(viewModel.bottomSheetData).isEqualTo(EditAssetBottomSheetEnum.DISMISS)
    }

    @Test
    fun when_show_select_billing_date_dialog_then_dialog_shown() {
        viewModel.showSelectBillingDateDialog()

        val state = viewModel.dialogState
        assertThat(state).isInstanceOf(DialogState.Shown::class.java)
        assertThat((state as DialogState.Shown<*>).data).isEqualTo(SelectDayEnum.BILLING_DATE)
    }

    @Test
    fun when_show_select_repayment_date_dialog_then_dialog_shown() {
        viewModel.showSelectRepaymentDateDialog()

        val state = viewModel.dialogState
        assertThat(state).isInstanceOf(DialogState.Shown::class.java)
        assertThat((state as DialogState.Shown<*>).data).isEqualTo(SelectDayEnum.REPAYMENT_DATE)
    }

    @Test
    fun when_dismiss_dialog_then_dialog_dismiss() {
        viewModel.showSelectBillingDateDialog()
        assertThat(viewModel.dialogState).isInstanceOf(DialogState.Shown::class.java)

        viewModel.dismissDialog()

        assertThat(viewModel.dialogState).isEqualTo(DialogState.Dismiss)
    }

    @Test
    fun when_update_classification_with_bank_card_then_show_asset_classification_sheet() {
        // 银行卡类型需要继续选择银行
        viewModel.updateClassification(
            type = ClassificationTypeEnum.CAPITAL_ACCOUNT,
            classification = AssetClassificationEnum.BANK_CARD,
            classificationName = "银行卡",
        )

        assertThat(viewModel.bottomSheetData).isEqualTo(EditAssetBottomSheetEnum.ASSET_CLASSIFICATION)
    }

    @Test
    fun when_update_classification_with_non_bank_card_then_dismiss_sheet() {
        // 非银行卡类型直接保存并关闭
        viewModel.updateClassification(
            type = ClassificationTypeEnum.CAPITAL_ACCOUNT,
            classification = AssetClassificationEnum.CASH,
            classificationName = "现金",
        )

        assertThat(viewModel.bottomSheetData).isEqualTo(EditAssetBottomSheetEnum.DISMISS)
    }

    @Test
    fun when_new_asset_then_ui_state_loading_initially() {
        // 新建资产时初始状态为 Loading
        assertThat(viewModel.uiState.value).isInstanceOf(EditAssetUiState.Loading::class.java)
    }

    @Test
    fun when_update_asset_id_for_new_then_ui_state_success() = runTest {
        // 订阅 uiState 以激活 WhileSubscribed
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        // 新建资产（id=-1）触发默认数据加载
        viewModel.updateAssetId(-1L)

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(EditAssetUiState.Success::class.java)
        val success = state as EditAssetUiState.Success
        // 新资产 typeEnable 应为 true
        assertThat(success.typeEnable).isTrue()
    }

    @Test
    fun when_update_asset_id_for_existing_then_type_enable_false() = runTest {
        // 订阅 uiState 以激活 WhileSubscribed
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        // 编辑已有资产
        val asset = createAssetModel(id = 1L, name = "已有资产")
        assetRepository.addAsset(asset)

        viewModel.updateAssetId(1L)

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(EditAssetUiState.Success::class.java)
        val success = state as EditAssetUiState.Success
        // 编辑已有资产时 typeEnable 应为 false
        assertThat(success.typeEnable).isFalse()
        assertThat(success.assetName).isEqualTo("已有资产")
    }

    @Test
    fun when_update_day_for_billing_date_then_ui_state_updated() = runTest {
        // 订阅 uiState 以激活 WhileSubscribed
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        // 触发默认数据
        viewModel.updateAssetId(-1L)

        // 先显示选择账单日弹窗
        viewModel.showSelectBillingDateDialog()

        // 设置账单日
        viewModel.updateDay("15")

        // 弹窗应已关闭
        assertThat(viewModel.dialogState).isEqualTo(DialogState.Dismiss)

        // UI 状态中的账单日应更新
        val state = viewModel.uiState.value as EditAssetUiState.Success
        assertThat(state.billingDate).isEqualTo("15")
    }

    @Test
    fun when_update_day_for_repayment_date_then_ui_state_updated() = runTest {
        // 订阅 uiState 以激活 WhileSubscribed
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        // 触发默认数据
        viewModel.updateAssetId(-1L)

        // 先显示选择还款日弹窗
        viewModel.showSelectRepaymentDateDialog()

        // 设置还款日
        viewModel.updateDay("5")

        // 弹窗应已关闭
        assertThat(viewModel.dialogState).isEqualTo(DialogState.Dismiss)

        // UI 状态中的还款日应更新
        val state = viewModel.uiState.value as EditAssetUiState.Success
        assertThat(state.repaymentDate).isEqualTo("5")
    }

    @Test
    fun when_update_invisible_then_ui_state_updated() = runTest {
        // 订阅 uiState 以激活 WhileSubscribed
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        // 触发默认数据
        viewModel.updateAssetId(-1L)

        // 设置隐藏
        viewModel.updateInvisible(true)

        val state = viewModel.uiState.value as EditAssetUiState.Success
        assertThat(state.invisible).isTrue()
    }

    @Test
    fun when_save_new_asset_then_persisted_and_on_success() = runTest {
        var successCount = 0
        // 新建资产（id 默认 -1）保存：应落库并回调成功
        viewModel.save(
            assetName = "工资卡",
            totalAmount = "0",
            balance = "1000",
            openBank = "招商银行",
            cardNo = "6225",
            remark = "备注",
            onSuccess = { successCount++ },
        )

        val saved = assetRepository.lastUpdatedAsset
        assertThat(saved).isNotNull()
        assertThat(saved!!.name).isEqualTo("工资卡")
        assertThat(saved.openBank).isEqualTo("招商银行")
        assertThat(saved.cardNo).isEqualTo("6225")
        assertThat(saved.remark).isEqualTo("备注")
        // 1000 元 → 100000 分
        assertThat(saved.balance).isEqualTo(100000L)
        assertThat(successCount).isEqualTo(1)
    }

    @Test
    fun when_save_then_amount_strings_converted_to_cent() = runTest {
        // 元字符串入参经 toAmountCent 转分（HALF_UP）落库
        viewModel.save(
            assetName = "现金",
            totalAmount = "200.50",
            balance = "99.99",
            openBank = "",
            cardNo = "",
            remark = "",
            onSuccess = {},
        )

        val saved = assetRepository.lastUpdatedAsset
        assertThat(saved).isNotNull()
        assertThat(saved!!.totalAmount).isEqualTo(20050L)
        assertThat(saved.balance).isEqualTo(9999L)
    }

    @Test
    fun when_save_existing_asset_without_change_then_skip_persist() = runTest {
        // 已有资产且全字段未变：跳过落库但仍回调成功
        val asset = createAssetModel(id = 1L, name = "已有资产")
        assetRepository.addAsset(asset)
        viewModel.updateAssetId(1L)

        var successCount = 0
        viewModel.save(
            assetName = "已有资产",
            totalAmount = "0",
            balance = "0",
            openBank = "",
            cardNo = "",
            remark = "",
            onSuccess = { successCount++ },
        )

        // addAsset 不写 lastUpdatedAsset，未变化时 updateAsset 不应被调用
        assertThat(assetRepository.lastUpdatedAsset).isNull()
        assertThat(successCount).isEqualTo(1)
    }

    @Test
    fun when_save_existing_asset_with_name_change_then_persisted() = runTest {
        // 已有资产改名：落库新名
        val asset = createAssetModel(id = 1L, name = "旧名")
        assetRepository.addAsset(asset)
        viewModel.updateAssetId(1L)

        var successCount = 0
        viewModel.save(
            assetName = "新名",
            totalAmount = "0",
            balance = "0",
            openBank = "",
            cardNo = "",
            remark = "",
            onSuccess = { successCount++ },
        )

        val saved = assetRepository.lastUpdatedAsset
        assertThat(saved).isNotNull()
        assertThat(saved!!.name).isEqualTo("新名")
        assertThat(successCount).isEqualTo(1)
    }

    @Test
    fun when_save_in_progress_then_reentrant_call_ignored() = runTest {
        var successCount = 0
        // 成功路径不复位 doSaving，二次调用应被重入守卫拦截
        viewModel.save(
            assetName = "首次",
            totalAmount = "0",
            balance = "0",
            openBank = "",
            cardNo = "",
            remark = "",
            onSuccess = { successCount++ },
        )
        viewModel.save(
            assetName = "二次",
            totalAmount = "0",
            balance = "0",
            openBank = "",
            cardNo = "",
            remark = "",
            onSuccess = { successCount++ },
        )

        assertThat(successCount).isEqualTo(1)
        assertThat(assetRepository.lastUpdatedAsset!!.name).isEqualTo("首次")
    }

    @Test
    fun when_save_fails_then_doSaving_reset_allows_retry() = runTest {
        // 落库抛异常：catch 复位 doSaving，不回调成功；复位后可重试成功
        val backing = FakeAssetRepository()
        val throwingRepo = ThrowingAssetRepository(backing, shouldThrow = true)
        val failingViewModel = EditAssetViewModel(
            assetRepository = throwingRepo,
            saveAssetUseCase = SaveAssetUseCase(
                recordRepository = recordRepository,
                assetRepository = throwingRepo,
                coroutineContext = UnconfinedTestDispatcher(),
            ),
            getDefaultAssetUseCase = GetDefaultAssetUseCase(
                context = ApplicationProvider.getApplicationContext(),
                assetRepository = throwingRepo,
            ),
        )

        var successCount = 0
        failingViewModel.save(
            assetName = "失败",
            totalAmount = "0",
            balance = "0",
            openBank = "",
            cardNo = "",
            remark = "",
            onSuccess = { successCount++ },
        )
        // 失败：不落库、不回调成功
        assertThat(successCount).isEqualTo(0)
        assertThat(backing.lastUpdatedAsset).isNull()

        // doSaving 已复位 → 重试落库成功
        throwingRepo.shouldThrow = false
        failingViewModel.save(
            assetName = "重试",
            totalAmount = "0",
            balance = "0",
            openBank = "",
            cardNo = "",
            remark = "",
            onSuccess = { successCount++ },
        )
        assertThat(successCount).isEqualTo(1)
        assertThat(backing.lastUpdatedAsset).isNotNull()
        assertThat(backing.lastUpdatedAsset!!.name).isEqualTo("重试")
    }

    /**
     * 可注入 updateAsset 异常的 AssetRepository 装饰器，用于覆盖 save() 的 catch/重试路径。
     * core/testing 的 FakeAssetRepository 永不抛异常，无法触达失败分支。
     */
    private class ThrowingAssetRepository(
        private val delegate: FakeAssetRepository,
        var shouldThrow: Boolean = true,
    ) : AssetRepository by delegate {
        override suspend fun updateAsset(asset: AssetModel) {
            if (shouldThrow) {
                throw RuntimeException("save failed")
            }
            delegate.updateAsset(asset)
        }
    }

    /**
     * 创建测试用的 AssetModel
     */
    private fun createAssetModel(
        id: Long = 1L,
        name: String = "测试资产",
    ): AssetModel = AssetModel(
        id = id,
        booksId = 1L,
        name = name,
        iconResId = 0,
        totalAmount = 0L,
        billingDate = "",
        repaymentDate = "",
        type = ClassificationTypeEnum.CAPITAL_ACCOUNT,
        classification = AssetClassificationEnum.CASH,
        invisible = false,
        openBank = "",
        cardNo = "",
        remark = "",
        sort = 0,
        modifyTime = 1704067200000L,
        balance = 0L,
    )
}
