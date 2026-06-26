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

package cn.wj.android.cashbook.core.data.uitl

import cn.wj.android.cashbook.core.common.DB_FILE_NAME
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

/** [isAllowedBackupEntry] / [isWithinDir] 白名单 + Zip Slip 纯判据单测 */
class BackupEntryPolicyTest {

    @Test
    fun isAllowedBackupEntry_whitelist() {
        assertThat(isAllowedBackupEntry(DB_FILE_NAME)).isTrue()
        assertThat(isAllowedBackupEntry("record_images/img_1.jpg")).isTrue()
        assertThat(isAllowedBackupEntry("settings.json")).isTrue()
        assertThat(isAllowedBackupEntry("manifest.json")).isTrue()
        // traversal / 非白名单一律拒绝
        assertThat(isAllowedBackupEntry("../evil.sh")).isFalse()
        assertThat(isAllowedBackupEntry("record_images/../../evil")).isFalse()
        assertThat(isAllowedBackupEntry("random.txt")).isFalse()
        assertThat(isAllowedBackupEntry("record_images/")).isFalse() // 仅前缀无文件名
    }

    @Test
    fun isWithinDir_blocksTraversal() {
        val base = File("/cache/recovery").canonicalFile
        assertThat(isWithinDir(File(base, "record_images/img_1.jpg"), base)).isTrue()
        assertThat(isWithinDir(File(base, "../escape"), base)).isFalse()
    }
}
