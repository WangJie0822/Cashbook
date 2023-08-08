@file:Suppress("unused")

package cn.wj.android.cashbook.feature.records.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.haibin.calendarview.Calendar
import com.haibin.calendarview.MonthView

/**
 * 日历月视图
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/7/20
 */
class MonthView(context: Context) : MonthView(context) {

    private val textPaint: Paint by lazy {
        Paint().apply {
            textSize = dip2px(8, context)
            color = Color.BLUE
            isAntiAlias = true
            isFakeBoldText = true
        }
    }

    private val schemaPaint: Paint by lazy {
        Paint().apply {
            textSize = dip2px(8, context)
            color = Color.parseColor("#FFED5353")
            isAntiAlias = true
            isFakeBoldText = true
            style = Paint.Style.FILL
            textAlign = Paint.Align.CENTER
        }
    }

    private val radio = dip2px(7, context)
    private val padding = dip2px(4, context)
    private val schemeBaseLine: Float by lazy {
        val metrics = schemaPaint.fontMetrics
        radio - metrics.descent + (metrics.bottom - metrics.top) / 2 + dip2px(1, context)
    }

    override fun onDrawSelected(
        canvas: Canvas,
        calendar: Calendar,
        x: Int,
        y: Int,
        hasScheme: Boolean
    ): Boolean {
        mSelectedPaint.style = Paint.Style.FILL
        canvas.drawRect(
            x + padding,
            y + padding,
            x + mItemWidth - padding,
            y + mItemHeight - padding,
            mSelectedPaint
        )
        return true
    }

    override fun onDrawScheme(canvas: Canvas, calendar: Calendar, x: Int, y: Int) {
        schemaPaint.color = calendar.schemeColor

        canvas.drawCircle(
            x + mItemWidth - padding - radio / 2,
            y + padding + radio,
            radio,
            schemaPaint
        )

        canvas.drawText(
            calendar.scheme,
            x + mItemWidth - padding - radio / 2 - calendar.scheme.textWidth / 2,
            y + padding + schemeBaseLine, textPaint
        )
    }

    override fun onDrawText(
        canvas: Canvas,
        calendar: Calendar,
        x: Int,
        y: Int,
        hasScheme: Boolean,
        isSelected: Boolean
    ) {
        val cx = x + mItemWidth / 2
        val top = y - mItemHeight / 6

        when {
            isSelected -> {
                canvas.drawText(
                    calendar.day.toString(), cx.toFloat(), mTextBaseLine + top,
                    mSelectTextPaint
                )
                canvas.drawText(
                    calendar.lunar,
                    cx.toFloat(),
                    mTextBaseLine + y + mItemHeight / 10,
                    mSelectedLunarTextPaint
                )
            }

            hasScheme -> {
                canvas.drawText(
                    calendar.day.toString(), cx.toFloat(), mTextBaseLine + top,
                    if (calendar.isCurrentDay) mCurDayTextPaint else if (calendar.isCurrentMonth) mSchemeTextPaint else mOtherMonthTextPaint
                )
                canvas.drawText(
                    calendar.lunar, cx.toFloat(), mTextBaseLine + y + mItemHeight / 10,
                    if (calendar.isCurrentDay) mCurDayLunarTextPaint else mSchemeLunarTextPaint
                )
            }

            else -> {
                canvas.drawText(
                    calendar.day.toString(), cx.toFloat(), mTextBaseLine + top,
                    if (calendar.isCurrentDay) mCurDayTextPaint else if (calendar.isCurrentMonth) mCurMonthTextPaint else mOtherMonthTextPaint
                )
                canvas.drawText(
                    calendar.lunar, cx.toFloat(), mTextBaseLine + y + mItemHeight / 10,
                    if (calendar.isCurrentDay) mCurDayLunarTextPaint else if (calendar.isCurrentMonth) mCurMonthLunarTextPaint else mOtherMonthLunarTextPaint
                )
            }
        }
    }

    private val String.textWidth: Float
        get() = textPaint.measureText(this)

    private fun <N : Number> dip2px(dpValue: N, context: Context): Float {
        val density = context.resources.displayMetrics.density
        return dpValue.toFloat() * density
    }

    private fun getColorById(@ColorRes colorResId: Int, context: Context): Int {
        return ContextCompat.getColor(context, colorResId)
    }
}