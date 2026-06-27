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

import java.io.File

/**
 * [RecordImageFileStorage] 的内存忠实复刻测试替身：
 * write 记录字节、exists/delete 据此判定、isManaged/路径派生复用真实顶层函数规则。
 */
class FakeRecordImageFileStorage : RecordImageFileStorage {

    val files = linkedMapOf<String, ByteArray>()

    /** 测试用：write 命中这些相对路径时抛 IOException（模拟坏行/IO 失败） */
    val failWritePaths = mutableSetOf<String>()
    private var counter = 0L

    override fun newRelativePath(): String = newRecordImageRelativePath("new${counter++}")

    override fun relativePathForId(id: Long): String = recordImageRelativePath(id)

    override fun isManaged(path: String): Boolean = isManagedImagePath(path)

    override fun resolve(relativePath: String): File = File(relativePath)

    override fun exists(relativePath: String): Boolean = files.containsKey(relativePath)

    override fun write(relativePath: String, bytes: ByteArray) {
        if (relativePath in failWritePaths) throw java.io.IOException("simulated write failure")
        files[relativePath] = bytes
    }

    override fun delete(relativePath: String): Boolean = files.remove(relativePath) != null

    override fun baseDir(): File = File(RECORD_IMAGES_DIR)
}
