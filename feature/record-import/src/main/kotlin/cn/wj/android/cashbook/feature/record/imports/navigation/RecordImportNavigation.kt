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

package cn.wj.android.cashbook.feature.record.imports.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import cn.wj.android.cashbook.feature.record.imports.screen.RecordImportRoute

/** 账单导入路由 */
private const val ROUTE_RECORD_IMPORT = "record_import/{fileUri}"

/** 路由参数 - 文件 URI */
private const val KEY_FILE_URI = "fileUri"

/**
 * 跳转到账单导入界面
 *
 * @param fileUri 选择的账单文件路径（需要 URL 编码）
 */
fun NavController.naviToRecordImport(fileUri: String) {
    this.navigate("record_import/${java.net.URLEncoder.encode(fileUri, "UTF-8")}")
}

/**
 * 账单导入界面
 *
 * @param onRequestPopBackStack 导航到上一级
 * @param onImportResult 导入完成回调，参数为提示消息；回调负责返回上一级并展示提示
 */
fun NavGraphBuilder.recordImportScreen(
    onRequestPopBackStack: () -> Unit,
    onImportResult: (String) -> Unit,
) {
    composable(
        route = ROUTE_RECORD_IMPORT,
        arguments = listOf(
            navArgument(KEY_FILE_URI) {
                type = NavType.StringType
            },
        ),
    ) {
        RecordImportRoute(
            onRequestPopBackStack = onRequestPopBackStack,
            onImportResult = onImportResult,
        )
    }
}
