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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.common.ApplicationInfo
import cn.wj.android.cashbook.core.common.BACKUP_FILE_NAME
import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryState.Companion.FAILED_BACKUP_PATH_EMPTY
import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryState.Companion.FAILED_BACKUP_PATH_UNAUTHORIZED
import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryState.Companion.FAILED_BACKUP_WEBDAV
import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryState.Companion.FAILED_BLANK_BACKUP_PATH
import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryState.Companion.SUCCESS_BACKUP
import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryState.Companion.SUCCESS_RECOVERY
import cn.wj.android.cashbook.core.design.component.CbAlertDialog
import cn.wj.android.cashbook.core.design.component.CbListItem
import cn.wj.android.cashbook.core.design.component.CbPasswordTextField
import cn.wj.android.cashbook.core.design.component.CbScaffold
import cn.wj.android.cashbook.core.design.component.CbTextButton
import cn.wj.android.cashbook.core.design.component.CbTextField
import cn.wj.android.cashbook.core.design.component.CbTopAppBar
import cn.wj.android.cashbook.core.design.component.Loading
import cn.wj.android.cashbook.core.design.component.TextFieldState
import cn.wj.android.cashbook.core.design.icon.CbIcons
import cn.wj.android.cashbook.core.design.theme.PreviewTheme
import cn.wj.android.cashbook.core.model.enums.AutoBackupModeEnum
import cn.wj.android.cashbook.core.model.model.BackupModel
import cn.wj.android.cashbook.core.ui.DevicePreviews
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.settings.viewmodel.BackupAndRecoveryUiState
import cn.wj.android.cashbook.feature.settings.viewmodel.BackupAndRecoveryViewModel

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
    modifier: Modifier = Modifier,
    viewModel: BackupAndRecoveryViewModel = hiltViewModel(),
) {
    val shouldDisplayBookmark by viewModel.shouldDisplayBookmark.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()

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
    onAutoBackupModeSelected: (AutoBackupModeEnum) -> Unit,
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

                        is Int -> {
                            if (uiState is BackupAndRecoveryUiState.Success) {
                                AutoBackupModeDialog(
                                    autoBackupMode = uiState.autoBackup,
                                    onAutoBackupModeSelected = onAutoBackupModeSelected,
                                    onRequestDismissDialog = onRequestDismissDialog,
                                )
                            }
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
        )
    }
}
