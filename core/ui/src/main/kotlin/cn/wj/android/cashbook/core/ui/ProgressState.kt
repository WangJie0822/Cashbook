package cn.wj.android.cashbook.core.ui

sealed class ProgressState {

    object Dismiss : ProgressState()
    object Show : ProgressState()
}