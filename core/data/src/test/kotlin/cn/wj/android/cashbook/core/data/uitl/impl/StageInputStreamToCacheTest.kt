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
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream

/**
 * [stageInputStreamToCache] 单测：守护 content:// 恢复分支「暂存 zip 进缓存」的流式化（#2 backlog）。
 *
 * 与 [stageLocalBackupToCache] 对称：本地文件路径分支已流式 copyTo，content:// 分支原用整流
 * `readBytes()` 把整个备份 zip（含图片 BLOB）读入堆内存，改为流式 copyTo（O(1) 内存）。
 * name 来自 DocumentFile.name（provider 可控），用 isWithinDir canonical 校验拒绝路径穿越。
 */
class StageInputStreamToCacheTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val backupBytes = ByteArray(64 * 1024) { ((it * 31 + 7) % 256).toByte() }

    @Test
    fun stream_copiedIntoCache_bytesIdentical() {
        val cacheDir = tempFolder.newFolder("cache")
        val input = TrackingInputStream(ByteArrayInputStream(backupBytes))

        val staged = stageInputStreamToCache(input, cacheDir, "Cashbook_Backup_File_x.zip")

        assertThat(staged.absolutePath)
            .isEqualTo(File(cacheDir, "Cashbook_Backup_File_x.zip").absolutePath)
        assertThat(staged.exists()).isTrue()
        assertThat(staged.readBytes()).isEqualTo(backupBytes)
        assertThat(input.closed).isTrue() // 成功路径关流（copyTo 后 use 关闭）
    }

    @Test
    fun destAlreadyExists_overwritten() {
        val cacheDir = tempFolder.newFolder("cache")
        File(cacheDir, "Cashbook_Backup_File_x.zip").writeBytes("stale-leftover".toByteArray())
        val input = TrackingInputStream(ByteArrayInputStream(backupBytes))

        val staged = stageInputStreamToCache(input, cacheDir, "Cashbook_Backup_File_x.zip")

        assertThat(staged.readBytes()).isEqualTo(backupBytes)
        assertThat(input.closed).isTrue()
    }

    @Test
    fun nameWithPathTraversal_rejected_stillClosesStream() {
        // M-1 回归：require 拒绝路径穿越时，caller 已打开的 input 流仍须关闭（防 content-resolver fd 泄漏）。
        // 修复前 require 先于 input.use 抛异常、流从不关闭 → closed=false 此断言失败；body 包进 input.use 后转绿。
        val cacheDir = tempFolder.newFolder("cache")
        val input = TrackingInputStream(ByteArrayInputStream(backupBytes))

        try {
            stageInputStreamToCache(input, cacheDir, "../escape.zip")
            throw AssertionError("expected IllegalArgumentException for path traversal name")
        } catch (e: IllegalArgumentException) {
            // 期望：越出 cacheDir 被 isWithinDir 拒绝
        }
        assertThat(input.closed).isTrue() // 拒绝路径也须关流（M-1）
    }

    /** 记录 close() 是否被调用的 InputStream 装饰器（守护 M-1：任何退出路径都须关流）。 */
    private class TrackingInputStream(private val delegate: InputStream) : InputStream() {
        var closed = false
            private set

        override fun read(): Int = delegate.read()
        override fun read(b: ByteArray, off: Int, len: Int): Int = delegate.read(b, off, len)
        override fun close() {
            closed = true
            delegate.close()
        }
    }
}
