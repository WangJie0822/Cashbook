@file:Suppress("unused")
@file:JvmName("UriExt")

package cn.wj.android.cashbook.base.ext.base

import android.net.Uri

fun Uri.isContentScheme() = this.scheme == "content"