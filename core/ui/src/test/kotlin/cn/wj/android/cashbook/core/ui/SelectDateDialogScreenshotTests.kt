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
import cn.wj.android.cashbook.core.testing.util.captureMultiTheme
import cn.wj.android.cashbook.core.ui.component.SelectDateDialog
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "480dpi")
@LooperMode(LooperMode.Mode.PAUSED)
class SelectDateDialogScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun selectDateDialog_monthSelection_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "SelectDateDialog") {
            SelectDateDialog(
                onDialogDismiss = {},
                currentDate = YearMonth.of(2024, 6),
                onDateSelected = { _, _ -> },
            )
        }
    }

    @Test
    fun selectDateDialog_yearSelectable_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "SelectDateDialog",
            overrideFileName = "SelectDateDialog_yearSelectable",
        ) {
            SelectDateDialog(
                onDialogDismiss = {},
                currentDate = YearMonth.of(2024, 6),
                yearSelectable = true,
                onDateSelected = { _, _ -> },
            )
        }
    }

    @Test
    fun selectDateDialog_yearSelected_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "SelectDateDialog",
            overrideFileName = "SelectDateDialog_yearSelected",
        ) {
            SelectDateDialog(
                onDialogDismiss = {},
                currentDate = YearMonth.of(2024, 6),
                yearSelectable = true,
                yearSelected = true,
                onDateSelected = { _, _ -> },
            )
        }
    }
}
