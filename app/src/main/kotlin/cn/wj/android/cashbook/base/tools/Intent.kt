@file:Suppress("unused")
@file:JvmName("IntentTools")

package cn.wj.android.cashbook.base.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.ifCondition
import cn.wj.android.cashbook.manager.AppManager


/**
 * Intent 跳转相关工具
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/17
 */

/** 通过 [context] 唤起邮件应用，根据标题 [subject] 内容 [text] 新建邮件发送给 [email] */
fun jumpSendEmail(email: String, subject: String = "", text: String = "", context: Context = AppManager.getContext()) {
    fun fixedEmailData(email: String): Uri {
        val emailStart = "mailto:"
        return Uri.parse(email.ifCondition(!email.startsWith(emailStart)) { "$emailStart$email" })
    }
    context.startActivity(Intent(Intent.ACTION_SENDTO).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        data = fixedEmailData(email)
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, text)
    })
}

/** 通过 [context] 唤起浏览器应用，打开指定路径 [url] */
fun jumpBrowser(url: String, context: Context = AppManager.getContext()) {
    context.startActivity(Intent.createChooser(Intent().apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        action = Intent.ACTION_VIEW
        data = Uri.parse(url)
    }, getStringById(R.string.please_select_browser, context)))
}

/** 通过 [context] 跳转应用详情界面 */
fun jumpAppDetails(context: Context = AppManager.getContext()) {
    context.startActivity(Intent().apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        data = Uri.fromParts("package", context.packageName, null)
    })
}