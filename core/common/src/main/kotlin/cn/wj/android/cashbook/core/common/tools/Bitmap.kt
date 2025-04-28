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

package cn.wj.android.cashbook.core.common.tools

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Size
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.common.manager.AppManager
import java.io.ByteArrayOutputStream
import java.io.InputStream

private val SIZE_ZERO = Size.parseSize("0x0")
private const val DEFAULT_IN_SAMPLE_SIZE = 6

fun Uri.getCompressedBitmap(
    inSampleSize: Int,
    reSize: Boolean,
    context: Context = AppManager.context,
): Bitmap? {
    return try {
        var inputStream: InputStream? = context.contentResolver.openInputStream(this)
        inputStream?.let { stream ->
            // 第一次解码获取图片尺寸
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            var bitmap = BitmapFactory.decodeStream(stream, null, options)
            stream.close()

            // 计算采样率
            options.inSampleSize = inSampleSize
            options.inJustDecodeBounds = false

            // 重新打开流解码 Bitmap
            inputStream = context.contentResolver.openInputStream(this)
            bitmap = BitmapFactory.decodeStream(inputStream, null, options)
            logger().i("getCompressedBitmap after inSampleSize size ${(bitmap?.allocationByteCount ?: 0) / 1024}KB")
            inputStream?.close()

            // 处理旋转
            inputStream = context.contentResolver.openInputStream(this)
            bitmap = rotateBitmapIfRequired(context, bitmap!!, this)
            inputStream?.close()

            if (reSize) {
                // 二次缩放
                bitmap = resizeBitmap(bitmap)
            }

            logger().i("getCompressedBitmap final size ${bitmap.allocationByteCount / 1024}KB")
            bitmap
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun rotateBitmapIfRequired(context: Context, bitmap: Bitmap, uri: Uri): Bitmap {
    val input = context.contentResolver.openInputStream(uri)
    val exif = ExifInterface(input!!)
    val orientation = exif.getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_UNDEFINED,
    )
    input.close()

    return when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
        else -> bitmap
    }
}

private fun rotateBitmap(bitmap: Bitmap, degree: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degree) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private fun resizeBitmap(bitmap: Bitmap, maxSize: Size = SIZE_ZERO): Bitmap {
    var scaledBitmap = bitmap
    if (maxSize == SIZE_ZERO) {
        val ratio = DEFAULT_IN_SAMPLE_SIZE / 10f
        val newWidth = (bitmap.width * ratio).toInt()
        val newHeight = (bitmap.height * ratio).toInt()
        scaledBitmap = bitmap.scale(newWidth, newHeight)
    } else if (bitmap.width > maxSize.width || bitmap.height > maxSize.height) {
        val ratio =
            (maxSize.width.toFloat() / bitmap.width).coerceAtMost(maxSize.height.toFloat() / bitmap.height)
        val newWidth = (bitmap.width * ratio).toInt()
        val newHeight = (bitmap.height * ratio).toInt()
        scaledBitmap = bitmap.scale(newWidth, newHeight)
    }
    return scaledBitmap
}

/**
 * 将 Bitmap 转换为字节数组
 * @param format 压缩格式（默认 JPEG）
 * @param quality 压缩质量（0-100，仅对 JPEG 有效）
 */
fun Bitmap.toByteArray(
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
    quality: Int = 80,
): ByteArray? {
    return try {
        val outputStream = ByteArrayOutputStream()
        this.compress(format, quality, outputStream)
        outputStream.flush()
        outputStream.close()
        outputStream.toByteArray()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * 将字节数组转换为 Bitmap
 */
fun ByteArray.toBitmap(): Bitmap? {
    return try {
        BitmapFactory.decodeByteArray(this, 0, this.size)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
