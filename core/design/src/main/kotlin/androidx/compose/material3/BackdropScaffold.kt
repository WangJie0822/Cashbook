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

package androidx.compose.material3

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material.BackdropScaffold as M2BackdropScaffold
import androidx.compose.material.BackdropScaffoldState as M2BackdropScaffoldState
import androidx.compose.material.BackdropValue as M2BackdropValue
import androidx.compose.material.rememberBackdropScaffoldState as rememberM2BackdropScaffoldState

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BackdropScaffold(
    appBar: @Composable () -> Unit,
    backLayerContent: @Composable () -> Unit,
    frontLayerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    scaffoldState: BackdropScaffoldState = rememberBackdropScaffoldState(BackdropValue.Concealed),
    gesturesEnabled: Boolean = true,
    peekHeight: Dp = BackdropScaffoldDefaults.PeekHeight,
    headerHeight: Dp = BackdropScaffoldDefaults.HeaderHeight,
    persistentAppBar: Boolean = true,
    stickyFrontLayer: Boolean = true,
    backLayerBackgroundColor: Color = MaterialTheme.colorScheme.primary,
    backLayerContentColor: Color = contentColorFor(backLayerBackgroundColor),
    frontLayerShape: Shape = MaterialTheme.shapes.large.copy(
        topStart = CornerSize(16.dp),
        topEnd = CornerSize(16.dp),
    ),
    frontLayerElevation: Dp = BackdropScaffoldDefaults.FrontLayerElevation,
    frontLayerBackgroundColor: Color = MaterialTheme.colorScheme.surface,
    frontLayerContentColor: Color = contentColorFor(frontLayerBackgroundColor),
    frontLayerScrimColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.60f),
) {
    M2BackdropScaffold(
        appBar = appBar,
        backLayerContent = backLayerContent,
        frontLayerContent = frontLayerContent,
        modifier = modifier,
        scaffoldState = scaffoldState.proxy,
        gesturesEnabled = gesturesEnabled,
        peekHeight = peekHeight,
        headerHeight = headerHeight,
        persistentAppBar = persistentAppBar,
        stickyFrontLayer = stickyFrontLayer,
        backLayerBackgroundColor = backLayerBackgroundColor,
        backLayerContentColor = backLayerContentColor,
        frontLayerShape = frontLayerShape,
        frontLayerElevation = frontLayerElevation,
        frontLayerBackgroundColor = frontLayerBackgroundColor,
        frontLayerContentColor = frontLayerContentColor,
        frontLayerScrimColor = frontLayerScrimColor,
    )
}

object BackdropScaffoldDefaults {

    /**
     * The default peek height of the back layer.
     */
    val PeekHeight = 56.dp

    /**
     * The default header height of the front layer.
     */
    val HeaderHeight = 48.dp

    /**
     * The default shape of the front layer.
     */
    val frontLayerShape: Shape
        @Composable
        get() = androidx.compose.material.MaterialTheme.shapes.large
            .copy(topStart = CornerSize(16.dp), topEnd = CornerSize(16.dp))

    /**
     * The default elevation of the front layer.
     */
    val FrontLayerElevation = 1.dp

    /**
     * The default color of the scrim applied to the front layer.
     */
    val frontLayerScrimColor: Color
        @Composable get() = androidx.compose.material.MaterialTheme.colors.surface.copy(alpha = 0.60f)
}

@OptIn(ExperimentalMaterialApi::class)
class BackdropScaffoldState(
    initialValue: BackdropValue,
    private val animationSpec: AnimationSpec<Float> = SpringSpec(),
    private val confirmStateChange: (BackdropValue) -> Boolean = { true },
) {

    private val m2InitialValue: M2BackdropValue = when (initialValue) {
        BackdropValue.Revealed -> M2BackdropValue.Revealed
        BackdropValue.Concealed -> M2BackdropValue.Concealed
    }

    private val m2ConfirmStateChange: (M2BackdropValue) -> Boolean = {
        confirmStateChange.invoke(
            when (it) {
                M2BackdropValue.Revealed -> BackdropValue.Revealed
                M2BackdropValue.Concealed -> BackdropValue.Concealed
            },
        )
    }

    private var _proxy: M2BackdropScaffoldState? = null

    internal val proxy: M2BackdropScaffoldState
        get() {
            if (null == _proxy) {
                _proxy = M2BackdropScaffoldState(
                    m2InitialValue,
                    animationSpec,
                    m2ConfirmStateChange,
                )
            }
            return _proxy!!
        }

    @Composable
    internal fun InitProxy() {
        _proxy = rememberM2BackdropScaffoldState(
            m2InitialValue,
            animationSpec,
            m2ConfirmStateChange,
        )
    }

    val isConcealed: Boolean
        get() = proxy.isConcealed

    val isRevealed: Boolean
        get() = proxy.isRevealed
}

enum class BackdropValue {
    /**
     * Indicates the back layer is concealed and the front layer is active.
     */
    Concealed,

    /**
     * Indicates the back layer is revealed and the front layer is inactive.
     */
    Revealed,
}

@Composable
fun rememberBackdropScaffoldState(
    initialValue: BackdropValue,
    animationSpec: AnimationSpec<Float> = SpringSpec(),
    confirmStateChange: (BackdropValue) -> Boolean = { true },
): BackdropScaffoldState {
    val state = remember {
        BackdropScaffoldState(
            initialValue = initialValue,
            animationSpec = animationSpec,
            confirmStateChange = confirmStateChange,
        )
    }
    state.InitProxy()
    return state
}
