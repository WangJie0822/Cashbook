/*
 * Copyright 2021 The Cashbook Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.wj.android.cashbook.core.testing.repository

import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryManager
import cn.wj.android.cashbook.core.data.uitl.BackupRecoveryState
import cn.wj.android.cashbook.core.model.model.BackupModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeBackupRecoveryManager : BackupRecoveryManager {

    private val _isWebDAVConnected = MutableStateFlow(false)
    private val _backupState = MutableStateFlow<BackupRecoveryState>(BackupRecoveryState.None)
    private val _recoveryState = MutableStateFlow<BackupRecoveryState>(BackupRecoveryState.None)

    override val isWebDAVConnected: Flow<Boolean> = _isWebDAVConnected
    override val backupState: Flow<BackupRecoveryState> = _backupState
    override val recoveryState: Flow<BackupRecoveryState> = _recoveryState

    /** 用于测试验证的记录 */
    var refreshWebDAVConnectedCalled = false
        private set
    var requestBackupCalled = false
        private set
    var lastRequestBackupOnlyLocal: Boolean? = null
        private set
    var lastRecoveryPath: String? = null
        private set
    var lastGetRecoveryListOnlyLocal: Boolean? = null
        private set
    var lastGetRecoveryListLocalPath: String? = null
        private set

    /** 用于控制 getRecoveryList 返回值 */
    private var recoveryList: List<BackupModel> = emptyList()

    fun setWebDAVConnected(connected: Boolean) {
        _isWebDAVConnected.value = connected
    }

    fun setRecoveryList(list: List<BackupModel>) {
        recoveryList = list
    }

    override fun refreshWebDAVConnected() {
        refreshWebDAVConnectedCalled = true
    }

    override fun updateBackupState(state: BackupRecoveryState) {
        _backupState.value = state
    }

    override fun updateRecoveryState(state: BackupRecoveryState) {
        _recoveryState.value = state
    }

    override suspend fun requestAutoBackup() {
        requestBackupCalled = true
    }

    override suspend fun requestBackup(onlyLocal: Boolean) {
        requestBackupCalled = true
        lastRequestBackupOnlyLocal = onlyLocal
    }

    override suspend fun requestRecovery(path: String) {
        lastRecoveryPath = path
    }

    override suspend fun getRecoveryList(
        onlyLocal: Boolean,
        localPath: String,
    ): List<BackupModel> {
        lastGetRecoveryListOnlyLocal = onlyLocal
        lastGetRecoveryListLocalPath = localPath
        return recoveryList
    }
}
