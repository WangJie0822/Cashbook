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

package cn.wj.android.cashbook.core.model.model

/**
 * 图片数据实体类
 *
 * @param id 主键自增长
 * @param recordId 关联的记录 id
 * @param path 图片路径
 * @param bytes 图片数据
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2025/2/24
 */
data class ImageModel(
    val id: Long,
    val recordId: Long,
    val path: String,
    val bytes: ByteArray,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImageModel

        if (id != other.id) return false
        if (recordId != other.recordId) return false
        if (path != other.path) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + recordId.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}
