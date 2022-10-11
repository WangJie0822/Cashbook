package cn.wj.android.cashbook.biometric

import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.fragment.app.FragmentActivity
import com.orhanobut.logger.Logger
import javax.crypto.Cipher

/**
 * [Build.VERSION_CODES.Q] 及以上版本实现
 *
 * - 创建时间：2021/1/11
 *
 * @author 王杰
 */
@RequiresApi(Build.VERSION_CODES.Q)
class BiometricQ(val activity: FragmentActivity) : BiometricInterface by BiometricM(activity) {

    override var encrypt = true
    override var keyAlias = "DEFAULT_KEY_NAME"
    override var ivBytes: ByteArray? = null
    override var title = "验证指纹"
    override var subTitle = ""
    override var hint = "请按压指纹感应区验证指纹"
    override var negative = "取消"

    override fun authenticate(onSuccess: (Cipher) -> Unit, onError: (Int, String) -> Unit) {
        val cancellationSignal = android.os.CancellationSignal()
        cancellationSignal.setOnCancelListener {
            onError.invoke(5, "用户取消")
        }
        val prompt = with(BiometricPrompt.Builder(activity)) {
            if (title.isNotBlank()) {
                setTitle(title)
            }
            if (subTitle.isNotBlank()) {
                setSubtitle(subTitle)
            }
            if (hint.isNotBlank()) {
                setDescription(hint)
            }
            setNegativeButton(negative, activity.mainExecutor) { dialog, _ ->
                dialog?.dismiss()
                cancellationSignal.cancel()
            }
            build()
        }
        val loadCipher = try {
            loadCipher(encrypt, keyAlias, ivBytes)
        } catch (throwable: Throwable) {
            null
        }
        if (null == loadCipher) {
            onError.invoke(BiometricInterface.ERROR_FAILED, "指纹验证失败")
            return
        }
        prompt.authenticate(
            BiometricPrompt.CryptoObject(loadCipher),
            cancellationSignal,
            activity.mainExecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                    try {
                        val cipher = result?.cryptoObject?.cipher
                            ?: throw RuntimeException("cipher is null!")
                        onSuccess.invoke(cipher)
                    } catch (throwable: Throwable) {
                        Logger.e(throwable, "authenticate")
                        onError.invoke(BiometricInterface.ERROR_FAILED, "指纹验证失败")
                    }
                }

                override fun onAuthenticationHelp(helpCode: Int, helpString: CharSequence?) {
                    onError.invoke(helpCode, helpString.toString())
                }

                override fun onAuthenticationFailed() {
                    onError.invoke(BiometricInterface.ERROR_FAILED, "指纹验证失败")
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    onError.invoke(errorCode, errString.toString())
                }
            })
    }
}