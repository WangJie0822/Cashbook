# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

Cashbook 是一个 Android 记账应用，使用 Kotlin + Jetpack Compose 构建，采用多模块架构。支持多账本、资产管理、标签、统计图表、WebDAV 备份等功能。

## 常用命令

### 构建

```bash
# 构建所有 flavor 的 APK
./gradlew :app:assemble

# 构建特定 flavor (Online/Offline/Canary/Dev) + buildType (Debug/Release)
./gradlew :app:assembleOnlineDebug
./gradlew :app:assembleOfflineRelease
```

### 代码格式检查 (Spotless)

```bash
# 检查格式
./gradlew spotlessCheck --init-script gradle/init.gradle.kts --no-configuration-cache

# 自动修复格式
./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache

# build-logic 的格式修复
./gradlew -p build-logic spotlessApply --init-script ../gradle/init.gradle.kts --no-configuration-cache
```

### Lint

```bash
./gradlew :app:lintOnlineRelease :app:lintOfflineRelease :app:lintDevRelease :lint:lint -Dlint.baselines.continue=true
```

### 测试

```bash
# 单元测试 (指定 flavor)
./gradlew testOnlineDebugUnitTest
./gradlew testOfflineDebugUnitTest

# 运行单个模块的测试
./gradlew :core:data:testDebugUnitTest
./gradlew :feature:records:testDebugUnitTest

# 截图测试 (Roborazzi)
./gradlew verifyRoborazziOnlineDebug   # 校验
./gradlew recordRoborazziOnlineDebug   # 生成基准截图
```

## 架构

### 分层结构

```
app → feature/* → core/*
```

- **app**: 主入口，Hilt Application，导航设置，多渠道配置
- **feature/**: 功能模块 (tags, types, books, assets, records, settings)
- **core/**: 基础模块

### 核心模块职责

| 模块 | 职责 |
|------|------|
| `core/domain` | UseCase 业务逻辑 |
| `core/data` | Repository 数据仓库 |
| `core/database` | Room 数据库，schema 在 `core/database/schemas/` |
| `core/datastore` | Proto DataStore 偏好存储 |
| `core/datastore-proto` | Proto 定义 (JVM 库) |
| `core/network` | Retrofit + OkHttp 网络层 |
| `core/model` | 数据模型 (JVM 库，Compose 稳定性标注) |
| `core/design` | 设计系统、主题、通用 Composable |
| `core/ui` | 业务相关 UI 组件 |
| `core/common` | 公共工具、常量、BuildConfig |
| `core/testing` | 测试工具、自定义 TestRunner |

### Convention Plugins (build-logic/)

所有模块配置通过 `build-logic/convention/` 下的约定插件统一管理，插件 ID 定义在 `gradle/convention.versions.toml`，以 `cashbook.*` 为前缀。Feature 模块统一使用 `cashbook.android.library.feature` 插件，自动包含 Compose、Hilt、Jacoco 及 `:core:design`、`:core:ui` 依赖。

### 关键技术栈

- **DI**: Hilt (通过 KSP)
- **UI**: Jetpack Compose (BOM 管理版本)
- **导航**: Navigation Compose + Hilt Navigation
- **数据库**: Room
- **网络**: Retrofit 3 + OkHttp + Kotlin Serialization
- **异步**: Kotlin Coroutines + Flow
- **同步**: WorkManager (`sync/work` 模块)
- **图表**: Compose Canvas 自绘制 (`core/design` 中的 CbPieChart、CbLineChart)

### 多渠道 (Product Flavors)

- **Online**: 在线版 (支持网络更新检查)
- **Offline**: 离线版
- **Canary**: 预览版
- **Dev**: 开发版

### 签名配置

本地构建 Release 需要在 `gradle/signing.versions.toml` 中配置签名信息（该文件不入库）。

## 代码规范

- 源文件需包含 Apache 2.0 License Header（Spotless 自动检查，模板在 `spotless/` 目录）
- Kotlin 格式化使用 ktlint (android mode)
- 自定义 Lint 规则在 `lint/` 模块，通过 `:core:design` 发布
- **禁止在 `app/`、`feature/`、`core/ui/` 中直接使用以下 Material3 组件，必须使用 `core/design` 中对应的设计系统封装：**

  | Material3 组件 | 项目封装 |
  |---|---|
  | `MaterialTheme()` | `CashbookTheme` |
  | `TopAppBar` | `CbTopAppBar` |
  | `Scaffold` | `CbScaffold` |
  | `Divider` | `CbHorizontalDivider` / `CbVerticalDivider` |
  | `TextField` | `CbTextField` |
  | `OutlinedTextField` | `CbOutlinedTextField` |
  | `FloatingActionButton` | `CbFloatingActionButton` |
  | `SmallFloatingActionButton` | `CbSmallFloatingActionButton` |
  | `ListItem` | `CbListItem` |
  | `ModalBottomSheet` | `CbModalBottomSheet` |
  | `TextButton` | `CbTextButton` |
  | `IconButton` | `CbIconButton` |
  | `Card` | `CbCard` |
  | `ElevatedCard` | `CbElevatedCard` |
  | `AlertDialog` | `CbAlertDialog` |
  | `BaseAlterDialog` | `CbBaseAlterDialog` |
  | `Tab` | `CbTab` |
  | `TabRow` | `CbTabRow` |
  | `Icons` | `CbIcons` |

  > 访问 `MaterialTheme.colorScheme`/`.typography`/`.shapes` 属性是允许的，仅禁止作为 composable 调用。
  > 违反此规则会触发 lint `Design` error，构建将中止。
- `compose_compiler_config.conf` 声明了 `core/model` 中模型类的 Compose 稳定性
- 测试使用自定义 TestRunner: `cn.wj.android.cashbook.core.testing.CashbookTestRunner`
- 包名: `cn.wj.android.cashbook`

## 规范（强制）
- 所有修改新增功能必须确认是否新增对应测试，功能开发必须在测试通过才算完成
- 