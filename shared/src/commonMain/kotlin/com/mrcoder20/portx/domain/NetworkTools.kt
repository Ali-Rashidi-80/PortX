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
    // Remove userinfo if present in URI authority (RFC 3986 e.g. user:pass@example.com or user@10.0.0.1)
    val atIdx = host.indexOf('@')
    if (atIdx != -1) {
        host = host.substring(atIdx + 1)
    }
    // Remove query, fragment or path if user pasted full URL (e.g. example.com/test, example.com?q=1, example.com#hash)
    val slashIdx = host.indexOf('/')
    if (slashIdx != -1) {
        host = host.substring(0, slashIdx)
    }
    val questionIdx = host.indexOf('?')
    if (questionIdx != -1) {
        host = host.substring(0, questionIdx)
    }
    val hashIdx = host.indexOf('#')
    if (hashIdx != -1) {
        host = host.substring(0, hashIdx)
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
    val regex = Regex("""(?:time|zeit|temps|время|时间|時間|tiempo|tempo|czas|zaman)\s*[=<:]?\s*([\d.]+)\s*(?:ms|мс|毫秒)""", RegexOption.IGNORE_CASE)
    val match = regex.find(output) ?: return null
    val raw = match.groupValues[1]
    return raw.toDoubleOrNull()?.toLong()?.coerceAtLeast(1L)
}

/**
 * Extracts Time-To-Live (TTL) integer or IPv6 Hop Limit (hlim) from native ping output (RFC 8200).
 */
fun parseTtlFromPingOutput(output: String): Int? {
    val regex = Regex("""(?:ttl|hlim)[=:]\s*(\d+)""", RegexOption.IGNORE_CASE)
    val match = regex.find(output) ?: return null
    return match.groupValues[1].toIntOrNull()
}

/**
 * Determines whether a target IP/hostname is a loopback, link-local, or private RFC 1918 / RFC 4193 / RFC 6598 address.
 */
fun isTargetLocalOrPrivate(target: String): Boolean {
    val clean = target.trim().lowercase().removePrefix("[").removeSuffix("]").let {
        if (it.contains("%")) it.substringBefore("%") else it
    }
    if (clean == "localhost" || clean == "::1" || clean == "0.0.0.0" || clean == "::" || clean == "255.255.255.255") return true
    
    // RFC 4291: IPv4-mapped IPv6 address (e.g. ::ffff:192.168.1.1)
    if (clean.startsWith("::ffff:") || clean.startsWith("0:0:0:0:0:ffff:")) {
        val mappedIpv4 = clean.substringAfterLast(":")
        return isTargetLocalOrPrivate(mappedIpv4)
    }

    // RFC 1122: Loopback 127.0.0.0/8 & Current Network 0.0.0.0/8
    if (clean.startsWith("127.") || clean.startsWith("0.")) return true
    
    // RFC 1918: 10.0.0.0/8 & 192.168.0.0/16
    if (clean.startsWith("10.") || clean.startsWith("192.168.")) return true
    
    // RFC 3927: IPv4 Link-Local 169.254.0.0/16
    if (clean.startsWith("169.254.")) return true

    // RFC 5737: Documentation and Examples (192.0.2.0/24, 198.51.100.0/24, 203.0.113.0/24)
    if (clean.startsWith("192.0.2.") || clean.startsWith("198.51.100.") || clean.startsWith("203.0.113.")) return true

    // RFC 2544 / RFC 6815: Benchmark Testing (198.18.0.0/15)
    if (clean.startsWith("198.18.") || clean.startsWith("198.19.")) return true
    
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

    // RFC 1112 / RFC 6890: IPv4 Multicast 224.0.0.0/4 & Reserved 240.0.0.0/4
    val firstOctet = clean.substringBefore(".").toIntOrNull()
    if (firstOctet != null && firstOctet in 224..255) return true

    // IPv6 checks (ensure presence of ':')
    if (clean.contains(":")) {
        // RFC 4291: Multicast ff00::/8
        if (clean.startsWith("ff")) return true
        // RFC 4291: Link-Local fe80::/10 (fe80 - febf)
        if (clean.startsWith("fe8") || clean.startsWith("fe9") || clean.startsWith("fea") || clean.startsWith("feb")) return true
        // RFC 4193: Unique Local Address (ULA) fc00::/7 (fc00 - fdff)
        if (clean.startsWith("fc") || clean.startsWith("fd")) return true
        // RFC 3849: Documentation Prefix 2001:db8::/32
        if (clean.startsWith("2001:db8:") || clean.startsWith("2001:0db8:")) return true
        // RFC 5180 / RFC 7343: IPv6 Benchmark Testing 2001:2::/48
        if (clean.startsWith("2001:2:") || clean.startsWith("2001:0002:")) return true
        // RFC 6666: Discard Prefix 100::/64
        if (clean.startsWith("100::") || clean.startsWith("0100::")) return true
    }

    // Standard local/private/special-use domain name suffixes (RFC 6762, RFC 8375, RFC 6761, RFC 7686)
    if (clean.endsWith(".local") || clean.endsWith(".lan") || clean.endsWith(".internal") || 
        clean.endsWith(".home.arpa") || clean.endsWith(".localhost") || clean.endsWith(".test") || 
        clean.endsWith(".invalid") || clean.endsWith(".example") || clean.endsWith(".onion")) {
        return true
    }

    return false
}

val HOSTNAME_REGEX = Regex("""^([a-zA-Z0-9_]([a-zA-Z0-9_\-]{0,61}[a-zA-Z0-9_])?\.)*[a-zA-Z0-9_]([a-zA-Z0-9_\-]{0,61}[a-zA-Z0-9_])?$""")

/**
 * Validates whether an input target is a compliant IPv4, IPv6, or valid Hostname/FQDN.
 * Enforces RFC 1035 max domain length (253 characters) to eliminate ReDoS risks.
 */
fun isValidTarget(target: String): Boolean {
    val raw = target.trim()
    val clean = if (raw.endsWith(".") && raw.length > 1 && !raw.endsWith("..")) raw.dropLast(1) else raw
    if (clean.isBlank() || clean.length > 253) return false
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
    val raw = ip.trim().removePrefix("[").removeSuffix("]")
    if (raw.isBlank()) return false
    val clean = if (raw.contains("%")) raw.substringBefore("%") else raw
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

    // IPv4 check: 4 decimal octets 0..255 strictly containing only digits 0..9 without leading zeros
    val parts = clean.split(".")
    if (parts.size == 4) {
        return parts.all { part ->
            part.isNotEmpty() && part.all { it in '0'..'9' } &&
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

/**
 * Extracts referral WHOIS server hostname from response (RFC 3912 / IANA referrals).
 */
fun extractNextWhoisServer(response: String, currentServer: String): String? {
    val lines = response.lines()
    for (line in lines) {
        val trimmed = line.trim()
        val lower = trimmed.lowercase()
        if (lower.startsWith("whois:") ||
            lower.startsWith("whois server:") ||
            lower.startsWith("refer:") ||
            lower.startsWith("registrar whois server:") ||
            lower.startsWith("registry whois server:") ||
            lower.startsWith("referralserver:")
        ) {
            val candidate = trimmed.substringAfter(":")
                .trim()
                .removePrefix("whois://")
                .removePrefix("rwhois://")
                .substringBefore("/")
                .substringBefore(":")
                .trim()
            if (candidate.isNotBlank() && !candidate.equals(currentServer, ignoreCase = true) && !candidate.contains("iana.org", ignoreCase = true)) {
                return candidate
            }
        }
    }
    return null
}

