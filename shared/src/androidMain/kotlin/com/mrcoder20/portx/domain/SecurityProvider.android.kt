package com.mrcoder20.portx.domain

import java.io.File

class AndroidSecurityProvider : SecurityProvider {
    override fun isDeviceCompromised(): Boolean {
        return checkRootFiles() || checkSuBinary() || checkSystemProperties() || 
               isEmulator() || isDebuggerConnected() || isHookingDetected()
    }

    override fun getSecurityMessage(): String? {
        val message = when {
            checkRootFiles() || checkSuBinary() -> "Security Alert: Rooted Device Detected."
            isDebuggerConnected() -> "Security Alert: Active Debugger Detected."
            isEmulator() -> "Security Alert: Virtual Environment (Emulator) Detected."
            isHookingDetected() -> "Security Alert: Runtime Instrumentation (Hooking) Detected."
            else -> null
        }
        return message?.let { "$it Access restricted for system integrity." }
    }

    private fun isDebuggerConnected(): Boolean = android.os.Debug.isDebuggerConnected()

    private fun isEmulator(): Boolean {
        val model = android.os.Build.MODEL
        val hardware = android.os.Build.HARDWARE
        return model.contains("sdk", true) || model.contains("Emulator", true) || 
               hardware.contains("goldfish", true) || hardware.contains("ranchu", true)
    }

    private fun isHookingDetected(): Boolean {
        return try {
            File("/proc/self/maps").useLines { lines ->
                lines.any { line ->
                    line.contains("frida", true) || line.contains("xposed", true) || 
                    line.contains("substrate", true) || line.contains("magisk", true)
                }
            }
        } catch (e: Exception) { false }
    }

    private fun checkRootFiles(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/data/local/su",
            "/system/app/SuperSU.apk",
            "/system/app/SuperSU/SuperSU.apk",
            "/system/app/Magisk/Magisk.apk"
        )
        return paths.any { File(it).exists() }
    }

    private fun checkSuBinary(): Boolean {
        return try {
            val process = ProcessBuilder(listOf("which", "su")).redirectErrorStream(true).start()
            val finished = process.waitFor(500, java.util.concurrent.TimeUnit.MILLISECONDS)
            if (!finished) {
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        process.destroyForcibly()
                    } else {
                        process.destroy()
                    }
                } catch (_: Throwable) { process.destroy() }
                return false
            }
            val output = process.inputStream.bufferedReader().readLine()?.trim()
            val exitCode = process.exitValue()
            try { process.destroy() } catch (_: Throwable) {}
            exitCode == 0 && !output.isNullOrBlank() && 
                !output.contains("not found", ignoreCase = true) && 
                !output.contains("no su", ignoreCase = true)
        } catch (e: Exception) { false }
    }

    private fun checkSystemProperties(): Boolean {
        val buildTags = android.os.Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }
}

actual fun getSecurityProvider(): SecurityProvider = AndroidSecurityProvider()
