@file:Suppress("unused", "DEPRECATION")
@file:JvmName("BarTools")

package cn.wj.android.cashbook.base.tools

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.view.Display
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.Window
import android.view.WindowManager
import cn.wj.android.cashbook.manager.AppManager
import kotlin.math.roundToInt

/* ----------------------------------------------------------------------------------------- */
/* |                                   状态栏 导航栏相关                                     | */
/* ----------------------------------------------------------------------------------------- */

/**
 * 通过 [context] 对象，获取并返回状态栏高度[Int]
 * > [context] 可选，默认[AppManager.getContext]
 */
@JvmOverloads
fun getStatusBarHeight(context: Context = AppManager.getContext()): Int {
    var result = 0
    try {
        val resources = Resources.getSystem()
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            val size1 = context.resources.getDimensionPixelSize(resourceId)
            val size2 = resources.getDimensionPixelSize(resourceId)
            result = if (size2 >= size1) {
                size2
            } else {
                val density1 = context.resources.displayMetrics.density
                val density2 = resources.displayMetrics.density
                val f = size1 * density2 / density1
                (if (f >= 0) f + 0.5f else f - 0.5f).roundToInt()
            }
        }
    } catch (throwable: Throwable) {
        funLogger("BarTools").e(throwable, "getStatusBarHeight")
    }
    return result
}

/**
 * 判断是否显示状态栏
 *
 * @param activity [Activity] 对象
 * @return 是否显示状态栏
 */
fun isStatusBarVisible(activity: Activity): Boolean {
    return activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN == 0
}

/**
 * 设置状态栏是否为浅色模式
 * - API 23 及其以上有效
 *
 * @param window [Window] 对象
 * @param isLightMode 是否是浅色模式
 */
fun setStatusBarLightMode(window: Window, isLightMode: Boolean) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        return
    }
    var vis = window.decorView.systemUiVisibility
    vis = if (isLightMode) {
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        vis or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    } else {
        vis and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
    }
    window.decorView.systemUiVisibility = vis
}

/**
 * 设置状态栏是否为浅色模式
 * - 小米手机使用，需要 MIUI6 以上
 *
 * @param window [Window] 对象
 * @param isLightMode 是否是浅色模式
 */
@SuppressLint("PrivateApi")
fun setMIUIStatusBarLightMode(window: Window, isLightMode: Boolean) {
    try {
        val darkModeFlag: Int
        val layoutParams = Class.forName("android.view.MiuiWindowManager\$LayoutParams")
        val field = layoutParams.getField("EXTRA_FLAG_STATUS_BAR_DARK_MODE")
        darkModeFlag = field.getInt(layoutParams)
        val extraFlagField =
            window.javaClass.getMethod(
                "setExtraFlags",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType
            )
        if (isLightMode) {
            extraFlagField.invoke(window, darkModeFlag, darkModeFlag) //状态栏透明且黑色字体
        } else {
            extraFlagField.invoke(window, 0, darkModeFlag) //清除黑色字体
        }
    } catch (e: Exception) {
        funLogger("BarTools").e(e, "setMIUIStatusBarLightMode")
    }
}

/**
 * 设置状态栏图标为深色和魅族特定的文字风格
 * - 魅族手机使用
 *
 * @param window [Window] 对象
 * @param isLightMode 是否是浅色模式
 */
fun setFlymeStatusBarLightMode(window: Window, isLightMode: Boolean) {
    try {
        val lp = window.attributes
        val darkFlag = WindowManager.LayoutParams::class.java
            .getDeclaredField("MEIZU_FLAG_DARK_STATUS_BAR_ICON")
        val meizuFlags = WindowManager.LayoutParams::class.java
            .getDeclaredField("meizuFlags")
        darkFlag.isAccessible = true
        meizuFlags.isAccessible = true
        val bit = darkFlag.getInt(null)
        var value = meizuFlags.getInt(lp)
        value = if (isLightMode) {
            value or bit
        } else {
            value and bit.inv()
        }
        meizuFlags.setInt(lp, value)
        window.attributes = lp
    } catch (e: Exception) {
        funLogger("BarTools").e(e, "setFlymeStatusBarLightMode")
    }
}

/**
 * 获取导航栏高度
 *
 * @return 导航栏高度，单位：px
 */
fun getNavigationBarHeight(): Int {
    val res = Resources.getSystem()
    val resourceId = res.getIdentifier("navigation_bar_height", "dimen", "android")
    return if (resourceId != 0) {
        res.getDimensionPixelSize(resourceId)
    } else {
        0
    }
}

/**
 * 是否支持导航栏
 *
 * @param context [Context] 对象
 *
 * @return 是否支持
 */
@SuppressLint("ObsoleteSdkInt")
@JvmOverloads
fun isSupportNavigationBar(context: Context = AppManager.getContext()): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val display = wm.defaultDisplay
        val size = Point()
        val realSize = Point()
        display.getSize(size)
        display.getRealSize(realSize)
        return realSize.y != size.y || realSize.x != size.x
    }
    val menu = ViewConfiguration.get(AppManager.getApplication()).hasPermanentMenuKey()
    val back = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK)
    return !menu && !back
}

/**
 * 判断导航栏是否显示
 *
 * @param activity [Activity] 对象
 *
 * @return 导航栏是否显示
 */
fun isNavigationBarVisible(activity: Activity): Boolean {
    val show: Boolean
    val display: Display = activity.window.windowManager.defaultDisplay
    val point = Point()
    display.getRealSize(point)

    val decorView: View = activity.window.decorView
    val conf: Configuration = activity.resources.configuration
    show = if (Configuration.ORIENTATION_LANDSCAPE == conf.orientation) {
        val contentView: View = decorView.findViewById(android.R.id.content)
        point.x != contentView.width
    } else {
        val rect = Rect()
        decorView.getWindowVisibleDisplayFrame(rect)
        rect.bottom != point.y
    }
    return show
}

/**
 * 隐藏导航栏
 *
 * @param window [Window] 对象
 */
@SuppressLint("ObsoleteSdkInt")
fun hideNavigationBar(window: Window) {
    if (Build.VERSION.SDK_INT in Build.VERSION_CODES.HONEYCOMB_MR1..Build.VERSION_CODES.JELLY_BEAN_MR2) {
        // API 12 .. 18
        window.decorView.systemUiVisibility = View.GONE
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        // API 19 ..
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }
}