@file:Suppress("unused")
@file:JvmName("StringExt")

package cn.wj.android.cashbook.base.ext.base

/** 从对象[String]以及候选对象[strArray]中按先后顺序获取非空[String]对象，若全部为空返回`""` */
fun String?.orEmpty(vararg strArray: String?): String {
    return this ?: (strArray.firstOrNull {
        null != it
    } ?: "")
}

/** 字符串不为 `null` 且内容不为空时执行 [block] */
fun String?.runIfNotNullAndBlank(block: String.() -> Unit) {
    if (this.isNullOrBlank()) {
        return
    }
    this.block()
}

fun String?.ifNullOrBlank(block: () -> String): String {
    return if (this.isNullOrBlank()) {
        block.invoke()
    } else {
        this
    }
}

/** 对金额字符串进行格式化 */
fun String?.moneyFormat(): String {
    return this?.toBigDecimalOrNull().orElse("0".toBigDecimal()).formatToNumber()
}

fun String?.toMoneyFloat():Float{
    return this?.toFloatOrNull().orElse(0f)
}