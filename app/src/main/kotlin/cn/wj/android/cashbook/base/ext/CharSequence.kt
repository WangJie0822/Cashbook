@file:Suppress("unused")
@file:JvmName("CharSequenceExt")

package cn.wj.android.cashbook.base.ext

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import cn.wj.android.cashbook.manager.AppManager
import okhttp3.internal.toHexString
import java.security.MessageDigest
import java.util.*

/* ----------------------------------------------------------------------------------------- */
/* |                                      字符序列相关                                      | */
/* ----------------------------------------------------------------------------------------- */

/**
 * 将文本[this]复制到剪切板
 * > [label] 可选，默认 `"Label`
 * > [context] 可选，默认[AppManager.getContext]
 */
@JvmOverloads
fun String.copyToClipboard(
    label: CharSequence = "Label",
    context: Context = AppManager.getContext(),
) {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    manager.setPrimaryClip(ClipData.newPlainText(label, this))
}

fun String.shaEncode(): String {
    val sha = MessageDigest.getInstance("SHA")
    val byteArray = this.toByteArray()
    val md5Bytes = sha.digest(byteArray)
    val sb = StringBuilder()
    md5Bytes.forEachIndexed { index, byte ->
        val value = byte.toInt() and 0xff
        if (value < 16) {
            sb.append("0")
        }
        sb.append(value.toHexString())
    }
    return sb.toString()
}

/** 将 [bytes] 转换为 16 进制 [String] */
fun bytesToHexString(bytes: ByteArray?): String {
    val stringBuilder = StringBuilder("")
    if (bytes == null || bytes.isEmpty()) {
        return ""
    }
    for (element in bytes) {
        val v = element.toInt() and 0xFF
        val hv = Integer.toHexString(v).uppercase(Locale.getDefault())
        if (hv.length < 2) {
            stringBuilder.append(0)
        }
        stringBuilder.append(hv)
    }
    return stringBuilder.toString()
}

/** 将 16 进制 [str] 转换为 [ByteArray] */
fun hexStringToBytes(str: String?): ByteArray? {
    if (str == null || str == "") {
        return null
    }
    val result = str.uppercase(Locale.getDefault())
    val length = result.length / 2
    val hexChars = result.toCharArray()
    val d = ByteArray(length)
    fun charToByte(c: Char) = "0123456789ABCDEF".indexOf(c).toByte()
    for (i in 0 until length) {
        val pos = i * 2
        d[i] =
            (charToByte(hexChars[pos]).toInt() shl 4 or charToByte(hexChars[pos + 1]).toInt()).toByte()
    }
    return d
}