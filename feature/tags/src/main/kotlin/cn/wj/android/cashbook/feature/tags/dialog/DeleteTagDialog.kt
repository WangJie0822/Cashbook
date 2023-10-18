package cn.wj.android.cashbook.feature.tags.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import cn.wj.android.cashbook.core.model.model.TagModel
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.tags.viewmodel.DeleteTagDialogViewModel

@Composable
internal fun DeleteTagDialogRoute(
    tagModel: TagModel,
    onRequestDismissDialog: () -> Unit,
    viewModel: DeleteTagDialogViewModel = hiltViewModel<DeleteTagDialogViewModel>().apply {
        setProgressDialogHintText(stringResource(id = R.string.tag_deleting))
    }
) {

    DeleteTagDialog(
        tagModel = tagModel,
        onRequestDeleteTag = { viewModel.deleteTag(it, onRequestDismissDialog) },
        onRequestDismissDialog = onRequestDismissDialog,
    )
}

@Composable
private fun DeleteTagDialog(
    tagModel: TagModel,
    onRequestDeleteTag: (TagModel) -> Unit,
    onRequestDismissDialog: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onRequestDismissDialog,
        text = {
            Text(text = stringResource(id = R.string.tag_delete_hint_format).format(tagModel.name))
        },
        confirmButton = {
            TextButton(onClick = {
                onRequestDeleteTag(tagModel)
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