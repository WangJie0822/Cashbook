package cn.wj.android.cashbook.core.datastore.serializer

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import cn.wj.android.cashbook.core.datastore.SearchHistory
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

/**
 * [SearchHistory] 数据序列化实现
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/10/19
 */
class SearchHistorySerializer @Inject constructor() : Serializer<SearchHistory> {

    override val defaultValue: SearchHistory = SearchHistory.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): SearchHistory = try {
        SearchHistory.parseFrom(input)
    } catch (exception: InvalidProtocolBufferException) {
        throw CorruptionException("Cannot read proto.", exception)
    }

    override suspend fun writeTo(t: SearchHistory, output: OutputStream) {
        t.writeTo(output)
    }
}
