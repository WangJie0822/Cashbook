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

package cn.wj.android.cashbook.feature.books.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Visibility
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.common.tools.dateFormat
import cn.wj.android.cashbook.core.design.component.CbAlertDialog
import cn.wj.android.cashbook.core.design.component.CbCard
import cn.wj.android.cashbook.core.design.component.CbFloatingActionButton
import cn.wj.android.cashbook.core.design.component.CbIconButton
import cn.wj.android.cashbook.core.design.component.CbScaffold
import cn.wj.android.cashbook.core.design.component.CbTextButton
import cn.wj.android.cashbook.core.design.component.CbTopAppBar
import cn.wj.android.cashbook.core.design.component.Loading
import cn.wj.android.cashbook.core.design.icon.CbIcons
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.books.viewmodel.MyBooksUiState
import cn.wj.android.cashbook.feature.books.viewmodel.MyBooksViewModel

/**
 * 我的账本
 *
 * @param onRequestNaviToEditBook 导航到编辑账本
 * @param onRequestPopBackStack 导航上上一级
 */
@Composable
internal fun MyBooksRoute(
    onRequestNaviToEditBook: (Long) -> Unit,
    onRequestPopBackStack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyBooksViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MyBooksScreen(
        uiState = uiState,
        onBookSelected = viewModel::onBookSelected,
        onEditBookClick = onRequestNaviToEditBook,
        onDeleteBookClick = viewModel::onDeleteBookClick,
        dialogState = viewModel.dialogState,
        onConfirmDelete = viewModel::confirmDeleteBook,
        onDismissDialog = viewModel::onDismissDialog,
        onBackClick = onRequestPopBackStack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MyBooksScreen(
    uiState: MyBooksUiState,
    onBookSelected: (Long) -> Unit,
    onEditBookClick: (Long) -> Unit,
    onDeleteBookClick: (Long) -> Unit,
    dialogState: DialogState,
    onConfirmDelete: (Long) -> Unit,
    onDismissDialog: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CbScaffold(
        modifier = modifier,
        topBar = {
            CbTopAppBar(
                onBackClick = onBackClick,
                title = { Text(text = stringResource(id = R.string.my_books)) },
            )
        },
        floatingActionButton = {
            CbFloatingActionButton(onClick = { onEditBookClick(-1L) }) {
                Icon(imageVector = CbIcons.Add, contentDescription = null)
            }
        },
        content = { paddingValues ->
            MyBooksContent(
                modifier = Modifier.padding(paddingValues),
                uiState = uiState,
                onBookSelected = onBookSelected,
                onEditBookClick = onEditBookClick,
                onDeleteBookClick = onDeleteBookClick,
                dialogState = dialogState,
                onConfirmDelete = onConfirmDelete,
                onDismissDialog = onDismissDialog,
            )
        },
    )
}

@Composable
private fun MyBooksContent(
    modifier: Modifier,
    uiState: MyBooksUiState,
    onBookSelected: (Long) -> Unit,
    onEditBookClick: (Long) -> Unit,
    onDeleteBookClick: (Long) -> Unit,
    dialogState: DialogState,
    onConfirmDelete: (Long) -> Unit,
    onDismissDialog: () -> Unit,
) {
    Box(
        modifier = modifier,
        content = {
            if (dialogState is DialogState.Shown<*>) {
                (dialogState.data as? Long)?.let { bookId ->
                    CbAlertDialog(
                        onDismissRequest = onDismissDialog,
                        title = { Text(text = stringResource(id = R.string.delete_books)) },
                        text = { Text(text = stringResource(id = R.string.delete_books_confirm)) },
                        dismissButton = {
                            CbTextButton(onClick = onDismissDialog) {
                                Text(text = stringResource(id = R.string.cancel))
                            }
                        },
                        confirmButton = {
                            CbTextButton(onClick = { onConfirmDelete(bookId) }) {
                                Text(text = stringResource(id = R.string.confirm))
                            }
                        },
                    )
                }
            }

            when (uiState) {
                MyBooksUiState.Loading -> {
                    Loading(modifier = Modifier.align(Alignment.Center))
                }

                is MyBooksUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        content = {
                            items(uiState.booksList) { item ->
                                CbCard(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .padding(top = 8.dp)
                                        .fillMaxWidth(),
                                    onClick = {
                                        onBookSelected(item.data.id)
                                    },
                                ) {
                                    ConstraintLayout(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .paint(
                                                painter = painterResource(id = R.drawable.im_top_background),
                                                contentScale = ContentScale.Crop,
                                            ),
                                    ) {
                                        val (selected, name, description, time, more) = createRefs()

                                        Icon(
                                            imageVector = CbIcons.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.constrainAs(selected) {
                                                visibility = if (item.selected) {
                                                    Visibility.Visible
                                                } else {
                                                    Visibility.Invisible
                                                }
                                                top.linkTo(parent.top, 8.dp)
                                                end.linkTo(parent.end, 8.dp)
                                            },
                                        )

                                        Text(
                                            text = item.data.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier
                                                .padding(horizontal = 16.dp)
                                                .constrainAs(name) {
                                                    centerHorizontallyTo(parent)
                                                    top.linkTo(selected.bottom, 24.dp)
                                                },
                                        )

                                        Text(
                                            text = item.data.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier
                                                .padding(horizontal = 16.dp)
                                                .constrainAs(description) {
                                                    centerHorizontallyTo(parent)
                                                    top.linkTo(name.bottom, 8.dp)
                                                },
                                        )

                                        Text(
                                            text = stringResource(id = R.string.modify_time_with_colon) + item.data.modifyTime.dateFormat(),
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier
                                                .constrainAs(time) {
                                                    bottom.linkTo(parent.bottom, 8.dp)
                                                    start.linkTo(parent.start, 8.dp)
                                                }
                                                .background(
                                                    color = MaterialTheme.colorScheme.primaryContainer.copy(
                                                        alpha = 0.7f,
                                                    ),
                                                    shape = MaterialTheme.shapes.small,
                                                )
                                                .padding(horizontal = 8.dp),
                                        )
                                        Box(
                                            modifier = Modifier.constrainAs(more) {
                                                centerVerticallyTo(time)
                                                end.linkTo(parent.end, 8.dp)
                                            },
                                        ) {
                                            var expanded by remember {
                                                mutableStateOf(false)
                                            }
                                            CbIconButton(
                                                onClick = { expanded = true },
                                                content = {
                                                    Icon(
                                                        imageVector = CbIcons.MoreHoriz,
                                                        contentDescription = null,
                                                    )
                                                },
                                                modifier = Modifier,
                                            )
                                            DropdownMenu(
                                                expanded = expanded,
                                                onDismissRequest = { expanded = false },
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text(text = stringResource(id = R.string.modify)) },
                                                    onClick = {
                                                        expanded = false
                                                        onEditBookClick(item.data.id)
                                                    },
                                                )
                                                if (!item.selected) {
                                                    DropdownMenuItem(
                                                        text = { Text(text = stringResource(id = R.string.delete)) },
                                                        onClick = {
                                                            expanded = false
                                                            onDeleteBookClick(item.data.id)
                                                        },
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        },
                    )
                }
            }
        },
    )
}
