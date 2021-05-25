package cn.wj.android.cashbook.widget.recyclerview.layoutmanager

import android.graphics.Rect
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView 流式布局 LayoutManager
 *
 * @author 王杰
 */
class FlowLayoutManager : RecyclerView.LayoutManager() {

    /** 总高度 */
    private var totalHeight = 0

    /** 控件尺寸 */
    private var tempWidth = 0
    private var tempHeight = 0

    private var left = 0
    private var top = 0
    private var right = 0

    /** 最大容器宽度 */
    private var usedMaxWidth = 0

    /** 竖直方向偏移量 */
    private var verticalScrollOffset = 0

    /** 单行数据 */
    private var row = Row()

    /** 行数据列表 */
    private val rowList = arrayListOf<Row>()

    /** 所有 Item 位置信息 */
    private val rectArray = SparseArray<Rect>()

    override fun isAutoMeasureEnabled(): Boolean {
        // 设置主动测量，适配 wrap_content
        return true
    }

    override fun canScrollVertically(): Boolean {
        // 允许垂直滚动
        return true
    }

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        // 初始化
        totalHeight = 0
        var currentLineTop = top
        var currentLineWidth = 0
        var itemLeft: Int
        var itemTop: Int
        var maxHeightItem = 0
        row = Row()
        rowList.clear()
        rectArray.clear()
        removeAllViews()

        if (itemCount == 0) {
            detachAndScrapAttachedViews(recycler)
            verticalScrollOffset = 0
            return
        }
        if (childCount == 0 && state.isPreLayout) {
            return
        }

        detachAndScrapAttachedViews(recycler)

        if (childCount == 0) {
            tempWidth = width
            tempHeight = height
            left = paddingLeft
            right = paddingRight
            top = paddingTop
            usedMaxWidth = tempWidth - left - right
        }

        for (i in 0 until itemCount) {
            val childAt = recycler.getViewForPosition(i)
            if (childAt.visibility == View.GONE) {
                continue
            }
            val lp = childAt.layoutParams as RecyclerView.LayoutParams
            measureChild(childAt, 0, 0)
            val childWith = getDecoratedMeasuredWidth(childAt) + lp.leftMargin + lp.rightMargin
            val childHeight = getDecoratedMeasuredHeight(childAt) + lp.topMargin + lp.bottomMargin
            if (currentLineWidth + childWith <= usedMaxWidth) {
                // 加上当前 Item 宽度小于最大宽度，无需换行
                itemLeft = left + currentLineWidth
                itemTop = currentLineTop
                val rect = rectArray.get(i) ?: Rect()
                rect.set(itemLeft, itemTop, itemLeft + childWith, itemTop + childHeight)
                rectArray.put(i, rect)
                currentLineWidth += childWith
                maxHeightItem = maxHeightItem.coerceAtLeast(childHeight)
                row.items.add(Item(childHeight, childAt, rect))
                row.currentTop = currentLineTop
                row.maxHeight = maxHeightItem
            } else {
                // 换行
                formatAboveRow()
                currentLineTop += maxHeightItem
                totalHeight += maxHeightItem
                itemTop = currentLineTop
                itemLeft = left
                val rect = rectArray.get(i) ?: Rect()
                rect.set(itemLeft, itemTop, itemLeft + childWith, itemTop + childHeight)
                rectArray.put(i, rect)
                currentLineWidth = childWith
                maxHeightItem = childHeight
                row.items.add(Item(childHeight, childAt, rect))
                row.currentTop = currentLineTop
                row.maxHeight = maxHeightItem
            }
            if (i == itemCount - 1) {
                formatAboveRow()
                totalHeight += maxHeightItem
            }
        }
        totalHeight = totalHeight.coerceAtLeast(getVerticalSpace())
        fillLayout(state)
    }

    override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State): Int {
        // 实际滑动距离
        var travel = dy
        if (verticalScrollOffset + dy < 0) {
            // 到顶后不能继续向上滑动
            travel = -verticalScrollOffset
        } else if (verticalScrollOffset + dy > totalHeight - getVerticalSpace()) {
            // 到底后不能继续向下滑动
            travel = totalHeight - getVerticalSpace() - verticalScrollOffset
        }

        // 偏移量计算
        verticalScrollOffset += travel

        // 移动 item
        offsetChildrenVertical(-travel)
        fillLayout(state)
        return travel
    }

    /**
     * 格式化每行，居中显示
     */
    private fun formatAboveRow() {
        val items = row.items
        for (i in 0 until items.size) {
            val item = items[i]
            val view = item.view
            val position = getPosition(view)
            if (rectArray.get(position).top < row.currentTop + (row.maxHeight - items[i].useHeight) / 2) {
                val rect = rectArray.get(position) ?: Rect()
                rect.set(rectArray.get(position).left,
                        (row.currentTop + (row.maxHeight - items[i].useHeight) / 2),
                        rectArray.get(position).right,
                        (row.currentTop + (row.maxHeight - items[i].useHeight) / 2 + getDecoratedMeasuredHeight(view)))
                rectArray.put(position, rect)
                items[i] = item.copy(rect = rect)
            }
        }
        row.items = items
        rowList.add(row)
        row = Row()
    }

    private fun getVerticalSpace(): Int {
        return height - paddingBottom - paddingTop
    }

    /**
     * 对出现在屏幕上的 item 进行展示，超出屏幕的回收到缓存
     */
    private fun fillLayout(state: RecyclerView.State) {
        if (itemCount == 0 || state.isPreLayout) {
            return
        }

        for (l in 0 until rowList.size) {
            val row = rowList[l]
            row.currentTop
            val items = row.items
            for (i in 0 until items.size) {
                val scrap = items[i].view
                addView(scrap)
                measureChild(scrap, 0, 0)
                val rect = items[i].rect
                layoutDecoratedWithMargins(scrap, rect.left, rect.top - verticalScrollOffset, rect.right, rect.bottom - verticalScrollOffset)
            }
        }
    }

}

/**
 * Item 数据实体类
 *
 * @param useHeight 所需高度
 * @param view 对应 [View] 对象
 * @param rect 对应 [Rect] 对象
 */
data class Item(
        val useHeight: Int,
        val view: View,
        val rect: Rect
)

/**
 * 行数据实体类
 *
 * @param currentTop 顶部位置
 * @param maxHeight 所需最大高度
 * @param items [Item] 列表
 */
data class Row(
        var currentTop: Int = 0,
        var maxHeight: Int = 0,
        var items: ArrayList<Item> = arrayListOf()
)