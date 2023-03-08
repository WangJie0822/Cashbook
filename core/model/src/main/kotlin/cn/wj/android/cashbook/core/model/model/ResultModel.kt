package cn.wj.android.cashbook.core.model.model

sealed class ResultModel {

    open val isSuccess: Boolean = false
    open val isFailure: Boolean = false

    companion object {
        fun <T> success(value: T): Success<T> {
            return Success(value)
        }

        fun success(): Success<Nothing?> {
            return Success(null)
        }

        fun <T : Throwable?> failure(throwable: T): Failure<T> {
            return Failure(Failure.FAILURE_THROWABLE, throwable)
        }

        fun failure(code: Int): Failure<Throwable?> {
            return Failure(code, null)
        }
    }

    class Success<T>(val value: T) : ResultModel() {
        override val isSuccess: Boolean = true
        override val isFailure: Boolean = false
    }

    class Failure<T : Throwable?>(val code: Int, val throwable: T) : ResultModel() {
        override val isSuccess: Boolean = false
        override val isFailure: Boolean = true

        companion object {
            /** 默认错误码 */
            const val FAILURE_THROWABLE = -1687

            /** 编辑记录 - 金额不能为 0 */
            const val FAILURE_EDIT_RECORD_AMOUNT_MUST_NOT_BE_ZERO = -1688

            /** 编辑记录 - 类型不能为空 */
            const val FAILURE_EDIT_RECORD_TYPE_MUST_NOT_BE_NULL = -1689

            /** 编辑记录 - 类型与支出类型不匹配 */
            const val FAILURE_EDIT_RECORD_TYPE_NOT_MATCH_CATEGORY = -1690
        }
    }
}
