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
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.feature.records.model.SearchFilterEvent
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "zh-rCN-w411dp-h891dp-480dpi")
@LooperMode(LooperMode.Mode.PAUSED)
class SearchCategoryPickerTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var fixture: SearchFilterTestFixture

    @After fun close() { if (::fixture.isInitialized) compose.runOnIdle { fixture.close() } }

    private fun show(types: Set<RecordTypeCategoryEnum> = emptySet()) {
        compose.runOnUiThread {
            fixture = SearchFilterTestFixture()
            fixture.event(SearchFilterEvent.OpenDrawer)
            fixture.event(SearchFilterEvent.UpdateTypes(types))
            fixture.event(SearchFilterEvent.OpenCategories)
        }
        compose.setContent { CashbookTheme { val state by fixture.vm.filterState.collectAsState(); SearchCategoryPicker(state, fixture::event) } }
    }

    @Test fun wholeGroupMinusOne_expressesDynamicGroupInsteadOfFlattenedIds() {
        show()
        compose.onNodeWithTag("category_group_10").performClick()
        compose.onNodeWithTag("category_item_11").assertIsOn().performClick().assertIsOff()
        compose.onNodeWithText("餐饮 · 整组，排除 1 项").assertIsDisplayed()
        compose.runOnIdle {
            val selection = fixture.vm.filterState.value.categoryDraft!!
            assertThat(selection.wholeGroupIds).containsExactly(10L)
            assertThat(selection.excludedTypeIds).containsExactly(11L)
            assertThat(selection.exactTypeIds).isEmpty()
        }
    }

    @Test fun parentDirectRecord_isAnIndividualOption() {
        show()
        compose.onNodeWithTag("category_item_10").performClick().assertIsOn()
        compose.onNodeWithTag("category_item_11").assertIsOff()
        compose.runOnIdle { assertThat(fixture.vm.filterState.value.categoryDraft!!.exactTypeIds).containsExactly(10L) }
    }

    @Test fun browserTypeAndSearch_keepSelectionThenRemoveFromSummary() {
        show()
        compose.onNodeWithTag("category_item_11").performClick()
        compose.onNodeWithTag("category_tab_INCOME").performClick()
        compose.onNodeWithTag("category_item_20").performClick()
        compose.onNodeWithTag("category_search").performTextInput("工资")
        compose.onNodeWithTag("category_item_20").assertIsOn()
        compose.onNodeWithTag("category_remove_exact_11").performClick()
        compose.runOnIdle { assertThat(fixture.vm.filterState.value.categoryDraft!!.exactTypeIds).containsExactly(20L) }
    }

    @Test fun allowedTypes_limitTabsWithoutLosingOtherCompatibleChoices() {
        show(setOf(RecordTypeCategoryEnum.EXPENDITURE))
        compose.onNodeWithTag("category_tab_INCOME").assertDoesNotExist()
        compose.onNodeWithTag("category_item_11").performClick()
        compose.onNodeWithText("完成").performClick()
        compose.runOnIdle { assertThat(fixture.vm.filterState.value.drawerDraft!!.categories.exactTypeIds).containsExactly(11L) }
    }

    @Test fun deletedSelectedCategory_remainsVisibleAndCanBeRemovedExplicitly() {
        show()
        compose.onNodeWithTag("category_item_11").performClick()
        compose.runOnIdle { runBlocking { fixture.types.deleteById(11L) } }
        compose.onNodeWithText("不可用（11）").assertIsDisplayed()
        compose.runOnIdle { assertThat(fixture.vm.filterState.value.categoryDraft!!.exactTypeIds).containsExactly(11L) }
        compose.onNodeWithTag("category_remove_exact_11").performClick()
        compose.runOnIdle { assertThat(fixture.vm.filterState.value.categoryDraft!!.isUnrestricted).isTrue() }
    }

    @Test
    @Config(qualifiers = "zh-rCN-w640dp-h360dp-480dpi")
    fun shortLandscape_keepsCategoryTargetsAndDoneVisible() {
        show()
        compose.onNodeWithText("完成").assertIsDisplayed()
        val height = compose.onNodeWithTag("category_item_11").fetchSemanticsNode().boundsInRoot.height
        assertThat(height).isAtLeast(with(compose.density) { 48.dp.toPx() })
        compose.onNodeWithTag("category_item_11").performClick().assertIsOn()
    }
}
