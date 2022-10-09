package cn.wj.android.cashbook.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cn.wj.android.cashbook.data.constants.SWITCH_INT_ON
import cn.wj.android.cashbook.data.database.dao.*
import cn.wj.android.cashbook.data.database.table.*

/**
 * 记账本数据库
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/15
 */
@Database(
    entities = [BooksTable::class, AssetTable::class, TypeTable::class, RecordTable::class, TagTable::class],
    version = 5
)
abstract class CashbookDatabase : RoomDatabase() {

    /** 获取账本相关数据库操作接口 */
    abstract fun booksDao(): BooksDao

    /** 获取资产相关数据库操作接口 */
    abstract fun assetDao(): AssetDao

    /** 获取类型数据库操作接口 */
    abstract fun typeDao(): TypeDao

    /** 获取记录数据库操作接口 */
    abstract fun recordDao(): RecordDao

    /** 获取标签数据库操作接口 */
    abstract fun tagDao(): TagDao

    companion object {

        /**
         * 数据库升级 1 -> 2
         * - 新增 db_tag 表
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 新增 标签 表
                database.execSQL("CREATE TABLE IF NOT EXISTS `db_tag` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `name` TEXT NOT NULL)")
            }
        }

        /**
         * 数据库升级 2 -> 3
         * - db_record 表 type 重命名为 type_enum，first_type_id 重命名为 type_id，删除了 second_type_id
         * - db_type 表删除了 system
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 更新记录表数据
                // 将旧表重命名
                database.execSQL("ALTER TABLE `db_record` RENAME TO `db_record_temp`")
                // 新建新表
                database.execSQL("CREATE TABLE IF NOT EXISTS `db_record` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `type_enum` TEXT NOT NULL, `type_id` INTEGER NOT NULL, `asset_id` INTEGER NOT NULL, `into_asset_id` INTEGER NOT NULL, `books_id` INTEGER NOT NULL, `record_id` INTEGER NOT NULL, `amount` REAL NOT NULL, `charge` REAL NOT NULL, `remark` TEXT NOT NULL, `tag_ids` TEXT NOT NULL, `reimbursable` INTEGER NOT NULL, `system` INTEGER NOT NULL, `record_time` INTEGER NOT NULL, `create_time` INTEGER NOT NULL, `modify_time` INTEGER NOT NULL)")
                // 从旧表中查询数据插入新表
                database.execSQL("INSERT INTO `db_record` SELECT `id`, `type`, `first_type_id`, `asset_id`, `into_asset_id`, `books_id`, `record_id`, `amount`, `charge`, `remark`, `tag_ids`, `reimbursable`, `system`, `record_time`, `create_time`, `modify_time` FROM `db_record_temp`")
                // 删除旧表
                database.execSQL("DROP TABLE `db_record_temp`")

                // 更新分类表数据
                // 将旧表重命名
                database.execSQL("ALTER TABLE `db_type` RENAME TO `db_type_temp`")
                // 新建新表
                database.execSQL("CREATE TABLE IF NOT EXISTS `db_type` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `parent_id` INTEGER NOT NULL, `name` TEXT NOT NULL, `icon_res_name` TEXT NOT NULL, `type` TEXT NOT NULL, `record_type` INTEGER NOT NULL, `child_enable` INTEGER NOT NULL, `refund` INTEGER NOT NULL, `reimburse` INTEGER NOT NULL, `sort` INTEGER NOT NULL)")
                // 从旧表中查询数据插入新表
                database.execSQL("INSERT INTO `db_type` SELECT `id`, `parent_id`, `name`, `icon_res_name`, `type`, `record_type`, `child_enable`, `refund`, `reimburse`, `sort` FROM `db_type_temp`")
                // 删除旧表
                database.execSQL("DROP TABLE `db_type_temp`")
            }
        }

        /**
         * 数据库升级 3 -> 4
         * - db_tag 表新增 books_id、shared
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 标签表新增所属账本主键
                database.execSQL("ALTER TABLE `db_tag` ADD `books_id` INTEGER DEFAULT -1 NOT NULL")
                database.execSQL("ALTER TABLE `db_tag` ADD `shared` INTEGER DEFAULT $SWITCH_INT_ON NOT NULL")
            }
        }

        /**
         * 数据库升级 4 -> 5
         * - db_asset 表新增 open_bank、card_no、remark
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 资产表新增字段
                database.execSQL("ALTER TABLE `db_asset` ADD `open_bank` TEXT DEFAULT '' NOT NULL")
                database.execSQL("ALTER TABLE `db_asset` ADD `card_no` TEXT DEFAULT '' NOT NULL")
                database.execSQL("ALTER TABLE `db_asset` ADD `remark` TEXT DEFAULT '' NOT NULL")
            }
        }

        /** 数据库升级列表 */
        val MIGRATION_LIST = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
    }
}