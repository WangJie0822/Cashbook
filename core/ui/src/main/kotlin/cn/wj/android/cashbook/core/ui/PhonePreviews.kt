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

package cn.wj.android.cashbook.core.ui

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Preview

/**
 * 用于展示控件在不同主题预览效果的注解
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/7/11
 */
@Preview(
    name = "phone-light",
    device = "spec:shape=Normal,width=360,height=640,unit=dp,dpi=480",
    uiMode = UI_MODE_NIGHT_NO,
)
@Preview(
    name = "phone-night",
    device = "spec:shape=Normal,width=360,height=640,unit=dp,dpi=480",
    uiMode = UI_MODE_NIGHT_YES,
)
annotation class PhonePreviews
