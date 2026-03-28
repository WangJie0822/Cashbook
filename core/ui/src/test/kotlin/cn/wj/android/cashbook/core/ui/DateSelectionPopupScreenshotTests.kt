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

package cn.wj.android.cashbook.core.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import cn.wj.android.cashbook.core.model.entity.DateSelectionEntity
import cn.wj.android.cashbook.core.testing.util.captureMultiTheme
import cn.wj.android.cashbook.core.ui.component.DateSelectionPopup
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "480dpi")
@LooperMode(LooperMode.Mode.PAUSED)
class DateSelectionPopupScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun dateSelectionPopup_byDay_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "DateSelectionPopup") {
            DateSelectionPopup(
                expanded = true,
                onDismissRequest = {},
                currentSelection = DateSelectionEntity.ByDay(LocalDate.of(2024, 6, 15)),
                onDateSelected = {},
            )
        }
    }

    @Test
    fun dateSelectionPopup_byMonth_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "DateSelectionPopup",
            overrideFileName = "DateSelectionPopup_byMonth",
        ) {
            DateSelectionPopup(
                expanded = true,
                onDismissRequest = {},
                currentSelection = DateSelectionEntity.ByMonth(YearMonth.of(2024, 6)),
                onDateSelected = {},
            )
        }
    }

    @Test
    fun dateSelectionPopup_byYear_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "DateSelectionPopup",
            overrideFileName = "DateSelectionPopup_byYear",
        ) {
            DateSelectionPopup(
                expanded = true,
                onDismissRequest = {},
                currentSelection = DateSelectionEntity.ByYear(2024),
                onDateSelected = {},
            )
        }
    }

    @Test
    fun dateSelectionPopup_dateRange_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "DateSelectionPopup",
            overrideFileName = "DateSelectionPopup_dateRange",
        ) {
            DateSelectionPopup(
                expanded = true,
                onDismissRequest = {},
                currentSelection = DateSelectionEntity.DateRange(
                    from = LocalDate.of(2024, 1, 1),
                    to = LocalDate.of(2024, 6, 30),
                ),
                onDateSelected = {},
            )
        }
    }

    @Test
    fun dateSelectionPopup_all_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "DateSelectionPopup",
            overrideFileName = "DateSelectionPopup_all",
        ) {
            DateSelectionPopup(
                expanded = true,
                onDismissRequest = {},
                currentSelection = DateSelectionEntity.All,
                onDateSelected = {},
            )
        }
    }
}
