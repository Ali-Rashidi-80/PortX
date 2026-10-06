package com.mrcoder20.portx.presentation.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrcoder20.portx.domain.LocalizedStrings
import com.mrcoder20.portx.presentation.ui.theme.*
import com.mrcoder20.portx.presentation.viewmodel.ScanViewModel
import com.mrcoder20.portx.presentation.viewmodel.ScanUIState
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

// --- Data Models & Helpers ---

data class DisplayPort(val number: Int, val title: String, val description: String, val color: Color)

fun getPortColor(port: Int, accent: Color): Color {
    return when (port) {
        // High-risk, unencrypted, industrial or dangerous vectors
        21, 23, 102, 135, 137, 138, 139, 445, 502, 1883, 2049, 2181, 2375, 2379, 5683, 47808, 5555, 10250, 11211 -> DangerNeon
        // High-privilege / Admin / Database / Remote access / VPN
        22, 1194, 1433, 1521, 3306, 3389, 4840, 51820, 5432, 5900, 6379, 6443, 8123, 8200, 8500, 9042, 9200, 9300, 27017 -> SecondaryNeon
        // Standard Web / HTTPS / Proxy
        80, 443, 8000, 8080, 8081, 8088, 8443, 8888, 9090, 3000, 5000 -> accent
        // Standard Low-risk / Infrastructure
        53, 123 -> TertiaryNeon
        else -> TertiaryNeon
    }
}

fun getServiceTitle(port: Int, rawService: String? = null): String {
    return when (port) {
        21 -> "FTP File Transfer"
        22 -> "SSH Secure Shell"
        23 -> "Telnet Remote Shell"
        25 -> "SMTP Mail Relay"
        53 -> "DNS Name Server"
        67, 68 -> "DHCP Network Config"
        69 -> "TFTP Trivial FTP"
        80 -> "HTTP Web Server"
        102 -> "Siemens S7comm PLC"
        110 -> "POP3 Mail Server"
        123 -> "NTP Time Sync"
        135 -> "MS RPC Endpoint Mapper"
        137, 138, 139 -> "NetBIOS Session Service"
        143 -> "IMAP Mail Server"
        161, 162 -> "SNMP Network Agent"
        389 -> "LDAP Directory Service"
        443 -> "HTTPS Secure Web"
        445 -> "SMB / Active Directory"
        465 -> "SMTPS Secure Mail"
        502 -> "Modbus Industrial ICS"
        514 -> "Syslog Logging Agent"
        515 -> "LPD Line Printer"
        548 -> "AFP Apple Filing"
        587 -> "SMTP Submission"
        631 -> "IPP Internet Printing"
        636 -> "LDAPS Secure Directory"
        993 -> "IMAPS Secure Mail"
        995 -> "POP3S Secure Mail"
        1194 -> "OpenVPN Server"
        1433 -> "Microsoft SQL Server"
        1521 -> "Oracle Database"
        1700 -> "LoRaWAN Gateway"
        1723 -> "PPTP VPN Tunnel"
        1812, 1813 -> "RADIUS Auth/Acct"
        1883 -> "MQTT IoT Broker"
        1884 -> "MQTT-SN Gateway"
        1900 -> "SSDP UPnP Discovery"
        2049 -> "NFS Network Share"
        2181 -> "ZooKeeper Cluster Node"
        2375, 2376 -> "Docker Daemon API"
        2379, 2380 -> "etcd Datastore"
        3000 -> "Grafana / React Web App"
        3306 -> "MariaDB Database"
        3389 -> "RDP Remote Desktop"
        4222 -> "NATS Message Broker"
        4840 -> "OPC UA Industrial"
        5000 -> "UPnP / Web Service"
        5060, 5061 -> "SIP VoIP Signaling"
        5432 -> "PostgreSQL Database"
        5555 -> "Android ADB Debugger"
        5672 -> "RabbitMQ Message Broker"
        5683, 5684 -> "CoAP IoT Node"
        5900 -> "VNC Remote Display"
        6379 -> "Redis In-Memory DB"
        6443 -> "Kubernetes API Server"
        8000 -> "HTTP Web Service"
        8080 -> "HTTP Proxy / Alternate"
        8081, 8088 -> "HTTP Alternate Web"
        8123 -> "ClickHouse Analytical DB"
        8200 -> "HashiCorp Vault"
        8443 -> "HTTPS Secure Alt"
        8500 -> "Consul Service Mesh"
        8883 -> "MQTTS Secure IoT"
        8888, 9090 -> "HTTP Web Console"
        9000 -> "SonarQube / MinIO S3 API"
        9042 -> "Cassandra Database"
        9092 -> "Apache Kafka Broker"
        9100 -> "RAW JetDirect Printer"
        9200, 9300 -> "Elasticsearch Cluster"
        10250 -> "Kubelet Node Agent"
        11211 -> "Memcached Cache"
        11311 -> "ROS Master Node"
        27017 -> "MongoDB Database"
        47808 -> "BACnet Building Automation"
        50051 -> "gRPC Microservice"
        51820 -> "WireGuard VPN Tunnel"
        else -> {
            if (!rawService.isNullOrBlank() && rawService != "unknown") {
                rawService.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } + " Service"
            } else {
                "Service on Port $port"
            }
        }
    }
}

fun getServiceDescription(port: Int, rawService: String? = null): String {
    return when (port) {
        21 -> "Unencrypted file transfer protocol"
        22 -> "Encrypted remote terminal and file transfer"
        23 -> "Plaintext remote terminal session (insecure)"
        25 -> "Simple Mail Transfer Protocol relay"
        53 -> "Domain Name System resolver / nameserver"
        67, 68 -> "Dynamic Host Configuration Protocol"
        69 -> "Trivial File Transfer Protocol"
        80 -> "Standard HyperText Transfer Protocol"
        102 -> "Siemens Step7 industrial programmable logic controller"
        110 -> "Post Office Protocol version 3"
        123 -> "Network Time Protocol clock synchronization"
        135 -> "Microsoft Windows RPC endpoint locator"
        137, 138, 139 -> "NetBIOS name resolution and session transport"
        143 -> "Internet Message Access Protocol"
        161, 162 -> "Simple Network Management Protocol"
        389 -> "Lightweight Directory Access Protocol"
        443 -> "TLS/SSL encrypted web service"
        445 -> "Microsoft SMB file sharing / Windows domain"
        465 -> "Secure SMTP over SSL/TLS wrapper"
        502 -> "Modbus TCP industrial supervisory control"
        514 -> "Syslog system event logging listener"
        515 -> "Line Printer Daemon network print spooler"
        548 -> "Apple Filing Protocol macOS file sharing"
        587 -> "Mail message submission with STARTTLS"
        631 -> "Internet Printing Protocol CUPS service"
        636 -> "Secure LDAP over TLS"
        993 -> "Secure IMAP over TLS"
        995 -> "Secure POP3 over TLS"
        1194 -> "OpenVPN virtual private network daemon"
        1433 -> "Microsoft SQL Server database engine"
        1521 -> "Oracle TNS database listener"
        1700 -> "Semtech LoRaWAN packet forwarder interface"
        1723 -> "Point-to-Point Tunneling Protocol"
        1812, 1813 -> "Remote Authentication Dial-In User Service"
        1883 -> "MQTT telemetry transport broker (unencrypted)"
        1884 -> "MQTT for Sensor Networks bridge listener"
        1900 -> "Simple Service Discovery Protocol"
        2049 -> "Network File System Unix sharing"
        2181 -> "Apache ZooKeeper distributed coordination service"
        2375, 2376 -> "Docker container engine control API"
        2379, 2380 -> "etcd distributed consensus key-value datastore"
        3000 -> "Grafana metrics dashboard or web application frontend"
        3306 -> "MariaDB / relational SQL database"
        3389 -> "Windows Remote Desktop Protocol terminal"
        4222 -> "High-performance NATS publish-subscribe messaging"
        4840 -> "OPC Unified Architecture industrial server"
        5000 -> "Web framework or UPnP media streaming"
        5060, 5061 -> "Session Initiation Protocol VoIP telephony"
        5432 -> "PostgreSQL advanced relational database"
        5555 -> "Android Debug Bridge network listener"
        5672 -> "Advanced Message Queuing Protocol broker"
        5683, 5684 -> "Constrained Application Protocol sensor telemetry"
        5900 -> "Virtual Network Computing remote desktop"
        6379 -> "Redis in-memory key-value data store"
        6443 -> "Kubernetes cluster control plane API"
        8000 -> "Alternate HTTP web / application server"
        8080 -> "Common HTTP alternate / proxy listener"
        8081, 8088 -> "Alternate HTTP web application port"
        8123 -> "ClickHouse columnar analytical database HTTP port"
        8200 -> "HashiCorp Vault secret management"
        8443 -> "Secure HTTPS alternate web service"
        8500 -> "Consul cluster discovery and KV store"
        8883 -> "Secure MQTT IoT telemetry over TLS"
        8888, 9090 -> "Alternate web console or Prometheus"
        9000 -> "SonarQube code quality server or MinIO high-performance S3 object storage"
        9042 -> "Apache Cassandra distributed CQL native transport"
        9092 -> "Apache Kafka event streaming broker"
        9100 -> "HP JetDirect raw network print channel"
        9200, 9300 -> "Elasticsearch RESTful search and transport"
        10250 -> "Kubernetes Kubelet node agent API"
        11211 -> "Memcached high-speed memory object cache"
        11311 -> "Robot Operating System master node coordinator"
        27017 -> "MongoDB NoSQL document database"
        47808 -> "BACnet/IP building automation network"
        50051 -> "High-speed HTTP/2 gRPC microservice endpoint"
        51820 -> "High-speed encrypted WireGuard VPN UDP channel"
        else -> {
            if (!rawService.isNullOrBlank() && rawService != "unknown") {
                "Active $rawService service"
            } else {
                "Active Network Service"
            }
        }
    }
}

// --- Dashboard Implementation ---

@Composable
fun DashboardScreen(viewModel: ScanViewModel) {
    val state by viewModel.uiState.collectAsState()
    val appSettings = LocalAppSettings.current
    val accent = LocalAccentColor.current
    val lang = appSettings.language

    BoxWithConstraints(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        val width = maxWidth
        
        when {
            width > 1200.dp -> LargeDesktopDashboard(state, viewModel, accent, lang)
            width > 800.dp -> DesktopDashboard(state, viewModel, accent, lang)
            else -> MobileDashboard(state, viewModel, accent, lang)
        }
    }
}

@Composable
fun LargeDesktopDashboard(state: ScanUIState, viewModel: ScanViewModel, accent: Color, lang: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // --- 1. FULL-WIDTH COMMAND CENTER HUD ---
        DashboardIpInput(
            state = state,
            accent = accent,
            lang = lang,
            onIpChange = { viewModel.onIpChange(it) },
            onStartScan = { viewModel.startScan() },
            onStopScan = { viewModel.stopScan() }
        )

        state.error?.let {
            Surface(
                color = DangerNeon.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DangerNeon.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    it, color = DangerNeon, modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        // --- 2. BALANCED 3-COLUMN BENTO GRID ---
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Col 1: Visualizer Radar & Live Engine Activity
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SecurityVisualizerCard(
                    state,
                    modifier = Modifier.fillMaxWidth().height(260.dp),
                    accent = accent,
                    lang = lang
                )
                EngineLogsCard(
                    state,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    accent = accent,
                    lang = lang
                )
            }

            // Col 2: Concurrency & Engine Parameters
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AdvancedParametersCard(state, viewModel, accent, lang)
                EngineConfigurationCard(state, viewModel, accent, lang)
            }

            // Col 3: Live Discovered Services & Banners
            Column(
                modifier = Modifier.weight(1.15f).fillMaxHeight()
            ) {
                ActiveServicesCard(
                    state,
                    modifier = Modifier.fillMaxSize(),
                    accent = accent,
                    lang = lang
                )
            }
        }
    }
}

@Composable
fun DesktopDashboard(state: ScanUIState, viewModel: ScanViewModel, accent: Color, lang: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. FULL-WIDTH COMMAND CENTER HUD ---
        DashboardIpInput(
            state = state,
            accent = accent,
            lang = lang,
            onIpChange = { viewModel.onIpChange(it) },
            onStartScan = { viewModel.startScan() },
            onStopScan = { viewModel.stopScan() }
        )

        state.error?.let {
            Text(it, color = DangerNeon, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp))
        }

        // --- 2. BALANCED 2-COLUMN GRID ---
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Left: Visualizer & Logs
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SecurityVisualizerCard(
                    state,
                    modifier = Modifier.fillMaxWidth().height(250.dp),
                    accent = accent,
                    lang = lang
                )
                EngineLogsCard(
                    state,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    accent = accent,
                    lang = lang
                )
            }

            // Right: Discovered Services & Settings
            Column(
                modifier = Modifier.weight(1.1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ActiveServicesCard(
                    state,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    accent = accent,
                    lang = lang
                )
                AdvancedParametersCard(state, viewModel, accent = accent, lang = lang)
                EngineConfigurationCard(state, viewModel, accent = accent, lang = lang)
            }
        }
    }
}

@Composable
fun MobileDashboard(state: ScanUIState, viewModel: ScanViewModel, accent: Color, lang: String) {
    var showSettings by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        
        DashboardIpInput(
            state = state, 
            accent = accent,
            lang = lang,
            onIpChange = { viewModel.onIpChange(it) },
            onStartScan = { 
                viewModel.startScan()
                showSettings = false 
            },
            onStopScan = { viewModel.stopScan() },
            showSettingsToggle = true, 
            onSettingsToggle = { showSettings = !showSettings }
        )

        AnimatedVisibility(
            visible = showSettings,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AdvancedParametersCard(state, viewModel, accent, lang)
                EngineConfigurationCard(state, viewModel, accent, lang)
            }
        }

        SecurityVisualizerCard(state, modifier = Modifier.height(240.dp), smallSize = true, accent = accent, lang = lang)
        ActiveServicesCard(state, modifier = Modifier.weight(1f), accent = accent, lang = lang)
        
        state.error?.let { errorMsg ->
            Text(
                text = errorMsg, color = DangerNeon, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 8.dp).align(Alignment.CenterHorizontally)
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun DashboardIpInput(
    state: ScanUIState, 
    accent: Color,
    lang: String,
    onIpChange: (String) -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    showSettingsToggle: Boolean = false,
    onSettingsToggle: () -> Unit = {}
) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isDark) GlassBackground else GlassLight,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, if (isDark) GlassBorder else GlassBorderLight)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showSettingsToggle) {
                IconButton(onClick = onSettingsToggle, modifier = Modifier.padding(start = 4.dp)) {
                    Icon(Icons.Default.Tune, null, tint = accent)
                }
            } else {
                Icon(Icons.Default.Language, null, tint = accent, modifier = Modifier.padding(start = 12.dp).size(24.dp))
            }
            
            OutlinedTextField(
                value = state.ip,
                onValueChange = onIpChange,
                placeholder = { Text(LocalizedStrings.get("target", lang), color = if (LocalAppSettings.current.theme == "DARK") TextMuted else TextMutedLight, maxLines = 1) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    unfocusedTextColor = if (LocalAppSettings.current.theme == "DARK") Color.White else Color.Black,
                    focusedTextColor = if (LocalAppSettings.current.theme == "DARK") Color.White else Color.Black,
                    cursorColor = accent
                ),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Go,
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Uri
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onGo = {
                        if (state.isLoading) onStopScan() else onStartScan()
                    }
                ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
            )
            
            val infiniteTransition = rememberInfiniteTransition()
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.6f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse)
            )

            Button(
                onClick = { if (state.isLoading) onStopScan() else onStartScan() },
                colors = ButtonDefaults.buttonColors(containerColor = if (state.isLoading) DangerNeon.copy(alpha = pulseAlpha) else accent),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(end = 2.dp).height(52.dp).width(96.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(if (state.isLoading) Icons.Default.Stop else Icons.Default.FlashOn, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (state.isLoading) LocalizedStrings.get("stop", lang) else LocalizedStrings.get("scan", lang), fontWeight = FontWeight.Black, color = Color.Black, fontSize = 13.sp)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdvancedParametersCard(state: ScanUIState, viewModel: ScanViewModel, accent: Color, lang: String) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(LocalizedStrings.get("advanced", lang), style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp), color = accent, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ScanChip(LocalizedStrings.get("banner_grabbing", lang), state.bannerGrabbing, accent) { viewModel.toggleBannerGrabbing(it) }
                ScanChip(LocalizedStrings.get("full_port_scan", lang), state.allPorts, accent) { viewModel.toggleAllPorts(it) }
                ScanChip(LocalizedStrings.get("multi_protocol", lang), state.allProtocols, accent) { viewModel.toggleAllProtocols(it) }
                ScanChip(LocalizedStrings.get("stealth_mode", lang), state.scanType == "SYN", accent) { viewModel.onScanTypeChange(if(it) "SYN" else "TCP") }
            }

            if (!state.allPorts) {
                Spacer(Modifier.height(16.dp))
                Text(LocalizedStrings.get("port_range", lang), style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp), color = if (isDark) TextMuted else TextMutedLight, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                
                val currentRange = "${state.startPort}-${state.endPort}"
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScanChip(LocalizedStrings.get("top_100", lang), currentRange == "1-100", accent) {
                        viewModel.onStartPortChange("1")
                        viewModel.onEndPortChange("100")
                    }
                    ScanChip(LocalizedStrings.get("standard_ports", lang), currentRange == "1-1024", accent) {
                        viewModel.onStartPortChange("1")
                        viewModel.onEndPortChange("1024")
                    }
                    ScanChip(LocalizedStrings.get("web_ports", lang), currentRange == "80-8443", accent) {
                        viewModel.onStartPortChange("80")
                        viewModel.onEndPortChange("8443")
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = state.startPort,
                        onValueChange = { if (it.all { ch -> ch.isDigit() } && it.length <= 5) viewModel.onStartPortChange(it) },
                        label = { Text(LocalizedStrings.get("start_port", lang), fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent,
                            unfocusedBorderColor = if (isDark) GlassBorder else GlassBorderLight,
                            focusedTextColor = if (isDark) Color.White else Color.Black,
                            unfocusedTextColor = if (isDark) Color.White else Color.Black
                        ),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                    )
                    OutlinedTextField(
                        value = state.endPort,
                        onValueChange = { if (it.all { ch -> ch.isDigit() } && it.length <= 5) viewModel.onEndPortChange(it) },
                        label = { Text(LocalizedStrings.get("end_port", lang), fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent,
                            unfocusedBorderColor = if (isDark) GlassBorder else GlassBorderLight,
                            focusedTextColor = if (isDark) Color.White else Color.Black,
                            unfocusedTextColor = if (isDark) Color.White else Color.Black
                        ),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                    )
                }
            }
        }
    }
}

@Composable
fun EngineConfigurationCard(state: ScanUIState, viewModel: ScanViewModel, accent: Color, lang: String) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(LocalizedStrings.get("engine_configuration", lang), style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp), color = if (isDark) TextMuted else TextMutedLight, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(LocalizedStrings.get("parallel_threads", lang), color = if (isDark) Color.White else Color.Black, style = MaterialTheme.typography.bodySmall)
                    Text("${state.concurrentScans} ${LocalizedStrings.get("connections", lang)}", color = accent, style = MaterialTheme.typography.labelMedium)
                }
                Slider(
                    value = state.concurrentScans.toFloat(),
                    onValueChange = { viewModel.onConcurrentScansChange(it.toInt()) },
                    valueRange = 10f..2000f,
                    modifier = Modifier.weight(2f),
                    colors = SliderDefaults.colors(
                        thumbColor = accent, 
                        activeTrackColor = accent, 
                        inactiveTrackColor = if (isDark) GlassBorder else GlassBorderLight
                    )
                )
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = (if (isDark) GlassBorder else GlassBorderLight).copy(alpha = 0.5f))
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(LocalizedStrings.get("socket_timeout", lang), color = if (isDark) Color.White else Color.Black, style = MaterialTheme.typography.bodySmall)
                    Text("${state.timeout} ${LocalizedStrings.get("ms", lang)}", color = accent, style = MaterialTheme.typography.labelMedium)
                }
                Slider(
                    value = state.timeout.toFloat(),
                    onValueChange = { viewModel.onTimeoutChange(it.toInt()) },
                    valueRange = 100f..3000f,
                    steps = 28,
                    modifier = Modifier.weight(2f),
                    colors = SliderDefaults.colors(
                        thumbColor = accent, 
                        activeTrackColor = accent, 
                        inactiveTrackColor = if (isDark) GlassBorder else GlassBorderLight
                    )
                )
            }
        }
    }
}

@Composable
fun SecurityVisualizerCard(state: ScanUIState, modifier: Modifier = Modifier, smallSize: Boolean = false, accent: Color, lang: String) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    GlassCard(modifier = modifier) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            val displayProgress = if (state.isLoading) state.progress / 100f else {
                val result = state.result
                if (result != null) result.securityScore / 100f else 0f
            }
            val statusColor = when {
                state.isLoading -> accent
                state.result != null -> {
                    val score = state.result.securityScore
                    when {
                        score > 80 -> TertiaryNeon
                        score > 50 -> if (isDark) WarningNeon else WarningLight
                        else -> if (isDark) DangerNeon else DangerLight
                    }
                }
                else -> (if (isDark) Color.White else Color.Black).copy(alpha = 0.2f)
            }
            AdvancedLiquidGauge(progress = displayProgress, isLoading = state.isLoading, color = statusColor, gaugeSize = if (smallSize) 180.dp else 220.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (state.isLoading) {
                    Text("${state.progress}%", style = (if (smallSize) MaterialTheme.typography.displaySmall else MaterialTheme.typography.displayMedium).copy(fontWeight = FontWeight.Black, color = if (isDark) Color.White else Color.Black, shadow = Shadow(color = statusColor, blurRadius = 30f)))
                    Text(LocalizedStrings.get("engine_running", lang), style = MaterialTheme.typography.labelMedium.copy(color = statusColor, letterSpacing = 2.sp))
                } else {
                    val result = state.result
                    if (result != null) {
                        Text("${result.securityScore}%", style = (if (smallSize) MaterialTheme.typography.displaySmall else MaterialTheme.typography.displayMedium).copy(fontWeight = FontWeight.Black, color = if (isDark) Color.White else Color.Black, shadow = Shadow(color = statusColor, blurRadius = 30f)))
                        Text(LocalizedStrings.get("security_score", lang), style = MaterialTheme.typography.labelMedium.copy(color = statusColor, letterSpacing = 2.sp))
                        if (!state.firewallStatus.isNullOrBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = state.firewallStatus,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = if (isDark) TextMuted else TextMutedLight,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    } else {
                        Icon(Icons.Default.Radar, null, tint = if (isDark) Color.White.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.3f), modifier = Modifier.size(if (smallSize) 32.dp else 48.dp))
                        Text(LocalizedStrings.get("ready", lang), style = MaterialTheme.typography.titleMedium.copy(color = if (isDark) TextMuted else TextMutedLight, letterSpacing = 2.sp))
                    }
                }
            }
        }
    }
}

@Composable
fun EngineLogsCard(state: ScanUIState, modifier: Modifier = Modifier, accent: Color, lang: String = "en") {
    val isDark = LocalAppSettings.current.theme == "DARK"
    val logListState = rememberLazyListState()
    LaunchedEffect(state.logs.size) { if (state.logs.isNotEmpty()) logListState.animateScrollToItem(state.logs.size - 1) }
    GlassCard(modifier = modifier, contentPadding = PaddingValues(12.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Terminal, null, tint = accent, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
                Text(LocalizedStrings.get("live_engine_logs", lang), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = if (isDark) TextMuted else TextMutedLight)
            }
            Spacer(Modifier.height(12.dp))
            Box(modifier = Modifier.weight(1f).fillMaxWidth().background(if (isDark) Color.Black.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.05f), RoundedCornerShape(8.dp)).border(1.dp, GlassBorder.copy(alpha = 0.2f), RoundedCornerShape(8.dp)).padding(8.dp)) {
                LazyColumn(state = logListState, modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(state.logs, key = { index, log -> "${index}_$log" }) { _, log ->
                        Row(modifier = Modifier.padding(vertical = 2.dp)) {
                            Text("> ", color = accent, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold))
                            Text(log, color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                        }
                    }
                    if (state.logs.isEmpty()) item { Text(LocalizedStrings.get("waiting_engine_activity", lang), color = (if (isDark) TextMuted else TextMutedLight).copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)) }
                }
            }
        }
    }
}

@Composable
fun ActiveServicesCard(state: ScanUIState, modifier: Modifier = Modifier, accent: Color, lang: String) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val result = state.result
    GlassCard(modifier = modifier) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Text(LocalizedStrings.get("active_services", lang), style = MaterialTheme.typography.labelLarge.copy(color = if (isDark) TextMuted else TextMutedLight, fontWeight = FontWeight.Bold, letterSpacing = 1.sp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (result != null && result.openPorts.isNotEmpty()) {
                        Text("${result.openPorts.size} ${LocalizedStrings.get("found", lang)}", style = MaterialTheme.typography.labelSmall, color = accent, modifier = Modifier.background(accent.copy(alpha = 0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    Row(modifier = Modifier.background(if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f), CircleShape).border(1.dp, if (isDark) GlassBorder else GlassBorderLight, CircleShape)) {
                        IconButton(onClick = { scope.launch { listState.animateScrollToItem(0) } }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.KeyboardArrowUp, null, tint = if (isDark) Color.White else Color.Black, modifier = Modifier.size(18.dp)) }
                        IconButton(onClick = { scope.launch { val count = state.result?.openPorts?.distinct()?.size ?: 0; if (count > 0) listState.animateScrollToItem(count - 1) } }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.KeyboardArrowDown, null, tint = if (isDark) Color.White else Color.Black, modifier = Modifier.size(18.dp)) }
                    }
                }
            }

            if (result != null && (!result.deviceName.isNullOrBlank() || !result.osFingerprint.isNullOrBlank())) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .background(if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                        .border(1.dp, (if (isDark) GlassBorder else GlassBorderLight).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Devices, null, tint = accent, modifier = Modifier.size(16.dp))
                    val deviceText = buildString {
                        if (!result.deviceName.isNullOrBlank()) append(result.deviceName)
                        if (!result.osFingerprint.isNullOrBlank()) {
                            if (isNotEmpty()) append(" • ")
                            append(result.osFingerprint)
                        }
                    }
                    Text(
                        text = deviceText,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isDark) Color.White else Color.Black,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            if (state.anomalies.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .background(DangerNeon.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .border(1.dp, DangerNeon.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, null, tint = DangerNeon, modifier = Modifier.size(16.dp))
                    Text(
                        text = "${state.anomalies.size} ${LocalizedStrings.get("threats_detected", lang)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = DangerNeon
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
                    val portsToShow = result?.openPorts?.distinct()?.sorted()?.map { portNumber ->
                        val banner = (result.portBanners[portNumber] ?: "").trim()
                        val rawService = (result.portServices[portNumber] ?: "").trim()
                        val title = getServiceTitle(portNumber, rawService.ifEmpty { null })
                        val description = if (banner.isNotEmpty()) banner else getServiceDescription(portNumber, rawService.ifEmpty { null })
                        DisplayPort(portNumber, title, description, getPortColor(portNumber, accent))
                    } ?: emptyList()
                    if (portsToShow.isEmpty()) item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(if (state.isLoading) Icons.Default.HourglassEmpty else Icons.Default.SearchOff, null, tint = (if (isDark) TextMuted else TextMutedLight).copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(12.dp))
                                Text(if (state.isLoading) LocalizedStrings.get("scanning_network", lang) else LocalizedStrings.get("no_services", lang), color = if (isDark) TextMuted else TextMutedLight, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    } else items(portsToShow, key = { it.number }) { port -> PortItem(port) }
                }
                Box(modifier = Modifier.fillMaxWidth().height(32.dp).align(Alignment.BottomCenter).background(Brush.verticalGradient(listOf(Color.Transparent, (if (isDark) BackgroundDark else BackgroundLight).copy(alpha = 0.5f)))))
            }
        }
    }
}

@Composable
fun ScanChip(label: String, checked: Boolean, accent: Color, onCheckedChange: (Boolean) -> Unit) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    Surface(onClick = { onCheckedChange(!checked) }, shape = RoundedCornerShape(12.dp), color = if (checked) accent.copy(alpha = 0.15f) else (if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f)), border = BorderStroke(1.dp, if (checked) accent else (if (isDark) GlassBorder else GlassBorderLight))) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (checked) accent else (if (isDark) TextMuted else TextMutedLight)))
            Text(label, style = MaterialTheme.typography.labelMedium, color = if (checked) (if (isDark) Color.White else Color.Black) else (if (isDark) TextMuted else TextMutedLight))
        }
    }
}

@Composable
fun AdvancedLiquidGauge(progress: Float, isLoading: Boolean, color: Color, gaugeSize: androidx.compose.ui.unit.Dp = 220.dp) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, 
        targetValue = 360f, 
        animationSpec = infiniteRepeatable(tween(if (isLoading) 2500 else 8000, easing = LinearEasing))
    )
    val dotRotation0 by infiniteTransition.animateFloat(
        initialValue = 0f, 
        targetValue = 360f, 
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), repeatMode = RepeatMode.Restart)
    )
    val dotRotation1 by infiniteTransition.animateFloat(
        initialValue = 0f, 
        targetValue = 360f, 
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), repeatMode = RepeatMode.Restart)
    )
    val dotRotation2 by infiniteTransition.animateFloat(
        initialValue = 0f, 
        targetValue = 360f, 
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing), repeatMode = RepeatMode.Restart)
    )
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = spring(stiffness = Spring.StiffnessLow))
    Box(modifier = Modifier.size(gaugeSize), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(gaugeSize * 0.85f)) {
            val strokeWidth = (gaugeSize.toPx() * 0.045f)
            drawCircle(color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.05f), style = Stroke(width = strokeWidth))
            drawArc(brush = Brush.sweepGradient(0f to color.copy(alpha = 0.2f), 0.5f to color, 1f to color.copy(alpha = 0.2f), center = center), startAngle = rotation, sweepAngle = 360f * animatedProgress, useCenter = false, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
        }
        Box(modifier = Modifier.size(gaugeSize * 0.68f).clip(CircleShape).background(Brush.verticalGradient(listOf((if (isDark) Color.White else Color.Black).copy(alpha = 0.08f), Color.Transparent))).border(1.dp, color.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
            if (isLoading) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.width * 0.42f
                    val dotRadius = gaugeSize.toPx() * 0.012f
                    val dotRotations = listOf(dotRotation0, dotRotation1, dotRotation2)
                    dotRotations.forEachIndexed { index, rot ->
                        val angleRad = (rot * (kotlin.math.PI / 180.0)).toFloat()
                        val x = center.x + radius * kotlin.math.cos(angleRad)
                        val y = center.y + radius * kotlin.math.sin(angleRad)
                        drawCircle(color = color, radius = dotRadius, center = Offset(x, y), alpha = 0.8f - (index * 0.2f))
                    }
                }
            }
        }
    }
}

@Composable
fun PortItem(port: DisplayPort) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    Surface(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(1.dp, if (isDark) GlassBorder else GlassBorderLight, RoundedCornerShape(16.dp)).clickable { /* Detail */ }, color = if (isDark) GlassSurface else Color.Black.copy(alpha = 0.02f)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp).background(port.color.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).border(1.dp, port.color.copy(alpha = 0.3f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Text(port.number.toString(), color = port.color, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), fontSize = if (port.number > 9999) 9.sp else 11.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = port.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color.Black),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = port.description,
                    style = MaterialTheme.typography.bodySmall.copy(color = if (isDark) TextSecondary else TextSecondaryLight),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = if (isDark) TextMuted else TextMutedLight, modifier = Modifier.size(20.dp))
        }
    }
}

