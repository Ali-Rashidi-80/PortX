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
            val socket = Socket("whois.iana.org", 43)
            socket.soTimeout = 7000
            val out = socket.getOutputStream()
            out.write((cleanHost + "\r\n").toByteArray())
            out.flush()
            
            val scanner = Scanner(socket.getInputStream())
            val sb = StringBuilder()
            while (scanner.hasNextLine()) {
                sb.append(scanner.nextLine()).append("\n")
            }
            socket.close()
            
            val result = sb.toString()
            if (result.contains("whois:", true)) {
                val nextServer = result.lines()
                    .find { it.contains("whois:", true) && !it.contains("iana.org") }
                    ?.substringAfter(":")?.trim() ?: return@withContext result
                
                if (nextServer.isBlank()) return@withContext result

                try {
                    val socket2 = Socket(nextServer, 43)
                    socket2.soTimeout = 7000
                    socket2.getOutputStream().write((cleanHost + "\r\n").toByteArray())
                    val scanner2 = Scanner(socket2.getInputStream())
                    val sb2 = StringBuilder()
                    while (scanner2.hasNextLine()) {
                        sb2.append(scanner2.nextLine()).append("\n")
                    }
                    socket2.close()
                    sb2.toString()
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
                // Try next
            }
        }
        null
    }

    override fun getLocalIpInfo(): LocalIpInfo {
        var ip = "127.0.0.1"
        var name = "unknown"
        var isWifi = false
        
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    val iface = interfaces.nextElement()
                    if (iface.isLoopback || !iface.isUp) continue
                    
                    val dispName = iface.displayName.lowercase()
                    if (dispName.contains("wi-fi") || dispName.contains("wlan") || iface.name.lowercase().contains("wlan")) {
                        isWifi = true
                    }

                    val addresses = iface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val addr = addresses.nextElement()
                        val hostAddr = addr.hostAddress
                        if (hostAddr == null || hostAddr.contains(":")) continue
                        ip = hostAddr
                        name = iface.displayName
                    }
                }
            }
        } catch (e: Exception) {
            // Non-fatal interface resolution fallback
            println("NetworkTools.jvm: Interface resolution warning: ${e.message}")
        }

        return LocalIpInfo(ip, name, isWifi)
    }
}

actual fun getNetworkTools(): NetworkTools = JvmNetworkTools()
