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

package cn.wj.android.cashbook.feature.records.model

import android.graphics.Bitmap
import cn.wj.android.cashbook.core.common.tools.toBitmap
import cn.wj.android.cashbook.core.common.tools.toByteArray
import cn.wj.android.cashbook.core.model.model.ImageModel

data class ImageViewModel(
    val id: Long,
    val recordId: Long,
    val path: String,
    val bitmap: Bitmap?,
)

private val EMPTY_BYTE_ARRAY = byteArrayOf()

fun ImageViewModel.asModel(): ImageModel {
    return ImageModel(
        id = this.id,
        recordId = this.recordId,
        path = path,
        bytes = bitmap?.toByteArray() ?: EMPTY_BYTE_ARRAY,
    )
}

fun ImageModel.asViewModel(): ImageViewModel {
    return ImageViewModel(
        id = this.id,
        recordId = this.recordId,
        path = path,
        bitmap = bytes.toBitmap(),
    )
}
