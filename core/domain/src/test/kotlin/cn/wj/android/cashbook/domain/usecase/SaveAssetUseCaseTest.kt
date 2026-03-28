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

import cn.wj.android.cashbook.core.model.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.core.model.model.RECORD_TYPE_BALANCE_EXPENDITURE
import cn.wj.android.cashbook.core.model.model.RECORD_TYPE_BALANCE_INCOME
import cn.wj.android.cashbook.core.testing.data.createAssetModel
import cn.wj.android.cashbook.core.testing.repository.FakeAssetRepository
import cn.wj.android.cashbook.core.testing.repository.FakeRecordRepository
import cn.wj.android.cashbook.core.testing.util.TestDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SaveAssetUseCaseTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var recordRepository: FakeRecordRepository
    private lateinit var assetRepository: FakeAssetRepository
    private lateinit var useCase: SaveAssetUseCase

    @Before
    fun setup() {
        recordRepository = FakeRecordRepository()
        assetRepository = FakeAssetRepository()
        useCase = SaveAssetUseCase(
            recordRepository = recordRepository,
            assetRepository = assetRepository,
            coroutineContext = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun when_save_new_asset_then_directly_inserts() = runTest {
        val asset = createAssetModel(id = -1L, name = "新资产", balance = 100000L)

        useCase(asset)

        assertThat(assetRepository.lastUpdatedAsset).isEqualTo(asset)
        // 新资产不生成平账记录
        assertThat(recordRepository.lastUpdatedRecord).isNull()
    }

    @Test
    fun when_modify_asset_without_balance_change_then_no_record_generated() = runTest {
        val oldAsset = createAssetModel(id = 1L, name = "旧资产", balance = 100000L)
        assetRepository.addAsset(oldAsset)

        val newAsset = oldAsset.copy(name = "改名资产")
        useCase(newAsset)

        // 没有余额变化，不生成平账记录
        assertThat(recordRepository.lastUpdatedRecord).isNull()
    }

    @Test
    fun given_capital_account_when_balance_increases_then_generates_income_record() = runTest {
        val oldAsset = createAssetModel(
            id = 1L,
            balance = 100000L,
            type = ClassificationTypeEnum.CAPITAL_ACCOUNT,
        )
        assetRepository.addAsset(oldAsset)

        val newAsset = oldAsset.copy(balance = 150000L)
        useCase(newAsset)

        // 资金账户余额增加 → 收入平账记录
        val record = recordRepository.lastUpdatedRecord
        assertThat(record).isNotNull()
        assertThat(record!!.typeId).isEqualTo(RECORD_TYPE_BALANCE_INCOME.id)
        assertThat(record.amount).isEqualTo(50000L)
        assertThat(record.assetId).isEqualTo(1L)
    }

    @Test
    fun given_capital_account_when_balance_decreases_then_generates_expenditure_record() = runTest {
        val oldAsset = createAssetModel(
            id = 1L,
            balance = 100000L,
            type = ClassificationTypeEnum.CAPITAL_ACCOUNT,
        )
        assetRepository.addAsset(oldAsset)

        val newAsset = oldAsset.copy(balance = 70000L)
        useCase(newAsset)

        // 资金账户余额减少 → 支出平账记录
        val record = recordRepository.lastUpdatedRecord
        assertThat(record).isNotNull()
        assertThat(record!!.typeId).isEqualTo(RECORD_TYPE_BALANCE_EXPENDITURE.id)
        assertThat(record.amount).isEqualTo(30000L)
    }

    @Test
    fun given_credit_card_when_used_amount_increases_then_generates_expenditure_record() = runTest {
        val oldAsset = createAssetModel(
            id = 1L,
            balance = 50000L,
            type = ClassificationTypeEnum.CREDIT_CARD_ACCOUNT,
        )
        assetRepository.addAsset(oldAsset)

        val newAsset = oldAsset.copy(balance = 80000L)
        useCase(newAsset)

        // 信用卡已使用额度增加 → 支出平账记录
        val record = recordRepository.lastUpdatedRecord
        assertThat(record).isNotNull()
        assertThat(record!!.typeId).isEqualTo(RECORD_TYPE_BALANCE_EXPENDITURE.id)
        assertThat(record.amount).isEqualTo(30000L)
    }

    @Test
    fun given_credit_card_when_used_amount_decreases_then_generates_income_record() = runTest {
        val oldAsset = createAssetModel(
            id = 1L,
            balance = 80000L,
            type = ClassificationTypeEnum.CREDIT_CARD_ACCOUNT,
        )
        assetRepository.addAsset(oldAsset)

        val newAsset = oldAsset.copy(balance = 50000L)
        useCase(newAsset)

        // 信用卡已使用额度减少 → 收入平账记录
        val record = recordRepository.lastUpdatedRecord
        assertThat(record).isNotNull()
        assertThat(record!!.typeId).isEqualTo(RECORD_TYPE_BALANCE_INCOME.id)
        assertThat(record.amount).isEqualTo(30000L)
    }

    @Test
    fun when_modify_asset_then_updates_non_balance_fields_first() = runTest {
        val oldAsset = createAssetModel(
            id = 1L,
            name = "旧名称",
            openBank = "旧银行",
            balance = 100000L,
        )
        assetRepository.addAsset(oldAsset)

        val newAsset = oldAsset.copy(name = "新名称", openBank = "新银行")
        useCase(newAsset)

        val updated = assetRepository.lastUpdatedAsset
        assertThat(updated).isNotNull()
        assertThat(updated!!.name).isEqualTo("新名称")
        assertThat(updated.openBank).isEqualTo("新银行")
    }
}
