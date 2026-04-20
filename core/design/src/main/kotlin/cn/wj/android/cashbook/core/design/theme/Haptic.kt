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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * 将无参回调包装为带触觉反馈的版本
 *
 * 主要用于按钮/FAB/选择等主动交互路径，补齐 Material 手感
 *
 * @param type 触觉类型，默认 [HapticFeedbackType.TextHandleMove]（轻微的"点击感"）
 * @param onClick 原始回调
 */
@Composable
fun rememberHapticOnClick(
    type: HapticFeedbackType = HapticFeedbackType.TextHandleMove,
    onClick: () -> Unit,
): () -> Unit {
    val haptic = LocalHapticFeedback.current
    return remember(haptic, type, onClick) {
        { performHapticThen(haptic, type, onClick) }
    }
}

private fun performHapticThen(
    haptic: HapticFeedback,
    type: HapticFeedbackType,
    block: () -> Unit,
) {
    haptic.performHapticFeedback(type)
    block()
}
