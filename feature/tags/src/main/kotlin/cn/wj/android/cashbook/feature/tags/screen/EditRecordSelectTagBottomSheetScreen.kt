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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.design.component.CbHorizontalDivider
import cn.wj.android.cashbook.core.design.component.CbTextButton
import cn.wj.android.cashbook.core.design.component.Empty
import cn.wj.android.cashbook.core.design.preview.PreviewTheme
import cn.wj.android.cashbook.core.model.model.Selectable
import cn.wj.android.cashbook.core.model.model.TagModel
import cn.wj.android.cashbook.core.ui.DevicePreviews
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.tags.dialog.EditTagDialogRoute
import cn.wj.android.cashbook.feature.tags.preview.EditRecordSelectTagBottomSheetListPreviewParameterProvider
import cn.wj.android.cashbook.feature.tags.viewmodel.EditRecordSelectTagBottomSheetViewModel

/**
 * 编辑记录界面选择标签抽屉
 *
 * @param selectedTagIdList 已选择标签id列表
 * @param onTagIdListChange 已选择标签id列表变化回调
 */
@Composable
internal fun EditRecordSelectTagBottomSheetRoute(
    selectedTagIdList: List<Long>,
    onTagIdListChange: (List<Long>) -> Unit,
    onRequestDismissSheet: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditRecordSelectTagBottomSheetViewModel = hiltViewModel<EditRecordSelectTagBottomSheetViewModel>().apply {
        updateSelectedTags(selectedTagIdList)
    },
) {
    // 显示列表数据
    val tagList by viewModel.tagListData.collectAsStateWithLifecycle()

    EditRecordSelectTagBottomSheetScreen(
        dialogState = viewModel.dialogState,
        onRequestDismissDialog = viewModel::dismissDialog,
        tagList = tagList,
        onAddTagClick = viewModel::displayAddTagDialog,
        onDoneClick = {
            onRequestDismissSheet()
            val tagIds = tagList.filter { it.selected }.map { it.data.id }
            onTagIdListChange(tagIds)
        },
        onTagItemClick = viewModel::updateSelectedTagList,
        modifier = modifier,
    )
}

/**
 * 编辑记录界面选择标签抽屉
 *
 * @param dialogState 弹窗状态
 * @param onRequestDismissDialog 隐藏弹窗
 * @param tagList 标签列表数据
 * @param onAddTagClick 添加标签点击回调
 * @param onTagItemClick 标签列表 item 点击回调
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EditRecordSelectTagBottomSheetScreen(
    dialogState: DialogState,
    onRequestDismissDialog: () -> Unit,
    tagList: List<Selectable<TagModel>>,
    onAddTagClick: () -> Unit,
    onDoneClick: () -> Unit,
    onTagItemClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        if (dialogState is DialogState.Shown<*>) {
            EditTagDialogRoute(
                tagModel = null,
                onRequestDismissDialog = onRequestDismissDialog,
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            content = {
                ConstraintLayout(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    val (title, subTitle, add, done) = createRefs()
                    Text(
                        text = stringResource(id = R.string.please_select_tags),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.constrainAs(title) {
                            top.linkTo(parent.top)
                            start.linkTo(parent.start)
                        },
                    )
                    Text(
                        text = stringResource(id = R.string.allow_multiple_choices),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.constrainAs(subTitle) {
                            top.linkTo(title.bottom, 8.dp)
                            start.linkTo(parent.start)
                        },
                    )
                    CbTextButton(
                        modifier = Modifier.constrainAs(add) {
                            top.linkTo(parent.top)
                            end.linkTo(done.start)
                            bottom.linkTo(parent.bottom)
                        },
                        onClick = onAddTagClick,
                    ) {
                        Text(
                            text = stringResource(id = R.string.add),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    CbTextButton(
                        modifier = Modifier.constrainAs(done) {
                            top.linkTo(parent.top)
                            end.linkTo(parent.end)
                            bottom.linkTo(parent.bottom)
                        },
                        onClick = onDoneClick,
                    ) {
                        Text(
                            text = stringResource(id = R.string.done),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                CbHorizontalDivider()

                if (tagList.isEmpty()) {
                    Empty(
                        hintText = stringResource(id = R.string.tags_empty_hint),
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    FlowRow(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .defaultMinSize(minHeight = 200.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        tagList.forEach {
                            ElevatedFilterChip(
                                selected = it.selected,
                                onClick = { onTagItemClick(it.data.id) },
                                label = { Text(text = it.data.name) },
                            )
                        }
                    }
                }
            },
        )
    }
}

@DevicePreviews
@Composable
fun EditRecordSelectTagBottomSheetScreenNoTags() {
    PreviewTheme(defaultEmptyImagePainter = painterResource(id = R.drawable.vector_no_data_200)) {
        EditRecordSelectTagBottomSheetScreen(
            dialogState = DialogState.Dismiss,
            onRequestDismissDialog = {},
            tagList = emptyList(),
            onAddTagClick = {},
            onDoneClick = {},
            onTagItemClick = {},
        )
    }
}

@DevicePreviews
@Composable
fun EditRecordSelectTagBottomSheetScreenWithList(
    @PreviewParameter(EditRecordSelectTagBottomSheetListPreviewParameterProvider::class)
    tagList: List<Selectable<TagModel>>,
) {
    PreviewTheme {
        EditRecordSelectTagBottomSheetScreen(
            dialogState = DialogState.Dismiss,
            onRequestDismissDialog = {},
            tagList = tagList,
            onAddTagClick = {},
            onDoneClick = {},
            onTagItemClick = {},
        )
    }
}
