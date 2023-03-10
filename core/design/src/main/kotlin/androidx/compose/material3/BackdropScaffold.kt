package androidx.compose.material3

import androidx.compose.material.BackdropScaffold as M2BackdropScaffold
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.BackdropScaffoldDefaults
import androidx.compose.material.BackdropScaffoldState
import androidx.compose.material.BackdropValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberBackdropScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
        topEnd = CornerSize(16.dp)
    ),
    frontLayerElevation: Dp = BackdropScaffoldDefaults.FrontLayerElevation,
    frontLayerBackgroundColor: Color = MaterialTheme.colorScheme.surface,
    frontLayerContentColor: Color = contentColorFor(frontLayerBackgroundColor),
    frontLayerScrimColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.60f),
    snackbarHost: @Composable (androidx.compose.material.SnackbarHostState) -> Unit = {
        androidx.compose.material.SnackbarHost(
            it
        )
    }
) {

    M2BackdropScaffold(
        appBar = appBar,
        backLayerContent = backLayerContent,
        frontLayerContent = frontLayerContent,
        modifier = modifier,
        scaffoldState = scaffoldState,
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
        snackbarHost = snackbarHost
    )
}