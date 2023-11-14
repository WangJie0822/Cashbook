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

package cn.wj.android.cashbook.core.ui

import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

/**
 * 这个 [CompositionLocal] 用于提供一个 [NavController]
 *
 * ```
 * CompositionLocalProvider(
 *     LocalNavController provides navController
 * ) { }
 * ```
 *
 * 再使用 [NavController] 处理导航事件
 */
val LocalNavController =
    staticCompositionLocalOf<NavController> { error("No NavController provided") }

/**
 * 安全的返回上一级页面
 * - 防止在二级页面快速点击返回时，将所有页面出栈，导致应用无法显示
 */
fun NavController.popBackStackSafety(): Boolean {
    return if (currentDestination == graph.findStartDestination()) {
        false
    } else {
        popBackStack()
    }
}
