@file:Suppress("unused")
@file:JvmName("UriExt")

package cn.wj.android.cashbook.base.ext

import android.net.Uri

fun Uri.isContentScheme() = this.scheme == "content"