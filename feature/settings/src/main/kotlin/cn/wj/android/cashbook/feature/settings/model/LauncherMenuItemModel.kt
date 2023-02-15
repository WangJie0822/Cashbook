package cn.wj.android.cashbook.feature.settings.model

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 首页菜单数据
 *
 * @param vectorStart 开始显示图标
 * @param tittle 菜单标题
 * @param subTitle 菜单副标题，为 `null` 时不显示
 * @param onClick 菜单点击事件
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/10
 */
data class LauncherMenuItemModel(
    val vectorStart: ImageVector,
    val tittle: String,
    val subTitle: String? = null,
    val onClick: () -> Unit,
)