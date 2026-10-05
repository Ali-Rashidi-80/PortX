package com.mrcoder20.portx.domain

import android.content.ClipData
import android.content.ClipboardManager as AndroidClipboard
import android.content.Context
import android.widget.Toast
import com.mrcoder20.portx.appContext

import android.os.Build
import android.os.Handler
import android.os.Looper

class AndroidClipboardManager : ClipboardManager {
    override fun copyToClipboard(text: String) {
        try {
            val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as? AndroidClipboard ?: return
            val clip = ClipData.newPlainText("PortX Data", text)
            clipboard.setPrimaryClip(clip)
            
            // Only show toast on Android 12 and below (API < 33) to prevent double-popups, and ensure MainLooper
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                Handler(Looper.getMainLooper()).post {
                    try {
                        Toast.makeText(appContext, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

actual fun getClipboardManager(): ClipboardManager = AndroidClipboardManager()
