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

package cn.wj.android.cashbook.core.data.repository.impl

import cn.wj.android.cashbook.core.data.uitl.FakeRecordImageFileStorage
import cn.wj.android.cashbook.core.model.model.ImageModel
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** [persistNewImages] 新图落盘转换单测（写文件 + path 相对化 + bytes 置空） */
class PersistNewImagesTest {

    @Test
    fun newImageBytes_writesFile_relativePath_emptiesBytes() {
        val storage = FakeRecordImageFileStorage()
        val result = persistNewImages(
            listOf(ImageModel(id = -1L, recordId = -1L, path = "content://pick/1", bytes = byteArrayOf(5, 6, 7))),
            storage,
        )
        // 文件已写、内容为原 bytes
        assertThat(storage.files.values.single()).isEqualTo(byteArrayOf(5, 6, 7))
        // 落库前的图片 path 相对化、bytes 置空
        assertThat(result.single().path).startsWith("record_images/")
        assertThat(result.single().bytes).isEmpty()
    }

    @Test
    fun alreadyManagedImageWithEmptyBytes_notRewritten() {
        val storage = FakeRecordImageFileStorage()
        val existing = ImageModel(id = 9L, recordId = 1L, path = "record_images/img_9.jpg", bytes = byteArrayOf())
        val result = persistNewImages(listOf(existing), storage)
        assertThat(storage.files).isEmpty() // 已托管 + bytes 空 → 不重写
        assertThat(result.single()).isEqualTo(existing) // 原样返回
    }

    @Test
    fun emptyBytesUnmanagedPath_notWritten() {
        val storage = FakeRecordImageFileStorage()
        val noBytes = ImageModel(id = 3L, recordId = 1L, path = "content://old/3", bytes = byteArrayOf())
        val result = persistNewImages(listOf(noBytes), storage)
        assertThat(storage.files).isEmpty() // 无 bytes → 无可落盘
        assertThat(result.single()).isEqualTo(noBytes)
    }
}
