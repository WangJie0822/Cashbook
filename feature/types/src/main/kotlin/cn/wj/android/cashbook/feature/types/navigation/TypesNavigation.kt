package cn.wj.android.cashbook.feature.types.navigation

import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cn.wj.android.cashbook.core.model.entity.RecordTypeEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.feature.types.screen.SelectRecordTypeListScreen

@Composable
fun SelectRecordTypeList(
    typeCategory: RecordTypeCategoryEnum,
    selectedType: RecordTypeEntity?,
    overTypeList: @Composable LazyGridItemScope.() -> Unit,
    underTypeList: @Composable LazyGridItemScope.() -> Unit,
    onTypeSelected: (RecordTypeEntity?) -> Unit,
    onTypeSettingClick: () -> Unit,
) {
    SelectRecordTypeListScreen(
        typeCategory = typeCategory,
        selectedType = selectedType,
        overTypeList = overTypeList,
        underTypeList = underTypeList,
        onTypeSelected = onTypeSelected,
        onTypeSettingClick = onTypeSettingClick,
    )

}