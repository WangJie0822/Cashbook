@file:Suppress("unused")
@file:JvmName("DatabaseTransform")

package cn.wj.android.cashbook.data.transform

import cn.wj.android.cashbook.base.ext.base.orElse
import cn.wj.android.cashbook.base.tools.DATE_FORMAT_NO_SECONDS
import cn.wj.android.cashbook.base.tools.dateFormat
import cn.wj.android.cashbook.base.tools.toLongTime
import cn.wj.android.cashbook.data.constants.SWITCH_INT_OFF
import cn.wj.android.cashbook.data.constants.SWITCH_INT_ON
import cn.wj.android.cashbook.data.database.table.AssetTable
import cn.wj.android.cashbook.data.database.table.BooksTable
import cn.wj.android.cashbook.data.database.table.RecordTable
import cn.wj.android.cashbook.data.database.table.TypeTable
import cn.wj.android.cashbook.data.entity.AssetEntity
import cn.wj.android.cashbook.data.entity.BooksEntity
import cn.wj.android.cashbook.data.entity.RecordEntity
import cn.wj.android.cashbook.data.entity.TypeEntity
import cn.wj.android.cashbook.data.enums.AssetClassificationEnum
import cn.wj.android.cashbook.data.enums.ClassificationTypeEnum
import cn.wj.android.cashbook.data.enums.CurrencyEnum
import cn.wj.android.cashbook.data.enums.RecordTypeEnum
import cn.wj.android.cashbook.data.enums.TypeEnum

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
internal fun AssetTable.toAssetEntity(balance: String): AssetEntity {
    return AssetEntity(
        id = id.orElse(-1),
        booksId = booksId,
        name = name,
        totalAmount = totalAmount,
        billingDate = billingDate,
        repaymentDate = repaymentDate,
        type = ClassificationTypeEnum.fromName(type).orElse(ClassificationTypeEnum.CREDIT_CARD_ACCOUNT),
        classification = AssetClassificationEnum.fromName(classification).orElse(AssetClassificationEnum.CASH),
        invisible = invisible == SWITCH_INT_ON,
        sort = sort,
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
        totalAmount = totalAmount,
        billingDate = billingDate,
        repaymentDate = repaymentDate,
        type = type.name,
        classification = classification.name,
        invisible = if (invisible) SWITCH_INT_ON else SWITCH_INT_OFF,
        sort = sort,
        createTime = createTime.toLongTime().orElse(0L),
        modifyTime = modifyTime.toLongTime().orElse(0L)
    )
}

/** 将数据库数据转换为对应数据实体类 */
internal fun TypeTable.toTypeEntity(): TypeEntity {
    return TypeEntity(
        id = id.orElse(-1L),
        parentId = parentId,
        name = name,
        iconResName = iconResName,
        type = TypeEnum.fromName(type).orElse(TypeEnum.FIRST),
        recordType = RecordTypeEnum.fromPosition(recordType).orElse(RecordTypeEnum.INCOME),
        childEnable = childEnable == SWITCH_INT_ON,
        sort = sort,
        childList = arrayListOf()
    )
}

/** 将数据实体类转换为对应数据库数据 */
internal fun TypeEntity.toTypeTable(): TypeTable {
    return TypeTable(
        id = if (-1L == id) null else id,
        parentId = parentId,
        name = name,
        iconResName = iconResName,
        type = type.name,
        recordType = recordType.position,
        childEnable = if (childEnable) SWITCH_INT_ON else SWITCH_INT_OFF,
        sort = sort
    )
}

/** 将数据实体类转换为对应数据库数据 */
internal fun RecordEntity.toRecordTable(): RecordTable {
    return RecordTable(
        id = if (-1L == id) null else id,
        type = type.name,
        firstTypeId = firstType?.id.orElse(-1L),
        secondTypeId = secondType?.id.orElse(-1L),
        assetId = asset?.id.orElse(-1L),
        intoAssetId = intoAsset?.id.orElse(-1L),
        booksId = booksId,
        amount = amount,
        charge = charge,
        remark = remark,
        // TODO
        tagIds = "",
        reimbursable = if (reimbursable) SWITCH_INT_ON else SWITCH_INT_OFF,
        recordTime = recordTime.toLongTime(DATE_FORMAT_NO_SECONDS).orElse(0L),
        createTime = createTime.toLongTime().orElse(0L),
        modifyTime = modifyTime.toLongTime().orElse(0L)
    )
}