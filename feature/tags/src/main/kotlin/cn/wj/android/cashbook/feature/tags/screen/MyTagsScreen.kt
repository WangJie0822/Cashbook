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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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

/**
 * 我的标签
 *
 * @param onRequestPopBackStack 导航到上一级
 * @param onRequestNaviToTagStatistic 导航到标签数据分析
 */
@Composable
internal fun MyTagsRoute(
    modifier: Modifier = Modifier,
    onRequestNaviToTagStatistic: (Long) -> Unit = {},
    onRequestPopBackStack: () -> Unit = {},
    viewModel: MyTagsViewModel = hiltViewModel(),
) {

    // 标签列表
    val tagList by viewModel.tagListData.collectAsStateWithLifecycle()

    MyTagsScreen(
        tagList = tagList,
        dialogState = viewModel.dialogState,
        onRequestDismissDialog = viewModel::dismissDialog,
        onEditTagClick = viewModel::showEditTagDialog,
        onEditTagConfirmClick = viewModel::modifyTag,
        onDeleteTagClick = viewModel::showDeleteTagDialog,
        onDeleteTagConfirmClick = viewModel::deleteTag,
        onTagStatisticClick = onRequestNaviToTagStatistic,
        onBackClick = onRequestPopBackStack,
        modifier = modifier,
    )
}

/**
 * 我的标签
 *
 * @param tagList 标签列表数据
 * @param dialogState 弹窗状态
 * @param onRequestDismissDialog 隐藏弹窗
 * @param onEditTagClick 编辑标签点击回调
 * @param onEditTagConfirmClick 编辑标签确认点击回调
 * @param onDeleteTagClick 删除标签点击回调
 * @param onDeleteTagConfirmClick 删除标签确认点击回调
 * @param onTagStatisticClick 统计数据点击回调
 * @param onBackClick 返回点击回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MyTagsScreen(
    tagList: List<TagModel>,
    dialogState: DialogState,
    onRequestDismissDialog: () -> Unit,
    onEditTagClick: (TagModel?) -> Unit,
    onEditTagConfirmClick: (TagModel) -> Unit,
    onDeleteTagClick: (TagModel) -> Unit,
    onDeleteTagConfirmClick: (TagModel) -> Unit,
    onTagStatisticClick: (Long) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {

    CashbookScaffold(
        modifier = modifier,
        topBar = {
            CashbookTopAppBar(
                onBackClick = onBackClick,
                title = { Text(text = stringResource(id = R.string.my_tags)) },
                actions = {
                    IconButton(onClick = { onEditTagClick(null) }) {
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
                            onConfirmClick = onEditTagConfirmClick,
                            onRequestDismissDialog = onRequestDismissDialog,
                        )
                    }

                    is TagDialogState.Delete -> {
                        DeleteTagDialog(
                            tagEntity = data.tag,
                            onConfirmClick = onDeleteTagConfirmClick,
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

/**
 * 编辑标签弹窗
 *
 * @param tagEntity 标签数据，`null` 为新建
 * @param onConfirmClick 确认点击回调
 * @param onRequestDismissDialog 隐藏弹窗
 */
@Composable
internal fun EditTagDialog(
    tagEntity: TagModel?,
    onConfirmClick: (TagModel) -> Unit,
    onRequestDismissDialog: () -> Unit,
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
        onDismissRequest = onRequestDismissDialog,
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
                colors = OutlinedTextFieldDefaults.colors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirmClick(
                    tagEntity?.copy(name = tagName) ?: TagModel(-1L, tagName)
                )
            }) {
                Text(text = stringResource(id = R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onRequestDismissDialog) {
                Text(text = stringResource(id = R.string.cancel))
            }
        },
    )
}

/**
 * 删除标签弹窗
 *
 * @param tagEntity 标签数据
 * @param onConfirmClick 确认点击回调
 * @param onRequestDismissDialog 隐藏弹窗
 */
@Composable
internal fun DeleteTagDialog(
    tagEntity: TagModel,
    onConfirmClick: (TagModel) -> Unit,
    onRequestDismissDialog: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onRequestDismissDialog,
        text = {
            Text(text = stringResource(id = R.string.tag_delete_hint_format).format(tagEntity.name))
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirmClick(tagEntity)
            }) {
                Text(text = stringResource(id = R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onRequestDismissDialog) {
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
        MyTagsRoute()
    }
}