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

package cn.wj.android.cashbook.core.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cn.wj.android.cashbook.core.common.SWITCH_INT_OFF
import cn.wj.android.cashbook.core.common.SWITCH_INT_ON
import cn.wj.android.cashbook.core.database.CashbookDatabase
import cn.wj.android.cashbook.core.database.table.AssetTable
import cn.wj.android.cashbook.core.database.table.BooksTable
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * AssetDao 数据库操作测试
 */
@RunWith(AndroidJUnit4::class)
class AssetDaoTest {

    private lateinit var database: CashbookDatabase
    private lateinit var assetDao: AssetDao
    private lateinit var booksDao: BooksDao

    /** 测试用账本 id */
    private var testBookId: Long = 0L

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            CashbookDatabase::class.java,
        ).allowMainThreadQueries().build()
        assetDao = database.assetDao()
        booksDao = database.booksDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    /** 创建测试用账本并返回其 id */
    private suspend fun createTestBook(): Long {
        return booksDao.insert(
            BooksTable(
                id = null,
                name = "测试账本",
                description = "",
                bgUri = "",
                modifyTime = System.currentTimeMillis(),
            ),
        )
    }

    /** 创建测试用资产表记录 */
    private fun createAssetTable(
        id: Long? = null,
        booksId: Long = testBookId,
        name: String = "测试资产",
        balance: Long = 0L,
        totalAmount: Long = 0L,
        billingDate: String = "",
        repaymentDate: String = "",
        type: Int = 0,
        classification: Int = 0,
        invisible: Int = SWITCH_INT_OFF,
        openBank: String = "",
        cardNo: String = "",
        remark: String = "",
        sort: Int = 0,
        modifyTime: Long = System.currentTimeMillis(),
    ): AssetTable = AssetTable(
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

    @Test
    fun when_insertAsset_then_queryByIdReturnsIt() = runTest {
        testBookId = createTestBook()

        // 插入资产记录
        val asset = createAssetTable(name = "银行卡", balance = 100000L)
        assetDao.insert(asset)

        // 通过 id 查询（autoGenerate 从 1 开始）
        val result = assetDao.queryAssetById(1L)
        assertThat(result).isNotNull()
        assertThat(result!!.name).isEqualTo("银行卡")
        assertThat(result.balance).isEqualTo(100000L)
        assertThat(result.booksId).isEqualTo(testBookId)
    }

    @Test
    fun when_updateAsset_then_queryReflectsChanges() = runTest {
        testBookId = createTestBook()

        // 插入资产记录
        val asset = createAssetTable(name = "储蓄卡", balance = 50000L)
        assetDao.insert(asset)

        // 查询获取插入后的完整记录（包含自增 id）
        val inserted = assetDao.queryAssetById(1L)
        assertThat(inserted).isNotNull()

        // 更新资产信息
        val updated = inserted!!.copy(name = "工资卡", balance = 200000L, remark = "主账户")
        assetDao.update(updated)

        // 验证更新后的数据
        val result = assetDao.queryAssetById(1L)
        assertThat(result).isNotNull()
        assertThat(result!!.name).isEqualTo("工资卡")
        assertThat(result.balance).isEqualTo(200000L)
        assertThat(result.remark).isEqualTo("主账户")
    }

    @Test
    fun when_queryVisibleByBookId_then_returnsOnlyVisible() = runTest {
        testBookId = createTestBook()

        // 插入可见资产
        assetDao.insert(createAssetTable(name = "可见资产1", invisible = SWITCH_INT_OFF))
        assetDao.insert(createAssetTable(name = "可见资产2", invisible = SWITCH_INT_OFF))

        // 插入隐藏资产
        assetDao.insert(createAssetTable(name = "隐藏资产1", invisible = SWITCH_INT_ON))

        // 查询可见资产，应只返回 2 条
        val visibleAssets = assetDao.queryVisibleAssetByBookId(testBookId)
        assertThat(visibleAssets).hasSize(2)

        val names = visibleAssets.map { it.name }
        assertThat(names).containsExactly("可见资产1", "可见资产2")
    }

    @Test
    fun when_queryInvisibleByBookId_then_returnsOnlyInvisible() = runTest {
        testBookId = createTestBook()

        // 插入可见资产
        assetDao.insert(createAssetTable(name = "可见资产", invisible = SWITCH_INT_OFF))

        // 插入隐藏资产
        assetDao.insert(createAssetTable(name = "隐藏资产1", invisible = SWITCH_INT_ON))
        assetDao.insert(createAssetTable(name = "隐藏资产2", invisible = SWITCH_INT_ON))

        // 查询隐藏资产，应只返回 2 条
        val invisibleAssets = assetDao.queryInvisibleAssetByBookId(testBookId)
        assertThat(invisibleAssets).hasSize(2)

        val names = invisibleAssets.map { it.name }
        assertThat(names).containsExactly("隐藏资产1", "隐藏资产2")
    }

    @Test
    fun when_deleteById_then_assetIsRemoved() = runTest {
        testBookId = createTestBook()

        // 插入资产记录
        assetDao.insert(createAssetTable(name = "待删除资产"))
        val inserted = assetDao.queryAssetById(1L)
        assertThat(inserted).isNotNull()

        // 删除资产
        assetDao.deleteById(1L)

        // 验证已被删除
        val result = assetDao.queryAssetById(1L)
        assertThat(result).isNull()
    }

    @Test
    fun when_visibleById_then_assetBecomesVisible() = runTest {
        testBookId = createTestBook()

        // 插入隐藏资产
        assetDao.insert(createAssetTable(name = "隐藏资产", invisible = SWITCH_INT_ON))

        // 验证初始状态为隐藏
        val before = assetDao.queryAssetById(1L)
        assertThat(before).isNotNull()
        assertThat(before!!.invisible).isEqualTo(SWITCH_INT_ON)

        // 调用 visibleById 设置为可见
        assetDao.visibleById(1L)

        // 验证已变为可见
        val after = assetDao.queryAssetById(1L)
        assertThat(after).isNotNull()
        assertThat(after!!.invisible).isEqualTo(SWITCH_INT_OFF)
    }

    @Test
    fun when_queryByNonExistentId_then_returnsNull() = runTest {
        // 查询不存在的资产 id，应返回 null
        val result = assetDao.queryAssetById(999L)
        assertThat(result).isNull()
    }
}
