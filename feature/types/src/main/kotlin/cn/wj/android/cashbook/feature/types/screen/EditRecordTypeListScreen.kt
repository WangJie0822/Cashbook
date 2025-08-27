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

package cn.wj.android.cashbook.feature.types.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.common.RECORD_TYPE_COLUMNS
import cn.wj.android.cashbook.core.common.tools.funLogger
import cn.wj.android.cashbook.core.design.component.CbVerticalGrid
import cn.wj.android.cashbook.core.design.component.CbVerticalGridRowExpansion
import cn.wj.android.cashbook.core.design.component.painterDrawableResource
import cn.wj.android.cashbook.core.design.theme.fixedContainerColorFor
import cn.wj.android.cashbook.core.model.entity.RECORD_TYPE_SETTINGS
import cn.wj.android.cashbook.core.model.entity.RecordTypeEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.ui.DevicePreviews
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.core.ui.component.TypeIcon
import cn.wj.android.cashbook.core.ui.expand.typeColor
import cn.wj.android.cashbook.feature.types.viewmodel.EditRecordTypeListViewModel

/**
 * 编辑记录页面标签列表
 *
 * @param typeCategory 记录大类
 * @param defaultTypeId 默认类型 id
 * @param onTypeSelect 类型选中回调
 * @param onRequestNaviToTypeManager 导航到类型管理
 */
@Composable
internal fun EditRecordTypeListRoute(
    typeCategory: RecordTypeCategoryEnum,
    defaultTypeId: Long,
    onTypeSelect: (Long) -> Unit,
    onRequestNaviToTypeManager: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditRecordTypeListViewModel = hiltViewModel<EditRecordTypeListViewModel>().apply {
        update(typeCategory, defaultTypeId)
    },
) {
    val currentTypeCategory by viewModel.currentTypeCategoryData.collectAsStateWithLifecycle()
    val typeList by viewModel.typeListData.collectAsStateWithLifecycle()

    val currentSelectedTypeId by viewModel.currentSelectedTypeId.collectAsStateWithLifecycle()

    LaunchedEffect(currentSelectedTypeId) {
        if (currentSelectedTypeId != -1L) {
            onTypeSelect(currentSelectedTypeId)
        }
    }

    EditRecordTypeListScreen(
        currentTypeCategory = currentTypeCategory,
        typeList = typeList,
        onTypeSelect = viewModel::updateTypeId,
        onTypeSettingClick = onRequestNaviToTypeManager,
        modifier = modifier,
    )
}

/**
 * 编辑记录页面标签列表
 *
 * @param onTypeSelect 类型选中回调
 * @param onTypeSettingClick 类型设置点击
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun EditRecordTypeListScreen(
    currentTypeCategory: RecordTypeCategoryEnum,
    typeList: List<RecordTypeEntity>,
    onTypeSelect: (Long) -> Unit,
    onTypeSettingClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 等待typeList加载完成，确保CbVerticalGridRowExpansion的defaultExpandedIndex正确
    if (typeList.isEmpty()) {
        return
    }
    val typeColor = currentTypeCategory.typeColor
    val primaryList = remember(typeList) {
        typeList.filter { it.parentId == -1L }
    }
    val defaultExpandedIndex = remember(primaryList) {
        primaryList.indexOfFirst {
            funLogger().i("aaa name=${it.name}")
            it.selected or it.child.any { v -> v.selected }
        }
    }
    val clickCallback: (RecordTypeEntity) -> Unit = { type ->
        val selected = if (!type.selected) {
            // 当前为选中，更新为选中
            type.copy(selected = true)
        } else {
            // 当前已选中，取消选中
            if (type.parentId != -1L) {
                // 二级分类，选择父类型
                (typeList.firstOrNull { it.id == type.parentId } ?: typeList.first())
                    .copy(selected = true)
            } else {
                // 一级分类，无法取消，不做处理
                null
            }
        }
        selected?.let { onTypeSelect(it.id) }
    }

    CbVerticalGridRowExpansion(
        modifier = modifier,
        columns = RECORD_TYPE_COLUMNS,
        items = primaryList,
        defaultExpandedIndex = defaultExpandedIndex,
        expandedRowContent = { child ->
            CbVerticalGrid(
                modifier = Modifier,
                columns = RECORD_TYPE_COLUMNS,
                items = child.child,
            ) { type ->
                TypeItem(
                    modifier = Modifier.fillMaxWidth(),
                    first = false,
                    shapeType = 0,
                    iconPainter = painterDrawableResource(idStr = type.iconResName),
                    typeColor = typeColor,
                    showMore = false,
                    title = type.name,
                    selected = type.selected,
                    onTypeClick = { clickCallback(type) },
                )
            }
        },
    ) { type, expandCallback ->
        if (type == RECORD_TYPE_SETTINGS) {
            // 设置项
            TypeItem(
                modifier = Modifier.fillMaxWidth(),
                first = true,
                shapeType = type.shapeType,
                iconPainter = painterResource(id = R.drawable.vector_baseline_settings_24),
                typeColor = typeColor,
                showMore = false,
                title = stringResource(id = R.string.settings),
                selected = true,
                onTypeClick = onTypeSettingClick,
            )
        } else {
            TypeItem(
                modifier = Modifier.fillMaxWidth(),
                first = true,
                shapeType = type.shapeType,
                iconPainter = painterDrawableResource(idStr = type.iconResName),
                typeColor = typeColor,
                showMore = type.child.isNotEmpty(),
                title = type.name,
                selected = type.selected,
                onTypeClick = {
                    expandCallback()
                    clickCallback(type)
                },
            )
        }
    }
}

/**
 * 类型列表 item
 *
 * @param first 是否是一级类型
 * @param shapeType 背景 shape 类型
 * @param iconPainter 显示图片
 * @param showMore 是否显示更多标记
 * @param title 类型标题
 * @param selected 是否被选中
 * @param onTypeClick 类型点击回调
 */
@Composable
internal fun TypeItem(
    first: Boolean,
    shapeType: Int,
    iconPainter: Painter,
    typeColor: Color,
    showMore: Boolean,
    title: String,
    selected: Boolean,
    onTypeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 背景颜色，用于区分一级分类、二级分类
    val backgroundColor =
        if (first) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
    val backgroundShape = if (first) {
        RectangleShape
    } else {
        when (shapeType) {
            -1 -> RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
            1 -> RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
            else -> RectangleShape
        }
    }
    // 列表数据
    Column(
        modifier = modifier
            .background(color = backgroundColor, shape = backgroundShape)
            .clickable(onClick = onTypeClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 根据选中状态显示主要颜色
        val color = if (selected) typeColor else fixedContainerColorFor(color = typeColor)
        // 记录类型对应的图标，使用圆形边框
        TypeIcon(
            painter = iconPainter,
            containerColor = color,
            showMore = showMore,
        )
        // 类型名称
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@DevicePreviews
@Composable
private fun EditRecordTypeListScreenPreview() {
    EditRecordTypeListScreen(
        currentTypeCategory = RecordTypeCategoryEnum.EXPENDITURE,
        typeList = arrayListOf("餐饮", "生活", "游戏", "通讯").mapIndexed { nameIdx, name ->
            RecordTypeEntity(
                1, 0, name + nameIdx, "vector_type_dining_24",
                RecordTypeCategoryEnum.EXPENDITURE, nameIdx,
                arrayOfNulls<String>(6).mapIndexed { idx, v ->
                    RecordTypeEntity(
                        1, 0, name + idx.toString(), "vector_type_dining_24",
                        RecordTypeCategoryEnum.EXPENDITURE, 1,
                        arrayListOf(),
                        false, 1, false,
                    )
                },
                nameIdx == 0, 1, false,
            )
        },
        onTypeSelect = {
        },
        onTypeSettingClick = {},
    )
}
