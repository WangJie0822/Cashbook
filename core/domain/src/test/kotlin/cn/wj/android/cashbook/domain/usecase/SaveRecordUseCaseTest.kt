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
import cn.wj.android.cashbook.core.common.NO_ASSET_ID
import cn.wj.android.cashbook.core.testing.data.createRecordModel
import cn.wj.android.cashbook.core.testing.repository.FakeRecordRepository
import cn.wj.android.cashbook.core.testing.repository.FakeTypeRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SaveRecordUseCaseTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var recordRepository: FakeRecordRepository
    private lateinit var typeRepository: FakeTypeRepository
    private lateinit var useCase: SaveRecordUseCase

    @Before
    fun setup() {
        recordRepository = FakeRecordRepository()
        typeRepository = FakeTypeRepository()
        useCase = SaveRecordUseCase(
            recordRepository = recordRepository,
            typeRepository = typeRepository,
            coroutineContext = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun when_save_record_then_needRelated_fetched_from_type_repository() = runTest {
        // 使用固定退款类型 ID，自动判断为 needRelated
        val record = createRecordModel(typeId = FIXED_TYPE_ID_REFUND, amount = 100L)

        useCase(record, listOf(1L), listOf(2L), emptyList())

        assertThat(recordRepository.lastUpdatedNeedRelated).isTrue()
        assertThat(recordRepository.lastUpdatedRecord).isEqualTo(record)
        assertThat(recordRepository.lastUpdatedTagIdList).containsExactly(1L)
    }

    @Test
    fun when_save_record_with_no_related_type_then_needRelated_is_false() = runTest {
        val record = createRecordModel(typeId = 2L, amount = 100L)

        useCase(record, emptyList(), emptyList(), emptyList())

        assertThat(recordRepository.lastUpdatedNeedRelated).isFalse()
    }

    @Test
    fun when_save_record_then_record_is_passed_to_repository() = runTest {
        val record = createRecordModel(
            id = 10L,
            amount = 10000L,
            remark = "测试记录",
        )

        useCase(record, listOf(1L, 2L), listOf(3L), emptyList())

        assertThat(recordRepository.lastUpdatedRecord).isEqualTo(record)
        assertThat(recordRepository.lastUpdatedTagIdList).containsExactly(1L, 2L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun when_amount_is_zero_then_throws() = runTest {
        val record = createRecordModel(amount = 0L)
        useCase(record, emptyList(), emptyList(), emptyList())
    }

    @Test(expected = IllegalArgumentException::class)
    fun when_amount_is_negative_then_throws() = runTest {
        val record = createRecordModel(amount = -100L)
        useCase(record, emptyList(), emptyList(), emptyList())
    }

    @Test(expected = IllegalArgumentException::class)
    fun when_charges_is_negative_then_throws() = runTest {
        val record = createRecordModel(amount = 100L, charges = -1L)
        useCase(record, emptyList(), emptyList(), emptyList())
    }

    @Test(expected = IllegalArgumentException::class)
    fun when_concessions_is_negative_then_throws() = runTest {
        val record = createRecordModel(amount = 100L, concessions = -1L)
        useCase(record, emptyList(), emptyList(), emptyList())
    }

    @Test(expected = IllegalArgumentException::class)
    fun when_recordTime_is_zero_then_throws() = runTest {
        val record = createRecordModel(amount = 100L, recordTime = 0L)
        useCase(record, emptyList(), emptyList(), emptyList())
    }

    @Test
    fun when_assetId_is_zero_then_normalized_to_no_asset_id() = runTest {
        // 模拟历史版本升级后 assetId 为 0 的场景
        val record = createRecordModel(amount = 100L, assetId = 0L)

        useCase(record, emptyList(), emptyList(), emptyList())

        // 验证 assetId 被转换为 NO_ASSET_ID（-1L）
        assertThat(recordRepository.lastUpdatedRecord?.assetId).isEqualTo(NO_ASSET_ID)
    }

    @Test
    fun when_assetId_is_negative_but_not_no_asset_id_then_normalized() = runTest {
        // 模拟 assetId 为其他无效负数的场景
        val record = createRecordModel(amount = 100L, assetId = -2L)

        useCase(record, emptyList(), emptyList(), emptyList())

        // 验证 assetId 被转换为 NO_ASSET_ID（-1L）
        assertThat(recordRepository.lastUpdatedRecord?.assetId).isEqualTo(NO_ASSET_ID)
    }

    @Test
    fun when_assetId_is_valid_positive_then_unchanged() = runTest {
        // 模拟有效的 assetId
        val record = createRecordModel(amount = 100L, assetId = 5L)

        useCase(record, emptyList(), emptyList(), emptyList())

        // 验证 assetId 保持不变
        assertThat(recordRepository.lastUpdatedRecord?.assetId).isEqualTo(5L)
    }

    @Test
    fun when_assetId_is_no_asset_id_then_unchanged() = runTest {
        // 模拟 assetId 已经是 NO_ASSET_ID 的场景
        val record = createRecordModel(amount = 100L, assetId = NO_ASSET_ID)

        useCase(record, emptyList(), emptyList(), emptyList())

        // 验证 assetId 保持为 NO_ASSET_ID（-1L）
        assertThat(recordRepository.lastUpdatedRecord?.assetId).isEqualTo(NO_ASSET_ID)
    }
}
