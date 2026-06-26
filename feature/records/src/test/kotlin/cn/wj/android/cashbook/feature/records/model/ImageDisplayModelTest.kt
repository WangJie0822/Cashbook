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

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

/** [imageCoilModel] 双轨选择纯函数单测 */
class ImageDisplayModelTest {

    @Test
    fun managedPathWithExistingFile_returnsFile() {
        val file = File("/x/record_images/img_1.jpg")
        val model = imageCoilModel(
            path = "record_images/img_1.jpg",
            file = file,
            fileExists = true,
            bitmapPresent = true,
        )
        assertThat(model).isEqualTo(ImageDisplaySource.FromFile(file))
    }

    @Test
    fun managedPathWithMissingFile_fallsBackToBitmap() {
        val file = File("/x/record_images/img_1.jpg")
        val model = imageCoilModel(
            path = "record_images/img_1.jpg",
            file = file,
            fileExists = false,
            bitmapPresent = true,
        )
        assertThat(model).isEqualTo(ImageDisplaySource.FromBitmap)
    }

    @Test
    fun unmanagedPath_fallsBackToBitmap() {
        val file = File("/x/content")
        val model = imageCoilModel(
            path = "content://media/1",
            file = file,
            fileExists = false,
            bitmapPresent = true,
        )
        assertThat(model).isEqualTo(ImageDisplaySource.FromBitmap)
    }

    @Test
    fun missingFileAndNoBitmap_returnsNone() {
        val file = File("/x/record_images/img_1.jpg")
        val model = imageCoilModel(
            path = "record_images/img_1.jpg",
            file = file,
            fileExists = false,
            bitmapPresent = false,
        )
        assertThat(model).isEqualTo(ImageDisplaySource.None)
    }
}
