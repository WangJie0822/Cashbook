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

package cn.wj.android.cashbook.core.data.testdoubles

import cn.wj.android.cashbook.core.common.SWITCH_INT_OFF
import cn.wj.android.cashbook.core.common.SWITCH_INT_ON
import cn.wj.android.cashbook.core.database.dao.AssetDao
import cn.wj.android.cashbook.core.database.table.AssetTable

/**
 * AssetDao 的测试替身，使用内存列表存储数据
 */
class FakeAssetDao : AssetDao {

    /** 资产数据列表 */
    val assets = mutableListOf<AssetTable>()

    /** 自增主键计数器 */
    private var nextId = 1L

    override suspend fun insert(assetTable: AssetTable) {
        val withId = if (assetTable.id == null) assetTable.copy(id = nextId++) else assetTable
        assets.add(withId)
    }

    override suspend fun update(assetTable: AssetTable) {
        val index = assets.indexOfFirst { it.id == assetTable.id }
        if (index >= 0) {
            assets[index] = assetTable
        }
    }

    override suspend fun queryAssetById(assetId: Long): AssetTable? {
        return assets.firstOrNull { it.id == assetId }
    }

    override suspend fun queryVisibleAssetByBookId(bookId: Long): List<AssetTable> {
        return assets.filter { it.booksId == bookId && it.invisible == SWITCH_INT_OFF }
    }

    override suspend fun queryInvisibleAssetByBookId(bookId: Long): List<AssetTable> {
        return assets.filter { it.booksId == bookId && it.invisible == SWITCH_INT_ON }
    }

    override suspend fun deleteById(assetId: Long) {
        assets.removeAll { it.id == assetId }
    }

    override suspend fun visibleById(id: Long) {
        val index = assets.indexOfFirst { it.id == id }
        if (index >= 0) {
            assets[index] = assets[index].copy(invisible = SWITCH_INT_OFF)
        }
    }
}
