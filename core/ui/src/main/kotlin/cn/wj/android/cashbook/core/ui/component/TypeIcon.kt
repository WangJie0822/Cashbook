package cn.wj.android.cashbook.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import cn.wj.android.cashbook.core.design.icon.CashbookIcons
import cn.wj.android.cashbook.core.design.theme.fixedContentColorFor

@Composable
fun TypeIcon(
    painter: Painter,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = fixedContentColorFor(backgroundColor = containerColor),
    showMore: Boolean = false,
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .background(
                color = containerColor,
                shape = CircleShape
            ),
    ) {
        Icon(
            painter = painter,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier
                .align(Alignment.Center)
                .size(16.dp),
        )
        if (showMore) {
            // 横向菜单标记，一级分类有二级分类时显示
            Icon(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(12.dp)
                    .background(color = contentColor, shape = CircleShape)
                    .padding(1.dp)
                    .background(color = containerColor, shape = CircleShape),
                imageVector = CashbookIcons.MoreHoriz,
                contentDescription = null,
                tint = contentColor
            )
        }
    }
}