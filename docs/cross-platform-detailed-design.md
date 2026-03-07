# Cashbook 跨平台详细设计文档

## 1. 文档概述

### 1.1 复用范围

本文档拆解 Cashbook 项目的核心业务逻辑、数据模型和接口定义，为 iOS、鸿蒙、Web、Flutter 等平台提供可直接落地的实现参考。每个模块均明确标注「通用复用」和「Android 特有」部分。

### 1.2 适配指导原则

1. **数据模型优先**：先翻译所有 Model 和 Enum，确保数据结构一致
2. **接口契约对齐**：Repository 接口保持一致的方法签名和语义
3. **业务逻辑直译**：UseCase 的算法逻辑可 1:1 翻译，仅替换异步原语
4. **UI 独立实现**：各平台使用原生 UI 框架，参考页面结构和状态管理模式

---

## 2. 核心数据模型设计（跨平台通用）

### 2.1 交易记录模型 RecordModel 【通用复用】

| 字段名 | Kotlin 类型 | 通用类型 | 含义 | 约束 | 跨平台适配 |
|--------|------------|---------|------|------|-----------|
| id | Long | Int64 | 主键 | 自增，默认 -1L 表示新记录 | iOS: Int64; Web: number; 鸿蒙: number |
| booksId | Long | Int64 | 所属账本 ID | FK → BooksModel.id | 同上 |
| typeId | Long | Int64 | 记录类型 ID | FK → RecordTypeModel.id | 同上 |
| assetId | Long | Int64 | 关联资产 ID | FK → AssetModel.id, -1L 表示无 | 同上 |
| relatedAssetId | Long | Int64 | 转入资产 ID | 仅转账时使用, -1L 表示无; **数据库层字段名为 `intoAssetId`（列名 `into_asset_id`）** | 同上 |
| amount | String | String | 原始金额 | 不能为零, 使用字符串避免精度丢失 | 各平台统一使用 String |
| finalAmount | String | String | 最终金额 | 支出/转账: amount + charges - concessions; 收入: amount - charges | 同上 |
| charges | String | String | 手续费 | 默认 "0"; **数据库层字段名为 `charge`（单数）** | 同上 |
| concessions | String | String | 优惠折让 | 默认 "0" | 同上 |
| remark | String | String | 备注 | 可为空串 | 直接复用 |
| reimbursable | Boolean | Boolean | 是否可报销 | 默认 false | 直接复用 |
| recordTime | String | String | 记录时间 | 格式 "yyyy-MM-dd HH:mm:ss"; **数据库层类型为 Long（时间戳毫秒）** | 直接复用 |

> **Model ↔ Database 层字段映射说明**：RecordModel 与数据库表 `db_record` 之间存在以下命名和类型差异，Repository 层负责转换：
> - `charges`(String) ↔ `charge`(Double)
> - `relatedAssetId`(Long) ↔ `intoAssetId`(Long)，列名 `into_asset_id`
> - `amount`/`finalAmount`/`concessions`(String) ↔ 数据库中均为 Double
> - `recordTime`(String, "yyyy-MM-dd HH:mm:ss") ↔ 数据库中为 Long（时间戳毫秒）
> - `reimbursable`(Boolean) ↔ 数据库中为 Int（0/1）

### 2.2 资产模型 AssetModel 【部分通用】

| 字段名 | Kotlin 类型 | 通用类型 | 含义 | 约束 | 跨平台适配 |
|--------|------------|---------|------|------|-----------|
| id | Long | Int64 | 主键 | 自增 | 通用 |
| booksId | Long | Int64 | 所属账本 ID | FK | 通用 |
| name | String | String | 资产名称 | 非空 | 通用 |
| iconResId | Int | **N/A** | 图标资源 ID | **Android R.drawable** | 【Android特有】替换为 String 资源标识符，如 "ic_cash"，各平台建立映射 |
| totalAmount | String | String | 总额度 | 信用卡使用 | 通用 |
| billingDate | String | String | 账单日 | 信用卡专用, 如 "15" | 通用 |
| repaymentDate | String | String | 还款日 | 信用卡专用, 如 "5" | 通用 |
| type | ClassificationTypeEnum | Enum/Int | 资产大类 | 5 种分类 | 通用，按枚举值映射 |
| classification | AssetClassificationEnum | Enum/Int | 资产分类 | 30+ 种 | 通用，按枚举值映射 |
| invisible | Boolean | Boolean | 是否隐藏 | 默认 false | 通用 |
| openBank | String | String | 开户行 | 银行卡专用 | 通用 |
| cardNo | String | String | 卡号 | 银行卡专用 | 通用 |
| remark | String | String | 备注 | 可为空串 | 通用 |
| sort | Int | Int | 排序 | 越小越靠前 | 通用 |
| modifyTime | String | String | 修改时间 | Model 层为 String; **数据库层为 Long（时间戳毫秒）** | 通用 |
| balance | String | String | 当前余额 | 信用卡为已用额度; Model 层为 String, **数据库层为 Double** | 通用 |

> **注意**：`AssetModel` 与 `BooksModel` 对 `modifyTime` 的类型选择不同——`AssetModel` 在 Model 层使用 `String`（需 Repository 层转换），`BooksModel` 在 Model 层直接使用 `Long`。跨平台实现时建议统一为 `Long` 时间戳。

### 2.3 账本模型 BooksModel 【通用复用】

| 字段名 | Kotlin 类型 | 通用类型 | 含义 | 约束 |
|--------|------------|---------|------|------|
| id | Long | Int64 | 主键 | 自增 |
| name | String | String | 账本名称 | 非空，不可重复 |
| description | String | String | 描述 | 可为空串 |
| bgUri | String | String | 背景图 URI | 可为空串 |
| modifyTime | Long | Int64 | 修改时间 | 时间戳毫秒 |

### 2.4 记录类型模型 RecordTypeModel 【通用复用】

| 字段名 | Kotlin 类型 | 通用类型 | 含义 | 约束 |
|--------|------------|---------|------|------|
| id | Long | Int64 | 主键 | 自增; 特殊 ID: -1101L(平账支出), -1102L(平账收入) |
| parentId | Long | Int64 | 父类型 ID | -1L 表示一级类型 |
| name | String | String | 类型名称 | 非空 |
| iconName | String | String | 图标资源名 | 资源标识字符串 |
| typeLevel | TypeLevelEnum | Enum | 层级 | FIRST(一级) / SECOND(二级) |
| typeCategory | RecordTypeCategoryEnum | Enum | 收支分类 | EXPENDITURE / INCOME / TRANSFER |
| protected | Boolean | Boolean | 是否受保护 | 系统预设不可删除 |
| sort | Int | Int | 排序 | 一级直接排序, 二级 = parentSort*1000 + 序号 |
| needRelated | Boolean | Boolean | 是否需关联 | 动态计算：退款/报销类型 |

### 2.5 标签模型 TagModel 【通用复用】

| 字段名 | Kotlin 类型 | 通用类型 | 含义 | 约束 |
|--------|------------|---------|------|------|
| id | Long | Int64 | 主键 | 自增 |
| name | String | String | 标签名 | 非空 |
| invisible | Boolean | Boolean | 是否隐藏 | 默认 false |

> **注意**：数据库表 `db_tag` 还包含 `books_id`（所属账本 ID，默认 -1）字段，但 Model 层未暴露该字段，由 Repository 层在查询时自动根据当前账本过滤。跨平台实现时数据库建表需包含此字段。

### 2.6 图片模型 ImageModel 【通用复用】

| 字段名 | Kotlin 类型 | 通用类型 | 含义 | 约束 |
|--------|------------|---------|------|------|
| id | Long | Int64 | 主键 | 自增 |
| recordId | Long | Int64 | 关联记录 ID | FK |
| path | String | String | 图片路径 | 可为空串 |
| bytes | ByteArray | Byte[] | 图片二进制数据 | BLOB 存储 |

### 2.7 应用设置模型 AppSettingsModel 【部分通用】

| 字段名 | 类型 | 含义 | 跨平台适配 |
|--------|------|------|-----------|
| useGithub | Boolean | 使用 GitHub 源(否则用 Gitee) | 【通用复用】 |
| autoCheckUpdate | Boolean | 自动检查更新 | 【通用复用】 |
| ignoreUpdateVersion | String | 忽略的更新版本号 | 【通用复用】 |
| mobileNetworkDownloadEnable | Boolean | 允许流量下载 | 【通用复用】 |
| needSecurityVerificationWhenLaunch | Boolean | 启动验证 | 【通用复用】 |
| enableFingerprintVerification | Boolean | 指纹验证 | 【通用复用】逻辑通用，认证 API 平台特有 |
| passwordIv | String | 密码加密向量 | 【通用复用】加密算法通用，密钥存储平台适配 |
| fingerprintIv | String | 指纹加密向量 | 同上 |
| passwordInfo | String | 加密后的密码 | 同上 |
| fingerprintPasswordInfo | String | 指纹加密密码 | 同上 |
| darkMode | DarkModeEnum | 主题模式 | 【通用复用】值通用，设置系统主题方式平台特有 |
| dynamicColor | Boolean | 动态配色 | 【Android特有】Material You 特性，其他平台可忽略 |
| verificationMode | VerificationModeEnum | 验证时机 | 【通用复用】 |
| agreedProtocol | Boolean | 已同意协议 | 【通用复用】 |
| webDAVDomain | String | WebDAV 服务器 | 【通用复用】 |
| webDAVAccount | String | WebDAV 账号 | 【通用复用】 |
| webDAVPassword | String | WebDAV 密码 | 【通用复用】应使用安全存储 |
| backupPath | String | 备份路径 | 【通用复用】路径格式平台适配 |
| autoBackup | AutoBackupModeEnum | 自动备份模式 | 【通用复用】调度机制平台特有 |
| lastBackupMs | Long | 上次备份时间戳 | 【通用复用】 |
| keepLatestBackup | Boolean | 仅保留最新备份 | 【通用复用】 |
| imageQuality | ImageQualityEnum | 图片质量 | 【通用复用】压缩参数通用 |

### 2.8 记录设置模型 RecordSettingsModel 【通用复用】

| 字段名 | 类型 | 含义 |
|--------|------|------|
| currentBookId | Long | 当前账本 ID |
| defaultTypeId | Long | 默认记录类型 ID |
| lastAssetId | Long | 上次使用的资产 ID |
| refundTypeId | Long | 退款类型 ID |
| reimburseTypeId | Long | 报销类型 ID |
| creditCardPaymentTypeId | Long | 信用卡还款类型 ID |
| topUpInTotal | Boolean | 充值账户计入总资产 |

### 2.9 核心枚举定义 【通用复用】

#### RecordTypeCategoryEnum - 收支类型

| 枚举值 | 含义 | 整数映射 |
|--------|------|---------|
| EXPENDITURE | 支出 | 0 |
| INCOME | 收入 | 1 |
| TRANSFER | 转账 | 2 |

#### ClassificationTypeEnum - 资产大类

| 枚举值 | 含义 | 计算属性 |
|--------|------|---------|
| CAPITAL_ACCOUNT | 资金账户 | - |
| CREDIT_CARD_ACCOUNT | 信用卡账户 | isCreditCard=true |
| TOP_UP_ACCOUNT | 充值账户 | isTopUp=true |
| INVESTMENT_FINANCIAL_ACCOUNT | 投资理财 | - |
| DEBT_ACCOUNT | 债务 | - |

#### AssetClassificationEnum - 资产分类（30+ 种）

| 分组 | 枚举值列表 | 计算属性 |
|------|-----------|---------|
| 资金账户 | CASH, WECHAT, ALIPAY, DOUYIN, BANK_CARD, OTHER_CAPITAL | - |
| 信用卡 | CREDIT_CARD, ANT_CREDIT_PAY, JD_IOUS, DOUYIN_MONTH, OTHER_CREDIT_CARD | isCreditCard=true |
| 充值账户 | PHONE_CHARGE, BUS_CARD, MEAL_CARD, MEMBER_CARD, DEPOSIT, OTHER_TOP_UP | - |
| 投资理财 | STOCK, FUND, OTHER_INVESTMENT_FINANCIAL | isInvestmentFinancialAccount=true |
| 债务 | BORROW, LEND, DEBT | isDebt=true |
| 银行卡 | BANK_CARD_ZG, BANK_CARD_ZS, ... (15家银行) | isBankCard=true, hasBankInfo=true |

#### AutoBackupModeEnum - 自动备份模式

| 枚举值 | 含义 |
|--------|------|
| CLOSE | 关闭 |
| WHEN_LAUNCH | 每次启动 |
| EACH_DAY | 每天 |
| EACH_WEEK | 每周 |

#### ImageQualityEnum - 图片质量

| 枚举值 | inSampleSize | reSize | 含义 |
|--------|-------------|--------|------|
| ORIGINAL | 1 | false | 原图 |
| HIGH | 2 | false | 高质量(1/2采样) |
| MEDIUM | 3 | true | 中质量(1/3采样+缩放) |

### 2.10 通用结果模型 ResultModel 【通用复用】

```
sealed ResultModel<T> {
    Success(value: T)
    Failure(code: Int, throwable: Throwable?)
}

// 错误码常量
FAILURE_THROWABLE = -1687                        // 通用异常
FAILURE_EDIT_RECORD_AMOUNT_MUST_NOT_BE_ZERO = -1688  // 金额不能为零
FAILURE_EDIT_RECORD_TYPE_MUST_NOT_BE_NULL = -1689    // 类型不能为空
FAILURE_EDIT_RECORD_TYPE_NOT_MATCH_CATEGORY = -1690  // 类型分类不匹配
```

**适配指导**：
- iOS: 使用 `enum Result<T> { case success(T); case failure(Int, Error?) }`
- Web/TS: 使用 `type Result<T> = { ok: true; value: T } | { ok: false; code: number; error?: Error }`
- 鸿蒙: 使用联合类型或自定义 class

---

## 3. 核心接口设计（抽象化）

### 3.1 通用业务接口

#### 3.1.1 RecordRepository 【通用复用】

```
interface RecordRepository {
    // 查询
    queryById(recordId: Int64) -> RecordModel?
    queryByTypeId(typeId: Int64) -> List<RecordModel>
    queryRelatedById(recordId: Int64) -> List<RecordModel>
    queryRecordByYearMonth(year: String, month: String) -> Stream<List<RecordModel>>
    queryRecordListBetweenDate(from: String, to: String) -> List<RecordModel>
    queryImagesByRecordId(recordId: Int64) -> List<ImageModel>

    // 分页查询 (page从0开始)
    queryPagingRecordListByAssetId(assetId: Int64, page: Int, size: Int) -> List<RecordModel>
    queryPagingRecordListByTypeId(typeId: Int64, page: Int, size: Int) -> List<RecordModel>
    queryPagingRecordListByTagId(tagId: Int64, page: Int, size: Int) -> List<RecordModel>
    queryPagingRecordListByKeyword(keyword: String, page: Int, size: Int) -> List<RecordModel>

    // 特殊查询
    getLastThreeMonthReimbursableRecordList() -> List<RecordModel>
    getLastThreeMonthRefundableRecordList() -> List<RecordModel>
    getLastThreeMonthRecordCountByAssetId(assetId: Int64) -> Int

    // 修改
    updateRecord(record: RecordModel, tagIdList: List<Int64>,
                 needRelated: Boolean, relatedRecordIdList: List<Int64>,
                 relatedImageList: List<ImageModel>) -> Void
    deleteRecord(recordId: Int64) -> Void

    // 特殊操作
    changeRecordTypeBeforeDeleteType(typeId: Int64, defaultTypeId: Int64) -> Void  // 删除类型前迁移记录
    getRelatedIdListById(recordId: Int64) -> List<Int64>           // 获取记录的关联记录 ID 列表
    getRecordIdListFromRelatedId(relatedId: Int64) -> List<Int64>  // 通过关联 ID 反查记录 ID 列表
    queryRelatedRecordCountById(recordId: Int64) -> Int            // 查询关联记录数量

    // 搜索历史
    searchHistoryListData -> Stream<List<String>>
    addSearchHistory(keyword: String) -> Void
    clearSearchHistory() -> Void
}
```

**各平台实现建议**：
- 【适配指导】iOS: 定义为 `protocol RecordRepository`，使用 `async` 方法和 `AsyncStream`
- 【适配指导】鸿蒙: 定义为 TypeScript `interface`，使用 `Promise` 和 `EventEmitter`
- 【适配指导】Web: 定义为 TypeScript `interface`，使用 `Promise` 和 `Observable`
- 【适配指导】Flutter: 定义为 `abstract class`，使用 `Future` 和 `Stream`

#### 3.1.2 AssetRepository 【通用复用】

```
interface AssetRepository {
    // 数据流
    currentVisibleAssetListData -> Stream<List<AssetModel>>
    currentVisibleAssetTypeData -> Stream<List<AssetTypeViewsModel>>
    currentInvisibleAssetListData -> Stream<List<AssetModel>>
    currentInvisibleAssetTypeData -> Stream<List<AssetTypeViewsModel>>
    topUpInTotalData -> Stream<Boolean>

    // 查询
    getAssetById(assetId: Int64) -> AssetModel?
    getVisibleAssetsByBookId(bookId: Int64) -> List<AssetModel>
    getInvisibleAssetsByBookId(bookId: Int64) -> List<AssetModel>

    // 修改
    updateAsset(asset: AssetModel) -> Void     // 自动判断新增/更新
    deleteById(assetId: Int64) -> Void
    visibleAssetById(id: Int64) -> Void         // 取消隐藏
    updateTopUpInTotal(topUpInTotal: Boolean) -> Void
}
```

#### 3.1.3 BooksRepository 【通用复用】

```
interface BooksRepository {
    booksListData -> Stream<List<BooksModel>>
    currentBook -> Stream<BooksModel>

    selectBook(id: Int64) -> Void
    insertBook(book: BooksModel) -> Int64       // 返回新ID
    deleteBook(id: Int64) -> Boolean            // 级联删除所有数据
    updateBook(book: BooksModel) -> Void
    isDuplicated(book: BooksModel) -> Boolean
    getDefaultBook(id: Int64) -> BooksModel
}
```

#### 3.1.4 TypeRepository 【通用复用】

```
interface TypeRepository {
    // 数据流 (按收支分类的一级类型列表)
    firstExpenditureTypeListData -> Stream<List<RecordTypeModel>>
    firstIncomeTypeListData -> Stream<List<RecordTypeModel>>
    firstTransferTypeListData -> Stream<List<RecordTypeModel>>

    // 查询
    getRecordTypeById(typeId: Int64) -> RecordTypeModel?
    getNoNullRecordTypeById(typeId: Int64) -> RecordTypeModel
    getSecondRecordTypeListByParentId(parentId: Int64) -> List<RecordTypeModel>
    needRelated(typeId: Int64) -> Boolean
    isReimburseType(typeId: Int64) -> Boolean
    isRefundType(typeId: Int64) -> Boolean
    isCreditPaymentType(typeId: Int64) -> Boolean

    // 修改
    update(model: RecordTypeModel) -> Void
    deleteById(id: Int64) -> Void
    changeTypeToSecond(id: Int64, parentId: Int64) -> Void
    changeSecondTypeToFirst(id: Int64) -> Void
    countByName(name: String) -> Int                   // 按名称统计类型数量（重名检查）
    generateSortById(id: Int64) -> Int                 // 生成排序值
    setReimburseType(typeId: Int64) -> Void
    setRefundType(typeId: Int64) -> Void
    setCreditPaymentType(typeId: Int64) -> Void
}
```

#### 3.1.5 TagRepository 【通用复用】

```
interface TagRepository {
    tagListData -> Stream<List<TagModel>>

    getRelatedTag(recordId: Int64) -> List<TagModel>
    getTagById(tagId: Int64) -> TagModel?
    countTagByName(name: String) -> Int

    updateTag(tag: TagModel) -> Void
    deleteTag(tag: TagModel) -> Void
    deleteRelatedWithAsset(assetId: Int64) -> Void   // 删除资产时清理关联标签
}
```

#### 3.1.6 SettingRepository 【部分通用】

```
interface SettingRepository {
    // 数据流
    appSettingsModel -> Stream<AppSettingsModel>
    recordSettingsModel -> Stream<RecordSettingsModel>
    gitDataModel -> Stream<GitDataModel>              // Git 版本信息数据
    tempKeysModel -> Stream<TempKeysModel>             // 临时密钥数据

    // 通用设置更新方法 (每个设置字段一个方法, 此处列举关键方法)
    updateDarkMode(mode: DarkModeEnum) -> Void
    updateAutoCheckUpdate(enabled: Boolean) -> Void
    updateWebDAV(domain: String, account: String, password: String) -> Void
    updateAutoBackupMode(mode: AutoBackupModeEnum) -> Void
    updateBackupPath(path: String) -> Void
    updateBackupMs(ms: Long) -> Void
    updateKeepLatestBackup(keep: Boolean) -> Void
    updateMobileNetworkBackupEnable(enabled: Boolean) -> Void

    // 版本检查 [通用复用]
    syncLatestVersion() -> Boolean
    getLatestUpdateInfo() -> UpgradeInfoEntity

    // Markdown 内容读取 [通用复用]
    getContentByMarkdownType(type: MarkdownTypeEnum) -> String
}
```

### 3.2 Android 特有接口

#### 3.2.1 NetworkMonitor 【Android特有】

```
// Android 实现: ConnectivityManager.NetworkCallback
interface NetworkMonitor {
    isOnline -> Stream<Boolean>
    isWifi -> Stream<Boolean>
}
```

**适配指导**：
- iOS: 使用 `NWPathMonitor`，监听 `.satisfied` 状态和 `usesInterfaceType(.wifi)`
- 鸿蒙: 使用 `@ohos.net.connection` 的 `NetConnection.on('netAvailable')`
- Web: 监听 `window.addEventListener('online')` 和 `navigator.connection`

#### 3.2.2 AppUpgradeManager 【Android特有】

```
// Android 实现: WorkManager 后台下载 + 前台通知 + Intent 安装
interface AppUpgradeManager {
    upgradeState -> Stream<AppUpgradeStateEnum>
    startDownload(info: UpgradeInfoEntity) -> Void
    cancelDownload() -> Void
    retry() -> Void
}
```

**适配指导**：
- iOS: 应用更新通过 App Store 分发，不需要应用内下载安装，可跳转到 App Store 页面
- 鸿蒙: 应用更新通过 AppGallery，使用 `@ohos.bundle.installer`
- Web: 无需下载安装，刷新页面即可获取最新版本

#### 3.2.3 BackupRecoveryManager 【部分通用】

核心逻辑（Zip 打包/解压、WebDAV 上传下载）为通用逻辑。以下为 Android 特有部分：

| 功能 | Android 实现 | 适配指导 |
|------|-------------|---------|
| 本地文件读写 | SAF (DocumentFile) + 传统 File | iOS: FileManager + UIDocumentPicker; 鸿蒙: @ohos.file.fs; Web: File System Access API |
| 权限检查 | READ/WRITE_EXTERNAL_STORAGE | iOS: 无需运行时权限(沙盒); 鸿蒙: @ohos.abilityAccessCtrl; Web: 浏览器弹窗授权 |
| 缓存目录 | context.cacheDir | iOS: FileManager.cachesDirectory; 鸿蒙: context.cacheDir; Web: 内存/临时存储 |

---

## 4. 核心业务逻辑详解（跨平台通用）

### 4.1 记录保存逻辑 (SaveRecordUseCase) 【通用复用】

**输入**：RecordModel, tagIdList, relatedRecordIdList, relatedImageList

**核心步骤**：
```
1. 检查记录类型是否需要关联处理
   - 通过 TypeRepository.needRelated(typeId) 判断
   - 退款/报销类型需要关联原始记录

2. 调用 RecordRepository.updateRecord() 进入数据库事务：
   a. 计算最终金额：
      - 支出: finalAmount = amount + charges - concessions
      - 收入: finalAmount = amount - charges
      - 转账: finalAmount = amount + charges - concessions（与支出相同）
   b. 更新资产余额：
      - 资金类账户: balance += (收入金额) 或 balance -= (支出金额)
      - 信用卡账户: balance += (支出金额, 即已用额度增加) 或 balance -= (收入金额)
      - 转账: 转出资产 balance -= amount, 转入资产 balance += amount
   c. 保存标签关联 (db_tag_with_record 表)
   d. 保存图片关联 (db_image_with_related 表)
   e. 保存关联记录 (db_record_with_related 表)
   f. 计算关联记录的汇总金额，更新主记录的 finalAmount

3. 更新上次使用的资产 ID (RecordSettings.lastAssetId)
4. 递增 recordDataVersion 和 assetDataVersion
```

**边界条件**：
- 金额为零时拒绝保存（返回错误码 -1688）
- 类型为空时拒绝保存（返回错误码 -1689）
- 编辑已有记录时，先回退旧记录对资产的影响，再应用新记录

### 4.2 资产保存与平账逻辑 (SaveAssetUseCase) 【通用复用】

**输入**：AssetModel

**核心步骤**：
```
1. 判断新增还是编辑 (id == -1L 为新增)

2. 新增场景：
   - 直接保存资产到数据库

3. 编辑场景：
   a. 更新资产基本信息
   b. 计算余额差异 = 新余额 - 旧余额
   c. 若差异 != 0, 自动生成平账记录：
      - 普通账户:
        · 差异 > 0 → 生成 RecordTypeModel.RECORD_TYPE_BALANCE_INCOME (平账收入, id=-1102L)
        · 差异 < 0 → 生成 RecordTypeModel.RECORD_TYPE_BALANCE_EXPENDITURE (平账支出, id=-1101L)
      - 信用卡账户 (逻辑相反):
        · 差异 > 0 → 生成平账支出 (已用额度增加)
        · 差异 < 0 → 生成平账收入 (已用额度减少)
   d. 平账记录的 amount = abs(差异), assetId = 当前资产 ID

4. 递增 assetDataVersion
```

### 4.3 统计分析 - 饼图数据生成 (TransRecordViewsToAnalyticsPieUseCase) 【通用复用】

**输入**：typeCategory (支出/收入), recordViewsList

**核心算法**：
```
1. 过滤指定 typeCategory 的记录

2. 按一级分类 (parentId 或自身 typeId) 汇总金额：
   - 创建 Map<一级TypeId, 累计金额>
   - 遍历每条记录:
     · 获取类型信息
     · 若为二级类型, 获取其父类型 ID
     · 计算贡献金额:
       - 支出: amount + charges - concessions
       - 收入: amount - charges
     · 累加到对应一级类型的总额

3. 计算总金额 = 所有分类金额之和

4. 生成饼图数据:
   - 对每个一级分类:
     · percent = 分类金额 / 总金额 * 100
     · 构建 AnalyticsRecordPieEntity(typeId, typeName, typeIconResName, typeCategory, totalAmount, percent)

5. 按占比降序排列
```

### 4.4 统计分析 - 柱状图数据生成 (TransRecordViewsToAnalyticsBarUseCase) 【通用复用】

**输入**：fromDate, toDate, yearSelected, recordViewsList

**核心算法**：
```
1. 确定时间分段策略:
   - yearSelected=true: 12 个月 (1月~12月)
   - toDate=null: 按月内每天 (1日~月末)
   - 其他: 日期范围内每天

2. 对每个时间段:
   a. 筛选该时间段内的记录
   b. 分别累计:
      - 支出总额 = Σ(支出记录的 amount + charges - concessions)
      - 收入总额 = Σ(收入记录的 amount - charges)
      - 转账费用 = Σ(转账记录的 charges + concessions)
      注意: 转账的手续费计入支出
   c. 余额 = 收入总额 - 支出总额

3. 构建 AnalyticsRecordBarEntity 列表
```

### 4.5 记录按日期分组 (GetCurrentMonthRecordViewsMapUseCase) 【通用复用】

**输入**：recordViewsEntityList

**核心算法**：
```
1. 按记录日期 (取日期部分) 分组

2. 按日期倒序排列

3. 对每个日期组:
   a. 计算日收入 = Σ(收入记录的 finalAmount)
   b. 计算日支出 = Σ(支出记录的 finalAmount) + Σ(转账记录的 charges + concessions)
   c. 确定日期类型:
      - dayType = 0: 今天
      - dayType = -1: 昨天
      - dayType = -2: 前天
      - dayType = 1: 其他日期

4. 构建 Map<RecordDayEntity, List<RecordViewsEntity>>
```

### 4.6 资产列表获取与排序 (GetAssetListUseCase) 【通用复用】

**输入**：currentTypeId, selectedAssetId, isRelated

**核心逻辑**：
```
1. 获取所有可见资产列表 (AssetRepository.currentVisibleAssetListData)

2. 对每个资产查询最近 3 个月的记录使用次数
   (RecordRepository.getLastThreeMonthRecordCountByAssetId)

3. 按使用次数降序排序

4. 特殊过滤:
   - 如果 isRelated=true 且当前类型为信用卡还款:
     只显示信用卡类型的资产
   - 排除已选择的资产 (selectedAssetId)

5. 返回排序后的资产列表
```

### 4.7 安全认证逻辑 【部分通用】

**核心流程（通用部分）**：
```
1. 启动时检查 needSecurityVerificationWhenLaunch 设置
2. 若需要验证:
   a. 检查是否有已保存的密码 (passwordInfo 非空)
   b. 检查是否支持指纹 (enableFingerprintVerification)
   c. 优先使用指纹认证 → 解密 fingerprintPasswordInfo
   d. 回退到密码认证 → 用户输入密码 → 与 passwordInfo 比对
3. 验证通过后进入主界面
```

**Android 特有逻辑**：
- 密码加密使用 `AndroidKeyStore` 的 AES 密钥 + CBC 模式
- 指纹认证使用 `BiometricPrompt` API
- 加密向量 (IV) 分别存储在 `passwordIv` 和 `fingerprintIv`

**适配指导**：
- iOS: 使用 `Keychain` 存储加密密码，`LAContext.evaluatePolicy(.deviceOwnerAuthenticationWithBiometrics)` 进行生物认证
- 鸿蒙: 使用 `@ohos.security.huks` 密钥管理，`@ohos.userIAM.userAuth` 进行认证
- Web: 使用 `Web Crypto API` 加密，`WebAuthn` 进行生物认证（支持有限）

### 4.8 版本驱动数据刷新机制 【通用复用】

**核心设计思想**：避免全表监听的性能开销，通过版本号计数器驱动数据流刷新。

**机制说明**：
```
1. 定义版本计数器（存储在 DataStore/偏好存储中）:
   - recordDataVersion: Int   // 记录数据版本
   - assetDataVersion: Int    // 资产数据版本
   - typeDataVersion: Int     // 类型数据版本
   - tagDataVersion: Int      // 标签数据版本
   - booksDataVersion: Int    // 账本数据版本

2. 数据变更时递增对应版本号:
   - 保存/删除记录 → recordDataVersion++, assetDataVersion++
   - 保存/删除资产 → assetDataVersion++
   - 保存/删除类型 → typeDataVersion++
   - 保存/删除标签 → tagDataVersion++
   - 切换/修改账本 → booksDataVersion++

3. Repository 数据流监听版本号变化:
   - 各 Repository 的 Flow/Stream 通过 flatMapLatest(dataVersion) 触发
   - 版本号变化时自动重新查询数据库，推送最新数据给 UI 层

4. 跨平台实现建议:
   - iOS: 使用 Combine 的 CurrentValueSubject 作为版本触发器
   - Web: 使用 RxJS BehaviorSubject 或 React Query invalidation
   - 鸿蒙: 使用 @Watch 装饰器监听版本变化
   - Flutter: 使用 StreamController 或 Riverpod 的 invalidation
```

---

## 5. 数据库 Schema 详细设计 【通用复用】

### 5.1 完整建表 SQL（跨平台直接复用）

```sql
-- 账本表
CREATE TABLE db_books (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    bg_uri TEXT NOT NULL DEFAULT '',
    modify_time INTEGER NOT NULL
);

-- 资产表
CREATE TABLE db_asset (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    books_id INTEGER NOT NULL,
    name TEXT NOT NULL,
    balance REAL NOT NULL DEFAULT 0,
    total_amount REAL NOT NULL DEFAULT 0,
    billing_date TEXT NOT NULL DEFAULT '',
    repayment_date TEXT NOT NULL DEFAULT '',
    type INTEGER NOT NULL,
    classification INTEGER NOT NULL,
    invisible INTEGER NOT NULL DEFAULT 0,
    open_bank TEXT NOT NULL DEFAULT '',
    card_no TEXT NOT NULL DEFAULT '',
    remark TEXT NOT NULL DEFAULT '',
    sort INTEGER NOT NULL DEFAULT 0,
    modify_time INTEGER NOT NULL
);

-- 记录类型表
CREATE TABLE db_type (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    parent_id INTEGER NOT NULL DEFAULT -1,
    name TEXT NOT NULL,
    icon_name TEXT NOT NULL,
    type_level INTEGER NOT NULL,
    type_category INTEGER NOT NULL,
    protected INTEGER NOT NULL DEFAULT 0,
    sort INTEGER NOT NULL DEFAULT 0
);

-- 记录表
CREATE TABLE db_record (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    type_id INTEGER NOT NULL,
    asset_id INTEGER NOT NULL DEFAULT -1,
    into_asset_id INTEGER NOT NULL DEFAULT -1,
    books_id INTEGER NOT NULL,
    amount REAL NOT NULL,
    final_amount REAL NOT NULL,
    concessions REAL NOT NULL DEFAULT 0,
    charge REAL NOT NULL DEFAULT 0,
    remark TEXT NOT NULL DEFAULT '',
    reimbursable INTEGER NOT NULL DEFAULT 0,
    record_time INTEGER NOT NULL
);

-- 标签表
CREATE TABLE db_tag (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    books_id INTEGER NOT NULL DEFAULT -1,
    invisible INTEGER NOT NULL DEFAULT 0
);

-- 标签-记录关联表 (多对多)
CREATE TABLE db_tag_with_record (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    record_id INTEGER NOT NULL,
    tag_id INTEGER NOT NULL
);

-- 记录-记录关联表 (报销/退款关联)
CREATE TABLE db_record_with_related (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    record_id INTEGER NOT NULL,
    related_record_id INTEGER NOT NULL
);

-- 图片-记录关联表
CREATE TABLE db_image_with_related (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    record_id INTEGER NOT NULL,
    image_path TEXT NOT NULL DEFAULT '',
    image_bytes BLOB NOT NULL
);
```

### 5.2 Model ↔ Database 字段映射表

以下列出 Model 层与 Database 层存在命名或类型差异的关键字段，Repository 层负责转换：

| 表 | Model 字段名 | Model 类型 | DB 列名 | DB 类型 | 说明 |
|----|------------|-----------|---------|---------|------|
| db_record | `charges` | String | `charge` | REAL(Double) | 命名不同（复数↔单数）+ 类型转换 |
| db_record | `relatedAssetId` | Long | `into_asset_id` | INTEGER(Long) | 命名不同 |
| db_record | `amount` | String | `amount` | REAL(Double) | 类型转换 |
| db_record | `finalAmount` | String | `final_amount` | REAL(Double) | 类型转换 |
| db_record | `concessions` | String | `concessions` | REAL(Double) | 类型转换 |
| db_record | `recordTime` | String("yyyy-MM-dd HH:mm:ss") | `record_time` | INTEGER(Long, 毫秒时间戳) | 命名+类型转换 |
| db_record | `reimbursable` | Boolean | `reimbursable` | INTEGER(Int, 0/1) | 类型转换 |
| db_asset | `balance` | String | `balance` | REAL(Double) | 类型转换 |
| db_asset | `totalAmount` | String | `total_amount` | REAL(Double) | 类型转换 |
| db_asset | `modifyTime` | String | `modify_time` | INTEGER(Long, 毫秒时间戳) | 类型转换 |
| db_asset | `invisible` | Boolean | `invisible` | INTEGER(Int, 0/1) | 类型转换 |
| db_asset | `iconResId` | Int | _(不存储)_ | - | Android 资源 ID，仅运行时映射 |
| db_tag | _(Model 层无)_ | - | `books_id` | INTEGER(Long) | Model 未暴露，Repository 层自动过滤 |

> **跨平台建议**：数据库层保持与 Android 一致的列名和类型（确保备份恢复兼容），Model 层可根据目标语言习惯调整命名和类型。

### 5.3 实体关系图

```
db_books (1) ──────┬──── (N) db_asset
                   │
                   └──── (N) db_record ──┬── (N) db_tag_with_record ── (1) db_tag
                                         │
                                         ├── (N) db_record_with_related
                                         │
                                         └── (N) db_image_with_related

db_type (树形自关联: parent_id → id)
    └──── (1:N) db_record (type_id)
```

### 5.4 关键事务逻辑（insertRecordTransaction）【通用复用】

```
BEGIN TRANSACTION;

-- 1. 计算最终金额
IF typeCategory == INCOME:
    finalAmount = amount - charge
ELSE:  -- EXPENDITURE 或 TRANSFER（计算方式相同）
    finalAmount = amount + charge - concessions

-- 2. 插入记录
INSERT INTO db_record (...) VALUES (...);
recordId = LAST_INSERT_ROWID();

-- 3. 更新资产余额（注意：TRANSFER 与 EXPENDITURE 处理方式相同）
IF assetId != -1:
    IF asset.classification.isCreditCard:
        IF typeCategory == INCOME:
            UPDATE db_asset SET balance = balance - finalAmount WHERE id = assetId;
        ELSE:  -- EXPENDITURE 或 TRANSFER
            UPDATE db_asset SET balance = balance + finalAmount WHERE id = assetId;
    ELSE:
        IF typeCategory == INCOME:
            UPDATE db_asset SET balance = balance + finalAmount WHERE id = assetId;
        ELSE:  -- EXPENDITURE 或 TRANSFER
            UPDATE db_asset SET balance = balance - finalAmount WHERE id = assetId;

-- 4. 转账: 更新转入资产
IF typeCategory == TRANSFER AND intoAssetId != -1:
    IF intoAsset.classification.isCreditCard:
        UPDATE db_asset SET balance = balance - amount WHERE id = intoAssetId;
    ELSE:
        UPDATE db_asset SET balance = balance + amount WHERE id = intoAssetId;

-- 5. 保存标签关联
FOR EACH tagId IN tagIdList:
    INSERT INTO db_tag_with_record (record_id, tag_id) VALUES (recordId, tagId);

-- 6. 保存图片关联
FOR EACH image IN imageList:
    INSERT INTO db_image_with_related (record_id, image_path, image_bytes)
        VALUES (recordId, image.path, image.bytes);

-- 7. 保存关联记录
FOR EACH relatedId IN relatedRecordIdList:
    INSERT INTO db_record_with_related (record_id, related_record_id)
        VALUES (recordId, relatedId);

-- 8. 处理关联记录金额
-- 8a. 累计所有关联记录的 finalAmount（注意：取的是 finalAmount，不是 amount）
relatedAmount = SELECT SUM(final_amount) FROM db_record WHERE id IN (relatedRecordIdList);
-- 8b. 将关联记录的 finalAmount 置为 0（已被当前记录抵消）
UPDATE db_record SET final_amount = 0 WHERE id IN (relatedRecordIdList);
-- 8c. 主记录 finalAmount = 自身计算金额 - 关联记录金额之和
UPDATE db_record SET final_amount = (finalAmount - relatedAmount) WHERE id = recordId;

COMMIT;
```

---

## 6. 第三方依赖适配建议

### 6.1 依赖清单与跨平台替代

| Android 依赖 | 用途 | iOS 替代 | 鸿蒙替代 | Web 替代 |
|-------------|------|---------|---------|---------|
| Room 2.7.1 | SQLite ORM | GRDB / SQLite.swift | @ohos.data.relationalStore | sql.js / Dexie.js (IndexedDB) |
| Retrofit 3 + OkHttp | HTTP 客户端 | URLSession / Alamofire | @ohos.net.http | fetch / axios |
| Kotlin Serialization | JSON 序列化 | Codable (内置) | JSON.parse (内置) | JSON.parse (内置) |
| Proto DataStore | 偏好存储 | UserDefaults | @ohos.data.preferences | localStorage / IndexedDB |
| Hilt 2.56 | 依赖注入 | Swinject / 手动 DI | 手动 DI | InversifyJS / tsyringe |
| WorkManager | 后台任务 | BGTaskScheduler | WorkSchedulerExtensionAbility | Service Worker |
| Navigation Compose | 导航 | NavigationStack | router | React Router / Vue Router |
| Compose BOM | 声明式 UI | SwiftUI (内置) | ArkUI (内置) | React / Vue |
| MPChartLib | 图表 | Charts (DGCharts) | @ohos.chart | Chart.js / ECharts |
| JSoup | HTML/XML 解析 | SwiftSoup | 内置 XML 解析 | DOMParser (内置) |
| BiometricPrompt | 生物认证 | LocalAuthentication | @ohos.userIAM.userAuth | WebAuthn API |
| Coil | 图片加载 | Kingfisher / SDWebImage | Image 组件 | 原生 img 标签 |
| DoKit | 开发调试 | FLEX / Pulse | DevEco Profiler | Chrome DevTools |

### 6.2 WebDAV 实现适配

WebDAV 是标准 HTTP 扩展协议，各平台通过原生 HTTP 客户端实现：

| 操作 | HTTP 方法 | 请求体 | 响应 |
|------|----------|--------|------|
| 检查存在 | HEAD | 无 | 200(存在) / 404(不存在) |
| 创建目录 | MKCOL | 无 | 201(成功) |
| 上传文件 | PUT | 文件流 | 200/201(成功) |
| 列表文件 | PROPFIND (Depth: 1) | XML 属性请求 | 207 Multi-Status XML |
| 下载文件 | GET | 无 | 文件流 |

**PROPFIND 请求体模板**（各平台通用）：
```xml
<?xml version="1.0" encoding="utf-8"?>
<d:propfind xmlns:d="DAV:">
  <d:prop>
    <d:displayname/>
    <d:getcontentlength/>
    <d:getlastmodified/>
  </d:prop>
</d:propfind>
```

---

## 7. 跨平台适配踩坑提示

### 7.1 Android 项目中的潜在适配问题

| 问题 | 位置 | 影响 | 建议 |
|------|------|------|------|
| **资源 ID 硬编码** | AssetModel.iconResId, AssetTypeViewsModel.nameResId | 其他平台无 Android R 类 | 改为字符串标识符，各平台维护资源映射表 |
| **Context 耦合** | BackupRecoveryManager, SettingRepository, 通知服务 | 其他平台无 Android Context | 抽象为 FileSystemProvider, NotificationProvider 接口 |
| **Android 资源引用** | AssetHelper 中 30+ 图标和名称映射到 R.drawable / R.string | 图标资源无法跨平台 | 提取为配置数据，图标使用 SVG 矢量格式统一管理 |
| **时间类型混用** | Model 层用 String, 数据库层用 Long(时间戳), DataStore 也用 Long | 容易混淆 | 建议统一为 Long 时间戳，仅在 UI 层格式化为字符串 |
| **SAF 路径格式** | BackupRecoveryManager 中 content:// URI 处理 | iOS/Web 无 SAF | 抽象文件选择接口，各平台实现原生文件选择器 |
| **隐式 Intent** | APK 安装使用 Intent.ACTION_VIEW | 仅 Android 有 Intent 机制 | 各平台使用原生安装/更新机制 |

### 7.2 各平台特有风险点

#### iOS 适配注意

| 风险点 | 说明 | 应对方案 |
|--------|------|---------|
| 沙盒机制 | 应用只能访问自己的沙盒目录 | 备份文件存储在 Documents 目录，通过 iCloud 或 WebDAV 同步 |
| 后台限制 | 后台任务最多 30 秒 | 自动备份使用 BGTaskScheduler，注册 BGProcessingTaskRequest |
| App Store 审核 | 不允许应用内下载安装 APK | 版本更新跳转 App Store |
| Keychain 共享 | 设备迁移时 Keychain 数据可能丢失 | 提供密码重置机制 |

#### 鸿蒙适配注意

| 风险点 | 说明 | 应对方案 |
|--------|------|---------|
| Stage 模型 | 使用 Ability 而非 Activity | 主界面使用 UIAbility，后台任务使用 WorkSchedulerExtensionAbility |
| ArkTS 限制 | 不支持反射和动态类加载 | DI 使用手动构造函数注入 |
| 分布式能力 | 鸿蒙支持跨设备协同 | 可利用分布式数据管理实现多设备同步 |

#### Web 适配注意

| 风险点 | 说明 | 应对方案 |
|--------|------|---------|
| 跨域限制 | WebDAV 服务器需配置 CORS | 通过后端代理或 WebDAV 服务器配置 Access-Control 头 |
| 无后台能力 | 浏览器标签关闭后无法执行任务 | 不支持自动备份，仅支持手动备份 |
| 存储限制 | localStorage 约 5MB，IndexedDB 约 50MB~无限 | 数据库使用 IndexedDB 存储，图片使用 Blob |
| 数据安全 | 浏览器环境无安全存储 | 敏感信息（密码、WebDAV 凭证）不在前端存储，或使用 Web Crypto API 加密 |
| SQLite 支持 | 浏览器不原生支持 SQLite | 使用 sql.js (Emscripten 编译的 SQLite) 或 OPFS + SQLite WASM |

### 7.3 金额计算精度提示

Android 项目中金额在 Model 层使用 `String` 类型，数据库使用 `Double` 类型。跨平台开发需注意：

| 平台 | 精度方案 |
|------|---------|
| iOS | 使用 `Decimal` 类型或 `NSDecimalNumber`，避免 Double |
| Web | 使用 `decimal.js` 库或将金额乘以 100 转为整数运算 |
| 鸿蒙 | 使用 `decimal.js` 或自定义高精度计算 |
| Flutter | 使用 `decimal` package |
| 通用建议 | 数据库层建议改用 `INTEGER` 存储分（金额×100），避免浮点问题 |
