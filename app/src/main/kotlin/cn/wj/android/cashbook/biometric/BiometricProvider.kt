package cn.wj.android.cashbook.biometric

import android.os.Build
import androidx.fragment.app.FragmentActivity
import javax.crypto.Cipher

/**
 * 生物识别功能提供者，实现 [BiometricInterface] 接口，实际功能由 [BiometricQ] [BiometricM] [BiometricUn] 代理
 *
 * - 创建时间：2021/1/11
 *
 * @author 王杰
 */
class BiometricProvider(private val activity: FragmentActivity, supportQ: Boolean = true) :
    BiometricInterface by when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && supportQ -> BiometricQ(activity)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> BiometricM(activity)
        else -> BiometricUn()
    }

/** 标记 - 是否支持 AndroidQ API */
var biometricSupportQ: Boolean = true

/** 生物识别对象 */
val FragmentActivity.biometric: BiometricInterface
    get() = BiometricProvider(this, biometricSupportQ)

/** 是否支持生物识别 */
fun FragmentActivity.supportBiometric(): Boolean {
    return biometric.checkBiometric() == BiometricInterface.HW_AVAILABLE
}

/** 尝试进行生物识别认证，成功回调 [onSuccess] 回传 [Cipher] 对象，失败回调 [onError] 回传错误码 [Int] 错误信息 [String] */
fun BiometricInterface.tryAuthenticate(
    onSuccess: (Cipher) -> Unit,
    onError: (Int, String) -> Unit
) {
    when (val resultCode = checkBiometric()) {
        BiometricInterface.ERROR_HW_UNAVAILABLE -> {
            // 不支持
            onError.invoke(resultCode, "当前设备不支持指纹识别")
        }
        BiometricInterface.ERROR_NO_BIOMETRICS -> {
            // 没有有效指纹
            onError.invoke(resultCode, "请添加至少一个有效指纹")
        }
        BiometricInterface.ERROR_NO_DEVICE_CREDENTIAL -> {
            // 没有设置锁屏
            onError.invoke(resultCode, "请先设置锁屏")
        }
        else -> {
            // 支持
            authenticate(onSuccess, onError)
        }
    }
}