@file:Suppress("DEPRECATION")

package cn.wj.android.cashbook.core.design.security.biometric

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.Context
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import cn.wj.android.cashbook.core.ui.R
import javax.crypto.Cipher

@SuppressLint("InlinedApi")
@RequiresPermission(allOf = [android.Manifest.permission.USE_BIOMETRIC, android.Manifest.permission.USE_FINGERPRINT])
@Composable
fun BiometricAuthenticate(
    title: String,
    subTitle: String,
    hint: String,
    cryptoCipher: Cipher,
    onSuccess: (Cipher) -> Unit,
    onError: (Int, String) -> Unit,
) {
    when (val resultCode = checkBiometric()) {
        ERROR_HW_UNAVAILABLE -> {
            // 不支持
            onError.invoke(resultCode, stringResource(id = R.string.device_not_support_fingerprint))
        }

        ERROR_NO_BIOMETRICS -> {
            // 没有有效指纹
            onError.invoke(
                resultCode,
                stringResource(id = R.string.please_add_at_least_one_fingerprint)
            )
        }

        ERROR_NO_DEVICE_CREDENTIAL -> {
            // 没有设置锁屏
            onError.invoke(
                resultCode,
                stringResource(id = R.string.please_set_device_credential_first)
            )
        }

        else -> {
            // 支持
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                BiometricAuthenticateQ(
                    title = title,
                    subTitle = subTitle,
                    hint = hint,
                    cryptoCipher = cryptoCipher,
                    onSuccess = onSuccess,
                    onError = onError
                )
            } else
                BiometricAuthenticateM(
                    title = title,
                    subTitle = subTitle,
                    hint = hint,
                    cryptoCipher = cryptoCipher,
                    onSuccess = onSuccess,
                    onError = onError
                )
        }
    }
}

internal val fingerprintManager: FingerprintManagerCompat
    @Composable
    get() = FingerprintManagerCompat.from(LocalContext.current.applicationContext)

@RequiresApi(Build.VERSION_CODES.P)
@RequiresPermission(android.Manifest.permission.USE_BIOMETRIC)
@Composable
internal fun BiometricAuthenticateQ(
    title: String,
    subTitle: String,
    hint: String,
    cryptoCipher: Cipher,
    onSuccess: (Cipher) -> Unit,
    onError: (Int, String) -> Unit,
) {
    val cancellationSignal = android.os.CancellationSignal()
    cancellationSignal.setOnCancelListener {
        onError.invoke(5, "用户取消")
    }
    val context = LocalContext.current
    val prompt = with(BiometricPrompt.Builder(context)) {
        if (title.isNotBlank()) {
            setTitle(title)
        }
        if (subTitle.isNotBlank()) {
            setSubtitle(subTitle)
        }
        if (hint.isNotBlank()) {
            setDescription(hint)
        }
        setNegativeButton(stringResource(id = R.string.cancel), context.mainExecutor) { dialog, _ ->
            dialog?.dismiss()
            cancellationSignal.cancel()
        }
        build()
    }
    val verifyFailedText = stringResource(id = R.string.fingerprint_verification_failed)
    prompt.authenticate(
        BiometricPrompt.CryptoObject(cryptoCipher),
        cancellationSignal,
        context.mainExecutor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                try {
                    val cipher = result?.cryptoObject?.cipher
                        ?: throw RuntimeException("cipher is null!")
                    onSuccess.invoke(cipher)
                } catch (throwable: Throwable) {
                    onError.invoke(ERROR_FAILED, verifyFailedText)
                }
            }

            override fun onAuthenticationHelp(helpCode: Int, helpString: CharSequence?) {
                onError.invoke(helpCode, helpString.toString())
            }

            override fun onAuthenticationFailed() {
                onError.invoke(ERROR_FAILED, verifyFailedText)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                onError.invoke(errorCode, errString.toString())
            }
        })
}

@Composable
internal fun BiometricAuthenticateM(
    title: String,
    subTitle: String,
    hint: String,
    cryptoCipher: Cipher,
    onSuccess: (Cipher) -> Unit,
    onError: (Int, String) -> Unit,
) {
    // TODO
}

@SuppressLint("InlinedApi")
@RequiresPermission(android.Manifest.permission.USE_FINGERPRINT)
@Composable
fun checkBiometric(context: Context = LocalContext.current.applicationContext): Int {
    return checkBiometricUpM(context)
}

@RequiresPermission(android.Manifest.permission.USE_FINGERPRINT)
@Composable
private fun checkBiometricUpM(context: Context): Int {
    val km = context.getSystemService(KeyguardManager::class.java)
    return when {
        !fingerprintManager.isHardwareDetected -> {
            // 不支持指纹
            ERROR_HW_UNAVAILABLE
        }

        !km.isKeyguardSecure -> {
            // 未设置锁屏
            ERROR_NO_DEVICE_CREDENTIAL
        }

        !fingerprintManager.hasEnrolledFingerprints() -> {
            // 未注册有效指纹
            ERROR_NO_BIOMETRICS
        }

        else -> {
            // 支持指纹识别
            HW_AVAILABLE
        }
    }
}

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