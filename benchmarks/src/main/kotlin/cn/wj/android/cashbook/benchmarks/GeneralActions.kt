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

package cn.wj.android.cashbook.benchmarks

import android.Manifest
import android.os.Build
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import cn.wj.android.cashbook.core.common.TestTag

/** 允许通知权限 */
fun MacrobenchmarkScope.allowNotifications() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val command = "pm grant $packageName ${Manifest.permission.POST_NOTIFICATIONS}"
        device.executeShellCommand(command)
    }
}

/** 开启启动页并允许通知权限 */
fun MacrobenchmarkScope.startActivityAndAllowNotifications() {
    startActivityAndWait()
    allowNotifications()
}

/** 获取 `CbTopAppBar` */
fun MacrobenchmarkScope.getTopAppBar(): UiObject2 {
    device.wait(Until.hasObject(By.res(TestTag.CB_TOP_APP_BAR)), 2_000)
    return device.findObject(By.res(TestTag.CB_TOP_APP_BAR))
}
