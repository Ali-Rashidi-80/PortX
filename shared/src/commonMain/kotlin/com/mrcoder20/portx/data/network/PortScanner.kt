package com.mrcoder20.portx.data.network

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.random.Random
import kotlin.time.TimeSource

// ============================================================
// VERSION & CONSTANTS
// ============================================================

const val VERSION = "5.2.0"
const val APP_NAME = "PortX Engine"
const val MAX_PACKET_SIZE = 65535
const val RING_BUFFER_SIZE = 1048576
const val UDP_SCAN_LIMIT = 1000
const val PROBE_READ_TIMEOUT = 2000L

// ============================================================
// DATA MODELS
// ============================================================

@Serializable
data class ScanConfig(
    val target: String,
    val startPort: Int,
    val endPort: Int,
    val scanType: String = "TCP",
    val timeoutMs: Int = 1000,
    val concurrency: Int = 2000,
    val rate: Int = 20000,
    val serviceDetect: Boolean = true,
    val randomizePorts: Boolean = true
)

@Serializable
data class ScanPortResult(
    val port: Int,
    val protocol: String = "TCP",
    val state: String,
    val service: String = "unknown",
    val version: String = "",
    val product: String = "",
    val banner: String = "",
    val reason: String = "",
    val httpInfo: HttpInfo? = null,
    val rtt: Long = 0
)

@Serializable
data class HttpInfo(
    val title: String = "",
    val server: String = "",
    val status: Int = 0
)

@Serializable
data class ScanSummary(
    val target: String,
    val totalPorts: Int,
    val openPorts: Int,
    val closedPorts: Int,
    val filtered: Int,
    val durationMs: Long,
    val results: List<ScanPortResult>
)

// ============================================================
// ADVANCED ADAPTIVE TIMING (Neural-Inspired)
// ============================================================

class AdaptiveTiming(private val minRate: Int, private val maxRate: Int) {
    @kotlin.concurrent.Volatile
    private var currentRate = maxRate / 2
    private var state = "optimized"
    private var epsilon = 0.15
    private val alpha = 0.5
    private val qTable = mutableMapOf<String, Double>()
    
    // Moving average for RTT-based adaptive timeouts
    @kotlin.concurrent.Volatile
    private var avgRtt = 200L
    
    fun getRate() = currentRate
    fun getAdaptiveTimeout(baseTimeout: Int): Long {
        return max(baseTimeout.toLong(), (avgRtt * 2.5).toLong()).coerceIn(200L, 5000L)
    }

    private fun getAction(currentState: String): String {
        val candidateActions = if (currentState == "congested" || currentState == "high_latency") {
            listOf("safety", "maintain", "increase")
        } else {
            listOf("maintain", "increase", "turbo", "safety")
        }
        if (Random.nextDouble() < epsilon) {
            return candidateActions.random()
        }
        return candidateActions.maxByOrNull { qTable["$currentState:$it"] ?: 0.0 } ?: "maintain"
    }

    private fun update(state: String, action: String, reward: Double, nextState: String) {
        val key = "$state:$action"
        val nextKey = "$nextState:maintain"
        val currentQ = qTable[key] ?: 0.0
        val maxNextQ = qTable[nextKey] ?: 0.0
        qTable[key] = currentQ + alpha * (reward + 0.9 * maxNextQ - currentQ)
        if (epsilon > 0.02) epsilon *= 0.99
    }

    fun adapt(successRate: Double, latencyMs: Long) {
        // Update moving average RTT
        avgRtt = (avgRtt * 0.7 + latencyMs * 0.3).toLong()

        val nextState = when {
            successRate < 70.0 -> "congested"
            latencyMs > 800 -> "high_latency"
            else -> "optimized"
        }
        
        val action = getAction(nextState)
        val reward = (successRate / 10.0) - (latencyMs / 150.0) + (if (action == "turbo" && successRate > 90.0) 30.0 else 0.0)
        
        update(state, action, reward, nextState)
        
        when (action) {
            "turbo" -> currentRate = (currentRate * 1.5).toInt().coerceIn(minRate, maxRate)
            "increase" -> currentRate = (currentRate * 1.2).toInt().coerceIn(minRate, maxRate)
            "safety" -> currentRate = (currentRate * 0.5).toInt().coerceIn(minRate, maxRate)
            "maintain" -> { /* Stable */ }
        }
        state = nextState
    }
}

// ============================================================
// MAIN SCAN ENGINE (Extreme Performance)
// ============================================================

class PortScanner(private val dispatcher: CoroutineDispatcher = Dispatchers.Default) {

    private val timing = AdaptiveTiming(200, 50000) // Extreme max rate

    suspend fun scan(
        config: ScanConfig,
        onProgress: (Int) -> Unit = {}
    ): ScanSummary = withContext(dispatcher) {
        val timeSource = TimeSource.Monotonic
        val startTime = timeSource.markNow()
        val selectorManager = SelectorManager(dispatcher)
        
        // DNS CACHING & Host Sanitization
        val resolvedTarget = com.mrcoder20.portx.domain.sanitizeHost(config.target)

        val results = mutableListOf<ScanPortResult>()
        val concurrency = (if (config.concurrency > 0) config.concurrency else 1000).coerceIn(10, 2500)
        val ports = (config.startPort..config.endPort).toList().let {
            if (config.randomizePorts) it.shuffled() else it
        }
        
        val totalPorts = ports.size
        val scanPasses = if (config.scanType == "TCP/UDP") listOf("TCP", "UDP") else listOf(config.scanType)
        val totalOperations = totalPorts * scanPasses.size

        if (totalOperations <= 0) {
            selectorManager.close()
            return@withContext ScanSummary(
                target = resolvedTarget,
                totalPorts = 0,
                openPorts = 0,
                closedPorts = 0,
                filtered = 0,
                durationMs = 0L,
                results = emptyList()
            )
        }
        
        var scannedCount = 0
        var openCount = 0
        var closedCount = 0
        var filteredCount = 0
        var successBatch = 0
        var batchLatency = 0L

        // CHANNELS (Bounded for Backpressure & OOM Safety)
        val portChannel = Channel<Pair<String, Int>>(concurrency)
        val bannerChannel = Channel<ScanPortResult>(concurrency / 2)
        val finalResultsChannel = Channel<ScanPortResult>(concurrency * 2)

        // WORKER POOL: Main Scanner
        val workers = List(concurrency) {
            launch {
                for ((proto, port) in portChannel) {
                    val finalResult = try {
                        val start = timeSource.markNow()
                        val result = if (proto == "UDP") {
                            scanUdpPort(selectorManager, resolvedTarget, port)
                        } else {
                            val adaptiveTimeout = timing.getAdaptiveTimeout(config.timeoutMs)
                            var res = scanTcpPort(selectorManager, resolvedTarget, port, adaptiveTimeout)
                            
                            // Accuracy Retry on filtered ports
                            if (res.state == "filtered" && concurrency > 500) {
                                delay(15)
                                res = scanTcpPort(selectorManager, resolvedTarget, port, adaptiveTimeout * 2)
                            }
                            res
                        }
                        val latency = start.elapsedNow().inWholeMilliseconds
                        result.copy(rtt = latency)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        ScanPortResult(port, proto, "closed", reason = e.message ?: "error")
                    }

                    if (finalResult.state == "open" && config.serviceDetect && proto == "TCP") {
                        bannerChannel.send(finalResult)
                    } else {
                        finalResultsChannel.send(finalResult)
                    }
                }
            }
        }

        // WORKER POOL: Banner Detectors (Decoupled)
        val bannerWorkers = List(max(10, concurrency / 10)) {
            launch {
                for (res in bannerChannel) {
                    val enriched = try {
                        val socket = withTimeoutOrNull(2500) {
                            aSocket(selectorManager).tcp().connect(resolvedTarget.removePrefix("[").removeSuffix("]"), res.port) {
                                socketTimeout = 2000
                            }
                        }
                        if (socket != null) {
                            val probe = try {
                                probeOpenTcpPort(socket, resolvedTarget, res.port)
                            } finally {
                                try { socket.close() } catch (_: Exception) {}
                            }
                            probe.copy(rtt = res.rtt)
                        } else res
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) { res }
                    finalResultsChannel.send(enriched)
                }
            }
        }

        // PRODUCER
        launch {
            for (protocol in scanPasses) {
                for (port in ports) {
                    portChannel.send(protocol to port)
                }
            }
            portChannel.close()
        }

        // COORDINATOR: Lifecycle management of decoupled worker channels
        val pipelineCoordinator = launch {
            try {
                workers.joinAll()
            } finally {
                bannerChannel.close()
            }
            try {
                bannerWorkers.joinAll()
            } finally {
                finalResultsChannel.close()
            }
        }

        // CONSUMER: Final Result Aggregator
        val consumerJob = launch {
            for (res in finalResultsChannel) {
                results.add(res)
                batchLatency += res.rtt
                
                when (res.state) {
                    "open" -> {
                        openCount++
                        successBatch++
                    }
                    "filtered", "open|filtered" -> {
                        filteredCount++
                    }
                    else -> {
                        closedCount++
                    }
                }
                
                scannedCount++
                
                // Adaptive Feedback
                if (scannedCount % 50 == 0) {
                    val successRate = (successBatch.toDouble() / 50.0) * 100.0
                    val avgBatchLatency = if (scannedCount > 0) batchLatency / 50 else 0L
                    timing.adapt(successRate, avgBatchLatency)
                    successBatch = 0
                    batchLatency = 0
                }

                if (scannedCount % 50 == 0 || scannedCount == totalOperations) {
                    onProgress(((scannedCount * 100) / totalOperations).coerceAtMost(100))
                }
            }
        }

        try {
            consumerJob.join()
        } finally {
            pipelineCoordinator.cancel()
            workers.forEach { it.cancel() }
            bannerWorkers.forEach { it.cancel() }
            try {
                selectorManager.close()
            } catch (_: Exception) {}
        }

        ScanSummary(
            target = resolvedTarget,
            totalPorts = totalOperations,
            openPorts = openCount,
            closedPorts = closedCount,
            filtered = filteredCount,
            durationMs = startTime.elapsedNow().inWholeMilliseconds,
            results = results.sortedBy { it.port }
        )
    }

    private suspend fun scanTcpPort(
        selector: SelectorManager, 
        target: String, 
        port: Int, 
        timeout: Long
    ): ScanPortResult {
        return try {
            val socket = withTimeoutOrNull(timeout + 100) {
                aSocket(selector).tcp().connect(target.removePrefix("[").removeSuffix("]"), port) {
                    socketTimeout = timeout
                }
            }

            if (socket != null) {
                try { socket.close() } catch (_: Exception) {}
                ScanPortResult(port, "TCP", "open")
            } else {
                ScanPortResult(port, "TCP", "filtered", reason = "timeout")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ScanPortResult(port, "TCP", "closed", reason = e.message ?: "refused")
        }
    }

    internal fun getUdpProbePayload(port: Int): ByteArray {
        return when (port) {
            53 -> byteArrayOf(
                0x10, 0x00, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0x00, 0x01
            )
            123 -> ByteArray(48).apply { this[0] = 0x1B }
            161 -> byteArrayOf(
                0x30, 0x26, 0x02, 0x01, 0x00, 0x04, 0x06, 0x70,
                0x75, 0x62, 0x6c, 0x69, 0x63, 0xa0.toByte(), 0x19, 0x02,
                0x04, 0x00, 0x00, 0x00, 0x01, 0x02, 0x01, 0x00,
                0x02, 0x01, 0x00, 0x30, 0x0b, 0x30, 0x09, 0x06,
                0x05, 0x2b, 0x06, 0x01, 0x02, 0x01, 0x05, 0x00
            )
            1900 -> "M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nMAN: \"ssdp:discover\"\r\nMX: 1\r\nST: ssdp:all\r\n\r\n".encodeToByteArray()
            5683 -> byteArrayOf(0x40, 0x00, 0x00, 0x01) // RFC 7252 CoAP Ping (Empty Confirmable message)
            else -> "PROBE\r\n".encodeToByteArray()
        }
    }

    internal fun parseUdpResponseBanner(port: Int, bytes: ByteArray): String {
        return try {
            when {
                port == 53 && bytes.size >= 12 -> {
                    val flags = ((bytes[2].toInt() and 0xFF) shl 8) or (bytes[3].toInt() and 0xFF)
                    val isResponse = (flags and 0x8000) != 0
                    val rcode = flags and 0x000F
                    val rcodeName = when (rcode) {
                        0 -> "NoError"
                        1 -> "FormatError"
                        2 -> "ServerFailure"
                        3 -> "NXDomain"
                        4 -> "NotImplemented"
                        5 -> "Refused"
                        else -> "RCODE $rcode"
                    }
                    val anCount = ((bytes[6].toInt() and 0xFF) shl 8) or (bytes[7].toInt() and 0xFF)
                    if (isResponse) "DNS Response ($rcodeName, Answers: $anCount)" else "DNS Query/Server"
                }
                port == 123 && bytes.size >= 48 -> {
                    val vn = (bytes[0].toInt() and 0x38) ushr 3
                    val mode = bytes[0].toInt() and 0x07
                    val stratum = bytes[1].toInt() and 0xFF
                    val modeName = when (mode) {
                        3 -> "Client"
                        4 -> "Server"
                        5 -> "Broadcast"
                        else -> "Mode $mode"
                    }
                    "NTP v$vn $modeName (Stratum $stratum)"
                }
                port == 161 && bytes.isNotEmpty() && bytes[0] == 0x30.toByte() -> {
                    val hasGetResponse = bytes.any { it == 0xA2.toByte() }
                    if (hasGetResponse) "SNMP Response Agent (v1/v2c)" else "SNMP Agent"
                }
                port == 5683 && bytes.size >= 4 -> {
                    val type = (bytes[0].toInt() and 0x30) ushr 4
                    val typeName = when (type) {
                        0 -> "CON"
                        1 -> "NON"
                        2 -> "ACK"
                        3 -> "RST"
                        else -> "Type $type"
                    }
                    val code = bytes[1].toInt() and 0xFF
                    val codeClass = code ushr 5
                    val codeDetail = code and 0x1F
                    "CoAP $typeName ($codeClass.$codeDetail)"
                }
                port == 1900 -> {
                    val text = try { bytes.decodeToString() } catch (_: Exception) { "" }
                    val server = text.lines().find { it.startsWith("Server:", ignoreCase = true) }?.removePrefix("Server:")?.trim()
                    val st = text.lines().find { it.startsWith("ST:", ignoreCase = true) }?.removePrefix("ST:")?.trim()
                    when {
                        !server.isNullOrBlank() -> "SSDP Server: $server"
                        !st.isNullOrBlank() -> "SSDP ST: $st"
                        text.startsWith("HTTP/") -> text.lines().firstOrNull() ?: "SSDP Service"
                        else -> "SSDP Service"
                    }
                }
                else -> {
                    val text = try {
                        bytes.decodeToString()
                    } catch (_: Exception) {
                        bytes.filter { it in 32..126 }.toByteArray().decodeToString()
                    }
                    text.filter { (it.code >= 32 && it.code !in 127..159) || it == '\n' || it == '\r' || it == '\t' }.trim().take(150)
                }
            }
        } catch (_: Exception) {
            ""
        }
    }

    private suspend fun scanUdpPort(selector: SelectorManager, target: String, port: Int): ScanPortResult {
        return try {
            val address = InetSocketAddress(target.removePrefix("[").removeSuffix("]"), port)
            val socket = aSocket(selector).udp().bind()
            var banner = ""
            val state = try {
                val payload = getUdpProbePayload(port)
                val packet = buildPacket { writeFully(payload) }
                socket.send(Datagram(packet, address))
                val udpTimeout = maxOf(600L, timing.getAdaptiveTimeout(800))
                val response = withTimeoutOrNull(udpTimeout) {
                    socket.receive()
                }
                if (response != null) {
                    @Suppress("DEPRECATION")
                    val bytes = response.packet.readBytes()
                    banner = parseUdpResponseBanner(port, bytes)
                    "open"
                } else "open|filtered"
            } finally {
                try { socket.close() } catch (_: Exception) {}
            }
            ScanPortResult(port, "UDP", state, guessService(port), banner = banner)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ScanPortResult(port, "UDP", "closed")
        }
    }

    private suspend fun probeOpenTcpPort(socket: Socket, target: String, port: Int): ScanPortResult {
        var service = guessService(port)
        var version = ""
        var httpInfo: HttpInfo? = null

        val grabbed = tryGrabBanner(socket, target, port) ?: ""
        val banner = grabbed
        
        val lowBanner = grabbed.lowercase()
        when {
            lowBanner.contains("ssh") -> {
                service = "ssh"
                version = grabbed.lines().firstOrNull()?.removePrefix("SSH-") ?: ""
            }
            lowBanner.contains("http") || lowBanner.contains("apache") || lowBanner.contains("nginx") -> {
                service = if (port in setOf(443, 8443)) "https" else "http"
                val title = extractTitle(grabbed)
                val server = grabbed.lines().find { it.startsWith("Server:", true) }?.removePrefix("Server:")?.trim() ?: ""
                val statusMatch = Regex("""HTTP/\d+(?:\.\d+)?\s+(\d{3})""", RegexOption.IGNORE_CASE).find(grabbed)
                val status = statusMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
                httpInfo = HttpInfo(title = title, server = server, status = status)
                version = if (server.isNotEmpty()) server else if (status > 0) "HTTP $status" else ""
            }
            lowBanner.contains("ftp") -> {
                service = "ftp"
                version = grabbed.lines().firstOrNull() ?: ""
            }
            lowBanner.contains("mariadb") || lowBanner.contains("mysql") || (port == 3306 && grabbed.isNotEmpty()) -> {
                service = "mariadb"
                val verMatch = Regex("""(\d+\.\d+\.\d+[\w.-]*)""").find(grabbed)
                version = verMatch?.value ?: (if (lowBanner.contains("mariadb")) "MariaDB" else "MariaDB Compatible")
            }
            lowBanner.contains("esmtp") || lowBanner.contains("smtp") || (port in setOf(25, 465, 587) && grabbed.startsWith("220")) -> {
                service = "smtp"
                version = grabbed.lines().firstOrNull() ?: ""
            }
            lowBanner.contains("redis") || lowBanner.contains("+pong") || lowBanner.contains("-noauth") || lowBanner.contains("-err") || port == 6379 -> {
                service = "redis"
                version = if (lowBanner.contains("noauth") || lowBanner.contains("auth required")) "Auth Protected" else if (lowBanner.contains("+pong")) "Unauthenticated" else ""
            }
            lowBanner.contains("docker") || port in setOf(2375, 2376) -> {
                service = "docker"
            }
            lowBanner.contains("elasticsearch") || (port in setOf(9200, 9300) && (lowBanner.contains("cluster_name") || lowBanner.contains("tagline"))) -> {
                service = "elasticsearch"
                val verMatch = Regex(""""number"\s*:\s*"([^"]+)"""").find(grabbed)
                if (verMatch != null) version = verMatch.groupValues[1]
            }
            lowBanner.contains("version ") && port == 11211 -> {
                service = "memcached"
                version = grabbed.removePrefix("VERSION ").trim()
            }
            lowBanner.contains("rfb") || port == 5900 -> {
                service = "vnc"
                if (lowBanner.contains("rfb")) version = grabbed.lines().firstOrNull()?.trim() ?: ""
            }
            port == 9100 || lowBanner.contains("jetdirect") || lowBanner.contains("pjl") -> {
                service = "jetdirect"
            }
            port == 5432 || lowBanner.contains("postgresql") || lowBanner.contains("password authentication failed") -> {
                service = "postgres"
                version = when {
                    grabbed == "S" || grabbed.startsWith("S") -> "PostgreSQL (SSL Enabled)"
                    grabbed == "N" || grabbed.startsWith("N") -> "PostgreSQL (Plain)"
                    lowBanner.contains("password authentication failed") || lowBanner.contains("fatal") -> "PostgreSQL Server"
                    else -> ""
                }
            }
            port in setOf(1883, 8883) || lowBanner.contains("mqtt") -> {
                service = if (port == 8883) "mqtts" else "mqtt"
                version = when {
                    lowBanner.contains("returncode: 0") || lowBanner.contains("accepted") -> "MQTT 3.1.1 (Connected)"
                    lowBanner.contains("returncode: 5") || lowBanner.contains("auth") -> "MQTT 3.1.1 (Auth Required)"
                    lowBanner.contains("returncode:") -> "MQTT 3.1.1 Broker"
                    else -> ""
                }
            }
            port == 1433 || lowBanner.contains("microsoft sql") -> {
                service = "mssql"
            }
            port == 1521 || lowBanner.contains("oracle") -> {
                service = "oracle"
            }
            port == 27017 || lowBanner.contains("mongodb") -> {
                service = "mongodb"
            }
            port == 3389 || lowBanner.contains("rdp") -> {
                service = "rdp"
            }
            port == 502 || lowBanner.contains("modbus") -> {
                service = "modbus"
                if (grabbed.isNotBlank() && grabbed != "modbus") {
                    version = grabbed.lines().firstOrNull()?.take(50) ?: "Modbus/TCP"
                }
            }
            port == 102 -> {
                service = "s7comm"
            }
            port == 4840 -> {
                service = "opcua"
            }
            port in setOf(2379, 2380) || lowBanner.contains("etcd") -> {
                service = "etcd"
            }
            port == 8123 || lowBanner.contains("clickhouse") -> {
                service = "clickhouse-http"
                val verMatch = Regex("""ClickHouse\s*([\d.]+)""", RegexOption.IGNORE_CASE).find(grabbed)
                if (verMatch != null) version = verMatch.groupValues[1]
            }
            port == 9042 || lowBanner.contains("cql") || lowBanner.contains("cassandra") -> {
                service = "cassandra"
            }
            port == 9092 || lowBanner.contains("kafka") -> {
                service = "kafka"
            }
            port == 5672 || lowBanner.contains("amqp") || lowBanner.contains("rabbitmq") -> {
                service = "rabbitmq"
                version = if (lowBanner.contains("rabbitmq")) "RabbitMQ Broker" else "AMQP 0-9-1"
            }
            port == 2181 || lowBanner.contains("imok") || lowBanner.contains("zookeeper") -> {
                service = "zookeeper"
                version = "Apache ZooKeeper Node"
            }
        }

        val declarativeMatch = com.mrcoder20.portx.domain.DeclarativeSignatureRegistry.findMatch(port, grabbed)
        if (declarativeMatch != null) {
            if (service == "unknown" || service.isBlank()) {
                service = declarativeMatch.id.substringAfter("-")
            }
            val extracted = declarativeMatch.extractVersion(grabbed)
            if (!extracted.isNullOrBlank() && version.isBlank()) {
                version = extracted
            }
        }

        return ScanPortResult(
            port = port,
            protocol = "TCP",
            state = "open",
            service = service,
            version = version,
            banner = banner.take(150),
            httpInfo = httpInfo
        )
    }

    private suspend fun tryGrabBanner(socket: Socket, target: String, port: Int): String? = withTimeoutOrNull(2500) {
        try {
            val receiveChannel = socket.openReadChannel()
            val sendChannel = socket.openWriteChannel(autoFlush = true)

            val httpPorts = setOf(80, 8080, 443, 8000, 8081, 8088, 8123, 8443, 8888, 9090, 2379, 3000, 5000)
            if (httpPorts.contains(port)) {
                val hostHeader = if (target.contains(":") && !target.startsWith("[")) "[$target]" else target
                val hostWithPort = if (port == 80 || port == 443) hostHeader else "$hostHeader:$port"
                sendChannel.writeStringUtf8("GET / HTTP/1.1\r\nHost: $hostWithPort\r\nUser-Agent: PortX/5.1\r\nConnection: close\r\n\r\n")
            } else if (port == 6379) {
                sendChannel.writeStringUtf8("PING\r\n")
            } else if (port == 11211) {
                sendChannel.writeStringUtf8("version\r\n")
            } else if (port == 502) {
                sendChannel.writeFully(byteArrayOf(0x00, 0x01, 0x00, 0x00, 0x00, 0x05, 0x01, 0x2b, 0x0e, 0x01, 0x00))
            } else if (port == 9100) {
                sendChannel.writeStringUtf8("@PJL INFO ID\r\n")
            } else if (port == 1883) {
                // MQTT 3.1.1 CONNECT packet
                sendChannel.writeFully(byteArrayOf(0x10, 0x0c, 0x00, 0x04, 0x4d, 0x51, 0x54, 0x54, 0x04, 0x02, 0x00, 0x3c, 0x00, 0x00))
            } else if (port == 5432) {
                // PostgreSQL SSLRequest packet (8 bytes: Length=8, Code=80877103)
                sendChannel.writeFully(byteArrayOf(0x00, 0x00, 0x00, 0x08, 0x04, 0xd2.toByte(), 0x16, 0x2f))
            } else if (port == 2181) {
                // Apache ZooKeeper 4-letter word command
                sendChannel.writeStringUtf8("ruok\r\n")
            } else if (port == 5672) {
                // AMQP 0-9-1 Protocol Header (RFC AMQP)
                sendChannel.writeFully(byteArrayOf(0x41, 0x4d, 0x51, 0x50, 0x00, 0x00, 0x09, 0x01))
            } else if (port == 53) {
                // DNS over TCP: 2-byte length prefix (0x00, 0x11 = 17 bytes) + DNS query payload
                sendChannel.writeFully(byteArrayOf(
                    0x00, 0x11, 0x10, 0x00, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0x00, 0x01
                ))
            }

            val buffer = ByteArray(2048)
            var totalRead = 0
            val read = receiveChannel.readAvailable(buffer, 0, buffer.size)
            if (read > 0) {
                totalRead += read
                if (httpPorts.contains(port) && totalRead < buffer.size) {
                    val currentText = buffer.decodeToString(0, totalRead)
                    if (!currentText.contains("\r\n\r\n") && !currentText.contains("</title>", ignoreCase = true)) {
                        withTimeoutOrNull(600) {
                            val nextRead = receiveChannel.readAvailable(buffer, totalRead, buffer.size - totalRead)
                            if (nextRead > 0) totalRead += nextRead
                        }
                    }
                }
                if (port == 53 && totalRead >= 14) {
                    val dnsParsed = parseUdpResponseBanner(53, buffer.copyOfRange(2, totalRead))
                    if (dnsParsed.isNotEmpty()) return@withTimeoutOrNull "TCP $dnsParsed"
                }
                if (port == 2181) {
                    val text = buffer.decodeToString(0, totalRead).trim()
                    if (text.contains("imok")) return@withTimeoutOrNull "ZooKeeper Node (imok)"
                    if (text.isNotBlank()) return@withTimeoutOrNull "ZooKeeper: $text"
                }
                if (port == 5672 && totalRead >= 4 && buffer[0] == 0x41.toByte() && buffer[1] == 0x4d.toByte()) {
                    return@withTimeoutOrNull "AMQP 0-9-1 Connection Handshake"
                }
                if (port == 1883 && totalRead >= 4 && buffer[0] == 0x20.toByte() && buffer[1] == 0x02.toByte()) {
                    val returnCode = buffer[3].toInt() and 0xFF
                    val rcDesc = when (returnCode) {
                        0 -> "Accepted"
                        1 -> "Unacceptable Protocol Version"
                        2 -> "Identifier Rejected"
                        3 -> "Server Unavailable"
                        4 -> "Bad User/Password"
                        5 -> "Not Authorized"
                        else -> "Code $returnCode"
                    }
                    return@withTimeoutOrNull "MQTT 3.1.1 CONNACK (ReturnCode: $returnCode - $rcDesc)"
                }
                if (port == 5432 && totalRead == 1) {
                    val char = buffer[0].toInt().toChar()
                    if (char == 'S' || char == 'N') return@withTimeoutOrNull char.toString()
                }
                val raw = buffer.decodeToString(0, totalRead)
                val sanitized = raw.filter { (it.code >= 32 && it.code !in 127..159) || it == '\n' || it == '\r' || it == '\t' }.trim()
                if (sanitized.isNotEmpty()) sanitized else null
            } else null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    internal fun extractTitle(banner: String): String {
        val regex = Regex("""<title\b[^>]*>(.*?)</title>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        var raw = regex.find(banner)?.groupValues?.get(1)?.trim()?.replace("\n", " ")?.replace("\r", "") ?: return ""
        
        // Strip nested HTML tags and comments inside title
        raw = Regex("""<[^>]*>""").replace(raw, "")

        // Decode decimal and hex numeric character references (e.g. &#65; -> A, &#x41; -> A)
        raw = Regex("""&#(\d+);""").replace(raw) { match ->
            val code = match.groupValues[1].toIntOrNull()
            if (code != null && code in 32..65535) code.toChar().toString() else match.value
        }
        raw = Regex("""&#x([0-9a-fA-F]+);""").replace(raw) { match ->
            val code = match.groupValues[1].toIntOrNull(16)
            if (code != null && code in 32..65535) code.toChar().toString() else match.value
        }

        return raw.replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
            .replace("&copy;", "©")
            .replace("&reg;", "®")
            .replace("&trade;", "™")
            .replace("&mdash;", "—")
            .replace("&ndash;", "–")
            .replace("&bull;", "•")
            .replace("&zwnj;", "\u200C")
            .replace("&rlm;", "\u200F")
            .replace("&lrm;", "\u200E")
            .replace("&laquo;", "«")
            .replace("&raquo;", "»")
            .replace("|", "/")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .take(120)
    }


    internal fun guessService(port: Int): String {
        return when (port) {
            7 -> "echo"
            20 -> "ftp-data"
            21 -> "ftp"
            22 -> "ssh"
            23 -> "telnet"
            25 -> "smtp"
            53 -> "dns"
            67, 68 -> "dhcp"
            69 -> "tftp"
            80 -> "http"
            102 -> "s7comm"
            110 -> "pop3"
            123 -> "ntp"
            135 -> "epmap"
            137, 138, 139 -> "netbios"
            143 -> "imap"
            161, 162 -> "snmp"
            389 -> "ldap"
            443 -> "https"
            445 -> "microsoft-ds"
            465 -> "smtps"
            502 -> "modbus"
            514 -> "syslog"
            515 -> "lpd"
            548 -> "afp"
            587 -> "smtp-msa"
            631 -> "ipp"
            636 -> "ldaps"
            993 -> "imaps"
            995 -> "pop3s"
            1194 -> "openvpn"
            1433 -> "mssql"
            1521 -> "oracle"
            1700 -> "lorawan"
            1723 -> "pptp"
            1812, 1813 -> "radius"
            1883, 8883 -> "mqtt"
            1884 -> "mqtt-sn"
            1900 -> "ssdp"
            2049 -> "nfs"
            2181 -> "zookeeper"
            2375, 2376 -> "docker"
            2379, 2380 -> "etcd"
            3000 -> "http-alt"
            3306 -> "mariadb"
            3389 -> "rdp"
            4222 -> "nats"
            4840 -> "opcua"
            5000 -> "http-alt"
            5060, 5061 -> "sip"
            5432 -> "postgres"
            5555 -> "adb"
            5672 -> "rabbitmq"
            5683, 5684 -> "coap"
            5900 -> "vnc"
            6379 -> "redis"
            6443 -> "kubernetes-api"
            8000 -> "http-alt"
            8080 -> "http-proxy"
            8081, 8088 -> "http-alt"
            8123 -> "clickhouse-http"
            8200 -> "vault"
            8443 -> "https-alt"
            8500 -> "consul"
            8888, 9090 -> "http-alt"
            9000 -> "sonarqube"
            9042 -> "cassandra"
            9092 -> "kafka"
            9100 -> "jetdirect"
            9200, 9300 -> "elasticsearch"
            10250 -> "kubelet"
            11211 -> "memcached"
            11311 -> "ros"
            27017 -> "mongodb"
            47808 -> "bacnet"
            50051 -> "grpc"
            51820 -> "wireguard"
            else -> "unknown"
        }
    }
}
