package cn.wj.android.cashbook.core.datastore.serializer

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import cn.wj.android.cashbook.core.datastore.GitInfos
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

/**
 * [GitInfos] 数据序列化实现
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/7/12
 */
class GitInfosSerializer @Inject constructor() : Serializer<GitInfos> {

    override val defaultValue: GitInfos = GitInfos.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): GitInfos = try {
        GitInfos.parseFrom(input)
    } catch (exception: InvalidProtocolBufferException) {
        throw CorruptionException("Cannot read proto.", exception)
    }

    override suspend fun writeTo(t: GitInfos, output: OutputStream) {
        t.writeTo(output)
    }
}
