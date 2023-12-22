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

package cn.wj.android.cashbook.benchmarks.baselineprofile.settings

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import cn.wj.android.cashbook.benchmarks.PACKAGE_NAME
import cn.wj.android.cashbook.benchmarks.getTopAppBar
import cn.wj.android.cashbook.benchmarks.startActivityAndAllowNotifications
import cn.wj.android.cashbook.core.common.TestTag
import org.junit.Rule
import org.junit.Test

/**
 * `LauncherScreen` 基线配置
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/12/21
 */
class LauncherBaselineProfile {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() = baselineProfileRule.collect(PACKAGE_NAME) {
        pressHome()
        startActivityAndAllowNotifications()
        goToLauncherScreen()
    }
}

fun MacrobenchmarkScope.goToLauncherScreen() {
    val confirmSelector = By.res(TestTag.Launcher.LAUNCHER_PROTOCOL_CONFIRM)
    val confirmButton = device.findObject(confirmSelector)
    confirmButton.click()
    device.waitForIdle()
    val launcherTitleSelector = By.res(TestTag.Launcher.LAUNCHER_TITLE)
    getTopAppBar().wait(Until.hasObject(launcherTitleSelector), 2_000L)
}
