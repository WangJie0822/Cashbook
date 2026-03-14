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
import cn.wj.android.cashbook.core.testing.repository.FakeRecordRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTypeRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import cn.wj.android.cashbook.domain.usecase.GetAssetListUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class EditRecordSelectAssetBottomSheetViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var assetRepository: FakeAssetRepository
    private lateinit var recordRepository: FakeRecordRepository
    private lateinit var typeRepository: FakeTypeRepository
    private lateinit var getAssetListUseCase: GetAssetListUseCase
    private lateinit var viewModel: EditRecordSelectAssetBottomSheetViewModel

    @Before
    fun setup() {
        assetRepository = FakeAssetRepository()
        recordRepository = FakeRecordRepository()
        typeRepository = FakeTypeRepository()
        getAssetListUseCase = GetAssetListUseCase(
            assetRepository = assetRepository,
            recordRepository = recordRepository,
            typeRepository = typeRepository,
        )
        viewModel = EditRecordSelectAssetBottomSheetViewModel(getAssetListUseCase)
    }

    @Test
    fun when_initial_state_then_empty_list() {
        // 初始状态资产列表为空
        assertThat(viewModel.assetListData.value).isEmpty()
    }

    @Test
    fun when_assets_available_then_list_not_empty() = runTest {
        // 订阅 assetListData 以激活 WhileSubscribed
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.assetListData.collect {}
        }

        // 添加可见资产
        val asset = createAssetModel(id = 1L)
        assetRepository.setVisibleAssets(listOf(asset))

        // 调用 update 触发加载
        viewModel.update(
            currentTypeId = 1L,
            selectedAssetId = -1L,
            isRelated = false,
        )

        assertThat(viewModel.assetListData.value).hasSize(1)
    }

    @Test
    fun when_selected_asset_then_excluded_from_list() = runTest {
        // 订阅 assetListData 以激活 WhileSubscribed
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.assetListData.collect {}
        }

        // 添加两个资产
        val asset1 = createAssetModel(id = 1L)
        val asset2 = createAssetModel(id = 2L)
        assetRepository.setVisibleAssets(listOf(asset1, asset2))

        // 选中 asset1，应该被排除
        viewModel.update(
            currentTypeId = 1L,
            selectedAssetId = 1L,
            isRelated = false,
        )

        val result = viewModel.assetListData.value
        assertThat(result).hasSize(1)
        assertThat(result[0].id).isEqualTo(2L)
    }

    @Test
    fun given_credit_payment_type_when_is_related_then_only_credit_cards() = runTest {
        // 订阅 assetListData 以激活 WhileSubscribed
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.assetListData.collect {}
        }

        // 设置信用卡还款类型
        typeRepository.setCreditPayment(100L)

        // 添加一个普通资产和一个信用卡
        val capitalAsset = createAssetModel(
            id = 1L,
            type = ClassificationTypeEnum.CAPITAL_ACCOUNT,
            classification = AssetClassificationEnum.CASH,
        )
        val creditCard = createAssetModel(
            id = 2L,
            type = ClassificationTypeEnum.CREDIT_CARD_ACCOUNT,
            classification = AssetClassificationEnum.CREDIT_CARD,
        )
        assetRepository.setVisibleAssets(listOf(capitalAsset, creditCard))

        // 关联资产且当前类型为信用卡还款
        viewModel.update(
            currentTypeId = 100L,
            selectedAssetId = -1L,
            isRelated = true,
        )

        // 仅展示信用卡
        val result = viewModel.assetListData.value
        assertThat(result).hasSize(1)
        assertThat(result[0].type).isEqualTo(ClassificationTypeEnum.CREDIT_CARD_ACCOUNT)
    }

    @Test
    fun when_update_params_then_list_refreshed() = runTest {
        // 订阅 assetListData 以激活 WhileSubscribed
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.assetListData.collect {}
        }

        // 添加资产
        val asset1 = createAssetModel(id = 1L)
        val asset2 = createAssetModel(id = 2L)
        assetRepository.setVisibleAssets(listOf(asset1, asset2))

        // 第一次更新，排除 asset1
        viewModel.update(
            currentTypeId = 1L,
            selectedAssetId = 1L,
            isRelated = false,
        )
        assertThat(viewModel.assetListData.value).hasSize(1)

        // 第二次更新，不排除任何资产
        viewModel.update(
            currentTypeId = 1L,
            selectedAssetId = -1L,
            isRelated = false,
        )
        assertThat(viewModel.assetListData.value).hasSize(2)
    }

    /**
     * 创建测试用的 AssetModel
     */
    private fun createAssetModel(
        id: Long = 1L,
        type: ClassificationTypeEnum = ClassificationTypeEnum.CAPITAL_ACCOUNT,
        classification: AssetClassificationEnum = AssetClassificationEnum.CASH,
    ): AssetModel = AssetModel(
        id = id,
        booksId = 1L,
        name = "测试资产$id",
        iconResId = 0,
        totalAmount = 0L,
        billingDate = "",
        repaymentDate = "",
        type = type,
        classification = classification,
        invisible = false,
        openBank = "",
        cardNo = "",
        remark = "",
        sort = 0,
        modifyTime = 1704067200000L,
        balance = 0L,
    )
}
