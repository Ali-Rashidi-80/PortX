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
import com.mrcoder20.portx.presentation.ui.components.DisintegrationContainer
import com.mrcoder20.portx.presentation.ui.components.PortXVerticalScrollbar
import com.mrcoder20.portx.presentation.ui.components.PortXScrollStateVerticalScrollbar
import com.mrcoder20.portx.presentation.ui.components.cyberPulse
import com.mrcoder20.portx.presentation.ui.components.springPress
import com.mrcoder20.portx.presentation.ui.components.touchDragScroll
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
            width >= 1020.dp -> LargeDesktopDashboard(state, viewModel, accent, lang)
            width >= 620.dp -> DesktopDashboard(state, viewModel, accent, lang)
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
    var leftSubTab by remember { mutableStateOf(0) }
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
            // Left Column (weight 1f): Visualizer & Tabbed Logs/Config
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SecurityVisualizerCard(
                    state,
                    modifier = Modifier.fillMaxWidth().height(230.dp),
                    accent = accent,
                    lang = lang
                )
                
                CyberSegmentedControl(
                    items = listOf(
                        LocalizedStrings.get("logs_tab", lang) to Icons.Default.Terminal,
                        LocalizedStrings.get("engine_config", lang) to Icons.Default.Tune
                    ),
                    selectedIndex = leftSubTab,
                    onIndexSelected = { leftSubTab = it },
                    accent = accent,
                    modifier = Modifier.fillMaxWidth()
                )

                AnimatedContent(
                    targetState = leftSubTab,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    transitionSpec = { fadeIn() togetherWith fadeOut() }
                ) { tab ->
                    if (tab == 0) {
                        EngineLogsCard(
                            state,
                            modifier = Modifier.fillMaxSize(),
                            accent = accent,
                            lang = lang,
                            onClearLogs = { viewModel.clearLogs() }
                        )
                    } else {
                        val scrollState = rememberScrollState()
                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(scrollState)
                                    .padding(end = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AdvancedParametersCard(state, viewModel, accent = accent, lang = lang)
                                EngineConfigurationCard(state, viewModel, accent = accent, lang = lang)
                            }
                            PortXScrollStateVerticalScrollbar(
                                scrollState = scrollState,
                                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                            )
                        }
                    }
                }
            }

            // Right Column (weight 1.25f): Discovered Services & CVE Advisories (Full Height)
            Column(
                modifier = Modifier.weight(1.25f).fillMaxHeight()
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
fun MobileDashboard(state: ScanUIState, viewModel: ScanViewModel, accent: Color, lang: String) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    var showSettings by remember { mutableStateOf(false) }
    var selectedMobileTab by remember { mutableStateOf(0) }
    var isRadarCollapsed by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(10.dp))
        
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
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AdvancedParametersCard(state, viewModel, accent, lang)
                EngineConfigurationCard(state, viewModel, accent, lang)
            }
        }

        // Responsive collapsible or compact radar HUD
        AnimatedVisibility(
            visible = !isRadarCollapsed,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            SecurityVisualizerCard(
                state = state,
                modifier = Modifier.fillMaxWidth().height(135.dp),
                smallSize = true,
                accent = accent,
                lang = lang,
                onCollapse = { isRadarCollapsed = true }
            )
        }

        if (isRadarCollapsed) {
            Surface(
                onClick = { isRadarCollapsed = false },
                shape = RoundedCornerShape(12.dp),
                color = if (isDark) GlassBackground else GlassLight,
                border = BorderStroke(1.dp, if (isDark) GlassBorder else GlassBorderLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val score = state.result?.securityScore ?: 0
                    val statusColor = when {
                        state.isLoading -> accent
                        state.result != null -> if (score > 80) TertiaryNeon else if (score > 50) (if (isDark) WarningNeon else WarningLight) else (if (isDark) DangerNeon else DangerLight)
                        else -> accent
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier.size(28.dp).background(statusColor.copy(alpha = 0.15f), CircleShape).border(1.dp, statusColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (state.isLoading) "${state.progress}%" else "$score%",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = statusColor, fontSize = 9.sp)
                            )
                        }
                        Text(
                            text = if (state.isLoading) LocalizedStrings.get("engine_running", lang) else if (state.result != null) "${LocalizedStrings.get("security_score", lang)}: $score%" else LocalizedStrings.get("ready", lang),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isDark) Color.White else Color.Black
                        )
                        if (!state.firewallStatus.isNullOrBlank()) {
                            Text(
                                text = "• ${state.firewallStatus}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = if (isDark) TextMuted else TextMutedLight,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                    Icon(Icons.Default.ExpandMore, contentDescription = "Expand", tint = accent, modifier = Modifier.size(18.dp))
                }
            }
        }

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
        Spacer(Modifier.height(4.dp))
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
                // Dedicated Cyber Search Input Box
                Surface(
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color.Black.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, (if (isDark) GlassBorder else GlassBorderLight).copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (showSettingsToggle) {
                            IconButton(onClick = onSettingsToggle, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Tune, null, tint = accent, modifier = Modifier.size(18.dp))
                            }
                        } else {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.padding(start = 4.dp, end = 4.dp).size(20.dp)
                            )
                        }

                        OutlinedTextField(
                            value = state.ip,
                            onValueChange = onIpChange,
                            placeholder = {
                                Text(
                                    LocalizedStrings.get("target", lang),
                                    color = if (isDark) TextMuted else TextMutedLight,
                                    fontSize = 13.sp,
                                    maxLines = 1
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent,
                                unfocusedTextColor = if (isDark) Color.White else Color.Black,
                                focusedTextColor = if (isDark) Color.White else Color.Black,
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
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.Monospace,
                                textDirection = TextDirection.Ltr,
                                fontSize = 14.sp
                            )
                        )

                        if (state.ip.isNotEmpty()) {
                            val ipTrimmed = state.ip.trim()
                            val isIpv4 = ipTrimmed.split(".").let { parts -> parts.size == 4 && parts.all { p -> p.toIntOrNull() in 0..255 } }
                            val isLocal = ipTrimmed == "127.0.0.1" || ipTrimmed.equals("localhost", ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = (if (isLocal) TertiaryNeon else accent).copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, (if (isLocal) TertiaryNeon else accent).copy(alpha = 0.4f)),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Text(
                                    if (isLocal) "LOCAL" else if (isIpv4) "IPv4" else "HOST",
                                    color = if (isLocal) TertiaryNeon else accent,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            IconButton(
                                onClick = { onIpChange("") },
                                modifier = Modifier.size(28.dp).springPress()
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = if (isDark) TextMuted else TextMutedLight, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                val infiniteTransition = rememberInfiniteTransition()
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.6f, targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse)
                )

                Button(
                    onClick = { if (state.isLoading) onStopScan() else onStartScan() },
                    colors = ButtonDefaults.buttonColors(containerColor = if (state.isLoading) DangerNeon.copy(alpha = pulseAlpha) else accent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(46.dp).width(90.dp).springPress(pressedScale = 0.94f),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(if (state.isLoading) Icons.Default.Stop else Icons.Default.FlashOn, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (state.isLoading) LocalizedStrings.get("stop", lang) else LocalizedStrings.get("scan", lang), fontWeight = FontWeight.Black, color = Color.Black, fontSize = 12.sp)
                }
            }

            // Quick Target Presets Row
            val isRtl = lang == "fa" || lang == "ar"
            val ipPresetsScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .touchDragScroll(ipPresetsScrollState, isVertical = false, isRtl = isRtl)
                    .horizontalScroll(ipPresetsScrollState)
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
                        border = BorderStroke(1.dp, if (isSelected) accent else (if (isDark) GlassBorder else GlassBorderLight).copy(alpha = 0.3f)),
                        modifier = Modifier.springPress(pressedScale = 0.94f)
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
                Spacer(Modifier.width(16.dp))
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
            Spacer(Modifier.height(14.dp))

            // Engine Features in a clean 2x2 symmetrical grid
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScanChip(
                        label = LocalizedStrings.get("banner_grabbing", lang),
                        checked = state.bannerGrabbing,
                        accent = accent,
                        modifier = Modifier.weight(1f)
                    ) { viewModel.toggleBannerGrabbing(it) }

                    ScanChip(
                        label = LocalizedStrings.get("full_port_scan", lang),
                        checked = state.allPorts,
                        accent = accent,
                        modifier = Modifier.weight(1f)
                    ) { viewModel.toggleAllPorts(it) }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScanChip(
                        label = LocalizedStrings.get("multi_protocol", lang),
                        checked = state.allProtocols,
                        accent = accent,
                        modifier = Modifier.weight(1f)
                    ) { viewModel.toggleAllProtocols(it) }

                    ScanChip(
                        label = LocalizedStrings.get("stealth_mode", lang),
                        checked = state.scanType == "SYN",
                        accent = accent,
                        modifier = Modifier.weight(1f)
                    ) { viewModel.onScanTypeChange(if (it) "SYN" else "TCP") }
                }
            }

            if (!state.allPorts) {
                val currentRange = "${state.startPort}-${state.endPort}"
                Spacer(Modifier.height(16.dp))
                Text(LocalizedStrings.get("port_range", lang), style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp), color = if (isDark) TextMuted else TextMutedLight, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
fun SecurityVisualizerCard(
    state: ScanUIState, 
    modifier: Modifier = Modifier, 
    smallSize: Boolean = false, 
    accent: Color, 
    lang: String,
    onCollapse: (() -> Unit)? = null
) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    GlassCard(modifier = modifier, contentPadding = if (smallSize) PaddingValues(10.dp) else PaddingValues(16.dp)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                contentAlignment = Alignment.Center, 
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                if (onCollapse != null) {
                    IconButton(
                        onClick = onCollapse,
                        modifier = Modifier.align(Alignment.TopEnd).size(26.dp)
                    ) {
                        Icon(Icons.Default.ExpandLess, contentDescription = "Collapse", tint = accent, modifier = Modifier.size(18.dp))
                    }
                }
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
                AdvancedLiquidGauge(progress = displayProgress, isLoading = state.isLoading, color = statusColor, gaugeSize = if (smallSize) 125.dp else 210.dp)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (state.isLoading) {
                        Text(
                            "${state.progress}%", 
                            style = (if (smallSize) MaterialTheme.typography.titleLarge else MaterialTheme.typography.displayMedium).copy(
                                fontWeight = FontWeight.Black, 
                                color = if (isDark) Color.White else Color.Black, 
                                shadow = Shadow(color = statusColor, blurRadius = 25f)
                            )
                        )
                        Text(LocalizedStrings.get("engine_running", lang), style = (if (smallSize) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium).copy(color = statusColor, letterSpacing = if (smallSize) 1.sp else 2.sp))
                    } else {
                        val result = state.result
                        if (result != null) {
                            Text(
                                "${result.securityScore}%", 
                                style = (if (smallSize) MaterialTheme.typography.titleLarge else MaterialTheme.typography.displayMedium).copy(
                                    fontWeight = FontWeight.Black, 
                                    color = if (isDark) Color.White else Color.Black, 
                                    shadow = Shadow(color = statusColor, blurRadius = 25f)
                                )
                            )
                            Text(LocalizedStrings.get("security_score", lang), style = (if (smallSize) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium).copy(color = statusColor, letterSpacing = if (smallSize) 1.sp else 2.sp))
                        } else {
                            Icon(Icons.Default.Radar, null, tint = if (isDark) Color.White.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.3f), modifier = Modifier.size(if (smallSize) 28.dp else 48.dp))
                            Text(LocalizedStrings.get("ready", lang), style = (if (smallSize) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleMedium).copy(color = if (isDark) TextMuted else TextMutedLight, letterSpacing = if (smallSize) 1.sp else 2.sp))
                        }
                    }
                }
            }

            // Bottom Profile / Perimeter Status Container (clean, concise, and localized)
            if (!state.firewallStatus.isNullOrBlank()) {
                Spacer(Modifier.height(if (smallSize) 4.dp else 8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.03f),
                    border = BorderStroke(1.dp, (if (isDark) GlassBorder else GlassBorderLight).copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = if (smallSize) 4.dp else 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(if (smallSize) 13.dp else 16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        val localizedFirewall = when {
                            state.firewallStatus.contains("Open Perimeter") -> LocalizedStrings.get("firewall_open", lang)
                            state.firewallStatus.contains("Hardened Perimeter") -> LocalizedStrings.get("firewall_hardened", lang)
                            state.firewallStatus.contains("Standard") -> LocalizedStrings.get("firewall_standard", lang)
                            state.firewallStatus.contains("Firewall") -> LocalizedStrings.get("firewall_filtered", lang)
                            else -> state.firewallStatus
                        }
                        Text(
                            text = localizedFirewall,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = if (smallSize) 10.sp else 11.sp),
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
    val isRtl = lang == "fa" || lang == "ar"
    val logListState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current
    var isLogsCopied by remember { mutableStateOf(false) }
    var isClearingLogs by remember { mutableStateOf(false) }

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
                            modifier = Modifier.size(28.dp).springPress()
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
                            onClick = { isClearingLogs = true },
                            modifier = Modifier.size(28.dp).springPress(pressedScale = 0.90f)
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = LocalizedStrings.get("clear_logs", lang),
                                tint = DangerNeon.copy(alpha = 0.85f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            DisintegrationContainer(
                isDisintegrating = isClearingLogs,
                accent = DangerNeon,
                particleCount = 180,
                onDisintegrated = {
                    onClearLogs?.invoke()
                    isClearingLogs = false
                },
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (isDark) Color.Black.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                        .border(1.dp, (if (isDark) GlassBorder else GlassBorderLight).copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(8.dp)
                ) {
                    if (state.logs.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                            contentAlignment = if (isRtl) Alignment.TopEnd else Alignment.TopStart
                        ) {
                            Text(
                                LocalizedStrings.get("waiting_engine_activity", lang),
                                color = (if (isDark) TextMuted else TextMutedLight).copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    textDirection = if (isRtl) TextDirection.Rtl else TextDirection.Ltr,
                                    textAlign = if (isRtl) TextAlign.Right else TextAlign.Left
                                )
                            )
                        }
                    } else {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            LazyColumn(
                                state = logListState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .touchDragScroll(logListState, isVertical = true)
                                    .padding(end = 12.dp)
                            ) {
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
        border = BorderStroke(1.dp, if (isSelected) chipColor else (if (isDark) GlassBorder.copy(alpha = 0.3f) else GlassBorderLight.copy(alpha = 0.3f))),
        modifier = Modifier.requiredHeight(28.dp).springPress(pressedScale = 0.94f)
    ) {
        Row(
            modifier = Modifier.fillMaxHeight().padding(horizontal = 8.dp),
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
    var showExportModal by remember { mutableStateOf(false) }

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
                    if (result != null) {
                        Surface(
                            onClick = { showExportModal = true },
                            shape = RoundedCornerShape(8.dp),
                            color = accent.copy(alpha = if (isDark) 0.14f else 0.10f),
                            border = BorderStroke(1.dp, accent.copy(alpha = if (isDark) 0.45f else 0.35f)),
                            modifier = Modifier.padding(horizontal = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.FileDownload,
                                    contentDescription = LocalizedStrings.get("export_report", lang),
                                    tint = accent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    LocalizedStrings.get("export_report", lang),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                    color = if (isDark) Color.White else Color.Black
                                )
                            }
                        }
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
                    val isRtl = lang == "fa" || lang == "ar"
                    val chipsScrollState = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .touchDragScroll(chipsScrollState, isVertical = false, isRtl = isRtl)
                            .horizontalScroll(chipsScrollState),
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
                        Spacer(Modifier.width(16.dp))
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
                        .touchDragScroll(listState, isVertical = true)
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

            if (showExportModal && result != null) {
                ReportExportDialog(
                    scan = result,
                    accent = accent,
                    lang = lang,
                    isDark = isDark,
                    onDismiss = { showExportModal = false }
                )
            }
        }
    }
}

@Composable
fun ScanChip(label: String, checked: Boolean, accent: Color, modifier: Modifier = Modifier, onCheckedChange: (Boolean) -> Unit) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    Surface(
        onClick = { onCheckedChange(!checked) }, 
        shape = RoundedCornerShape(12.dp), 
        color = if (checked) accent.copy(alpha = 0.15f) else (if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f)), 
        border = BorderStroke(1.dp, if (checked) accent else (if (isDark) GlassBorder else GlassBorderLight)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), 
            verticalAlignment = Alignment.CenterVertically, 
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (checked) accent else (if (isDark) TextMuted else TextMutedLight)))
            Text(
                label, 
                style = MaterialTheme.typography.labelMedium, 
                color = if (checked) (if (isDark) Color.White else Color.Black) else (if (isDark) TextMuted else TextMutedLight),
                softWrap = false
            )
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

fun getPortRiskLevel(port: Int, lang: String = "en"): Triple<String, Color, ImageVector> {
    return when (port) {
        21, 23, 102, 135, 137, 138, 139, 445, 502, 1883, 2049, 2181, 2375, 2379, 5555, 10250, 11211 -> {
            val label = when (lang) {
                "fa" -> "بحرانی / پرخطر"
                "ar" -> "حرج / عالي الخطورة"
                else -> "CRITICAL / HIGH RISK"
            }
            Triple(label, DangerNeon, Icons.Default.Warning)
        }
        22, 1194, 1433, 1521, 3306, 3389, 4840, 51820, 5432, 5900, 6379, 6443, 8123, 8200, 8500, 9042, 9200, 9300, 27017 -> {
            val label = when (lang) {
                "fa" -> "دسترسی ویژه / مدیریتی"
                "ar" -> "وصول متميز / إداري"
                else -> "PRIVILEGED / ADMINISTRATIVE"
            }
            Triple(label, SecondaryNeon, Icons.Default.Lock)
        }
        80, 443, 8000, 8080, 8081, 8088, 8443, 8888, 9090, 3000, 5000 -> {
            val label = when (lang) {
                "fa" -> "وب‌سرویس استاندارد"
                "ar" -> "خدمة ويب قياسية"
                else -> "STANDARD WEB SERVICE"
            }
            Triple(label, PrimaryNeon, Icons.Default.Language)
        }
        else -> {
            val label = when (lang) {
                "fa" -> "سرویس شبکه استاندارد"
                "ar" -> "خدمة شبكة قياسية"
                else -> "STANDARD NETWORK SERVICE"
            }
            Triple(label, TertiaryNeon, Icons.Default.CheckCircle)
        }
    }
}

fun getPortSecurityAdvisory(port: Int, lang: String = "en"): String {
    if (lang == "fa") {
        return when (port) {
            21 -> "پروتکل FTP ناامن است و اطلاعات ورود را به صورت متن خام منتقل می‌کند. به SFTP (پورت ۲۲) یا FTPS مهاجرت کنید و دسترسی خارجی فایروال را مسدود نمایید."
            22 -> "فقط احراز هویت با کلید عمومی SSH را مجاز کنید، ورود مستقیم کاربر root را غیرفعال کرده و از Fail2ban جهت جلوگیری از حملات Brute-force استفاده کنید."
            23 -> "پروتکل Telnet کاملاً منسوخ و فاقد رمزنگاری است. این سرویس را فوراً متوقف کرده و SSH را جایگزین نمایید."
            53 -> "سرویس DNS شناسایی شد. مطمئن شوید Open Recursion غیرفعال باشد تا از حملات تقویت DNS و مسموم‌سازی کش جلوگیری شود."
            80 -> "ترافیک وب HTTP غیررمزنگاری‌شده است. تغییر مسیر خودکار به HTTPS با هدرهای HSTS و گواهینامه‌های معتبر TLS را اجباری کنید."
            135 -> "سرویس Microsoft RPC یک بردار حمله پرخطر برای نفوذ جانبی و افزایش سطح دسترسی در شبکه ویندوزی است. دسترسی این پورت را در فایروال مسدود کنید."
            137, 138, 139 -> "سرویس‌های NetBIOS اطلاعات ساختار شبکه داخلی را افشا می‌کنند. آن‌ها را به VLAN داخلی محدود کرده یا SMBv1 را کاملاً غیرفعال نمایید."
            443 -> "اندپوینت وب امن HTTPS. پیکربندی TLS را بررسی کنید تا نسخه‌های قدیمی TLS 1.0/1.1 غیرفعال باشند و از رمزنگاری‌های مدرن استفاده شود."
            445 -> "اشتراک فایل SMB / اکتیو دایرکتوری، بردار اصلی انتشار باج‌افزارها (مانند WannaCry) است. هرگز نباید در اینترنت عمومی باز باشد."
            1433 -> "پایگاه‌داده Microsoft SQL Server. رمزهای پیچیده اعمال کنید، کاربر 'sa' را غیرفعال کرده و دسترسی را فقط به VPN یا IPهای مجاز محدود سازید."
            1883 -> "بروکر IoT پروتکل MQTT بدون رمزنگاری است. به پورت ۸۸۸۳ با احراز هویت دوطرفه گواهینامه دیجیتال (mTLS) مهاجرت نمایید."
            3306 -> "پورت پایگاه‌داده MySQL/MariaDB در دسترس است. تنها به localhost (127.0.0.1) گوش فرا دهید یا از تونل امن SSH استفاده کنید."
            3389 -> "سرویس ریموت دسکتاپ ویندوز (RDP). هدف دائمی حملات نفوذ باج‌افزاری است. احراز هویت در سطح شبکه (NLA) را فعال کرده و اتصال را به VPN محدود کنید."
            5432 -> "پورت پایگاه‌داده PostgreSQL. دسترسی را در pg_hba.conf به رنج‌های IP معتبر محدود کرده و اتصال اجباری SSL/TLS را فعال کنید."
            6379 -> "پایگاه‌داده حافظه‌محور Redis به طور پیش‌فرض فاقد احراز هویت است. به 127.0.0.1 متصل شوید، requirepass قوی تعیین کرده و دستورات خطرناک را غیرفعال کنید."
            5900 -> "سرویس ریموت دسکتاپ VNC ترافیک صفحه را منتقل می‌کند. رمز عبور قوی قرار دهید، احراز هویت امن را اجباری کرده و از تونل SSH یا VPN استفاده کنید."
            8080, 8443 -> "سرویس وب یا پراکسی جانبی. مسیرهای عیب‌یابی، Swagger و پنل‌های مدیریتی را بررسی کرده و از عدم استفاده از رمزهای پیش‌فرض مطمئن شوید."
            27017 -> "پورت پایگاه‌داده MongoDB باز است. مطمئن شوید احراز هویت (auth = true) فعال و رمزنگاری داده‌ها در حال انتقال (TLS) برقرار باشد."
            else -> "بررسی کنید که آیا این سرویس واقعاً نیاز به دسترسی عمومی دارد یا خیر. اصل حداقل دسترسی و فیلتر دقیق ترافیک ورودی فایروال را اعمال کنید."
        }
    } else if (lang == "ar") {
        return when (port) {
            21 -> "ينقل بروتوكول FTP غير المشفر بيانات الاعتماد كنص صريح. قم بالترقية إلى SFTP أو FTPS وحظر الوصول الخارجي."
            22 -> "فرض المصادقة بمفتاح SSH فقط، وتعطيل تسجيل دخول root، وتفعيل الحماية ضد هجمات القوة الغاشمة."
            23 -> "بروتوكول Telnet قديم وغير مشفر نهائياً. أوقف هذه الخدمة فوراً واستبدلها بـ SSH."
            53 -> "تم اكتشاف محلل DNS. تأكد من تعطيل التكرار المفتوح لمنع هجمات التضخيم والتسميم."
            80 -> "حركة مرور HTTP غير مشفرة. فرض إعادة التوجيه إلى HTTPS مع ترويسات HSTS وشهادات حديثة."
            135 -> "خدمة Microsoft RPC تشكل خطراً كبيراً للحركة الجانبية وتصعيد الامتيازات. احظر المنفذ في جدار الحماية."
            137, 138, 139 -> "خدمات NetBIOS تكشف تفاصيل الشبكة الداخلية. اعزلها داخل شبكة محلية أو عطل SMBv1."
            443 -> "نقطة نهاية HTTPS آمنة. تأكد من تعطيل بروتوكولات TLS القديمة وتفعيل حزم التشفير القوية."
            445 -> "مشاركة ملفات SMB. المتجه الرئيسي لبرمجيات الفدية مثل WannaCry. لا تعرضه للإنترنت العام نهائياً."
            1433 -> "قاعدة بيانات SQL Server. فرض كلمات مرور قوية، وتعطيل حساب 'sa'، وتقييد الوصول لشبكة VPN."
            1883 -> "وسيط MQTT غير مشفر. الانتقال إلى المنفذ 8883 مع مصادقة TLS المتبادلة."
            3306 -> "قاعدة بيانات MySQL/MariaDB مكشوفة. اربطها حصرياً بـ 127.0.0.1 أو استخدم نفق SSH."
            3389 -> "بروتوكول سطح المكتب البعيد RDP. هدف رئيسي لهجمات الفدية. اطلب اتصال VPN وفعّل NLA."
            5432 -> "قاعدة بيانات PostgreSQL. قيد الوصول في pg_hba.conf واشترط اتصالات SSL/TLS."
            6379 -> "ذاكرة Redis المؤقتة. اربط بـ 127.0.0.1، وعيّن requirepass قوي، وعطّل الأوامر الخطيرة."
            5900 -> "خدمة التحكم عن بعد VNC. عيّن كلمة مرور قوية واشترط الاتصال عبر نفق SSH أو VPN."
            8080, 8443 -> "خدمة ويب بديلة أو وكيل. افحص نقاط النهاية المعرضة وكلمات المرور الافتراضية."
            27017 -> "قاعدة بيانات MongoDB. تأكد من تفعيل المصادقة والتشفير عبر TLS."
            else -> "تحقق مما إذا كانت الخدمة تتطلب وصولاً عاماً. طبّق مبدأ الامتياز الأقل وتصفية جدار الحماية."
        }
    }
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
        5900 -> "VNC Remote Display server exposes desktop frames and inputs. Enforce strong authentication and tunnel through SSH or VPN."
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
    val (riskLabel, riskColor, riskIcon) = getPortRiskLevel(port.number, lang)
    val advisory = getPortSecurityAdvisory(port.number, lang)
    val dialogScrollState = rememberScrollState()
    var copiedAction by remember { mutableStateOf<String?>(null) }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

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
                            "\u2066${target}:${port.number}\u2069 • ${LocalizedStrings.get("tcp_listening", lang)}",
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
                        .padding(start = if (isRtl) 12.dp else 0.dp, end = if (isRtl) 0.dp else 12.dp),
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
                                Text(LocalizedStrings.get("port_open", lang), color = TertiaryNeon, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
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
                                LocalizedStrings.get("detected_banner", lang),
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
                                    LocalizedStrings.get("security_advisory", lang),
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
                    modifier = Modifier.align(if (isRtl) Alignment.CenterStart else Alignment.CenterEnd).fillMaxHeight()
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
                Icon(if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp), tint = Color.Black)
                Spacer(Modifier.width(6.dp))
                Text(if (isCopied) LocalizedStrings.get("copied", lang) else LocalizedStrings.get("copy_details", lang), fontWeight = FontWeight.Bold, color = Color.Black)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, (if (isDark) GlassBorder else GlassBorderLight).copy(alpha = 0.6f))
            ) {
                Text(LocalizedStrings.get("close", lang), color = if (isDark) Color.White else Color.Black)
            }
        },
        modifier = Modifier.padding(16.dp).widthIn(max = 520.dp)
    )
}

