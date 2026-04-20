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

package cn.wj.android.cashbook.feature.schedule.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.feature.schedule.screen.EditScheduleRoute
import cn.wj.android.cashbook.feature.schedule.screen.MySchedulesRoute
import kotlinx.serialization.Serializable

/** 周期记账 */
@Serializable
object MySchedules

/** 编辑周期规则 */
@Serializable
data class EditSchedule(val scheduleId: Long = -1L)

fun NavController.naviToMySchedules() {
    this.navigate(MySchedules)
}

fun NavController.naviToEditSchedule(scheduleId: Long = -1L) {
    this.navigate(EditSchedule(scheduleId = scheduleId))
}

/** 周期记账列表界面 */
fun NavGraphBuilder.mySchedulesScreen(
    onRequestNaviToEditSchedule: (Long) -> Unit,
    onRequestPopBackStack: () -> Unit,
) {
    composable<MySchedules> {
        MySchedulesRoute(
            onRequestNaviToEditSchedule = onRequestNaviToEditSchedule,
            onRequestPopBackStack = onRequestPopBackStack,
        )
    }
}

/** 编辑周期规则界面 */
fun NavGraphBuilder.editScheduleScreen(
    typeListContent: @Composable (typeCategory: RecordTypeCategoryEnum, currentTypeId: Long, onTypeChange: (Long) -> Unit) -> Unit,
    assetBottomSheetContent: @Composable (currentTypeId: Long, selectedAssetId: Long, onAssetChange: (Long) -> Unit) -> Unit,
    tagBottomSheetContent: @Composable (selectedTagIdList: List<Long>, onTagIdListChange: (List<Long>) -> Unit, onRequestDismissSheet: () -> Unit) -> Unit,
    onRequestPopBackStack: () -> Unit,
) {
    composable<EditSchedule> { backStackEntry ->
        val route = backStackEntry.toRoute<EditSchedule>()
        EditScheduleRoute(
            scheduleId = route.scheduleId,
            typeListContent = typeListContent,
            assetBottomSheetContent = assetBottomSheetContent,
            tagBottomSheetContent = tagBottomSheetContent,
            onRequestPopBackStack = onRequestPopBackStack,
        )
    }
}
