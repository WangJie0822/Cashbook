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

package cn.wj.android.cashbook.core.model.enums

/**
 * 图片质量
 *
 * @param inSampleSize 图片采样率，value <= 1 返回原图，value > 1 取 1/value
 * @param reSize 进行二次缩放
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2025/4/26
 */
enum class ImageQualityEnum(val inSampleSize: Int, val reSize: Boolean) {

    /** 原图 */
    ORIGINAL(inSampleSize = 1, reSize = false),

    /** 高质量 */
    HIGH(inSampleSize = 2, reSize = false),

    /** 中质量 */
    MEDIUM(inSampleSize = 3, reSize = true),
    ;

    companion object {
        fun ordinalOf(ordinal: Int): ImageQualityEnum {
            return entries.first { it.ordinal == ordinal }
        }
    }
}
