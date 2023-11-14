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
@file:JvmName("IntExt")

package cn.wj.android.cashbook.core.common.ext

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import cn.wj.android.cashbook.core.common.tools.getColorById
import cn.wj.android.cashbook.core.common.tools.getColorStateListById
import cn.wj.android.cashbook.core.common.tools.getDrawableById
import cn.wj.android.cashbook.core.common.tools.getStringById

/**
 * Int 相关拓展
 *
 * - 创建时间：2019/11/15
 *
 * @author 王杰
 */

/** String 字符串 */
fun Int.string(context: Context): String = getStringById(this, context)

/** 颜色值 */
fun Int.color(context: Context): Int = getColorById(this, context)

/** 颜色值 */
fun Int.colorStateList(context: Context): ColorStateList? = getColorStateListById(this, context)

/** 图片资源 */
fun Int.drawable(context: Context): Drawable? = getDrawableById(this, context)
