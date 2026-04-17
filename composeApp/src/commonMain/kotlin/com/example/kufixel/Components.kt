package com.example.kufixel

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ButtonLabel(label: String, visible: Boolean, onDismiss: () -> Unit) {
    if (visible) {
        LaunchedEffect(visible) {
            delay(1500)
            onDismiss()
        }

        Popup(
            alignment = Alignment.TopCenter,
            offset = IntOffset(0, -100)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.8f),
                shape = RoundedCornerShape(8.dp),
                tonalElevation = 4.dp
            ) {
                Text(
                    text = label,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 14.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun LabeledIconButton(
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    content: @Composable () -> Unit
) {
    var showLabel by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        IconButton(
            onClick = {
                onClick()
                showLabel = true
            },
            enabled = enabled,
            colors = colors
        ) {
            content()
        }
        if (showLabel) {
            ButtonLabel(label = label, visible = showLabel, onDismiss = { showLabel = false })
        }
    }
}

@Composable
fun LabeledButton(
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: androidx.compose.foundation.BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    var showLabel by remember { mutableStateOf(false) }

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Button(
            onClick = {
                onClick()
                showLabel = true
            },
            enabled = enabled,
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = border,
            contentPadding = contentPadding,
            interactionSource = interactionSource,
            content = content
        )
        if (showLabel) {
            ButtonLabel(label = label, visible = showLabel, onDismiss = { showLabel = false })
        }
    }
}

@Composable
fun LabeledTextButton(
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = ButtonDefaults.textShape,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    elevation: ButtonElevation? = null,
    border: androidx.compose.foundation.BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    var showLabel by remember { mutableStateOf(false) }

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        TextButton(
            onClick = {
                onClick()
                showLabel = true
            },
            enabled = enabled,
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = border,
            contentPadding = contentPadding,
            interactionSource = interactionSource,
            content = content
        )
        if (showLabel) {
            ButtonLabel(label = label, visible = showLabel, onDismiss = { showLabel = false })
        }
    }
}
