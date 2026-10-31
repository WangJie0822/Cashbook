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

package cn.wj.android.cashbook.core.data.repository

import androidx.paging.PagingData
import cn.wj.android.cashbook.core.model.model.RecordFilterRequest
import cn.wj.android.cashbook.core.model.model.RecordFilterSummary
import cn.wj.android.cashbook.core.model.model.RecordKeywordQuery
import cn.wj.android.cashbook.core.model.model.RecordSearchStatistics
import cn.wj.android.cashbook.core.model.model.RecordViewsModel
import kotlinx.coroutines.flow.Flow

interface RecordFilterRepository {
    fun validate(request: RecordFilterRequest): RecordFilterValidation
    fun validate(request: RecordFilterRequest, hasKeyword: Boolean): RecordFilterValidation
    fun observeRecords(request: RecordFilterRequest): Flow<PagingData<RecordViewsModel>>
    fun observeRecords(request: RecordFilterRequest, keyword: RecordKeywordQuery): Flow<PagingData<RecordViewsModel>>
    fun observeSummary(request: RecordFilterRequest): Flow<RecordFilterSummary>
    fun observeStatistics(request: RecordFilterRequest): Flow<RecordSearchStatistics>
    fun observeStatistics(request: RecordFilterRequest, keyword: RecordKeywordQuery): Flow<RecordSearchStatistics>
    fun observeKeywordStatistics(query: RecordKeywordQuery): Flow<RecordSearchStatistics>
}

sealed interface RecordFilterValidation {
    data object Valid : RecordFilterValidation
    data object InvalidRange : RecordFilterValidation
    data class TooManyParameters(val required: Int, val limit: Int) : RecordFilterValidation
}
