# 备份/恢复流式化设计（下载 get + 导出 SAF + 上传 put(file)）

- 日期：2026-07-10
- 状态：设计已经节点1四维评审（feasibility/security/reverse/impact）+ controller hands-on 核验，待用户 review
- 关联：#2「恢复 content:// 流式化」（已完成，`stageInputStreamToCache`）的对称后续；本 spec 处理**创建/下载/上传**方向残留的 `readBytes()` 堆物化

## 1. 背景与目标

Cashbook 的备份 zip 含数据库 + 用户图片（可达数十 MB）。恢复侧的整流 `readBytes()` 堆物化已在 #2 消除（`stageInputStreamToCache` 8KB 流式 copyTo）。本次消除剩余 3 处同类反模式，使备份/恢复全链路峰值堆内存从 O(fileSize) 降到 O(8KB)。

**目标**：三处流式化，行为保持（下载/导出/上传结果不变），单测守护，无残留 High 风险。

**非目标**：备份 zip 打包（`putZipFileEntry`/`writeStoredZipEntry` 已流式 `copyTo(zos)`）；恢复侧（#2 已完成）；WebDAV 传输协议/凭据机制变更。

## 2. 现状：3 处 `readBytes()` 堆物化（实证 file:line）

| # | 位置 | 现状 | 方向 |
|---|---|---|---|
| A | `core/data/.../BackupRecoveryManagerImpl.kt:485` 导出到 SAF | `outputStream.use { it.write(zippedFile.readBytes()) }` 全量物化整个 zip | 创建/导出 |
| B | `core/data/.../BackupRecoveryManagerImpl.kt:236` + `core/network/.../OkHttpWebDAVHandler.kt:221` 下载 | `WebDAVHandler.get(url): ByteArray?` = `response.body?.bytes()` 全量入堆，`getWebFile` `writeBytes(bytes)` | WebDAV 下载 |
| C | `core/network/.../OkHttpWebDAVHandler.kt:109` 上传 | `put(dataStream)` 内 `dataStream.readBytes()` 全量物化；`upload` content:// 分支（`BackupRecoveryManagerImpl.kt:150-152`）用它 | WebDAV 上传 |

**已流式、无需改（对照）**：`putZipFileEntry:568`（`FileInputStream.buffered().copyTo(zos)`）、`writeStoredZipEntry:934`、`put(url, file)`（`OkHttpWebDAVHandler.kt:142` `file.asRequestBody()`）、上传 file 分支、`stageInputStreamToCache:997`。

## 3. 节点1四维评审结论（已 hands-on 核验）

四维（feasibility/security/reverse/impact）一致收敛，**无 Critical**；feasibility/reverse 标的 High 均落在「`put(dataStream)` 流式化」与「示例代码漏 `use`」——在下述改进方案（改走 `put(file)` + 嵌套 use）下消解。

**三个决定性强断言已 controller 独立核验成立**：

1. **F5 存活窗口**（上传改法基石）：`upload(:527)` 被调用时，cache zip 删除 `backupCacheDir.deleteAllFiles()` 在 `:549` 才执行且 `if (!ApplicationInfo.isDev)` 门控——上传时本地 zip 确实存活，可复用走 `put(file)`。
2. **OkHttp DI 未覆盖 retry/redirect**：`DataSourceModule.kt:70-75` 的 `OkHttpClient.Builder()` 仅设 timeout + interceptor，**无** `retryOnConnectionFailure(false)`/`followRedirects(false)`——重试/重定向会重发 body（非 repeatable 的流式 body 会读到耗尽流）。
3. **LoggerInterceptor 破坏 one-shot 流**：`LoggerInterceptor.kt:139-141` 无条件 `requestBody.writeTo(buffer)`、**无 `isOneShot` 守卫**，且是 network interceptor + debug LEVEL_BODY——debug 下会 100% 消耗 one-shot 上传流。

结论 2+3 共同判定：**自定义流式 `RequestBody` 上传方案不可行**（debug 必坏 + release retry/redirect 可能坏 + chunked 被坚果云拒）。改走 `put(file)`（repeatable + 有 Content-Length）规避全部。

**合并 finding（去重/severity 校准后）**：

| # | 主题 | 维度 | 校准 | 处置（本 spec 采纳） |
|---|---|---|---|---|
| 1 | 上传 `put(dataStream)` 流式化撞 retry/redirect + chunked + LoggerInterceptor | reverse/impact/feasibility/security | High（规避） | 不改 `put(dataStream)`；`upload` 改用本地 `zippedFile` 走 `put(file)` |
| 2 | 下载/导出 `copyTo` 漏关目标流 → 截断/fd 泄漏 | feasibility | High（实施约束） | 嵌套双 `use`：`out.use { in.use { copyTo } }` |
| 3 | `get` 失败态：保 `""` 哨兵 + `.part` 临时名成功才 rename + 保 `isSuccessful` gate | impact/reverse/security | Medium | 见 §4 Commit 3 |
| 4 | 流式下载无大小上限 → 磁盘 DoS | security | Medium | copy 循环 max-bytes cap（`MAX_RECOVERY_TOTAL_BYTES`），超限删 part + false |
| 5 | MockWebServer happy-path 抓不到 chunked/retry/截断会假绿 | reverse/feasibility | Medium | 补 Content-Length header 断言 + 大文件 + 断连用例 |
| 6 | 三处风险不对称须拆三笔独立 commit | reverse | Low | §4 三笔独立可回滚 |
| 7 | getWebFile dest 加 `isWithinDir` guard（与 #2 对称，非可利用）；导出 copyTo 失败删 backupFile | security | Low | 纳入（defense-in-depth） |
| 8 | pre-existing：`followSslRedirects=true`、URL 日志 | security | Low | 记 §7 backlog，本次不动 |

## 4. 方案：三笔独立 commit

拆分依据（finding 6）：三处风险差异大，导出最安全收益最大先落，get 接口变更连带测试独立一笔，上传独立一笔；每笔可独立 revert。

### Commit 1 — 导出到 SAF 流式化（最安全、最大单缓冲）

`BackupRecoveryManagerImpl.kt:484-486` content:// 导出分支：

```kotlin
// 现状：outputStream.use { it.write(zippedFile.readBytes()) }
outputStream.use { out ->
    zippedFile.inputStream().buffered().use { it.copyTo(out) }
}
```

- 嵌套双 `use`（finding 2）：保留 `outputStream.use`（SAF 流 close 时才 finalize），内层关 input。
- finding 7：`copyTo` 抛异常时删已创建的 `backupFile`（与 null-stream 分支 `:480-482` 对称），避免留半截目标文件。
- 纯本地、零接口变更、无网络耦合。

### Commit 2 — WebDAV 上传改走 `put(file)`（消两条 High，不碰 `put(dataStream)`）

- `upload` 改签名接收本地 `File`（存活的 `zippedFile`），`startBackup:527` 传 `zippedFile` 而非 `backupFileUri`：

```kotlin
private suspend fun upload(localZip: File): Boolean = withContext(ioCoroutineContext) {
    withCredentials { root -> put(root + localZip.name, localZip, "application/octet-stream") }
}
```

- `zippedFile` 与原 content:// 目标内容一致（SAF 目标本就是 `zippedFile` 写出，见 Commit 1），语义等价。
- `put(url, file)`（`OkHttpWebDAVHandler.kt:142` `file.asRequestBody()`）已流式 + repeatable + 有 Content-Length，天然规避 chunked/one-shot/retry 三坑。
- `put(url, dataStream, contentType)`（`OkHttpWebDAVHandler.kt:105`）**保持不动**；其唯一生产调用方（`upload` content 分支）移除后成 dead code → 加 KDoc 注明「保留供未来流式上传，当前无生产调用方；如需启用须先解决 LoggerInterceptor one-shot + chunked 兼容，见 spec」，不删（保测试 + 避免连带删 `put(InputStream)` 接口方法影响其他潜在实现）。
- 复用既有 `zippedFile` 省去从 SAF `openInputStream` 读回的一次全量 IO（附带收益）。

### Commit 3 — WebDAV 下载 `get` 接口流式化（连带测试独立一笔）

**接口**（`WebDAVHandler.kt:55`）：
```kotlin
// 现状：suspend fun get(url: String): ByteArray?
suspend fun get(url: String, dest: File): Boolean   // 流式下载到 dest；成功 true
```

**OkHttp 实现**（`OkHttpWebDAVHandler.kt:208-227` 重写）：
```kotlin
override suspend fun get(url: String, dest: File): Boolean = withContext(ioCoroutineContext) {
    if (url.isBlank()) return@withContext false
    runCatching {
        callFactory.newCall(request(url)).execute().use { response ->
            if (!response.isSuccessful) return@use false            // finding 3：非2xx 不写 dest
            val body = response.body ?: return@use false
            body.byteStream().use { input ->
                dest.outputStream().use { output ->                 // finding 2：嵌套双 use
                    copyWithCap(input, output, MAX_RECOVERY_TOTAL_BYTES) // finding 4：size cap
                }
            }
            true
        }
    }.getOrElse { throwable ->
        logger().e(throwable, "get(url=<$url>)")
        false
    }.also { ok -> if (!ok) dest.delete() }                          // finding 3：失败清残留
}
```

- `copyWithCap`：抽 top-level `internal fun`（便于单测，符合 CLAUDE.md「抽纯函数」约定），逐块累计，超 cap 抛/返 false。
- **Offline**（`OfflineWebDAVHandler.kt:45`）：`override suspend fun get(url: String, dest: File): Boolean = false`。
- **调用方 `getWebFile`**（`BackupRecoveryManagerImpl.kt:221-240`）：
```kotlin
withCredentials {
    if (!get(url, backupCacheFile)) return@withCredentials ""   // finding 3：保 "" 哨兵
    require(isWithinDir(backupCacheFile, cacheDir))              // finding 7：与 #2 对称
    backupCacheFile.path
}
```
移除 `:233 createNewFile`（实现负责写 dest）与 `:237 writeBytes`。
> 关于 `.part` 临时名（finding 3）：`getWebFile` 已在入口 `deleteAllFiles()`（:230）清缓存目录 + 每次用固定 `backupCacheFile`，且失败经 `.also{ dest.delete() }` 清残留 + Boolean 门控保 `""` 哨兵——半截文件不会被当完整备份。`.part`→rename 为可选加固（若实现简单则采纳，否则「失败删 + Boolean 门控」已闭合，spec 不强制）。

**测试**（`OkHttpWebDAVHandlerTest.kt:140/148/157` 重写 + 补）：
- `get` 3 用例改为传 `TemporaryFolder` dest：空 url→false 且 dest 未写；2xx→true 且 `dest.readBytes()==body`；非2xx→false 且 dest 无残留。
- 补 size cap 超限用例（大 body→false + dest 删）。
- finding 5：上传方向改 `put(file)` 后，对 `put(file)` 补 `RecordedRequest.getHeader("Content-Length")!=null` 断言（证非 chunked）。

## 5. 影响面（impact 维度已核验闭合）

- `WebDAVHandler.get` 生产调用**仅** `getWebFile:236`；实现**仅** OkHttp + Offline 两个（无 Fake/mock 实现 WebDAVHandler）；测试仅 `OkHttpWebDAVHandlerTest.kt`。
- DI/Hilt（`DataSourceModule:90-100` 按接口 provide，构造签名不变）、备份 zip 格式、DataStore、feature/app 层（经 `BackupRecoveryManager` 门面，不直接依赖 `WebDAVHandler`）**均不受影响**。
- 纯进程内接口变更，无跨进程/序列化/已发布 API 契约。
- `put(dataStream)` 签名不变，编译零影响；其唯一生产调用方 `upload:151` 由 Commit 2 移除。

## 6. 测试策略

- **Commit 1**：`BackupRecoveryManagerImpl` 导出分支——core:data test 不实例化 Impl（`CombineProtoDataSource` final 不可 mock），故抽 `copyStream`/复用逻辑为可测；导出整体行为由现有 `BackupZipRoundTripTest`（JVM）+ 真机 journey 覆盖。
- **Commit 2**：`upload` 走 `put(file)`——`OkHttpWebDAVHandlerTest` 对 `put(file)` 补 Content-Length header 断言；upload 编排等价性由真机 journey（WebDAV 上传）覆盖。
- **Commit 3**：`OkHttpWebDAVHandlerTest` get 3 重写 + size cap；`copyWithCap` top-level 单测；MockWebServer 局限（自动 dechunk、happy-path）已知，chunked 服务端兼容非本次引入（改 put(file) 后无 chunked）。
- 完整链路：core:network/core:data 单测 + `:app:compileOnlineDebugKotlin`（跨模块 Hilt）+ spotlessCheck + lint；真机 journey（备份创建→WebDAV 上传→下载恢复往返）有设备时补跑。

## 7. Backlog（pre-existing，非本次 scope）

- security Low：WebDAV client `followSslRedirects=true`（TLS→cleartext 重定向保留 Authorization），可选 `.followSslRedirects(false)` hardening；URL 日志 userinfo 泄漏（debug-only、opt-in、logcat-only，极低）。
- `put(dataStream)` 若未来需真流式上传：须解决 LoggerInterceptor one-shot 守卫 + `contentLength()` 显式 + `isOneShot()=true` + 坚果云 chunked PoC。
- `getWebFile` 的 `.part` 临时名 rename（若 Commit 3 未采纳，作为加固 backlog）。

## 8. 成功标准

- 三处峰值堆内存 O(fileSize)→O(8KB)。
- 导出/上传/下载行为保持：导出 zip 完整、WebDAV 上传成功且非 chunked（有 Content-Length）、下载恢复往返成功、下载失败干净收敛到 Failed（不 crash、不假成功）。
- 无残留 High；single-responsibility 三笔独立可回滚。
- 单测绿 + 编译 + spotless + lint 通过。
