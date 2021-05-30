package cn.wj.android.cashbook.widget.calculator

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.databinding.Observable
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.orElse
import cn.wj.android.cashbook.base.tools.getStringById
import cn.wj.android.cashbook.databinding.LayoutCalculatorBinding

/**
 * 计算器控件
 *
 * > [王杰](mailto:15555650921@163.com) 创建于2021/5/30
 */
class CalculatorView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, @AttrRes defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {

    private val viewModel: CalculatorViewModel by lazy {
        CalculatorViewModel()
    }

    private val symbolZero: String by lazy {
        getStringById(R.string.symbol_calculator_zero, context)
    }

    private val symbolPlus: String by lazy {
        getStringById(R.string.symbol_calculator_plus, context)
    }

    private val symbolMinus: String by lazy {
        getStringById(R.string.symbol_calculator_minus, context)
    }

    private val symbolTimes: String by lazy {
        getStringById(R.string.symbol_calculator_times, context)
    }

    private val symbolDiv: String by lazy {
        getStringById(R.string.symbol_calculator_div, context)
    }

    private val symbolBracketStart: String by lazy {
        getStringById(R.string.symbol_calculator_bracket_start, context)
    }

    private val symbolBracketEnd: String by lazy {
        getStringById(R.string.symbol_calculator_bracket_end, context)
    }

    private val symbolPoint: String by lazy {
        getStringById(R.string.symbol_calculator_point, context)
    }

    init {
        removeAllViews()
        if (isInEditMode) {
            val v = LayoutInflater.from(context).inflate(R.layout.layout_calculator, this, false)
            addView(v, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        } else {
            val binding = LayoutCalculatorBinding.inflate(LayoutInflater.from(context))
            addView(binding.root, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            initView(binding)
        }
    }

    private fun initView(binding: LayoutCalculatorBinding) {
        binding.viewModel = viewModel

    }

    fun bindCalculatorStr(liveData: MutableLiveData<String>) {
        liveData.value = viewModel.calculatorStr.get()
        viewModel.calculatorStr.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                liveData.value = viewModel.calculatorStr.get()
            }
        })
    }

    fun bindCalculatorStr(field: ObservableField<String>) {
        field.set(viewModel.calculatorStr.get())
        viewModel.calculatorStr.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                field.set(viewModel.calculatorStr.get())
            }
        })
    }

    private fun String.isComputeSign(): Boolean {
        return this in arrayOf(symbolPlus, symbolMinus, symbolTimes, symbolDiv)
    }

    private fun String.isSymbol(): Boolean {
        return this in arrayOf(symbolPlus, symbolMinus, symbolTimes, symbolDiv, symbolBracketStart, symbolBracketEnd)
    }

    private fun String.isNumber(): Boolean {
        return !isSymbol() && this != symbolPoint
    }

    inner class CalculatorViewModel {

        /** 计算显示文本 */
        val calculatorStr: ObservableField<String> = ObservableField(symbolZero)

        /** 清除点击 */
        val onClearClick: () -> Unit = {
            // 默认为 0
            calculatorStr.set(symbolZero)
        }

        /** 退格点击 */
        val onBackspaceClick: () -> Unit = {
            val current = calculatorStr.get().orElse(symbolZero)
            calculatorStr.set(
                if (current.length > 1) {
                    // 长度大于 1，移除最后一个
                    current.dropLast(1)
                } else {
                    // 默认为 0
                    symbolZero
                }
            )
        }

        /** 计算符号点击 */
        val onComputeSignClick: (String) -> Unit = { symbol ->
            val current = calculatorStr.get().orElse(symbolZero)
            val last = current.last().toString()
            calculatorStr.set(
                if (last.isComputeSign() || last == symbolPoint) {
                    // 最后一个是计算符号或者小数点，直接替换
                    current.dropLast(1) + symbol
                } else if (last == symbolBracketStart) {
                    // 最后一个是括号开始，不变
                    current
                } else {
                    // 其他情况，直接拼接
                    current + symbol
                }
            )
        }

        /** 数字点击 */
        val onNumberClick: (String) -> Unit = { num ->
            val current = calculatorStr.get().orElse(symbolZero)
            calculatorStr.set(
                when {
                    current == symbolZero -> {
                        // 为 0 时直接替换值
                        num
                    }
                    current.endsWith(symbolBracketEnd) -> {
                        // 以括号结尾，添加乘号
                        current + symbolTimes + num
                    }
                    else -> {
                        // 其他，直接拼接
                        current + num
                    }
                }
            )
        }

        /** 小数点点击 */
        val onPointClick: () -> Unit = {
            val current = calculatorStr.get().orElse(symbolZero)
            val last = current.last().toString()
            calculatorStr.set(
                if (last.isSymbol()) {
                    // 是计算符号或括号，添加 0
                    current + symbolZero + symbolPoint
                } else {
                    // 直接添加
                    current + symbolPoint
                }
            )
        }

        /** 括号点击 */
        val onBracketClick: () -> Unit = {
            val current = calculatorStr.get().orElse(symbolZero)
            val last = current.last().toString()
            val startCount = current.count { it.toString() == symbolBracketStart }
            val endCount = current.count { it.toString() == symbolBracketEnd }
            calculatorStr.set(
                if (last == symbolBracketStart || last.isComputeSign()) {
                    // 是括号开始或是计算符号，继续添加括号开始
                    current + symbolBracketStart
                } else if (last == symbolPoint) {
                    // 小数点
                    if (startCount == endCount) {
                        // 括号已完成匹配，将小数点替换为乘号并添加括号
                        current.dropLast(1) + symbolTimes + symbolBracketStart
                    } else {
                        // 括号不匹配，移除小数点并添加括号
                        current.dropLast(1) + symbolBracketEnd
                    }
                } else {
                    // 其他情况
                    if (startCount == endCount) {
                        // 括号已完成匹配，添加乘号及括号
                        current + symbolTimes + symbolBracketStart
                    } else {
                        // 括号未完成匹配，添加括号结束
                        current + symbolBracketEnd
                    }
                }
            )
        }
    }
}