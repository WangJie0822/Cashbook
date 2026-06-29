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

/** [deleteManagedImageFiles] / [managedImagesToDelete] 删点清理单测（仅删托管文件、不碰非托管/未列出文件） */
class RecordImageOrphanTest {

    private fun image(path: String) = ImageModel(id = 0L, recordId = 1L, path = path, bytes = ByteArray(0))

    @Test
    fun deleteManagedImageFiles_deletesManagedAndKeepsOthers() {
        val storage = FakeRecordImageFileStorage().apply {
            files["record_images/img_3.jpg"] = byteArrayOf(1)
            files["record_images/keep.jpg"] = byteArrayOf(2)
        }

        deleteManagedImageFiles(listOf("record_images/img_3.jpg", "content://old/4"), storage)

        assertThat(storage.files).doesNotContainKey("record_images/img_3.jpg") // 托管且列出 → 删
        assertThat(storage.files).containsKey("record_images/keep.jpg") // 未列出 → 不删
    }

    @Test
    fun deleteManagedImageFiles_unmanagedPathTouchesNothing() {
        val storage = FakeRecordImageFileStorage().apply {
            files["record_images/keep.jpg"] = byteArrayOf(1)
        }

        deleteManagedImageFiles(listOf("content://old/4"), storage) // 非托管 path → 不碰任何文件

        assertThat(storage.files).containsKey("record_images/keep.jpg")
    }

    // region managedImagesToDelete（编辑路径 diff：旧托管 − 持久化后仍引用的托管）

    @Test
    fun managedImagesToDelete_replaceImage_returnsRemovedOnly() {
        val storage = FakeRecordImageFileStorage()
        // 旧有 a、b 两托管图，编辑后保留 a、新增 c（b 被移除）
        val result = managedImagesToDelete(
            oldManagedPaths = setOf("record_images/a.jpg", "record_images/b.jpg"),
            persistedImages = listOf(image("record_images/a.jpg"), image("record_images/c.jpg")),
            storage = storage,
        )
        assertThat(result).containsExactly("record_images/b.jpg")
    }

    @Test
    fun managedImagesToDelete_keepAll_returnsEmpty() {
        val storage = FakeRecordImageFileStorage()
        val result = managedImagesToDelete(
            oldManagedPaths = setOf("record_images/a.jpg"),
            persistedImages = listOf(image("record_images/a.jpg")),
            storage = storage,
        )
        assertThat(result).isEmpty()
    }

    @Test
    fun managedImagesToDelete_removeAll_returnsAll() {
        val storage = FakeRecordImageFileStorage()
        val result = managedImagesToDelete(
            oldManagedPaths = setOf("record_images/a.jpg", "record_images/b.jpg"),
            persistedImages = emptyList(),
            storage = storage,
        )
        assertThat(result).containsExactly("record_images/a.jpg", "record_images/b.jpg")
    }

    @Test
    fun managedImagesToDelete_unmanagedKeptNotCounted() {
        val storage = FakeRecordImageFileStorage()
        // 持久化后保留一张非托管图（content://）不应影响托管 diff → a 仍判被移除
        val result = managedImagesToDelete(
            oldManagedPaths = setOf("record_images/a.jpg"),
            persistedImages = listOf(image("content://old/x")),
            storage = storage,
        )
        assertThat(result).containsExactly("record_images/a.jpg")
    }

    // endregion
}
