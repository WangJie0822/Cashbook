# UI 截图测试补充设计

## 概述

为 Cashbook 项目补充全面的 UI 截图测试（Roborazzi），覆盖 core/design 组件、core/ui 组件和全部 feature Screen。建立视觉回归基线，防止 UI 变更引入意外问题。

## 现状

- 仅有 1 个截图测试：`BackgroundScreenshotTests`（core/design）
- 测试基础设施完善：Roborazzi v1.46.1、`ScreenshotHelper`（captureMultiTheme/captureMultiDevice）、Fake Repository、TestDataFactory
- ViewModel 单元测试覆盖广泛，但 UI 层无回归保护

## 设计决策

| 决策项 | 选择 | 理由 |
|--------|------|------|
| 覆盖策略 | 全量覆盖所有组件和 Screen | 建立完整视觉基线 |
| 主题变体 | 组件用 `captureMultiTheme()`（4 变体），Screen 用 `captureMultiTheme()` + `captureMultiDevice()`（4+3 变体） | 组件关心主题，Screen 还需关心响应式布局 |
| Screen 测试方式 | 直接测内部 Composable，构造 UiState 传入 | 不涉及 ViewModel/Hilt，复杂度低，ViewModel 已有单元测试 |
| 状态覆盖 | 尽可能完整覆盖所有 UiState 分支 | 加载中、成功、空、错误、对话框等 |
| 文件组织 | 各模块 `src/test/` 下 | 与现有 BackgroundScreenshotTests 一致 |

## Section 1：基础设施配置

### 1.1 Feature 模块 Roborazzi 插件

在以下 7 个模块的 `build.gradle.kts` 中添加 Roborazzi 插件：

- `feature/records`
- `feature/settings`
- `feature/assets`
- `feature/tags`
- `feature/books`
- `feature/types`
- `core/ui`

```kotlin
plugins {
    // 现有插件...
    alias(libs.plugins.takahirom.roborazzi)
}
```

### 1.2 测试类统一模板

```kotlin
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "480dpi")
@LooperMode(LooperMode.Mode.PAUSED)
class XxxScreenshotTests {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    // ...
}
```

### 1.3 截图存储

各模块 `src/test/screenshots/` 目录，沿用现有约定。

## Section 2：Core Design 组件截图测试

放在 `core/design/src/test/kotlin/cn/wj/android/cashbook/core/design/`。

### 测试清单（22 个组件）

| 组件 | 测试文件 | 状态变体 |
|------|---------|---------|
| Calculator | `CalculatorScreenshotTests.kt` | 默认状态、有输入文本 |
| CalendarView | `CalendarViewScreenshotTests.kt` | 默认日期、选中某日 |
| CbPieChart | `PieChartScreenshotTests.kt` | 有数据（多 slice）、空数据、选中某 slice |
| CbLineChart | `LineChartScreenshotTests.kt` | 单数据集、多数据集、空数据、showZeroLine |
| CbWheelPicker | `WheelPickerScreenshotTests.kt` | 默认选中、不同 visibleItemCount |
| DateRangePickerDialog | `DateRangePickerScreenshotTests.kt` | 默认状态 |
| CbAlertDialog | `AlertDialogScreenshotTests.kt` | 有 title+text、有 icon、仅 confirm、confirm+dismiss |
| CbCard / CbElevatedCard | `CardScreenshotTests.kt` | 普通、可点击、Elevated 各一 |
| CbTextField | `TextFieldScreenshotTests.kt` | 空、有文本、有 label、有错误；Outlined；Password（隐藏/显示） |
| CbTopAppBar | `TopAppBarScreenshotTests.kt` | 有返回键、有 actions、无 navigation icon |
| CbTextButton | `TextButtonScreenshotTests.kt` | enabled、disabled |
| CbIconButton | `IconButtonScreenshotTests.kt` | enabled、disabled |
| CbFloatingActionButton | `FloatingActionButtonScreenshotTests.kt` | 普通 FAB、Small FAB |
| Empty | `EmptyScreenshotTests.kt` | 带提示文字、带按钮 |
| Loading | `LoadingScreenshotTests.kt` | 默认 hint、自定义 hint |
| CbModalBottomSheet | `ModalBottomSheetScreenshotTests.kt` | 有内容展开状态 |
| CbScaffold | `ScaffoldScreenshotTests.kt` | 带 topBar+FAB+content |
| CbTab / CbTabRow | `TabRowScreenshotTests.kt` | 多 tab、不同选中项 |
| CbListItem | `ListItemScreenshotTests.kt` | 仅 headline、全属性 |
| CbHorizontalDivider / CbVerticalDivider | `DividerScreenshotTests.kt` | 默认样式 |
| CbVerticalGrid | `VerticalGridScreenshotTests.kt` | 2 列、3 列 |
| Footer | `FooterScreenshotTests.kt` | 默认文字 |

### 跳过的文件

- `Background.kt` — 已有测试
- `Remember.kt`、`Resources.kt`、`SimpleLifecycleObserver.kt`、`TextFieldState.kt` — 非可视组件

### 特殊处理

- Dialog/Sheet 类组件（AlertDialog、DateRangePickerDialog、ModalBottomSheet）需包裹在 Box 中或通过 content lambda 渲染，避免 Dialog window 截图问题

## Section 3：Core UI 组件截图测试

放在 `core/ui/src/test/kotlin/cn/wj/android/cashbook/core/ui/`。

| 组件 | 测试文件 | 状态变体 |
|------|---------|---------|
| DateSelectionPopup | `DateSelectionPopupScreenshotTests.kt` | 展开状态，分别选中：按日、按月、按年、按范围、全部 |
| SelectDateDialog | `SelectDateDialogScreenshotTests.kt` | 默认月份选择、年份可选模式、年份已选中模式 |
| TypeIcon | `TypeIconScreenshotTests.kt` | 默认样式、自定义颜色、showMore=true |

### 跳过的文件

- BackHandler.kt、DialogState.kt、DevicePreviews.kt、Navigation.kt、expand/*.kt — 非可视组件

## Section 4：Feature Screen 截图测试

放在对应 feature 模块的 `src/test/kotlin/.../screen/`。每个 Screen 使用 `captureMultiTheme()` + `captureMultiDevice()`。

### feature/records（8 个）

| Screen | 测试文件 | 状态变体 |
|--------|---------|---------|
| LauncherContentScreen | `LauncherContentScreenScreenshotTests.kt` | Loading、有记录列表、空记录、展开 BottomSheet |
| EditRecordScreen | `EditRecordScreenScreenshotTests.kt` | Loading、支出编辑、收入编辑、转账编辑、显示计算器 |
| AnalyticsScreen | `AnalyticsScreenScreenshotTests.kt` | Loading、有数据（饼图+折线图）、无数据、展开 DatePopup |
| CalendarScreen | `CalendarScreenScreenshotTests.kt` | Loading、有日历数据、选中某日展示记录列表 |
| SearchScreen | `SearchScreenScreenshotTests.kt` | 空搜索、有搜索结果、无结果 |
| SelectRelatedRecordScreen | `SelectRelatedRecordScreenScreenshotTests.kt` | Loading、有关联记录列表、空列表 |
| TypedAnalyticsScreen | `TypedAnalyticsScreenScreenshotTests.kt` | Loading、有分类统计数据、无数据 |
| AssetInfoContentScreen | `AssetInfoContentScreenScreenshotTests.kt` | Loading、有资产记录、空记录 |

### feature/settings（5 个）

| Screen | 测试文件 | 状态变体 |
|--------|---------|---------|
| LauncherScreen | `LauncherScreenScreenshotTests.kt` | Loading、正常主界面、显示书签提示 |
| SettingScreen | `SettingScreenScreenshotTests.kt` | Loading、Success（各开关状态）、暗色模式对话框、密码对话框 |
| BackupAndRecoveryScreen | `BackupAndRecoveryScreenScreenshotTests.kt` | Loading、有备份数据、WebDAV 已/未配置 |
| AboutUsScreen | `AboutUsScreenScreenshotTests.kt` | Loading、正常展示 |
| MarkdownScreen | `MarkdownScreenScreenshotTests.kt` | Loading、有 Markdown 内容 |

### feature/assets（5 个）

| Screen | 测试文件 | 状态变体 |
|--------|---------|---------|
| MyAssetScreen | `MyAssetScreenScreenshotTests.kt` | Loading、有资产列表、空资产 |
| EditAssetScreen | `EditAssetScreenScreenshotTests.kt` | Loading、新建资产、编辑已有资产 |
| AssetInfoScreen | `AssetInfoScreenScreenshotTests.kt` | Loading、有资产详情+记录列表 |
| EditRecordSelectAssetBottomSheetScreen | `EditRecordSelectAssetBottomSheetScreenScreenshotTests.kt` | 有资产选择列表、空列表 |
| InvisibleAssetScreen | `InvisibleAssetScreenScreenshotTests.kt` | 有隐藏资产列表、空列表 |

### feature/tags（2 个）

| Screen | 测试文件 | 状态变体 |
|--------|---------|---------|
| MyTagsScreen | `MyTagsScreenScreenshotTests.kt` | Loading、有标签列表、空标签 |
| EditRecordSelectTagBottomSheetScreen | `EditRecordSelectTagBottomSheetScreenScreenshotTests.kt` | 有标签选择列表、空列表 |

### feature/books（2 个）

| Screen | 测试文件 | 状态变体 |
|--------|---------|---------|
| MyBooksScreen | `MyBooksScreenScreenshotTests.kt` | Loading、有账本列表 |
| EditBookScreen | `EditBookScreenScreenshotTests.kt` | Loading、新建账本、编辑已有账本 |

### feature/types（2 个）

| Screen | 测试文件 | 状态变体 |
|--------|---------|---------|
| MyCategoriesScreen | `MyCategoriesScreenScreenshotTests.kt` | Loading、有分类列表（支出/收入 tab） |
| EditRecordTypeListScreen | `EditRecordTypeListScreenScreenshotTests.kt` | Loading、有类型列表、编辑模式 |

### 特殊处理

- **LauncherContentScreen**：使用 `flowOf(PagingData.from(list)).collectAsLazyPagingItems()` 构造 Paging 数据
- **BottomSheet 类 Screen**：直接渲染 Sheet 内容部分
- **private Screen 函数**：改为 `internal` 并添加 `@VisibleForTesting` 注解
- **Dialog 状态**：通过 `dialogState` 参数控制对话框显示

## 工作量估算

| 层级 | 测试类数 | 测试方法数（约） |
|------|---------|--------------|
| 基础设施配置 | — | — |
| Core Design 组件 | 22 | ~50 |
| Core UI 组件 | 3 | ~11 |
| Feature Screen | 24 | ~80 |
| **合计** | **49** | **~141** |

## 实施顺序

1. 基础设施配置（Roborazzi 插件 + private→internal）
2. Core Design 组件截图测试
3. Core UI 组件截图测试
4. Feature Screen 截图测试（按模块：records → settings → assets → tags → books → types）
5. 运行 `./gradlew recordRoborazziOnlineDebug` 生成基准截图
6. 运行 `./gradlew verifyRoborazziOnlineDebug` 验证通过
