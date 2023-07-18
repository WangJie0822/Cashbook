package cn.wj.android.cashbook.core.model.model

data class Selectable<T>(
    val data: T,
    val selected: Boolean,
)