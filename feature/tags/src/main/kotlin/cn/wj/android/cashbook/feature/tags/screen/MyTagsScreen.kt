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
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.design.component.CbIconButton
import cn.wj.android.cashbook.core.design.component.CbScaffold
import cn.wj.android.cashbook.core.design.component.CbTopAppBar
import cn.wj.android.cashbook.core.design.component.Empty
import cn.wj.android.cashbook.core.design.icon.CbIcons
import cn.wj.android.cashbook.core.design.preview.PreviewDropdownMenu
import cn.wj.android.cashbook.core.design.preview.PreviewTheme
import cn.wj.android.cashbook.core.model.model.TagModel
import cn.wj.android.cashbook.core.ui.DevicePreviews
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.tags.dialog.DeleteTagDialogRoute
import cn.wj.android.cashbook.feature.tags.dialog.EditTagDialogRoute
import cn.wj.android.cashbook.feature.tags.model.TagDialogState
import cn.wj.android.cashbook.feature.tags.preview.MyTagsListPreviewParameterProvider
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
        onRequestSwitchTagInvisible = viewModel::switchInvisibleState,
        onRequestShowEditTagDialog = viewModel::showEditTagDialog,
        onRequestShowDeleteTagDialog = viewModel::showDeleteTagDialog,
        onRequestNaviToTagStatistic = onRequestNaviToTagStatistic,
        onRequestPopBackStack = onRequestPopBackStack,
        modifier = modifier,
    )
}

/**
 * 我的标签
 *
 * @param dialogState 弹窗状态
 * @param onRequestDismissDialog 隐藏弹窗
 * @param tagList 标签列表数据
 * @param onRequestSwitchTagInvisible 切换标签隐藏状态
 * @param onRequestShowEditTagDialog 显示编辑弹窗
 * @param onRequestShowDeleteTagDialog 显示删除弹窗
 * @param onRequestNaviToTagStatistic 调整标签数据统计
 * @param onRequestPopBackStack 返回上一页
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun MyTagsScreen(
    dialogState: DialogState,
    onRequestDismissDialog: () -> Unit,
    tagList: List<TagModel>,
    onRequestSwitchTagInvisible: (TagModel) -> Unit,
    onRequestShowEditTagDialog: (TagModel?) -> Unit,
    onRequestShowDeleteTagDialog: (TagModel) -> Unit,
    onRequestNaviToTagStatistic: (Long) -> Unit,
    onRequestPopBackStack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CbScaffold(
        modifier = modifier,
        topBar = {
            CbTopAppBar(
                onBackClick = onRequestPopBackStack,
                title = { Text(text = stringResource(id = R.string.my_tags)) },
                actions = {
                    // 添加标签按钮
                    CbIconButton(onClick = { onRequestShowEditTagDialog(null) }) {
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

            if (tagList.isEmpty()) {
                // 空布局
                Empty(
                    hintText = stringResource(id = R.string.tags_empty_hint),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                // 标签列表
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    tagList.forEach { tagModel ->
                        Box {
                            var expanded by remember { mutableStateOf(false) }
                            ElevatedFilterChip(
                                selected = !tagModel.invisible,
                                onClick = { expanded = true },
                                label = { Text(text = tagModel.name) },
                            )
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                            ) {
                                TagDropdownMenuContent(
                                    invisible = tagModel.invisible,
                                    onInvisibleClick = {
                                        onRequestSwitchTagInvisible(tagModel)
                                        expanded = false
                                    },
                                    onEditTagClick = {
                                        onRequestShowEditTagDialog(tagModel)
                                        expanded = false
                                    },
                                    onDeleteTagClick = {
                                        onRequestShowDeleteTagDialog(tagModel)
                                        expanded = false
                                    },
                                    onTagStatisticClick = {
                                        onRequestNaviToTagStatistic(tagModel.id)
                                        expanded = false
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

/**
 * 标签下拉菜单
 *
 * @param invisible 标签是否隐藏
 * @param onInvisibleClick 隐藏标签点击
 * @param onEditTagClick 编辑标签点击
 * @param onDeleteTagClick 删除标签点击
 * @param onTagStatisticClick 统计数据点击
 */
@Composable
private fun TagDropdownMenuContent(
    invisible: Boolean,
    onInvisibleClick: () -> Unit,
    onEditTagClick: () -> Unit,
    onDeleteTagClick: () -> Unit,
    onTagStatisticClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(text = stringResource(id = R.string.modify)) },
        onClick = {
            onEditTagClick()
        },
    )
    DropdownMenuItem(
        text = { Text(text = stringResource(id = R.string.delete)) },
        onClick = {
            onDeleteTagClick()
        },
    )
    DropdownMenuItem(
        text = {
            Text(
                text = stringResource(
                    id = if (invisible) {
                        R.string.visible_tag
                    } else {
                        R.string.invisible_tag
                    },
                ),
            )
        },
        onClick = {
            onInvisibleClick()
        },
    )
    DropdownMenuItem(
        text = { Text(text = stringResource(id = R.string.statistic_data)) },
        onClick = {
            onTagStatisticClick()
        },
    )
}

@DevicePreviews
@Composable
fun MyTagsScreenWithList(
    @PreviewParameter(MyTagsListPreviewParameterProvider::class)
    tagList: List<TagModel>,
) {
    PreviewTheme(defaultEmptyImagePainter = painterResource(id = R.drawable.vector_no_data_200),) {
        MyTagsScreen(
            dialogState = DialogState.Dismiss,
            onRequestDismissDialog = {},
            tagList = tagList,
            onRequestSwitchTagInvisible = {},
            onRequestShowEditTagDialog = {},
            onRequestShowDeleteTagDialog = {},
            onRequestNaviToTagStatistic = {},
            onRequestPopBackStack = {},
        )
    }
}

@PreviewLightDark
@Composable
fun MyTagsScreenTagDropDownMenu() {
    PreviewTheme {
        Row {
            PreviewDropdownMenu {
                TagDropdownMenuContent(
                    invisible = true,
                    onInvisibleClick = {},
                    onEditTagClick = {},
                    onDeleteTagClick = {},
                    onTagStatisticClick = {},
                )
            }
            PreviewDropdownMenu {
                TagDropdownMenuContent(
                    invisible = false,
                    onInvisibleClick = {},
                    onEditTagClick = {},
                    onDeleteTagClick = {},
                    onTagStatisticClick = {},
                )
            }
        }
    }
}
