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

package cn.wj.android.cashbook.feature.records.view

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.model.entity.DateSelectionEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.enums.TypeLevelEnum
import cn.wj.android.cashbook.core.model.model.CategoryFilterSelection
import cn.wj.android.cashbook.core.model.model.RecordFilter
import cn.wj.android.cashbook.core.testing.data.createRecordTypeModel
import cn.wj.android.cashbook.core.testing.data.createTagModel
import cn.wj.android.cashbook.core.testing.util.captureMultiDevice
import cn.wj.android.cashbook.core.testing.util.captureMultiTheme
import cn.wj.android.cashbook.feature.records.model.SearchFilterMessage
import cn.wj.android.cashbook.feature.records.model.SearchFilterState
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "zh-rCN-w411dp-h891dp-480dpi")
@LooperMode(LooperMode.Mode.PAUSED)
class SearchFilterDrawerScreenshotTests {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val parent = createRecordTypeModel(id = 10L, name = "餐饮与日常生活开销")
    private val catalog = listOf(parent) + (1L..901L).map { createRecordTypeModel(id = 1000L + it, parentId = 10L, name = if (it == 1L) "朋友聚餐与家庭特别餐饮开支" else "分类 $it", typeLevel = TypeLevelEnum.SECOND) }
    private val selection = CategoryFilterSelection(wholeGroupIds = setOf(10L), excludedTypeIds = setOf(1001L))
    private val state = SearchFilterState(
        bookId = 1L,
        bookName = "日常账本",
        settingsLoaded = true,
        catalogLoaded = true,
        tagsLoaded = true,
        catalog = catalog,
        tags = listOf(createTagModel(id = 41L, name = "旅行"), createTagModel(id = 42L, name = "旧项目", invisible = true)),
        drawerDraft = RecordFilter(
            dateSelection = DateSelectionEntity.DateRange(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 3, 31)),
            typeCategories = setOf(RecordTypeCategoryEnum.EXPENDITURE),
            categories = selection,
            tagIds = setOf(42L, 999L),
        ),
    )

    @Test fun emptyDrawer_multipleThemes() { compose.captureMultiTheme("SearchFilterDrawer_empty") { SearchFilterDrawer(state.copy(drawerDraft = RecordFilter(), catalog = emptyList(), tags = emptyList()), {}) } }

    @Test fun selectedDrawer_multipleThemes() { compose.captureMultiTheme("SearchFilterDrawer_selected") { SearchFilterDrawer(state.copy(removedTypeIds = setOf(20L), message = SearchFilterMessage.INCOMPATIBLE_CATEGORIES_REMOVED), {}) } }

    @Test fun categoryPicker_manyCategories_multipleThemes() { compose.captureMultiTheme("SearchCategoryPicker_many") { SearchCategoryPicker(state.copy(categoryDraft = selection), {}) } }

    @Test fun categoryPicker_manyCategories_multipleDevices() { compose.captureMultiDevice("SearchCategoryPicker_many") { CashbookTheme { SearchCategoryPicker(state.copy(categoryDraft = selection), {}) } } }

    @Test fun selectedDrawer_multipleDevices() { compose.captureMultiDevice("SearchFilterDrawer_selected") { CashbookTheme { SearchFilterDrawer(state, {}) } } }

    private val tagState = state.copy(
        tagDraft = setOf(42L, 999L),
        tags = state.tags + (1L..100L).map { createTagModel(id = 1000L + it, name = "旅行与朋友聚会的长标签名称 $it") },
    )

    @Test fun tagPicker_selectedHiddenAndMissing_multipleThemes() { compose.captureMultiTheme("SearchTagPicker_selected") { SearchTagPicker(tagState, {}) } }

    @Test fun tagPicker_selectedHiddenAndMissing_multipleDevices() { compose.captureMultiDevice("SearchTagPicker_selected") { CashbookTheme { SearchTagPicker(tagState, {}) } } }

    @Test fun tagPicker_hiddenCandidates_multipleThemes() { compose.captureMultiTheme("SearchTagPicker_hidden") { SearchTagPicker(tagState.copy(showHiddenTags = true), {}) } }

    @Test fun rightSidebar_multipleDevices() { compose.captureMultiDevice("SearchFilterSidebar_right") { CashbookTheme { SearchFilterSidebar(state, {}) { Text("搜索结果") } } } }
}
