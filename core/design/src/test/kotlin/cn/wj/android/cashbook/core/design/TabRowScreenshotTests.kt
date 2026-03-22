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
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import cn.wj.android.cashbook.core.design.component.CbTab
import cn.wj.android.cashbook.core.design.component.CbTabRow
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
class TabRowScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val tabs = listOf("标签一", "标签二", "标签三")

    @Test
    fun cbTabRow_firstSelected_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "TabRow") {
            CbTabRow(selectedTabIndex = 0) {
                tabs.forEachIndexed { index, title ->
                    CbTab(
                        selected = index == 0,
                        onClick = {},
                        text = { Text(text = title) },
                    )
                }
            }
        }
    }

    @Test
    fun cbTabRow_secondSelected_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "TabRow",
            overrideFileName = "TabRow_secondSelected",
        ) {
            CbTabRow(selectedTabIndex = 1) {
                tabs.forEachIndexed { index, title ->
                    CbTab(
                        selected = index == 1,
                        onClick = {},
                        text = { Text(text = title) },
                    )
                }
            }
        }
    }
}
