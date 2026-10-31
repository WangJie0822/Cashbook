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

package cn.wj.android.cashbook.core.testing.repository

/** 只供测试替身使用；真实匹配仍由 Room 对照测试验证。SQLite LIKE 默认只折叠 ASCII。 */
internal fun sqliteLikeContains(value: String, keyword: String): Boolean {
    fun asciiFold(text: String) = text.map { if (it in 'A'..'Z') it.lowercaseChar() else it }.joinToString("")
    val pattern = asciiFold(("%" + keyword + "%").substringBefore('\u0000')).map {
        when (it) {
            '%' -> ".*"
            '_' -> "."
            else -> Regex.escape(it.toString())
        }
    }.joinToString("")
    return Regex(pattern, RegexOption.DOT_MATCHES_ALL).matches(asciiFold(value.substringBefore('\u0000')))
}
