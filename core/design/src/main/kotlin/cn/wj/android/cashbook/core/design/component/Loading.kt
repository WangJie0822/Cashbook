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

package cn.wj.android.cashbook.core.design.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cn.wj.android.cashbook.core.design.theme.CashbookTheme

@Composable
fun Loading(
    modifier: Modifier = Modifier,
    hintText: String = LocalDefaultLoadingHint.current,
) {
    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 120.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LinearProgressIndicator()
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp),
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
val LocalDefaultLoadingHint =
    staticCompositionLocalOf<String> { error("No Loading hint provided") }

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LoadingPreview() {
    CashbookTheme {
        CashbookGradientBackground {
            CompositionLocalProvider(
                LocalDefaultLoadingHint provides "数据加载中",
            ) {
                Loading()
            }
        }
    }
}
