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

package cn.wj.android.cashbook.core.common.model

import cn.wj.android.cashbook.core.common.ext.logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

typealias DataVersion = MutableStateFlow<Int>

fun StateFlow<Int>.updateVersion() {
    if (this is DataVersion) {
        val oldValue = value
        val newValue = oldValue + 1
        val result = tryEmit(newValue)
        logger().i("updateVersion() oldValue = <$oldValue>, newValue = <$newValue>, result = <$result>")
    }
}

fun dataVersion(): MutableStateFlow<Int> = MutableStateFlow(0)

val recordDataVersion: StateFlow<Int> = dataVersion()
val assetDataVersion: StateFlow<Int> = dataVersion()
val bookDataVersion: StateFlow<Int> = dataVersion()
val typeDataVersion: StateFlow<Int> = dataVersion()
val tagDataVersion: StateFlow<Int> = dataVersion()
