package cn.wj.android.cashbook.core.ui

/**
 * 弹窗状态
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/19
 */
sealed class DialogState {

    /** 弹窗隐藏 */
    object Dismiss : DialogState()

    /** 弹窗显示 */
    class Shown<T>(val data: T) : DialogState()
}