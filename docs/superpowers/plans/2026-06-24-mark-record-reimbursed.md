# 待报销记录手动「标记已报销」Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在记录详情弹窗为「可报销且未关联」的支出新增手动「标记已报销/改回待报销」动作（二次确认），把这类记录移出待报销列表并显示「已报销」标签，金额统计完全不变。

**Architecture:** 新增持久化布尔列 `RecordTable.reimbursed`（Room Migration13To14 纯加列）。待报销查询与报销关联选择器查询均加 `AND reimbursed=0`，配合「标记按钮仅在未关联记录显示」，保证「已手动标记」与「已真实关联」两态由构造互斥（无 stale-flag）。字段以 `reimbursed: Boolean = false` 末位默认参贯穿模型链（收窄构造站点改动面）。详情弹窗经新 `RecordDetailsSheetViewModel` → `UpdateRecordReimbursedUseCase` → Repository（按 currentBookId 守护 + 先写库后 bump `recordDataVersion`）写库。

**Tech Stack:** Kotlin, Jetpack Compose, Room, Hilt(KSP), Coroutines/Flow, JUnit4 + Truth, Roborazzi(截图), Room MigrationTestHelper(androidTest)。

设计依据：`docs/superpowers/specs/2026-06-24-mark-record-reimbursed-design.md`。

## Global Constraints

- 金额全链路 `Long`，单位：分；本功能**不触碰任何金额字段/口径**（finalAmount/recordAmount/analyticsPie/吸收簇/月度汇总）。
- `core:model` 是 JVM 库（`cashbook.jvm.library`）：测试任务 `:core:model:test`（**无** `testDebugUnitTest`）。
- Android 库（core:data/core:domain/core:database/feature:*）测试任务 `:<module>:testDebugUnitTest`；DAO/迁移 instrumented 测试 `:core:database:connectedDebugAndroidTest`。
- DB 升版必须导出 schema `14.json` 且 `runMigrationsAndValidate` 通过；Room 新增列须 `@ColumnInfo(defaultValue = "0")` 才能与 `ALTER TABLE ... ADD COLUMN ... DEFAULT 0` 生成的 schema 匹配。
- UI 只用 `core/design` 封装组件（`CbAlertDialog`/`CbTextButton` 等），禁止直接用 Material3 同名组件（lint `Design` error）。
- `core:data` 的 test 源集**不依赖** `core:testing`（仅 junit+truth，自带 `createXxx`）；`core:domain`/`feature:*` test 用 `core:testing` 的 `FakeXxxRepository`/`createXxxModel`。
- 接口/DAO 新增方法必须同步 `RecordRepositoryImpl` + `FakeRecordRepository`(core:testing) + `FakeRecordDao`(core:data test)，否则相关 test 模块编译失败。
- 改 Composable/ViewModel 签名必须同步该模块 `src/test` 的截图测试与 ViewModelTest（整模块测试源集编译）。
- 本机受代理 Maven Central TLS 环境限制，`connectedDebugAndroidTest`（migration/DAO instrumented）可能无法跑——这些任务 **compile-verified + 标注真机补跑**，不阻塞后续。
- spotless：源文件需 Apache 2.0 License Header；ktlint(android)。提交前 `spotlessApply`。
- 每个 commit 原子化，遵循 `[类型|模块|功能][影响范围]说明` 中文格式。

---

### Task 1: 数据库列 + 版本 + Migration13To14 + schema + 迁移测试

**Files:**
- Modify: `core/common/src/main/kotlin/cn/wj/android/cashbook/core/common/ApplicationInfo.kt:29`
- Modify: `core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/table/ColumnNames.kt:58`
- Modify: `core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/table/RecordTable.kt:66`
- Create: `core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/migration/Migration13To14.kt`
- Modify: `core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/migration/DatabaseMigrations.kt:48`
- Create (generated): `core/database/schemas/cn.wj.android.cashbook.core.database.CashbookDatabase/14.json`
- Test: `core/database/src/androidTest/kotlin/cn/wj/android/cashbook/core/database/DatabaseTest.kt`（新增 `migrate13_14`）

**Interfaces:**
- Produces: `RecordTable.reimbursed: Int`（列名 `reimbursed`，SQL `DEFAULT 0`）；`Migration13To14`（注册进 `MIGRATION_LIST`）；`ApplicationInfo.DB_VERSION = 14`；`ColumnNames.TABLE_RECORD_REIMBURSED = "reimbursed"`。

- [ ] **Step 1: 写失败的迁移测试** `core/database/src/androidTest/.../DatabaseTest.kt`（在 `migrate12_13` 之后、`migrateAll` 之前插入，并在文件顶部 import 区加 `import cn.wj.android.cashbook.core.database.migration.Migration13To14`）

```kotlin
    /**
     * 测试数据库升级 13 -> 14
     * - 新增 db_record.reimbursed 列（INTEGER NOT NULL DEFAULT 0）
     * - 存量行迁移后默认值为 0
     */
    @Test
    @Throws(IOException::class)
    fun migrate13_14() {
        log("migrate13_14()")
        helper.createDatabase(testDbName, 13).use { db ->
            // v13 db_record 全列插入一行（无 reimbursed 列）
            db.execSQL(
                "INSERT INTO `db_record` " +
                    "(`id`,`type_id`,`asset_id`,`into_asset_id`,`books_id`,`amount`,`final_amount`," +
                    "`concessions`,`charge`,`remark`,`reimbursable`,`record_time`) " +
                    "VALUES (1,1,-1,-1,1,1000,1000,0,0,'r',1,1704067200000)",
            )
        }
        var reimbursedValue = -1
        helper.runMigrationsAndValidate(testDbName, 14, true, Migration13To14).use { db ->
            db.query("SELECT `reimbursed` FROM `db_record` WHERE `id`=1").use { cursor ->
                cursor.moveToFirst()
                reimbursedValue = cursor.getInt(0)
            }
        }
        log("migrate13_14() reimbursedValue=$reimbursedValue")
        // 存量行迁移后 reimbursed 默认 0
        Assert.assertEquals(0, reimbursedValue)
    }
```

- [ ] **Step 2: 运行测试确认失败（编译期/红）**

Run: `./gradlew :core:database:compileDebugAndroidTestKotlin`
Expected: FAIL（`Migration13To14` 未定义 / `unresolved reference`）。
> 真机执行 `:core:database:connectedDebugAndroidTest --tests "*.DatabaseTest.migrate13_14"` 受本机代理 TLS 限制，本任务以 compile-verified 为准 + 标注真机补跑。

- [ ] **Step 3: 升 DB_VERSION** `ApplicationInfo.kt:29`

```kotlin
    const val DB_VERSION = 14
```

- [ ] **Step 4: 加列名常量** `ColumnNames.kt`（`TABLE_RECORD_RECORD_TIME` 行之后，第 58 行附近）

```kotlin
const val TABLE_RECORD_REIMBURSED = "reimbursed"
```

- [ ] **Step 5: 加表列** `RecordTable.kt`（`reimbursable` 行之后；KDoc `@param reimbursable` 之后补 `@param reimbursed 是否已手动标记已报销`）

```kotlin
    @ColumnInfo(name = TABLE_RECORD_REIMBURSABLE) val reimbursable: Int,
    @ColumnInfo(name = TABLE_RECORD_REIMBURSED, defaultValue = "0") val reimbursed: Int = SWITCH_INT_OFF,
    @ColumnInfo(name = TABLE_RECORD_RECORD_TIME) val recordTime: Long,
```

文件顶部 import 区加：`import cn.wj.android.cashbook.core.common.SWITCH_INT_OFF`

- [ ] **Step 6: 新建迁移类** `core/database/.../migration/Migration13To14.kt`（含 Apache 2.0 License Header）

```kotlin
package cn.wj.android.cashbook.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cn.wj.android.cashbook.core.common.ext.logger
import org.intellij.lang.annotations.Language

/**
 * 数据库升级 13 -> 14
 * - 新增 db_record.reimbursed 列（手动「已报销」标记，INTEGER NOT NULL DEFAULT 0）
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2026/6/24
 */
object Migration13To14 : Migration(13, 14) {

    @Language("SQL")
    private const val SQL_ADD_REIMBURSED =
        "ALTER TABLE db_record ADD COLUMN reimbursed INTEGER NOT NULL DEFAULT 0"

    override fun migrate(db: SupportSQLiteDatabase) {
        logger().i("migrate(db)")
        db.execSQL(SQL_ADD_REIMBURSED)
    }
}
```

- [ ] **Step 7: 注册迁移** `DatabaseMigrations.kt`（`MIGRATION_LIST` 末尾 `Migration12To13,` 之后）

```kotlin
            Migration12To13,
            Migration13To14,
        )
```

- [ ] **Step 8: 生成 schema 14.json**

Run: `./gradlew :core:database:kspDebugKotlin`
Expected: 生成 `core/database/schemas/cn.wj.android.cashbook.core.database.CashbookDatabase/14.json`，其中 `db_record` 含 `reimbursed`（`"notNull": true, "defaultValue": "0"`）。
> 若本机离线缓存缺依赖：去 `--offline`，清继承代理后加 `-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897` 暖缓存。

- [ ] **Step 9: 验证编译通过**

Run: `./gradlew :core:database:compileDebugAndroidTestKotlin`
Expected: PASS（`Migration13To14` 已解析）。

- [ ] **Step 10: spotless + 提交**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
git add core/common/.../ApplicationInfo.kt core/database/.../ColumnNames.kt core/database/.../RecordTable.kt core/database/.../migration/Migration13To14.kt core/database/.../migration/DatabaseMigrations.kt "core/database/schemas/cn.wj.android.cashbook.core.database.CashbookDatabase/14.json" core/database/src/androidTest/.../DatabaseTest.kt
git commit -m "[feat|core|报销][公共]db_record 加 reimbursed 列 + Migration13To14 纯加列(DB 13→14)"
```

---

### Task 2: DAO —— 待报销/选择器查询排除 reimbursed + 新增 updateRecordReimbursed

**Files:**
- Modify: `core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/dao/RecordDao.kt`（:356-368 / :371-387 / :418-432 + 新增写方法）
- Test: `core/database/src/androidTest/kotlin/cn/wj/android/cashbook/core/database/dao/RecordDaoTest.kt`

**Interfaces:**
- Produces: `RecordDao.updateRecordReimbursed(recordId: Long, booksId: Long, reimbursed: Int)`；`queryReimbursableUnrelated`/`getExpenditureReimburseRecordListAfterTime`/`getLastThreeMonthExpenditureReimburseRecordListByKeyword` 均排除 `reimbursed=1`。

- [ ] **Step 1: 写失败的 DAO 测试** `RecordDaoTest.kt`（在文件末尾、类内追加；沿用该文件既有 `recordDao`/`insertRecord`/`testBookId` helper 风格——实施时先 Read 文件确认 helper 名）

```kotlin
    @Test
    fun queryReimbursableUnrelated_excludes_reimbursed_marked() = runTest {
        // 可报销 + 未关联 + 未标记 → 命中
        val unmarkedId = insertRecord(reimbursable = 1, reimbursed = 0, typeCategory = EXPENDITURE)
        // 可报销 + 未关联 + 已手动标记 → 排除
        insertRecord(reimbursable = 1, reimbursed = 1, typeCategory = EXPENDITURE)

        val result = recordDao.queryReimbursableUnrelated(testBookId)

        assertThat(result.map { it.id }).containsExactly(unmarkedId)
    }

    @Test
    fun updateRecordReimbursed_sets_and_clears_flag_scoped_by_book() = runTest {
        val id = insertRecord(reimbursable = 1, reimbursed = 0, typeCategory = EXPENDITURE)

        recordDao.updateRecordReimbursed(recordId = id, booksId = testBookId, reimbursed = 1)
        assertThat(recordDao.queryById(id)!!.reimbursed).isEqualTo(1)

        recordDao.updateRecordReimbursed(recordId = id, booksId = testBookId, reimbursed = 0)
        assertThat(recordDao.queryById(id)!!.reimbursed).isEqualTo(0)

        // booksId 守护：错误账本不改
        recordDao.updateRecordReimbursed(recordId = id, booksId = testBookId + 999, reimbursed = 1)
        assertThat(recordDao.queryById(id)!!.reimbursed).isEqualTo(0)
    }
```
> 注：`insertRecord(... reimbursed = ...)` 需该测试类 helper 支持新参数——若现有 helper 无 `reimbursed` 形参，本步同时给 helper 加 `reimbursed: Int = 0` 形参并写入 `RecordTable.reimbursed`。

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :core:database:compileDebugAndroidTestKotlin`
Expected: FAIL（`updateRecordReimbursed` 未定义）。

- [ ] **Step 3: 改三处查询 SQL** `RecordDao.kt`

`getExpenditureReimburseRecordListAfterTime`（:356-368）`reimbursable=$SWITCH_INT_ON` 行后加一行：
```sql
        AND reimbursed=$SWITCH_INT_OFF
```
`queryReimbursableUnrelated`（:371-387）`reimbursable = $SWITCH_INT_ON` 行后加：
```sql
        AND reimbursed = $SWITCH_INT_OFF
```
`getLastThreeMonthExpenditureReimburseRecordListByKeyword`（:418-432）`reimbursable=$SWITCH_INT_ON` 行后加：
```sql
        AND reimbursed=$SWITCH_INT_OFF
```
文件顶部 import 区加：`import cn.wj.android.cashbook.core.common.SWITCH_INT_OFF`

- [ ] **Step 4: 新增写方法** `RecordDao.kt`（`updateRecord(list)` 附近，:329 之后）

```kotlin
    @Query("UPDATE db_record SET reimbursed=:reimbursed WHERE id=:recordId AND books_id=:booksId")
    suspend fun updateRecordReimbursed(recordId: Long, booksId: Long, reimbursed: Int)
```

- [ ] **Step 5: 验证编译**

Run: `./gradlew :core:database:compileDebugAndroidTestKotlin`
Expected: PASS。
> 真机：`:core:database:connectedDebugAndroidTest --tests "*.RecordDaoTest.queryReimbursableUnrelated_excludes_reimbursed_marked" --tests "*.RecordDaoTest.updateRecordReimbursed_sets_and_clears_flag_scoped_by_book"` 受环境限制标注补跑。

- [ ] **Step 6: spotless + 提交**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
git add core/database/src/main/.../dao/RecordDao.kt core/database/src/androidTest/.../dao/RecordDaoTest.kt
git commit -m "[feat|core|报销][公共]RecordDao 待报销/选择器查询排除 reimbursed + 新增 updateRecordReimbursed"
```

---

### Task 3: core:model —— 4 数据类加 reimbursed + ModelTransfer 透传

**Files:**
- Modify: `core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/model/RecordModel.kt:48`
- Modify: `core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/model/RecordViewsModel.kt:55`
- Modify: `core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/entity/RecordViewsEntity.kt:69`
- Modify: `core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/entity/RecordEntity.kt:54`
- Modify: `core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/transfer/ModelTransfer.kt`
- Test: `core/model/src/test/kotlin/cn/wj/android/cashbook/core/model/transfer/ModelTransferReimbursedTest.kt`（新建）

**Interfaces:**
- Produces: 4 数据类末位字段 `reimbursed: Boolean = false`；`RecordViewsModel.asEntity()`/`RecordModel.asEntity()`/`RecordEntity.asModel()` 透传 `reimbursed`。
- Consumes: 无（纯 model 层）。

- [ ] **Step 1: 写失败的透传测试** `core/model/src/test/.../transfer/ModelTransferReimbursedTest.kt`（含 License Header）

```kotlin
package cn.wj.android.cashbook.core.model.transfer

import cn.wj.android.cashbook.core.model.model.RecordModel
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ModelTransferReimbursedTest {

    private fun model(reimbursed: Boolean) = RecordModel(
        id = 1L, booksId = 1L, typeId = 1L, assetId = -1L, relatedAssetId = -1L,
        amount = 0L, finalAmount = 0L, charges = 0L, concessions = 0L, remark = "",
        reimbursable = true, recordTime = 0L, reimbursed = reimbursed,
    )

    @Test
    fun recordModel_asEntity_then_asModel_preserves_reimbursed() {
        assertThat(model(true).asEntity().asModel().reimbursed).isTrue()
        assertThat(model(false).asEntity().asModel().reimbursed).isFalse()
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :core:model:test --tests "*.ModelTransferReimbursedTest"`
Expected: FAIL（`RecordModel` 无 `reimbursed` 参数 / `unresolved`）。

- [ ] **Step 3: 4 数据类各加末位字段**（均加在最后一个参数之后，含尾逗号）

`RecordModel.kt`（`recordTime` 之后；KDoc 补 `@param reimbursed 是否已手动标记已报销`）：
```kotlin
    val recordTime: Long,
    val reimbursed: Boolean = false,
```
`RecordViewsModel.kt`（`recordTime` 之后）：
```kotlin
    val recordTime: Long,
    val reimbursed: Boolean = false,
```
`RecordViewsEntity.kt`（`recordTime` 之后，`) : RecordViews {` 之前）：
```kotlin
    /** 记录时间，毫秒时间戳 */
    val recordTime: Long,
    /** 是否已手动标记已报销 */
    val reimbursed: Boolean = false,
) : RecordViews {
```
`RecordEntity.kt`（`recordTime` 之后）：
```kotlin
    /** 记录时间，毫秒时间戳 */
    val recordTime: Long,
    /** 是否已手动标记已报销 */
    val reimbursed: Boolean = false,
)
```

- [ ] **Step 4: ModelTransfer 三处映射透传** `ModelTransfer.kt`

`RecordModel.asEntity()`（:26）`recordTime` 行后加 `reimbursed = this.reimbursed,`；
`RecordEntity.asModel()`（:43）`recordTime` 行后加 `reimbursed = this.reimbursed,`；
`RecordViewsModel.asEntity()`（:77）`recordTime = this.recordTime,` 行后加 `reimbursed = this.reimbursed,`。

- [ ] **Step 5: 运行测试通过**

Run: `./gradlew :core:model:test --tests "*.ModelTransferReimbursedTest"`
Expected: PASS。

- [ ] **Step 6: spotless + 提交**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
git add core/model/src/main/.../model/RecordModel.kt core/model/src/main/.../model/RecordViewsModel.kt core/model/src/main/.../entity/RecordViewsEntity.kt core/model/src/main/.../entity/RecordEntity.kt core/model/src/main/.../transfer/ModelTransfer.kt core/model/src/test/.../transfer/ModelTransferReimbursedTest.kt
git commit -m "[feat|core|报销][公共]RecordModel/ViewsModel/ViewsEntity/Entity 加 reimbursed 透传"
```

---

### Task 4: core:data —— Repository mapper/接口/实现 + FakeRecordDao

**Files:**
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/RecordRepository.kt`（接口 + `asModel`/`asTable` :227-259）
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordRepositoryImpl.kt`
- Modify: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/testdoubles/FakeRecordDao.kt`
- Test: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordRepositoryImplTest.kt`

**Interfaces:**
- Consumes: `RecordTable.reimbursed:Int`(T1), `RecordModel.reimbursed:Boolean`(T3), `RecordDao.updateRecordReimbursed`(T2)。
- Produces: `RecordRepository.updateRecordReimbursed(recordId: Long, reimbursed: Boolean)`。

- [ ] **Step 1: 写失败的 Repository 测试** `RecordRepositoryImplTest.kt`（沿用该文件既有构造 `RecordRepositoryImpl(FakeRecordDao, ...)` 风格——实施时先 Read 确认构造与 `FakeCombineProtoDataSource` 的 currentBookId 设定）

```kotlin
    @Test
    fun updateRecordReimbursed_delegates_to_dao_scoped_by_currentBook() = runTest {
        val record = RecordTable(
            id = 1L, typeId = 1L, assetId = -1L, intoAssetId = -1L, booksId = TEST_BOOK_ID,
            amount = 0L, finalAmount = 0L, concessions = 0L, charge = 0L, remark = "",
            reimbursable = 1, recordTime = 0L,
        )
        fakeRecordDao.addRecord(record)

        repository.updateRecordReimbursed(recordId = 1L, reimbursed = true)
        assertThat(fakeRecordDao.queryById(1L)!!.reimbursed).isEqualTo(1)

        repository.updateRecordReimbursed(recordId = 1L, reimbursed = false)
        assertThat(fakeRecordDao.queryById(1L)!!.reimbursed).isEqualTo(0)
    }

    @Test
    fun getReimbursableUnrelatedRecordList_excludes_reimbursed() = runTest {
        // 依赖 FakeRecordDao.queryReimbursableUnrelated 的 reimbursed 过滤（本任务 Step 5）
        fakeRecordDao.addRecord(
            RecordTable(2L, EXPENDITURE_TYPE_ID, -1L, -1L, TEST_BOOK_ID, 0L, 0L, 0L, 0L, "", 1, 0L, reimbursed = 1),
        )
        val result = repository.getReimbursableUnrelatedRecordList()
        assertThat(result.any { it.id == 2L }).isFalse()
    }
```
> 注：`TEST_BOOK_ID`/`EXPENDITURE_TYPE_ID`/`fakeRecordDao`/`repository` 按该测试文件既有命名对齐；`FakeRecordDao` 需有对应 type 项使 `queryReimbursableUnrelated` 的 expenditure 过滤命中（沿用文件既有 type 装载 helper）。

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*.RecordRepositoryImplTest"`
Expected: FAIL（`updateRecordReimbursed` 未定义）。

- [ ] **Step 3: mapper 透传** `RecordRepository.kt`

`RecordTable.asModel()`（:227）`recordTime` 行后加：
```kotlin
        reimbursed = this.reimbursed == SWITCH_INT_ON,
```
`RecordModel.asTable()`（:244）`recordTime`/`reimbursable` 之间一致风格，在 `reimbursable = ...,` 后加：
```kotlin
        reimbursed = if (this.reimbursed) SWITCH_INT_ON else SWITCH_INT_OFF,
```
（`SWITCH_INT_ON`/`SWITCH_INT_OFF` 已在该文件 import，:20-21）

- [ ] **Step 4: 接口 + 实现新增方法**

`RecordRepository.kt` 接口（`getReimbursableUnrelatedRecordList` 附近，:147 之后）：
```kotlin
    /** 手动设置/清除「已报销」标记（按当前账本守护，写后 bump recordDataVersion） */
    suspend fun updateRecordReimbursed(recordId: Long, reimbursed: Boolean)
```
`RecordRepositoryImpl.kt`（`getReimbursableUnrelatedRecordList` 实现 :427-432 之后）：
```kotlin
    override suspend fun updateRecordReimbursed(recordId: Long, reimbursed: Boolean): Unit =
        withContext(coroutineContext) {
            val booksId = combineProtoDataSource.recordSettingsData.first().currentBookId
            recordDao.updateRecordReimbursed(
                recordId = recordId,
                booksId = booksId,
                reimbursed = if (reimbursed) SWITCH_INT_ON else SWITCH_INT_OFF,
            )
            recordDataVersion.updateVersion()
        }
```
`RecordRepositoryImpl.kt` 顶部 import 区加：`import cn.wj.android.cashbook.core.common.SWITCH_INT_ON` 与 `import cn.wj.android.cashbook.core.common.SWITCH_INT_OFF`。

- [ ] **Step 5: FakeRecordDao 同步** `FakeRecordDao.kt`

`queryReimbursableUnrelated`（:314-327）过滤体加 `record.reimbursed == SWITCH_INT_OFF &&`；
`getExpenditureReimburseRecordListAfterTime`（:303-312）与 `getLastThreeMonthExpenditureReimburseRecordListByKeyword`（:350-361）各加 `it.reimbursed == SWITCH_INT_OFF &&`（忠实复刻 SQL 排除）；
类内新增实现：
```kotlin
    override suspend fun updateRecordReimbursed(recordId: Long, booksId: Long, reimbursed: Int) {
        val index = records.indexOfFirst { it.id == recordId && it.booksId == booksId }
        if (index >= 0) {
            records[index] = records[index].copy(reimbursed = reimbursed)
        }
    }
```
（`SWITCH_INT_OFF` 已在 FakeRecordDao import，:21 仅 `SWITCH_INT_ON`——补 `import cn.wj.android.cashbook.core.common.SWITCH_INT_OFF`）

- [ ] **Step 6: 运行测试通过**

Run: `./gradlew :core:data:testDebugUnitTest`
Expected: PASS。

- [ ] **Step 7: spotless + 提交**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
git add core/data/src/main/.../repository/RecordRepository.kt core/data/src/main/.../repository/impl/RecordRepositoryImpl.kt core/data/src/test/.../testdoubles/FakeRecordDao.kt core/data/src/test/.../repository/impl/RecordRepositoryImplTest.kt
git commit -m "[feat|core|报销][公共]RecordRepository 透传 reimbursed + updateRecordReimbursed(currentBook 守护+bump)"
```

---

### Task 5: core:testing —— FakeRecordRepository + TestDataFactory

**Files:**
- Modify: `core/testing/src/main/kotlin/cn/wj/android/cashbook/core/testing/repository/FakeRecordRepository.kt`
- Modify: `core/testing/src/main/kotlin/cn/wj/android/cashbook/core/testing/data/TestDataFactory.kt`

**Interfaces:**
- Consumes: `RecordRepository.updateRecordReimbursed`(T4)。
- Produces: `FakeRecordRepository.updateRecordReimbursed` 忠实实现 + `lastReimbursedRecordId`/`lastReimbursedValue` 断言钩子；`createRecordModel`/`createRecordViewsModel` 加 `reimbursed` 参。

- [ ] **Step 1: FakeRecordRepository 新增实现 + 排除过滤**

`getReimbursableUnrelatedRecordList`（:309-316）过滤体加 `!record.reimbursed &&`；
类内新增（断言钩子 + 内存改写）：
```kotlin
    /** 最近一次 updateRecordReimbursed 入参，供测试断言 */
    var lastReimbursedRecordId: Long = -1L
        private set
    var lastReimbursedValue: Boolean? = null
        private set

    override suspend fun updateRecordReimbursed(recordId: Long, reimbursed: Boolean) {
        lastReimbursedRecordId = recordId
        lastReimbursedValue = reimbursed
        val index = records.indexOfFirst { it.id == recordId }
        if (index >= 0) {
            records[index] = records[index].copy(reimbursed = reimbursed)
        }
    }
```

- [ ] **Step 2: TestDataFactory 加参** `TestDataFactory.kt`

`createRecordModel`（:34）形参末加 `reimbursed: Boolean = false,`（在 `recordTime` 形参之后），构造体 `recordTime = recordTime,` 后加 `reimbursed = reimbursed,`；
`createRecordViewsModel`（:156）同样末加形参 `reimbursed: Boolean = false,`，构造体加 `reimbursed = reimbursed,`。

- [ ] **Step 3: 验证编译（core:testing + 一个下游）**

Run: `./gradlew :core:testing:compileKotlin :core:domain:testDebugUnitTest`
Expected: PASS（core:testing 实现完整，下游 domain 测试可编译运行）。

- [ ] **Step 4: 提交**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
git add core/testing/src/main/.../repository/FakeRecordRepository.kt core/testing/src/main/.../data/TestDataFactory.kt
git commit -m "[feat|core|报销][公共]FakeRecordRepository 实现 updateRecordReimbursed + 工厂加 reimbursed 参"
```

---

### Task 6: core:domain —— 转换透传 + UpdateRecordReimbursedUseCase

**Files:**
- Modify: `core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/RecordModelTransToViewsUseCase.kt`（:69 单条 + :162 批量）
- Create: `core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/UpdateRecordReimbursedUseCase.kt`
- Test: `core/domain/src/test/kotlin/cn/wj/android/cashbook/domain/usecase/UpdateRecordReimbursedUseCaseTest.kt`（新建）
- Test: `core/domain/src/test/kotlin/cn/wj/android/cashbook/domain/usecase/RecordModelTransToViewsUseCaseTest.kt`（补透传断言）

**Interfaces:**
- Consumes: `FakeRecordRepository.updateRecordReimbursed`(T5), `RecordViewsModel.reimbursed`(T3)。
- Produces: `UpdateRecordReimbursedUseCase.invoke(recordId: Long, reimbursed: Boolean)`。

- [ ] **Step 1: 写失败的 UseCase 测试** `UpdateRecordReimbursedUseCaseTest.kt`（含 License Header）

```kotlin
package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.testing.repository.FakeRecordRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class UpdateRecordReimbursedUseCaseTest {

    private val repository = FakeRecordRepository()
    private val useCase = UpdateRecordReimbursedUseCase(repository, kotlinx.coroutines.Dispatchers.Unconfined)

    @Test
    fun invoke_delegates_to_repository() = runTest {
        useCase(recordId = 7L, reimbursed = true)
        assertThat(repository.lastReimbursedRecordId).isEqualTo(7L)
        assertThat(repository.lastReimbursedValue).isTrue()
    }
}
```

- [ ] **Step 2: 写失败的透传断言** `RecordModelTransToViewsUseCaseTest.kt`（在既有测试类追加；沿用文件既有 fake 装配）

```kotlin
    @Test
    fun transBatch_preserves_reimbursed() = runTest {
        val record = createRecordModel(id = 1L, typeId = EXPENDITURE_TYPE_ID, reimbursable = true, reimbursed = true)
        // 沿用文件既有 type 装配，使 typeId 解析为 EXPENDITURE
        val result = useCase(listOf(record))
        assertThat(result.single().reimbursed).isTrue()
    }
```
> `EXPENDITURE_TYPE_ID`/`useCase`/type 装配按该测试文件既有命名对齐。

- [ ] **Step 3: 运行确认失败**

Run: `./gradlew :core:domain:testDebugUnitTest --tests "*.UpdateRecordReimbursedUseCaseTest" --tests "*.RecordModelTransToViewsUseCaseTest"`
Expected: FAIL。

- [ ] **Step 4: 转换透传** `RecordModelTransToViewsUseCase.kt`

单条 `invoke`（:69-87）`RecordViewsModel(...)` 内 `recordTime = recordModel.recordTime,` 后加 `reimbursed = recordModel.reimbursed,`；
批量 `transBatch`（:162-180）`RecordViewsModel(...)` 内同样加 `reimbursed = recordModel.reimbursed,`。

- [ ] **Step 5: 新建 UseCase** `UpdateRecordReimbursedUseCase.kt`（含 License Header）

```kotlin
package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.data.repository.RecordRepository
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * 手动设置/清除记录「已报销」标记用例
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2026/6/24
 */
class UpdateRecordReimbursedUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
    @Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext,
) {
    suspend operator fun invoke(recordId: Long, reimbursed: Boolean) =
        withContext(coroutineContext) {
            recordRepository.updateRecordReimbursed(recordId, reimbursed)
        }
}
```

- [ ] **Step 6: 运行测试通过**

Run: `./gradlew :core:domain:testDebugUnitTest`
Expected: PASS。

- [ ] **Step 7: 提交**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
git add core/domain/src/main/.../usecase/RecordModelTransToViewsUseCase.kt core/domain/src/main/.../usecase/UpdateRecordReimbursedUseCase.kt core/domain/src/test/.../usecase/UpdateRecordReimbursedUseCaseTest.kt core/domain/src/test/.../usecase/RecordModelTransToViewsUseCaseTest.kt
git commit -m "[feat|core|报销][公共]ViewsUseCase 透传 reimbursed + 新增 UpdateRecordReimbursedUseCase"
```

---

### Task 7: core:ui —— 字符串资源

**Files:**
- Modify: `core/ui/src/main/res/values/strings_records.xml`

**Interfaces:**
- Produces: `R.string.mark_as_reimbursed` / `revert_to_pending_reimbursement` / `mark_reimbursed_confirm_hint` / `revert_reimbursed_confirm_hint`。

- [ ] **Step 1: 新增字符串**（在 `pending_reimbursement` 附近追加）

```xml
    <string name="mark_as_reimbursed">标记已报销</string>
    <string name="revert_to_pending_reimbursement">改回待报销</string>
    <string name="mark_reimbursed_confirm_hint">确认将该记录标记为已报销？标记后将移出待报销列表，金额统计不变。</string>
    <string name="revert_reimbursed_confirm_hint">确认改回待报销？</string>
```

- [ ] **Step 2: 验证编译**

Run: `./gradlew :core:ui:compileDebugKotlin`
Expected: PASS（资源可解析）。

- [ ] **Step 3: 提交**

```bash
git add core/ui/src/main/res/values/strings_records.xml
git commit -m "[feat|core|报销][公共]新增标记已报销/改回待报销相关字符串"
```

---

### Task 8: feature:records —— 报销状态共享判定 fn + 两处显示接入

**Files:**
- Create: `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/view/ReimbursementDisplayStatus.kt`
- Modify: `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/screen/LauncherContentScreen.kt:709-727`
- Modify: `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/view/RecordDetailsSheet.kt:193-200`
- Test: `feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/view/ReimbursementDisplayStatusTest.kt`（新建）

**Interfaces:**
- Consumes: `RecordViewsEntity.reimbursed`(T3)。
- Produces: `internal enum class ReimbursementDisplayStatus { MARKED_REIMBURSED, PENDING, NONE }` + `internal fun RecordViewsEntity.reimbursementDisplayStatus()`。

- [ ] **Step 1: 写失败的判定测试** `ReimbursementDisplayStatusTest.kt`（含 License Header；用 `RecordDetailsSheetData` 或直接构造 `RecordViewsEntity`）

```kotlin
package cn.wj.android.cashbook.feature.records.view

import cn.wj.android.cashbook.core.model.enums.RecordRelatedNatureEnum
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.RecordModel
import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ReimbursementDisplayStatusTest {

    private fun entity(
        category: RecordTypeCategoryEnum = RecordTypeCategoryEnum.EXPENDITURE,
        reimbursable: Boolean = true,
        reimbursed: Boolean = false,
        related: List<RecordModel> = emptyList(),
    ) = RecordViewsEntity(
        id = 1L, typeId = 1L, typeCategory = category, typeName = "t", typeIconResName = "i",
        assetId = null, assetName = null, assetIconResId = null,
        relatedAssetId = null, relatedAssetName = null, relatedAssetIconResId = null,
        amount = 0L, finalAmount = 0L, charges = 0L, concessions = 0L, remark = "",
        reimbursable = reimbursable, relatedTags = emptyList(), relatedImage = emptyList(),
        relatedRecord = related, relatedAmount = 0L,
        relatedNature = RecordRelatedNatureEnum.NONE, recordTime = 0L, reimbursed = reimbursed,
    )

    @Test fun pending_when_reimbursable_unrelated_unmarked() {
        assertThat(entity().reimbursementDisplayStatus()).isEqualTo(ReimbursementDisplayStatus.PENDING)
    }

    @Test fun marked_when_reimbursed_flag_set() {
        assertThat(entity(reimbursed = true).reimbursementDisplayStatus())
            .isEqualTo(ReimbursementDisplayStatus.MARKED_REIMBURSED)
    }

    @Test fun none_when_not_expenditure() {
        assertThat(entity(category = RecordTypeCategoryEnum.INCOME).reimbursementDisplayStatus())
            .isEqualTo(ReimbursementDisplayStatus.NONE)
    }

    @Test fun none_when_related() {
        val related = listOf(
            RecordModel(2L, 1L, 1L, 1L, 1L, 0L, 0L, 0L, 0L, "", false, 0L),
        )
        assertThat(entity(reimbursed = true, related = related).reimbursementDisplayStatus())
            .isEqualTo(ReimbursementDisplayStatus.NONE)
    }

    @Test fun none_when_not_reimbursable() {
        assertThat(entity(reimbursable = false).reimbursementDisplayStatus())
            .isEqualTo(ReimbursementDisplayStatus.NONE)
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :feature:records:testDebugUnitTest --tests "*.ReimbursementDisplayStatusTest"`
Expected: FAIL（`reimbursementDisplayStatus` 未定义）。

- [ ] **Step 3: 新建判定文件** `ReimbursementDisplayStatus.kt`（含 License Header）

```kotlin
package cn.wj.android.cashbook.feature.records.view

import cn.wj.android.cashbook.core.model.entity.RecordViewsEntity
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum

/**
 * 支出记录「未关联」时的报销显示态（待报销列表 / 详情弹窗 / 主列表共用，防两处逻辑漂移）。
 *
 * 已关联（relatedRecord 非空）的记录走各处既有 relatedNature 标签，此处返回 NONE。
 */
internal enum class ReimbursementDisplayStatus { MARKED_REIMBURSED, PENDING, NONE }

internal fun RecordViewsEntity.reimbursementDisplayStatus(): ReimbursementDisplayStatus = when {
    typeCategory != RecordTypeCategoryEnum.EXPENDITURE -> ReimbursementDisplayStatus.NONE
    relatedRecord.isNotEmpty() -> ReimbursementDisplayStatus.NONE
    !reimbursable -> ReimbursementDisplayStatus.NONE
    reimbursed -> ReimbursementDisplayStatus.MARKED_REIMBURSED
    else -> ReimbursementDisplayStatus.PENDING
}
```

- [ ] **Step 4: 接入主列表** `LauncherContentScreen.kt:722-728`（`else if (... EXPENDITURE && reimbursable)` 分支替换为 when）

将原：
```kotlin
                    } else if (item.typeCategory == RecordTypeCategoryEnum.EXPENDITURE && item.reimbursable) {
                        Text(
                            text = stringResource(id = R.string.pending_reimbursement),
                            color = LocalContentColor.current.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
```
替换为：
```kotlin
                    } else {
                        val statusRes = when (item.reimbursementDisplayStatus()) {
                            ReimbursementDisplayStatus.MARKED_REIMBURSED -> R.string.reimbursed
                            ReimbursementDisplayStatus.PENDING -> R.string.pending_reimbursement
                            ReimbursementDisplayStatus.NONE -> null
                        }
                        statusRes?.let { res ->
                            Text(
                                text = stringResource(id = res),
                                color = LocalContentColor.current.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                        }
                    }
```
文件顶部 import 加 `import cn.wj.android.cashbook.feature.records.view.ReimbursementDisplayStatus` 与 `import cn.wj.android.cashbook.feature.records.view.reimbursementDisplayStatus`。

- [ ] **Step 5: 接入详情弹窗** `RecordDetailsSheet.kt:193-200`（`RecordRelatedNatureEnum.NONE ->` 分支替换）

将原：
```kotlin
                                        RecordRelatedNatureEnum.NONE ->
                                            if (recordData.reimbursable) R.string.pending_reimbursement else null
```
替换为：
```kotlin
                                        RecordRelatedNatureEnum.NONE ->
                                            when (recordData.reimbursementDisplayStatus()) {
                                                ReimbursementDisplayStatus.MARKED_REIMBURSED -> R.string.reimbursed
                                                ReimbursementDisplayStatus.PENDING -> R.string.pending_reimbursement
                                                ReimbursementDisplayStatus.NONE -> null
                                            }
```
（同包 `feature.records.view`，无需 import）

- [ ] **Step 6: 运行测试通过**

Run: `./gradlew :feature:records:testDebugUnitTest --tests "*.ReimbursementDisplayStatusTest"`
Expected: PASS。

- [ ] **Step 7: spotless + 提交**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
git add feature/records/src/main/.../view/ReimbursementDisplayStatus.kt feature/records/src/main/.../screen/LauncherContentScreen.kt feature/records/src/main/.../view/RecordDetailsSheet.kt feature/records/src/test/.../view/ReimbursementDisplayStatusTest.kt
git commit -m "[feat|feature|报销][公共]报销显示态共享判定 fn + 主列表/详情弹窗已报销标签"
```

---

### Task 9: feature:records —— 详情弹窗按钮 + 二次确认 + VM + 接线

**Files:**
- Modify: `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/enums/RecordDetailsDialogEnum.kt`
- Create: `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/RecordDetailsSheetViewModel.kt`
- Modify: `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/view/RecordDetailsSheet.kt`
- Modify: `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/navigation/RecordNavigation.kt:302-314`
- Modify: `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/preview/RecordDetailsSheetPreviewParameterProvider.kt`（仅当预览函数直接调用 `RecordDetailsSheet` 才需补参——实际调用在 `RecordDetailsSheet.kt:477` 预览函数内）
- Test: `feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/RecordDetailsSheetViewModelTest.kt`（新建）

**Interfaces:**
- Consumes: `UpdateRecordReimbursedUseCase`(T6), `reimbursementDisplayStatus()`(T8), 字符串(T7)。
- Produces: `RecordDetailsSheetViewModel.markReimbursed(recordId, reimbursed: Boolean)`；`RecordDetailsSheet` 新增 `onMarkReimbursed`/`onRevertReimbursed` 两参；`RecordDetailSheetContent` 对外签名不变。

- [ ] **Step 1: 写失败的 VM 测试** `RecordDetailsSheetViewModelTest.kt`（含 License Header；用 `FakeRecordRepository` 构造真实 `UpdateRecordReimbursedUseCase`）

```kotlin
package cn.wj.android.cashbook.feature.records.viewmodel

import cn.wj.android.cashbook.core.testing.repository.FakeRecordRepository
import cn.wj.android.cashbook.domain.usecase.UpdateRecordReimbursedUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RecordDetailsSheetViewModelTest {

    private val repository = FakeRecordRepository()
    private val viewModel = RecordDetailsSheetViewModel(
        UpdateRecordReimbursedUseCase(repository, Dispatchers.Unconfined),
    )

    @Test
    fun markReimbursed_true_delegates() = runTest {
        viewModel.markReimbursed(recordId = 5L, reimbursed = true)
        assertThat(repository.lastReimbursedRecordId).isEqualTo(5L)
        assertThat(repository.lastReimbursedValue).isTrue()
    }

    @Test
    fun markReimbursed_false_delegates() = runTest {
        viewModel.markReimbursed(recordId = 9L, reimbursed = false)
        assertThat(repository.lastReimbursedValue).isFalse()
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :feature:records:testDebugUnitTest --tests "*.RecordDetailsSheetViewModelTest"`
Expected: FAIL（`RecordDetailsSheetViewModel` 未定义）。

- [ ] **Step 3: 新建 VM** `RecordDetailsSheetViewModel.kt`（含 License Header）

```kotlin
package cn.wj.android.cashbook.feature.records.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.wj.android.cashbook.domain.usecase.UpdateRecordReimbursedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 记录详情弹窗 ViewModel：承载「标记已报销 / 改回待报销」写动作。
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2026/6/24
 */
@HiltViewModel
class RecordDetailsSheetViewModel @Inject constructor(
    private val updateRecordReimbursedUseCase: UpdateRecordReimbursedUseCase,
) : ViewModel() {

    fun markReimbursed(recordId: Long, reimbursed: Boolean) {
        viewModelScope.launch {
            updateRecordReimbursedUseCase(recordId, reimbursed)
        }
    }
}
```

- [ ] **Step 4: 枚举加两态** `RecordDetailsDialogEnum.kt`

```kotlin
    /** 图片预览 */
    IMAGE_PREVIEW,

    /** 标记已报销确认 */
    MARK_REIMBURSED_CONFIRM,

    /** 改回待报销确认 */
    REVERT_REIMBURSED_CONFIRM,
}
```

- [ ] **Step 5: RecordDetailsSheet 加参 + 按钮 + 确认弹窗** `RecordDetailsSheet.kt`

(a) 函数签名（:81）加两参：
```kotlin
internal fun RecordDetailsSheet(
    recordData: RecordViewsEntity?,
    onRequestNaviToEditRecord: (Long) -> Unit,
    onRequestNaviToAssetInfo: (Long) -> Unit,
    onMarkReimbursed: (Long) -> Unit,
    onRevertReimbursed: (Long) -> Unit,
    onRequestDismissSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
```
(b) `dialogState` 的 `when(this)`（:94）追加两分支（在 IMAGE_PREVIEW 之后）：
```kotlin
                RecordDetailsDialogEnum.MARK_REIMBURSED_CONFIRM -> {
                    CbAlertDialog(
                        onDismissRequest = { dialogState = DialogState.Dismiss },
                        text = { Text(text = stringResource(id = R.string.mark_reimbursed_confirm_hint)) },
                        confirmButton = {
                            CbTextButton(onClick = {
                                onMarkReimbursed(recordData.id)
                                dialogState = DialogState.Dismiss
                            }) { Text(text = stringResource(id = R.string.confirm)) }
                        },
                        dismissButton = {
                            CbTextButton(onClick = { dialogState = DialogState.Dismiss }) {
                                Text(text = stringResource(id = R.string.cancel))
                            }
                        },
                    )
                }

                RecordDetailsDialogEnum.REVERT_REIMBURSED_CONFIRM -> {
                    CbAlertDialog(
                        onDismissRequest = { dialogState = DialogState.Dismiss },
                        text = { Text(text = stringResource(id = R.string.revert_reimbursed_confirm_hint)) },
                        confirmButton = {
                            CbTextButton(onClick = {
                                onRevertReimbursed(recordData.id)
                                dialogState = DialogState.Dismiss
                            }) { Text(text = stringResource(id = R.string.confirm)) }
                        },
                        dismissButton = {
                            CbTextButton(onClick = { dialogState = DialogState.Dismiss }) {
                                Text(text = stringResource(id = R.string.cancel))
                            }
                        },
                    )
                }
```
(c) 标题行（:146 `if (!recordData.isBalanceRecord)` 编辑按钮之后、删除按钮之前）插入按资格的按钮：
```kotlin
                        when (recordData.reimbursementDisplayStatus()) {
                            ReimbursementDisplayStatus.PENDING -> {
                                CbTextButton(onClick = {
                                    dialogState = DialogState.Shown(RecordDetailsDialogEnum.MARK_REIMBURSED_CONFIRM)
                                }) {
                                    Text(
                                        text = stringResource(id = R.string.mark_as_reimbursed),
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                            ReimbursementDisplayStatus.MARKED_REIMBURSED -> {
                                CbTextButton(onClick = {
                                    dialogState = DialogState.Shown(RecordDetailsDialogEnum.REVERT_REIMBURSED_CONFIRM)
                                }) {
                                    Text(
                                        text = stringResource(id = R.string.revert_to_pending_reimbursement),
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                            ReimbursementDisplayStatus.NONE -> Unit
                        }
```
(d) import 加：`import cn.wj.android.cashbook.core.design.component.CbAlertDialog`。
(e) 预览函数 `RecordDetailsSheetWithData`（:479）补两参：
```kotlin
        RecordDetailsSheet(
            recordData = recordData,
            onRequestNaviToEditRecord = {},
            onRequestNaviToAssetInfo = {},
            onMarkReimbursed = {},
            onRevertReimbursed = {},
            onRequestDismissSheet = {},
        )
```

- [ ] **Step 6: RecordDetailSheetContent 接线** `RecordNavigation.kt:302-314`（对外签名不变，加内部 VM 默认参 + 传两回调）

```kotlin
@Composable
fun RecordDetailSheetContent(
    recordEntity: RecordViewsEntity?,
    onRequestNaviToEditRecord: (Long) -> Unit,
    onRequestNaviToAssetInfo: (Long) -> Unit,
    onRequestDismissSheet: () -> Unit,
    viewModel: RecordDetailsSheetViewModel = hiltViewModel(),
) {
    RecordDetailsSheet(
        recordData = recordEntity,
        onRequestNaviToEditRecord = onRequestNaviToEditRecord,
        onRequestNaviToAssetInfo = onRequestNaviToAssetInfo,
        onMarkReimbursed = { id ->
            viewModel.markReimbursed(id, reimbursed = true)
            onRequestDismissSheet()
        },
        onRevertReimbursed = { id ->
            viewModel.markReimbursed(id, reimbursed = false)
            onRequestDismissSheet()
        },
        onRequestDismissSheet = onRequestDismissSheet,
    )
}
```
import 加：`import androidx.hilt.navigation.compose.hiltViewModel` 与 `import cn.wj.android.cashbook.feature.records.viewmodel.RecordDetailsSheetViewModel`（若未引入）。

- [ ] **Step 7: 运行模块测试**

Run: `./gradlew :feature:records:testDebugUnitTest --tests "*.RecordDetailsSheetViewModelTest"`
Expected: PASS。

- [ ] **Step 8: 全模块编译 + 截图基准重录**

Run: `./gradlew :feature:records:testDebugUnitTest`
Expected: PASS（若 `RecordDetailsSheet` 截图因新按钮变化失败 → `./gradlew recordRoborazziOnlineDebug -Pcom.cashbook.records` 重录后再跑校验；实施时按既有截图测试命令）。

- [ ] **Step 9: spotless + 提交**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
git add feature/records/src/main/.../enums/RecordDetailsDialogEnum.kt feature/records/src/main/.../viewmodel/RecordDetailsSheetViewModel.kt feature/records/src/main/.../view/RecordDetailsSheet.kt feature/records/src/main/.../navigation/RecordNavigation.kt feature/records/src/test/.../viewmodel/RecordDetailsSheetViewModelTest.kt feature/records/src/test/.../**/*.png
git commit -m "[feat|feature|报销][公共]详情弹窗标记已报销/改回待报销按钮+二次确认+VM 接线"
```

---

### Task 10: feature:records —— 编辑保存保留 reimbursed

**Files:**
- Modify: `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/EditRecordViewModel.kt:469-472`
- Test: `feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/EditRecordViewModelTest.kt`

**Interfaces:**
- Consumes: `RecordModel.reimbursed`(T3), `FakeRecordRepository.lastUpdatedRecord`(已有)。

- [ ] **Step 1: 写失败的保留测试** `EditRecordViewModelTest.kt`（沿用该文件既有 VM 装配与保存触发；下示为断言要点，实施时套用文件既有 setup）

```kotlin
    @Test
    fun save_preserves_reimbursed_flag_for_expenditure() = runTest {
        // 装载一条 reimbursable + reimbursed 的支出记录到编辑态（沿用文件既有装载 helper）
        loadRecordForEdit(createRecordModel(id = 1L, reimbursable = true, reimbursed = true, typeId = EXPENDITURE_TYPE_ID))
        viewModel.trySave()  // 文件既有保存入口名
        assertThat(fakeRecordRepository.lastUpdatedRecord!!.reimbursed).isTrue()
    }

    @Test
    fun save_clears_reimbursed_when_category_leaves_expenditure() = runTest {
        loadRecordForEdit(createRecordModel(id = 1L, reimbursable = true, reimbursed = true, typeId = INCOME_TYPE_ID))
        viewModel.trySave()
        assertThat(fakeRecordRepository.lastUpdatedRecord!!.reimbursed).isFalse()
    }
```
> `loadRecordForEdit`/`trySave`/`EXPENDITURE_TYPE_ID`/`INCOME_TYPE_ID`/`fakeRecordRepository` 按该测试文件既有命名对齐（实施时先 Read 文件确认保存触发链与 type 装配）。

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :feature:records:testDebugUnitTest --tests "*.EditRecordViewModelTest"`
Expected: FAIL（第二个用例：当前未清零）。

- [ ] **Step 3: 加保留守卫** `EditRecordViewModel.kt:469-472`（`recordEntity.copy(...)` 内 `reimbursable = ...` 行之后加一行）

```kotlin
                    recordModel = recordEntity.copy(
                        relatedAssetId = if (typeCategory != RecordTypeCategoryEnum.TRANSFER) -1L else recordEntity.relatedAssetId,
                        concessions = if (typeCategory == RecordTypeCategoryEnum.INCOME) 0L else recordEntity.concessions,
                        reimbursable = if (typeCategory != RecordTypeCategoryEnum.EXPENDITURE) false else recordEntity.reimbursable,
                        reimbursed = if (typeCategory != RecordTypeCategoryEnum.EXPENDITURE) false else recordEntity.reimbursed,
                    ),
```

- [ ] **Step 4: 运行测试通过**

Run: `./gradlew :feature:records:testDebugUnitTest --tests "*.EditRecordViewModelTest"`
Expected: PASS。

- [ ] **Step 5: spotless + 提交**

```bash
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache
git add feature/records/src/main/.../viewmodel/EditRecordViewModel.kt feature/records/src/test/.../viewmodel/EditRecordViewModelTest.kt
git commit -m "[feat|feature|报销][公共]编辑保存保留 reimbursed(改类别离开支出时清零)"
```

---

### Task 11: 全链路验证 + 全量测试 + lint

**Files:** 无新增代码（验证 + 必要的截图基准/编译修复）。

- [ ] **Step 1: 全量编译（含跨模块 Hilt 全图）**

Run: `./gradlew :app:compileOnlineDebugKotlin`
Expected: PASS（app 有 flavor，必须带 `Online`；验证跨模块 Hilt 注入 `RecordDetailsSheetViewModel`/`UpdateRecordReimbursedUseCase` 全图）。

- [ ] **Step 2: 受影响模块全量测试**

Run: `./gradlew :core:model:test :core:data:testDebugUnitTest :core:domain:testDebugUnitTest :feature:records:testDebugUnitTest`
Expected: 全 PASS。任一截图测试因新按钮/标签变化失败 → 重录基准后再跑校验。

- [ ] **Step 3: spotless 校验**

Run: `./gradlew spotlessCheck --init-script gradle/init.gradle.kts --no-configuration-cache`
Expected: PASS。

- [ ] **Step 4: lint（feature:records 单模块）**

Run: `./gradlew :feature:records:lintRelease`
Expected: PASS（无 `Design` error——确认全部用 `CbAlertDialog`/`CbTextButton`）。
> 首次跑 lint 缺 `lint-gradle` 缓存：去 `--offline` 并加本地代理 `-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897` 暖缓存。

- [ ] **Step 5: 端到端 journey 自检（真机/模拟器，环境允许时）**

手动验证链路：
1. 任意记录列表（主列表/日历/资产/搜索/待报销）打开一条「可报销且未关联」支出详情 → 出现「标记已报销」按钮。
2. 点「标记已报销」→ 二次确认弹窗 → 确认 → 弹窗关闭；该记录从待报销列表消失；主列表显示「已报销」标签。
3. 重开该记录详情 → 出现「改回待报销」按钮 → 确认 → 回到待报销列表、标签变「待报销」。
4. 编辑该记录（保持支出）保存 → reimbursed 保留；改为收入保存 → reimbursed 清零。
5. 新建报销款「选关联支出」选择器中**不出现**已标记记录。
6. 月度统计 / 资产 / 分类金额**无变化**（标记前后逐分一致）。

> 真机受本机代理 Maven Central TLS 限制时，记 backlog 待环境恢复补跑（参照 spec §5）。

- [ ] **Step 6: 收尾（无新代码改动则跳过提交）**

若 Step 2/4 触发截图基准或编译修复，按各自任务追加提交；否则本任务无提交。

---

## 自审记录（plan vs spec 覆盖）

- spec §3.1 两态互斥不变量 → Task 2（选择器查询排除）+ Task 9（按钮按 `reimbursementDisplayStatus` 仅 PENDING/MARKED 显示）✓
- spec §3.2 数据层 → Task 1 + Task 2 ✓
- spec §3.3 模型透传（默认参缓解）→ Task 3（4 数据类 + ModelTransfer）+ Task 4（asModel/asTable）+ Task 6（ViewsUseCase）✓
- spec §3.4 Repository/Domain → Task 4（接口/Impl/currentBook 守护/先写后 bump）+ Task 6（UseCase）✓
- spec §3.5 UI（命名 VM / 按钮 / 二次确认 / 状态共享 fn / 编辑保留）→ Task 8 + Task 9 + Task 10 ✓
- spec §3.6 字符串 → Task 7 ✓
- spec §4 节点1 finding 处置 → booksId 守护(T2/T4)、先写后 bump(T4)、默认参(T3)、命名 VM(T9)、共享 fn 防漂移(T8)、Fake 同步(T4/T5)、DB 升版+迁移测试(T1) ✓
- spec §5 测试策略 → 各 Task 内 TDD + Task 11 全量 ✓
- spec §6 影响评估 → 默认参收窄构造面(T3)、Fake 同步(T4/T5)、RecordDetailsSheet 签名仅两处(T9)、备份零改动(无需任务)、金额零触碰(全程不碰) ✓
