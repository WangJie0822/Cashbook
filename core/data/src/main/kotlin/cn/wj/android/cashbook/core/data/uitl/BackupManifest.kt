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

package cn.wj.android.cashbook.core.data.uitl

import org.json.JSONException
import org.json.JSONObject

/** 当前备份格式版本（v1 = 旧 db-only zip；v2 = db + record_images + settings.json + manifest.json） */
const val BACKUP_FORMAT_VERSION = 2

/** 备份 zip 内 manifest 文件名 */
const val MANIFEST_ENTRY = "manifest.json"

/** 备份 zip 内设置项文件名 */
const val SETTINGS_ENTRY = "settings.json"

/** 备份 zip 内图片目录前缀 */
const val RECORD_IMAGES_ENTRY_PREFIX = "record_images/"

/** 构造 manifest JSON（格式版本戳 + app 版本，供前向不兼容守护） */
internal fun buildManifestJson(formatVersion: Int, appVersion: String): String =
    JSONObject()
        .put("formatVersion", formatVersion)
        .put("appVersion", appVersion)
        .toString()

/** 解析 manifest 格式版本；缺失 / 非法一律视为 1（旧 db-only 备份无 manifest） */
internal fun parseManifestFormatVersion(json: String): Int =
    try {
        JSONObject(json).optInt("formatVersion", 1)
    } catch (e: JSONException) {
        1
    }
