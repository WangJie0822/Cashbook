package cn.wj.android.cashbook.data.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import cn.wj.android.cashbook.R

/**
 * 无数据界面控制数据实体类
 *
 * @param textResId 提示文本资源id
 * @param imgResId 显示图片资源id
 * @param showButton 是否显示按钮
 * @param buttonTextResId 按钮文本资源id
 * @param onButtonClick 按钮点击事件
 */
data class NoDataModel(
    @StringRes val textResId: Int,
    @DrawableRes val imgResId: Int = R.drawable.vector_no_data_200,
    val showButton: Boolean = false,
    @StringRes val buttonTextResId: Int = 0,
    val onButtonClick: () -> Unit = {}
)