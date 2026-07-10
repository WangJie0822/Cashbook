# 备份/恢复流式化 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 消除备份/恢复「导出 SAF / WebDAV 上传 / WebDAV 下载」三方向的 `readBytes()` 堆物化，峰值堆内存 O(fileSize)→O(8KB)，行为保持。

**Architecture:** 三笔独立 commit。导出用嵌套双 `use` 流式 copy；上传改走已流式的 `put(url, file)`（规避 `put(dataStream)` 的 chunked/one-shot/retry 三坑，本次不改 `put(dataStream)`）；下载把 `WebDAVHandler.get(url): ByteArray?` 改为 `get(url, dest, maxBytes): Boolean` 流式写文件 + 大小上限 + 失败清理。

**Tech Stack:** Kotlin, OkHttp 4.12.0, okio 3.9.0, kotlinx-coroutines, JUnit4 + Truth + MockWebServer（core:network test）；core:data test 仅 junit4 + truth。

## Global Constraints

- 峰值堆内存不得整流物化整个备份文件（禁止 `readBytes()`/`bytes()` 全量入堆）。
- **嵌套双 `use`**：`copyTo` 不 flush/close 目标流，输入流与输出流都必须各自 `use`（否则 fd 泄漏 + 目标文件截断）。
- **core:network 不依赖 core:data**：下载大小上限 `maxBytes` 由 core:data 调用方（`getWebFile`）传入常量 `MAX_RECOVERY_TOTAL_BYTES`，core:network 不重复定义该常量。
- **`get` 失败契约**：`get(...)==false` ⇒ dest 无残留（实现负责删）+ 调用方 `getWebFile` 映射到 `""` 哨兵（不返回半截 path）。
- 非 2xx 响应**不写** dest（保留 `isSuccessful` gate）。
- `put(dataStream)`（`OkHttpWebDAVHandler.kt:105`）本次**不改**；其唯一生产调用方由 Task 2 移除后成 dead code，加 KDoc 注明。
- 每个源文件需 Apache 2.0 License Header（Spotless 检查，模板在 `spotless/`）；提交前 `spotlessApply`。
- core:network 模块无 flavor，测试任务 `:core:network:testDebugUnitTest`；core:data 同理 `:core:data:testDebugUnitTest`。

---

### Task 1: 导出到 SAF 流式化（Commit 1）

**Files:**
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/uitl/impl/BackupRecoveryManagerImpl.kt`（新增 top-level `writeFileToStream`；改导出段 `:479-508`）
- Test: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/util/BackupStreamCopyTest.kt`（新建）

**Interfaces:**
- Produces: `internal fun writeFileToStream(src: File, out: OutputStream)`（流式把 src 写入 out，负责关闭 out 与内部 input 流）

- [ ] **Step 1: 写失败测试（内容完整 + out 被关闭）**

创建 `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/util/BackupStreamCopyTest.kt`（含 License Header）：

```kotlin
package cn.wj.android.cashbook.core.data.util

import cn.wj.android.cashbook.core.data.uitl.impl.writeFileToStream
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FilterOutputStream
import java.io.OutputStream

class BackupStreamCopyTest {

    @Test
    fun writeFileToStream_copies_full_content() {
        val src = File.createTempFile("src", ".bin")
        val payload = ByteArray(200_000) { (it % 256).toByte() }
        src.writeBytes(payload)
        val out = ByteArrayOutputStream()

        writeFileToStream(src, out)

        assertThat(out.toByteArray()).isEqualTo(payload)
        src.delete()
    }

    @Test
    fun writeFileToStream_closes_output_stream() {
        val src = File.createTempFile("src", ".bin")
        src.writeBytes("hello".toByteArray())
        var closed = false
        val out: OutputStream = object : FilterOutputStream(ByteArrayOutputStream()) {
            override fun close() { closed = true; super.close() }
        }

        writeFileToStream(src, out)

        assertThat(closed).isTrue()
        src.delete()
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew :core:data:testDebugUnitTest --tests "cn.wj.android.cashbook.core.data.util.BackupStreamCopyTest"`
Expected: FAIL（`writeFileToStream` 未定义，编译错误）

- [ ] **Step 3: 实现 `writeFileToStream`**

在 `BackupRecoveryManagerImpl.kt` 文件末尾（与既有 `stageInputStreamToCache`/`writeStoredZipEntry` 等 top-level internal fun 同区域）新增：

```kotlin
/**
 * 流式把 [src] 文件内容写入 [out]，8KB 缓冲、O(1) 堆内存。
 * 负责关闭 [out] 与内部 input 流（嵌套双 use，避免 copyTo 不 flush/close 目标流导致的截断/泄漏）。
 * 抽为 top-level internal fun 便于单测（无需构造 Manager / Android Context）。
 */
internal fun writeFileToStream(src: File, out: OutputStream) {
    out.use { output ->
        src.inputStream().buffered().use { it.copyTo(output) }
    }
}
```

确认文件顶部已 import `java.io.OutputStream`（如缺则补）。

- [ ] **Step 4: 运行测试确认通过**

Run: `./gradlew :core:data:testDebugUnitTest --tests "cn.wj.android.cashbook.core.data.util.BackupStreamCopyTest"`
Expected: PASS（2 个测试）

- [ ] **Step 5: 导出段接线 + copyTo 失败删 backupFile**

`BackupRecoveryManagerImpl.kt` content:// 导出分支 `:479-486`，现状：
```kotlin
val outputStream = context.contentResolver.openOutputStream(backupFile.uri)
if (outputStream == null) {
    backupFile.delete()
    return@runCatching BackupRecoveryState.FAILED_BACKUP_PATH_UNAUTHORIZED
}
outputStream.use {
    it.write(zippedFile.readBytes())
}
```
改为：
```kotlin
val outputStream = context.contentResolver.openOutputStream(backupFile.uri)
if (outputStream == null) {
    backupFile.delete()
    return@runCatching BackupRecoveryState.FAILED_BACKUP_PATH_UNAUTHORIZED
}
try {
    writeFileToStream(zippedFile, outputStream)
} catch (e: IOException) {
    backupFile.delete() // 写入中途失败：删半截目标文件（与 openOutputStream==null 分支对称）
    throw e
}
```
确认文件已 import `java.io.IOException`（如缺则补）。

- [ ] **Step 6: 编译 + spotless**

Run: `./gradlew :core:data:compileDebugKotlin :core:data:testDebugUnitTest`
Expected: BUILD SUCCESSFUL，BackupStreamCopyTest 2 PASS
Run: `./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache`

- [ ] **Step 7: Commit**

```bash
git add core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/uitl/impl/BackupRecoveryManagerImpl.kt \
        core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/util/BackupStreamCopyTest.kt
git commit -m "[perf|core:data|备份导出流式][公共]导出 SAF 改嵌套双 use 流式 copy 消 readBytes 堆物化 + 失败删半截文件"
```

---

### Task 2: WebDAV 上传改走 `put(file)`（Commit 2）

**Files:**
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/uitl/impl/BackupRecoveryManagerImpl.kt`（`upload` 签名 `:143-158`；调用点 `:527`）
- Modify: `core/network/src/main/kotlin/cn/wj/android/cashbook/core/network/util/OkHttpWebDAVHandler.kt`（`put(dataStream)` 加 dead-code KDoc `:105`）
- Test: `core/network/src/test/kotlin/cn/wj/android/cashbook/core/network/util/OkHttpWebDAVHandlerTest.kt`（补 put(file) 头断言）

**Interfaces:**
- Consumes: `WebDAVHandler.put(url: String, file: File, contentType: String): Boolean`（既有，已流式 `asRequestBody`）
- Produces: `private suspend fun upload(localZip: File): Boolean`

- [ ] **Step 1: 写失败测试（put(file) 请求带 Content-Length、非 chunked）**

在 `OkHttpWebDAVHandlerTest.kt` 的 put 用例区（`:102` 之后）追加。文件顶部补 import：`import java.io.File`、`import okhttp3.mockwebserver.RecordedRequest`（后者按需，`takeRequest()` 返回类型已隐含可用）。

```kotlin
    @Test
    fun when_put_file_then_request_has_content_length_not_chunked() = runTest {
        val file = File.createTempFile("backup", ".zip")
        file.writeBytes("zip-binary-body".toByteArray())
        mockWebServer.enqueue(MockResponse().setResponseCode(201))

        val ok = handler.put(url(), file, "application/octet-stream")

        assertThat(ok).isTrue()
        val recorded = mockWebServer.takeRequest()
        assertThat(recorded.getHeader("Content-Length")).isNotNull()
        assertThat(recorded.getHeader("Transfer-Encoding")).isNull()
        assertThat(recorded.body.readUtf8()).isEqualTo("zip-binary-body")
        file.delete()
    }
```

- [ ] **Step 2: 运行测试确认通过（守护现状：put(file) 本就流式非 chunked）**

Run: `./gradlew :core:network:testDebugUnitTest --tests "cn.wj.android.cashbook.core.network.util.OkHttpWebDAVHandlerTest.when_put_file_then_request_has_content_length_not_chunked"`
Expected: PASS（`file.asRequestBody()` 天然设 Content-Length = file.length()，非 chunked）
> 此测试是**回归护栏**（守护「上传走 put(file) 即非 chunked」这一 Task 2 依赖的前提），故一开始即 PASS，非红→绿。

- [ ] **Step 3: 改 `upload` 签名为接收本地 File**

`BackupRecoveryManagerImpl.kt:143-158`，现状 `upload(backupFileUri: Uri)` 含 content:// 读回分支，改为：

```kotlin
private suspend fun upload(localZip: File): Boolean = withContext(ioCoroutineContext) {
    this@BackupRecoveryManagerImpl.logger()
        .i("upload(localZip = <${localZip.name}>)")
    withCredentials { root ->
        put(root + localZip.name, localZip, "application/octet-stream")
    }
}
```
移除原 content:// 的 `DocumentFile.fromSingleUri(...).name` + `openInputStream().use { put(bis) }` 分支（不再从 SAF 读回，直接用本地 zip）。确认不再需要的 import（若 `DocumentFile`/`Uri` 仍被文件他处使用则保留）。

- [ ] **Step 4: 改调用点传本地 `zippedFile`**

`BackupRecoveryManagerImpl.kt:527`，现状 `if (upload(backupFileUri))` 改为：
```kotlin
if (upload(zippedFile)) {
```
`zippedFile`（`:465 val zippedFile = File(zippedPath)`）在 `startBackup` 作用域内、且 `backupCacheDir.deleteAllFiles()`（`:549`）晚于此处，上传时存活；内容与 `backupFileUri` 目标一致（SAF 目标本就是 `zippedFile` 由 Task 1 写出）。

- [ ] **Step 5: `put(dataStream)` 加 dead-code KDoc**

`OkHttpWebDAVHandler.kt:104-105`，在 `override fun put(url: String, dataStream: InputStream, contentType: String)` 上方补 KDoc：
```kotlin
    /**
     * 流式版本保留：当前无生产调用方（备份上传已改走 [put] 的 File 重载）。
     * 若未来启用 InputStream 上传，须先解决：LoggerInterceptor 无 isOneShot 守卫会消耗流、
     * contentLength 未知触发 chunked（坚果云等可能拒收）、OkHttp retry/redirect 重发耗尽流。
     * 详见 docs/superpowers/specs/2026-07-10-backup-streaming-design.md。
     */
    @WorkerThread
    override fun put(url: String, dataStream: InputStream, contentType: String): Boolean {
```

- [ ] **Step 6: 编译（跨模块 Hilt 全图）+ 测试 + spotless**

Run: `./gradlew :core:network:testDebugUnitTest :core:data:compileDebugKotlin :app:compileOnlineDebugKotlin`
Expected: BUILD SUCCESSFUL（upload 唯一调用点已改；app 跨模块 Hilt 图编译通过）
Run: `./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache`

- [ ] **Step 7: Commit**

```bash
git add core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/uitl/impl/BackupRecoveryManagerImpl.kt \
        core/network/src/main/kotlin/cn/wj/android/cashbook/core/network/util/OkHttpWebDAVHandler.kt \
        core/network/src/test/kotlin/cn/wj/android/cashbook/core/network/util/OkHttpWebDAVHandlerTest.kt
git commit -m "[perf|上传|WebDAV上传流式][公共]upload 改用本地 zip 走 put(file) 消 readBytes 堆物化，规避 chunked/one-shot/retry"
```

---

### Task 3: WebDAV 下载 `get` 接口流式化（Commit 3）

**Files:**
- Modify: `core/network/src/main/kotlin/cn/wj/android/cashbook/core/network/util/WebDAVHandler.kt`（接口 `get` 签名 `:55`）
- Modify: `core/network/src/main/kotlin/cn/wj/android/cashbook/core/network/util/OkHttpWebDAVHandler.kt`（`get` 重写 `:208`；新增 top-level `copyToCapped`）
- Modify: `core/network/src/main/kotlin/cn/wj/android/cashbook/core/network/util/OfflineWebDAVHandler.kt`（`get` 签名 `:45`）
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/uitl/impl/BackupRecoveryManagerImpl.kt`（`getWebFile` `:221-240`）
- Test: `core/network/src/test/kotlin/cn/wj/android/cashbook/core/network/util/OkHttpWebDAVHandlerTest.kt`（get 3 重写 + cap + copyToCapped）

**Interfaces:**
- Produces: `internal fun InputStream.copyToCapped(out: OutputStream, maxBytes: Long): Long`（8KB 流式 copy，累计超 maxBytes 抛 IOException，返回已 copy 字节数）
- Produces: `suspend fun WebDAVHandler.get(url: String, dest: File, maxBytes: Long): Boolean`（流式下载写 dest；非 2xx/失败/超 cap → false 且 dest 无残留）
- Consumes: `MAX_RECOVERY_TOTAL_BYTES`（`BackupRecoveryManagerImpl` 既有常量，`getWebFile` 传入）；`isWithinDir(file, dir)`（`BackupRecoveryManagerImpl` 既有）

- [ ] **Step 1: 写 `copyToCapped` 失败测试**

在 `OkHttpWebDAVHandlerTest.kt` get 用例区之前新增（文件顶部补 import：`import java.io.ByteArrayOutputStream`、`import java.io.IOException`、`import org.junit.Assert.assertThrows`）：

```kotlin
    // ---------------- copyToCapped ----------------

    @Test
    fun copyToCapped_within_cap_copies_all_bytes() {
        val input = ByteArrayInputStream(ByteArray(500) { 1 })
        val out = ByteArrayOutputStream()
        val copied = input.copyToCapped(out, maxBytes = 1000)
        assertThat(copied).isEqualTo(500L)
        assertThat(out.size()).isEqualTo(500)
    }

    @Test
    fun copyToCapped_exceeds_cap_throws() {
        val input = ByteArrayInputStream(ByteArray(2000))
        val out = ByteArrayOutputStream()
        assertThrows(IOException::class.java) {
            input.copyToCapped(out, maxBytes = 1000)
        }
    }
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :core:network:testDebugUnitTest --tests "*OkHttpWebDAVHandlerTest.copyToCapped*"`
Expected: FAIL（`copyToCapped` 未定义）

- [ ] **Step 3: 实现 `copyToCapped`**

在 `OkHttpWebDAVHandler.kt` 文件末尾（class 外，与文件内其他 top-level 同级）新增：

```kotlin
/**
 * 流式把 receiver 输入流内容写入 [out]，8KB 缓冲、O(1) 堆内存；
 * 累计写入超过 [maxBytes] 立即抛 IOException（防不可信来源的磁盘耗尽 DoS）。返回已 copy 字节数。
 * 抽为 top-level internal fun 便于单测。调用方负责各自 use 输入/输出流。
 */
internal fun InputStream.copyToCapped(out: OutputStream, maxBytes: Long): Long {
    val buffer = ByteArray(8 * 1024)
    var total = 0L
    while (true) {
        val read = read(buffer)
        if (read < 0) break
        total += read
        if (total > maxBytes) throw IOException("download exceeds cap: $maxBytes bytes")
        out.write(buffer, 0, read)
    }
    return total
}
```
文件顶部补 import：`import java.io.IOException`、`import java.io.InputStream`（若 InputStream 已 import 则跳过）、`import java.io.OutputStream`、`import java.io.File`。

- [ ] **Step 4: 运行确认通过**

Run: `./gradlew :core:network:testDebugUnitTest --tests "*OkHttpWebDAVHandlerTest.copyToCapped*"`
Expected: PASS（2 个）

- [ ] **Step 5: 改接口签名 + Offline 实现**

`WebDAVHandler.kt:55`，现状 `suspend fun get(url: String): ByteArray?` 改为：
```kotlin
    /**
     * 流式下载 [url] 到 [dest] 文件；成功返回 true。
     * 失败/非 2xx/超 [maxBytes] 上限 → 返回 false 且 dest 无残留。
     */
    suspend fun get(url: String, dest: File, maxBytes: Long): Boolean
```
接口文件顶部补 `import java.io.File`。

`OfflineWebDAVHandler.kt:45`，现状 `override suspend fun get(url: String): ByteArray? = null` 改为：
```kotlin
    override suspend fun get(url: String, dest: File, maxBytes: Long): Boolean = false
```
补 `import java.io.File`。

- [ ] **Step 6: 写 get 流式重写的失败测试（3 重写 + cap）**

在 `OkHttpWebDAVHandlerTest.kt` 替换现有 3 个 get 用例（`:138-158`）为：

```kotlin
    // ---------------- get (GET) 流式下载到 dest ----------------

    @Test
    fun when_get_blank_url_then_false_and_no_request() = runTest {
        val dest = File.createTempFile("dl", ".zip").also { it.delete() }
        assertThat(handler.get("", dest, maxBytes = 1024)).isFalse()
        assertThat(mockWebServer.requestCount).isEqualTo(0)
        assertThat(dest.exists()).isFalse()
    }

    @Test
    fun when_get_success_then_writes_dest_and_true() = runTest {
        val body = "backup-binary-content"
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(body))
        val dest = File.createTempFile("dl", ".zip")

        val ok = handler.get(url(), dest, maxBytes = 1_000_000)

        assertThat(ok).isTrue()
        assertThat(dest.readText()).isEqualTo(body)
        dest.delete()
    }

    @Test
    fun when_get_error_response_then_false_and_dest_deleted() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(404))
        val dest = File.createTempFile("dl", ".zip")

        val ok = handler.get(url(), dest, maxBytes = 1_000_000)

        assertThat(ok).isFalse()
        assertThat(dest.exists()).isFalse()
    }

    @Test
    fun when_get_exceeds_cap_then_false_and_dest_deleted() = runTest {
        val body = "x".repeat(10_000)
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(body))
        val dest = File.createTempFile("dl", ".zip")

        val ok = handler.get(url(), dest, maxBytes = 100)

        assertThat(ok).isFalse()
        assertThat(dest.exists()).isFalse()
    }
```
更新文件顶部类 KDoc 第 4 行 `get(GET) 对 200/404/空 url 的字节流映射` → `get(GET) 流式下载到 dest 文件 + 大小上限 + 失败清理`。

- [ ] **Step 7: 运行确认失败（旧 get 签名不匹配）**

Run: `./gradlew :core:network:testDebugUnitTest --tests "*OkHttpWebDAVHandlerTest*"`
Expected: FAIL（编译错误：`get(url())` 旧签名 / OkHttp 实现未改）

- [ ] **Step 8: 重写 `OkHttpWebDAVHandler.get`**

`OkHttpWebDAVHandler.kt:207-227`，整个 `get` 方法替换为：

```kotlin
    @WorkerThread
    override suspend fun get(url: String, dest: File, maxBytes: Long): Boolean = withContext(ioCoroutineContext) {
        if (url.isBlank()) {
            return@withContext false
        }
        val ok = runCatching {
            callFactory.newCall(
                Request.Builder()
                    .url(url)
                    .addHeader("Authorization", Credentials.basic(account, password))
                    .build(),
            ).execute().use { response ->
                logger().i("get(url = <$url>), response = <${response.code}>")
                if (!response.isSuccessful) return@use false          // 非 2xx 不写 dest
                val body = response.body ?: return@use false
                body.byteStream().use { input ->                      // 嵌套双 use
                    dest.outputStream().use { output ->
                        input.copyToCapped(output, maxBytes)          // 流式 + 大小上限
                    }
                }
                true
            }
        }.getOrElse { throwable ->
            logger().e(throwable, "get(url = <$url>)")
            false
        }
        if (!ok) dest.delete()                                        // 失败清半截 dest
        ok
    }
```

- [ ] **Step 9: 运行确认通过（含 copyToCapped + get 4 用例 + 既有 exists/put/list）**

Run: `./gradlew :core:network:testDebugUnitTest --tests "*OkHttpWebDAVHandlerTest*"`
Expected: PASS（全部：exists 3 + put 4 + list 2 + get 4 + copyToCapped 2）

- [ ] **Step 10: 改 `getWebFile` 调用方（哨兵 + isWithinDir）**

`BackupRecoveryManagerImpl.kt:221-240`，现状：
```kotlin
private suspend fun getWebFile(url: String): String = withContext(ioCoroutineContext) {
    val fileName = url.split("/").last()
    if (!fileName.startsWith(BACKUP_FILE_NAME) || !fileName.endsWith(BACKUP_FILE_EXT)) {
        return@withContext ""
    }
    val cacheDir = File(context.cacheDir, BACKUP_CACHE_FILE_NAME)
    cacheDir.deleteAllFiles()
    cacheDir.mkdirs()
    val backupCacheFile = File(cacheDir, fileName)
    backupCacheFile.createNewFile()

    withCredentials {
        val bytes = get(url) ?: return@withCredentials ""
        backupCacheFile.writeBytes(bytes)
        backupCacheFile.path
    }
}
```
改为（移除 `createNewFile`/`writeBytes`；改流式 get + 哨兵 + isWithinDir）：
```kotlin
private suspend fun getWebFile(url: String): String = withContext(ioCoroutineContext) {
    val fileName = url.split("/").last()
    if (!fileName.startsWith(BACKUP_FILE_NAME) || !fileName.endsWith(BACKUP_FILE_EXT)) {
        return@withContext ""
    }
    val cacheDir = File(context.cacheDir, BACKUP_CACHE_FILE_NAME)
    cacheDir.deleteAllFiles()
    cacheDir.mkdirs()
    val backupCacheFile = File(cacheDir, fileName)

    withCredentials {
        if (!get(url, backupCacheFile, MAX_RECOVERY_TOTAL_BYTES)) {
            return@withCredentials ""                                 // 下载失败 → "" 哨兵
        }
        require(isWithinDir(backupCacheFile, cacheDir))               // 与 #2 对称的 canonical 守卫
        backupCacheFile.path
    }
}
```

- [ ] **Step 11: 编译（跨模块 Hilt 全图）+ 全量单测 + spotless**

Run: `./gradlew :core:network:testDebugUnitTest :core:data:testDebugUnitTest :app:compileOnlineDebugKotlin`
Expected: BUILD SUCCESSFUL；`core:network` 全测 PASS；`core:data` 全测（含 Task 1 的 BackupStreamCopyTest）PASS
Run: `./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache`

- [ ] **Step 12: Commit**

```bash
git add core/network/src/main/kotlin/cn/wj/android/cashbook/core/network/util/WebDAVHandler.kt \
        core/network/src/main/kotlin/cn/wj/android/cashbook/core/network/util/OkHttpWebDAVHandler.kt \
        core/network/src/main/kotlin/cn/wj/android/cashbook/core/network/util/OfflineWebDAVHandler.kt \
        core/network/src/test/kotlin/cn/wj/android/cashbook/core/network/util/OkHttpWebDAVHandlerTest.kt \
        core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/uitl/impl/BackupRecoveryManagerImpl.kt
git commit -m "[perf|core:network|WebDAV下载流式][公共]get 改流式下载到 dest（嵌套 use+size cap+isSuccessful gate+失败清理），getWebFile 保 '' 哨兵+isWithinDir"
```

---

## 完成后验证（三笔提交后）

- [ ] **完整链路验证**：`./gradlew :core:network:testDebugUnitTest :core:data:testDebugUnitTest :app:compileOnlineDebugKotlin spotlessCheck :core:network:lintRelease :core:data:lintRelease`
- [ ] **节点2 full-review**（`comprehensive-review:full-review`）对本次 3 笔 diff 做最终审查（改动 < ~50 行/笔但跨模块 + 接口变更 + 安全面，跑满或至少两维快审）。
- [ ] **真机 journey（有设备时）**：备份创建（含图片）→ WebDAV 上传 → 从 WebDAV 下载恢复往返；下载失败（断网/404）干净收敛到 Failed 不崩溃。无设备则记 backlog。

## Self-Review（plan 作者已过一遍）

- **Spec 覆盖**：Commit 1/2/3 ↔ Task 1/2/3 一一对应；finding 2（嵌套 use）→ Task1 Step3 + Task3 Step8；finding 3（哨兵/失败清理/isSuccessful）→ Task3 Step8/Step10；finding 4（size cap）→ Task3 copyToCapped；finding 5（测试盲区）→ Task2 header 断言 + Task3 cap 用例；finding 7（isWithinDir/导出失败删）→ Task3 Step10 + Task1 Step5。
- **Placeholder 扫描**：无 TBD/TODO；每个代码步骤给完整代码。
- **类型一致性**：`get(url, dest, maxBytes): Boolean` 在接口/OkHttp/Offline/getWebFile/测试五处签名一致；`copyToCapped(out, maxBytes): Long` 定义与调用一致；`writeFileToStream(src, out)` 定义（Task1）与测试 import 路径 `cn.wj.android.cashbook.core.data.uitl.impl.writeFileToStream` 一致。
- **偏离 spec 的细化**：spec Commit 3 写 `get(url, dest): Boolean` + OkHttp 内 `MAX_RECOVERY_TOTAL_BYTES`；本 plan 细化为 `get(url, dest, maxBytes): Boolean` 参数化，由 core:data 调用方传 `MAX_RECOVERY_TOTAL_BYTES`——因 core:network 不依赖 core:data，不能引用该常量（Global Constraints 已列）。行为等价。
