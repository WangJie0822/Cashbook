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

package cn.wj.android.cashbook.core.design.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SignalCellularNodata
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import cn.wj.android.cashbook.core.design.component.CashbookGradientBackground
import cn.wj.android.cashbook.core.design.component.LocalDefaultEmptyImagePainter
import cn.wj.android.cashbook.core.design.component.LocalDefaultLoadingHint
import cn.wj.android.cashbook.core.design.theme.CashbookTheme

@Composable
fun PreviewTheme(
    defaultEmptyImagePainter: Painter = rememberVectorPainter(image = Icons.Filled.SignalCellularNodata),
    content: @Composable () -> Unit,
) {
    CashbookTheme {
        CashbookGradientBackground {
            CompositionLocalProvider(
                LocalDefaultEmptyImagePainter provides defaultEmptyImagePainter,
                LocalDefaultLoadingHint provides "数据加载中",
                content = content,
            )
        }
    }
}

@Composable
fun PreviewDropdownMenu(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.0.dp,
        shadowElevation = 3.0.dp,
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .width(IntrinsicSize.Max),
            content = content,
        )
    }
}
