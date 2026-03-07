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

import cn.wj.android.cashbook.core.model.enums.AssetClassificationEnum
import cn.wj.android.cashbook.core.model.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.core.model.model.AssetModel
import cn.wj.android.cashbook.core.testing.repository.FakeAssetRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MyAssetViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var assetRepository: FakeAssetRepository
    private lateinit var viewModel: MyAssetViewModel

    @Before
    fun setup() {
        assetRepository = FakeAssetRepository()
        viewModel = MyAssetViewModel(assetRepository)
    }

    @Test
    fun when_initial_state_then_loading() {
        // 初始状态应为 Loading
        assertThat(viewModel.uiState.value).isEqualTo(MyAssetUiState.Loading)
    }

    @Test
    fun when_display_more_dialog_then_show_more_true() {
        // 初始状态 showMoreDialog 为 false
        assertThat(viewModel.showMoreDialog).isFalse()

        // 显示更多弹窗
        viewModel.displayShowMoreDialog()

        assertThat(viewModel.showMoreDialog).isTrue()
    }

    @Test
    fun when_dismiss_more_dialog_then_show_more_false() {
        // 先显示弹窗
        viewModel.displayShowMoreDialog()
        assertThat(viewModel.showMoreDialog).isTrue()

        // 隐藏弹窗
        viewModel.dismissShowMoreDialog()

        assertThat(viewModel.showMoreDialog).isFalse()
    }

    @Test
    fun when_assets_emitted_then_ui_state_success() = runTest {
        // 订阅 uiState 以激活 WhileSubscribed
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        // 设置可见资产列表
        val asset = createAssetModel(
            id = 1L,
            balance = "1000.00",
            type = ClassificationTypeEnum.CAPITAL_ACCOUNT,
            classification = AssetClassificationEnum.CASH,
        )
        assetRepository.setVisibleAssets(listOf(asset))

        // 验证 uiState 变为 Success
        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(MyAssetUiState.Success::class.java)
        val success = state as MyAssetUiState.Success
        assertThat(success.totalAsset).isEqualTo("1000")
        assertThat(success.totalLiabilities).isEqualTo("0")
        assertThat(success.netAsset).isEqualTo("1000")
    }

    @Test
    fun when_credit_card_asset_then_counted_as_liabilities() = runTest {
        // 订阅 uiState 以激活 WhileSubscribed
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        // 信用卡应被计入负债
        val creditCard = createAssetModel(
            id = 1L,
            balance = "500.00",
            type = ClassificationTypeEnum.CREDIT_CARD_ACCOUNT,
            classification = AssetClassificationEnum.CREDIT_CARD,
        )
        assetRepository.setVisibleAssets(listOf(creditCard))

        val state = viewModel.uiState.value as MyAssetUiState.Success
        assertThat(state.totalLiabilities).isEqualTo("500")
        assertThat(state.totalAsset).isEqualTo("0")
        assertThat(state.netAsset).isEqualTo("-500")
    }

    @Test
    fun when_borrow_asset_then_counted_as_liabilities() = runTest {
        // 订阅 uiState 以激活 WhileSubscribed
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        // 借入资产应被计入负债
        val borrow = createAssetModel(
            id = 1L,
            balance = "2000.00",
            type = ClassificationTypeEnum.DEBT_ACCOUNT,
            classification = AssetClassificationEnum.BORROW,
        )
        assetRepository.setVisibleAssets(listOf(borrow))

        val state = viewModel.uiState.value as MyAssetUiState.Success
        assertThat(state.totalLiabilities).isEqualTo("2000")
        assertThat(state.totalAsset).isEqualTo("0")
    }

    @Test
    fun when_top_up_not_in_total_then_excluded_from_asset() = runTest {
        // 订阅 uiState 以激活 WhileSubscribed
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        // 充值账户在 topUpInTotal=false 时不计入资产总额
        val topUp = createAssetModel(
            id = 1L,
            balance = "300.00",
            type = ClassificationTypeEnum.TOP_UP_ACCOUNT,
            classification = AssetClassificationEnum.PHONE_CHARGE,
        )
        assetRepository.setVisibleAssets(listOf(topUp))

        val state = viewModel.uiState.value as MyAssetUiState.Success
        // topUpInTotal 默认 false，充值账户不计入
        assertThat(state.topUpInTotal).isFalse()
        assertThat(state.totalAsset).isEqualTo("0")
    }

    @Test
    fun when_update_top_up_in_total_then_included_in_asset() = runTest {
        // 订阅 uiState 以激活 WhileSubscribed
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        // 充值账户在 topUpInTotal=true 时计入资产总额
        val topUp = createAssetModel(
            id = 1L,
            balance = "300.00",
            type = ClassificationTypeEnum.TOP_UP_ACCOUNT,
            classification = AssetClassificationEnum.PHONE_CHARGE,
        )
        assetRepository.setVisibleAssets(listOf(topUp))

        // 更新 topUpInTotal
        viewModel.updateTopUpInTotal(true)

        val state = viewModel.uiState.value as MyAssetUiState.Success
        assertThat(state.topUpInTotal).isTrue()
        assertThat(state.totalAsset).isEqualTo("300")
    }

    @Test
    fun when_initial_then_asset_typed_list_empty() {
        // 初始资产分类列表为空
        assertThat(viewModel.assetTypedListData.value).isEmpty()
    }

    /**
     * 创建测试用的 AssetModel
     */
    private fun createAssetModel(
        id: Long = 1L,
        balance: String = "0",
        type: ClassificationTypeEnum = ClassificationTypeEnum.CAPITAL_ACCOUNT,
        classification: AssetClassificationEnum = AssetClassificationEnum.CASH,
    ): AssetModel = AssetModel(
        id = id,
        booksId = 1L,
        name = "测试资产",
        iconResId = 0,
        totalAmount = "",
        billingDate = "",
        repaymentDate = "",
        type = type,
        classification = classification,
        invisible = false,
        openBank = "",
        cardNo = "",
        remark = "",
        sort = 0,
        modifyTime = "2024-01-01",
        balance = balance,
    )
}
