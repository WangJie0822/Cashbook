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
import cn.wj.android.cashbook.core.model.enums.TypeLevelEnum
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class RecordFilterTest {

    @Test
    fun emptySelection_isUnrestricted() {
        assertThat(CategoryFilterSelection().isUnrestricted).isTrue()
        assertThat(CategoryFilterSelection().matches(12L, 10L)).isTrue()
    }

    @Test
    fun wholeGroup_keepsFutureChildAndExclusions() {
        val selection = CategoryFilterSelection(wholeGroupIds = setOf(10L), excludedTypeIds = setOf(11L))
        assertThat(selection.matches(typeId = 12L, parentId = 10L)).isTrue()
        assertThat(selection.matches(typeId = 11L, parentId = 10L)).isFalse()
        assertThat(selection.matches(typeId = 13L, parentId = 20L)).isFalse()
        assertThat(selection.matches(typeId = 10L, parentId = -1L)).isTrue()
    }

    @Test
    fun exactParent_doesNotSelectItsChildren() {
        val selection = CategoryFilterSelection(exactTypeIds = setOf(10L))
        assertThat(selection.matches(10L, -1L)).isTrue()
        assertThat(selection.matches(11L, 10L)).isFalse()
    }

    @Test
    fun exclusionsWithoutPositiveSelection_doNotBecomeUnrestricted() {
        val selection = CategoryFilterSelection(excludedTypeIds = setOf(11L))
        assertThat(selection.isUnrestricted).isFalse()
        assertThat(selection.matches(12L, 10L)).isFalse()
    }

    @Test
    fun selectingGroup_replacesCurrentExactChildrenAndClearsTheirExclusions() {
        val before = CategoryFilterSelection(exactTypeIds = setOf(11L, 21L), excludedTypeIds = setOf(12L))
        val selected = selectCategoryGroup(before, 10L, true, catalog)
        assertThat(selected.wholeGroupIds).containsExactly(10L)
        assertThat(selected.exactTypeIds).containsExactly(21L)
        assertThat(selected.excludedTypeIds).isEmpty()
        assertThat(selected.matches(13L, 10L)).isTrue()
    }

    @Test
    fun deselectingChild_preservesDynamicGroup() {
        val selected = selectCategoryItem(CategoryFilterSelection(wholeGroupIds = setOf(10L)), catalog[1], false)
        assertThat(selected.wholeGroupIds).containsExactly(10L)
        assertThat(selected.excludedTypeIds).containsExactly(11L)
        assertThat(selected.matches(13L, 10L)).isTrue()
    }

    @Test
    fun reselectingExcludedChild_removesException() {
        val selected = selectCategoryItem(
            CategoryFilterSelection(wholeGroupIds = setOf(10L), excludedTypeIds = setOf(11L)),
            catalog[1],
            true,
        )
        assertThat(selected.excludedTypeIds).isEmpty()
        assertThat(selected.wholeGroupIds).containsExactly(10L)
        assertThat(selected.exactTypeIds).isEmpty()
    }

    @Test
    fun deselectingParentItself_doesNotDeselectWholeGroup() {
        val selected = selectCategoryItem(CategoryFilterSelection(wholeGroupIds = setOf(10L)), catalog[0], false)
        assertThat(selected.matches(10L, -1L)).isFalse()
        assertThat(selected.matches(11L, 10L)).isTrue()
    }

    @Test
    fun cancellingGroup_keepsOtherGroupsAndRemovesItsCurrentMembers() {
        val selected = selectCategoryGroup(
            CategoryFilterSelection(wholeGroupIds = setOf(10L, 20L), excludedTypeIds = setOf(11L, 21L)),
            10L,
            false,
            catalog,
        )
        assertThat(selected.wholeGroupIds).containsExactly(20L)
        assertThat(selected.excludedTypeIds).containsExactly(21L)
    }

    @Test
    fun cancellingLastGroup_clearsDanglingExceptions() {
        val selected = selectCategoryGroup(
            CategoryFilterSelection(wholeGroupIds = setOf(10L), excludedTypeIds = setOf(999L)),
            10L,
            false,
            catalog,
        )
        assertThat(selected.isUnrestricted).isTrue()
    }

    @Test
    fun changingTypes_removesOnlyKnownIncompatibleSelections() {
        val changed = changeFilterTypes(
            RecordFilter(categories = CategoryFilterSelection(exactTypeIds = setOf(11L, 21L, 999L), wholeGroupIds = setOf(20L))),
            setOf(RecordTypeCategoryEnum.EXPENDITURE),
            catalog,
        )
        assertThat(changed.filter.categories.exactTypeIds).containsExactly(11L, 999L)
        assertThat(changed.filter.categories.wholeGroupIds).isEmpty()
        assertThat(changed.removedTypeIds).containsExactly(20L, 21L)
        assertThat(changed.filter.typeCategories).containsExactly(RecordTypeCategoryEnum.EXPENDITURE)
    }

    @Test
    fun allTypes_keepsCompatibleAndUnavailableSelections() {
        val filter = RecordFilter(categories = CategoryFilterSelection(exactTypeIds = setOf(11L, 999L), wholeGroupIds = setOf(20L)))
        val changed = changeFilterTypes(filter, emptySet(), catalog)
        assertThat(changed.filter.categories).isEqualTo(filter.categories)
        assertThat(changed.removedTypeIds).isEmpty()
    }

    @Test
    fun removingLastExactSelection_isExplicitlyUnrestricted() {
        val selection = selectCategoryItem(CategoryFilterSelection(exactTypeIds = setOf(11L)), catalog[1], false)
        assertThat(selection.isUnrestricted).isTrue()
    }

    @Test
    fun queryRequest_preservesBookAndClosedDateSelection() {
        val filter = RecordFilter(
            dateSelection = DateSelectionEntity.DateRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)),
            categories = CategoryFilterSelection(wholeGroupIds = setOf(10L), excludedTypeIds = setOf(11L)),
            tagIds = setOf(41L, 42L),
        )
        val request = filter.toRequest(bookId = 2L, monthStartDay = 1)
        assertThat(request.bookId).isEqualTo(2L)
        assertThat(request.categories).isEqualTo(filter.categories)
        assertThat(request.tagIds).containsExactly(41L, 42L)
        assertThat(Instant.ofEpochMilli(request.fromInclusive).atZone(ZoneId.systemDefault()).toLocalDate())
            .isEqualTo(LocalDate.of(2026, 10, 1))
        assertThat(Instant.ofEpochMilli(request.toExclusive).atZone(ZoneId.systemDefault()).toLocalDate())
            .isEqualTo(LocalDate.of(2026, 11, 1))
    }

    private val catalog = listOf(
        type(10L),
        type(11L, 10L),
        type(12L, 10L),
        type(20L, category = RecordTypeCategoryEnum.INCOME),
        type(21L, 20L, RecordTypeCategoryEnum.INCOME),
    )

    private fun type(id: Long, parentId: Long = -1L, category: RecordTypeCategoryEnum = RecordTypeCategoryEnum.EXPENDITURE) =
        RecordTypeModel(
            id = id,
            parentId = parentId,
            name = "分类$id",
            iconName = "",
            typeLevel = if (parentId == -1L) TypeLevelEnum.FIRST else TypeLevelEnum.SECOND,
            typeCategory = category,
            protected = false,
            sort = 0,
            needRelated = false,
        )
}
