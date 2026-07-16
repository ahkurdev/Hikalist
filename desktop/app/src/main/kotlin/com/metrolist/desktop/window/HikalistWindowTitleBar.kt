package com.metrolist.desktop.window

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.metrolist.desktop.ui.components.HikalistLogo

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HikalistWindowTitleBar(
    controller: DesktopWindowController,
    onClose: () -> Unit,
) {
    val maximized by controller.maximized.collectAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TITLE_BAR_HEIGHT)
            .background(MaterialTheme.colorScheme.surface),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            Row(
                modifier = Modifier.fillMaxHeight().padding(horizontal = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HikalistLogo(22.dp)
                Text(
                    text = "Hikalist",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        WindowControlButton(
            contentDescription = "Minimize",
            onClick = controller::minimize,
        ) {
            Icon(Icons.Default.Remove, null, Modifier.size(17.dp))
        }
        WindowControlButton(
            contentDescription = if (maximized) "Restore" else "Maximize",
            onClick = controller::toggleMaximize,
        ) {
            Icon(
                imageVector = if (maximized) Icons.Default.FilterNone else Icons.Default.CropSquare,
                contentDescription = null,
                modifier = Modifier.size(if (maximized) 15.dp else 14.dp),
            )
        }
        WindowControlButton(
            contentDescription = "Close",
            destructive = true,
            onClick = onClose,
        ) {
            Icon(Icons.Default.Close, null, Modifier.size(18.dp))
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun WindowControlButton(
    contentDescription: String,
    destructive: Boolean = false,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    var hovered by remember { mutableStateOf(false) }
    val hoverColor = if (destructive) Color(0xFFE81123) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val containerColor by animateColorAsState(
        targetValue = if (hovered) hoverColor else Color.Transparent,
        label = "window-control-hover",
    )
    val contentColor = if (destructive && hovered) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .width(46.dp)
            .fillMaxHeight()
            .background(containerColor)
            .semantics { this.contentDescription = contentDescription }
            .onPointerEvent(PointerEventType.Enter) { hovered = true }
            .onPointerEvent(PointerEventType.Exit) { hovered = false }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) { content() }
    }
}

private val TITLE_BAR_HEIGHT = 38.dp
