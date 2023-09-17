package cn.wj.android.cashbook.core.design.component

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import cn.wj.android.cashbook.core.common.tools.funLogger

@SuppressLint("DiscouragedApi")
@Composable
fun painterDrawableResource(idStr: String): Painter {
    return runCatching {
        val context = LocalContext.current
        painterResource(
            id = context.resources.getIdentifier(
                idStr,
                "drawable",
                context.packageName
            )
        )
    }.getOrElse { throwable ->
        funLogger("ResourcesKt").e(throwable, "painterDrawableResource(idStr = <$idStr>)")
        throw throwable
    }
}