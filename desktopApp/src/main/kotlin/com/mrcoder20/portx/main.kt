package com.mrcoder20.portx

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.jetbrains.compose.resources.painterResource
import com.mrcoder20.portx.shared.Res
import com.mrcoder20.portx.shared.ic1
import com.mrcoder20.portx.di.initKoin
import java.awt.Cursor
import java.awt.Dimension

fun main() {
    // High-performance desktop networking optimizations
    System.setProperty("java.net.preferIPv6Addresses", "false")
    System.setProperty("sun.net.useExclusiveBind", "false")
    System.setProperty("file.encoding", "UTF-8")

    initKoin()
    application {
        val windowState = rememberWindowState(
            placement = WindowPlacement.Floating,
            position = WindowPosition.Aligned(Alignment.Center),
            size = DpSize(1240.dp, 820.dp)
        )
        val icon = painterResource(Res.drawable.ic1)
        
        Window(
            onCloseRequest = ::exitApplication,
            title = "PortX",
            state = windowState,
            undecorated = true, // Keeps custom sleek neon title bar
            icon = icon
        ) {
            // Explicitly force system hardware cursor to be visible and prevent cursor hiding in undecorated mode
            window.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
            window.minimumSize = Dimension(960, 640)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerHoverIcon(PointerIcon.Default)
            ) {
                App(
                    onMinimize = { windowState.isMinimized = true },
                    onMaximize = { 
                        windowState.placement = if (windowState.placement == WindowPlacement.Maximized) 
                            WindowPlacement.Floating else WindowPlacement.Maximized 
                    },
                    onClose = { exitApplication() },
                    windowDraggableArea = { content ->
                        WindowDraggableArea {
                            content()
                        }
                    }
                )
            }
        }
    }
}
