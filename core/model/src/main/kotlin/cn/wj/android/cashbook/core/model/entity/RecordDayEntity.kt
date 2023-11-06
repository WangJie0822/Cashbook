package cn.wj.android.cashbook.core.model.entity

data class RecordDayEntity(
    val day: Int,
    val dayType:Int,
    val dayIncome: String,
    val dayExpand: String,
) : RecordViews