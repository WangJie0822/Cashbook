# 固化报销、退款、信用卡还款特殊类型设计

## 背景

当前报销、退款、信用卡还款三个特殊类型由用户在"我的类型"界面手动标记，类型 ID 存储在 DataStore 的 `RecordSettingsModel` 中。用户可以修改、删除这些类型，甚至将任意类型设为特殊类型，导致数据不一致风险。

## 目标

1. 将退款、报销、信用卡还款固化为系统内置类型，不允许用户修改删除
2. 移除类型管理中的"设置报销/退款/信用卡还款"菜单
3. 兼容旧版本中用户已修改过类型的场景

## 设计决策

| 决策 | 选择 |
|------|------|
| 固化范围 | 退款、报销、信用卡还款三个特殊类型全部固化 |
| 存储方式 | `db_type` 表 + 固定 ID + `protected=1` |
| 旧数据兼容 | 替换策略：插入固定类型，迁移旧记录引用，清理旧类型 |
| 固定 ID 范围 | 负数 ID（`-2001`、`-2002`、`-2003`） |
| UI 行为 | 固定类型在列表中只读展示，不提供任何操作菜单 |
| 类型层级 | 作为一级类型，不挂在任何父类下 |
| 数据库版本 | 合并到 v12 迁移（未发布） |
| 迁移方式 | 两阶段：Migration SQL 插入固定类型 + 应用层首次启动迁移旧记录 |

## 第一部分：固定类型定义

在 `TypeTable.kt` 中新增三个固定类型常量：

```kotlin
/** 固定类型 - 退款（收入） */
val TYPE_TABLE_REFUND: TypeTable
    get() = TypeTable(
        id = -2001L,
        parentId = -1L,
        name = "退款",
        iconName = "vector_refund",
        typeLevel = TypeLevelEnum.FIRST.code,
        typeCategory = RecordTypeCategoryEnum.INCOME.code,
        protected = SWITCH_INT_ON,
        sort = 0,
    )

/** 固定类型 - 报销（收入） */
val TYPE_TABLE_REIMBURSE: TypeTable
    get() = TypeTable(
        id = -2002L,
        parentId = -1L,
        name = "报销",
        iconName = "vector_reimburse",
        typeLevel = TypeLevelEnum.FIRST.code,
        typeCategory = RecordTypeCategoryEnum.INCOME.code,
        protected = SWITCH_INT_ON,
        sort = 0,
    )

/** 固定类型 - 信用卡还款（转账） */
val TYPE_TABLE_CREDIT_CARD_PAYMENT: TypeTable
    get() = TypeTable(
        id = -2003L,
        parentId = -1L,
        name = "信用卡还款",
        iconName = "vector_credit_card_payment",
        typeLevel = TypeLevelEnum.FIRST.code,
        typeCategory = RecordTypeCategoryEnum.TRANSFER.code,
        protected = SWITCH_INT_ON,
        sort = 0,
    )
```

关键点：
- 负数 ID 不与 autoincrement 冲突
- `protected = 1`，UI 层据此禁止编辑删除
- 一级类型（`parentId = -1`，`typeLevel = FIRST`）
- `needRelated` 由代码逻辑判断（退款/报销 → true）

## 第二部分：数据库迁移（合并到 Migration11To12）

在现有 `Migration11To12` 中追加 SQL：

```sql
INSERT OR IGNORE INTO db_type (id, parent_id, name, icon_name, type_level, type_category, protected, sort)
VALUES (-2001, -1, '退款', 'vector_refund', 0, 1, 1, 0);

INSERT OR IGNORE INTO db_type (id, parent_id, name, icon_name, type_level, type_category, protected, sort)
VALUES (-2002, -1, '报销', 'vector_reimburse', 0, 1, 1, 0);

INSERT OR IGNORE INTO db_type (id, parent_id, name, icon_name, type_level, type_category, protected, sort)
VALUES (-2003, -1, '信用卡还款', 'vector_credit_card_payment', 0, 2, 1, 0);
```

同时更新预置数据库 `cashbook_init.db`，新安装用户直接包含这三条记录。

## 第三部分：应用层记录迁移（首次启动）

在 `TypeRepositoryImpl` 中新增一次性迁移逻辑。

### 触发条件

应用启动时检查 DataStore 中的 typeId 状态：如果 `refundTypeId != -2001L` 或 `reimburseTypeId != -2002L` 或 `creditCardPaymentTypeId != -2003L`，则执行迁移。

### 迁移流程（以退款为例，报销和信用卡还款同理）

```
1. 从 DataStore 读取旧的 refundTypeId（如 42L）
2. 如果 42L == -2001L → 已迁移，跳过
3. 如果 42L > 0：
   a. 将 db_record 中 typeId=42 的记录批量更新为 typeId=-2001
   b. 如果旧类型是一级类型且有子类型（parentId=42 的记录），将这些子类型提升为一级（parentId=-1, typeLevel=0）
   c. 检查 db_type 中 id=42 是否还有其他记录引用
      - 无引用 → 删除该类型
      - 有引用 → 保留为普通类型（protected=0）
4. 更新 DataStore：refundTypeId = -2001L
```

### DataStore 中无记录的情况（refundTypeId == 0）

- 按名称 + 分类查找旧类型：
  - 退款：`name="退款" AND typeCategory=1`
  - 报销：`name="报销" AND typeCategory=1`
  - 信用卡还款：`name="还信用卡" AND typeCategory=2`（注意旧版名称为"还信用卡"，非"信用卡还款"）
- 找到则执行相同的迁移流程
- 未找到则直接设置 DataStore 为固定 ID

### 迁移完成标志

DataStore 中三个 typeId 都变为对应的负数固定 ID 后，后续启动不再执行迁移。

### 幂等性与崩溃安全

迁移涉及两个存储系统：数据库和 DataStore，无法在一个事务中保证原子性。设计为幂等操作：

1. **数据库操作**在事务中完成（UPDATE 记录、处理子类型、DELETE/保留旧类型）
2. **DataStore 更新**在数据库事务成功后执行
3. 如果 DataStore 写入前崩溃：重启后检测到 DataStore 中的旧值，重新执行迁移——由于记录已迁移完成，UPDATE 语句匹配 0 行，等效于空操作，然后更新 DataStore，安全完成

### 备份恢复兼容

备份恢复（`BackupRecoveryManagerImpl`）完成后，DataStore 状态可能不一致，且恢复的旧版备份可能不包含固定类型行。处理方式：

1. 恢复完成后，先用 `INSERT OR IGNORE` 确保三条固定类型行（`-2001`、`-2002`、`-2003`）存在于 `db_type` 表中
2. 重置 DataStore 迁移标志（将三个 typeId 设为 0）
3. 应用层迁移在下次启动时自动触发，完成旧记录引用的更新

## 第四部分：TypeRepository 简化

### 移除的内容

- `_refundTypeId`、`_reimburseTypeId`、`_creditCardPaymentTypeId` 缓存字段
- `getRefundTypeId()`、`getReimburseTypeId()`、`getCreditCardPaymentTypeId()` 私有方法
- `setReimburseType()`、`setRefundType()`、`setCreditPaymentType()` 接口方法及实现

### 简化后的判断逻辑

```kotlin
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

### CombineProtoDataSource 同步修改

`CombineProtoDataSource` 中的 `needRelated()` 方法也需同步修改为固定 ID 比较，不再从 DataStore 读取。

### DataStore 字段处理

`RecordSettingsModel` 中的 `refundTypeId`、`reimburseTypeId`、`creditCardPaymentTypeId` 保留作为迁移标志，不再用于业务逻辑。计划在下一个大版本中移除。

## 第五部分：UI 层变更

### MyCategoriesScreen

- 移除收入类型项的"设置退款"/"设置报销"菜单项
- 移除转账类型项的"设置信用卡还款"菜单项
- `protected == true` 的类型不显示任何操作菜单，只读展示

### MyCategoriesViewModel

- 移除 `setReimburseType()`、`setRefundType()`、`setCreditCardPaymentType()` 方法

### ExpandableRecordTypeModel

- 移除 `reimburseType`、`refundType`、`creditCardPaymentType` 布尔字段

### EditRecordTypeListScreen

- 无变化，固定类型正常出现在类型选择列表中

## 第六部分：TransactionDao 与 RecordDao 的影响

### TransactionDao

- 核心逻辑不变：`insertRecordTransaction()`、`deleteRecordTransaction()`、关联记录机制、`finalAmount` 重算
- `resolveType()` 无需修改：固定类型在 `db_type` 表中，正常查库即可

### 类型删除保护

在 `TypeRepository.deleteById()` 中增加防御性校验：固定 ID 拒绝删除。

## 第七部分：测试影响

### 需要修改的测试

- **FakeTypeRepository**：移除 set*Type 实现，is*Type/needRelated 改为固定 ID 比较
- **MyCategoriesViewModelTest**：移除"设置特殊类型"相关测试，新增 protected 类型不可操作测试
- **TransactionDaoLogicTest**：涉及报销/退款场景的测试数据改用固定 ID

### 需要新增的测试

- **记录迁移逻辑测试**：
  - 旧 typeId 存在 → 记录迁移到固定 ID，旧类型无引用时被删除
  - 旧 typeId 存在 → 旧类型仍有其他记录引用时保留为普通类型
  - DataStore 中无记录 → 按名称查找旧类型并迁移
  - 已迁移完成 → 跳过不重复执行
- **TypeRepository 简化后的测试**：
  - `isReimburseType(-2002L)` → true
  - `isRefundType(-2001L)` → true
  - `needRelated(-2001L)` → true
  - `deleteById(-2001L)` → 拒绝删除

## 变更范围总结

| 模块 | 变更 |
|------|------|
| **TypeTable** | 新增 `TYPE_TABLE_REFUND(-2001)`、`TYPE_TABLE_REIMBURSE(-2002)`、`TYPE_TABLE_CREDIT_CARD_PAYMENT(-2003)` 常量 |
| **Migration11To12** | 追加 3 条 INSERT SQL |
| **cashbook_init.db** | 预置 3 条固定类型 |
| **TypeRepositoryImpl** | 新增一次性迁移逻辑；简化特殊类型判断为固定 ID 比较；移除 set*Type 方法和缓存 |
| **TypeRepository 接口** | 移除 `setReimburseType`、`setRefundType`、`setCreditPaymentType` |
| **MyCategoriesViewModel** | 移除 set*Type 方法 |
| **MyCategoriesScreen** | 移除"设置退款/报销/信用卡还款"菜单；protected 类型不显示操作菜单 |
| **ExpandableRecordTypeModel** | 移除 `reimburseType`、`refundType`、`creditCardPaymentType` 字段 |
| **CombineProtoDataSource** | `needRelated()` 改为固定 ID 比较 |
| **BackupRecoveryManagerImpl** | 恢复完成后重置迁移标志 |
| **测试** | 修改 Fake、ViewModel 测试；新增迁移逻辑测试 |
