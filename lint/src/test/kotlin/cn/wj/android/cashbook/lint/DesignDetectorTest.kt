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

package cn.wj.android.cashbook.lint

import cn.wj.android.cashbook.lint.design.DesignDetector
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

class DesignDetectorTest {

    @Test
    fun material_method_calls_are_reported() {
        lint().issues(DesignDetector.ISSUE)
            .files(
                MATERIAL3_STUB,
                kotlin(
                    """
                    package com.example
                    import androidx.compose.material3.MaterialTheme
                    import androidx.compose.material3.Scaffold
                    import androidx.compose.material3.TextField
                    import androidx.compose.material3.Card
                    import androidx.compose.material3.AlertDialog
                    fun screen() {
                        MaterialTheme()
                        Scaffold()
                        TextField()
                        Card()
                        AlertDialog()
                    }
                    """,
                ).indented(),
            )
            .run()
            .expectErrorCount(5)
    }

    @Test
    fun material_usage_message_names_design_system_replacement() {
        lint().issues(DesignDetector.ISSUE)
            .files(
                MATERIAL3_STUB,
                kotlin(
                    """
                    package com.example
                    import androidx.compose.material3.MaterialTheme
                    fun app() {
                        MaterialTheme()
                    }
                    """,
                ).indented(),
            )
            .run()
            .expectContains("Using MaterialTheme instead of CashbookTheme")
    }

    @Test
    fun icons_receiver_is_reported() {
        lint().issues(DesignDetector.ISSUE)
            .files(
                kotlin(
                    """
                    package com.example
                    val a = Icons.Default
                    """,
                ).indented(),
            )
            .run()
            .expectContains("Using Icons instead of CbIcons")
    }

    @Test
    fun design_system_components_are_clean() {
        lint().issues(DesignDetector.ISSUE)
            .files(
                DESIGN_STUB,
                kotlin(
                    """
                    package com.example
                    import cn.wj.android.cashbook.core.design.component.CbScaffold
                    import cn.wj.android.cashbook.core.design.component.CbTextField
                    fun screen() {
                        CbScaffold()
                        CbTextField()
                    }
                    """,
                ).indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun progress_indicators_are_reported() {
        lint().issues(DesignDetector.ISSUE)
            .files(
                MATERIAL3_STUB,
                kotlin(
                    """
                    package com.example
                    import androidx.compose.material3.LinearProgressIndicator
                    import androidx.compose.material3.CircularProgressIndicator
                    fun screen() {
                        LinearProgressIndicator()
                        CircularProgressIndicator()
                    }
                    """,
                ).indented(),
            )
            .run()
            .expectContains("Using LinearProgressIndicator instead of CbLinearProgressIndicator")
    }

    private companion object {
        /** Material3 组件桩，使被测代码中的调用可解析、UCallExpression 能拿到 methodName。 */
        private val MATERIAL3_STUB: TestFile = kotlin(
            """
            package androidx.compose.material3
            fun MaterialTheme() {}
            fun Scaffold() {}
            fun TextField() {}
            fun Card() {}
            fun AlertDialog() {}
            fun LinearProgressIndicator() {}
            fun CircularProgressIndicator() {}
            """,
        ).indented()

        /** 设计系统封装组件桩，用于验证 detector 不误报合规调用。 */
        private val DESIGN_STUB: TestFile = kotlin(
            """
            package cn.wj.android.cashbook.core.design.component
            fun CbScaffold() {}
            fun CbTextField() {}
            """,
        ).indented()
    }
}
