# Cashbook 分类 / 搜索 / 资产 四项体验改进 — 设计

> 状态：**v2（已过 agent-teams:team-review 四维评审 + controller 独立核验，待用户最终确认）**
> 日期：2026-05-29
> 范围：1 个 spec，4 个小节；因文件共享，writing-plans 阶段拆 **2 组串行 phase**（②→④ 共享 RecordDao/RecordRepositoryImpl；①→③ 共享 MyCategoriesScreen）。

## 决策摘要（已与用户对齐）

| # | 诉求 | 类型 | 已定方向 |
|---|------|------|----------|
| ① | 受保护类型（报销/退款/还信用卡）点击无统计弹窗 | Bug | 点击仍弹菜单，但受保护类型只保留"统计数据"项 |
| ② | 搜索按金额搜不到 | Bug | 统一搜索框：数字按金额（元→分）匹配 **amount OR finalAmount 任一命中**；支持小数 |
| ③ | 分类不支持排序 | Feature | 仅一级分类长按拖动排序；管理页与记账选择器均按 sort 显示 |
| ④ | 资产详情显示全部记录 | Feature | 替换为按月视图 + 按日分组 + 收入/支出/结余统计，采用**资产余额口径** |

## 通用约束

- **无需 DB schema 迁移**：`db_type.sort` 字段已存在（`TypeTable.kt:62`、schema 12）；按月查询复用现有 `record_time`。
- ③ 若引入第三方库（方案 A），**须 `./gradlew dependencyGuardBaseline` 重生成 4 份基线随 commit**（`app/dependencies/{Online,Offline,Canary}ReleaseRuntimeClasspath.txt` + `app-catalog/dependencies/releaseRuntimeClasspath.txt`）。
- 复用现有设计系统组件（`Cb*` 前缀），禁用裸 Material3（lint 强制）。
- 金额全链路 `Long`（分）。④ 统计采用 `verifyAssetBalance`/`calculateRecordAmount` 口径（见 ④）；注意 `finalAmount` 是预计算字段、`calculateRecordAmount` 是即时计算，二者语义不同，按各小节明确口径使用。
- 每项必须配单元测试，功能在测试通过后才算完成。

---

## ① 受保护类型统计弹窗修复（Bug）

### 根因（实证）
`MyCategoriesScreen.kt:657`（`FirstTypeItem`）/`:769`（`SecondTypeItem`）点击处理为 `if (!data.protected) { expandedMenu = true }`。报销（`-2002`）、退款（`-2001`）、还信用卡（`-2003`）三个固定类型 `protected=true`（`TypeTable.kt:100/113/126`），故点击永不展开 `DropdownMenu`，而"统计数据"项（`:695-701`）就在该菜单内 → 被整体拦掉。`protected` 本意仅防删改。

### 方案
- 点击处理改为**始终** `expandedMenu = true`。
- `DropdownMenu` 内按 `protected` 条件渲染：`protected==true` 只渲染"统计数据"项，隐藏 编辑/删除/改为二级/添加二级；`protected==false` 维持现状。
- 一级、二级各改一处（二级 `SecondTypeItem` 当前数据下无 protected 命中对象——三个特殊类型均为一级，改动为防御性、保持一致）。

### 受影响文件
- `feature/types/.../screen/MyCategoriesScreen.kt`

### 验证点（实现期）
- 统计页（`onRequestNaviToTypeStatistics`→`GetTypeRecordViewsUseCase`→`RecordDao.queryRecordByTypeId:157`）对特殊类型（-2001/-2002/-2003）能正常出数据——固定 ID 记录由 `migrateSpecialTypes:177-201` 迁移，记账保存的 typeId 可为固定 ID，基本可行，实测确认。
- 误删/误改已被数据层兜底（`MyCategoriesViewModel.requestDeleteType:154` 对 protected 只提示、`TypeRepositoryImpl.deleteById:137-139` 对固定 ID `require` 抛异常）；UI 隐藏为冗余保护。补一条"绕过 UI 直调 changeTypeToSecond 不破坏 needRelated 标记"的防御性测试。

### 测试
- UI 测试：点击受保护类型 → 菜单出现且仅含"统计数据"；普通类型 → 全部菜单项可见。

---

## ② 金额搜索（Bug）

### 根因（实证）
`RecordDao.queryRecordByKeyword`（`RecordDao.kt:242-257`）SQL 仅 `remark LIKE '%'||:keyword||'%'`，不查金额。链路：`SearchScreen.kt:135`→`SearchViewModel.kt:89`→`GetSearchRecordViewsUseCase.kt:39-52`→`RecordRepositoryImpl.kt:238`→DAO。

### 方案
- **哨兵处理（关键，纠正 v1 错误）**：`String.toAmountCent()`（`Money.kt:52-55`）对**非数字返回 `0L`**（不是 -1L，v1 写错）。因此 Repository 层**不得直接用其返回值当哨兵**，须先 `keyword.toBigDecimalOrNull() != null` 判定是否合法数字：是→转分得 `amountCent`；否→显式传 `-1L`。
- **匹配字段（用户决策）**：`amount` 与 `finalAmount` 任一命中（用户列表显示的金额视记录类型为 `finalAmount` 或 `amount`，见 `LauncherContentScreen.kt:723-732`，故两者都匹配最不漏）。DAO 条件：
  ```sql
  WHERE books_id = :booksId
    AND ( remark LIKE '%'||:keyword||'%'
          OR (:amountCent != -1 AND (amount = :amountCent OR final_amount = :amountCent)) )
  ORDER BY record_time DESC
  LIMIT :pageSize OFFSET :pageNum
  ```
- 支持小数（100.5→10050）；保留备注搜索（向后兼容）。本项目**无配套 count 查询**（分页靠 `items.isNotEmpty()` 判尾，`SearchRecordPagingSource:126-128`），无需改 count。
- 边界（实现期处理）：超大数字 `toLong()` 静默截断（`Money.kt:54`），对 keyword 数值/长度做上界校验；LIKE 内 `%`/`_` 通配为既有行为，本次不扩大处理范围。

### 受影响文件
- `core/database/.../dao/RecordDao.kt`（`queryRecordByKeyword` 加金额条件）
- `core/data/.../repository/impl/RecordRepositoryImpl.kt`（`:238` keyword→amountCent 合法性判定）
- `core/testing/.../repository/FakeRecordRepository.kt:146`（同步加金额过滤）
- `core/testing/.../dao/FakeRecordDao.kt:193-200`（现为子串匹配 amount/charge/concessions，须对齐为"精确等值 amount OR final_amount + 非数字哨兵"语义，与真实 DAO 一致）

### 测试
- `GetSearchRecordViewsUseCaseTest`（现 `:60-91` 无金额用例）新增：整数/小数金额命中（amount 与 finalAmount 两路）、非数字关键字回退备注搜索且**不**误命中 0 元记录、keyword 恰为 0/0.00 的行为、备注与金额 OR 同时命中。

---

## ③ 一级分类长按拖动排序（Feature）

### 现状与可行性（实证）
- 记账类型选择器 `EditRecordTypeListScreen` 数据源 `GetRecordTypeListUseCase.kt:58/61` **已 `.sortedBy { it.sort }`** → 改 sort 后选择器自动按新序。
- **但分类管理页 `MyCategoriesScreen` 自身是另一条链路且未排序**：`MyCategoriesViewModel` 一级数据来自 `TypeRepository.firstXxxTypeListData`→`getFirstRecordTypeList`→`TypeDao.queryByLevel:52-53`（**SQL 无 ORDER BY**）。不补排序则拖动后本页跳回原序。
- `db_type.sort`：特殊类型/平账 sort 全为 `0`（`TypeTable.kt:75/88/101/114/127`），普通类型 `sort=countByLevel+1`（`TypeRepositoryImpl.kt:158`）。特殊类型识别靠 **id**（`isReimburseType:108`/`needRelated:104`/`isCreditPaymentType`），改 sort 不破坏识别。
- 主列表 `ExpandableTypeList`（`MyCategoriesScreen.kt:483-512`）是 `LazyColumn{ typeList.forEach{ item{} } }` **无 key** 结构。
- 项目未引入 reorderable 库。

### 方案
- **管理页排序生效（纠正 v1 遗漏）**：`MyCategoriesViewModel` 一级列表显式 `.sortedBy { it.sort }`（或 `getFirstRecordTypeList` SQL 加 `ORDER BY sort`）。
- **列表结构改造**：`ExpandableTypeList` 从 `forEach{item{}}` 改为 `items(items=typeList, key={it.data.id})`，展开的二级 `SecondTypeList` 作为同一 item 内内容。保留：点击一级弹 `DropdownMenu:663`、点击箭头 `first.expanded:642` 展开折叠、展开插入 `SecondTypeList:706`。**不波及** `DialogExpandableTypeList:516` / `SelectFirstTypeDialog:434` 两个独立列表。
- **拖拽能力**：
  - **方案 A（推荐）**：引入 `sh.calvin.reorderable`（原生支持 keyed `LazyColumn`）。**【需验证】** 与 Compose BOM `2026.05.01` 兼容性，先做最小 PoC（编译+运行 ReorderableItem）；不通过回退方案 B。供应链：个人域名 group，引入前核实许可证/维护活跃度/传递依赖并锁定版本。
  - **方案 B（备选）**：`detectDragGesturesAfterLongPress` 自实现，无新依赖、无供应链风险。
- **sort 重写（纠正撞值）**：拖拽结束后为**全部一级类型**（含特殊类型）重写**连续稠密** sort 值；并将新建类型的 sort 生成从 `count+1` 改为 `max(existing)+1`（`generateSortById`），避免删除/特殊类型场景撞值。
- **Data**：`TypeDao` 新增 `updateSort(id, sort)` + 批量更新；`TypeRepository`（interface）暴露接口，`TypeRepositoryImpl` + **`FakeTypeRepository`（`core/testing`）** 同步实现（否则测试模块编译失败）；调 `typeDataVersion.updateVersion()` 刷新。
- **特殊类型策略**：允许拖动并参与连续 sort 重写；记账选择器报销/退款依赖 `needRelated` 标记（`GetRecordTypeListUseCase:109-113`）而非 sort，重写不破坏。

### 受影响文件
- `feature/types/.../screen/MyCategoriesScreen.kt`（`ExpandableTypeList` 改 keyed items + 拖拽）
- `feature/types/.../viewmodel/MyCategoriesViewModel.kt`（一级 `.sortedBy{sort}` + 拖拽结束更新 sort）
- `core/database/.../dao/TypeDao.kt`（新增 updateSort）
- `core/data/.../repository/TypeRepository.kt` + `impl/TypeRepositoryImpl.kt`（接口 + 实现 + generateSortById 改 max+1）
- `core/testing/.../repository/FakeTypeRepository.kt`（实现新接口）
- `gradle/libs.versions.toml` + 模块 `build.gradle.kts`（方案 A 新增依赖）
- **依赖基线**：方案 A 时 `app/dependencies/*.txt` + `app-catalog/dependencies/releaseRuntimeClasspath.txt`（`dependencyGuardBaseline` 重生成）

### 测试
- `TypeDao.updateSort` 单测；Repository 批量更新 + generateSortById `max+1` 测试；拖拽后 sort 连续、管理页与记账选择器均按新序输出的测试。

---

## ④ 资产详情改按月（Feature）

### 现状与可复用（实证）
- 真实记录列表 UI 在 **`feature/records/.../screen/AssetInfoContentScreen.kt`**（`AssetInfoContentScreen:68`，`topContent.invoke():76`、点击跳详情 `onRecordItemClick:88-94`、`Empty:81`、`Footer:100`），经 **`app/.../ui/MainApp.kt:561`** 的 `assetRecordListContent = { assetId, topContent, onRecordItemClick -> }` 跨模块注入；`AssetInfoScreen.kt`（feature/assets）提供 `topContent`。**v1 把改点写成 AssetInfoScreen 是错的**。
- 当前 `AssetInfoContentViewModel:57-67` 用 Paging3 `AssetRecordPagingSource`→`GetAssetRecordViewsUseCase`→`queryPagingRecordListByAssetId`（`RecordDao.queryRecordByAssetId:139`，无日期过滤）。
- 月份切换可复用 `LauncherContentViewModel._dateSelection:91` + `DateSelectionEntity`；按日期范围毫秒查询参考 `RecordDao.queryViewsBetweenDate:91-112`（**用毫秒时间戳范围，勿用 `queryRecordByYearMonth` 字符串解析路径，避免月边界/时区错归**）。
- 资产余额口径权威逻辑：`TransactionDao.verifyAssetBalance:208-238`。

### 方案
- **完全替换**资产详情记录视图为按月视图；默认当前月、可切换；记录**按日分组**（仿首页 `insertSeparators`）；`topContent` 头加 **月份切换器 + 收入/支出/结余统计卡**。
- **状态**：`AssetInfoContentViewModel` 新增 `MutableStateFlow<YearMonth>` + `updateMonth(year, month)`（参考 `LauncherContentViewModel:91`）。
- **DAO 新增**（毫秒范围）：
  - `queryRecordByAssetIdBetweenDate(booksId, assetId, startMillis, endMillis, pageNum, pageSize)`（参考 `queryRecordByTypeIdBetween:192-199` 模式，命中 `asset_id OR into_asset_id`）。
  - 按 `assetId` + 日期范围的轻量视图查询（统计用，参考 `queryViewsBetweenDate`，命中 `asset_id OR into_asset_id`）。
- **统计口径（用户决策：资产余额口径）**：逐记录按 `verifyAssetBalance:208-238` 规则计算该记录对本资产的余额变动 `delta`：
  - 本资产为源（`asset_id==thisAsset`）：普通资产 `INCOME→+recordAmount`、其余 `→ −recordAmount`；信用卡**反向**（`INCOME→−`、其余 `→+`），`recordAmount=calculateRecordAmount`。
  - 本资产为转账目标（`into_asset_id==thisAsset` 且 TRANSFER）：普通资产 `+amount`、信用卡 `−amount`。
  - **结余** = Σ delta = 该资产当月真实余额净变化；**收入** = Σ(正向 delta)、**支出** = Σ|负向 delta|（满足 收入−支出=结余）。
  - 信用卡资产的"收入/支出"标签语义在实现期对照真实余额变化校验，必要时 UI 文案调整（避免"结余 -2000"歧义）。
- 资产头部（余额/额度/账单日/还款日）保持**实时当前值**，不随所选月变。

### 受影响文件
- `app/.../ui/MainApp.kt`（`assetRecordListContent` 注入处，传入月份切换器/统计卡所需回调）
- `feature/assets/.../screen/AssetInfoScreen.kt`（构造 `topContent`：月份切换器 + 统计卡）
- `feature/records/.../screen/AssetInfoContentScreen.kt`（按日分组渲染，确认 topContent 槽）
- `feature/records/.../viewmodel/AssetInfoContentViewModel.kt`（月份状态 + 新 PagingSource + 统计流）
- `core/database/.../dao/RecordDao.kt`（新增按 assetId+日期范围的分页与视图查询）
- `core/data/.../repository/RecordRepository.kt` + `impl/RecordRepositoryImpl.kt`（新增方法）
- `core/domain/.../usecase/GetAssetRecordViewsUseCase.kt`（改接收月份参数）
- 测试连带：`core/domain/.../GetAssetRecordViewsUseCaseTest.kt:53`、`feature/records/.../AssetInfoContentViewModelTest.kt:150` + 其 `TestPagingSource:189-204` 镜像

### 测试
- 新 DAO 查询单测（按月+资产，含转账目标命中）；月份切换 ViewModel 测试；资产余额口径统计测试（普通资产/信用卡反向/转入转出/含手续费优惠各场景，验证 收入−支出=结余=余额净变化）。

---

## phase 依赖（评审证伪"4 路独立"）

- ②④ 共享 `RecordDao.kt` / `RecordRepositoryImpl.kt` → **②→④ 串行**。
- ①③ 共享 `MyCategoriesScreen.kt`（③ 重写的一级 item 正是 ① 改的 `FirstTypeItem`）→ **①→③ 串行（③ 在 ① 之后，③ 须保留 ① 的"始终弹菜单+按 protected 条件渲染"）**。
- 两组之间可独立。

## 已决策（原待确认项）

- **② 匹配字段**：amount OR finalAmount 任一命中（用户 2026-05-29 决策）。
- **④ 统计口径**：资产余额口径（用户 2026-05-29 决策）。

## 非目标（YAGNI）

- 不做二级分类拖动排序（本次仅一级）。
- 不改首页记录列表的分组方式（仍按日期）。
- 不为搜索新增独立金额筛选框（统一搜索框内兼容）。
- 不引入"全部记录" Tab（资产详情直接替换为按月）。

## 评审勘误记录（agent-teams:team-review 四维 + controller 核验）

| 严重度 | 来源维度 | 问题 | 处置 | controller 核验 |
|---|---|---|---|---|
| Critical | feasibility/security/reverse | `toAmountCent()` 非数字返回 0L 非 -1L，哨兵方案失效 | ② 改为 `toBigDecimalOrNull()` 判定 | ✅ `Money.kt:53` |
| High | reverse | 列表显示 finalAmount，搜 amount 所见非所搜 | ② 改 amount OR finalAmount | ✅ `LauncherContentScreen.kt:723-732` |
| High | reverse | 管理页自身未按 sort 排，拖完跳回 | ③ 加 `MyCategoriesViewModel.sortedBy{sort}` | ✅ `TypeDao.kt:52-53` 无 ORDER BY |
| High | impact | ④ 真实 UI 在 AssetInfoContentScreen+MainApp，非 AssetInfoScreen | ④ 改受影响文件 | ✅ `MainApp.kt:561`、`AssetInfoContentScreen.kt:68/76` |
| High | impact | ④ UseCase 改签名破坏 2 测试文件 | ④ 受影响文件补测试 | 采纳（reviewer file:line） |
| High | reverse | generateSortById count+1 撞值 | ③ 连续重写 + 新建 max+1 | 采纳 |
| High | impact/feasibility | 新依赖破坏 dependencyGuard 基线 | ③ 重生成基线 | ✅ 本会话已实证 dependencyGuard 机制 |
| Medium | impact/reverse | ExpandableTypeList 无 key，reorderable 需 keyed items | ③ 重写列表结构 | ✅ `MyCategoriesScreen.kt:483-512` |
| Medium | impact | TypeRepository 是 interface，需改 FakeTypeRepository | ③ 受影响文件补 | 采纳 |
| Medium | impact/reverse | FakeRecordDao 子串匹配与新精确语义分叉 | ② 对齐 Fake | 采纳 |
| Medium | reverse | 消费口径信用卡符号/转入=0 反直觉 | ④ 改资产余额口径（用户决策） | ✅ `verifyAssetBalance:222` |
| Low | feasibility | 无配套 count 查询 | ② 删该表述 | ✅ |
| Low | reverse | 时区/月边界 | ④ 用毫秒范围查询 | 采纳 |
| Low | impact | 4 phase 非独立 | 改 2 组串行 | ✅ 文件共享 |
| — | security | 无 SQL 注入（Room 参数化）、无敏感数据/权限越界 | 无需处理 | ✅ |
