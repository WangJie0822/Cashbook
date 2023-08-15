package androidx.compose.material3

import androidx.compose.material.ModalBottomSheetDefaults as M2ModalBottomSheetDefaults
import androidx.compose.material.ModalBottomSheetLayout as M2ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState as M2ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue as M2ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState as rememberM2ModalBottomSheetState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

/**
 * 适配 Material3 的 ModalBottomSheetLayout
 *
 * - 将原始的 Material2 主题参数替换为 Material3
 *
 * > [王杰](mailto:15555650921@163.com) 创建于 2023/2/27
 */
@Deprecated(
    message = "Material3 is support BottomSheet now, this Method is deprecated, use ModalBottomSheet instead",
    replaceWith = ReplaceWith("ModalBottomSheet", "androidx.compose.material3.ModalBottomSheet"),
)
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ModalBottomSheetLayout(
    sheetContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    sheetState: ModalBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden),
    sheetShape: Shape = MaterialTheme.shapes.large,
    sheetElevation: Dp = ModalBottomSheetDefaults.Elevation,
    sheetBackgroundColor: Color = MaterialTheme.colorScheme.surface,
    sheetContentColor: Color = contentColorFor(sheetBackgroundColor),
    scrimColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f),
    content: @Composable () -> Unit
) {

    M2ModalBottomSheetLayout(
        sheetContent = sheetContent,
        modifier = modifier,
        sheetState = sheetState.proxy,
        sheetShape = sheetShape,
        sheetElevation = sheetElevation,
        sheetBackgroundColor = sheetBackgroundColor,
        sheetContentColor = sheetContentColor,
        scrimColor = scrimColor,
        content = content
    )
}

@Composable
fun rememberModalBottomSheetState(
    initialValue: ModalBottomSheetValue,
    animationSpec: AnimationSpec<Float> = SpringSpec(),
    confirmValueChange: (ModalBottomSheetValue) -> Boolean = { true },
    skipHalfExpanded: Boolean = false,
): ModalBottomSheetState {
    val sheetState = remember {
        ModalBottomSheetState(initialValue, animationSpec, confirmValueChange, skipHalfExpanded)
    }
    sheetState.InitProxy()
    return sheetState
}

@OptIn(ExperimentalMaterialApi::class)
class ModalBottomSheetState(
    initialValue: ModalBottomSheetValue,
    private val animationSpec: AnimationSpec<Float> = SpringSpec(),
    private val confirmValueChange: (ModalBottomSheetValue) -> Boolean = { true },
    private val skipHalfExpanded: Boolean = false,
) {
    private val m2InitialValue: M2ModalBottomSheetValue = when (initialValue) {
        ModalBottomSheetValue.Hidden -> M2ModalBottomSheetValue.Hidden
        ModalBottomSheetValue.Expanded -> M2ModalBottomSheetValue.Expanded
        ModalBottomSheetValue.HalfExpanded -> M2ModalBottomSheetValue.HalfExpanded
    }

    private val m2ConfirmValueChange: (M2ModalBottomSheetValue) -> Boolean = {
        confirmValueChange.invoke(
            when (it) {
                M2ModalBottomSheetValue.Hidden -> ModalBottomSheetValue.Hidden
                M2ModalBottomSheetValue.Expanded -> ModalBottomSheetValue.Expanded
                M2ModalBottomSheetValue.HalfExpanded -> ModalBottomSheetValue.HalfExpanded
            }
        )
    }

    private var _proxy: M2ModalBottomSheetState? = null

    internal val proxy: M2ModalBottomSheetState
        get() {
            if (null == _proxy) {
                _proxy = M2ModalBottomSheetState(
                    initialValue = m2InitialValue,
                    animationSpec = animationSpec,
                    confirmValueChange = m2ConfirmValueChange,
                    isSkipHalfExpanded = skipHalfExpanded,
                )
            }
            return _proxy!!
        }


    @Composable
    internal fun InitProxy() {
        _proxy = rememberM2ModalBottomSheetState(
            initialValue = m2InitialValue,
            animationSpec = animationSpec,
            confirmValueChange = m2ConfirmValueChange,
            skipHalfExpanded = skipHalfExpanded,
        )
    }


    val isVisible: Boolean
        get() = proxy.isVisible

    suspend fun hide() {
        proxy.hide()
    }

    suspend fun show() {
        proxy.show()
    }
}

enum class ModalBottomSheetValue {
    /**
     * The bottom sheet is not visible.
     */
    Hidden,

    /**
     * The bottom sheet is visible at full height.
     */
    Expanded,

    /**
     * The bottom sheet is partially visible at 50% of the screen height. This state is only
     * enabled if the height of the bottom sheet is more than 50% of the screen height.
     */
    HalfExpanded
}

object ModalBottomSheetDefaults {

    /**
     * The default elevation used by [ModalBottomSheetLayout].
     */
    val Elevation = M2ModalBottomSheetDefaults.Elevation

    /**
     * The default scrim color used by [ModalBottomSheetLayout].
     */
    val scrimColor: Color
        @Composable
        get() = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f)
}
