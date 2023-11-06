package cn.wj.android.cashbook.core.database.migration

import cn.wj.android.cashbook.core.database.table.TABLE_ASSET
import cn.wj.android.cashbook.core.database.table.TABLE_BOOKS
import cn.wj.android.cashbook.core.database.table.TABLE_RECORD
import cn.wj.android.cashbook.core.database.table.TABLE_RECORD_RELATED
import cn.wj.android.cashbook.core.database.table.TABLE_TAG
import cn.wj.android.cashbook.core.database.table.TABLE_TAG_RELATED
import cn.wj.android.cashbook.core.database.table.TABLE_TYPE
import org.intellij.lang.annotations.Language

@Language("SQL")
internal const val SQL_QUERY_ALL_FROM_ASSET = "SELECT * FROM `$TABLE_ASSET`"

@Language("SQL")
internal const val SQL_QUERY_ALL_FROM_BOOKS = "SELECT * FROM `$TABLE_BOOKS`"

@Language("SQL")
internal const val SQL_QUERY_ALL_FROM_RECORD = "SELECT * FROM `$TABLE_RECORD`"

@Language("SQL")
internal const val SQL_QUERY_ALL_FROM_RECORD_RELATED = "SELECT * FROM `$TABLE_RECORD_RELATED`"

@Language("SQL")
internal const val SQL_QUERY_ALL_FROM_TAG = "SELECT * FROM `$TABLE_TAG`"

@Language("SQL")
internal const val SQL_QUERY_ALL_FROM_TAG_RELATED = "SELECT * FROM `$TABLE_TAG_RELATED`"

@Language("SQL")
internal const val SQL_QUERY_ALL_FROM_TYPE = "SELECT * FROM `$TABLE_TYPE`"

/** 重命名资产表 */
@Language("SQL")
internal const val SQL_RENAME_TABLE_ASSET_TO_TEMP = """
    ALTER TABLE `${TABLE_ASSET}` RENAME TO `${TABLE_ASSET}_temp`
"""

/** 查询所有资产临时表数据 */
@Language("SQL")
internal const val SQL_QUERY_ALL_FROM_ASSET_TEMP = """
    SELECT * FROM `${TABLE_ASSET}_temp`
"""

/** 删除资产临时表 */
@Language("SQL")
internal const val SQL_DROP_TABLE_ASSET_TEMP = """
    DROP TABLE `${TABLE_ASSET}_temp`
"""

/** 重命名账本表 */
@Language("SQL")
internal const val SQL_RENAME_TABLE_BOOKS_TO_TEMP = """
    ALTER TABLE `${TABLE_BOOKS}` RENAME TO `${TABLE_BOOKS}_temp`
"""

/** 查询所有账本临时表数据 */
@Language("SQL")
internal const val SQL_QUERY_ALL_FROM_BOOKS_TEMP = """
    SELECT * FROM `${TABLE_BOOKS}_temp`
"""

/** 删除账本临时表 */
@Language("SQL")
internal const val SQL_DROP_TABLE_BOOKS_TEMP = """
    DROP TABLE `${TABLE_BOOKS}_temp`
"""

/** 重命名标签表 */
@Language("SQL")
internal const val SQL_RENAME_TABLE_TAG_TO_TEMP = """
    ALTER TABLE `${TABLE_TAG}` RENAME TO `${TABLE_TAG}_temp`
"""

/** 查询所有标签临时表数据 */
@Language("SQL")
internal const val SQL_QUERY_ALL_FROM_TAG_TEMP = """
        SELECT * FROM `${TABLE_TAG}_temp`
"""

/** 删除标签临时表 */
@Language("SQL")
internal const val SQL_DROP_TABLE_TAG_TEMP = """
    DROP TABLE `${TABLE_TAG}_temp`
"""

/** 重命名类型表 */
@Language("SQL")
internal const val SQL_RENAME_TABLE_TYPE_TO_TEMP = """
    ALTER TABLE `${TABLE_TYPE}` RENAME TO `${TABLE_TYPE}_temp`
"""

/** 查询所有类型临时表数据 */
@Language("SQL")
internal const val SQL_QUERY_ALL_FROM_TYPE_TEMP = """
    SELECT * FROM `${TABLE_TYPE}_temp`
"""

/** 删除类型临时表 */
@Language("SQL")
internal const val SQL_DROP_TABLE_TYPE_TEMP = """
    DROP TABLE `${TABLE_TYPE}_temp`
"""

/** 重命名记录表 */
@Language("SQL")
internal const val SQL_RENAME_TABLE_RECORD_TO_TEMP = """
    ALTER TABLE `${TABLE_RECORD}` RENAME TO `${TABLE_RECORD}_temp`
"""

/** 查询所有记录临时表数据 */
@Language("SQL")
internal const val SQL_QUERY_ALL_FROM_RECORD_TEMP = """
    SELECT * FROM `${TABLE_RECORD}_temp`
"""

/** 删除记录临时表 */
@Language("SQL")
internal const val SQL_DROP_TABLE_RECORD_TEMP = """
    DROP TABLE `${TABLE_RECORD}_temp`
"""