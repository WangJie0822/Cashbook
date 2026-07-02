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

package cn.wj.android.cashbook.shared

import kotlin.test.Test
import kotlin.test.assertTrue

/** KMP 测试运行器 + expect/actual 验证（Phase 0 PoC）。 */
class PlatformTest {
    @Test
    fun platformName_isNonEmpty() {
        val name = platformName()
        assertTrue(name.isNotEmpty())
    }
}
