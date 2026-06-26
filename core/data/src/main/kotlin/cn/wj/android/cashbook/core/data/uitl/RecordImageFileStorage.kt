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

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

/** 记录图片相对路径子目录名（应用私有 filesDir 下） */
const val RECORD_IMAGES_DIR = "record_images"

/** 由图片行 id 确定性派生相对路径（backfill 用，崩溃可重入同名覆盖） */
internal fun recordImageRelativePath(id: Long): String = "$RECORD_IMAGES_DIR/img_$id.jpg"

/** 由唯一 token 派生相对路径（新图未有 id 时用 uuid） */
internal fun newRecordImageRelativePath(token: String): String = "$RECORD_IMAGES_DIR/img_$token.jpg"

/** path 是否为本应用托管的相对图片路径（双轨读判据：true 才尝试文件、false 回退 bytes） */
fun isManagedImagePath(path: String): Boolean = path.startsWith("$RECORD_IMAGES_DIR/")

internal fun resolveRecordImage(baseDir: File, relativePath: String): File = File(baseDir, relativePath)

/** 原子写：先写 .tmp 再 rename（同卷原子），避免半文件 */
internal fun writeRecordImageAtomic(baseDir: File, relativePath: String, bytes: ByteArray) {
    val target = File(baseDir, relativePath)
    target.parentFile?.mkdirs()
    val tmp = File(target.parentFile, target.name + ".tmp")
    tmp.writeBytes(bytes)
    if (target.exists()) {
        target.delete()
    }
    if (!tmp.renameTo(target)) {
        // 跨实现极少数 rename 失败兜底：直接复制再删 tmp
        tmp.copyTo(target, overwrite = true)
        tmp.delete()
    }
}

internal fun deleteRecordImageFile(baseDir: File, relativePath: String): Boolean {
    val f = File(baseDir, relativePath)
    return if (f.exists()) f.delete() else false
}

/**
 * 计算孤儿文件：不在引用集（按文件名）、是普通文件、lastModified 早于 now-grace 才算
 * （grace window 保护刚写入但尚未被 DB 引用的新文件 / backfill 在途文件）。纯函数，便于单测。
 */
internal fun computeOrphanFiles(
    referencedNames: Set<String>,
    files: List<File>,
    nowMs: Long,
    graceWindowMs: Long,
): List<File> = files.filter { f ->
    f.isFile && f.name !in referencedNames && f.lastModified() < nowMs - graceWindowMs
}

/**
 * 记录图片文件存储入口（接口便于单测注入 fake）。
 *
 * 图片落 `filesDir/record_images/` 应用私有目录（minSdk 24 无需权限），
 * path 存相对值、读取时按 filesDir 实时解析，禁存绝对路径。
 */
interface RecordImageFileStorage {
    /** 为新图生成唯一相对路径 */
    fun newRelativePath(): String

    /** 由图片行 id 派生确定性相对路径 */
    fun relativePathForId(id: Long): String

    /** path 是否本应用托管相对图片路径 */
    fun isManaged(path: String): Boolean

    /** 相对路径解析为 filesDir 下的真实 File */
    fun resolve(relativePath: String): File

    /** 相对路径对应文件是否存在 */
    fun exists(relativePath: String): Boolean

    /** 原子写入图片字节 */
    fun write(relativePath: String, bytes: ByteArray)

    /** best-effort 删除图片文件，返回是否实际删除 */
    fun delete(relativePath: String): Boolean

    /** 图片根目录（filesDir/record_images），孤儿扫描用 */
    fun baseDir(): File
}

class RecordImageFileStorageImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : RecordImageFileStorage {

    private val filesDir: File get() = context.filesDir

    override fun newRelativePath(): String = newRecordImageRelativePath(UUID.randomUUID().toString())

    override fun relativePathForId(id: Long): String = recordImageRelativePath(id)

    override fun isManaged(path: String): Boolean = isManagedImagePath(path)

    override fun resolve(relativePath: String): File = resolveRecordImage(filesDir, relativePath)

    override fun exists(relativePath: String): Boolean = resolve(relativePath).exists()

    override fun write(relativePath: String, bytes: ByteArray) =
        writeRecordImageAtomic(filesDir, relativePath, bytes)

    override fun delete(relativePath: String): Boolean = deleteRecordImageFile(filesDir, relativePath)

    override fun baseDir(): File = File(filesDir, RECORD_IMAGES_DIR)
}
