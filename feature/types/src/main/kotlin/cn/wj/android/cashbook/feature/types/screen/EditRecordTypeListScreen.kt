@file:OptIn(ExperimentalFoundationApi::class)

package cn.wj.android.cashbook.feature.types.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.common.RECORD_TYPE_COLUMNS
import cn.wj.android.cashbook.core.design.component.painterDrawableResource
import cn.wj.android.cashbook.core.design.theme.LocalExtendedColors
import cn.wj.android.cashbook.core.model.entity.RECORD_TYPE_SETTINGS
import cn.wj.android.cashbook.core.model.entity.RecordTypeEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.types.viewmodel.EditRecordTypeListViewModel

@Composable
internal fun EditRecordTypeListRoute(
    typeCategory: RecordTypeCategoryEnum,
    selectedTypeId: Long,
    onTypeSelect: (Long) -> Unit,
    onTypeSettingClick: () -> Unit,
    headerContent: @Composable (modifier: Modifier) -> Unit,
    footerContent: @Composable (modifier: Modifier) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditRecordTypeListViewModel = hiltViewModel<EditRecordTypeListViewModel>().apply {
        update(typeCategory, selectedTypeId)
    },
) {
    val typeList by viewModel.typeListData.collectAsStateWithLifecycle()

    EditRecordTypeListScreen(
        typeList = typeList,
        onTypeSelect = onTypeSelect,
        onTypeSettingClick = onTypeSettingClick,
        headerContent = headerContent,
        footerContent = footerContent,
        modifier = modifier,
    )
}

@Composable
internal fun EditRecordTypeListScreen(
    typeList: List<RecordTypeEntity>,
    onTypeSelect: (Long) -> Unit,
    onTypeSettingClick: () -> Unit,
    headerContent: @Composable (modifier: Modifier) -> Unit,
    footerContent: @Composable (modifier: Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {

    LazyVerticalGrid(
        modifier = modifier.padding(horizontal = 16.dp),
        columns = GridCells.Fixed(RECORD_TYPE_COLUMNS),
        content = {
            item(
                span = {
                    GridItemSpan(maxLineSpan)
                },
            ) {
                headerContent(modifier = Modifier.animateItemPlacement())
            }

            // 分类列表
            items(typeList, key = { it.id }) { type ->
                if (type == RECORD_TYPE_SETTINGS) {
                    // 设置项
                    TypeItem(
                        modifier = Modifier.animateItemPlacement(),
                        first = true,
                        shapeType = type.shapeType,
                        iconPainter = painterResource(id = cn.wj.android.cashbook.core.ui.R.drawable.vector_baseline_settings_24),
                        showMore = false,
                        title = stringResource(id = R.string.settings),
                        selected = true,
                        onTypeClick = onTypeSettingClick,
                    )
                } else {
                    TypeItem(
                        modifier = Modifier.animateItemPlacement(),
                        first = type.parentId == -1L,
                        shapeType = type.shapeType,
                        iconPainter = painterDrawableResource(idStr = type.iconResName),
                        showMore = type.child.isNotEmpty(),
                        title = type.name,
                        selected = type.selected,
                        onTypeClick = {
                            val selected = if (!type.selected) {
                                // 当前为选中，更新为选中
                                type.copy(selected = true)
                            } else {
                                // 当前已选中，取消选中
                                if (type.parentId != -1L) {
                                    // 二级分类，选择父类型
                                    (typeList.firstOrNull { it.id == type.parentId }
                                        ?: typeList.first()).copy(selected = true)
                                } else {
                                    // 一级分类，无法取消，不做处理
                                    null
                                }
                            }
                            selected?.let { onTypeSelect(it.id) }
                        },
                    )
                }
            }

            item(
                span = {
                    GridItemSpan(maxLineSpan)
                },
            ) {
                footerContent(modifier = Modifier.animateItemPlacement())
            }
        },
    )
}

@Composable
internal fun TypeItem(
    modifier: Modifier = Modifier,
    first: Boolean,
    shapeType: Int,
    iconPainter: Painter,
    showMore: Boolean,
    title: String,
    selected: Boolean,
    onTypeClick: () -> Unit
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
    ConstraintLayout(
        modifier = modifier
            .background(color = backgroundColor, shape = backgroundShape)
            .clickable(onClick = onTypeClick)
            .padding(vertical = 8.dp),
    ) {
        // 约束条件
        val (iconBg, iconMore, text) = createRefs()
        // 根据选中状态显示主要颜色
        val color =
            if (selected) LocalExtendedColors.current.selected else LocalExtendedColors.current.unselected
        // 记录类型对应的图标，使用圆形边框
        Icon(
            painter = iconPainter,
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .size(32.dp)
                .background(
                    color = if (selected) color.copy(alpha = 0.3f) else Color.Unspecified,
                    shape = CircleShape
                )
                .clip(CircleShape)
                .padding(4.dp)
                .constrainAs(iconBg) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        )
        if (showMore) {
            // 横向菜单标记，一级分类有二级分类时显示
            Icon(
                modifier = Modifier
                    .size(12.dp)
                    .background(color = color, shape = CircleShape)
                    .clip(CircleShape)
                    .constrainAs(iconMore) {
                        bottom.linkTo(iconBg.bottom)
                        end.linkTo(iconBg.end)
                    },
                imageVector = Icons.Default.MoreHoriz,
                contentDescription = null,
                tint = backgroundColor
            )
        }
        // 类型名称
        Text(
            modifier = Modifier.constrainAs(text) {
                top.linkTo(iconBg.bottom)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                bottom.linkTo(parent.bottom)
            }, text = title, color = color, style = MaterialTheme.typography.labelMedium
        )
    }
}