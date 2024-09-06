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

package cn.wj.android.cashbook.core.design.component

import androidx.annotation.IntDef
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.util.Pair
import androidx.fragment.app.FragmentActivity
import cn.wj.android.cashbook.core.common.tools.DATE_FORMAT_DATE
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Locale

private const val NOT_FRAGMENT_ACTIVITY_ERROR_TEXT = "Please show dialog in FragmentActivity scope"

@Composable
fun DateRangePickerDialog(
    onDismissRequest: () -> Unit,
    onPositiveButtonClick: (Pair<Long, Long>) -> Unit,
    onNegativeButtonClick: () -> Unit,
    title: CharSequence? = null,
    positiveButton: CharSequence? = null,
    negativeButton: CharSequence? = null,
    selection: Pair<Long, Long>? = null,
    dateFormat: SimpleDateFormat = SimpleDateFormat(DATE_FORMAT_DATE, Locale.getDefault()),
    @DateInputMode inputMode: Int = DATE_INPUT_MODE_CALENDAR,
) {
    val fm = (LocalContext.current as? FragmentActivity)?.supportFragmentManager
        ?: throw RuntimeException(NOT_FRAGMENT_ACTIVITY_ERROR_TEXT)
    val fixedSelection = selection?.run {
        val fixedFirst = if (null == first) {
            null
        } else {
            first + (1000L * 60L * 60L * 24)
        }
        val fixedSecond = if (null == second) {
            null
        } else {
            second + (1000L * 60L * 60L * 24)
        }
        Pair.create(fixedFirst, fixedSecond)
    }
    val dialog = MaterialDatePicker.Builder.dateRangePicker()
        .setSelection(fixedSelection)
        .setInputMode(inputMode)
        .setTextInputFormat(dateFormat)
        .setTitleText(title)
        .setPositiveButtonText(positiveButton)
        .setNegativeButtonText(negativeButton)
        .build().apply {
            addOnCancelListener { onDismissRequest() }
            addOnPositiveButtonClickListener {
                onPositiveButtonClick(it)
            }
            addOnNegativeButtonClickListener { onNegativeButtonClick() }
        }

    DisposableEffect(dialog) {
        dialog.show(fm, "dateRangePicker")

        onDispose {
            dialog.dismiss()
        }
    }
}

/** 日历样式 */
const val DATE_INPUT_MODE_CALENDAR = MaterialDatePicker.INPUT_MODE_CALENDAR

/** 输入框样式 */
const val DATE_INPUT_MODE_TEXT = MaterialDatePicker.INPUT_MODE_TEXT

@IntDef(value = [DATE_INPUT_MODE_CALENDAR, DATE_INPUT_MODE_TEXT])
@Retention(
    AnnotationRetention.SOURCE,
)
annotation class DateInputMode
