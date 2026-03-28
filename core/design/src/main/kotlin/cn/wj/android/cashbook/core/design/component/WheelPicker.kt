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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 滚轮选择器
 *
 * @param items 选项列表
 * @param selectedIndex 当前选中索引
 * @param onItemSelected 选中回调
 * @param modifier Modifier
 * @param visibleItemCount 可见项数（奇数）
 * @param itemHeight 每项高度
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CbWheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    visibleItemCount: Int = 3,
    itemHeight: Dp = 40.dp,
) {
    val halfCount = visibleItemCount / 2
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = selectedIndex,
    )

    // 滚动停止后回调选中项
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress && items.isNotEmpty()) {
            val index = listState.firstVisibleItemIndex.coerceIn(0, items.size - 1)
            if (index != selectedIndex) {
                onItemSelected(index)
            }
        }
    }

    // 外部 selectedIndex 变化时滚动到对应位置
    LaunchedEffect(selectedIndex) {
        if (selectedIndex in items.indices && listState.firstVisibleItemIndex != selectedIndex) {
            listState.scrollToItem(selectedIndex)
        }
    }

    val totalHeight = itemHeight * visibleItemCount

    Box(
        modifier = modifier.height(totalHeight),
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
            modifier = Modifier
                .height(totalHeight)
                .fillMaxWidth()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    drawContent()
                    // 上下渐变遮罩
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.3f to Color.Black,
                            0.7f to Color.Black,
                            1f to Color.Transparent,
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                },
        ) {
            // 上方占位
            items(halfCount) {
                Box(modifier = Modifier.height(itemHeight))
            }
            items(items.size) { index ->
                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    val isSelected = index == selectedIndex
                    Text(
                        text = items[index],
                        textAlign = TextAlign.Center,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        },
                        style = if (isSelected) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.bodyMedium
                        },
                    )
                }
            }
            // 下方占位
            items(halfCount) {
                Box(modifier = Modifier.height(itemHeight))
            }
        }

        // 选中行指示线
        CbHorizontalDivider(
            modifier = Modifier.align(Alignment.Center)
                .then(Modifier.graphicsLayer { translationY = -(itemHeight.toPx() / 2) }),
        )
        CbHorizontalDivider(
            modifier = Modifier.align(Alignment.Center)
                .then(Modifier.graphicsLayer { translationY = (itemHeight.toPx() / 2) }),
        )
    }
}
