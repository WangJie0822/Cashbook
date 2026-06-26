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
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** [deleteManagedImageFiles] 删点清理单测（仅删托管文件、不碰非托管/未列出文件） */
class RecordImageOrphanTest {

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
}
