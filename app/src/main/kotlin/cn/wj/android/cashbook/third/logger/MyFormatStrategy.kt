@file:Suppress("unused")

package cn.wj.android.cashbook.third.logger

import android.util.Log
import com.orhanobut.logger.FormatStrategy
import com.orhanobut.logger.LogStrategy
import com.orhanobut.logger.LogcatLogStrategy
import com.orhanobut.logger.Logger

/**
 * 日志打印格式化
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/4/26
 */
class MyFormatStrategy private constructor(builder: Builder) : FormatStrategy {

    private val methodCount = builder.methodCount
    private val methodOffset = builder.methodOffset
    private val showThreadInfo = builder.showThreadInfo

    private val logStrategy: LogStrategy? = builder.logStrategy

    private val tag: String? = builder.tag

    private val borderPriority: Int = builder.borderPriority
    private val headerPriority: Int = builder.headerPriority

    override fun log(priority: Int, onceOnlyTag: String?, message: String) {
        val tag = formatTag(onceOnlyTag)

        if (priority >= borderPriority || (headerPriority <= borderPriority && priority >= headerPriority)) {
            logTopBorder(priority, tag)
        }

        if (priority >= headerPriority) {
            logHeaderContent(priority, tag, methodCount)
        }

        //get bytes of message with system's default charset (which is UTF-8 for Android)

        //get bytes of message with system's default charset (which is UTF-8 for Android)
        val bytes = message.toByteArray()
        val length = bytes.size
        if (length <= CHUNK_SIZE) {
            if (methodCount > 0 && priority >= headerPriority) {
                logDivider(priority, tag)
            }
            logContent(priority, tag, message)
            if (priority >= borderPriority || (headerPriority <= borderPriority && priority >= headerPriority)) {
                logBottomBorder(priority, tag)
            }
            return
        }

        if (methodCount > 0 && priority >= headerPriority) {
            logDivider(priority, tag)
        }

        var i = 0
        while (i < length) {
            val count = (length - i).coerceAtMost(CHUNK_SIZE)
            //create a new String with system's default charset (which is UTF-8 for Android)
            logContent(priority, tag, String(bytes, i, count))
            i += CHUNK_SIZE
        }

        logBottomBorder(priority, tag)
    }

    private fun logTopBorder(priority: Int, tag: String?) {
        logChunk(priority, tag, TOP_BORDER)
    }

    private fun logHeaderContent(priority: Int, tag: String?, methodCount: Int) {
        var methodCountVar = methodCount
        val trace = Thread.currentThread().stackTrace
        if (showThreadInfo) {
            logChunk(
                priority,
                tag,
                HORIZONTAL_LINE.toString() + " Thread: " + Thread.currentThread().name
            )
            logDivider(priority, tag)
        }
        var level = ""
        val stackOffset = getStackOffset(trace) + methodOffset

        //corresponding method count with the current stack may exceeds the stack trace. Trims the count
        if (methodCountVar + stackOffset > trace.size) {
            methodCountVar = trace.size - stackOffset - 1
        }
        for (i in methodCountVar downTo 1) {
            val stackIndex = i + stackOffset
            if (stackIndex >= trace.size) {
                continue
            }
            val builder = StringBuilder()
            builder.append(HORIZONTAL_LINE)
                .append(' ')
                .append(level)
                .append(getSimpleClassName(trace[stackIndex].className))
                .append(".")
                .append(trace[stackIndex].methodName)
                .append(" ")
                .append(" (")
                .append(trace[stackIndex].fileName)
                .append(":")
                .append(trace[stackIndex].lineNumber)
                .append(")")
            level += "   "
            logChunk(priority, tag, builder.toString())
        }
    }

    private fun logBottomBorder(priority: Int, tag: String?) {
        logChunk(priority, tag, BOTTOM_BORDER)
    }

    private fun logDivider(priority: Int, tag: String?) {
        logChunk(priority, tag, MIDDLE_BORDER)
    }

    private fun logContent(priority: Int, tag: String?, chunk: String) {
        val lines = chunk.split(System.getProperty("line.separator").orEmpty()).toTypedArray()
        for (line in lines) {
            val printChunk = if (priority >= borderPriority || (headerPriority <= borderPriority && priority >= headerPriority)) {
                "$HORIZONTAL_LINE $line"
            } else {
                line
            }
            logChunk(priority, tag, printChunk)
        }
    }

    private fun logChunk(priority: Int, tag: String?, chunk: String) {
        logStrategy!!.log(priority, tag, chunk)
    }

    private fun getSimpleClassName(name: String): String {
        val lastIndex = name.lastIndexOf(".")
        return name.substring(lastIndex + 1)
    }

    /**
     * Determines the starting index of the stack trace, after method calls made by this class.
     *
     * @param trace the stack trace
     * @return the stack offset
     */
    private fun getStackOffset(trace: Array<StackTraceElement>): Int {
        var i = MIN_STACK_OFFSET
        while (i < trace.size) {
            val e = trace[i]
            val name = e.className
            val loggerPrinterClass = Class.forName("com.orhanobut.logger.LoggerPrinter")
            if (name != loggerPrinterClass.name && name != Logger::class.java.name) {
                return --i
            }
            i++
        }
        return -1
    }

    private fun formatTag(tag: String?): String? {
        return if (!tag.isNullOrBlank() && this.tag != tag) {
            this.tag + "-" + tag
        } else {
            this.tag
        }
    }

    class Builder internal constructor() {
        internal var methodCount = 2
        internal var methodOffset = 0
        internal var showThreadInfo = true

        internal var logStrategy: LogStrategy? = null

        internal var tag: String? = "PRETTY_LOGGER"

        internal var borderPriority = Log.DEBUG
        internal var headerPriority = Log.DEBUG

        fun methodCount(`val`: Int): Builder {
            methodCount = `val`
            return this
        }

        fun methodOffset(`val`: Int): Builder {
            methodOffset = `val`
            return this
        }

        fun showThreadInfo(`val`: Boolean): Builder {
            showThreadInfo = `val`
            return this
        }

        fun logStrategy(`val`: LogStrategy?): Builder {
            logStrategy = `val`
            return this
        }

        fun headerPriority(`val`: Int): Builder {
            headerPriority = `val`
            return this
        }

        fun borderPriority(`val`: Int): Builder {
            borderPriority = `val`
            return this
        }

        fun tag(tag: String?): Builder {
            this.tag = tag
            return this
        }

        fun build(): MyFormatStrategy {
            if (logStrategy == null) {
                logStrategy = LogcatLogStrategy()
            }
            return MyFormatStrategy(this)
        }
    }

    companion object {
        private const val CHUNK_SIZE = 4000

        /**
         * The minimum stack trace index, starts at this class after two native calls.
         */
        private const val MIN_STACK_OFFSET = 5

        /**
         * Drawing toolbox
         */
        private const val TOP_LEFT_CORNER = '┌'
        private const val BOTTOM_LEFT_CORNER = '└'
        private const val MIDDLE_CORNER = '├'
        private const val HORIZONTAL_LINE = '│'
        private const val DOUBLE_DIVIDER =
            "────────────────────────────────────────────────────────"
        private const val SINGLE_DIVIDER =
            "┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄"
        private const val TOP_BORDER = TOP_LEFT_CORNER.toString() + DOUBLE_DIVIDER + DOUBLE_DIVIDER
        private const val BOTTOM_BORDER =
            BOTTOM_LEFT_CORNER.toString() + DOUBLE_DIVIDER + DOUBLE_DIVIDER
        private const val MIDDLE_BORDER = MIDDLE_CORNER.toString() + SINGLE_DIVIDER + SINGLE_DIVIDER

        fun newBuilder() = Builder()
    }
}