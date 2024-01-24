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

package cn.wj.android.cashbook.feature.settings.enums

enum class MainAppBookmarkEnum {

    /** 无提示 */
    NONE,

    /** 解密失败 */
    PASSWORD_DECODE_FAILED,

    /** 密码错误 */
    PASSWORD_WRONG,

    /** 认证失败 */
    ERROR,

    /** 更新下载中 */
    UPDATE_DOWNLOADING,

    /** 不需要更新 */
    NO_NEED_UPDATE,

    /** 开始下载 */
    START_DOWNLOAD,

    /** 下载失败 */
    DOWNLOAD_FAILED,

    /** 安装失败 */
    INSTALL_FAILED,
}
