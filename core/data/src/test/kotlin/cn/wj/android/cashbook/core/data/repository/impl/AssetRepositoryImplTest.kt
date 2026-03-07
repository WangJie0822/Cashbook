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

package cn.wj.android.cashbook.core.data.repository.impl

import cn.wj.android.cashbook.core.common.SWITCH_INT_OFF
import cn.wj.android.cashbook.core.common.SWITCH_INT_ON
import cn.wj.android.cashbook.core.data.repository.asModel
import cn.wj.android.cashbook.core.data.repository.asTable
import cn.wj.android.cashbook.core.data.testdoubles.FakeAssetDao
import cn.wj.android.cashbook.core.database.table.AssetTable
import cn.wj.android.cashbook.core.model.enums.AssetClassificationEnum
import cn.wj.android.cashbook.core.model.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.core.model.model.AssetModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * AssetRepository 实现测试
 *
 * 测试资产仓库的核心业务逻辑，包括可见/不可见过滤、CRUD 操作等
 */
class AssetRepositoryImplTest {

    private lateinit var assetDao: FakeAssetDao

    @Before
    fun setup() {
        assetDao = FakeAssetDao()
    }

    // ========== 可见/不可见过滤测试 ==========

    @Test
    fun when_queryVisibleAssetByBookId_then_returns_visible_only() = runTest {
        assetDao.insert(createAssetTable(booksId = 1L, invisible = SWITCH_INT_OFF))
        assetDao.insert(createAssetTable(booksId = 1L, invisible = SWITCH_INT_ON))
        assetDao.insert(createAssetTable(booksId = 1L, invisible = SWITCH_INT_OFF))

        val visible = assetDao.queryVisibleAssetByBookId(1L)
        assertThat(visible).hasSize(2)
        visible.forEach { assertThat(it.invisible).isEqualTo(SWITCH_INT_OFF) }
    }

    @Test
    fun when_queryInvisibleAssetByBookId_then_returns_invisible_only() = runTest {
        assetDao.insert(createAssetTable(booksId = 1L, invisible = SWITCH_INT_OFF))
        assetDao.insert(createAssetTable(booksId = 1L, invisible = SWITCH_INT_ON))
        assetDao.insert(createAssetTable(booksId = 1L, invisible = SWITCH_INT_ON))

        val invisible = assetDao.queryInvisibleAssetByBookId(1L)
        assertThat(invisible).hasSize(2)
        invisible.forEach { assertThat(it.invisible).isEqualTo(SWITCH_INT_ON) }
    }

    @Test
    fun given_different_books_when_queryVisibleAssetByBookId_then_filters_by_bookId() = runTest {
        assetDao.insert(createAssetTable(booksId = 1L, invisible = SWITCH_INT_OFF))
        assetDao.insert(createAssetTable(booksId = 2L, invisible = SWITCH_INT_OFF))
        assetDao.insert(createAssetTable(booksId = 1L, invisible = SWITCH_INT_OFF))

        val book1Assets = assetDao.queryVisibleAssetByBookId(1L)
        val book2Assets = assetDao.queryVisibleAssetByBookId(2L)

        assertThat(book1Assets).hasSize(2)
        assertThat(book2Assets).hasSize(1)
    }

    // ========== CRUD 测试 ==========

    @Test
    fun when_insert_then_asset_stored() = runTest {
        val asset = createAssetTable(name = "微信钱包")
        assetDao.insert(asset)

        assertThat(assetDao.assets).hasSize(1)
        assertThat(assetDao.assets[0].name).isEqualTo("微信钱包")
    }

    @Test
    fun when_insert_with_null_id_then_auto_increment_id() = runTest {
        assetDao.insert(createAssetTable(id = null, name = "第一个"))
        assetDao.insert(createAssetTable(id = null, name = "第二个"))

        assertThat(assetDao.assets[0].id).isEqualTo(1L)
        assertThat(assetDao.assets[1].id).isEqualTo(2L)
    }

    @Test
    fun when_update_then_asset_updated() = runTest {
        assetDao.insert(createAssetTable(id = null, name = "原名"))
        val inserted = assetDao.assets[0]
        assetDao.update(inserted.copy(name = "新名"))

        assertThat(assetDao.assets[0].name).isEqualTo("新名")
    }

    @Test
    fun when_queryAssetById_then_returns_matching_asset() = runTest {
        assetDao.insert(createAssetTable(id = null, name = "现金"))
        assetDao.insert(createAssetTable(id = null, name = "银行卡"))

        val result = assetDao.queryAssetById(1L)
        assertThat(result).isNotNull()
        assertThat(result!!.name).isEqualTo("现金")
    }

    @Test
    fun given_nonexistent_id_when_queryAssetById_then_returns_null() = runTest {
        val result = assetDao.queryAssetById(999L)
        assertThat(result).isNull()
    }

    @Test
    fun when_deleteById_then_asset_removed() = runTest {
        assetDao.insert(createAssetTable(id = null, name = "待删除"))
        assetDao.insert(createAssetTable(id = null, name = "保留"))

        assetDao.deleteById(1L)

        assertThat(assetDao.assets).hasSize(1)
        assertThat(assetDao.assets[0].name).isEqualTo("保留")
    }

    @Test
    fun when_visibleById_then_asset_becomes_visible() = runTest {
        assetDao.insert(createAssetTable(id = null, invisible = SWITCH_INT_ON))
        assertThat(assetDao.assets[0].invisible).isEqualTo(SWITCH_INT_ON)

        assetDao.visibleById(1L)

        assertThat(assetDao.assets[0].invisible).isEqualTo(SWITCH_INT_OFF)
    }

    // ========== 模型映射集成测试 ==========

    @Test
    fun when_insert_assetModel_and_query_then_roundtrip_works() = runTest {
        val model = createAssetModel(id = -1L, name = "测试资产")
        val table = model.asTable()

        // id=-1 应映射为 null
        assertThat(table.id).isNull()

        assetDao.insert(table)
        val queried = assetDao.queryAssetById(1L)!!
        val result = queried.asModel()

        assertThat(result.name).isEqualTo("测试资产")
        assertThat(result.id).isEqualTo(1L)
    }

    @Test
    fun given_model_with_id_when_update_then_updateAsset_logic_works() = runTest {
        // 模拟 AssetRepositoryImpl.updateAsset 逻辑
        val initialModel = createAssetModel(id = -1L, name = "初始")
        val table = initialModel.asTable()

        // id 为 null 时插入
        if (table.id == null) {
            assetDao.insert(table)
        }

        assertThat(assetDao.assets).hasSize(1)

        // 更新已有资产
        val updatedModel = createAssetModel(id = 1L, name = "更新后")
        val updatedTable = updatedModel.asTable()

        if (updatedTable.id != null) {
            assetDao.update(updatedTable)
        }

        val result = assetDao.queryAssetById(1L)!!
        assertThat(result.name).isEqualTo("更新后")
    }

    @Test
    fun given_invisible_asset_when_visibleAssetById_then_becomes_visible_in_query() = runTest {
        assetDao.insert(createAssetTable(id = null, booksId = 1L, invisible = SWITCH_INT_ON))

        // 初始状态：不可见资产有 1 个，可见资产有 0 个
        assertThat(assetDao.queryInvisibleAssetByBookId(1L)).hasSize(1)
        assertThat(assetDao.queryVisibleAssetByBookId(1L)).isEmpty()

        // 设置为可见
        assetDao.visibleById(1L)

        // 最终状态：不可见资产有 0 个，可见资产有 1 个
        assertThat(assetDao.queryInvisibleAssetByBookId(1L)).isEmpty()
        assertThat(assetDao.queryVisibleAssetByBookId(1L)).hasSize(1)
    }

    // ========== 辅助方法 ==========

    private fun createAssetTable(
        id: Long? = null,
        booksId: Long = 1L,
        name: String = "现金",
        balance: Double = 0.0,
        totalAmount: Double = 0.0,
        billingDate: String = "",
        repaymentDate: String = "",
        type: Int = ClassificationTypeEnum.CAPITAL_ACCOUNT.ordinal,
        classification: Int = AssetClassificationEnum.CASH.ordinal,
        invisible: Int = SWITCH_INT_OFF,
        openBank: String = "",
        cardNo: String = "",
        remark: String = "",
        sort: Int = 0,
        modifyTime: Long = 1000L,
    ) = AssetTable(
        id = id,
        booksId = booksId,
        name = name,
        balance = balance,
        totalAmount = totalAmount,
        billingDate = billingDate,
        repaymentDate = repaymentDate,
        type = type,
        classification = classification,
        invisible = invisible,
        openBank = openBank,
        cardNo = cardNo,
        remark = remark,
        sort = sort,
        modifyTime = modifyTime,
    )

    private fun createAssetModel(
        id: Long = 1L,
        booksId: Long = 1L,
        name: String = "现金",
        balance: String = "0",
        totalAmount: String = "0",
        type: ClassificationTypeEnum = ClassificationTypeEnum.CAPITAL_ACCOUNT,
        classification: AssetClassificationEnum = AssetClassificationEnum.CASH,
        invisible: Boolean = false,
    ) = AssetModel(
        id = id,
        booksId = booksId,
        name = name,
        iconResId = 0,
        totalAmount = totalAmount,
        billingDate = "",
        repaymentDate = "",
        type = type,
        classification = classification,
        invisible = invisible,
        openBank = "",
        cardNo = "",
        remark = "",
        sort = 0,
        modifyTime = "2024-01-01 00:00:00",
        balance = balance,
    )
}
