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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun Empty(
    modifier: Modifier = Modifier,
    imageResId: Int,
    hintResId: Int,
    buttonResId: Int = -1,
    onButtonClick: () -> Unit = {},
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(painter = painterResource(id = imageResId), contentDescription = null)
        Spacer(modifier = Modifier
            .fillMaxWidth()
            .height(16.dp))
        Text(text = stringResource(id = hintResId))
        if (buttonResId != -1) {
            Spacer(modifier = Modifier
                .fillMaxWidth()
                .height(16.dp))
            FilledTonalButton(onClick = onButtonClick) {
                Text(text = stringResource(id = buttonResId))
            }
        }
    }
}