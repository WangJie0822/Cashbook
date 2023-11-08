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

import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.common.ext.decimalFormat
import cn.wj.android.cashbook.core.common.ext.toBigDecimalOrZero
import cn.wj.android.cashbook.core.common.model.assetDataVersion
import cn.wj.android.cashbook.core.common.model.updateVersion
import cn.wj.android.cashbook.core.data.helper.AssetHelper
import cn.wj.android.cashbook.core.data.repository.AssetRepository
import cn.wj.android.cashbook.core.data.repository.asModel
import cn.wj.android.cashbook.core.data.repository.asTable
import cn.wj.android.cashbook.core.database.dao.AssetDao
import cn.wj.android.cashbook.core.datastore.datasource.AppPreferencesDataSource
import cn.wj.android.cashbook.core.model.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.core.model.model.AssetModel
import cn.wj.android.cashbook.core.model.model.AssetTypeViewsModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * 资产类型数据仓库
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/20
 */
class AssetRepositoryImpl @Inject constructor(
    private val assetDao: AssetDao,
    private val appPreferencesDataSource: AppPreferencesDataSource,
    @Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext,
) : AssetRepository {

    override val currentVisibleAssetListData: Flow<List<AssetModel>> =
        combine(assetDataVersion, appPreferencesDataSource.appData) { _, appData ->
            getVisibleAssetsByBookId(appData.currentBookId)
        }

    override val currentVisibleAssetTypeData: Flow<List<AssetTypeViewsModel>> =
        currentVisibleAssetListData.mapLatest { list ->
            val result = mutableListOf<AssetTypeViewsModel>()
            ClassificationTypeEnum.entries.forEach { type ->
                val assetList = list.filter { it.type == type }
                if (assetList.isNotEmpty()) {
                    var totalAmount = BigDecimal.ZERO
                    assetList.forEach { asset ->
                        totalAmount += asset.balance.toBigDecimalOrZero()
                    }
                    result.add(
                        AssetTypeViewsModel(
                            nameResId = AssetHelper.getNameResIdByType(type),
                            totalAmount = totalAmount.decimalFormat(),
                            assetList = assetList,
                        ),
                    )
                }
            }
            result
        }

    override val currentInvisibleAssetListData: Flow<List<AssetModel>> =
        combine(assetDataVersion, appPreferencesDataSource.appData) { _, appData ->
            getInvisibleAssetsByBookId(appData.currentBookId)
        }

    override val currentInvisibleAssetTypeData: Flow<List<AssetTypeViewsModel>> =
        currentInvisibleAssetListData.mapLatest { list ->
            val result = mutableListOf<AssetTypeViewsModel>()
            ClassificationTypeEnum.entries.forEach { type ->
                val assetList = list.filter { it.type == type }
                if (assetList.isNotEmpty()) {
                    var totalAmount = BigDecimal.ZERO
                    assetList.forEach { asset ->
                        totalAmount += asset.balance.toBigDecimalOrZero()
                    }
                    result.add(
                        AssetTypeViewsModel(
                            nameResId = AssetHelper.getNameResIdByType(type),
                            totalAmount = totalAmount.decimalFormat(),
                            assetList = assetList,
                        ),
                    )
                }
            }
            result
        }

    override suspend fun getAssetById(assetId: Long): AssetModel? = withContext(coroutineContext) {
        assetDao.queryAssetById(assetId)?.asModel()
    }

    override suspend fun getVisibleAssetsByBookId(bookId: Long): List<AssetModel> =
        withContext(coroutineContext) {
            assetDao.queryVisibleAssetByBookId(bookId)
                .map { it.asModel() }
        }

    override suspend fun getInvisibleAssetsByBookId(bookId: Long): List<AssetModel> =
        withContext(coroutineContext) {
            assetDao.queryInvisibleAssetByBookId(bookId)
                .map { it.asModel() }
        }

    override suspend fun updateAsset(asset: AssetModel) = withContext(coroutineContext) {
        var table = asset.asTable()
        if (table.booksId <= 0) {
            table = table.copy(booksId = appPreferencesDataSource.appData.first().currentBookId)
        }
        if (null == table.id) {
            // 插入
            assetDao.insert(table)
        } else {
            assetDao.update(table)
        }
        assetDataVersion.updateVersion()
    }

    override suspend fun deleteById(assetId: Long) = withContext(coroutineContext) {
        assetDao.deleteById(assetId)
        assetDataVersion.updateVersion()
    }

    override suspend fun visibleAssetById(id: Long) = withContext(coroutineContext) {
        assetDao.visibleById(id)
        assetDataVersion.updateVersion()
    }
}
