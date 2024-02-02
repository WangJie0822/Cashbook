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

package cn.wj.android.cashbook.core.network.datasource

/**
 * Url 定义
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/20
 */
object UrlDefinition {

    /** 使用 Gitee api */
    const val BASE_URL = "https://gitee.com/api/v5/"

    /** 查询 Gitee 中的 Release 信息 */
    const val GITEE_RELEASE_LIST = "https://gitee.com/api/v5/repos/{owner}/{repo}/releases"

    /** 查询 Github 中的 Release 信息 */
    const val GITHUB_RELEASE_LIST = "https://api.github.com/repos/{owner}/{repo}/releases"
}
