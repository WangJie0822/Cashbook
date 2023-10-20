package cn.wj.android.cashbook.core.common

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * 应用全局协程
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/10/20
 */
class ApplicationCoroutineScope(
    override val coroutineContext: CoroutineContext = SupervisorJob() + Dispatchers.Main.immediate
) : CoroutineScope