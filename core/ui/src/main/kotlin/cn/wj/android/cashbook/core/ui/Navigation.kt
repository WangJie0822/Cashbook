package cn.wj.android.cashbook.core.ui

import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavController

/**
 * 这个 [CompositionLocal] 用于提供一个 [NavController]
 *
 * ```
 * CompositionLocalProvider(
 *     LocalNavController provides navController
 * ) { }
 * ```
 *
 * 再使用 [NavController] 处理导航事件
 */
val LocalNavController =
    staticCompositionLocalOf<NavController> { error("No NavController provided") }
