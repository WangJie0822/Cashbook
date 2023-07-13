package cn.wj.android.cashbook.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Window
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import cn.wj.android.cashbook.core.design.component.CashbookGradientBackground
import cn.wj.android.cashbook.core.design.component.CashbookScaffold
import cn.wj.android.cashbook.core.design.component.CashbookTopAppBar
import cn.wj.android.cashbook.core.design.component.Empty
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.enums.MarkdownTypeEnum
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.android.AndroidEntryPoint
import io.noties.markwon.Markwon
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MarkdownActivity : AppCompatActivity() {

    private val viewModel: MarkdownViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        viewModel.updateMarkdownType(intent.getIntExtra(EXTRA_TYPE, -1))

        var uiState: ActivityUiState by mutableStateOf(ActivityUiState.Loading)

        // Update the uiState
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState
                    .onEach {
                        uiState = it
                    }
                    .collect()
            }
        }

        setContent {
            val systemUiController = rememberSystemUiController()
            val darkTheme = shouldUseDarkTheme(uiState = uiState)

            // 更新系统 UI 适配主题
            DisposableEffect(systemUiController, darkTheme) {
                systemUiController.systemBarsDarkContentEnabled = !darkTheme
                onDispose {}
            }

            CashbookTheme(
                darkTheme = darkTheme,
                disableDynamicTheming = shouldDisableDynamicTheming(uiState = uiState),
            ) {
                ProvideLocalState(
                    onBackPressedDispatcher = this.onBackPressedDispatcher,
                ) {
                    CashbookGradientBackground {

                        val markdownData by viewModel.markdownData.collectAsStateWithLifecycle()

                        MarkdownScreen(
                            markdownData = markdownData,
                            onBackClick = { finish() },
                        )
                    }
                }
            }
        }
    }

    companion object {

        private const val EXTRA_TYPE = "extra_type"

        fun actionStart(context: Context, mdType: MarkdownTypeEnum) {
            context.startActivity(Intent(context, MarkdownActivity::class.java).apply {
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                putExtra(EXTRA_TYPE, mdType.ordinal)
            })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MarkdownScreen(
    markdownData: String,
    onBackClick: () -> Unit,
) {
    CashbookScaffold(
        topBar = {
            CashbookTopAppBar(
                onBackClick = onBackClick,
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier.padding(paddingValues),
        ) {
            if (markdownData.isBlank()) {
                Empty(hintText = stringResource(id = R.string.markdown_no_data_hint))
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

