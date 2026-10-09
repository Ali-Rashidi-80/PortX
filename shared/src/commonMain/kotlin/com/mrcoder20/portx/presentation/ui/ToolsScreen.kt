package com.mrcoder20.portx.presentation.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrcoder20.portx.domain.DateFormatter
import com.mrcoder20.portx.domain.LocalizedStrings
import com.mrcoder20.portx.presentation.ui.components.LaunchedAutoScrollHint
import com.mrcoder20.portx.presentation.ui.components.LocalTouchEmulation
import com.mrcoder20.portx.presentation.ui.components.PortXScrollStateVerticalScrollbar
import com.mrcoder20.portx.presentation.ui.components.PortXVerticalScrollbar
import com.mrcoder20.portx.presentation.ui.components.horizontalFadingEdges
import com.mrcoder20.portx.presentation.ui.components.springPress
import com.mrcoder20.portx.presentation.ui.components.touchDragScroll
import com.mrcoder20.portx.presentation.ui.theme.*
import com.mrcoder20.portx.presentation.viewmodel.ToolsViewModel
import org.koin.compose.koinInject

@Composable
fun ToolsScreen(viewModel: ToolsViewModel = koinInject()) {
    val state by viewModel.uiState.collectAsState()
    val appSettings = LocalAppSettings.current
    val accent = LocalAccentColor.current
    val isDark = appSettings.theme == "DARK"
    val lang = appSettings.language
    val isTouchEmulated = LocalTouchEmulation.current

    val toolKeys = remember { listOf("LOCAL", "PING", "DNS", "WHOIS") }
    val toolDisplayItems = remember(lang) {
        listOf(
            LocalizedStrings.get("tool_local", lang) to Icons.Default.Lan,
            LocalizedStrings.get("tool_ping", lang) to Icons.Default.NetworkPing,
            LocalizedStrings.get("tool_dns", lang) to Icons.Default.Dns,
            LocalizedStrings.get("tool_whois", lang) to Icons.Default.Public
        )
    }
    val activeIndex = toolKeys.indexOf(state.activeTool).coerceAtLeast(0)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isMobile = maxWidth < 640.dp || isTouchEmulated

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isMobile) 10.dp else 16.dp),
                verticalArrangement = Arrangement.spacedBy(if (isMobile) 8.dp else 14.dp)
            ) {
                Spacer(modifier = Modifier.height(if (isMobile) 8.dp else 24.dp))

                // 1. TOOL SELECTOR (CYBER SEGMENTED CONTROL)
                CyberSegmentedControl(
                    items = toolDisplayItems,
                    selectedIndex = activeIndex,
                    onIndexSelected = { index ->
                        viewModel.selectTool(toolKeys[index])
                    },
                    accent = accent,
                    modifier = Modifier.fillMaxWidth()
                )

                // 2. INTEGRATED ACTION BAR (Hides automatically for LOCAL)
                AnimatedVisibility(
                    visible = state.activeTool != "LOCAL",
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(if (isMobile) 4.dp else 6.dp)) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (isDark) Modifier else Modifier.shadow(
                                        elevation = 2.dp,
                                        shape = RoundedCornerShape(if (isMobile) 10.dp else 14.dp),
                                        ambientColor = Color(0x120F172A),
                                        spotColor = Color(0x180F172A)
                                    )
                                ),
                            color = if (isDark) GlassBackground else Color.White,
                            shape = RoundedCornerShape(if (isMobile) 10.dp else 14.dp),
                            border = BorderStroke(1.dp, if (isDark) GlassBorder else BorderLight)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (isMobile) 42.dp else 52.dp)
                                    .padding(horizontal = if (isMobile) 6.dp else 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (state.activeTool) {
                                        "DNS" -> Icons.Default.Dns
                                        "WHOIS" -> Icons.Default.Public
                                        else -> Icons.Default.NetworkPing
                                    },
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier
                                        .padding(start = if (isMobile) 2.dp else 4.dp, end = 4.dp)
                                        .size(if (isMobile) 16.dp else 20.dp)
                                )

                                BasicTextField(
                                    value = state.target,
                                    onValueChange = { viewModel.onTargetChange(it) },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = if (isMobile) 11.5.sp else 13.sp,
                                        color = if (isDark) Color.White else Color.Black,
                                        textDirection = TextDirection.Ltr
                                    ),
                                    cursorBrush = SolidColor(accent),
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        imeAction = androidx.compose.ui.text.input.ImeAction.Go,
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Uri
                                    ),
                                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                        onGo = {
                                            if (state.isLoading) {
                                                viewModel.stopActiveTool()
                                            } else {
                                                when (state.activeTool) {
                                                    "PING" -> viewModel.runPing()
                                                    "DNS" -> viewModel.runDnsLookup()
                                                    "WHOIS" -> viewModel.runWhois()
                                                }
                                            }
                                        }
                                    ),
                                    modifier = Modifier.weight(1f),
                                    decorationBox = { innerTextField ->
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            if (state.target.isEmpty()) {
                                                Text(
                                                    when (state.activeTool) {
                                                        "DNS" -> LocalizedStrings.get("placeholder_dns", lang)
                                                        "WHOIS" -> LocalizedStrings.get("placeholder_whois", lang)
                                                        else -> LocalizedStrings.get("target", lang)
                                                    },
                                                    color = if (isDark) TextMuted else TextMutedLight,
                                                    fontSize = if (isMobile) 10.5.sp else 12.sp,
                                                    maxLines = 1
                                                )
                                            }
                                            innerTextField()
                                        }
                                    }
                                )

                                if (state.target.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.onTargetChange("") },
                                        modifier = Modifier
                                            .size(if (isMobile) 24.dp else 28.dp)
                                            .springPress()
                                            .pointerHoverIcon(PointerIcon.Hand)
                                    ) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = LocalizedStrings.get("clear", lang),
                                            tint = if (isDark) TextMuted else TextMutedLight,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.padding(end = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(if (isMobile) 4.dp else 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { viewModel.copyResultsToClipboard() },
                                        modifier = Modifier
                                            .size(if (isMobile) 28.dp else 36.dp)
                                            .springPress()
                                            .then(
                                                if (isDark) Modifier else Modifier.shadow(
                                                    elevation = 1.dp,
                                                    shape = CircleShape,
                                                    spotColor = Color(0x100F172A),
                                                    ambientColor = Color(0x0A0F172A)
                                                )
                                            )
                                            .background(
                                                if (isDark) Color.White.copy(alpha = 0.06f) else Color.White,
                                                CircleShape
                                            )
                                            .border(
                                                1.dp,
                                                if (isDark) Color.Transparent else BorderLight,
                                                CircleShape
                                            )
                                    ) {
                                        Icon(
                                            Icons.Default.ContentCopy,
                                            contentDescription = LocalizedStrings.get("copy", lang),
                                            tint = if (isDark) Color.White else Color.Black,
                                            modifier = Modifier.size(if (isMobile) 13.dp else 16.dp)
                                        )
                                    }

                                    val infiniteTransition = rememberInfiniteTransition()
                                    val pulseAlpha by infiniteTransition.animateFloat(
                                        initialValue = 0.65f,
                                        targetValue = 1f,
                                        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse)
                                    )

                                    Button(
                                        onClick = {
                                            if (state.isLoading) {
                                                viewModel.stopActiveTool()
                                            } else {
                                                when (state.activeTool) {
                                                    "PING" -> viewModel.runPing()
                                                    "DNS" -> viewModel.runDnsLookup()
                                                    "WHOIS" -> viewModel.runWhois()
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (state.isLoading) DangerNeon.copy(alpha = pulseAlpha) else accent
                                        ),
                                        shape = RoundedCornerShape(if (isMobile) 8.dp else 10.dp),
                                        modifier = Modifier
                                            .height(if (isMobile) 30.dp else 38.dp)
                                            .width(if (isMobile) 56.dp else 74.dp)
                                            .springPress(pressedScale = 0.94f),
                                        contentPadding = PaddingValues(horizontal = if (isMobile) 4.dp else 6.dp)
                                    ) {
                                        Icon(
                                            if (state.isLoading) Icons.Default.Stop else Icons.Default.FlashOn,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(if (isMobile) 13.dp else 16.dp)
                                        )
                                        Spacer(Modifier.width(2.dp))
                                        Text(
                                            if (state.isLoading) LocalizedStrings.get("stop", lang) else LocalizedStrings.get("go", lang),
                                            fontWeight = FontWeight.Black,
                                            color = Color.Black,
                                            fontSize = if (isMobile) 10.5.sp else 12.sp
                                        )
                                    }
                                }
                            }
                        }


                    }
                }

                state.error?.let {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DangerNeon.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, DangerNeon.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, null, tint = DangerNeon, modifier = Modifier.size(16.dp))
                            Text(
                                LocalizedStrings.get(it, lang),
                                color = DangerNeon,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                // 3. RESULTS DISPLAY
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    AnimatedContent(
                        targetState = state.activeTool,
                        transitionSpec = { fadeIn() togetherWith fadeOut() }
                    ) { tool ->
                        when (tool) {
                            "PING" -> PingResultPanel(
                                results = state.pingResults,
                                isLoading = state.isLoading,
                                accent = accent,
                                lang = lang,
                                isMobile = isMobile,
                                pingMode = state.pingMode,
                                pingPort = state.pingPort,
                                onPingModeChange = { viewModel.setPingMode(it) },
                                onPingPortChange = { viewModel.setPingPort(it) },
                                onCopyItem = { viewModel.copyIndividualResult(it) }
                            )
                            "DNS" -> DnsResultPanel(
                                dnsResult = state.dnsResult,
                                isLoading = state.isLoading,
                                accent = accent,
                                lang = lang,
                                isMobile = isMobile,
                                filterType = state.dnsFilterType,
                                viewMode = state.dnsViewMode,
                                onFilterChange = { viewModel.setDnsFilterType(it) },
                                onViewModeChange = { viewModel.setDnsViewMode(it) },
                                onCopyItem = { viewModel.copyIndividualResult(it) },
                                onSelectForPing = { selectedIp ->
                                    viewModel.onTargetChange(selectedIp)
                                    viewModel.selectTool("PING")
                                }
                            )
                            "WHOIS" -> WhoisResultPanel(
                                result = state.whoisResult,
                                isLoading = state.isLoading,
                                lang = lang,
                                isMobile = isMobile,
                                onCopy = { viewModel.copyIndividualResult(it) }
                            )
                            else -> LocalInfoPanel(
                                info = state.localIp,
                                subnetInfo = state.subnetInfo,
                                publicIp = state.publicIp,
                                isLoading = state.isLoading,
                                accent = accent,
                                lang = lang,
                                isMobile = isMobile,
                                onCopy = { viewModel.copyIndividualResult(it) },
                                onRefresh = { viewModel.refreshLocalInfo() },
                                onPingSubnet = { subnetTarget ->
                                    viewModel.onTargetChange(subnetTarget)
                                    viewModel.selectTool("PING")
                                }
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = state.snackbarMessage != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp)
            ) {
                state.snackbarMessage?.let { msg ->
                    PremiumSnackbar(message = msg)
                }
            }
        }
    }
}

@Composable
fun PingResultPanel(
    results: List<com.mrcoder20.portx.domain.PingResult>,
    isLoading: Boolean,
    accent: Color,
    lang: String = "en",
    isMobile: Boolean = false,
    pingMode: String = "ICMP",
    pingPort: Int = 80,
    onPingModeChange: (String) -> Unit = {},
    onPingPortChange: (Int) -> Unit = {},
    onCopyItem: (String) -> Unit
) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    val isRtl = lang == "fa"
    val listState = rememberLazyListState()
    LaunchedEffect(results.size) {
        if (results.isNotEmpty()) listState.animateScrollToItem(results.size - 1)
    }

    // KPI Metrics Calculation
    val totalSent = results.size
    val totalReceived = results.count { it.isSuccess }
    val packetLoss = if (totalSent > 0) ((totalSent - totalReceived) * 100) / totalSent else 0
    val latencies = remember(results) {
        val regex = "(\\d+(?:\\.\\d+)?)\\s*ms".toRegex()
        results.filter { it.isSuccess }.mapNotNull { res ->
            regex.find(res.message)?.groupValues?.get(1)?.toDoubleOrNull()
        }
    }

    val avgLatencyStr = if (latencies.isNotEmpty()) {
        val avg = kotlin.math.round(latencies.average() * 10) / 10.0
        "${avg} ms"
    } else "—"

    val minLatencyStr = if (latencies.isNotEmpty()) {
        val min = kotlin.math.round((latencies.minOrNull() ?: 0.0) * 10) / 10.0
        "${min} ms"
    } else "—"

    val maxLatencyStr = if (latencies.isNotEmpty()) {
        val max = kotlin.math.round((latencies.maxOrNull() ?: 0.0) * 10) / 10.0
        "${max} ms"
    } else "—"

    val jitterStr = if (latencies.size > 1) {
        val meanDiff = latencies.zipWithNext { a, b -> kotlin.math.abs(b - a) }.average()
        val j = kotlin.math.round(meanDiff * 10) / 10.0
        "${j} ms"
    } else "—"

    GlassCard(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(if (isMobile) 10.dp else 14.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        LocalizedStrings.get("tactical_icmp_output", lang),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = if (isMobile) 11.sp else 12.sp),
                        color = accent
                    )
                    CyberBadge(text = if (pingMode == "TCP") "TCP:$pingPort" else "ICMP", color = if (pingMode == "TCP") SecondaryNeon else accent, hasPulseDot = isLoading)
                }

                // Mode Selector (ICMP vs TCP)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        onClick = { onPingModeChange("ICMP") },
                        shape = RoundedCornerShape(6.dp),
                        color = if (pingMode == "ICMP") accent.copy(alpha = 0.2f) else (if (isDark) Color.White.copy(alpha = 0.04f) else Color.White),
                        border = BorderStroke(1.dp, if (pingMode == "ICMP") accent else (if (isDark) GlassBorder.copy(alpha = 0.3f) else BorderLight)),
                        modifier = Modifier.height(if (isMobile) 22.dp else 24.dp).springPress(pressedScale = 0.94f)
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
                            Text("ICMP", color = if (pingMode == "ICMP") accent else (if (isDark) Color.White else Color.Black), fontSize = if (isMobile) 8.5.sp else 9.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Surface(
                        onClick = { onPingModeChange("TCP") },
                        shape = RoundedCornerShape(6.dp),
                        color = if (pingMode == "TCP") SecondaryNeon.copy(alpha = 0.2f) else (if (isDark) Color.White.copy(alpha = 0.04f) else Color.White),
                        border = BorderStroke(1.dp, if (pingMode == "TCP") SecondaryNeon else (if (isDark) GlassBorder.copy(alpha = 0.3f) else BorderLight)),
                        modifier = Modifier.height(if (isMobile) 22.dp else 24.dp).springPress(pressedScale = 0.94f)
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
                            Text("TCP Ping", color = if (pingMode == "TCP") SecondaryNeon else (if (isDark) Color.White else Color.Black), fontSize = if (isMobile) 8.5.sp else 9.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (isLoading) {
                        Text(
                            LocalizedStrings.get("querying", lang),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = accent
                        )
                    }
                }
            }

            if (pingMode == "TCP") {
                Spacer(Modifier.height(4.dp))
                val portChipsScrollState = rememberScrollState()
                val commonPorts = listOf(80 to "HTTP", 443 to "HTTPS", 22 to "SSH", 53 to "DNS", 8080 to "Alt", 3389 to "RDP")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(portChipsScrollState),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${LocalizedStrings.get("ping_port_label", lang)}:",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        color = if (isDark) TextMuted else TextMutedLight
                    )
                    commonPorts.forEach { (p, pLabel) ->
                        val isSelected = pingPort == p
                        Surface(
                            onClick = { onPingPortChange(p) },
                            shape = RoundedCornerShape(5.dp),
                            color = if (isSelected) SecondaryNeon.copy(alpha = 0.2f) else (if (isDark) Color.White.copy(alpha = 0.04f) else Color.White),
                            border = BorderStroke(0.75.dp, if (isSelected) SecondaryNeon else (if (isDark) GlassBorder.copy(alpha = 0.3f) else BorderLight)),
                            modifier = Modifier.height(20.dp).springPress(pressedScale = 0.94f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    "$p",
                                    color = if (isSelected) SecondaryNeon else (if (isDark) Color.White else Color.Black),
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    pLabel,
                                    color = if (isSelected) SecondaryNeon.copy(alpha = 0.8f) else (if (isDark) TextMuted else TextMutedLight),
                                    fontSize = 7.5.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(if (isMobile) 6.dp else 10.dp))

            // KPI Telemetry Grid
            if (results.isNotEmpty()) {
                val lossColor = when {
                    packetLoss == 0 -> TertiaryNeon
                    packetLoss < 20 -> WarningNeon
                    else -> DangerNeon
                }

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val isNarrow = maxWidth < 560.dp || isMobile
                    if (isNarrow) {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            // Row 1 (4 Key Metrics): TX, RX, Loss %, Avg Latency
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                MetricTile(
                                    title = LocalizedStrings.get("packets_tx", lang),
                                    value = "$totalSent",
                                    accent = accent,
                                    isCompact = true,
                                    modifier = Modifier.weight(1f)
                                )
                                MetricTile(
                                    title = LocalizedStrings.get("packets_rx", lang),
                                    value = "$totalReceived",
                                    accent = SecondaryNeon,
                                    isCompact = true,
                                    modifier = Modifier.weight(1f)
                                )
                                MetricTile(
                                    title = LocalizedStrings.get("packet_loss", lang),
                                    value = "$packetLoss%",
                                    accent = lossColor,
                                    isCompact = true,
                                    modifier = Modifier.weight(1f)
                                )
                                MetricTile(
                                    title = LocalizedStrings.get("avg_latency", lang),
                                    value = avgLatencyStr,
                                    accent = Color(0xFF00E5FF),
                                    isCompact = true,
                                    modifier = Modifier.weight(1.15f)
                                )
                            }
                            // Row 2 (3 Secondary Metrics): Min, Max, Jitter
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                MetricTile(
                                    title = LocalizedStrings.get("min_latency", lang),
                                    value = minLatencyStr,
                                    accent = TertiaryNeon,
                                    isCompact = true,
                                    modifier = Modifier.weight(1f)
                                )
                                MetricTile(
                                    title = LocalizedStrings.get("max_latency", lang),
                                    value = maxLatencyStr,
                                    accent = WarningNeon,
                                    isCompact = true,
                                    modifier = Modifier.weight(1f)
                                )
                                MetricTile(
                                    title = LocalizedStrings.get("jitter", lang),
                                    value = jitterStr,
                                    accent = SecondaryNeon,
                                    isCompact = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MetricTile(
                                    title = LocalizedStrings.get("packets_tx", lang),
                                    value = "$totalSent",
                                    accent = accent,
                                    modifier = Modifier.weight(1f)
                                )
                                MetricTile(
                                    title = LocalizedStrings.get("packets_rx", lang),
                                    value = "$totalReceived",
                                    accent = SecondaryNeon,
                                    modifier = Modifier.weight(1f)
                                )
                                MetricTile(
                                    title = LocalizedStrings.get("packet_loss", lang),
                                    value = "$packetLoss%",
                                    accent = lossColor,
                                    modifier = Modifier.weight(1f)
                                )
                                MetricTile(
                                    title = LocalizedStrings.get("avg_latency", lang),
                                    value = avgLatencyStr,
                                    accent = Color(0xFF00E5FF),
                                    modifier = Modifier.weight(1.1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MetricTile(
                                    title = LocalizedStrings.get("min_latency", lang),
                                    value = minLatencyStr,
                                    accent = TertiaryNeon,
                                    modifier = Modifier.weight(1f)
                                )
                                MetricTile(
                                    title = LocalizedStrings.get("max_latency", lang),
                                    value = maxLatencyStr,
                                    accent = WarningNeon,
                                    modifier = Modifier.weight(1f)
                                )
                                MetricTile(
                                    title = LocalizedStrings.get("jitter", lang),
                                    value = jitterStr,
                                    accent = SecondaryNeon,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Interactive Latency Sparkline Bar Visualization
                if (latencies.isNotEmpty()) {
                    Spacer(Modifier.height(if (isMobile) 6.dp else 10.dp))
                    PingLatencySparkline(
                        latencies = latencies,
                        accent = accent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isMobile) 26.dp else 40.dp)
                    )
                }

                Spacer(Modifier.height(if (isMobile) 6.dp else 10.dp))
            }

            // Terminal Log Box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 100.dp)
                    .background(
                        if (isDark) Color.Black.copy(alpha = 0.35f) else SurfaceInsetLight,
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        1.dp,
                        if (isDark) GlassBorder.copy(alpha = 0.3f) else BorderLight,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(8.dp)
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .touchDragScroll(listState, isVertical = true)
                            .absolutePadding(
                                right = if (isRtl) 10.dp else 0.dp,
                                left = if (!isRtl) 10.dp else 0.dp
                            ),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(results, key = { index, res -> "${index}_${res.sequence}" }) { index, res ->
                            val statusColor = if (res.isSuccess) {
                                val match = "(\\d+(?:\\.\\d+)?)\\s*ms".toRegex().find(res.message)
                                val ms = match?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
                                when {
                                    ms < 50.0 -> TertiaryNeon
                                    ms < 150.0 -> Color(0xFF00E5FF)
                                    else -> WarningNeon
                                }
                            } else DangerNeon

                            Surface(
                                onClick = { onCopyItem(res.message) },
                                shape = RoundedCornerShape(6.dp),
                                color = if (isDark) Color.White.copy(alpha = 0.025f) else Color.White,
                                border = BorderStroke(0.5.dp, if (isDark) GlassBorder.copy(alpha = 0.2f) else BorderLight),
                                modifier = Modifier.fillMaxWidth().springPress(pressedScale = 0.98f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Sequence badge
                                    Text(
                                        "[#${res.sequence}]",
                                        color = accent.copy(alpha = 0.85f),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    // Status Dot
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(statusColor)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        res.message,
                                        color = if (res.isSuccess) (if (isDark) Color.White else Color.Black) else DangerNeon,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.5.sp,
                                        textAlign = TextAlign.Start,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = LocalizedStrings.get("copy", lang),
                                        tint = if (isDark) TextMuted.copy(alpha = 0.5f) else TextMutedLight.copy(alpha = 0.5f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                        if (results.isEmpty() && !isLoading) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        LocalizedStrings.get("engine_ready", lang),
                                        color = if (isDark) TextMuted else TextMutedLight,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
                PortXVerticalScrollbar(
                    listState = listState,
                    modifier = Modifier.align(if (isRtl) AbsoluteAlignment.CenterRight else AbsoluteAlignment.CenterLeft).fillMaxHeight()
                )
            }
        }
    }
}

@Composable
fun PingLatencySparkline(
    latencies: List<Double>,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    val recentLatencies = remember(latencies) { latencies.takeLast(24) }
    val maxVal = remember(recentLatencies) { (recentLatencies.maxOrNull() ?: 100.0).coerceAtLeast(20.0) }

    Canvas(
        modifier = modifier
            .background(
                if (isDark) Color.Black.copy(alpha = 0.25f) else SurfaceInsetLight,
                RoundedCornerShape(8.dp)
            )
            .border(
                1.dp,
                if (isDark) GlassBorder.copy(alpha = 0.2f) else BorderLight,
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        val count = recentLatencies.size
        if (count == 0) return@Canvas

        val barSpacing = 4.dp.toPx()
        val totalSpacing = barSpacing * (count - 1).coerceAtLeast(0)
        val calculatedBarWidth = ((size.width - totalSpacing) / count).coerceIn(4.dp.toPx(), 24.dp.toPx())
        val actualTotalWidth = count * calculatedBarWidth + totalSpacing
        val startX = if (actualTotalWidth < size.width) (size.width - actualTotalWidth) / 2f else 0f

        recentLatencies.forEachIndexed { i, lat ->
            val barHeight = ((lat / maxVal) * size.height).toFloat().coerceIn(3.dp.toPx(), size.height)
            val x = startX + i * (calculatedBarWidth + barSpacing)
            val y = size.height - barHeight

            val barColor = when {
                lat < 40.0 -> TertiaryNeon
                lat < 100.0 -> Color(0xFF00E5FF)
                lat < 180.0 -> WarningNeon
                else -> DangerNeon
            }

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(barColor, barColor.copy(alpha = 0.5f)),
                    startY = y,
                    endY = size.height
                ),
                topLeft = Offset(x, y),
                size = Size(calculatedBarWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx())
            )
        }
    }
}

@Composable
fun DnsResultPanel(
    dnsResult: com.mrcoder20.portx.domain.DnsResolutionResult?,
    isLoading: Boolean,
    accent: Color,
    lang: String = "en",
    isMobile: Boolean = false,
    filterType: String = "ALL",
    viewMode: String = "CARDS",
    onFilterChange: (String) -> Unit = {},
    onViewModeChange: (String) -> Unit = {},
    onCopyItem: (String) -> Unit,
    onSelectForPing: (String) -> Unit
) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    val isRtl = lang == "fa"
    val dnsListState = rememberLazyListState()
    val rawScrollState = rememberScrollState()

    val records = dnsResult?.records ?: emptyList()
    val filteredRecords = remember(records, filterType) {
        when (filterType) {
            "A_AAAA" -> records.filter { it.type == "A" || it.type == "AAAA" }
            "MX" -> records.filter { it.type == "MX" }
            "TXT" -> records.filter { it.type == "TXT" }
            "NS" -> records.filter { it.type == "NS" }
            "CNAME" -> records.filter { it.type == "CNAME" }
            "SOA" -> records.filter { it.type == "SOA" }
            "CAA" -> records.filter { it.type == "CAA" }
            "PTR" -> records.filter { it.type == "PTR" }
            else -> records
        }
    }

    GlassCard(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(if (isMobile) 10.dp else 14.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        LocalizedStrings.get("dns_resolution_records", lang),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = if (isMobile) 11.sp else 12.sp),
                        color = accent
                    )
                    CyberBadge(text = "DNS", color = accent, hasPulseDot = isLoading)
                }

                // View Switcher (Cards vs Dig) + Copy All
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        onClick = { onViewModeChange(if (viewMode == "CARDS") "RAW" else "CARDS") },
                        shape = RoundedCornerShape(6.dp),
                        color = (if (viewMode == "RAW") SecondaryNeon else accent).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, (if (viewMode == "RAW") SecondaryNeon else accent).copy(alpha = 0.4f)),
                        modifier = Modifier.height(if (isMobile) 24.dp else 26.dp).springPress(pressedScale = 0.94f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                if (viewMode == "CARDS") Icons.Default.Terminal else Icons.Default.ViewAgenda,
                                null,
                                tint = if (viewMode == "RAW") SecondaryNeon else accent,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                if (viewMode == "CARDS") LocalizedStrings.get("dns_view_raw", lang) else LocalizedStrings.get("dns_view_cards", lang),
                                color = if (viewMode == "RAW") SecondaryNeon else accent,
                                fontSize = if (isMobile) 8.5.sp else 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (records.isNotEmpty()) {
                        IconButton(
                            onClick = { onCopyItem(dnsResult?.rawDigOutput ?: "") },
                            modifier = Modifier.size(if (isMobile) 24.dp else 28.dp).springPress()
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = LocalizedStrings.get("copy", lang),
                                tint = accent,
                                modifier = Modifier.size(if (isMobile) 13.dp else 15.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(if (isMobile) 6.dp else 8.dp))

            // Diagnostic Meta Bar: Server, Response Time, Record Count, DNSSEC
            if (dnsResult != null && records.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CyberBadge(
                        text = "${records.size} ${LocalizedStrings.get("dns_records_count", lang)}",
                        color = SecondaryNeon,
                        fontSize = if (isMobile) 8f else 9f
                    )
                    CyberBadge(
                        text = dnsResult.serverUsed,
                        color = Color(0xFF00E5FF),
                        fontSize = if (isMobile) 8f else 9f
                    )
                    val latencyColor = when {
                        dnsResult.responseTimeMs < 60L -> TertiaryNeon
                        dnsResult.responseTimeMs < 180L -> Color(0xFF00E5FF)
                        else -> WarningNeon
                    }
                    CyberBadge(
                        text = "${dnsResult.responseTimeMs} ms",
                        color = latencyColor,
                        fontSize = if (isMobile) 8f else 9f
                    )
                    if (dnsResult.hasDnssec) {
                        CyberBadge(
                            text = LocalizedStrings.get("dns_dnssec_valid", lang),
                            color = TertiaryNeon,
                            fontSize = if (isMobile) 8f else 9f
                        )
                    }
                }
                Spacer(Modifier.height(if (isMobile) 6.dp else 8.dp))
            }

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = accent
                )
                Spacer(Modifier.height(if (isMobile) 6.dp else 8.dp))
            }

            // Filter Chips Bar (in Cards Mode)
            if (viewMode == "CARDS" && records.isNotEmpty()) {
                val filterScrollState = rememberScrollState()
                val filterOptions = listOf(
                    "ALL" to LocalizedStrings.get("dns_filter_all", lang),
                    "A_AAAA" to LocalizedStrings.get("dns_filter_ip", lang),
                    "MX" to LocalizedStrings.get("dns_filter_mx", lang),
                    "TXT" to LocalizedStrings.get("dns_filter_txt", lang),
                    "NS" to LocalizedStrings.get("dns_filter_ns", lang),
                    "CNAME" to LocalizedStrings.get("dns_filter_cname", lang),
                    "SOA" to LocalizedStrings.get("dns_filter_soa", lang),
                    "CAA" to LocalizedStrings.get("dns_filter_caa", lang),
                    "PTR" to LocalizedStrings.get("dns_filter_ptr", lang)
                ).filter { (key, _) ->
                    when (key) {
                        "ALL" -> true
                        "A_AAAA" -> records.any { it.type == "A" || it.type == "AAAA" }
                        else -> records.any { it.type == key }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isMobile) 24.dp else 26.dp)
                        .horizontalFadingEdges(filterScrollState, fadeWidth = 10.dp, isRtl = isRtl)
                        .touchDragScroll(filterScrollState, isVertical = false, isRtl = isRtl)
                        .horizontalScroll(filterScrollState),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    filterOptions.forEach { (key, label) ->
                        val isSelected = filterType == key
                        val count = when (key) {
                            "ALL" -> records.size
                            "A_AAAA" -> records.count { it.type == "A" || it.type == "AAAA" }
                            else -> records.count { it.type == key }
                        }
                        Surface(
                            onClick = { onFilterChange(key) },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) accent.copy(alpha = 0.2f) else (if (isDark) Color.White.copy(alpha = 0.04f) else Color.White),
                            border = BorderStroke(1.dp, if (isSelected) accent else (if (isDark) GlassBorder.copy(alpha = 0.3f) else BorderLight)),
                            modifier = Modifier.height(if (isMobile) 22.dp else 24.dp).springPress(pressedScale = 0.94f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    label,
                                    color = if (isSelected) accent else (if (isDark) Color.White else Color.Black),
                                    fontSize = if (isMobile) 8.5.sp else 9.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                Text(
                                    "($count)",
                                    color = if (isSelected) accent else (if (isDark) TextMuted else TextMutedLight),
                                    fontSize = if (isMobile) 7.5.sp else 8.5.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(if (isMobile) 6.dp else 8.dp))
            }

            // Results Container
            if (viewMode == "RAW") {
                // Terminal / Raw Dig View
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(
                            if (isDark) Color.Black.copy(alpha = 0.35f) else SurfaceInsetLight,
                            RoundedCornerShape(10.dp)
                        )
                        .border(
                            1.dp,
                            if (isDark) GlassBorder.copy(alpha = 0.3f) else BorderLight,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(if (isMobile) 8.dp else 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .touchDragScroll(rawScrollState, isVertical = true)
                            .verticalScroll(rawScrollState)
                    ) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            SelectionContainer {
                                Text(
                                    text = dnsResult?.rawDigOutput ?: LocalizedStrings.get("awaiting_dns", lang),
                                    color = if (dnsResult != null) (if (isDark) Color.White else Color.Black) else (if (isDark) TextMuted else TextMutedLight),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = if (isMobile) 10.5.sp else 11.5.sp,
                                    lineHeight = if (isMobile) 15.sp else 17.sp,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    PortXScrollStateVerticalScrollbar(
                        scrollState = rawScrollState,
                        modifier = Modifier.align(if (isRtl) AbsoluteAlignment.CenterRight else AbsoluteAlignment.CenterLeft).fillMaxHeight()
                    )
                }
            } else {
                // Visual Cards View
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(
                            if (isDark) Color.Black.copy(alpha = 0.25f) else SurfaceInsetLight,
                            RoundedCornerShape(10.dp)
                        )
                        .border(
                            1.dp,
                            if (isDark) GlassBorder.copy(alpha = 0.3f) else BorderLight,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(6.dp)
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        LazyColumn(
                            state = dnsListState,
                            modifier = Modifier
                                .fillMaxSize()
                                .touchDragScroll(dnsListState, isVertical = true)
                                .absolutePadding(
                                    right = if (isRtl) 10.dp else 0.dp,
                                    left = if (!isRtl) 10.dp else 0.dp
                                ),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            itemsIndexed(filteredRecords, key = { index, rec -> "${index}_${rec.type}_${rec.value}" }) { _, rec ->
                                val badgeColor = when (rec.type) {
                                    "A" -> SecondaryNeon
                                    "AAAA" -> Color(0xFF00E5FF)
                                    "MX" -> Color(0xFFFFAB00)
                                    "TXT" -> Color(0xFF40C4FF)
                                    "NS" -> Color(0xFFB388FF)
                                    "CNAME" -> Color(0xFFFF4081)
                                    "SOA" -> Color(0xFFFF5252)
                                    "CAA" -> Color(0xFFFF6E40)
                                    "PTR" -> Color(0xFF76FF03)
                                    else -> accent
                                }

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(
                                            if (isDark) Modifier else Modifier.shadow(
                                                elevation = 1.dp,
                                                shape = RoundedCornerShape(8.dp),
                                                spotColor = Color(0x100F172A),
                                                ambientColor = Color(0x0A0F172A)
                                            )
                                        )
                                        .clickable { onCopyItem(rec.value) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isDark) Color.White.copy(alpha = 0.035f) else Color.White,
                                    border = BorderStroke(0.75.dp, if (isDark) GlassBorder else BorderLight)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = if (isMobile) 8.dp else 10.dp, vertical = if (isMobile) 6.dp else 8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            CyberBadge(text = rec.type, color = badgeColor, fontSize = if (isMobile) 8f else 9f)

                                            rec.priority?.let { pref ->
                                                Spacer(Modifier.width(4.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFFFFAB00).copy(alpha = 0.15f),
                                                    border = BorderStroke(0.5.dp, Color(0xFFFFAB00).copy(alpha = 0.4f)),
                                                    modifier = Modifier.height(16.dp)
                                                ) {
                                                    Text(
                                                        "Pref: $pref",
                                                        color = Color(0xFFFFAB00),
                                                        fontSize = 7.5.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 4.dp)
                                                    )
                                                }
                                            }

                                            Spacer(Modifier.width(6.dp))

                                            Text(
                                                rec.name,
                                                color = if (isDark) TextMuted else TextMutedLight,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = if (isMobile) 9.5.sp else 10.5.sp,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            // TTL Pill
                                            Text(
                                                "TTL: ${rec.ttl}s",
                                                color = if (isDark) TextMuted.copy(alpha = 0.7f) else TextMutedLight.copy(alpha = 0.7f),
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 8.sp,
                                                modifier = Modifier.padding(horizontal = 4.dp)
                                            )

                                            // Quick Action: Ping for A/AAAA
                                            if (rec.type == "A" || rec.type == "AAAA") {
                                                Surface(
                                                    onClick = { onSelectForPing(rec.value) },
                                                    shape = RoundedCornerShape(5.dp),
                                                    color = accent.copy(alpha = 0.12f),
                                                    border = BorderStroke(0.5.dp, accent.copy(alpha = 0.4f)),
                                                    modifier = Modifier.padding(end = 4.dp).springPress(pressedScale = 0.92f)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                    ) {
                                                        Icon(Icons.Default.NetworkPing, null, tint = accent, modifier = Modifier.size(10.dp))
                                                        Text(LocalizedStrings.get("ping_action", lang), color = accent, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }

                                            Icon(
                                                Icons.Default.ContentCopy,
                                                contentDescription = LocalizedStrings.get("copy", lang),
                                                tint = if (isDark) TextMuted.copy(alpha = 0.5f) else TextMutedLight.copy(alpha = 0.5f),
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }

                                        Spacer(Modifier.height(4.dp))

                                        // Value Content
                                        SelectionContainer {
                                            Text(
                                                rec.value,
                                                color = if (isDark) Color.White else Color.Black,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = if (isMobile) 11.sp else 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                textAlign = TextAlign.Start,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }

                            if (filteredRecords.isEmpty() && !isLoading) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            LocalizedStrings.get("awaiting_dns", lang),
                                            color = if (isDark) TextMuted else TextMutedLight,
                                            fontSize = 11.5.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                    PortXVerticalScrollbar(
                        listState = dnsListState,
                        modifier = Modifier.align(if (isRtl) AbsoluteAlignment.CenterRight else AbsoluteAlignment.CenterLeft).fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
fun WhoisResultPanel(
    result: String?,
    isLoading: Boolean,
    lang: String = "en",
    isMobile: Boolean = false,
    onCopy: (String) -> Unit = {}
) {
    val accent = LocalAccentColor.current
    val isDark = LocalAppSettings.current.theme == "DARK"
    val isRtl = lang == "fa"

    val whoisSummary = remember(result, lang) {
        if (result == null) null
        else {
            val registrarMatch = "(?i)Registrar:\\s*([^\\r\\n]+)".toRegex().find(result)?.groupValues?.get(1)?.trim()
            val rawCreated = "(?i)(?:Creation Date|created):\\s*([^\\r\\n]+)".toRegex().find(result)?.groupValues?.get(1)?.trim()
            val rawExpiry = "(?i)(?:Registry Expiry Date|paid-till|Expiration Date):\\s*([^\\r\\n]+)".toRegex().find(result)?.groupValues?.get(1)?.trim()
            val createdFormatted = DateFormatter.formatWhoisDate(rawCreated, lang)
            val expiryFormatted = DateFormatter.formatWhoisDate(rawExpiry, lang)
            Triple(registrarMatch, createdFormatted, expiryFormatted)
        }
    }

    GlassCard(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(if (isMobile) 10.dp else 14.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        LocalizedStrings.get("whois_authority_data", lang),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = if (isMobile) 11.sp else 12.sp),
                        color = accent
                    )
                    CyberBadge(text = "WHOIS", color = accent, hasPulseDot = isLoading)
                }
                if (!result.isNullOrBlank()) {
                    IconButton(onClick = { onCopy(result) }, modifier = Modifier.size(if (isMobile) 32.dp else 36.dp)) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = LocalizedStrings.get("copy_whois", lang),
                            tint = accent,
                            modifier = Modifier.size(if (isMobile) 15.dp else 18.dp)
                        )
                    }
                }
            }

            // Structured WHOIS Identity Dossier Banner
            if (whoisSummary != null && (whoisSummary.first != null || whoisSummary.second != "—" || whoisSummary.third != "—")) {
                Spacer(Modifier.height(if (isMobile) 6.dp else 10.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isDark) Modifier else Modifier.shadow(
                                elevation = 1.5.dp,
                                shape = RoundedCornerShape(if (isMobile) 8.dp else 12.dp),
                                spotColor = Color(0x140F172A),
                                ambientColor = Color(0x0C0F172A)
                            )
                        ),
                    shape = RoundedCornerShape(if (isMobile) 8.dp else 12.dp),
                    color = if (isDark) Color.White.copy(alpha = 0.04f) else Color.White,
                    border = BorderStroke(1.dp, if (isDark) accent.copy(alpha = 0.25f) else BorderLight)
                ) {
                    Column(modifier = Modifier.padding(if (isMobile) 8.dp else 12.dp), verticalArrangement = Arrangement.spacedBy(if (isMobile) 4.dp else 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                LocalizedStrings.get("whois_summary", lang),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = if (isMobile) 10.sp else 11.sp),
                                color = accent
                            )
                            CyberBadge(text = LocalizedStrings.get("verified_registrar", lang), color = TertiaryNeon, fontSize = if (isMobile) 8f else 9f)
                        }

                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val isNarrow = maxWidth < 480.dp || isMobile
                            if (isNarrow) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    whoisSummary.first?.let { reg ->
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Text(
                                                LocalizedStrings.get("registrar", lang),
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                                color = if (isDark) TextMuted else TextMutedLight
                                            )
                                            Text(
                                                reg,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp),
                                                color = if (isDark) Color.White else Color.Black,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    if (whoisSummary.second != "—" || whoisSummary.third != "—") {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (whoisSummary.second != "—") {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        LocalizedStrings.get("created_date", lang),
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                                        color = if (isDark) TextMuted else TextMutedLight
                                                    )
                                                    Text(
                                                        whoisSummary.second,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                                        color = if (isDark) Color.White else Color.Black,
                                                        maxLines = 1,
                                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                            if (whoisSummary.third != "—") {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        LocalizedStrings.get("expiry_date", lang),
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                                        color = if (isDark) TextMuted else TextMutedLight
                                                    )
                                                    Text(
                                                        whoisSummary.third,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                                        color = WarningNeon,
                                                        maxLines = 1,
                                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    whoisSummary.first?.let { reg ->
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                LocalizedStrings.get("registrar", lang),
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                color = if (isDark) TextMuted else TextMutedLight
                                            )
                                            Text(
                                                reg,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                                color = if (isDark) Color.White else Color.Black,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    if (whoisSummary.second != "—") {
                                        Column(modifier = Modifier.weight(1.3f)) {
                                            Text(
                                                LocalizedStrings.get("created_date", lang),
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                color = if (isDark) TextMuted else TextMutedLight
                                            )
                                            Text(
                                                whoisSummary.second,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                                color = if (isDark) Color.White else Color.Black,
                                                maxLines = 2,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    if (whoisSummary.third != "—") {
                                        Column(modifier = Modifier.weight(1.3f)) {
                                            Text(
                                                LocalizedStrings.get("expiry_date", lang),
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                color = if (isDark) TextMuted else TextMutedLight
                                            )
                                            Text(
                                                whoisSummary.third,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                                color = WarningNeon,
                                                maxLines = 2,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(if (isMobile) 6.dp else 10.dp))
            if (isLoading && result == null) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp), color = accent)
                Spacer(Modifier.height(8.dp))
            }

            val whoisScrollState = rememberScrollState()
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 100.dp)
                    .background(
                        if (isDark) Color.Black.copy(alpha = 0.25f) else SurfaceInsetLight,
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        1.dp,
                        if (isDark) GlassBorder.copy(alpha = 0.3f) else BorderLight,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(if (isMobile) 8.dp else 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .touchDragScroll(whoisScrollState, isVertical = true)
                        .verticalScroll(whoisScrollState)
                        .absolutePadding(
                            right = if (isRtl) 10.dp else 0.dp,
                            left = if (!isRtl) 10.dp else 0.dp
                        )
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        SelectionContainer {
                            Text(
                                result ?: LocalizedStrings.get("ready_whois", lang),
                                color = if (result != null) (if (isDark) Color.White else Color.Black) else (if (isDark) TextMuted else TextMutedLight),
                                fontFamily = FontFamily.Monospace,
                                fontSize = if (isMobile) 10.5.sp else 11.sp,
                                lineHeight = if (isMobile) 15.sp else 17.sp,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                PortXScrollStateVerticalScrollbar(
                    scrollState = whoisScrollState,
                    modifier = Modifier.align(if (isRtl) AbsoluteAlignment.CenterRight else AbsoluteAlignment.CenterLeft).fillMaxHeight()
                )
            }
        }
    }
}

@Composable
fun LocalInfoPanel(
    info: com.mrcoder20.portx.domain.LocalIpInfo?,
    subnetInfo: com.mrcoder20.portx.domain.SubnetInfo? = null,
    publicIp: String?,
    isLoading: Boolean,
    accent: Color,
    lang: String,
    isMobile: Boolean = false,
    onCopy: (String) -> Unit,
    onRefresh: () -> Unit,
    onPingSubnet: (String) -> Unit
) {
    val isTouchEmulated = LocalTouchEmulation.current
    val scrollState = rememberScrollState()
    val isRtl = lang == "fa"
    val isDark = LocalAppSettings.current.theme == "DARK"

    // Derive subnet suggestion (e.g. 192.168.1.1 or 192.168.1.0/24)
    val subnetHint = remember(info?.ipAddress, subnetInfo) {
        if (subnetInfo != null) {
            subnetInfo.hostRangeStart
        } else {
            val ip = info?.ipAddress ?: ""
            if (ip.matches(Regex("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$"))) {
                val parts = ip.split(".")
                "${parts[0]}.${parts[1]}.${parts[2]}.1"
            } else "192.168.1.1"
        }
    }

    GlassCard(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(if (isMobile) 10.dp else 14.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (!isTouchEmulated) {
                            Modifier.absolutePadding(
                                right = if (isRtl) 10.dp else 0.dp,
                                left = if (!isRtl) 10.dp else 0.dp
                            )
                        } else Modifier
                    )
                    .touchDragScroll(scrollState, isVertical = true)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            LocalizedStrings.get("device_environment", lang),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = accent
                        )
                        CyberBadge(text = LocalizedStrings.get("badge_local", lang), color = SecondaryNeon)
                    }

                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = accent, strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = onRefresh, modifier = Modifier.size(40.dp).springPress()) {
                            Icon(Icons.Default.Refresh, LocalizedStrings.get("refresh", lang), tint = accent, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                info?.let {
                    InfoItem(
                        label = LocalizedStrings.get("internal_ip", lang),
                        value = it.ipAddress,
                        icon = Icons.Default.Lan,
                        color = accent,
                        lang = lang,
                        onClick = { onCopy(it.ipAddress) }
                    )

                    InfoItem(
                        label = LocalizedStrings.get("interface", lang),
                        value = it.interfaceName,
                        icon = Icons.Default.SettingsInputComponent,
                        color = SecondaryNeon,
                        lang = lang,
                        onClick = { onCopy(it.interfaceName) }
                    )

                    InfoItem(
                        label = LocalizedStrings.get("connection", lang),
                        value = if (it.isWifi) LocalizedStrings.get("wifi", lang) else LocalizedStrings.get("wired", lang),
                        icon = if (it.isWifi) Icons.Default.Wifi else Icons.Default.SettingsEthernet,
                        color = TertiaryNeon,
                        lang = lang,
                        onClick = null
                    )
                }

                publicIp?.let {
                    InfoItem(
                        label = LocalizedStrings.get("public_ip", lang),
                        value = it,
                        icon = Icons.Default.Public,
                        color = Color(0xFF00E5FF),
                        lang = lang,
                        onClick = { onCopy(it) }
                    )
                }

                // Subnet & CIDR Diagnostic Card
                subnetInfo?.let { sub ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isDark) Modifier else Modifier.shadow(
                                    elevation = 1.5.dp,
                                    shape = RoundedCornerShape(12.dp),
                                    spotColor = Color(0x140F172A),
                                    ambientColor = Color(0x0C0F172A)
                                )
                            ),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color.White.copy(alpha = 0.04f) else Color.White,
                        border = BorderStroke(1.dp, if (isDark) SecondaryNeon.copy(alpha = 0.3f) else BorderLight)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(if (isMobile) 10.dp else 14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Lan, null, tint = SecondaryNeon, modifier = Modifier.size(16.dp))
                                    Text(
                                        LocalizedStrings.get("subnet_calculator", lang),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = if (isMobile) 10.5.sp else 11.5.sp),
                                        color = SecondaryNeon
                                    )
                                }
                                CyberBadge(text = "/${sub.cidr}", color = SecondaryNeon, fontSize = 9f)
                            }

                            // Subnet Details Grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(LocalizedStrings.get("subnet_cidr", lang), style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp), color = if (isDark) TextMuted else TextMutedLight)
                                    Text("${sub.networkAddress}/${sub.cidr}", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.5.sp), color = if (isDark) Color.White else Color.Black)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(LocalizedStrings.get("subnet_broadcast", lang), style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp), color = if (isDark) TextMuted else TextMutedLight)
                                    Text(sub.broadcastAddress, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.5.sp), color = if (isDark) Color.White else Color.Black)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1.4f)) {
                                    Text(LocalizedStrings.get("subnet_range", lang), style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp), color = if (isDark) TextMuted else TextMutedLight)
                                    Text("${sub.hostRangeStart} – ${sub.hostRangeEnd}", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp), color = if (isDark) Color.White else Color.Black)
                                }
                                Column(modifier = Modifier.weight(0.8f)) {
                                    Text(LocalizedStrings.get("subnet_total_hosts", lang), style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp), color = if (isDark) TextMuted else TextMutedLight)
                                    Text("${sub.usableHostsCount} Hosts", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.5.sp), color = TertiaryNeon)
                                }
                            }
                        }
                    }
                }

                // Subnet Quick Action Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isDark) Modifier else Modifier.shadow(
                                elevation = 1.5.dp,
                                shape = RoundedCornerShape(14.dp),
                                spotColor = Color(0x140F172A),
                                ambientColor = Color(0x0C0F172A)
                            )
                        ),
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) Color.White.copy(alpha = 0.03f) else Color.White,
                    border = BorderStroke(1.dp, if (isDark) accent.copy(alpha = 0.25f) else BorderLight)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                LocalizedStrings.get("scan_subnet", lang),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isDark) Color.White else Color.Black
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "${LocalizedStrings.get("gateway_label", lang)}: $subnetHint",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = if (isDark) TextMuted else TextMutedLight
                            )
                        }

                        Button(
                            onClick = { onPingSubnet(subnetHint) },
                            colors = ButtonDefaults.buttonColors(containerColor = accent),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.springPress(pressedScale = 0.94f),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.NetworkPing, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(LocalizedStrings.get("ping_gateway", lang), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }

                if (info == null && publicIp == null && !isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                        Button(
                            onClick = onRefresh,
                            colors = ButtonDefaults.buttonColors(containerColor = accent.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, accent),
                            modifier = Modifier.springPress(pressedScale = 0.94f)
                        ) {
                            Icon(Icons.Default.Refresh, null, tint = if (isDark) Color.White else Color.Black)
                            Spacer(Modifier.width(8.dp))
                            Text(LocalizedStrings.get("refresh", lang), color = if (isDark) Color.White else Color.Black)
                        }
                    }
                }
            }

            if (!isTouchEmulated) {
                PortXScrollStateVerticalScrollbar(
                    scrollState = scrollState,
                    modifier = Modifier
                        .align(if (isRtl) AbsoluteAlignment.CenterRight else AbsoluteAlignment.CenterLeft)
                        .fillMaxHeight()
                )
            }
        }
    }
}

@Composable
fun InfoItem(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    lang: String = "en",
    onClick: (() -> Unit)? = null
) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    val clickModifier = if (onClick != null) {
        Modifier
            .clickable { onClick() }
            .springPress(pressedScale = 0.98f)
            .pointerHoverIcon(PointerIcon.Hand)
    } else Modifier

    val shadowModifier = if (isDark) {
        Modifier
    } else {
        Modifier.shadow(
            elevation = if (onClick != null) 2.dp else 1.dp,
            shape = RoundedCornerShape(12.dp),
            ambientColor = Color(0x100F172A),
            spotColor = Color(0x140F172A)
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(shadowModifier)
            .then(clickModifier),
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) Color.White.copy(alpha = 0.035f) else Color.White,
        border = BorderStroke(1.dp, if (isDark) GlassBorder else BorderLight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.12f), CircleShape)
                    .border(1.dp, color.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                    color = if (isDark) TextMuted else TextMutedLight,
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.height(2.dp))
                val isPureIp = value.all { it.isDigit() || it == '.' || it == ':' }
                val valueFontSize = when {
                    value.length > 25 -> 11.5.sp
                    value.length > 18 -> 13.sp
                    else -> 14.sp
                }
                Text(
                    value,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = if (isPureIp) FontFamily.Monospace else FontFamily.Default,
                        fontSize = valueFontSize,
                        lineHeight = if (value.length > 25) 16.sp else 18.sp,
                        textDirection = if (isPureIp) TextDirection.Ltr else TextDirection.ContentOrLtr
                    ),
                    color = if (isDark) Color.White else Color.Black
                )
            }
            if (onClick != null) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = LocalizedStrings.get("copy", lang),
                    tint = if (isDark) TextMuted.copy(alpha = 0.6f) else TextMutedLight.copy(alpha = 0.6f),
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}
