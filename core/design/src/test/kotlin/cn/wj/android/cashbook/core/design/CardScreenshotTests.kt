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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import cn.wj.android.cashbook.core.design.component.CbCard
import cn.wj.android.cashbook.core.design.component.CbElevatedCard
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
class CardScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun cbCard_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "Card") {
            CbCard {
                Text("Card content", Modifier.padding(16.dp))
            }
        }
    }

    @Test
    fun cbCard_clickable_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "Card",
            overrideFileName = "Card_clickable",
        ) {
            CbCard(onClick = {}) {
                Text("Card content", Modifier.padding(16.dp))
            }
        }
    }

    @Test
    fun cbElevatedCard_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "Card",
            overrideFileName = "ElevatedCard",
        ) {
            CbElevatedCard {
                Text("Card content", Modifier.padding(16.dp))
            }
        }
    }
}
