package cn.wj.android.cashbook.core.design.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompatTextField(
    modifier: Modifier = Modifier,
    initializedText: String,
    label: String,
    placeholder: String? = null,
    supportingText: String? = null,
    onValueChange: (String) -> Unit,
    onValueVerify: ((String) -> Boolean)? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    maxLength: Int = Int.MAX_VALUE,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = TextFieldDefaults.filledShape,
    colors: TextFieldColors = TextFieldDefaults.textFieldColors(),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    var text by remember {
        mutableStateOf(initializedText)
    }
    val verify: (String) -> Boolean = {
        it.length <= maxLength && onValueVerify?.invoke(it) != false
    }
    val placeholderL: @Composable (() -> Unit)? = if (!placeholder.isNullOrBlank()) {
        { Text(text = placeholder) }
    } else {
        null
    }
    val supportingTextL: @Composable (() -> Unit)? = if (!supportingText.isNullOrBlank()) {
        { Text(text = supportingText) }
    } else {
        null
    }

    TextField(
        value = text,
        onValueChange = {
            if (verify(it)) {
                text = it
                onValueChange(it)
            }
        },
        label = { Text(text = label) },
        placeholder = placeholderL,
        supportingText = supportingTextL,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        isError = isError,
        colors = colors,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        interactionSource = interactionSource,
        singleLine = singleLine,
        maxLines = maxLines,
        shape = shape,
        modifier = modifier,
    )
}

@Composable
fun PasswordTextField(
    modifier: Modifier = Modifier,
    initializedText: String,
    label: String,
    placeholder: String? = null,
    supportingText: String? = null,
    onValueChange: (String) -> Unit,
    onValueVerify: ((String) -> Boolean)? = null,
    isError: Boolean = false,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    var visible by remember {
        mutableStateOf(false)
    }

    CompatTextField(
        modifier = modifier,
        initializedText = initializedText,
        label = label,
        placeholder = placeholder,
        supportingText = supportingText,
        onValueChange = onValueChange,
        onValueVerify = onValueVerify,
        isError = isError,
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = null
                )
            }
        },
        visualTransformation = if (!visible) PasswordVisualTransformation() else VisualTransformation.None,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (!visible) KeyboardType.Password else KeyboardType.Text),
        keyboardActions = keyboardActions,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompatTextField(
    textFieldState: TextFieldState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = TextFieldDefaults.filledShape,
    colors: TextFieldColors = TextFieldDefaults.textFieldColors(),
) {
    TextField(
        value = textFieldState.text,
        onValueChange = {
            textFieldState.onTextChange(it)
            textFieldState.enableShowErrors()
        },
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        isError = textFieldState.showErrors(),
        supportingText = {
            textFieldState.getError()?.let { error -> Text(text = error) }
        },
        colors = colors,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        interactionSource = interactionSource,
        singleLine = singleLine,
        maxLines = maxLines,
        shape = shape,
        modifier = modifier.onFocusChanged { focusState ->
            textFieldState.onFocusChange(focusState.isFocused)
            if (!focusState.isFocused) {
                textFieldState.enableShowErrors()
            }
        },
    )
}

