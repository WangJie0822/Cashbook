# 图片存储迁文件系统 + 备份恢复增强 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把记录图片从 DB BLOB 迁到应用私有文件系统（双轨读 + app 层 backfill，本期不删列不动 schema），并把图片与设备无关设置纳入备份 zip、加固恢复链路。

**Architecture:** 新增 `RecordImageFileStorage`（core/data）作为图片文件落盘/解析/删除的唯一入口；新图保存时写文件 + path 存相对值 + bytes 置空；显示按「文件优先、bytes 回退」双轨；存量靠新增 TempKeys 标志 `imagesToFilesMigrated` 触发的 app 层逐行幂等 backfill 迁移；备份 zip 升级为 `cashbook.db`(VACUUM 副本) + `record_images/` + `settings.json`(白名单) + `manifest.json`(版本戳)，恢复侧重写为 entry 白名单 + mkdirs + 保留 Zip Slip 防护 + 合并语义。

**Tech Stack:** Kotlin, Room, Proto DataStore, Hilt, Coil, org.json(Android 内置), JUnit4 + Truth, Roborazzi(截图), Room instrumented androidTest。

## Global Constraints

以下为全局约束，每个 Task 隐含包含（值逐字取自 spec 与项目 CLAUDE.md）：

- **本期绝不删 `bytes` 列、不动 Room schema**：`db_image_with_related.path` 仍 `String`、`bytes` 仍 `ByteArray NOT NULL`，仅取值语义变 → 无 migration / 无 schema JSON / 无 DB version bump。`ImageWithRelatedTable` / `ImageModel` 字段不变。
- **`path` 只存相对值**：形如 `record_images/img_<id>.jpg` 或 `record_images/img_<uuid>.jpg`，**禁存绝对路径**；读取时一律 `File(context.filesDir, path)` 实时解析。
- **图片不有损二次压缩**：backfill/备份均按已有 bytes 原样落盘；备份 zip 对图片不再压缩。
- **minSdk 24**：应用私有 `filesDir` 无需权限；VACUUM 用 in-place `execSQL("VACUUM")`，**不用** `VACUUM INTO`（需 SQLite 3.27+/约 API30）。
- **测试替身忠实复刻**：`FakeRecordDao`/`FakeTransactionDao`/`FakeCombineProtoDataSource`/`FakeSettingRepository` 新增方法必须复刻真实 SQL/语义，不得 `emptyList()` 桩或宽松匹配。
- **DAO 接口新增抽象方法须同步对应 Fake**，否则 `:core:data:compileDebugUnitTestKotlin` 失败。
- **新增 TempKeys 标志镜像现有全链**：proto + TempKeysModel + CombineProtoDataSource(读 map + 写方法) + FakeCombineProtoDataSource + 消费点 + 测试。
- **模块测试任务名**：`core/model`(JVM 库) 用 `:core:model:test`；`core/data`/`core/database`/`feature:*`(Android 库) 用 `:<module>:testDebugUnitTest`；instrumented 用 `:core:database:connectedDebugAndroidTest` / `:core:data:connectedDebugAndroidTest`（需真机/模拟器）。
- **源文件须含 Apache 2.0 License Header**（Spotless 检查，模板在 `spotless/`，照抄同目录现有文件头）；ktlint(android) 格式；提交前 `./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache`。
- **KDoc 禁用含 `..` 的方括号表达**（`[from..today]` 触发 ktlint `Closing bracket expected`），区间用「from 到 today」表述。
- **提交信息格式**：`[类型|模块|功能][公共]说明`，原子化、最小范围 stage。

包名根：`cn.wj.android.cashbook`。worktree 绝对路径前缀：`D:/Work/Workspace/Owner/Cashbook/.claude/worktrees/asset-backup-image-improvements/`（下文 `Files:` 用仓库相对路径）。

---

# Phase 1：图片存储基础（双轨读 + 新图写文件，无 schema 变更）

## Task 1: RecordImageFileStorage 文件落盘工具（纯逻辑 + 接口）

**Files:**
- Create: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/uitl/RecordImageFileStorage.kt`
- Create: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/uitl/RecordImageFileStorageTest.kt`
- Modify: 绑定模块（见 Step 6，定位 `@Binds` of `BackupRecoveryManager` 所在 module）

**Interfaces:**
- Produces:
  - `interface RecordImageFileStorage`：`fun newRelativePath(): String`、`fun relativePathForId(id: Long): String`、`fun isManaged(path: String): Boolean`、`fun resolve(relativePath: String): File`、`fun exists(relativePath: String): Boolean`、`fun write(relativePath: String, bytes: ByteArray)`、`fun delete(relativePath: String): Boolean`、`fun baseDir(): File`
  - 顶层 `internal fun`：`recordImageRelativePath(id: Long): String`、`newRecordImageRelativePath(token: String): String`、`isManagedImagePath(path: String): Boolean`、`resolveRecordImage(baseDir: File, relativePath: String): File`、`writeRecordImageAtomic(baseDir: File, relativePath: String, bytes: ByteArray)`、`deleteRecordImageFile(baseDir: File, relativePath: String): Boolean`
  - `const val RECORD_IMAGES_DIR = "record_images"`

- [ ] **Step 1: 写失败测试（纯逻辑函数）**

新建 `RecordImageFileStorageTest.kt`（含 License Header，照抄同目录现有文件头）：

```kotlin
package cn.wj.android.cashbook.core.data.uitl

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RecordImageFileStorageTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun recordImageRelativePath_derivesDeterministicNameFromId() {
        assertThat(recordImageRelativePath(42L)).isEqualTo("record_images/img_42.jpg")
        // 同一 id 始终同名（backfill 崩溃可重入的基石）
        assertThat(recordImageRelativePath(42L)).isEqualTo(recordImageRelativePath(42L))
    }

    @Test
    fun newRecordImageRelativePath_usesToken() {
        assertThat(newRecordImageRelativePath("abc")).isEqualTo("record_images/img_abc.jpg")
    }

    @Test
    fun isManagedImagePath_trueOnlyForManagedPrefix() {
        assertThat(isManagedImagePath("record_images/img_1.jpg")).isTrue()
        assertThat(isManagedImagePath("content://media/external/images/1")).isFalse()
        assertThat(isManagedImagePath("/data/user/0/x/files/record_images/img_1.jpg")).isFalse()
    }

    @Test
    fun writeRecordImageAtomic_createsParentAndWritesBytes() {
        val base = tempFolder.root
        writeRecordImageAtomic(base, "record_images/img_7.jpg", byteArrayOf(1, 2, 3))
        val written = File(base, "record_images/img_7.jpg")
        assertThat(written.exists()).isTrue()
        assertThat(written.readBytes()).isEqualTo(byteArrayOf(1, 2, 3))
    }

    @Test
    fun writeRecordImageAtomic_overwritesSameNameIdempotently() {
        val base = tempFolder.root
        writeRecordImageAtomic(base, "record_images/img_7.jpg", byteArrayOf(1))
        writeRecordImageAtomic(base, "record_images/img_7.jpg", byteArrayOf(9, 9))
        assertThat(File(base, "record_images/img_7.jpg").readBytes()).isEqualTo(byteArrayOf(9, 9))
        // 无残留 .tmp
        assertThat(File(base, "record_images/img_7.jpg.tmp").exists()).isFalse()
    }

    @Test
    fun deleteRecordImageFile_returnsTrueWhenDeletedFalseWhenAbsent() {
        val base = tempFolder.root
        writeRecordImageAtomic(base, "record_images/img_7.jpg", byteArrayOf(1))
        assertThat(deleteRecordImageFile(base, "record_images/img_7.jpg")).isTrue()
        assertThat(deleteRecordImageFile(base, "record_images/img_7.jpg")).isFalse()
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*RecordImageFileStorageTest" --no-configuration-cache`
Expected: FAIL（`recordImageRelativePath` 等未定义，编译错误）

- [ ] **Step 3: 写实现**

新建 `RecordImageFileStorage.kt`（含 License Header）：

```kotlin
package cn.wj.android.cashbook.core.data.uitl

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

/** 记录图片相对路径子目录名（应用私有 filesDir 下） */
const val RECORD_IMAGES_DIR = "record_images"

/** 由图片行 id 确定性派生相对路径（backfill 用，崩溃可重入同名覆盖） */
internal fun recordImageRelativePath(id: Long): String = "$RECORD_IMAGES_DIR/img_$id.jpg"

/** 由唯一 token 派生相对路径（新图未有 id 时用 uuid） */
internal fun newRecordImageRelativePath(token: String): String = "$RECORD_IMAGES_DIR/img_$token.jpg"

/** path 是否为本应用托管的相对图片路径（双轨读判据：true 才尝试文件、false 回退 bytes） */
internal fun isManagedImagePath(path: String): Boolean = path.startsWith("$RECORD_IMAGES_DIR/")

internal fun resolveRecordImage(baseDir: File, relativePath: String): File = File(baseDir, relativePath)

/** 原子写：先写 .tmp 再 rename（同卷原子），避免半文件 */
internal fun writeRecordImageAtomic(baseDir: File, relativePath: String, bytes: ByteArray) {
    val target = File(baseDir, relativePath)
    target.parentFile?.mkdirs()
    val tmp = File(target.parentFile, target.name + ".tmp")
    tmp.writeBytes(bytes)
    if (target.exists()) {
        target.delete()
    }
    if (!tmp.renameTo(target)) {
        // 跨实现极少数 rename 失败兜底：直接复制再删 tmp
        tmp.copyTo(target, overwrite = true)
        tmp.delete()
    }
}

internal fun deleteRecordImageFile(baseDir: File, relativePath: String): Boolean {
    val f = File(baseDir, relativePath)
    return if (f.exists()) f.delete() else false
}

/** 记录图片文件存储入口（接口便于单测注入 fake） */
interface RecordImageFileStorage {
    /** 为新图生成唯一相对路径 */
    fun newRelativePath(): String

    /** 由图片行 id 派生确定性相对路径 */
    fun relativePathForId(id: Long): String

    /** path 是否本应用托管相对图片路径 */
    fun isManaged(path: String): Boolean

    /** 相对路径解析为 filesDir 下的真实 File */
    fun resolve(relativePath: String): File

    /** 相对路径对应文件是否存在 */
    fun exists(relativePath: String): Boolean

    /** 原子写入图片字节 */
    fun write(relativePath: String, bytes: ByteArray)

    /** best-effort 删除图片文件，返回是否实际删除 */
    fun delete(relativePath: String): Boolean

    /** 图片根目录（filesDir/record_images），孤儿扫描用 */
    fun baseDir(): File
}

class RecordImageFileStorageImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : RecordImageFileStorage {

    private val filesDir: File get() = context.filesDir

    override fun newRelativePath(): String = newRecordImageRelativePath(UUID.randomUUID().toString())

    override fun relativePathForId(id: Long): String = recordImageRelativePath(id)

    override fun isManaged(path: String): Boolean = isManagedImagePath(path)

    override fun resolve(relativePath: String): File = resolveRecordImage(filesDir, relativePath)

    override fun exists(relativePath: String): Boolean = resolve(relativePath).exists()

    override fun write(relativePath: String, bytes: ByteArray) =
        writeRecordImageAtomic(filesDir, relativePath, bytes)

    override fun delete(relativePath: String): Boolean = deleteRecordImageFile(filesDir, relativePath)

    override fun baseDir(): File = File(filesDir, RECORD_IMAGES_DIR)
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*RecordImageFileStorageTest" --no-configuration-cache`
Expected: PASS（6 测试）

- [ ] **Step 5: 绑定接口到 Hilt**

定位绑定 `BackupRecoveryManager` 的 `@Binds` 模块（`grep -rn "abstract fun bindBackupRecoveryManager\|@Binds" core/data/src/main/kotlin/.../di/`），在同一 `@Module @InstallIn(SingletonComponent::class)` 内追加：

```kotlin
@Binds
@Singleton
abstract fun bindRecordImageFileStorage(impl: RecordImageFileStorageImpl): RecordImageFileStorage
```

（import `cn.wj.android.cashbook.core.data.uitl.RecordImageFileStorage` / `...RecordImageFileStorageImpl`、`javax.inject.Singleton`）

- [ ] **Step 6: 编译 + spotless + commit**

Run: `./gradlew :core:data:compileDebugKotlin --no-configuration-cache` → 无 `e:` 错误
Run: `./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache`

```bash
git add core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/uitl/RecordImageFileStorage.kt \
        core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/uitl/RecordImageFileStorageTest.kt \
        core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/di/*.kt
git commit -m "[feat|core:data|图片存储][公共]新增 RecordImageFileStorage 文件落盘工具 + 纯逻辑单测 + Hilt 绑定"
```

---

## Task 2: 新图保存写文件（path 相对化 + bytes 置空）

**Files:**
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordRepositoryImpl.kt:82-96`（`updateRecord`）
- Create: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/uitl/FakeRecordImageFileStorage.kt`
- Test: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordRepositoryImplImageTest.kt`

**Interfaces:**
- Consumes: `RecordImageFileStorage`（Task 1）、`ImageModel(id, recordId, path, bytes)`、`RecordRepository.updateRecord(record, tagIdList, needRelated, relatedRecordIdList, relatedImageList)`
- Produces: `FakeRecordImageFileStorage`（test 替身，in-memory `MutableMap<String, ByteArray>`）

- [ ] **Step 1: 写 FakeRecordImageFileStorage（忠实复刻 write/exists/delete/isManaged）**

新建 `FakeRecordImageFileStorage.kt`（License Header）：

```kotlin
package cn.wj.android.cashbook.core.data.uitl

import java.io.File

/** in-memory 忠实复刻：write 记录字节、exists/delete 据此判定、isManaged 复用真实前缀规则 */
class FakeRecordImageFileStorage : RecordImageFileStorage {

    val files = linkedMapOf<String, ByteArray>()
    private var counter = 0L

    override fun newRelativePath(): String = newRecordImageRelativePath("new${counter++}")

    override fun relativePathForId(id: Long): String = recordImageRelativePath(id)

    override fun isManaged(path: String): Boolean = isManagedImagePath(path)

    override fun resolve(relativePath: String): File = File(relativePath)

    override fun exists(relativePath: String): Boolean = files.containsKey(relativePath)

    override fun write(relativePath: String, bytes: ByteArray) {
        files[relativePath] = bytes
    }

    override fun delete(relativePath: String): Boolean = files.remove(relativePath) != null

    override fun baseDir(): File = File(RECORD_IMAGES_DIR)
}
```

- [ ] **Step 2: 写失败测试**

新建 `RecordRepositoryImplImageTest.kt`（License Header）。构造 `RecordRepositoryImpl` 需其全部依赖——**先 Read `RecordRepositoryImpl` 构造函数**确认依赖清单，测试里用项目既有 `FakeRecordDao`/`FakeTransactionDao` 等（参照同目录现有 `*Test` 的构造方式）。核心断言：

```kotlin
@Test
fun updateRecord_withNewImageBytes_writesFileAndStoresRelativePathAndEmptiesBytes() = runTest {
    val storage = FakeRecordImageFileStorage()
    val repository = createRepository(recordImageFileStorage = storage) // helper 见下
    val newImage = ImageModel(id = -1L, recordId = -1L, path = "content://pick/1", bytes = byteArrayOf(5, 6, 7))

    repository.updateRecord(
        record = createRecordModel(),
        tagIdList = emptyList(),
        needRelated = false,
        relatedRecordIdList = emptyList(),
        relatedImageList = listOf(newImage),
    )

    // 文件已写、内容为原 bytes
    assertThat(storage.files.values.single()).isEqualTo(byteArrayOf(5, 6, 7))
    // 落库的图片 path 为托管相对值、bytes 置空（断言 FakeTransactionDao 收到的图片）
    val persisted = /* 从 FakeTransactionDao 捕获的 imageWithRecords 取唯一图片 */
    assertThat(persisted.path).startsWith("record_images/")
    assertThat(persisted.bytes).isEmpty()
}

@Test
fun updateRecord_withAlreadyManagedImage_doesNotRewrite() = runTest {
    val storage = FakeRecordImageFileStorage()
    val repository = createRepository(recordImageFileStorage = storage)
    val existing = ImageModel(id = 9L, recordId = 1L, path = "record_images/img_9.jpg", bytes = byteArrayOf())

    repository.updateRecord(createRecordModel(), emptyList(), false, emptyList(), listOf(existing))

    assertThat(storage.files).isEmpty() // 已托管 + bytes 空 → 不重写
}
```

> 实现 `createRepository`/`createRecordModel` helper：参照 `core/data` test 既有约定（该源集不依赖 `core:testing`，各测试自带 `private fun createXxx`）。`createRepository` 用既有 Fake DAO 装配 `RecordRepositoryImpl`，新增 `recordImageFileStorage` 参数默认 `FakeRecordImageFileStorage()`。捕获落库图片：用已有 `FakeTransactionDao.imageWithRecords`（`FakeTransactionDao.kt:42`）。

- [ ] **Step 3: 跑测试确认失败**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*RecordRepositoryImplImageTest" --no-configuration-cache`
Expected: FAIL（`RecordRepositoryImpl` 构造缺 `recordImageFileStorage` 参数 / 行为未实现）

- [ ] **Step 4: 改 RecordRepositoryImpl.updateRecord**

给 `RecordRepositoryImpl` 构造追加注入 `private val recordImageFileStorage: RecordImageFileStorage`，并改 `updateRecord`（现 82-96 行）为先落盘新图再委托：

```kotlin
override suspend fun updateRecord(
    record: RecordModel,
    tagIdList: List<Long>,
    needRelated: Boolean,
    relatedRecordIdList: List<Long>,
    relatedImageList: List<ImageModel>,
) {
    logger().i("updateRecord(record = <$record>, tagIdList = <$tagIdList>")
    val persistedImages = relatedImageList.map { image ->
        if (image.bytes.isNotEmpty() && !recordImageFileStorage.isManaged(image.path)) {
            // 新图：写文件 + path 相对化 + bytes 置空（NOT NULL 故置空数组非 null）
            val relativePath = recordImageFileStorage.newRelativePath()
            recordImageFileStorage.write(relativePath, image.bytes)
            image.copy(path = relativePath, bytes = ByteArray(0))
        } else {
            image
        }
    }
    transactionDao.updateRecordTransaction(
        // ↓ 保留 82-96 行原有的其余实参，仅把 relatedImageList 改为 persistedImages
        relatedImageList = persistedImages,
    )
}
```

> 注意：保留原 `transactionDao.updateRecordTransaction(...)` 调用的其余实参原样（Read 当前 90-95 行逐字保留），仅替换 `relatedImageList = relatedImageList` → `relatedImageList = persistedImages`。

- [ ] **Step 5: 跑测试确认通过**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*RecordRepositoryImplImageTest" --no-configuration-cache`
Expected: PASS

- [ ] **Step 6: 编译 + spotless + commit**

```bash
git add core/data/src/main/kotlin/.../RecordRepositoryImpl.kt \
        core/data/src/test/kotlin/.../FakeRecordImageFileStorage.kt \
        core/data/src/test/kotlin/.../RecordRepositoryImplImageTest.kt
git commit -m "[feat|core:data|图片存储][公共]新图保存写文件系统+path相对化+bytes置空 + 单测"
```

---

## Task 3: 双轨读显示（文件优先、bytes 回退）

**Files:**
- Modify: `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/dialog/ImagePreviewDialog.kt:82-89`
- 排查并改：`grep -rn "AsyncImage\|rememberAsyncImagePainter" feature/records/src/main` 找全部图片显示点（缩略图、记录详情、编辑页 imageList 预览），逐处套同款双轨。
- Test: 双轨选择逻辑抽纯函数 + 单测：`feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/model/ImageDisplayModelTest.kt`

**Interfaces:**
- Consumes: `ImageViewModel(id, recordId, path: String, bitmap: Bitmap?)`、`RecordImageFileStorage.isManaged`/`resolve`（或纯函数 `isManagedImagePath`）
- Produces: 顶层 `internal fun imageCoilModel(path: String, file: File?, bitmap: Bitmap?): Any?`（文件存在→File；否则→bitmap）

- [ ] **Step 1: 写失败测试（纯选择逻辑）**

新建 `ImageDisplayModelTest.kt`（License Header）。把双轨选择抽为纯函数避免 Compose/Context 依赖：

```kotlin
package cn.wj.android.cashbook.feature.records.model

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

class ImageDisplayModelTest {

    @Test
    fun managedPathWithExistingFile_returnsFile() {
        val file = File("/x/record_images/img_1.jpg")
        val model = imageCoilModel(path = "record_images/img_1.jpg", file = file, fileExists = true, bitmapPresent = true)
        assertThat(model).isEqualTo(ImageDisplaySource.FromFile(file))
    }

    @Test
    fun managedPathWithMissingFile_fallsBackToBitmap() {
        val file = File("/x/record_images/img_1.jpg")
        val model = imageCoilModel(path = "record_images/img_1.jpg", file = file, fileExists = false, bitmapPresent = true)
        assertThat(model).isEqualTo(ImageDisplaySource.FromBitmap)
    }

    @Test
    fun unmanagedPath_fallsBackToBitmap() {
        val file = File("/x/content")
        val model = imageCoilModel(path = "content://media/1", file = file, fileExists = false, bitmapPresent = true)
        assertThat(model).isEqualTo(ImageDisplaySource.FromBitmap)
    }

    @Test
    fun missingFileAndNoBitmap_returnsNone() {
        val file = File("/x/record_images/img_1.jpg")
        val model = imageCoilModel(path = "record_images/img_1.jpg", file = file, fileExists = false, bitmapPresent = false)
        assertThat(model).isEqualTo(ImageDisplaySource.None)
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew :feature:records:testDebugUnitTest --tests "*ImageDisplayModelTest" --no-configuration-cache`
Expected: FAIL（`imageCoilModel`/`ImageDisplaySource` 未定义）

- [ ] **Step 3: 写纯函数实现**

新建 `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/model/ImageDisplaySource.kt`（License Header）：

```kotlin
package cn.wj.android.cashbook.feature.records.model

import cn.wj.android.cashbook.core.data.uitl.isManagedImagePath
import java.io.File

/** 双轨读结果：文件 / bitmap 回退 / 无 */
sealed interface ImageDisplaySource {
    data class FromFile(val file: File) : ImageDisplaySource
    object FromBitmap : ImageDisplaySource
    object None : ImageDisplaySource
}

/**
 * 双轨选择：托管相对 path 且文件存在 → 用文件；否则有 bitmap 回退 bitmap；都无 → None。
 * 纯函数，file 是否存在与 bitmap 是否存在由调用方传入（Composable 侧解析 filesDir/解码 bytes）。
 */
internal fun imageCoilModel(
    path: String,
    file: File,
    fileExists: Boolean,
    bitmapPresent: Boolean,
): ImageDisplaySource = when {
    isManagedImagePath(path) && fileExists -> ImageDisplaySource.FromFile(file)
    bitmapPresent -> ImageDisplaySource.FromBitmap
    else -> ImageDisplaySource.None
}
```

> 若 `core/data` 的 `isManagedImagePath` 为 `internal` 跨模块不可见，则在 `feature/records` 内复制一个 `private const val RECORD_IMAGES_PREFIX = "record_images/"` 判据，或把 `isManagedImagePath` 提升为 `public`（Task 1 一并调整并补注释）。**优先提升为 public**（单一事实源）。

- [ ] **Step 4: 跑测试确认通过**

Run: `./gradlew :feature:records:testDebugUnitTest --tests "*ImageDisplayModelTest" --no-configuration-cache`
Expected: PASS

- [ ] **Step 5: 接到显示 Composable（ImagePreviewDialog）**

改 `ImagePreviewDialog.kt:82-89`，把 `model = list[index].path` 改为双轨解析（`LocalContext` 取 filesDir，`remember` 缓存 File 判定）：

```kotlin
val context = LocalContext.current
val item = list[index]
val source = remember(item.path, item.bitmap) {
    val file = File(context.filesDir, item.path)
    imageCoilModel(
        path = item.path,
        file = file,
        fileExists = file.exists(),
        bitmapPresent = item.bitmap != null,
    )
}
val placeholder = rememberAsyncImagePainter(item.bitmap)
AsyncImage(
    model = when (source) {
        is ImageDisplaySource.FromFile -> source.file
        else -> item.bitmap
    },
    placeholder = placeholder,
    error = placeholder,
    contentScale = ContentScale.Crop,
    contentDescription = null,
    modifier = Modifier.fillMaxSize(),
)
```

（import `androidx.compose.ui.platform.LocalContext`、`androidx.compose.runtime.remember`、`java.io.File`、`...model.ImageDisplaySource`、`...model.imageCoilModel`。对 grep 出的其它图片显示点套同款。）

- [ ] **Step 6: 编译 + spotless + commit**

Run: `./gradlew :feature:records:compileDebugKotlin --no-configuration-cache`
Run: `./gradlew spotlessApply --init-script gradle/init.gradle.kts --no-configuration-cache`

```bash
git add feature/records/src/main/kotlin/.../model/ImageDisplaySource.kt \
        feature/records/src/main/kotlin/.../dialog/ImagePreviewDialog.kt \
        feature/records/src/test/kotlin/.../model/ImageDisplayModelTest.kt \
        core/data/src/main/kotlin/.../uitl/RecordImageFileStorage.kt
git commit -m "[feat|feature:records|图片存储][公共]图片显示双轨读(文件优先bytes回退)+纯函数选择逻辑单测"
```

---

# Phase 2：存量 backfill + 孤儿清理

## Task 4: TempKeys `imagesToFilesMigrated` 标志全链

**Files:**
- Modify: `core/datastore-proto/src/main/proto/temp_keys.proto:8-11`
- Modify: `core/model/src/main/kotlin/cn/wj/android/cashbook/core/model/model/TempKeysModel.kt:24-31`
- Modify: `core/datastore/src/main/kotlin/cn/wj/android/cashbook/core/datastore/datasource/CombineProtoDataSource.kt:134-141,194-200`
- Modify: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/testdoubles/FakeCombineProtoDataSource.kt:99-104,274-280`
- Test: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/repository/impl/SettingRepositoryImplTest.kt`（新增默认值 + 写入用例）

**Interfaces:**
- Produces: `TempKeysModel.imagesToFilesMigrated: Boolean = false`、`CombineProtoDataSource.updateImagesToFilesMigrated(migrated: Boolean)`、`FakeCombineProtoDataSource.updateImagesToFilesMigrated(...)`

- [ ] **Step 1: 写失败测试（SettingRepositoryImplTest 默认值 + 写入）**

在 `SettingRepositoryImplTest.kt` 新增：

```kotlin
@Test
fun when_read_tempKeys_then_imagesToFilesMigrated_default_false() = runTest {
    val model = repository.tempKeysModel.first()
    assertThat(model.imagesToFilesMigrated).isFalse()
}

@Test
fun when_updateImagesToFilesMigrated_then_flag_persisted() = runTest {
    fakeDataSource.updateImagesToFilesMigrated(true)
    assertThat(repository.tempKeysModel.first().imagesToFilesMigrated).isTrue()
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*SettingRepositoryImplTest" --no-configuration-cache`
Expected: FAIL（`imagesToFilesMigrated` 未定义）

- [ ] **Step 3: 改 proto**

`temp_keys.proto` 在 `message TempKeys` 内追加（字段号 4，紧接 finalAmountNetRecalcDone=3）：

```protobuf
message TempKeys {
  bool db9To10dataMigrated = 1; // 数据库版本9升级到10之后是否进行数据迁移
  bool preferenceSplit = 2; // app_preferences 是否已拆分
  bool finalAmountNetRecalcDone = 3; // finalAmount 净自付重算是否已执行（proto3 默认 false，首启触发一次）
  bool imagesToFilesMigrated = 4; // 图片 BLOB→文件系统 backfill 是否已完成（proto3 默认 false，首启触发一次）
}
```

- [ ] **Step 4: 改 TempKeysModel**

```kotlin
data class TempKeysModel(
    /** 数据库数据是否执行版本9到版本10迁移 */
    val db9To10DataMigrated: Boolean,
    /** proto app_preferences 是否拆分 */
    val preferenceSplit: Boolean,
    /** finalAmount 净自付重算是否已执行 */
    val finalAmountNetRecalcDone: Boolean = false,
    /** 图片 BLOB→文件系统 backfill 是否已完成 */
    val imagesToFilesMigrated: Boolean = false,
)
```

- [ ] **Step 5: 改 CombineProtoDataSource（读 map + 写方法）**

读流（134-141）map 内追加：
```kotlin
            imagesToFilesMigrated = it.imagesToFilesMigrated,
```
写方法区（194-200 附近）追加：
```kotlin
    suspend fun updateImagesToFilesMigrated(migrated: Boolean) {
        tempKeys.updateData { it.copy { this.imagesToFilesMigrated = migrated } }
    }
```

- [ ] **Step 6: 改 FakeCombineProtoDataSource（忠实复刻）**

初始化 `_tempKeys`（99-104）补字段（也可省略走默认值，但显式更清晰）；更新方法区（274-280）追加：
```kotlin
    suspend fun updateImagesToFilesMigrated(migrated: Boolean) {
        _tempKeys.update { it.copy(imagesToFilesMigrated = migrated) }
    }
```

- [ ] **Step 7: 跑测试确认通过 + 编译**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*SettingRepositoryImplTest" --no-configuration-cache`
Expected: PASS
Run: `./gradlew :core:datastore:compileDebugKotlin :core:model:test --no-configuration-cache`

- [ ] **Step 8: spotless + commit**

```bash
git add core/datastore-proto/src/main/proto/temp_keys.proto \
        core/model/src/main/kotlin/.../TempKeysModel.kt \
        core/datastore/src/main/kotlin/.../CombineProtoDataSource.kt \
        core/data/src/test/kotlin/.../FakeCombineProtoDataSource.kt \
        core/data/src/test/kotlin/.../SettingRepositoryImplTest.kt
git commit -m "[feat|core:datastore|图片存储][公共]TempKeys 新增 imagesToFilesMigrated 标志全链 + 单测"
```

---

## Task 5: 全表图片查询 DAO + backfill 仓库方法（逐行幂等）

**Files:**
- Modify: `core/database/src/main/kotlin/cn/wj/android/cashbook/core/database/dao/RecordDao.kt`（新增 2 抽象方法）
- Modify: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/testdoubles/FakeRecordDao.kt`（同步实现）
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/RecordRepository.kt`（接口加 `backfillImagesToFiles`）
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordRepositoryImpl.kt`（实现）
- Modify: `core/testing/src/main/kotlin/cn/wj/android/cashbook/core/testing/repository/FakeRecordRepository.kt`（接口实现，no-op 计数）
- Test: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordBackfillTest.kt`

**Interfaces:**
- Consumes: `RecordImageFileStorage`（Task 1）、`ImageWithRelatedTable(id, recordId, path, bytes)`
- Produces:
  - `RecordDao.queryAllImages(): List<ImageWithRelatedTable>`
  - `RecordDao.updateImagePathAndBytes(id: Long, path: String, bytes: ByteArray)`
  - `RecordRepository.backfillImagesToFiles()`

- [ ] **Step 1: 写失败测试（backfill 逐行幂等）**

新建 `RecordBackfillTest.kt`（License Header）：

```kotlin
@Test
fun backfill_movesBlobToFile_setsRelativePath_emptiesBytes() = runTest {
    val storage = FakeRecordImageFileStorage()
    val dao = FakeRecordDao().apply {
        images.add(ImageWithRelatedTable(id = 3L, recordId = 1L, path = "content://old/3", bytes = byteArrayOf(1, 2)))
    }
    val repository = createRepository(recordDao = dao, recordImageFileStorage = storage)

    repository.backfillImagesToFiles()

    // 文件按 id 确定性命名写入
    assertThat(storage.files["record_images/img_3.jpg"]).isEqualTo(byteArrayOf(1, 2))
    // 行已更新：path 相对化、bytes 置空
    val row = dao.queryAllImages().single()
    assertThat(row.path).isEqualTo("record_images/img_3.jpg")
    assertThat(row.bytes).isEmpty()
}

@Test
fun backfill_isIdempotent_skipsAlreadyMigratedRows() = runTest {
    val storage = FakeRecordImageFileStorage()
    val dao = FakeRecordDao().apply {
        images.add(ImageWithRelatedTable(id = 3L, recordId = 1L, path = "record_images/img_3.jpg", bytes = byteArrayOf()))
    }
    val repository = createRepository(recordDao = dao, recordImageFileStorage = storage)

    repository.backfillImagesToFiles() // 再跑一次

    assertThat(storage.files).isEmpty() // 已迁移行（bytes 空）跳过、不重写
    assertThat(dao.queryAllImages().single().path).isEqualTo("record_images/img_3.jpg")
}

@Test
fun backfill_reentrant_afterFileWrittenButRowNotCommitted_overwritesSameFile() = runTest {
    // 模拟上次崩溃：文件已写、行仍是旧 path+非空 bytes → 重跑按同名覆盖、不产孤儿
    val storage = FakeRecordImageFileStorage().apply { files["record_images/img_3.jpg"] = byteArrayOf(9) }
    val dao = FakeRecordDao().apply {
        images.add(ImageWithRelatedTable(id = 3L, recordId = 1L, path = "content://old/3", bytes = byteArrayOf(1, 2)))
    }
    val repository = createRepository(recordDao = dao, recordImageFileStorage = storage)

    repository.backfillImagesToFiles()

    assertThat(storage.files.keys).containsExactly("record_images/img_3.jpg") // 无新孤儿
    assertThat(storage.files["record_images/img_3.jpg"]).isEqualTo(byteArrayOf(1, 2)) // 同名覆盖为权威 bytes
    assertThat(dao.queryAllImages().single().bytes).isEmpty()
}

@Test
fun backfill_skipsRowsWithNullId() = runTest {
    val storage = FakeRecordImageFileStorage()
    val dao = FakeRecordDao().apply {
        images.add(ImageWithRelatedTable(id = null, recordId = 1L, path = "content://x", bytes = byteArrayOf(1)))
    }
    val repository = createRepository(recordDao = dao, recordImageFileStorage = storage)

    repository.backfillImagesToFiles()

    assertThat(storage.files).isEmpty() // 无 id 无法派生确定文件名，跳过（不应存在，安全网）
}
```

> `FakeRecordDao` 须暴露可变 `images` 列表并复刻 `queryAllImages`/`updateImagePathAndBytes`/已有 `queryImagesByRecordId(s)` 语义。`createRepository` 增加 `recordDao`/`recordImageFileStorage` 形参。

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*RecordBackfillTest" --no-configuration-cache`
Expected: FAIL

- [ ] **Step 3: 加 RecordDao 抽象方法**

`RecordDao.kt` 图片查询区（460-472 附近）追加：
```kotlin
    /** 全表图片（backfill / 孤儿扫描引用集用） */
    @Query("SELECT * FROM db_image_with_related")
    suspend fun queryAllImages(): List<ImageWithRelatedTable>

    /** backfill 逐行更新 path + 置空 bytes（不动其它列） */
    @Query("UPDATE db_image_with_related SET image_path = :path, image_bytes = :bytes WHERE id = :id")
    suspend fun updateImagePathAndBytes(id: Long, path: String, bytes: ByteArray)
```

- [ ] **Step 4: 同步 FakeRecordDao**

`FakeRecordDao.kt` 加（若无 `images` 列表则新增 `val images = mutableListOf<ImageWithRelatedTable>()`，并让已有 `queryImagesByRecordId(s)` 基于它过滤——忠实复刻 SQL）：
```kotlin
    override suspend fun queryAllImages(): List<ImageWithRelatedTable> = images.toList()

    override suspend fun updateImagePathAndBytes(id: Long, path: String, bytes: ByteArray) {
        val idx = images.indexOfFirst { it.id == id }
        if (idx >= 0) images[idx] = images[idx].copy(path = path, bytes = bytes)
    }
```

- [ ] **Step 5: 加 RecordRepository 接口方法 + 实现**

接口 `RecordRepository.kt`：
```kotlin
    /** 存量图片 BLOB → 文件系统 backfill（逐行幂等，崩溃可重入） */
    suspend fun backfillImagesToFiles()
```
实现 `RecordRepositoryImpl.kt`：
```kotlin
    override suspend fun backfillImagesToFiles() {
        recordDao.queryAllImages().forEach { image ->
            val id = image.id ?: return@forEach // 无 id 无法派生确定文件名，跳过
            // 仅处理 bytes 非空且尚未托管的行（已迁移行 bytes 空 → 跳过，幂等）
            if (image.bytes.isEmpty() && recordImageFileStorage.isManaged(image.path)) return@forEach
            if (image.bytes.isEmpty()) return@forEach // bytes 空但 path 非托管：无可迁移数据，跳过
            val relativePath = recordImageFileStorage.relativePathForId(id)
            recordImageFileStorage.write(relativePath, image.bytes) // 同名覆盖、可重入
            // 逐行提交：先文件后 DB；此行提交后该行 bytes 才释放
            recordDao.updateImagePathAndBytes(id, relativePath, ByteArray(0))
        }
    }
```

- [ ] **Step 6: FakeRecordRepository 实现接口**

`core/testing` 的 `FakeRecordRepository.kt` 加（计数便于 Task 6 测）：
```kotlin
    var backfillImagesToFilesCount = 0
        private set

    override suspend fun backfillImagesToFiles() {
        backfillImagesToFilesCount++
    }
```

- [ ] **Step 7: 跑测试确认通过 + 编译全相关模块**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*RecordBackfillTest" --no-configuration-cache`
Expected: PASS
Run: `./gradlew :core:database:compileDebugKotlin :core:data:compileDebugUnitTestKotlin --no-configuration-cache`

- [ ] **Step 8: spotless + commit**

```bash
git add core/database/src/main/kotlin/.../RecordDao.kt \
        core/data/src/test/kotlin/.../FakeRecordDao.kt \
        core/data/src/main/kotlin/.../RecordRepository.kt \
        core/data/src/main/kotlin/.../RecordRepositoryImpl.kt \
        core/testing/src/main/kotlin/.../FakeRecordRepository.kt \
        core/data/src/test/kotlin/.../RecordBackfillTest.kt
git commit -m "[feat|core:data|图片存储][公共]全表图片查询DAO+逐行幂等backfill仓库方法+忠实Fake+单测"
```

---

## Task 6: backfill 接入首屏 gate（非阻塞）

**Files:**
- Modify: `feature/records/src/main/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/LauncherContentViewModel.kt:72-97`
- Test: `feature/records/src/test/kotlin/cn/wj/android/cashbook/feature/records/viewmodel/LauncherContentViewModelTest.kt`

**Interfaces:**
- Consumes: `settingRepository.tempKeysModel.imagesToFilesMigrated`、`recordRepository.backfillImagesToFiles()`、`combineProtoDataSource.updateImagesToFilesMigrated` —— 但注意：置位标志应走 **Repository/DataSource**。检查 `LauncherContentViewModel` 现有依赖（构造仅 `booksRepository/settingRepository/recordRepository/recordModelTransToViewsUseCase`），置位须有写入口。

> **关键设计抉择**：`finalAmountNetRecalcDone` 的置位封装在 `recordRepository.recalculateAllFinalAmount()` 内部（F2 统一副作用）。同理，**把 `imagesToFilesMigrated` 置位封装进 `recordRepository.backfillImagesToFiles()` 内部**（迁移成功后置位），ViewModel 只读标志决定是否调用——避免给 ViewModel 注入 datastore。故 Task 5 的 `backfillImagesToFiles` 实现末尾需追加置位：`settingRepository.updateImagesToFilesMigrated(true)` 或经 combineProtoDataSource。**修订 Task 5 Step 5**：在 backfill 成功跑完后置位。需给 `RecordRepositoryImpl` 注入 `settingRepository` 或 `combineProtoDataSource`（Read 现有依赖；`RecordRepositoryImpl` 若已有 `combineProtoDataSource` 则直接用其 `updateImagesToFilesMigrated`）。同步在 `SettingRepository` 暴露 `updateImagesToFilesMigrated` 或复用 datasource。

- [ ] **Step 1: 写失败测试**

`LauncherContentViewModelTest.kt` 新增（参照 83-110 既有 gate 测式样）：

```kotlin
@Test
fun when_imagesToFiles_not_migrated_then_backfill_called() {
    settingRepository.setTempKeys(
        TempKeysModel(
            db9To10DataMigrated = true,
            preferenceSplit = true,
            finalAmountNetRecalcDone = true,
            imagesToFilesMigrated = false,
        ),
    )
    val freshRecordRepository = FakeRecordRepository()
    val useCase = RecordModelTransToViewsUseCase(
        recordRepository = freshRecordRepository,
        typeRepository = FakeTypeRepository(),
        assetRepository = FakeAssetRepository(),
        tagRepository = FakeTagRepository(),
        coroutineContext = dispatcherRule.testDispatcher,
    )
    LauncherContentViewModel(
        booksRepository = booksRepository,
        settingRepository = settingRepository,
        recordRepository = freshRecordRepository,
        recordModelTransToViewsUseCase = useCase,
    )
    assertThat(freshRecordRepository.backfillImagesToFilesCount).isEqualTo(1)
}

@Test
fun when_imagesToFiles_already_migrated_then_backfill_not_called() {
    settingRepository.setTempKeys(
        TempKeysModel(
            db9To10DataMigrated = true,
            preferenceSplit = true,
            finalAmountNetRecalcDone = true,
            imagesToFilesMigrated = true,
        ),
    )
    val freshRecordRepository = FakeRecordRepository()
    val useCase = RecordModelTransToViewsUseCase(
        recordRepository = freshRecordRepository,
        typeRepository = FakeTypeRepository(),
        assetRepository = FakeAssetRepository(),
        tagRepository = FakeTagRepository(),
        coroutineContext = dispatcherRule.testDispatcher,
    )
    LauncherContentViewModel(
        booksRepository = booksRepository,
        settingRepository = settingRepository,
        recordRepository = freshRecordRepository,
        recordModelTransToViewsUseCase = useCase,
    )
    assertThat(freshRecordRepository.backfillImagesToFilesCount).isEqualTo(0)
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew :feature:records:testDebugUnitTest --tests "*LauncherContentViewModelTest" --no-configuration-cache`
Expected: FAIL

- [ ] **Step 3: 改 LauncherContentViewModel init（非阻塞，try/catch 幂等重试）**

在 init 块 `else` 分支内、`finalAmountNetRecalcDone` 处理之后追加（与净自付同款非阻塞 + try/catch）：

```kotlin
            if (!tempKeys.imagesToFilesMigrated) {
                // 图片 BLOB→文件 backfill 后台静默跑；逐行幂等、崩溃可重入，
                // backfillImagesToFiles 内部成功后置位 imagesToFilesMigrated。
                // try/catch：失败不连累已放行首屏，标志未置位则下次启动幂等重试。
                try {
                    recordRepository.backfillImagesToFiles()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    this@LauncherContentViewModel.logger()
                        .e(e, "background image backfill failed, will retry next launch")
                }
            }
```

> 注意：backfill 是非阻塞（首屏不等它）。它在 `else`（db9To10 已迁移）分支；对全新安装/刚 9→10 迁移用户，首启时本就无存量 content-uri 图片，backfill 遍历到的行很少或为空，开销可忽略。置位封装在 `backfillImagesToFiles` 内（见 Task 5 修订）。

- [ ] **Step 4: 跑测试确认通过**

Run: `./gradlew :feature:records:testDebugUnitTest --tests "*LauncherContentViewModelTest" --no-configuration-cache`
Expected: PASS（含既有 3 用例不回归）

- [ ] **Step 5: spotless + commit**

```bash
git add feature/records/src/main/kotlin/.../LauncherContentViewModel.kt \
        feature/records/src/test/kotlin/.../LauncherContentViewModelTest.kt
git commit -m "[feat|feature:records|图片存储][公共]首屏gate接入图片backfill(非阻塞+幂等重试)+单测"
```

---

## Task 7: 删图后 best-effort 删文件（删点清理）

**Files:**
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordRepositoryImpl.kt`（在删记录/替换图片的仓库路径，DB 删成功后删文件）
- Test: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordImageOrphanTest.kt`

**Interfaces:**
- Consumes: `RecordImageFileStorage.delete`、`RecordDao.queryImagesByRecordId(s)`（取被删行的 path 集）

> **设计**：DB 删除发生在 `TransactionDao`（core/database，无 Context/文件能力）。故文件删除在 **core/data 仓库层**：删记录前先查这些记录的图片 path（托管的），DB 删成功后 best-effort 删文件（失败留孤儿待 Task 8 扫描）。涉及仓库方法：删记录（单/批）、编辑替换图片。Read `RecordRepositoryImpl` 找到 `deleteRecord`/批量删的实现，按下式插入「删前捕获 path → DB 删 → 删文件」。

- [ ] **Step 1: 写失败测试**

新建 `RecordImageOrphanTest.kt`：

```kotlin
@Test
fun deleteRecord_alsoDeletesManagedImageFiles() = runTest {
    val storage = FakeRecordImageFileStorage().apply {
        files["record_images/img_3.jpg"] = byteArrayOf(1)
    }
    val dao = FakeRecordDao().apply {
        images.add(ImageWithRelatedTable(id = 3L, recordId = 50L, path = "record_images/img_3.jpg", bytes = byteArrayOf()))
        // + 该 record 的其它表数据（参照既有删记录测试构造）
    }
    val repository = createRepository(recordDao = dao, recordImageFileStorage = storage, /* transactionDao 等 */)

    repository.deleteRecord(/* recordId = 50L 对应的 RecordModel/Id，参照既有签名 */)

    assertThat(storage.files).doesNotContainKey("record_images/img_3.jpg")
}

@Test
fun deleteRecord_unmanagedImagePath_doesNotTouchFiles() = runTest {
    val storage = FakeRecordImageFileStorage().apply { files["record_images/keep.jpg"] = byteArrayOf(1) }
    val dao = FakeRecordDao().apply {
        images.add(ImageWithRelatedTable(id = 4L, recordId = 51L, path = "content://old/4", bytes = byteArrayOf(2)))
    }
    val repository = createRepository(recordDao = dao, recordImageFileStorage = storage)

    repository.deleteRecord(/* recordId = 51L */)

    assertThat(storage.files).containsKey("record_images/keep.jpg") // 非托管 path 不删任何文件
}
```

> 删记录的确切方法签名以 `RecordRepositoryImpl` 现有为准（Read 确认；可能是 `deleteRecord(id)` 或 `deleteRecord(RecordModel)`）。

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*RecordImageOrphanTest" --no-configuration-cache`
Expected: FAIL

- [ ] **Step 3: 改 RecordRepositoryImpl 删记录路径**

在删记录的仓库方法内（DB 删除委托前后）插入：

```kotlin
    // 删前捕获托管图片相对路径（删后关联已清无法查）
    val managedImagePaths = recordDao.queryImagesByRecordId(recordId)
        .map { it.path }
        .filter { recordImageFileStorage.isManaged(it) }
    // ... 原有 DB 删除委托（transactionDao.deleteRecordTransaction / deleteRecordsBatch）...
    // DB 删成功后 best-effort 删文件（失败留孤儿，Task 8 扫描兜底）
    managedImagePaths.forEach { recordImageFileStorage.delete(it) }
```

> 编辑替换图片（`deleteOldRelatedImages` 路径）若经仓库层，则同款：替换前查旧 path、DB 删后删文件。若替换完全在 DAO `@Transaction` 内不经仓库，则该路径的文件孤儿由 Task 8 启动扫描兜底（在测试计划注明）。

- [ ] **Step 4: 跑测试确认通过**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*RecordImageOrphanTest" --no-configuration-cache`
Expected: PASS

- [ ] **Step 5: spotless + commit**

```bash
git add core/data/src/main/kotlin/.../RecordRepositoryImpl.kt \
        core/data/src/test/kotlin/.../RecordImageOrphanTest.kt
git commit -m "[feat|core:data|图片存储][公共]删记录后best-effort删托管图片文件(删点清理)+单测"
```

---

## Task 8: 启动孤儿扫描（限定目录 + grace window + 标志后）

**Files:**
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/RecordRepository.kt`（加 `cleanupOrphanImageFiles(graceWindowMs: Long)`）
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/impl/RecordRepositoryImpl.kt`（实现）
- Modify: `core/testing/.../FakeRecordRepository.kt`
- Modify: `feature/records/.../LauncherContentViewModel.kt`（backfill 置位后调用，非阻塞）
- Test: `core/data/src/test/kotlin/.../RecordOrphanScanTest.kt`

**Interfaces:**
- Produces: `RecordRepository.cleanupOrphanImageFiles(graceWindowMs: Long = 60_000L)` —— 扫 `baseDir()` 直接子文件，文件名不在 DB 引用集、且 lastModified 早于 `now - graceWindowMs` 才删。

- [ ] **Step 1: 写失败测试（用真实临时目录注入 storage）**

> 孤儿扫描依赖真实文件目录枚举（`baseDir().listFiles()`），`FakeRecordImageFileStorage`（in-memory）不便测枚举。本 Task 用一个**基于 TemporaryFolder 的真实 storage**：直接 `RecordImageFileStorageImpl` 不可（需 Context）。故抽纯函数 `internal fun computeOrphanFiles(referencedNames: Set<String>, files: List<File>, nowMs: Long, graceWindowMs: Long): List<File>` 并单测它（纯逻辑），仓库方法薄委托。

新建 `RecordOrphanScanTest.kt`：

```kotlin
@Test
fun computeOrphanFiles_returnsUnreferencedAndOldEnough() {
    val referenced = setOf("img_1.jpg")
    val f1 = fakeFile("img_1.jpg", lastModified = 0L)      // 被引用 → 保留
    val f2 = fakeFile("img_2.jpg", lastModified = 0L)      // 未引用 + 旧 → 孤儿
    val f3 = fakeFile("img_3.jpg", lastModified = 99_000L) // 未引用但在 grace window 内 → 保留
    val orphans = computeOrphanFiles(referenced, listOf(f1, f2, f3), nowMs = 100_000L, graceWindowMs = 60_000L)
    assertThat(orphans.map { it.name }).containsExactly("img_2.jpg")
}

@Test
fun computeOrphanFiles_skipsDirectoriesAndNonFiles() {
    val dir = fakeDir("sub")
    val orphans = computeOrphanFiles(emptySet(), listOf(dir), nowMs = 100_000L, graceWindowMs = 0L)
    assertThat(orphans).isEmpty()
}
```

> `fakeFile`/`fakeDir`：用 `mockk`/匿名 `File` 子类或 TemporaryFolder 建真实文件并 `setLastModified`。优先 TemporaryFolder 建真实文件（确定性、无 mock 依赖）。

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*RecordOrphanScanTest" --no-configuration-cache`
Expected: FAIL

- [ ] **Step 3: 写纯函数 + 仓库方法**

`RecordImageFileStorage.kt` 加顶层纯函数：
```kotlin
/** 计算孤儿文件：不在引用集、是普通文件、lastModified 早于 now-grace 才算（保护新写文件） */
internal fun computeOrphanFiles(
    referencedNames: Set<String>,
    files: List<File>,
    nowMs: Long,
    graceWindowMs: Long,
): List<File> = files.filter { f ->
    f.isFile && f.name !in referencedNames && f.lastModified() < nowMs - graceWindowMs
}
```
`RecordRepositoryImpl.cleanupOrphanImageFiles`：
```kotlin
    override suspend fun cleanupOrphanImageFiles(graceWindowMs: Long) {
        val referenced = recordDao.queryAllImages()
            .map { it.path }
            .filter { recordImageFileStorage.isManaged(it) }
            .map { it.substringAfterLast('/') } // 仅文件名
            .toSet()
        val dir = recordImageFileStorage.baseDir()
        val children = dir.listFiles()?.toList() ?: return // 目录不存在/空 → 无孤儿
        val now = System.currentTimeMillis()
        computeOrphanFiles(referenced, children, now, graceWindowMs).forEach { it.delete() }
    }
```
接口与 FakeRecordRepository 加对应方法（Fake 计数 no-op）。

- [ ] **Step 4: 接入 LauncherContentViewModel（backfill 后、非阻塞）**

在 backfill try 块成功后追加（限定 `imagesToFilesMigrated` 置位后才扫，避免误删未 backfill 的——实际上扫描只删「不在 DB 引用集」的文件，未 backfill 行 path 非托管不在引用集，但其文件也不在 record_images/，故安全；grace window 再加保护）：
```kotlin
                try {
                    recordRepository.cleanupOrphanImageFiles(60_000L)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    this@LauncherContentViewModel.logger().e(e, "orphan image cleanup failed")
                }
```

- [ ] **Step 5: 跑测试确认通过 + commit**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*RecordOrphanScanTest" --no-configuration-cache`
Expected: PASS
```bash
git add core/data/src/main/kotlin/.../RecordImageFileStorage.kt \
        core/data/src/main/kotlin/.../RecordRepository.kt \
        core/data/src/main/kotlin/.../RecordRepositoryImpl.kt \
        core/testing/src/main/kotlin/.../FakeRecordRepository.kt \
        feature/records/src/main/kotlin/.../LauncherContentViewModel.kt \
        core/data/src/test/kotlin/.../RecordOrphanScanTest.kt
git commit -m "[feat|core:data|图片存储][公共]启动孤儿图片扫描(限定目录+grace window)+纯函数单测+首屏接入"
```

---

# Phase 3：备份格式升级

## Task 9: 备份 zip 纳入图片 + settings + manifest + VACUUM 副本

**Files:**
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/uitl/impl/BackupRecoveryManagerImpl.kt`（`startBackup` 367-514）
- Create: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/uitl/BackupManifest.kt`（manifest 序列化纯逻辑）
- Test: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/uitl/BackupManifestTest.kt`

**Interfaces:**
- Produces:
  - `BackupManifest.kt`：`const val BACKUP_FORMAT_VERSION = 2`、`const val MANIFEST_ENTRY = "manifest.json"`、`const val SETTINGS_ENTRY = "settings.json"`、`const val RECORD_IMAGES_ENTRY_PREFIX = "record_images/"`、`internal fun buildManifestJson(formatVersion: Int, appVersion: String): String`、`internal fun parseManifestFormatVersion(json: String): Int`
- Consumes: `RecordImageFileStorage.baseDir()`、`SettingRepository.exportSettings()`（Task 11，先占位空实现/后置；**Task 排序**：Task 11 可在 Task 9 之前做，或 Task 9 先打包 db+images+manifest，settings 留到 Task 11/12 接入。建议先做 Task 11 再做 Task 9，见自审）。

> **依赖顺序修正**：`settings.json` 内容来自 Task 11 的 `exportSettings()`。为避免占位，**实际执行顺序建议 Task 11 → Task 9**。本计划文档按 Phase 编号，执行时把 Task 11 提到 Task 9 前。

- [ ] **Step 1: 写 manifest 纯逻辑失败测试**

新建 `BackupManifestTest.kt`：
```kotlin
@Test
fun buildManifestJson_containsFormatVersionAndAppVersion() {
    val json = buildManifestJson(formatVersion = 2, appVersion = "v1.2.0")
    assertThat(parseManifestFormatVersion(json)).isEqualTo(2)
    assertThat(json).contains("v1.2.0")
}

@Test
fun parseManifestFormatVersion_missingField_returnsOne() {
    // 旧 db-only 备份无 manifest → 调用方按缺失视为版本 1
    assertThat(parseManifestFormatVersion("{}")).isEqualTo(1)
}

@Test
fun parseManifestFormatVersion_malformed_returnsOne() {
    assertThat(parseManifestFormatVersion("not json")).isEqualTo(1)
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*BackupManifestTest" --no-configuration-cache`
Expected: FAIL

- [ ] **Step 3: 写 BackupManifest.kt**

```kotlin
package cn.wj.android.cashbook.core.data.uitl

import org.json.JSONObject

const val BACKUP_FORMAT_VERSION = 2
const val MANIFEST_ENTRY = "manifest.json"
const val SETTINGS_ENTRY = "settings.json"
const val RECORD_IMAGES_ENTRY_PREFIX = "record_images/"

internal fun buildManifestJson(formatVersion: Int, appVersion: String): String =
    JSONObject()
        .put("formatVersion", formatVersion)
        .put("appVersion", appVersion)
        .toString()

/** 解析 manifest 格式版本；缺失/非法一律视为 1（旧 db-only 备份） */
internal fun parseManifestFormatVersion(json: String): Int =
    try {
        JSONObject(json).optInt("formatVersion", 1)
    } catch (e: org.json.JSONException) {
        1
    }
```

- [ ] **Step 4: 跑通纯逻辑测试**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*BackupManifestTest" --no-configuration-cache`
Expected: PASS

- [ ] **Step 5: 改 startBackup 打包 db(VACUUM) + images + settings + manifest**

把 `startBackup` 的 zip 生成段（407-417）替换为多 entry 打包。在 `databaseFile.copyTo(databaseCacheFile)`（405）之后、原 ZipOutputStream 段之前/替换之：

```kotlin
            // 对备份副本 in-place VACUUM 压实（失败/ENOSPC 回退未 VACUUM 副本，不让备份失败）
            runCatching {
                context.openOrCreateDatabase(databaseCacheFile.absolutePath, Context.MODE_PRIVATE, null).use { db ->
                    db.execSQL("VACUUM")
                    db.rawQuery("PRAGMA integrity_check", null).use { c ->
                        c.moveToFirst()
                        val ok = c.getString(0)
                        if (!"ok".equals(ok, ignoreCase = true)) {
                            this@BackupRecoveryManagerImpl.logger().w("backup VACUUM integrity_check = <$ok>, keep copy")
                        }
                    }
                }
            }.onFailure {
                this@BackupRecoveryManagerImpl.logger().w(it, "backup VACUUM failed, fallback to checkpointed copy")
            }

            // 多 entry 打包：db + record_images/* + settings.json + manifest.json（最高压缩）
            val settingsJson = settingRepository.exportSettings() // Task 11 提供
            val manifestJson = buildManifestJson(BACKUP_FORMAT_VERSION, ApplicationInfo.versionName)
            val imagesDir = recordImageFileStorage.baseDir()
            val zippedPath =
                backupCacheDir.absolutePath + File.separator + BACKUP_FILE_NAME + dateFormat + BACKUP_FILE_EXT
            ZipOutputStream(FileOutputStream(zippedPath)).use { zos ->
                zos.setLevel(Deflater.BEST_COMPRESSION)
                // db（comment 保留 applicationInfo，兼容旧恢复对 comment 的依赖被白名单取代后仍无害）
                putZipFileEntry(zos, databaseCacheFile, databaseCacheFile.name)
                // 图片原图（保持不有损）
                imagesDir.listFiles()?.filter { it.isFile }?.forEach { img ->
                    putZipFileEntry(zos, img, RECORD_IMAGES_ENTRY_PREFIX + img.name)
                }
                // settings + manifest
                putZipBytesEntry(zos, SETTINGS_ENTRY, settingsJson.toByteArray())
                putZipBytesEntry(zos, MANIFEST_ENTRY, manifestJson.toByteArray())
            }
```

新增私有辅助（companion 外、类内 private）：
```kotlin
    private fun putZipFileEntry(zos: ZipOutputStream, file: File, entryName: String) {
        val entry = ZipEntry(entryName)
        entry.comment = ApplicationInfo.applicationInfo
        zos.putNextEntry(entry)
        BufferedInputStream(FileInputStream(file)).use { zos.write(it.readBytes()) }
        zos.closeEntry()
    }

    private fun putZipBytesEntry(zos: ZipOutputStream, entryName: String, bytes: ByteArray) {
        val entry = ZipEntry(entryName)
        entry.comment = ApplicationInfo.applicationInfo
        zos.putNextEntry(entry)
        zos.write(bytes)
        zos.closeEntry()
    }
```

> 给 `BackupRecoveryManagerImpl` 构造注入 `private val recordImageFileStorage: RecordImageFileStorage`。`ApplicationInfo.versionName` 若无则用现有版本常量（Read `ApplicationInfo` 确认字段名，可能是 `versionName`/`applicationInfo`）。import `java.util.zip.Deflater`。

- [ ] **Step 6: 编译 + spotless + commit**

Run: `./gradlew :core:data:compileDebugKotlin --no-configuration-cache`
```bash
git add core/data/src/main/kotlin/.../uitl/BackupManifest.kt \
        core/data/src/main/kotlin/.../uitl/impl/BackupRecoveryManagerImpl.kt \
        core/data/src/test/kotlin/.../uitl/BackupManifestTest.kt
git commit -m "[feat|core:data|备份][公共]备份zip纳入图片+settings+manifest版本戳+VACUUM副本(ENOSPC回退)"
```

---

## Task 10: 恢复侧重写（白名单 + mkdirs + Zip Slip + 合并 + 前向守护 + 图片快照）

**Files:**
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/uitl/impl/BackupRecoveryManagerImpl.kt`（`startRecovery` 解压循环 631-661、`createPreRestoreBackup` 529-562、恢复后 683-708）
- Create: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/uitl/BackupEntryPolicy.kt`（entry 白名单 + Zip Slip 纯判据）
- Test: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/uitl/BackupEntryPolicyTest.kt`

**Interfaces:**
- Produces:
  - `internal fun isAllowedBackupEntry(entryName: String): Boolean`（`cashbook.db` / `record_images/` 前缀 / `settings.json` / `manifest.json`）
  - `internal fun isWithinDir(destFile: File, baseDir: File): Boolean`（Zip Slip canonicalPath 防护）

- [ ] **Step 1: 写白名单 + Zip Slip 纯判据失败测试**

新建 `BackupEntryPolicyTest.kt`：
```kotlin
@Test
fun isAllowedBackupEntry_whitelist() {
    assertThat(isAllowedBackupEntry("cashbook.db")).isTrue()
    assertThat(isAllowedBackupEntry("record_images/img_1.jpg")).isTrue()
    assertThat(isAllowedBackupEntry("settings.json")).isTrue()
    assertThat(isAllowedBackupEntry("manifest.json")).isTrue()
    assertThat(isAllowedBackupEntry("../evil.sh")).isFalse()
    assertThat(isAllowedBackupEntry("record_images/../../evil")).isFalse()
    assertThat(isAllowedBackupEntry("random.txt")).isFalse()
}

@Test
fun isWithinDir_blocksTraversal() {
    val base = File("/cache/recovery").canonicalFile
    assertThat(isWithinDir(File(base, "record_images/img_1.jpg"), base)).isTrue()
    assertThat(isWithinDir(File(base, "../escape"), base)).isFalse()
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*BackupEntryPolicyTest" --no-configuration-cache`
Expected: FAIL

- [ ] **Step 3: 写 BackupEntryPolicy.kt**

```kotlin
package cn.wj.android.cashbook.core.data.uitl

import java.io.File

/** 解压 entry 白名单（替代旧 comment 判据）：仅这些路径允许写出 */
internal fun isAllowedBackupEntry(entryName: String): Boolean {
    if (entryName.contains("..")) return false // 先挡 traversal
    return entryName == DB_FILE_NAME_CONST ||
        entryName == SETTINGS_ENTRY ||
        entryName == MANIFEST_ENTRY ||
        (entryName.startsWith(RECORD_IMAGES_ENTRY_PREFIX) && entryName.length > RECORD_IMAGES_ENTRY_PREFIX.length)
}

/** Zip Slip：destFile 规范化路径须在 baseDir 内 */
internal fun isWithinDir(destFile: File, baseDir: File): Boolean =
    destFile.canonicalPath.startsWith(baseDir.canonicalPath + File.separator)
```

> `DB_FILE_NAME_CONST` = 现有 `DB_FILE_NAME` 常量值（`cashbook.db`，Read `BackupRecoveryManagerImpl` companion 确认实际值并复用同一常量，勿硬编码重复）。可把白名单函数放在能见到 `DB_FILE_NAME` 的位置，或把 `DB_FILE_NAME` 提到可共享处。

- [ ] **Step 4: 跑通纯判据测试**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*BackupEntryPolicyTest" --no-configuration-cache`
Expected: PASS

- [ ] **Step 5: 改解压循环（631-661）为白名单 + mkdirs + 保留 Zip Slip**

替换循环体判据：把 `if (comment.isNullOrBlank()) continue` 改为 `if (!isAllowedBackupEntry(entry.name)) continue`；目录 entry `mkdirs` 后 continue；写文件前 `destFile.parentFile?.mkdirs()`；Zip Slip 用 `isWithinDir(destFile, cacheDir)`：

```kotlin
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement() as ZipEntry
                    val entryName = entry.name
                    if (!isAllowedBackupEntry(entryName)) {
                        continue
                    }
                    val destFile = File(cacheDir, entryName)
                    // Zip Slip 防护（保留）：含嵌套 record_images/*
                    if (!isWithinDir(destFile, cacheDir)) {
                        continue
                    }
                    if (entry.isDirectory) {
                        destFile.mkdirs()
                        continue
                    }
                    destFile.parentFile?.mkdirs()
                    if (destFile.exists()) {
                        destFile.delete()
                    }
                    destFile.createNewFile()
                    BufferedInputStream(zipFile.getInputStream(entry)).use { bis ->
                        BufferedOutputStream(FileOutputStream(destFile)).use { bos ->
                            bos.write(bis.readBytes())
                        }
                    }
                    destFiles.add(destFile)
                }
```

- [ ] **Step 6: 前向不兼容守护（读 manifest）**

dbFile 解析之后、createPreRestoreBackup 之前插入：
```kotlin
            val manifestFile = destFiles.firstOrNull { it.name == MANIFEST_ENTRY }
            val formatVersion = manifestFile?.let { parseManifestFormatVersion(it.readText()) } ?: 1
            if (formatVersion > BACKUP_FORMAT_VERSION) {
                this@BackupRecoveryManagerImpl.logger()
                    .e("startRecovery(), backup formatVersion <$formatVersion> newer than supported <$BACKUP_FORMAT_VERSION>")
                return@runCatching BackupRecoveryState.FAILED_FILE_FORMAT_ERROR
            }
```

- [ ] **Step 7: 恢复前快照含图片（createPreRestoreBackup 扩展）**

`createPreRestoreBackup` 在 db 快照后追加：把当前 `filesDir/record_images/` 整体复制到 `preRestoreDir/pre-restore-images/`（与 db 快照同为一个回滚单元）：
```kotlin
        // 图片快照（与 db 快照同回滚单元）
        val imagesDir = recordImageFileStorage.baseDir()
        if (imagesDir.exists()) {
            val snapshotImages = File(preRestoreDir, "pre-restore-images")
            imagesDir.copyRecursively(snapshotImages, overwrite = true)
        }
```

- [ ] **Step 8: 恢复 db 成功后拷回图片 + 应用 settings（合并语义）**

`recoveryFromDb` 返回 true 后（686 行 INSERT OR IGNORE 之后、recalculateAllFinalAmount 之前/之后）追加：
```kotlin
                // 图片合并恢复：解压出的 record_images/ 叠加到 filesDir/record_images/（不清空现有目录）
                val restoredImagesDir = File(cacheDir, "record_images")
                if (restoredImagesDir.exists()) {
                    restoredImagesDir.copyRecursively(recordImageFileStorage.baseDir(), overwrite = true)
                }
                // settings 白名单恢复（任一校验失败整体跳过，绝不恢复 WebDAV/backupPath/autoBackup）
                destFiles.firstOrNull { it.name == SETTINGS_ENTRY }?.let { sf ->
                    runCatching { settingRepository.importSettings(sf.readText()) }
                        .onFailure { this@BackupRecoveryManagerImpl.logger().w(it, "import settings skipped") }
                }
```

- [ ] **Step 9: 编译 + spotless + commit**

Run: `./gradlew :core:data:compileDebugKotlin --no-configuration-cache`
```bash
git add core/data/src/main/kotlin/.../uitl/BackupEntryPolicy.kt \
        core/data/src/main/kotlin/.../uitl/impl/BackupRecoveryManagerImpl.kt \
        core/data/src/test/kotlin/.../uitl/BackupEntryPolicyTest.kt
git commit -m "[feat|core:data|恢复][公共]恢复侧重写:entry白名单+mkdirs+ZipSlip+图片合并+manifest前向守护+恢复前图片快照"
```

---

# Phase 4：设置项备份（白名单·设备无关·排除凭据）

> **执行顺序**：本 Phase 的 Task 11 应在 Phase 3 的 Task 9/10 之前完成（备份/恢复要调 `exportSettings`/`importSettings`）。

## Task 11: SettingRepository.exportSettings / importSettings（白名单 + 校验）

**Files:**
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/SettingRepository.kt`（加 2 方法）
- Create: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/SettingsBackupCodec.kt`（纯序列化/校验逻辑）
- Modify: `core/data/src/main/kotlin/cn/wj/android/cashbook/core/data/repository/impl/SettingRepositoryImpl.kt`（实现，调 codec + datasource）
- Modify: `core/testing/src/main/kotlin/cn/wj/android/cashbook/core/testing/repository/FakeSettingRepository.kt`
- Test: `core/data/src/test/kotlin/cn/wj/android/cashbook/core/data/repository/SettingsBackupCodecTest.kt`

**Interfaces:**
- Produces:
  - `SettingRepository.exportSettings(): String`、`SettingRepository.importSettings(json: String)`
  - codec 纯逻辑：`internal data class SettingsBackup(val useGithub, autoCheckUpdate, ignoreUpdateVersion, mobileNetworkDownloadEnable, mobileNetworkBackupEnable, darkMode: Int, dynamicColor, imageQuality: Int, canary, logcatInRelease, monthStartDay: Int)`、`internal fun encodeSettingsBackup(b: SettingsBackup): String`、`internal fun decodeSettingsBackup(json: String): SettingsBackup?`（任一校验失败返 null）

- [ ] **Step 1: 写 codec 失败测试（白名单 round-trip + 排除 + 校验拒绝）**

新建 `SettingsBackupCodecTest.kt`：
```kotlin
@Test
fun encodeDecode_roundTrip() {
    val b = SettingsBackup(
        useGithub = true, autoCheckUpdate = false, ignoreUpdateVersion = "v1.0",
        mobileNetworkDownloadEnable = true, mobileNetworkBackupEnable = false,
        darkMode = 2, dynamicColor = true, imageQuality = 1,
        canary = false, logcatInRelease = true, monthStartDay = 5,
    )
    val decoded = decodeSettingsBackup(encodeSettingsBackup(b))
    assertThat(decoded).isEqualTo(b)
}

@Test
fun encode_excludesCredentialAndDeviceFields() {
    val json = encodeSettingsBackup(sampleBackup())
    // 白名单序列化天然不含这些键
    listOf("passwordIv", "fingerprintIv", "passwordInfo", "webDAVPassword", "webDAVDomain",
           "webDAVAccount", "backupPath", "autoBackup", "currentBookId", "verificationMode")
        .forEach { assertThat(json).doesNotContain(it) }
}

@Test
fun decode_rejectsDarkModeOutOfRange() {
    val json = encodeSettingsBackup(sampleBackup()).replace("\"darkMode\":2", "\"darkMode\":9")
    assertThat(decodeSettingsBackup(json)).isNull()
}

@Test
fun decode_rejectsMonthStartDayOutOfRange() {
    val json = encodeSettingsBackup(sampleBackup().copy(monthStartDay = 5)).replace("\"monthStartDay\":5", "\"monthStartDay\":40")
    assertThat(decodeSettingsBackup(json)).isNull()
}

@Test
fun decode_malformedJson_returnsNull() {
    assertThat(decodeSettingsBackup("not json")).isNull()
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*SettingsBackupCodecTest" --no-configuration-cache`
Expected: FAIL

- [ ] **Step 3: 写 SettingsBackupCodec.kt（白名单 + 严格校验）**

```kotlin
package cn.wj.android.cashbook.core.data.repository

import org.json.JSONException
import org.json.JSONObject

internal data class SettingsBackup(
    val useGithub: Boolean,
    val autoCheckUpdate: Boolean,
    val ignoreUpdateVersion: String,
    val mobileNetworkDownloadEnable: Boolean,
    val mobileNetworkBackupEnable: Boolean,
    val darkMode: Int,        // DarkModeEnum ordinal 0..2
    val dynamicColor: Boolean,
    val imageQuality: Int,    // ImageQualityEnum ordinal 0..2
    val canary: Boolean,
    val logcatInRelease: Boolean,
    val monthStartDay: Int,   // 1..28
)

internal fun encodeSettingsBackup(b: SettingsBackup): String =
    JSONObject()
        .put("useGithub", b.useGithub)
        .put("autoCheckUpdate", b.autoCheckUpdate)
        .put("ignoreUpdateVersion", b.ignoreUpdateVersion)
        .put("mobileNetworkDownloadEnable", b.mobileNetworkDownloadEnable)
        .put("mobileNetworkBackupEnable", b.mobileNetworkBackupEnable)
        .put("darkMode", b.darkMode)
        .put("dynamicColor", b.dynamicColor)
        .put("imageQuality", b.imageQuality)
        .put("canary", b.canary)
        .put("logcatInRelease", b.logcatInRelease)
        .put("monthStartDay", b.monthStartDay)
        .toString()

/** 严格校验：缺字段/枚举越界/monthStartDay 越界/非法 JSON 一律返 null（整体跳过设置恢复） */
internal fun decodeSettingsBackup(json: String): SettingsBackup? = try {
    val o = JSONObject(json)
    val darkMode = o.getInt("darkMode")
    val imageQuality = o.getInt("imageQuality")
    val monthStartDay = o.getInt("monthStartDay")
    if (darkMode !in 0..2 || imageQuality !in 0..2 || monthStartDay !in 1..28) {
        null
    } else {
        SettingsBackup(
            useGithub = o.getBoolean("useGithub"),
            autoCheckUpdate = o.getBoolean("autoCheckUpdate"),
            ignoreUpdateVersion = o.getString("ignoreUpdateVersion"),
            mobileNetworkDownloadEnable = o.getBoolean("mobileNetworkDownloadEnable"),
            mobileNetworkBackupEnable = o.getBoolean("mobileNetworkBackupEnable"),
            darkMode = darkMode,
            dynamicColor = o.getBoolean("dynamicColor"),
            imageQuality = imageQuality,
            canary = o.getBoolean("canary"),
            logcatInRelease = o.getBoolean("logcatInRelease"),
            monthStartDay = monthStartDay,
        )
    }
} catch (e: JSONException) {
    null
}
```

- [ ] **Step 4: 跑通 codec 测试**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*SettingsBackupCodecTest" --no-configuration-cache`
Expected: PASS

- [ ] **Step 5: 接口 + 实现 exportSettings/importSettings**

`SettingRepository.kt` 加：
```kotlin
    /** 导出设备无关偏好白名单为 JSON（排除凭据/WebDAV/设备绑定项） */
    suspend fun exportSettings(): String

    /** 导入白名单设置；任一校验失败整体跳过，绝不恢复 WebDAV/backupPath/autoBackup */
    suspend fun importSettings(json: String)
```
`SettingRepositoryImpl.kt`（用现有 update* 方法 + 枚举 ordinalOf 逐字段恢复）：
```kotlin
    override suspend fun exportSettings(): String {
        val app = appSettingsModel.first()
        val record = recordSettingsModel.first()
        return encodeSettingsBackup(
            SettingsBackup(
                useGithub = app.useGithub,
                autoCheckUpdate = app.autoCheckUpdate,
                ignoreUpdateVersion = app.ignoreUpdateVersion,
                mobileNetworkDownloadEnable = app.mobileNetworkDownloadEnable,
                mobileNetworkBackupEnable = app.mobileNetworkBackupEnable,
                darkMode = app.darkMode.ordinal,
                dynamicColor = app.dynamicColor,
                imageQuality = app.imageQuality.ordinal,
                canary = app.canary,
                logcatInRelease = app.logcatInRelease,
                monthStartDay = record.monthStartDay,
            ),
        )
    }

    override suspend fun importSettings(json: String) {
        val b = decodeSettingsBackup(json) ?: return // 校验失败整体跳过
        updateUseGithub(b.useGithub)
        updateAutoCheckUpdate(b.autoCheckUpdate)
        updateIgnoreUpdateVersion(b.ignoreUpdateVersion)
        updateMobileNetworkDownloadEnable(b.mobileNetworkDownloadEnable)
        updateMobileNetworkBackupEnable(b.mobileNetworkBackupEnable)
        updateDarkMode(DarkModeEnum.ordinalOf(b.darkMode))
        updateDynamicColor(b.dynamicColor)
        updateImageQuality(ImageQualityEnum.ordinalOf(b.imageQuality))
        updateCanary(b.canary)
        updateLogcatInRelease(b.logcatInRelease)
        updateMonthStartDay(b.monthStartDay)
        // 注：不调用任何 WebDAV/backupPath/autoBackup/verification/security 更新（设备本地，安全）
    }
```
（import `DarkModeEnum`/`ImageQualityEnum`、`kotlinx.coroutines.flow.first`）

- [ ] **Step 6: FakeSettingRepository 实现**

```kotlin
    override suspend fun exportSettings(): String =
        encodeSettingsBackup(
            SettingsBackup(
                useGithub = _appSettingsModel.value.useGithub,
                autoCheckUpdate = _appSettingsModel.value.autoCheckUpdate,
                ignoreUpdateVersion = _appSettingsModel.value.ignoreUpdateVersion,
                mobileNetworkDownloadEnable = _appSettingsModel.value.mobileNetworkDownloadEnable,
                mobileNetworkBackupEnable = _appSettingsModel.value.mobileNetworkBackupEnable,
                darkMode = _appSettingsModel.value.darkMode.ordinal,
                dynamicColor = _appSettingsModel.value.dynamicColor,
                imageQuality = _appSettingsModel.value.imageQuality.ordinal,
                canary = _appSettingsModel.value.canary,
                logcatInRelease = _appSettingsModel.value.logcatInRelease,
                monthStartDay = _recordSettingsModel.value.monthStartDay,
            ),
        )

    override suspend fun importSettings(json: String) {
        val b = decodeSettingsBackup(json) ?: return
        _appSettingsModel.value = _appSettingsModel.value.copy(
            useGithub = b.useGithub,
            autoCheckUpdate = b.autoCheckUpdate,
            ignoreUpdateVersion = b.ignoreUpdateVersion,
            mobileNetworkDownloadEnable = b.mobileNetworkDownloadEnable,
            mobileNetworkBackupEnable = b.mobileNetworkBackupEnable,
            darkMode = DarkModeEnum.ordinalOf(b.darkMode),
            dynamicColor = b.dynamicColor,
            imageQuality = ImageQualityEnum.ordinalOf(b.imageQuality),
            canary = b.canary,
            logcatInRelease = b.logcatInRelease,
        )
        _recordSettingsModel.value = _recordSettingsModel.value.copy(monthStartDay = b.monthStartDay)
    }
```

> `SettingsBackup`/`encode`/`decode` 为 `core/data` 的 `internal`；`core/testing` 是独立模块，跨模块 internal 不可见。**解法**：把 codec 的 `SettingsBackup`/`encodeSettingsBackup`/`decodeSettingsBackup` 设为 `public`（仍在 core/data），或让 FakeSettingRepository 的 export/import 直接用 org.json 自行实现（不复用 codec）。**优先**：FakeSettingRepository 直接 `JSONObject` 简单实现（测试替身只需行为正确），不强依赖 core/data internal。据此调整上面 Fake 实现为内联 org.json。

- [ ] **Step 7: 加 SettingRepositoryImplTest 集成用例**

```kotlin
@Test
fun exportThenImport_roundTripsWhitelistFields() = runTest {
    repository.updateDarkMode(DarkModeEnum.DARK)
    repository.updateMonthStartDay(7)
    val json = repository.exportSettings()
    // 改回再导入，断言被覆盖
    repository.updateDarkMode(DarkModeEnum.LIGHT)
    repository.importSettings(json)
    assertThat(repository.appSettingsModel.first().darkMode).isEqualTo(DarkModeEnum.DARK)
    assertThat(repository.recordSettingsModel.first().monthStartDay).isEqualTo(7)
}

@Test
fun importSettings_doesNotTouchWebDAVOrBackupPath() = runTest {
    repository.updateWebDAV("https://dav.example/", "acc", "pwd")
    repository.updateBackupPath("/sdcard/backup")
    val json = repository.exportSettings() // 不含 WebDAV/backupPath
    repository.importSettings(json)
    val app = repository.appSettingsModel.first()
    assertThat(app.webDAVDomain).isEqualTo("https://dav.example/")
    assertThat(app.backupPath).isEqualTo("/sdcard/backup")
}
```

- [ ] **Step 8: 跑测试 + 编译 + spotless + commit**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*SettingsBackupCodecTest" --tests "*SettingRepositoryImplTest" --no-configuration-cache`
Expected: PASS
```bash
git add core/data/src/main/kotlin/.../SettingsBackupCodec.kt \
        core/data/src/main/kotlin/.../SettingRepository.kt \
        core/data/src/main/kotlin/.../impl/SettingRepositoryImpl.kt \
        core/testing/src/main/kotlin/.../FakeSettingRepository.kt \
        core/data/src/test/kotlin/.../SettingsBackupCodecTest.kt \
        core/data/src/test/kotlin/.../impl/SettingRepositoryImplTest.kt
git commit -m "[feat|core:data|设置备份][公共]SettingRepository白名单exportSettings/importSettings(org.json+严格校验+排除凭据)+单测"
```

---

## Task 12: UI「设置项纳入备份」开关

**Files:**
- Modify: `feature/settings/src/main/kotlin/cn/wj/android/cashbook/feature/settings/viewmodel/BackupAndRecoveryViewModel.kt`
- Modify: `feature/settings/src/main/kotlin/cn/wj/android/cashbook/feature/settings/screen/BackupAndRecoveryScreen.kt`
- Modify: 对应 `*ScreenshotTests` / `*ViewModelTest`（签名同步，模块测试源集整体编译）

> **范围裁剪（YAGNI）**：spec 提「UI 暴露开关」。当前备份**总是**纳入 settings.json（备份体积增量极小、且恢复侧严格校验+排除凭据已安全）。是否值得引入用户开关需确认——**默认实现为「始终纳入、无开关」**，省去新 proto 字段 + UI 状态 + 测试连锁。若用户坚持要开关，再加一个 AppSettings proto 布尔 `settingsBackupEnable`（走 Task 4 同款全链）。**本 Task 默认仅做：在 BackupAndRecoveryScreen 增一行只读说明「备份包含应用偏好设置（不含账号/密码/WebDAV）」**，无交互开关。

- [ ] **Step 1: 加只读说明文案**

在 `BackupAndRecoveryScreen.kt` 备份区块加一行 `CbListItem`/`Text` 说明（`stringResource`，新增字符串资源 `feature/settings/src/main/res/values/strings.xml` + `values-en`）。文案：中「备份将包含应用偏好设置（深色模式、图片质量、月起始日等），不含账号、密码与 WebDAV 凭据」。

- [ ] **Step 2: 编译 + 截图基线同步**

Run: `./gradlew :feature:settings:compileDebugKotlin --no-configuration-cache`
若 `BackupAndRecoveryScreen` 有截图测试，`recordRoborazziOnlineDebug` 重录该屏基线（仅文案行新增，视觉抽检非塌陷）。

- [ ] **Step 3: spotless + commit**

```bash
git add feature/settings/src/main/kotlin/.../BackupAndRecoveryScreen.kt \
        feature/settings/src/main/res/values/strings.xml \
        feature/settings/src/main/res/values-en/strings.xml \
        feature/settings/src/test/  # 若截图基线变化
git commit -m "[feat|feature:settings|设置备份][公共]备份页加「含应用偏好不含凭据」只读说明+基线"
```

---

# Phase 5：instrumented 测试 + 截图 + 完整链路

## Task 13: androidTest（backfill / 备份恢复往返 / Zip Slip / 孤儿）

**Files:**
- Create/Modify: `core/data/src/androidTest/kotlin/.../BackupRecoveryImageInstrumentedTest.kt`（或 `core/database` androidTest，按 Robolectric/真机能力）
- Modify: `core/data/src/test/.../BackupRecoveryManagerSchemeTest.kt`（补可 JVM 化的 entry 策略集成）

> **环境前提**：`connectedDebugAndroidTest` 首次需联网拉 UTP（不能 `--offline`）；本机经代理拉 Maven Central 时 TLS 可能不稳（属环境问题）。Robolectric 单测 fork JVM 不继承代理，需要 `android-all-instrumented` 暖缓存（见 CLAUDE.local.md）。**优先**把可纯 JVM 化的逻辑（已在 Task 1/3/5/8/9/10/11 用纯函数 + Fake 覆盖）留 JVM；仅「真实 SQLite copyData + 真实 filesDir + 真实 zip 往返 + 崩溃重入」必须 instrumented。

- [ ] **Step 1: 写 instrumented 用例（覆盖 spec 测试计划 ①②③④⑤⑥）**

用例清单（每个 `@Test`，真机/模拟器 `:core:data:connectedDebugAndroidTest`）：
1. `backfill_blobToFile_endToEnd`：建含 BLOB 图片行的真库 → 调 `backfillImagesToFiles` → 断言 filesDir/record_images 文件存在、行 path 相对化、bytes 空。
2. `backfill_crashReentry_idempotent`：backfill 跑一半（写文件后中断模拟）→ 再跑 → 无重复文件、最终一致。
3. `oldDbOnlyBackup_recovery_materializesImagesViaBackfill`：构造旧 db-only zip（图片在 BLOB）→ 恢复 → 重启触发 backfill → 图片落文件。
4. `newFormatZip_roundTrip`：备份（db + record_images + settings + manifest）→ 恢复 → 图片文件 + 设置白名单字段一致。
5. `maliciousZip_zipSlip_noWriteOutsideCacheDir`：构造含 `../` traversal entry 的 zip → 恢复 → 断言 cacheDir 外无文件写出。
6. `orphanCleanup_doesNotDeleteReferencedOrGraceWindowFiles`：建引用文件 + 孤儿 + 新写文件 → 扫描 → 仅删旧孤儿。
7. `preRestoreSnapshot_includesImages`：恢复前 `pre-restore-images/` 含当前图片副本。

- [ ] **Step 2: 跑 instrumented（需设备）**

Run: `./gradlew :core:data:connectedDebugAndroidTest --no-configuration-cache`（去 `--offline`，清继承代理 + 加 `-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897`）
Expected: 全 PASS。若环境（代理 TLS/无设备）受阻则记 backlog，不阻断本地 JVM 链路完成。

- [ ] **Step 3: commit**

```bash
git add core/data/src/androidTest/kotlin/.../BackupRecoveryImageInstrumentedTest.kt
git commit -m "[test|core:data|备份恢复][公共]instrumented:backfill/新旧格式往返/ZipSlip/孤儿/恢复前图片快照7用例"
```

---

## Task 14: 含图片的记录详情截图基线

**Files:**
- Modify: `feature/records/src/test/.../*ScreenshotTests.kt`（至少 1 处 `relatedImage` 非空）
- Modify/Create: 对应 `src/test/screenshots/` 基线 PNG

- [ ] **Step 1: 加非空 relatedImage 截图用例**

在记录详情截图测试新增一个 case，`relatedImage` 传含 1 张图（path = `record_images/img_demo.jpg`、bitmap = 测试用纯色 Bitmap），守护 path→双轨→显示路径。**先 grep `\.now()`** 排查时间脆弱性。

- [ ] **Step 2: record 基线**

Run: `./gradlew recordRoborazziOnlineDebug --no-configuration-cache`（或模块级），视觉抽检 New 非塌陷（Read 基线 PNG），再 `verifyRoborazziOnlineDebug` 0 diff 确认确定性。

- [ ] **Step 3: commit**

```bash
git add feature/records/src/test/
git commit -m "[test|feature:records|图片存储][公共]记录详情含图片截图基线(守护path双轨显示路径)"
```

---

## Task 15: 完整链路验证（真机端到端）

**Files:** 无（验证 + 记录）

- [ ] **Step 1: 端到端手测脚本**

真机走完整链路并记录每步结果：
1. 加图记账 → 保存 → 进 DB 查 `db_image_with_related`：新行 `image_path` 为 `record_images/...`、`image_bytes` 为空（`length(image_bytes)=0`），filesDir/record_images 下有文件。
2. 杀进程重启 → 老 BLOB 图片（若构造了存量）经 backfill 落文件、首屏不卡。
3. 记录详情/预览图片正常显示（双轨）。
4. 备份 → 解压 zip 确认含 `cashbook.db` + `record_images/*` + `settings.json` + `manifest.json`。
5. 卸载重装 → 恢复该 zip → 图片显示、设置白名单字段恢复、WebDAV/backupPath 未被覆盖。
6. 删记录 → 对应文件被删（或孤儿被启动扫描清理）。

- [ ] **Step 2: 记录验证结果**

把每步实际结果（命令/截图/DB 查询输出）记入会话或 backlog；任一失败定位根因修复重验。

---

# 自审：spec 覆盖映射

| spec 章节/要求 | 对应 Task |
|---|---|
| A. 存储布局 record_images/ + path 相对值 | T1, T2 |
| A. 新图写文件 + bytes 置空 | T2 |
| A. 双轨读（File 优先 bytes 回退，Coil 传 File） | T3 |
| A. 存量 backfill 逐行幂等 + TempKeys 标志全链 | T4, T5, T6 |
| A. 空间回收（备份副本 VACUUM） | T9 |
| A. 孤儿清理（删点删文件 + 启动扫描限定/grace） | T7, T8 |
| A. 原子性（temp+rename、删 DB 后删文件、读容忍缺文件） | T1(write), T3(None 容错), T7 |
| A. 本期不删 bytes 字段（保留双轨、不动 schema） | Global Constraints |
| B. zip 结构 db+images+settings+manifest | T9 |
| B. VACUUM 副本 + integrity_check + ENOSPC 回退 | T9 |
| B. 最高压缩 BEST_COMPRESSION | T9 |
| B. 向后兼容旧 db-only 备份 | T10(formatVersion 缺省=1), T13(用例3) |
| B. 恢复侧重写（白名单+mkdirs+Zip Slip+目录处理） | T10 |
| B. 前向不兼容守护（manifest 版本 fail-fast） | T10 |
| B. 恢复前快照含图片 + 合并语义 | T10(Step7/8) |
| C. 设置白名单导出（设备无关偏好） | T11 |
| C. 显式排除凭据/WebDAV/设备绑定/recordSettings ids | T11(不调对应 update) |
| C. org.json 零依赖 | T11 |
| C. 恢复严格校验、失败整体跳过、绝不恢复 WebDAV/backupPath/autoBackup | T11 |
| C. 新增接口 + Fake + Test + UI 开关 | T11, T12 |
| 测试计划 androidTest ①-⑥ | T13 |
| 测试计划 JVM（settings/双轨/Fake 同步/Mapping） | T1,T3,T5,T11 |
| 测试计划 截图基线（relatedImage 非空） | T14 |
| 测试计划 完整链路真机 | T15 |

**遗留/执行注意**：
- **执行顺序**：Task 11（exportSettings/importSettings）须先于 Task 9/10（备份恢复调用它）。Task 4 须先于 Task 5/6。其余按编号。
- **Task 5/6 耦合修订**：`imagesToFilesMigrated` 置位封装在 `backfillImagesToFiles()` 内部（成功后置位），需给 `RecordRepositoryImpl` 注入设置写入口（复用现有 `combineProtoDataSource` 或 `settingRepository`）；执行 Task 5 时一并落实置位，Task 6 仅读标志决定调用。
- **Task 12 范围**：默认不做交互开关（YAGNI），仅加只读说明；若用户要开关再走 proto 全链。
- **`ApplicationInfo` 字段名**（versionName/applicationInfo/DB_VERSION/DB_FILE_NAME）执行时 Read 确认，复用既有常量，勿硬编码。
- **`isManagedImagePath` 可见性**：跨模块用到处（feature:records）需 public，Task 1/3 落实。
