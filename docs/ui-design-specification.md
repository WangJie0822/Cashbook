# Cashbook UI 设计规范文档

## 1. 文档概述

### 1.1 目的

本文档从 Cashbook Android 项目中提取完整的 UI 设计规范，涵盖设计令牌、组件库、图标系统、页面结构、导航关系和状态管理模式。其他平台（iOS、鸿蒙、Web、Flutter）的开发者可基于此文档，构建与 Android 版风格和架构一致的 UI。

### 1.2 设计基础

- **设计语言**: Material Design 3
- **种子颜色 (Seed Color)**: `#03776A`（青绿色）
- **支持主题**: 亮色 / 暗色 / 跟随系统
- **动态主题**: 可选支持（Android 12+ Material You）
- **响应式**: 支持手机、平板、可折叠设备

---

## 2. 设计令牌 (Design Tokens)

### 2.1 调色板 (Color Palette)

#### 2.1.1 Material 3 标准色系

**亮色主题 (Light Theme)**

| 色系 | 颜色角色 | 十六进制值 | 用途 |
|------|---------|----------|------|
| Primary | primary | `#006B5F` | 主要交互色 |
| | onPrimary | `#FFFFFF` | 主色上的文本/图标 |
| | primaryContainer | `#75F8E3` | 主色容器背景 |
| | onPrimaryContainer | `#00201C` | 主色容器内文本 |
| Secondary | secondary | `#904D00` | 次要交互色 |
| | onSecondary | `#FFFFFF` | 次色上的文本 |
| | secondaryContainer | `#FFDCC2` | 次色容器背景 |
| | onSecondaryContainer | `#2E1500` | 次色容器内文本 |
| Tertiary | tertiary | `#99405F` | 第三交互色 |
| | onTertiary | `#FFFFFF` | 第三色上的文本 |
| | tertiaryContainer | `#FFD9E1` | 第三色容器背景 |
| | onTertiaryContainer | `#3F001C` | 第三色容器内文本 |
| Error | error | `#BA1A1A` | 错误状态 |
| | onError | `#FFFFFF` | 错误色上的文本 |
| | errorContainer | `#FFDAD6` | 错误容器背景 |
| | onErrorContainer | `#410002` | 错误容器内文本 |
| Neutral | background | `#F3FEFF` | 页面背景 |
| | onBackground | `#002022` | 背景上的文本 |
| | surface | `#F3FEFF` | 表面颜色 |
| | onSurface | `#002022` | 表面上的文本 |
| | surfaceVariant | `#DAE5E1` | 表面变体 |
| | onSurfaceVariant | `#3F4946` | 表面变体上的文本 |
| Outline | outline | `#6F7976` | 轮廓色 |
| | outlineVariant | `#BEC9C5` | 轮廓变体 |
| Special | inverseSurface | `#00373A` | 反色表面 |
| | inverseOnSurface | `#C3FBFF` | 反色表面上的文本 |
| | inversePrimary | `#55DBC7` | 反色主色 |
| | shadow | `#000000` | 阴影 |
| | scrim | `#000000` | 蒙层 |
| | surfaceTint | `#006B5F` | 表面着色 |

**暗色主题 (Dark Theme)**

| 色系 | 颜色角色 | 十六进制值 |
|------|---------|----------|
| Primary | primary | `#55DBC7` |
| | onPrimary | `#003731` |
| | primaryContainer | `#005047` |
| | onPrimaryContainer | `#75F8E3` |
| Secondary | secondary | `#FFBB7C` |
| | onSecondary | `#4D2700` |
| | secondaryContainer | `#6D3900` |
| | onSecondaryContainer | `#FFDCC2` |
| Tertiary | tertiary | `#FFB1C6` |
| | onTertiary | `#5E1131` |
| | tertiaryContainer | `#7B2947` |
| | onTertiaryContainer | `#FFD9E1` |
| Error | error | `#FFB4AB` |
| | onError | `#690005` |
| | errorContainer | `#93000A` |
| | onErrorContainer | `#FFDAD6` |
| Neutral | background | `#002022` |
| | onBackground | `#70F5FF` |
| | surface | `#002022` |
| | onSurface | `#70F5FF` |
| | surfaceVariant | `#3F4946` |
| | onSurfaceVariant | `#BEC9C5` |
| Outline | outline | `#899390` |
| | outlineVariant | `#3F4946` |
| Special | inverseSurface | `#70F5FF` |
| | inverseOnSurface | `#002022` |
| | inversePrimary | `#006B5F` |

#### 2.1.2 业务扩展色系 (Extended Colors)

项目定义了 8 组业务专用色系，每组包含 4 个颜色（主色、主色上文本、容器色、容器上文本）。

**交易类型色**

| 类型 | 属性 | 亮色值 | 暗色值 | 用途 |
|------|------|--------|--------|------|
| 支出 | expenditure | `#B02F00` | `#FFB5A0` | 支出金额、标签 |
| | onExpenditure | `#FFFFFF` | `#5F1500` | 支出色上的文本 |
| | expenditureContainer | `#FFDBD1` | `#872200` | 支出容器背景 |
| | onExpenditureContainer | `#3B0900` | `#FFDBD1` | 支出容器内文本 |
| 收入 | income | `#006D3A` | `#69DD92` | 收入金额、标签 |
| | onIncome | `#FFFFFF` | `#00391B` | 收入色上的文本 |
| | incomeContainer | `#86FAAC` | `#00522A` | 收入容器背景 |
| | onIncomeContainer | `#00210E` | `#86FAAC` | 收入容器内文本 |
| 转账 | transfer | `#006398` | `#94CCFF` | 转账金额、标签 |
| | onTransfer | `#FFFFFF` | `#003352` | 转账色上的文本 |
| | transferContainer | `#CDE5FF` | `#004B74` | 转账容器背景 |
| | onTransferContainer | `#001D32` | `#CDE5FF` | 转账容器内文本 |

**选项状态色**

| 类型 | 属性 | 亮色值 | 暗色值 | 用途 |
|------|------|--------|--------|------|
| 选中 | selected | `#984715` | `#FFB690` | 选中状态标识 |
| | onSelected | `#FFFFFF` | `#552100` | |
| | selectedContainer | `#FFDBCB` | `#783100` | |
| | onSelectedContainer | `#341100` | `#FFDBCB` | |
| 未选中 | unselected | `#566500` | `#BDD062` | 未选中状态标识 |
| | onUnselected | `#FFFFFF` | `#2C3400` | |
| | unselectedContainer | `#D9EC7B` | `#404C00` | |
| | onUnselectedContainer | `#181E00` | `#D9EC7B` | |
| 第四色 | quaternary | `#755B00` | `#ECC248` | 额外强调色 |
| | onQuaternary | `#FFFFFF` | `#3D2E00` | |
| | quaternaryContainer | `#FFDF90` | `#584400` | |
| | onQuaternaryContainer | `#241A00` | `#FFDF90` | |

**第三方平台色**

| 平台 | 属性 | 亮色值 | 暗色值 |
|------|------|--------|--------|
| GitHub | github | `#006874` | `#4FD8EB` |
| | onGithub | `#FFFFFF` | `#00363D` |
| | githubContainer | `#97F0FF` | `#004F58` |
| | onGithubContainer | `#001F24` | `#97F0FF` |
| Gitee | gitee | `#BA162C` | `#FFB3B1` |
| | onGitee | `#FFFFFF` | `#680012` |
| | giteeContainer | `#FFDAD8` | `#92001C` |
| | onGiteeContainer | `#410006` | `#FFDAD8` |

#### 2.1.3 颜色自动匹配函数

项目提供两个辅助函数用于颜色匹配，跨平台实现时应复刻此逻辑：

- **fixedContentColorFor(backgroundColor)**: 给定背景色，返回合适的前景色（文本/图标）
- **fixedContainerColorFor(color)**: 给定颜色，返回对应的容器色

匹配优先级：Material 标准色映射 → 扩展色映射 → 默认内容色

### 2.2 字体排版 (Typography)

采用 Material Design 3 标准排版规范，共 15 种文本样式：

| 类别 | 名称 | 字体大小 | 字重 | 行高 | 字距 | 典型用途 |
|------|------|---------|------|------|------|---------|
| Display | displayLarge | 57sp | Normal | 64sp | -0.25sp | 超大标题 |
| | displayMedium | 45sp | Normal | 52sp | 0sp | 大标题 |
| | displaySmall | 36sp | Normal | 44sp | 0sp | 中大标题 |
| Headline | headlineLarge | 32sp | Normal | 40sp | 0sp | 页面标题 |
| | headlineMedium | 28sp | Normal | 36sp | 0sp | 区块标题 |
| | headlineSmall | 24sp | Normal | 32sp | 0sp | 卡片标题 |
| Title | titleLarge | 22sp | Bold | 28sp | 0sp | TopAppBar 标题 |
| | titleMedium | 18sp | Bold | 24sp | 0.1sp | 按钮文本、导航项 |
| | titleSmall | 14sp | Medium | 20sp | 0.1sp | 小标题、标签 |
| Body | bodyLarge | 16sp | Normal | 24sp | 0.5sp | 主要正文 |
| | bodyMedium | 14sp | Normal | 20sp | 0.25sp | 常规正文 |
| | bodySmall | 12sp | Normal | 16sp | 0.4sp | 辅助文本 |
| Label | labelLarge | 14sp | Medium | 20sp | 0.1sp | 按钮标签 |
| | labelMedium | 12sp | Medium | 16sp | 0.5sp | 小标签 |
| | labelSmall | 10sp | Medium | 16sp | 0sp | 最小标签 |

> **跨平台注意**: `sp` 为 Android 可缩放像素单位。iOS 使用 `pt`，Web 使用 `rem/px`，需按平台 DPI 做换算。

### 2.3 间距与圆角 (Spacing & Shape)

**间距约定**

| 用途 | 值 | 使用场景 |
|------|-----|---------|
| 标准内边距 | 16dp | 页面内容边距、卡片内边距 |
| 小间距 | 8dp | 组件间距、列表项间距 |
| 大间距 | 32dp | 区域间隔 |
| 底部页脚最小高度 | 88dp | 列表底部页脚 |
| 加载组件最小高度 | 120dp | Loading 组件 |
| 空状态最小高度 | 250dp | Empty 组件 |

**圆角规范**

| 组件 | 圆角 | 说明 |
|------|------|------|
| 小圆角 | ShapeDefaults.Small | 按钮、芯片 |
| 中等圆角 | ShapeDefaults.Medium | 卡片 |
| 大圆角 | ShapeDefaults.Large | 对话框 |
| 底部弹窗顶部 | 16dp (仅顶部) | BottomSheet、BackdropScaffold 前景层 |
| 圆形 | CircleShape | TypeIcon 容器 |

### 2.4 渐变系统 (Gradient)

**GradientColors 结构**

```
GradientColors {
    top: Color       // 渐变顶部颜色
    bottom: Color    // 渐变底部颜色
    container: Color // 容器颜色
}
```

**默认渐变配置**（静态主题时使用）

| 属性 | 值 | 说明 |
|------|-----|------|
| top | surface 色 | 页面顶部 |
| bottom | tertiaryContainer 色 | 页面底部 |
| container | surface 色 | 容器 |

**渐变绘制参数**

- 渐变角度: 11.06 度（与垂直轴夹角）
- 顶部渐变: 起始 0% → 终止 72.4%（淡出）
- 底部渐变: 起始 25.52% → 终止 100%（淡入）
- 两个渐变区域重叠形成自然过渡

**动态主题时**: 使用空渐变（仅保留容器色），让系统动态配色自然呈现。

### 2.5 背景系统 (Background)

```
BackgroundTheme {
    color: Color           // 背景颜色 = surface
    tonalElevation: Dp     // 色调高度 = 2dp
}
```

- 使用 Surface 组件渲染背景
- 色调高度 2dp 产生微妙的立体感
- 重置绝对色调高度为 0dp 避免嵌套堆积

### 2.6 图标色调 (Tint)

```
TintTheme {
    iconTint: Color?  // 图标着色，可为 null
}
```

| 主题模式 | iconTint 值 | 效果 |
|---------|-------------|------|
| 动态主题 | primary 色 | 图标统一着色 |
| 静态主题 | null | 图标保持原始颜色 |

### 2.7 主题切换逻辑

```
CashbookTheme(darkTheme, disableDynamicTheming) {
    1. 选择配色方案:
       - 动态主题启用 && Android 12+ → 系统动态配色
       - 否则 → 自定义配色 (Light/Dark AndroidColorScheme)

    2. 选择扩展颜色:
       - 暗色 → DarkExtendedColors
       - 亮色 → LightExtendedColors

    3. 配置渐变:
       - 动态主题 → 空渐变
       - 静态主题 → 默认渐变

    4. 配置背景:
       - color = surface, tonalElevation = 2dp

    5. 配置色调:
       - 动态主题 → TintTheme(primary)
       - 静态主题 → TintTheme(null)

    6. 注入全局可访问:
       - ExtendedColors
       - GradientColors
       - BackgroundTheme
       - TintTheme
       - MaterialTheme(colorScheme, typography)
}
```

**暗色模式控制**: 通过 `DarkModeEnum` 枚举：

| 值 | 含义 |
|-----|------|
| FOLLOW_SYSTEM | 跟随系统 |
| LIGHT | 强制亮色 |
| DARK | 强制暗色 |

---

## 3. 图标系统 (Icon System)

### 3.1 图标来源

全部使用 **Material Design Icons (Filled 样式)**，无自定义图标文件。部分图标使用 `AutoMirrored` 变体以支持 RTL 布局。

### 3.2 图标清单

**导航类**

| 图标名 | Material Icon | 用途 |
|--------|--------------|------|
| ArrowBack | AutoMirrored.Filled.ArrowBack | 返回按钮 |
| KeyboardArrowRight | AutoMirrored.Filled.KeyboardArrowRight | 向右箭头/展开 |
| KeyboardArrowDown | Filled.KeyboardArrowDown | 向下箭头/收起 |
| ArrowDropDown | Filled.ArrowDropDown | 下拉选择 |
| Menu | Filled.Menu | 侧边栏菜单 |

**操作类**

| 图标名 | Material Icon | 用途 |
|--------|--------------|------|
| Add | Filled.Add | 新增/添加 |
| Close | Filled.Close | 关闭 |
| Check | Filled.Check | 确认/完成 |
| CheckCircle | Filled.CheckCircle | 完成圆形 |
| Cancel | Filled.Cancel | 取消 |
| ContentCopy | Filled.ContentCopy | 复制 |
| DeleteForever | Filled.DeleteForever | 永久删除 |
| RemoveCircle | Filled.RemoveCircle | 移除 |
| SaveAs | Filled.SaveAs | 保存 |
| EditNote | Filled.EditNote | 编辑 |
| CleaningServices | Filled.CleaningServices | 清空 |
| Backspace | AutoMirrored.Filled.Backspace | 退格删除 |

**功能类**

| 图标名 | Material Icon | 用途 |
|--------|--------------|------|
| Search | Filled.Search | 搜索 |
| CalendarMonth | Filled.CalendarMonth | 日历 |
| DateRange | Filled.DateRange | 日期范围 |
| Analytics | Filled.Analytics | 统计图表 |
| DonutSmall | Filled.DonutSmall | 饼图 |
| LibraryBooks | AutoMirrored.Filled.LibraryBooks | 账本 |
| WebAsset | Filled.WebAsset | 资产 |
| Category | Filled.Category | 分类 |
| Layers | Filled.Layers | 标签 |
| Settings | Filled.Settings | 设置 |
| Info | Filled.Info | 关于 |
| PhotoLibrary | Filled.PhotoLibrary | 图片 |

**状态/安全类**

| 图标名 | Material Icon | 用途 |
|--------|--------------|------|
| Fingerprint | Filled.Fingerprint | 生物识别 |
| Visibility | Filled.Visibility | 显示密码 |
| VisibilityOff | Filled.VisibilityOff | 隐藏密码 |
| Error | Filled.Error | 错误提示 |
| MoreVert | Filled.MoreVert | 竖向更多菜单 |
| MoreHoriz | Filled.MoreHoriz | 横向更多菜单 |

### 3.3 图标使用规范

- 导航栏图标尺寸: 默认 24dp
- TypeIcon 内图标尺寸: 20dp（容器 32dp）
- TypeIcon "更多"标记: 12dp 小圆形，内含 MoreHoriz 图标
- 图标着色: 跟随主题色调（动态主题时为 primary 色，静态主题时保持原色）

---

## 4. 通用组件库 (Common Components)

### 4.1 布局组件

#### CbScaffold（页面脚手架）

Material 3 Scaffold 的直接包装，默认背景透明（配合 CashbookBackground 使用）。

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| topBar | Composable | 空 | 顶部应用栏 |
| bottomBar | Composable | 空 | 底部导航栏 |
| snackbarHost | Composable | 空 | Snackbar 宿主 |
| floatingActionButton | Composable | 空 | FAB 按钮 |
| floatingActionButtonPosition | FabPosition | End | FAB 位置 |
| containerColor | Color | Transparent | 容器颜色 |
| content | Composable(PaddingValues) | 必需 | 页面内容 |

#### CashbookBackground（纯色背景）

使用 Surface 组件，颜色和色调高度来自 BackgroundTheme。

#### CashbookGradientBackground（渐变背景）

双层线性渐变，渐变参数见 [2.4 渐变系统](#24-渐变系统-gradient)。

#### CbVerticalGrid（垂直网格）

简单的等宽列网格布局。

| 参数 | 类型 | 说明 |
|------|------|------|
| columns | Int | 列数 |
| items | List\<T\> | 数据列表 |
| content | Composable(T) | 单项渲染 |

实现: Column → Row（每行 columns 个等宽列）

#### BackdropScaffold（双层框架）

Material 3 风格的背景层 + 前景层双层框架，支持上拉/下拉展开/隐藏。

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| appBar | Composable | 必需 | 顶部应用栏 |
| backLayerContent | Composable | 必需 | 背景层内容（上拉展开） |
| frontLayerContent | Composable | 必需 | 前景层内容（主要内容） |
| scaffoldState | BackdropScaffoldState | 必需 | 状态管理 |
| gesturesEnabled | Boolean | true | 是否启用手势 |
| peekHeight | Dp | 56dp | 前景层初始高度 |
| headerHeight | Dp | 48dp | AppBar 高度 |
| frontLayerShape | Shape | 16dp 顶部圆角 | 前景层形状 |
| frontLayerElevation | Dp | 1dp | 前景层高度 |
| backLayerBackgroundColor | Color | primary | 背景层颜色 |

**状态**: `BackdropValue.Concealed`（隐藏）/ `BackdropValue.Revealed`（展开）

### 4.2 输入组件

#### CbTextField（标准文本输入）

基于 Material 3 TextField，集成 TextFieldState 状态管理。

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| textFieldState | TextFieldState | 必需 | 状态管理对象 |
| enabled | Boolean | true | 是否可编辑 |
| readOnly | Boolean | false | 是否只读 |
| label | Composable? | null | 标签 |
| placeholder | Composable? | null | 占位符 |
| leadingIcon | Composable? | null | 前置图标 |
| trailingIcon | Composable? | null | 尾部图标 |
| singleLine | Boolean | false | 单行模式 |
| keyboardOptions | KeyboardOptions | Default | 键盘配置 |

#### CbOutlinedTextField（轮廓文本输入）

带边框的文本输入框，参数同 CbTextField，使用 OutlinedTextField 样式。

#### CbPasswordTextField（密码输入框）

| 特性 | 说明 |
|------|------|
| 尾部图标 | 切换显示/隐藏（Visibility / VisibilityOff） |
| 视觉转换 | 隐藏时密码掩码，显示时明文 |
| 键盘类型 | 隐藏时 Password，显示时 Text |
| 单行 | 始终 true |

#### TextFieldState（输入状态管理）

独立的文本输入状态管理类，跨平台实现时应复刻此模式。

| 属性/方法 | 说明 |
|-----------|------|
| text | 当前文本（可观察） |
| isValid | 是否通过验证 |
| isFocused | 当前焦点状态 |
| validator | 验证函数 `(String) -> Boolean` |
| filter | 过滤函数 `(String) -> Boolean` |
| errorFor | 错误消息函数 `(String) -> String` |
| delayAfterTextChange | 文本变化防抖延迟（默认 200ms） |
| showErrors() | 仅在被聚焦过后显示错误 |

#### Calculator（计算器）

内嵌数值计算器组件，用于金额输入。

| 参数 | 类型 | 说明 |
|------|------|------|
| defaultText | String | 初始显示值 |
| primaryColor | Color | 主题色（确认按钮背景） |
| onConfirmClick | (String) -> Unit | 确认回调（返回计算结果） |

**布局**: 4 列等宽网格

```
| C    | ÷    | ×    | 退格  |
| 1    | 2    | 3    | -    |
| 4    | 5    | 6    | +    |
| 7    | 8    | 9    | =/确认|
| ()   | 0    | .    | (续) |
```

- 确认按钮: 背景 = primaryColor，高度 = 100dp
- 输出框: 只读 TextField，聚焦文本色 = primaryColor
- 内边距: 16dp

### 4.3 日期时间组件

#### CalendarView（日历视图）

月份日历选择器，支持翻页浏览。

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| onDateSelected | (LocalDate) -> Unit | 必需 | 日期选择回调 |
| selectDate | LocalDate | today | 当前选中日期 |
| minYear | Int | 当前年-20 | 最小年份 |
| maxYear | Int | 当前年+1 | 最大年份 |
| weekStart | DayOfWeek | SUNDAY | 周起始日 |
| schemeContent | Composable | 空 | 自定义日期内容（如标记） |

**布局**:

```
Column
├── Row (周标题栏，背景 surfaceVariant 20%透明度)
│   └── 周一 ~ 周日 简称
└── HorizontalPager (月份水平翻页)
    └── 日期网格 (Row × Column)
```

**日期颜色规则**:

| 场景 | 文本颜色 | 字重 |
|------|---------|------|
| 前月/后月日期 | onSurface (30% 透明度) | Normal |
| 今天 | primary | Bold |
| 选中日期 | onSecondaryContainer | Normal |
| 普通日期 | onSurface | Normal |

#### DateRangePickerDialog（日期范围选择器）

| 参数 | 类型 | 说明 |
|------|------|------|
| onPositiveButtonClick | (Pair\<Long,Long\>) -> Unit | 确认回调（时间戳对） |
| onNegativeButtonClick | () -> Unit | 取消回调 |
| title | CharSequence? | 标题 |
| selection | Pair\<Long,Long\>? | 初始选中范围 |
| inputMode | Int | 日历模式 / 文本输入模式 |

### 4.4 展示组件

#### CbCard（标准卡片）

Material 3 Card 的直接包装。

| 变体 | 说明 |
|------|------|
| CbCard (非交互) | 纯展示卡片 |
| CbCard (可点击) | 带 onClick 的交互卡片 |
| CbElevatedCard | 更高视觉层级，使用 elevatedCardColors/elevatedCardElevation |

#### CbListItem（列表项）

Material 3 ListItem 包装，**默认背景透明**。

```
|----------------------------------------|
|            | overline      |           |
| leading    | headline      | trailing  |
|            | supporting    |           |
|----------------------------------------|
```

| 参数 | 类型 | 说明 |
|------|------|------|
| headlineContent | Composable | 主要内容（必需） |
| overlineContent | Composable? | 上方辅助内容 |
| supportingContent | Composable? | 下方辅助内容 |
| leadingContent | Composable? | 左侧内容（图标） |
| trailingContent | Composable? | 右侧内容（操作） |

#### Loading（加载状态）

```
Column (最小高度 120dp，居中)
├── LinearProgressIndicator
├── Spacer (高度 32dp)
└── Text (提示文本，透明度 50%)
```

- 提示文本通过 `LocalDefaultLoadingHint` 全局注入

#### Empty（空状态）

```
Column (最小高度 250dp，居中)
├── Image (空状态图片)
├── Text (提示文本，透明度 50%，边距 16dp)
└── button (可选操作按钮)
```

- 空状态图片通过 `LocalDefaultEmptyImagePainter` 全局注入
- 支持三种图片源: Painter / ImageVector / ImageBitmap

#### Footer（页脚提示）

```
Box (最小高度 88dp，宽度填满，居中)
└── Text (提示文本，bodySmall 样式，onSurface 30%透明度)
```

#### CbHorizontalDivider / CbVerticalDivider（分隔线）

Material 3 Divider 包装，**默认透明度 30%**。

### 4.5 操作组件

#### CbIconButton（图标按钮）

Material 图标按钮的直接包装。

#### CbTextButton（文本按钮）

Material 3 TextButton 的直接包装。

#### CbFloatingActionButton（浮动操作按钮）

| 变体 | 说明 |
|------|------|
| CbFloatingActionButton | 标准 FAB |
| CbSmallFloatingActionButton | 小尺寸 FAB |

### 4.6 反馈组件

#### CbTopAppBar（顶部应用栏）

默认背景透明。

| 变体 | 说明 |
|------|------|
| 带返回按钮 | 参数 `onBackClick`，自动使用 ArrowBack 图标 |
| 自定义导航 | 参数 `navigationIcon: Composable` |

#### CbAlertDialog（标准对话框）

Material 3 AlertDialog 的直接包装。

#### CbBaseAlterDialog（自定义对话框）

基于 BasicAlertDialog，完全自定义内容。

#### CbModalBottomSheet（底部弹窗）

Material 3 ModalBottomSheet 包装，拖手默认添加状态栏边距。

#### CbTabRow + CbTab（标签栏）

Material 3 TabRow + Tab 的包装，默认指示器为底部条 (`SecondaryIndicator`)。

### 4.7 工具函数

| 函数 | 用途 |
|------|------|
| painterDrawableResource(idStr) | 运行时通过资源名称字符串获取图标 |
| rememberSnackbarHostState() | 便捷创建 SnackbarHostState |

---

## 5. 业务组件 (Business Components)

### 5.1 TypeIcon（类型图标）

显示记录类型的圆形图标，用于支出/收入/转账等分类标识。

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| painter | Painter | 必需 | 图标 |
| containerColor | Color | primaryContainer | 容器背景色 |
| contentColor | Color | 自动匹配 | 内容颜色 |
| showMore | Boolean | false | 是否显示"更多"标记 |

**设计细节**:
- 容器: 32dp 圆形
- 图标: 20dp
- "更多"标记: 右下角 12dp 小圆形，内含 MoreHoriz 图标（表示有二级分类）
- 容器颜色与文本颜色通过 `fixedContentColorFor` 自动匹配

### 5.2 SelectDateDialog（年月选择弹窗）

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| onDialogDismiss | () -> Unit | 必需 | 关闭回调 |
| currentDate | YearMonth | 必需 | 当前选中日期 |
| yearSelectable | Boolean | false | 是否显示"全年"选项 |
| yearSelected | Boolean | false | 全年是否被选中 |
| onDateSelected | (YearMonth, Boolean) -> Unit | 必需 | 选择回调 |

**布局**:
- 年份选择: 水平滚动列表（2000 ~ 当前年+1）
- 月份选择: 3x4 网格
- 全年选项: 月份下方（可选显示）
- 选中项: primaryContainer 背景标记
- 最小宽度 280dp，最大宽度 560dp

### 5.3 ProgressDialog（进度弹窗）

全局进度弹窗，使用 ProgressDialogManager 单例管理。

**功能**:
- 显示线性进度条 + 提示文本
- 保证最少显示时间 550ms（防止闪烁）
- 支持超时控制
- 支持取消操作
- 使用 CbCard 包装

**API**:

```
ProgressDialogManager {
    show(hint, cancelable, onDismiss)  // 显示
    dismiss()                           // 隐藏
    dialogState: DialogState            // 状态
}

runCatchWithProgress(hint, cancelable, minInterval=550ms, timeout, block)
// 在执行异步操作时自动显示/隐藏进度弹窗
```

### 5.4 RecordListItem（记录列表项）

首页和搜索结果中的记录条目。

```
|----------------------------------------------|
| TypeIcon | 类型名称  [标签芯片]    关联标记    |
|          | 时间 · 备注           金额(分类色) |
|          |                      资产名 手续费 |
|----------------------------------------------|
```

- 左侧: TypeIcon (32dp 圆形)
- 中部: 类型名称 + 标签（可选 Chip）+ 时间/备注
- 右侧: 关联记录标记（灰色）+ 金额（颜色跟随分类：支出红/收入绿/转账蓝）+ 资产名

### 5.5 AssetListItem（资产列表项）

```
|----------------------------------------------|
| 资产图标 | 资产名称              可用余额     |
|          | 账户号码(隐藏)        总额度       |
|          | [进度条 - 仅信用卡]               |
|----------------------------------------------|
```

- 信用卡类型: 显示额度使用进度条
- 普通资产: 显示可用余额

### 5.6 RecordDetailsSheet（记录详情底部弹窗）

Modal BottomSheet，显示记录完整信息。

**内容**:
- 记录类型 + 分类名称
- 金额（大字，分类颜色）
- 资产信息
- 标签列表
- 备注
- 图片列表
- 关联记录
- 操作按钮: 编辑 / 删除 / 查看资产

### 5.7 窗口自适应 (WindowAdaptiveInfo)

| 设备类型 | 尺寸等级 | 账本背景比例 |
|---------|---------|------------|
| 手机竖屏 | COMPACT | 1.5f |
| 平板/横屏 | MEDIUM | 2.5f |
| 大屏设备 | EXPANDED | 4f |

---

## 6. 页面规范 (Page Specifications)

### 6.1 导航结构图

```
启动应用
    |
    v
LauncherScreen (首页主容器 - ModalNavigationDrawer)
    |
    +-- 左侧抽屉菜单 (LauncherSheet)
    |   +-- 我的账本 --> MyBooksScreen
    |   |                 +-- 编辑账本 --> EditBookScreen
    |   +-- 我的资产 --> MyAssetScreen
    |   |                 +-- 资产详情 --> AssetInfoScreen
    |   |                 |                 +-- 编辑资产 --> EditAssetScreen
    |   |                 +-- 隐藏资产 --> InvisibleAssetScreen
    |   |                 +-- 新增资产 --> EditAssetScreen
    |   +-- 我的分类 --> MyCategoriesScreen
    |   +-- 我的标签 --> MyTagsScreen
    |   +-- 设置     --> SettingScreen
    |   |                 +-- 备份恢复 --> BackupAndRecoveryScreen
    |   +-- 关于我们 --> AboutUsScreen
    |                     +-- 版本/隐私 --> MarkdownScreen
    |
    +-- 主内容区域 (LauncherContentScreen)
        +-- 顶部操作栏
        |   +-- 搜索 --> SearchScreen
        |   +-- 日历 --> CalendarScreen
        |   +-- 分析 --> AnalyticsScreen --> TypedAnalyticsScreen
        |
        +-- FAB --> 新增记录 --> EditRecordScreen
        |                         +-- 类型选择 (EditRecordTypeListScreen - 内嵌)
        |                         +-- 资产选择 (BottomSheet - 内嵌)
        |                         +-- 标签选择 (BottomSheet - 内嵌)
        |                         +-- 关联记录 --> SelectRelatedRecordScreen
        |
        +-- 记录列表
            +-- 点击记录 --> RecordDetailsSheet (BottomSheet)
                              +-- 编辑 --> EditRecordScreen
                              +-- 删除 --> ConfirmDeleteRecordDialog
                              +-- 查看资产 --> AssetInfoScreen
                              +-- 查看图片 --> ImagePreviewDialog
```

### 6.2 路由定义

| 模块 | 路由 | 参数 | 说明 |
|------|------|------|------|
| Settings | `settings/launcher` | 无 | 首页主容器 |
| | `settings/setting` | 无 | 设置页 |
| | `settings/backup_and_recovery` | 无 | 备份恢复 |
| | `settings/about_us` | 无 | 关于我们 |
| Records | `record/analytics` | 无 | 数据分析 |
| | `record/typed_analytics` | tagId, typeId, date | 分类分析 |
| | `record/edit_record` | recordId, assetId | 编辑记录 |
| | `record/select_related_record` | 无 | 选择关联记录 |
| | `record/calendar` | 无 | 日历 |
| | `record/search` | 无 | 搜索 |
| Assets | `asset/my` | 无 | 我的资产 |
| | `asset/invisible` | 无 | 隐藏资产 |
| | `asset/info` | assetId | 资产详情 |
| | `asset/edit_asset` | assetId | 编辑资产 |
| Books | `/books/my` | 无 | 我的账本 |
| | `/books/edit` | bookId | 编辑账本 |
| Tags | `tag/my_tag` | 无 | 我的标签 |
| Types | `type/my_categories` | 无 | 我的分类 |

### 6.3 页面详细规范

#### 6.3.1 LauncherScreen（首页主容器）

**布局**: ModalNavigationDrawer

**抽屉菜单项** (LauncherSheet):
1. 标题文本
2. 我的账本（LibraryBooks 图标，显示当前账本名）
3. 我的资产（WebAsset 图标）
4. 我的分类（Category 图标）
5. 我的标签（Layers 图标）
6. [分割线]
7. 设置（Settings 图标）
8. 关于我们（Info 图标）

**核心功能**: 管理抽屉菜单状态、用户隐私协议验证、生物识别认证

#### 6.3.2 LauncherContentScreen（首页内容）

**布局**:

```
CbScaffold
├── topBar: LauncherTopBar
│   ├── 左: Menu 按钮（打开抽屉）
│   ├── 中: 日期选择（可点击，"YYYY-MM" 格式）
│   └── 右: Search + CalendarMonth + Analytics 按钮
├── FAB: Add 图标（新增记录）
└── content:
    ├── AsyncImage（账本背景图，比例 = bookImageRatio）
    └── BackdropScaffold
        ├── backLayer (背景层 - 月度统计):
        │   ├── 月收入
        │   └── 月支出 + 月结余（两列）
        └── frontLayer (前景层 - 记录列表):
            ├── 日期分组头（粘性头部）
            │   ├── 日期 + 星期
            │   └── 日收入 / 日支出
            ├── RecordListItem × N
            └── Footer（底部提示）
```

#### 6.3.3 EditRecordScreen（编辑记录）

**布局**:

```
CbScaffold
├── topBar: CbTopAppBar（返回 + 保存按钮）
├── SnackbarHost
└── content:
    ├── TabRow（支出 / 收入 / 转账）
    ├── 类型选择网格 (EditRecordTypeListContent)
    │   └── LazyVerticalGrid (4列)
    │       └── TypeIcon + 类型名称
    ├── 基本信息:
    │   ├── 金额输入 → Calculator (BottomSheet)
    │   ├── 资产选择 → 资产列表 (BottomSheet)
    │   ├── 日期时间 → DatePicker + TimePicker
    │   └── 备注输入
    ├── 可选信息:
    │   ├── 标签选择 → 标签列表 (BottomSheet)
    │   ├── 关联资产（仅转账）
    │   ├── 手续费 / 优惠 → Calculator (BottomSheet)
    │   ├── 图片上传 (LazyVerticalGrid)
    │   └── 关联记录 → SelectRelatedRecordScreen
    └── 删除按钮（仅编辑模式）
```

#### 6.3.4 SearchScreen（搜索）

```
SearchBar（搜索输入框）
├── 搜索历史（ElevatedSuggestionChip，FlowRow）
├── 清空历史按钮
└── 搜索结果（LazyColumn + Paging 分页）
    ├── RecordListItem × N
    └── 分页加载指示器
```

#### 6.3.5 CalendarScreen（日历）

```
CbScaffold
├── topBar: 日期导航 + 日期范围按钮
└── content:
    ├── TabRow (日视图 / 月视图)
    ├── 月视图:
    │   ├── CalendarView（日历组件）
    │   └── 选中日期的收支统计
    └── 日视图:
        ├── 柱状图 (MPChartLib LineChart)
        └── 日期对应的记录列表
```

#### 6.3.6 AnalyticsScreen（数据分析）

```
CbScaffold
├── topBar: 日期范围选择
└── content:
    ├── TabRow (支出 / 收入 / 转账)
    ├── 饼图（MPChartLib PieChart，中心显示总金额）
    └── 分类统计列表
        └── AnalyticsPieListItem × N
            ├── TypeIcon
            ├── 分类名称
            ├── 百分比 + 进度条
            └── 总金额
```

#### 6.3.7 TypedAnalyticsScreen（分类分析）

```
CbScaffold
├── topBar: 分类名称 + 日期选择
└── content:
    ├── TabRow (日视图 / 月视图 / 统计视图)
    ├── 月视图:
    │   ├── 柱状图 (LineChart)
    │   └── 月统计总览
    ├── 日视图:
    │   └── 该分类该日记录列表
    └── 统计视图:
        ├── 收支统计卡片
        └── 按资产统计列表
```

#### 6.3.8 MyAssetScreen（我的资产）

```
CbScaffold
├── topBar: "我的资产" + 隐藏资产按钮
├── FAB: 新增资产
└── content:
    └── BackdropScaffold
        ├── backLayer: 资产统计汇总
        │   ├── 总资产（大字）
        │   └── 各分类资产总额
        └── frontLayer: 资产列表 (LazyColumn)
            └── 按分类分组
                ├── 分组头
                └── AssetListItem × N（支持长按菜单）
```

#### 6.3.9 AssetInfoScreen（资产详情）

```
CbScaffold
├── topBar: 资产名称 + 编辑/更多按钮
└── content:
    ├── 资产信息卡片 (CbCard)
    │   ├── 资产类型 + 图标
    │   ├── 当前余额
    │   ├── 信用额度（信用卡）
    │   └── 账单日期（信用卡）
    └── 资产记录列表 (LazyPagingItems)
```

#### 6.3.10 EditAssetScreen（编辑资产）

```
CbScaffold
├── topBar: 返回 + 保存按钮
└── content:
    ├── TabRow (基本信息 / 高级设置)
    ├── 基本信息:
    │   ├── 资产名称输入
    │   ├── 资产分类选择 (LazyColumn)
    │   ├── 银行/机构选择
    │   └── 账户号码输入
    └── 高级设置:
        ├── 余额输入
        ├── 信用额度（信用卡）
        ├── 账单日/还款日（信用卡）
        ├── 隐藏开关
        ├── 排序调整
        └── 删除按钮
```

#### 6.3.11 MyBooksScreen（我的账本）

```
CbScaffold
├── topBar: "我的账本" + 返回按钮
├── FAB: 新增账本
└── content:
    └── LazyColumn
        └── 账本卡片 (CbCard) × N
            ├── 背景图 (AsyncImage)
            ├── 账本名称 + 创建日期
            └── 下拉菜单（编辑/删除）
```

#### 6.3.12 EditBookScreen（编辑账本）

```
CbScaffold
├── topBar: 返回 + 保存按钮
└── content:
    ├── 账本背景图预览 (AsyncImage)
    ├── 图片选择按钮
    ├── 账本名称输入 (CbTextField)
    └── 账本描述输入 (CbTextField, 多行)
```

#### 6.3.13 MyTagsScreen（我的标签）

```
CbScaffold
├── topBar: "我的标签" + 返回按钮
├── FAB: 新增标签
└── content:
    └── FlowRow
        └── ElevatedFilterChip × N
            ├── 标签名称
            ├── 使用次数
            └── 长按菜单（编辑/删除/隐藏）
```

#### 6.3.14 MyCategoriesScreen（我的分类）

```
CbScaffold
├── topBar: "我的分类" + 返回按钮
├── FAB: 新增分类
└── content:
    ├── TabRow (支出 / 收入 / 转账)
    └── LazyColumn
        └── CbElevatedCard (可展开分组) × N
            ├── TypeIcon
            ├── 分类名称
            ├── 使用次数
            └── 下拉菜单（编辑/删除）
```

**TypeIconGroupList（图标选择器）**:

```
Row (两列)
├── 左侧 (3/11): 图标分组列表
└── 右侧 (8/11): 4列图标网格
```

#### 6.3.15 SettingScreen（设置）

```
CbScaffold
├── topBar: "设置" + 返回按钮
└── content: LazyColumn
    ├── 外观设置
    │   ├── 深色模式选择 (RadioButton 组)
    │   ├── 动态颜色开关 (Switch)
    │   └── 字体大小选择
    ├── 功能设置
    │   ├── 自动备份模式
    │   ├── 图片质量
    │   ├── 生物识别开关
    │   └── 手势密码设置
    ├── 高级设置
    │   ├── 备份与恢复（导航）
    │   ├── 清除缓存
    │   └── 导入数据
    └── 关于应用
        ├── 应用版本
        └── 更新检查
```

#### 6.3.16 BackupAndRecoveryScreen（备份恢复）

```
CbScaffold
├── topBar: "备份与恢复" + 返回按钮
└── content: LazyColumn
    ├── WebDAV 配置
    │   ├── 服务器地址输入
    │   ├── 用户名/密码输入
    │   └── 连接测试按钮
    ├── 备份管理
    │   ├── 备份文件列表
    │   ├── 本地备份按钮
    │   └── 云备份按钮
    └── 恢复管理
        ├── 文件选择
        └── 恢复按钮
```

#### 6.3.17 AboutUsScreen（关于我们）

```
CbScaffold
├── topBar: "关于我们" + 返回按钮
└── content: LazyColumn
    ├── 应用 Logo + 名称 + 版本号
    ├── 链接区（GitHub/Gitee）
    ├── 法律文件（隐私政策/用户协议 → MarkdownScreen）
    ├── 反馈区（邮件/Bug 报告）
    └── 致谢区
```

#### 6.3.18 MarkdownScreen（Markdown 展示）

```
CbScaffold
├── topBar: 标题 + 返回按钮
└── content:
    └── CashbookGradientBackground
        └── Markdown 渲染内容（支持滚动、链接、图片）
```

### 6.4 对话框与底部弹窗汇总

| 名称 | 类型 | 触发位置 | 功能 |
|------|------|---------|------|
| RecordDetailsSheet | BottomSheet | 首页/日历/搜索/分析 | 记录详情，编辑/删除/查看资产 |
| EditRecordSelectAssetBottomSheet | BottomSheet | 编辑记录 | 资产选择，支持新增 |
| EditRecordSelectTagBottomSheet | BottomSheet | 编辑记录 | 标签多选，支持新增 |
| Calculator | BottomSheet | 编辑记录 | 金额/手续费/优惠输入 |
| SelectDateDialog | AlertDialog | 首页 | 年月选择 |
| DateRangePickerDialog | AlertDialog | 日历/分析 | 日期范围选择 |
| DatePickerDialog | AlertDialog | 编辑记录 | 日期选择 |
| TimePicker | AlertDialog | 编辑记录 | 时间选择 |
| EditTagDialog | AlertDialog | 标签管理/标签选择 | 编辑/新增标签 |
| DeleteTagDialog | AlertDialog | 标签管理 | 删除标签确认 |
| ConfirmDeleteRecordDialog | AlertDialog | 记录详情 | 删除记录确认 |
| ImagePreviewDialog | Dialog | 记录详情 | 图片轮播预览 |
| ProgressDialog | Dialog | 全局 | 异步操作进度 |

---

## 7. 状态管理模式 (State Management Patterns)

### 7.1 UiState 定义模式

#### 模式一: sealed interface（推荐，用于大多数页面）

```
sealed interface XxxUiState {
    data object Loading : XxxUiState            // 加载中
    data class Success(                          // 加载完成
        val field1: Type1,
        val field2: Type2,
    ) : XxxUiState
}
```

#### 模式二: sealed class（用于需要共享属性的场景）

```
sealed class XxxUiState(
    open val sharedField: String = "",          // 共享属性
) {
    data object Loading : XxxUiState()
    data class Success(
        override val sharedField: String,       // 覆盖共享属性
        val extraField: Type,
    ) : XxxUiState()
}
```

**命名约定**: `[Feature/Screen]UiState`，如 `LauncherUiState`、`EditRecordUiState`

**设计原则**:
- 仅 Loading + Success 两态（错误通过 Bookmark/Dialog 处理）
- Success 包含所有展示数据（已格式化）
- 不存储业务逻辑，只存储 UI 最终形式

### 7.2 ViewModel 暴露状态

#### 主状态: StateFlow

```
val uiState: StateFlow<XxxUiState> = combine(source1, source2) { a, b ->
    XxxUiState.Success(...)
}.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = XxxUiState.Loading,
)
```

#### 内部可变状态: MutableStateFlow

```
private val _dateData = MutableStateFlow(YearMonth.now())
// 更新: _dateData.tryEmit(newValue)
```

#### 简单 UI 状态: Compose mutableStateOf

```
var shouldDisplayDrawerSheet by mutableStateOf(false)
    private set

var shouldDisplayBookmark by mutableStateOf(BookmarkEnum.NONE)
    private set
```

### 7.3 三层状态模型

```
+-------------------------------------------+
|  Layer 1: 页面主状态 (UiState)              |
|  - sealed interface/class                  |
|  - Loading + Success 两态                   |
|  - 包含所有展示数据                         |
+-------------------------------------------+
|  Layer 2: 临时交互状态 (Bookmark/Dialog)     |
|  - mutableStateOf<Enum> 控制提示            |
|  - mutableStateOf<DialogState> 控制弹窗     |
|  - 控制 BottomSheet 显示/隐藏               |
+-------------------------------------------+
|  Layer 3: 数据编辑状态                       |
|  - MutableStateFlow<T>                      |
|  - combine(...) 合并为 UiState               |
|  - 支持撤销/比较操作                         |
+-------------------------------------------+
```

### 7.4 Dialog/BottomSheet 状态机

#### DialogState 基类

```
sealed interface DialogState {
    data object Dismiss : DialogState              // 隐藏
    class Shown<T>(val data: T) : DialogState      // 显示（携带数据）
}
```

**使用**:
```
// 显示
dialogState = DialogState.Shown(someData)

// 隐藏
dialogState = DialogState.Dismiss

// 判断
when (dialogState) {
    is DialogState.Dismiss -> { /* 不渲染 */ }
    is DialogState.Shown<*> -> { /* 渲染弹窗，使用 data */ }
}
```

#### Bookmark 枚举（简单提示）

```
enum class EditRecordBookmarkEnum {
    NONE,                      // 无提示
    AMOUNT_MUST_NOT_BE_ZERO,   // 金额不能为零
    TYPE_NOT_MATCH_CATEGORY,   // 类型与分类不匹配
    SAVE_FAILED,               // 保存失败
}
```

- 触发: `shouldDisplayBookmark = BookmarkEnum.XXX`
- 重置: `shouldDisplayBookmark = BookmarkEnum.NONE`
- UI 侧: 通过 LaunchedEffect 监听变化，显示 Snackbar

#### BottomSheet 枚举

```
enum class EditRecordBottomSheetEnum {
    NONE, AMOUNT, CHARGES, CONCESSIONS, ASSETS, TAGS, IMAGES;

    val isCalculator: Boolean
        get() = this == AMOUNT || this == CHARGES || this == CONCESSIONS
}
```

- 显示: `bottomSheetType = EditRecordBottomSheetEnum.AMOUNT`
- 隐藏: `bottomSheetType = EditRecordBottomSheetEnum.NONE`

### 7.5 事件处理模式

**命名约定**:

| 前缀 | 用途 | 示例 |
|------|------|------|
| on | 用户交互事件 | `onPasswordClick()`, `onDateSelected(date)` |
| display/show | 显示 UI 元素 | `displayAmountSheet()`, `showFingerprintVerify()` |
| dismiss/hide | 隐藏 UI 元素 | `dismissDialog()`, `dismissBottomSheet()` |
| update | 更新数据 | `updateAmount(amount)`, `updateTypeCategory(type)` |

### 7.6 一次性事件处理

#### 导航

ViewModel 不直接执行导航，通过回调函数传递:

```
// ViewModel
fun trySave(hintText: String, onSuccess: () -> Unit) {
    viewModelScope.launch {
        if (result.isSuccess) {
            onSuccess.invoke()  // 触发导航
        }
    }
}

// Screen
trySave(hintText = "保存中...") {
    navController.popBackStack()  // 导航发生在 Composable 层
}
```

#### Toast/Snackbar

通过 Bookmark 状态 + LaunchedEffect 实现:

```
// ViewModel
var shouldDisplayBookmark by mutableStateOf("")

// UI
LaunchedEffect(shouldDisplayBookmark) {
    if (shouldDisplayBookmark.isNotBlank()) {
        snackbarHostState.showSnackbar(shouldDisplayBookmark)
        dismissBookmark()
    }
}
```

### 7.7 防重复提交

```
private var inSave = false

fun trySave(...) {
    if (inSave) return
    inSave = true
    viewModelScope.launch {
        try { ... }
        finally { inSave = false }
    }
}
```

### 7.8 数据流约定

```
数据源 (Repository/UseCase)
    |
    v
StateFlow (combine / mapLatest / flatMapLatest)
    |
    v
UiState (sealed interface)
    |
    v
UI 层 (collectAsStateWithLifecycle)
    |
    v
用户交互 --> 事件方法 --> 更新 MutableStateFlow --> StateFlow 重新 emit --> UI 重新渲染
```

---

## 8. 跨平台适配指南

### 8.1 通用适配原则

1. **设计令牌直接复用**: 颜色值、字体规格、间距数值可直接使用
2. **组件语义复用**: 组件功能和布局结构保持一致，使用各平台原生组件库
3. **状态管理模式复用**: 三层状态模型、DialogState 模式可 1:1 翻译
4. **导航结构复用**: 路由定义和页面间关系保持一致

### 8.2 iOS (SwiftUI) 适配

| Android 概念 | iOS 对应 |
|-------------|---------|
| MaterialTheme | 自定义 ThemeManager + Color/Font 扩展 |
| Compose Composable | SwiftUI View |
| StateFlow | @Published + Combine |
| mutableStateOf | @State / @StateObject |
| ViewModel | ObservableObject |
| sealed interface | enum with associated values |
| ModalNavigationDrawer | 自定义侧滑菜单或 NavigationSplitView |
| BottomSheet | .sheet / .presentationDetents |
| Navigation Compose | NavigationStack + NavigationPath |
| Hilt DI | 使用 @EnvironmentObject 或第三方 DI |
| BackdropScaffold | 自定义 ZStack + gesture |
| LazyColumn | List / LazyVStack |
| HorizontalPager | TabView(.page) |

**颜色适配**:
```swift
extension Color {
    static let cbPrimary = Color(hex: 0x006B5F)
    static let cbIncome = Color(hex: 0x006D3A)
    // ...
}
```

### 8.3 鸿蒙 HarmonyOS (ArkTS/ArkUI) 适配

| Android 概念 | 鸿蒙对应 |
|-------------|---------|
| MaterialTheme | 自定义 ThemeManager + @Styles |
| Composable | @Component struct |
| StateFlow | @State / @Link / @Provide/@Consume |
| ViewModel | ViewModel (ArkTS) |
| sealed interface | union type 或 enum |
| BottomSheet | Sheet / ActionSheet |
| Navigation | Navigation + NavRouter |
| LazyColumn | List + LazyForEach |

### 8.4 Web (React/Vue) 适配

| Android 概念 | Web 对应 |
|-------------|---------|
| MaterialTheme | CSS Variables / Theme Provider |
| Composable | React Component / Vue SFC |
| StateFlow | Zustand / Pinia store |
| mutableStateOf | useState / ref |
| ViewModel | Custom Hook / Composable |
| sealed interface | TypeScript discriminated union |
| BottomSheet | 自定义 Drawer / 第三方组件 |
| Navigation | React Router / Vue Router |
| LazyColumn | 虚拟列表 (react-window) |

**CSS Variables 示例**:
```css
:root {
    --cb-primary: #006B5F;
    --cb-on-primary: #FFFFFF;
    --cb-income: #006D3A;
    --cb-expenditure: #B02F00;
    --cb-transfer: #006398;
    /* ... */
}
[data-theme="dark"] {
    --cb-primary: #55DBC7;
    --cb-on-primary: #003731;
    --cb-income: #69DD92;
    --cb-expenditure: #FFB5A0;
    --cb-transfer: #94CCFF;
}
```

### 8.5 Flutter 适配

| Android 概念 | Flutter 对应 |
|-------------|-------------|
| MaterialTheme | ThemeData + ColorScheme |
| Composable | StatelessWidget / StatefulWidget |
| StateFlow | StreamController / Riverpod Provider |
| ViewModel | ChangeNotifier / StateNotifier |
| sealed interface | sealed class (Dart 3) |
| BottomSheet | showModalBottomSheet |
| Navigation | GoRouter / Navigator 2.0 |
| Hilt DI | Riverpod / GetIt |
| LazyColumn | ListView.builder |

**ThemeData 示例**:
```dart
final cashbookLightTheme = ThemeData(
    colorScheme: ColorScheme(
        primary: Color(0xFF006B5F),
        onPrimary: Color(0xFFFFFFFF),
        secondary: Color(0xFF904D00),
        // ...
    ),
    typography: TextTheme(
        displayLarge: TextStyle(fontSize: 57, fontWeight: FontWeight.normal),
        titleLarge: TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
        // ...
    ),
);
```

---

## 附录 A: 设计令牌层次结构

```
CashbookTheme
├── Material ColorScheme (23 个标准色)
│   ├── Primary (4)
│   ├── Secondary (4)
│   ├── Tertiary (4)
│   ├── Error (4)
│   ├── Neutral (surface/background/outline, 7+)
│   └── Special (inverse/shadow/scrim)
├── ExtendedColors (32 个业务色)
│   ├── 交易类型 (income/expenditure/transfer, 各4)
│   ├── 选择状态 (selected/unselected/quaternary, 各4)
│   └── 第三方 (github/gitee, 各4)
├── Typography (15 种文本样式)
│   ├── Display (3)
│   ├── Headline (3)
│   ├── Title (3)
│   ├── Body (3)
│   └── Label (3)
├── GradientColors (top/bottom/container)
├── BackgroundTheme (color/tonalElevation)
└── TintTheme (iconTint)
```

## 附录 B: 设备预览配置

| 设备 | 宽度 | 高度 | 主题 |
|------|------|------|------|
| 手机竖屏 (亮色) | 360dp | 640dp | Light |
| 手机竖屏 (暗色) | 360dp | 640dp | Dark |
| 手机横屏 | 640dp | 360dp | Light |
| 可折叠设备 | 673dp | 841dp | Light |
| 平板 | 1280dp | 800dp | Light |

## 附录 C: 全局 CompositionLocal 注入列表

| Provider | 类型 | 用途 |
|----------|------|------|
| LocalExtendedColors | ExtendedColors | 业务扩展颜色 |
| LocalGradientColors | GradientColors | 渐变配色 |
| LocalBackgroundTheme | BackgroundTheme | 背景配置 |
| LocalTintTheme | TintTheme | 图标色调 |
| LocalNavController | NavController | 导航控制器 |
| LocalBackPressedDispatcher | OnBackPressedDispatcher | 返回事件分发 |
| LocalDefaultLoadingHint | String | Loading 默认提示文本 |
| LocalDefaultEmptyImagePainter | Painter | Empty 默认图片 |
| LocalProgressDialogHint | String | 进度弹窗默认提示 |
