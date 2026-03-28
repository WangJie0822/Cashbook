# 补充缺失测试设计文档

## 概述

基于覆盖率报告和项目分析，按价值密度优先（方案 A）全面补充缺失的单元测试。采用混合策略：核心逻辑用 JVM 单元测试，涉及 Android 框架的部分用 Robolectric。

## 现有测试基础设施

| 设施 | 状态 |
|------|------|
| Fake Repository（7 个） | 已有，部分需增强 |
| TestDispatcherRule | 已有 |
| TestDataFactory | 已有 |
| JUnit 4 + Google Truth | 已配置 |
| Robolectric | 已配置 |
| MockWebServer | **需新增** |
| Turbine | 不新增（保持现有 Flow 测试模式） |

## 测试基础设施补充

### 新增依赖

- `com.squareup.okhttp3:mockwebserver` — 用于 `core/network` 的 HTTP 模拟测试
- 添加到 `gradle/libs.versions.toml` 并在 `core/network/build.gradle.kts` 中引用

### Fake 增强

- `FakeRecordRepository.queryExportRecords()` — 从固定返回空列表改为返回可配置的 `exportRecordsList` 字段
- 新增 `FakeDailyAccountExporter` — 放在 `core/testing` 模块中，记录传入参数并返回可配置的导出数量

---

## P0：core/common 测试补充

### NumberTest.kt

- 路径：`core/common/src/test/kotlin/cn/wj/android/cashbook/core/common/ext/NumberTest.kt`
- 测试类型：JVM 单元测试
- 覆盖目标：`ext/Number.kt`

| 用例 | 说明 |
|------|------|
| `when_null_string_toBigDecimalOrZero_then_returns_zero` | null 返回 BigDecimal.ZERO |
| `when_empty_string_toBigDecimalOrZero_then_returns_zero` | 空字符串返回 0 |
| `when_invalid_string_toBigDecimalOrZero_then_returns_zero` | "abc" 返回 0 |
| `when_valid_string_toBigDecimalOrZero_then_returns_value` | "19.99" 正确解析 |
| `when_null_number_toBigDecimalOrZero_then_returns_zero` | null Number 返回 0 |
| `when_valid_number_toBigDecimalOrZero_then_returns_value` | 正常数字正确转换 |
| `when_null_string_toFloatOrZero_then_returns_zero` | null 返回 0f |
| `when_valid_string_toFloatOrZero_then_returns_value` | 正常解析 |
| `when_null_string_toDoubleOrZero_then_returns_zero` | null 返回 0.0 |
| `when_valid_string_toDoubleOrZero_then_returns_value` | 正常解析 |
| `when_null_string_toIntOrZero_then_returns_zero` | null 返回 0 |
| `when_valid_string_toIntOrZero_then_returns_value` | 正常解析 |
| `when_single_digit_completeZero_then_pads` | 5 → "05" |
| `when_double_digit_completeZero_then_no_pad` | 10 → "10" |
| `when_zero_completeZero_then_pads` | 0 → "00" |

### StringTest.kt

- 路径：`core/common/src/test/kotlin/cn/wj/android/cashbook/core/common/ext/StringTest.kt`
- 测试类型：JVM 单元测试
- 覆盖目标：`ext/String.kt`

| 用例 | 说明 |
|------|------|
| `when_https_url_then_isWebUri_true` | "https://example.com" → true |
| `when_http_url_then_isWebUri_true` | "http://example.com" → true |
| `when_ftp_url_then_isWebUri_false` | "ftp://example.com" → false |
| `when_empty_string_then_isWebUri_false` | "" → false |
| `when_content_uri_then_isContentUri_true` | "content://xxx" → true |
| `when_http_uri_then_isContentUri_false` | "http://xxx" → false |
| `when_positive_amount_withCNY_then_adds_prefix` | "19.99" → "¥19.99" |
| `when_negative_amount_withCNY_then_negative_before_prefix` | "-19.99" → "-¥19.99" |
| `when_already_has_cny_withCNY_then_no_duplicate` | "¥19.99" → "¥19.99"（去重） |

---

## P0：core/domain 测试补充

### ExportRecordUseCaseTest.kt

- 路径：`core/domain/src/test/kotlin/cn/wj/android/cashbook/domain/usecase/ExportRecordUseCaseTest.kt`
- 测试类型：JVM 单元测试
- 依赖：`FakeRecordRepository`（增强后）、`FakeDailyAccountExporter`

| 用例 | 说明 |
|------|------|
| `when_export_records_then_delegates_to_repository_and_exporter` | 验证正确调用 repository 查询 + exporter 导出 |
| `when_no_records_then_returns_zero` | 无记录时返回 0 |
| `when_has_records_then_returns_count` | 有记录时返回正确数量 |
| `when_repository_returns_records_then_passes_to_exporter` | repository 返回的数据完整传递给 exporter |

---

## P0：Feature ViewModel 测试补充

### AssetInfoContentViewModelTest.kt

- 路径：`feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/AssetInfoContentViewModelTest.kt`
- 测试类型：Robolectric
- 依赖：FakeGetAssetRecordViewsUseCase（测试内新建）

| 用例 | 说明 |
|------|------|
| `when_update_asset_id_then_paging_source_uses_correct_id` | 更新 assetId 后 PagingSource 使用正确 ID |
| `when_load_returns_data_then_paging_result_is_page` | 正常加载返回 LoadResult.Page |
| `when_load_returns_empty_then_next_key_is_null` | 空数据时 nextKey 为 null |

### RecordImportViewModelTest.kt

- 路径：`feature/record-import/src/test/kotlin/cn/wj/android/cashbook/feature/record/imports/viewmodel/RecordImportViewModelTest.kt`
- 测试类型：Robolectric
- 依赖：SavedStateHandle + FakeRecordRepository + FakeTypeRepository + FakeAssetRepository + FakeBooksRepository
- 需要测试资源文件：小型微信账单 CSV 放在 test resources 中

| 用例 | 说明 |
|------|------|
| `when_file_not_found_then_ui_state_error` | 文件不存在时进入 Error 状态 |
| `when_parse_success_then_ui_state_ready` | 解析成功进入 Ready 状态 |
| `when_toggle_item_selection_then_selected_state_changes` | 切换选中状态 |
| `when_select_all_then_all_items_selected` | 全选 |
| `when_confirm_import_then_records_saved_and_state_done` | 确认导入后数据写入且进入 Done |
| `when_duplicate_detected_then_item_marked` | 重复检测（精确+模糊） |

### TypeIconGroupListViewModelTest.kt

- 路径：`feature/types/src/test/kotlin/cn/wj/android/cashbook/feature/types/viewmodel/TypeIconGroupListViewModelTest.kt`
- 测试类型：Robolectric（AndroidViewModel 需 Application 上下文）

| 用例 | 说明 |
|------|------|
| `when_initial_then_first_group_selected` | 默认选中第一组 |
| `when_select_group_then_icon_list_updates` | 切换组后图标列表更新 |
| `when_select_group_then_selected_state_correct` | 选中状态正确传播 |

---

## P1：core/network 测试补充

### NetworkDataSourceTest.kt

- 路径：`core/network/src/test/kotlin/cn/wj/android/cashbook/core/network/NetworkDataSourceTest.kt`
- 测试类型：MockWebServer + JVM
- 依赖：MockWebServer、Retrofit（指向 localhost）、kotlinx.serialization.json.Json

| 用例 | 说明 |
|------|------|
| `when_use_gitee_then_calls_gitee_api` | useGitee=true 时请求 Gitee API |
| `when_not_use_gitee_then_calls_github_api` | useGitee=false 时请求 GitHub API |
| `when_release_name_starts_with_Release_then_returns_it` | 筛选 "Release" 前缀的版本 |
| `when_canary_true_then_includes_pre_release` | canary=true 时包含 "Pre Release" 前缀 |
| `when_canary_false_then_excludes_pre_release` | canary=false 时排除预发布版 |
| `when_no_matching_release_then_returns_null` | 无匹配版本时返回 null |
| `when_api_error_then_propagates_exception` | 网络错误（如 HTTP 500）时异常向上传播 |

### LoggerInterceptorTest.kt

- 路径：`core/network/src/test/kotlin/cn/wj/android/cashbook/core/network/LoggerInterceptorTest.kt`
- 测试类型：MockWebServer + JVM

| 用例 | 说明 |
|------|------|
| `when_level_none_then_no_log_output` | LEVEL_NONE 不输出日志 |
| `when_level_basic_then_logs_method_and_url` | LEVEL_BASIC 输出请求方法和 URL |
| `when_level_body_then_logs_response_body` | LEVEL_BODY 输出响应体 |
| `when_redact_header_then_header_value_hidden` | 脱敏 header 值隐藏 |

### GitReleaseEntitySerializationTest.kt

- 路径：`core/network/src/test/kotlin/cn/wj/android/cashbook/core/network/GitReleaseEntitySerializationTest.kt`
- 测试类型：纯 JVM 单元测试

| 用例 | 说明 |
|------|------|
| `when_valid_json_then_deserializes_correctly` | 正常 JSON 反序列化 |
| `when_browser_download_url_field_then_maps_to_downloadUrl` | @SerialName 映射正确 |
| `when_unknown_fields_then_ignores` | 未知字段不报错 |
| `when_null_fields_then_handles_gracefully` | nullable 字段为 null 时正常 |

---

## P1：core/datastore 测试补充

### CombineProtoDataSourceTest.kt

- 路径：`core/datastore/src/test/kotlin/cn/wj/android/cashbook/core/datastore/CombineProtoDataSourceTest.kt`
- 测试类型：Robolectric（加密依赖 Android Keystore）/ FakeDataStore
- 依赖：FakeDataStore（内存实现替代真实 Proto DataStore）

**数据读写：**

| 用例 | 说明 |
|------|------|
| `when_update_dark_mode_then_app_settings_reflects` | 更新深色模式后同步 |
| `when_update_dynamic_color_then_app_settings_reflects` | 更新动态配色后同步 |
| `when_update_current_book_id_then_record_settings_reflects` | 更新当前账本 ID 后同步 |
| `when_update_keywords_then_search_history_reflects` | 更新搜索关键词后同步 |
| `when_update_latest_version_data_then_git_data_reflects` | 更新版本信息后同步 |

**WebDAV 加密/解密：**

| 用例 | 说明 |
|------|------|
| `when_update_webdav_then_password_encrypted` | 写入 WebDAV 后密码不是明文 |
| `when_read_webdav_then_password_decrypted` | 读取时密码还原 |
| `when_legacy_plain_password_then_returns_as_is` | 旧格式兼容 |

**数据迁移：**

| 用例 | 说明 |
|------|------|
| `when_split_app_preferences_then_data_migrated` | 旧 AppPreferences 拆分到 AppSettings + RecordSettings |
| `when_split_already_done_then_idempotent` | 重复拆分不出错 |

**needRelated() 逻辑：**

| 用例 | 说明 |
|------|------|
| `when_type_is_refund_then_returns_true` | 退款类型返回 true |
| `when_type_is_reimburse_then_returns_true` | 报销类型返回 true |
| `when_type_is_normal_then_returns_false` | 普通类型返回 false |

---

## P2：app 模块测试补充

### MainViewModelTest.kt

- 路径：`app/src/test/kotlin/cn/wj/android/cashbook/ui/MainViewModelTest.kt`
- 测试类型：JVM 单元测试
- 依赖：FakeSettingRepository

| 用例 | 说明 |
|------|------|
| `when_initial_then_ui_state_loading` | 初始状态为 Loading |
| `when_settings_emitted_then_ui_state_success` | 设置数据发射后转为 Success |
| `when_dark_mode_changes_then_ui_state_reflects` | 深色模式变更正确反映 |
| `when_dynamic_color_changes_then_ui_state_reflects` | 动态配色变更正确反映 |

### ActivityUiStateTest.kt

- 路径：`app/src/test/kotlin/cn/wj/android/cashbook/ui/ActivityUiStateTest.kt`
- 测试类型：Robolectric（shouldUseDarkTheme 调用 AppCompatDelegate）

| 用例 | 说明 |
|------|------|
| `when_loading_shouldDisableDynamicTheming_then_false` | Loading 时不禁用动态配色 |
| `when_success_dynamicColor_false_then_disable_true` | dynamicColor=false 时禁用 |
| `when_success_dynamicColor_true_then_disable_false` | dynamicColor=true 时不禁用 |
| `when_loading_shouldUseDarkTheme_then_false` | Loading 时不使用深色 |
| `when_dark_mode_follow_system_then_returns_system_value` | 跟随系统 |
| `when_dark_mode_on_then_returns_true` | 强制深色 |
| `when_dark_mode_off_then_returns_false` | 强制浅色 |

---

## 不在范围内

| 模块/文件 | 原因 |
|----------|------|
| `ext/Int.kt`、`ext/Time.kt`、`ext/File.kt`、`ext/Flow.kt` | 无业务逻辑或逻辑极简 |
| `tools/Resource.kt`、`manager/AppManager.kt` | 强依赖 Android 框架，JVM 单元测试不适用 |
| `MainActivity`、`MarkdownActivity`、`MainApp.kt` | Compose + Activity 生命周期粘合代码，需 Instrumented Test |
| `OfflineDataSource`、`DataSourceModule`、`UrlDefinition` | 无逻辑或纯 DI 配置 |
| `WebDAVHandler` / `OkHttpWebDAVHandler` | 依赖真实 WebDAV 服务器 |
| `DataStoreModule`、各 Serializer | DI 配置 / 纯 protobuf 委托 |
| `ExampleUnitTest.kt` | 保留占位，不修改 |

---

## 测试约定（与现有代码一致）

- **断言库**：Google Truth (`assertThat`)
- **协程测试**：`TestDispatcherRule` + `UnconfinedTestDispatcher` + `runTest`
- **ViewModel 测试**：Robolectric + `backgroundScope.launch` 收集 StateFlow
- **命名规范**：`when_条件_then_预期结果`
- **文件头**：Apache 2.0 License Header
- **测试替身**：手写 Fake，不使用 Mockito/MockK

## 新增测试汇总

| 模块 | 新增测试类 | 类型 | 优先级 |
|------|----------|------|--------|
| core/common | `NumberTest`、`StringTest` | JVM | P0 |
| core/domain | `ExportRecordUseCaseTest` | JVM | P0 |
| feature/records | `AssetInfoContentViewModelTest` | Robolectric | P0 |
| feature/record-import | `RecordImportViewModelTest` | Robolectric | P0 |
| feature/types | `TypeIconGroupListViewModelTest` | Robolectric | P0 |
| core/network | `NetworkDataSourceTest`、`LoggerInterceptorTest`、`GitReleaseEntitySerializationTest` | MockWebServer + JVM | P1 |
| core/datastore | `CombineProtoDataSourceTest` | Robolectric/Fake | P1 |
| app | `MainViewModelTest`、`ActivityUiStateTest` | Robolectric | P2 |

**共 12 个新测试类，约 70+ 测试用例。**
