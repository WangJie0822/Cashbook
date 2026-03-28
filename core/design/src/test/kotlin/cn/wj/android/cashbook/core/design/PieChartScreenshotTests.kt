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

package cn.wj.android.cashbook.core.design

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import cn.wj.android.cashbook.core.design.component.CbPieChart
import cn.wj.android.cashbook.core.design.component.PieSlice
import cn.wj.android.cashbook.core.testing.util.captureMultiTheme
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
class PieChartScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val sampleSlices = listOf(
        PieSlice(label = "餐饮", value = 0.4f, color = Color(0xFFE57373)),
        PieSlice(label = "交通", value = 0.25f, color = Color(0xFF64B5F6)),
        PieSlice(label = "购物", value = 0.2f, color = Color(0xFFFFD54F)),
        PieSlice(label = "娱乐", value = 0.15f, color = Color(0xFF81C784)),
    )

    @Test
    fun cbPieChart_withData_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "PieChart") {
            CbPieChart(
                slices = sampleSlices,
                centerText = "¥8,000",
                modifier = Modifier.size(200.dp),
            )
        }
    }

    @Test
    fun cbPieChart_empty_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "PieChart",
            overrideFileName = "PieChart_empty",
        ) {
            CbPieChart(
                slices = emptyList(),
                centerText = "¥0",
                modifier = Modifier.size(200.dp),
            )
        }
    }

    @Test
    fun cbPieChart_selectedSlice_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "PieChart",
            overrideFileName = "PieChart_selectedSlice",
        ) {
            CbPieChart(
                slices = sampleSlices,
                centerText = "¥8,000",
                selectedIndex = 0,
                modifier = Modifier.size(200.dp),
            )
        }
    }
}
