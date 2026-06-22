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

package cn.wj.android.cashbook.feature.settings.navigation

/**
 * 首页抽屉菜单点击回调聚合
 *
 * @param onMyAssetClick 我的资产点击回调
 * @param onMyBookClick 我的账本点击回调
 * @param onMyCategoryClick 我的分类点击回调
 * @param onMyTagClick 我的标签点击回调
 * @param onReimbursementClick 待报销点击回调
 * @param onSettingClick 设置点击回调
 * @param onAboutUsClick 关于我们点击回调
 */
data class LauncherDrawerActions(
    val onMyAssetClick: () -> Unit,
    val onMyBookClick: () -> Unit,
    val onMyCategoryClick: () -> Unit,
    val onMyTagClick: () -> Unit,
    val onReimbursementClick: () -> Unit,
    val onSettingClick: () -> Unit,
    val onAboutUsClick: () -> Unit,
)
