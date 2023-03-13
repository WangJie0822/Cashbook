package cn.wj.android.cashbook.feature.records.model

sealed class RecordDialogState {

    object Dismiss : RecordDialogState()

    class Show(val recordId: Long) : RecordDialogState()
}
