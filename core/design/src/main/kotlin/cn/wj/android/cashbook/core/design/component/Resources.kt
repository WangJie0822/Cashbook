package cn.wj.android.cashbook.core.design.component

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource

@SuppressLint("DiscouragedApi")
@Composable
fun painterDrawableResource(idStr: String): Painter {
    val context = LocalContext.current
    return painterResource(
        id = context.resources.getIdentifier(
            idStr,
            "drawable",
            context.packageName
        )
    )
}