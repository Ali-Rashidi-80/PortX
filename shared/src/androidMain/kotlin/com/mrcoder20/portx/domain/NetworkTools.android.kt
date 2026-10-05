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
        try {
            val start = System.currentTimeMillis()
            val process = Runtime.getRuntime().exec(arrayOf("/system/bin/ping", "-c", "1", "-W", timeoutSec.toString(), ipStr))
            val finished = process.waitFor(timeoutMs + 1000L, TimeUnit.MILLISECONDS)
            if (!finished) {
                process.destroy()
                return PingAttempt(false, null, null)
            }
            val output = process.inputStream.bufferedReader().readText()
            val elapsed = System.currentTimeMillis() - start

            if (process.exitValue() == 0 && !output.contains("100% packet loss", ignoreCase = true)) {
                val parsedTime = parseTimeFromPingOutput(output) ?: elapsed
                val parsedTtl = parseTtlFromPingOutput(output)
                return PingAttempt(true, parsedTime, parsedTtl)
            }
        } catch (e: Exception) {
            // Android exec failed or restricted, fallback to isReachable
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
            val client = SecurityHarden.createSecureClient()
            val response: HttpResponse = client.get("https://rdap.org/domain/$cleanHost")
            if (response.status.value in 200..299) {
                response.bodyAsText().take(5000)
            } else {
                "WHOIS data not available for $cleanHost via RDAP."
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            "WHOIS Resolution Error (Mobile): ${e.message}. Domain might be invalid or RDAP is blocked."
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
                val response: HttpResponse = client.get(url)
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
        return try {
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNetwork = cm?.activeNetwork
            val caps = cm?.getNetworkCapabilities(activeNetwork)
            val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

            var ip = "0.0.0.0"
            var name = "unknown"
            
            val interfaces = NetworkInterface.getNetworkInterfaces()
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    val iface = interfaces.nextElement()
                    if (iface.isLoopback || !iface.isUp) continue
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
            LocalIpInfo(ip, name, isWifi)
        } catch (e: Exception) {
            LocalIpInfo("0.0.0.0", "Error", false)
        }
    }
}

actual fun getNetworkTools(): NetworkTools = AndroidNetworkTools()
