/*
 * Copyright 2021 The Cashbook Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.wj.android.cashbook.feature.budget.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import cn.wj.android.cashbook.feature.budget.screen.BudgetRoute

/** 预算管理路由 */
const val ROUTE_BUDGET = "budget"

/**
 * 预算管理界面导航
 *
 * @param onBackClick 返回回调
 */
fun NavGraphBuilder.budgetScreen(onBackClick: () -> Unit) {
    composable(route = ROUTE_BUDGET) {
        BudgetRoute(onBackClick = onBackClick)
    }
}
