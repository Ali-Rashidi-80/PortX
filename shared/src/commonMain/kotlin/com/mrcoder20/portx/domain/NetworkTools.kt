package com.mrcoder20.portx.domain

import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.*

interface NetworkTools {
    suspend fun ping(host: String): Flow<PingResult>
    suspend fun pingTcp(host: String, port: Int = 80, timeoutMs: Int = 2000): Flow<PingResult>
    suspend fun dnsLookup(host: String): List<String>
    suspend fun dnsResolve(host: String): DnsResolutionResult
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

data class DnsRecord(
    val type: String, // A, AAAA, MX, TXT, NS, CNAME, SOA, CAA, PTR, SRV
    val name: String,
    val value: String,
    val ttl: Long = 300,
    val priority: Int? = null,
    val provider: String = "DNS"
)

data class DnsResolutionResult(
    val target: String,
    val records: List<DnsRecord> = emptyList(),
    val serverUsed: String = "DNS Resolver",
    val responseTimeMs: Long = 0,
    val hasDnssec: Boolean = false,
    val rawDigOutput: String = ""
) {
    val ips: List<String>
        get() = records.filter { it.type == "A" || it.type == "AAAA" }.map { it.value }.distinct()
}

data class SubnetInfo(
    val ip: String,
    val subnetMask: String = "255.255.255.0",
    val cidr: String = "24",
    val networkAddress: String = "192.168.1.0",
    val broadcastAddress: String = "192.168.1.255",
    val hostRangeStart: String = "192.168.1.1",
    val hostRangeEnd: String = "192.168.1.254",
    val usableHostsCount: Int = 254
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

/**
 * Heuristic detector for Web Application Firewalls (WAF), SYN-proxies, and TCP Tarpits.
 * Identifies environments where middleboxes or cloud proxies reflexively acknowledge all scanned ports.
 */
fun isSuspectedTarpitOrWaf(
    target: String = "",
    openPorts: Collection<Int>,
    banners: Map<Int, String> = emptyMap(),
    osFingerprint: String? = null
): Boolean {
    if (osFingerprint?.contains("WAF", ignoreCase = true) == true ||
        osFingerprint?.contains("SYN-Proxy", ignoreCase = true) == true) {
        return true
    }
    val allBannersLower = banners.values.joinToString(" ").lowercase()
    if (allBannersLower.contains("cloudflare")) {
        return true
    }

    val totalPorts = openPorts.size
    if (totalPorts < 40) return false

    val isLocal = if (target.isNotBlank()) isTargetLocalOrPrivate(target) else false
    if (isLocal) return false

    val ports = openPorts.toSet()
    val nonBlankBannerCount = banners.count { it.value.isNotBlank() }
    val bannerRatio = if (totalPorts > 0) nonBlankBannerCount.toDouble() / totalPorts.toDouble() else 0.0

    return ports.containsAll(listOf(1, 2, 3, 4, 5, 6, 7)) || bannerRatio < 0.08
}

/**
 * Formats an IPv4 or IPv6 address into its standard reverse DNS in-addr.arpa or ip6.arpa domain name.
 */
fun formatReverseDnsArpa(ip: String): String {
    val clean = ip.trim().removePrefix("[").removeSuffix("]").substringBefore("%")
    if (clean.contains(".")) {
        val parts = clean.split(".")
        if (parts.size == 4) {
            return "${parts[3]}.${parts[2]}.${parts[1]}.${parts[0]}.in-addr.arpa"
        }
    } else if (clean.contains(":")) {
        val tokens = clean.split(":")
        val expandedTokens = mutableListOf<String>()
        val doubleColonIndex = tokens.indexOf("")
        if (doubleColonIndex != -1) {
            val before = tokens.subList(0, doubleColonIndex).filter { it.isNotEmpty() }
            val after = tokens.subList(doubleColonIndex, tokens.size).filter { it.isNotEmpty() }
            val missing = 8 - before.size - after.size
            expandedTokens.addAll(before)
            repeat(missing) { expandedTokens.add("0") }
            expandedTokens.addAll(after)
        } else {
            expandedTokens.addAll(tokens)
        }
        val fullHex = expandedTokens.joinToString("") { it.padStart(4, '0') }
        val reversedNibbles = fullHex.reversed().map { it.toString() }.joinToString(".")
        return "$reversedNibbles.ip6.arpa"
    }
    return clean
}

/**
 * Calculates Subnet, CIDR, Network, Broadcast, and Host Range information for a given IPv4.
 */
fun calculateSubnetInfo(ip: String): SubnetInfo {
    val clean = ip.trim().removePrefix("[").removeSuffix("]").substringBefore("%")
    if (clean.matches(Regex("""^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$"""))) {
        val parts = clean.split(".").mapNotNull { it.toIntOrNull() }
        if (parts.size == 4 && parts.all { it in 0..255 }) {
            val first = parts[0]
            val (mask, cidr) = when {
                first in 1..126 -> Pair("255.0.0.0", "8")
                first in 128..191 -> Pair("255.255.0.0", "16")
                else -> Pair("255.255.255.0", "24")
            }
            val netAddr = when (cidr) {
                "8" -> "${parts[0]}.0.0.0"
                "16" -> "${parts[0]}.${parts[1]}.0.0"
                else -> "${parts[0]}.${parts[1]}.${parts[2]}.0"
            }
            val bcastAddr = when (cidr) {
                "8" -> "${parts[0]}.255.255.255"
                "16" -> "${parts[0]}.${parts[1]}.255.255"
                else -> "${parts[0]}.${parts[1]}.${parts[2]}.255"
            }
            val startHost = when (cidr) {
                "8" -> "${parts[0]}.0.0.1"
                "16" -> "${parts[0]}.${parts[1]}.0.1"
                else -> "${parts[0]}.${parts[1]}.${parts[2]}.1"
            }
            val endHost = when (cidr) {
                "8" -> "${parts[0]}.255.255.254"
                "16" -> "${parts[0]}.${parts[1]}.255.254"
                else -> "${parts[0]}.${parts[1]}.${parts[2]}.254"
            }
            val count = when (cidr) {
                "8" -> 16777214
                "16" -> 65534
                else -> 254
            }
            return SubnetInfo(
                ip = clean,
                subnetMask = mask,
                cidr = cidr,
                networkAddress = netAddr,
                broadcastAddress = bcastAddr,
                hostRangeStart = startHost,
                hostRangeEnd = endHost,
                usableHostsCount = count
            )
        }
    }
    return SubnetInfo(ip = clean)
}

/**
 * Formats a DNS Resolution Result into standard RFC Dig / BIND terminal text.
 */
fun formatDigOutput(
    domain: String,
    server: String,
    responseTimeMs: Long,
    records: List<DnsRecord>,
    hasDnssec: Boolean
): String {
    val sb = StringBuilder()
    sb.append("; <<>> PortX Cyber Dig Engine v5.2 <<>> $domain\n")
    sb.append(";; Got answer:\n")
    val flags = if (hasDnssec) "qr rd ra ad" else "qr rd ra"
    sb.append(";; ->>HEADER<<- opcode: QUERY, status: NOERROR, flags: $flags\n")
    sb.append(";; QUERY: 1, ANSWER: ${records.size}, AUTHORITY: 0, ADDITIONAL: 0\n\n")
    sb.append(";; QUESTION SECTION:\n")
    sb.append(";$domain.\t\t\tIN\tANY\n\n")
    sb.append(";; ANSWER SECTION:\n")
    if (records.isEmpty()) {
        sb.append("; (No matching DNS records returned)\n")
    } else {
        records.forEach { r ->
            val priorityPart = if (r.priority != null) "${r.priority} " else ""
            val formattedValue = if (r.type == "TXT" && !r.value.startsWith("\"")) "\"${r.value}\"" else r.value
            sb.append("${r.name}.\t\t${r.ttl}\tIN\t${r.type.padEnd(5)}\t$priorityPart$formattedValue\n")
        }
    }
    sb.append("\n;; Query time: $responseTimeMs msec\n")
    sb.append(";; SERVER: $server\n")
    sb.append(";; WHEN: PortX Cyber Suite Engine\n")
    sb.append(";; MSG SIZE  rcvd: ${records.size * 32 + 84}\n")
    return sb.toString()
}

/**
 * High-resilience multi-record DoH Resolver.
 * Queries Cloudflare, Google, Quad9, and fallback endpoints over HTTPS and direct IP to bypass SNI blocking.
 */
suspend fun queryDohMultiRecords(
    cleanHost: String,
    recordTypes: List<String> = listOf("A", "AAAA", "MX", "TXT", "NS", "CNAME", "SOA", "CAA")
): Pair<List<DnsRecord>, Pair<String, Boolean>> {
    val providers = listOf(
        Pair("Cloudflare DoH [1.1.1.1]", "https://1.1.1.1/dns-query"),
        Pair("Google DoH [8.8.8.8]", "https://8.8.8.8/resolve"),
        Pair("Cloudflare FQDN", "https://cloudflare-dns.com/dns-query"),
        Pair("Google FQDN", "https://dns.google/resolve"),
        Pair("Quad9 DoH [9.9.9.9]", "https://dns.quad9.net/dns-query"),
        Pair("Shecan DoH", "https://doh.shecan.ir/dns-query"),
        Pair("AdGuard DoH", "https://dns.adguard-dns.com/dns-query")
    )

    val client = SecurityHarden.createSecureClient()
    val allRecords = mutableListOf<DnsRecord>()
    var successfulProvider = "DoH Resolver"
    var isDnssec = false

    try {
        for ((providerName, baseUrl) in providers) {
            val batchRecords = mutableListOf<DnsRecord>()
            var providerDnssec = false

            for (type in recordTypes) {
                try {
                    val url = "$baseUrl?name=$cleanHost&type=$type"
                    val response = client.get(url) {
                        header("Accept", "application/dns-json")
                    }
                    if (response.status.value in 200..299) {
                        val body = response.bodyAsText()
                        val json = Json.parseToJsonElement(body).jsonObject
                        if (json["AD"]?.jsonPrimitive?.booleanOrNull == true) {
                            providerDnssec = true
                        }
                        val answers = json["Answer"]?.jsonArray
                        if (answers != null && answers.isNotEmpty()) {
                            for (ans in answers) {
                                val ansObj = ans.jsonObject
                                val typeNum = ansObj["type"]?.jsonPrimitive?.intOrNull
                                val resolvedTypeName = when (typeNum) {
                                    1 -> "A"
                                    28 -> "AAAA"
                                    5 -> "CNAME"
                                    15 -> "MX"
                                    16 -> "TXT"
                                    2 -> "NS"
                                    6 -> "SOA"
                                    257 -> "CAA"
                                    12 -> "PTR"
                                    33 -> "SRV"
                                    else -> type
                                }
                                val ttl = ansObj["TTL"]?.jsonPrimitive?.longOrNull ?: 300L
                                val rawName = ansObj["name"]?.jsonPrimitive?.contentOrNull?.removeSuffix(".") ?: cleanHost
                                val data = ansObj["data"]?.jsonPrimitive?.contentOrNull?.trim() ?: ""
                                if (data.isNotBlank()) {
                                    if (resolvedTypeName == "MX") {
                                        val parts = data.split(Regex("""\s+"""))
                                        val priority = parts.getOrNull(0)?.toIntOrNull()
                                        val mxHost = parts.getOrNull(1)?.removeSuffix(".") ?: data
                                        batchRecords.add(DnsRecord(type = "MX", name = rawName, value = mxHost, ttl = ttl, priority = priority, provider = providerName))
                                    } else if (resolvedTypeName == "TXT") {
                                        val cleanTxt = data.removePrefix("\"").removeSuffix("\"").replace("\\\"", "\"")
                                        batchRecords.add(DnsRecord(type = "TXT", name = rawName, value = cleanTxt, ttl = ttl, provider = providerName))
                                    } else {
                                        val cleanVal = data.removeSuffix(".")
                                        batchRecords.add(DnsRecord(type = resolvedTypeName, name = rawName, value = cleanVal, ttl = ttl, provider = providerName))
                                    }
                                }
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Try next record type
                }
            }

            if (batchRecords.isNotEmpty()) {
                allRecords.addAll(batchRecords)
                successfulProvider = providerName
                isDnssec = providerDnssec
                break
            }
        }
    } finally {
        try { client.close() } catch (_: Exception) {}
    }

    return Pair(allRecords, Pair(successfulProvider, isDnssec))
}

/**
 * Multi-Tier Comprehensive DNS Resolver.
 * Resolves all record types (A, AAAA, MX, TXT, NS, CNAME, SOA, CAA, PTR) with fallback to JNDI/System.
 */
suspend fun performComprehensiveDnsResolve(
    host: String,
    jndiOrSystemResolver: (suspend (String) -> List<DnsRecord>)? = null,
    systemReverseResolver: (suspend (String) -> String?)? = null
): DnsResolutionResult {
    val cleanHost = sanitizeHost(host)
    if (cleanHost.isBlank()) {
        return DnsResolutionResult(target = host, serverUsed = "None", rawDigOutput = "; Error: Empty Host")
    }

    val start = System.currentTimeMillis()

    // 1. IP Target: Reverse DNS (PTR) Resolution
    if (isValidIpAddress(cleanHost)) {
        val reverseHost = systemReverseResolver?.let {
            try { it(cleanHost) } catch (_: Exception) { null }
        }
        val records = mutableListOf<DnsRecord>()
        var server = "Reverse DNS Resolver"
        if (!reverseHost.isNullOrBlank() && reverseHost != cleanHost) {
            records.add(DnsRecord(type = "PTR", name = cleanHost, value = reverseHost, provider = "System Reverse DNS"))
            server = "System Canonical Resolver"
        } else {
            val arpaName = formatReverseDnsArpa(cleanHost)
            val (dohRecords, prov) = try {
                queryDohMultiRecords(arpaName, listOf("PTR"))
            } catch (_: Exception) {
                Pair(emptyList<DnsRecord>(), Pair("DoH", false))
            }
            if (dohRecords.isNotEmpty()) {
                records.addAll(dohRecords.map { it.copy(name = cleanHost) })
                server = prov.first
            } else {
                records.add(DnsRecord(type = "PTR", name = cleanHost, value = cleanHost, provider = "Direct Host"))
            }
        }
        val elapsed = (System.currentTimeMillis() - start).coerceAtLeast(1L)
        val dig = formatDigOutput(cleanHost, server, elapsed, records, false)
        return DnsResolutionResult(target = cleanHost, records = records, serverUsed = server, responseTimeMs = elapsed, rawDigOutput = dig)
    }

    // 2. Domain Target: Try Encrypted DoH Multi-Record Query (A, AAAA, MX, TXT, NS, CNAME, SOA, CAA)
    val (dohRecords, provInfo) = try {
        queryDohMultiRecords(cleanHost)
    } catch (_: Exception) {
        Pair(emptyList<DnsRecord>(), Pair("System DNS", false))
    }

    val records = mutableListOf<DnsRecord>()
    records.addAll(dohRecords)
    var serverUsed = provInfo.first
    var hasDnssec = provInfo.second

    // 3. Fallback/Augment via JVM JNDI / System Resolver if DoH produced few/no records
    if (records.isEmpty() && jndiOrSystemResolver != null) {
        val jndiResults = try {
            jndiOrSystemResolver(cleanHost)
        } catch (_: Exception) {
            emptyList()
        }
        if (jndiResults.isNotEmpty()) {
            records.addAll(jndiResults)
            serverUsed = "System JNDI / Port 53"
        }
    }

    // Deduplicate records by type + value
    val distinctRecords = records.distinctBy { "${it.type}_${it.value.lowercase()}" }
        .sortedWith(compareBy({
            when (it.type) {
                "A" -> 1
                "AAAA" -> 2
                "CNAME" -> 3
                "MX" -> 4
                "NS" -> 5
                "TXT" -> 6
                "SOA" -> 7
                "CAA" -> 8
                "PTR" -> 9
                else -> 10
            }
        }, { it.priority ?: 999 }, { it.value }))

    val elapsed = (System.currentTimeMillis() - start).coerceAtLeast(1L)
    val dig = formatDigOutput(cleanHost, serverUsed, elapsed, distinctRecords, hasDnssec)

    return DnsResolutionResult(
        target = cleanHost,
        records = distinctRecords,
        serverUsed = serverUsed,
        responseTimeMs = elapsed,
        hasDnssec = hasDnssec,
        rawDigOutput = dig
    )
}

/**
 * Backward-compatible helper for basic IP-only DNS lookups.
 */
suspend fun queryDoh(cleanHost: String, recordType: String = "A"): List<String> {
    val (records, _) = queryDohMultiRecords(cleanHost, listOf(recordType))
    return records.map { it.value }.distinct()
}

/**
 * Backward-compatible helper for basic resilient DNS lookups.
 */
suspend fun performResilientDnsLookup(
    host: String,
    systemResolver: suspend (String) -> List<String>,
    systemReverseResolver: (suspend (String) -> String?)? = null,
    dohResolver: (suspend (String, String) -> List<String>)? = null
): List<String> {
    val cleanHost = sanitizeHost(host)
    if (cleanHost.isBlank()) return emptyList()

    if (isValidIpAddress(cleanHost)) {
        val reverseHost = systemReverseResolver?.let {
            try { it(cleanHost) } catch (_: Exception) { null }
        }
        if (!reverseHost.isNullOrBlank() && reverseHost != cleanHost) {
            return listOf(reverseHost)
        }
        val arpaName = formatReverseDnsArpa(cleanHost)
        val dohPtr = try {
            if (dohResolver != null) dohResolver(arpaName, "PTR") else queryDoh(arpaName, "PTR")
        } catch (_: Exception) {
            emptyList()
        }
        if (dohPtr.isNotEmpty()) return dohPtr
        return listOf(cleanHost)
    }

    val systemResults = try {
        withTimeoutOrNull(2500) { systemResolver(cleanHost) } ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }
    if (systemResults.isNotEmpty()) return systemResults

    val dohLookup = dohResolver ?: { targetHost, recordType -> queryDoh(targetHost, recordType) }
    val aRecords = try { dohLookup(cleanHost, "A") } catch (_: Exception) { emptyList() }
    val aaaaRecords = try { dohLookup(cleanHost, "AAAA") } catch (_: Exception) { emptyList() }
    return (aRecords + aaaaRecords).filter { it.isNotBlank() && isValidIpAddress(it) }.distinct()
}




