package cn.wj.android.cashbook.feature.record.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import cn.wj.android.cashbook.feature.record.screen.MainRoute

fun NavController.naviToRecordMain() {
    this.navigate("record/main")
}

fun NavGraphBuilder.recordMainScreen() {
    composable(route = "record/main") {
        MainRoute()
    }
}