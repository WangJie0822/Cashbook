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

import android.content.ActivityNotFoundException
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.BitmapFactory
import android.text.Spanned
import android.text.style.ClickableSpan
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.feature.settings.view.MarkdownContent
import cn.wj.android.cashbook.feature.settings.view.openMarkdownLink
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import dagger.hilt.android.testing.HiltTestApplication
import io.noties.markwon.core.spans.BlockQuoteSpan
import io.noties.markwon.core.spans.BulletListItemSpan
import io.noties.markwon.core.spans.CodeBlockSpan
import io.noties.markwon.core.spans.CodeSpan
import io.noties.markwon.core.spans.EmphasisSpan
import io.noties.markwon.core.spans.HeadingSpan
import io.noties.markwon.core.spans.StrongEmphasisSpan
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import java.io.File

/**
 * 从真实页面的原生文本与点击副作用守护链接接线，避免仅测试未被页面使用的打开函数。
 *
 * 覆盖标题/强调/列表/代码样式、重组与滚动、主题字号、失败反馈、空态与返回。
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "480dpi")
@LooperMode(LooperMode.Mode.PAUSED)
class MarkdownScreenBehaviorTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun markdownLink_clickableSpan_opensSystemChooserWithOriginalUrl() {
        val linkLabel = "查看隐私说明"
        val originalUrl = "https://example.org/privacy?q=a%20b&lang=zh#details"
        composeTestRule.setContent {
            CashbookTheme {
                MarkdownScreen(
                    title = "隐私政策",
                    markdownData = "# 隐私政策\n\n[$linkLabel]($originalUrl)",
                    onBackClick = {},
                )
            }
        }

        composeTestRule.runOnIdle {
            val linkViews = textViews(composeTestRule.activity.window.decorView)
                .filter { it.text.contains(linkLabel) }
                .toList()
            assertWithMessage("Markdown 链接必须由保留原生可点击 Span 的文本承载")
                .that(linkViews)
                .hasSize(1)
            val linkView = linkViews.single()
            assertThat(linkView.text).isInstanceOf(Spanned::class.java)
            val text = linkView.text as Spanned
            val labelStart = text.toString().indexOf(linkLabel)
            val labelEnd = labelStart + linkLabel.length
            val linkSpans = text.getSpans(labelStart, labelEnd, ClickableSpan::class.java)
                .filter { text.getSpanStart(it) <= labelStart && text.getSpanEnd(it) >= labelEnd }
            assertWithMessage("链接标签必须保留可点击 Span，不能在 Compose 文本转换时丢失")
                .that(linkSpans)
                .hasSize(1)

            linkSpans.single().onClick(linkView)

            val chooser = shadowOf(composeTestRule.activity).nextStartedActivity
            assertThat(chooser).isNotNull()
            assertThat(chooser.action).isEqualTo(Intent.ACTION_CHOOSER)
            @Suppress("DEPRECATION")
            val target = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
            assertThat(target).isNotNull()
            assertThat(target!!.action).isEqualTo(Intent.ACTION_VIEW)
            assertThat(target.dataString).isEqualTo(originalUrl)
        }
    }

    @Test
    fun markdownContent_retainsHeadingEmphasisListsQuoteAndCodeSpans() {
        composeTestRule.setContent {
            CashbookTheme {
                MarkdownScreen(
                    title = "样式",
                    markdownData = "# 标题\n\n**加粗**与*斜体*\n\n- 列表\n\n> 引用\n\n`行内代码`\n\n```\n代码块\n```",
                    onBackClick = {},
                )
            }
        }
        composeTestRule.runOnIdle {
            val text = markdownText("行内代码").text as Spanned
            listOf(
                HeadingSpan::class.java,
                StrongEmphasisSpan::class.java,
                EmphasisSpan::class.java,
                BulletListItemSpan::class.java,
                BlockQuoteSpan::class.java,
                CodeSpan::class.java,
                CodeBlockSpan::class.java,
            ).forEach { type ->
                assertWithMessage("保留 ${type.simpleName}")
                    .that(text.getSpans(0, text.length, type).toList())
                    .isNotEmpty()
            }
        }
    }

    @Test
    fun sameMarkdown_recompositionKeepsTextIdentityAndLongContentScrollPosition() {
        val title = mutableStateOf("初始标题")
        val longMarkdown = (1..160).joinToString("\n\n") { "段落 $it：长文内容" }
        composeTestRule.setContent {
            CashbookTheme {
                MarkdownScreen(title = title.value, markdownData = longMarkdown, onBackClick = {})
            }
        }
        lateinit var originalView: TextView
        lateinit var originalText: CharSequence
        var position = 0
        composeTestRule.runOnIdle {
            originalView = markdownText("段落 160")
            originalText = originalView.text
            val scroll = originalView.parent as ScrollView
            assertThat(scroll.height).isGreaterThan(0)
            assertThat(scroll.height).isLessThan(originalView.height)
            scroll.scrollTo(0, 400)
            position = scroll.scrollY
            assertThat(position).isGreaterThan(0)
            title.value = "重组后的标题"
        }
        composeTestRule.runOnIdle {
            val updated = markdownText("段落 160")
            assertThat(updated).isSameInstanceAs(originalView)
            assertThat(updated.text).isSameInstanceAs(originalText)
            assertThat((updated.parent as ScrollView).scrollY).isEqualTo(position)
        }
        composeTestRule.onNodeWithText("重组后的标题").assertIsDisplayed()
    }

    @Test
    @OptIn(ExperimentalRoborazziApi::class)
    fun scrolledMarkdown_keepsAllTitleBarPixelsUnchanged() {
        val title = "标题边界"
        val markdown = (1..160).joinToString("\n\n") { "段落 $it：滚动正文不能覆盖标题和返回按钮。" }
        composeTestRule.setContent {
            CashbookTheme {
                MarkdownScreen(title = title, markdownData = markdown, onBackClick = {})
            }
        }
        val titleBottom = kotlin.math.ceil(composeTestRule.onNodeWithText(title).fetchSemanticsNode().boundsInRoot.bottom).toInt()
        val rootTop = kotlin.math.round(composeTestRule.onRoot().fetchSemanticsNode().boundsInWindow.top).toInt()
        val headerHeight = composeTestRule.runOnIdle {
            val location = IntArray(2)
            (markdownText("段落 160").parent as ScrollView).getLocationInWindow(location)
            location[1] - rootTop
        }
        assertThat(headerHeight).isAtLeast(titleBottom)
        fun headerPixels(name: String): IntArray {
            val file = File("build/tmp/markdown-viewport/$name.png")
            file.parentFile!!.mkdirs()
            composeTestRule.onRoot().captureRoboImage(
                file.absolutePath,
                roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record, recordOptions = RoborazziOptions.RecordOptions(resizeScale = 1.0)),
            )
            val bitmap = checkNotNull(BitmapFactory.decodeFile(file.absolutePath))
            return IntArray(bitmap.width * headerHeight).also { bitmap.getPixels(it, 0, bitmap.width, 0, 0, bitmap.width, headerHeight) }
        }
        val expected = headerPixels("before")
        composeTestRule.runOnIdle {
            val scroll = markdownText("段落 160").parent as ScrollView
            scroll.scrollTo(0, 500)
            assertThat(scroll.scrollY).isEqualTo(500)
        }
        val after = headerPixels("after")
        val changed = expected.indices.count { index -> expected[index] != after[index] }
        assertWithMessage("长文滚动后标题区不得被原生正文绘制覆盖").that(changed).isEqualTo(0)
    }

    @Test
    fun changedMarkdown_updatesVisibleTextAndClickableDestination() {
        val markdown = mutableStateOf("[旧链接](https://example.org/old)")
        composeTestRule.setContent {
            CashbookTheme {
                MarkdownScreen(title = "更新", markdownData = markdown.value, onBackClick = {})
            }
        }
        composeTestRule.runOnIdle { markdown.value = "[新链接](https://example.org/new#next)" }
        composeTestRule.runOnIdle {
            val view = markdownText("新链接")
            assertThat(view.text.toString()).doesNotContain("旧链接")
            clickLink(view, "新链接")
            val chooser = shadowOf(composeTestRule.activity).nextStartedActivity

            @Suppress("DEPRECATION")
            val target = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
            assertThat(target!!.dataString).isEqualTo("https://example.org/new#next")
        }
    }

    @Test
    fun themeAndFontScale_changesUpdateNativeTextAndLinkColors() {
        val dark = mutableStateOf(false)
        val fontScale = mutableStateOf(1f)
        var expectedTextColor = 0
        var expectedLinkColor = 0
        var expectedTextSize = 0f
        composeTestRule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale.value)) {
                CashbookTheme(darkTheme = dark.value) {
                    expectedTextColor = MaterialTheme.colorScheme.onSurface.toArgb()
                    expectedLinkColor = MaterialTheme.colorScheme.primary.toArgb()
                    expectedTextSize = with(LocalDensity.current) {
                        MaterialTheme.typography.bodyLarge.fontSize.toPx()
                    }
                    MarkdownScreen(
                        title = "主题",
                        markdownData = "[主题链接](https://example.org/theme)",
                        onBackClick = {},
                    )
                }
            }
        }
        var previousSize = 0f
        composeTestRule.runOnIdle {
            val view = markdownText("主题链接")
            previousSize = view.textSize
            assertThat(view.currentTextColor).isEqualTo(expectedTextColor)
            assertThat(view.linkTextColors.defaultColor).isEqualTo(expectedLinkColor)
            dark.value = true
            fontScale.value = 1.5f
        }
        composeTestRule.runOnIdle {
            val view = markdownText("主题链接")
            assertThat(view.currentTextColor).isEqualTo(expectedTextColor)
            assertThat(view.linkTextColors.defaultColor).isEqualTo(expectedLinkColor)
            assertThat(view.textSize).isEqualTo(expectedTextSize)
            assertThat(view.textSize).isGreaterThan(previousSize)
        }
    }

    @Test
    fun unavailableHandler_actualSpanShowsFailureAndKeepsMarkdown() {
        composeTestRule.setContent {
            val baseContext = LocalContext.current
            val context = object : ContextWrapper(baseContext) {
                override fun startActivity(intent: Intent) {
                    throw ActivityNotFoundException("合成环境无处理程序")
                }
            }
            CompositionLocalProvider(LocalContext provides context) {
                CashbookTheme {
                    MarkdownScreen(
                        title = "打开失败",
                        markdownData = "保留正文 [网页](https://example.org/unavailable)",
                        onBackClick = {},
                    )
                }
            }
        }
        composeTestRule.runOnIdle { clickLink(markdownText("保留正文"), "网页") }
        composeTestRule.onNodeWithText("无法打开此链接").assertIsDisplayed()
        composeTestRule.runOnIdle {
            assertThat(markdownText("保留正文").text.toString()).contains("保留正文")
            assertThat(shadowOf(composeTestRule.activity).nextStartedActivity).isNull()
        }
    }

    @Test
    fun rejectedScheme_actualSpanShowsFailureWithoutStartingActivity() {
        composeTestRule.setContent {
            CashbookTheme {
                MarkdownScreen(
                    title = "非法链接",
                    markdownData = "[邮件](mailto:test@example.org)",
                    onBackClick = {},
                )
            }
        }
        composeTestRule.runOnIdle { clickLink(markdownText("邮件"), "邮件") }
        composeTestRule.onNodeWithText("无法打开此链接").assertIsDisplayed()
        composeTestRule.runOnIdle {
            assertThat(shadowOf(composeTestRule.activity).nextStartedActivity).isNull()
        }
    }

    @Test
    fun invalidWebUrls_areRejectedWithoutStartingActivity() {
        composeTestRule.runOnUiThread {
            listOf(
                "", "/relative", "https:///missing-host", "http://", "https://?q=1",
                "javascript:alert(1)", "file:///sdcard/private", "intent://example.org",
                "ftp://example.org", "https://example.org/contains space",
            ).forEach { url ->
                assertWithMessage("拒绝非法网页地址：$url")
                    .that(openMarkdownLink(composeTestRule.activity, url)).isFalse()
                assertThat(shadowOf(composeTestRule.activity).nextStartedActivity).isNull()
            }
        }
    }

    @Test
    fun recoverableStartFailures_returnFalse() {
        composeTestRule.runOnUiThread {
            listOf(
                ActivityNotFoundException("无处理程序"),
                SecurityException("系统拒绝"),
                IllegalArgumentException("启动参数被系统拒绝"),
            ).forEach { failure ->
                val context = object : ContextWrapper(composeTestRule.activity) {
                    override fun startActivity(intent: Intent) = throw failure
                }
                assertThat(openMarkdownLink(context, "http://example.org/path")).isFalse()
            }
        }
    }

    @Test
    fun validWebUrls_preserveCaseEncodingQueryAndFragment() {
        composeTestRule.runOnUiThread {
            listOf(
                "http://example.org/path",
                "HTTPS://Example.ORG/a%2Fb?q=%E4%B8%AD&value=one%20two#Part",
            ).forEach { url ->
                assertThat(openMarkdownLink(composeTestRule.activity, url)).isTrue()
                val chooser = shadowOf(composeTestRule.activity).nextStartedActivity
                assertThat(chooser.action).isEqualTo(Intent.ACTION_CHOOSER)
                @Suppress("DEPRECATION")
                val target = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                assertThat(target!!.action).isEqualTo(Intent.ACTION_VIEW)
                assertThat(target.dataString).isEqualTo(url)
            }
        }
    }

    @Test
    fun latestFailureCallback_isUsedWithoutReparsingSameMarkdown() {
        val callbackVersion = mutableIntStateOf(1)
        var reportedVersion = 0
        composeTestRule.setContent {
            val version = callbackVersion.intValue
            CashbookTheme {
                MarkdownContent(
                    markdownData = "[不支持](mailto:test@example.org)",
                    onLinkFailure = { reportedVersion = version },
                )
            }
        }
        lateinit var originalText: CharSequence
        composeTestRule.runOnIdle {
            originalText = markdownText("不支持").text
            callbackVersion.intValue = 2
        }
        composeTestRule.runOnIdle {
            val view = markdownText("不支持")
            assertThat(view.text).isSameInstanceAs(originalText)
            clickLink(view, "不支持")
            assertThat(reportedVersion).isEqualTo(2)
        }
    }

    @Test
    fun emptyMarkdown_preservesEmptyHintAndBackNavigation() {
        var backCount = 0
        composeTestRule.setContent {
            CompositionLocalProvider(cn.wj.android.cashbook.core.design.component.LocalDefaultEmptyImagePainter provides androidx.compose.ui.graphics.painter.BitmapPainter(androidx.compose.ui.graphics.ImageBitmap(1, 1))) {
                CashbookTheme {
                    MarkdownScreen(title = "空文档", markdownData = " \n", onBackClick = { backCount++ })
                }
            }
        }
        val hint = composeTestRule.activity.getString(cn.wj.android.cashbook.core.ui.R.string.markdown_no_data_hint)
        composeTestRule.onNodeWithText(hint).assertIsDisplayed()
        val back = composeTestRule.activity.getString(cn.wj.android.cashbook.core.design.R.string.cd_design_navigate_back)
        composeTestRule.onNodeWithContentDescription(back).performClick()
        composeTestRule.runOnIdle {
            assertThat(backCount).isEqualTo(1)
            assertThat(textViews(composeTestRule.activity.window.decorView).toList()).isEmpty()
        }
    }

    private fun markdownText(containing: String): TextView =
        textViews(composeTestRule.activity.window.decorView).single { it.text.contains(containing) }

    private fun clickLink(view: TextView, label: String) {
        val text = view.text as Spanned
        val start = text.toString().indexOf(label)
        assertThat(start).isAtLeast(0)
        text.getSpans(start, start + label.length, ClickableSpan::class.java).single().onClick(view)
    }

    private fun textViews(view: View): Sequence<TextView> = sequence {
        if (view is TextView) yield(view)
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                yieldAll(textViews(view.getChildAt(index)))
            }
        }
    }
}
