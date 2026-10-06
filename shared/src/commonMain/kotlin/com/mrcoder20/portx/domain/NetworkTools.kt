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
    host = host.trim()
    if (host.endsWith(".") && host.length > 1 && !host.endsWith("..")) {
        host = host.dropLast(1)
    }
    return host
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
    if (clean == "localhost" || clean == "::1" || clean == "0.0.0.0" || clean == "::" || clean == "255.255.255.255") return true
    
    // RFC 4291: IPv4-mapped IPv6 address (e.g. ::ffff:192.168.1.1)
    if (clean.startsWith("::ffff:") || clean.startsWith("0:0:0:0:0:ffff:")) {
        val mappedIpv4 = clean.substringAfterLast(":")
        return isTargetLocalOrPrivate(mappedIpv4)
    }

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

    // RFC 1112: IPv4 Multicast 224.0.0.0/4 (224.0.0.0 - 239.255.255.255)
    val firstOctet = clean.substringBefore(".").toIntOrNull()
    if (firstOctet != null && firstOctet in 224..239) return true

    // IPv6 checks (ensure presence of ':')
    if (clean.contains(":")) {
        // RFC 4291: Multicast ff00::/8
        if (clean.startsWith("ff")) return true
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

val HOSTNAME_REGEX = Regex("""^([a-zA-Z0-9_]([a-zA-Z0-9_\-]{0,61}[a-zA-Z0-9_])?\.)*[a-zA-Z0-9_]([a-zA-Z0-9_\-]{0,61}[a-zA-Z0-9_])?$""")

/**
 * Validates whether an input target is a compliant IPv4, IPv6, or valid Hostname/FQDN.
 */
fun isValidTarget(target: String): Boolean {
    val clean = target.trim()
    if (clean.isBlank()) return false
    val isAllNumericDotted = Regex("""^[0-9.]+$""").matches(clean)
    return when {
        isAllNumericDotted || clean.contains(":") -> isValidIpAddress(clean)
        else -> HOSTNAME_REGEX.matches(clean)
    }
}

/**
 * Validates whether an input string is a strictly compliant IPv4 or IPv6 address.
 * Neutralizes HTML error pages, captive portals, or corrupted text from polluting IP state.
 */
fun isValidIpAddress(ip: String): Boolean {
    val clean = ip.trim().removePrefix("[").removeSuffix("]")
    if (clean.isBlank()) return false

    // Support RFC 4291 Section 2.5.5.2: IPv4-mapped IPv6 (e.g. ::ffff:192.168.1.1)
    if (clean.contains(":") && clean.contains(".")) {
        val lastColon = clean.lastIndexOf(':')
        val prefix = clean.substring(0, lastColon)
        val ipv4Part = clean.substring(lastColon + 1)
        if (!isValidIpAddress(ipv4Part)) return false
        val pseudoIpv6 = "$prefix:0:0"
        return isValidIpAddress(pseudoIpv6)
    }

    // IPv4 check: 4 decimal octets 0..255 without malformed leading zeros
    val parts = clean.split(".")
    if (parts.size == 4) {
        return parts.all { part ->
            part.toIntOrNull()?.let { it in 0..255 && (part == "0" || !part.startsWith("0")) } ?: false
        }
    }

    // IPv6 check: RFC 4291 compliant structure
    if (clean.contains(":")) {
        // Triple colon (or more) is strictly invalid
        if (clean.contains(":::")) return false

        // Double colon :: can appear at most once
        val doubleColonIndex = clean.indexOf("::")
        if (doubleColonIndex != -1 && doubleColonIndex != clean.lastIndexOf("::")) return false

        // Single colon at start or end is invalid (unless part of ::)
        if (clean.startsWith(":") && !clean.startsWith("::")) return false
        if (clean.endsWith(":") && !clean.endsWith("::")) return false

        val tokens = clean.split(":")
        if (doubleColonIndex == -1) {
            // Full IPv6 must have exactly 8 groups
            if (tokens.size != 8) return false
            return tokens.all { token ->
                token.length in 1..4 && token.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
            }
        } else {
            // Compressed IPv6 with :: can have at most 7 non-empty groups
            val nonEmptyTokens = tokens.filter { it.isNotEmpty() }
            if (nonEmptyTokens.size > 7) return false
            return nonEmptyTokens.all { token ->
                token.length in 1..4 && token.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
            }
        }
    }

    return false
}

