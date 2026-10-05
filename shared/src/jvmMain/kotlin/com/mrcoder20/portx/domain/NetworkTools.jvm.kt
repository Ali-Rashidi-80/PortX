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
        val cmd = if (isWindows) {
            listOf("ping", "-n", "1", "-w", timeoutMs.toString(), ipStr)
        } else if (os.contains("mac")) {
            listOf("ping", "-c", "1", "-W", timeoutMs.toString(), ipStr)
        } else {
            val timeoutSec = maxOf(1, timeoutMs / 1000)
            listOf("ping", "-c", "1", "-W", timeoutSec.toString(), ipStr)
        }

        try {
            val start = System.currentTimeMillis()
            val process = ProcessBuilder(cmd).redirectErrorStream(true).start()
            val finished = process.waitFor(timeoutMs + 1000L, TimeUnit.MILLISECONDS)
            if (!finished) {
                process.destroyForcibly()
                return PingAttempt(false, null, null)
            }
            val output = process.inputStream.bufferedReader().readText()
            val elapsed = System.currentTimeMillis() - start

            val isFailure = output.contains("100% loss", ignoreCase = true) ||
                    output.contains("100% packet loss", ignoreCase = true) ||
                    output.contains("Request timed out", ignoreCase = true) ||
                    output.contains("Destination host unreachable", ignoreCase = true)

            if (process.exitValue() == 0 && !isFailure) {
                val parsedTime = parseTimeFromPingOutput(output) ?: elapsed
                val parsedTtl = parseTtlFromPingOutput(output)
                return PingAttempt(true, parsedTime, parsedTtl)
            }
        } catch (e: Exception) {
            // ProcessBuilder fallback to address.isReachable
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

    override suspend fun dnsLookup(host: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val cleanHost = sanitizeHost(host)
            if (cleanHost.isBlank()) return@withContext emptyList()
            InetAddress.getAllByName(cleanHost)
                .mapNotNull { it.hostAddress }
                .filter { it.isNotBlank() }
                .distinct()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun whois(host: String): String = withContext(Dispatchers.IO) {
        try {
            val cleanHost = sanitizeHost(host).lowercase().removePrefix("www.")
            if (cleanHost.isBlank()) return@withContext "Error: Target host is empty"

            val socket = java.net.Socket()
            val result = try {
                socket.connect(java.net.InetSocketAddress("whois.iana.org", 43), 7000)
                socket.soTimeout = 7000
                val out = socket.getOutputStream()
                out.write((cleanHost + "\r\n").toByteArray(Charsets.UTF_8))
                out.flush()
                
                val reader = socket.getInputStream().bufferedReader(Charsets.UTF_8)
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line).append("\n")
                }
                sb.toString()
            } finally {
                try { socket.close() } catch (_: Exception) {}
            }
            
            if (result.contains("whois:", true)) {
                val nextServer = result.lines()
                    .find { it.contains("whois:", true) && !it.contains("iana.org") }
                    ?.substringAfter(":")?.trim() ?: return@withContext result
                
                if (nextServer.isBlank()) return@withContext result

                try {
                    val socket2 = java.net.Socket()
                    val redirectedResult = try {
                        socket2.connect(java.net.InetSocketAddress(nextServer, 43), 7000)
                        socket2.soTimeout = 7000
                        socket2.getOutputStream().write((cleanHost + "\r\n").toByteArray(Charsets.UTF_8))
                        socket2.getOutputStream().flush()
                        val reader2 = socket2.getInputStream().bufferedReader(Charsets.UTF_8)
                        val sb2 = StringBuilder()
                        var line2: String?
                        while (reader2.readLine().also { line2 = it } != null) {
                            sb2.append(line2).append("\n")
                        }
                        sb2.toString()
                    } finally {
                        try { socket2.close() } catch (_: Exception) {}
                    }
                    redirectedResult
                } catch (e: Exception) {
                    result + "\n\n[Authority Redirect to $nextServer failed: ${e.message}]"
                }
            } else {
                result
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            "WHOIS Resolution Error: ${e.localizedMessage}. Ensure you are entering a valid domain (e.g. google.com)."
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
                        if (ip.isNotEmpty()) return@withContext ip
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
