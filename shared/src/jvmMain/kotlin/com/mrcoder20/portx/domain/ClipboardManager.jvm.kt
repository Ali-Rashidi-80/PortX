package com.mrcoder20.portx.domain

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

class JvmClipboardManager : ClipboardManager {
    override fun copyToClipboard(text: String) {
        val selection = StringSelection(text)
        try {
            Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
        } catch (e: IllegalStateException) {
            // System clipboard locked by another application, retry once after short delay
            try {
                Thread.sleep(60)
                Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
            } catch (_: Exception) {}
        } catch (_: Exception) {}
    }
}

actual fun getClipboardManager(): ClipboardManager = JvmClipboardManager()
