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
    },
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
