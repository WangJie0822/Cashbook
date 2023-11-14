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

@file:Suppress("unused")

package cn.wj.android.cashbook.core.common.tools

import java.util.regex.Pattern

/**
 * 判断字符序列是否匹配正则表达式
 *
 * @param regex 正则表达式
 *
 * @return 匹配正则表达式：true 不匹配正则表达式：false
 */
fun CharSequence?.isMatch(regex: String): Boolean {
    return !isNullOrBlank() && Pattern.matches(regex, this)
}
