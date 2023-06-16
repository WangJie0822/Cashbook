package cn.wj.android.cashbook.feature.tags.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.design.component.CommonDivider
import cn.wj.android.cashbook.core.model.entity.TagEntity
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.tags.model.TagDialogState
import cn.wj.android.cashbook.feature.tags.viewmodel.SelectTagViewModel
import com.google.accompanist.flowlayout.FlowRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectTagBottomSheetScreen(
    selectedTagIds: List<Long>,
    onTagItemClick: (TagEntity) -> Unit,
    viewModel: SelectTagViewModel = hiltViewModel<SelectTagViewModel>().apply {
        this.selectedTagIds.value = selectedTagIds
    },
) {
    // 显示列表数据
    val tagList: List<TagEntity> by viewModel.tagListData.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxWidth()){
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
                        onClick = viewModel::onAddClick,
                    ) {
                        Text(
                            text = stringResource(id = R.string.add),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                CommonDivider()

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
            },
        )
        if (viewModel.dialogState is TagDialogState.Edit) {
            EditTagDialog(
                tagEntity = null,
                onConfirm = viewModel::addTag,
                onDismiss = viewModel::dismissDialog,
            )
        }
    }
}