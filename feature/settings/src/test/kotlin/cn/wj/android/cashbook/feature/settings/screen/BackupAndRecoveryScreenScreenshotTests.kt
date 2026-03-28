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
import cn.wj.android.cashbook.core.model.enums.AutoBackupModeEnum
import cn.wj.android.cashbook.core.testing.util.captureMultiDevice
import cn.wj.android.cashbook.core.testing.util.captureMultiTheme
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.feature.settings.viewmodel.BackupAndRecoveryUiState
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
class BackupAndRecoveryScreenScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val successWithWebDAV = BackupAndRecoveryUiState.Success(
        webDAVDomain = "https://dav.jianguoyun.com/dav/",
        webDAVAccount = "user@example.com",
        webDAVPassword = "password123",
        backupPath = "/storage/emulated/0/Cashbook/backup",
        lastBackupTime = "2024-01-01 12:00:00",
        autoBackup = AutoBackupModeEnum.EACH_DAY,
        keepLatestBackup = true,
        mobileNetworkBackupEnable = false,
    )

    private val successWithoutWebDAV = BackupAndRecoveryUiState.Success(
        webDAVDomain = "https://dav.jianguoyun.com/dav/",
        webDAVAccount = "",
        webDAVPassword = "",
        backupPath = "",
        lastBackupTime = "",
        autoBackup = AutoBackupModeEnum.CLOSE,
        keepLatestBackup = false,
        mobileNetworkBackupEnable = false,
    )

    @Test
    fun backupAndRecoveryScreen_loading_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "BackupAndRecoveryScreen",
            overrideFileName = "BackupAndRecoveryScreen_loading",
        ) {
            BackupAndRecoveryScreen(
                uiState = BackupAndRecoveryUiState.Loading,
                shouldDisplayBookmark = 0,
                onRequestDismissBookmark = {},
                dialogState = DialogState.Dismiss,
                onBackupListItemClick = {},
                onRequestDismissDialog = {},
                isConnected = false,
                onConnectStateClick = { _, _, _ -> },
                onBackupPathSelected = {},
                onBackupClick = {},
                onRecoveryClick = { _, _ -> },
                onAutoBackupClick = {},
                onKeepLatestBackupChanged = {},
                onMobileNetworkBackupEnableChanged = {},
                onNoWifiConfirmBackupClick = {},
                onAutoBackupModeSelected = {},
                onDbMigrateClick = {},
                onBackClick = {},
                onShowSnackbar = { _, _ -> SnackbarResult.Dismissed },
            )
        }
    }

    @Test
    fun backupAndRecoveryScreen_withWebDAV_multipleThemes() {
        composeTestRule.captureMultiTheme(name = "BackupAndRecoveryScreen") {
            BackupAndRecoveryScreen(
                uiState = successWithWebDAV,
                shouldDisplayBookmark = 0,
                onRequestDismissBookmark = {},
                dialogState = DialogState.Dismiss,
                onBackupListItemClick = {},
                onRequestDismissDialog = {},
                isConnected = true,
                onConnectStateClick = { _, _, _ -> },
                onBackupPathSelected = {},
                onBackupClick = {},
                onRecoveryClick = { _, _ -> },
                onAutoBackupClick = {},
                onKeepLatestBackupChanged = {},
                onMobileNetworkBackupEnableChanged = {},
                onNoWifiConfirmBackupClick = {},
                onAutoBackupModeSelected = {},
                onDbMigrateClick = {},
                onBackClick = {},
                onShowSnackbar = { _, _ -> SnackbarResult.Dismissed },
            )
        }
    }

    @Test
    fun backupAndRecoveryScreen_withoutWebDAV_multipleThemes() {
        composeTestRule.captureMultiTheme(
            name = "BackupAndRecoveryScreen",
            overrideFileName = "BackupAndRecoveryScreen_noWebDAV",
        ) {
            BackupAndRecoveryScreen(
                uiState = successWithoutWebDAV,
                shouldDisplayBookmark = 0,
                onRequestDismissBookmark = {},
                dialogState = DialogState.Dismiss,
                onBackupListItemClick = {},
                onRequestDismissDialog = {},
                isConnected = false,
                onConnectStateClick = { _, _, _ -> },
                onBackupPathSelected = {},
                onBackupClick = {},
                onRecoveryClick = { _, _ -> },
                onAutoBackupClick = {},
                onKeepLatestBackupChanged = {},
                onMobileNetworkBackupEnableChanged = {},
                onNoWifiConfirmBackupClick = {},
                onAutoBackupModeSelected = {},
                onDbMigrateClick = {},
                onBackClick = {},
                onShowSnackbar = { _, _ -> SnackbarResult.Dismissed },
            )
        }
    }

    @Test
    fun backupAndRecoveryScreen_withWebDAV_multipleDevices() {
        composeTestRule.captureMultiDevice(screenshotName = "BackupAndRecoveryScreen") {
            CashbookTheme {
                BackupAndRecoveryScreen(
                    uiState = successWithWebDAV,
                    shouldDisplayBookmark = 0,
                    onRequestDismissBookmark = {},
                    dialogState = DialogState.Dismiss,
                    onBackupListItemClick = {},
                    onRequestDismissDialog = {},
                    isConnected = true,
                    onConnectStateClick = { _, _, _ -> },
                    onBackupPathSelected = {},
                    onBackupClick = {},
                    onRecoveryClick = { _, _ -> },
                    onAutoBackupClick = {},
                    onKeepLatestBackupChanged = {},
                    onMobileNetworkBackupEnableChanged = {},
                    onNoWifiConfirmBackupClick = {},
                    onAutoBackupModeSelected = {},
                    onDbMigrateClick = {},
                    onBackClick = {},
                    onShowSnackbar = { _, _ -> SnackbarResult.Dismissed },
                )
            }
        }
    }

    @Test
    fun backupAndRecoveryScreen_loading_multipleDevices() {
        composeTestRule.captureMultiDevice(
            screenshotName = "BackupAndRecoveryScreen_loading",
        ) {
            CashbookTheme {
                BackupAndRecoveryScreen(
                    uiState = BackupAndRecoveryUiState.Loading,
                    shouldDisplayBookmark = 0,
                    onRequestDismissBookmark = {},
                    dialogState = DialogState.Dismiss,
                    onBackupListItemClick = {},
                    onRequestDismissDialog = {},
                    isConnected = false,
                    onConnectStateClick = { _, _, _ -> },
                    onBackupPathSelected = {},
                    onBackupClick = {},
                    onRecoveryClick = { _, _ -> },
                    onAutoBackupClick = {},
                    onKeepLatestBackupChanged = {},
                    onMobileNetworkBackupEnableChanged = {},
                    onNoWifiConfirmBackupClick = {},
                    onAutoBackupModeSelected = {},
                    onDbMigrateClick = {},
                    onBackClick = {},
                    onShowSnackbar = { _, _ -> SnackbarResult.Dismissed },
                )
            }
        }
    }
}
