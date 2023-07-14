package cn.wj.android.cashbook.feature.settings.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.design.component.CashbookScaffold
import cn.wj.android.cashbook.core.design.component.CashbookTopAppBar
import cn.wj.android.cashbook.core.design.component.Empty
import cn.wj.android.cashbook.core.design.theme.PreviewTheme
import cn.wj.android.cashbook.core.model.enums.MarkdownTypeEnum
import cn.wj.android.cashbook.core.ui.DevicePreviews
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.settings.viewmodel.MarkdownViewModel
import io.noties.markwon.Markwon

@Composable
internal fun MarkdownRoute(
    markdownType: MarkdownTypeEnum?,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MarkdownViewModel = hiltViewModel<MarkdownViewModel>().apply {
        updateMarkdownType(markdownType)
    }
) {

    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val markdownData by viewModel.markdownData.collectAsStateWithLifecycle()

    MarkdownScreen(
        isSyncing = isSyncing,
        markdownData = markdownData,
        onRetryClick = viewModel::onRetryClick,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MarkdownScreen(
    isSyncing: Boolean,
    markdownData: String,
    onRetryClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CashbookScaffold(
        modifier = modifier,
        topBar = {
            CashbookTopAppBar(
                onBackClick = onBackClick,
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
        ) {
            if (markdownData.isBlank()) {
                Empty(
                    hintText = stringResource(id = R.string.markdown_no_data_hint),
                    button = {
                        FilledTonalButton(onClick = onRetryClick) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(text = stringResource(id = R.string.retry))
                        }
                    },
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            } else {
                Text(
                    text = buildAnnotatedString {
                        append(Markwon.create(LocalContext.current).toMarkdown(markdownData))
                    },
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(state = rememberScrollState()),
                )
            }
        }
    }
}

@DevicePreviews
@Composable
private fun MarkdownScreenPreview() {
    PreviewTheme {
        MarkdownScreen(
            isSyncing = false,
            markdownData = """
            # 标题一
            ## 标题二
            * 重点内容 **加粗** *斜体* `code`
            正常内容
        """.trimIndent(),
            onRetryClick = {},
            onBackClick = {},
        )
    }
}

@DevicePreviews
@Composable
private fun MarkdownScreenEmptyPreview() {
    PreviewTheme(
        defaultEmptyImagePainter = painterResource(id = R.drawable.vector_no_data_200)
    ) {
        MarkdownScreen(
            isSyncing = true,
            markdownData = "",
            onRetryClick = {},
            onBackClick = {},
        )
    }
}