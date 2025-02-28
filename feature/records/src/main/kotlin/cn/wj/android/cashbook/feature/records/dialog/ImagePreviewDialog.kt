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

package cn.wj.android.cashbook.feature.records.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cn.wj.android.cashbook.core.design.component.CbIconButton
import cn.wj.android.cashbook.core.design.icon.CbIcons
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.records.model.ImageViewModel
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter

/**
 * 图片预览弹窗
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2025/2/27
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun ImagePreviewDialog(
    onRequestDismissDialog: () -> Unit,
    list: List<ImageViewModel>,
    index: Int = 0,
) {
    Dialog(
        onDismissRequest = onRequestDismissDialog,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.surface),
        ) {
            val containerWidthDp = LocalConfiguration.current.screenWidthDp.dp
            HorizontalUncontainedCarousel(
                state = rememberCarouselState(index) { list.size },
                itemWidth = containerWidthDp,
            ) { index ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onRequestDismissDialog.invoke() },
                ) {
                    val placeholder = rememberAsyncImagePainter(list[index].bitmap)
                    AsyncImage(
                        model = list[index].path,
                        placeholder = placeholder,
                        error = placeholder,
                        contentScale = ContentScale.Crop,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Text(
                        text = "${index + 1}/${list.size}",
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 4.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(size = 8.dp),
                            )
                            .padding(horizontal = 8.dp),
                    )
                }
            }
            Text(
                text = stringResource(R.string.image_preview),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp),
            )
            CbIconButton(
                onClick = onRequestDismissDialog,
                modifier = Modifier
                    .align(Alignment.TopEnd),
            ) {
                Icon(imageVector = CbIcons.Cancel, contentDescription = null)
            }
        }
    }
}
