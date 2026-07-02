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

/**
 * 数字转换工具方法（BigDecimal 部分）。
 *
 * 纯 Kotlin 部分（`toFloatOrZero`/`toDoubleOrZero`/`toIntOrZero`/`completeZero`）已迁至 shared/commonMain
 * 的 `Number.kt`。此处 BigDecimal 版本因唯一非测试消费方 `Migration6To7`（历史 migration）依赖 BigDecimal
 * 算术、且 `java.math.BigDecimal` 不可迁 commonMain，保留在 core:common（Android 侧）。
 *
 * ⚠️ 文件名刻意为 `BigDecimalExt.kt`（而非 `Number.kt`）：Kotlin 顶层函数按文件名生成 facade 类，
 * 若与 shared 的 `Number.kt`（同包 `core.common.ext`）同名，会都编译成 `NumberKt`，在同时依赖两模块的
 * 消费方（如 `core:database`）classpath 上互相遮蔽、致对方函数 unresolved。改名后 facade 为 `BigDecimalExtKt`，
 * 与 shared 的 `NumberKt` 不冲突；函数包名不变，消费方 `import ...ext.toBigDecimalOrZero` 零改动。
 */

/** 将 [String] 类型数字转换为 [BigDecimal]，为空或转换失败，返回值为 `0` 的 [BigDecimal] */
fun String?.toBigDecimalOrZero(): BigDecimal {
    return this?.toBigDecimalOrNull() ?: "0".toBigDecimal()
}

/** 将 [Number] 类型数字转换为 [BigDecimal]，为空或转换失败，返回值为 `0` 的 [BigDecimal] */
fun Number?.toBigDecimalOrZero(): BigDecimal {
    return this?.toString().toBigDecimalOrZero()
}
