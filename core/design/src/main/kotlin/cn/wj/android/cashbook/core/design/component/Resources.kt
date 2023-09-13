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
    val context = LocalContext.current
    funLogger("ResourcesKt").d("painterDrawableResource(idStr = <$idStr>)")
    return painterResource(
        id = context.resources.getIdentifier(
            idStr,
            "drawable",
            context.packageName
        )
    )
}