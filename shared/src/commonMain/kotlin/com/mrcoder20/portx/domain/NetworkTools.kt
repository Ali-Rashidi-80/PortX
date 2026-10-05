package com.mrcoder20.portx.domain

import kotlinx.coroutines.flow.Flow

interface NetworkTools {
    suspend fun ping(host: String): Flow<PingResult>
    suspend fun dnsLookup(host: String): List<String>
    suspend fun whois(host: String): String
    fun getLocalIpInfo(): LocalIpInfo
    suspend fun getPublicIp(): String?
}

data class PingResult(
    val sequence: Int,
    val timeMs: Long?,
    val isSuccess: Boolean,
    val message: String
)

data class PingAttempt(
    val isSuccess: Boolean,
    val timeMs: Long?,
    val ttl: Int?
)

data class LocalIpInfo(
    val ipAddress: String,
    val interfaceName: String,
    val isWifi: Boolean
)

expect fun getNetworkTools(): NetworkTools

/**
 * Sanitizes user input for network operations.
 * Strips URL schemes (http://, https://), trailing paths, query strings, and single-port colons.
 */
fun sanitizeHost(input: String): String {
    var host = input.trim()
    if (host.startsWith("http://", ignoreCase = true)) {
        host = host.substring(7)
    } else if (host.startsWith("https://", ignoreCase = true)) {
        host = host.substring(8)
    }
    // Remove query or path if user pasted full URL (e.g. example.com/test or example.com?q=1)
    val slashIdx = host.indexOf('/')
    if (slashIdx != -1) {
        host = host.substring(0, slashIdx)
    }
    val questionIdx = host.indexOf('?')
    if (questionIdx != -1) {
        host = host.substring(0, questionIdx)
    }
    // Handle port if present, while avoiding stripping colons from IPv6 addresses (e.g. 2001:db8::1)
    if (host.startsWith("[") && host.contains("]:")) {
        host = host.substring(1, host.indexOf("]:"))
    } else if (host.contains(':') && !host.contains("::")) {
        val colonCount = host.count { it == ':' }
        if (colonCount == 1) {
            host = host.substring(0, host.indexOf(':'))
        }
    }
    return host.trim()
}

/**
 * Extracts round-trip time in milliseconds from native ping output (Windows, Linux, macOS, Android).
 */
fun parseTimeFromPingOutput(output: String): Long? {
    val regex = Regex("""time[=<]?\s*([\d.]+)\s*ms""", RegexOption.IGNORE_CASE)
    val match = regex.find(output) ?: return null
    val raw = match.groupValues[1]
    return raw.toDoubleOrNull()?.toLong()?.coerceAtLeast(1L)
}

/**
 * Extracts Time-To-Live (TTL) integer from native ping output.
 */
fun parseTtlFromPingOutput(output: String): Int? {
    val regex = Regex("""ttl[=:]\s*(\d+)""", RegexOption.IGNORE_CASE)
    val match = regex.find(output) ?: return null
    return match.groupValues[1].toIntOrNull()
}
