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

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.wj.android.cashbook.core.design.component.CbCard
import cn.wj.android.cashbook.core.design.component.CbFloatingActionButton
import cn.wj.android.cashbook.core.design.component.CbIconButton
import cn.wj.android.cashbook.core.design.component.CbScaffold
import cn.wj.android.cashbook.core.design.component.CbTextButton
import cn.wj.android.cashbook.core.design.component.CbTextField
import cn.wj.android.cashbook.core.design.component.CbTopAppBar
import cn.wj.android.cashbook.core.design.component.Loading
import cn.wj.android.cashbook.core.design.component.TextFieldState
import cn.wj.android.cashbook.core.design.icon.CbIcons
import cn.wj.android.cashbook.core.design.preview.PreviewTheme
import cn.wj.android.cashbook.core.model.model.BooksModel
import cn.wj.android.cashbook.core.ui.DevicePreviews
import cn.wj.android.cashbook.core.ui.R
import cn.wj.android.cashbook.core.ui.expand.bookImageRatio
import cn.wj.android.cashbook.feature.books.enums.EditBookBookmarkEnum
import cn.wj.android.cashbook.feature.books.viewmodel.EditBookUiState
import cn.wj.android.cashbook.feature.books.viewmodel.EditBookViewModel
import coil.compose.AsyncImage

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
        onSaveClick = { name, description, bgUri ->
            viewModel.onSaveClick(
                name = name,
                description = description,
                bgUri = bgUri,
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
    onSaveClick: (name: String, description: String, bgUri: Uri?) -> Unit,
    onBackClick: () -> Unit,
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo(),
    modifier: Modifier = Modifier,
) {
    // 提示文本
    val blankNameHint = stringResource(id = R.string.please_enter_book_name)
    val nameDuplicatedHint = stringResource(id = R.string.book_name_exists)
    val bgImgTypeErrorHint = stringResource(id = R.string.bg_img_type_error)
    val bgImgSaveFailedHint = stringResource(id = R.string.bg_img_save_failed)

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(shouldDisplayBookmark) {
        val hintText = when (shouldDisplayBookmark) {
            EditBookBookmarkEnum.NAME_DUPLICATED -> nameDuplicatedHint
            EditBookBookmarkEnum.BG_IMG_TYPE_ERROR -> bgImgTypeErrorHint
            EditBookBookmarkEnum.BG_IMG_SAVE_FAILED -> bgImgSaveFailedHint
            else -> ""
        }
        if (hintText.isNotBlank()) {
            val result = snackbarHostState.showSnackbar(hintText)
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
    var bgUri: Uri? by remember(uiState) {
        mutableStateOf(if (data?.bgUri.isNullOrBlank()) null else Uri.parse(data!!.bgUri))
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
            CbFloatingActionButton(
                onClick = {
                    if (nameTextFieldState.isValid) {
                        onSaveClick(nameTextFieldState.text, descriptionTextFieldState.text, bgUri)
                    }
                },
            ) {
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
                        CbCard(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(top = 8.dp)
                                .fillMaxWidth()
                                .aspectRatio(windowAdaptiveInfo.bookImageRatio),
                        ) {
                            ConstraintLayout(
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                val launcher =
                                    rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) {
                                        bgUri = it
                                    }

                                val (bg, clear, pickImage) = createRefs()

                                AsyncImage(
                                    modifier = Modifier
                                        .constrainAs(bg) {
                                            centerTo(parent)
                                        }
                                        .fillMaxSize(),
                                    model = bgUri,
                                    placeholder = painterResource(id = R.drawable.im_top_background),
                                    error = painterResource(id = R.drawable.im_top_background),
                                    fallback = painterResource(id = R.drawable.im_top_background),
                                    contentScale = ContentScale.FillBounds,
                                    contentDescription = null,
                                )

                                CbIconButton(
                                    modifier = Modifier.constrainAs(clear) {
                                        top.linkTo(parent.top, 8.dp)
                                        end.linkTo(parent.end, 8.dp)
                                    },
                                    onClick = { bgUri = null },
                                ) {
                                    Icon(
                                        imageVector = CbIcons.CleaningServices,
                                        tint = MaterialTheme.colorScheme.primaryContainer,
                                        contentDescription = null,
                                    )
                                }

                                CbTextButton(
                                    modifier = Modifier
                                        .constrainAs(pickImage) {
                                            centerTo(parent)
                                            verticalChainWeight = 0.4f
                                        }
                                        .background(
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(
                                                alpha = 0.7f,
                                            ),
                                            shape = MaterialTheme.shapes.small,
                                        ),
                                    onClick = { launcher.launch(arrayOf("image/*")) },
                                ) {
                                    Text(
                                        text = stringResource(R.string.click_to_select_book_background),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                }
                            }
                        }

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

@DevicePreviews
@Composable
private fun EditBookScreenPreview() {
    PreviewTheme {
        EditBookScreen(
            shouldDisplayBookmark = EditBookBookmarkEnum.NONE,
            onDismissBookmark = {},
            uiState = EditBookUiState.Success(
                data = BooksModel(
                    id = -1L,
                    name = "测试",
                    description = "描述",
                    bgUri = "",
                    modifyTime = System.currentTimeMillis(),
                ),
            ),
            onSaveClick = { _, _, _ -> },
            onBackClick = {},
        )
    }
}
