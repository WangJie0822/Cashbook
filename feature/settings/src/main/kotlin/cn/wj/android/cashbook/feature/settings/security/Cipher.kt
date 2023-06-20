package cn.wj.android.cashbook.feature.settings.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.MessageDigest
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec

private const val ANDROID_KEY_STORE = "AndroidKeyStore"

/** 使用别名为 [keyAlias] 的密钥获取加密用的 [Cipher] */
internal fun loadEncryptCipher(keyAlias: String): Cipher {
    val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
    keyStore.load(null)
    if (!keyStore.containsAlias(keyAlias)) {
        // 秘钥生成器
        val keyGenerator =
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        val builder = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setUserAuthenticationRequired(false)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
        keyGenerator.init(builder.build())
        keyGenerator.generateKey()
    }
    val key = keyStore.getKey(keyAlias, null)
    val cipher = Cipher.getInstance(
        KeyProperties.KEY_ALGORITHM_AES + "/"
                + KeyProperties.BLOCK_MODE_CBC + "/"
                + KeyProperties.ENCRYPTION_PADDING_PKCS7
    )
    cipher.init(Cipher.ENCRYPT_MODE, key)
    return cipher
}

/** 使用别名为 [keyAlias] 的密钥和向量数据 [bytes] 获取解密用的 [Cipher] */
internal fun loadDecryptCipher(keyAlias: String, bytes: ByteArray): Cipher {
    val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
    keyStore.load(null)
    if (!keyStore.containsAlias(keyAlias)) {
        // 秘钥生成器
        val keyGenerator =
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        val builder = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setUserAuthenticationRequired(false)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
        keyGenerator.init(builder.build())
        keyGenerator.generateKey()
    }
    val key = keyStore.getKey(keyAlias, null)
    val cipher = Cipher.getInstance(
        KeyProperties.KEY_ALGORITHM_AES + "/"
                + KeyProperties.BLOCK_MODE_CBC + "/"
                + KeyProperties.ENCRYPTION_PADDING_PKCS7
    )
    // 使用CBC模式，需要一个向量iv，可增加加密算法的强度
    // 解密时必传，加密时生成
    val iv = IvParameterSpec(bytes)
    cipher.init(Cipher.DECRYPT_MODE, key, iv)
    return cipher
}

/** 将 [ByteArray] 转换为 16 进制 [String] */
fun ByteArray?.toHexString(): String {
    val stringBuilder = StringBuilder("")
    if (this == null || this.isEmpty()) {
        return ""
    }
    for (element in this) {
        val v = element.toInt() and 0xFF
        val hv = Integer.toHexString(v).uppercase(Locale.getDefault())
        if (hv.length < 2) {
            stringBuilder.append(0)
        }
        stringBuilder.append(hv)
    }
    return stringBuilder.toString()
}

/** 将 16 进制 [String] 转换为 [ByteArray] */
fun String?.hexToBytes(): ByteArray? {
    if (this == null || this == "") {
        return null
    }
    val result = this.uppercase(Locale.getDefault())
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

fun String.shaEncode(): String {
    val sha = MessageDigest.getInstance("SHA")
    val byteArray = this.toByteArray()
    val md5Bytes = sha.digest(byteArray)
    val sb = StringBuilder()
    md5Bytes.forEachIndexed { _, byte ->
        val value = byte.toInt() and 0xff
        if (value < 16) {
            sb.append("0")
        }
        sb.append(Integer.toHexString(value))
    }
    return sb.toString()
}