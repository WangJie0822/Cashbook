# 预算管理 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 Cashbook 增加按账本 + 一级支出分类的月度预算管理（独立预算屏 + 抽屉入口 + 超支视觉），消费可配置月周期。

**Architecture:** 新建 `feature:budget` 模块；新 Room 表 `db_budget`（Migration12To13，搭车 F3 清理 db_record_temp）；「已花」复用 `TransRecordViewsToAnalyticsPieUseCase` 净自付聚合 + 总体直接 Σ EXPENDITURE；级联删除挂 `TransactionDao.deleteBookTransaction` 与 `TypeRepositoryImpl.deleteById`。

**Tech Stack:** Kotlin + Jetpack Compose + Room + Hilt + Coroutines/Flow + Roborazzi 截图。

**依据 spec:** `docs/superpowers/specs/2026-06-23-budget-management-design.md`（v2，已过节点1 四维评审）。

## Global Constraints

- 金额全链路 `Long`，单位：分（守 CLAUDE.md 金额约定）；用户输入经校验转分，显示用 `Long.toMoneyCNY()`。
- 「已花」净自付口径 `analyticsPieNetAmount`（`core/model/.../RecordAmount.kt:66`），**禁止自行用 BigDecimal/Double 重实现金额计算**。
- `db_budget.type_id = -1L` 表总体预算（哨兵，非 NULL）；`(books_id, type_id)` 唯一索引。
- 禁止在 `app/feature/core:ui` 直接用 Material3 组件，必须用 core:design 的 `Cb*` 封装。
- 表重建用 `_new` 模式——但本期 db_budget 是**纯新增表**，直接 `CREATE TABLE` 不套 `_new`。
- 测试替身忠实复刻 DAO/SQL 语义，禁空桩。
- License Header：每个新 Kotlin 文件加 Apache 2.0 头（照抄现有文件头，年份 2021）。
- 模块测试源集整体编译：改 Composable/ViewModel 签名须同步对应 `*ScreenshotTests`/`*ViewModelTest`。
- DAO 逻辑测试走 androidTest（设备门控，本机 compile-verified；有设备时跑 `:core:database:connectedDebugAndroidTest`）。

**常量（新增到 `core/common` 或 `core/model`）：**
- `BUDGET_TYPE_ID_TOTAL = -1L`（总体预算 type_id 哨兵）
- `BUDGET_AMOUNT_MAX_CENT = 999999_00L`（限额上界 999 万元，防溢出/误输入）

---

### Task 1: DB 基础（BudgetTable + BudgetDao + Migration12To13 + 注册）

**Files:**
- Create: `core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/table/BudgetTable.kt`
- Create: `core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/dao/BudgetDao.kt`
- Create: `core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/migration/Migration12To13.kt`
- Modify: `core/common/src/main/kotlin/cn/wj/android/cashbook/core/common/ApplicationInfo.kt:29`（`DB_VERSION = 12` → `13`）
- Modify: `core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/CashbookDatabase.kt`（加 entity + `abstract fun budgetDao()`）
- Modify: `core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/di/DatabaseModule.kt`（加 `providesBudgetDao`）
- Modify: `core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/migration/DatabaseMigrations.kt:47`（`MIGRATION_LIST` 追加 `Migration12To13`）
- Test: `core/database/src/androidTest/kotlin/cn/wj/android/cashbook/core/database/DatabaseTest.kt`（加 `migrate12_13`）

**Interfaces:**
- Produces: `BudgetTable(id, booksId, typeId, amount)`；`BudgetDao` 方法 `queryByBooksFlow(booksId): Flow<List<BudgetTable>>` / `queryByBooks(booksId): List<BudgetTable>` / `queryByBooksAndType(booksId, typeId): BudgetTable?` / `upsert(budget: BudgetTable)` / `deleteByBooksAndType(booksId, typeId)` / `deleteByBooksId(booksId)` / `deleteByTypeId(typeId)`。

- [ ] **Step 1: 写 BudgetTable**（参照 `TypeTable.kt` 文件头 + 结构）

```kotlin
package cn.wj.android.cashbook.core.database.table

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 预算数据表
 *
 * @param id 主键自增长
 * @param booksId 所属账本 id
 * @param typeId 一级支出分类 id；-1 表示总体预算
 * @param amount 月度限额（单位：分）
 */
@Entity(
    tableName = "db_budget",
    indices = [Index(value = ["books_id", "type_id"], unique = true)],
)
data class BudgetTable(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long?,
    @ColumnInfo(name = "books_id") val booksId: Long,
    @ColumnInfo(name = "type_id") val typeId: Long,
    @ColumnInfo(name = "amount") val amount: Long,
)
```

- [ ] **Step 2: 写 BudgetDao**（参照 `BooksDao.kt`；`upsert` 用 `@Upsert` 依赖唯一索引原子处理）

```kotlin
package cn.wj.android.cashbook.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import cn.wj.android.cashbook.core.database.table.BudgetTable
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query("SELECT * FROM db_budget WHERE books_id = :booksId")
    fun queryByBooksFlow(booksId: Long): Flow<List<BudgetTable>>

    @Query("SELECT * FROM db_budget WHERE books_id = :booksId")
    suspend fun queryByBooks(booksId: Long): List<BudgetTable>

    @Query("SELECT * FROM db_budget WHERE books_id = :booksId AND type_id = :typeId")
    suspend fun queryByBooksAndType(booksId: Long, typeId: Long): BudgetTable?

    @Upsert
    suspend fun upsert(budget: BudgetTable)

    @Query("DELETE FROM db_budget WHERE books_id = :booksId AND type_id = :typeId")
    suspend fun deleteByBooksAndType(booksId: Long, typeId: Long)

    @Query("DELETE FROM db_budget WHERE books_id = :booksId")
    suspend fun deleteByBooksId(booksId: Long)

    @Query("DELETE FROM db_budget WHERE type_id = :typeId")
    suspend fun deleteByTypeId(typeId: Long)
}
```

- [ ] **Step 3: 写 Migration12To13**（纯新增表 + F3 搭车；参照 `Migration11To12` 的 `@Language("SQL")` + 扩展函数组织）

```kotlin
package cn.wj.android.cashbook.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cn.wj.android.cashbook.core.common.ext.logger
import org.intellij.lang.annotations.Language

/**
 * 数据库升级 12 -> 13
 * - 新增 db_budget 预算表 + (books_id, type_id) 唯一索引
 * - F3 搭车：清理历史 Migration6To7 遗漏的 db_record_temp 临时表
 *
 * > 创建于 2026/6/23
 */
object Migration12To13 : Migration(12, 13) {

    @Language("SQL")
    private const val SQL_CREATE_BUDGET = """
        CREATE TABLE IF NOT EXISTS `db_budget` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT,
            `books_id` INTEGER NOT NULL,
            `type_id` INTEGER NOT NULL,
            `amount` INTEGER NOT NULL
        )
    """

    @Language("SQL")
    private const val SQL_INDEX_BUDGET = """
        CREATE UNIQUE INDEX IF NOT EXISTS `index_db_budget_books_id_type_id`
        ON `db_budget`(`books_id`, `type_id`)
    """

    @Language("SQL")
    private const val SQL_DROP_RECORD_TEMP = "DROP TABLE IF EXISTS `db_record_temp`"

    override fun migrate(db: SupportSQLiteDatabase) {
        logger().i("migrate(db)")
        db.execSQL(SQL_CREATE_BUDGET)
        db.execSQL(SQL_INDEX_BUDGET)
        db.execSQL(SQL_DROP_RECORD_TEMP)
    }
}
```

> ⚠️ schema 校验：Room 生成的 `CREATE TABLE`/索引 SQL 须与 `@Entity` 完全一致。若 `runMigrationsAndValidate` 报 schema 不匹配（列顺序/索引名），以 Room 生成的 `13.json`（见 Step 7）为准对齐本 migration 的 DDL（索引名 Room 默认 `index_db_budget_books_id_type_id`）。

- [ ] **Step 4: 改 ApplicationInfo / CashbookDatabase / DatabaseModule / DatabaseMigrations**

`ApplicationInfo.kt:29`: `const val DB_VERSION = 13`
`CashbookDatabase.kt`: entities 数组加 `BudgetTable::class,` + import；类体加 `abstract fun budgetDao(): BudgetDao` + import。
`DatabaseModule.kt`: 加
```kotlin
    @Provides
    @Singleton
    fun providesBudgetDao(
        database: CashbookDatabase,
    ): BudgetDao = database.budgetDao()
```
（+ `import cn.wj.android.cashbook.core.database.dao.BudgetDao`）
`DatabaseMigrations.kt:47`: `MIGRATION_LIST` 在 `Migration11To12,` 后加 `Migration12To13,`。

- [ ] **Step 5: 写 androidTest `migrate12_13`**（参照 `DatabaseTest.kt` 现有 `migrateX_Y`）

```kotlin
    @Test
    @Throws(IOException::class)
    fun migrate12_13() {
        helper.createDatabase(TEST_DB, 12).use { db ->
            // 模拟历史泄漏的 db_record_temp 残留
            db.execSQL("CREATE TABLE IF NOT EXISTS `db_record_temp` (`id` INTEGER PRIMARY KEY)")
        }
        helper.runMigrationsAndValidate(TEST_DB, 13, true, Migration12To13).use { db ->
            // db_budget 建成 + db_record_temp 清除 由 validateDroppedTables=true + schema 校验保证
            db.query("SELECT count(*) FROM db_budget").use { c ->
                c.moveToFirst()
                assertEquals(0, c.getInt(0))
            }
        }
    }
```
（确认 `TEST_DB` 常量名与现有测试一致；`Migration12To13` import）

- [ ] **Step 6: 编译验证**

Run: `./gradlew :core:database:compileDebugKotlin --offline --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: 生成/更新 Room schema json**

Room 编译期按 `room.schemaLocation` 生成 `core/database/schemas/.../13.json`。确认 `:core:database:compileDebugKotlin`（或 kspDebugKotlin）后 `core/database/schemas/cn.wj.android.cashbook.core.database.CashbookDatabase/13.json` 存在且含 db_budget。

- [ ] **Step 8: Commit**

```bash
git add core/database core/common/src/main/kotlin/cn/wj/android/cashbook/core/common/ApplicationInfo.kt
git commit -m "[feat|feature|预算][公共]新增 db_budget 表 + BudgetDao + Migration12To13(搭车F3清理db_record_temp)"
```

---

### Task 2: 级联删除（删账本 / 删一级分类 → 清预算）

**Files:**
- Modify: `core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/dao/TransactionDao.kt`（`deleteBookTransaction` 内增删 budget）
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/impl/TypeRepositoryImpl.kt`（`deleteById` 内增删该 type 的预算 + 注入 `budgetDao`）
- Test: `core/database/src/androidTest/kotlin/cn/wj/android/cashbook/core/database/dao/TransactionDaoTest.kt`（级联用例）

**Interfaces:**
- Consumes: `BudgetDao.deleteByBooksId` / `deleteByTypeId`（Task 1）。

- [ ] **Step 1: 读 `TransactionDao.deleteBookTransaction` 现有实现**

Run: 查看 `TransactionDao.kt` 中 `deleteBookTransaction` 的 `@Transaction` 体（含哪些表删除）。

- [ ] **Step 2: 在 `deleteBookTransaction` 事务内增删 budget**

在该 `@Transaction suspend fun deleteBookTransaction(booksId: Long)` 体末尾增：
```kotlin
        // 删除该账本下所有预算
        budgetDao().deleteByBooksId(booksId)
```
> 若 `TransactionDao` 是 abstract class 持有其他 dao 句柄则照其模式；若是 interface + `@Transaction` default，则在同事务内调 `@Query DELETE FROM db_budget WHERE books_id=:booksId`（直接在 TransactionDao 加该 `@Query` 方法并在事务内调用）。**执行时读现有 deleteBookTransaction 结构决定具体写法**，保持与现有级联（记录/资产/关联）同模式。

- [ ] **Step 3: `TypeRepositoryImpl.deleteById` 加级联**

`TypeRepositoryImpl` 构造加 `private val budgetDao: BudgetDao`（import `core.database.dao.BudgetDao`）。`deleteById(id)` 体内（删 type 后）增：
```kotlin
        budgetDao.deleteByTypeId(id)
```

- [ ] **Step 4: 写级联 androidTest**（参照 `TransactionDaoTest` 现有 deleteBookTransaction 测试）

```kotlin
    @Test
    fun when_deleteBookTransaction_then_budgetRemoved() = runTest {
        val bookId = 1L
        budgetDao.upsert(BudgetTable(id = null, booksId = bookId, typeId = -1L, amount = 50000))
        budgetDao.upsert(BudgetTable(id = null, booksId = bookId, typeId = 10L, amount = 10000))
        budgetDao.upsert(BudgetTable(id = null, booksId = 2L, typeId = -1L, amount = 30000))
        transactionDao.deleteBookTransaction(bookId)
        assertEquals(0, budgetDao.queryByBooks(bookId).size)
        assertEquals(1, budgetDao.queryByBooks(2L).size) // 其他账本不受影响
    }
```
（在 `TransactionDaoTest` 的 `@Before` 取 `budgetDao = db.budgetDao()`）

- [ ] **Step 5: 编译验证**

Run: `./gradlew :core:database:compileDebugKotlin :core:data:compileDebugKotlin --offline --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add core/database core/data
git commit -m "[feat|feature|预算][公共]删账本/删一级分类级联清理预算"
```

---

### Task 3: core:model 预算领域模型 + 组装纯函数 + 金额校验

**Files:**
- Create: `core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/model/BudgetModel.kt`
- Create: `core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/entity/BudgetProgressEntity.kt`（含 `BudgetItem` + `BudgetStateEnum` + 组装 internal fun）
- Create: `core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/model/BudgetAmount.kt`（常量 + 校验纯函数）
- Test: `core/model/src/test/kotlin/cn/wj/android/cashbook/core/model/BudgetProgressTest.kt`
- Test: `core/model/src/test/kotlin/cn/wj/android/cashbook/core/model/BudgetAmountTest.kt`

**Interfaces:**
- Produces: `BudgetModel(id, booksId, typeId, amount)`；`BudgetStateEnum{NORMAL,NEAR,OVER}`；`BudgetItem(typeId, typeName, typeIconName, limit, spent, progress, overAmount, state)`；`BudgetProgressEntity(overall, categoryList)`；`internal fun buildBudgetItem(typeId, typeName, typeIconName, limit, spent): BudgetItem`；`const val BUDGET_TYPE_ID_TOTAL = -1L`；`const val BUDGET_AMOUNT_MAX_CENT = 999999_00L`；`fun parseBudgetAmountCent(input: String): Long?`。

- [ ] **Step 1: 写 BudgetModel + BudgetProgressEntity + BudgetStateEnum + buildBudgetItem**

`BudgetModel.kt`:
```kotlin
package cn.wj.android.cashbook.core.model.model

/** 预算领域模型（对应 db_budget） */
data class BudgetModel(
    val id: Long?,
    val booksId: Long,
    val typeId: Long,
    val amount: Long,
)
```

`BudgetProgressEntity.kt`:
```kotlin
package cn.wj.android.cashbook.core.model.entity

/** 总体预算 type_id 哨兵 */
const val BUDGET_TYPE_ID_TOTAL = -1L

/** 预算状态：正常 / 接近(80~100%) / 超支(>100%) */
enum class BudgetStateEnum { NORMAL, NEAR, OVER }

/**
 * 单条预算进度项
 * @param typeId 一级分类 id；[BUDGET_TYPE_ID_TOTAL] 表总体
 * @param progress 进度 [0,1]，limit<=0 时为 null（不画进度条）
 * @param overAmount 超支额 = max(0, spent-limit)
 */
data class BudgetItem(
    val typeId: Long,
    val typeName: String,
    val typeIconName: String,
    val limit: Long,
    val spent: Long,
    val progress: Float?,
    val overAmount: Long,
    val state: BudgetStateEnum,
)

data class BudgetProgressEntity(
    val overall: BudgetItem?,
    val categoryList: List<BudgetItem>,
)

/** 组装单条预算进度（纯函数，便于单测） */
internal fun buildBudgetItem(
    typeId: Long,
    typeName: String,
    typeIconName: String,
    limit: Long,
    spent: Long,
): BudgetItem {
    val ratio = if (limit > 0) spent.toFloat() / limit else 0f
    val progress = if (limit > 0) ratio.coerceAtMost(1f) else null
    val state = when {
        limit <= 0 -> BudgetStateEnum.NORMAL
        ratio > 1f -> BudgetStateEnum.OVER
        ratio >= 0.8f -> BudgetStateEnum.NEAR
        else -> BudgetStateEnum.NORMAL
    }
    val overAmount = (spent - limit).coerceAtLeast(0L)
    return BudgetItem(typeId, typeName, typeIconName, limit, spent, progress, overAmount, state)
}
```

- [ ] **Step 2: 写 BudgetProgressTest（先失败）**

```kotlin
package cn.wj.android.cashbook.core.model

import cn.wj.android.cashbook.core.model.entity.BudgetStateEnum
import cn.wj.android.cashbook.core.model.entity.buildBudgetItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BudgetProgressTest {
    @Test fun normal_below_80_percent() {
        val item = buildBudgetItem(10L, "餐饮", "ic", limit = 1000, spent = 300)
        assertEquals(BudgetStateEnum.NORMAL, item.state)
        assertEquals(0.3f, item.progress!!, 0.001f)
        assertEquals(0L, item.overAmount)
    }
    @Test fun near_at_80_percent() {
        val item = buildBudgetItem(10L, "餐饮", "ic", limit = 1000, spent = 800)
        assertEquals(BudgetStateEnum.NEAR, item.state)
    }
    @Test fun near_upper_bound_100() {
        val item = buildBudgetItem(10L, "餐饮", "ic", limit = 1000, spent = 1000)
        assertEquals(BudgetStateEnum.NEAR, item.state)
        assertEquals(1f, item.progress!!, 0.001f)
    }
    @Test fun over_above_100_percent() {
        val item = buildBudgetItem(10L, "餐饮", "ic", limit = 500, spent = 650)
        assertEquals(BudgetStateEnum.OVER, item.state)
        assertEquals(150L, item.overAmount)
        assertEquals(1f, item.progress!!, 0.001f) // clamp
    }
    @Test fun limit_zero_progress_null_normal() {
        val item = buildBudgetItem(10L, "餐饮", "ic", limit = 0, spent = 300)
        assertNull(item.progress)
        assertEquals(BudgetStateEnum.NORMAL, item.state)
        assertEquals(0L, item.overAmount)
    }
    @Test fun spent_zero() {
        val item = buildBudgetItem(10L, "餐饮", "ic", limit = 1000, spent = 0)
        assertEquals(BudgetStateEnum.NORMAL, item.state)
        assertEquals(0f, item.progress!!, 0.001f)
    }
}
```

- [ ] **Step 3: 写 BudgetAmount.kt**

```kotlin
package cn.wj.android.cashbook.core.model.model

import java.math.BigDecimal
import java.math.RoundingMode

/** 预算限额上界：999 万元（分） */
const val BUDGET_AMOUNT_MAX_CENT = 999999_00L

/**
 * 解析用户输入的限额（元）为分；非法返回 null。
 * 拒绝：非数字 / ≤0 / 超上界。用 BigDecimal 比较避免 toLong 溢出回绕。
 */
fun parseBudgetAmountCent(input: String): Long? {
    val bd = input.trim().toBigDecimalOrNull() ?: return null
    val cent = bd.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP)
    if (cent < BigDecimal.ONE || cent > BigDecimal.valueOf(BUDGET_AMOUNT_MAX_CENT)) return null
    return cent.toLong()
}
```

- [ ] **Step 4: 写 BudgetAmountTest**

```kotlin
package cn.wj.android.cashbook.core.model

import cn.wj.android.cashbook.core.model.model.parseBudgetAmountCent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BudgetAmountTest {
    @Test fun valid() { assertEquals(199900L, parseBudgetAmountCent("1999")) }
    @Test fun valid_decimal() { assertEquals(1999L, parseBudgetAmountCent("19.99")) }
    @Test fun reject_zero() { assertNull(parseBudgetAmountCent("0")) }
    @Test fun reject_negative() { assertNull(parseBudgetAmountCent("-50")) }
    @Test fun reject_non_number() { assertNull(parseBudgetAmountCent("abc")) }
    @Test fun reject_empty() { assertNull(parseBudgetAmountCent("")) }
    @Test fun reject_overflow_huge() { assertNull(parseBudgetAmountCent("99999999999999999999")) }
    @Test fun reject_above_upper_bound() { assertNull(parseBudgetAmountCent("10000000")) } // 1000万 > 999万
    @Test fun accept_upper_bound() { assertEquals(99999900L, parseBudgetAmountCent("999999")) }
}
```

- [ ] **Step 5: 跑测试（JVM 库用 `:test`）**

Run: `./gradlew :core:model:test --offline --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL（BudgetProgressTest + BudgetAmountTest 全绿）

- [ ] **Step 6: Commit**

```bash
git add core/model
git commit -m "[feat|feature|预算][公共]预算领域模型+进度组装纯函数+限额校验(防溢出)"
```

---

### Task 4: core:data BudgetRepository + Impl + Hilt 绑定 + Fake

**Files:**
- Create: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/BudgetRepository.kt`
- Create: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/impl/BudgetRepositoryImpl.kt`
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/di/DataModule.kt`（`@Binds bindBudgetRepository`）
- Create: `core/testing/src/main/kotlin/cn/wj/android/cashbook/core/testing/repository/FakeBudgetRepository.kt`
- Create: `core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/transfer/BudgetTransfer.kt`（BudgetTable↔BudgetModel `asModel`/`asTable`，若项目用 transfer 包）

**Interfaces:**
- Consumes: `BudgetDao`（Task 1）、`BudgetModel`（Task 3）。
- Produces: `BudgetRepository`：`getBudgetsByBooksFlow(booksId): Flow<List<BudgetModel>>` / `getBudgetsByBooks(booksId): List<BudgetModel>` / `upsertBudget(booksId, typeId, amount)` / `deleteBudget(booksId, typeId)`。

- [ ] **Step 1: 读 transfer 模式**

Run: 查看 `core/model/.../transfer/` 现有 `asModel`/`asEntity` 写法（如 TypeTable→model），决定 BudgetTransfer 风格；若 Repository 直接手转亦可，保持与现有 RecordRepositoryImpl 一致。

- [ ] **Step 2: 写 BudgetRepository 接口**

```kotlin
package cn.wj.android.cashbook.core.data.repository

import cn.wj.android.cashbook.core.model.model.BudgetModel
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun getBudgetsByBooksFlow(booksId: Long): Flow<List<BudgetModel>>
    suspend fun getBudgetsByBooks(booksId: Long): List<BudgetModel>
    suspend fun upsertBudget(booksId: Long, typeId: Long, amount: Long)
    suspend fun deleteBudget(booksId: Long, typeId: Long)
}
```
（文件头照抄）

- [ ] **Step 3: 写 BudgetRepositoryImpl**（参照 `BooksRepositoryImpl` 的 `@Inject` + `withContext(coroutineContext)`）

```kotlin
class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao,
    @Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext,
) : BudgetRepository {

    override fun getBudgetsByBooksFlow(booksId: Long): Flow<List<BudgetModel>> =
        budgetDao.queryByBooksFlow(booksId).map { list -> list.map { it.asModel() } }

    override suspend fun getBudgetsByBooks(booksId: Long): List<BudgetModel> =
        withContext(coroutineContext) {
            budgetDao.queryByBooks(booksId).map { it.asModel() }
        }

    override suspend fun upsertBudget(booksId: Long, typeId: Long, amount: Long) =
        withContext(coroutineContext) {
            val existing = budgetDao.queryByBooksAndType(booksId, typeId)
            budgetDao.upsert(
                BudgetTable(id = existing?.id, booksId = booksId, typeId = typeId, amount = amount),
            )
        }

    override suspend fun deleteBudget(booksId: Long, typeId: Long) =
        withContext(coroutineContext) {
            budgetDao.deleteByBooksAndType(booksId, typeId)
        }
}

private fun BudgetTable.asModel() = BudgetModel(id = id, booksId = booksId, typeId = typeId, amount = amount)
```
（imports：BudgetDao / BudgetTable / BudgetModel / Dispatcher / CashbookDispatchers / Flow.map / withContext / Inject / CoroutineContext）

- [ ] **Step 4: DataModule 绑定**（参照 `bindBooksRepository`）

```kotlin
    @Binds
    @Singleton
    fun bindBudgetRepository(
        impl: BudgetRepositoryImpl,
    ): BudgetRepository
```
（+ imports BudgetRepository / BudgetRepositoryImpl）

- [ ] **Step 5: 写 FakeBudgetRepository（core:testing，忠实内存桩，复刻唯一约束 upsert 语义）**

```kotlin
class FakeBudgetRepository : BudgetRepository {
    private val data = MutableStateFlow<List<BudgetModel>>(emptyList())
    override fun getBudgetsByBooksFlow(booksId: Long): Flow<List<BudgetModel>> =
        data.map { list -> list.filter { it.booksId == booksId } }
    override suspend fun getBudgetsByBooks(booksId: Long): List<BudgetModel> =
        data.value.filter { it.booksId == booksId }
    override suspend fun upsertBudget(booksId: Long, typeId: Long, amount: Long) {
        // 复刻 (books_id,type_id) 唯一：存在则替换，否则新增
        val others = data.value.filterNot { it.booksId == booksId && it.typeId == typeId }
        data.value = others + BudgetModel(id = null, booksId = booksId, typeId = typeId, amount = amount)
    }
    override suspend fun deleteBudget(booksId: Long, typeId: Long) {
        data.value = data.value.filterNot { it.booksId == booksId && it.typeId == typeId }
    }
}
```

- [ ] **Step 6: 编译验证**

Run: `./gradlew :core:data:compileDebugKotlin :core:testing:compileDebugKotlin --offline --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add core/data core/testing core/model
git commit -m "[feat|feature|预算][公共]BudgetRepository+Impl+Hilt绑定+FakeBudgetRepository"
```

---

### Task 5: core:domain GetBudgetProgressUseCase

**Files:**
- Create: `core/domain/src/main/kotlin/cn/wj/android/cashbook/domain/usecase/GetBudgetProgressUseCase.kt`
- Test: `core/domain/src/test/kotlin/cn/wj/android/cashbook/domain/usecase/GetBudgetProgressUseCaseTest.kt`

**Interfaces:**
- Consumes: `BudgetRepository`（Task 4）、`GetRecordViewsBetweenDateUseCase`、`TransRecordViewsToAnalyticsPieUseCase`、`SettingRepository.recordSettingsModel`、`BooksRepository.currentBook`、`TypeRepository.getRecordTypeById`、`DateSelectionEntity.currentMonthPeriod`、`analyticsPieNetAmount`、`buildBudgetItem`。
- Produces: `GetBudgetProgressUseCase.invoke(today: LocalDate = LocalDate.now()): BudgetProgressEntity`。

- [ ] **Step 1: 确认依赖签名**

Run: 确认 `GetRecordViewsBetweenDateUseCase.invoke` 返回类型（应为 `List<RecordViewsModel>`，字段 `type.typeCategory` / `finalAmount` / `amount` / `charges` / `concessions` / `isBalanceRecord`）；`TransRecordViewsToAnalyticsPieUseCase.invoke(typeCategory, recordViewsList): List<AnalyticsRecordPieEntity>`（`AnalyticsRecordPieEntity.typeId` / `totalAmount`）。

- [ ] **Step 2: 写 GetBudgetProgressUseCase**

```kotlin
package cn.wj.android.cashbook.domain.usecase

import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.data.repository.BooksRepository
import cn.wj.android.cashbook.core.data.repository.BudgetRepository
import cn.wj.android.cashbook.core.data.repository.SettingRepository
import cn.wj.android.cashbook.core.data.repository.TypeRepository
import cn.wj.android.cashbook.core.model.entity.BUDGET_TYPE_ID_TOTAL
import cn.wj.android.cashbook.core.model.entity.BudgetProgressEntity
import cn.wj.android.cashbook.core.model.entity.DateSelectionEntity
import cn.wj.android.cashbook.core.model.entity.buildBudgetItem
import cn.wj.android.cashbook.core.model.enums.RecordTypeCategoryEnum
import cn.wj.android.cashbook.core.model.model.analyticsPieNetAmount
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * 聚合「本周期预算进度」：复用 [TransRecordViewsToAnalyticsPieUseCase] 算各分类已花（净自付），
 * 总体已花直接对本周期 EXPENDITURE 求 Σ analyticsPieNetAmount（不靠 Σ pieList，规避孤儿少算）。
 */
class GetBudgetProgressUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val booksRepository: BooksRepository,
    private val settingRepository: SettingRepository,
    private val typeRepository: TypeRepository,
    private val getRecordViewsBetweenDateUseCase: GetRecordViewsBetweenDateUseCase,
    private val transRecordViewsToAnalyticsPieUseCase: TransRecordViewsToAnalyticsPieUseCase,
    @Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext,
) {
    suspend operator fun invoke(today: LocalDate = LocalDate.now()): BudgetProgressEntity =
        withContext(coroutineContext) {
            val monthStartDay = settingRepository.recordSettingsModel.first().monthStartDay
            val booksId = booksRepository.currentBook.first().id
            val period = DateSelectionEntity.currentMonthPeriod(today, monthStartDay)
            val records = getRecordViewsBetweenDateUseCase(period, monthStartDay)
            val pieList = transRecordViewsToAnalyticsPieUseCase(RecordTypeCategoryEnum.EXPENDITURE, records)
            val budgets = budgetRepository.getBudgetsByBooks(booksId)

            val totalSpent = records
                .filter { !it.isBalanceRecord && it.type.typeCategory == RecordTypeCategoryEnum.EXPENDITURE }
                .sumOf {
                    analyticsPieNetAmount(
                        RecordTypeCategoryEnum.EXPENDITURE,
                        it.finalAmount, it.amount, it.charges, it.concessions,
                    )
                }

            val overall = budgets.firstOrNull { it.typeId == BUDGET_TYPE_ID_TOTAL }
                ?.let { buildBudgetItem(BUDGET_TYPE_ID_TOTAL, "", "", it.amount, totalSpent) }

            val categoryList = budgets
                .filter { it.typeId != BUDGET_TYPE_ID_TOTAL }
                .map { b ->
                    val spent = pieList.firstOrNull { it.typeId == b.typeId }?.totalAmount ?: 0L
                    val type = typeRepository.getRecordTypeById(b.typeId)
                    buildBudgetItem(b.typeId, type?.name ?: "", type?.iconName ?: "", b.amount, spent)
                }

            BudgetProgressEntity(overall = overall, categoryList = categoryList)
        }
}
```
> 执行时核对 `RecordViewsModel` 字段名（`type`/`finalAmount`/`amount`/`charges`/`concessions`/`isBalanceRecord`）与 `RecordTypeModel.typeCategory`，按实际微调。

- [ ] **Step 3: 写 GetBudgetProgressUseCaseTest**（用 `core:testing` 的 Fake + `createXxxModel`）

测试构造：FakeRecordRepository 注入本周期 EXPENDITURE 记录（餐饮净自付 700、交通 650）、FakeBudgetRepository 注入 (总体 limit=2000, 餐饮 limit=1000, 交通 limit=500)、FakeSettingRepository monthStartDay=1、FakeBooksRepository currentBook.id=1。断言：
```kotlin
    @Test fun aggregates_overall_and_categories() = runTest {
        // ... 构造 fakes + records ...
        val result = useCase(today = LocalDate.of(2024, 1, 15))
        assertEquals(1350L, result.overall!!.spent)   // 700+650，直接 Σ EXPENDITURE
        assertEquals(2000L, result.overall.limit)
        val canting = result.categoryList.first { it.typeId == cantingTypeId }
        assertEquals(700L, canting.spent)
        assertEquals(BudgetStateEnum.NEAR, canting.state) // 700/1000=70%? -> NORMAL；按构造调整断言
        val jiaotong = result.categoryList.first { it.typeId == jiaotongTypeId }
        assertEquals(BudgetStateEnum.OVER, jiaotong.state) // 650/500
    }
    @Test fun no_overall_budget_returns_null_overall() { /* budgets 不含 -1L */ }
    @Test fun category_without_record_spent_zero() { /* 设了预算但无记录 */ }
```
> 执行时按 `core:testing` 现有 `FakeRecordRepository`/`createRecordViewsModel` 真实接口构造数据；若 `GetRecordViewsBetweenDateUseCase` 内部依赖未被 Fake 覆盖，改注入 `FakeRecordRepository.queryRecordListBetweenDate` 返本周期记录。断言数值按实际构造校准（上面注释处）。

- [ ] **Step 4: 跑测试**

Run: `./gradlew :core:domain:testDebugUnitTest --offline --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add core/domain
git commit -m "[feat|feature|预算][公共]GetBudgetProgressUseCase(复用Pie口径+总体直接ΣEXPENDITURE)"
```

---

### Task 6: feature:budget 模块骨架 + BudgetViewModel

**Files:**
- Create: `feature/budget/build.gradle.kts`（照抄 `feature/tags/build.gradle.kts`，namespace `cn.wj.android.cashbook.feature.budget`）
- Create: `feature/budget/src/main/AndroidManifest.xml`（若 tags 有则照抄）
- Modify: `settings.gradle.kts`（`include(":feature:budget")`）
- Modify: `app/build.gradle.kts:158` 后（`implementation(projects.feature.budget)`）
- Create: `feature/budget/src/main/kotlin/cn/wj/android/cashbook/feature/budget/viewmodel/BudgetViewModel.kt`
- Test: `feature/budget/src/test/kotlin/cn/wj/android/cashbook/feature/budget/viewmodel/BudgetViewModelTest.kt`

**Interfaces:**
- Consumes: `GetBudgetProgressUseCase`（Task 5）、`BudgetRepository`、`BooksRepository`、`SettingRepository`、`TypeRepository.firstExpenditureTypeListData`、`recordDataVersion`、`parseBudgetAmountCent`、`BUDGET_TYPE_ID_TOTAL`。
- Produces: `BudgetUiState`（Loading / Success(BudgetProgressEntity, addableTypes: List<RecordTypeModel>)）；`BudgetViewModel.onSetBudget(typeId, input)` / `onDeleteBudget(typeId)`。

- [ ] **Step 1: 建模块骨架**

照抄 `feature/tags/build.gradle.kts` → `feature/budget/build.gradle.kts`，改 `namespace`。`settings.gradle.kts` 加 include。`app/build.gradle.kts` 加依赖。

- [ ] **Step 2: 编译空模块**

Run: `./gradlew :feature:budget:compileDebugKotlin --offline --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL（空模块）

- [ ] **Step 3: 写 BudgetViewModel**（参照 `TypedAnalyticsViewModel` 的 `recordDataVersion` + `flatMapLatest`/`combine` + `stateIn`）

```kotlin
@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val getBudgetProgressUseCase: GetBudgetProgressUseCase,
    private val budgetRepository: BudgetRepository,
    private val booksRepository: BooksRepository,
    private val settingRepository: SettingRepository,
    private val typeRepository: TypeRepository,
) : ViewModel() {

    // 以当前账本流为外层 flatMapLatest，防切账本错配帧
    val uiState: StateFlow<BudgetUiState> =
        booksRepository.currentBook.flatMapLatest { book ->
            combine(
                recordDataVersion,
                settingRepository.recordSettingsModel.map { it.monthStartDay }.distinctUntilChanged(),
                budgetRepository.getBudgetsByBooksFlow(book.id),
                typeRepository.firstExpenditureTypeListData,
            ) { _, _, budgets, firstExpTypes ->
                val progress = getBudgetProgressUseCase()
                val setTypeIds = budgets.map { it.typeId }.toSet()
                val addable = firstExpTypes.filter { it.id > 0 && it.id !in setTypeIds } // 排除固定类型(负id)+已设
                BudgetUiState.Success(progress, addable)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetUiState.Loading)

    fun onSetBudget(typeId: Long, input: String) {
        val cent = parseBudgetAmountCent(input) ?: return // 非法静默不落库（UI 层提示）
        viewModelScope.launch {
            val booksId = booksRepository.currentBook.first().id
            budgetRepository.upsertBudget(booksId, typeId, cent)
        }
    }

    fun onDeleteBudget(typeId: Long) {
        viewModelScope.launch {
            val booksId = booksRepository.currentBook.first().id
            budgetRepository.deleteBudget(booksId, typeId)
        }
    }
}

sealed interface BudgetUiState {
    data object Loading : BudgetUiState
    data class Success(
        val progress: BudgetProgressEntity,
        val addableTypes: List<RecordTypeModel>,
    ) : BudgetUiState
}
```
> `recordDataVersion` import 自 `core.common.model.recordDataVersion`（同 TypedAnalyticsViewModel）。`onSetBudget` 校验失败的 UI 提示由 Screen 层在调用前 `parseBudgetAmountCent` 判断并 toast/inline error（见 Task 7）。

- [ ] **Step 4: 写 BudgetViewModelTest**（JVM，注入 Fakes + MainDispatcherRule）

测试：① 设限额→uiState.Success 含该预算；② 删限额→移除；③ 非法输入(0/负/abc)→不落库；④ addableTypes 排除已设 + 排除负 id 固定类型；⑤ 切账本 uiState 反映新账本预算。
```kotlin
    @Test fun set_budget_appears_in_state() = runTest { /* onSetBudget(10, "1000") → progress.categoryList 含 10 */ }
    @Test fun invalid_input_not_persisted() = runTest { /* onSetBudget(10, "0") → 无变化 */ }
    @Test fun addable_excludes_set_and_fixed() = runTest { /* firstExp 含 -1101 平账 + 10/11；设 10 → addable 仅 11 */ }
```
> 按 `feature` 模块测试惯例用 `core:testing` Fakes；`recordDataVersion` 是全局 MutableStateFlow，测试可直接 bump。

- [ ] **Step 5: 跑测试**

Run: `./gradlew :feature:budget:testDebugUnitTest --offline --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add feature/budget settings.gradle.kts app/build.gradle.kts
git commit -m "[feat|feature|预算][公共]新建feature:budget模块+BudgetViewModel(响应式+选择器排除固定类型)"
```

---

### Task 7: BudgetScreen + Route + navigation + 限额对话框 + 截图基线

**Files:**
- Create: `feature/budget/src/main/kotlin/cn/wj/android/cashbook/feature/budget/screen/BudgetScreen.kt`（Route + Screen + 进度行 + 限额对话框）
- Create: `feature/budget/src/main/kotlin/cn/wj/android/cashbook/feature/budget/navigation/BudgetNavigation.kt`（`budgetScreen(onBackClick)` + route 常量）
- Create: `feature/budget/src/main/res/values/strings.xml`（标题/总体预算/分类预算/添加/限额/已超支 等文案）
- Test: `feature/budget/src/test/kotlin/cn/wj/android/cashbook/feature/budget/BudgetScreenScreenshotTests.kt`

**Interfaces:**
- Consumes: `BudgetViewModel` / `BudgetUiState`（Task 6）。
- Produces: `fun NavGraphBuilder.budgetScreen(onBackClick: () -> Unit)` + `const val ROUTE_BUDGET`（供 app MainApp 接线）。

- [ ] **Step 1: 写 BudgetScreen**（全用 `Cb*` 封装：`CbScaffold`/`CbTopAppBar`/`CbAlertDialog`/`CbTextField`/`CbListItem`；进度条用 `LinearProgressIndicator`（Material3 进度条非禁列，确认 lint 不禁）或 core:design 既有进度组件）

Route 层：
```kotlin
@Composable
internal fun BudgetRoute(
    onBackClick: () -> Unit,
    viewModel: BudgetViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BudgetScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onSetBudget = viewModel::onSetBudget,
        onDeleteBudget = viewModel::onDeleteBudget,
    )
}
```
Screen 层：`CbScaffold` + `CbTopAppBar(title=预算管理, onBackClick)`；body 按 `uiState`：
- Loading → 居中进度
- Success → `LazyColumn`：总体预算卡（progress 行或「设置总体预算」引导）→ 「分类预算 + 添加」标题行 → 各 `BudgetItem` 进度行（图标 + 名称 + 进度条 + `¥已花/¥限额` + 超支额 OVER 红）→ 空态引导。
- 点行 → 弹限额对话框（`CbAlertDialog`，`CbTextField` 数字键盘，确认时 `parseBudgetAmountCent` 校验，非法 inline error 不 dismiss；含删除按钮）。
- 「+ 添加分类预算」→ 弹分类选择（`addableTypes`，`CbListItem`）→ 选后弹限额对话框。

> 完整 Composable 布局参照 `feature/records/.../screen/TypedAnalyticsScreen.kt`（汇总卡 + LazyColumn 模式）与 `RecordMonthSummaryHeader`（进度/金额展示）。配色 state→color：NORMAL=`colorScheme.primary`，NEAR=橙（`Color(0xFFFF9800)` 或 core:design token），OVER=`colorScheme.error`。

- [ ] **Step 2: 写 BudgetNavigation**

```kotlin
const val ROUTE_BUDGET = "budget"

fun NavGraphBuilder.budgetScreen(onBackClick: () -> Unit) {
    composable(route = ROUTE_BUDGET) {
        BudgetRoute(onBackClick = onBackClick)
    }
}
```

- [ ] **Step 3: 写 strings.xml**（中文文案：预算管理/总体预算/设置总体预算/分类预算/添加分类预算/限额/已花/已超支%s/暂无预算，点击添加/请输入大于0的金额）

- [ ] **Step 4: 编译**

Run: `./gradlew :feature:budget:compileDebugKotlin --offline --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 写截图测试 + record 基线**（参照 `feature/records/.../*ScreenshotTests.kt`，固定日期数据，4 态：NORMAL/NEAR/OVER 混合 + 空态；multiTheme + multiDevice）

Run record: `./gradlew :feature:budget:recordRoborazziDebug --offline --no-daemon --console=plain`
确认基线 PNG 落 `feature/budget/src/test/screenshots/`，Read 抽检非塌陷（topbar + 列表正常渲染）。

- [ ] **Step 6: verify 0 diff**

Run: `./gradlew :feature:budget:verifyRoborazziDebug --offline --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL（0 diff）

- [ ] **Step 7: Commit**

```bash
git add feature/budget
git commit -m "[feat|feature|预算][公共]BudgetScreen+导航+限额对话框+截图基线4态"
```

---

### Task 8: 抽屉入口接线（LauncherDrawerActions + LauncherScreen + MainApp + 截图 re-record）

**Files:**
- Modify: `feature/settings/.../navigation/LauncherDrawerActions.kt`（加 `onBudgetClick`）
- Modify: `feature/settings/.../screen/LauncherScreen.kt`（`:79` Route wrap 构造 + 抽屉项 NavigationDrawerItem 待报销后）
- Modify: `feature/settings/.../res/values/strings*.xml`（「预算管理」抽屉项文案）
- Modify: `feature/settings/src/test/.../LauncherScreenScreenshotTests.kt`（4 构造点补 `onBudgetClick` + re-record success 两态）
- Modify: `app/src/main/kotlin/cn/wj/android/cashbook/ui/MainApp.kt`（`:416` 构造补 `onBudgetClick` + `budgetScreen` 导航接线 + 跳转）

**Interfaces:**
- Consumes: `budgetScreen` / `ROUTE_BUDGET`（Task 7）。

- [ ] **Step 1: LauncherDrawerActions 加回调**

`data class` 加（保持 public）：在 `onReimbursementClick` 后加 `val onBudgetClick: () -> Unit,` + KDoc `@param onBudgetClick 预算管理点击回调`。

- [ ] **Step 2: LauncherScreen Route wrap（`:79` 构造）+ 抽屉项**

`:79` 构造 `LauncherDrawerActions(...)` 补 `onBudgetClick = wrap(actions.onBudgetClick),`。
抽屉项：在「待报销」`NavigationDrawerItem`（`:223`）后加预算管理项（照其 label/icon/onClick=`actions.onBudgetClick` 模式，icon 用 `CbIcons` 合适图标如 `Savings`/`AccountBalanceWallet`）。

- [ ] **Step 3: MainApp 接线（`:416`）**

`:416` 构造 `LauncherDrawerActions(...)` 补 `onBudgetClick = { navController.navigate(ROUTE_BUDGET) },`；NavHost 内加 `budgetScreen(onBackClick = { navController.popBackStack() })`（+ import）。app 依赖 `projects.feature.budget` 已在 Task 6 加。

- [ ] **Step 4: 截图测试 4 构造点补 onBudgetClick**

`LauncherScreenScreenshotTests.kt` 的 4 处 `LauncherDrawerActions(` 各补 `onBudgetClick = {},`。

- [ ] **Step 5: 编译**

Run: `./gradlew :feature:settings:compileDebugKotlin :app:compileDebugKotlin --offline --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: re-record LauncherScreen success（新增抽屉项→UI 变化非 0 diff）**

Run: `./gradlew :feature:settings:recordRoborazziDebug --offline --no-daemon --console=plain`
git status 应见 `launcherScreen_success_*` PNG 变化（新增抽屉项）；Read 抽检新增「预算管理」项渲染正常。

- [ ] **Step 7: verify 0 diff**

Run: `./gradlew :feature:settings:verifyRoborazziDebug --offline --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
git add feature/settings app
git commit -m "[feat|feature|预算][公共]左抽屉新增预算管理入口+MainApp导航接线+截图re-record"
```

---

### Task 9: Low backlog③ RecordMonthSummaryHeader 固定周期态中性标签

**Files:**
- Modify: `feature/records/.../view/RecordMonthSummaryHeader.kt:121-135`（monthSwitchable 派生标签）
- Modify: `core/ui/src/main/res/values/strings_records.xml`（新增中性 `summary_income/expend/balance`，**不删** `month_*`）
- Test: `feature/records/.../*ScreenshotTests.kt`（TypedAnalytics/AssetInfoContent 固定态 re-record）

**Interfaces:**
- 无新增对外接口（纯 UI 标签调整）。

- [ ] **Step 1: 加中性 string**

`core/ui/.../strings_records.xml` 加（不删现有 `month_income/expend/balance`）：
```xml
    <string name="summary_income">收入</string>
    <string name="summary_expend">支出</string>
    <string name="summary_balance">结余</string>
```

- [ ] **Step 2: RecordMonthSummaryHeader 按 monthSwitchable 派生标签**

`:121-135` 三个 `SummaryColumn` 的 `label` 改：
```kotlin
                    SummaryColumn(
                        label = stringResource(
                            id = if (monthSwitchable) R.string.month_income else R.string.summary_income,
                        ),
                        amount = summary.income,
                        modifier = Modifier.weight(1f),
                    )
```
（expend/balance 同理用 `month_expend`/`summary_expend`、`month_balance`/`summary_balance`）

- [ ] **Step 3: 编译**

Run: `./gradlew :feature:records:compileDebugKotlin --offline --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: re-record 受影响固定态截图**

Run: `./gradlew :feature:records:recordRoborazziDebug --offline --no-daemon --console=plain`
git status 应仅见 TypedAnalytics/AssetInfoContent 固定态(monthSwitchable=false)截图变化（月→中性标签）；月份态(monthSwitchable=true)0 diff。Read 抽检。

- [ ] **Step 5: verify 0 diff**

Run: `./gradlew :feature:records:verifyRoborazziDebug --offline --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add feature/records core/ui
git commit -m "[fix|feature|统计][公共]固定周期态汇总卡用中性标签(收入/支出/结余)"
```

---

## 收尾验证（全 Task 完成后）

- [ ] 全量编译：`./gradlew :app:assembleOnlineDebug --offline --no-daemon --console=plain` → BUILD SUCCESSFUL
- [ ] 受影响模块测试：`./gradlew :core:model:test :core:domain:testDebugUnitTest :feature:budget:testDebugUnitTest --offline --no-daemon --console=plain`
- [ ] 截图：`:feature:budget:verifyRoborazziDebug :feature:settings:verifyRoborazziDebug :feature:records:verifyRoborazziDebug`
- [ ] spotless：`./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache`
- [ ] androidTest（有设备时）：`:core:database:connectedDebugAndroidTest`（migrate12_13 + 级联）
- [ ] UI 功能验证（有设备/模拟器时）：装 app → 抽屉进预算管理 → 设总体+分类限额 → 记一笔支出 → 验证已花/进度/超支视觉 → 删预算/删账本级联。
- [ ] 节点2 `comprehensive-review:full-review` 对全 diff 终审。

## Self-Review 记录

- **Spec 覆盖**：§3 数据模型→T1；§5 级联→T2；§3 model/§6 校验→T3；§4.3 Repository→T4；§4.2 UseCase→T5；§4 ViewModel→T6；§6 UI→T7；§6.2 抽屉/§9 接线→T8；§7 Low backlog③→T9。全覆盖。
- **类型一致**：`buildBudgetItem`/`BudgetItem`/`BudgetProgressEntity`/`parseBudgetAmountCent`/`BUDGET_TYPE_ID_TOTAL` 跨 T3→T5→T6 一致；`BudgetRepository` 方法名 T4 定义、T5/T6 消费一致；`onBudgetClick` T8 全 4 构造点。
- **执行时坐实点**（plan 已标注，非 placeholder）：`GetRecordViewsBetweenDateUseCase` 返回类型/`RecordViewsModel` 字段名（T5 Step1）、`deleteBookTransaction` 事务结构（T2 Step1）、`TypedAnalyticsScreen` 布局模板（T7）、`CbIcons` 预算图标名（T8）。
