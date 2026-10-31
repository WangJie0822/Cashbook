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

import android.content.ActivityNotFoundException
import android.content.Context
import cn.wj.android.cashbook.core.common.tools.jumpBrowser
import java.net.URI
import java.net.URISyntaxException

/** 仅允许绝对网页地址，保留原始 URL 并沿用应用已有的系统分发入口。 */
internal fun openMarkdownLink(context: Context, url: String): Boolean {
    val uri = try {
        URI(url)
    } catch (_: URISyntaxException) {
        return false
    }
    val isWebScheme = uri.scheme.equals("http", ignoreCase = true) ||
        uri.scheme.equals("https", ignoreCase = true)
    if (!isWebScheme || uri.host.isNullOrBlank()) {
        return false
    }
    return try {
        jumpBrowser(url = url, context = context)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    } catch (_: IllegalArgumentException) {
        false
    }
}
