package cn.wj.android.cashbook.data.entity

/**
 * 返回数据包装
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/8/17
 */
data class DataResult<T>(
    val code: Int = RESULT_CODE_SUCCESS,
    val msg: String = "",
    val data: T? = null
) {

    val success: Boolean
        get() = code == RESULT_CODE_SUCCESS

    companion object {
        fun <T> success(data: T? = null): DataResult<T> {
            return DataResult(data = data)
        }

        fun <T> failed(code: Int = RESULT_CODE_FAILED, msg: String = ""): DataResult<T> {
            return DataResult(code, msg, null)
        }
    }
}

/** 返回码 - 成功 */
const val RESULT_CODE_SUCCESS = 0

/** 返回码 - 失败 */
const val RESULT_CODE_FAILED = -1

/** 返回码 - WebDAV 备份失败 */
const val RESULT_CODE_WEBDAV_FAILED = -1001

/** 返回码 - 恢复 未知文件 */
const val RESULT_CODE_RECOVERY_UNKNOWN_FILE = -1002

/** 返回码 - 恢复 渠道错误 */
const val RESULT_CODE_RECOVERY_CHANNEL_ERROR = -1003

/** 返回码 - 恢复 路径错误 */
const val RESULT_CODE_RECOVERY_PATH_ERROR = -1004