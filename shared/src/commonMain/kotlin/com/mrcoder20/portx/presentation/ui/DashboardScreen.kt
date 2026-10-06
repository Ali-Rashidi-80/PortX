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
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import com.mrcoder20.portx.presentation.ui.components.PortXVerticalScrollbar
import com.mrcoder20.portx.presentation.ui.components.PortXScrollStateVerticalScrollbar
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
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
                    lang = lang,
                    onClearLogs = { viewModel.clearLogs() }
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
                    lang = lang,
                    onClearLogs = { viewModel.clearLogs() }
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
    var selectedMobileTab by remember { mutableStateOf(0) }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        
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

        CyberSegmentedControl(
            items = listOf(
                LocalizedStrings.get("services_tab", lang) to Icons.Default.Hub,
                LocalizedStrings.get("logs_tab", lang) to Icons.Default.Terminal
            ),
            selectedIndex = selectedMobileTab,
            onIndexSelected = { selectedMobileTab = it },
            accent = accent,
            modifier = Modifier.fillMaxWidth()
        )

        AnimatedContent(
            targetState = selectedMobileTab,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            transitionSpec = { fadeIn() togetherWith fadeOut() }
        ) { tab ->
            if (tab == 0) {
                ActiveServicesCard(state, modifier = Modifier.fillMaxSize(), accent = accent, lang = lang)
            } else {
                EngineLogsCard(
                    state,
                    modifier = Modifier.fillMaxSize(),
                    accent = accent,
                    lang = lang,
                    onClearLogs = { viewModel.clearLogs() }
                )
            }
        }
        
        state.error?.let { errorMsg ->
            Text(
                text = errorMsg, color = DangerNeon, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 8.dp).align(Alignment.CenterHorizontally)
            )
        }
        Spacer(Modifier.height(6.dp))
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
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, if (isDark) GlassBorder else GlassBorderLight)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showSettingsToggle) {
                    IconButton(onClick = onSettingsToggle) {
                        Icon(Icons.Default.Tune, null, tint = accent)
                    }
                } else {
                    Icon(Icons.Default.Language, null, tint = accent, modifier = Modifier.padding(start = 8.dp).size(22.dp))
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
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        textDirection = TextDirection.Ltr
                    ),
                    trailingIcon = {
                        if (state.ip.isNotEmpty()) {
                            val ipTrimmed = state.ip.trim()
                            val isIpv4 = ipTrimmed.split(".").let { parts -> parts.size == 4 && parts.all { p -> p.toIntOrNull() in 0..255 } }
                            val isLocal = ipTrimmed == "127.0.0.1" || ipTrimmed.equals("localhost", ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = (if (isLocal) TertiaryNeon else accent).copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, (if (isLocal) TertiaryNeon else accent).copy(alpha = 0.35f)),
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Text(
                                    if (isLocal) "LOCAL" else if (isIpv4) "IPv4" else "HOST",
                                    color = if (isLocal) TertiaryNeon else accent,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                )
                
                val infiniteTransition = rememberInfiniteTransition()
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.6f, targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse)
                )

                Button(
                    onClick = { if (state.isLoading) onStopScan() else onStartScan() },
                    colors = ButtonDefaults.buttonColors(containerColor = if (state.isLoading) DangerNeon.copy(alpha = pulseAlpha) else accent),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.padding(end = 2.dp).height(46.dp).width(90.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(if (state.isLoading) Icons.Default.Stop else Icons.Default.FlashOn, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (state.isLoading) LocalizedStrings.get("stop", lang) else LocalizedStrings.get("scan", lang), fontWeight = FontWeight.Black, color = Color.Black, fontSize = 12.sp)
                }
            }

            // Quick Target Presets Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    LocalizedStrings.get("quick_targets", lang) + ":",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = if (isDark) TextMuted else TextMutedLight
                )
                listOf(
                    "127.0.0.1" to "Localhost",
                    "192.168.1.1" to "Gateway",
                    "scanme.nmap.org" to "Nmap Echo"
                ).forEach { (targetVal, label) ->
                    val isSelected = state.ip.trim() == targetVal
                    Surface(
                        onClick = { onIpChange(targetVal) },
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) accent.copy(alpha = 0.15f) else (if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.03f)),
                        border = BorderStroke(1.dp, if (isSelected) accent else (if (isDark) GlassBorder else GlassBorderLight).copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = if (isSelected) accent else (if (isDark) Color.White else Color.Black))
                            Text(targetVal, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace), color = accent)
                        }
                    }
                }
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
                    ScanChip(LocalizedStrings.get("top_20", lang), currentRange == "1-100", accent) {
                        viewModel.setPortRange("1", "100")
                    }
                    ScanChip(LocalizedStrings.get("standard_ports", lang), currentRange == "1-1024", accent) {
                        viewModel.setPortRange("1", "1024")
                    }
                    ScanChip(LocalizedStrings.get("web_ports", lang), currentRange == "80-8443", accent) {
                        viewModel.setPortRange("80", "8443")
                    }
                    ScanChip(LocalizedStrings.get("db_services", lang), currentRange == "1433-27017", accent) {
                        viewModel.setPortRange("1433", "27017")
                    }
                    ScanChip(LocalizedStrings.get("remote_iot", lang), currentRange == "22-1883", accent) {
                        viewModel.setPortRange("22", "1883")
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
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            textDirection = TextDirection.Ltr
                        )
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
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            textDirection = TextDirection.Ltr
                        )
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
    GlassCard(modifier = modifier, contentPadding = PaddingValues(16.dp)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                contentAlignment = Alignment.Center, 
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
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
                AdvancedLiquidGauge(progress = displayProgress, isLoading = state.isLoading, color = statusColor, gaugeSize = if (smallSize) 170.dp else 210.dp)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (state.isLoading) {
                        Text("${state.progress}%", style = (if (smallSize) MaterialTheme.typography.displaySmall else MaterialTheme.typography.displayMedium).copy(fontWeight = FontWeight.Black, color = if (isDark) Color.White else Color.Black, shadow = Shadow(color = statusColor, blurRadius = 30f)))
                        Text(LocalizedStrings.get("engine_running", lang), style = MaterialTheme.typography.labelMedium.copy(color = statusColor, letterSpacing = 2.sp))
                    } else {
                        val result = state.result
                        if (result != null) {
                            Text("${result.securityScore}%", style = (if (smallSize) MaterialTheme.typography.displaySmall else MaterialTheme.typography.displayMedium).copy(fontWeight = FontWeight.Black, color = if (isDark) Color.White else Color.Black, shadow = Shadow(color = statusColor, blurRadius = 30f)))
                            Text(LocalizedStrings.get("security_score", lang), style = MaterialTheme.typography.labelMedium.copy(color = statusColor, letterSpacing = 2.sp))
                        } else {
                            Icon(Icons.Default.Radar, null, tint = if (isDark) Color.White.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.3f), modifier = Modifier.size(if (smallSize) 32.dp else 48.dp))
                            Text(LocalizedStrings.get("ready", lang), style = MaterialTheme.typography.titleMedium.copy(color = if (isDark) TextMuted else TextMutedLight, letterSpacing = 2.sp))
                        }
                    }
                }
            }

            // Bottom Profile / Perimeter Status Container (moved out of gauge to avoid cluttering)
            if (!state.firewallStatus.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.03f),
                    border = BorderStroke(1.dp, (if (isDark) GlassBorder else GlassBorderLight).copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = state.firewallStatus,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isDark) TextMuted else TextMutedLight,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EngineLogsCard(
    state: ScanUIState, 
    modifier: Modifier = Modifier, 
    accent: Color, 
    lang: String = "en",
    onClearLogs: (() -> Unit)? = null
) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    val logListState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current
    var isLogsCopied by remember { mutableStateOf(false) }

    LaunchedEffect(state.logs.size) { if (state.logs.isNotEmpty()) logListState.animateScrollToItem(state.logs.size - 1) }
    LaunchedEffect(isLogsCopied) {
        if (isLogsCopied) {
            kotlinx.coroutines.delay(2000)
            isLogsCopied = false
        }
    }

    GlassCard(modifier = modifier, contentPadding = PaddingValues(12.dp)) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Terminal, null, tint = accent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        LocalizedStrings.get("live_engine_logs", lang),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = if (isDark) Color.White.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.85f)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.06f),
                        border = BorderStroke(1.dp, (if (isDark) GlassBorder else GlassBorderLight).copy(alpha = 0.3f))
                    ) {
                        Text(
                            "${state.logs.size} ${LocalizedStrings.get("logs_count", lang)}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (state.logs.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(state.logs.joinToString("\n")))
                                isLogsCopied = true
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                if (isLogsCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = LocalizedStrings.get("copy_all_logs", lang),
                                tint = if (isLogsCopied) TertiaryNeon else (if (isDark) TextMuted else TextMutedLight),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    if (onClearLogs != null && state.logs.isNotEmpty()) {
                        IconButton(
                            onClick = onClearLogs,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = LocalizedStrings.get("clear_logs", lang),
                                tint = if (isDark) TextMuted else TextMutedLight,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(if (isDark) Color.Black.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                    .border(1.dp, (if (isDark) GlassBorder else GlassBorderLight).copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .padding(8.dp)
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    LazyColumn(state = logListState, modifier = Modifier.fillMaxSize().padding(end = 12.dp)) {
                        itemsIndexed(state.logs, key = { index, log -> "${index}_$log" }) { _, log ->
                            val logColor = when {
                                log.contains("completed", ignoreCase = true) || log.contains("found", ignoreCase = true) || log.contains("active", ignoreCase = true) -> TertiaryNeon
                                log.contains("error", ignoreCase = true) || log.contains("failed", ignoreCase = true) || log.contains("refused", ignoreCase = true) -> DangerNeon
                                log.contains("warning", ignoreCase = true) || log.contains("timeout", ignoreCase = true) -> WarningNeon
                                log.contains("probing", ignoreCase = true) || log.contains("scan", ignoreCase = true) -> accent
                                else -> (if (isDark) Color.White else Color.Black).copy(alpha = 0.85f)
                            }
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("> ", color = accent, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold))
                                Text(
                                    log,
                                    color = logColor,
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                                    textAlign = TextAlign.Start
                                )
                            }
                        }
                        if (state.logs.isEmpty()) item {
                            Text(
                                LocalizedStrings.get("waiting_engine_activity", lang),
                                color = (if (isDark) TextMuted else TextMutedLight).copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                            )
                        }
                    }
                    PortXVerticalScrollbar(
                        listState = logListState,
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
fun FilterChipMini(
    label: String,
    count: Int? = null,
    isSelected: Boolean,
    accent: Color,
    isDanger: Boolean = false,
    onClick: () -> Unit
) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    val chipColor = if (isDanger) DangerNeon else accent
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) chipColor.copy(alpha = 0.18f) else (if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.04f)),
        border = BorderStroke(1.dp, if (isSelected) chipColor else (if (isDark) GlassBorder.copy(alpha = 0.3f) else GlassBorderLight.copy(alpha = 0.3f)))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 11.sp
                ),
                color = if (isSelected) (if (isDark) Color.White else Color.Black) else (if (isDark) TextMuted else TextMutedLight)
            )
            if (count != null && count > 0) {
                Text(
                    count.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (isSelected) chipColor else (if (isDark) TextMuted else TextMutedLight)
                )
            }
        }
    }
}

@Composable
fun ActiveServicesCard(state: ScanUIState, modifier: Modifier = Modifier, accent: Color, lang: String) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    val isRtl = lang == "fa" || lang == "ar"
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val result = state.result
    var selectedPortForDetail by remember { mutableStateOf<DisplayPort?>(null) }
    var portSearchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }

    val allPortsToShow = remember(result, accent) {
        result?.openPorts?.distinct()?.sorted()?.map { portNumber ->
            val banner = (result.portBanners[portNumber] ?: "").trim()
            val rawService = (result.portServices[portNumber] ?: "").trim()
            val title = getServiceTitle(portNumber, rawService.ifEmpty { null })
            val description = if (banner.isNotEmpty()) banner else getServiceDescription(portNumber, rawService.ifEmpty { null })
            DisplayPort(portNumber, title, description, getPortColor(portNumber, accent))
        } ?: emptyList()
    }

    val threatCount = remember(allPortsToShow) { allPortsToShow.count { it.color == DangerNeon } }
    val webCount = remember(allPortsToShow) { allPortsToShow.count { it.number in listOf(80, 443, 8000, 8008, 8080, 8443, 8888, 9000, 9090, 3000, 5000) } }
    val dbCount = remember(allPortsToShow) { allPortsToShow.count { it.number in listOf(1433, 1521, 3306, 5432, 6379, 27017, 9200, 11211) } }
    val remoteCount = remember(allPortsToShow) { allPortsToShow.count { it.number in listOf(21, 22, 23, 3389, 5900, 5985, 5986) } }

    val filteredPorts = remember(allPortsToShow, portSearchQuery, selectedCategoryFilter) {
        allPortsToShow.filter { port ->
            val matchesCategory = when (selectedCategoryFilter) {
                "THREATS" -> port.color == DangerNeon
                "WEB" -> port.number in listOf(80, 443, 8000, 8008, 8080, 8443, 8888, 9000, 9090, 3000, 5000)
                "DATABASE" -> port.number in listOf(1433, 1521, 3306, 5432, 6379, 27017, 9200, 11211)
                "REMOTE" -> port.number in listOf(21, 22, 23, 3389, 5900, 5985, 5986)
                else -> true
            }
            if (!matchesCategory) return@filter false

            if (portSearchQuery.isBlank()) true
            else {
                val q = portSearchQuery.trim().lowercase()
                port.number.toString().contains(q) ||
                port.title.lowercase().contains(q) ||
                port.description.lowercase().contains(q)
            }
        }
    }

    GlassCard(modifier = modifier) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Text(LocalizedStrings.get("active_services", lang), style = MaterialTheme.typography.labelLarge.copy(color = if (isDark) TextMuted else TextMutedLight, fontWeight = FontWeight.Bold, letterSpacing = 1.sp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (allPortsToShow.isNotEmpty()) {
                        val countText = if (filteredPorts.size != allPortsToShow.size) "${filteredPorts.size} / ${allPortsToShow.size}" else "${allPortsToShow.size}"
                        Text("$countText ${LocalizedStrings.get("found", lang)}", style = MaterialTheme.typography.labelSmall, color = accent, modifier = Modifier.background(accent.copy(alpha = 0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    Row(modifier = Modifier.background(if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f), CircleShape).border(1.dp, if (isDark) GlassBorder else GlassBorderLight, CircleShape)) {
                        IconButton(onClick = { scope.launch { listState.animateScrollToItem(0) } }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.KeyboardArrowUp, null, tint = if (isDark) Color.White else Color.Black, modifier = Modifier.size(18.dp)) }
                        IconButton(onClick = { scope.launch { if (filteredPorts.isNotEmpty()) listState.animateScrollToItem(filteredPorts.size - 1) } }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.KeyboardArrowDown, null, tint = if (isDark) Color.White else Color.Black, modifier = Modifier.size(18.dp)) }
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

            // --- PORT SEARCH & FILTER CONTROLS ---
            if (allPortsToShow.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                    OutlinedTextField(
                        value = portSearchQuery,
                        onValueChange = { portSearchQuery = it },
                        placeholder = { 
                            Text(
                                LocalizedStrings.get("search_ports_placeholder", lang), 
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) TextMuted else TextMutedLight,
                                maxLines = 1
                            ) 
                        },
                        leadingIcon = { 
                            Icon(Icons.Default.Search, null, tint = accent, modifier = Modifier.size(18.dp)) 
                        },
                        trailingIcon = {
                            if (portSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { portSearchQuery = "" }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Close, null, tint = if (isDark) TextMuted else TextMutedLight, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent,
                            unfocusedBorderColor = if (isDark) GlassBorder else GlassBorderLight,
                            focusedTextColor = if (isDark) Color.White else Color.Black,
                            unfocusedTextColor = if (isDark) Color.White else Color.Black
                        ),
                        textStyle = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.ContentOrLtr)
                    )

                    // Quick category chips
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChipMini(
                            label = LocalizedStrings.get("filter_all", lang),
                            count = allPortsToShow.size,
                            isSelected = selectedCategoryFilter == "ALL",
                            accent = accent,
                            onClick = { selectedCategoryFilter = "ALL" }
                        )
                        if (threatCount > 0) {
                            FilterChipMini(
                                label = LocalizedStrings.get("filter_threats", lang),
                                count = threatCount,
                                isSelected = selectedCategoryFilter == "THREATS",
                                accent = accent,
                                isDanger = true,
                                onClick = { selectedCategoryFilter = "THREATS" }
                            )
                        }
                        if (webCount > 0) {
                            FilterChipMini(
                                label = LocalizedStrings.get("filter_web", lang),
                                count = webCount,
                                isSelected = selectedCategoryFilter == "WEB",
                                accent = accent,
                                onClick = { selectedCategoryFilter = "WEB" }
                            )
                        }
                        if (dbCount > 0) {
                            FilterChipMini(
                                label = LocalizedStrings.get("filter_db", lang),
                                count = dbCount,
                                isSelected = selectedCategoryFilter == "DATABASE",
                                accent = accent,
                                onClick = { selectedCategoryFilter = "DATABASE" }
                            )
                        }
                        if (remoteCount > 0) {
                            FilterChipMini(
                                label = LocalizedStrings.get("filter_remote", lang),
                                count = remoteCount,
                                isSelected = selectedCategoryFilter == "REMOTE",
                                accent = accent,
                                onClick = { selectedCategoryFilter = "REMOTE" }
                            )
                        }
                    }
                }
            }

            // --- PORTS LIST + AUTOMATIC RTL/LTR SCROLLBAR ---
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = if (isRtl) 14.dp else 0.dp,
                            end = if (!isRtl) 14.dp else 0.dp
                        ),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    if (filteredPorts.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        if (state.isLoading) Icons.Default.HourglassEmpty else Icons.Default.SearchOff,
                                        null,
                                        tint = (if (isDark) TextMuted else TextMutedLight).copy(alpha = 0.3f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        if (state.isLoading) LocalizedStrings.get("scanning_network", lang)
                                        else if (portSearchQuery.isNotEmpty() || selectedCategoryFilter != "ALL") LocalizedStrings.get("no_ports_found", lang)
                                        else LocalizedStrings.get("no_services", lang),
                                        color = if (isDark) TextMuted else TextMutedLight,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    if (portSearchQuery.isNotEmpty() || selectedCategoryFilter != "ALL") {
                                        Spacer(Modifier.height(8.dp))
                                        TextButton(onClick = {
                                            portSearchQuery = ""
                                            selectedCategoryFilter = "ALL"
                                        }) {
                                            Text(LocalizedStrings.get("clear_filter", lang), color = accent, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        items(filteredPorts, key = { it.number }) { port ->
                            PortItem(port = port, onClick = { selectedPortForDetail = port })
                        }
                    }
                }

                // Automatic Left for RTL (fa/ar), Right for LTR (en/etc.)
                PortXVerticalScrollbar(
                    listState = listState,
                    modifier = Modifier
                        .align(if (isRtl) AbsoluteAlignment.CenterLeft else AbsoluteAlignment.CenterRight)
                        .fillMaxHeight()
                )

                Box(modifier = Modifier.fillMaxWidth().height(32.dp).align(Alignment.BottomCenter).background(Brush.verticalGradient(listOf(Color.Transparent, (if (isDark) BackgroundDark else BackgroundLight).copy(alpha = 0.5f)))))
            }

            if (selectedPortForDetail != null) {
                PortDetailDialog(
                    port = selectedPortForDetail!!,
                    target = result?.target ?: state.ip.ifBlank { "127.0.0.1" },
                    rawBanner = result?.portBanners?.get(selectedPortForDetail!!.number),
                    rawService = result?.portServices?.get(selectedPortForDetail!!.number),
                    accent = accent,
                    lang = lang,
                    onDismiss = { selectedPortForDetail = null }
                )
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
fun PortItem(port: DisplayPort, onClick: () -> Unit = {}) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, if (isDark) GlassBorder else GlassBorderLight, RoundedCornerShape(16.dp))
            .clickable { onClick() }, 
        color = if (isDark) GlassSurface else Color.Black.copy(alpha = 0.02f)
    ) {
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

fun getPortRiskLevel(port: Int): Triple<String, Color, ImageVector> {
    return when (port) {
        21, 23, 102, 135, 137, 138, 139, 445, 502, 1883, 2049, 2181, 2375, 2379, 5555, 10250, 11211 -> 
            Triple("CRITICAL / HIGH RISK", DangerNeon, Icons.Default.Warning)
        22, 1194, 1433, 1521, 3306, 3389, 4840, 51820, 5432, 5900, 6379, 6443, 8123, 8200, 8500, 9042, 9200, 9300, 27017 -> 
            Triple("PRIVILEGED / ADMINISTRATIVE", SecondaryNeon, Icons.Default.Lock)
        80, 443, 8000, 8080, 8081, 8088, 8443, 8888, 9090, 3000, 5000 -> 
            Triple("STANDARD WEB SERVICE", PrimaryNeon, Icons.Default.Language)
        else -> 
            Triple("STANDARD NETWORK SERVICE", TertiaryNeon, Icons.Default.CheckCircle)
    }
}

fun getPortSecurityAdvisory(port: Int): String {
    return when (port) {
        21 -> "Unencrypted FTP transmits credentials in plaintext. Migrate to SFTP (Port 22) or FTPS (TLS). Block external access at perimeter firewall."
        22 -> "Enforce SSH public key authentication only, disable root login (PermitRootLogin no), and implement rate-limiting or Fail2ban."
        23 -> "Telnet is deprecated and completely unencrypted. Terminate this legacy daemon immediately and replace with SSH."
        53 -> "DNS resolver detected. Ensure open recursion is disabled to prevent DNS amplification and cache poisoning attacks."
        80 -> "Unencrypted HTTP traffic. Enforce HTTPS redirection with HSTS headers and modern TLS certificates."
        135 -> "Microsoft RPC Endpoint Mapper is a high-profile vector for lateral movement and privilege escalation. Block port 135 at the network perimeter."
        137, 138, 139 -> "NetBIOS services expose internal network topology and credentials. Isolate to internal VLAN or disable SMBv1 entirely."
        443 -> "Secure HTTPS web endpoint. Audit TLS configuration to ensure TLS 1.0/1.1 are disabled and strong cipher suites are enforced."
        445 -> "SMB / Active Directory file sharing. Primary vector for ransomware propagation (e.g. WannaCry/EternalBlue). Never expose to the public internet."
        1433 -> "Microsoft SQL Server database. Enforce strong authentication, disable the 'sa' account, and restrict access via VPN or IP whitelist."
        1883 -> "Unencrypted MQTT IoT broker. Migrate to port 8883 with mutual TLS (mTLS) authentication."
        3306 -> "MySQL/MariaDB database port exposed. Bind exclusively to localhost (127.0.0.1) or enforce SSH tunneling."
        3389 -> "Remote Desktop Protocol (RDP). High-risk target for brute-force ransomware attacks. Require VPN connection and enable NLA."
        5432 -> "PostgreSQL database port. Restrict access in pg_hba.conf to trusted IP ranges and require SSL/TLS connections."
        6379 -> "Redis in-memory store. By default lacks authentication or encryption. Bind to 127.0.0.1, set strong requirepass, and disable dangerous commands."
        8080, 8443 -> "Alternate HTTP/HTTPS proxy or admin service. Inspect exposed debug endpoints (e.g. Spring Actuator) and default passwords."
        27017 -> "MongoDB database port exposed. Verify authorization is enabled (auth = true) and TLS encryption is active."
        else -> "Verify if this service requires public accessibility. Apply principle of least privilege and strict firewall ingress filtering."
    }
}

@Composable
fun PortDetailDialog(
    port: DisplayPort,
    target: String,
    rawBanner: String?,
    rawService: String?,
    accent: Color,
    lang: String,
    onDismiss: () -> Unit
) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    val clipboardManager = LocalClipboardManager.current
    var isCopied by remember { mutableStateOf(false) }
    val (riskLabel, riskColor, riskIcon) = getPortRiskLevel(port.number)
    val advisory = getPortSecurityAdvisory(port.number)
    val dialogScrollState = rememberScrollState()
    var copiedAction by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(copiedAction) {
        if (copiedAction != null) {
            kotlinx.coroutines.delay(2000)
            copiedAction = null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) SurfaceDark else SurfaceLight,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(port.color.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .border(1.dp, port.color.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            port.number.toString(),
                            color = port.color,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            fontSize = if (port.number > 9999) 10.sp else 12.sp
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            port.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isDark) Color.White else Color.Black
                        )
                        Text(
                            "\u2066${target}:${port.number}\u2069 • TCP LISTENING",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, textDirection = TextDirection.Ltr),
                            color = if (isDark) TextMuted else TextMutedLight
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, null, tint = if (isDark) TextMuted else TextMutedLight)
                }
            }
        },
        text = {
            Box(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(dialogScrollState)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Risk Level & State Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = riskColor.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, riskColor.copy(alpha = 0.4f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(riskIcon, null, tint = riskColor, modifier = Modifier.size(16.dp))
                                Text(
                                    text = riskLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                                    color = riskColor,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = TertiaryNeon.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, TertiaryNeon.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(TertiaryNeon))
                                Text("OPEN", color = TertiaryNeon, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }

                    // Quick Actions Row (cURL, IP:Port copy)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val urlStr = if (port.number == 443 || port.number == 8443) "https://${target}:${port.number}" else "http://${target}:${port.number}"
                                clipboardManager.setText(AnnotatedString(urlStr))
                                copiedAction = "URL"
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, (if (isDark) GlassBorder else GlassBorderLight).copy(alpha = 0.6f)),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(if (copiedAction == "URL") Icons.Default.Check else Icons.Default.Link, null, tint = accent, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (copiedAction == "URL") LocalizedStrings.get("copied", lang) else LocalizedStrings.get("copy_url", lang), fontSize = 11.sp, color = if (isDark) Color.White else Color.Black)
                        }

                        OutlinedButton(
                            onClick = {
                                val curlCmd = "curl -v -m 5 http://${target}:${port.number}/"
                                clipboardManager.setText(AnnotatedString(curlCmd))
                                copiedAction = "CURL"
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, (if (isDark) GlassBorder else GlassBorderLight).copy(alpha = 0.6f)),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(if (copiedAction == "CURL") Icons.Default.Check else Icons.Default.Terminal, null, tint = SecondaryNeon, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (copiedAction == "CURL") LocalizedStrings.get("copied", lang) else LocalizedStrings.get("copy_curl", lang), fontSize = 11.sp, color = if (isDark) Color.White else Color.Black)
                        }
                    }

                    // Service & Banner Box
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color.Black.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.04f),
                        border = BorderStroke(1.dp, (if (isDark) GlassBorder else GlassBorderLight).copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "DETECTED BANNER / SERVICE SIGNATURE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp, letterSpacing = 1.sp),
                                color = accent
                            )
                            val bannerText = if (!rawBanner.isNullOrBlank()) rawBanner.trim() else port.description
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Text(
                                    text = bannerText,
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = if (isDark) Color.White.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.9f),
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // Security Advisory Box
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.03f),
                        border = BorderStroke(1.dp, (if (isDark) GlassBorder else GlassBorderLight).copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Security, null, tint = SecondaryNeon, modifier = Modifier.size(14.dp))
                                Text(
                                    "SECURITY ADVISORY & HARDENING",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp, letterSpacing = 1.sp),
                                    color = SecondaryNeon
                                )
                            }
                            Text(
                                text = advisory,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = if (isDark) TextSecondary else TextSecondaryLight
                            )
                        }
                    }
                }

                PortXScrollStateVerticalScrollbar(
                    scrollState = dialogScrollState,
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val fullDetails = buildString {
                        appendLine("Port: ${port.number}")
                        appendLine("Title: ${port.title}")
                        appendLine("Target: ${target}:${port.number}")
                        appendLine("Status: OPEN (TCP)")
                        appendLine("Banner: ${rawBanner ?: port.description}")
                        appendLine("Risk: $riskLabel")
                        appendLine("Advisory: $advisory")
                    }
                    clipboardManager.setText(AnnotatedString(fullDetails))
                    isCopied = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (isCopied) LocalizedStrings.get("copied", lang) else LocalizedStrings.get("copy_details", lang), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(LocalizedStrings.get("close", lang), color = if (isDark) Color.White else Color.Black)
            }
        },
        modifier = Modifier.padding(16.dp).widthIn(max = 520.dp)
    )
}

