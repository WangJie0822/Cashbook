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

package cn.wj.android.cashbook.core.data.uitl

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** [RecordImageFileStorage] 纯逻辑顶层函数单测（路径派生 / 原子写 / 删除） */
class RecordImageFileStorageTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun recordImageRelativePath_derivesDeterministicNameFromId() {
        assertThat(recordImageRelativePath(42L)).isEqualTo("record_images/img_42.jpg")
        // 同一 id 始终同名（backfill 崩溃可重入的基石）
        assertThat(recordImageRelativePath(42L)).isEqualTo(recordImageRelativePath(42L))
    }

    @Test
    fun newRecordImageRelativePath_usesToken() {
        assertThat(newRecordImageRelativePath("abc")).isEqualTo("record_images/img_abc.jpg")
    }

    @Test
    fun isManagedImagePath_trueOnlyForManagedPrefix() {
        assertThat(isManagedImagePath("record_images/img_1.jpg")).isTrue()
        assertThat(isManagedImagePath("content://media/external/images/1")).isFalse()
        assertThat(isManagedImagePath("/data/user/0/x/files/record_images/img_1.jpg")).isFalse()
    }

    @Test
    fun isManagedImagePath_rejectsTraversal() {
        // 恶意恢复 DB 的 path 含 `..` → 不得判为托管（CWE-22，防逃逸 record_images）
        assertThat(isManagedImagePath("record_images/../../etc/passwd")).isFalse()
        assertThat(isManagedImagePath("record_images/..")).isFalse()
        assertThat(isManagedImagePath("record_images/a/../../../x")).isFalse()
    }

    @Test
    fun writeRecordImageAtomic_createsParentAndWritesBytes() {
        val base = tempFolder.root
        writeRecordImageAtomic(base, "record_images/img_7.jpg", byteArrayOf(1, 2, 3))
        val written = File(base, "record_images/img_7.jpg")
        assertThat(written.exists()).isTrue()
        assertThat(written.readBytes()).isEqualTo(byteArrayOf(1, 2, 3))
    }

    @Test
    fun writeRecordImageAtomic_overwritesSameNameIdempotently() {
        val base = tempFolder.root
        writeRecordImageAtomic(base, "record_images/img_7.jpg", byteArrayOf(1))
        writeRecordImageAtomic(base, "record_images/img_7.jpg", byteArrayOf(9, 9))
        assertThat(File(base, "record_images/img_7.jpg").readBytes()).isEqualTo(byteArrayOf(9, 9))
        // 无残留 .tmp
        assertThat(File(base, "record_images/img_7.jpg.tmp").exists()).isFalse()
    }

    @Test
    fun deleteRecordImageFile_returnsTrueWhenDeletedFalseWhenAbsent() {
        val base = tempFolder.root
        writeRecordImageAtomic(base, "record_images/img_7.jpg", byteArrayOf(1))
        assertThat(deleteRecordImageFile(base, "record_images/img_7.jpg")).isTrue()
        assertThat(deleteRecordImageFile(base, "record_images/img_7.jpg")).isFalse()
    }
}
