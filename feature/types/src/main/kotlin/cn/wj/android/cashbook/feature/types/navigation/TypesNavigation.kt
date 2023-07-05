package cn.wj.android.cashbook.feature.types.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.feature.types.screen.EditRecordTypeListRoute

@Composable
fun EditRecordTypeListContent(
    typeCategory: RecordTypeCategoryEnum,
    selectedTypeId: Long,
    onTypeSelect: (Long) -> Unit,
    onTypeSettingClick: () -> Unit,
    headerContent: @Composable (modifier: Modifier) -> Unit,
    footerContent: @Composable (modifier: Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    EditRecordTypeListRoute(
        typeCategory = typeCategory,
        selectedTypeId = selectedTypeId,
        onTypeSelect = onTypeSelect,
        onTypeSettingClick = onTypeSettingClick,
        headerContent = headerContent,
        footerContent = footerContent,
        modifier = modifier,
    )

}