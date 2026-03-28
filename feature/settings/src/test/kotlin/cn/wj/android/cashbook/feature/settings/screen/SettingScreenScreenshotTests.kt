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

package cn.wj.android.cashbook.feature.settings.screen

import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarResult
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.model.enums.DarkModeEnum
import cn.wj.android.cashbook.core.model.enums.ImageQualityEnum
import cn.wj.android.cashbook.core.model.enums.VerificationModeEnum
import cn.wj.android.cashbook.core.testing.util.captureMultiDevice
import cn.wj.android.cashbook.core.testing.util.captureMultiTheme
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.feature.settings.enums.SettingPasswordStateEnum
import cn.wj.android.cashbook.feature.settings.viewmodel.SettingUiState
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "480dpi")
@LooperMode(LooperMode.Mode.PAUSED)
class SettingScreenScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val successUiState = SettingUiState.Success(
        mobileNetworkDownloadEnable = false,
        imageQuality = ImageQualityEnum.ORIGINAL,
        needSecurityVerificationWhenLaunch = false,
        verificationMode = VerificationModeEnum.WHEN_LAUNCH,
        enableFingerprintVerification = false,
        hasPassword = false,
        darkMode = DarkModeEnum.FOLLOW_SYSTEM,
        dynamicColor = false,
    )

    @Test
    fun settingScreen_loading_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "SettingScreen",
            overrideFileName = "SettingScreen_loading",
        ) {
            SettingScreen(
                supportFingerprint = false,
                uiState = SettingUiState.Loading,
                shouldDisplayBookmark = "",
                onRequestDismissBookmark = {},
                dialogState = DialogState.Dismiss,
                onRequestDismissDialog = {},
                onMobileNetworkDownloadEnableChanged = {},
                onImageQualityClick = {},
                onImageQualitySelected = {},
                onNeedSecurityVerificationWhenLaunchChanged = {},
                onVerificationModeClick = {},
                onEnableFingerprintVerificationChanged = {},
                onPasswordClick = {},
                onClearPasswordClick = {},
                onCreateConfirmClick = { SettingPasswordStateEnum.SUCCESS },
                onModifyConfirmClick = { _, _, _ -> },
                onClearConfirmClick = { _, _ -> },
                onVerifyConfirmClick = { _, _ -> },
                onFingerprintVerifySuccess = {},
                onFingerprintVerifyError = { _, _ -> },
                onVerificationModeSelected = {},
                onDarkModeClick = {},
                onDarkModeSelected = {},
                onDynamicColorClick = {},
                onDynamicColorSelected = {},
                onBackupAndRecoveryClick = {},
                onBackClick = {},
                onShowSnackbar = { _, _ -> SnackbarResult.Dismissed },
            )
        }
    }

    @Test
    fun settingScreen_success_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "SettingScreen") {
            SettingScreen(
                supportFingerprint = true,
                uiState = successUiState,
                shouldDisplayBookmark = "",
                onRequestDismissBookmark = {},
                dialogState = DialogState.Dismiss,
                onRequestDismissDialog = {},
                onMobileNetworkDownloadEnableChanged = {},
                onImageQualityClick = {},
                onImageQualitySelected = {},
                onNeedSecurityVerificationWhenLaunchChanged = {},
                onVerificationModeClick = {},
                onEnableFingerprintVerificationChanged = {},
                onPasswordClick = {},
                onClearPasswordClick = {},
                onCreateConfirmClick = { SettingPasswordStateEnum.SUCCESS },
                onModifyConfirmClick = { _, _, _ -> },
                onClearConfirmClick = { _, _ -> },
                onVerifyConfirmClick = { _, _ -> },
                onFingerprintVerifySuccess = {},
                onFingerprintVerifyError = { _, _ -> },
                onVerificationModeSelected = {},
                onDarkModeClick = {},
                onDarkModeSelected = {},
                onDynamicColorClick = {},
                onDynamicColorSelected = {},
                onBackupAndRecoveryClick = {},
                onBackClick = {},
                onShowSnackbar = { _, _ -> SnackbarResult.Dismissed },
            )
        }
    }

    @Test
    fun settingScreen_success_multipleDevices() {
        composeTestRule.captureMultiDevice(screenshotName = "SettingScreen") {
            CashbookTheme {
                SettingScreen(
                    supportFingerprint = true,
                    uiState = successUiState,
                    shouldDisplayBookmark = "",
                    onRequestDismissBookmark = {},
                    dialogState = DialogState.Dismiss,
                    onRequestDismissDialog = {},
                    onMobileNetworkDownloadEnableChanged = {},
                    onImageQualityClick = {},
                    onImageQualitySelected = {},
                    onNeedSecurityVerificationWhenLaunchChanged = {},
                    onVerificationModeClick = {},
                    onEnableFingerprintVerificationChanged = {},
                    onPasswordClick = {},
                    onClearPasswordClick = {},
                    onCreateConfirmClick = { SettingPasswordStateEnum.SUCCESS },
                    onModifyConfirmClick = { _, _, _ -> },
                    onClearConfirmClick = { _, _ -> },
                    onVerifyConfirmClick = { _, _ -> },
                    onFingerprintVerifySuccess = {},
                    onFingerprintVerifyError = { _, _ -> },
                    onVerificationModeSelected = {},
                    onDarkModeClick = {},
                    onDarkModeSelected = {},
                    onDynamicColorClick = {},
                    onDynamicColorSelected = {},
                    onBackupAndRecoveryClick = {},
                    onBackClick = {},
                    onShowSnackbar = { _, _ -> SnackbarResult.Dismissed },
                )
            }
        }
    }

    @Test
    fun settingScreen_loading_multipleDevices() {
        composeTestRule.captureMultiDevice(screenshotName = "SettingScreen_loading") {
            CashbookTheme {
                SettingScreen(
                    supportFingerprint = false,
                    uiState = SettingUiState.Loading,
                    shouldDisplayBookmark = "",
                    onRequestDismissBookmark = {},
                    dialogState = DialogState.Dismiss,
                    onRequestDismissDialog = {},
                    onMobileNetworkDownloadEnableChanged = {},
                    onImageQualityClick = {},
                    onImageQualitySelected = {},
                    onNeedSecurityVerificationWhenLaunchChanged = {},
                    onVerificationModeClick = {},
                    onEnableFingerprintVerificationChanged = {},
                    onPasswordClick = {},
                    onClearPasswordClick = {},
                    onCreateConfirmClick = { SettingPasswordStateEnum.SUCCESS },
                    onModifyConfirmClick = { _, _, _ -> },
                    onClearConfirmClick = { _, _ -> },
                    onVerifyConfirmClick = { _, _ -> },
                    onFingerprintVerifySuccess = {},
                    onFingerprintVerifyError = { _, _ -> },
                    onVerificationModeSelected = {},
                    onDarkModeClick = {},
                    onDarkModeSelected = {},
                    onDynamicColorClick = {},
                    onDynamicColorSelected = {},
                    onBackupAndRecoveryClick = {},
                    onBackClick = {},
                    onShowSnackbar = { _, _ -> SnackbarResult.Dismissed },
                )
            }
        }
    }
}
