package cn.wj.android.cashbook.core.design.component

import androidx.compose.material3.Divider
import androidx.compose.material3.DividerDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

@Composable
fun CommonDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = DividerDefaults.Thickness,
    color: Color = DividerDefaults.color.copy(alpha = 0.3f),
) {
    Divider(
        modifier = modifier,
        thickness = thickness,
        color = color,
    )
}