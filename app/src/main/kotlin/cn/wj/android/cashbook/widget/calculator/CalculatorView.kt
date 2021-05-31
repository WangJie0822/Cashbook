package cn.wj.android.cashbook.widget.calculator

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.databinding.Observable
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
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

    /** 确认点击 */
    private var onConfirmClick: (() -> Unit)? = null

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

    fun setEqualsBackground(color: Int) {
        viewModel.equalsBackground.set(color)
    }

    fun setOnConfirmClick(onClick: () -> Unit) {
        onConfirmClick = onClick
    }

    inner class CalculatorViewModel {

        /** 计算显示文本 */
        val calculatorStr: ObservableField<String> = ObservableField(SYMBOL_ZERO)

        /** 等号按钮文本 */
        val equalsStr: ObservableField<String> = object : ObservableField<String>(calculatorStr) {
            override fun get(): String {
                val current = calculatorStr.get().orElse(SYMBOL_ZERO)
                return getStringById(
                    if (current == SYMBOL_ZERO || CalculatorUtils.hasComputeSign(current) || !CalculatorUtils.hasNumber(current)) {
                        // 为0或有运算符或没有数字时显示等号
                        R.string.symbol_calculator_equals
                    } else {
                        // 有结果，显示确认
                        R.string.confirm
                    }, context
                )
            }
        }

        /** 等号背景颜色 */
        val equalsBackground: ObservableInt = ObservableInt()

        /** 清除点击 */
        val onClearClick: () -> Unit = {
            // 默认为 0
            calculatorStr.set(SYMBOL_ZERO)
        }

        /** 退格点击 */
        val onBackspaceClick: () -> Unit = {
            if (history.isNotBlank()) {
                calculatorStr.set(history)
                history = ""
            } else {
                val current = calculatorStr.get().orElse(SYMBOL_ZERO)
                calculatorStr.set(
                    if (current.length > 1) {
                        // 长度大于 1，移除最后一个
                        current.dropLast(1)
                    } else {
                        // 默认为 0
                        SYMBOL_ZERO
                    }
                )
            }
        }

        /** 计算符号点击 */
        val onComputeSignClick: (String) -> Unit = { symbol ->
            val current = calculatorStr.get().orElse(SYMBOL_ZERO)
            val last = current.last().toString()
            calculatorStr.set(
                if (CalculatorUtils.hasComputeSign(last) || last == SYMBOL_POINT) {
                    // 最后一个是计算符号或者小数点，直接替换
                    current.dropLast(1) + symbol
                } else if (last == SYMBOL_BRACKET_START) {
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
            val current = calculatorStr.get().orElse(SYMBOL_ZERO)
            calculatorStr.set(
                when {
                    current == SYMBOL_ZERO -> {
                        // 为 0 时直接替换值
                        num
                    }
                    current.endsWith(SYMBOL_BRACKET_END) -> {
                        // 以括号结尾，添加乘号
                        current + SYMBOL_TIMES + num
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
            val current = calculatorStr.get().orElse(SYMBOL_ZERO)
            val last = current.last().toString()
            calculatorStr.set(
                if (CalculatorUtils.hasSymbol(last)) {
                    // 是计算符号或括号，添加 0
                    current + SYMBOL_ZERO + SYMBOL_POINT
                } else {
                    // 直接添加
                    current + SYMBOL_POINT
                }
            )
        }

        /** 括号点击 */
        val onBracketClick: () -> Unit = {
            val current = calculatorStr.get().orElse(SYMBOL_ZERO)
            val last = current.last().toString()
            val startCount = current.count { it.toString() == SYMBOL_BRACKET_START }
            val endCount = current.count { it.toString() == SYMBOL_BRACKET_END }
            calculatorStr.set(
                if (current == SYMBOL_ZERO) {
                    // 默认 0，替换为括号
                    SYMBOL_BRACKET_START
                } else if (last == SYMBOL_BRACKET_START || CalculatorUtils.hasComputeSign(last)) {
                    // 是括号开始或是计算符号，继续添加括号开始
                    current + SYMBOL_BRACKET_START
                } else if (last == SYMBOL_POINT) {
                    // 小数点
                    if (startCount == endCount) {
                        // 括号已完成匹配，将小数点替换为乘号并添加括号
                        current.dropLast(1) + SYMBOL_TIMES + SYMBOL_BRACKET_START
                    } else {
                        // 括号不匹配，移除小数点并添加括号
                        current.dropLast(1) + SYMBOL_BRACKET_END
                    }
                } else {
                    // 其他情况
                    if (startCount == endCount) {
                        // 括号已完成匹配，添加乘号及括号
                        current + SYMBOL_TIMES + SYMBOL_BRACKET_START
                    } else {
                        // 括号未完成匹配，添加括号结束
                        current + SYMBOL_BRACKET_END
                    }
                }
            )
        }

        private var history = ""

        /** 等号点击 */
        val onEqualsClick: () -> Unit = fun() {
            if (equalsStr.get() != SYMBOL_EQUALS) {
                onConfirmClick?.invoke()
                return
            }
            val current = calculatorStr.get().orElse(SYMBOL_ZERO)
            val result = CalculatorUtils.calculatorFromString(current)
            calculatorStr.set(
                if (result.startsWith(SYMBOL_ERROR)) {
                    if (history.isBlank()) {
                        history = current
                    }
                    result.replace(SYMBOL_ERROR, "")
                } else {
                    result
                }
            )
        }
    }
}