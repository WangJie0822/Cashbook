package cn.wj.android.cashbook.core.ui

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder

private val navControllerMap = hashMapOf<NavGraphBuilder, NavController?>()

var NavGraphBuilder.controller: NavController?
    get() = navControllerMap[this]
    set(value) {
        navControllerMap[this] = value
    }