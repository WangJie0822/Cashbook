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

@file:Suppress("DEPRECATION", "unused")

package cn.wj.android.cashbook.core.design.security.biometric

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.Context
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import javax.crypto.Cipher

/** 标记 - 是否支持 AndroidQ API */
var biometricSupportQ: Boolean = true

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
    val hintData = LocalBiometricAuthenticateHintData.current
    when (val resultCode = checkBiometric()) {
        ERROR_HW_UNAVAILABLE -> {
            // 不支持
            onError.invoke(resultCode, hintData.unSupportHint)
        }

        ERROR_NO_BIOMETRICS -> {
            // 没有有效指纹
            onError.invoke(resultCode, hintData.noFingerprintHint)
        }

        ERROR_NO_DEVICE_CREDENTIAL -> {
            // 没有设置锁屏
            onError.invoke(resultCode, hintData.noDeviceCredentialHint)
        }

        else -> {
            // 支持
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && biometricSupportQ) {
                BiometricAuthenticateQ(
                    title = title,
                    subTitle = subTitle,
                    hint = hint,
                    cryptoCipher = cryptoCipher,
                    onSuccess = onSuccess,
                    onError = onError,
                )
            } else {
                BiometricAuthenticateM(
                    title = title,
                    subTitle = subTitle,
                    hint = hint,
                    cryptoCipher = cryptoCipher,
                    onSuccess = onSuccess,
                    onError = onError,
                )
            }
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
    val hintData = LocalBiometricAuthenticateHintData.current
    val cancellationSignal = android.os.CancellationSignal()
    cancellationSignal.setOnCancelListener {
        onError.invoke(ERROR_CANCELED, hintData.userCancelHint)
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
        setNegativeButton(hintData.cancelHint, context.mainExecutor) { dialog, _ ->
            dialog?.dismiss()
            cancellationSignal.cancel()
        }
        build()
    }
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
                    onError.invoke(ERROR_FAILED, hintData.verificationFailedHint)
                }
            }

            override fun onAuthenticationHelp(helpCode: Int, helpString: CharSequence?) {
                onError.invoke(helpCode, helpString.toString())
            }

            override fun onAuthenticationFailed() {
                onError.invoke(ERROR_FAILED, hintData.verificationFailedHint)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                onError.invoke(errorCode, errString.toString())
            }
        },
    )
}

@SuppressLint("ObsoleteSdkInt")
@RequiresApi(Build.VERSION_CODES.M)
@RequiresPermission(android.Manifest.permission.USE_FINGERPRINT)
@Composable
internal fun BiometricAuthenticateM(
    title: String,
    subTitle: String,
    hint: String,
    cryptoCipher: Cipher,
    onSuccess: (Cipher) -> Unit,
    onError: (Int, String) -> Unit,
) {
    val hintData = LocalBiometricAuthenticateHintData.current
    val cancellationSignal = androidx.core.os.CancellationSignal()
    cancellationSignal.setOnCancelListener {
        onError.invoke(ERROR_CANCELED, hintData.userCancelHint)
    }
    AlertDialog(
        onDismissRequest = { cancellationSignal.cancel() },
        title = { Text(text = title) },
        text = {
            Column {
                Text(text = subTitle)
                Text(text = hint)
            }
        },
        confirmButton = {
            TextButton(onClick = { cancellationSignal.cancel() }) {
                Text(text = "Cancel")
            }
        },
    )
    fingerprintManager.authenticate(
        FingerprintManagerCompat.CryptoObject(cryptoCipher),
        0,
        cancellationSignal,
        object : FingerprintManagerCompat.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: FingerprintManagerCompat.AuthenticationResult?) {
                try {
                    val cipher = result?.cryptoObject?.cipher
                        ?: throw RuntimeException("cipher is null!")
                    onSuccess.invoke(cipher)
                } catch (throwable: Throwable) {
                    onError.invoke(ERROR_FAILED, hintData.verificationFailedHint)
                }
            }

            override fun onAuthenticationHelp(helpCode: Int, helpString: CharSequence?) {
                onError.invoke(helpCode, helpString.toString())
            }

            override fun onAuthenticationFailed() {
                onError.invoke(ERROR_FAILED, hintData.verificationFailedHint)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                onError.invoke(errorCode, errString.toString())
            }
        },
        null,
    )
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

data class BiometricAuthenticateHintData(
    val unSupportHint: String,
    val noFingerprintHint: String,
    val noDeviceCredentialHint: String,
    val cancelHint: String,
    val userCancelHint: String,
    val verificationFailedHint: String,
)

/**
 * 这个 [CompositionLocal] 用于提供一个 [BiometricAuthenticateHintData]
 *
 * ```
 * CompositionLocalProvider(
 *     LocalEmptyImagePainter provides painterResource(id = R.drawable.)
 * ) { }
 * ```
 *
 * 再使用 [BiometricAuthenticateHintData] 显示错误提示
 */
val LocalBiometricAuthenticateHintData =
    staticCompositionLocalOf<BiometricAuthenticateHintData> { error("No Hint data provided") }

@Composable
fun ProvideBiometricAuthenticateHintData(
    hintData: BiometricAuthenticateHintData,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalBiometricAuthenticateHintData provides hintData,
        content = content,
    )
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
