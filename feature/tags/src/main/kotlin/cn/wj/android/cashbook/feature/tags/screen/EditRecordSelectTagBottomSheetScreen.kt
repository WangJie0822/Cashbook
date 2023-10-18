package cn.wj.android.cashbook.feature.tags.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.design.component.CommonDivider
import cn.wj.android.cashbook.core.design.component.Empty
import cn.wj.android.cashbook.core.design.theme.PreviewTheme
import cn.wj.android.cashbook.core.model.model.Selectable
import cn.wj.android.cashbook.core.model.model.TagModel
import cn.wj.android.cashbook.core.ui.DevicePreviews
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.tags.dialog.EditTagDialogRoute
import cn.wj.android.cashbook.feature.tags.viewmodel.EditRecordSelectTagBottomSheetViewModel

/**
 * 编辑记录界面选择标签抽屉
 *
 * @param selectedTagIdList 已选择标签id列表
 * @param onTagIdListChange 已选择标签id列表变化回调
 */
@Composable
internal fun EditRecordSelectTagBottomSheetRoute(
    modifier: Modifier = Modifier,
    selectedTagIdList: List<Long> = emptyList(),
    onTagIdListChange: (List<Long>) -> Unit = {},
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
        onTagItemClick = { tagEntity ->
            viewModel.updateSelectedTagList(
                id = tagEntity.id,
                onResult = { selectedList ->
                    onTagIdListChange(selectedList)
                },
            )
        },
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
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun EditRecordSelectTagBottomSheetScreen(
    dialogState: DialogState,
    onRequestDismissDialog: () -> Unit,
    tagList: List<Selectable<TagModel>>,
    onAddTagClick: () -> Unit,
    onTagItemClick: (TagModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = {
                ConstraintLayout(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    val (title, subTitle, add) = createRefs()
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
                    TextButton(
                        modifier = Modifier.constrainAs(add) {
                            top.linkTo(parent.top)
                            end.linkTo(parent.end)
                            bottom.linkTo(parent.bottom)
                        },
                        onClick = onAddTagClick,
                    ) {
                        Text(
                            text = stringResource(id = R.string.add),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                CommonDivider()

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
                            FilterChip(
                                selected = it.selected,
                                onClick = { onTagItemClick(it.data) },
                                label = { Text(text = it.data.name) },
                            )
                        }
                    }
                }
            },
        )
        if (dialogState is DialogState.Shown<*>) {
            EditTagDialogRoute(
                tagModel = null,
                onRequestDismissDialog = onRequestDismissDialog,
            )
        }
    }
}

@DevicePreviews
@Composable
private fun EditRecordSelectTagBottomSheetScreenPreview() {
    PreviewTheme(
        defaultEmptyImagePainter = painterResource(id = R.drawable.vector_no_data_200),
    ) {
        PreviewTheme(
            defaultEmptyImagePainter = painterResource(id = R.drawable.vector_no_data_200),
        ) {
            EditRecordSelectTagBottomSheetRoute()
        }
    }
}