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

package cn.wj.android.cashbook.feature.books.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.core.common.enums.MimeType
import cn.wj.android.cashbook.core.common.manager.DocumentOperationManager
import cn.wj.android.cashbook.core.data.repository.BooksRepository
import cn.wj.android.cashbook.core.model.model.BooksModel
import cn.wj.android.cashbook.feature.books.enums.EditBookBookmarkEnum
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 编辑账本 ViewModel
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/8/3
 */
@HiltViewModel
class EditBookViewModel @Inject constructor(
    private val booksRepository: BooksRepository,
    private val dom: DocumentOperationManager,
) : ViewModel() {

    var shouldDisplayBookmark by mutableStateOf(EditBookBookmarkEnum.NONE)
        private set

    /** 账本 id */
    private val _bookIdData = MutableStateFlow(-1L)

    /** 账本数据 */
    private val _defaultBookData = _bookIdData.mapLatest { bookId ->
        booksRepository.getDefaultBook(bookId)
    }

    val uiState = _defaultBookData.mapLatest {
        EditBookUiState.Success(it)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = EditBookUiState.Loading,
        )

    fun updateBookId(bookId: Long) {
        _bookIdData.tryEmit(bookId)
    }

    fun onSaveClick(name: String, description: String, bgUri: Uri?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            var book = _defaultBookData.first().copy(
                name = name,
                description = description,
                modifyTime = System.currentTimeMillis(),
            )
            if (booksRepository.isDuplicated(book)) {
                shouldDisplayBookmark = EditBookBookmarkEnum.NAME_DUPLICATED
                return@launch
            }

            if (null != bgUri) {
                // 复制背景文件
                var targetName = "${System.currentTimeMillis()}_${dom.getFileNameByUri(bgUri)}"
                val mimeType = dom.getMimeTypeByUri(bgUri)
                if (mimeType?.isImage == true) {
                    targetName = if (targetName.endsWith(MimeType.Image.PNG.subtype)) {
                        targetName
                    } else {
                        targetName + ".${MimeType.Image.PNG.subtype}"
                    }
                    val uri = dom.copyFileToFilesDir(bgUri, targetName, mimeType)
                    if (null == uri) {
                        shouldDisplayBookmark = EditBookBookmarkEnum.BG_IMG_SAVE_FAILED
                        return@launch
                    } else {
                        book = book.copy(bgUri = uri.toString())
                    }
                } else {
                    shouldDisplayBookmark = EditBookBookmarkEnum.BG_IMG_TYPE_ERROR
                    return@launch
                }
            } else {
                book = book.copy(bgUri = "")
            }

            booksRepository.updateBook(book)
            onSuccess()
        }
    }

    fun onDismissBookmark() {
        shouldDisplayBookmark = EditBookBookmarkEnum.NONE
    }
}

sealed interface EditBookUiState {
    data object Loading : EditBookUiState
    data class Success(
        val data: BooksModel,
    ) : EditBookUiState
}
