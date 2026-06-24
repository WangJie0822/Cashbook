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

package cn.wj.android.cashbook.feature.budget

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.model.entity.BUDGET_TYPE_ID_TOTAL
import cn.wj.android.cashbook.core.model.entity.BudgetProgressEntity
import cn.wj.android.cashbook.core.model.entity.buildBudgetItem
import cn.wj.android.cashbook.core.testing.util.captureMultiDevice
import cn.wj.android.cashbook.core.testing.util.captureMultiTheme
import cn.wj.android.cashbook.feature.budget.screen.BudgetScreen
import cn.wj.android.cashbook.feature.budget.viewmodel.BudgetUiState
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
@Config(application = HiltTestApplication::class, qualifiers = "480dpi")
@LooperMode(LooperMode.Mode.PAUSED)
class BudgetScreenScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    // 混合态：总体 NORMAL(64%) + 餐饮 NEAR(90%) + 交通 OVER(130%) + 购物 NORMAL(30%)
    private val sampleState = BudgetUiState.Success(
        progress = BudgetProgressEntity(
            overall = buildBudgetItem(BUDGET_TYPE_ID_TOTAL, "", "", limit = 500000L, spent = 320000L),
            categoryList = listOf(
                buildBudgetItem(10L, "餐饮", "ic", limit = 100000L, spent = 90000L),
                buildBudgetItem(11L, "交通", "ic", limit = 50000L, spent = 65000L),
                buildBudgetItem(12L, "购物", "ic", limit = 100000L, spent = 30000L),
            ),
        ),
        addableTypes = emptyList(),
    )

    private val emptyState = BudgetUiState.Success(
        progress = BudgetProgressEntity(overall = null, categoryList = emptyList()),
        addableTypes = emptyList(),
    )

    @Test
    fun budgetScreen_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "BudgetScreen") {
            BudgetScreen(uiState = sampleState, onBackClick = {}, onSetBudget = { _, _ -> }, onDeleteBudget = {})
        }
    }

    @Test
    fun budgetScreen_multipleDevices() {
        composeTestRule.captureMultiDevice(screenshotName = "BudgetScreen") {
            CashbookTheme {
                BudgetScreen(uiState = sampleState, onBackClick = {}, onSetBudget = { _, _ -> }, onDeleteBudget = {})
            }
        }
    }

    @Test
    fun budgetScreen_empty_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "BudgetScreen", overrideFileName = "BudgetScreen_empty") {
            BudgetScreen(uiState = emptyState, onBackClick = {}, onSetBudget = { _, _ -> }, onDeleteBudget = {})
        }
    }

    @Test
    fun budgetScreen_empty_multipleDevices() {
        composeTestRule.captureMultiDevice(screenshotName = "BudgetScreen_empty") {
            CashbookTheme {
                BudgetScreen(uiState = emptyState, onBackClick = {}, onSetBudget = { _, _ -> }, onDeleteBudget = {})
            }
        }
    }
}
