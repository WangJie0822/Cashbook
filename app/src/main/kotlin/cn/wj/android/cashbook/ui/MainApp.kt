package cn.wj.android.cashbook.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumedWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import cn.wj.android.cashbook.core.design.component.CashbookBackground
import cn.wj.android.cashbook.core.ui.onBackground
import cn.wj.android.cashbook.feature.record.navigation.LauncherContent
import cn.wj.android.cashbook.feature.settings.navigation.ROUTE_SETTINGS_LAUNCHER
import cn.wj.android.cashbook.feature.settings.navigation.settingsLauncherScreen
import kotlinx.coroutines.launch

private const val START_DESTINATION = ROUTE_SETTINGS_LAUNCHER

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class
)
@Composable
fun MainApp() {
    CashbookBackground {
        val navController = rememberNavController()
        val coroutineScope = rememberCoroutineScope()
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = Color.onBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { paddingValues ->
            NavHost(
                navController = navController,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .consumedWindowInsets(paddingValues),
                startDestination = START_DESTINATION
            ) {
                settingsLauncherScreen { modifier, drawerState ->
                    LauncherContent(
                        modifier = modifier,
                        onMenuIconClick = {
                            coroutineScope.launch {
                                drawerState.open()
                            }
                        },
                        onSearchIconClick = {

                        },
                        onCalendarIconClick = {

                        },
                        onAssetIconClick = {

                        },
                    )
                }
            }
        }
    }
}