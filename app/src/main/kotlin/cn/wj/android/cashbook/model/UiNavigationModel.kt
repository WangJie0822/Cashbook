@file:Suppress("unused")

package cn.wj.android.cashbook.model

import android.app.Activity
import android.os.Bundle

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
        /**
         * 关闭界面
         *
         * @param resultCode 返回码，默认 [Activity.RESULT_OK]
         * @param result 返回数据，默认 `null`
         */
        fun close(
            resultCode: Int = Activity.RESULT_CANCELED,
            result: Bundle? = null
        ): UiNavigationModel {
            return UiNavigationModel(
                jump = null,
                close = UiCloseModel(resultCode, result)
            )
        }

        /**
         * 界面跳转
         *
         * @param path 界面路径
         * @param data 跳转数据，默认 `null`
         */
        fun jump(path: String, data: Bundle? = null): UiNavigationModel {
            return UiNavigationModel(
                jump = UiJumpModel(path, data),
                close = null
            )
        }
    }
}

/**
 * 界面关闭数据
 *
 * @param resultCode 返回码，默认 [Activity.RESULT_OK]
 * @param result 返回数据，默认 `null`
 */
data class UiCloseModel(
    var resultCode: Int = Activity.RESULT_CANCELED,
    var result: Bundle? = null
)

/**
 * 界面跳转数据
 *
 * @param path 界面路径
 * @param data 跳转数据，默认 `null`
 */
data class UiJumpModel(
    val path: String,
    val data: Bundle? = null
)