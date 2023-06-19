package cn.wj.android.cashbook.core.common

/** 正则 - 金额，正负小数、整数 */
const val PATTERN_SIGN_MONEY = "(-?)|(-?\\d+[.]?\\d?|(-?\\d+))"

/** 密码校验正则 必须包含大小写字母及数字，大于8位 */
const val PASSWORD_REGEX =
    "^(?=.*[0-9])(?=.*[A-Z])(?=.*[a-z])[0-9a-zA-Z!@#\$%^&*,\\\\._]{8,24}\$"