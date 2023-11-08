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

package cn.wj.android.cashbook.ui

import androidx.activity.OnBackPressedDispatcher
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import cn.wj.android.cashbook.core.design.component.LocalDefaultEmptyImagePainter
import cn.wj.android.cashbook.core.design.component.LocalDefaultLoadingHint
import cn.wj.android.cashbook.core.model.enums.DarkModeEnum
import cn.wj.android.cashbook.core.ui.LocalBackPressedDispatcher
import cn.wj.android.cashbook.core.ui.LocalProgressDialogHint
import cn.wj.android.cashbook.core.ui.R

sealed interface ActivityUiState {
    data object Loading : ActivityUiState

    data class Success(
        val darkMode: DarkModeEnum,
        val dynamicColor: Boolean,
    ) : ActivityUiState
}

@Composable
internal fun ProvideLocalState(
    onBackPressedDispatcher: OnBackPressedDispatcher,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalBackPressedDispatcher provides onBackPressedDispatcher,
        LocalDefaultEmptyImagePainter provides painterResource(id = R.drawable.vector_no_data_200),
        LocalDefaultLoadingHint provides stringResource(id = R.string.data_in_loading),
        LocalProgressDialogHint provides stringResource(id = R.string.progress_loading_default),
        content = content,
    )
}

@Composable
internal fun shouldDisableDynamicTheming(
    uiState: ActivityUiState,
): Boolean = when (uiState) {
    ActivityUiState.Loading -> false
    is ActivityUiState.Success -> !uiState.dynamicColor
}

@Composable
internal fun shouldUseDarkTheme(
    uiState: ActivityUiState,
): Boolean = when (uiState) {
    ActivityUiState.Loading -> isSystemInDarkTheme()
    is ActivityUiState.Success -> when (uiState.darkMode) {
        DarkModeEnum.FOLLOW_SYSTEM -> {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            isSystemInDarkTheme()
        }

        DarkModeEnum.LIGHT -> {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            false
        }

        DarkModeEnum.DARK -> {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            true
        }
    }
}
