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
 * 自动备份
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/7/18
 */
enum class AutoBackupModeEnum {

    /** 关闭 */
    CLOSE,

    /** 每次启动 */
    WHEN_LAUNCH,

    /** 每天 */
    EACH_DAY,

    /** 每周 */
    EACH_WEEK,
    ;

    companion object {
        fun ordinalOf(ordinal: Int): AutoBackupModeEnum {
            return entries.first { it.ordinal == ordinal }
        }
    }
}
