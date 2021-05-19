@file:Suppress("unused")

package cn.wj.android.cashbook.data.model

import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.color
import cn.wj.android.cashbook.base.ext.base.string
import com.google.android.material.snackbar.Snackbar

/**
 * [Snackbar] 显示数据实体类
 *
 * @param content 显示文本
 * @param contentBgColor 显示背景颜色
 * @param contentColor 显示文字颜色
 * @param targetId 目标显示控件 id
 * @param duration 显示时长
 * @param actionText 按钮文本
 * @param actionColor 按钮文本颜色
 * @param onAction 按钮点击事件
 * @param onCallback Snackbar 回调
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/16
 */
data class SnackbarModel(
    val content: String? = "",
    @ColorInt val contentBgColor: Int = R.color.colorSecondary.color,
    @ColorInt val contentColor: Int = R.color.colorOnSecondary.color,
    @IdRes val targetId: Int = 0,
    val duration: Int = Snackbar.LENGTH_SHORT,
    val actionText: String? = null,
    @ColorInt val actionColor: Int = R.color.colorPrimary.color,
    val onAction: View.OnClickListener? = null,
    val onCallback: Snackbar.Callback? = null
) {

    /**
     * 次构造方法
     *
     * @param resId 显示文本资源 id
     * @param contentBgColor 显示背景颜色
     * @param contentColor 显示文字颜色
     * @param targetId 目标显示控件 id
     * @param duration 显示时长
     * @param actionText 按钮文本
     * @param actionColor 按钮文本颜色
     * @param onAction 按钮点击事件
     * @param onCallback Snackbar 回调
     */
    constructor(
        @StringRes resId: Int,
        @ColorInt contentBgColor: Int = R.color.colorSecondary.color,
        @ColorInt contentColor: Int = R.color.colorOnSecondary.color,
        @IdRes targetId: Int = 0,
        duration: Int = Snackbar.LENGTH_SHORT,
        actionText: String? = null,
        @ColorInt actionColor: Int = R.color.colorPrimary.color,
        onAction: View.OnClickListener? = null,
        onCallback: Snackbar.Callback? = null
    ) : this(
        resId.string,
        contentBgColor,
        contentColor,
        targetId,
        duration,
        actionText,
        actionColor,
        onAction,
        onCallback
    )
}