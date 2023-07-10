package cn.wj.android.cashbook.ui

import android.os.Bundle
import android.view.Window
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import cn.wj.android.cashbook.core.common.ext.logger
import cn.wj.android.cashbook.core.design.component.LocalDefaultEmptyImagePainter
import cn.wj.android.cashbook.core.design.component.LocalDefaultLoadingHintPainter
import cn.wj.android.cashbook.core.design.theme.CashbookTheme
import cn.wj.android.cashbook.core.model.enums.DarkModeEnum
import cn.wj.android.cashbook.core.ui.LocalBackPressedDispatcher
import cn.wj.android.cashbook.core.ui.R
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val verifyViewModel: VerifyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        logger().i("onCreate(savedInstanceState)")

        var uiState: MainActivityUiState by mutableStateOf(MainActivityUiState.Loading)

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

        splashScreen.setKeepOnScreenCondition {
            when (uiState) {
                MainActivityUiState.Loading -> true
                is MainActivityUiState.Success -> false
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
                    MainApp(
                        viewModel = verifyViewModel,
                    )
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        logger().i("onStop()")
        verifyViewModel.onActivityStop()
    }
}

@Composable
private fun ProvideLocalState(
    onBackPressedDispatcher: OnBackPressedDispatcher,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalBackPressedDispatcher provides onBackPressedDispatcher,
        LocalDefaultEmptyImagePainter provides painterResource(id = R.drawable.vector_no_data_200),
        LocalDefaultLoadingHintPainter provides stringResource(id = R.string.data_in_loading),
        content = content
    )
}

@Composable
private fun shouldDisableDynamicTheming(
    uiState: MainActivityUiState,
): Boolean = when (uiState) {
    MainActivityUiState.Loading -> false
    is MainActivityUiState.Success -> !uiState.appDataModel.dynamicColor
}

@Composable
private fun shouldUseDarkTheme(
    uiState: MainActivityUiState,
): Boolean = when (uiState) {
    MainActivityUiState.Loading -> isSystemInDarkTheme()
    is MainActivityUiState.Success -> when (uiState.appDataModel.darkMode) {
        DarkModeEnum.FOLLOW_SYSTEM -> isSystemInDarkTheme()
        DarkModeEnum.LIGHT -> false
        DarkModeEnum.DARK -> true
    }
}