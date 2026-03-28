# 固化报销、退款、信用卡还款特殊类型 - 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将退款、报销、信用卡还款固化为系统内置类型（固定负数 ID、protected=1），移除用户可修改的设置逻辑，兼容旧版本数据。

**Architecture:** 数据库迁移插入固定类型行 → 应用层首次启动迁移旧记录引用 → TypeRepository 简化为固定 ID 比较 → UI 层移除设置菜单，protected 类型只读展示。

**Tech Stack:** Kotlin, Room (SQLite Migration), Proto DataStore, Jetpack Compose, Hilt, JUnit 4 + Truth

**Spec:** `docs/superpowers/specs/2026-03-22-fix-special-type-design.md`

---

### Task 1: 新增固定类型 ID 常量 + TypeTable 常量

**Files:**
- Modify: `core/common/src/main/kotlin/cn/wj/android/cashbook/core/common/Constants.kt`
- Modify: `core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/table/TypeTable.kt:61-85`

- [ ] **Step 1: 在 Constants.kt 中新增固定类型 ID 常量**

```kotlin
/** 固定类型 ID - 退款 */
const val FIXED_TYPE_ID_REFUND = -2001L
/** 固定类型 ID - 报销 */
const val FIXED_TYPE_ID_REIMBURSE = -2002L
/** 固定类型 ID - 信用卡还款 */
const val FIXED_TYPE_ID_CREDIT_CARD_PAYMENT = -2003L
```

- [ ] **Step 2: 在 TypeTable.kt 末尾追加三个固定类型常量**

在现有 `TYPE_TABLE_BALANCE_INCOME` 之后添加：

```kotlin
/** 固定类型 - 退款（收入） */
val TYPE_TABLE_REFUND: TypeTable
    get() = TypeTable(
        id = FIXED_TYPE_ID_REFUND,
        parentId = -1L,
        name = "退款",
        iconName = "vector_refund",
        typeLevel = TypeLevelEnum.FIRST.ordinal,
        typeCategory = RecordTypeCategoryEnum.INCOME.ordinal,
        protected = SWITCH_INT_ON,
        sort = 0,
    )

/** 固定类型 - 报销（收入） */
val TYPE_TABLE_REIMBURSE: TypeTable
    get() = TypeTable(
        id = FIXED_TYPE_ID_REIMBURSE,
        parentId = -1L,
        name = "报销",
        iconName = "vector_reimburse",
        typeLevel = TypeLevelEnum.FIRST.ordinal,
        typeCategory = RecordTypeCategoryEnum.INCOME.ordinal,
        protected = SWITCH_INT_ON,
        sort = 0,
    )

/** 固定类型 - 信用卡还款（转账） */
val TYPE_TABLE_CREDIT_CARD_PAYMENT: TypeTable
    get() = TypeTable(
        id = FIXED_TYPE_ID_CREDIT_CARD_PAYMENT,
        parentId = -1L,
        name = "信用卡还款",
        iconName = "vector_credit_card_payment",
        typeLevel = TypeLevelEnum.FIRST.ordinal,
        typeCategory = RecordTypeCategoryEnum.TRANSFER.ordinal,
        protected = SWITCH_INT_ON,
        sort = 0,
    )
```

注意：使用 `.ordinal` 而非 `.code`，与现有平账类型风格保持一致。需要新增 `import cn.wj.android.cashbook.core.common.SWITCH_INT_ON` 和 `import cn.wj.android.cashbook.core.common.FIXED_TYPE_ID_*`。

- [ ] **Step 2: 验证编译通过**

Run: `./gradlew :core:database:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```
git add core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/table/TypeTable.kt
git commit -m "[feat|core|database][公共]新增退款、报销、信用卡还款固定类型常量"
```

---

### Task 2: Migration11To12 追加固定类型 INSERT

**Files:**
- Modify: `core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/migration/Migration11To12.kt:32-41,197-215`

- [ ] **Step 1: 添加三条 INSERT SQL 常量**

在 `Migration11To12` 的 `// region 索引创建` 之前（约第 139 行），添加新的 region：

```kotlin
// region 固定类型插入

@Language("SQL")
private const val SQL_INSERT_TYPE_REFUND = """
    INSERT OR IGNORE INTO db_type (id, parent_id, name, icon_name, type_level, type_category, protected, sort)
    VALUES (-2001, -1, '退款', 'vector_refund', 0, 1, 1, 0)
"""

@Language("SQL")
private const val SQL_INSERT_TYPE_REIMBURSE = """
    INSERT OR IGNORE INTO db_type (id, parent_id, name, icon_name, type_level, type_category, protected, sort)
    VALUES (-2002, -1, '报销', 'vector_reimburse', 0, 1, 1, 0)
"""

@Language("SQL")
private const val SQL_INSERT_TYPE_CREDIT_CARD_PAYMENT = """
    INSERT OR IGNORE INTO db_type (id, parent_id, name, icon_name, type_level, type_category, protected, sort)
    VALUES (-2003, -1, '信用卡还款', 'vector_credit_card_payment', 0, 2, 1, 0)
"""

private fun SupportSQLiteDatabase.insertFixedTypes() {
    execSQL(SQL_INSERT_TYPE_REFUND)
    execSQL(SQL_INSERT_TYPE_REIMBURSE)
    execSQL(SQL_INSERT_TYPE_CREDIT_CARD_PAYMENT)
}

// endregion
```

- [ ] **Step 2: 在 migrate() 方法中调用 insertFixedTypes()**

修改 `migrate()` 方法，在 `createIndices()` 之后添加 `insertFixedTypes()`：

```kotlin
override fun migrate(db: SupportSQLiteDatabase) {
    logger().i("migrate(db)")
    with(db) {
        migrateRecord()
        migrateAsset()
        createIndices()
        insertFixedTypes()
    }
}
```

- [ ] **Step 3: 验证编译通过**

Run: `./gradlew :core:database:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 提交**

```
git add core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/migration/Migration11To12.kt
git commit -m "[feat|core|database][公共]Migration11To12追加固定类型INSERT"
```

---

### Task 3: TypeRepository 接口移除 set*Type 方法

**Files:**
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/TypeRepository.kt:55-57,72-73`

- [ ] **Step 1: 移除三个 set 方法声明**

从 `TypeRepository` 接口中删除以下三行：

```kotlin
// 删除第 55 行
suspend fun setReimburseType(typeId: Long)
// 删除第 57 行
suspend fun setRefundType(typeId: Long)
// 删除第 73 行
suspend fun setCreditPaymentType(typeId: Long)
```

- [ ] **Step 2: 暂不编译（下个 Task 同步修改实现类后一起验证）**

---

### Task 4: TypeRepositoryImpl 简化 + 迁移逻辑

**Files:**
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/impl/TypeRepositoryImpl.kt`

- [ ] **Step 1: 移除缓存字段和 get*TypeId 私有方法**

删除以下代码（第 49-100 行）：
- `_refundTypeId`、`_reimburseTypeId`、`_creditCardPaymentTypeId` 字段
- `getRefundTypeId()`、`getReimburseTypeId()`、`getCreditCardPaymentTypeId()` 方法

- [ ] **Step 2: 简化 is*Type 和 needRelated 方法**

替换原有实现为固定 ID 比较：

```kotlin
import cn.wj.android.cashbook.core.database.table.TYPE_TABLE_REFUND
import cn.wj.android.cashbook.core.database.table.TYPE_TABLE_REIMBURSE
import cn.wj.android.cashbook.core.database.table.TYPE_TABLE_CREDIT_CARD_PAYMENT

override suspend fun needRelated(typeId: Long): Boolean = withContext(coroutineContext) {
    typeId == TYPE_TABLE_REFUND.id || typeId == TYPE_TABLE_REIMBURSE.id
}

override suspend fun isReimburseType(typeId: Long): Boolean = withContext(coroutineContext) {
    typeId == TYPE_TABLE_REIMBURSE.id
}

override suspend fun isRefundType(typeId: Long): Boolean = withContext(coroutineContext) {
    typeId == TYPE_TABLE_REFUND.id
}

override suspend fun isCreditPaymentType(typeId: Long): Boolean = withContext(coroutineContext) {
    TYPE_TABLE_CREDIT_CARD_PAYMENT.id == typeId
}
```

- [ ] **Step 3: 移除 set*Type 方法实现**

删除以下方法（第 161-169 行和第 224-227 行）：
- `setReimburseType()`
- `setRefundType()`
- `setCreditPaymentType()`

- [ ] **Step 4: 在 deleteById 中增加防御性校验**

```kotlin
override suspend fun deleteById(id: Long): Unit = withContext(coroutineContext) {
    // 固定类型不允许删除
    require(id != TYPE_TABLE_REFUND.id && id != TYPE_TABLE_REIMBURSE.id && id != TYPE_TABLE_CREDIT_CARD_PAYMENT.id) {
        "Cannot delete fixed type: $id"
    }
    typeDao.deleteById(id)
    typeDataVersion.updateVersion()
}
```

- [ ] **Step 5: 简化 getRecordTypeById 和 getFirstRecordTypeList 中的 needRelated 调用**

将 `combineProtoDataSource.needRelated(typeId)` 替换为本地方法调用。由于 `needRelated` 现在是纯比较操作，可直接内联：

```kotlin
override suspend fun getRecordTypeById(typeId: Long): RecordTypeModel? =
    withContext(coroutineContext) {
        typeDao.queryById(typeId)?.asModel(
            typeId == TYPE_TABLE_REFUND.id || typeId == TYPE_TABLE_REIMBURSE.id,
        )
    }

private suspend fun getFirstRecordTypeList(): List<RecordTypeModel> =
    withContext(coroutineContext) {
        typeDao.queryByLevel(TypeLevelEnum.FIRST.ordinal)
            .map {
                val id = it.id ?: -1L
                it.asModel(id == TYPE_TABLE_REFUND.id || id == TYPE_TABLE_REIMBURSE.id)
            }
    }

override suspend fun getSecondRecordTypeListByParentId(parentId: Long): List<RecordTypeModel> =
    withContext(coroutineContext) {
        typeDao.queryByParentId(parentId)
            .map {
                val id = it.id ?: -1L
                it.asModel(id == TYPE_TABLE_REFUND.id || id == TYPE_TABLE_REIMBURSE.id)
            }
    }
```

- [ ] **Step 6: 新增一次性迁移方法**

在 `TypeRepositoryImpl` 中添加：

```kotlin
/**
 * 应用层一次性迁移：将旧的特殊类型记录引用迁移到固定 ID
 * 设计为幂等操作，崩溃后重试安全
 */
suspend fun migrateSpecialTypes(): Unit = withContext(coroutineContext) {
    val settings = combineProtoDataSource.recordSettingsData.first()

    migrateOneType(
        oldTypeId = settings.refundTypeId,
        fixedTypeId = FIXED_TYPE_ID_REFUND,
        fallbackName = "退款",
        fallbackCategory = RecordTypeCategoryEnum.INCOME.ordinal,
        updateDataStore = { combineProtoDataSource.updateRefundTypeId(it) },
    )
    migrateOneType(
        oldTypeId = settings.reimburseTypeId,
        fixedTypeId = FIXED_TYPE_ID_REIMBURSE,
        fallbackName = "报销",
        fallbackCategory = RecordTypeCategoryEnum.INCOME.ordinal,
        updateDataStore = { combineProtoDataSource.updateReimburseTypeId(it) },
    )
    migrateOneType(
        oldTypeId = settings.creditCardPaymentTypeId,
        fixedTypeId = FIXED_TYPE_ID_CREDIT_CARD_PAYMENT,
        fallbackName = "还信用卡",
        fallbackCategory = RecordTypeCategoryEnum.TRANSFER.ordinal,
        updateDataStore = { combineProtoDataSource.updateCreditCardPaymentTypeId(it) },
    )
}

private suspend fun migrateOneType(
    oldTypeId: Long,
    fixedTypeId: Long,
    fallbackName: String,
    fallbackCategory: Int,
    updateDataStore: suspend (Long) -> Unit,
) {
    if (oldTypeId == fixedTypeId) return // 已迁移

    val targetOldId = if (oldTypeId > 0L) {
        oldTypeId
    } else {
        // DataStore 无记录，按名称查找
        typeDao.queryByName(fallbackName)
            ?.takeIf { it.typeCategory == fallbackCategory }
            ?.id ?: 0L
    }

    if (targetOldId > 0L) {
        // 在事务中执行数据库操作
        typeDao.migrateTypeRecords(targetOldId, fixedTypeId)
    }

    // 更新 DataStore（数据库事务之后，幂等安全）
    updateDataStore(fixedTypeId)
}
```

注意：`typeDao.migrateTypeRecords()` 需要在 Task 5 中添加。

- [ ] **Step 7: 验证编译通过**

Run: `./gradlew :core:data:compileDebugKotlin`
Expected: 编译错误（TypeDao 缺少 migrateTypeRecords 方法，FakeTypeRepository 缺少移除的方法）——这是预期的，将在后续 Task 中修复。

---

### Task 5: TypeDao 新增迁移辅助查询 + TransactionDao 新增迁移事务方法

**Files:**
- Modify: `core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/dao/TypeDao.kt`
- Modify: `core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/dao/TransactionDao.kt`

注意：Room DAO interface 中不能有带方法体的 `@Transaction` 方法（只有 abstract class 可以）。`TransactionDao` 已经是 abstract class，所以事务方法放在这里。

- [ ] **Step 1: TypeDao 添加辅助 SQL 方法**

```kotlin
@Query("UPDATE db_record SET type_id = :newTypeId WHERE type_id = :oldTypeId")
suspend fun updateRecordTypeId(oldTypeId: Long, newTypeId: Long)

@Query("UPDATE db_type SET parent_id = -1, type_level = 0 WHERE parent_id = :parentId")
suspend fun promoteChildTypes(parentId: Long)

@Query("SELECT COUNT(*) FROM db_record WHERE type_id = :typeId")
suspend fun countRecordsByTypeId(typeId: Long): Int
```

- [ ] **Step 2: TransactionDao 添加迁移事务方法**

在 `TransactionDao` 中添加（它是 abstract class，可以有带方法体的 @Transaction）：

```kotlin
/**
 * 将引用旧类型的记录迁移到新固定类型，并清理旧类型
 * 在一个事务中完成，保证原子性
 */
@Transaction
open suspend fun migrateTypeRecords(oldTypeId: Long, fixedTypeId: Long) {
    typeDao.updateRecordTypeId(oldTypeId, fixedTypeId)
    typeDao.promoteChildTypes(oldTypeId)
    val remainingCount = typeDao.countRecordsByTypeId(oldTypeId)
    if (remainingCount == 0) {
        typeDao.deleteById(oldTypeId)
    }
}
```

注意：TransactionDao 已注入了 TypeDao（检查构造函数，可能需要添加）。如果 TransactionDao 未持有 TypeDao 引用，需要在构造函数中添加。

- [ ] **Step 3: 更新 Task 4 中 TypeRepositoryImpl 的迁移方法**

将 `typeDao.migrateTypeRecords(...)` 改为 `transactionDao.migrateTypeRecords(...)`。TypeRepositoryImpl 需要注入 `TransactionDao`（检查是否已有，未有则添加构造函数参数）。

- [ ] **Step 4: 验证编译通过**

Run: `./gradlew :core:database:compileDebugKotlin && ./gradlew :core:data:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 提交**

```
git add core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/dao/TypeDao.kt
git add core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/dao/TransactionDao.kt
git add core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/TypeRepository.kt
git add core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/impl/TypeRepositoryImpl.kt
git commit -m "[refactor|core|data,database][公共]TypeRepository简化为固定ID比较，新增迁移事务方法"
```

---

### Task 6: CombineProtoDataSource 修改 needRelated

**Files:**
- Modify: `core/datastore/src/main/kotlin/cn/wj/android/cashbook/core/datastore/datasource/CombineProtoDataSource.kt:356-359`

- [ ] **Step 1: 修改 needRelated 为固定 ID 比较**

`CombineProtoDataSource` 在 `core/datastore` 模块中，不能依赖 `core/database`。但常量定义在 `core/common`（`Constants.kt`），可以直接引用：

```kotlin
import cn.wj.android.cashbook.core.common.FIXED_TYPE_ID_REFUND
import cn.wj.android.cashbook.core.common.FIXED_TYPE_ID_REIMBURSE

suspend fun needRelated(typeId: Long): Boolean {
    return typeId == FIXED_TYPE_ID_REFUND || typeId == FIXED_TYPE_ID_REIMBURSE
}
```

- [ ] **Step 2: 验证编译通过**

Run: `./gradlew :core:datastore:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```
git add core/datastore/src/main/kotlin/cn/wj/android/cashbook/core/datastore/datasource/CombineProtoDataSource.kt
git commit -m "[refactor|core|datastore][公共]CombineProtoDataSource.needRelated改为固定ID比较"
```

---

### Task 7: FakeTypeRepository 和 FakeTypeDao 更新

**Files:**
- Modify: `core/testing/src/main/kotlin/cn/wj/android/cashbook/core/testing/repository/FakeTypeRepository.kt`
- Modify: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/testdoubles/FakeTypeDao.kt`（如果存在 migrateTypeRecords 相关需要）

- [ ] **Step 1: FakeTypeRepository 移除 set*Type 方法和相关集合**

移除以下成员：
- `reimburseTypeSet`、`refundTypeSet`、`creditPaymentTypeSet`、`needRelatedSet` 字段
- `setNeedRelated()`、`setReimburse()`、`setRefund()`、`setCreditPayment()` 辅助方法
- `setReimburseType()`、`setRefundType()`、`setCreditPaymentType()` override 方法

- [ ] **Step 2: 修改 is*Type 和 needRelated 为固定 ID 比较**

```kotlin
import cn.wj.android.cashbook.core.database.table.TYPE_TABLE_REFUND
import cn.wj.android.cashbook.core.database.table.TYPE_TABLE_REIMBURSE
import cn.wj.android.cashbook.core.database.table.TYPE_TABLE_CREDIT_CARD_PAYMENT

override suspend fun needRelated(typeId: Long): Boolean {
    return typeId == TYPE_TABLE_REFUND.id || typeId == TYPE_TABLE_REIMBURSE.id
}

override suspend fun isReimburseType(typeId: Long): Boolean {
    return typeId == TYPE_TABLE_REIMBURSE.id
}

override suspend fun isRefundType(typeId: Long): Boolean {
    return typeId == TYPE_TABLE_REFUND.id
}

override suspend fun isCreditPaymentType(typeId: Long): Boolean {
    return typeId == TYPE_TABLE_CREDIT_CARD_PAYMENT.id
}
```

- [ ] **Step 3: deleteById 增加固定类型保护**

```kotlin
override suspend fun deleteById(id: Long) {
    require(id != TYPE_TABLE_REFUND.id && id != TYPE_TABLE_REIMBURSE.id && id != TYPE_TABLE_CREDIT_CARD_PAYMENT.id) {
        "Cannot delete fixed type: $id"
    }
    types.removeAll { it.id == id }
    updateFlows()
}
```

- [ ] **Step 4: 验证编译通过**

Run: `./gradlew :core:testing:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 提交**

```
git add core/testing/src/main/kotlin/cn/wj/android/cashbook/core/testing/repository/FakeTypeRepository.kt
git commit -m "[refactor|core|testing][公共]FakeTypeRepository适配固定类型变更"
```

---

### Task 8: ExpandableRecordTypeModel 移除特殊类型标记字段

**Files:**
- Modify: `feature/types/src/main/kotlin/cn/wj/android/cashbook/feature/types/model/ExpandableRecordTypeModel.kt`

- [ ] **Step 1: 移除三个布尔字段**

修改为：

```kotlin
data class ExpandableRecordTypeModel(
    val data: RecordTypeModel,
    val list: List<ExpandableRecordTypeModel>,
) {
    var expanded by mutableStateOf(false)
}
```

- [ ] **Step 2: 暂不编译（下一步 Task 同步修改所有引用后一起验证）**

---

### Task 9: MyCategoriesViewModel 移除 set*Type 方法和简化 uiState

**Files:**
- Modify: `feature/types/src/main/kotlin/cn/wj/android/cashbook/feature/types/viewmodel/MyCategoriesViewModel.kt`

- [ ] **Step 1: 移除三个 set 方法**

删除第 271-290 行的 `setReimburseType()`、`setRefundType()`、`setCreditCardPaymentType()` 方法。

- [ ] **Step 2: 简化 uiState 中 ExpandableRecordTypeModel 的构建**

移除 `reimburseType`、`refundType`、`creditCardPaymentType` 参数：

```kotlin
val uiState = combine(_dataVersion, _currentTypeList) { _, typeList ->
    val firstTypeList = typeList.map { first ->
        ExpandableRecordTypeModel(
            data = first,
            list = typeRepository.getSecondRecordTypeListByParentId(first.id)
                .map { second ->
                    ExpandableRecordTypeModel(
                        data = second,
                        list = emptyList(),
                    )
                },
        )
    }
    MyCategoriesUiState.Success(
        selectedTab = _selectedTabData.first(),
        typeList = firstTypeList,
    )
}
```

- [ ] **Step 3: 简化 requestDeleteType 中 DeleteType 对话框数据的构建**

移除 `ExpandableRecordTypeModel` 构建时的 `reimburseType`、`refundType`、`creditCardPaymentType` 参数：

```kotlin
// 在 requestDeleteType 中构建 DeleteType 时
.map { first ->
    ExpandableRecordTypeModel(
        data = first,
        list = typeRepository.getSecondRecordTypeListByParentId(first.id)
            .filter { it.id != id }
            .map { second ->
                ExpandableRecordTypeModel(
                    data = second,
                    list = emptyList(),
                )
            },
    )
}
```

- [ ] **Step 4: 暂不编译（UI 层修改在下一个 Task）**

---

### Task 10: MyCategoriesScreen UI 层变更

**Files:**
- Modify: `feature/types/src/main/kotlin/cn/wj/android/cashbook/feature/types/screen/MyCategoriesScreen.kt`

- [ ] **Step 1: 在 FirstTypeItem 中添加 protected 类型保护**

修改 `FirstTypeItem`，当 `first.data.protected` 为 true 时不显示操作菜单：

```kotlin
// 将 Modifier.clickable { expandedMenu = true } 改为：
modifier = Modifier.clickable {
    if (!first.data.protected) {
        expandedMenu = true
    }
},
```

- [ ] **Step 2: 移除 FirstTypeItem 中的"设置退款/报销/信用卡还款"菜单项**

删除第 701-758 行的两个 `if` 块（INCOME 的退款/报销菜单和 TRANSFER 的信用卡还款菜单）。

- [ ] **Step 3: 移除 FirstTypeItem 的 onRequestSet*Type 参数**

从 `FirstTypeItem` 函数签名中删除：
- `onRequestSetRefundType: (Long) -> Unit`
- `onRequestSetReimburseType: (Long) -> Unit`
- `onRequestSetCreditCardPaymentType: (Long) -> Unit`

- [ ] **Step 4: 在 SecondTypeItem 中添加 protected 类型保护**

同 FirstTypeItem，当 `second.data.protected` 为 true 时不显示菜单：

```kotlin
modifier = Modifier
    .fillMaxWidth()
    .padding(8.dp)
    .clickable {
        if (!second.data.protected) {
            expandedMenu = true
        }
    },
```

- [ ] **Step 5: 移除 SecondTypeItem 中的"设置退款/报销/信用卡还款"菜单项**

删除第 903-960 行的两个 `if` 块。

- [ ] **Step 6: 移除 SecondTypeItem 和 SecondTypeList 的 onRequestSet*Type 参数**

从 `SecondTypeItem` 和 `SecondTypeList` 函数签名中删除三个 `onRequestSet*Type` 参数。

- [ ] **Step 7: 更新所有调用点**

在 `MyCategoriesScreen` 的主 Composable 中，移除传递给 `FirstTypeItem`、`SecondTypeList` 的 `onRequestSet*Type` lambda 参数，以及从 `MyCategoriesViewModel` 调用的 `setReimburseType`、`setRefundType`、`setCreditCardPaymentType`。

- [ ] **Step 8: 验证编译通过**

Run: `./gradlew :feature:types:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 9: 提交**

```
git add feature/types/src/main/kotlin/cn/wj/android/cashbook/feature/types/
git commit -m "[refactor|feature|types][公共]移除特殊类型设置菜单，protected类型只读展示"
```

---

### Task 11: 更新测试 - MyCategoriesViewModelTest

**Files:**
- Modify: `feature/types/src/test/kotlin/cn/wj/android/cashbook/feature/types/viewmodel/MyCategoriesViewModelTest.kt`

- [ ] **Step 1: 删除三个 set*Type 测试**

删除以下测试方法（第 466-508 行）：
- `when_set_reimburse_type_then_type_updated()`
- `when_set_refund_type_then_type_updated()`
- `when_set_credit_card_payment_type_then_type_updated()`

- [ ] **Step 2: 运行测试**

Run: `./gradlew :feature:types:testDebugUnitTest`
Expected: 所有测试通过（19 个，减少了 3 个）

- [ ] **Step 3: 提交**

```
git add feature/types/src/test/kotlin/cn/wj/android/cashbook/feature/types/viewmodel/MyCategoriesViewModelTest.kt
git commit -m "[test|feature|types][公共]移除已废弃的set*Type测试用例"
```

---

### Task 12: 更新其他调用 FakeTypeRepository 辅助方法的测试

**Files:**
- Modify: `core/domain/src/test/kotlin/cn/wj/android/cashbook/domain/usecase/SaveRecordUseCaseTest.kt`
- Modify: `core/domain/src/test/kotlin/cn/wj/android/cashbook/domain/usecase/GetRecordTypeListUseCaseTest.kt`
- Modify: `core/domain/src/test/kotlin/cn/wj/android/cashbook/domain/usecase/GetAssetListUseCaseTest.kt`
- Modify: `core/domain/src/test/kotlin/cn/wj/android/cashbook/domain/usecase/GetRelatedRecordViewsUseCaseTest.kt`
- Modify: `feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/SelectRelatedRecordViewModelTest.kt`
- Modify: `feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/EditRecordViewModelTest.kt`
- Modify: `feature/assets/src/test/kotlin/cn/wj/android/cashbook/feature/assets/viewmodel/EditRecordSelectAssetBottomSheetViewModelTest.kt`
- Modify: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/repository/impl/TypeRepositoryImplTest.kt`

FakeTypeRepository 的 `setReimburse()`、`setRefund()`、`setCreditPayment()`、`setNeedRelated()` 辅助方法已移除。这些测试需要改为直接使用固定 ID：

- [ ] **Step 1: 更新测试中的类型 ID**

在每个测试文件中，将以下调用模式：
```kotlin
typeRepository.setNeedRelated(1L)  // 改为使用 FIXED_TYPE_ID_REFUND 作为 typeId
typeRepository.setReimburse(10L)   // 改为在测试数据中使用 FIXED_TYPE_ID_REIMBURSE 作为 typeId
typeRepository.setRefund(11L)      // 改为在测试数据中使用 FIXED_TYPE_ID_REFUND 作为 typeId
typeRepository.setCreditPayment(100L) // 改为在测试数据中使用 FIXED_TYPE_ID_CREDIT_CARD_PAYMENT 作为 typeId
```

替换为：在 `createRecordTypeModel()` 和 `createRecordModel()` 中直接使用固定 ID，FakeTypeRepository 的 `needRelated()`/`isRefundType()` 等方法会自动匹配。

- [ ] **Step 2: 更新 TypeRepositoryImplTest 中的 set*Type 测试**

`TypeRepositoryImplTest` 中可能有 `setCreditPaymentType` 等测试用例，这些需要删除。

- [ ] **Step 3: 运行全部受影响模块的测试**

Run: `./gradlew :core:domain:testDebugUnitTest :feature:records:testDebugUnitTest :feature:assets:testDebugUnitTest :core:data:testDebugUnitTest`
Expected: 所有测试通过

- [ ] **Step 4: 提交**

```
git add core/domain/src/test/ feature/records/src/test/ feature/assets/src/test/ core/data/src/test/
git commit -m "[test|all|all][公共]适配固定类型变更，移除setNeedRelated/setReimburse/setRefund/setCreditPayment调用"
```

---

### Task 13: 更新测试 - TransactionDaoLogicTest 使用固定 ID

**Files:**
- Modify: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/testdoubles/TransactionDaoLogicTest.kt`

- [ ] **Step 1: 检查并更新涉及报销/退款场景的测试数据**

搜索测试中使用的 typeId，如果涉及报销/退款相关场景（needRelated=true 的记录），将 typeId 替换为固定常量 `TYPE_TABLE_REFUND.id` 或 `TYPE_TABLE_REIMBURSE.id`。

- [ ] **Step 2: 运行测试**

Run: `./gradlew :core:data:testDebugUnitTest`
Expected: 所有测试通过

- [ ] **Step 3: 提交**

```
git add core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/testdoubles/TransactionDaoLogicTest.kt
git commit -m "[test|core|data][公共]TransactionDaoLogicTest适配固定类型ID"
```

---

### Task 14: 新增迁移逻辑测试

**Files:**
- Modify: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/testdoubles/FakeTypeDao.kt`
- Create: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/repository/TypeMigrationTest.kt`

- [ ] **Step 1: FakeTypeDao 添加迁移相关方法**

在 `FakeTypeDao` 中实现新增的方法：

```kotlin
// 需要额外持有 record 数据用于模拟迁移
private val records = mutableListOf<FakeRecord>()

data class FakeRecord(val id: Long, var typeId: Long)

fun addRecord(id: Long, typeId: Long) {
    records.add(FakeRecord(id, typeId))
}

override suspend fun updateRecordTypeId(oldTypeId: Long, newTypeId: Long) {
    records.filter { it.typeId == oldTypeId }.forEach { it.typeId = newTypeId }
}

override suspend fun promoteChildTypes(parentId: Long) {
    // 在 FakeTypeDao 的 types 列表中更新
    val typesFlow = _types.value.toMutableList()
    typesFlow.forEachIndexed { index, table ->
        if (table.parentId == parentId) {
            typesFlow[index] = table.copy(parentId = -1L, typeLevel = 0)
        }
    }
    _types.value = typesFlow
}

override suspend fun countRecordsByTypeId(typeId: Long): Int {
    return records.count { it.typeId == typeId }
}

```

注意：`migrateTypeRecords` 在 `TransactionDao` 中，需要为 `FakeTransactionDao` 也添加对应的实现（或通过 FakeTypeDao 的辅助方法组合实现）。

- [ ] **Step 2: 编写迁移测试**

```kotlin
class TypeMigrationTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    // 测试用例：
    // 1. 旧 typeId 存在 → 记录迁移到固定 ID，旧类型无引用时被删除
    // 2. 旧 typeId 存在 → 旧类型仍有其他记录引用时保留为普通类型
    // 3. DataStore 中无记录 → 按名称查找旧类型并迁移
    // 4. 已迁移完成 → 跳过不重复执行
    // 5. 旧类型有子类型 → 子类型提升为一级
}
```

具体测试实现取决于 `TypeRepositoryImpl.migrateSpecialTypes()` 的最终接口形态。核心断言：
- `when_old_type_exists_and_no_other_references_then_old_type_deleted`
- `when_old_type_exists_and_has_other_references_then_old_type_kept`
- `when_datastore_empty_then_find_by_name_and_migrate`
- `when_already_migrated_then_skip`
- `when_old_type_has_children_then_children_promoted`

- [ ] **Step 3: 运行测试**

Run: `./gradlew :core:data:testDebugUnitTest`
Expected: 所有测试通过

- [ ] **Step 4: 提交**

```
git add core/data/src/test/kotlin/
git commit -m "[test|core|data][公共]新增特殊类型迁移逻辑测试"
```

---

### Task 15: 新增启动触发点调用 migrateSpecialTypes()

**Files:**
- Modify: `sync/work/src/main/kotlin/cn/wj/android/cashbook/sync/workers/InitWorker.kt`
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/TypeRepository.kt`

`InitWorker` 是应用启动时执行的 Worker，在其 `doWork()` 中添加迁移调用。

- [ ] **Step 1: 在 TypeRepository 接口中添加迁移方法**

```kotlin
suspend fun migrateSpecialTypes()
```

- [ ] **Step 2: 在 InitWorker 构造函数中注入 TypeRepository**

```kotlin
@HiltWorker
class InitWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val settingRepository: SettingRepository,
    private val typeRepository: TypeRepository,  // 新增
    @Dispatcher(CashbookDispatchers.IO) private val ioDispatcher: CoroutineContext,
) : CoroutineWorker(appContext, workerParams) {
```

- [ ] **Step 3: 在 doWork() 开头调用迁移**

```kotlin
override suspend fun doWork(): Result = withContext(ioDispatcher) {
    this@InitWorker.logger().i("doWork(), init worker")

    // 一次性迁移特殊类型
    typeRepository.migrateSpecialTypes()

    settingRepository.appSettingsModel.first().let { appDateModel ->
        // ... 现有逻辑不变
    }
    Result.success()
}
```

- [ ] **Step 4: 验证编译通过**

Run: `./gradlew :sync:work:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 提交**

```
git add sync/work/src/main/kotlin/cn/wj/android/cashbook/sync/workers/InitWorker.kt
git add core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/TypeRepository.kt
git commit -m "[feat|sync|work][公共]应用启动时触发特殊类型迁移"
```

---

### Task 16: BackupRecoveryManagerImpl 恢复后重置迁移标志

**Files:**
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/uitl/impl/BackupRecoveryManagerImpl.kt`

- [ ] **Step 1: 在 startRecovery 成功后插入固定类型并重置 DataStore**

找到 `recoveryFromDb` 调用成功后的分支（约第 602-603 行），在返回成功之前添加：

```kotlin
if (DatabaseMigrations.recoveryFromDb(backupDatabase, currentDatabase)) {
    // 确保固定类型行存在（旧版备份可能不包含）
    currentDatabase.execSQL(
        "INSERT OR IGNORE INTO db_type (id, parent_id, name, icon_name, type_level, type_category, protected, sort) VALUES (-2001, -1, '退款', 'vector_refund', 0, 1, 1, 0)",
    )
    currentDatabase.execSQL(
        "INSERT OR IGNORE INTO db_type (id, parent_id, name, icon_name, type_level, type_category, protected, sort) VALUES (-2002, -1, '报销', 'vector_reimburse', 0, 1, 1, 0)",
    )
    currentDatabase.execSQL(
        "INSERT OR IGNORE INTO db_type (id, parent_id, name, icon_name, type_level, type_category, protected, sort) VALUES (-2003, -1, '信用卡还款', 'vector_credit_card_payment', 0, 2, 1, 0)",
    )
    // 重置迁移标志，下次启动时自动触发应用层迁移
    combineProtoDataSource.updateRefundTypeId(0L)
    combineProtoDataSource.updateReimburseTypeId(0L)
    combineProtoDataSource.updateCreditCardPaymentTypeId(0L)
    BackupRecoveryState.SUCCESS_RECOVERY
} else {
    BackupRecoveryState.FAILED_BACKUP_PATH_UNAUTHORIZED
}
```

注意：`BackupRecoveryManagerImpl` 需要注入 `CombineProtoDataSource`（检查是否已有，如果没有需要添加构造函数参数）。

- [ ] **Step 2: 验证编译通过**

Run: `./gradlew :core:data:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```
git add core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/uitl/impl/BackupRecoveryManagerImpl.kt
git commit -m "[fix|core|data][公共]备份恢复后确保固定类型存在并重置迁移标志"
```

---

### Task 17: 全量测试验证

**Files:** 无新增/修改

- [ ] **Step 1: 运行全部单元测试**

Run: `./gradlew testDebugUnitTest`
Expected: 所有测试通过

- [ ] **Step 2: 运行 Spotless 格式检查**

Run: `./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache`
Expected: 格式化完成

- [ ] **Step 3: 运行 Lint 检查**

Run: `./gradlew :app:lintOnlineRelease :app:lintOfflineRelease :app:lintDevRelease :lint:lint -Dlint.baselines.continue=true`
Expected: 无新增错误

- [ ] **Step 4: 最终提交（如果有格式化修改）**

```
git add -A
git commit -m "[chore|all|all][公共]代码格式化"
```

---

### Task 18: 更新预置数据库 cashbook_init.db（手动步骤）

**Files:**
- Modify: `core/database/src/main/assets/cashbook_init.db`

- [ ] **Step 1: 使用 SQLite 工具打开预置数据库**

```bash
sqlite3 core/database/src/main/assets/cashbook_init.db
```

- [ ] **Step 2: 插入三条固定类型**

```sql
INSERT OR IGNORE INTO db_type (id, parent_id, name, icon_name, type_level, type_category, protected, sort)
VALUES (-2001, -1, '退款', 'vector_refund', 0, 1, 1, 0);

INSERT OR IGNORE INTO db_type (id, parent_id, name, icon_name, type_level, type_category, protected, sort)
VALUES (-2002, -1, '报销', 'vector_reimburse', 0, 1, 1, 0);

INSERT OR IGNORE INTO db_type (id, parent_id, name, icon_name, type_level, type_category, protected, sort)
VALUES (-2003, -1, '信用卡还款', 'vector_credit_card_payment', 0, 2, 1, 0);
```

- [ ] **Step 3: 验证插入成功**

```sql
SELECT * FROM db_type WHERE id < 0;
```

Expected: 看到三条新记录。

- [ ] **Step 4: 提交**

```
git add core/database/src/main/assets/cashbook_init.db
git commit -m "[feat|core|database][公共]预置数据库添加固定特殊类型"
```
