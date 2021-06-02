@file:Suppress("unused")

package cn.wj.android.cashbook.data.constants

import android.app.Activity
import com.google.android.material.bottomsheet.BottomSheetBehavior

/**
 * 常量
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/15
 */

/** 数据库文件名 */
const val DB_FILE_NAME = "cashbook.db"

/** Activity 界面跳转动画时长 ms */
const val ACTIVITY_ANIM_DURATION = 230L

/** 启动页等待时长 */
const val SPLASH_WAIT_MS = 200L

/** 首页返回点击间隔 */
const val MAIN_BACK_PRESS_INTERVAL_MS = 2_000L

/** 开关 - 开 */
const val SWITCH_INT_ON = 1

/** 开关 - 关 */
const val SWITCH_INT_OFF = 0

/** 资产类型大类 */


/** Activity 返回码  */
const val ACTIVITY_RESULT_CANCELED = Activity.RESULT_CANCELED
const val ACTIVITY_RESULT_OK = Activity.RESULT_OK
const val ACTIVITY_RESULT_FIRST_USER = Activity.RESULT_FIRST_USER

/** [BottomSheetBehavior] 状态码 */
const val BOTTOM_SHEET_STATE_COLLAPSED = BottomSheetBehavior.STATE_COLLAPSED
const val BOTTOM_SHEET_STATE_DRAGGING = BottomSheetBehavior.STATE_DRAGGING
const val BOTTOM_SHEET_STATE_EXPANDED = BottomSheetBehavior.STATE_EXPANDED
const val BOTTOM_SHEET_STATE_HALF_EXPANDED = BottomSheetBehavior.STATE_HALF_EXPANDED
const val BOTTOM_SHEET_STATE_HIDDEN = BottomSheetBehavior.STATE_HIDDEN
const val BOTTOM_SHEET_STATE_SETTLING = BottomSheetBehavior.STATE_SETTLING