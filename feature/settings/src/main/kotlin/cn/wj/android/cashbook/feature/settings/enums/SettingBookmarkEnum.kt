package cn.wj.android.cashbook.feature.settings.enums

/**
 * 关于我们界面事件
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/14
 */
enum class SettingBookmarkEnum {

    /** 无提示 */
    NONE,

    /** 密码不能为空 */
    PASSWORD_MUST_NOT_BLANK,

    /** 两次输入的密码不一致 */
    PASSWORD_CONFIRM_FAILED,

    /** 密码格式错误 */
    PASSWORD_FORMAT_ERROR,

    /** 加密失败 */
    PASSWORD_ENCODE_FAILED,

    /** 密码错误 */
    PASSWORD_WRONG,
}