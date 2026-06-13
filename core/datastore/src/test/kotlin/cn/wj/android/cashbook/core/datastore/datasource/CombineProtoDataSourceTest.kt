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

package cn.wj.android.cashbook.core.datastore.datasource

import androidx.datastore.core.DataStore
import cn.wj.android.cashbook.core.common.FIXED_TYPE_ID_CREDIT_CARD_PAYMENT
import cn.wj.android.cashbook.core.common.FIXED_TYPE_ID_REFUND
import cn.wj.android.cashbook.core.common.FIXED_TYPE_ID_REIMBURSE
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * CombineProtoDataSource 单元测试
 *
 * 覆盖范围：
 * - needRelated() 对退款/报销/普通类型 ID 的判断逻辑（真实构造 SUT 实例调用）
 * - decryptWebDAVPassword() 对空字符串和不含冒号旧明文的向后兼容逻辑
 * - encryptWebDAVPassword() 对空字符串的短路逻辑
 *
 * 注：涉及 AndroidKeyStore 的加解密路径需要真实 Android 环境，不在此测试。
 */
class CombineProtoDataSourceTest {

    // ------------------- needRelated -------------------
    // needRelated 仅比较 typeId 与固定常量、不读取任何 DataStore，
    // 故注入发射空流的假 DataStore 即可真实构造 SUT 并直调 suspend 方法。

    @Test
    fun when_type_is_refund_then_need_related_true() = runTest {
        assertThat(createDataSource().needRelated(FIXED_TYPE_ID_REFUND)).isTrue()
    }

    @Test
    fun when_type_is_reimburse_then_need_related_true() = runTest {
        assertThat(createDataSource().needRelated(FIXED_TYPE_ID_REIMBURSE)).isTrue()
    }

    @Test
    fun when_type_is_credit_card_payment_then_need_related_false() = runTest {
        // 相邻负固定类型 ID（信用卡还款 -2003）应为 false，
        // 区分"精确匹配 REFUND/REIMBURSE"与"所有负 ID 都为 true"的错误实现。
        assertThat(createDataSource().needRelated(FIXED_TYPE_ID_CREDIT_CARD_PAYMENT)).isFalse()
    }

    @Test
    fun when_type_is_normal_positive_then_need_related_false() = runTest {
        assertThat(createDataSource().needRelated(1L)).isFalse()
    }

    @Test
    fun when_type_is_zero_then_need_related_false() = runTest {
        assertThat(createDataSource().needRelated(0L)).isFalse()
    }

    // ------------------- decryptWebDAVPassword -------------------

    @Test
    fun when_blank_password_decrypt_then_returns_blank() {
        // 空字符串解密应直接返回空字符串（不触发 AndroidKeyStore）
        val result = CombineProtoDataSource.decryptWebDAVPassword("")
        assertThat(result).isEqualTo("")
    }

    @Test
    fun when_legacy_plain_password_then_returns_as_is() {
        // 不含 ":" 的旧明文密码应原样返回，保证向后兼容
        val plainPassword = "mypassword123"
        val result = CombineProtoDataSource.decryptWebDAVPassword(plainPassword)
        assertThat(result).isEqualTo(plainPassword)
    }

    // ------------------- encryptWebDAVPassword -------------------

    @Test
    fun when_blank_password_encrypt_then_returns_blank() {
        // 空字符串加密应直接返回空字符串（不触发 AndroidKeyStore）
        val result = CombineProtoDataSource.encryptWebDAVPassword("")
        assertThat(result).isEqualTo("")
    }

    // ------------------- 测试辅助 -------------------

    /** 发射空流、updateData 不被使用的假 DataStore（needRelated 路径不触达）。 */
    private fun <T> emptyDataStore(): DataStore<T> = object : DataStore<T> {
        override val data: Flow<T> = emptyFlow()
        override suspend fun updateData(transform: suspend (t: T) -> T): T =
            throw UnsupportedOperationException("DataStore not exercised by needRelated tests")
    }

    @Suppress("DEPRECATION")
    private fun createDataSource(): CombineProtoDataSource = CombineProtoDataSource(
        appPreferences = emptyDataStore(),
        appSettings = emptyDataStore(),
        recordSettings = emptyDataStore(),
        gitInfos = emptyDataStore(),
        searchHistory = emptyDataStore(),
        tempKeys = emptyDataStore(),
    )
}
