@file:Suppress("unused")
@file:JvmName("StringTransform")

package cn.wj.android.cashbook.data.transform

import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.color
import cn.wj.android.cashbook.data.model.SnackbarModel
import com.google.android.material.snackbar.Snackbar

/**
 * String 类型转换相关
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/21
 */


/** 根据 [String] 生成并返回 [SnackbarModel] */
fun String?.toSnackbarModel(
    @ColorInt contentBgColor: Int = R.color.color_secondary.color,
    @ColorInt contentColor: Int = R.color.color_on_secondary.color,
    @IdRes targetId: Int = 0,
    duration: Int = Snackbar.LENGTH_SHORT,
    actionText: String? = null,
    @ColorInt actionColor: Int = R.color.color_primary.color,
    onAction: View.OnClickListener? = null,
    onCallback: Snackbar.Callback? = null
): SnackbarModel {
    return SnackbarModel(
        this.orEmpty(),
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