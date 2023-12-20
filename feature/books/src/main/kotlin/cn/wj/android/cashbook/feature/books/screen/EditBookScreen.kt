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
import cn.wj.android.cashbook.core.design.component.CbFloatingActionButton
import cn.wj.android.cashbook.core.design.component.CbScaffold
import cn.wj.android.cashbook.core.design.component.CbTextField
import cn.wj.android.cashbook.core.design.component.CbTopAppBar
import cn.wj.android.cashbook.core.design.component.Loading
import cn.wj.android.cashbook.core.design.component.TextFieldState
import cn.wj.android.cashbook.core.design.icon.CbIcons
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.feature.books.enums.EditBookBookmarkEnum
import cn.wj.android.cashbook.feature.books.viewmodel.EditBookUiState
import cn.wj.android.cashbook.feature.books.viewmodel.EditBookViewModel

@Composable
internal fun EditBookRoute(
    bookId: Long,
    onRequestPopBackStack: () -> Unit,
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
            viewModel.onSaveClick(
                name = name,
                description = description,
                onSuccess = onRequestPopBackStack,
            )
        },
        onBackClick = onRequestPopBackStack,
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
            val result = snackbarHostState.showSnackbar(nameDuplicatedHint)
            if (result == SnackbarResult.Dismissed) {
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

    CbScaffold(
        modifier = modifier,
        topBar = {
            CbTopAppBar(
                onBackClick = onBackClick,
                title = { Text(text = stringResource(id = R.string.edit_book)) },
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
        floatingActionButton = {
            CbFloatingActionButton(onClick = {
                if (nameTextFieldState.isValid) {
                    onSaveClick(nameTextFieldState.text, descriptionTextFieldState.text)
                }
            }) {
                Icon(imageVector = CbIcons.SaveAs, contentDescription = null)
            }
        },
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (uiState) {
                EditBookUiState.Loading -> {
                    Loading(modifier = Modifier.align(Alignment.Center))
                }

                is EditBookUiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        CbTextField(
                            textFieldState = nameTextFieldState,
                            label = { Text(text = stringResource(id = R.string.book_name)) },
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .padding(horizontal = 16.dp),
                        )
                        CbTextField(
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
