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

package cn.wj.android.cashbook.feature.records.dialog

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import cn.wj.android.cashbook.core.design.component.CbAlertDialog
import cn.wj.android.cashbook.core.design.component.CbTextButton
import cn.wj.android.cashbook.core.model.model.ResultModel
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.records.viewmodel.ConfirmDeleteRecordDialogViewModel

@Composable
internal fun ConfirmDeleteRecordDialogRoute(
    recordId: Long,
    onResult: (ResultModel) -> Unit,
    onDismissRequest: () -> Unit,
    viewModel: ConfirmDeleteRecordDialogViewModel = hiltViewModel(),
) {
    val recordRemovingText = stringResource(id = R.string.record_removing)
    ConfirmDeleteRecordDialog(
        onDeleteRecordConfirm = {
            viewModel.onDeleteRecordConfirm(recordRemovingText, recordId, onResult)
        },
        onDismissRequest = onDismissRequest,
    )
}

@Composable
internal fun ConfirmDeleteRecordDialog(
    onDeleteRecordConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    CbAlertDialog(
        onDismissRequest = onDismissRequest,
        text = {
            Text(text = stringResource(id = R.string.record_delete_hint))
        },
        confirmButton = {
            CbTextButton(onClick = onDeleteRecordConfirm) {
                Text(text = stringResource(id = R.string.confirm))
            }
        },
        dismissButton = {
            CbTextButton(onClick = onDismissRequest) {
                Text(text = stringResource(id = R.string.cancel))
            }
        },
    )
}
