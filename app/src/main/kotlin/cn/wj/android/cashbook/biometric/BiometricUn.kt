package cn.wj.android.cashbook.biometric

import javax.crypto.Cipher

/**
 * 低版本实现，不支持生物识别
 *
 * - 创建时间：2021/1/11
 *
 * @author 王杰
 */
class BiometricUn : BiometricInterface {

    override var encrypt = true
    override var keyAlias = "DEFAULT_KEY_ALIAS"
    override var ivBytes: ByteArray? = null
    override var title = "验证指纹"
    override var subTitle = ""
    override var hint = ""
    override var negative = "取消"

    override fun checkBiometric(): Int {
        return BiometricInterface.ERROR_HW_UNAVAILABLE
    }

    override fun authenticate(onSuccess: (Cipher) -> Unit, onError: (Int, String) -> Unit) {
    }
}