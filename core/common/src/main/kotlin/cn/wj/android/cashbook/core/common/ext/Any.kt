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

import cn.wj.android.cashbook.core.common.tools.funLogger
import com.orhanobut.logger.Printer
import java.text.DecimalFormat

/** 使用类名作为日志打印 TAG */
val Any.tag: String
    get() = javaClass.simpleName

/** 使用 [tag] 或者为空时使用 [Any.tag] 作为日志打印 TAG 获取 [Printer] 对象进行日志打印 */
fun Any.logger(tag: String? = null): Printer {
    val realTag = (tag ?: this.tag) + "(${this.hashCode()})"
    return funLogger(realTag)
}

/** 当对象为 null 时使用备用对象 [t] */
fun <T> T?.orElse(t: T): T {
    return this ?: t
}

/** 如果满足 [condition] 返回 [defaultValue] 的值，否则返回当前值 */
inline fun <T> T.ifCondition(condition: Boolean, crossinline defaultValue: () -> T): T {
    return if (condition) {
        defaultValue.invoke()
    } else {
        this
    }
}

/** 对任意格式数字数据进行格式化并返回 [String] */
fun <T> T?.decimalFormat(pattern: String = "#.##"): String {
    return DecimalFormat(pattern).format(this?.toString()?.toBigDecimalOrNull() ?: return "")
}
