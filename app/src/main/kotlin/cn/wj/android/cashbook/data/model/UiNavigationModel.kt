@file:Suppress("unused")

package cn.wj.android.cashbook.data.model

import android.os.Bundle
import cn.wj.android.cashbook.data.constants.ACTIVITY_RESULT_CANCELED

/**
 * 界面跳转数据
 *
 * @param jump 界面跳转数据
 * @param close 界面关闭数据
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/5/16
 */
data class UiNavigationModel(
    val jump: UiJumpModel? = null,
    val close: UiCloseModel? = null
) {
    companion object {
        fun builder(block: Builder.() -> Unit): UiNavigationModel {
            val builder = Builder()
            builder.block()
            return builder.build()
        }
    }

    class Builder {

        private var close: UiCloseModel? = null
        private var jump: UiJumpModel? = null

        /**
         * 关闭界面
         *
         * @param resultCode 返回码，默认 [ACTIVITY_RESULT_CANCELED]
         * @param result 返回数据，默认 `null`
         */
        fun close(
            resultCode: Int = ACTIVITY_RESULT_CANCELED,
            result: Bundle? = null,
            both: Boolean = false
        ): Builder {
            this.close = UiCloseModel(resultCode, result, both)
            return this
        }


        /**
         * 界面跳转
         *
         * @param path 界面路径
         * @param data 跳转数据，默认 `null`
         * @param onArrival 跳转成功回调
         * @param onIntercept 跳转拦截回调
         */
        fun jump(
            path: String,
            data: Bundle? = null,
            onArrival: (() -> Unit)? = null,
            onIntercept: (() -> Unit)? = null
        ): Builder {
            this.jump = UiJumpModel(path, data, onArrival, onIntercept)
            return this
        }

        fun build(): UiNavigationModel {
            return UiNavigationModel(jump, close)
        }
    }
}

/**
 * 界面关闭数据
 *
 * @param resultCode 返回码，默认 [ACTIVITY_RESULT_CANCELED]
 * @param result 返回数据，默认 `null`
 * @param both Dialog 中使用，为 `true` 时同步关闭 Activity
 */
data class UiCloseModel(
    val resultCode: Int = ACTIVITY_RESULT_CANCELED,
    val result: Bundle? = null,
    val both: Boolean = false
)

/**
 * 界面跳转数据
 *
 * @param path 界面路径
 * @param data 跳转数据，默认 `null`
 * @param onArrival 跳转成功回调
 * @param onIntercept 跳转拦截回调
 */
data class UiJumpModel(
    val path: String,
    val data: Bundle? = null,
    val onArrival: (() -> Unit)? = null,
    val onIntercept: (() -> Unit)? = null
)