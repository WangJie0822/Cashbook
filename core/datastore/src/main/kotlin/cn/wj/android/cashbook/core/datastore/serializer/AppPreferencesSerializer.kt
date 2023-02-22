package cn.wj.android.cashbook.core.datastore.serializer

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import cn.wj.android.cashbook.core.datastore.AppPreferences
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

/**
 * [AppPreferences] 数据序列化实现
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/21
 */
class AppPreferencesSerializer @Inject constructor() : Serializer<AppPreferences> {

    override val defaultValue: AppPreferences = AppPreferences.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): AppPreferences = try {
        AppPreferences.parseFrom(input)
    } catch (exception: InvalidProtocolBufferException) {
        throw CorruptionException("Cannot read proto.", exception)
    }

    override suspend fun writeTo(t: AppPreferences, output: OutputStream) {
        t.writeTo(output)
    }
}
