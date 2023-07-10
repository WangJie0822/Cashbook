package cn.wj.android.cashbook.core.design.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun Loading(
    modifier: Modifier = Modifier,
    hintText: String = LocalDefaultLoadingHintPainter.current,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp),
        )
        Text(text = hintText, color = LocalContentColor.current.copy(0.5f))
    }
}

/**
 * 这个 [CompositionLocal] 用于提供一个 [String]
 *
 * ```
 * CompositionLocalProvider(
 *     LocalDefaultLoadingHintPainter provides stringResource(id = R.string.xxxx)
 * ) { }
 * ```
 *
 * 再使用 [String] 显示加载中界面
 */
val LocalDefaultLoadingHintPainter =
    staticCompositionLocalOf<String> { error("No Loading hint provided") }