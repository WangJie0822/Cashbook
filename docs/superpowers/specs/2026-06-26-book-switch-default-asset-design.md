# 设计：切换账本后默认资产按账本隔离

- 日期：2026-06-26
- 状态：已通过节点 1 四维评审，待用户审阅
- 子项目：三项改进之一（任务 1，顺序 1→3→2，共用 worktree `worktree-asset-backup-image-improvements`）

## 问题

切换账本后，新建记录时默认带出的资产是**另一个账本**的资产。

### 根因（实证）

- `core/data/.../repository/impl/RecordRepositoryImpl.kt:379` `getDefaultRecord` 用全局 `lastAssetId` 作为新建记录的默认 `assetId`。
- `lastAssetId` 是 `core/datastore-proto/src/main/proto/record_settings.proto:10` 中的**单个全局字段**，不按账本隔离；记录写库时由 `RecordRepositoryImpl.kt:100`（位于 `updateRecord`）的 `updateLastAssetId(record.assetId)` 刷新。
- 但资产是**按账本隔离**的：`AssetRepositoryImpl.kt:51-54` `currentVisibleAssetListData` 按 `currentBookId` 过滤；`AssetTable.booksId`。
- 显示侧 `EditRecordViewModel.kt:158` 用 `getAssetById(record.assetId)`（`AssetRepositoryImpl.kt:111`）**不校验账本归属**，于是切账本后原样显示上一个账本的资产。

## 目标与期望行为

- 新建记录的默认资产 = **当前账本中最近创建的、且资产仍存在、可见、属于本账本**的那条记录的资产；取不到则 `-1L`（未选择，用户手动选）。
- 不再串到其他账本的资产。
- 满足"每个账本各自记住上次资产"（用户选定）。

## 方案 A（选定）：从记录派生，零存储改动

不再读全局 `lastAssetId`，改为查询当前账本最近一条"有效可见在册资产"的记录的 `asset_id`。天然按账本隔离、永不串号、自愈（删记录/删资产后自动回退），且无 proto/DB 迁移。

> 备选方案 B（按账本存 proto map）改动面更大、需防悬空、删账本残留条目；方案 C（仅校验不串号）不满足"按账本记忆"。均已排除，详见会话记录。

## 详细设计

### 1. 新增 DAO 查询（core/database `RecordDao`）

```kotlin
@Query(
    """
    SELECT asset_id FROM db_record
    WHERE books_id = :booksId
      AND asset_id IN (
        SELECT id FROM db_asset
        WHERE books_id = :booksId AND invisible = $SWITCH_INT_OFF
      )
    ORDER BY id DESC LIMIT 1
    """,
)
suspend fun queryLastUsedAssetId(booksId: Long): Long?
```

- 返回当前账本中、资产仍存在且可见且属本账本的**最近创建**记录的 `asset_id`；无则 `null`。
- 子查询同时保证：不串号（`db_asset.books_id` 二次过滤）、不悬空（已删资产不在 `db_asset`）、不带出隐藏资产（`invisible = SWITCH_INT_OFF`，与 `AssetDao.queryVisibleAssetByBookId` 一致）。
- `asset_id = -1`（无资产记录）天然不命中子查询（资产 id 从 1 自增），被跳过。
- 实证依据：Room 子查询（`RecordDao.kt:49/174/383` 已有同构用法）、可空标量 `Long?`（`RecordDao.kt:558` `queryEarliestRecordTime`）、`id` 自增主键（`RecordTable.kt:56`）、列名（`ColumnNames.kt:20-22,46-51`）、`SWITCH_INT_OFF` 可见哨兵（`AssetDao.kt:44`）。

### 2. 修改 getDefaultRecord（core/data `RecordRepositoryImpl`）

`RecordRepositoryImpl.kt:379`：

```kotlin
// 旧：assetId = appDataModel.lastAssetId,
assetId = recordDao.queryLastUsedAssetId(appDataModel.currentBookId) ?: -1L,
```

### 3. 移除死写（core/data `RecordRepositoryImpl`）

- 删除 `updateRecord` 中 `RecordRepositoryImpl.kt:100` 的 `combineProtoDataSource.updateLastAssetId(record.assetId)`——`getDefaultRecord` 改造后无人再读 `lastAssetId`。
- proto 字段 `record_settings.proto:10 lastAssetId` 保留为 legacy（**不复用字段号**），加注释标注已弃用。
- `CombineProtoDataSource.updateLastAssetId`（`:231`）、proto→model 映射（`:105`）、`splitAppPreferences` 拷贝（`:184`）、`RecordSettingsModel.lastAssetId` 一律保留不动（仅搬运/持久化，不构成语义消费），避免扩大改动面。`updateLastAssetId` 成为未被生产调用的 legacy 方法，加注释说明。

### 4. 同步测试替身（core/data test `FakeRecordDao`）

- `FakeRecordDao`（`:34 class FakeRecordDao : RecordDao`）必须实现 `queryLastUsedAssetId`，否则 `:core:data:compileDebugUnitTestKotlin` 失败（DAO 新增抽象方法约定）。
- 当前 `FakeRecordDao` 无 `assets` 集合（仅 records/relatedRecords/images/types/tagWithRecords）。**新增 `assets: MutableList<AssetTable>` 集合**，并忠实复刻查询语义：`records.filter { booksId 匹配 && assetId 命中"本账本可见在册资产 id 集" }.maxByOrNull { id }?.assetId`。禁止用 `emptyList`/`-1L`/忽略资产归属的宽松桩（CLAUDE.md 测试替身忠实复刻约定，防假阳性）。

## 语义决策（显式固定，防后续争议）

1. **"最近"= 最近创建**（`ORDER BY id DESC`，`id` 为自增主键 = 插入顺序）。非"record_time 最近"、非"最后编辑"。批量导入/恢复后默认资产会变为最后插入行的资产——**接受为已知语义**，结果仍是本账本有效资产，不引入额外机制规避。
2. **跳过无资产记录**：若最近一条记录无资产（`asset_id=-1`），回溯到更早的有效资产记录；都没有则 `-1L`。倾向"给一个有效可用默认"优于"默认无资产"。
3. **无记录默认 `-1L`**（旧逻辑为 proto-0，0 非合法资产 id）——更符合"未选择"约定，属改进。
4. 转账记录取 `asset_id`（转出资产），与旧 `updateLastAssetId(record.assetId)` 写入一致，无新回归；`into_asset_id`（转入）不参与判定。

## 可选改进（低风险，可否决）

`EditRecordViewModel.kt:105` `_defaultRecordData` 是未共享冷流，被 `_displayRecordData`/`defaultTypeIdData`/`selectedTypeCategoryData` 三处独立收集，每次开记账页会触发 3× `getDefaultRecordUseCase`（旧实现读内存、廉价；新实现走 DB 查询，放大 3×）。

可选：`_defaultRecordData` 末尾加 `.shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)`，三处下游共享一次计算，去重 + 去抖（避免三次收集间落入写入致默认值不确定）。属"改进正在动的代码"，但触碰热路径，**默认采纳、用户可否决**。若否决则接受 3× 廉价查询（LIMIT 1 + `books_id` 索引，开销极小）。

## 边界与非回归（评审核验）

- 编辑已有记录不受影响：`GetDefaultRecordUseCase.kt:43` 对 `recordId` 先走 `queryById` 短路，仅新建（id=-1）走 `getDefaultRecord`。
- `initAssetId`（从资产详情带预选资产进入）仍显式覆盖 `assetId`，不受影响（`EditRecordViewModel.kt:301-312`）。
- `SaveAssetUseCase.kt:78` `getDefaultRecord(typeId).copy(assetId = assetModel.id, ...)` 立即覆盖 assetId，自动记账不受默认值变化影响。
- 无 schema 变更：纯 `@Query` SELECT，不触 `@Entity`，**无需 migration / schema JSON / version bump**。
- 索引：未加 `(books_id, id)` 覆盖索引（避免 migration 扩面）；LIMIT 1 + 最近记录通常有资产 → 实际近 O(1)，最坏（账本开头多条无资产记录）反向扫描，记录在案。
- 向后兼容：旧版本写入的 proto `lastAssetId` 残留值、备份恢复随 proto blob 还原的值，因无人读取而无害。

## 测试计划

1. **`RecordDaoTest`（core/database androidTest，真 Room）**——`queryLastUsedAssetId` 判别性用例（该 SQL 唯一可信验证层）：
   - 只返回当前账本、排除其他账本资产；
   - 跳过已删资产（asset 不在 db_asset）→ 回退更早记录；
   - 跳过隐藏资产（invisible）→ 回退更早记录；
   - 跳过 `asset_id=-1` 无资产记录；
   - 空账本返回 `null`；
   - 多条记录返回最大 id（最近创建）那条的 asset。
2. **`RecordRepositoryImplTest`（core/data JVM，经忠实 FakeRecordDao）**——`getDefaultRecord` 返回 assetId 按账本隔离；无记录返回 -1L。
3. **回归**：`GetDefaultRecordUseCaseTest`/`EditRecordViewModelTest` 走 `FakeRecordRepository`（`getDefaultRecord` 仍硬编码 -1L，:287，不感知派生逻辑），保持不变；若需在 domain/feature 层断言派生行为再增强 Fake，本任务暂不改。

## 不在本任务范围

- 任务 2（备份纳入设置项）、任务 3（图片存储调研）——各自独立 spec。

## 节点 1 四维评审 finding 处置

| 维度 | 严重度 | finding | 处置 |
|---|---|---|---|
| security | — | 无 blocking（参数绑定无注入；双重 books_id 反修复跨账本泄露；不触敏感字段） | — |
| feasibility | High | FakeRecordDao 未同步新方法→编译失败 | 采纳（§4 加 assets + 忠实实现） |
| feasibility/impact | Medium | 新行为 JVM 零覆盖、FakeRecordRepository 硬编码 -1L | 采纳（§测试计划 1+2） |
| reverse | Medium→Low | ORDER BY id DESC 导入/恢复后跳变 | 接受为语义（§语义决策 1） |
| reverse | Medium | 跳过无资产记录可能回溯 | 采纳为显式语义（§语义决策 2） |
| reverse | Medium | _defaultRecordData 冷流 3× 查询 | 采纳（可选，§可选改进） |
| reverse | Medium→Low | 无覆盖索引最坏反向扫描 | 接受（§边界，不加索引避免迁移扩面） |
| reverse | Low | 隐藏资产被带出 | 采纳（§1 加 invisible 过滤） |
| feasibility/impact | Low | 文案 saveRecord 实为 updateRecord:100 | 采纳（已更正全文） |
| reverse | Low | lastAssetId 成 vestigial | 采纳（§3 移除死写 + proto legacy 注释） |
