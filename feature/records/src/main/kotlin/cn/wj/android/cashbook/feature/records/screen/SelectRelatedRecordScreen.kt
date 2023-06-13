package cn.wj.android.cashbook.feature.records.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.common.Symbol
import cn.wj.android.cashbook.core.design.component.CommonTopBar
import cn.wj.android.cashbook.core.design.component.CompatTextField
import cn.wj.android.cashbook.core.design.theme.LocalExtendedColors
import cn.wj.android.cashbook.feature.records.viewmodel.EditRecordViewModel
import cn.wj.android.cashbook.feature.records.viewmodel.SelectRelatedRecordViewModel

@Composable
internal fun SelectRelatedRecordRoute(
    onBackPressed: () -> Unit,
    parentViewModel: EditRecordViewModel = hiltViewModel(),
) {

    SelectRelatedRecordScreen(
        onBackPressed = onBackPressed,
        parentViewModel = parentViewModel,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectRelatedRecordScreen(
    onBackPressed: () -> Unit,
    parentViewModel: EditRecordViewModel = hiltViewModel(),
    viewModel: SelectRelatedRecordViewModel = hiltViewModel<SelectRelatedRecordViewModel>().apply {
        currentTypeData.value = parentViewModel.selectedTypeData.value
    },
) {
    val dateTime: String by parentViewModel.dateTimeData.collectAsStateWithLifecycle()
    val amount: String by parentViewModel.amountData.collectAsStateWithLifecycle()

    val recordList by viewModel.recordListData.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { CommonTopBar(onBackClick = onBackPressed, text = "选择关联记录") }
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues),
        ) {
            ListItem(
                headlineText = {
                    Text(
                        text = dateTime,
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                trailingContent = {
                    Text(
                        text = "${Symbol.rmb}$amount",
                        color = LocalExtendedColors.current.income,
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
            )
            CompatTextField(
                initializedText = "",
                label = "请输入金额或备注进行搜索",
                onValueChange = viewModel::onKeywordsChanged,
                colors = TextFieldDefaults.outlinedTextFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            LazyColumn {
                items(recordList) {
                    Text(text = "${it.typeName}")
                }
            }
        }
    }
}