package cn.wj.android.cashbook.biometric

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec

/**
 * 生物识别接口
 *
 * - 创建时间：2021/1/11
 *
 * @author 王杰
 */
interface BiometricInterface {

    /** 标记 - 是否是加密 */
    var encrypt: Boolean

    /** 密钥别名 */
    var keyAlias: String

    /** 解密向量字节数组，加密可为 `null` */
    var ivBytes: ByteArray?

    /** 弹窗标题 */
    var title: String

    /** 弹窗副标题 */
    var subTitle: String

    /** 默认提示文本 */
    var hint: String

    /** 取消按钮文本 */
    var negative: String

    /** 检查指纹识别支持装汤 */
    fun checkBiometric(): Int

    /** 发起认证，成功回调 [onSuccess] 回传 [Cipher] 对象，失败回调 [onError] 回传错误码 [Int] 错误信息 [String] */
    fun authenticate(onSuccess: (Cipher) -> Unit, onError: (Int, String) -> Unit)

    /** 根据是否加密 [encrypt] 密钥别名 [keyAlias] 以及解密时必须的向量字节数组 [bytes] 创建 [Cipher] 对象 */
    @RequiresApi(Build.VERSION_CODES.M)
    fun loadCipher(encrypt: Boolean, keyAlias: String, bytes: ByteArray? = null): Cipher {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        if (!keyStore.containsAlias(keyAlias)) {
            // 秘钥生成器
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val builder = KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(false)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            keyGenerator.init(builder.build())
            keyGenerator.generateKey()
        }
        val key = keyStore.getKey(keyAlias, null)
        val cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                + KeyProperties.BLOCK_MODE_CBC + "/"
                + KeyProperties.ENCRYPTION_PADDING_PKCS7)
        if (encrypt) {
            cipher.init(Cipher.ENCRYPT_MODE, key)
        } else {
            // 使用CBC模式，需要一个向量iv，可增加加密算法的强度
            // 解密时必穿，加密时生成
            val iv = IvParameterSpec(bytes)
            cipher.init(Cipher.DECRYPT_MODE, key, iv)
        }
        return cipher
    }

    companion object {

        /** 硬件可用 */
        const val HW_AVAILABLE = 0

        /** 硬件不可用 */
        const val ERROR_HW_UNAVAILABLE = 1

        /** 传感器无法处理当前数据 */
        const val ERROR_UNABLE_TO_PROCESS = 2

        /** 传感器超时 */
        const val ERROR_TIMEOUT = 3

        /** 存储空间不足导致失败 */
        const val ERROR_NO_SPACE = 4

        /** 操作被取消 */
        const val ERROR_CANCELED = 5

        /** 多次失败导致被锁定 */
        const val ERROR_LOCKOUT = 7

        /** 失败次数太多导致永久锁定，需用户强认证 */
        const val ERROR_LOCKOUT_PERMANENT = 9

        /** 用户取消认证 */
        const val ERROR_USER_CANCELED = 10

        /** 用户没有注册任何生物识别特征 */
        const val ERROR_NO_BIOMETRICS = 11

        /** 设备没有生物识别传感器 */
        const val ERROR_HW_NOT_PRESENT = 12

        /** 设备没有设置其他安全设置 */
        const val ERROR_NO_DEVICE_CREDENTIAL = 14

        /** 密钥相关错误 */
        const val ERROR_UN_SAFE = 15

        /** 识别失败 */
        const val ERROR_FAILED = 16

        /** 识别很好 */
        const val ACQUIRED_GOOD = 0

        /** 只识别到部分，提示用户用力按压或调整位置 */
        const val ACQUIRED_PARTIAL = 1

        /** 图像过于杂乱，提示用户清洁传感器 */
        const val ACQUIRED_INSUFFICIENT = 2

        /** 图像过于杂乱，提示用户清洁传感器 */
        const val ACQUIRED_IMAGER_DIRTY = 3

        /** 未运动而无法识别，提示用户移动手指 */
        const val ACQUIRED_TOO_SLOW = 4

        /** 识别过程中移动手指导致失败，提示用户不要移动手指 */
        const val ACQUIRED_TOO_FAST = 5
    }
}