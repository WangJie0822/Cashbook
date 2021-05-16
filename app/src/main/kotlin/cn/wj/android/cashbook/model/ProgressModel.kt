@file:Suppress("unused")

package cn.wj.android.cashbook.model

/**
 * 进度条弹窗控制 Model
 *
 * @param cancelable 能否取消
 * @param hint 提示文本
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/16
 */
data class ProgressModel(
    val cancelable: Boolean = true,
    val hint: String = ""
)