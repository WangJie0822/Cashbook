package cn.wj.android.cashbook.base.ui

/** 返回点击观察接口 */
interface OnBackPressedWatcher {

    /** 处理返回按键逻辑，由关注的上级界面调用 */
    fun handleOnBackPressed(): Boolean
}