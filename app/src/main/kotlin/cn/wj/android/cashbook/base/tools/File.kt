@file:Suppress("unused", "BlockingMethodInNonBlockingContext")
@file:JvmName("FileTools")

package cn.wj.android.cashbook.base.tools

import cn.wj.android.cashbook.base.ext.base.logger
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** 获取路径对应的 [File] 对象，如果不存在则创建 */
fun String.createFileIfNotExists(child: String? = null): File {
    val file = if (child.isNullOrBlank()) {
        File(this)
    } else {
        File(this, child)
    }
    try {
        if (!file.exists()) {
            // 文件不存在
            file.parent?.let { parentPath ->
                val parent = File(parentPath)
                if (!parent.exists()) {
                    // 父路径不存在，创建
                    parent.mkdirs()
                }
            }
            // 创建文件
            file.createNewFile()
        }
    } catch (throwable: Throwable) {
        logger().e(throwable, "createFileIfNotExists")
    }
    return file
}

/** 删除路径下所有文件 */
fun String.deleteFiles() {
    val file = File(this)
    if (!file.exists()) {
        // 文件不存在
        return
    }
    if (file.isDirectory) {
        // 是目录，遍历删除子文件
        file.listFiles()?.forEach {
            it.path.deleteFiles()
        }
    }
    // 删除文件
    file.delete()
}

/** 将集合中的文件压缩到 [zippedFilePath] */
fun Collection<String>?.zipToFile(zippedFilePath: String?, comment: String? = null): Boolean {
    if (this.isNullOrEmpty() || zippedFilePath.isNullOrBlank()) {
        return false
    }
    ZipOutputStream(FileOutputStream(zippedFilePath)).use {
        for (srcFile in this) {
            if (!zipFile(getFileByPath(srcFile)!!, "", it, comment)) {
                return false
            }
        }
        return true
    }
}

@Throws(IOException::class)
private fun zipFile(
    srcFile: File,
    rootPath: String,
    zos: ZipOutputStream,
    comment: String?
): Boolean {
    var rootPath1 = rootPath
    if (!srcFile.exists()) return true
    rootPath1 = rootPath1 + (if (isSpace(rootPath1)) "" else File.separator) + srcFile.name
    if (srcFile.isDirectory) {
        val fileList = srcFile.listFiles()
        if (fileList == null || fileList.isEmpty()) {
            val entry = ZipEntry("$rootPath1/")
            entry.comment = comment
            zos.putNextEntry(entry)
            zos.closeEntry()
        } else {
            for (file in fileList) {
                if (!zipFile(file, rootPath1, zos, comment)) return false
            }
        }
    } else {
        BufferedInputStream(FileInputStream(srcFile)).use { `is` ->
            val entry = ZipEntry(rootPath1)
            entry.comment = comment
            zos.putNextEntry(entry)
            zos.write(`is`.readBytes())
            zos.closeEntry()
        }
    }
    return true
}

private fun isSpace(s: String?): Boolean {
    if (s == null) return true
    var i = 0
    val len = s.length
    while (i < len) {
        if (!Character.isWhitespace(s[i])) {
            return false
        }
        ++i
    }
    return true
}

private fun getFileByPath(filePath: String): File? {
    return if (isSpace(filePath)) null else File(filePath)
}