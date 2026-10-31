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

package cn.wj.android.cashbook.feature.settings.view

import android.util.TypedValue
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ScrollView
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.core.MarkwonTheme

/** 原生滚动容器承担长文滚动，TextView 直接保留 Markwon 的样式和链接 Span。 */
@Composable
internal fun MarkdownContent(
    markdownData: String,
    onLinkFailure: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography.bodyLarge
    val textColor = colors.onSurface.toArgb()
    val linkColor = colors.primary.toArgb()
    val codeColor = colors.onSurfaceVariant.toArgb()
    val codeBackground = colors.surfaceVariant.toArgb()
    val quoteColor = colors.outline.toArgb()
    val latestOnLinkFailure by rememberUpdatedState(onLinkFailure)
    val markwon = remember(context, density, linkColor, codeColor, codeBackground, quoteColor) {
        Markwon.builder(context)
            .usePlugin(object : AbstractMarkwonPlugin() {
                override fun configureTheme(builder: MarkwonTheme.Builder) {
                    builder.linkColor(linkColor)
                        .codeTextColor(codeColor)
                        .codeBackgroundColor(codeBackground)
                        .blockQuoteColor(quoteColor)
                }

                override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                    builder.linkResolver { view, url ->
                        if (!openMarkdownLink(view.context, url)) latestOnLinkFailure()
                    }
                }
            })
            .build()
    }
    val parsed = remember(markwon, markdownData) { markwon.toMarkdown(markdownData) }
    val textSize = with(density) { typography.fontSize.toPx() }
    val lineHeight = with(density) { typography.lineHeight.toPx() }.toInt()
    val padding = with(density) { 16.dp.roundToPx() }
    var savedScrollY by rememberSaveable(markdownData) { mutableIntStateOf(0) }

    key(context) {
        AndroidView(
            modifier = modifier.fillMaxSize(),
            factory = { viewContext ->
                ScrollView(viewContext).apply {
                    isFillViewport = true
                    // AndroidView 不自动裁剪原生内容，滚动绘制必须限制在正文视口内。
                    outlineProvider = ViewOutlineProvider.BOUNDS
                    clipToOutline = true
                    addView(
                        TextView(viewContext),
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                        ),
                    )
                }
            },
            update = { scroll ->
                val text = scroll.getChildAt(0) as TextView
                text.setTextColor(textColor)
                text.setLinkTextColor(linkColor)
                text.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
                text.setLineSpacing((lineHeight - text.paint.fontSpacing).coerceAtLeast(0f), 1f)
                text.setPadding(padding, padding, padding, padding)
                // Source 改变后监听器也接到新保存状态，避免继续更新旧文档的滚动位置。
                scroll.setOnScrollChangeListener { _, _, scrollY, _, _ -> savedScrollY = scrollY }
                // 普通重组不重复 setText，避免丢失原生选择、Span 身份与滚动位置。
                if (text.tag !== parsed) {
                    val position = savedScrollY
                    markwon.setParsedMarkdown(text, parsed)
                    text.tag = parsed
                    scroll.post {
                        if (text.tag === parsed) scroll.scrollTo(0, position)
                    }
                }
            },
        )
    }
}
