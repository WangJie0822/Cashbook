@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package cn.wj.android.cashbook.widget.picker

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.TextPaint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.widget.Scroller
import cn.wj.android.cashbook.R
import kotlin.math.abs

/**
 * 滚轮视图，可设置是否循环模式
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/6/23
 */
class EasyPickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 文字大小
    private val textSize: Int

    // 颜色，默认Color.BLACK
    private val textColor: Int

    // 文字之间的间隔，默认10dp
    private val textPadding: Int

    // 文字最大放大比例，默认2.0f
    private val textMaxScale: Float

    // 文字最小alpha值，范围0.0f~1.0f，默认0.4f
    private val textMinAlpha: Float

    // 是否循环模式，默认是
    private val isRecycleMode: Boolean

    // 正常状态下最多显示几个文字，默认3（偶数时，边缘的文字会截断）
    private val maxShowNum: Int

    private val textPaint: TextPaint
    private val fm: Paint.FontMetrics

    private val scroller: Scroller
    private var velocityTracker: VelocityTracker? = null
    private val minimumVelocity: Int
    private val maximumVelocity: Int
    private val scaledTouchSlop: Int

    // 数据
    private val dataList = ArrayList<Any>()

    // 中间x坐标
    private var cx: Int = 0

    // 中间y坐标
    private var cy: Int = 0

    // 文字最大宽度
    private var maxTextWidth: Float = 0.toFloat()

    // 文字高度
    private val textHeight: Int

    // 实际内容宽度
    private var contentWidth: Int = 0

    // 实际内容高度
    private var contentHeight: Int = 0

    // 按下时的y坐标
    private var downY: Float = 0.toFloat()

    // 本次滑动的y坐标偏移值
    private var offsetY: Float = 0.toFloat()

    // 在fling之前的offsetY
    private var oldOffsetY: Float = 0.toFloat()

    // 当前选中项
    private var curIndex: Int = 0
    private var offsetIndex: Int = 0

    // 回弹距离
    private var bounceDistance: Float = 0.toFloat()

    // 是否正处于滑动状态
    private var isSliding = false

    private val scrollYVelocity: Int
        get() {
            velocityTracker!!.computeCurrentVelocity(1000, maximumVelocity.toFloat())
            return velocityTracker!!.yVelocity.toInt()
        }

    private var onScrollChangedListener: OnScrollChangedListener? = null

    init {

        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.EasyPickerView, defStyleAttr, 0)
        textSize = a.getDimensionPixelSize(
            R.styleable.EasyPickerView_epvTextSize, TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 16f, resources.displayMetrics
            ).toInt()
        )
        textColor = a.getColor(R.styleable.EasyPickerView_epvTextColor, Color.BLACK)
        textPadding = a.getDimensionPixelSize(
            R.styleable.EasyPickerView_epvTextPadding, TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 20f, resources.displayMetrics
            ).toInt()
        )
        textMaxScale = a.getFloat(R.styleable.EasyPickerView_epvTextMaxScale, 2.0f)
        textMinAlpha = a.getFloat(R.styleable.EasyPickerView_epvTextMinAlpha, 0.4f)
        isRecycleMode = a.getBoolean(R.styleable.EasyPickerView_epvRecycleMode, true)
        maxShowNum = a.getInteger(R.styleable.EasyPickerView_epvMaxShowNum, 3)
        a.recycle()

        textPaint = TextPaint()
        textPaint.color = textColor
        textPaint.textSize = textSize.toFloat()
        textPaint.isAntiAlias = true
        fm = textPaint.fontMetrics
        textHeight = (fm.bottom - fm.top).toInt()

        scroller = Scroller(context)
        minimumVelocity = ViewConfiguration.get(getContext()).scaledMinimumFlingVelocity
        maximumVelocity = ViewConfiguration.get(getContext()).scaledMaximumFlingVelocity
        scaledTouchSlop = ViewConfiguration.get(getContext()).scaledTouchSlop

        if (isInEditMode) {
            for (i in 0 until 10) {
                dataList.add("item$i")
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var mode = MeasureSpec.getMode(widthMeasureSpec)
        var width = MeasureSpec.getSize(widthMeasureSpec)
        contentWidth = (maxTextWidth * textMaxScale + paddingLeft.toFloat() + paddingRight.toFloat()).toInt()
        if (mode != MeasureSpec.EXACTLY) { // wrap_content
            width = contentWidth
        }

        mode = MeasureSpec.getMode(heightMeasureSpec)
        var height = MeasureSpec.getSize(heightMeasureSpec)
        contentHeight = textHeight * maxShowNum + textPadding * maxShowNum
        if (mode != MeasureSpec.EXACTLY) { // wrap_content
            height = contentHeight + paddingTop + paddingBottom
        }

        cx = width / 2
        cy = height / 2

        setMeasuredDimension(width, height)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        parent.requestDisallowInterceptTouchEvent(true)
        return super.dispatchTouchEvent(event)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        addVelocityTracker(event)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (!scroller.isFinished) {
                    scroller.forceFinished(true)
                    finishScroll()
                }
                downY = event.y
            }

            MotionEvent.ACTION_MOVE -> {
                offsetY = event.y - downY
                if (isSliding || abs(offsetY) > scaledTouchSlop) {
                    isSliding = true
                    reDraw()
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val scrollYVelocity = 2 * scrollYVelocity / 3
                if (abs(scrollYVelocity) > minimumVelocity) {
                    oldOffsetY = offsetY
                    scroller.fling(0, 0, 0, scrollYVelocity, 0, 0, -Integer.MAX_VALUE, Integer.MAX_VALUE)
                    invalidate()
                } else {
                    finishScroll()
                }

                // 没有滑动，则判断点击事件
                if (!isSliding) {
                    if (downY < contentHeight / 3) {
                        moveBy(-1)
                    } else if (downY > 2 * contentHeight / 3) {
                        moveBy(1)
                    }
                }

                isSliding = false
                recycleVelocityTracker()
            }
            else -> {
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        if (dataList.size > 0) {
            canvas.clipRect(
                cx - contentWidth / 2,
                cy - contentHeight / 2,
                cx + contentWidth / 2,
                cy + contentHeight / 2
            )

            // 绘制文字，从当前中间项往前、后一共绘制maxShowNum个字
            val size = dataList.size
            val centerPadding = textHeight + textPadding
            val half = maxShowNum / 2 + 1
            for (i in -half..half) {
                var index = curIndex - offsetIndex + i

                if (isRecycleMode) {
                    if (index < 0) {
                        index = (index + 1) % dataList.size + dataList.size - 1
                    } else if (index > dataList.size - 1) {
                        index %= dataList.size
                    }
                }

                if (index in 0 until size) {
                    // 计算每个字的中间y坐标
                    var tempY = cy + i * centerPadding
                    tempY += (offsetY % centerPadding).toInt()

                    // 根据每个字中间y坐标到cy的距离，计算出scale值
                    val scale = 1.0f - 1.0f * abs(tempY - cy) / centerPadding

                    // 根据textMaxScale，计算出tempScale值，即实际text应该放大的倍数，范围 1~textMaxScale
                    var tempScale = scale * (textMaxScale - 1.0f) + 1.0f
                    tempScale = if (tempScale < 1.0f) 1.0f else tempScale

                    // 计算文字alpha值
                    var textAlpha = textMinAlpha
                    if (textMaxScale != 1f) {
                        val tempAlpha = (tempScale - 1) / (textMaxScale - 1)
                        textAlpha = (1 - textMinAlpha) * tempAlpha + textMinAlpha
                    }

                    textPaint.textSize = textSize * tempScale
                    textPaint.alpha = (255 * textAlpha).toInt()

                    // 绘制
                    val tempFm = textPaint.fontMetrics
                    val text = dataList[index].toString()
                    val textWidth = textPaint.measureText(text)
                    canvas.drawText(text, cx - textWidth / 2, tempY - (tempFm.ascent + tempFm.descent) / 2, textPaint)
                }
            }
        }
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            offsetY = oldOffsetY + scroller.currY

            if (!scroller.isFinished) {
                reDraw()
            } else {
                finishScroll()
            }
        }
    }

    private fun addVelocityTracker(event: MotionEvent) {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }

        velocityTracker!!.addMovement(event)
    }

    private fun recycleVelocityTracker() {
        if (velocityTracker != null) {
            velocityTracker!!.recycle()
            velocityTracker = null
        }
    }

    private fun reDraw() {
        // curIndex需要偏移的量
        val i = (offsetY / (textHeight + textPadding)).toInt()
        if (isRecycleMode || curIndex - i >= 0 && curIndex - i < dataList.size) {
            if (offsetIndex != i) {
                offsetIndex = i

                onScrollChangedListener?.onScrollChanged(getNowIndex(-offsetIndex))
            }
            postInvalidate()
        } else {
            finishScroll()
        }
    }

    private fun finishScroll() {
        // 判断结束滑动后应该停留在哪个位置
        val centerPadding = textHeight + textPadding
        val v = offsetY % centerPadding
        if (v > 0.5f * centerPadding) {
            ++offsetIndex
        } else if (v < -0.5f * centerPadding) {
            --offsetIndex
        }

        // 重置curIndex
        curIndex = getNowIndex(-offsetIndex)

        // 计算回弹的距离
        bounceDistance = offsetIndex * centerPadding - offsetY
        offsetY += bounceDistance

        // 更新
        onScrollChangedListener?.onScrollFinished(curIndex)

        // 重绘
        reset()
        postInvalidate()
    }

    private fun getNowIndex(offsetIndex: Int): Int {
        var index = curIndex + offsetIndex
        if (isRecycleMode) {
            if (dataList.isEmpty()) {
                index = 0
            } else {
                if (index < 0) {
                    index = (index + 1) % dataList.size + dataList.size - 1
                } else if (index > dataList.size - 1) {
                    index %= dataList.size
                }
            }
        } else {
            if (index < 0) {
                index = 0
            } else if (index > dataList.size - 1) {
                index = dataList.size - 1
            }
        }
        return index
    }

    private fun reset() {
        offsetY = 0f
        oldOffsetY = 0f
        offsetIndex = 0
        bounceDistance = 0f
    }

    /**
     * 设置要显示的数据
     *
     * @param dataList 要显示的数据
     */
    fun setDataList(dataList: List<Any>) {
        this.dataList.clear()
        this.dataList.addAll(dataList)

        // 更新maxTextWidth
        if (dataList.isNotEmpty()) {
            val size = dataList.size
            for (i in 0 until size) {
                val tempWidth = textPaint.measureText(dataList[i].toString())
                if (tempWidth > maxTextWidth) {
                    maxTextWidth = tempWidth
                }
            }
            curIndex = 0
        }
        requestLayout()
        invalidate()
    }

    /**
     * 获取当前状态下，选中的下标
     *
     * @return 选中的下标
     */
    fun getCurIndex(): Int {
        return getNowIndex(-offsetIndex)
    }

    fun indexOf(item: Any): Int {
        return dataList.indexOfFirst { item.toString() == it.toString() }
    }

    /**
     * 滚动到指定位置
     *
     * @param index 需要滚动到的指定位置
     */
    fun moveTo(index: Int) {
        if (index < 0 || index >= dataList.size || curIndex == index) {
            return
        }

        if (!scroller.isFinished) {
            scroller.forceFinished(true)
        }

        finishScroll()

        val dy: Int
        val centerPadding = textHeight + textPadding
        if (!isRecycleMode) {
            dy = (curIndex - index) * centerPadding
        } else {
            val offsetIndex = curIndex - index
            val d1 = abs(offsetIndex) * centerPadding
            val d2 = (dataList.size - abs(offsetIndex)) * centerPadding

            dy = if (offsetIndex > 0) {
                if (d1 < d2) {
                    d1 // ascent
                } else {
                    -d2 // descent
                }
            } else {
                if (d1 < d2) {
                    -d1 // descent
                } else {
                    d2 // ascent
                }
            }
        }
        scroller.startScroll(0, 0, 0, dy, 500)
        invalidate()
    }

    /**
     * 滚动指定的偏移量
     *
     * @param offsetIndex 指定的偏移量
     */
    fun moveBy(offsetIndex: Int) {
        moveTo(getNowIndex(offsetIndex))
    }

    fun setOnScrollChangedListener(onScrollChangedListener: OnScrollChangedListener) {
        this.onScrollChangedListener = onScrollChangedListener
    }

    fun getCurrentData(): Any {
        return dataList[getCurIndex()]
    }

    /**
     * 滚动发生变化时的回调接口
     */
    interface OnScrollChangedListener {

        fun onScrollChanged(curIndex: Int)

        fun onScrollFinished(curIndex: Int)
    }
}
