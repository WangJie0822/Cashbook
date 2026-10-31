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
import cn.wj.android.cashbook.core.testing.data.createTagModel
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

/** 控件经真实 VM 事件修改子草稿，防止候选显示条件误改业务筛选。 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "zh-rCN-w411dp-h891dp-480dpi")
@LooperMode(LooperMode.Mode.PAUSED)
class SearchTagPickerTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var fixture: SearchFilterTestFixture

    @After fun close() { if (::fixture.isInitialized) compose.runOnIdle { fixture.close() } }

    private fun show(selected: Set<Long> = emptySet(), keyword: String? = null) {
        compose.runOnUiThread {
            fixture = SearchFilterTestFixture()
            if (keyword != null) fixture.vm.onKeywordChange(keyword)
            fixture.event(SearchFilterEvent.OpenDrawer)
            selected.forEach { fixture.event(SearchFilterEvent.ToggleTag(it)) }
            fixture.event(SearchFilterEvent.OpenTags)
        }
        compose.setContent {
            CashbookTheme {
                val state by fixture.vm.filterState.collectAsState()
                SearchTagPicker(state, fixture::event)
            }
        }
    }

    @Test fun hiddenCandidates_defaultOffAndHidingAgainKeepsSelectedTag() {
        show()
        compose.onNodeWithTag("tag_show_hidden").assertIsOff()
        compose.onNodeWithTag("filter_tag_41").assertIsDisplayed().assertIsOff()
        compose.onNodeWithTag("filter_tag_42").assertDoesNotExist()
        compose.onNodeWithTag("tag_show_hidden").performClick().assertIsOn()
        compose.onNodeWithTag("filter_tag_42").performClick().assertIsOn()
        compose.onNodeWithTag("tag_show_hidden").performClick().assertIsOff()
        compose.onNodeWithTag("filter_tag_42").assertDoesNotExist()
        compose.onNodeWithTag("tag_selected_42").assertIsDisplayed()
        compose.onNodeWithText("旧项目（已隐藏）").assertIsDisplayed()
        compose.onNodeWithText("不可用（42）").assertDoesNotExist()
        compose.runOnIdle {
            assertThat(fixture.vm.filterState.value.tagDraft).containsExactly(42L)
            assertThat(fixture.vm.filterState.value.drawerDraft!!.tagIds).isEmpty()
        }
    }

    @Test fun nameSearch_isCaseInsensitiveAndDoesNotChangeKeywordOrBusinessRequests() {
        show(keyword = "午餐")
        compose.runOnIdle { fixture.tags.addTag(createTagModel(id = 43, name = "Travel")) }
        compose.waitUntil { fixture.filters.keywordRequests.isNotEmpty() }
        lateinit var beforeRequests: List<Int>
        var beforeRevision = 0L
        compose.runOnIdle {
            beforeRequests = businessRequests()
            beforeRevision = fixture.vm.filterState.value.queryContext.criteriaRevision
        }
        compose.onNodeWithTag("tag_search").performTextInput("TrAv")
        compose.onNodeWithTag("filter_tag_43").assertIsDisplayed()
        compose.onNodeWithTag("filter_tag_41").assertDoesNotExist()
        compose.runOnIdle {
            val state = fixture.vm.filterState.value
            assertThat(state.tagSearchQuery).isEqualTo("TrAv")
            assertThat(state.queryDraft).isEqualTo("午餐")
            assertThat(state.submittedKeyword).isEqualTo("午餐")
            assertThat(state.queryContext.criteriaRevision).isEqualTo(beforeRevision)
            assertThat(businessRequests()).isEqualTo(beforeRequests)
            assertThat(state.tagDraft).isEmpty()
        }
    }

    @Test fun cancelDiscardsChildAndConfirmOnlyUpdatesParentDraft() {
        show()
        compose.onNodeWithTag("filter_tag_41").performClick()
        compose.onNodeWithTag("tag_back").performClick()
        compose.runOnIdle {
            assertThat(fixture.vm.filterState.value.tagDraft).isNull()
            assertThat(fixture.vm.filterState.value.drawerDraft!!.tagIds).isEmpty()
            assertThat(fixture.vm.filterState.value.appliedFilter).isNull()
            fixture.event(SearchFilterEvent.OpenTags)
        }
        compose.onNodeWithTag("filter_tag_41").performClick()
        compose.onNodeWithTag("tag_done").performClick()
        compose.runOnIdle {
            assertThat(fixture.vm.filterState.value.tagDraft).isNull()
            assertThat(fixture.vm.filterState.value.drawerDraft!!.tagIds).containsExactly(41L)
            assertThat(fixture.vm.filterState.value.appliedFilter).isNull()
            assertThat(businessRequests()).containsExactly(0, 0, 0, 0, 0)
            fixture.event(SearchFilterEvent.ApplyDrawer)
            assertThat(fixture.vm.filterState.value.appliedFilter!!.tagIds).containsExactly(41L)
        }
    }

    @Test fun clearOnlyChangesChildAndReopeningResetsCandidateDisplaySettings() {
        show(selected = setOf(41))
        compose.onNodeWithTag("tag_show_hidden").performClick()
        compose.onNodeWithTag("tag_search").performTextInput("旧")
        compose.onNodeWithTag("tag_clear").performClick()
        compose.runOnIdle {
            assertThat(fixture.vm.filterState.value.tagDraft).isEmpty()
            assertThat(fixture.vm.filterState.value.drawerDraft!!.tagIds).containsExactly(41L)
        }
        compose.onNodeWithTag("tag_back").performClick()
        compose.runOnIdle { fixture.event(SearchFilterEvent.OpenTags) }
        compose.onNodeWithTag("tag_show_hidden").assertIsOff()
        compose.onNodeWithTag("filter_tag_41").assertIsOn()
        compose.onNodeWithTag("filter_tag_42").assertDoesNotExist()
        compose.runOnIdle {
            assertThat(fixture.vm.filterState.value.tagSearchQuery).isEmpty()
            assertThat(fixture.vm.filterState.value.tagDraft).containsExactly(41L)
        }
    }

    @Test fun selectedCatalogTagBecomingHiddenThenDeletedStaysVisibleAndRemovable() {
        show()
        compose.onNodeWithTag("filter_tag_41").performClick()
        compose.runOnIdle { runBlocking { fixture.tags.updateTag(createTagModel(id = 41, name = "旅行", invisible = true)) } }
        compose.onNodeWithTag("filter_tag_41").assertDoesNotExist()
        compose.onNodeWithText("旅行（已隐藏）").assertIsDisplayed()
        compose.onNodeWithText("不可用（41）").assertDoesNotExist()
        compose.runOnIdle { runBlocking { fixture.tags.deleteTag(createTagModel(id = 41, name = "旅行", invisible = true)) } }
        compose.onNodeWithTag("tag_selected_41").assertIsDisplayed()
        compose.onNodeWithText("不可用（41）").assertIsDisplayed()
        compose.runOnIdle { assertThat(fixture.vm.filterState.value.tagDraft).containsExactly(41L) }
        compose.onNodeWithTag("tag_remove_41").performClick()
        compose.onNodeWithTag("tag_selected_41").assertDoesNotExist()
        compose.runOnIdle { assertThat(fixture.vm.filterState.value.tagDraft).isEmpty() }
    }

    @Test fun noNameMatches_showsTextEmptyStateWithoutChangingSelectedTags() {
        show(selected = setOf(41))
        compose.onNodeWithTag("tag_search").performTextInput("不存在")
        compose.onNodeWithText("暂无标签").assertIsDisplayed()
        compose.onNodeWithTag("filter_tag_41").assertDoesNotExist()
        compose.onNodeWithTag("tag_selected_41").assertIsDisplayed()
        compose.onNodeWithTag("tag_remove_41").performClick()
        compose.runOnIdle { assertThat(fixture.vm.filterState.value.tagDraft).isEmpty() }
    }

    @Test
    @Config(qualifiers = "zh-rCN-w640dp-h360dp-480dpi")
    fun shortLandscape_keepsCandidateDoneAndSelectedRemoveAccessible() {
        show(selected = setOf(42))
        compose.onNodeWithTag("tag_done").assertIsDisplayed()
        compose.onNodeWithTag("tag_clear").assertIsDisplayed()
        compose.onNodeWithTag("tag_remove_42").assertIsDisplayed()
        compose.onNodeWithTag("filter_tag_41").assertIsDisplayed()
        val height = compose.onNodeWithTag("filter_tag_41").fetchSemanticsNode().boundsInRoot.height
        assertThat(height).isAtLeast(with(compose.density) { 48.dp.toPx() })
        compose.onNodeWithTag("tag_remove_42").performClick()
        compose.onNodeWithTag("filter_tag_41").performClick().assertIsOn()
    }

    private fun businessRequests() = listOf(
        fixture.filters.recordRequests.size,
        fixture.filters.summaryRequests.size,
        fixture.filters.keywordRequests.size,
        fixture.filters.combinedRecordRequests.size,
        fixture.filters.combinedStatisticsRequests.size,
    )
}
