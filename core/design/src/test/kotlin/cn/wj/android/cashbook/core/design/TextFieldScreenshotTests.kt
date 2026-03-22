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
import cn.wj.android.cashbook.core.design.component.CbOutlinedTextField
import cn.wj.android.cashbook.core.design.component.CbPasswordTextField
import cn.wj.android.cashbook.core.design.component.CbTextField
import cn.wj.android.cashbook.core.design.component.TextFieldState
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
class TextFieldScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun cbTextField_empty_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "TextField") {
            CbTextField(
                textFieldState = TextFieldState(defaultText = ""),
                label = { Text(text = "标签") },
                placeholder = { Text(text = "请输入内容") },
            )
        }
    }

    @Test
    fun cbTextField_withText_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "TextField",
            overrideFileName = "TextField_withText",
        ) {
            CbTextField(
                textFieldState = TextFieldState(defaultText = "已输入内容"),
                label = { Text(text = "标签") },
            )
        }
    }

    @Test
    fun cbTextField_error_multipleThemes() {
        val errorState = TextFieldState(
            defaultText = "",
            validator = { false },
            errorFor = { "必填项" },
        ).apply {
            requestErrors()
        }
        composeTestRule.captureMultiTheme(
            name = "TextField",
            overrideFileName = "TextField_error",
        ) {
            CbTextField(textFieldState = errorState)
        }
    }

    @Test
    fun cbOutlinedTextField_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "TextField",
            overrideFileName = "OutlinedTextField",
        ) {
            CbOutlinedTextField(
                textFieldState = TextFieldState(defaultText = "已输入内容"),
                label = { Text(text = "标签") },
            )
        }
    }

    @Test
    fun cbPasswordTextField_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "TextField",
            overrideFileName = "PasswordTextField",
        ) {
            CbPasswordTextField(
                textFieldState = TextFieldState(defaultText = "password123"),
                label = { Text(text = "密码") },
            )
        }
    }
}
