package cn.wj.android.cashbook.core.testing

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * 一个自定义运行工具，用于设置测试用仪器化应用程序类
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/12/15
 */
class CashbookTestRunner : AndroidJUnitRunner() {

    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?,
    ): Application {
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}