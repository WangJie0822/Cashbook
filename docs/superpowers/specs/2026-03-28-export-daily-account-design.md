# 导出一日记账功能设计

## 概述

在"备份与恢复"页面的"从微信导入"下方，新增"导出到一日记账"功能。用户可选择账本和日期范围，将记录导出为一日记账通用导入格式的 CSV 文件，通过系统分享 Intent 保存或发送。

## 一日记账模板格式

CSV 共 9 列：

| 列名 | 说明 | 示例 |
|---|---|---|
| 日期 | yyyy/M/d HH:mm:ss | 2024/12/8 13:46:11 |
| 类型 | 支出/收入 | 支出 |
| 账户 | 资产名称 | 中国银行(3319) |
| 类别 | 父类型名称 | 餐饮 |
| 子类别 | 子类型名称（一级类型留空） | 早餐 |
| 金额 | 元，保留2位小数 | 13.00 |
| 备注 | 备注文本 | 这是一条备注 |
| 货币类型 | 固定 CNY | CNY |
| 图片 | 留空 | |

## 需求决策

- **导出范围**：用户可选择账本 + 日期范围
- **文件格式**：CSV（UTF-8 with BOM），无需额外依赖
- **分类映射**：类别 = 父类型名称，子类别 = 子类型名称（一级类型时子类别留空）
- **交互方式**：Bottom Sheet 配置参数 → 导出 → 系统分享
- **转账记录**：跳过不导出，仅导出收入和支出

## 数据流

```
BackupAndRecoveryScreen (Bottom Sheet)
  → 用户选择账本 + 日期范围 → 点击导出
  → ViewModel 调用 ExportRecordUseCase
  → UseCase 查询记录 + 解析类型/资产名称 → 生成 CSV
  → 返回文件路径 → 调用系统分享 Intent
  → Snackbar 提示结果
```

## 字段映射

| 一日记账列 | 来源 |
|---|---|
| 日期 | `recordTime` → 格式化为 `yyyy/M/d HH:mm:ss` |
| 类型 | `typeCategory`: EXPENDITURE→"支出", INCOME→"收入" |
| 账户 | `assetId` → `AssetTable.name` |
| 类别 | 若子类型(SECOND)：取父类型 `name`；若一级类型(FIRST)：取自身 `name` |
| 子类别 | 若子类型(SECOND)：取自身 `name`；若一级类型：留空 |
| 金额 | `amount`(分) → 除以100转元，保留2位小数 |
| 备注 | `remark` |
| 货币类型 | 固定 `CNY` |
| 图片 | 留空 |

## UI 设计

### 入口

在 BackupAndRecoveryScreen 的"导入账单"区块下方，新增：
- Section Header："导出账单"
- ListItem：headline="导出到一日记账"，supporting="导出记录为一日记账通用导入格式(.csv)"

### Bottom Sheet

```
┌─────────────────────────────────┐
│  导出到一日记账                    │
│                                 │
│  账本    [当前账本 ▼]             │
│                                 │
│  开始日期  [2024/01/01 ▼]        │
│  结束日期  [2024/12/31 ▼]        │
│                                 │
│  ┌─────────────────────────┐    │
│  │       导出 (128条)       │    │
│  └─────────────────────────┘    │
└─────────────────────────────────┘
```

- 账本选择器：复用 BooksModel 列表，下拉选择，默认当前账本
- 日期选择：Material3 DatePicker，默认范围为当前账本最早记录到今天；若账本无记录则默认当天，导出按钮显示 0 条并禁用
- 导出按钮：显示匹配记录条数（实时查询），0 条时禁用
- 导出中：按钮显示 loading 状态，防止重复点击
- 导出完成：自动弹出系统分享 Intent，Snackbar 提示"成功导出 X 条记录"

## 文件变更范围

| 层级 | 文件 | 变更 |
|---|---|---|
| **core/database** | `dao/RecordDao.kt` | 新增 JOIN 查询：record + type + parent_type + asset，按账本+日期范围，排除转账 |
| **core/model** | 新增 `ExportRecordModel.kt` | 导出专用数据模型（含父类别名、子类别名、资产名等） |
| **core/data** | `repository/RecordRepository.kt` | 新增按账本+日期范围查询导出记录的方法 |
| **core/data** | 新增 `helper/DailyAccountExporter.kt` | CSV 生成器：接收记录列表 → 写 CSV 文件 |
| **core/domain** | 新增 `ExportRecordUseCase.kt` | 编排导出流程：查询 → 生成 CSV → 返回文件路径 |
| **feature/settings** | `screen/BackupAndRecoveryScreen.kt` | 新增导出入口 ListItem + ExportBottomSheet |
| **feature/settings** | `viewmodel/BackupAndRecoveryViewModel.kt` | 新增导出相关状态和方法 |
| **core/ui** | `res/values/strings_settings.xml` | 新增导出相关字符串资源 |

## CSV 格式细节

- **编码**：UTF-8 with BOM（确保 Excel 正确识别中文）
- **分隔符**：逗号
- **首行**：`日期,类型,账户,类别,子类别,金额,备注,货币类型,图片`
- **文件名**：`一日记账_${账本名}_${开始日期}_${结束日期}.csv`
- **金额**：无千分位，保留2位小数（如 `13.00`）
- **日期格式**：`yyyy/M/d HH:mm:ss`（与模板一致）
- **字段含逗号/换行/引号时**：用双引号包裹，内部引号转义为 `""`

## 设计规范遵循

### 组件约束（Lint 强制）

所有 UI 必须使用 `core/design` 中的封装组件，禁止直接使用 Material3 原始组件：

| 使用 | 禁止 |
|---|---|
| `CbModalBottomSheet` | `ModalBottomSheet` |
| `CbListItem` | `ListItem` |
| `CbTextButton` | `TextButton` |
| `CbIconButton` | `IconButton` |
| `CbAlertDialog` | `AlertDialog` |

> 访问 `MaterialTheme.colorScheme`/`.typography` 属性是允许的。

### 间距 Token

使用 `LocalSpacing.current` 获取间距值（定义在 `core/design/theme/Spacing.kt`）：

| Token | 值 | 用途 |
|---|---|---|
| `spacing.extraSmall` | 4.dp | 紧凑间距 |
| `spacing.small` | 8.dp | 列表项间距、图标与文字间距 |
| `spacing.medium` | 16.dp | 卡片内边距、区块间距 |
| `spacing.large` | 24.dp | 区块分隔 |
| `spacing.extraLarge` | 32.dp | 页面级间距 |

> 注：BackupAndRecoveryScreen 现有代码部分仍使用硬编码 `16.dp`，新增代码应优先使用 Token，与现有代码保持一致即可。

### 无障碍

- 所有可交互元素必须有语义描述
- 装饰性图标使用 `contentDescription = null`
- 功能性图标提供 `contentDescription = stringResource(...)`
- 无障碍字符串资源定义在 `core/design/res/values/strings_content_description.xml`（设计系统级）或 `core/ui/res/values/strings_settings.xml`（业务级）
- Bottom Sheet 标题使用 `semantics { heading() }` 标记
- 日期选择按钮包含当前选中值的语义描述

### 文本样式

遵循 BackupAndRecoveryScreen 现有模式：

| 元素 | 样式 |
|---|---|
| Section Header | 默认（bodyMedium），`MaterialTheme.colorScheme.primary`，`padding(start = spacing.medium)` |
| ListItem headline | `CbListItem` 默认（bodyMedium） |
| ListItem supporting | `CbListItem` 默认（bodySmall） |
| Bottom Sheet 标题 | `MaterialTheme.typography.titleMedium` |
| 导出按钮 | Material3 `Button` 默认样式 |

### 日期选择器

复用项目已有的 `DateRangePickerDialog`（`core/design/component/DateTimePicker.kt`），基于 Material3 `MaterialDatePicker` 封装，而非自行实现。

### 账本选择器

复用 `feature/record-import` 中 `ImportSummarySection` 的 `DropdownMenu` 模式：`Row + clickable + DropdownMenu + DropdownMenuItem`。

## 不在本次范围内

- 不支持导出为 XLS/XLSX 格式
- 不支持导出图片
- 不支持导出转账记录
- 不支持其他记账应用格式（架构预留，不实现）
