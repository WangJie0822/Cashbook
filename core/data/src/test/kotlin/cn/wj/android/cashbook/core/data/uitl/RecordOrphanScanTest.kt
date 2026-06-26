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

/** [computeOrphanFiles] 孤儿计算纯函数单测（引用集 / grace window / 目录跳过） */
class RecordOrphanScanTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun computeOrphanFiles_returnsUnreferencedAndOldEnough() {
        val referenced = tempFolder.newFile("img_1.jpg").apply { setLastModified(1_000L) } // 被引用 → 保留
        val orphan = tempFolder.newFile("img_2.jpg").apply { setLastModified(1_000L) } // 未引用 + 旧 → 孤儿
        val fresh = tempFolder.newFile("img_3.jpg").apply { setLastModified(99_000L) } // 未引用但在 grace 内 → 保留

        val orphans = computeOrphanFiles(
            referencedNames = setOf("img_1.jpg"),
            files = listOf(referenced, orphan, fresh),
            nowMs = 100_000L,
            graceWindowMs = 60_000L,
        )

        assertThat(orphans.map { it.name }).containsExactly("img_2.jpg")
    }

    @Test
    fun computeOrphanFiles_skipsDirectories() {
        val dir = tempFolder.newFolder("sub")

        val orphans = computeOrphanFiles(
            referencedNames = emptySet(),
            files = listOf(dir),
            nowMs = 100_000L,
            graceWindowMs = 0L,
        )

        assertThat(orphans).isEmpty()
    }
}
