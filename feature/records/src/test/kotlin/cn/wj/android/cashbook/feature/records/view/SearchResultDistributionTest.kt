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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.RecordCategoryNet
import cn.wj.android.cashbook.core.model.model.RecordFilterSummary
import cn.wj.android.cashbook.core.model.model.RecordSearchStatistics
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltTestApplication
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
class SearchResultDistributionTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val mixed = RecordSearchStatistics(
        RecordFilterSummary(3L, 400L, -200L),
        listOf(
            RecordCategoryNet(1L, RecordTypeCategoryEnum.EXPENDITURE, "餐饮", 100L),
            RecordCategoryNet(2L, RecordTypeCategoryEnum.INCOME, "同名", 400L),
            RecordCategoryNet(3L, RecordTypeCategoryEnum.TRANSFER, "同名", -300L),
        ),
    )

    private fun show(statistics: RecordSearchStatistics) {
        compose.setContent {
            CashbookTheme {
                val state = rememberSearchDistributionState("test", statistics.categories)
                LazyColumn(Modifier.fillMaxSize().testTag("distribution_host")) { searchResultDistribution(statistics, state) }
            }
        }
    }

    @Test fun signedNetAndCombinedTypes_selectAndCancelWithoutChangingAmounts() {
        show(mixed)
        compose.onNodeWithText("-¥3.00").assertIsDisplayed()
        compose.onNodeWithText("37.50%").assertIsDisplayed()
        compose.onNodeWithTag("distribution_center").assertTextContains("¥8.00", substring = true)
        compose.onNodeWithTag("distribution_group_2_3").performClick().assertIsSelected()
        compose.onNodeWithTag("distribution_center").assertTextContains("转账 · 同名", substring = true).assertTextContains("-¥3.00", substring = true)
        compose.onNodeWithTag("distribution_group_2_3").performClick().assertIsNotSelected()
        compose.onNodeWithTag("distribution_center").assertTextContains("占比基数", substring = true)
    }

    @Test fun actualSectorTapHighlightsTheCorrespondingLegend() {
        show(mixed)
        val chart = compose.onNodeWithTag("distribution_chart")
        val width = chart.fetchSemanticsNode().boundsInRoot.width
        chart.performTouchInput { click(Offset(width / 2f, 20f)) }
        compose.onNodeWithTag("distribution_group_0_1").assertIsSelected()
        compose.onNodeWithTag("distribution_center").assertTextContains("支出 · 餐饮", substring = true)
    }

    @Test fun verySmallNonzeroCategoryStillBelongsToTheChartAndCanBeSelected() {
        show(
            RecordSearchStatistics(
                RecordFilterSummary(2, 0, 1000001),
                listOf(
                    RecordCategoryNet(1, RecordTypeCategoryEnum.EXPENDITURE, "大额", 1000000),
                    RecordCategoryNet(2, RecordTypeCategoryEnum.EXPENDITURE, "小额", 1),
                ),
            ),
        )
        compose.onNodeWithText("<0.01%").assertIsDisplayed()
        compose.onNodeWithTag("distribution_group_0_2").performClick()
        val description = compose.onNodeWithTag("distribution_chart").fetchSemanticsNode().config[SemanticsProperties.StateDescription]
        assertThat(description).contains("2 个非零分类")
        assertThat(description).contains("已选中支出 · 小额")
        compose.onNodeWithTag("distribution_center").assertTextContains("¥0.01", substring = true)
    }

    @Test fun expandedLegendsAreReachableThroughTheMainLazyList() {
        val categories = (1L..60L).map { RecordCategoryNet(it, RecordTypeCategoryEnum.EXPENDITURE, "分类$it", (61L - it) * 100L) }
        show(RecordSearchStatistics(RecordFilterSummary(60, 0, 183000), categories))
        compose.onNodeWithTag("distribution_group_0_60").assertDoesNotExist()
        compose.onNodeWithTag("distribution_expand").performClick()
        compose.onNodeWithTag("distribution_host").performScrollToNode(hasTestTag("distribution_group_0_60"))
        compose.onNodeWithTag("distribution_group_0_60").assertIsDisplayed().performClick().assertIsSelected()
        compose.onNodeWithTag("distribution_host").performScrollToNode(hasTestTag("distribution_expand_bottom"))
        compose.onNodeWithTag("distribution_expand_bottom").performClick()
        compose.onNodeWithTag("distribution_group_0_60").assertDoesNotExist()
        compose.onNodeWithTag("distribution_center").assertTextContains("分类60", substring = true)
    }

    @Test fun zeroContributionKeepsItsLegendButDoesNotDrawAPie() {
        show(RecordSearchStatistics(RecordFilterSummary(2, 0, 0), listOf(RecordCategoryNet(3, RecordTypeCategoryEnum.TRANSFER, "免费转账", 0))))
        compose.onNodeWithText("暂无可绘制的净收支").assertIsDisplayed()
        compose.onNodeWithTag("distribution_chart").assertDoesNotExist()
        compose.onNodeWithTag("distribution_group_2_3").assertIsDisplayed()
        compose.onNodeWithText("0%").assertIsDisplayed()
    }

    @Test fun noMatchesIsDistinctFromMatchedRecordsWithZeroContribution() {
        show(RecordSearchStatistics(RecordFilterSummary(0, 0, 0), emptyList()))
        compose.onNodeWithText("暂无匹配账单").assertIsDisplayed()
        compose.onNodeWithText("暂无可绘制的净收支").assertDoesNotExist()
    }

    @Test fun negativeOnlyStillDrawsACompletePieWithSignedMoney() {
        show(RecordSearchStatistics(RecordFilterSummary(1, 0, -300), listOf(RecordCategoryNet(3, RecordTypeCategoryEnum.TRANSFER, "优惠转账", -300))))
        compose.onNodeWithTag("distribution_chart").assertIsDisplayed()
        compose.onNodeWithText("100.00%").assertIsDisplayed()
        compose.onNodeWithTag("distribution_group_2_3").performClick()
        compose.onNodeWithTag("distribution_center").assertTextContains("-¥3.00", substring = true)
    }

    @Test fun foldingAndRestoringRetainTheSelection() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            CashbookTheme {
                val state = rememberSearchDistributionState("test", mixed.categories)
                LazyColumn { searchResultDistribution(mixed, state) }
            }
        }
        compose.onNodeWithTag("distribution_group_2_3").performClick()
        compose.onNodeWithTag("distribution_toggle").performClick()
        compose.onNodeWithTag("distribution_chart").assertDoesNotExist()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("distribution_chart").assertDoesNotExist()
        compose.onNodeWithTag("distribution_toggle").performClick()
        compose.onNodeWithTag("distribution_group_2_3").assertIsSelected()
    }

    @Test fun removedGroupDoesNotRegainAStaleSelectionIfItsIdReturns() {
        val data = mutableStateOf(mixed)
        compose.setContent {
            CashbookTheme {
                val state = rememberSearchDistributionState("test", data.value.categories)
                LazyColumn { searchResultDistribution(data.value, state) }
            }
        }
        compose.onNodeWithTag("distribution_group_2_3").performClick()
        compose.runOnIdle { data.value = RecordSearchStatistics(RecordFilterSummary(2, 400, 100), mixed.categories.take(2)) }
        compose.onNodeWithTag("distribution_center").assertTextContains("占比基数", substring = true)
        compose.runOnIdle { data.value = mixed }
        compose.onNodeWithTag("distribution_group_2_3").assertIsNotSelected()
    }
}
