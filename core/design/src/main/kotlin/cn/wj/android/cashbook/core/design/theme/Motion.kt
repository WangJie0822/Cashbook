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

package cn.wj.android.cashbook.core.design.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 动效 Design Token
 *
 * 对齐 Material 3 的 duration / easing 规范：
 *
 * - [durationShort]  200ms — 按钮态变化、Ripple
 * - [durationMedium] 250ms — 容器切换、图表入场
 * - [durationLong]   400ms — 页面大切换
 *
 * easing 提供两档：
 * - [emphasizedEasing]：Material 3 emphasized 曲线，推荐用于品牌主路径
 * - [standardEasing]：兼容老代码的标准曲线
 */
@Immutable
data class CbMotion(
    val durationShort: Int = 200,
    val durationMedium: Int = 250,
    val durationLong: Int = 400,
    val emphasizedEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
    val standardEasing: Easing = FastOutSlowInEasing,
)

val LocalMotion = staticCompositionLocalOf { CbMotion() }
