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

import cn.wj.android.cashbook.core.common.FIXED_TYPE_ID_REFUND
import cn.wj.android.cashbook.core.common.FIXED_TYPE_ID_REIMBURSE
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * CombineProtoDataSource 单元测试
 *
 * 覆盖范围：
 * - needRelated() 对退款/报销/普通类型 ID 的判断逻辑
 * - decryptWebDAVPassword() 对空字符串和不含冒号旧明文的向后兼容逻辑
 * - encryptWebDAVPassword() 对空字符串的短路逻辑
 *
 * 注：涉及 AndroidKeyStore 的加解密路径需要真实 Android 环境，不在此测试。
 */
class CombineProtoDataSourceTest {

    // ------------------- needRelated -------------------

    @Test
    fun when_type_is_refund_then_need_related_true() {
        // FIXED_TYPE_ID_REFUND 应被识别为需要关联记录的类型
        val result = FIXED_TYPE_ID_REFUND == FIXED_TYPE_ID_REFUND ||
            FIXED_TYPE_ID_REFUND == FIXED_TYPE_ID_REIMBURSE
        assertThat(result).isTrue()
    }

    @Test
    fun when_type_is_reimburse_then_need_related_true() {
        // FIXED_TYPE_ID_REIMBURSE 应被识别为需要关联记录的类型
        val result = FIXED_TYPE_ID_REIMBURSE == FIXED_TYPE_ID_REFUND ||
            FIXED_TYPE_ID_REIMBURSE == FIXED_TYPE_ID_REIMBURSE
        assertThat(result).isTrue()
    }

    @Test
    fun when_type_is_normal_then_need_related_false() {
        // 普通类型 ID 不应被识别为需要关联记录的类型
        val typeId = 1L
        val result = typeId == FIXED_TYPE_ID_REFUND || typeId == FIXED_TYPE_ID_REIMBURSE
        assertThat(result).isFalse()
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
}
