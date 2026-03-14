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

package cn.wj.android.cashbook.core.design.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec

private const val ANDROID_KEY_STORE = "AndroidKeyStore"

/** WebDAV 密码加密专用密钥别名 */
private const val WEBDAV_KEY_ALIAS = "CashbookWebDAVKey"

/** AES/CBC/PKCS7 变换 */
private const val AES_CBC_TRANSFORMATION =
    KeyProperties.KEY_ALGORITHM_AES + "/" +
        KeyProperties.BLOCK_MODE_CBC + "/" +
        KeyProperties.ENCRYPTION_PADDING_PKCS7

/** 确保 WebDAV 密钥存在，不存在则生成 */
private fun ensureWebDAVKey(): java.security.Key {
    val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
    keyStore.load(null)
    if (!keyStore.containsAlias(WEBDAV_KEY_ALIAS)) {
        val keyGenerator =
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        val builder = KeyGenParameterSpec.Builder(
            WEBDAV_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setUserAuthenticationRequired(false)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
        keyGenerator.init(builder.build())
        keyGenerator.generateKey()
    }
    return keyStore.getKey(WEBDAV_KEY_ALIAS, null)
}

/**
 * 使用 AndroidKeyStore 加密字符串
 *
 * 返回格式: Base64(iv):Base64(encrypted)
 */
fun encryptString(plainText: String): String {
    if (plainText.isBlank()) return plainText
    val key = ensureWebDAVKey()
    val cipher = Cipher.getInstance(AES_CBC_TRANSFORMATION)
    cipher.init(Cipher.ENCRYPT_MODE, key)
    val iv = cipher.iv
    val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
    return Base64.encodeToString(iv, Base64.NO_WRAP) + ":" +
        Base64.encodeToString(encrypted, Base64.NO_WRAP)
}

/**
 * 使用 AndroidKeyStore 解密字符串
 *
 * 如果传入的字符串不含 ":" 分隔符（旧的明文数据），直接返回原文，保证向后兼容
 */
fun decryptString(encryptedText: String): String {
    if (encryptedText.isBlank() || !encryptedText.contains(":")) return encryptedText
    return try {
        val parts = encryptedText.split(":")
        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val encrypted = Base64.decode(parts[1], Base64.NO_WRAP)
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
        keyStore.load(null)
        val key = keyStore.getKey(WEBDAV_KEY_ALIAS, null)
        val cipher = Cipher.getInstance(AES_CBC_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
        String(cipher.doFinal(encrypted), Charsets.UTF_8)
    } catch (_: Exception) {
        // 解密失败，可能是旧的明文数据，直接返回原文
        encryptedText
    }
}

/** 使用别名为 [keyAlias] 的密钥获取加密用的 [Cipher] */
fun loadEncryptCipher(keyAlias: String): Cipher {
    val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
    keyStore.load(null)
    if (!keyStore.containsAlias(keyAlias)) {
        // 秘钥生成器
        val keyGenerator =
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        val builder = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setUserAuthenticationRequired(false)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
        keyGenerator.init(builder.build())
        keyGenerator.generateKey()
    }
    val key = keyStore.getKey(keyAlias, null)
    val cipher = Cipher.getInstance(
        KeyProperties.KEY_ALGORITHM_AES + "/" +
            KeyProperties.BLOCK_MODE_CBC + "/" +
            KeyProperties.ENCRYPTION_PADDING_PKCS7,
    )
    cipher.init(Cipher.ENCRYPT_MODE, key)
    return cipher
}

/** 使用别名为 [keyAlias] 的密钥和向量数据 [bytes] 获取解密用的 [Cipher] */
fun loadDecryptCipher(keyAlias: String, bytes: ByteArray): Cipher {
    val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
    keyStore.load(null)
    if (!keyStore.containsAlias(keyAlias)) {
        // 秘钥生成器
        val keyGenerator =
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        val builder = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setUserAuthenticationRequired(false)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
        keyGenerator.init(builder.build())
        keyGenerator.generateKey()
    }
    val key = keyStore.getKey(keyAlias, null)
    val cipher = Cipher.getInstance(
        KeyProperties.KEY_ALGORITHM_AES + "/" +
            KeyProperties.BLOCK_MODE_CBC + "/" +
            KeyProperties.ENCRYPTION_PADDING_PKCS7,
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
