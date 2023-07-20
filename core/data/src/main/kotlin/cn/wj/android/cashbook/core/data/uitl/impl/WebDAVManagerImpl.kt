package cn.wj.android.cashbook.core.data.uitl.impl

import cn.wj.android.cashbook.core.common.BACKUP_DIR_NAME
import cn.wj.android.cashbook.core.common.annotation.CashbookDispatchers
import cn.wj.android.cashbook.core.common.annotation.Dispatcher
import cn.wj.android.cashbook.core.common.model.dataVersion
import cn.wj.android.cashbook.core.common.model.updateVersion
import cn.wj.android.cashbook.core.data.repository.SettingRepository
import cn.wj.android.cashbook.core.data.uitl.WebDAVManager
import com.github.sardine.Sardine
import com.github.sardine.SardineFactory
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

class WebDAVManagerImpl @Inject constructor(
    settingRepository: SettingRepository,
    @Dispatcher(CashbookDispatchers.IO) private val coroutineContext: CoroutineContext,
) : WebDAVManager {

    private val dataVersion = dataVersion()

    override val isConnected: Flow<Boolean> =
        combine(dataVersion, settingRepository.appDataMode) { _, appDataModel ->
            checkConnectedStatus(
                appDataModel.webDAVDomain,
                appDataModel.webDAVAccount,
                appDataModel.webDAVPassword
            )
        }

    override fun requestConnected() {
        dataVersion.updateVersion()
    }

    private suspend fun checkConnectedStatus(
        domain: String,
        account: String,
        password: String
    ): Boolean = withContext(coroutineContext) {
        withCredentials(domain, account, password) { root ->
            exists(root) || createDirectory(root)
        }
    }

    private suspend fun <T> withCredentials(
        domain: String,
        account: String,
        password: String,
        block: suspend Sardine.(String) -> T
    ): T = withContext(coroutineContext) {
        val sardine = SardineFactory.begin(account, password)
        sardine.block(domain.backupPath)
    }

    private val String.backupPath: String
        get() {
            val url = URL("$this${if (this.endsWith("/")) "" else "/"}$BACKUP_DIR_NAME/")
            val raw = url.toString().replace("davs://", "https://").replace("dav://", "http://")
            return URLEncoder.encode(raw, "UTF-8")
                .replace("\\+".toRegex(), "%20")
                .replace("%3A".toRegex(), ":")
                .replace("%2F".toRegex(), "/")
        }

}