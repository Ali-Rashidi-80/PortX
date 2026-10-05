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
    } else if (host.startsWith("[") && host.endsWith("]")) {
        host = host.substring(1, host.length - 1)
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
    val regex = Regex("""(?:time|zeit|temps|время)[=<]?\s*([\d.]+)\s*(?:ms|мс)""", RegexOption.IGNORE_CASE)
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

/**
 * Determines whether a target IP/hostname is a loopback, link-local, or private RFC 1918 / RFC 4193 / RFC 6598 address.
 */
fun isTargetLocalOrPrivate(target: String): Boolean {
    val clean = target.trim().lowercase().removePrefix("[").removeSuffix("]")
    if (clean == "localhost" || clean == "::1" || clean == "0.0.0.0" || clean == "::") return true
    
    // RFC 1122: Loopback 127.0.0.0/8
    if (clean.startsWith("127.")) return true
    
    // RFC 1918: 10.0.0.0/8 & 192.168.0.0/16
    if (clean.startsWith("10.") || clean.startsWith("192.168.")) return true
    
    // RFC 3927: IPv4 Link-Local 169.254.0.0/16
    if (clean.startsWith("169.254.")) return true
    
    // RFC 1918: 172.16.0.0 - 172.31.255.255
    if (clean.startsWith("172.")) {
        val secondOctet = clean.substringAfter("172.").substringBefore(".").toIntOrNull()
        if (secondOctet != null && secondOctet in 16..31) return true
    }

    // RFC 6598: Carrier-Grade NAT (CGNAT) 100.64.0.0/10 (100.64.0.0 - 100.127.255.255)
    if (clean.startsWith("100.")) {
        val secondOctet = clean.substringAfter("100.").substringBefore(".").toIntOrNull()
        if (secondOctet != null && secondOctet in 64..127) return true
    }

    // IPv6 checks (ensure presence of ':')
    if (clean.contains(":")) {
        // RFC 4291: Link-Local fe80::/10 (fe80 - febf)
        if (clean.startsWith("fe8") || clean.startsWith("fe9") || clean.startsWith("fea") || clean.startsWith("feb")) return true
        // RFC 4193: Unique Local Address (ULA) fc00::/7 (fc00 - fdff)
        if (clean.startsWith("fc") || clean.startsWith("fd")) return true
    }

    // Standard local/private domain name suffixes (RFC 6762, RFC 8375, RFC 6761)
    if (clean.endsWith(".local") || clean.endsWith(".lan") || clean.endsWith(".internal") || clean.endsWith(".home.arpa") || clean.endsWith(".localhost")) {
        return true
    }

    return false
}

