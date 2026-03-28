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

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 间距 Design Token
 *
 * > 通过 [LocalSpacing] 在 Compose 树中提供
 */
@Immutable
data class CbSpacing(
    /** 紧凑间距 */
    val extraSmall: Dp = 4.dp,
    /** 列表项间距、图标与文字间距 */
    val small: Dp = 8.dp,
    /** 卡片内边距、区块间距 */
    val medium: Dp = 16.dp,
    /** 区块分隔 */
    val large: Dp = 24.dp,
    /** 页面级间距 */
    val extraLarge: Dp = 32.dp,
)

val LocalSpacing = staticCompositionLocalOf { CbSpacing() }
