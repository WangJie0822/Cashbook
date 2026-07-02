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
 * 标签数据实体类
 *
 * @param id 标签主键
 * @param name 标签名
 * @param invisible 是否隐藏
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/3/1
 */
data class TagModel(
    val id: Long,
    val name: String,
    val invisible: Boolean,
)
