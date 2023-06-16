package cn.wj.android.cashbook.feature.tags.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.design.component.CommonTopBar
import cn.wj.android.cashbook.core.design.component.Empty
import cn.wj.android.cashbook.core.model.entity.TagEntity
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.tags.model.TagDialogState
import cn.wj.android.cashbook.feature.tags.viewmodel.MyTagsViewModel
import com.google.accompanist.flowlayout.FlowRow

@Composable
internal fun MyTagsRoute(
    onBackClick: () -> Unit,
    onTagStatisticClick: (TagEntity) -> Unit,
) {

    MyTagsScreen(
        onBackClick = onBackClick,
        onTagStatisticClick = onTagStatisticClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MyTagsScreen(
    onBackClick: () -> Unit,
    onTagStatisticClick: (TagEntity) -> Unit,
    viewModel: MyTagsViewModel = hiltViewModel(),
) {
    // 标签列表
    val tagList by viewModel.tagListData.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CommonTopBar(
                text = stringResource(id = R.string.my_tags),
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = { viewModel.showEditTagDialog() }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
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
            // 空布局
            if (tagList.isEmpty()) {
                Empty(
                    modifier = Modifier.fillMaxSize(),
                    imageResId = cn.wj.android.cashbook.core.common.R.drawable.vector_no_data_200,
                    hintResId = R.string.tags_empty_hint
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
                                        viewModel.showEditTagDialog(it)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(text = stringResource(id = R.string.delete)) },
                                    onClick = {
                                        expanded = false
                                        viewModel.showDeleteTagDialog(it)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(text = stringResource(id = R.string.statistic_data)) },
                                    onClick = {
                                        expanded = false
                                        onTagStatisticClick(it)
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // 编辑标签弹窗
            val dialogState = viewModel.dialogState
            if (dialogState is TagDialogState.Edit) {
                EditTagDialog(
                    tagEntity = dialogState.tag,
                    onConfirm = viewModel::modifyTag,
                    onDismiss = viewModel::dismissDialog,
                )
            }

            // 删除弹窗
            if (dialogState is TagDialogState.Delete) {
                DeleteTagDialog(
                    tagEntity = dialogState.tag,
                    onConfirm = viewModel::deleteTag,
                    onDismiss = viewModel::dismissDialog,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTagDialog(
    tagEntity: TagEntity?,
    onConfirm: (TagEntity) -> Unit,
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
                    tagEntity?.copy(name = tagName) ?: TagEntity(-1L, tagName, false)
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
    tagEntity: TagEntity,
    onConfirm: (TagEntity) -> Unit,
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

