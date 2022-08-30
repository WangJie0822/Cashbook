package cn.wj.android.cashbook

import cn.wj.android.cashbook.base.ext.base.tag
import cn.wj.android.cashbook.base.tools.funLogger

/**
 * 测试基类
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2022/8/30
 */
abstract class TestBase {

    /** 日志打印 */
    protected fun log(message: String, vararg args: Any) {
        funLogger("TEST-$tag").i(message, *args)
    }
}