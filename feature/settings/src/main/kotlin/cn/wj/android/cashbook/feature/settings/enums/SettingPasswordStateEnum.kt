package cn.wj.android.cashbook.feature.settings.enums

/**
 * 密码相关操作状态
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/14
 */
enum class SettingPasswordStateEnum {

    /** 成功 */
    SUCCESS,

    /** 加密失败 */
    PASSWORD_ENCODE_FAILED,

    /** 解密失败 */
    PASSWORD_DECODE_FAILED,

    /** 密码错误 */
    PASSWORD_WRONG,
}