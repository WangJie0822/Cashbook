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

import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * 设计系统线性进度条（不确定进度）：薄封装 Material3 [LinearProgressIndicator]，用于加载等场景。
 */
@Composable
fun CbLinearProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.linearColor,
) {
    LinearProgressIndicator(
        modifier = modifier,
        color = color,
    )
}

/**
 * 设计系统线性进度条（确定进度）：薄封装 Material3 [LinearProgressIndicator]，用于预算进度等场景。
 *
 * @param progress 进度提供器，返回 0f~1f
 */
@Composable
fun CbLinearProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.linearColor,
) {
    LinearProgressIndicator(
        progress = progress,
        modifier = modifier,
        color = color,
    )
}
