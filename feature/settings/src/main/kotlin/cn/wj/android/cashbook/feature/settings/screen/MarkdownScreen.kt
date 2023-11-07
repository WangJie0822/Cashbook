package cn.wj.android.cashbook.feature.settings.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.design.component.CashbookGradientBackground
import cn.wj.android.cashbook.core.design.component.CashbookScaffold
import cn.wj.android.cashbook.core.design.component.CashbookTopAppBar
import cn.wj.android.cashbook.core.design.component.Empty
import cn.wj.android.cashbook.core.model.enums.MarkdownTypeEnum
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.settings.viewmodel.MarkdownViewModel
import io.noties.markwon.Markwon

/**
 * Markdown 显示界面
 *
 * @param markdownType 数据类型
 * @param onRequestPopBackStack 导航到上一级
 */
@Composable
fun MarkdownRoute(
    modifier: Modifier = Modifier,
    markdownType: MarkdownTypeEnum?,
    onRequestPopBackStack: () -> Unit,
    viewModel: MarkdownViewModel = hiltViewModel<MarkdownViewModel>().apply {
        updateMarkdownType(markdownType)
    }
) {

    val markdownData by viewModel.markdownData.collectAsStateWithLifecycle()

    MarkdownScreen(
        title = when (markdownType) {
            MarkdownTypeEnum.CHANGELOG -> stringResource(id = R.string.version_info)
            MarkdownTypeEnum.PRIVACY_POLICY -> stringResource(id = R.string.user_agreement_and_privacy_policy)
            else -> stringResource(id = R.string.unknown_type)
        },
        markdownData = markdownData,
        onBackClick = onRequestPopBackStack,
        modifier = modifier,
    )
}

/**
 * Markdown 显示界面
 *
 * @param title 标题文本
 * @param markdownData Markdown 显示数据
 * @param onBackClick 返回点击回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MarkdownScreen(
    title: String,
    markdownData: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CashbookGradientBackground {
        CashbookScaffold(
            modifier = modifier,
            topBar = {
                CashbookTopAppBar(title = { Text(text = title) }, onBackClick = onBackClick)
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
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                } else {
                    Text(
                        text = buildAnnotatedString {
                            append(Markwon.create(LocalContext.current).toMarkdown(markdownData))
                        },
                        modifier = Modifier
                            .verticalScroll(state = rememberScrollState())
                            .padding(16.dp),
                    )
                }
            }
        }
    }
}