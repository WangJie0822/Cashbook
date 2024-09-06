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