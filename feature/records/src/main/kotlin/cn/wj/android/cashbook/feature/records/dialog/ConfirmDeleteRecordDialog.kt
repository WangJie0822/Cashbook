package cn.wj.android.cashbook.feature.records.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import cn.wj.android.cashbook.core.model.model.ResultModel
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.records.viewmodel.ConfirmDeleteRecordDialogViewModel

@Composable
internal fun ConfirmDeleteRecordDialogRoute(
    recordId: Long,
    onResult: (ResultModel) -> Unit,
    onDialogDismiss: () -> Unit,
    viewModel: ConfirmDeleteRecordDialogViewModel = hiltViewModel(),
) {
    val recordRemovingText = stringResource(id = R.string.record_removing)
    ConfirmDeleteRecordDialog(
        onDeleteRecordConfirm = {
            viewModel.onDeleteRecordConfirm(recordRemovingText, recordId, onResult)
        },
        onDialogDismiss = onDialogDismiss,
    )
}

@Composable
internal fun ConfirmDeleteRecordDialog(
    onDeleteRecordConfirm: () -> Unit,
    onDialogDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDialogDismiss,
        text = {
            Text(text = stringResource(id = R.string.record_delete_hint))
        },
        confirmButton = {
            TextButton(onClick = onDeleteRecordConfirm) {
                Text(text = stringResource(id = R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDialogDismiss) {
                Text(text = stringResource(id = R.string.cancel))
            }
        },
    )
}