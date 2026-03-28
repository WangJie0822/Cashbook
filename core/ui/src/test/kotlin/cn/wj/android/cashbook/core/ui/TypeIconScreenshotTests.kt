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
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import cn.wj.android.cashbook.core.design.icon.CbIcons
import cn.wj.android.cashbook.core.testing.util.captureMultiTheme
import cn.wj.android.cashbook.core.ui.component.TypeIcon
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
class TypeIconScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun typeIcon_default_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "TypeIcon") {
            TypeIcon(
                painter = rememberVectorPainter(image = CbIcons.Category),
            )
        }
    }

    @Test
    fun typeIcon_customColor_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "TypeIcon",
            overrideFileName = "TypeIcon_customColor",
        ) {
            TypeIcon(
                painter = rememberVectorPainter(image = CbIcons.Category),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            )
        }
    }

    @Test
    fun typeIcon_showMore_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "TypeIcon",
            overrideFileName = "TypeIcon_showMore",
        ) {
            TypeIcon(
                painter = rememberVectorPainter(image = CbIcons.Category),
                showMore = true,
            )
        }
    }
}
