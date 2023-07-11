package cn.wj.android.cashbook.feature.records.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import cn.wj.android.cashbook.core.design.theme.PreviewTheme
import cn.wj.android.cashbook.core.model.model.ResultModel
import cn.wj.android.cashbook.core.ui.DevicePreviews
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.records.viewmodel.ConfirmDeleteRecordDialogViewModel

@Composable
internal fun ConfirmDeleteRecordDialogRoute(
    recordId: Long,
    onResult: (ResultModel) -> Unit,
    onDialogDismiss: () -> Unit,
    viewModel: ConfirmDeleteRecordDialogViewModel = hiltViewModel(),
) {
    ConfirmDeleteRecordDialog(
        recordId = recordId,
        onDeleteRecordConfirm = {
            viewModel.onDeleteRecordConfirm(it, onResult)
        },
        onDialogDismiss = onDialogDismiss,
    )
}

@Composable
internal fun ConfirmDeleteRecordDialog(
    recordId: Long,
    onDeleteRecordConfirm: (Long) -> Unit,
    onDialogDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDialogDismiss,
        text = {
            Text(text = stringResource(id = R.string.record_delete_hint))
        },
        confirmButton = {
            TextButton(onClick = {
                onDeleteRecordConfirm.invoke(recordId)
            }) {
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

@DevicePreviews
@Composable
private fun ConfirmDeleteRecordDialogPreview() {
    PreviewTheme {
        ConfirmDeleteRecordDialog(
            recordId = -1L,
            onDeleteRecordConfirm = {},
            onDialogDismiss = {},
        )
    }
}