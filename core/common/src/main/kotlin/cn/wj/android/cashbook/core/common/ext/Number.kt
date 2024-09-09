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

import java.math.BigDecimal

/** 将 [String] 类型数字转换为 [BigDecimal]，为空或转换失败，返回值为 `0` 的 [BigDecimal] */
fun String?.toBigDecimalOrZero(): BigDecimal {
    return this?.toBigDecimalOrNull() ?: "0".toBigDecimal()
}

/** 将 [Number] 类型数字转换为 [BigDecimal]，为空或转换失败，返回值为 `0` 的 [BigDecimal] */
fun Number?.toBigDecimalOrZero(): BigDecimal {
    return this?.toString().toBigDecimalOrZero()
}

/** 将 [String] 类型数字转换为 [Float]，为空或转换失败，返回值为 `0f` 的 [Float] */
fun String?.toFloatOrZero(): Float {
    return this?.toFloatOrNull() ?: 0f
}

/** 将 [String] 类型数字转换为 [Double]，为空或转换失败，返回值为 `0.0` 的 [Double] */
fun String?.toDoubleOrZero(): Double {
    return this?.toDoubleOrNull() ?: 0.0
}

/** 将 [String] 类型数字转换为 [Int]，为空或转换失败，返回值为 `0` 的 [Int] */
fun String?.toIntOrZero(): Int {
    return this?.toIntOrNull() ?: 0
}

/** 将 [Int] 类型的数字转换为 [String]，并在数字小于 10 时，在前补 0 */
fun Int.completeZero(): String {
    return if (this >= 10) {
        this.toString()
    } else {
        "0$this"
    }
}
