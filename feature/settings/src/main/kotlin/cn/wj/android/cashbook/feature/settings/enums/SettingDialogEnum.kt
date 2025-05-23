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

/**
 * 设置页弹窗
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/20
 */
enum class SettingDialogEnum {

    /** 创建密码 */
    CREATE_PASSWORD,

    /** 修改密码 */
    MODIFY_PASSWORD,

    /** 验证密码 */
    VERIFY_PASSWORD,

    /** 清除密码 */
    CLEAR_PASSWORD,

    /** 图片质量 */
    IMAGE_QUALITY,

    /** 黑夜模式 */
    DARK_MODE,

    /** 动态配色 */
    DYNAMIC_COLOR,

    /** 安全验证模式 */
    VERIFICATION_MODE,
}
