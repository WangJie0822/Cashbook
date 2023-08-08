package cn.wj.android.cashbook.feature.books.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.data.repository.fake.FakeBooksRepository
import cn.wj.android.cashbook.core.design.component.CashbookFloatingActionButton
import cn.wj.android.cashbook.core.design.component.CashbookScaffold
import cn.wj.android.cashbook.core.design.component.CashbookTopAppBar
import cn.wj.android.cashbook.core.design.component.CompatTextField
import cn.wj.android.cashbook.core.design.component.Loading
import cn.wj.android.cashbook.core.design.component.TextFieldState
import cn.wj.android.cashbook.core.design.icon.CashbookIcons
import cn.wj.android.cashbook.core.design.theme.PreviewTheme
import cn.wj.android.cashbook.core.ui.DevicePreviews
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.books.enums.EditBookBookmarkEnum
import cn.wj.android.cashbook.feature.books.viewmodel.EditBookUiState
import cn.wj.android.cashbook.feature.books.viewmodel.EditBookViewModel

@Composable
internal fun EditBookRoute(
    bookId: Long,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditBookViewModel = hiltViewModel<EditBookViewModel>().apply {
        updateBookId(bookId)
    },
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    EditBookScreen(
        shouldDisplayBookmark = viewModel.shouldDisplayBookmark,
        onDismissBookmark = viewModel::onDismissBookmark,
        uiState = uiState,
        onSaveClick = { name, description ->
            viewModel.onSaveClick(name = name, description = description, onSuccess = onBackClick)
        },
        onBackClick = onBackClick,
        modifier = modifier,
    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditBookScreen(
    shouldDisplayBookmark: EditBookBookmarkEnum,
    onDismissBookmark: () -> Unit,
    uiState: EditBookUiState,
    onSaveClick: (String, String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {

    // 提示文本
    val blankNameHint = stringResource(id = R.string.please_enter_book_name)
    val nameDuplicatedHint = stringResource(id = R.string.book_name_exists)

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(shouldDisplayBookmark) {
        if (shouldDisplayBookmark == EditBookBookmarkEnum.NAME_DUPLICATED) {
            if (snackbarHostState.showSnackbar(nameDuplicatedHint) == SnackbarResult.Dismissed) {
                onDismissBookmark()
            }
        }
    }

    val data = (uiState as? EditBookUiState.Success)?.data
    val nameTextFieldState = remember(uiState) {
        TextFieldState(
            defaultText = data?.name.orEmpty(),
            validator = { it.isNotBlank() },
            errorFor = { blankNameHint },
        )
    }
    val descriptionTextFieldState = remember(uiState) {
        TextFieldState(defaultText = data?.description.orEmpty())
    }

    CashbookScaffold(
        modifier = modifier,
        topBar = {
            CashbookTopAppBar(
                onBackClick = onBackClick,
                title = { Text(text = stringResource(id = R.string.edit_book)) },
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
        floatingActionButton = {
            CashbookFloatingActionButton(onClick = {
                if (nameTextFieldState.isValid) {
                    onSaveClick(nameTextFieldState.text, descriptionTextFieldState.text)
                }
            }) {
                Icon(imageVector = CashbookIcons.SaveAs, contentDescription = null)
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (uiState) {
                EditBookUiState.Loading -> {
                    Loading(modifier = Modifier.align(Alignment.Center))
                }

                is EditBookUiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        CompatTextField(
                            textFieldState = nameTextFieldState,
                            label = { Text(text = stringResource(id = R.string.book_name)) },
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .padding(horizontal = 16.dp),
                        )
                        CompatTextField(
                            textFieldState = descriptionTextFieldState,
                            label = { Text(text = stringResource(id = R.string.book_description)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .padding(horizontal = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

@DevicePreviews
@Composable
private fun EditBookPreview() {
    PreviewTheme {
        EditBookRoute(
            bookId = 1L,
            onBackClick = {},
            viewModel = EditBookViewModel(booksRepository = FakeBooksRepository).apply {
                updateBookId(
                    1L
                )
            })
    }
}