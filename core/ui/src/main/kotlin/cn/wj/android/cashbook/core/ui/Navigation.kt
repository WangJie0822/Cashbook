package cn.wj.android.cashbook.core.ui

import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

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

/**
 * 安全的返回上一级页面
 * - 防止在二级页面快速点击返回时，将所有页面出栈，导致应用无法显示
 */
fun NavController.popBackStackSafety(): Boolean {
    return if (currentDestination == graph.findStartDestination()) {
        false
    } else {
        popBackStack()
    }
}
