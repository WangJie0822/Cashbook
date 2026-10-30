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

package cn.wj.android.cashbook.feature.records.model

import androidx.paging.PagingData
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.CategoryFilterSelection
import cn.wj.android.cashbook.core.model.model.RecordFilter
import cn.wj.android.cashbook.core.model.model.RecordFilterRequest
import cn.wj.android.cashbook.core.model.model.RecordFilterSummary
import cn.wj.android.cashbook.core.model.model.RecordTypeModel
import cn.wj.android.cashbook.core.model.model.TagModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

enum class SearchMode { Initial, Keyword, Filter }

data class SearchQueryContext(
    val criteriaRevision: Long = 0L,
    val bookId: Long = -1L,
    val mode: SearchMode = SearchMode.Initial,
    val request: RecordFilterRequest? = null,
    val keyword: String = "",
)

/** 每套条件持有独立分页缓存，切换时不重放上一条件的页面。 */
data class SearchRecordsSource(
    val context: SearchQueryContext = SearchQueryContext(),
    val data: Flow<PagingData<RecordViewsEntity>> = flowOf(PagingData.empty()),
)

enum class SearchFilterMessage {
    INVALID_DATE, TOO_MANY_CONDITIONS, QUERY_FAILED, METADATA_FAILED, RESTORE_FAILED,
    UNAVAILABLE_SELECTIONS, INCOMPATIBLE_CATEGORIES_REMOVED,
}

data class SearchFilterState(
    val mode: SearchMode = SearchMode.Initial,
    val queryDraft: String = "",
    val submittedKeyword: String = "",
    val appliedFilter: RecordFilter? = null,
    val drawerDraft: RecordFilter? = null,
    val categoryDraft: CategoryFilterSelection? = null,
    val catalog: List<RecordTypeModel> = emptyList(),
    val tags: List<TagModel> = emptyList(),
    val categoryBrowseType: RecordTypeCategoryEnum = RecordTypeCategoryEnum.EXPENDITURE,
    val categoryBrowseParentId: Long? = null,
    val categorySearchQuery: String = "",
    val removedTypeIds: Set<Long> = emptySet(),
    val message: SearchFilterMessage? = null,
    val bookId: Long? = null,
    val bookName: String = "",
    val monthStartDay: Int = 1,
    val settingsLoaded: Boolean = false,
    val catalogLoaded: Boolean = false,
    val tagsLoaded: Boolean = false,
    val queryContext: SearchQueryContext = SearchQueryContext(),
    val recordQueryFailure: SearchQueryContext? = null,
) {
    val metadataReady: Boolean get() = bookId != null && settingsLoaded && catalogLoaded && tagsLoaded
}

sealed interface SearchFilterSummaryState {
    data object Hidden : SearchFilterSummaryState
    data class Loading(val context: SearchQueryContext) : SearchFilterSummaryState
    data class Ready(val context: SearchQueryContext, val summary: RecordFilterSummary) : SearchFilterSummaryState
    data class Failed(val context: SearchQueryContext, val message: String) : SearchFilterSummaryState
}
