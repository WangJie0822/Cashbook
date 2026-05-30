> 生成方式：Workflow 工具编排 14 路维度评审（安全×4/性能×3/正确性×4/架构/测试/质量）→ 对抗式核验 → 综合去重，共 29 个 subagent。
> 本报告未经 team-review/full-review，因属用户显式授权的 Workflow 编排 ad-hoc 全量评审（schema 强校验聚合 + 超单会话 context 的 405 文件扫描）。
> **controller 已对全部 Critical + 7 项关键 High hands-on 复核**，结论见下「controller 复核」节；其余 Medium/Low/Info 为 subagent 核验结论，引用前请按 file:line 自核。

## controller 复核（独立 hands-on 核验，2026-05-30）

| 发现 | 复核动作 | 结论 |
|---|---|---|
| Critical 明文口令日志 | Read SettingViewModel.kt:187/220/247 + CashbookApplication.kt:91 门控 + AboutUsViewModel 开关 | ✅ 成立：`.i("...pwd=<$pwd>...")` 三处明文；`logcatEnable||logcatInRelease` 使 Release 开关后可输出 |
| High WebDAV 凭据日志 | Read DataSourceModule.kt:53-69 + grep redactHeader | ✅ 成立：LEVEL_BODY(debug)，全仓 redactHeader 生产零调用 |
| High 导入去重单位错配 | Read RecordDao.kt:438(Double 参数)+ImportedBillItem.kt:40(amount:Double 元)+RecordImportViewModel.kt:183/285 | ✅ 成立：写库 toCent()存分，去重传元比分列，恒不命中（死逻辑） |
| High Keystore 无用户认证 | Read Cipher.kt:112/140 | ✅ 成立：`setUserAuthenticationRequired(false)` |
| High N+1 | Read RecordModelTransToViewsUseCase.kt 全文 | ✅ 成立：单条 invoke 6+ 次 Repository 查询，被 .map 逐条调用 |
| High 恢复=合并非替换 | Read DatabaseMigrations.kt:84-102 | ✅ 成立：copyData 仅 CONFLICT_REPLACE 灌入，不清空 to 库，备份后新增残留 |
| High 迁移测试缺口 | Read DatabaseTest.kt:496/701 | ✅ 成立：migrate6_7 空桩、migrateAll 全注释、无 migrate11_12 |

> 复核未发现 subagent 虚构（synthesis 已在核验阶段自行剔除一处造假引用 MainAppViewModel.kt:281）。Workflow 报告「剔除误报 0」属偏松，但抽样核验的关键项证据均对得上。

---

# Cashbook 全维度评审报告

> 范围：本报告汇总「经对抗式核验未被推翻」的发现，覆盖安全（网络/WebDAV、加密/生物识别/密钥、Android 平台面、数据层注入/导入完整性）、性能（数据库、Compose）、正确性（金额、业务 UseCase/Repository、数据库迁移、备份恢复/同步）、架构（分层/DI/设计系统）、测试覆盖、错误处理与 Kotlin 规范等维度。
> 所有 `verdict=uncertain` 的发现在报告中显式标注「待运行期确认」。本批次发现中 **无 uncertain**；少数发现核验后被下调严重度（标注「校准后」），均在明细中注明依据。

---

## 一、总体结论

**项目健康度：中等偏好，但存在 1 个明确的高危凭据泄露问题与 1 个跨维度反复出现的金额单位错配 bug，需优先处置。**

整体工程质量良好：分层架构清晰、设计系统强约束（lint 拦截）、金额按「分 Long」全链路约定、迁移测试有相当覆盖、Compose 重计算多处已正确 `remember`。但存在两类系统性问题：(1) **WebDAV/应用锁凭据在日志中明文泄露**（Critical + 多条 High 同源）；(2) **账单导入去重的金额比较单位/类型双重错配**，被 4 个维度独立发现（同一根因 RecordDao.queryByTimeAndAmount），导致「同天同金额」模糊去重成为死逻辑。

各维度一句话小结：

- **安全·网络与 WebDAV**：WebDAV Basic 凭据在 debug 日志明文泄露、备份链路允许 dav:// 明文回退（仅靠 manifest 单标志兜底），是该维度核心风险。
- **安全·加密/生物识别/密钥**：应用锁明文口令进日志且 Release 可经后门开关启用（Critical）；Keystore 密钥未要求用户认证使生物识别门禁可被绕过（High）；加密用 CBC 非 AEAD、口令单次无盐 SHA-1。
- **安全·Android 平台面**：仅权限/FileProvider 范围偏宽等最小化卫生问题，均 Low/Info，无可被外部直接利用的越权。
- **安全·数据层注入与导入完整性**：导入去重金额单位错配（High）外，xlsx 解压炸弹/XML 实体扩展/路径遍历等导入 DoS 与卫生问题为 Medium/Low。
- **性能·数据库**：记录视图转换 N+1（单页 50 条触发数百次单行查询）与 Analytics 全年全量物化后逐条放大是两个 High 热点。
- **性能·Compose**：unstable Map 参数致 FrontLayerContent 不可跳过、列表缺 key 等 Medium 重组问题；其余多为 Low/Info 微优化。
- **正确性·金额处理**：导入去重单位错配（High）；两处 Analytics 饼图内联重算金额公式绕过 `calculateRecordAmount`（违反项目强制约定，Medium）。
- **正确性·业务 UseCase/Repository**：batchImport 缺转账目标资产余额更新且跳过平账类型（当前调用链未触发，Low）；月度结余 isCreditCard 入参经核验实为从资产派生，原 Medium 降为 Info。
- **正确性·数据库迁移**：金额 REAL→INTEGER 全表重建（Migration11To12）零测试、最复杂的 migrate6_7 为空桩、migrateAll 端到端校验被注释（High/Medium）。
- **正确性·备份恢复与同步**：恢复为 upsert 合并而非清空替换（语义偏差，High）；AutoBackupWorker 恒返回 success 不重试不上报（Medium）。
- **架构·分层/DI**：Repository 接口泄漏 RecordTable、feature/record-import 直依赖 core:database 且不依赖 core:domain（破坏分层，Medium×2）。
- **测试·覆盖与质量**：四个 `*RepositoryImplTest` 实测 Fake 而非 Impl（虚假覆盖，High）；导入解析链路与 Migration11To12 零测试（High）。
- **质量·错误处理与 Kotlin 规范**：多处 `runCatching` 吞 `CancellationException` 破坏协程取消（Medium×2 + Low×1）；图片工具 `printStackTrace` + InputStream 泄漏。

**跨维度去重说明**：金额单位错配（RecordDao.queryByTimeAndAmount，Long 分列 vs Double 元值）被 5 条发现独立命中（数据层注入、正确性·金额、备份恢复、架构、测试各一），实为**同一根因**，已合并为「TOP 1」并保留最高 High。WebDAV Authorization 明文进日志被 3 条发现命中（DataSourceModule/LoggerInterceptor 同一装配缺陷 redactHeader 未调用），合并为一条 High。

---

## 二、发现明细

### Critical

| 标题 | 维度 | file:line | 严重度 | 影响 | 建议 |
|---|---|---|---|---|---|
| 明文用户口令写入日志，且 Release 可经后门开关启用（校准：核心成立，原 evidence 中"MainAppViewModel.kt:281 记录指纹口令 SHA"为造假引用，已剔除；其余三处明文口令日志 + Release 后门确认） | 安全·加密/密钥 | feature/settings/.../viewmodel/SettingViewModel.kt:220（另 :187/:247） | Critical | 应用锁明文口令进 logcat；Release 用户打开"输出日志"后门开关即可被同设备应用/ADB/日志 SDK 捕获，口令常跨服务复用 | 移除所有把 pwd/oldPwd/newPwd 拼入日志的语句；敏感路径即使 DEBUG 也不打印明文；复核 logcatInRelease 后门 |

### High

| 标题 | 维度 | file:line | 严重度 | 影响 | 建议 |
|---|---|---|---|---|---|
| **账单导入去重金额单位+类型双重错配**：Long(分)列与 Double(元)值直接比较，"同天同金额"模糊去重永不命中（**跨 5 维同源合并**：数据层注入 / 正确性·金额 / 备份恢复 / 架构 / 测试） | 数据层/金额/备份/架构/测试 | core/database/.../dao/RecordDao.kt:435（参数 :442）；调用 feature/record-import/.../RecordImportViewModel.kt:183；列 RecordTable.kt:61 | High | 微信无单号/单号不一致条目重复导入不被标记为 POSSIBLE，重复记账、金额翻倍、资产余额偏差；精确单号分支仍有效故仅在无单号时暴露 | 统一为 Long 分：DAO/Repository/Fake 的 amount 改 Long，调用处传 `item.amount.toCent()`，补"同天同额无单号"命中单测 |
| WebDAV Basic 凭据在 debug 日志明文泄露（**3 条同源合并**：redactHeader 生产未调用 + LoggerInterceptor headersToRedact 空集 + DataSourceModule LEVEL_BODY） | 安全·网络 / 质量·错误处理 | core/network/.../di/DataSourceModule.kt:53-69（:57）；LoggerInterceptor.kt:126-127；OkHttpWebDAVHandler.kt:69/91/116/141/178/216 | High（debug 限定，部分核验意见认为可视作 Medium） | debug/Dev/Canary 渠道下 `Authorization: Basic base64(user:pass)`（可解码）写入 logcat，能读日志者还原 WebDAV 账密读改云端账本备份；Release 走 LEVEL_NONE | 构造 LoggerInterceptor 后调用 `redactHeader("Authorization")`（项目已有 API 与测试）；或为 WebDAV 单独提供强制 redact 的 Call.Factory |
| 备份链路允许 dav:// 明文 HTTP 回退，Basic 凭据可被中间人窃取（仅靠 manifest cleartext 单标志兜底） | 安全·网络 | core/data/.../BackupRecoveryManagerImpl.kt:330-339（:334） | High | dav:// 无条件 replace 为 http://（明文），随后带 Basic 凭据+整库发请求；代码层不拒绝 http，唯一兜底是 manifest `usesCleartextTraffic=false` | scheme 为 http 的 WebDAV 地址显式拒绝或强制 https；dav:// 映射到 https；保留 manifest 作第二层防御 |
| Keystore 密钥未要求用户认证，生物识别门禁可被绕过 | 安全·密钥 | core/design/.../security/Cipher.kt:112（及 :141/:52、CombineProtoDataSource.kt:396） | High | `setUserAuthenticationRequired(false)` 使 cipher 可独立 init/doFinal，root/可调试设备直接解密 passwordInfo/WebDAV 密码，绕过 BiometricPrompt，"启动安全验证"形同虚设 | 对应用锁/指纹密钥设 `setUserAuthenticationRequired(true)`（配 setInvalidatedByBiometricEnrollment），让解密真正依赖 BiometricPrompt |
| RecordModelTransToViewsUseCase 每条记录 6+ 次 DAO 查询，被多个 UseCase 在 .map 中逐条调用（贯穿统计/列表/资产详情的 N+1） | 性能·数据库 | core/domain/.../RecordModelTransToViewsUseCase.kt:46-90 | High | 单页 50 条记录触发数百次 SQLite 单行查询 + 协程上下文切换；记录列表/按类型标签分析/当前月视图高频屏卡顿、耗电；项目已有 JOIN 视图证明可批量化 | 批量解析一页记录（typeId/assetId/recordId 收集后 IN 批量查建 Map 内存组装），或列表路径改走 queryViewsBetweenDate JOIN 视图 |
| Analytics 路径一次性加载整段日期范围记录后逐条 N+1 放大，date 范围可达全年 | 性能·数据库 | core/domain/.../GetRecordViewsBetweenDateUseCase.kt:43-47 | High | 按年/全部统计时全量物化范围内所有 RecordTable，再对每条额外 6 次查询，数千条即数万次调用，分析页长时间阻塞（已有进度弹窗佐证耗时被感知） | 改用聚合 SQL/JOIN 视图在 DB 层取回轻量视图，去掉逐条 transToViews |
| 恢复为 upsert 合并而非清空替换，备份后新增数据在"恢复"后仍残留 | 正确性·备份恢复 | core/database/.../migration/DatabaseMigrations.kt:86（copyData :86-102） | High | 恢复语义实为"备份覆盖同主键行 + 保留当前库独有行"的并集，非"还原到备份时刻"；备份后新增记录/资产/账本恢复后仍存在、已删行无法复活，污染统计与余额 | 若需完整还原则事务内先 DELETE 再插入（注意外键/余额重算），或 UI 明确告知"恢复为合并"；恢复前自动安全备份 |
| Migration11To12（金额 REAL→INTEGER 全表重建）完全没有迁移测试 | 正确性·迁移 / 测试 | core/database/src/androidTest/.../DatabaseTest.kt（无 migrate11_12） | High | v11→v12 含 DROP TABLE + `CAST(ROUND(amount*100) AS INTEGER)` 金额换算 + 14 索引 + 固定类型插入，是全链路风险最高一笔却零覆盖，回归将在用户机上丢金额/崩溃 | 新增 migrate11_12：v11 插小数金额行，runMigrationsAndValidate(12)，断言金额变分整数、索引存在、固定类型已插（含 .005 边界） |
| 微信账单导入解析/匹配链路（WechatBillParser/BillCategoryMatcher/BillPaymentMatcher + 元→分）零测试覆盖 | 测试·覆盖 | feature/record-import/src/test/.../RecordImportViewModelTest.kt:48-105 | High | 解析金额错位、收支方向、分类/支付匹配、元→分精度错均无测试发现；导入为批量写入，金额转换错将污染大量记录与资产余额 | 为 WechatBillParser.parse 加表驱动单测、为两 Matcher 加映射单测、为 VM 加 happy-path 断言预览金额(分)/方向/去重与落库 RecordTable.amount |
| 四个 *RepositoryImplTest 均未实例化 RepositoryImpl，实为测试 Fake DAO/DataSource（命名误导、虚假覆盖） | 测试·覆盖 | core/data/src/test/.../AssetRepositoryImplTest.kt:38-58；SettingRepositoryImplTest.kt:37-67 | High | RepositoryImpl 真实业务逻辑（asModel/asTable 映射、dataVersion 自增、IO dispatcher 切换、异常分支）未被覆盖；映射 bug 时测试仍全绿（注：AssetRepositoryImplTest:150-207 直测了 asTable/asModel 函数本身，但未经 Impl 路径） | 真正实例化 RepositoryImpl(fakeDao, fakeDataSource, testDispatcher) 断言映射/副作用，或重命名为 FakeXxxDaoTest 诚实反映被测对象 |
| Migration11To12 无测试 + migrate6_7 空桩 + migrateAll 被注释（端到端迁移校验缺失） | 测试·覆盖 / 迁移 | core/database/src/androidTest/.../DatabaseTest.kt:494-497,699-716 | High | 金额元→分批量转换/最复杂的 6→7 五表重组/1→12 端到端 schema 校验均无拦截；空 migrate6_7 桩给出"已覆盖"假象 | 补 Migration11To12 与 migrate6_7 测试体，恢复并实现 migrateAll 链路校验 |
| 调试构建下 WebDAV Basic 认证头明文写入日志（与上"WebDAV Basic 凭据泄露"同源，质量维度视角） | 质量·错误处理 | core/network/.../LoggerInterceptor.kt:126-127；DataSourceModule.kt:57-68 | High（debug 限定） | 同 WebDAV 凭据泄露条目；Dev/Canary 渠道及开发者设备存在真实泄露面 | 同上：调用 redactHeader("Authorization")；复核 body 段是否打印含凭据响应体 |

> 注：上表"WebDAV Basic 凭据泄露"与"调试构建下 WebDAV Basic 认证头明文写入日志"为**同一装配缺陷的两个维度视角**（安全·网络 与 质量·错误处理），根因均为 `DataSourceModule` 未调用 `redactHeader`。修复一处即同时关闭两条。两条核验意见均指出泄露面限 debug/Dev/Canary 调试构建（Release LEVEL_NONE），故 High 评级偏保守，实际风险介于 Medium-High。

### Medium

| 标题 | 维度 | file:line | 严重度 | 影响 | 建议 |
|---|---|---|---|---|---|
| 更新 APK 下载后无完整性校验即调用系统安装器 | 安全·网络 | sync/util/WorkManagerAppUpgradeManager.kt:177-191；ApkDownloadWorker.kt:79-126 | Medium | release JSON 被篡改/响应被劫持时下载并安装未校验 APK；OS 安装期签名校验+externalFilesDir 私有限制风险 | release 携带并校验 APK SHA-256/签名；安装前用 PackageManager 校验证书指纹 |
| WebDAV 服务器地址用户输入未做 scheme/host 校验即落库使用 | 安全·网络 | feature/settings/.../BackupAndRecoveryViewModel.kt:137-154 | Medium | 可配任意 scheme(含 http)/host，凭据被发往任意服务器；配合明文回退放大误配置概率 | saveWebDAV/updateWebDAV 前校验 https/davs 前缀、host 合法；UI 提示而非静默接受 |
| xlsx 解析无解压大小上限，恶意 ZIP（解压炸弹）可触发 OOM 崩溃 | 安全·数据层 | core/data/.../helper/WechatBillParser.kt:95 | Medium | 诱导导入特制 xlsx 致 OOM（DoS）；外层 catch Exception 不捕 OutOfMemoryError | 对 entry 限制累计读取字节数/校验压缩比，超阈值中止返回 null |
| XML 解析器未限制内部实体扩展，恶意 sharedStrings.xml 可触发实体扩展放大（billion laughs） | 安全·数据层 | core/data/.../helper/WechatBillParser.kt:334 | Medium | 内嵌实体扩展放大内存致 OOM/卡顿（DoS）；Android 默认不取外部实体故无 XXE 文件读取 | 关闭 DOCTYPE 处理（FEATURE_PROCESS_DOCDECL=false）或加文本总量上限 |
| 导入文件缓存路径遍历：缓存文件名直接取自 content provider DISPLAY_NAME，未校验 `../` | 安全·数据层 | feature/settings/.../BackupAndRecoveryScreen.kt:804 | Medium | 理论上 input 文件被写到 cacheDir 外（覆盖 databases/ 等）；DISPLAY_NAME 通常不含分隔符且需用户主动选恶意源故门槛高 | 取 `File(displayName).name` 去路径成分，或写前校验 canonicalPath 前缀（复用 BackupRecoveryManagerImpl 现成逻辑） |
| AES 使用 CBC 模式无认证（非 AEAD），密文可被篡改 | 安全·密钥 | core/design/.../security/Cipher.kt:35 | Medium | WebDAV 密码/passwordInfo 密文被篡改无法检测，CBC 有 padding-oracle/位翻转风险；解密失败静默回退原文 | 改 AES/GCM/NoPadding，存 iv+GCM tag；解密失败明确视为错误而非回退原文 |
| 备份文件为未加密的原始 SQLite 数据库，无备份口令 | 安全·密钥 | core/data/.../BackupRecoveryManagerImpl.kt:392 | Medium | 备份含全部账本明文，落外部存储/第三方 WebDAV 后任何可读者获完整财务数据 | 提供可选口令加密（PBKDF2/scrypt/Argon2 + AES-GCM），或 UI 告知明文并强制 https/davs |
| 口令摘要使用单次 SHA-1，无盐无慢哈希 | 安全·密钥 | core/design/.../security/Cipher.kt:193 | Medium | 若 Keystore 层被绕过得到无盐 SHA-1，对常见数字/短口令可彩虹表/暴力快速还原 | 改加盐慢哈希（PBKDF2WithHmacSHA256/scrypt/Argon2），每用户随机盐 |
| Debug 构建下 WebDAV Authorization 被明文记录（同 WebDAV 凭据泄露根因，密钥维度记录） | 安全·密钥 | core/network/.../di/DataSourceModule.kt:57 | Medium | 同 WebDAV 凭据泄露；限定 debug | 调用 redactHeader("Authorization") |
| TransRecordViewsToAnalyticsPieUseCase 内联重算 amount+charge-concessions，绕过 calculateRecordAmount | 正确性·金额 | core/domain/.../TransRecordViewsToAnalyticsPieUseCase.kt:48-52（及 74-78） | Medium | 口径副本与 DAO 并存，未来转账/收入口径调整会使饼图金额与账目主链路漂移；当前仅统计收支分类故影响有限 | 抽公共纯函数 recordAmount(category,amount,charge,concessions) 供 DAO 与 UseCase 复用 |
| TransRecordViewsToAnalyticsPieSecondUseCase 同样内联重算金额公式（第二份副本） | 正确性·金额 | core/domain/.../TransRecordViewsToAnalyticsPieSecondUseCase.kt:48-52（及 66-70） | Medium | 与上同源口径漂移，三处（DAO+两 UseCase）公式各自演化放大遗漏概率 | 与上一并抽公共函数复用 |
| dailySummaries 以 `Map<String,RecordDayEntity>` 作 Composable 参数，FrontLayerContent 不可跳过（unstable parameter） | 性能·Compose | feature/records/.../LauncherContentScreen.kt:188,291,479,484（实声明 :483） | Medium | 每日汇总刷新/切月时整个 FrontLayerContent（含列表容器）重组一次；LazyColumn item 级 skip 仍保护列表项，成本落容器/Empty/Footer | 包 @Immutable wrapper/ImmutableMap，或在 ViewModel insertSeparators 时把 dayIncome/dayExpand 并入 DayHeader 避免下传整张 Map |
| SelectRelatedRecordScreen 两处 items 未提供 key，列表增删/重排丢失 item 身份 | 性能·Compose | feature/records/.../SelectRelatedRecordScreen.kt:156,183 | Medium | 两列表间移动条目时按 index 复用 slot，已组合项错位复用、状态/动画错配、不必要重组 | 两处补 `key = { it.id }`（RecordViewsEntity.id 稳定） |
| 日历每格农历/节日计算未缓存，月视图重组时对约 42 格重跑 solarToLunar | 性能·Compose | core/design/.../component/CalendarView.kt:250 | Medium | 月份切换/schemas 到达时全部日期格重算农历换算+节日匹配，低端机滑月可能掉帧 | `remember(currentDate){...}` 缓存逐格农历文本，或上层按 YearMonth 预计算整月文本 |
| 饼图标记线(leader line)布局在 Canvas 绘制 lambda 内每帧重算，含 ellipsize 与防重叠排序 | 性能·Compose | core/design/.../component/PieChart.kt:258 | Medium | 入场动画高 progress 帧区间每帧重算布局(文本测量+排序)，切片多时动画尾段抖动 | 把 layouts 提到 Canvas 外 remember 缓存（key 取 slices+尺寸），Canvas 内仅绘制 |
| queryAssetRecordsBetweenDateFlow 使用 pageSize=Int.MAX_VALUE 无界全量查询 | 性能·数据库 | core/data/.../RecordRepositoryImpl.kt:241-258 | Medium | 依赖"资产月度记录量级有限"假设，高频资产/宽日期全量物化进内存喂月度汇总；OR into_asset_id 难走单列索引 | 月度结余在 DB 层用 SUM+CASE 聚合，避免 Int.MAX_VALUE 无界 LIMIT 作常态接口 |
| 前导通配 LIKE 查询无法走索引导致全表扫描（关键字搜索/微信单号匹配） | 性能·数据库 | core/database/.../dao/RecordDao.kt:262-281（搜索 :267 / 微信去重 :424） | Medium | 搜索与微信去重匹配在记录量大时退化全表扫描；导入逐笔 LIKE 去重叠加批量显著拖慢 | 搜索引入 FTS4/5；微信单号结构化存储+等值匹配替代前导通配 LIKE |
| Repository 接口签名泄漏 core:database 的 RecordTable，破坏 data 层抽象边界 | 架构·分层 | core/data/.../repository/RecordRepository.kt:163 | Medium | batchImportRecords 是唯一以 DB 表实体为公共 API 参数的方法，强制 feature 层编译期依赖 core:database，schema 变更放大改动半径 | 参数改领域模型（List<RecordModel>/ImportRecordModel），Impl 内用 asTable() 转换；同步改 Fake |
| feature/record-import 直接依赖 core:database 并在 ViewModel 构造 RecordTable，破坏 feature→domain→data→database 分层 | 架构·分层 | feature/record-import/build.gradle.kts:32 | Medium | 8 个 feature 中唯一直依赖 core:database 且唯一不依赖 core:domain；持久化 schema 知识下沉 UI 层，RecordTable 变更直接编译破坏 ViewModel | 随上一条修复后移除 database 依赖，ViewModel 构造领域模型；导入业务规则可下沉为 core:domain 的 ImportRecordsUseCase |
| 去重查询 queryByTimeAndAmount 用 Double 比对 Long(分)（架构维度视角，同 TOP 1 根因） | 架构·分层 | core/database/.../dao/RecordDao.kt:435 | High（归入 TOP1，此处架构视角记录为高危） | 同 TOP1；额外违反 CLAUDE.md 强制金额约定 | 同 TOP1 |
| AutoBackupWorker 恒返回 Result.success()，自动备份失败既不重试也不上报 | 正确性·备份恢复 | sync/work/.../AutoBackupWorker.kt:55 | Medium | 可恢复失败(WebDAV 上传失败/IO 异常)被记成功，不触发退避重试、无失败提示，长期"自以为已备份实际从未成功" | doWork 读备份结果：可重试错误返 Result.retry()+setBackoffCriteria，不可重试返 failure()；失败发本地通知 |
| 缺少 1→12 端到端 migrateAll 校验（migrateAll 被整体注释） | 正确性·迁移 | core/database/src/androidTest/.../DatabaseTest.kt:702-715 | Medium | 任何中间迁移产出表结构与最终 @Entity/12.json 不一致都不会被自动发现；Room 端到端打开校验被关闭 | 取消注释并启用 migrateAll：createDatabase(1)+addMigrations(*MIGRATION_LIST) 打开触发 writableDatabase |
| migrate6_7 测试为空，最大规模表结构重组迁移无任何断言 | 正确性·迁移 | core/database/src/androidTest/.../DatabaseTest.kt:494-497 | Medium | 余额回算(BigDecimal)、标签关联拆分、MODIFY_BALANCE 过滤等复杂数据搬迁无回归保护 | 补全 migrate6_7：断言迁移后不含 MODIFY_BALANCE、db_tag_with_record 行数与 tag_ids 一致、balance 回算正确 |
| FakeTransactionDao 重写 deleteBookTransaction/deleteAssetRelatedData 为简化实现，JVM 测试注释与实际不符且不验证余额回退 | 测试·覆盖 | core/data/src/test/.../FakeTransactionDao.kt:236-254,308-311；TransactionDaoLogicTest.kt:178-252 | Medium | JVM 单测走 Fake 简化逻辑，余额回退正确性该层未验证，误导性注释给"真实默认实现已覆盖"错觉；androidTest 真机有兜底覆盖故限 Medium | 修正 Fake:308-311 注释或不 override 这两方法复用接口默认实现；弱化 LogicTest 对简化方法的行删除断言 |
| Bitmap.kt 用 printStackTrace() 吞异常，且 ExifInterface 路径异常时泄漏 InputStream | 质量·错误处理 | core/common/.../tools/Bitmap.kt:73-76,79-94,133-136,145-148 | Medium | 未走统一 logger；图片旋转读 EXIF 异常时 ContentResolver InputStream 未关闭，频繁选图累积 fd 泄漏；input!! 在 null 时抛 NPE 被吞 | printStackTrace 改 logger().e；ExifInterface 流用 input?.use{}；多处 openInputStream 统一 use 包裹 |
| runCatchWithProgress 用 runCatching 吞掉 CancellationException 破坏协程取消 | 质量·错误处理 | core/ui/.../DialogState.kt:111-137（:123-130） | Medium | 取消后仍当普通失败继续 dismiss，在已取消作用域做无意义工作并掩盖取消信号（协程+runCatching 典型反模式） | 失败分支显式 `if (throwable is CancellationException) throw throwable` 重抛 |
| BooksRepositoryImpl.deleteBook 的 runCatching 吞 CancellationException | 质量·错误处理 | core/data/.../repository/impl/BooksRepositoryImpl.kt:69-82 | Medium | 账本删除事务取消时静默当失败返回，调用方无法区分取消与真实失败，CancellationException 被记为 error 污染日志 | getOrElse 分支重抛 CancellationException，或 DB 事务用专门结果封装 |

### Low

| 标题 | 维度 | file:line | 严重度 | 影响 | 建议 |
|---|---|---|---|---|---|
| WebDAV 异常日志可能间接暴露含凭据信息的请求上下文 | 安全·网络 | core/network/.../util/OkHttpWebDAVHandler.kt:73-79 | Low | 不泄露密码，但服务器域名/路径/文件名(含日期)等元数据进日志，配合其它日志拼出网盘与备份规律 | 生产渠道收敛 WebDAV i 级日志，仅保留响应码与失败原因；与 redact 一并处理 |
| 加解密逻辑在两处重复实现，易产生不一致维护风险 | 安全·密钥 | core/datastore/.../CombineProtoDataSource.kt:384（vs Cipher.kt:29-97） | Low | 未来改 GCM/加用户认证需同步两处，遗漏其一致加解密不兼容或安全修复只覆盖部分路径 | WebDAV 密码加解密统一复用 core/design security 单一实现，删除重复副本 |
| decrypt 静默回退原文掩盖解密/数据损坏错误 | 安全·密钥 | core/design/.../security/Cipher.kt:93（及 CombineProtoDataSource.kt:425-441） | Low | Keystore 密钥失效/密文损坏时把密文当明文返回用于 Basic 认证；与 CBC 无认证叠加 | 区分"历史明文"与"解密异常"，后者记录可观测错误并提示重输，而非把密文当明文 |
| REQUEST_INSTALL_PACKAGES 未按渠道门控，Offline 渠道仍声明该敏感权限 | 安全·平台 | app/src/main/AndroidManifest.xml:26；app/build.gradle.kts:100-118 | Low | 违反权限最小化；Offline 持有用不到的安装权限扩大攻击面；导出组件关闭+不可变 PendingIntent 故可利用性低 | 改 manifestPlaceholders 门控仅 Online/Canary/Dev 注入，或 Offline 用 tools:node="remove" |
| FileProvider 暴露整个公共 Download 目录(external-path)，共享范围宽于实际所需 | 安全·平台 | app/src/main/res/xml/file_paths.xml:19-21 | Low | provider exported=false 且需显式 grant 故不构成越权；但 external-path Download 当前无使用方，配置面比实际宽 | 删除无使用方的 external-path Download 项；保留 external-files-path/cache-path 即可 |
| 微信单号去重采用 remark LIKE 通配模糊匹配，存在误判/绕过空间 | 安全·数据层 | core/database/.../dao/RecordDao.kt:420 | Low | 含 %/_ 单号误命中无关记录或备注被改后重复导入；无注入风险 | 增独立 transactionId 列加唯一/索引做结构化去重，或 LIKE 对元字符 ESCAPE |
| 导入条目缺金额/方向/类型有效性校验：金额可负/天文数字、未匹配类型(-1)条目被静默丢弃 | 安全·数据层 | core/data/.../helper/WechatBillParser.kt:272 | Low | 异常金额污染账目/余额；mappedTypeId=-1L 条目在事务里被 queryTypeById 静默跳过，imported 计数与勾选数不一致无提示 | convertToItem/confirmImport 前校验金额正且有界，非法跳过计入 skipped；typeId<=0 UI 层禁止勾选或回填默认 |
| 类型子分类逐父查询 N+1（一级类型列表渲染与编辑均逐个 queryByParentId） | 性能·数据库 | core/domain/.../GetRecordTypeListUseCase.kt:53-60 | Low（校准：原 Medium，一级分类数量级数十，finding 自承影响有限，下调） | 构建分类树需 1+M 次查询；记账页/我的分类页每次刷新触发 | 新增 typeDao 按 parent_id IN 批量查，一次取回二级分类内存按 parentId 分组 |
| db_tag 表缺少 books_id 索引，按账本删除标签为全表扫描 | 性能·数据库 | core/database/.../table/TagTable.kt:33-41 | Low | 删账本按 books_id 删标签全表扫描；标签表通常很小影响有限，属与其它表不一致的索引缺口 | 加 Index("books_id")（配套 schema 版本迁移与新 Migration），与 AssetTable 一致 |
| 分页全部基于 OFFSET，深翻页随 offset 线性退化；列表 UseCase 取页后又在内存重排 | 性能·数据库 | core/database/.../dao/RecordDao.kt:138-145 | Low | 深翻页每页成本随页码增长；多数用户不深翻影响低；GetAsset/TypeRecordViewsUseCase 对已排序结果再 sortedByDescending 冗余 | 高频列表改 keyset/seek 分页；移除冗余 sortedByDescending；优先级低 |
| DayHeaderItem 在 composition 体内做 split/toIntOrNull 解析未 remember，滚动每帧重复计算 | 性能·Compose | feature/records/.../LauncherContentScreen.kt:575-591 | Low | 单 header 计算小，但 LazyColumn 滚动时新进入 header 都执行；非热点瓶颈 | `remember(dateStr,dateSelectionType)` 缓存，或 ViewModel 端构造 DayHeader 时预算好 month/year |
| 列表项点击用 rememberHapticOnClick 包裹现场新建 lambda，remember(onClick) 缓存键每次都变，缓存恒失效 | 性能·Compose | feature/records/.../LauncherContentScreen.kt:540-544（同写法遍布多屏） | Low | remember 形同虚设，代价仅每次重组多分配一轻量 lambda+Modifier.clickable 重建；列表项数据不变时不重组故触发频率低 | 回调改接收稳定标识（onRecordItemClick:(Long)->Unit + item.id），或 item 作用域内 remember(item.id) | 
| WheelPicker 占位与选项缺稳定 key，且每帧离屏合成绘制渐变遮罩 | 性能·Compose | core/design/.../component/WheelPicker.kt:111 | Low | 缺 key 可能项重组/状态错位；离屏合成+每帧渐变遮罩是该视觉效果固定成本；可见项少影响有限 | items 增稳定 key；离屏渐变遮罩可保留，卡顿时再评估 Brush 直接绘制 |
| 饼图 totalValue 在每次重组重算（未随 sweepAngles 一起缓存） | 性能·Compose | core/design/.../component/PieChart.kt:131 | Low | 极小：一次 slices.sumOf，切片数小；无正确性问题 | 并入 remember(slices) 一并缓存 |
| 我的资产页用 verticalScroll + Column.forEach 渲染全部资产项（非懒加载） | 性能·Compose | feature/assets/.../MyAssetScreen.kt:332 | Low | 资产规模有限时可接受；数量很大时全部一次 compose+measure 无回收，潜在首帧/重组成本上升 | 预期项多则改 LazyColumn+items（分组头与子项扁平化加稳定 key）；当前列为观察项 |
| Double.toCent() 用浮点 *100 再 roundToLong，相对 BigDecimal 版存在理论精度风险（导入链路使用） | 正确性·金额 | core/common/.../ext/Money.kt:58 | Low | 常规 2 位小数账单 roundToLong 容错足够，仅异常大额/构造边界值可能 1 分误差；属健壮性改进非现网 bug | 走 BigDecimal 路径或先转字符串再 toAmountCent 统一精度策略；补大额/边界单测 |
| batchImportRecordsTransaction 缺转账目标资产余额更新且用 queryTypeById 跳过平账类型，与单条插入路径口径分叉 | 正确性·业务 | core/database/.../dao/TransactionDao.kt:395-451 | Low | 未来批量导入支持转账/平账时会现转账目标余额不更新(账实不平)或平账记录被静默丢弃；当前唯一调用方恒 intoAssetId=-1L 且真实类型故不触发 | 改用 resolveType；补 TRANSFER intoAssetId 目标资产余额累计；或 KDoc 声明仅支持非转账真实类型并断言 |
| Migration8To9 新增 bg_uri 列默认值用反引号 `` 而非字符串字面量 '' | 正确性·迁移 | core/database/.../migration/Migration8To9.kt:44 | Low（校准：原 Medium，已被 migrate8_9 测试覆盖且实际产出空串通过，属风格/健壮性隐患非当前回归，下调） | 依赖 SQLite"反引号引用无法解析时当字符串字面量"的非标准 quirk 侥幸得空串，写法脆弱且与其它迁移不一致 | 改单引号字面量 `DEFAULT '' NOT NULL`，与 Migration4To5/5To6 对齐 |
| Migration6To7 写入 record_time 时用 getString 取值（类型隐式强转） | 正确性·迁移 | core/database/.../migration/Migration6To7.kt:516-519 | Low | record_time 列为 INTEGER 亲和，数字字符串隐式转整数通常不出错；含非数字字符时落库为文本与 Long 期望不符 | 改 `it.getLong(it.getColumnIndexOrThrow("record_time"))` 与列类型一致 |
| v11→v12 金额换算 CAST(ROUND(amount*100) AS INTEGER) 对历史 double 存在半分舍入风险 | 正确性·迁移 | core/database/.../migration/Migration11To12.kt:68-71,118-119 | Low | 极少数历史金额 REAL→分换算可能 1 分误差（浮点固有边界）；影响面小但金额按分精确下值得记录 | 换算前对元值做字符串/定点修正；在 migrate11_12 测试覆盖 1.005/0.005 边界固化预期 |
| 周期自动备份未设置任何 WorkManager Constraints | 正确性·备份恢复 | sync/work/.../AutoBackupWorker.kt:73 | Low | 缺 StorageNotLow/网络约束，低存储/无网时仍唤起 Worker 做无效尝试（多在业务层降级影响有限） | 加合理 Constraints（setRequiresStorageNotLow 等）；网络判断刻意下沉则注释说明 |
| content-uri 备份先删同名文件再 createFile，createFile 失败时留下删除窗口 | 正确性·备份恢复 | core/data/.../BackupRecoveryManagerImpl.kt:414 | Low | 仅同秒同名碰撞且随后 createFile 失败时丢失该旧备份；触发概率低、keepLatest 主删路径顺序正确 | 改"先 createFile 写入成功再删旧文件"，或临时名写成功后重命名 |
| 多个 Compose 屏文件偏大（最大 ~55KB / SettingScreen），可读性与维护成本偏高 | 架构·分层 | feature/settings/.../SettingScreen.kt（及 BackupAndRecoveryScreen/EditRecordScreen/MyCategoriesScreen/MainApp） | Low | 纯 UI 体量无分层越界；单文件过大不利 review/增量编译/重组边界拆分；非阻塞 | 可选拆 section/对话框/列表项为同包私有子文件；非阻塞 |
| RecordRepositoryImpl/TypeRepositoryImpl/BackupRecoveryManagerImpl 真实 impl 逻辑未被实例化覆盖 | 测试·覆盖 | core/data/.../RecordRepositoryImpl.kt；TypeRepositoryImpl.kt；BackupRecoveryManagerImpl.kt | Low（校准：原 Medium，标题"无任何单元测试"被既存的 RecordRepositoryImplTest/TypeRepositoryImplTest 证伪，但二者与 High 项同源 Fake-pattern；仅 Backup 真无单测，下调） | 与"虚假覆盖"同源；BackupRecoveryManagerImpl 真实备份/恢复/WebDAV/zip 逻辑无 JVM 单测 | 为 BackupRecoveryManagerImpl 纯逻辑(文件名生成/列表过滤/状态机)补单测，IO 部分临时目录端到端验证一次 |
| 截图测试只渲染硬编码 UiState，不经 ViewModel 映射，且依赖手工录制基线可能随签名变更静默失配 | 测试·覆盖 | feature/assets/src/test/.../AssetInfoScreenScreenshotTests.kt:45-132；core/testing/.../ScreenshotHelper.kt:44-49 | Low | 验渲染外观而非数据正确性，金额/分类计算 bug 不被捕获；非 fork PR 下 CI 自动 recordRoborazzi 回填基线，弱化对意外视觉/签名变更把关 | 截图保渲染、VM 单测保 UiState 计算；评估受信任分支也要求人工确认基线变更 |
| Time.kt 的 dateFormat 捕获了错误的异常类型（ParseException 而非 IllegalArgumentException） | 质量·错误处理 | core/common/.../tools/Time.kt:68-75,97-104 | Low | 非法 format 模式抛 IllegalArgumentException 会穿透崩溃而非返回 ""；当前调用方均传硬编码合法常量故现实风险有限 | format 路径 catch 改 IllegalArgumentException 或 Exception |
| PagingSource.load 的 runCatching 将 CancellationException 包成 LoadResult.Error | 质量·错误处理 | feature/records/.../SearchViewModel.kt:122-134；AssetInfoContentViewModel.kt:142-154 | Low | 正常取消(关键字变更切换 PagingSource)被当加载错误记日志、短暂触发错误 UI 态；Paging 容忍度高 | getOrElse 分支先重抛 CancellationException 再返回 LoadResult.Error |
| WAL checkpoint busy 仅打 warn 仍继续复制数据库，备份可能不完整 | 正确性·备份恢复 | core/data/.../BackupRecoveryManagerImpl.kt:380-390 | Low | 有并发写/长事务时备份缺最近写入（仅复制主 db 不含 -wal/-shm）；代码识别但未重试/失败处理 | busy!=0 时重试 checkpoint 数次或标记备份失败/告警，而非静默继续 |

### Info

| 标题 | 维度 | file:line | 严重度 | 影响 | 建议 |
|---|---|---|---|---|---|
| 月度结余 isCreditCard 由调用方传入（校准：核验证伪标题断言——实为从资产 type 派生 AssetInfoViewModel.kt:77，与 verifyAssetBalance 同源，仅在 Success 态渲染无瞬态窗口，原 Medium 降 Info） | 正确性·业务 | core/domain/.../GetAssetMonthSummaryUseCase.kt:48-86 | Info | 无方向符号反转的实际正确性缺陷；isCreditCard 下沉进 UseCase 删入参属可维护性改进 | 可将信用卡判定下沉 UseCase 内用 getAssetById 读资产 type 推导，删入参消除 desync 单点 |
| EditRecordViewModel 在 UI 层重复实现记录金额聚合公式，业务规则轻微外溢 | 正确性·业务 | feature/records/.../EditRecordViewModel.kt:143-149 | Info | 无即时正确性错误(关联记录均支出，公式与 DAO 支出分支一致)；存在公式分叉后显示与入库口径不一致演进风险 | 下沉到 domain 复用共享 recordAmount 入口，或加注释标明与 calculateRecordAmount 口径绑定 |
| toMoneyFormat 用字符串后缀('00'/'0')裁剪去尾零，依赖 toMoneyString 固定两位小数隐式契约，较脆弱 | 正确性·金额 | core/common/.../ext/Money.kt:39-46 | Info | 当前无功能缺陷；一旦上游格式契约变化而此处未同步，会现金额被错误截断隐患（需先改 toMoneyString 才触发） | 改基于数值格式化（按 fen%100/fen%10 判保留位），降低与 toMoneyString 隐式耦合 |
| Migration5To6 用字符串 format 拼接 UPDATE SQL（值为内部数值，非注入但偏离参数化范式） | 安全·数据层 | core/database/.../migration/Migration5To6.kt:65,93 | Info | 无实际注入/数据风险；插入值均 DB 内部列读出外部不可控；仅范式/可维护性提示 | 可改 execSQL(sql, bindArgs) 参数化绑定（DelegateSQLiteDatabase.kt:268 已支持）；非必须 |
| app-catalog 为独立可安装展示模块且导出 LAUNCHER Activity（信息项，确认非主 App 风险面） | 安全·平台 | app-catalog/src/main/AndroidManifest.xml:27-35；app-catalog/build.gradle.kts:22-26 | Info | 独立 applicationId(cn.wj.android.cashbookcatalog) 开发期演示包，不打入主 APK，对主 App 攻击面无影响 | 无需代码改动；CI/发布脚本确认 app-catalog 不进对外分发产物 |
| Analytics 分类/拆分报表用 repeat() 在非 Lazy 容器内一次性物化全部行 | 性能·Compose | feature/records/.../AnalyticsScreen.kt:421-464,537-553 | Info | barDataList(按日约 30 行)与饼图分类行规模有界，一次物化可接受未达瓶颈；跨度拉大或分类极多时首帧组合时间线性增长 | 数据可控维持现状；若支持更大跨度则 SplitReports 行改外层 LazyColumn items 加 key |
| 导入底栏金额合计存在死分支：if/else 两支表达式完全相同 | 正确性·备份恢复 | feature/record-import/.../RecordImportScreen.kt:328 | Info | 无功能错误(对所选金额无符号求和)；复制粘贴残留误导维护者以为区分了收支正负 | 区分收支则补 else 取负/单独统计；仅绝对值合计则删 if/else 直接写并加注释 |
| 恢复迁移/版本失败复用 FAILED_BACKUP_PATH_UNAUTHORIZED 误导性错误码 | 正确性·备份恢复 | core/data/.../BackupRecoveryManagerImpl.kt:621 | Info | 版本不兼容/迁移失败显示为"路径未授权"，用户误判为权限问题难处置(不丢数据但降级提示不准确) | 为迁移失败/版本不匹配引入独立错误码（复用 FAILED_FILE_FORMAT_ERROR 或新增），UI 区分权限/格式/版本三类 |

---

## 三、优先修复清单（Top 10）

1. **移除应用锁明文口令日志 + 复核 Release 日志后门**（Critical，SettingViewModel.kt:220/187/247）：删除 pwd/oldPwd/newPwd 拼入日志的语句，敏感路径即使 DEBUG 也不打明文；评估 logcatInRelease 后门是否应禁用敏感日志。最高优先，凭据跨服务复用风险外溢。
2. **统一账单导入去重金额为 Long 分**（High，RecordDao.kt:435 等 5 处同源）：DAO/Repository/Fake 的 amount 改 Long，调用处传 `item.amount.toCent()`，补"同天同额无单号"命中单测。一次修复同时关闭数据层/金额/备份/架构/测试 5 个维度的发现且符合 CLAUDE.md 金额约定。
3. **WebDAV Authorization 日志脱敏**（High，DataSourceModule.kt:57）：构造 LoggerInterceptor 后调用 `redactHeader("Authorization")`（项目已有 API + 测试），一处关闭安全·网络与质量·错误处理两条同源 High。
4. **拒绝 dav:// 明文回退，带凭据请求强制 https**（High，BackupRecoveryManagerImpl.kt:334）：scheme 为 http 的 WebDAV 显式拒绝，dav:// 映射到 https，保留 manifest 作第二层防御。
5. **应用锁/指纹 Keystore 密钥设 setUserAuthenticationRequired(true)**（High，Cipher.kt:112/141）：让解密真正依赖 BiometricPrompt 成功，关闭 root/可调试设备绕过门禁的根因。
6. **补 Migration11To12 + migrate6_7 测试并恢复 migrateAll**（High，DatabaseTest.kt:494-497/702-715）：金额 REAL→INTEGER 全表重建零测试是数据损坏级风险，补断言金额换算/索引/固定类型 + 端到端 schema 校验。
7. **记录视图转换批量化消除 N+1**（High，RecordModelTransToViewsUseCase.kt:46-90 + GetRecordViewsBetweenDateUseCase.kt:43-47）：一页记录收集 id 后 IN 批量查建 Map 内存组装，或列表/Analytics 路径改走 JOIN 视图，直接消除列表与分析页卡顿热点。
8. **修正恢复语义（合并 vs 清空替换）**（High，DatabaseMigrations.kt:86）：明确并实现"完整还原"（事务内先 DELETE 再插入 + 余额重算）或 UI 明确告知"恢复为合并"；恢复前自动安全备份。
9. **补导入解析链路与 RepositoryImpl 真实测试**（High×2，RecordImportViewModelTest.kt + *RepositoryImplTest）：为 WechatBillParser/两 Matcher/VM happy-path 加测试；让 RepositoryImpl 测试真正实例化 Impl 断言映射与副作用（消除虚假覆盖）。
10. **AutoBackupWorker 失败重试/上报 + 抽公共 recordAmount 函数**（Medium，AutoBackupWorker.kt:55 + 两 Analytics UseCase）：备份失败返回 retry/failure 并通知用户；抽 `recordAmount(category,amount,charge,concessions)` 公共纯函数供 DAO 与两个饼图 UseCase 复用，消除三份口径副本。

---

## 四、亮点与做得好的地方

- **金额约定执行较彻底**：全链路 `Long` 分、`Double.toCent()`/`String.toAmountCent()` 转换、UI 层 `toMoneyString` 回显，落库链路正确（RecordImportViewModel.kt:285 写库用 `toCent()`）。本次去重 bug 恰恰是少数偏离该约定之处，反衬约定本身有效。
- **设计系统强约束**：禁用裸 Material3 组件并由 lint `Design` error 拦截，约束清晰且可构建期阻断。
- **迁移测试有相当覆盖**：migrate1_2…migrate10_11 多数有 `runMigrationsAndValidate` 断言，Migration8To9 反引号 quirk 与 Zip Slip 防护（BackupRecoveryManagerImpl.kt:575-577）等都有测试/防护到位——本次发现的缺口（11→12、6_7、migrateAll）是已有体系中的明确空洞而非全面缺失。
- **Compose 重计算多处已正确处理**：SplitReports `remember(barDataList)`、AnalyticsBarChart `remember(dataList,selectedTab)`、sweepAngles `remember(slices)`、getSpecialFestival 按年缓存等，说明性能意识普遍存在，剩余几处未 remember 属个别遗漏。
- **安全防护已有第二层**：manifest `usesCleartextTraffic=false`、FileProvider exported=false + 显式 grant + 不可变 PendingIntent、Zip Slip canonicalPath 校验、androidTest 真机覆盖余额回退（TransactionDaoTest）等，多处已有纵深防御；本次安全发现多为"代码层应再加一层"而非完全裸奔。
- **app-catalog 隔离干净**：独立 applicationId、allowBackup=false、不打入主 APK，演示模块与主 App 攻击面隔离到位。

> 重要提醒：本批次发现**无 uncertain（待运行期确认）项**——所有发现 verdict 均为 confirmed 或 adjusted（核验后调整严重度）。标注「校准」的条目其代码事实已核验，仅严重度按横向可比原则重新校准；其中 SettingViewModel 的 Critical 项剔除了一处造假引用（MainAppViewModel.kt:281）但核心成立，月度结余 isCreditCard 项因标题断言被调用链证伪而由 Medium 降为 Info。