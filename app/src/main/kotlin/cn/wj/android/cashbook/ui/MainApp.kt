package cn.wj.android.cashbook.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumedWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import cn.wj.android.cashbook.core.design.component.CashbookBackground
import cn.wj.android.cashbook.core.model.enums.LauncherMenuAction
import cn.wj.android.cashbook.feature.record.navigation.LauncherCollapsedTitleContent
import cn.wj.android.cashbook.feature.record.navigation.LauncherContent
import cn.wj.android.cashbook.feature.record.navigation.LauncherPinnedTitleContent
import cn.wj.android.cashbook.feature.settings.navigation.ROUTE_SETTINGS_LAUNCHER
import cn.wj.android.cashbook.feature.settings.navigation.settingsLauncherScreen

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
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
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
                settingsLauncherScreen(
                    onMenuClick = { action ->
                        when (action) {
                            LauncherMenuAction.SEARCH -> {

                            }

                            LauncherMenuAction.CALENDAR -> {

                            }

                            LauncherMenuAction.MY_ASSET -> {

                            }

                            LauncherMenuAction.MY_BOOK -> {

                            }

                            LauncherMenuAction.MY_CATEGORY -> {

                            }

                            LauncherMenuAction.MY_TAG -> {

                            }

                            LauncherMenuAction.SETTING -> {

                            }

                            LauncherMenuAction.ABOUT_US -> {

                            }

                            else -> {}
                        }
                    },
                    pinnedTitle = { LauncherPinnedTitleContent() },
                    collapsedTitle = { LauncherCollapsedTitleContent() },
                    content = { modifier ->
                        LauncherContent(modifier = modifier)
                    },
                )
            }
        }
    }
}