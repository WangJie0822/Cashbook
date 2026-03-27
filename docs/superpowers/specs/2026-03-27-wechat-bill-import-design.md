# 微信账单导入功能设计

## 概述

在备份与恢复页面新增"导入账单"功能，首期支持从微信支付导出的 `.xlsx` 账单文件导入记录。架构上预留扩展能力，后续可支持支付宝等其他格式。

## 需求决策

| 决策项 | 结论 |
|--------|------|
| 目标账本 | 导入时用户选择 |
| 支付方式映射 | 自动匹配 + 用户可修正 |
| 分类映射 | 关键词自动匹配 + 兜底默认分类 |
| 转账处理 | 简单按收入/支出处理，不建立转账关联 |
| 导入入口 | 备份页面新增"导入账单"区域，可扩展多格式 |
| 交互方式 | 全屏新页面：选文件 → 映射配置 + 预览 → 确认导入 |
| 重复检测 | 交易单号精确匹配 + 金额/时间模糊匹配，用户选择是否跳过 |
| Excel 解析 | 自行解析（ZIP + XML），零额外依赖 |

## 模块架构

```
feature/settings (入口)
  └─ BackupAndRecoveryScreen → 新增"导入账单"区域 → 跳转

feature/record-import (新模块)
  ├─ navigation/  — 路由定义、NavGraphBuilder 扩展
  ├─ screen/
  │   └─ RecordImportScreen  — 导入主页面（映射配置 + 记录预览 + 确认）
  ├─ viewmodel/
  │   └─ RecordImportViewModel — 状态管理、协调解析和导入逻辑
  └─ component/  — 页面内子组件（映射项、记录预览项等）

core/data
  ├─ helper/
  │   ├─ RecordImportHelper (接口) — 通用导入契约
  │   └─ WechatBillImportHelper (实现) — 微信账单解析 + 映射
  └─ repository/
      └─ RecordRepository — 复用已有能力，新增批量插入方法
```

- `feature/record-import` 使用 `cashbook.android.library.feature` 插件，遵循现有 feature 模块模式
- 解析器在 `core/data` 层，定义 `RecordImportHelper` 通用接口，后续格式只需新增实现类
- `feature/settings` 仅新增一个导航跳转入口

## 数据模型

### 中间模型（格式无关）

```kotlin
// 解析后的原始账单条目
data class ImportedBillItem(
    val transactionTime: Long,           // 交易时间戳
    val transactionType: String,         // 原始交易类型（如"商户消费"）
    val counterparty: String,            // 交易对方
    val description: String,             // 商品/描述
    val direction: BillDirection,        // 收入/支出
    val amount: Double,                  // 金额
    val paymentMethod: String,           // 支付方式原始文本
    val status: String,                  // 当前状态
    val transactionId: String,           // 交易单号（用于去重）
    val merchantId: String,              // 商户单号
    val remark: String,                  // 备注
)

enum class BillDirection { INCOME, EXPENDITURE }
```

### 映射与预览模型

```kotlin
// 支付方式映射
data class PaymentMethodMapping(
    val originalName: String,            // 原始文本，如"民生银行储蓄卡(1420)"
    val matchedAssetId: Long,            // 匹配到的资产 ID，-1 表示未匹配
    val matchedAssetName: String,        // 资产名称（用于展示）
)

// 单条导入预览
data class ImportPreviewItem(
    val billItem: ImportedBillItem,      // 原始账单条目
    val mappedTypeId: Long,              // 自动匹配的分类 ID
    val mappedTypeName: String,          // 分类名称
    val mappedAssetId: Long,             // 映射的资产 ID
    val duplicateStatus: DuplicateStatus,// 重复检测结果
    val selected: Boolean,               // 用户是否选择导入此条
)

enum class DuplicateStatus {
    NONE,        // 无重复
    POSSIBLE,    // 可能重复（金额+时间匹配）
    EXACT,       // 精确重复（交易单号匹配）
}

// 账单汇总信息
data class BillSummary(
    val totalCount: Int,                 // 总记录数
    val incomeCount: Int,                // 收入笔数
    val incomeAmount: Double,            // 收入总金额
    val expenditureCount: Int,           // 支出笔数
    val expenditureAmount: Double,       // 支出总金额
)
```

## 解析与映射逻辑

### xlsx 自行解析

xlsx 本质是 ZIP 包，核心文件：
- `xl/sharedStrings.xml` — 字符串池
- `xl/worksheets/sheet1.xml` — 单元格数据
- `xl/styles.xml` — 格式定义（日期格式判断）

解析流程：
1. 用 `ZipInputStream` 解压
2. 用 Android 内置 `XmlPullParser` 解析 XML
3. 跳过前 17 行头信息 + 1 行列标题，从第 19 行开始读取数据
4. 日期单元格（type=d 或 number 格式）转为时间戳

### 支付方式自动匹配策略

按优先级依次尝试：

| 优先级 | 策略 | 示例 |
|--------|------|------|
| 1 | 精确匹配资产名称 | "零钱" → 名称为"零钱"的微信资产 |
| 2 | 关键词匹配银行卡 | "民生银行储蓄卡(1420)" → classification 为 `BANK_CARD_MS` 且卡号包含 "1420" 的资产 |
| 3 | 匹配 classification 类型 | "零钱" → classification 为 `WECHAT` 的资产 |
| 4 | 未匹配 | 标记为未映射，用户必须手动指定 |

银行关键词映射表（内置）：

```
"中国" → BANK_CARD_ZG, "招商" → BANK_CARD_ZS, "工商" → BANK_CARD_GS,
"农业" → BANK_CARD_NY, "建设" → BANK_CARD_JS, "交通" → BANK_CARD_JT,
"邮政/邮储" → BANK_CARD_YZ, "华夏" → BANK_CARD_HX, "北京" → BANK_CARD_BJ,
"民生" → BANK_CARD_MS, "光大" → BANK_CARD_GD, "中信" → BANK_CARD_ZX,
"广发" → BANK_CARD_GF, "浦发" → BANK_CARD_PF, "兴业" → BANK_CARD_XY
```

### 分类自动匹配策略

基于"交易对方"和"商品"字段做关键词匹配，内置规则表：

```
餐饮: "餐""饭""小吃""奶茶""咖啡""外卖""美团""饿了么""肯德基""麦当劳"...
交通: "充电""加油""停车""打车""滴滴""高速""地铁""公交"...
购物: "超市""商城""淘宝""京东""拼多多"...
通讯: "中国电信""中国移动""中国联通""话费""充值"...
住房: "房租""物业""水电""燃气"...
```

匹配逻辑：遍历规则表，对 `counterparty + description` 做包含匹配，命中第一个即返回。未匹配的按收/支方向归入"其他支出"或"其他收入"默认分类。

### 重复检测策略

1. **精确匹配**：将交易单号存入 remark，查询已有记录的 remark 是否包含相同单号 → `EXACT`
2. **模糊匹配**：同一天 ± 同金额 ± 同收支方向 → `POSSIBLE`
3. 默认标记：`EXACT` 自动取消勾选，`POSSIBLE` 保持勾选但标黄提示，`NONE` 正常勾选

## UI 设计

### 入口（BackupAndRecoveryScreen）

在现有页面底部新增一个"导入账单"卡片区域：
- 标题："导入账单"
- 子项："从微信导入"（带图标），后续可加"从支付宝导入"等
- 点击后打开系统文件选择器（`.xlsx`），选择文件后跳转到导入页面

### 导入页面（RecordImportScreen）

单页分区布局，TopAppBar 带返回按钮和"导入"操作按钮：

**区域 1：导入概览**
- 文件名、记录总数、收入/支出笔数和金额汇总
- 目标账本选择器（下拉或点击弹出选择）

**区域 2：支付方式映射**
- 可折叠区域，列出所有去重后的支付方式
- 每行：原始名称 → 匹配到的资产名称（点击可修改）
- 未匹配的高亮显示，必须手动指定后才能导入

**区域 3：记录预览列表**
- LazyColumn 列表，每条显示：勾选框 | 时间 | 交易对方 | 金额 | 分类标签 | 重复状态标记
- `EXACT` 重复的默认不勾选，显示"已存在"标签
- `POSSIBLE` 重复的显示"可能重复"黄色标签
- 点击分类标签可修改该条的分类
- 列表顶部有全选/取消全选、筛选按钮（按重复状态筛选）

**区域 4：底部操作栏**
- 固定底部：显示"已选 X 条，共 ¥XXX" + "确认导入"按钮
- 存在未映射的支付方式时，按钮置灰不可点

### 导入结果

导入完成后显示 Snackbar 或 Dialog："成功导入 X 条记录，跳过 Y 条"，返回备份页面。

### 状态管理

```kotlin
sealed interface RecordImportUiState {
    data object Empty : RecordImportUiState
    data object Parsing : RecordImportUiState
    data class Ready(
        val fileName: String,
        val summary: BillSummary,
        val selectedBooksId: Long,
        val booksList: List<BooksModel>,
        val paymentMappings: List<PaymentMethodMapping>,
        val previewItems: List<ImportPreviewItem>,
        val hasUnmappedPayments: Boolean,
    ) : RecordImportUiState
    data object Importing : RecordImportUiState
    data class Done(val imported: Int, val skipped: Int) : RecordImportUiState
    data class Error(val message: String) : RecordImportUiState
}
```

状态流转：`Empty → Parsing → Ready → Importing → Done/Error`

## 数据写入

### 字段映射

`ImportPreviewItem` → `RecordTable`：

| RecordTable 字段 | 来源 |
|------------------|------|
| typeId | mappedTypeId |
| assetId | mappedAssetId |
| intoAssetId | -1（不处理转账关联） |
| booksId | 用户选择的账本 ID |
| amount | billItem.amount |
| finalAmount | 与 amount 相同 |
| concessions / charge | 0.0 |
| remark | `交易对方 - 商品描述 [微信单号:xxx]` |
| reimbursable | 0 |
| recordTime | billItem.transactionTime |

### 写入方式

- `RecordRepository` 新增 `batchInsertRecords(records: List<RecordTable>): List<Long>`
- 在数据库事务中批量插入
- 按资产分组汇总余额变化，一次性更新资产余额
- 触发 `recordDataVersion` 和 `assetDataVersion` 更新通知 UI 刷新

### 错误处理

- 解析失败：提示"文件格式不支持，请选择微信支付导出的 xlsx 文件"
- 写入失败：事务回滚，提示具体错误
- 大文件（>5000 条）：正常处理，解析阶段显示 loading

## 不在本次范围内

- 不更新标签（微信账单无标签信息）
- 不插入图片关联
- 不处理关联记录（退款、报销等）
- 不持久化分类映射规则（关键词规则内置）
- 不持久化支付方式映射（每次导入重新匹配）
- 不支持支付宝等其他格式（架构预留，不实现）
