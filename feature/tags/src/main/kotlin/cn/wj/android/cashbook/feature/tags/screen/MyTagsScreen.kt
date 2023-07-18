package cn.wj.android.cashbook.feature.tags.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.design.component.CashbookScaffold
import cn.wj.android.cashbook.core.design.component.CashbookTopAppBar
import cn.wj.android.cashbook.core.design.component.Empty
import cn.wj.android.cashbook.core.design.icon.CashbookIcons
import cn.wj.android.cashbook.core.design.theme.PreviewTheme
import cn.wj.android.cashbook.core.model.model.TagModel
import cn.wj.android.cashbook.core.ui.DevicePreviews
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.tags.model.TagDialogState
import cn.wj.android.cashbook.feature.tags.viewmodel.MyTagsViewModel
import com.google.accompanist.flowlayout.FlowRow

@Composable
internal fun MyTagsRoute(
    onBackClick: () -> Unit,
    onTagStatisticClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyTagsViewModel = hiltViewModel(),
) {

    // 标签列表
    val tagList by viewModel.tagListData.collectAsStateWithLifecycle()

    MyTagsScreen(
        tagList = tagList,
        dialogState = viewModel.dialogState,
        showEditTagDialog = viewModel::showEditTagDialog,
        showDeleteTagDialog = viewModel::showDeleteTagDialog,
        dismissDialog = viewModel::dismissDialog,
        modifyTag = viewModel::modifyTag,
        deleteTag = viewModel::deleteTag,
        onBackClick = onBackClick,
        onTagStatisticClick = onTagStatisticClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MyTagsScreen(
    tagList: List<TagModel>,
    dialogState: DialogState,
    showEditTagDialog: (TagModel?) -> Unit,
    showDeleteTagDialog: (TagModel) -> Unit,
    dismissDialog: () -> Unit,
    modifyTag: (TagModel) -> Unit,
    deleteTag: (TagModel) -> Unit,
    onBackClick: () -> Unit,
    onTagStatisticClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {

    CashbookScaffold(
        modifier = modifier,
        topBar = {
            CashbookTopAppBar(
                text = stringResource(id = R.string.my_tags),
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = { showEditTagDialog(null) }) {
                        Icon(imageVector = CashbookIcons.Add, contentDescription = null)
                    }
                },
            )
        }
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
                        EditTagDialog(
                            tagEntity = data.tag,
                            onConfirm = modifyTag,
                            onDismiss = dismissDialog,
                        )
                    }

                    is TagDialogState.Delete -> {
                        DeleteTagDialog(
                            tagEntity = data.tag,
                            onConfirm = deleteTag,
                            onDismiss = dismissDialog,
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
                    mainAxisSpacing = 8.dp,
                    crossAxisSpacing = 4.dp,
                ) {
                    tagList.forEach {
                        Box {
                            var expanded by remember { mutableStateOf(false) }
                            FilterChip(
                                selected = true,
                                onClick = { expanded = true },
                                label = { Text(text = it.name) },
                            )
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }) {
                                DropdownMenuItem(
                                    text = { Text(text = stringResource(id = R.string.modify)) },
                                    onClick = {
                                        expanded = false
                                        showEditTagDialog(it)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(text = stringResource(id = R.string.delete)) },
                                    onClick = {
                                        expanded = false
                                        showDeleteTagDialog(it)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTagDialog(
    tagEntity: TagModel?,
    onConfirm: (TagModel) -> Unit,
    onDismiss: () -> Unit,
) {
    val title = stringResource(
        id = if (null == tagEntity) {
            R.string.new_tag
        } else {
            R.string.edit_tag
        }
    )
    var tagName by remember {
        mutableStateOf(tagEntity?.name.orEmpty())
    }
    AlertDialog(
        modifier = Modifier.width(LocalConfiguration.current.screenWidthDp.dp - 80.dp),
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            TextField(
                value = tagName,
                onValueChange = { tagName = it },
                label = {
                    Text(
                        text = stringResource(id = R.string.tag_name),
                    )
                },
                colors = TextFieldDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    tagEntity?.copy(name = tagName) ?: TagModel(-1L, tagName)
                )
            }) {
                Text(text = stringResource(id = R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.cancel))
            }
        },
    )
}

@Composable
fun DeleteTagDialog(
    tagEntity: TagModel,
    onConfirm: (TagModel) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Text(text = stringResource(id = R.string.tag_delete_hint_format).format(tagEntity.name))
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(tagEntity)
            }) {
                Text(text = stringResource(id = R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.cancel))
            }
        },
    )
}

@DevicePreviews
@Composable
private fun MyTagsScreenPreview() {
    PreviewTheme(
        defaultEmptyImagePainter = painterResource(id = R.drawable.vector_no_data_200),
    ) {
        MyTagsScreen(
            tagList = listOf(
                TagModel(id = 1L, name = "标签1"),
                TagModel(id = 2L, name = "标签1标签1"),
                TagModel(id = 3L, name = "标签1标签1标签1"),
                TagModel(id = 4L, name = "标签1标签1标签1标签1"),
                TagModel(id = 5L, name = "标签1标签1标签1标签1标签1"),
                TagModel(id = 6L, name = "标签1标签1标签1标签1标签1标签1"),
                TagModel(id = 7L, name = "标签2"),
            ),
            dialogState = DialogState.Dismiss,
            showEditTagDialog = {},
            showDeleteTagDialog = {},
            dismissDialog = {},
            modifyTag = {},
            deleteTag = {},
            onBackClick = {},
            onTagStatisticClick = {}
        )
    }
}

@DevicePreviews
@Composable
private fun MyTagsScreenEmptyPreview() {
    PreviewTheme(
        defaultEmptyImagePainter = painterResource(id = R.drawable.vector_no_data_200),
    ) {
        MyTagsScreen(
            tagList = listOf(),
            dialogState = DialogState.Dismiss,
            showEditTagDialog = {},
            showDeleteTagDialog = {},
            dismissDialog = {},
            modifyTag = {},
            deleteTag = {},
            onBackClick = {},
            onTagStatisticClick = {}
        )
    }
}

