package cn.wj.android.cashbook.feature.settings.screen

import android.text.Spanned
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.common.ApplicationInfo
import cn.wj.android.cashbook.core.common.EMAIL_ADDRESS
import cn.wj.android.cashbook.core.common.GITEE_HOMEPAGE
import cn.wj.android.cashbook.core.common.GITHUB_HOMEPAGE
import cn.wj.android.cashbook.core.common.tools.jumpBrowser
import cn.wj.android.cashbook.core.common.tools.jumpSendEmail
import cn.wj.android.cashbook.core.design.component.CashbookScaffold
import cn.wj.android.cashbook.core.design.component.CashbookTopAppBar
import cn.wj.android.cashbook.core.design.component.TranparentListItem
import cn.wj.android.cashbook.core.design.icon.CashbookIcons
import cn.wj.android.cashbook.core.design.theme.LocalExtendedColors
import cn.wj.android.cashbook.core.design.theme.PreviewTheme
import cn.wj.android.cashbook.core.model.entity.UpdateInfoEntity
import cn.wj.android.cashbook.core.ui.DevicePreviews
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.settings.enums.AboutUsBookmarkEnum
import cn.wj.android.cashbook.feature.settings.viewmodel.AboutUsUiState
import cn.wj.android.cashbook.feature.settings.viewmodel.AboutUsViewModel
import io.noties.markwon.Markwon

/**
 * 关于我们界面
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/14
 */
@Composable
internal fun AboutUsRoute(
    onBackClick: () -> Unit,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,
    onVersionInfoClick: () -> Unit,
    onUserAgreementAndPrivacyPolicyClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AboutUsViewModel = hiltViewModel(),
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val shouldDisplayUpdateDialog by viewModel.updateInfoData.collectAsStateWithLifecycle()
    val shouldDisplayNoWifiUpdateDialog by viewModel.confirmUpdateInfoData.collectAsStateWithLifecycle()

    AboutUsScreen(
        onBackClick = onBackClick,
        onShowSnackbar = onShowSnackbar,
        uiState = uiState,
        inRequestUpdateData = viewModel.inRequestUpdateData,
        shouldDisplayBookmark = viewModel.shouldDisplayBookmark,
        shouldDisplayUpdateDialog = shouldDisplayUpdateDialog,
        shouldDisplayNoWifiUpdateDialog = shouldDisplayNoWifiUpdateDialog,
        onUseGiteeSwitch = viewModel::updateUseGitee,
        onAutoCheckUpdateSwitch = viewModel::updateAutoCheckUpdate,
        onCheckUpdate = viewModel::checkUpdate,
        onClearBookmarkState = viewModel::clearBookmarkState,
        onConfirmUpdate = viewModel::confirmUpdate,
        onDismissUpdate = viewModel::clearUpdate,
        onConfirmDownload = viewModel::confirmDownload,
        onDismissDownload = viewModel::clearDownload,
        onVersionInfoClick = onVersionInfoClick,
        onUserAgreementAndPrivacyPolicyClick = onUserAgreementAndPrivacyPolicyClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AboutUsScreen(
    onBackClick: () -> Unit,
    onShowSnackbar: suspend (String, String?) -> SnackbarResult,
    uiState: AboutUsUiState,
    inRequestUpdateData: Boolean,
    shouldDisplayBookmark: AboutUsBookmarkEnum,
    shouldDisplayUpdateDialog: UpdateInfoEntity?,
    shouldDisplayNoWifiUpdateDialog: UpdateInfoEntity?,
    onUseGiteeSwitch: (Boolean) -> Unit,
    onAutoCheckUpdateSwitch: (Boolean) -> Unit,
    onCheckUpdate: () -> Unit,
    onClearBookmarkState: () -> Unit,
    onConfirmUpdate: () -> Unit,
    onDismissUpdate: (Boolean) -> Unit,
    onConfirmDownload: (Boolean) -> Unit,
    onDismissDownload: () -> Unit,
    onVersionInfoClick: () -> Unit,
    onUserAgreementAndPrivacyPolicyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 文本 - 已经是最新版本
    val isLatestVersionText = stringResource(id = R.string.it_is_the_latest_version)
    // 文本 - 更新下载中
    val downloadingText = stringResource(id = R.string.update_downloading_hint)
    // 文本 - 开始下载
    val startDownloadText = stringResource(id = R.string.start_background_download)

    LaunchedEffect(shouldDisplayBookmark) {
        if (shouldDisplayBookmark != AboutUsBookmarkEnum.NONE) {
            val tipsText = when (shouldDisplayBookmark) {
                AboutUsBookmarkEnum.NO_NEED_UPDATE -> isLatestVersionText
                AboutUsBookmarkEnum.UPDATE_DOWNLOADING -> downloadingText
                AboutUsBookmarkEnum.START_DOWNLOAD -> startDownloadText
                else -> ""
            }
            val showSnackbarResult = onShowSnackbar(tipsText, null)
            if (SnackbarResult.Dismissed == showSnackbarResult) {
                onClearBookmarkState.invoke()
            }
        }
    }

    CashbookScaffold(
        topBar = {
            CashbookTopAppBar(
                text = stringResource(id = R.string.about_us),
                onBackClick = onBackClick,
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
        ) {
            // 升级提示弹窗
            if (null != shouldDisplayUpdateDialog) {
                val content = Markwon.create(LocalContext.current)
                    .toMarkdown(shouldDisplayUpdateDialog.versionInfo)
                UpdateHintDialog(
                    content = content,
                    ignoreUpdateVersion = uiState.ignoreUpdateVersion,
                    onConfirmClick = onConfirmUpdate,
                    onDismissClick = onDismissUpdate,
                )
            }
            // 非WiFi下载提示弹窗
            if (null != shouldDisplayNoWifiUpdateDialog) {
                NoWifiUpdateHintDialog(
                    onConfirmClick = onConfirmDownload,
                    onDismissClick = onDismissDownload,
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item {
                    Icon(
                        modifier = Modifier.padding(top = 16.dp),
                        painter = painterResource(id = R.drawable.ic_launcher),
                        contentDescription = null,
                        tint = Color.Unspecified,
                    )
                }
                item {
                    Text(
                        modifier = Modifier.padding(top = 8.dp),
                        text = stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                }
                item {
                    Text(
                        modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp),
                        text = stringResource(id = R.string.about_us_description),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                item {
                    val pleaseSelectEmailText = stringResource(id = R.string.please_select_email)
                    Text(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .clickable {
                                jumpSendEmail(
                                    email = EMAIL_ADDRESS, chooserTitle = pleaseSelectEmailText
                                )
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        text = stringResource(id = R.string.contact_me),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.inversePrimary,
                    )
                }
                item {
                    val pleaseSelectWebBrowserText =
                        stringResource(id = R.string.please_select_web_browser)
                    Row(
                        modifier = Modifier.padding(bottom = 8.dp),
                    ) {
                        Text(
                            modifier = Modifier
                                .clickable {
                                    jumpBrowser(
                                        url = GITHUB_HOMEPAGE,
                                        chooserTitle = pleaseSelectWebBrowserText
                                    )
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            text = stringResource(id = R.string.github),
                            style = MaterialTheme.typography.bodyLarge,
                            color = LocalExtendedColors.current.github,
                        )
                        Text(
                            modifier = Modifier
                                .clickable {
                                    jumpBrowser(
                                        url = GITEE_HOMEPAGE,
                                        chooserTitle = pleaseSelectWebBrowserText
                                    )
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            text = stringResource(id = R.string.gitee),
                            style = MaterialTheme.typography.bodyLarge,
                            color = LocalExtendedColors.current.gitee,
                        )
                    }
                }
                item {
                    Divider(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .fillMaxWidth(),
                    )
                    // 数据源
                    TranparentListItem(
                        headlineText = { Text(text = stringResource(id = R.string.switch_data_source)) },
                        supportingText = { Text(text = stringResource(id = R.string.data_source_hint)) },
                        trailingContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val useGitee = uiState.useGitee
                                Text(
                                    modifier = Modifier.padding(end = 8.dp),
                                    text = stringResource(id = if (useGitee) R.string.gitee else R.string.github),
                                    color = if (useGitee) LocalExtendedColors.current.gitee else LocalExtendedColors.current.github,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Switch(
                                    checked = useGitee,
                                    onCheckedChange = onUseGiteeSwitch,
                                    colors = SwitchDefaults.colors(
                                        uncheckedThumbColor = LocalExtendedColors.current.github,
                                        checkedThumbColor = LocalExtendedColors.current.gitee,
                                    ),
                                )
                            }
                        },
                    )
                    // 自动检查更新
                    TranparentListItem(
                        headlineText = { Text(text = stringResource(id = R.string.auto_check_update)) },
                        trailingContent = {
                            Switch(
                                checked = uiState.autoCheckUpdate,
                                onCheckedChange = onAutoCheckUpdateSwitch,
                            )
                        },
                    )
                    // 检查更新
                    val checkUpdateEnable = !inRequestUpdateData
                    val checkUpdateModifier = if (checkUpdateEnable) {
                        Modifier.clickable { onCheckUpdate.invoke() }
                    } else {
                        Modifier
                    }
                    val textColor = if (checkUpdateEnable) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    }
                    TranparentListItem(
                        modifier = checkUpdateModifier,
                        headlineText = {
                            Text(
                                text = stringResource(id = R.string.check_update),
                                color = textColor,
                            )
                        },
                    )
                    // 版本信息
                    TranparentListItem(
                        modifier = Modifier.clickable { onVersionInfoClick.invoke() },
                        headlineText = {
                            Text(text = stringResource(id = R.string.version_info))
                        },
                        trailingContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    modifier = Modifier.padding(end = 8.dp),
                                    text = ApplicationInfo.versionName,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Icon(
                                    imageVector = CashbookIcons.KeyboardArrowRight,
                                    contentDescription = null,
                                )
                            }
                        },
                    )
                    // 用户协议和隐私政策
                    TranparentListItem(
                        modifier = Modifier.clickable { onUserAgreementAndPrivacyPolicyClick.invoke() },
                        headlineText = {
                            Text(text = stringResource(id = R.string.user_agreement_and_privacy_policy))
                        },
                        trailingContent = {
                            Icon(
                                imageVector = CashbookIcons.KeyboardArrowRight,
                                contentDescription = null,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun UpdateHintDialog(
    content: Spanned,
    ignoreUpdateVersion: Boolean,
    onConfirmClick: () -> Unit,
    onDismissClick: (Boolean) -> Unit,
) {
    var ignore by remember {
        mutableStateOf(ignoreUpdateVersion)
    }
    AlertDialog(
        onDismissRequest = { onDismissClick.invoke(false) },
        text = {
            Column {
                Text(
                    text = AnnotatedString.Builder()
                        .append(content)
                        .toAnnotatedString(),
                )
                Row(
                    modifier = Modifier
                        .clickable { ignore = !ignore }
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = ignore, onCheckedChange = { ignore = it })
                    Text(text = stringResource(id = R.string.ignore_this_version))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirmClick) {
                Text(text = stringResource(id = R.string.update))
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismissClick.invoke(ignore) }) {
                Text(text = stringResource(id = R.string.cancel))
            }
        },
    )
}

@Composable
internal fun NoWifiUpdateHintDialog(
    onConfirmClick: (Boolean) -> Unit,
    onDismissClick: () -> Unit,
) {
    var noMOrePrompt by remember {
        mutableStateOf(false)
    }
    AlertDialog(
        onDismissRequest = onDismissClick,
        text = {
            Column {
                Text(
                    text = stringResource(id = R.string.no_wifi_download_available_hint),
                )
                Row(
                    modifier = Modifier
                        .clickable { noMOrePrompt = !noMOrePrompt }
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = noMOrePrompt, onCheckedChange = { noMOrePrompt = it })
                    Text(text = stringResource(id = R.string.no_more_prompt))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirmClick.invoke(noMOrePrompt) }) {
                Text(text = stringResource(id = R.string.update))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissClick) {
                Text(text = stringResource(id = R.string.cancel))
            }
        },
    )
}

@DevicePreviews
@Composable
private fun AboutUsScreenPreview() {
    PreviewTheme {
        AboutUsScreen(
            onBackClick = {},
            onShowSnackbar = { _, _ -> SnackbarResult.Dismissed },
            uiState = AboutUsUiState.Success(
                useGitee = true,
                autoCheckUpdate = false,
                ignoreUpdateVersion = false,
            ),
            inRequestUpdateData = false,
            shouldDisplayBookmark = AboutUsBookmarkEnum.NONE,
            shouldDisplayUpdateDialog = null,
            shouldDisplayNoWifiUpdateDialog = null,
            onUseGiteeSwitch = {},
            onAutoCheckUpdateSwitch = {},
            onCheckUpdate = {},
            onClearBookmarkState = {},
            onConfirmUpdate = {},
            onDismissUpdate = {},
            onConfirmDownload = {},
            onDismissDownload = {},
            onVersionInfoClick = {},
            onUserAgreementAndPrivacyPolicyClick = {},
        )
    }
}