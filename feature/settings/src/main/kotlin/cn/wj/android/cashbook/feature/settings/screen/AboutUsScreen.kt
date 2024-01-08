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

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.wj.android.cashbook.core.common.ApplicationInfo
import cn.wj.android.cashbook.core.common.EMAIL_ADDRESS
import cn.wj.android.cashbook.core.common.GITEE_HOMEPAGE
import cn.wj.android.cashbook.core.common.GITEE_LATEST
import cn.wj.android.cashbook.core.common.GITHUB_HOMEPAGE
import cn.wj.android.cashbook.core.common.GITHUB_LATEST
import cn.wj.android.cashbook.core.common.tools.jumpBrowser
import cn.wj.android.cashbook.core.common.tools.jumpSendEmail
import cn.wj.android.cashbook.core.design.component.CbAlertDialog
import cn.wj.android.cashbook.core.design.component.CbDivider
import cn.wj.android.cashbook.core.design.component.CbListItem
import cn.wj.android.cashbook.core.design.component.CbScaffold
import cn.wj.android.cashbook.core.design.component.CbTextButton
import cn.wj.android.cashbook.core.design.component.CbTopAppBar
import cn.wj.android.cashbook.core.design.icon.CbIcons
import cn.wj.android.cashbook.core.design.theme.LocalExtendedColors
import cn.wj.android.cashbook.core.model.enums.LogcatState
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.settings.viewmodel.AboutUsUiState
import cn.wj.android.cashbook.feature.settings.viewmodel.AboutUsViewModel
import cn.wj.android.cashbook.feature.settings.viewmodel.MainAppViewModel

/**
 * 关于我们界面
 *
 * @param onRequestNaviToChangelog 导航到修改日志
 * @param onRequestNaviToPrivacyPolicy 导航到用户隐私协议
 * @param onRequestPopBackStack 导航到上一级
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/14
 */
@Composable
internal fun AboutUsRoute(
    modifier: Modifier = Modifier,
    onRequestNaviToChangelog: () -> Unit,
    onRequestNaviToPrivacyPolicy: () -> Unit,
    onRequestPopBackStack: () -> Unit,
    viewModel: AboutUsViewModel = hiltViewModel(),
    mainViewModel: MainAppViewModel = viewModel(LocalContext.current as ViewModelStoreOwner),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AboutUsScreen(
        logcatDialogState = viewModel.logcatDialogState,
        onRequestUpdateLogcatState = viewModel::updateLogcatState,
        onRequestDismissDialog = viewModel::dismissDialog,
        uiState = uiState,
        inRequestUpdateData = mainViewModel.inRequestUpdateData,
        onNameClick = viewModel::countNameClicks,
        onUseGiteeSwitch = viewModel::updateUseGitee,
        onCanarySwitch = viewModel::updateCanary,
        onAutoCheckUpdateSwitch = viewModel::updateAutoCheckUpdate,
        onCheckUpdateClick = mainViewModel::checkUpdate,
        onVersionInfoClick = onRequestNaviToChangelog,
        onUserAgreementAndPrivacyPolicyClick = onRequestNaviToPrivacyPolicy,
        onBackClick = onRequestPopBackStack,
        modifier = modifier,
    )
}

/**
 * 关于我们界面
 *
 * @param uiState 界面 UI 状态
 * @param inRequestUpdateData 标记，是否正在更新数据
 * @param onUseGiteeSwitch 数据源切换回调
 * @param onAutoCheckUpdateSwitch 自动检测更新开关切换回调
 * @param onCheckUpdateClick 检查更新点击
 * @param onVersionInfoClick 版本信息点击回调
 * @param onUserAgreementAndPrivacyPolicyClick 用户隐私协议点击回调
 * @param onBackClick 返回点击回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AboutUsScreen(
    logcatDialogState: DialogState,
    onRequestUpdateLogcatState: (LogcatState) -> Unit,
    onRequestDismissDialog: () -> Unit,
    uiState: AboutUsUiState,
    inRequestUpdateData: Boolean,
    onNameClick: () -> Unit,
    onUseGiteeSwitch: (Boolean) -> Unit,
    onCanarySwitch: (Boolean) -> Unit,
    onAutoCheckUpdateSwitch: (Boolean) -> Unit,
    onCheckUpdateClick: () -> Unit,
    onVersionInfoClick: () -> Unit,
    onUserAgreementAndPrivacyPolicyClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CbScaffold(
        topBar = {
            CbTopAppBar(
                onBackClick = onBackClick,
                title = { Text(text = stringResource(id = R.string.about_us)) },
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
        ) {
            ((logcatDialogState as? DialogState.Shown<*>)?.data as? LogcatState)?.let { state ->
                var logcatState: LogcatState by remember(state) {
                    mutableStateOf(state)
                }
                CbAlertDialog(
                    onDismissRequest = onRequestDismissDialog,
                    title = { Text(text = stringResource(id = R.string.logcat_state)) },
                    text = {
                        Column(
                            modifier = Modifier.selectableGroup(),
                        ) {
                            LogcatState.entries.forEach { enum ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .selectable(
                                            selected = (enum == logcatState),
                                            onClick = { logcatState = enum },
                                            role = Role.RadioButton,
                                        )
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RadioButton(selected = enum == logcatState, onClick = null)
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
                        CbTextButton(onClick = { onRequestUpdateLogcatState(logcatState) }) {
                            Text(text = stringResource(id = R.string.save))
                        }
                    },
                    dismissButton = {
                        CbTextButton(onClick = onRequestDismissDialog) {
                            Text(text = stringResource(id = R.string.close))
                        }
                    },
                )
            }

            val pleaseSelectWebBrowserText = stringResource(id = R.string.please_select_web_browser)
            val currentContext = LocalContext.current
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item {
                    Image(
                        modifier = Modifier
                            .size(100.dp)
                            .padding(top = 16.dp),
                        painter = painterResource(id = R.drawable.ic_launcher),
                        contentDescription = null,
                    )
                }
                item {
                    Text(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .clickable(onClick = onNameClick),
                        text = stringResource(id = R.string.app_name_shown),
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
                                    email = EMAIL_ADDRESS,
                                    chooserTitle = pleaseSelectEmailText,
                                    context = currentContext,
                                )
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        text = stringResource(id = R.string.contact_me),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.inversePrimary,
                    )
                }
                item {
                    Row(
                        modifier = Modifier.padding(bottom = 8.dp),
                    ) {
                        Text(
                            modifier = Modifier
                                .clickable {
                                    jumpBrowser(
                                        url = GITHUB_HOMEPAGE,
                                        chooserTitle = pleaseSelectWebBrowserText,
                                        context = currentContext,
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
                                        chooserTitle = pleaseSelectWebBrowserText,
                                        context = currentContext,
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
                    CbDivider(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .fillMaxWidth(),
                    )
                    // 数据源
                    CbListItem(
                        headlineContent = { Text(text = stringResource(id = R.string.switch_data_source)) },
                        supportingContent = { Text(text = stringResource(id = R.string.data_source_hint)) },
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
                    if (!ApplicationInfo.isOffline) {
                        // 实验版本
                        CbListItem(
                            headlineContent = { Text(text = stringResource(id = R.string.canary_version)) },
                            supportingContent = { Text(text = stringResource(id = R.string.canary_version_hint)) },
                            trailingContent = {
                                Switch(
                                    checked = uiState.canary,
                                    onCheckedChange = onCanarySwitch,
                                )
                            },
                        )
                        // 自动检查更新
                        CbListItem(
                            headlineContent = { Text(text = stringResource(id = R.string.auto_check_update)) },
                            trailingContent = {
                                Switch(
                                    checked = uiState.autoCheckUpdate,
                                    onCheckedChange = onAutoCheckUpdateSwitch,
                                )
                            },
                        )
                    }
                    // 检查更新
                    val checkUpdateEnable = !inRequestUpdateData
                    val checkUpdateModifier = if (checkUpdateEnable) {
                        Modifier.clickable {
                            if (ApplicationInfo.isOffline) {
                                jumpBrowser(
                                    url = if (uiState.useGitee) GITEE_LATEST else GITHUB_LATEST,
                                    chooserTitle = pleaseSelectWebBrowserText,
                                    context = currentContext,
                                )
                            } else {
                                onCheckUpdateClick()
                            }
                        }
                    } else {
                        Modifier
                    }
                    val textColor = if (checkUpdateEnable) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    }
                    CbListItem(
                        modifier = checkUpdateModifier,
                        headlineContent = {
                            Text(
                                text = stringResource(id = R.string.check_update),
                                color = textColor,
                            )
                        },
                    )
                    // 版本信息
                    CbListItem(
                        modifier = Modifier.clickable { onVersionInfoClick.invoke() },
                        headlineContent = {
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
                                    imageVector = CbIcons.KeyboardArrowRight,
                                    contentDescription = null,
                                )
                            }
                        },
                    )
                    // 用户协议和隐私政策
                    CbListItem(
                        modifier = Modifier.clickable { onUserAgreementAndPrivacyPolicyClick.invoke() },
                        headlineContent = {
                            Text(text = stringResource(id = R.string.user_agreement_and_privacy_policy))
                        },
                        trailingContent = {
                            Icon(
                                imageVector = CbIcons.KeyboardArrowRight,
                                contentDescription = null,
                            )
                        },
                    )
                }
            }
        }
    }
}

/** 枚举对应文本 */
internal val LogcatState.text: String
    @Composable get() = stringResource(
        id = when (this) {
            LogcatState.NONE -> R.string.close
            LogcatState.ONCE -> R.string.logcat_once
            LogcatState.ALWAYS -> R.string.logcat_always
        },
    )
