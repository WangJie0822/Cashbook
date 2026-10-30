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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.model.entity.DateSelectionEntity
import cn.wj.android.cashbook.feature.records.model.SearchFilterEvent
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import java.time.LocalDate
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "zh-rCN-w411dp-h891dp-480dpi")
@LooperMode(LooperMode.Mode.PAUSED)
class SearchFilterDrawerTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var fixture: SearchFilterTestFixture

    @After fun close() { if (::fixture.isInitialized) compose.runOnIdle { fixture.close() } }

    private fun show() {
        compose.runOnUiThread { fixture = SearchFilterTestFixture(); fixture.event(SearchFilterEvent.OpenDrawer) }
        compose.setContent { CashbookTheme { val state by fixture.vm.filterState.collectAsState(); SearchFilterDrawer(state, fixture::event) } }
    }

    @Test fun hiddenTag_isMarkedSelectableAndAppliedOnlyOnDone() {
        show()
        compose.onNodeWithTag("filter_tag_42").performScrollTo().performClick().assertIsOn()
        compose.onNodeWithText("旧项目（已隐藏）").assertIsDisplayed()
        compose.runOnIdle { assertThat(fixture.vm.filterState.value.appliedFilter).isNull() }
        compose.onNodeWithText("完成").performClick()
        compose.runOnIdle { assertThat(fixture.vm.filterState.value.appliedFilter!!.tagIds).containsExactly(42L) }
    }

    @Test fun reset_changesDraftAndDismissCancelsWholeDrawer() {
        show()
        compose.onNodeWithTag("filter_tag_41").performScrollTo().performClick()
        compose.onNodeWithText("重置").performClick()
        compose.runOnIdle { assertThat(fixture.vm.filterState.value.drawerDraft!!.tagIds).isEmpty() }
        compose.onNodeWithTag("filter_drawer_close").performClick()
        compose.runOnIdle { assertThat(fixture.events.last()).isEqualTo(SearchFilterEvent.CancelDrawer) }
    }

    @Test fun categoryBack_discardsNestedSelectionButKeepsDrawerOpen() {
        show()
        compose.onNodeWithTag("filter_categories").performScrollTo().performClick()
        compose.onNodeWithTag("category_item_11").performClick()
        compose.onNodeWithTag("category_back").performClick()
        compose.runOnIdle {
            assertThat(fixture.vm.filterState.value.drawerDraft!!.categories.isUnrestricted).isTrue()
            assertThat(fixture.vm.filterState.value.drawerDraft).isNotNull()
            assertThat(fixture.events.last()).isEqualTo(SearchFilterEvent.CancelCategories)
        }
    }

    @Test fun realDatePicker_selectsBothCalendarDaysAndPreservesInitialDatesAcrossTimeZones() {
        val original = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Auckland"))
        try {
            show()
            compose.runOnIdle { fixture.event(SearchFilterEvent.UpdateDate(DateSelectionEntity.DateRange(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 3)))) }
            compose.onNodeWithTag("filter_date_range").performScrollTo().performClick()
            compose.onNodeWithText("2024年1月10日", substring = true).performClick()
            compose.onNodeWithTag("filter_date_done").assertIsNotEnabled()
            compose.onNodeWithText("2024年1月12日", substring = true).performClick()
            compose.onNodeWithTag("filter_date_done").performClick()
            compose.runOnIdle {
                assertThat(fixture.vm.filterState.value.drawerDraft!!.dateSelection).isEqualTo(DateSelectionEntity.DateRange(LocalDate.of(2024, 1, 10), LocalDate.of(2024, 1, 12)))
            }
            compose.onNodeWithTag("filter_date_range").performClick()
            compose.onNodeWithTag("filter_date_done").performClick()
            compose.runOnIdle {
                assertThat(fixture.vm.filterState.value.drawerDraft!!.dateSelection).isEqualTo(DateSelectionEntity.DateRange(LocalDate.of(2024, 1, 10), LocalDate.of(2024, 1, 12)))
            }
        } finally { TimeZone.setDefault(original) }
    }

    @Test fun typeChange_showsRemovedCategoryCount() {
        show()
        compose.onNodeWithTag("filter_categories").performScrollTo().performClick()
        compose.onNodeWithTag("category_tab_INCOME").performClick()
        compose.onNodeWithTag("category_item_20").performClick()
        compose.onNodeWithText("完成").performClick()
        compose.onNodeWithTag("filter_type_EXPENDITURE").performScrollTo().performClick()
        compose.onNodeWithText("已移除 1 项不兼容分类").assertIsDisplayed()
        compose.runOnIdle { assertThat(fixture.vm.filterState.value.drawerDraft!!.categories.isUnrestricted).isTrue() }
    }

    @Test fun invalidRange_disablesApplyAndShowsError() {
        show()
        compose.runOnIdle { fixture.event(SearchFilterEvent.UpdateDate(DateSelectionEntity.DateRange(LocalDate.of(2024, 2, 2), LocalDate.of(2024, 2, 1)))) }
        compose.onNodeWithText("完成").assertIsNotEnabled()
        compose.onNodeWithText("结束日期不能早于开始日期").assertIsDisplayed()
    }
}
