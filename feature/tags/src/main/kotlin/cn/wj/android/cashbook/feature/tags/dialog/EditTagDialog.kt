package cn.wj.android.cashbook.feature.tags.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cn.wj.android.cashbook.core.design.component.CompatTextField
import cn.wj.android.cashbook.core.design.component.TextFieldState
import cn.wj.android.cashbook.core.model.model.TagModel
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.tags.enums.EditTagDialogBookmarkEnum
import cn.wj.android.cashbook.feature.tags.viewmodel.EditTagDialogViewModel

@Composable
internal fun EditTagDialogRoute(
    tagModel: TagModel?,
    onRequestDismissDialog: () -> Unit,
    viewModel: EditTagDialogViewModel = hiltViewModel<EditTagDialogViewModel>().apply {
        setProgressDialogHintText(stringResource(id = R.string.tag_saving))
    },
) {

    EditTagDialog(
        bookmark = viewModel.bookmark,
        onRequestDismissBookmark = viewModel::dismissBookmark,
        tagModel = tagModel,
        onRequestSaveTag = { viewModel.saveTag(it, onRequestDismissDialog) },
        onRequestDismissDialog = onRequestDismissDialog,
    )
}

@Composable
private fun EditTagDialog(
    bookmark: EditTagDialogBookmarkEnum,
    onRequestDismissBookmark: () -> Unit,
    tagModel: TagModel?,
    onRequestSaveTag: (TagModel) -> Unit,
    onRequestDismissDialog: () -> Unit,
) {
    val snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
    val nameExistText = stringResource(id = R.string.tag_name_already_exist)
    val saveFailedText = stringResource(id = R.string.tag_save_failed)
    LaunchedEffect(bookmark) {
        val hintText = when (bookmark) {
            EditTagDialogBookmarkEnum.NAME_EXIST -> nameExistText
            EditTagDialogBookmarkEnum.SAVE_FAILED -> saveFailedText
            else -> ""
        }
        if (hintText.isNotBlank()) {
            val result = snackbarHostState.showSnackbar(hintText)
            if (result == SnackbarResult.Dismissed) {
                onRequestDismissBookmark()
            }
        }
    }
    val title = stringResource(
        id = if (null == tagModel) {
            R.string.new_tag
        } else {
            R.string.edit_tag
        }
    )
    val blankHintText = stringResource(id = R.string.tag_name_must_not_be_blank)
    val tagNameState = remember {
        TextFieldState(
            defaultText = tagModel?.name.orEmpty(),
            validator = { it.isNotBlank() },
            errorFor = { blankHintText },
        )
    }
    AlertDialog(
        modifier = Modifier.width(LocalConfiguration.current.screenWidthDp.dp - 80.dp),
        onDismissRequest = onRequestDismissDialog,
        title = { Text(text = title) },
        text = {
            Column {
                CompatTextField(
                    textFieldState = tagNameState,
                    label = {
                        Text(
                            text = stringResource(id = R.string.tag_name),
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
                SnackbarHost(snackbarHostState)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (tagNameState.isValid) {
                    val model =
                        tagModel?.copy(name = tagNameState.text) ?: TagModel(-1L, tagNameState.text)
                    onRequestSaveTag(model)
                }
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