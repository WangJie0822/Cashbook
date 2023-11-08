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

package cn.wj.android.cashbook.core.common

/** 正则 - 金额，正负小数、整数 */
const val PATTERN_SIGN_MONEY = "(-?)|(-?\\d+[.]?\\d{0,2}|(-?\\d+))"

/** 密码校验正则 必须包含大小写字母及数字，大于8位 */
const val PASSWORD_REGEX =
    "^(?=.*[0-9])(?=.*[A-Z])(?=.*[a-z])[0-9a-zA-Z!@#\$%^&*,\\\\._]{8,24}\$"
