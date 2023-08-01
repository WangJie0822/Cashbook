package cn.wj.android.cashbook.core.common.ext

import java.io.File

fun File.deleteAllFiles(): Boolean {
    if (!exists()) {
        return true
    }
    var result = true
    if (isDirectory) {
        listFiles()?.forEach { childFile ->
            if (!childFile.deleteAllFiles()) {
                result = false
            }
        }
    }
    return result && delete()
}