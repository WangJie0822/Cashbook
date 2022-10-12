@file:Suppress("unused")
@file:JvmName("DatabaseTransform")

package cn.wj.android.cashbook.data.transform

import cn.wj.android.cashbook.base.ext.base.orElse
import cn.wj.android.cashbook.base.ext.base.toDoubleOrZero
import cn.wj.android.cashbook.base.tools.dateFormat
import cn.wj.android.cashbook.base.tools.toLongTime
import cn.wj.android.cashbook.data.constants.SWITCH_INT_OFF
import cn.wj.android.cashbook.data.constants.SWITCH_INT_ON
import cn.wj.android.cashbook.data.database.table.*
import cn.wj.android.cashbook.data.entity.*
import cn.wj.android.cashbook.data.enums.*

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
internal fun BooksEntity.toAssetTable(): BooksTable {
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

/** 将数据库数据转换为对应数据实体类 */
internal fun AssetTable.toAssetEntity(balance: String, sort: Int = -1): AssetEntity {
    return AssetEntity(
        id = id.orElse(-1),
        booksId = booksId,
        name = name,
        totalAmount = totalAmount.toString(),
        billingDate = billingDate,
        repaymentDate = repaymentDate,
        type = ClassificationTypeEnum.fromName(type)
            .orElse(ClassificationTypeEnum.CREDIT_CARD_ACCOUNT),
        classification = AssetClassificationEnum.fromName(classification)
            .orElse(AssetClassificationEnum.CASH),
        invisible = invisible == SWITCH_INT_ON,
        openBank = openBank,
        cardNo = cardNo,
        remark = remark,
        sort = if (sort == -1) this.sort else sort,
        createTime = createTime.dateFormat(),
        modifyTime = modifyTime.dateFormat(),
        balance = balance
    )
}

/** 将数据实体类转换为对应数据库数据 */
internal fun AssetEntity.toAssetTable(): AssetTable {
    return AssetTable(
        id = if (-1L == id) null else id,
        booksId = booksId,
        name = name,
        totalAmount = totalAmount.toDoubleOrZero(),
        billingDate = billingDate,
        repaymentDate = repaymentDate,
        type = type.name,
        classification = classification.name,
        invisible = if (invisible) SWITCH_INT_ON else SWITCH_INT_OFF,
        openBank = openBank,
        cardNo = cardNo,
        remark = remark,
        sort = sort,
        createTime = createTime.toLongTime().orElse(0L),
        modifyTime = modifyTime.toLongTime().orElse(0L)
    )
}

/** 将数据库数据转换为对应数据实体类 */
internal fun TypeTable.toTypeEntity(parent: TypeEntity?): TypeEntity {
    return TypeEntity(
        id = id.orElse(-1L),
        parent = parent,
        name = name,
        iconResName = iconResName,
        type = TypeEnum.fromName(type).orElse(TypeEnum.FIRST),
        recordType = RecordTypeEnum.fromPosition(recordType).orElse(RecordTypeEnum.INCOME),
        childEnable = childEnable == SWITCH_INT_ON,
        refund = refund == SWITCH_INT_ON,
        reimburse = reimburse == SWITCH_INT_ON,
        sort = sort,
        childList = arrayListOf()
    )
}

/** 将数据实体类转换为对应数据库数据 */
internal fun TypeEntity.toTypeTable(): TypeTable {
    return TypeTable(
        id = if (-1L == id) null else id,
        parentId = parent?.id.orElse(-1L),
        name = name,
        iconResName = iconResName,
        type = type.name,
        recordType = recordType.position,
        childEnable = if (childEnable) SWITCH_INT_ON else SWITCH_INT_OFF,
        refund = if (refund) SWITCH_INT_ON else SWITCH_INT_OFF,
        reimburse = if (reimburse) SWITCH_INT_ON else SWITCH_INT_OFF,
        sort = sort
    )
}

/** 将数据库数据转换为对应数据实体类 */
internal fun TagTable.toTagEntity(): TagEntity {
    return TagEntity(
        id = id.orElse(-1L),
        name = name,
        booksId = booksId,
        shared = shared == SWITCH_INT_ON
    )
}

/** 将数据实体类转换为对应数据库数据 */
internal fun TagEntity.toTagTable(): TagTable {
    return TagTable(
        id = if (-1L == id) null else id,
        name = name,
        booksId = booksId,
        shared = if (shared) SWITCH_INT_ON else SWITCH_INT_OFF
    )
}

/** 将数据实体类转换为对应数据库数据 */
internal fun RecordEntity.toRecordTable(): RecordTable {
    return RecordTable(
        id = if (-1L == id) null else id,
        typeEnum = typeEnum.name,
        typeId = type?.id.orElse(-1L),
        assetId = asset?.id.orElse(-1L),
        intoAssetId = intoAsset?.id.orElse(-1L),
        booksId = booksId,
        recordId = record?.id.orElse(-1L),
        recordAmount = amount.toDoubleOrZero(),
        recordCharge = charge.toDoubleOrZero(),
        remark = remark,
        tagIds = with(StringBuilder()) {
            tags.forEach {
                if (isNotBlank()) {
                    append(",")
                }
                append(it.id.toString())
            }
            toString()
        },
        reimbursable = if (reimbursable) SWITCH_INT_ON else SWITCH_INT_OFF,
        system = if (system) SWITCH_INT_ON else SWITCH_INT_OFF,
        recordTime = recordTime,
        createTime = createTime.toLongTime().orElse(0L),
        modifyTime = modifyTime.toLongTime().orElse(0L)
    )
}