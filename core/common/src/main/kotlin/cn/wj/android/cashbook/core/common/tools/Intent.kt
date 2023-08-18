@file:Suppress("unused")
@file:JvmName("IntentTools")

package cn.wj.android.cashbook.core.common.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import cn.wj.android.cashbook.core.common.ext.ifCondition

/**
 * Intent 跳转相关工具
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/17
 */

fun jumpSendEmail(
    email: String,
    subject: String = "",
    text: String = "",
    chooserTitle: CharSequence? = null,
    context: Context
) {
    val emailStart = "mailto:"
    val fixedEmail =
        Uri.parse(email.ifCondition(!email.startsWith(emailStart)) { "$emailStart$email" })

    val intent = Intent(Intent.ACTION_SENDTO).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        data = fixedEmail
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}

/** 通过 [context] 唤起浏览器应用，打开指定路径 [url] */
fun jumpBrowser(
    url: String,
    chooserTitle: CharSequence? = null,
    context: Context
) {
    context.startActivity(
        Intent.createChooser(
            Intent().apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                action = Intent.ACTION_VIEW
                data = Uri.parse(url)
            },
            chooserTitle
        )
    )
}

/** 通过 [context] 跳转应用详情界面 */
fun jumpAppDetails(context: Context) {
    context.startActivity(Intent().apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        data = Uri.fromParts("package", context.packageName, null)
    })
}