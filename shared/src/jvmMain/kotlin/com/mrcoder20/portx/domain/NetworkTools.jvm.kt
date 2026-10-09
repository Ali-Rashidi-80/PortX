package com.mrcoder20.portx.domain

import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.Scanner
import java.util.concurrent.TimeUnit

class JvmNetworkTools : NetworkTools {
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
                val attempt = executeJvmPing(ipStr, address, 2000)
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
            emit(PingResult(0, null, false, "Ping Error: ${e.localizedMessage ?: e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    private fun executeJvmPing(ipStr: String, address: InetAddress, timeoutMs: Int): PingAttempt {
        val os = System.getProperty("os.name")?.lowercase() ?: ""
        val isWindows = os.contains("win")
        val isIpv6 = ipStr.contains(":")
        val cmd = if (isWindows) {
            listOf("ping", "-n", "1", "-w", timeoutMs.toString(), ipStr)
        } else if (os.contains("mac")) {
            if (isIpv6) listOf("ping6", "-c", "1", "-W", timeoutMs.toString(), ipStr)
            else listOf("ping", "-c", "1", "-W", timeoutMs.toString(), ipStr)
        } else {
            val timeoutSec = maxOf(1, timeoutMs / 1000)
            if (isIpv6) listOf("ping6", "-c", "1", "-W", timeoutSec.toString(), ipStr)
            else listOf("ping", "-c", "1", "-W", timeoutSec.toString(), ipStr)
        }

        var process: Process? = null
        try {
            val start = System.currentTimeMillis()
            val proc = ProcessBuilder(cmd).redirectErrorStream(true).start()
            process = proc
            val finished = proc.waitFor(timeoutMs + 1000L, TimeUnit.MILLISECONDS)
            if (!finished) {
                proc.destroyForcibly()
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
            // ProcessBuilder fallback to address.isReachable
        } finally {
            try { process?.destroyForcibly() } catch (_: Exception) {}
        }

        // Graceful fallback to InetAddress.isReachable
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
                var socket: Socket? = null
                try {
                    socket = Socket()
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
                queryJvmJndiDns(cleanHost)
            },
            systemReverseResolver = { cleanIp ->
                val addr = InetAddress.getByName(cleanIp)
                val canonical = addr.canonicalHostName
                if (canonical.isNotBlank() && canonical != cleanIp) canonical else null
            }
        )
    }

    private fun queryJvmJndiDns(cleanHost: String): List<DnsRecord> {
        val results = mutableListOf<DnsRecord>()
        val types = listOf("A", "AAAA", "MX", "TXT", "NS", "CNAME", "SOA")
        val env = java.util.Hashtable<String, String>()
        env["java.naming.factory.initial"] = "com.sun.jndi.dns.DnsContextFactory"
        env["java.naming.provider.url"] = "dns:"
        env["com.sun.jndi.dns.timeout.initial"] = "1500"
        env["com.sun.jndi.dns.timeout.retries"] = "1"
        try {
            val ictx = javax.naming.directory.InitialDirContext(env)
            for (t in types) {
                try {
                    val attrs = ictx.getAttributes(cleanHost, arrayOf(t))
                    val attr = attrs.get(t)
                    if (attr != null) {
                        val enum = attr.all
                        while (enum.hasMore()) {
                            val raw = enum.next().toString()
                            if (t == "MX") {
                                val parts = raw.trim().split(Regex("""\s+"""))
                                val priority = parts.getOrNull(0)?.toIntOrNull()
                                val host = parts.getOrNull(1)?.removeSuffix(".") ?: raw
                                results.add(DnsRecord(type = "MX", name = cleanHost, value = host, priority = priority, provider = "System JNDI"))
                            } else if (t == "TXT") {
                                val cleanTxt = raw.removePrefix("\"").removeSuffix("\"").replace("\\\"", "\"")
                                results.add(DnsRecord(type = "TXT", name = cleanHost, value = cleanTxt, provider = "System JNDI"))
                            } else {
                                val cleanVal = raw.removeSuffix(".")
                                results.add(DnsRecord(type = t, name = cleanHost, value = cleanVal, provider = "System JNDI"))
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}

        // Fallback: If JNDI couldn't resolve A/AAAA, add standard InetAddress.getAllByName
        if (results.none { it.type == "A" || it.type == "AAAA" }) {
            try {
                val addrs = InetAddress.getAllByName(cleanHost)
                for (a in addrs) {
                    val ip = a.hostAddress ?: continue
                    val type = if (ip.contains(":")) "AAAA" else "A"
                    results.add(DnsRecord(type = type, name = cleanHost, value = ip, provider = "System Resolver"))
                }
            } catch (_: Exception) {}
        }
        return results
    }

    override suspend fun whois(host: String): String = withContext(Dispatchers.IO) {
        val cleanHost = sanitizeHost(host).lowercase().removePrefix("www.")
        if (cleanHost.isBlank()) return@withContext "Error: Target host is empty"

        // Primary: Native WHOIS Port 43 Socket with iterative referral follow
        val socketResult = tryQueryWhoisSocket(cleanHost)
        if (socketResult != null && socketResult.isNotBlank()) {
            return@withContext socketResult
        }

        // Secondary: RDAP Fallback over HTTPS (resilient to Port 43 ISP / Firewall blocks)
        val rdapResult = tryQueryRdap(cleanHost)
        if (rdapResult != null && rdapResult.isNotBlank()) {
            return@withContext rdapResult
        }

        "WHOIS Resolution Error: Port 43 socket and RDAP queries were unavailable for $cleanHost."
    }

    private fun tryQueryWhoisSocket(cleanHost: String): String? {
        val visited = mutableSetOf<String>()
        var currentServer = if (isValidIpAddress(cleanHost)) "whois.arin.net" else "whois.iana.org"
        var bestResult: String? = null

        repeat(3) {
            if (visited.contains(currentServer)) return bestResult
            visited.add(currentServer)

            val raw = tryQuerySingleWhoisServer(currentServer, cleanHost) ?: return bestResult
            bestResult = raw

            val nextServer = extractNextWhoisServer(raw, currentServer)
            if (nextServer.isNullOrBlank() || visited.contains(nextServer)) {
                return bestResult
            }
            currentServer = nextServer
        }
        return bestResult
    }

    private fun tryQuerySingleWhoisServer(server: String, query: String, timeoutMs: Int = 6000): String? {
        val socket = java.net.Socket()
        return try {
            socket.connect(java.net.InetSocketAddress(server, 43), timeoutMs)
            socket.soTimeout = timeoutMs
            val out = socket.getOutputStream()
            out.write((query + "\r\n").toByteArray(Charsets.UTF_8))
            out.flush()
            val reader = socket.getInputStream().bufferedReader(Charsets.UTF_8)
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line).append("\n")
            }
            val content = sb.toString()
            if (content.isNotBlank()) content else null
        } catch (_: Exception) {
            null
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }



    private suspend fun tryQueryRdap(cleanHost: String): String? {
        val client = SecurityHarden.createSecureClient()
        return try {
            val rdapUrl = if (isValidIpAddress(cleanHost)) {
                "https://rdap.org/ip/$cleanHost"
            } else {
                "https://rdap.org/domain/$cleanHost"
            }
            val response = client.get(rdapUrl)
            if (response.status.value in 200..299) {
                "[RDAP Record via HTTPS]\n\n" + response.bodyAsText().take(6000)
            } else null
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        } finally {
            try { client.close() } catch (_: Exception) {}
        }
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
                    val response = client.get(url)
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
        data class InterfaceCandidate(
            val ip: String,
            val name: String,
            val isWifi: Boolean,
            val priority: Int
        )

        val candidates = mutableListOf<InterfaceCandidate>()

        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    val iface = interfaces.nextElement()
                    if (iface.isLoopback || !iface.isUp) continue
                    
                    val dispName = iface.displayName.lowercase()
                    val ifName = iface.name.lowercase()
                    val combinedName = "$dispName $ifName"

                    val isVirtual = combinedName.contains("veth") ||
                            combinedName.contains("wsl") ||
                            combinedName.contains("docker") ||
                            combinedName.contains("vmware") ||
                            combinedName.contains("virtualbox") ||
                            combinedName.contains("vbox") ||
                            combinedName.contains("hyper-v") ||
                            combinedName.contains("vethernet") ||
                            combinedName.contains("tap") ||
                            combinedName.contains("tun") ||
                            combinedName.contains("tailscale") ||
                            combinedName.contains("wireguard")

                    val isWifi = combinedName.contains("wi-fi") ||
                            combinedName.contains("wlan") ||
                            combinedName.contains("wireless") ||
                            combinedName.contains("802.11")

                    val isEthernet = combinedName.contains("ethernet") ||
                            combinedName.contains("eth") ||
                            combinedName.contains("en") ||
                            combinedName.contains("lan") ||
                            combinedName.contains("gigabit")

                    val basePriority = when {
                        isVirtual -> 5
                        isWifi -> 40
                        isEthernet -> 30
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
                                isWifi = isWifi,
                                priority = basePriority
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            println("NetworkTools.jvm: Interface resolution warning: ${e.message}")
        }

        val best = candidates.maxByOrNull { it.priority }
        return if (best != null) {
            LocalIpInfo(best.ip, best.name, best.isWifi)
        } else {
            LocalIpInfo("127.0.0.1", "Loopback", false)
        }
    }
}

actual fun getNetworkTools(): NetworkTools = JvmNetworkTools()
