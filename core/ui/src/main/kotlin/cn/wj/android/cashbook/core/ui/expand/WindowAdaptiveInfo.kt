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

package cn.wj.android.cashbook.core.ui.expand

import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.window.core.layout.WindowWidthSizeClass

/** 账本背景尺寸比例 */
val WindowAdaptiveInfo.bookImageRatio: Float
    get() = this.windowSizeClass.windowWidthSizeClass.bookImageRatio

/** 账本背景尺寸比例 */
val WindowWidthSizeClass.bookImageRatio: Float
    get() = when (this) {
        WindowWidthSizeClass.EXPANDED -> {
            4f
        }

        WindowWidthSizeClass.MEDIUM -> {
            2.5f
        }

        else -> {
            1.5f
        }
    }
