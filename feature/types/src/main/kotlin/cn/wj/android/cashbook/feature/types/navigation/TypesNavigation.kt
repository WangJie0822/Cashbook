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

package cn.wj.android.cashbook.feature.types.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.feature.types.screen.EditRecordTypeListRoute
import cn.wj.android.cashbook.feature.types.screen.MyCategoriesRoute

private const val ROUTE_MY_CATEGORIES = "type/my_categories"

fun NavController.naviToMyCategories() {
    this.navigate(ROUTE_MY_CATEGORIES)
}

/**
 * 我的分类界面
 *
 * @param onRequestPopBackStack 导航到上一级
 */
fun NavGraphBuilder.myCategoriesScreen(
    onRequestNaviToTypeStatistics: (Long) -> Unit,
    onRequestPopBackStack: () -> Unit,
) {
    composable(ROUTE_MY_CATEGORIES) {
        MyCategoriesRoute(
            onRequestNaviToTypeStatistics = onRequestNaviToTypeStatistics,
            onRequestPopBackStack = onRequestPopBackStack,
        )
    }
}

/**
 * 编辑记录页面标签列表
 *
 * @param typeCategory 记录大类
 * @param defaultTypeId 默认类型 id
 * @param onTypeSelect 类型选中回调
 * @param onRequestNaviToTypeManager 导航到类型管理
 */
@Composable
fun EditRecordTypeListContent(
    typeCategory: RecordTypeCategoryEnum,
    defaultTypeId: Long,
    onTypeSelect: (Long) -> Unit,
    onRequestNaviToTypeManager: () -> Unit,
) {
    EditRecordTypeListRoute(
        typeCategory = typeCategory,
        defaultTypeId = defaultTypeId,
        onTypeSelect = onTypeSelect,
        onRequestNaviToTypeManager = onRequestNaviToTypeManager,
    )
}
