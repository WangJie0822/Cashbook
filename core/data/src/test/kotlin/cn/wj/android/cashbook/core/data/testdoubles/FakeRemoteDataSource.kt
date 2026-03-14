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

package cn.wj.android.cashbook.core.data.testdoubles

import cn.wj.android.cashbook.core.network.datasource.RemoteDataSource
import cn.wj.android.cashbook.core.network.entity.GitReleaseEntity

/**
 * RemoteDataSource 的测试替身
 *
 * 模拟远程数据源的行为，可通过设置 releaseToReturn 控制返回值，
 * 设置 shouldThrow 模拟网络异常。
 */
class FakeRemoteDataSource : RemoteDataSource {

    /** 下一次 checkUpdate 返回的结果 */
    var releaseToReturn: GitReleaseEntity? = null

    /** 是否抛出异常，模拟网络错误 */
    var shouldThrow: Boolean = false

    /** 记录最近一次 checkUpdate 调用的 useGitee 参数 */
    var lastUseGitee: Boolean? = null

    /** 记录最近一次 checkUpdate 调用的 canary 参数 */
    var lastCanary: Boolean? = null

    override suspend fun checkUpdate(useGitee: Boolean, canary: Boolean): GitReleaseEntity? {
        lastUseGitee = useGitee
        lastCanary = canary
        if (shouldThrow) {
            throw RuntimeException("模拟网络异常")
        }
        return releaseToReturn
    }
}
