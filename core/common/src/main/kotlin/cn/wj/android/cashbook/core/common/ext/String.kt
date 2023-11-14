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

package cn.wj.android.cashbook.core.common.ext

import cn.wj.android.cashbook.core.common.Symbol

const val SCHEME_HTTPS = "https://"
const val SCHEME_HTTP = "http://"
const val SCHEME_DAVS = "davs://"
const val SCHEME_DAV = "dav://"
const val SCHEME_CONTENT = "content://"

val String.isWebUri: Boolean
    get() = startsWith(SCHEME_HTTPS) || startsWith(SCHEME_HTTP)

val String.isContentUri: Boolean
    get() = startsWith(SCHEME_CONTENT)

/** 给字符串添加上 CNY 符号 */
fun String.withCNY(): String {
    val source = if (this.contains(Symbol.CNY)) {
        this.replace(Symbol.CNY, "")
    } else {
        this
    }
    val negative = source.startsWith("-")
    return if (negative) {
        "-${Symbol.CNY}${source.replace("-", "")}"
    } else {
        "${Symbol.CNY}$source"
    }
}
