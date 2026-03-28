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

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.util.Pair
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.common.ApplicationInfo
import cn.wj.android.cashbook.core.common.BACKUP_FILE_NAME
import cn.wj.android.cashbook.core.common.tools.funLogger
import cn.wj.android.cashbook.core.common.tools.toDateString
import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryState.Companion.FAILED_BACKUP_PATH_EMPTY
import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryState.Companion.FAILED_BACKUP_PATH_UNAUTHORIZED
import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryState.Companion.FAILED_BACKUP_WEBDAV
import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryState.Companion.FAILED_BLANK_BACKUP_PATH
import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryState.Companion.SUCCESS_BACKUP
import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryState.Companion.SUCCESS_RECOVERY
import cn.wj.android.cashbook.core.design.component.CbAlertDialog
import cn.wj.android.cashbook.core.design.component.CbListItem
import cn.wj.android.cashbook.core.design.component.CbModalBottomSheet
import cn.wj.android.cashbook.core.design.component.CbPasswordTextField
import cn.wj.android.cashbook.core.design.component.CbScaffold
import cn.wj.android.cashbook.core.design.component.CbTextButton
import cn.wj.android.cashbook.core.design.component.CbTextField
import cn.wj.android.cashbook.core.design.component.CbTopAppBar
import cn.wj.android.cashbook.core.design.component.DateRangePickerDialog
import cn.wj.android.cashbook.core.design.component.Loading
import cn.wj.android.cashbook.core.design.component.TextFieldState
import cn.wj.android.cashbook.core.design.icon.CbIcons
import cn.wj.android.cashbook.core.design.preview.PreviewTheme
import cn.wj.android.cashbook.core.model.enums.AutoBackupModeEnum
import cn.wj.android.cashbook.core.model.model.BackupModel
import cn.wj.android.cashbook.core.model.model.BooksModel
import cn.wj.android.cashbook.core.ui.DevicePreviews
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.core.ui.LocalProgressDialogController
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.settings.viewmodel.BackupAndRecoveryUiState
import cn.wj.android.cashbook.feature.settings.viewmodel.BackupAndRecoveryViewModel
import cn.wj.android.cashbook.feature.settings.viewmodel.ExportState
import java.io.File
import java.util.Calendar
import java.util.TimeZone

/**
 * 备份恢复界面
 *
 * @param onRequestPopBackStack 导航到上一级
 * @param onShowSnackbar 显示 [androidx.compose.material3.Snackbar]，参数：(显示文本，action文本) -> [SnackbarResult]
 */
@Composable
internal fun BackupAndRecoveryRoute(
    onRequestPopBackStack: () -> Unit,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,
    onRequestNaviToRecordImport: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BackupAndRecoveryViewModel = hiltViewModel(),
) {
    val progressDialogController = LocalProgressDialogController.current
    viewModel.setProgressDialogController(progressDialogController)

    val shouldDisplayBookmark by viewModel.shouldDisplayBookmark.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
    val booksList by viewModel.booksList.collectAsStateWithLifecycle()
    val currentBook by viewModel.currentBook.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()

    BackupAndRecoveryScreen(
        uiState = uiState,
        shouldDisplayBookmark = shouldDisplayBookmark,
        onRequestDismissBookmark = viewModel::dismissBookmark,
        dialogState = viewModel.dialogState,
        onBackupListItemClick = viewModel::tryRecovery,
        onRequestDismissDialog = viewModel::dismissDialog,
        isConnected = isConnected,
        onConnectStateClick = viewModel::saveWebDAV,
        onBackupPathSelected = viewModel::saveBackupPath,
        onBackupClick = viewModel::backup,
        onRecoveryClick = viewModel::getRecoveryList,
        onAutoBackupClick = viewModel::displaySelectAutoBackupDialog,
        onAutoBackupModeSelected = viewModel::onAutoBackupModeSelected,
        onKeepLatestBackupChanged = viewModel::changeKeepLatestBackup,
        onMobileNetworkBackupEnableChanged = viewModel::onMobileNetworkBackupEnableChanged,
        onNoWifiConfirmBackupClick = viewModel::onNoWifiConfirmBackupClick,
        onDbMigrateClick = { viewModel.refreshDbMigrate(progressDialogController) },
        onRequestNaviToRecordImport = onRequestNaviToRecordImport,
        booksList = booksList,
        currentBook = currentBook,
        exportState = exportState,
        onGetEarliestRecordTime = viewModel::getEarliestRecordTime,
        onCountExportRecords = viewModel::countExportRecords,
        onExportRecords = viewModel::exportRecords,
        onResetExportState = viewModel::resetExportState,
        onBackClick = onRequestPopBackStack,
        onShowSnackbar = onShowSnackbar,
        modifier = modifier,
    )
}

/**
 * 备份恢复界面
 *
 * @param uiState 界面 UI 状态
 * @param shouldDisplayBookmark 是否显示提示
 * @param onRequestDismissBookmark 隐藏提示
 * @param dialogState 弹窗状态
 * @param onBackupListItemClick 备份列表 item 点击回调
 * @param onRequestDismissDialog 隐藏弹窗
 * @param isConnected WebDAV 连接状态
 * @param onConnectStateClick 连接状态点击回调
 * @param onBackupPathSelected 备份路径选择回调
 * @param onBackupClick 备份点击回调
 * @param onRecoveryClick 恢复点击回调
 * @param onAutoBackupClick 自动备份点击回调
 * @param onAutoBackupModeSelected 自动备份模式选择回调
 * @param booksList 账本列表
 * @param currentBook 当前选中账本
 * @param exportState 导出状态
 * @param onGetEarliestRecordTime 获取指定账本最早记录时间
 * @param onCountExportRecords 查询导出记录数量
 * @param onExportRecords 执行导出
 * @param onResetExportState 重置导出状态
 * @param onBackClick 返回点击回调
 * @param onShowSnackbar 显示 [androidx.compose.material3.Snackbar]，参数：(显示文本，action文本) -> [SnackbarResult]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BackupAndRecoveryScreen(
    uiState: BackupAndRecoveryUiState,
    shouldDisplayBookmark: Int,
    onRequestDismissBookmark: () -> Unit,
    dialogState: DialogState,
    onBackupListItemClick: (String) -> Unit,
    onRequestDismissDialog: () -> Unit,
    isConnected: Boolean,
    onConnectStateClick: (String, String, String) -> Unit,
    onBackupPathSelected: (String) -> Unit,
    onBackupClick: () -> Unit,
    onRecoveryClick: (Boolean, String) -> Unit,
    onAutoBackupClick: () -> Unit,
    onKeepLatestBackupChanged: (Boolean) -> Unit,
    onMobileNetworkBackupEnableChanged: (Boolean) -> Unit,
    onNoWifiConfirmBackupClick: (Boolean) -> Unit,
    onAutoBackupModeSelected: (AutoBackupModeEnum) -> Unit,
    onDbMigrateClick: () -> Unit,
    onRequestNaviToRecordImport: (String) -> Unit,
    booksList: List<BooksModel>,
    currentBook: BooksModel?,
    exportState: ExportState,
    onGetEarliestRecordTime: suspend (Long) -> Long?,
    onCountExportRecords: suspend (Long, Long, Long) -> Int,
    onExportRecords: (Long, Long, Long, String, Long, Long, File) -> Unit,
    onResetExportState: () -> Unit,
    onBackClick: () -> Unit,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,
    modifier: Modifier = Modifier,
) {
    // 提示文本
    val blankPathHint = stringResource(id = R.string.please_select_backup_path_first)
    val unauthorizedPathHint = stringResource(id = R.string.unauthorized_path)
    val onlyLocalHint = stringResource(id = R.string.backup_success_only_local)
    val backupSuccessHint = stringResource(id = R.string.backup_success)
    val recoverySuccessHint = stringResource(id = R.string.recovery_success)
    val backupPathEmptyHint = stringResource(id = R.string.backup_path_empty)

    LaunchedEffect(shouldDisplayBookmark) {
        if (shouldDisplayBookmark != 0) {
            val tipText = when (shouldDisplayBookmark) {
                FAILED_BLANK_BACKUP_PATH -> blankPathHint
                FAILED_BACKUP_PATH_UNAUTHORIZED -> unauthorizedPathHint
                FAILED_BACKUP_WEBDAV -> onlyLocalHint
                SUCCESS_BACKUP -> backupSuccessHint
                SUCCESS_RECOVERY -> recoverySuccessHint
                FAILED_BACKUP_PATH_EMPTY -> backupPathEmptyHint
                else -> ""
            }
            if (tipText.isBlank()) {
                onRequestDismissBookmark()
            } else {
                val snackbarResult = onShowSnackbar(tipText, null)
                if (snackbarResult == SnackbarResult.Dismissed) {
                    onRequestDismissBookmark()
                }
            }
        }
    }

    CbScaffold(
        modifier = modifier,
        topBar = {
            CbTopAppBar(
                onBackClick = onBackClick,
                title = { Text(text = stringResource(id = R.string.backup_and_recovery)) },
            )
        },
        content = { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                if (dialogState is DialogState.Shown<*>) {
                    when (val data = dialogState.data) {
                        is List<*> -> {
                            val backupList = data.mapNotNull { it as? BackupModel }
                            BackupListDialog(
                                backupList = backupList,
                                onBackupListItemClick = onBackupListItemClick,
                                onRequestDismissDialog = onRequestDismissDialog,
                            )
                        }

                        0 -> {
                            if (uiState is BackupAndRecoveryUiState.Success) {
                                AutoBackupModeDialog(
                                    autoBackupMode = uiState.autoBackup,
                                    onAutoBackupModeSelected = onAutoBackupModeSelected,
                                    onRequestDismissDialog = onRequestDismissDialog,
                                )
                            }
                        }

                        1 -> {
                            NoWifiBackupHintDialog(
                                onNoWifiConfirmBackupClick = onNoWifiConfirmBackupClick,
                                onRequestDismissDialog = onRequestDismissDialog,
                            )
                        }
                    }
                }

                when (uiState) {
                    BackupAndRecoveryUiState.Loading -> {
                        Loading(modifier = Modifier.align(Alignment.Center))
                    }

                    is BackupAndRecoveryUiState.Success -> {
                        BackupAndRecoveryScaffoldContent(
                            uiState = uiState,
                            isConnected = isConnected,
                            onConnectStateClick = onConnectStateClick,
                            onBackupPathSelected = onBackupPathSelected,
                            onBackupClick = onBackupClick,
                            onRecoveryClick = onRecoveryClick,
                            onAutoBackupClick = onAutoBackupClick,
                            onKeepLatestBackupChanged = onKeepLatestBackupChanged,
                            onMobileNetworkBackupEnableChanged = onMobileNetworkBackupEnableChanged,
                            onDbMigrateClick = onDbMigrateClick,
                            onRequestNaviToRecordImport = onRequestNaviToRecordImport,
                            booksList = booksList,
                            currentBook = currentBook,
                            exportState = exportState,
                            onGetEarliestRecordTime = onGetEarliestRecordTime,
                            onCountExportRecords = onCountExportRecords,
                            onExportRecords = onExportRecords,
                            onResetExportState = onResetExportState,
                            onShowSnackbar = onShowSnackbar,
                        )
                    }
                }
            }
        },
    )
}

/**
 * 备份列表弹窗
 *
 * @param backupList 备份列表数据
 * @param onBackupListItemClick 备份列表 item 点击回调
 * @param onRequestDismissDialog 隐藏弹窗
 */
@Composable
private fun BackupListDialog(
    backupList: List<BackupModel>,
    onBackupListItemClick: (String) -> Unit,
    onRequestDismissDialog: () -> Unit,
) {
    CbAlertDialog(
        onDismissRequest = onRequestDismissDialog,
        confirmButton = {
            Text(
                text = stringResource(id = R.string.cancel),
                modifier = Modifier.clickable { onRequestDismissDialog() },
            )
        },
        text = {
            LazyColumn(
                content = {
                    items(backupList) {
                        Text(
                            text = it.name.replace(BACKUP_FILE_NAME, ""),
                            softWrap = false,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .clickable { onBackupListItemClick(it.path) }
                                .padding(vertical = 4.dp),
                        )
                    }
                },
            )
        },
    )
}

/**
 * 自动备份模式弹窗
 *
 * @param autoBackupMode 当前选择模式
 * @param onAutoBackupModeSelected 模式切换回调
 * @param onRequestDismissDialog 隐藏弹窗
 */
@Composable
internal fun AutoBackupModeDialog(
    autoBackupMode: AutoBackupModeEnum,
    onAutoBackupModeSelected: (AutoBackupModeEnum) -> Unit,
    onRequestDismissDialog: () -> Unit,
) {
    CbAlertDialog(
        onDismissRequest = onRequestDismissDialog,
        title = { Text(text = stringResource(id = R.string.auto_backup)) },
        text = {
            Column(
                modifier = Modifier.selectableGroup(),
            ) {
                AutoBackupModeEnum.entries.forEach { enum ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .selectable(
                                selected = (enum == autoBackupMode),
                                onClick = { onAutoBackupModeSelected.invoke(enum) },
                                role = Role.RadioButton,
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = enum == autoBackupMode,
                            onClick = null,
                        )
                        Text(
                            text = enum.text,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            CbTextButton(onClick = onRequestDismissDialog) {
                Text(text = stringResource(id = R.string.close))
            }
        },
    )
}

/**
 * 无Wi-Fi备份提示弹窗
 *
 * @param onNoWifiConfirmBackupClick 确认点击
 * @param onRequestDismissDialog 隐藏弹窗
 */
@Composable
internal fun NoWifiBackupHintDialog(
    onNoWifiConfirmBackupClick: (Boolean) -> Unit,
    onRequestDismissDialog: () -> Unit,
) {
    var noMorePrompt by remember {
        mutableStateOf(false)
    }
    CbAlertDialog(
        onDismissRequest = onRequestDismissDialog,
        title = {
            Text(text = stringResource(R.string.warm_tip))
        },
        text = {
            Column {
                Text(
                    text = stringResource(id = R.string.no_wifi_backup_available_hint),
                )
                Row(
                    modifier = Modifier
                        .clickable { noMorePrompt = !noMorePrompt }
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = noMorePrompt, onCheckedChange = { noMorePrompt = it })
                    Text(text = stringResource(id = R.string.no_more_prompt))
                }
            }
        },
        confirmButton = {
            CbTextButton(onClick = { onNoWifiConfirmBackupClick.invoke(noMorePrompt) }) {
                Text(text = stringResource(id = R.string.confirm))
            }
        },
        dismissButton = {
            CbTextButton(onClick = onRequestDismissDialog) {
                Text(text = stringResource(id = R.string.cancel))
            }
        },
    )
}

/**
 * 备份恢复内容
 *
 * @param uiState 界面 UI 状态
 * @param isConnected WebDAV 连接状态
 * @param onConnectStateClick 连接状态点击回调
 * @param onBackupPathSelected 备份路径选择回调
 * @param onBackupClick 备份点击回调
 * @param onRecoveryClick 恢复点击回调
 * @param onAutoBackupClick 自动备份点击回调
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun BackupAndRecoveryScaffoldContent(
    uiState: BackupAndRecoveryUiState.Success,
    isConnected: Boolean,
    onConnectStateClick: (String, String, String) -> Unit,
    onBackupPathSelected: (String) -> Unit,
    onBackupClick: () -> Unit,
    onRecoveryClick: (Boolean, String) -> Unit,
    onAutoBackupClick: () -> Unit,
    onKeepLatestBackupChanged: (Boolean) -> Unit,
    onMobileNetworkBackupEnableChanged: (Boolean) -> Unit,
    onDbMigrateClick: () -> Unit,
    onRequestNaviToRecordImport: (String) -> Unit,
    booksList: List<BooksModel>,
    currentBook: BooksModel?,
    exportState: ExportState,
    onGetEarliestRecordTime: suspend (Long) -> Long?,
    onCountExportRecords: suspend (Long, Long, Long) -> Int,
    onExportRecords: (Long, Long, Long, String, Long, Long, File) -> Unit,
    onResetExportState: () -> Unit,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,
    modifier: Modifier = Modifier,
) {
    // 提示文本
    val textBlankErrorText = stringResource(id = R.string.text_must_not_be_blank)
    val webDAVDomainTextFieldState = remember(uiState.webDAVDomain) {
        TextFieldState(
            defaultText = uiState.webDAVDomain,
            validator = { it.isNotBlank() },
            errorFor = { textBlankErrorText },
        )
    }
    val webDAVAccountTextFieldState = remember(uiState.webDAVAccount) {
        TextFieldState(
            defaultText = uiState.webDAVAccount,
            validator = { it.isNotBlank() },
            errorFor = { textBlankErrorText },
        )
    }
    val webDAVPasswordTextFieldState = remember(uiState.webDAVPassword) {
        TextFieldState(
            defaultText = uiState.webDAVPassword,
            validator = { it.isNotBlank() },
            errorFor = { textBlankErrorText },
        )
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(state = rememberScrollState())
            .padding(vertical = 16.dp),
    ) {
        if (!ApplicationInfo.isOffline) {
            Text(
                text = stringResource(id = R.string.webdav_setting),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp),
            )

            CbTextField(
                textFieldState = webDAVDomainTextFieldState,
                label = { Text(text = stringResource(id = R.string.webdav_domain)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .padding(horizontal = 16.dp),
            )
            CbTextField(
                textFieldState = webDAVAccountTextFieldState,
                label = { Text(text = stringResource(id = R.string.webdav_account)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .padding(horizontal = 16.dp),
            )
            CbPasswordTextField(
                textFieldState = webDAVPasswordTextFieldState,
                label = { Text(text = stringResource(id = R.string.webdav_password)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .padding(horizontal = 16.dp),
            )

            FilledTonalButton(
                onClick = {
                    if (webDAVDomainTextFieldState.isValid && webDAVAccountTextFieldState.isValid && webDAVPasswordTextFieldState.isValid) {
                        // 参数可用
                        onConnectStateClick(
                            webDAVDomainTextFieldState.text,
                            webDAVAccountTextFieldState.text,
                            webDAVPasswordTextFieldState.text,
                        )
                    }
                },
                content = {
                    Icon(
                        imageVector = if (isConnected) CbIcons.CheckCircle else CbIcons.Error,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(text = if (isConnected) "已连接" else "未连接")
                },
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 8.dp, end = 16.dp),
            )

            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
            )
        }

        Text(
            text = stringResource(id = R.string.backup_and_recovery),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp),
        )

        val context = LocalContext.current
        var onSelectDirCallback: ((String) -> Unit)? by remember {
            mutableStateOf(null)
        }
        val selectDirLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree(),
            onResult = {
                it?.let { uri ->
                    val path = if (uri.scheme == "content") {
                        // 获取持久化授权
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                        )
                        uri.toString()
                    } else {
                        uri.path
                    }.orEmpty()
                    onSelectDirCallback?.invoke(path)
                }
            },
        )

        CbListItem(
            headlineContent = { Text(text = stringResource(id = R.string.backup_path)) },
            supportingContent = { Text(text = uiState.backupPath.ifBlank { stringResource(id = R.string.click_to_select_backup_path) }) },
            modifier = Modifier.clickable {
                onSelectDirCallback = onBackupPathSelected
                selectDirLauncher.launch(null)
            },
        )
        CbListItem(
            overlineContent = if (uiState.lastBackupTime.isBlank()) {
                null
            } else {
                @Composable {
                    Text(
                        text = stringResource(id = R.string.last_backup_time_with_colon) + uiState.lastBackupTime,
                        color = LocalContentColor.current.copy(0.5f),
                    )
                }
            },
            headlineContent = { Text(text = stringResource(id = R.string.backup)) },
            supportingContent = {
                Text(
                    text = stringResource(
                        id = if (ApplicationInfo.isOffline) {
                            R.string.backup_hint_offline
                        } else {
                            R.string.backup_hint
                        },
                    ),
                )
            },
            modifier = Modifier.clickable {
                if (uiState.backupPath.isBlank()) {
                    // 未设置备份路径，选择备份路径后进行备份
                    onSelectDirCallback = { path ->
                        onBackupPathSelected(path)
                        onBackupClick()
                    }
                    selectDirLauncher.launch(null)
                } else {
                    onBackupClick()
                }
            },
        )
        Box {
            var expended by remember {
                mutableStateOf(false)
            }
            CbListItem(
                headlineContent = { Text(text = stringResource(id = R.string.recovery)) },
                supportingContent = {
                    Text(
                        text = stringResource(
                            id = if (ApplicationInfo.isOffline) {
                                R.string.recovery_hint_offline
                            } else {
                                R.string.recovery_hint
                            },
                        ),
                    )
                },
                modifier = Modifier.combinedClickable(
                    onClick = {
                        onRecoveryClick(false, "")
                    },
                    onLongClick = {
                        expended = true
                    },
                ),
            )
            DropdownMenu(expanded = expended, onDismissRequest = { expended = false }) {
                DropdownMenuItem(
                    text = { Text(text = stringResource(id = R.string.recovery_from_backup_path)) },
                    onClick = {
                        expended = false
                        if (uiState.backupPath.isBlank()) {
                            // 未设置备份路径，选择后进行恢复
                            onSelectDirCallback = { path ->
                                onBackupPathSelected(path)
                                onRecoveryClick(true, "")
                            }
                            selectDirLauncher.launch(null)
                        } else {
                            onRecoveryClick(true, "")
                        }
                    },
                )
                DropdownMenuItem(
                    text = { Text(text = stringResource(id = R.string.recovery_from_custom_path)) },
                    onClick = {
                        expended = false
                        // 选择自定义路径后恢复
                        onSelectDirCallback = { path ->
                            onRecoveryClick(true, path)
                        }
                        selectDirLauncher.launch(null)
                    },
                )
            }
        }

        CbListItem(
            headlineContent = { Text(text = stringResource(id = R.string.auto_backup)) },
            supportingContent = {
                Text(text = uiState.autoBackup.text)
            },
            modifier = Modifier.clickable(onClick = onAutoBackupClick),
        )

        CbListItem(
            headlineContent = { Text(text = stringResource(id = R.string.only_save_latest_backup)) },
            supportingContent = {
                Text(text = stringResource(id = R.string.only_save_latest_backup_hint))
            },
            trailingContent = {
                Switch(
                    checked = uiState.keepLatestBackup,
                    onCheckedChange = onKeepLatestBackupChanged,
                )
            },
        )

        CbListItem(
            headlineContent = { Text(text = stringResource(id = R.string.mobile_network_backup_enable)) },
            supportingContent = {
                Text(text = stringResource(id = R.string.mobile_network_backup_enable_hint))
            },
            trailingContent = {
                Switch(
                    checked = uiState.mobileNetworkBackupEnable,
                    onCheckedChange = onMobileNetworkBackupEnableChanged,
                )
            },
        )

        CbListItem(
            headlineContent = { Text(text = stringResource(id = R.string.db_data_migrate)) },
            supportingContent = {
                Text(text = stringResource(id = R.string.db_data_migrate_hint))
            },
            modifier = Modifier.clickable(onClick = onDbMigrateClick),
        )

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp),
        )

        Text(
            text = stringResource(id = R.string.import_bill),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp),
        )

        val selectFileLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
            onResult = { uri ->
                if (uri != null) {
                    // 在回调中立即复制文件到缓存，避免 URI 权限在导航后失效
                    try {
                        // 用原始文件名保存，确保导入界面显示正确名称
                        val displayName = context.contentResolver.query(
                            uri,
                            arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                            null,
                            null,
                            null,
                        )?.use { cursor ->
                            if (cursor.moveToFirst()) cursor.getString(0) else null
                        } ?: "import_bill_temp.xlsx"
                        val cacheFile = java.io.File(context.cacheDir, displayName)
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            cacheFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        onRequestNaviToRecordImport(cacheFile.absolutePath)
                    } catch (e: Exception) {
                        // 文件复制失败，权限不足或文件无法读取
                        funLogger("BackupAndRecovery")
                            .e(e, "copy import file failed")
                    }
                }
            },
        )

        CbListItem(
            headlineContent = { Text(text = stringResource(id = R.string.import_from_wechat)) },
            supportingContent = {
                Text(text = stringResource(id = R.string.import_from_wechat_hint))
            },
            modifier = Modifier.clickable {
                selectFileLauncher.launch(
                    arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
                )
            },
        )

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp),
        )

        Text(
            text = stringResource(id = R.string.export_bill),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp),
        )

        // 导出 Bottom Sheet 状态
        var showExportSheet by remember { mutableStateOf(false) }

        CbListItem(
            headlineContent = { Text(text = stringResource(id = R.string.export_to_daily_account)) },
            supportingContent = {
                Text(text = stringResource(id = R.string.export_to_daily_account_hint))
            },
            modifier = Modifier.clickable { showExportSheet = true },
        )

        if (showExportSheet) {
            ExportBottomSheet(
                booksList = booksList,
                currentBook = currentBook,
                exportState = exportState,
                onGetEarliestRecordTime = onGetEarliestRecordTime,
                onCountExportRecords = onCountExportRecords,
                onExportRecords = onExportRecords,
                onResetExportState = onResetExportState,
                onShowSnackbar = onShowSnackbar,
                onDismissRequest = { showExportSheet = false },
            )
        }
    }
}

/**
 * 导出到一日记账 Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportBottomSheet(
    booksList: List<BooksModel>,
    currentBook: BooksModel?,
    exportState: ExportState,
    onGetEarliestRecordTime: suspend (Long) -> Long?,
    onCountExportRecords: suspend (Long, Long, Long) -> Int,
    onExportRecords: (Long, Long, Long, String, Long, Long, File) -> Unit,
    onResetExportState: () -> Unit,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current

    // 选中的账本 ID
    var selectedBooksId by remember { mutableLongStateOf(currentBook?.id ?: -1L) }
    // 日期范围
    var selectedStartDate by remember { mutableLongStateOf(0L) }
    var selectedEndDate by remember { mutableLongStateOf(0L) }
    // 记录数量
    var recordCount by remember { mutableIntStateOf(0) }
    // 账本下拉菜单展开状态
    var booksExpanded by remember { mutableStateOf(false) }
    // 日期选择器显示状态
    var showDatePicker by remember { mutableStateOf(false) }

    val selectedBook = booksList.find { it.id == selectedBooksId }

    // 初始化日期范围：根据选中账本获取最早记录时间
    LaunchedEffect(selectedBooksId) {
        if (selectedBooksId > 0) {
            val earliest = onGetEarliestRecordTime(selectedBooksId)
            if (earliest != null) {
                // 截断到当天 00:00:00
                val cal = Calendar.getInstance(TimeZone.getDefault()).apply {
                    timeInMillis = earliest
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                selectedStartDate = cal.timeInMillis
            } else {
                selectedStartDate = 0L
            }
            // 结束日期设为今天 00:00:00
            val todayCal = Calendar.getInstance(TimeZone.getDefault()).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            selectedEndDate = todayCal.timeInMillis
        }
    }

    // 响应式查询记录数量
    LaunchedEffect(selectedBooksId, selectedStartDate, selectedEndDate) {
        if (selectedBooksId > 0 && selectedStartDate > 0 && selectedEndDate >= selectedStartDate) {
            // endDate 查询使用 exclusive，+1 天
            val queryEndDate = selectedEndDate + 86_400_000L
            recordCount = onCountExportRecords(selectedBooksId, selectedStartDate, queryEndDate)
        } else {
            recordCount = 0
        }
    }

    // 处理导出结果
    val exportSuccessText = stringResource(id = R.string.export_success)
    val exportFailedText = stringResource(id = R.string.export_failed)
    LaunchedEffect(exportState) {
        when (val state = exportState) {
            is ExportState.Done -> {
                val file = File(state.filePath)
                if (file.exists()) {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.FileProvider",
                        file,
                    )
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, null))
                }
                onShowSnackbar(
                    String.format(exportSuccessText, state.count),
                    null,
                )
                onResetExportState()
                onDismissRequest()
            }

            is ExportState.Error -> {
                onShowSnackbar(
                    String.format(exportFailedText, state.message),
                    null,
                )
                onResetExportState()
            }

            else -> {}
        }
    }

    // 日期范围选择器
    if (showDatePicker) {
        val selection = if (selectedStartDate > 0 && selectedEndDate > 0) {
            Pair.create(selectedStartDate, selectedEndDate)
        } else {
            null
        }
        DateRangePickerDialog(
            onDismissRequest = { showDatePicker = false },
            onPositiveButtonClick = { pair ->
                showDatePicker = false
                pair.first?.let { start ->
                    // DateRangePickerDialog 内部加了一天的偏移，需要减回来
                    val cal = Calendar.getInstance(TimeZone.getDefault()).apply {
                        timeInMillis = start
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    selectedStartDate = cal.timeInMillis
                }
                pair.second?.let { end ->
                    val cal = Calendar.getInstance(TimeZone.getDefault()).apply {
                        timeInMillis = end
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    selectedEndDate = cal.timeInMillis
                }
            },
            onNegativeButtonClick = { showDatePicker = false },
            selection = selection,
        )
    }

    CbModalBottomSheet(
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            // 标题
            Text(
                text = stringResource(id = R.string.export_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .semantics { heading() },
            )

            // 账本选择器
            Text(
                text = stringResource(id = R.string.export_target_book),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Box {
                CbListItem(
                    headlineContent = {
                        Text(text = selectedBook?.name ?: "")
                    },
                    trailingContent = {
                        Icon(
                            imageVector = CbIcons.ArrowDropDown,
                            contentDescription = stringResource(id = R.string.export_target_book),
                        )
                    },
                    modifier = Modifier.clickable { booksExpanded = true },
                )
                DropdownMenu(
                    expanded = booksExpanded,
                    onDismissRequest = { booksExpanded = false },
                ) {
                    booksList.forEach { book ->
                        DropdownMenuItem(
                            text = { Text(text = book.name) },
                            onClick = {
                                selectedBooksId = book.id
                                booksExpanded = false
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 日期范围
            Text(
                text = stringResource(id = R.string.export_date_range),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (selectedStartDate > 0) {
                        selectedStartDate.toDateString()
                    } else {
                        stringResource(id = R.string.export_start_date)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "—",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                Text(
                    text = if (selectedEndDate > 0) {
                        selectedEndDate.toDateString()
                    } else {
                        stringResource(id = R.string.export_end_date)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = CbIcons.DateRange,
                    contentDescription = stringResource(id = R.string.export_date_range),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 导出按钮
            val isExporting = exportState is ExportState.Exporting
            Button(
                onClick = {
                    if (selectedBooksId > 0 && selectedStartDate > 0 && selectedEndDate >= selectedStartDate) {
                        val queryEndDate = selectedEndDate + 86_400_000L
                        onExportRecords(
                            selectedBooksId,
                            selectedStartDate,
                            queryEndDate,
                            selectedBook?.name ?: "",
                            selectedStartDate,
                            selectedEndDate,
                            context.cacheDir,
                        )
                    }
                },
                enabled = recordCount > 0 && !isExporting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Text(
                        text = stringResource(id = R.string.export_exporting),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                } else if (recordCount > 0) {
                    Text(
                        text = stringResource(id = R.string.export_confirm, recordCount),
                    )
                } else {
                    Text(
                        text = stringResource(id = R.string.export_confirm_empty),
                    )
                }
            }
        }
    }
}

/** 枚举类型对应的文本 */
internal val AutoBackupModeEnum.text: String
    @Composable get() = stringResource(
        id = when (this) {
            AutoBackupModeEnum.CLOSE -> R.string.close
            AutoBackupModeEnum.WHEN_LAUNCH -> R.string.each_launch
            AutoBackupModeEnum.EACH_DAY -> R.string.each_day
            AutoBackupModeEnum.EACH_WEEK -> R.string.each_week
        },
    )

@DevicePreviews
@Composable
private fun BackupAndRecoveryScreenPreview() {
    PreviewTheme {
        BackupAndRecoveryRoute(
            onRequestPopBackStack = {},
            onShowSnackbar = { _, _ -> SnackbarResult.Dismissed },
            onRequestNaviToRecordImport = {},
        )
    }
}
