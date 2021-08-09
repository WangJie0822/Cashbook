package cn.wj.android.cashbook.ui.main.activity

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.isContentScheme
import cn.wj.android.cashbook.base.ext.base.string
import cn.wj.android.cashbook.base.ui.BaseActivity
import cn.wj.android.cashbook.data.constants.ROUTE_PATH_BACKUP
import cn.wj.android.cashbook.data.transform.toSnackbarModel
import cn.wj.android.cashbook.databinding.ActivityBackupBinding
import cn.wj.android.cashbook.third.result.createForActivityResultLauncher
import cn.wj.android.cashbook.ui.main.viewmodel.BackupViewModel
import com.alibaba.android.arouter.facade.annotation.Route
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * 备份界面
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2021/8/6
 */
@Route(path = ROUTE_PATH_BACKUP)
class BackupActivity : BaseActivity<BackupViewModel, ActivityBackupBinding>() {

    override val viewModel: BackupViewModel by viewModel()

    /** 文件选择 launcher */
    private val selectFileLauncher = createForActivityResultLauncher(ActivityResultContracts.OpenDocumentTree())

    /** 权限申请 launcher */
    private val requestPermissionsLauncher = createForActivityResultLauncher(ActivityResultContracts.RequestMultiplePermissions())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_backup)
    }

    override fun observe() {
        // 选择备份路径
        viewModel.selectBackupPathEvent.observe(this) { backup ->
            selectFileLauncher.launch(null) {
                if (null == it) {
                    return@launch
                }
                val path = if (it.isContentScheme()) {
                    contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    it.toString()
                } else {
                    it.path
                }.orEmpty()
                viewModel.backupPathData.value = path
                if (backup) {
                    // 备份请求
                    viewModel.checkBackupPathEvent.value = path
                }
            }
        }
        // 检查备份路径
        viewModel.checkBackupPathEvent.observe(this, fun(path) {
            if (path.isNullOrBlank()) {
                viewModel.snackbarEvent.value = R.string.backup_path_exception.string.toSnackbarModel()
                return
            }
            if (path.isContentScheme()) {
                if (DocumentFile.fromTreeUri(this, Uri.parse(path))?.canRead() == true) {
                    // 有权限，开始备份
                    viewModel.tryBackup(path)
                } else {
                    // 重新选择备份路径
                    viewModel.selectBackupPathEvent.value = true
                }
            } else {
                // 申请权限
                requestPermissionsLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)) { map ->
                    if (map.values.contains(false)) {
                        // 有未同意权限
                        viewModel.snackbarEvent.value = R.string.backup_need_storage_permissions.string.toSnackbarModel()
                        return@launch
                    }
                    // 开始备份
                    viewModel.tryBackup(path)
                }
            }
        })
    }
}