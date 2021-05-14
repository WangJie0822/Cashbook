@file:Suppress("unused")

package cn.wj.android.cashbook.tools

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

/**
 * 数据转换相关
 *
 * > [jiewang41](mailto:jiewang41@iflytek.com) 创建于 20201/3/8
 */

/** 默认 [Json] 解析 */
val jsonDefault = Json {
    // 是否编码默认值
    encodeDefaults = true
    // 忽略未知 key 不抛出异常
    ignoreUnknownKeys = true
    // 是否使用宽松模式
    isLenient = true
    // 是否允许将 key-value 转换为 数组
    allowStructuredMapKeys = false
    // 是否对打印的 json 格式化
    prettyPrint = true
    // 指定打印缩进字符串
//    prettyPrintIndent = "    "
    // 非空类型为空或找不到对应枚举时使用默认值
    coerceInputValues = false
    // 将多态序列化为默认数组格式
    useArrayPolymorphism = false
    // 多态序列化的类描述符属性的名称
//    classDiscriminator = "type"
    // 是否取消对特殊浮点值的规范
    allowSpecialFloatingPointValues = false
    // 指定序列化模块
    serializersModule = SerializersModule {
        contextual(Any::class, AnyKSerializer)
    }
}

/**
 * [Any] 序列化器
 *
 * - 创建时间：2021/1/5
 *
 * @author 王杰
 */
object AnyKSerializer : KSerializer<Any> {

    override val descriptor: SerialDescriptor
        get() = buildClassSerialDescriptor("kotlin.Any") {
        }

    override fun serialize(encoder: Encoder, value: Any) {
        encoder.encodeStructure(descriptor) {
        }
    }

    override fun deserialize(decoder: Decoder): Any {
        return decoder.decodeStructure(descriptor) {
            Any()
        }
    }
}

/**
 * 使用 [json] 以及 [deserializer] 将 [String] 解析为 [T] 数据实体
 * > 转换失败返回 `null`
 */
inline fun <reified T> String?.toTypeEntity(
    json: Json = jsonDefault,
    deserializer: DeserializationStrategy<T>? = null
): T? {
    return when {
        this.isNullOrBlank() -> null
        null != deserializer -> json.decodeFromString(deserializer, this)
        else -> json.decodeFromString(this)
    }
}

/**
 * 使用 [json] 以及 [serializer] 将数据实体 [T] 转换为 [String]
 * > 转换失败返回 `""`
 */
inline fun <reified T> T?.toJsonString(
    json: Json = jsonDefault,
    serializer: SerializationStrategy<T>? = null
): String {
    return when {
        null == this -> ""
        null != serializer -> json.encodeToString(serializer, this)
        else -> json.encodeToString(this)
    }
}