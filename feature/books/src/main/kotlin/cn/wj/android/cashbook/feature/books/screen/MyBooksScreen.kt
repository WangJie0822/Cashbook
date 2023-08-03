package cn.wj.android.cashbook.feature.books.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Visibility
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.common.tools.dateFormat
import cn.wj.android.cashbook.core.design.component.CashbookFloatingActionButton
import cn.wj.android.cashbook.core.design.component.CashbookScaffold
import cn.wj.android.cashbook.core.design.component.CashbookTopAppBar
import cn.wj.android.cashbook.core.design.component.Loading
import cn.wj.android.cashbook.core.design.icon.CashbookIcons
import cn.wj.android.cashbook.core.design.theme.PreviewTheme
import cn.wj.android.cashbook.core.model.model.BooksModel
import cn.wj.android.cashbook.core.model.model.Selectable
import cn.wj.android.cashbook.core.ui.DevicePreviews
import cn.wj.android.cashbook.core.ui.DialogState
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.books.viewmodel.MyBooksUiState
import cn.wj.android.cashbook.feature.books.viewmodel.MyBooksViewModel

@Composable
internal fun MyBooksRoute(
    onEditBookClick: (Long) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyBooksViewModel = hiltViewModel(),
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MyBooksScreen(
        uiState = uiState,
        onBookSelected = viewModel::onBookSelected,
        onEditBookClick = onEditBookClick,
        onDeleteBookClick = viewModel::onDeleteBookClick,
        dialogState = viewModel.dialogState,
        onConfirmDelete = viewModel::confirmDeleteBook,
        onDismissDialog = viewModel::onDismissDialog,
        onBackClick = onBackClick,
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

    CashbookScaffold(
        modifier = modifier,
        topBar = {
            CashbookTopAppBar(
                onBackClick = onBackClick,
                text = stringResource(id = R.string.my_books),
            )
        },
        floatingActionButton = {
            CashbookFloatingActionButton(onClick = { onEditBookClick(-1L) }) {
                Icon(imageVector = CashbookIcons.Add, contentDescription = null)
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
@OptIn(ExperimentalMaterial3Api::class)
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
                    AlertDialog(
                        onDismissRequest = onDismissDialog,
                        title = { Text(text = stringResource(id = R.string.delete_books)) },
                        text = { Text(text = stringResource(id = R.string.delete_books_confirm)) },
                        dismissButton = {
                            TextButton(onClick = onDismissDialog) {
                                Text(text = stringResource(id = R.string.cancel))
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { onConfirmDelete(bookId) }) {
                                Text(text = stringResource(id = R.string.confirm))
                            }
                        }
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
                                Card(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .padding(top = 8.dp)
                                        .fillMaxWidth(),
                                    onClick = {
                                        onBookSelected(item.data.id)
                                    }
                                ) {

                                    ConstraintLayout(
                                        modifier = Modifier
                                            .fillMaxWidth(),
                                    ) {

                                        val (selected, name, description, time, more) = createRefs()

                                        Icon(
                                            imageVector = CashbookIcons.CheckCircle,
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
                                            modifier = Modifier.constrainAs(time) {
                                                top.linkTo(description.bottom, 16.dp)
                                                bottom.linkTo(parent.bottom, 8.dp)
                                                start.linkTo(parent.start, 8.dp)
                                            },
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
                                            IconButton(
                                                onClick = { expanded = true },
                                                content = {
                                                    Icon(
                                                        imageVector = CashbookIcons.MoreHoriz,
                                                        contentDescription = null
                                                    )
                                                },
                                                modifier = Modifier
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

@DevicePreviews
@Composable
private fun MyBooksScreenPreview() {
    PreviewTheme {
        MyBooksScreen(
            uiState = MyBooksUiState.Success(
                booksList = listOf(
                    Selectable(
                        BooksModel(
                            1L,
                            "默认账本",
                            "默认账本说明",
                            System.currentTimeMillis()
                        ), true
                    ),
                    Selectable(
                        BooksModel(
                            2L,
                            "默认账本2",
                            "默认账本说明2",
                            System.currentTimeMillis()
                        ), false
                    )
                )
            ),
            onBookSelected = {},
            onBackClick = {},
            onEditBookClick = {},
            onDeleteBookClick = {},
            dialogState = DialogState.Dismiss,
            onConfirmDelete = {},
            onDismissDialog = {},
        )
    }
}