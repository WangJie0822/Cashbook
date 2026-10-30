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

package cn.wj.android.cashbook.core.model.model

import cn.wj.android.cashbook.core.model.entity.DateSelectionEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum

/** 精确分类、动态父组与排除项；不能展平为选择当时的子分类快照。 */
data class CategoryFilterSelection(
    val exactTypeIds: Set<Long> = emptySet(),
    val wholeGroupIds: Set<Long> = emptySet(),
    val excludedTypeIds: Set<Long> = emptySet(),
) {
    val isUnrestricted: Boolean
        get() = exactTypeIds.isEmpty() && wholeGroupIds.isEmpty() && excludedTypeIds.isEmpty()

    fun matches(typeId: Long, parentId: Long): Boolean = isUnrestricted ||
        (typeId !in excludedTypeIds && (typeId in exactTypeIds || typeId in wholeGroupIds || parentId in wholeGroupIds))
}

data class RecordFilter(
    val dateSelection: DateSelectionEntity = DateSelectionEntity.All,
    val typeCategories: Set<RecordTypeCategoryEnum> = emptySet(),
    val categories: CategoryFilterSelection = CategoryFilterSelection(),
    val tagIds: Set<Long> = emptySet(),
)

/** 已冻结日期边界与账本的查询条件，结束时间不包含在结果中。 */
data class RecordFilterRequest(
    val bookId: Long,
    val fromInclusive: Long,
    val toExclusive: Long,
    val typeCategories: Set<RecordTypeCategoryEnum>,
    val categories: CategoryFilterSelection,
    val tagIds: Set<Long>,
)

fun RecordFilter.toRequest(bookId: Long, monthStartDay: Int): RecordFilterRequest {
    val (start, end) = dateSelection.toDateRange(monthStartDay)
    return RecordFilterRequest(
        bookId,
        start,
        end,
        typeCategories.toSet(),
        categories.copy(
            exactTypeIds = categories.exactTypeIds.toSet(),
            wholeGroupIds = categories.wholeGroupIds.toSet(),
            excludedTypeIds = categories.excludedTypeIds.toSet(),
        ),
        tagIds.toSet(),
    )
}

data class RecordFilterTypeUpdate(val filter: RecordFilter, val removedTypeIds: Set<Long>)

fun selectCategoryGroup(
    selection: CategoryFilterSelection,
    parentId: Long,
    selected: Boolean,
    catalog: List<RecordTypeModel>,
): CategoryFilterSelection {
    val members = catalog.filter { it.id == parentId || it.parentId == parentId }.map { it.id }.toSet() + parentId
    val groups = if (selected) selection.wholeGroupIds + parentId else selection.wholeGroupIds - parentId
    return selection.copy(
        exactTypeIds = selection.exactTypeIds - members,
        wholeGroupIds = groups,
        excludedTypeIds = if (groups.isEmpty()) emptySet() else selection.excludedTypeIds - members,
    )
}

fun selectCategoryItem(
    selection: CategoryFilterSelection,
    type: RecordTypeModel,
    selected: Boolean,
): CategoryFilterSelection {
    val coveredByGroup = type.id in selection.wholeGroupIds || type.parentId in selection.wholeGroupIds
    return selection.copy(
        exactTypeIds = if (selected && !coveredByGroup) selection.exactTypeIds + type.id else selection.exactTypeIds - type.id,
        excludedTypeIds = if (!selected && coveredByGroup) selection.excludedTypeIds + type.id else selection.excludedTypeIds - type.id,
    )
}

fun changeFilterTypes(
    filter: RecordFilter,
    allowedTypes: Set<RecordTypeCategoryEnum>,
    catalog: List<RecordTypeModel>,
): RecordFilterTypeUpdate {
    val incompatible = if (allowedTypes.isEmpty()) {
        emptySet()
    } else {
        catalog.filter { it.typeCategory !in allowedTypes }.map { it.id }.toSet()
    }
    val selectedIds = filter.categories.exactTypeIds + filter.categories.wholeGroupIds
    return RecordFilterTypeUpdate(
        filter = filter.copy(
            typeCategories = allowedTypes.toSet(),
            categories = filter.categories.copy(
                exactTypeIds = filter.categories.exactTypeIds - incompatible,
                wholeGroupIds = filter.categories.wholeGroupIds - incompatible,
                excludedTypeIds = filter.categories.excludedTypeIds - incompatible,
            ),
        ),
        removedTypeIds = selectedIds.intersect(incompatible),
    )
}
