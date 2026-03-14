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

package cn.wj.android.cashbook.feature.records.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import cn.wj.android.cashbook.core.common.ext.toMoneyCNY
import cn.wj.android.cashbook.core.design.component.CbPieChart
import cn.wj.android.cashbook.core.design.component.PieSlice
import cn.wj.android.cashbook.core.model.entity.AnalyticsRecordPieEntity

@Composable
internal fun AnalyticsPieChart(
    barCenterText: String,
    dataList: List<AnalyticsRecordPieEntity>,
    pieColorsCompose: List<Color>,
    onPieColorsCompose: List<Color>,
    modifier: Modifier = Modifier,
) {
    var selectedIndex by remember { mutableIntStateOf(-1) }

    val filteredList = remember(dataList) {
        dataList.filter { it.percent > 0.03f }
    }

    val slices = remember(filteredList, pieColorsCompose, onPieColorsCompose) {
        filteredList.mapIndexed { index, entity ->
            PieSlice(
                label = entity.typeName,
                value = entity.percent,
                color = pieColorsCompose[index % pieColorsCompose.size],
                labelColor = onPieColorsCompose[index % onPieColorsCompose.size],
            )
        }
    }

    val centerText = remember(selectedIndex, barCenterText, filteredList) {
        val data = filteredList.getOrNull(selectedIndex)
        if (data != null) {
            "${data.typeName}\n${data.totalAmount.toMoneyCNY()}"
        } else {
            barCenterText
        }
    }

    CbPieChart(
        slices = slices,
        centerText = centerText,
        modifier = modifier,
        selectedIndex = selectedIndex,
        showLeaderLines = true,
        onSliceSelected = { index ->
            selectedIndex = if (index == selectedIndex) -1 else index
        },
    )
}
