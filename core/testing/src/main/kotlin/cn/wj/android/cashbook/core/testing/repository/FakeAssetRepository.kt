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

package cn.wj.android.cashbook.core.testing.repository

import cn.wj.android.cashbook.core.data.repository.AssetRepository
import cn.wj.android.cashbook.core.model.model.AssetModel
import cn.wj.android.cashbook.core.model.model.AssetTypeViewsModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAssetRepository : AssetRepository {

    private val assets = mutableListOf<AssetModel>()
    private val _currentVisibleAssetListData = MutableStateFlow<List<AssetModel>>(emptyList())
    private val _currentVisibleAssetTypeData = MutableStateFlow<List<AssetTypeViewsModel>>(emptyList())
    private val _currentInvisibleAssetListData = MutableStateFlow<List<AssetModel>>(emptyList())
    private val _currentInvisibleAssetTypeData = MutableStateFlow<List<AssetTypeViewsModel>>(emptyList())
    private val _topUpInTotalData = MutableStateFlow(false)

    var lastUpdatedAsset: AssetModel? = null
        private set

    override val currentVisibleAssetListData: Flow<List<AssetModel>> = _currentVisibleAssetListData
    override val currentVisibleAssetTypeData: Flow<List<AssetTypeViewsModel>> = _currentVisibleAssetTypeData
    override val currentInvisibleAssetListData: Flow<List<AssetModel>> = _currentInvisibleAssetListData
    override val currentInvisibleAssetTypeData: Flow<List<AssetTypeViewsModel>> = _currentInvisibleAssetTypeData
    override val topUpInTotalData: Flow<Boolean> = _topUpInTotalData

    fun addAsset(asset: AssetModel) {
        assets.add(asset)
        updateFlows()
    }

    fun setVisibleAssets(list: List<AssetModel>) {
        _currentVisibleAssetListData.value = list
    }

    fun setInvisibleAssets(list: List<AssetModel>) {
        _currentInvisibleAssetListData.value = list
    }

    private fun updateFlows() {
        _currentVisibleAssetListData.value = assets.filter { !it.invisible }
        _currentInvisibleAssetListData.value = assets.filter { it.invisible }
    }

    override suspend fun getAssetById(assetId: Long): AssetModel? {
        return assets.find { it.id == assetId }
    }

    override suspend fun getVisibleAssetsByBookId(bookId: Long): List<AssetModel> {
        return assets.filter { it.booksId == bookId && !it.invisible }
    }

    override suspend fun getInvisibleAssetsByBookId(bookId: Long): List<AssetModel> {
        return assets.filter { it.booksId == bookId && it.invisible }
    }

    override suspend fun updateAsset(asset: AssetModel) {
        lastUpdatedAsset = asset
        val index = assets.indexOfFirst { it.id == asset.id }
        if (index >= 0) {
            assets[index] = asset
        } else {
            assets.add(asset)
        }
        updateFlows()
    }

    override suspend fun deleteById(assetId: Long) {
        assets.removeAll { it.id == assetId }
        updateFlows()
    }

    override suspend fun visibleAssetById(id: Long) {
        val index = assets.indexOfFirst { it.id == id }
        if (index >= 0) {
            assets[index] = assets[index].copy(invisible = false)
            updateFlows()
        }
    }

    override suspend fun updateTopUpInTotal(topUpInTotal: Boolean) {
        _topUpInTotalData.value = topUpInTotal
    }
}
