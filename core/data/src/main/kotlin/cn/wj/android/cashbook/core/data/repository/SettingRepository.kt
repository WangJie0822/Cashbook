package cn.wj.android.cashbook.core.data.repository

import cn.wj.android.cashbook.core.model.entity.UpdateInfoEntity
import cn.wj.android.cashbook.core.model.enums.DarkModeEnum
import cn.wj.android.cashbook.core.model.model.AppDataModel
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

/**
 * 设置相关数据仓库
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/6/14
 */
interface SettingRepository {

    /** 应用配置信息数据源 */
    val appDataMode: Flow<AppDataModel>

    /** 更新使用 github 配置 */
    suspend fun updateUseGithub(
        useGithub: Boolean,
        coroutineContext: CoroutineContext = Dispatchers.IO
    )

    /** 更新自动检查更新配置 */
    suspend fun updateAutoCheckUpdate(
        autoCheckUpdate: Boolean,
        coroutineContext: CoroutineContext = Dispatchers.IO
    )

    /** 更新忽略更新版本配置 */
    suspend fun updateIgnoreUpdateVersion(
        ignoreUpdateVersion: String,
        coroutineContext: CoroutineContext = Dispatchers.IO
    )

    /** 更新是否允许流量下载配置 */
    suspend fun updateMobileNetworkDownloadEnable(
        mobileNetworkDownloadEnable: Boolean,
        coroutineContext: CoroutineContext = Dispatchers.IO
    )

    /** 更新启动时是否需要安全验证配置 */
    suspend fun updateNeedSecurityVerificationWhenLaunch(
        needSecurityVerificationWhenLaunch: Boolean,
        coroutineContext: CoroutineContext = Dispatchers.IO
    )

    /** 更新是否允许指纹验证配置 */
    suspend fun updateEnableFingerprintVerification(
        enableFingerprintVerification: Boolean,
        coroutineContext: CoroutineContext = Dispatchers.IO
    )

    /** 更新密码加密向量 */
    suspend fun updatePasswordIv(
        iv: String,
        coroutineContext: CoroutineContext = Dispatchers.IO
    )

    /** 更新指纹加密向量 */
    suspend fun updateFingerprintIv(
        iv: String,
        coroutineContext: CoroutineContext = Dispatchers.IO
    )

    suspend fun updatePasswordInfo(
        passwordInfo: String,
        coroutineContext: CoroutineContext = Dispatchers.IO
    )

    suspend fun updateFingerprintPasswordInfo(
        fingerprintPasswordInfo: String,
        coroutineContext: CoroutineContext = Dispatchers.IO
    )

    suspend fun updateDarkMode(
        darkModeEnum: DarkModeEnum,
        coroutineContext: CoroutineContext = Dispatchers.IO
    )

    suspend fun updateDynamicColor(
        dynamicColor: Boolean,
        coroutineContext: CoroutineContext = Dispatchers.IO
    )

    /** 检查更新 */
    suspend fun checkUpdate(
        coroutineContext: CoroutineContext = Dispatchers.IO
    ): UpdateInfoEntity
}