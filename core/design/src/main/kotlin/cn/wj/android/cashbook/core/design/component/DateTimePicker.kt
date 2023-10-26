package cn.wj.android.cashbook.core.design.component

import androidx.annotation.IntDef
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.util.Pair
import androidx.fragment.app.FragmentActivity
import cn.wj.android.cashbook.core.common.tools.DATE_FORMAT_DATE
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val NOT_FRAGMENT_ACTIVITY_ERROR_TEXT = "Please show dialog in FragmentActivity scope"

/**
 * 对 **Material** 中组件 [MaterialDatePicker] 进行封装，使用 **Compose** 方式实现
 * > 必须在 [FragmentActivity] 周期内调用，否则会抛出异常
 *
 * @param onDismissRequest 用户点击返回按键或弹窗以外区域时回调
 * @param onPositiveButtonClick 确认按钮点击回调
 * @param onNegativeButtonClick 取消按钮点击回调
 * @param title 标题文本
 * @param positiveButton 确认按钮文本
 * @param negativeButton 取消按钮文本
 * @param selection 指定当前选中时间
 * @param dateFormat 指定输入框样式中日期格式，确认选择时也会以这个格式返回
 * @param inputMode 默认弹窗样式
 */
@Composable
fun DatePickerDialog(
    onDismissRequest: () -> Unit,
    onPositiveButtonClick: (Long) -> Unit,
    title: CharSequence? = null,
    positiveButton: CharSequence? = null,
    negativeButton: CharSequence? = null,
    onNegativeButtonClick: () -> Unit = {},
    selection: Long? = null,
    dateFormat: SimpleDateFormat = SimpleDateFormat(DATE_FORMAT_DATE, Locale.getDefault()),
    @DateInputMode inputMode: Int = DATE_INPUT_MODE_CALENDAR,
) {
    val fm = (LocalContext.current as? FragmentActivity)?.supportFragmentManager
        ?: throw RuntimeException(NOT_FRAGMENT_ACTIVITY_ERROR_TEXT)
    val dialog = MaterialDatePicker.Builder.datePicker()
        .setSelection(selection)
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
        dialog.show(fm, "datePicker")

        onDispose {
            dialog.dismiss()
        }
    }
}

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
    val dialog = MaterialDatePicker.Builder.dateRangePicker()
        .setSelection(selection)
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

/**
 * 对 **Material** 中组件 [MaterialTimePicker] 进行封装，使用 **Compose** 方式实现
 * > 必须在 [FragmentActivity] 周期内调用，否则会抛出异常
 *
 * @param onDismissRequest 用户点击返回按键或弹窗以外区域时回调
 * @param onPositiveButtonClick 确认按钮点击回调
 * @param onNegativeButtonClick 取消按钮点击回调
 * @param title 标题文本
 * @param positiveButton 确认按钮文本
 * @param negativeButton 取消按钮文本
 * @param format 指定时间格式，12小时 or 24小时
 * @param selection 指定当前选中时间
 * @param dateFormat 指定时间格式，确认选择时会以这个格式返回
 * @param inputMode 默认弹窗样式
 */
@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    onPositiveButtonClick: (String) -> Unit,
    onNegativeButtonClick: () -> Unit,
    title: CharSequence? = null,
    positiveButton: CharSequence? = null,
    negativeButton: CharSequence? = null,
    @PickerTimeFormat format: Int = TIME_FORMAT_CLOCK_24H,
    selection: Long? = null,
    dateFormat: SimpleDateFormat = SimpleDateFormat("HH:mm", Locale.getDefault()),
    @TimeInputMode inputMode: Int = TIME_INPUT_MODE_CLOCK,
) {
    val fm = (LocalContext.current as? FragmentActivity)?.supportFragmentManager
        ?: throw RuntimeException(NOT_FRAGMENT_ACTIVITY_ERROR_TEXT)
    val time = dateFormat.format(selection ?: System.currentTimeMillis())
    val dialog = MaterialTimePicker.Builder()
        .setTitleText(title)
        .setPositiveButtonText(positiveButton)
        .setNegativeButtonText(negativeButton)
        .setTimeFormat(format)
        .setInputMode(inputMode)
        .setHour(time.split(":").first().toIntOrNull() ?: 0)
        .setMinute(time.split(":").last().toIntOrNull() ?: 0)
        .build().apply {
            addOnCancelListener { onDismissRequest() }
            addOnPositiveButtonClickListener {
                val defFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val date = defFormat.parse("$hour:$minute") ?: Date()
                onPositiveButtonClick(dateFormat.format(date))
            }
            addOnNegativeButtonClickListener { onNegativeButtonClick() }
        }



    DisposableEffect(dialog) {
        dialog.show(fm, "timePicker")

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
    AnnotationRetention.SOURCE
)
annotation class DateInputMode

/** 时钟样式 */
const val TIME_INPUT_MODE_CLOCK = MaterialTimePicker.INPUT_MODE_CLOCK

/** 输入框样式 */
const val TIME_INPUT_MODE_KEYBOARD = MaterialTimePicker.INPUT_MODE_KEYBOARD

@IntDef(value = [TIME_INPUT_MODE_CLOCK, TIME_INPUT_MODE_KEYBOARD])
@Retention(
    AnnotationRetention.SOURCE
)
internal annotation class TimeInputMode

/** 12 小时制 */
const val TIME_FORMAT_CLOCK_12H = TimeFormat.CLOCK_12H

/** 24 小时制 */
const val TIME_FORMAT_CLOCK_24H = TimeFormat.CLOCK_24H

@IntDef(value = [TIME_FORMAT_CLOCK_12H, TIME_FORMAT_CLOCK_24H])
@Retention(
    AnnotationRetention.SOURCE
)
internal annotation class PickerTimeFormat