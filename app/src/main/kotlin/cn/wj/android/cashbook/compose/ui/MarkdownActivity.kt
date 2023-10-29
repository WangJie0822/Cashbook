package cn.wj.android.cashbook.compose.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.model.enums.MarkdownTypeEnum
import cn.wj.android.cashbook.feature.settings.screen.MarkdownRoute
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MarkdownActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var uiState: ActivityUiState by mutableStateOf(ActivityUiState.Loading)

        // 更新 uiState
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState
                    .onEach {
                        uiState = it
                    }
                    .collect()
            }
        }

        // 关闭系统装饰窗口，以允许应用自行处理
        enableEdgeToEdge()

        setContent {
            val darkTheme = shouldUseDarkTheme(uiState = uiState)

            // 更新系统 UI 适配主题
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT,
                    ) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        lightScrim,
                        darkScrim,
                    ) { darkTheme },
                )
                onDispose {}
            }

            CashbookTheme(
                darkTheme = darkTheme,
                disableDynamicTheming = shouldDisableDynamicTheming(uiState = uiState),
            ) {
                ProvideLocalState(
                    onBackPressedDispatcher = this.onBackPressedDispatcher,
                ) {
                    MarkdownRoute(
                        markdownType = MarkdownTypeEnum.ordinalOf(
                            intent.getIntExtra(
                                MARKDOWN_TYPE,
                                -1
                            )
                        ),
                        onRequestPopBackStack = ::finish
                    )
                }
            }
        }
    }

    companion object {

        private const val MARKDOWN_TYPE = "markdown_type"

        fun actionStart(context: Context, type: MarkdownTypeEnum) {
            context.startActivity(Intent(context, MarkdownActivity::class.java).apply {
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                putExtra(MARKDOWN_TYPE, type.ordinal)
            })
        }
    }
}