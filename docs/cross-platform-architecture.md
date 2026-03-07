# Cashbook 跨平台架构设计文档

## 1. 文档概述

### 1.1 目的

本文档基于 Cashbook Android 项目的现有架构，提炼出**通用可复用的核心架构逻辑**，为 iOS、鸿蒙、Web 等平台提供可参考的架构范式。文档将明确区分「跨平台通用层」和「Android 系统特有层」，帮助其他平台开发人员快速理解核心业务架构并进行适配开发。

### 1.2 适用平台

- iOS (Swift/SwiftUI)
- 鸿蒙 HarmonyOS (ArkTS/ArkUI)
- Web (TypeScript/React 或 Vue)
- Flutter (Dart)
- KMP (Kotlin Multiplatform)

### 1.3 核心复用目标

| 优先级 | 模块 | 复用方式 |
|--------|------|----------|
| P0 | 数据模型 (Model) | 直接翻译为目标语言 |
| P0 | 业务逻辑 (UseCase) | 算法和流程逻辑 1:1 复用 |
| P0 | 数据库 Schema | SQLite 通用，直接复用 |
| P1 | Repository 接口 | 接口契约复用，实现各平台适配 |
| P1 | 网络 API 协议 | RESTful 接口定义直接复用 |
| P2 | 状态管理模式 | 架构模式复用，具体实现适配 |
| P3 | UI 组件结构 | 页面结构和导航关系参考 |

---

## 2. 整体架构总览

### 2.1 Android 项目原有架构

Cashbook 采用 **Clean Architecture + MVVM + 多模块** 架构，分为三个主要层级：

```
┌─────────────────────────────────────────────────────┐
│                    App 层                            │
│  MainActivity, NavHost, Hilt Application, 主题配置    │
├─────────────────────────────────────────────────────┤
│                Feature 层 (6 个功能模块)               │
│  records │ assets │ books │ tags │ types │ settings  │
│  (ViewModel + Compose Screen)                        │
├─────────────────────────────────────────────────────┤
│                 Core 层 (11 个核心模块)                │
│  domain │ data │ database │ datastore │ network      │
│  model │ design │ ui │ common │ testing │ proto      │
├─────────────────────────────────────────────────────┤
│                 Sync 层                              │
│  WorkManager (自动同步、自动备份、APK下载)              │
└─────────────────────────────────────────────────────┘
```

**模块依赖方向**：`app → feature/* → core/domain → core/data → core/database + core/datastore + core/network`

### 2.2 跨平台通用架构层提炼

```
┌─────────────────────────────────────────────────────┐
│           【平台特有】 UI / 表现层                      │
│  页面渲染、导航、系统主题、权限、通知                      │
├─────────────────────────────────────────────────────┤
│           【通用复用】 状态管理层                        │
│  UI State 定义、用户交互事件处理                        │
├─────────────────────────────────────────────────────┤
│           【通用复用】 业务逻辑层 (UseCase)              │
│  记录管理、资产管理、统计分析、备份恢复                    │
├─────────────────────────────────────────────────────┤
│           【通用复用】 数据仓库层 (Repository 接口)      │
│  数据聚合、缓存策略、数据版本管理                         │
├─────────────────────────────────────────────────────┤
│           【部分通用】 数据源层                          │
│  数据库(SQLite通用) │ 偏好存储(各平台适配) │ 网络(HTTP通用)│
├─────────────────────────────────────────────────────┤
│           【通用复用】 数据模型层 (Model)                │
│  实体定义、枚举、数据转换                                │
└─────────────────────────────────────────────────────┘
```

### 2.3 Android 系统特有层剥离

以下是需要在其他平台重新实现的 Android 特有依赖：

| Android 特有组件 | 功能 | 其他平台替代方案 |
|-----------------|------|-----------------|
| Jetpack Compose | UI 渲染 | SwiftUI / ArkUI / React / Flutter Widget |
| ViewModel | 状态持有 + 生命周期感知 | iOS: ObservableObject / 鸿蒙: @State / Web: Store |
| Room | SQLite ORM | iOS: GRDB/CoreData / 鸿蒙: RDB / Web: IndexedDB/sql.js |
| Proto DataStore | 键值对偏好存储 | iOS: UserDefaults / 鸿蒙: Preferences / Web: localStorage |
| Hilt | 依赖注入 | iOS: Swinject / 鸿蒙: 手动DI / Web: InversifyJS |
| WorkManager | 后台任务调度 | iOS: BGTaskScheduler / 鸿蒙: WorkScheduler / Web: Service Worker |
| Navigation Compose | 页面导航 | iOS: NavigationStack / 鸿蒙: Router / Web: React Router |
| ConnectivityManager | 网络状态监听 | iOS: NWPathMonitor / 鸿蒙: NetConnection / Web: navigator.onLine |
| OkHttp + Retrofit | HTTP 客户端 | iOS: URLSession / 鸿蒙: @ohos.net.http / Web: fetch/axios |
| AndroidKeyStore | 安全存储 | iOS: Keychain / 鸿蒙: HUKS / Web: Web Crypto API |
| BiometricPrompt | 指纹/面容认证 | iOS: LocalAuthentication / 鸿蒙: UserAuth / Web: WebAuthn |

---

## 3. 核心模块划分与职责（跨平台视角）

### 3.1 通用模块

#### 3.1.1 数据模型层 (Model) 【通用复用】

**职责**：定义所有核心业务数据结构，不依赖任何平台 API。

**核心模型**：
- `RecordModel` - 交易记录（金额、类型、资产、时间、备注等）
- `AssetModel` - 资产账户（余额、分类、银行信息、信用卡信息等）
- `BooksModel` - 账本（名称、描述、背景）
- `RecordTypeModel` - 记录分类（支出/收入/转账，支持一级/二级层级）
- `TagModel` - 标签（名称、可见性）
- `ImageModel` - 图片附件（路径、二进制数据）

**核心枚举**：
- `RecordTypeCategoryEnum` - 收支类型（EXPENDITURE / INCOME / TRANSFER）
- `AssetClassificationEnum` - 资产分类（30+ 种，含现金、微信、支付宝、银行卡、信用卡等）
- `ClassificationTypeEnum` - 资产大类（资金/信用卡/充值/投资理财/债务）
- `DarkModeEnum` - 主题模式
- `AutoBackupModeEnum` - 自动备份策略

**跨平台适配说明**：
- 所有模型使用基础数据类型（Long、String、Boolean、Double），无平台依赖
- 金额统一使用 `String` 类型（避免浮点精度问题），数据库层使用 `Double`
- 时间使用 `String`（格式 `yyyy-MM-dd HH:mm:ss`）或 `Long`（时间戳毫秒）
- 唯一例外：`AssetModel.iconResId` 和 `AssetTypeViewsModel.nameResId` 为 Android 资源 ID

#### 3.1.2 业务逻辑层 (UseCase / Domain) 【通用复用】

**职责**：封装纯业务逻辑，不依赖任何平台 API，仅依赖 Repository 接口。

**核心 UseCase（23 个）**：

| UseCase | 职责 | 输入 | 输出 |
|---------|------|------|------|
| SaveRecordUseCase | 保存/更新记录及关联 | RecordModel, 标签/图片/关联记录 | Unit |
| DeleteRecordUseCase | 删除记录 | recordId | Unit |
| GetRecordViewsUseCase | 获取记录详情视图 | recordId | RecordViewsEntity? |
| GetRecordViewsBetweenDateUseCase | 按日期范围查询记录 | fromDate, toDate | List |
| GetCurrentMonthRecordViewsUseCase | 获取当月记录流 | year, month | Flow |
| GetCurrentMonthRecordViewsMapUseCase | 按日期分组（含每日收支统计） | recordList | Map |
| GetDefaultRecordUseCase | 获取默认记录模板 | typeId | RecordModel |
| SaveAssetUseCase | 保存资产（含余额差自动平账） | AssetModel | Unit |
| GetAssetListUseCase | 获取资产列表（按使用频率排序） | typeId, selectedAssetId | Flow |
| GetDefaultAssetUseCase | 获取默认资产 | - | AssetModel? |
| GetRecordTypeListUseCase | 获取分类列表（支持二级展开） | typeCategory, selectedId | List |
| GetSelectableBooksListUseCase | 获取可选账本列表 | - | Flow |
| GetSelectableVisibleTagListUseCase | 获取可选标签列表 | recordId | Flow |
| TransRecordViewsToAnalyticsPieUseCase | 转换为饼图统计数据（一级分类） | typeCategory, recordList | List |
| TransRecordViewsToAnalyticsPieSecondUseCase | 转换为饼图统计数据（二级分类下钻） | typeId, recordList | List |
| TransRecordViewsToAnalyticsBarUseCase | 转换为柱状图统计数据 | dateRange, recordList | List |
| RecordModelTransToViewsUseCase | 记录模型转显示视图模型 | RecordModel | RecordViewsEntity |
| GetAssetRecordViewsUseCase | 获取资产关联记录列表 | assetId, page | List |
| GetSearchRecordViewsUseCase | 搜索记录列表 | keyword, page | List |
| GetTypeRecordViewsUseCase | 获取类型关联记录列表 | typeId, page | List |
| GetTagRecordViewsUseCase | 获取标签关联记录列表 | tagId, page | List |
| GetRelatedRecordViewsUseCase | 获取关联记录视图列表 | recordId | List |
| GetDefaultRelatedRecordListUseCase | 获取默认可关联记录列表 | typeId | List |

**跨平台适配说明**：
- UseCase 内部逻辑为纯计算和数据转换，可直接翻译到任何语言
- 异步通过协程 `suspend` 函数实现，其他平台替换为：iOS async/await、Web Promise/async、鸿蒙 TaskPool
- `Flow` 数据流替换为：iOS Combine Publisher、Web RxJS Observable、鸿蒙 @Watch

#### 3.1.3 数据仓库层 (Repository 接口) 【通用复用】

**职责**：定义数据访问的抽象接口，隔离数据源实现。

**6 个核心 Repository**：

| Repository | 核心方法 | 数据源 |
|------------|---------|--------|
| RecordRepository | CRUD、分页查询、关键字搜索、日期范围查询 | DB + DataStore |
| AssetRepository | CRUD、按分类聚合、余额管理 | DB + DataStore |
| BooksRepository | CRUD、当前账本切换、重名检查 | DB + DataStore |
| TypeRepository | CRUD、层级管理、特殊类型标记 | DB + DataStore |
| TagRepository | CRUD、关联管理 | DB |
| SettingRepository | 应用设置读写、版本检查、备份恢复 | DataStore + Network + File |

**版本驱动更新机制**（关键设计）：
- 每个数据域维护一个 `dataVersion` 计数器（recordDataVersion、assetDataVersion、typeDataVersion、tagDataVersion、booksDataVersion）
- 数据变更时递增对应版本号，触发所有相关 Flow 重新计算
- 避免全表监听的性能开销
- 版本计数器存储在 DataStore/偏好存储中，各 Repository 的数据流通过 `flatMapLatest(dataVersion)` 监听版本变化
- 详细机制见《跨平台详细设计文档》section 4.8

#### 3.1.4 网络 API 协议 【通用复用】

**API 端点**：
- GitHub Release API: `GET https://api.github.com/repos/{owner}/{repo}/releases`
- Gitee Release API: `GET https://gitee.com/api/v5/repos/{owner}/{repo}/releases`
- 参数: `page`, `per_page`, `direction`
- 响应: JSON（`GitReleaseEntity`）

**WebDAV 协议**（备份同步）：
- `HEAD` - 检查文件存在
- `MKCOL` - 创建目录
- `PUT` - 上传文件
- `PROPFIND` - 列出文件
- `GET` - 下载文件
- 认证: HTTP Basic Authentication

### 3.2 Android 特有模块

#### 3.2.1 UI 层 【Android特有】

| 功能 | Android 实现 | 替代方案建议 |
|------|-------------|-------------|
| 声明式 UI | Jetpack Compose | 【适配指导】iOS: SwiftUI；鸿蒙: ArkUI (Build 函数)；Web: React JSX；Flutter: Widget |
| 页面导航 | Navigation Compose (单 Activity) | 【适配指导】iOS: NavigationStack + NavigationPath；鸿蒙: router.pushUrl()；Web: React Router |
| 底部弹窗 | ModalBottomSheet | 【适配指导】iOS: .sheet()；鸿蒙: Panel；Web: Modal/Drawer 组件 |
| 主题系统 | MaterialTheme + DynamicColor | 【适配指导】iOS: @Environment(\.colorScheme)；鸿蒙: AppTheme；Web: CSS Variables |
| 启动页 | SplashScreen API | 【适配指导】iOS: LaunchScreen.storyboard；鸿蒙: WindowStage |

#### 3.2.2 系统服务层 【Android特有】

| 功能 | Android 实现 | 替代方案建议 |
|------|-------------|-------------|
| 后台任务 | WorkManager (SyncWorker, AutoBackupWorker) | 【适配指导】iOS: BGTaskScheduler / BGAppRefreshTask；鸿蒙: WorkSchedulerExtensionAbility；Web: Service Worker + Periodic Sync |
| 前台服务 | UpgradeService (Foreground Service) | 【适配指导】iOS: URLSession background download；鸿蒙: ServiceExtensionAbility；Web: 不需要，浏览器内下载 |
| 通知 | NotificationChannel + NotificationCompat | 【适配指导】iOS: UNUserNotificationCenter；鸿蒙: @ohos.notificationManager；Web: Notification API |
| 安全认证 | BiometricPrompt + AndroidKeyStore | 【适配指导】iOS: LAContext (Face ID/Touch ID) + Keychain；鸿蒙: @ohos.userIAM.userAuth；Web: WebAuthn |
| 快捷方式 | ShortcutManager | 【适配指导】iOS: UIApplicationShortcutItem；鸿蒙: Shortcut Ability；Web: 不适用 |
| 文件存储 | SAF (Storage Access Framework) | 【适配指导】iOS: FileManager + UIDocumentPickerViewController；鸿蒙: @ohos.file.fs；Web: File System Access API |

---

## 4. 核心交互流程（跨平台通用版）

### 4.1 记录创建/编辑流程

```
1. 用户进入记录编辑页面
   - 若为编辑模式：通过 recordId 加载已有记录数据 (GetDefaultRecordUseCase)
   - 若为新建模式：加载默认记录模板（默认类型、上次使用的资产）
2. 用户选择记录类型（支出/收入/转账）
   - 加载对应分类列表 (GetRecordTypeListUseCase)
   - 支持一级→二级分类展开选择
3. 用户输入金额
   - 计算最终金额：
     · 支出/转账：finalAmount = amount + charges - concessions
     · 收入：finalAmount = amount - charges
   - 校验金额不能为零
4. 用户选择资产账户
   - 加载可用资产列表 (GetAssetListUseCase)
   - 按最近3个月使用频率排序
   - 转账模式需选择转入资产
5. 用户可选操作：添加标签、关联图片、关联记录、设置可报销
6. 保存记录 (SaveRecordUseCase)
   - **编辑已有记录时**：先删除旧记录（回退旧记录对资产余额的影响），再按新建流程插入
   - 计算最终金额（支出/转账: amount + charges - concessions; 收入: amount - charges）
   - 保存记录主体到数据库
   - 更新资产余额（资金类增减余额，信用卡类增减已用额度）
   - 转账时同步更新转入资产余额
   - 保存标签关联关系
   - 保存图片数据
   - 保存关联记录：将关联记录的 finalAmount 置 0，主记录 finalAmount = 计算金额 - 关联金额之和
   - 更新上次使用资产 ID
   - 递增 recordDataVersion 和 assetDataVersion，触发相关数据流刷新
```

### 4.2 资产管理流程

```
1. 展示资产概览
   - 按大类分组显示（资金账户/信用卡/充值账户/投资理财/债务）
   - 计算各类总额和净资产
   - 区分可见/隐藏资产
2. 新建资产
   - 选择资产分类 → 输入名称、余额
   - 信用卡类型额外填写：总额度、账单日、还款日
   - 银行卡类型额外填写：开户行、卡号
3. 编辑资产 (SaveAssetUseCase)
   - 更新基本信息
   - 若余额发生变化，自动生成"平账"记录：
     · 普通账户：余额增加→生成平账收入，余额减少→生成平账支出
     · 信用卡账户：已用额度增加→生成平账支出，已用额度减少→生成平账收入
   - 递增 assetDataVersion 触发刷新
4. 删除资产
   - 删除资产下所有记录
   - 删除资产关联关系
   - 删除资产本身
```

### 4.3 统计分析流程

```
1. 选择统计维度
   - 时间范围：按年 / 按月 / 自定义日期范围
   - 类型：支出 / 收入 / 余额
2. 获取数据 (GetRecordViewsBetweenDateUseCase)
   - 查询指定时间范围内的所有记录
   - 转换为显示模型（包含类型名称、资产名称等）
3. 生成饼图数据 (TransRecordViewsToAnalyticsPieUseCase)
   - 按一级分类汇总金额
   - 支出计算：amount + charges - concessions
   - 收入计算：amount - charges
   - 计算各分类占比，按占比降序排列
   - 支持下钻到二级分类 (TransRecordViewsToAnalyticsPieSecondUseCase)
4. 生成柱状图数据 (TransRecordViewsToAnalyticsBarUseCase)
   - 按年统计：12 个月的柱状图
   - 按月统计：每天的柱状图
   - 每个时间段计算总收入、总支出、余额
```

### 4.4 备份与恢复流程

```
1. 备份流程
   a. 用户触发备份（手动）或系统触发（自动备份策略）
   b. 检查网络环境（是否允许流量备份）
   c. 导出数据库文件（SQLite .db 文件）
   d. 打包为 Zip 文件，在 Zip 注释(comment)中写入校验标识
      · 校验标识用于恢复时验证备份文件的完整性和来源合法性
      · 各平台需使用相同的校验标识格式以实现跨平台恢复
   e. 保存到本地路径
   f. 如配置了 WebDAV，上传到远端
   g. 根据 keepLatestBackup 设置清理旧备份
2. 恢复流程
   a. 获取备份列表（本地 / WebDAV）
   b. 用户选择备份文件
   c. 下载（WebDAV）或读取（本地）备份文件
   d. 解压并验证完整性（读取 Zip 注释，比对校验标识）
   e. 校验数据库版本，必要时执行迁移（如 v8 → v11）
   f. 替换当前数据库文件
   g. 重置所有 dataVersion 计数器，触发全部数据流刷新
```

### 4.5 版本更新检查流程

```
1. 启动时或定期（WorkManager）触发版本检查
2. 调用 GitHub/Gitee Release API 获取最新版本
3. 比较本地版本号与远程版本号
4. 检查是否为忽略版本
5. 若需更新：展示更新弹窗（版本号、更新日志）
6. 用户确认后下载 APK（通过 WorkManager 后台下载）
7. 显示下载进度通知
8. 下载完成后触发安装
```

---

## 5. 关键设计决策（跨平台适配）

### 5.1 数据存储设计

**数据库方案**：

| 层面 | Android 方案 | 跨平台通用方案 |
|------|-------------|---------------|
| ORM | Room | 【适配指导】iOS: GRDB (Swift) / FMDB；鸿蒙: @ohos.data.relationalStore；Web: sql.js + IndexedDB；KMP: SQLDelight |
| SQL 语法 | SQLite 3 | 【通用复用】所有平台均可使用 SQLite，SQL 语句直接复用 |
| 迁移 | Room Migration (v1→v11) | 【适配指导】各平台需实现相同的迁移 SQL，迁移逻辑可直接复用 |
| 事务 | @Transaction 注解 | 【适配指导】各平台 SQLite API 均支持事务，逻辑复用 |

**键值对存储**：

| Android 方案 | 跨平台通用方案 |
|-------------|---------------|
| Proto DataStore (6 个存储) | 【适配指导】iOS: UserDefaults / Keychain (敏感数据)；鸿蒙: @ohos.data.preferences；Web: localStorage (非敏感) / IndexedDB (结构化)；简化方案：JSON 文件存储 |

**关键适配建议**：Proto DataStore 的 6 个存储可简化为 3 个 JSON 配置文件：
- `app_settings.json` - 应用设置
- `record_settings.json` - 记录设置
- `git_infos.json` - 版本信息

### 5.2 网络交互设计

【通用复用】网络层核心逻辑：
- API 协议：标准 RESTful + JSON
- 仅 2 个外部 API 端点（GitHub/Gitee Release）
- WebDAV 标准协议（备份同步）
- HTTP Basic 认证

| Android 方案 | 跨平台适配 |
|-------------|-----------|
| Retrofit + OkHttp | 【适配指导】iOS: URLSession + Alamofire；鸿蒙: @ohos.net.http；Web: fetch/axios；KMP: Ktor |
| Kotlin Serialization | 【适配指导】iOS: Codable；鸿蒙: JSON.parse；Web: JSON.parse；通用: 各语言 JSON 库 |
| 日志拦截器 | 【适配指导】各 HTTP 客户端均支持请求/响应日志中间件 |

### 5.3 状态管理设计

**Android 核心模式**：ViewModel + StateFlow + combine

```
ViewModel 持有:
  - 多个 MutableStateFlow（用户输入的可变状态）
  - 多个 Repository Flow（数据库驱动的响应式数据）
  - combine() 合并 → UiState (sealed interface: Loading / Success)
```

**跨平台状态管理方案**：

| 平台 | 状态持有 | 响应式数据流 | 状态合并 |
|------|---------|------------|---------|
| iOS | ObservableObject + @Published | Combine Publisher | combineLatest |
| 鸿蒙 | @State + @Observed | EventHub / emitter | 组合响应式属性 |
| Web/React | useReducer / Zustand | RxJS / React Query | useMemo + 依赖数组 |
| Flutter | ChangeNotifier / Riverpod | Stream / StreamBuilder | Riverpod combine |
| KMP | ViewModel (common) | kotlinx.coroutines Flow | combine (共享) |

**UI 状态模式复用**（所有平台统一）：
```
sealed UiState {
    object Loading
    data class Success(data: T)
    data class Error(message: String)
}
```

### 5.4 依赖注入设计

Android 使用 Hilt（基于编译期注解处理），其他平台建议：

| 平台 | 推荐方案 |
|------|---------|
| iOS | Swinject / 手动构造函数注入 |
| 鸿蒙 | 手动构造函数注入 / 简单 ServiceLocator |
| Web | InversifyJS / tsyringe / 手动模块导出 |
| Flutter | get_it / Riverpod |
| KMP | Koin (支持多平台) |

---

## 6. 跨平台架构约束与建议

### 6.1 需统一的核心规范

1. **数据模型命名**：使用驼峰命名（`recordTime`），数据库字段使用蛇形命名（`record_time`）
2. **金额处理**：统一使用字符串传递，内部计算使用高精度类型，避免浮点精度问题
3. **时间格式**：数据库存储使用 `Long` 时间戳（毫秒），显示层使用 `yyyy-MM-dd HH:mm:ss` 格式字符串
4. **异常码定义**：
   - `-1687` 通用异常
   - `-1688` 金额不能为零
   - `-1689` 类型不能为空
   - `-1690` 类型与分类不匹配
5. **备份文件格式**：Zip 压缩包，包含 SQLite 数据库文件，Zip 注释中写入校验标识（用于恢复时验证文件完整性和来源合法性），各平台需使用相同的校验标识格式
6. **数据库版本**：所有平台保持一致的 schema 版本号（当前 v11）和迁移路径（v1→v11 共 10 次迁移）
7. **金额精度**：数据库层当前使用 `REAL`(Double) 存储金额，存在浮点精度风险。各平台实现时应在业务层使用高精度类型（iOS: Decimal、Web: decimal.js、Kotlin: BigDecimal）进行计算，仅在数据库读写时转换。**长期建议**：数据库层改用 `INTEGER` 存储分（金额×100），从根本上消除精度问题

### 6.2 各平台需适配的差异点

| 差异项 | 关键点 |
|--------|--------|
| UI 渲染 | 各平台使用原生声明式 UI 框架，页面结构和导航逻辑参考 Android |
| 文件系统 | iOS 沙盒机制、Web 同源策略、鸿蒙应用沙箱，需适配文件路径处理 |
| 后台任务 | iOS 严格限制后台执行时间（约30秒），Web 无后台能力（Service Worker 有限），需简化自动备份策略 |
| 安全存储 | 密码和认证信息需使用各平台安全存储 API，不可明文存储 |
| 图片处理 | `ImageModel.bytes` 使用 ByteArray 存储，各平台需适配图片压缩和格式转换 |
| 系统通知 | iOS/Web 需用户主动授权通知权限，鸿蒙需声明权限 |
| 资源引用 | Android 使用 `R.drawable.*` 和 `R.string.*`，其他平台需建立等价的资源映射表 |

### 6.3 架构复用优先级

| 优先级 | 模块 | 复用策略 | 工作量评估 |
|--------|------|---------|-----------|
| **P0** | core/model | 直接翻译所有 data class 和 enum | 低 |
| **P0** | core/domain (23 UseCase) | 翻译业务逻辑算法，替换协程为本地异步 | 低~中 |
| **P0** | 数据库 Schema (8 表 + 迁移) | 复用 SQL DDL 和迁移脚本 | 低 |
| **P1** | core/data (6 Repository 接口) | 接口契约翻译，实现层各平台适配 | 中 |
| **P1** | core/network (API 定义) | 复用 API 端点和请求/响应模型 | 低 |
| **P2** | UI State 定义 | 翻译 sealed class 结构，ViewModel 逻辑重新组织 | 中 |
| **P2** | 备份恢复逻辑 | 核心 Zip 逻辑复用，文件系统和 WebDAV 操作适配 | 中~高 |
| **P3** | 导航结构和页面组织 | 参考页面层级关系，UI 全部重新实现 | 高 |
| **P3** | 后台同步和通知 | 各平台后台能力差异大，需重新设计 | 高 |

### 6.4 优化建议（针对跨平台不利设计）

1. **【优化建议】资源 ID 耦合**：`AssetModel.iconResId` 和 `AssetTypeViewsModel.nameResId` 使用 Android 资源 ID（Int），建议改为字符串标识（如 `"ic_cash"`），各平台建立资源名到本地资源的映射表
2. **【优化建议】AssetHelper 硬编码**：30+ 种资产分类的图标和名称映射硬编码在 `AssetHelper` 中，建议提取为配置文件或枚举属性
3. **【优化建议】Context 依赖**：`BackupRecoveryManager` 和 `SettingRepository` 直接注入 Android `Context`，建议抽象为平台无关的 `FileSystemProvider` 接口
4. **【优化建议】WebDAV XML 解析**：使用 JSoup（Android HTML 解析库）解析 PROPFIND XML 响应，建议改用标准 XML 解析器或封装为抽象接口
