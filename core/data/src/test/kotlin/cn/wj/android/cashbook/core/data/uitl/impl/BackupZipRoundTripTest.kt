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
import java.io.FileOutputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * 备份 zip 打包 → 解包 round-trip 单测（方案 B，无需设备）。
 *
 * 用生产 [writeStoredZipEntry] 打包图片（STORED）+ 普通 DEFLATE entry，再用 [ZipFile] 读回，
 * 校验：① STORED 图片 entry 字节一致且 method=STORED ② DEFLATE entry 可恢复字节一致。
 * 真实 DB+SAF 端到端往返由模拟器手动黑盒验证（core:data 无 androidTest 基建）。
 */
class BackupZipRoundTripTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun storedImageEntry_and_deflateEntry_roundTripByteIdentical() {
        // 伪 JPEG 内容（含高熵字节，验证 STORED 不依赖可压缩性）
        val imageBytes = ByteArray(4096) { ((it * 31 + 7) % 256).toByte() }
        val imgFile = tempFolder.newFile("img.jpg").apply { writeBytes(imageBytes) }
        val dbBytes = "fake-database-content-deflated".toByteArray()
        val zipFile = tempFolder.newFile("backup.zip")

        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            zos.setLevel(Deflater.BEST_COMPRESSION)
            // 生产 STORED 打包逻辑（两遍流式 CRC/size）
            writeStoredZipEntry(zos, imgFile, "record_images/img.jpg")
            // 普通 DEFLATE entry（仿 db/settings/manifest）
            zos.putNextEntry(ZipEntry("cashbook.db"))
            zos.write(dbBytes)
            zos.closeEntry()
        }

        ZipFile(zipFile).use { zf ->
            val imgEntry = zf.getEntry("record_images/img.jpg")
            assertThat(imgEntry.method).isEqualTo(ZipEntry.STORED)
            assertThat(zf.getInputStream(imgEntry).readBytes()).isEqualTo(imageBytes)

            val dbEntry = zf.getEntry("cashbook.db")
            assertThat(dbEntry.method).isEqualTo(ZipEntry.DEFLATED)
            assertThat(zf.getInputStream(dbEntry).readBytes()).isEqualTo(dbBytes)
        }
    }
}
