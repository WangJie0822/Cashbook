package cn.wj.android.cashbook.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle

object TextStyles {

    val titleSmall: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.titleSmall
}