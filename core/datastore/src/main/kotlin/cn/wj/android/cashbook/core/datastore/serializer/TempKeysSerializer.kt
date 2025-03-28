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

package cn.wj.android.cashbook.core.datastore.serializer

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import cn.wj.android.cashbook.core.datastore.TempKeys
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

/**
 * [TempKeys] 数据序列化实现
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2025/1/20
 */
class TempKeysSerializer @Inject constructor() : Serializer<TempKeys> {

    override val defaultValue: TempKeys = TempKeys.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): TempKeys = try {
        TempKeys.parseFrom(input)
    } catch (exception: InvalidProtocolBufferException) {
        throw CorruptionException("Cannot read proto.", exception)
    }

    override suspend fun writeTo(t: TempKeys, output: OutputStream) {
        t.writeTo(output)
    }
}
