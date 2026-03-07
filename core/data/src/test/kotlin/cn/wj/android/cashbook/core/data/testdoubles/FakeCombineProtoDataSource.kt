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

import cn.wj.android.cashbook.core.model.model.RecordSettingsModel
import cn.wj.android.cashbook.core.model.model.SearchHistoryModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * CombineProtoDataSource 的测试替身
 *
 * 由于 CombineProtoDataSource 依赖 DataStore/Proto，测试中不便直接使用，
 * 这里提供一个接口相似的 Fake 实现，供 Repository 测试使用。
 *
 * 注意：实际的 CombineProtoDataSource 是一个 final class 且构造函数需要多个 DataStore，
 * 所以不能直接继承。Repository 测试中需要通过调整构造方式来使用此 Fake。
 */
class FakeCombineProtoDataSource {

    private val _recordSettings = MutableStateFlow(
        RecordSettingsModel(
            currentBookId = 1L,
            defaultTypeId = 1L,
            lastAssetId = -1L,
            refundTypeId = 0L,
            reimburseTypeId = 0L,
            creditCardPaymentTypeId = 0L,
            topUpInTotal = false,
        ),
    )

    private val _searchHistory = MutableStateFlow(
        SearchHistoryModel(keywords = emptyList()),
    )

    /** 记录设置数据 */
    val recordSettingsData: Flow<RecordSettingsModel> = _recordSettings

    /** 搜索历史数据 */
    val searchHistoryData: Flow<SearchHistoryModel> = _searchHistory

    /** 充值计入总额数据 */
    val topUpInTotalData: Flow<Boolean> = _recordSettings.map { it.topUpInTotal }

    suspend fun updateCurrentBookId(bookId: Long) {
        _recordSettings.update { it.copy(currentBookId = bookId) }
    }

    suspend fun updateLastAssetId(lastAssetId: Long) {
        _recordSettings.update { it.copy(lastAssetId = lastAssetId) }
    }

    suspend fun updateRefundTypeId(id: Long) {
        _recordSettings.update { it.copy(refundTypeId = id) }
    }

    suspend fun updateReimburseTypeId(id: Long) {
        _recordSettings.update { it.copy(reimburseTypeId = id) }
    }

    suspend fun updateCreditCardPaymentTypeId(id: Long) {
        _recordSettings.update { it.copy(creditCardPaymentTypeId = id) }
    }

    suspend fun updateKeywords(keywords: List<String>) {
        _searchHistory.update { SearchHistoryModel(keywords = keywords) }
    }

    suspend fun updateTopUpInTotal(topUpInTotal: Boolean) {
        _recordSettings.update { it.copy(topUpInTotal = topUpInTotal) }
    }

    suspend fun updateDb9To10DataMigrated(migrated: Boolean) {
        // 简化实现，测试中不需要
    }

    suspend fun needRelated(typeId: Long): Boolean {
        val settings = _recordSettings.value
        return typeId == settings.reimburseTypeId || typeId == settings.refundTypeId
    }
}
