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

package cn.wj.android.cashbook.core.common.manager

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.common.enums.MimeType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * 文档
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2024/9/9
 */
class DocumentOperationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @Dispatcher(CashbookDispatchers.IO) private val ioCoroutineContext: CoroutineContext,
) {

    fun getFileNameByUri(uri: Uri): String {
        return DocumentFile.fromSingleUri(context, uri)?.name.orEmpty()
    }

    fun getMimeTypeByUri(uri: Uri): MimeType? {
        return MimeType.parse(context.contentResolver.getType(uri))
    }

    suspend fun copyFileToFilesDir(from: Uri, targetName: String, mimeType: MimeType): Uri? =
        withContext(ioCoroutineContext) {
            val targetFile =
                DocumentFile.fromFile(context.filesDir).createFile(mimeType.format, targetName)
                    ?: return@withContext null
            context.contentResolver.openInputStream(from)?.use { `is` ->
                context.contentResolver.openOutputStream(targetFile.uri)?.use {
                    it.write(`is`.readBytes())
                }
            }
            targetFile.uri
        }
}
