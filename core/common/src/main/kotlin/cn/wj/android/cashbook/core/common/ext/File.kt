package cn.wj.android.cashbook.core.common.ext

import java.io.File

fun File.deleteAllFiles() {
    if (!exists()) {
        return
    }
    if (isDirectory) {
        listFiles()?.forEach { childFile ->
            childFile.deleteAllFiles()
        }
    }
    delete()
}