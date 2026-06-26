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

import cn.wj.android.cashbook.core.common.DB_FILE_NAME
import java.io.File

/**
 * 解压 entry 白名单（替代旧 comment 判据）：仅这些路径允许写出。
 * 先挡含 `..` 的 traversal，再按白名单放行 db / record_images 下文件 / settings / manifest。
 */
internal fun isAllowedBackupEntry(entryName: String): Boolean {
    if (entryName.contains("..")) return false
    return entryName == DB_FILE_NAME ||
        entryName == SETTINGS_ENTRY ||
        entryName == MANIFEST_ENTRY ||
        (entryName.startsWith(RECORD_IMAGES_ENTRY_PREFIX) && entryName.length > RECORD_IMAGES_ENTRY_PREFIX.length)
}

/** Zip Slip 防护：destFile 规范化路径须在 baseDir 内（含嵌套子目录） */
internal fun isWithinDir(destFile: File, baseDir: File): Boolean =
    destFile.canonicalPath.startsWith(baseDir.canonicalPath + File.separator)
