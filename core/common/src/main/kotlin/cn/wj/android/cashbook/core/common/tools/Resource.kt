/*
 * Copyright 2021 The Cashbook Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("unused")
@file:JvmName("ResourceTools")

package cn.wj.android.cashbook.core.common.tools

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.Properties

/* ----------------------------------------------------------------------------------------- */
/* |                                        资源相关                                        | */
/* ----------------------------------------------------------------------------------------- */

/**
 * 根据资源id[colorResId] 获取颜色值[Int]
 * > [context]
 */
@ColorInt
fun getColorById(@ColorRes colorResId: Int, context: Context): Int {
    return ContextCompat.getColor(context, colorResId)
}

/**
 * 根据资源id[colorResId] 获取颜色值[Int]
 * > [context]
 */
fun getColorStateListById(
    @ColorRes colorResId: Int,
    context: Context,
): ColorStateList? {
    return ContextCompat.getColorStateList(context, colorResId)
}

/**
 * 根据资源id[resId] 获取字符串[String]
 * > [context]
 */
fun getStringById(@StringRes resId: Int, context: Context): String {
    return context.getString(resId)
}

/**
 * 根据资源id[resId] 获取 [Drawable]
 * > [context]
 */
fun getDrawableById(
    @DrawableRes resId: Int,
    context: Context,
): Drawable? {
    return ContextCompat.getDrawable(context, resId)
}

/**
 * 根据资源id[resId] 获取 尺寸数值[Float]，单位**px**
 * > [context]
 */
fun getFloatDimensionById(@DimenRes resId: Int, context: Context): Float {
    return context.resources.getDimension(resId)
}

/**
 * 根据资源id[resId] 获取 尺寸数值[Int]，单位**px**
 * > [context]
 */
fun getIntDimensionById(@DimenRes resId: Int, context: Context): Int {
    return context.resources.getDimensionPixelOffset(resId)
}

/**
 * 根据资源id字符串[idStr]、资源文件夹名称[defType]，获取资源id[Int]
 * > [context]
 */
@SuppressLint("DiscouragedApi")
fun getIdByString(idStr: String, defType: String, context: Context): Int {
    return context.resources.getIdentifier(idStr, defType, context.packageName)
}

/** 默认字体规模 */
const val RESOURCE_DEFAULT_FONT_SCALE = 1f

/**
 * 修正 Resources
 * - 使得应用文字大小不跟随系统
 *
 * @param resource Activity 的 Resources 对象
 * @param context [Context] 对象
 *
 * @return 修正后的 Resources 对象
 *
 * ```
 * override fun getResources(): Resources {
 *     return fixFontScaleResources(super.getResources(), applicationContext)
 * }
 * ```
 */
@JvmOverloads
fun fixFontScaleResources(resource: Resources?, context: Context? = null): Resources? {
    return if (resource == null) {
        null
    } else {
        val config = resource.configuration
        if (config.fontScale != RESOURCE_DEFAULT_FONT_SCALE) {
            // 不是默认字体规模，修复
            config.fontScale = RESOURCE_DEFAULT_FONT_SCALE
            @Suppress("DEPRECATION")
            resource.updateConfiguration(config, resource.displayMetrics)
            context?.createConfigurationContext(config)
        }
        resource
    }
}

/**
 * 从 Assets 中打开文件
 *
 * @param fileName 文件名
 * @param context [Context] 对象
 *
 * @return 文件输入流
 */
fun getAssetsStreamByName(
    fileName: String,
    context: Context,
): InputStream? {
    return try {
        context.assets.open(fileName)
    } catch (e: IOException) {
        null
    }
}

/**
 * 根据文件名[fileName] 从 Assets 读取字符串
 * > [context]
 */
fun getAssetsStringByName(fileName: String, context: Context): String? {
    return try {
        val br = BufferedReader(InputStreamReader(getAssetsStreamByName(fileName, context)))
        val sb = StringBuilder()
        var line: String?
        while (true) {
            line = br.readLine()
            if (line != null) {
                sb.append(line)
            } else {
                break
            }
        }
        sb.toString()
    } catch (e: IOException) {
        null
    }
}

/**
 * 根据Raw资源id[rawResId]读取 Raw 流
 * > [context]
 */
fun getRawStreamById(
    @RawRes rawResId: Int,
    context: Context,
): InputStream? {
    return try {
        context.resources.openRawResource(rawResId)
    } catch (e: Resources.NotFoundException) {
        null
    }
}

/**
 * 根据Raw资源id[rawResId]、关键字[key]读取 Raw资源中的文本
 * > [context]
 *
 * > [defaultValue] 获取失败的默认值 默认`""`
 */
@JvmOverloads
fun getRawValue(
    @RawRes rawResId: Int,
    key: String,
    context: Context,
    defaultValue: String = "",
): String {
    val rawStream = getRawStreamById(rawResId, context)
    var result = defaultValue
    if (null != rawStream) {
        try {
            val pro = Properties().apply { load(rawStream) }
            result = pro.getProperty(key, defaultValue)
        } catch (e: IOException) {
            funLogger("Resource").e(e, "catch")
        } finally {
            try {
                rawStream.close()
            } catch (e: IOException) {
                funLogger("Resource").e(e, "close")
            }
        }
    }
    return result
}
