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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import cn.wj.android.cashbook.core.design.component.CbLineChart
import cn.wj.android.cashbook.core.design.component.LineDataSet
import cn.wj.android.cashbook.core.design.component.LineEntry
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
class LineChartScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val chartModifier = Modifier.fillMaxWidth().height(200.dp)

    private val singleEntries = listOf(
        LineEntry(x = 0f, y = 100f, label = "1月"),
        LineEntry(x = 1f, y = 250f, label = "2月"),
        LineEntry(x = 2f, y = 180f, label = "3月"),
        LineEntry(x = 3f, y = 320f, label = "4月"),
    )

    @Test
    fun cbLineChart_singleDataSet_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "LineChart") {
            CbLineChart(
                dataSets = listOf(
                    LineDataSet(
                        label = "支出",
                        entries = singleEntries,
                        color = Color(0xFFE57373),
                    ),
                ),
                modifier = chartModifier,
            )
        }
    }

    @Test
    fun cbLineChart_multipleDataSets_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "LineChart",
            overrideFileName = "LineChart_multipleDataSets",
        ) {
            CbLineChart(
                dataSets = listOf(
                    LineDataSet(
                        label = "支出",
                        entries = singleEntries,
                        color = Color(0xFFE57373),
                    ),
                    LineDataSet(
                        label = "收入",
                        entries = listOf(
                            LineEntry(x = 0f, y = 200f, label = "1月"),
                            LineEntry(x = 1f, y = 350f, label = "2月"),
                            LineEntry(x = 2f, y = 280f, label = "3月"),
                            LineEntry(x = 3f, y = 400f, label = "4月"),
                        ),
                        color = Color(0xFF81C784),
                    ),
                ),
                modifier = chartModifier,
            )
        }
    }

    @Test
    fun cbLineChart_empty_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "LineChart",
            overrideFileName = "LineChart_empty",
        ) {
            CbLineChart(
                dataSets = emptyList(),
                modifier = chartModifier,
            )
        }
    }

    @Test
    fun cbLineChart_withZeroLine_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "LineChart",
            overrideFileName = "LineChart_withZeroLine",
        ) {
            CbLineChart(
                dataSets = listOf(
                    LineDataSet(
                        label = "收支",
                        entries = listOf(
                            LineEntry(x = 0f, y = -100f, label = "1月"),
                            LineEntry(x = 1f, y = 150f, label = "2月"),
                            LineEntry(x = 2f, y = -50f, label = "3月"),
                            LineEntry(x = 3f, y = 200f, label = "4月"),
                        ),
                        color = Color(0xFF64B5F6),
                    ),
                ),
                showZeroLine = true,
                modifier = chartModifier,
            )
        }
    }
}
