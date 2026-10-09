package com.mrcoder20.portx.domain

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.mrcoder20.portx.appContext
import io.ktor.client.statement.*
import io.ktor.client.request.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.concurrent.TimeUnit

class AndroidNetworkTools : NetworkTools {
    override suspend fun ping(host: String): Flow<PingResult> = flow {
        try {
            val cleanHost = sanitizeHost(host)
            if (cleanHost.isBlank()) {
                emit(PingResult(0, null, false, "Error: Target host is empty"))
                return@flow
            }

            val address = try {
                InetAddress.getByName(cleanHost)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }

            if (address == null) {
                emit(PingResult(0, null, false, "Error: Unknown host $cleanHost"))
                return@flow
            }

            val ipStr = address.hostAddress ?: cleanHost
            emit(PingResult(0, null, true, "Pinging $cleanHost [$ipStr] with 32 bytes of data:"))

            repeat(4) { i ->
                val attempt = executeAndroidPing(ipStr, address, 2000)
                if (attempt.isSuccess) {
                    val ttlPart = attempt.ttl?.let { " TTL=$it" } ?: ""
                    emit(PingResult(i + 1, attempt.timeMs, true, "Reply from $ipStr: bytes=32 time=${attempt.timeMs}ms$ttlPart"))
                } else {
                    emit(PingResult(i + 1, null, false, "Request timed out for $ipStr"))
                }
                kotlinx.coroutines.delay(800)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(PingResult(0, null, false, "Ping Error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    private fun executeAndroidPing(ipStr: String, address: InetAddress, timeoutMs: Int): PingAttempt {
        val timeoutSec = maxOf(1, timeoutMs / 1000)
        val isIpv6 = ipStr.contains(":")
        val pingBin = if (isIpv6 && java.io.File("/system/bin/ping6").exists()) "/system/bin/ping6" else "/system/bin/ping"
        var process: Process? = null
        try {
            val start = System.currentTimeMillis()
            val proc = ProcessBuilder(listOf(pingBin, "-c", "1", "-W", timeoutSec.toString(), ipStr))
                .redirectErrorStream(true)
                .start()
            process = proc
            val finished = proc.waitFor(timeoutMs + 1000L, TimeUnit.MILLISECONDS)
            if (!finished) {
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        proc.destroyForcibly()
                    } else {
                        proc.destroy()
                    }
                } catch (_: Throwable) { proc.destroy() }
                return PingAttempt(false, null, null)
            }
            val output = proc.inputStream.bufferedReader().readText()
            val elapsed = System.currentTimeMillis() - start

            val isFailure = output.contains("100% loss", ignoreCase = true) ||
                    output.contains("100% packet loss", ignoreCase = true) ||
                    output.contains("Request timed out", ignoreCase = true) ||
                    output.contains("Destination host unreachable", ignoreCase = true) ||
                    output.contains("Network is unreachable", ignoreCase = true) ||
                    output.contains("Permission denied", ignoreCase = true)

            if (proc.exitValue() == 0 && !isFailure) {
                val parsedTime = parseTimeFromPingOutput(output) ?: elapsed
                val parsedTtl = parseTtlFromPingOutput(output)
                return PingAttempt(true, parsedTime, parsedTtl)
            }
        } catch (e: Exception) {
            // Android exec failed or restricted, fallback to isReachable
        } finally {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    process?.destroyForcibly()
                } else {
                    process?.destroy()
                }
            } catch (_: Exception) {}
        }

        return try {
            val start = System.currentTimeMillis()
            val reachable = address.isReachable(timeoutMs)
            val elapsed = System.currentTimeMillis() - start
            if (reachable) {
                PingAttempt(true, elapsed, null)
            } else {
                PingAttempt(false, null, null)
            }
        } catch (e: Exception) {
            PingAttempt(false, null, null)
        }
    }

    override suspend fun pingTcp(host: String, port: Int, timeoutMs: Int): Flow<PingResult> = flow {
        try {
            val cleanHost = sanitizeHost(host)
            if (cleanHost.isBlank()) {
                emit(PingResult(0, null, false, "Error: Target host is empty"))
                return@flow
            }
            emit(PingResult(0, null, true, "TCP Ping to $cleanHost:$port (SYN/ACK Handshake):"))
            repeat(4) { i ->
                val start = System.currentTimeMillis()
                var socket: java.net.Socket? = null
                try {
                    socket = java.net.Socket()
                    socket.connect(java.net.InetSocketAddress(cleanHost, port), timeoutMs)
                    val elapsed = System.currentTimeMillis() - start
                    emit(PingResult(i + 1, elapsed, true, "Connected to $cleanHost:$port: time=${elapsed}ms TCP_SYN_ACK"))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    emit(PingResult(i + 1, null, false, "TCP connection to $cleanHost:$port failed: ${e.message ?: "timeout"}"))
                } finally {
                    try { socket?.close() } catch (_: Exception) {}
                }
                kotlinx.coroutines.delay(600)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(PingResult(0, null, false, "TCP Ping Error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun dnsLookup(host: String): List<String> = withContext(Dispatchers.IO) {
        performResilientDnsLookup(
            host = host,
            systemResolver = { cleanHost ->
                InetAddress.getAllByName(cleanHost)
                    .mapNotNull { it.hostAddress }
                    .filter { it.isNotBlank() }
                    .distinct()
            },
            systemReverseResolver = { cleanIp ->
                val addr = InetAddress.getByName(cleanIp)
                val canonical = addr.canonicalHostName
                if (canonical.isNotBlank() && canonical != cleanIp) canonical else null
            }
        )
    }

    override suspend fun dnsResolve(host: String): DnsResolutionResult = withContext(Dispatchers.IO) {
        performComprehensiveDnsResolve(
            host = host,
            jndiOrSystemResolver = { cleanHost ->
                try {
                    InetAddress.getAllByName(cleanHost)
                        .mapNotNull { it.hostAddress }
                        .filter { it.isNotBlank() }
                        .map { ip ->
                            val type = if (ip.contains(":")) "AAAA" else "A"
                            DnsRecord(type = type, name = cleanHost, value = ip, provider = "Android System Resolver")
                        }
                } catch (_: Exception) {
                    emptyList()
                }
            },
            systemReverseResolver = { cleanIp ->
                val addr = InetAddress.getByName(cleanIp)
                val canonical = addr.canonicalHostName
                if (canonical.isNotBlank() && canonical != cleanIp) canonical else null
            }
        )
    }

    override suspend fun whois(host: String): String = withContext(Dispatchers.IO) {
        val cleanHost = sanitizeHost(host).lowercase().removePrefix("www.")
        if (cleanHost.isBlank()) return@withContext "Error: Target host is empty"

        // 1. Primary on Android: RDAP over HTTPS (mobile carrier friendly)
        val client = SecurityHarden.createSecureClient()
        try {
            val rdapUrl = if (isValidIpAddress(cleanHost)) {
                "https://rdap.org/ip/$cleanHost"
            } else {
                "https://rdap.org/domain/$cleanHost"
            }
            val response: HttpResponse = client.get(rdapUrl)
            if (response.status.value in 200..299) {
                val body = response.bodyAsText()
                if (body.isNotBlank()) return@withContext "[RDAP Mobile]\n\n" + body.take(5000)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // RDAP failed, proceed to Port 43 fallback
        } finally {
            try { client.close() } catch (_: Exception) {}
        }

        // 2. Secondary fallback: Native Port 43 Socket query with iterative referral follow
        try {
            val visited = mutableSetOf<String>()
            var currentServer = if (isValidIpAddress(cleanHost)) "whois.arin.net" else "whois.iana.org"
            var bestContent: String? = null

            repeat(3) {
                if (visited.contains(currentServer)) return@withContext bestContent ?: "WHOIS data not available."
                visited.add(currentServer)

                val socket = java.net.Socket()
                val content = try {
                    socket.connect(java.net.InetSocketAddress(currentServer, 43), 6000)
                    socket.soTimeout = 6000
                    socket.getOutputStream().write((cleanHost + "\r\n").toByteArray(Charsets.UTF_8))
                    socket.getOutputStream().flush()
                    val reader = socket.getInputStream().bufferedReader(Charsets.UTF_8)
                    val sb = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        sb.append(line).append("\n")
                    }
                    val res = sb.toString()
                    if (res.isNotBlank()) res else null
                } catch (_: Exception) {
                    null
                } finally {
                    try { socket.close() } catch (_: Exception) {}
                }

                if (content != null) {
                    bestContent = content
                    val nextServer = extractNextWhoisServer(content, currentServer)
                    if (nextServer.isNullOrBlank() || visited.contains(nextServer)) {
                        return@withContext bestContent
                    }
                    currentServer = nextServer
                } else {
                    if (bestContent != null) return@withContext bestContent
                }
            }
            if (bestContent != null) return@withContext bestContent
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {}

        "WHOIS data not available for $cleanHost via RDAP or Port 43."
    }

    override suspend fun getPublicIp(): String? = withContext(Dispatchers.IO) {
        val providers = listOf(
            "https://api.ipify.org",
            "https://ifconfig.me/ip",
            "https://icanhazip.com",
            "https://ident.me"
        )
        
        val client = SecurityHarden.createSecureClient()
        try {
            providers.forEach { url ->
                try {
                    val response: HttpResponse = client.get(url)
                    if (response.status.value in 200..299) {
                        val ip = response.bodyAsText().trim()
                        if (isValidIpAddress(ip)) return@withContext ip
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Try next provider
                }
            }
        } finally {
            try { client.close() } catch (_: Exception) {}
        }
        null
    }

    override fun getLocalIpInfo(): LocalIpInfo {
        return try {
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNetwork = cm?.activeNetwork
            val caps = cm?.getNetworkCapabilities(activeNetwork)
            val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

            // Authoritative route lookup via Android LinkProperties
            val linkProps = activeNetwork?.let { cm.getLinkProperties(it) }
            val activeIpv4 = linkProps?.linkAddresses
                ?.mapNotNull { it.address }
                ?.firstOrNull { it is java.net.Inet4Address && !it.isLoopbackAddress && !it.isLinkLocalAddress }
                ?.hostAddress

            if (activeIpv4 != null) {
                val ifName = linkProps.interfaceName ?: (if (isWifi) "Wi-Fi" else "Cellular")
                return LocalIpInfo(activeIpv4, ifName, isWifi)
            }

            // Fallback: Prioritized interface enumeration
            data class InterfaceCandidate(
                val ip: String,
                val name: String,
                val isWifi: Boolean,
                val priority: Int
            )
            val candidates = mutableListOf<InterfaceCandidate>()

            val interfaces = NetworkInterface.getNetworkInterfaces()
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    val iface = interfaces.nextElement()
                    if (iface.isLoopback || !iface.isUp) continue

                    val nameLower = iface.name.lowercase()
                    val dispLower = iface.displayName.lowercase()
                    val combined = "$nameLower $dispLower"

                    val isCandidateWifi = combined.contains("wlan") || combined.contains("wi-fi")
                    val isCandidateCellular = combined.contains("rmnet") || combined.contains("ccmni") || combined.contains("pdp")
                    val isCandidateVirtual = combined.contains("dummy") || combined.contains("tun") || combined.contains("tap") || combined.contains("veth")

                    val priority = when {
                        isCandidateVirtual -> 5
                        isCandidateWifi -> 40
                        isCandidateCellular -> 30
                        else -> 15
                    }

                    val addresses = iface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val addr = addresses.nextElement()
                        if (addr.isLoopbackAddress || addr.isLinkLocalAddress) continue
                        val hostAddr = addr.hostAddress ?: continue
                        if (hostAddr.contains(":") || hostAddr.startsWith("169.254.") || hostAddr == "0.0.0.0") continue

                        candidates.add(
                            InterfaceCandidate(
                                ip = hostAddr,
                                name = iface.displayName,
                                isWifi = isCandidateWifi || isWifi,
                                priority = priority
                            )
                        )
                    }
                }
            }

            val best = candidates.maxByOrNull { it.priority }
            if (best != null) {
                LocalIpInfo(best.ip, best.name, best.isWifi)
            } else {
                LocalIpInfo("127.0.0.1", "Loopback", false)
            }
        } catch (e: Exception) {
            LocalIpInfo("0.0.0.0", "Error", false)
        }
    }
}

actual fun getNetworkTools(): NetworkTools = AndroidNetworkTools()
