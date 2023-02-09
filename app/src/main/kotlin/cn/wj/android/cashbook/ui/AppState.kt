package cn.wj.android.cashbook.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope

class AppState(
    val navController: NavHostController,
    val coroutineScope: CoroutineScope,
) {
}

@Composable
fun rememberAppState(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    navController: NavHostController = rememberNavController(),
): AppState {
//    NavigationTrackingSideEffect(navController)
    return remember(navController, coroutineScope) {
        AppState(navController, coroutineScope)
    }
}