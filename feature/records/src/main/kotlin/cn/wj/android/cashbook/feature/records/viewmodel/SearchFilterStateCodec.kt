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

package cn.wj.android.cashbook.feature.records.viewmodel

import androidx.lifecycle.SavedStateHandle
import cn.wj.android.cashbook.core.model.entity.DateSelectionEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.CategoryFilterSelection
import cn.wj.android.cashbook.core.model.model.RecordFilter
import cn.wj.android.cashbook.feature.records.model.SearchFilterMessage
import cn.wj.android.cashbook.feature.records.model.SearchFilterState
import cn.wj.android.cashbook.feature.records.model.SearchMode

internal object SearchFilterStateCodec {
    const val KEY = "search_filter_v1"
    fun write(handle: SavedStateHandle, state: SearchFilterState) {
        val filter = state.appliedFilter ?: RecordFilter()
        handle[KEY] = arrayListOf(
            "1", state.queryDraft, state.submittedKeyword, (state.appliedFilter != null).toString(),
            filter.dateSelection.getDisplayText(), filter.typeCategories.joinToString(",") { it.code.toString() },
            filter.categories.exactTypeIds.joinToString(","), filter.categories.wholeGroupIds.joinToString(","),
            filter.categories.excludedTypeIds.joinToString(","), filter.tagIds.joinToString(","),
        )
    }

    fun read(handle: SavedStateHandle): SearchFilterState {
        if (!handle.contains(KEY)) return SearchFilterState()
        return runCatching {
            val fields = requireNotNull(handle.get<ArrayList<String>>(KEY))
            require(fields.size == 10 && fields[0] == "1")
            val applied = fields[3].toBooleanStrict()
            fun ids(index: Int): Set<Long> = if (fields[index].isEmpty()) emptySet() else fields[index].split(',').map { it.toLong() }.toSet()
            val date = requireNotNull(DateSelectionEntity.fromDisplayTextOrNull(fields[4]))
            val types = if (fields[5].isEmpty()) emptySet() else fields[5].split(',').map { RecordTypeCategoryEnum.codeOf(it.toInt()) }.toSet()
            val filter = RecordFilter(date, types, CategoryFilterSelection(ids(6), ids(7), ids(8)), ids(9))
            require(filter.dateSelection.toDateRange().let { it.first < it.second })
            SearchFilterState(
                mode = if (fields[2].isNotBlank()) SearchMode.Keyword else if (applied) SearchMode.Filter else SearchMode.Initial,
                queryDraft = fields[1],
                submittedKeyword = fields[2],
                appliedFilter = filter.takeIf { applied },
            )
        }.getOrElse { SearchFilterState(message = SearchFilterMessage.RESTORE_FAILED) }
    }
}
