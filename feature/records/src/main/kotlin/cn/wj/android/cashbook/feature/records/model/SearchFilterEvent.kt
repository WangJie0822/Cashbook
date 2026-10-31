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

import cn.wj.android.cashbook.core.model.entity.DateSelectionEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum

enum class CategorySelectionKind { EXACT, GROUP, EXCLUDED }

sealed interface SearchFilterEvent {
    data object OpenDrawer : SearchFilterEvent
    data object CancelDrawer : SearchFilterEvent
    data object Back : SearchFilterEvent
    data object ResetDrawer : SearchFilterEvent
    data object ApplyDrawer : SearchFilterEvent
    data class UpdateDate(val date: DateSelectionEntity) : SearchFilterEvent
    data class UpdateTypes(val types: Set<RecordTypeCategoryEnum>) : SearchFilterEvent
    data class ToggleTag(val id: Long) : SearchFilterEvent
    data object OpenTags : SearchFilterEvent
    data object CancelTags : SearchFilterEvent
    data object ConfirmTags : SearchFilterEvent
    data class ToggleTagDraft(val id: Long) : SearchFilterEvent
    data object ClearTagSelection : SearchFilterEvent
    data class ChangeTagSearch(val keyword: String) : SearchFilterEvent
    data class ShowHiddenTags(val show: Boolean) : SearchFilterEvent
    data object OpenCategories : SearchFilterEvent
    data object CancelCategories : SearchFilterEvent
    data object ConfirmCategories : SearchFilterEvent
    data class ToggleCategoryGroup(val id: Long, val selected: Boolean) : SearchFilterEvent
    data class ToggleCategoryItem(val id: Long, val selected: Boolean) : SearchFilterEvent
    data class ChangeCategoryBrowser(val type: RecordTypeCategoryEnum, val parentId: Long? = null) : SearchFilterEvent
    data class ChangeCategorySearch(val keyword: String) : SearchFilterEvent
    data class RemoveCategory(val id: Long, val kind: CategorySelectionKind) : SearchFilterEvent
    data class UpdateQueryDraft(val keyword: String) : SearchFilterEvent
    data object ClearSubmittedKeyword : SearchFilterEvent
    data object Retry : SearchFilterEvent
}
