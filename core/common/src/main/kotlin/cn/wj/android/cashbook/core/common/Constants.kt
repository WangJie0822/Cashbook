/*
 * Copyright 2021 The Cashbook Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.wj.android.cashbook.core.common

/**
 * 常量
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/15
 */

/** 数据库文件名 */
const val DB_FILE_NAME = "cashbook.db"
const val DB_INIT_FILE_NAME = "cashbook_init.db"

/** 密码加密别名 */
const val KEY_ALIAS_PASSWORD = "KEY_ALIAS_PASSWORD"

/** 指纹加密别名 */
const val KEY_ALIAS_FINGERPRINT = "KEY_ALIAS_FINGERPRINT"

/** 开关 - 开 */
const val SWITCH_INT_ON = 1

/** 开关 - 关 */
const val SWITCH_INT_OFF = 0

/** 记录类型显示列数 */
const val RECORD_TYPE_COLUMNS = 5

/** 默认 - 每页数据 */
const val DEFAULT_PAGE_SIZE = 50

/** 备份文件夹 */
const val BACKUP_DIR_NAME = "Cashbook_Backup"

/** 备份缓存文件夹 */
const val BACKUP_CACHE_FILE_NAME = "Cashbook_Backup_Cache"

/** 备份文件名 */
const val BACKUP_FILE_NAME = "Cashbook_Backup_File_"

/** 备份文件后缀 */
const val BACKUP_FILE_EXT = ".zip"

/** 下载地址 */
const val ACTION_DOWNLOAD_URL = "action_download_url"

/** APK 名称 */
const val ACTION_APK_NAME = "action_apk_name"

/** 快捷方式类型 */
const val SHORTCUTS_TYPE = "shortcuts_type"

/** 快捷方式类型 - 记一笔 */
const val SHORTCUTS_TYPE_ADD = 0

/** 快捷方式类型 - 资产 */
const val SHORTCUTS_TYPE_ASSET = 1
