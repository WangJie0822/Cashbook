package cn.wj.android.cashbook.core.design.component

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cn.wj.android.cashbook.core.design.theme.CashbookTheme

@Composable
fun Empty(
    hintText: String,
    modifier: Modifier = Modifier,
    imagePainter: Painter = LocalDefaultEmptyImagePainter.current,
    buttonText: String? = null,
    onButtonClick: () -> Unit = {},
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = imagePainter,
            contentDescription = null,
        )
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp),
        )
        Text(text = hintText, color = LocalContentColor.current.copy(0.5f))
        if (null != buttonText) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
            )
            FilledTonalButton(onClick = onButtonClick) {
                Text(text = buttonText)
            }
        }
    }
}

@Composable
@NonRestartableComposable
fun Empty(
    imageVector: ImageVector,
    hintText: String,
    modifier: Modifier = Modifier,
    buttonText: String? = null,
    onButtonClick: () -> Unit = {},
) = Empty(
    imagePainter = rememberVectorPainter(image = imageVector),
    hintText = hintText,
    modifier = modifier,
    buttonText = buttonText,
    onButtonClick = onButtonClick
)

@Composable
@NonRestartableComposable
fun Empty(
    bitmap: ImageBitmap,
    hintText: String,
    modifier: Modifier = Modifier,
    filterQuality: FilterQuality = DrawScope.DefaultFilterQuality,
    buttonText: String? = null,
    onButtonClick: () -> Unit = {},
) = Empty(
    imagePainter = remember(bitmap) { BitmapPainter(bitmap, filterQuality = filterQuality) },
    hintText = hintText,
    modifier = modifier,
    buttonText = buttonText,
    onButtonClick = onButtonClick
)

/**
 * 这个 [CompositionLocal] 用于提供一个 [Painter]
 *
 * ```
 * CompositionLocalProvider(
 *     LocalEmptyImagePainter provides painterResource(id = R.drawable.xxxx)
 * ) { }
 * ```
 *
 * 再使用 [Painter] 显示无数据界面
 */
val LocalDefaultEmptyImagePainter =
    staticCompositionLocalOf<Painter> { error("No Empty Image Painter provided") }

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun EmptyPreview() {
    CashbookTheme {
        CashbookGradientBackground {
            Empty(
                imageVector = Icons.Filled.HourglassEmpty,
                hintText = "无数据提示文本",
                buttonText = "按钮文本"
            )
        }
    }
}