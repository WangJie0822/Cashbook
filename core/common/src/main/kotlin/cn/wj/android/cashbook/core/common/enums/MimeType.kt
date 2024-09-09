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

package cn.wj.android.cashbook.core.common.enums

/**
 * 媒体类型
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2024/9/9
 */
open class MimeType private constructor(val type: String, val subtype: String) {

    val isImage: Boolean
        get() = "image" == type

    val format: String
        get() = "$type/$subtype"

    /** 图片类型 */
    sealed class Image(type: String) : MimeType(type = "image", subtype = type) {
        data object JPEG : Image(type = "jpeg")
        data object PNG : Image(type = "png")
        data object WEBP : Image(type = "webp")
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + subtype.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val o = other as? MimeType ?: return false
        return this.format == o.format
    }

    override fun toString(): String {
        return "MimeType(type:$type, subtype:$subtype)"
    }

    companion object {
        fun parse(type: String?): MimeType? {
            if (null == type) {
                return null
            }
            val split = type.split("/")
            if (split.size != 2) {
                return null
            }
            val mimeType = MimeType(split[0], split[1])
            return when (mimeType) {
                Image.JPEG -> Image.JPEG
                Image.PNG -> Image.PNG
                Image.WEBP -> Image.WEBP
                else -> null
            }
        }
    }
}
