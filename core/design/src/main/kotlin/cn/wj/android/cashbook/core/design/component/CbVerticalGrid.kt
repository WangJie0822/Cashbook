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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * 横向网格布局
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/12/24
 */
@Composable
fun <T> CbVerticalGrid(
    columns: Int,
    items: List<T>,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    CbVerticalGridRowExpansion(
        columns,
        items,
        modifier,
        defaultExpandedIndex = null,
        expandedRowContent = {},
    ) { item, _ ->
        content(item)
    }
}

/**
 * 横向网格布局，行之间可以嵌套自定义内容
 *
 */
@Composable
fun <T> CbVerticalGridRowExpansion(
    columns: Int,
    items: List<T>,
    modifier: Modifier = Modifier,
    defaultExpandedIndex: Int? = null,
    expandedRowContent: @Composable (T) -> Unit = {},
    content: @Composable (T, () -> Unit) -> Unit,
) {
    var curExpandedIdx by remember { mutableStateOf(defaultExpandedIndex) }
    Column(
        modifier = modifier,
    ) {
        if (items.isEmpty()) {
            Spacer(modifier = Modifier.fillMaxWidth())
        } else {
            val fixedSize = if (items.size % columns != 0) {
                ((items.size / columns) + 1) * columns
            } else {
                items.size
            }
            val columnCount = fixedSize / columns
            for (col in 0 until columnCount) {
                var extend = false
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (r in 0 until columns) {
                        Box(modifier = Modifier.weight(1f)) {
                            val index = col * columns + r
                            if (index in items.indices) {
                                if (curExpandedIdx == index) {
                                    extend = true
                                }
                                content(items[index], { curExpandedIdx = index })
                            }
                        }
                    }
                }
                if (extend) {
                    expandedRowContent(items[curExpandedIdx!!])
                }
            }
        }
    }
}
