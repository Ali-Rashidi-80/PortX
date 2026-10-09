package com.mrcoder20.portx.presentation.ui.components

import androidx.compose.ui.input.pointer.PointerIcon
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Point
import java.awt.RenderingHints
import java.awt.Toolkit
import java.awt.image.BufferedImage

actual fun getDeviceEmulationPointerIcon(): PointerIcon {
    return try {
        val size = 26
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g2d = image.createGraphics()
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        // DevTools-style semi-transparent touch disk
        g2d.color = Color(0, 209, 255, 60)
        g2d.fillOval(2, 2, size - 4, size - 4)

        // High-contrast smooth border ring
        g2d.color = Color(0, 209, 255, 230)
        g2d.stroke = BasicStroke(1.5f)
        g2d.drawOval(2, 2, size - 4, size - 4)

        // Center contact pip
        g2d.color = Color(0, 209, 255, 255)
        g2d.fillOval(size / 2 - 2, size / 2 - 2, 4, 4)

        g2d.dispose()
        val cursor = Toolkit.getDefaultToolkit().createCustomCursor(
            image,
            Point(size / 2, size / 2),
            "PortXTouchEmulationCursor"
        )
        PointerIcon(cursor)
    } catch (e: Throwable) {
        PointerIcon.Hand
    }
}
