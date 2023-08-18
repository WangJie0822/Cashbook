package cn.wj.android.cashbook.feature.types.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.feature.types.screen.EditRecordTypeListRoute

/**
 * 编辑记录页面标签列表
 *
 * @param typeCategory 记录大类
 * @param selectedTypeId 当前选中的类型 id
 * @param onTypeSelect 类型选中回调
 * @param onRequestNaviToTypeManager 导航到类型管理
 * @param headerContent 头布局
 * @param footerContent 脚布局
 */
@Composable
fun EditRecordTypeListContent(
    typeCategory: RecordTypeCategoryEnum,
    selectedTypeId: Long,
    onTypeSelect: (Long) -> Unit,
    onRequestNaviToTypeManager: () -> Unit,
    headerContent: @Composable (modifier: Modifier) -> Unit,
    footerContent: @Composable (modifier: Modifier) -> Unit,
) {
    EditRecordTypeListRoute(
        typeCategory = typeCategory,
        selectedTypeId = selectedTypeId,
        onTypeSelect = onTypeSelect,
        onRequestNaviToTypeManager = onRequestNaviToTypeManager,
        headerContent = headerContent,
        footerContent = footerContent,
    )
}