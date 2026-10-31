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

package cn.wj.android.cashbook.feature.settings.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.testing.util.captureMultiDevice
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
class MarkdownScreenScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun markdownScreen_empty_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "MarkdownScreen",
            overrideFileName = "MarkdownScreen_empty",
        ) {
            MarkdownScreen(
                title = "版本信息",
                markdownData = "",
                onBackClick = {},
            )
        }
    }

    @Test
    fun markdownScreen_withContent_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "MarkdownScreen") {
            MarkdownScreen(
                title = "版本信息",
                markdownData = "# 更新日志\n\n## v1.0.0\n\n- 初始版本发布\n- 支持记账功能\n- 支持多账本管理",
                onBackClick = {},
            )
        }
    }

    @Test
    fun markdownScreen_withContent_multipleDevices() {
        composeTestRule.captureMultiDevice(screenshotName = "MarkdownScreen") {
            CashbookTheme {
                MarkdownScreen(
                    title = "版本信息",
                    markdownData = "# 更新日志\n\n## v1.0.0\n\n- 初始版本发布\n- 支持记账功能\n- 支持多账本管理",
                    onBackClick = {},
                )
            }
        }
    }

    @Test
    fun markdownScreen_empty_multipleDevices() {
        composeTestRule.captureMultiDevice(screenshotName = "MarkdownScreen_empty") {
            CashbookTheme {
                MarkdownScreen(
                    title = "版本信息",
                    markdownData = "",
                    onBackClick = {},
                )
            }
        }
    }

    @Test
    fun markdownScreen_formattedLinks_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "MarkdownScreen",
            overrideFileName = "MarkdownScreen_formattedLinks",
        ) {
            MarkdownScreen(
                title = "隐私政策",
                markdownData = formattedMarkdown,
                onBackClick = {},
            )
        }
    }

    @Test
    fun markdownScreen_formattedLinks_multipleDevices() {
        composeTestRule.captureMultiDevice(screenshotName = "MarkdownScreen_formattedLinks") {
            CashbookTheme {
                MarkdownScreen(
                    title = "隐私政策",
                    markdownData = formattedMarkdown,
                    onBackClick = {},
                )
            }
        }
    }

    private val formattedMarkdown = """
        # 使用说明

        这里保留 **重要内容**、*强调说明* 与 [查看完整隐私说明](https://example.org/privacy?q=app#details)。

        ## 数据与权限

        - 账单保存在你的账本中
        - 链接由系统选择的应用打开

        > 请在外部页面核对网址。

        行内代码：`amount = 100`

        ```
        收入 - 支出 = 结余
        ```
    """.trimIndent()
}
