package cn.wj.android.cashbook.feature.tags.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import cn.wj.android.cashbook.core.model.entity.TagEntity
import cn.wj.android.cashbook.core.ui.DevicePreviews
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.tags.viewmodel.EditRecordSelectTagBottomSheetViewModel
import com.google.accompanist.flowlayout.FlowRow

@Composable
internal fun EditRecordSelectTagBottomSheetRoute(
    selectedTagIdList: List<Long>,
    onTagIdListChange: (List<Long>) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditRecordSelectTagBottomSheetViewModel = hiltViewModel<EditRecordSelectTagBottomSheetViewModel>().apply {
        updateSelectedTags(selectedTagIdList)
    },
) {

    // 显示列表数据
    val tagList by viewModel.tagListData.collectAsStateWithLifecycle()

    EditRecordSelectTagBottomSheetScreen(
        tagList = tagList,
        onAddTagClick = viewModel::onAddClick,
        onAddTagConfirm = viewModel::addTag,
        onTagItemClick = { tagEntity ->
            viewModel.onTagItemClick(
                tag = tagEntity,
                onResult = { selectedList ->
                    onTagIdListChange(selectedList)
                },
            )
        },
        dialogState = viewModel.dialogState,
        dismissDialog = viewModel::dismissDialog,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditRecordSelectTagBottomSheetScreen(
    tagList: List<TagEntity>,
    onAddTagClick: () -> Unit,
    onAddTagConfirm: (TagEntity) -> Unit,
    onTagItemClick: (TagEntity) -> Unit,
    dialogState: DialogState,
    dismissDialog: () -> Unit,
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
                    Empty(hintText = stringResource(id = R.string.tags_empty_hint))
                } else {
                    FlowRow(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .defaultMinSize(minHeight = 200.dp),
                        mainAxisSpacing = 8.dp,
                        crossAxisSpacing = 4.dp,
                    ) {
                        tagList.forEach {
                            FilterChip(
                                selected = it.selected,
                                onClick = { onTagItemClick(it) },
                                label = { Text(text = it.name) },
                            )
                        }
                    }
                }
            },
        )
        if (dialogState is DialogState.Shown<*>) {
            EditTagDialog(
                tagEntity = null,
                onConfirm = onAddTagConfirm,
                onDismiss = dismissDialog,
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
        EditRecordSelectTagBottomSheetScreen(
            tagList = listOf(
                TagEntity(id = 1L, name = "标签1", selected = false),
                TagEntity(id = 2L, name = "标签1标签1", selected = false),
                TagEntity(id = 3L, name = "标签1标签1标签1", selected = false),
                TagEntity(id = 4L, name = "标签1标签1标签1标签1", selected = false),
                TagEntity(id = 5L, name = "标签1标签1标签1标签1标签1", selected = false),
                TagEntity(id = 6L, name = "标签1标签1标签1标签1标签1标签1", selected = false),
                TagEntity(id = 7L, name = "标签2", selected = false),
            ),
            onAddTagClick = {},
            onAddTagConfirm = {},
            onTagItemClick = {},
            dialogState = DialogState.Dismiss,
            dismissDialog = {},
        )
    }
}

@DevicePreviews
@Composable
private fun EditRecordSelectTagBottomSheetScreenEmptyPreview() {
    PreviewTheme(
        defaultEmptyImagePainter = painterResource(id = R.drawable.vector_no_data_200),
    ) {
        EditRecordSelectTagBottomSheetScreen(
            tagList = listOf(),
            onAddTagClick = {},
            onAddTagConfirm = {},
            onTagItemClick = {},
            dialogState = DialogState.Dismiss,
            dismissDialog = {},
        )
    }
}