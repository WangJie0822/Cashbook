package cn.wj.android.cashbook.core.ui

/**
 * 界面状态
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/7/5
 */
sealed interface UiState {

    /** 加载中 */
    object Loading : UiState

    /** 加载完成 */
    class Success<T>(val data: T) : UiState
}