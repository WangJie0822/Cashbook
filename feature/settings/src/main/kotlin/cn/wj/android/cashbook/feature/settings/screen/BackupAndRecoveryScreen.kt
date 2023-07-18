package cn.wj.android.cashbook.feature.settings.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.design.component.CashbookScaffold
import cn.wj.android.cashbook.core.design.component.CashbookTopAppBar
import cn.wj.android.cashbook.core.design.component.CompatTextField
import cn.wj.android.cashbook.core.design.component.TextFieldState
import cn.wj.android.cashbook.core.design.component.TransparentListItem
import cn.wj.android.cashbook.core.design.theme.PreviewTheme
import cn.wj.android.cashbook.core.model.enums.AutoBackupModeEnum
import cn.wj.android.cashbook.core.ui.DevicePreviews
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.settings.viewmodel.BackupAndRecoveryUiState
import cn.wj.android.cashbook.feature.settings.viewmodel.BackupAndRecoveryViewModel

@Composable
internal fun BackupAndRecoveryRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BackupAndRecoveryViewModel = hiltViewModel(),
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackupAndRecoveryScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BackupAndRecoveryScreen(
    uiState: BackupAndRecoveryUiState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CashbookScaffold(
        modifier = modifier,
        topBar = {
            CashbookTopAppBar(
                onBackClick = onBackClick,
                text = stringResource(id = R.string.backup_and_recovery),
            )
        },
        content = { paddingValues ->
            BackupAndRecoveryScaffoldContent(
                uiState = uiState,
                modifier = Modifier.padding(paddingValues),
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BackupAndRecoveryScaffoldContent(
    uiState: BackupAndRecoveryUiState,
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
        Text(
            text = stringResource(id = R.string.webdav_setting),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp)
        )

        CompatTextField(
            textFieldState = webDAVDomainTextFieldState,
            label = { Text(text = stringResource(id = R.string.webdav_domain)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .padding(horizontal = 16.dp),
        )
        CompatTextField(
            textFieldState = webDAVAccountTextFieldState,
            label = { Text(text = stringResource(id = R.string.webdav_account)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .padding(horizontal = 16.dp),
        )
        CompatTextField(
            textFieldState = webDAVPasswordTextFieldState,
            label = { Text(text = stringResource(id = R.string.webdav_password)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .padding(horizontal = 16.dp),
        )

        Text(
            text = stringResource(id = R.string.backup_and_recovery),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 32.dp, start = 16.dp),
        )

        TransparentListItem(
            headlineText = { Text(text = stringResource(id = R.string.backup_path)) },
            supportingText = { Text(text = stringResource(id = R.string.click_to_select_backup_path)) },
        )
        TransparentListItem(
            headlineText = { Text(text = stringResource(id = R.string.backup)) },
            supportingText = { Text(text = stringResource(id = R.string.backup_hint)) },
        )
        TransparentListItem(
            headlineText = { Text(text = stringResource(id = R.string.recovery)) },
            supportingText = { Text(text = stringResource(id = R.string.recovery_hint)) },
        )
        TransparentListItem(
            headlineText = { Text(text = stringResource(id = R.string.auto_backup)) },
            supportingText = {
                Text(
                    text = stringResource(
                        id = when (uiState.autoBackup) {
                            AutoBackupModeEnum.CLOSE -> R.string.close
                            AutoBackupModeEnum.WHEN_LAUNCH -> R.string.each_launch
                            AutoBackupModeEnum.EACH_DAY -> R.string.each_day
                            AutoBackupModeEnum.EACH_WEEK -> R.string.each_week
                        }
                    )
                )
            },
        )
    }
}

@DevicePreviews
@Composable
private fun BackupAndRecoveryScreenPreview() {
    PreviewTheme {
        BackupAndRecoveryScreen(
            uiState = BackupAndRecoveryUiState(),
            onBackClick = {},
        )
    }
}