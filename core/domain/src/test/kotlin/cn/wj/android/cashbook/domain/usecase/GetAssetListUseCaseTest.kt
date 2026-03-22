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

import cn.wj.android.cashbook.core.common.FIXED_TYPE_ID_CREDIT_CARD_PAYMENT
import cn.wj.android.cashbook.core.model.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.core.testing.data.createAssetModel
import cn.wj.android.cashbook.core.testing.data.createRecordModel
import cn.wj.android.cashbook.core.testing.repository.FakeAssetRepository
import cn.wj.android.cashbook.core.testing.repository.FakeRecordRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTypeRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class GetAssetListUseCaseTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var assetRepository: FakeAssetRepository
    private lateinit var recordRepository: FakeRecordRepository
    private lateinit var typeRepository: FakeTypeRepository
    private lateinit var useCase: GetAssetListUseCase

    @Before
    fun setup() {
        assetRepository = FakeAssetRepository()
        recordRepository = FakeRecordRepository()
        typeRepository = FakeTypeRepository()
        useCase = GetAssetListUseCase(
            assetRepository = assetRepository,
            recordRepository = recordRepository,
            typeRepository = typeRepository,
        )
    }

    @Test
    fun when_has_assets_then_sorted_by_recent_usage_descending() = runTest {
        val asset1 = createAssetModel(id = 1L, name = "资产A")
        val asset2 = createAssetModel(id = 2L, name = "资产B")
        assetRepository.addAsset(asset1)
        assetRepository.addAsset(asset2)
        // 资产B有更多记录
        recordRepository.addRecord(createRecordModel(id = 1L, assetId = 2L))
        recordRepository.addRecord(createRecordModel(id = 2L, assetId = 2L))
        recordRepository.addRecord(createRecordModel(id = 3L, assetId = 1L))

        val result = useCase(
            currentTypeId = 1L,
            selectedAssetId = -1L,
            isRelated = false,
        ).first()

        // 资产B使用次数更多，排在前面
        assertThat(result.first().name).isEqualTo("资产B")
    }

    @Test
    fun given_credit_payment_type_when_is_related_then_only_shows_credit_cards() = runTest {
        val capitalAsset = createAssetModel(
            id = 1L,
            name = "储蓄卡",
            type = ClassificationTypeEnum.CAPITAL_ACCOUNT,
        )
        val creditAsset = createAssetModel(
            id = 2L,
            name = "信用卡",
            type = ClassificationTypeEnum.CREDIT_CARD_ACCOUNT,
        )
        assetRepository.addAsset(capitalAsset)
        assetRepository.addAsset(creditAsset)

        val result = useCase(
            currentTypeId = FIXED_TYPE_ID_CREDIT_CARD_PAYMENT,
            selectedAssetId = -1L,
            isRelated = true,
        ).first()

        assertThat(result).hasSize(1)
        assertThat(result.first().name).isEqualTo("信用卡")
    }

    @Test
    fun when_selected_asset_then_excluded_from_result() = runTest {
        val asset1 = createAssetModel(id = 1L, name = "资产A")
        val asset2 = createAssetModel(id = 2L, name = "资产B")
        assetRepository.addAsset(asset1)
        assetRepository.addAsset(asset2)

        val result = useCase(
            currentTypeId = 1L,
            selectedAssetId = 1L,
            isRelated = false,
        ).first()

        assertThat(result).hasSize(1)
        assertThat(result.first().id).isEqualTo(2L)
    }

    @Test
    fun when_not_related_or_not_credit_payment_then_shows_all_types() = runTest {
        val capitalAsset = createAssetModel(
            id = 1L,
            name = "储蓄卡",
            type = ClassificationTypeEnum.CAPITAL_ACCOUNT,
        )
        val creditAsset = createAssetModel(
            id = 2L,
            name = "信用卡",
            type = ClassificationTypeEnum.CREDIT_CARD_ACCOUNT,
        )
        assetRepository.addAsset(capitalAsset)
        assetRepository.addAsset(creditAsset)

        val result = useCase(
            currentTypeId = 1L,
            selectedAssetId = -1L,
            isRelated = false,
        ).first()

        assertThat(result).hasSize(2)
    }
}
