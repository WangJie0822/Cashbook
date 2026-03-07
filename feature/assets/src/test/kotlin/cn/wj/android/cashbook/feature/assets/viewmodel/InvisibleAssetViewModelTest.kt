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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class InvisibleAssetViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var assetRepository: FakeAssetRepository
    private lateinit var viewModel: InvisibleAssetViewModel

    @Before
    fun setup() {
        assetRepository = FakeAssetRepository()
        viewModel = InvisibleAssetViewModel(assetRepository)
    }

    @Test
    fun when_initial_state_then_empty_list() {
        // 初始状态不可见资产列表为空
        assertThat(viewModel.assetTypedListData.value).isEmpty()
    }

    @Test
    fun when_visible_asset_then_removed_from_invisible_list() = runTest {
        // 添加一个隐藏资产
        val asset = createAssetModel(id = 1L, invisible = true)
        assetRepository.addAsset(asset)

        // 验证隐藏列表中有该资产
        val invisibleBefore = assetRepository.currentInvisibleAssetListData.first()
        assertThat(invisibleBefore).hasSize(1)

        // 调用 visibleAsset，使资产变为可见
        viewModel.visibleAsset(1L)

        // 验证隐藏列表中已无该资产
        val invisibleAfter = assetRepository.currentInvisibleAssetListData.first()
        assertThat(invisibleAfter).isEmpty()

        // 验证可见列表中有该资产
        val visibleAfter = assetRepository.currentVisibleAssetListData.first()
        assertThat(visibleAfter).hasSize(1)
        assertThat(visibleAfter[0].id).isEqualTo(1L)
    }

    @Test
    fun when_visible_asset_with_invalid_id_then_no_change() = runTest {
        // 添加一个隐藏资产
        val asset = createAssetModel(id = 1L, invisible = true)
        assetRepository.addAsset(asset)

        // 使用不存在的 id 调用 visibleAsset
        viewModel.visibleAsset(999L)

        // 隐藏列表不受影响
        val invisibleAfter = assetRepository.currentInvisibleAssetListData.first()
        assertThat(invisibleAfter).hasSize(1)
    }

    /**
     * 创建测试用的 AssetModel
     */
    private fun createAssetModel(
        id: Long = 1L,
        invisible: Boolean = false,
    ): AssetModel = AssetModel(
        id = id,
        booksId = 1L,
        name = "测试资产",
        iconResId = 0,
        totalAmount = "",
        billingDate = "",
        repaymentDate = "",
        type = ClassificationTypeEnum.CAPITAL_ACCOUNT,
        classification = AssetClassificationEnum.CASH,
        invisible = invisible,
        openBank = "",
        cardNo = "",
        remark = "",
        sort = 0,
        modifyTime = "2024-01-01",
        balance = "0",
    )
}
