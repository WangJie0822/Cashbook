package cn.wj.android.cashbook.feature.types.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.design.component.painterDrawableResource
import cn.wj.android.cashbook.core.design.theme.LocalExtendedColors
import cn.wj.android.cashbook.feature.types.viewmodel.TypeIconGroupListViewModel

@Composable
internal fun TypeIconGroupList(
    onTypeIconSelect: (String, String) -> Unit,
    viewModel: TypeIconGroupListViewModel = hiltViewModel(),
) {

    val groupList by viewModel.selectableGroupListData.collectAsStateWithLifecycle()
    val iconList by viewModel.iconListData.collectAsStateWithLifecycle()

    Row {
        LazyColumn(
            modifier = Modifier.weight(3f),
            content = {
                items(items = groupList) {
                    Text(
                        text = it.data.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (it.selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                            .clickable { viewModel.selectGroup(it.data.name) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            },
        )
        LazyVerticalGrid(
            modifier = Modifier
                .weight(8f),
            columns = GridCells.Fixed(4),
            content = {
                items(items = iconList) {
                    Column(
                        modifier = Modifier
                            .padding(8.dp)
                            .clickable {
                                onTypeIconSelect(it.name, it.iconResIdStr)
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            painter = painterDrawableResource(idStr = it.iconResIdStr),
                            contentDescription = null,
                            tint = LocalExtendedColors.current.unselected,
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    color = Color.Unspecified,
                                    shape = CircleShape
                                )
                                .clip(CircleShape)
                                .padding(4.dp),
                        )
                        Text(
                            text = it.name,
                            color = LocalExtendedColors.current.unselected,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            },
        )
    }
}