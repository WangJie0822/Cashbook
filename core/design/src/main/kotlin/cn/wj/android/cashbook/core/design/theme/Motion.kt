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

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

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
) {
    /**
     * 根据用户的"减少动画"偏好返回实际动画时长
     *
     * 当 [reduced] 为 true 时返回 0，tween/animate* 将瞬时完成，
     * 避免对动效敏感用户造成干扰；否则返回 [defaultMs]
     */
    fun effectiveDuration(defaultMs: Int, reduced: Boolean): Int =
        if (reduced) 0 else defaultMs
}

val LocalMotion = staticCompositionLocalOf { CbMotion() }

/**
 * 读取系统"减少动画"偏好
 *
 * Android 侧没有独立的 prefers-reduced-motion 开关，
 * 标准做法是读 [Settings.Global.ANIMATOR_DURATION_SCALE]：
 * 当辅助功能"移除动画"开启、或开发者选项把动画缩放设为 0 时，该值为 0
 *
 * 仅在 composition 初始化时读一次，避免每帧访问 ContentResolver；
 * 如需实时响应设置变化，用户重开入口即可
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
        scale == 0f
    }
}
