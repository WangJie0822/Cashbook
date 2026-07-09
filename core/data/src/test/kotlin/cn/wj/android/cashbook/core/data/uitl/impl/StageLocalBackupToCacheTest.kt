/*
 * Copyright 2021 The Cashbook Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.wj.android.cashbook.core.data.uitl.impl

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * [stageLocalBackupToCache] 单测：守护文件路径恢复分支「暂存 zip 进缓存」的修复。
 *
 * 该分支被 WebDAV 恢复（getWebFile 下到 cacheDir 内同名文件，走 same-file skip 不复制）与本地原始
 * 文件路径恢复（SAF 返回非 content:// 的 uri.path 回退，产出 cacheDir 外路径，需真正复制）共用。
 * 历史缺陷：旧实现 `createNewFile()` + `copyTo()`（默认 overwrite=false）→ 目标已被 createNewFile
 * 建出而必抛 [kotlin.io.FileAlreadyExistsException]，仅在「需真正复制」子情形触发（WebDAV 走 skip 故
 * 从未暴露）。修复为 `copyTo(overwrite=true)`。
 */
class StageLocalBackupToCacheTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val backupBytes = ByteArray(2048) { ((it * 17 + 3) % 256).toByte() }

    @Test
    fun fileOutsideCacheDir_copiedIntoCache_bytesIdentical() {
        // 备份文件在缓存目录之外（真实场景：用户选定的存储目录）
        val srcDir = tempFolder.newFolder("src")
        val cacheDir = tempFolder.newFolder("cache")
        val backup = File(srcDir, "Cashbook_Backup_File_x.zip").apply { writeBytes(backupBytes) }

        val staged = stageLocalBackupToCache(backup, cacheDir)

        // 旧实现此处会抛 FileAlreadyExistsException；修复后正确拷入缓存
        assertThat(staged.absolutePath).isEqualTo(File(cacheDir, backup.name).absolutePath)
        assertThat(staged.exists()).isTrue()
        assertThat(staged.readBytes()).isEqualTo(backupBytes)
    }

    @Test
    fun destAlreadyExists_overwritten_noException() {
        // 缓存目录已有同名残留（上次失败尝试遗留），应被覆盖为新内容而非抛异常
        val srcDir = tempFolder.newFolder("src")
        val cacheDir = tempFolder.newFolder("cache")
        val backup = File(srcDir, "Cashbook_Backup_File_x.zip").apply { writeBytes(backupBytes) }
        File(cacheDir, backup.name).writeBytes("stale-leftover".toByteArray())

        val staged = stageLocalBackupToCache(backup, cacheDir)

        assertThat(staged.readBytes()).isEqualTo(backupBytes)
    }

    @Test
    fun fileAlreadyInCacheDir_sameName_notRecopied_bytesIntact() {
        // 源即目标（备份已在缓存目录内、同名）：跳过复制，避免自拷贝抛异常，内容不变
        val cacheDir = tempFolder.newFolder("cache")
        val backup = File(cacheDir, "Cashbook_Backup_File_x.zip").apply { writeBytes(backupBytes) }

        val staged = stageLocalBackupToCache(backup, cacheDir)

        assertThat(staged.absolutePath).isEqualTo(backup.absolutePath)
        assertThat(staged.readBytes()).isEqualTo(backupBytes)
    }
}
