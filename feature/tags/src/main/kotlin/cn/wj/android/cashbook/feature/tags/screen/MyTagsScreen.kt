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

package cn.wj.android.cashbook.feature.tags.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.design.component.CbIconButton
import cn.wj.android.cashbook.core.design.component.CbScaffold
import cn.wj.android.cashbook.core.design.component.CbTopAppBar
import cn.wj.android.cashbook.core.design.component.Empty
import cn.wj.android.cashbook.core.design.icon.CbIcons
import cn.wj.android.cashbook.core.model.model.TagModel
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.tags.dialog.DeleteTagDialogRoute
import cn.wj.android.cashbook.feature.tags.dialog.EditTagDialogRoute
import cn.wj.android.cashbook.feature.tags.model.TagDialogState
import cn.wj.android.cashbook.feature.tags.viewmodel.MyTagsViewModel

/**
 * 我的标签
 *
 * @param onRequestPopBackStack 导航到上一级
 * @param onRequestNaviToTagStatistic 导航到标签数据分析
 */
@Composable
internal fun MyTagsRoute(
    onRequestNaviToTagStatistic: (Long) -> Unit,
    onRequestPopBackStack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyTagsViewModel = hiltViewModel(),
) {
    // 标签列表
    val tagList by viewModel.tagListData.collectAsStateWithLifecycle()

    MyTagsScreen(
        dialogState = viewModel.dialogState,
        onRequestDismissDialog = viewModel::dismissDialog,
        tagList = tagList,
        onEditTagClick = viewModel::showEditTagDialog,
        onDeleteTagClick = viewModel::showDeleteTagDialog,
        onTagStatisticClick = onRequestNaviToTagStatistic,
        onBackClick = onRequestPopBackStack,
        modifier = modifier,
    )
}

/**
 * 我的标签
 *
 * @param dialogState 弹窗状态
 * @param onRequestDismissDialog 隐藏弹窗
 * @param tagList 标签列表数据
 * @param onEditTagClick 编辑标签点击回调
 * @param onDeleteTagClick 删除标签点击回调
 * @param onTagStatisticClick 统计数据点击回调
 * @param onBackClick 返回点击回调
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun MyTagsScreen(
    dialogState: DialogState,
    onRequestDismissDialog: () -> Unit,
    tagList: List<TagModel>,
    onEditTagClick: (TagModel?) -> Unit,
    onDeleteTagClick: (TagModel) -> Unit,
    onTagStatisticClick: (Long) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CbScaffold(
        modifier = modifier,
        topBar = {
            CbTopAppBar(
                onBackClick = onBackClick,
                title = { Text(text = stringResource(id = R.string.my_tags)) },
                actions = {
                    CbIconButton(onClick = { onEditTagClick(null) }) {
                        Icon(imageVector = CbIcons.Add, contentDescription = null)
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(top = 8.dp, start = 16.dp, end = 16.dp),
        ) {
            // 编辑标签弹窗
            if (dialogState is DialogState.Shown<*>) {
                when (val data = dialogState.data) {
                    is TagDialogState.Edit -> {
                        EditTagDialogRoute(
                            tagModel = data.tag,
                            onRequestDismissDialog = onRequestDismissDialog,
                        )
                    }

                    is TagDialogState.Delete -> {
                        DeleteTagDialogRoute(
                            tagModel = data.tag,
                            onRequestDismissDialog = onRequestDismissDialog,
                        )
                    }
                }
            }

            // 空布局
            if (tagList.isEmpty()) {
                Empty(
                    hintText = stringResource(id = R.string.tags_empty_hint),
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                // 标签列表
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    tagList.forEach {
                        Box {
                            var expanded by remember { mutableStateOf(false) }
                            ElevatedFilterChip(
                                selected = true,
                                onClick = { expanded = true },
                                label = { Text(text = it.name) },
                            )
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(text = stringResource(id = R.string.modify)) },
                                    onClick = {
                                        expanded = false
                                        onEditTagClick(it)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(text = stringResource(id = R.string.delete)) },
                                    onClick = {
                                        expanded = false
                                        onDeleteTagClick(it)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(text = stringResource(id = R.string.statistic_data)) },
                                    onClick = {
                                        expanded = false
                                        onTagStatisticClick(it.id)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
