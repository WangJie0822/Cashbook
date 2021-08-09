@file:Suppress("unused", "BlockingMethodInNonBlockingContext")
@file:JvmName("FileTools")

package cn.wj.android.cashbook.base.tools

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import cn.wj.android.cashbook.base.ext.base.isContentScheme
import cn.wj.android.cashbook.base.ext.base.logger
import cn.wj.android.cashbook.manager.AppManager
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.ArrayList
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
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

/** 将当前路径对应的文件复制到指定位置 */
fun String.copyToPath(path: String, mimeType: String, subDir: String = "", context: Context = AppManager.getContext()) {
    if (path.isContentScheme()) {
        DocumentFile.fromTreeUri(context, Uri.parse(path))?.let { treeDoc ->
            val file = File(this)
            val fileName = file.name
            if (file.exists()) {
                val df = if (subDir.isNotBlank()) {
                    treeDoc.findFile(subDir) ?: treeDoc.createDirectory(subDir)
                } else {
                    treeDoc
                } ?: return
                df.findFile(fileName)?.delete()
                df.createFile(mimeType, fileName)?.writeBytes(file.readBytes(), context)
            }
        }
    } else {
        val file = if (this.isBlank()) {
            context.getExternalFilesDir(null) ?: return
        } else {
            File(this)
        }
        val fileName = file.name
        if (file.exists()) {
            file.copyTo("$path${File.separator}$subDir".createFileIfNotExists(fileName))
        }
    }
}

fun DocumentFile.writeBytes(data: ByteArray, context: Context = AppManager.getContext()): Boolean {
    context.contentResolver.openOutputStream(this.uri)?.let {
        it.write(data)
        it.close()
        return true
    }
    return false
}

fun DocumentFile.readBytes(context: Context = AppManager.getContext()): ByteArray? {
    context.contentResolver.openInputStream(this.uri)?.let {
        val len: Int = it.available()
        val buffer = ByteArray(len)
        it.read(buffer)
        it.close()
        return buffer
    }
    return null
}

fun String.unzipToDir(path: String): List<File> {
    return unzipFileByKeyword(File(this), File(path), null).orEmpty()
}

@Throws(IOException::class)
fun unzipFileByKeyword(
    zipFile: File?,
    destDir: File?,
    keyword: String?
): List<File>? {
    if (zipFile == null || destDir == null) return null
    val files = ArrayList<File>()
    val zip = ZipFile(zipFile)
    val entries = zip.entries()
    zip.use {
        if (isSpace(keyword)) {
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement() as ZipEntry
                val entryName = entry.name
                if (entryName.contains("../")) {
                    Log.e("ZipUtils", "entryName: $entryName is dangerous!")
                    continue
                }
                if (!unzipChildFile(destDir, files, zip, entry, entryName)) return files
            }
        } else {
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement() as ZipEntry
                val entryName = entry.name
                if (entryName.contains("../")) {
                    Log.e("ZipUtils", "entryName: $entryName is dangerous!")
                    continue
                }
                if (entryName.contains(keyword!!)) {
                    if (!unzipChildFile(destDir, files, zip, entry, entryName)) return files
                }
            }
        }
    }
    return files
}

@Throws(IOException::class)
private fun unzipChildFile(
    destDir: File,
    files: MutableList<File>,
    zip: ZipFile,
    entry: ZipEntry,
    name: String
): Boolean {
    val file = File(destDir, name)
    files.add(file)
    if (entry.isDirectory) {
        return createOrExistsDir(file)
    } else {
        if (!createOrExistsFile(file)) return false
        BufferedInputStream(zip.getInputStream(entry)).use { `in` ->
            BufferedOutputStream(FileOutputStream(file)).use { out ->
                out.write(`in`.readBytes())
            }
        }
    }
    return true
}

private fun createOrExistsDir(file: File?): Boolean {
    return file != null && if (file.exists()) file.isDirectory else file.mkdirs()
}

private fun createOrExistsFile(file: File?): Boolean {
    if (file == null) return false
    if (file.exists()) return file.isFile
    if (!createOrExistsDir(file.parentFile)) return false
    return try {
        file.createNewFile()
    } catch (e: IOException) {
        e.printStackTrace()
        false
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