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

package cn.wj.android.cashbook.core.network.okhttp

import android.util.Log
import okhttp3.internal.platform.Platform

/** 动态参数类型 */
typealias DynamicParams = () -> String

/** 日志打印接口 */
typealias InterceptorLogger = (String) -> Unit

/** 默认日志打印接口 */
val DEFAULT_LOGGER: InterceptorLogger by lazy(LazyThreadSafetyMode.NONE) {
    { msg ->
        Platform.get().log(Log.DEBUG, msg, null)
    }
}
