@file:Suppress("unused")
@file:JvmName("DatabaseTransform")

package cn.wj.android.cashbook.data.transform

import cn.wj.android.cashbook.base.ext.base.orElse
import cn.wj.android.cashbook.base.tools.dateFormat
import cn.wj.android.cashbook.base.tools.toLongTime
import cn.wj.android.cashbook.data.constants.SWITCH_INT_OFF
import cn.wj.android.cashbook.data.constants.SWITCH_INT_ON
import cn.wj.android.cashbook.data.database.table.BooksTable
import cn.wj.android.cashbook.data.entity.BooksEntity
import cn.wj.android.cashbook.data.enums.CurrencyEnum

/**
 * 数据库数据转换相关
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/21
 */


/** 将数据库数据转换为对应数据实体类 */
internal fun BooksTable.toBooksEntity(): BooksEntity {
    return BooksEntity(
        id = id.orElse(-1L),
        name = name,
        imageUrl = imageUrl,
        description = description,
        currency = CurrencyEnum.fromName(currency),
        selected = selected == SWITCH_INT_ON,
        createTime = createTime.dateFormat(),
        modifyTime = modifyTime.dateFormat()
    )
}

/** 将数据实体类转换为对应数据库数据 */
internal fun BooksEntity.toBooksTable(): BooksTable {
    return BooksTable(
        id = if (-1L == id) null else id,
        name = name,
        imageUrl = imageUrl,
        description = description,
        currency = currency?.name.orEmpty(),
        selected = if (selected) SWITCH_INT_ON else SWITCH_INT_OFF,
        createTime = createTime.toLongTime().orElse(0L),
        modifyTime = modifyTime.toLongTime().orElse(0L)
    )
}