package com.mrcoder20.portx

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.*
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
import java.awt.EventQueue
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener

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
            var isMaximized by remember { mutableStateOf(false) }
            var floatingBounds by remember { mutableStateOf<Rectangle?>(null) }

            // Ensure system cursor is always valid and visible
            DisposableEffect(window) {
                fun ensureCursor() {
                    EventQueue.invokeLater {
                        window.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
                        window.rootPane.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
                        window.contentPane.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
                    }
                }
                ensureCursor()

                val focusListener = object : WindowFocusListener {
                    override fun windowGainedFocus(e: WindowEvent?) = ensureCursor()
                    override fun windowLostFocus(e: WindowEvent?) {}
                }
                window.addWindowFocusListener(focusListener)
                window.minimumSize = Dimension(960, 640)

                onDispose {
                    window.removeWindowFocusListener(focusListener)
                }
            }

            LaunchedEffect(windowState.placement) {
                EventQueue.invokeLater {
                    window.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
                    window.rootPane.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
                    window.contentPane.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
                }
            }

            val toggleMaximize = {
                EventQueue.invokeLater {
                    if (!isMaximized) {
                        floatingBounds = window.bounds
                        val gc = window.graphicsConfiguration
                        val screenBounds = gc.bounds
                        val insets = Toolkit.getDefaultToolkit().getScreenInsets(gc)

                        val targetX = screenBounds.x + insets.left
                        val targetY = screenBounds.y + insets.top
                        val targetWidth = screenBounds.width - insets.left - insets.right
                        val targetHeight = screenBounds.height - insets.top - insets.bottom

                        window.setBounds(targetX, targetY, targetWidth, targetHeight)
                        isMaximized = true
                    } else {
                        val prev = floatingBounds
                        if (prev != null && prev.width >= 400 && prev.height >= 300) {
                            window.bounds = prev
                        } else {
                            val gc = window.graphicsConfiguration
                            val screenBounds = gc.bounds
                            val defaultW = 1240
                            val defaultH = 820
                            val targetX = screenBounds.x + (screenBounds.width - defaultW) / 2
                            val targetY = screenBounds.y + (screenBounds.height - defaultH) / 2
                            window.setBounds(targetX, targetY, defaultW, defaultH)
                        }
                        isMaximized = false
                    }
                    window.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
                    window.rootPane.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
                    window.contentPane.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerHoverIcon(PointerIcon.Default)
            ) {
                App(
                    onMinimize = { windowState.isMinimized = true },
                    onMaximize = toggleMaximize,
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
