package cn.wj.android.cashbook.core.design.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp

@Composable
fun Empty(
    imagePainter: Painter,
    hintText: String,
    modifier: Modifier = Modifier,
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
        Text(text = hintText)
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