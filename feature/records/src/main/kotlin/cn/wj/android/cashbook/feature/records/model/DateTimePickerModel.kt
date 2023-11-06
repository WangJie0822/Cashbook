package cn.wj.android.cashbook.feature.records.model

sealed class DateTimePickerModel {

    data class DatePicker(val dateMs: Long) : DateTimePickerModel()

    data class TimePicker(val timeMs: Long) : DateTimePickerModel()
}