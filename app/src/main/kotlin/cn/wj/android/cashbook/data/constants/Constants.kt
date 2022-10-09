@file:Suppress("unused")

package cn.wj.android.cashbook.data.constants

import android.app.Activity
import cn.wj.android.cashbook.BuildConfig
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

/** 通知渠道 - 应用 */
const val NOTIFICATION_CHANNEL_APP = "channel_app"
/** 通知渠道 - 更新 */
const val NOTIFICATION_CHANNEL_UPDATE = "channel_update"

/** 通知 id - 应用 */
const val NOTIFICATION_ID_APP = 2020
/** 通知 id - 更新 */
const val NOTIFICATION_ID_UPDATE = 2021
/** 通知 id - 更新异常 */
const val NOTIFICATION_ID_UPDATE_ERROR = 2022

/** 默认 - 每页数据 */
const val DEFAULT_PAGE_SIZE = 20

/** 备份文件夹 */
const val BACKUP_DIR_NAME = "Cashbook_Backup_${BuildConfig.BACKUP_DIR_SUFFIX}"
/** 备份缓存文件夹 */
const val BACKUP_CACHE_FILE_NAME = "Cashbook_Backup_Cache"
/** 备份文件名 */
const val BACKUP_FILE_NAME = "Cashbook_Backup_File_"
/** 备份文件后缀 */
const val BACKUP_FILE_EXT = ".zip"
/** 备份 - 资产数据文件名 */
const val BACKUP_ASSET_FILE_NAME = "Backup_Asset.json"
/** 备份 - 账本数据文件名 */
const val BACKUP_BOOKS_FILE_NAME = "Backup_Books.json"
/** 备份 - 记录数据文件名 */
const val BACKUP_RECORD_FILE_NAME = "Backup_Record.json"
/** 备份 - 记录标签文件名 */
const val BACKUP_TAG_FILE_NAME = "Backup_Tag.json"
/** 备份 - 记录分类文件名 */
const val BACKUP_TYPE_FILE_NAME = "Backup_Type.json"
/** 备份 - 版本备份信息文件名 */
const val BACKUP_INFO_NAME = "Backup_Info.json"

/** MIME 类型 - 压缩文件 */
const val MIME_TYPE_ZIP = "application/zip"

/** 一天对应毫秒数 */
const val MS_DAY = 1000L * 60L * 60L * 24L
/** 一周对应毫秒数 */
const val MS_WEEK = MS_DAY * 7L