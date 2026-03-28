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
import cn.wj.android.cashbook.core.design.component.CbAlertDialog
import cn.wj.android.cashbook.core.design.component.CbTextButton
import cn.wj.android.cashbook.core.testing.util.captureMultiTheme
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

/**
 * DateRangePickerDialog 截图测试
 *
 * 注意：DateRangePickerDialog 内部使用 MaterialDatePicker（Fragment 弹窗），
 * 需要 FragmentActivity 且弹窗在独立窗口渲染，Roborazzi 的 onRoot() 无法捕获。
 * 此处使用 CbAlertDialog 模拟日期范围选择器的外观进行截图测试。
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "480dpi")
@LooperMode(LooperMode.Mode.PAUSED)
class DateRangePickerScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun dateRangePicker_placeholder_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "DateRangePicker") {
            CbAlertDialog(
                onDismissRequest = {},
                title = { Text(text = "选择日期范围") },
                text = { Text(text = "2024/01/01 - 2024/12/31") },
                confirmButton = {
                    CbTextButton(onClick = {}) {
                        Text(text = "确认")
                    }
                },
                dismissButton = {
                    CbTextButton(onClick = {}) {
                        Text(text = "取消")
                    }
                },
            )
        }
    }
}
