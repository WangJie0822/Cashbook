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

package cn.wj.android.cashbook.feature.records.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import cn.wj.android.cashbook.core.data.uitl.RECORD_IMAGES_DIR
import cn.wj.android.cashbook.core.data.uitl.isManagedImagePath
import java.io.File

/** 图片双轨读结果：文件优先 / bitmap 回退 / 无 */
sealed interface ImageDisplaySource {
    data class FromFile(val file: File) : ImageDisplaySource
    object FromBitmap : ImageDisplaySource
    object None : ImageDisplaySource
}

/**
 * 双轨选择（纯函数，便于单测）：托管相对 path 且文件存在 → 用文件；
 * 否则有 bitmap 回退 bitmap；都无 → None。
 * file 是否存在与 bitmap 是否存在由调用方传入（Composable 侧解析 filesDir / 解码 bytes）。
 */
internal fun imageCoilModel(
    path: String,
    file: File,
    fileExists: Boolean,
    bitmapPresent: Boolean,
): ImageDisplaySource = when {
    isManagedImagePath(path) && fileExists -> ImageDisplaySource.FromFile(file)
    bitmapPresent -> ImageDisplaySource.FromBitmap
    else -> ImageDisplaySource.None
}

/**
 * 解析图片的 Coil model：托管文件存在传 `File`（节省内存、跨设备稳定），
 * 否则回退 bitmap（旧未 backfill 行 / 新选未落盘图），都无返回 null（Coil 走 error）。
 */
@Composable
internal fun rememberRecordImageModel(item: ImageViewModel): Any? {
    val context = LocalContext.current
    return remember(item.path, item.bitmap) {
        val baseDir = File(context.filesDir, RECORD_IMAGES_DIR)
        val file = File(context.filesDir, item.path)
        // canonical containment：file 规范化路径须在 record_images 内（防 `..`/symlink 逃逸，CWE-22 读取侧防护）
        val within = file.canonicalPath.startsWith(baseDir.canonicalPath + File.separator)
        when (val source = imageCoilModel(item.path, file, within && file.exists(), item.bitmap != null)) {
            is ImageDisplaySource.FromFile -> source.file
            ImageDisplaySource.FromBitmap -> item.bitmap
            ImageDisplaySource.None -> null
        }
    }
}
