package cn.wj.android.cashbook.core.common.annotation

import javax.inject.Qualifier
import kotlin.annotation.AnnotationRetention.RUNTIME

@Qualifier
@Retention(RUNTIME)
annotation class Dispatcher(val dispatcher: CashbookDispatchers)

enum class CashbookDispatchers {
    IO,
}
