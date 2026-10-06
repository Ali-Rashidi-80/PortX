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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrcoder20.portx.domain.DateFormatter
import com.mrcoder20.portx.domain.LocalizedStrings
import com.mrcoder20.portx.presentation.ui.components.PortXScrollStateVerticalScrollbar
import com.mrcoder20.portx.presentation.ui.components.PortXVerticalScrollbar
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

    val toolList = remember {
        listOf(
            "LOCAL" to Icons.Default.Lan,
            "PING" to Icons.Default.NetworkPing,
            "DNS" to Icons.Default.Dns,
            "WHOIS" to Icons.Default.Public
        )
    }
    val activeIndex = toolList.indexOfFirst { it.first == state.activeTool }.coerceAtLeast(0)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            // 1. TOOL SELECTOR (CYBER SEGMENTED CONTROL)
            CyberSegmentedControl(
                items = toolList,
                selectedIndex = activeIndex,
                onIndexSelected = { index ->
                    viewModel.selectTool(toolList[index].first)
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = if (isDark) GlassBackground else GlassLight,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, if (isDark) GlassBorder else GlassBorderLight)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
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
                                    .padding(start = 8.dp, end = 4.dp)
                                    .size(22.dp)
                            )

                            OutlinedTextField(
                                value = state.target,
                                onValueChange = { viewModel.onTargetChange(it) },
                                placeholder = {
                                    Text(
                                        when (state.activeTool) {
                                            "DNS" -> LocalizedStrings.get("placeholder_dns", lang)
                                            "WHOIS" -> LocalizedStrings.get("placeholder_whois", lang)
                                            else -> LocalizedStrings.get("target", lang)
                                        },
                                        color = if (isDark) TextMuted else TextMutedLight,
                                        fontSize = 13.sp
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
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    textDirection = TextDirection.Ltr,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp
                                )
                            )

                            if (state.target.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.onTargetChange("") },
                                    modifier = Modifier.size(36.dp).springPress()
                                ) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = if (isDark) TextMuted else TextMutedLight,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.padding(end = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { viewModel.copyResultsToClipboard() },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .springPress()
                                        .background(
                                            if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.05f),
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        tint = if (isDark) Color.White else Color.Black,
                                        modifier = Modifier.size(18.dp)
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
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .height(44.dp)
                                        .width(84.dp)
                                        .springPress(pressedScale = 0.94f),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(
                                        if (state.isLoading) Icons.Default.Stop else Icons.Default.FlashOn,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        if (state.isLoading) LocalizedStrings.get("stop", lang) else LocalizedStrings.get("go", lang),
                                        fontWeight = FontWeight.Black,
                                        color = Color.Black,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    // Quick Target Preset Suggestions
                    val presets = remember(state.activeTool) {
                        when (state.activeTool) {
                            "PING" -> listOf("8.8.8.8", "1.1.1.1", "127.0.0.1", "scanme.nmap.org")
                            "DNS" -> listOf("google.com", "cloudflare.com", "github.com", "wikipedia.org")
                            "WHOIS" -> listOf("google.com", "github.com", "kernel.org", "iana.org")
                            else -> emptyList()
                        }
                    }
                    if (presets.isNotEmpty()) {
                        val isRtl = lang == "fa" || lang == "ar"
                        val quickScrollState = rememberScrollState()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .touchDragScroll(quickScrollState, isVertical = false, isRtl = isRtl)
                                .horizontalScroll(quickScrollState)
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                LocalizedStrings.get("quick_targets", lang) + ":",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = if (isDark) TextMuted else TextMutedLight
                            )
                            presets.forEach { targetSuggestion ->
                                Surface(
                                    onClick = {
                                        viewModel.onTargetChange(targetSuggestion)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.03f),
                                    border = BorderStroke(0.5.dp, if (state.target == targetSuggestion) accent else (if (isDark) GlassBorder else GlassBorderLight))
                                ) {
                                    Text(
                                        targetSuggestion,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            fontWeight = if (state.target == targetSuggestion) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (state.target == targetSuggestion) accent else (if (isDark) Color.White else Color.Black),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(16.dp))
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
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.ErrorOutline, null, tint = DangerNeon, modifier = Modifier.size(16.dp))
                        Text(
                            it,
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
                            onCopyItem = { viewModel.copyIndividualResult(it) }
                        )
                        "DNS" -> DnsResultPanel(
                            results = state.dnsResults,
                            isLoading = state.isLoading,
                            accent = accent,
                            lang = lang,
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
                            onCopy = { viewModel.copyIndividualResult(it) }
                        )
                        else -> LocalInfoPanel(
                            info = state.localIp,
                            publicIp = state.publicIp,
                            isLoading = state.isLoading,
                            accent = accent,
                            lang = lang,
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

@Composable
fun PingResultPanel(
    results: List<com.mrcoder20.portx.domain.PingResult>,
    isLoading: Boolean,
    accent: Color,
    lang: String = "en",
    onCopyItem: (String) -> Unit
) {
    val isDark = LocalAppSettings.current.theme == "DARK"
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

    GlassCard(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp)) {
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
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = accent
                    )
                    CyberBadge(text = "ICMP", color = accent, hasPulseDot = isLoading)
                }
                if (isLoading) {
                    Text(
                        LocalizedStrings.get("querying", lang),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = accent
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // KPI Telemetry Grid (Row 1: TX, RX, Loss %, Avg; Row 2: Min, Max, Jitter)
            if (results.isNotEmpty()) {
                val lossColor = when {
                    packetLoss == 0 -> TertiaryNeon
                    packetLoss < 20 -> WarningNeon
                    else -> DangerNeon
                }

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

                Spacer(Modifier.height(8.dp))

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

                // Interactive Latency Sparkline Bar Visualization
                if (latencies.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    PingLatencySparkline(
                        latencies = latencies,
                        accent = accent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    )
                }

                Spacer(Modifier.height(10.dp))
            }

            // Terminal Log Box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(
                        if (isDark) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.04f),
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        1.dp,
                        if (isDark) GlassBorder.copy(alpha = 0.3f) else GlassBorderLight.copy(alpha = 0.2f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(10.dp)
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .touchDragScroll(listState, isVertical = true)
                            .padding(end = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
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
                                color = if (isDark) Color.White.copy(alpha = 0.02f) else Color.Black.copy(alpha = 0.02f),
                                border = BorderStroke(0.5.dp, if (isDark) GlassBorder.copy(alpha = 0.2f) else GlassBorderLight.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Sequence badge
                                    Text(
                                        "[#${res.sequence}]",
                                        color = accent.copy(alpha = 0.8f),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    // Status Dot
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(statusColor)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        res.message,
                                        color = if (res.isSuccess) (if (isDark) Color.White else Color.Black) else DangerNeon,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Start,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = "Copy line",
                                        tint = if (isDark) TextMuted.copy(alpha = 0.5f) else TextMutedLight.copy(alpha = 0.5f),
                                        modifier = Modifier.size(14.dp)
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
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
                PortXVerticalScrollbar(
                    listState = listState,
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
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
                if (isDark) Color.Black.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.03f),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        val count = recentLatencies.size
        if (count == 0) return@Canvas

        val barSpacing = 4.dp.toPx()
        val totalSpacing = barSpacing * (count - 1).coerceAtLeast(0)
        val barWidth = ((size.width - totalSpacing) / count).coerceIn(4.dp.toPx(), 18.dp.toPx())

        recentLatencies.forEachIndexed { i, lat ->
            val barHeight = ((lat / maxVal) * size.height).toFloat().coerceIn(4.dp.toPx(), size.height)
            val x = i * (barWidth + barSpacing)
            val y = size.height - barHeight

            val barColor = when {
                lat < 40.0 -> TertiaryNeon
                lat < 100.0 -> Color(0xFF00E5FF)
                lat < 180.0 -> WarningNeon
                else -> DangerNeon
            }

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(barColor, barColor.copy(alpha = 0.4f)),
                    startY = y,
                    endY = size.height
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
        }
    }
}

@Composable
fun DnsResultPanel(
    results: List<String>,
    isLoading: Boolean,
    accent: Color,
    lang: String = "en",
    onCopyItem: (String) -> Unit,
    onSelectForPing: (String) -> Unit
) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    val dnsListState = rememberLazyListState()

    GlassCard(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp)) {
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
                        LocalizedStrings.get("dns_resolution_records", lang),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = accent
                    )
                    CyberBadge(text = "DNS", color = accent, hasPulseDot = isLoading)
                }
                if (results.isNotEmpty()) {
                    CyberBadge(
                        text = "${results.size} ${LocalizedStrings.get("dns_records_count", lang)}",
                        color = SecondaryNeon
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = accent
                )
                Spacer(Modifier.height(8.dp))
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(
                        if (isDark) Color.Black.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.03f),
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        1.dp,
                        if (isDark) GlassBorder.copy(alpha = 0.3f) else GlassBorderLight.copy(alpha = 0.2f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(8.dp)
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    LazyColumn(
                        state = dnsListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .touchDragScroll(dnsListState, isVertical = true)
                            .padding(end = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(results, key = { index, ip -> "${index}_$ip" }) { _, ip ->
                            val recordType = when {
                                ip.contains(":") -> "AAAA"
                                ip.matches(Regex("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")) -> "A"
                                else -> "PTR"
                            }
                            val badgeColor = when (recordType) {
                                "A" -> SecondaryNeon
                                "AAAA" -> Color(0xFF00E5FF)
                                else -> accent
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onCopyItem(ip) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDark) Color.White.copy(alpha = 0.035f) else Color.Black.copy(alpha = 0.025f),
                                border = BorderStroke(1.dp, if (isDark) GlassBorder else GlassBorderLight)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    CyberBadge(text = recordType, color = badgeColor)

                                    Spacer(Modifier.width(12.dp))

                                    Text(
                                        ip,
                                        color = if (isDark) Color.White else Color.Black,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Start
                                    )

                                    // Quick Action: Ping this record
                                    if (recordType == "A" || recordType == "AAAA") {
                                        Surface(
                                            onClick = { onSelectForPing(ip) },
                                            shape = RoundedCornerShape(6.dp),
                                            color = accent.copy(alpha = 0.12f),
                                            border = BorderStroke(0.5.dp, accent.copy(alpha = 0.4f)),
                                            modifier = Modifier.padding(end = 8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Icon(Icons.Default.NetworkPing, null, tint = accent, modifier = Modifier.size(12.dp))
                                                Text("Ping", color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        tint = if (isDark) TextMuted else TextMutedLight,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        if (results.isEmpty() && !isLoading) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        LocalizedStrings.get("awaiting_dns", lang),
                                        color = if (isDark) TextMuted else TextMutedLight,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
                PortXVerticalScrollbar(
                    listState = dnsListState,
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                )
            }
        }
    }
}

@Composable
fun WhoisResultPanel(
    result: String?,
    isLoading: Boolean,
    lang: String = "en",
    onCopy: (String) -> Unit = {}
) {
    val accent = LocalAccentColor.current
    val isDark = LocalAppSettings.current.theme == "DARK"

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

    GlassCard(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp)) {
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
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = accent
                    )
                    CyberBadge(text = "WHOIS", color = accent, hasPulseDot = isLoading)
                }
                if (!result.isNullOrBlank()) {
                    IconButton(onClick = { onCopy(result) }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = LocalizedStrings.get("copy_whois", lang),
                            tint = accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Structured WHOIS Identity Dossier Banner
            if (whoisSummary != null && (whoisSummary.first != null || whoisSummary.second != "—" || whoisSummary.third != "—")) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.03f),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                LocalizedStrings.get("whois_summary", lang),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                color = accent
                            )
                            CyberBadge(text = "VERIFIED REGISTRAR", color = TertiaryNeon, fontSize = 9f)
                        }

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

            Spacer(Modifier.height(10.dp))
            if (isLoading && result == null) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp), color = accent)
                Spacer(Modifier.height(8.dp))
            }

            val whoisScrollState = rememberScrollState()
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(
                        if (isDark) Color.Black.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.03f),
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        1.dp,
                        if (isDark) GlassBorder.copy(alpha = 0.3f) else GlassBorderLight.copy(alpha = 0.2f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .touchDragScroll(whoisScrollState, isVertical = true)
                        .verticalScroll(whoisScrollState)
                        .padding(end = 10.dp)
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        SelectionContainer {
                            Text(
                                result ?: LocalizedStrings.get("ready_whois", lang),
                                color = if (result != null) (if (isDark) Color.White else Color.Black) else (if (isDark) TextMuted else TextMutedLight),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 17.sp,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                PortXScrollStateVerticalScrollbar(
                    scrollState = whoisScrollState,
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                )
            }
        }
    }
}

@Composable
fun LocalInfoPanel(
    info: com.mrcoder20.portx.domain.LocalIpInfo?,
    publicIp: String?,
    isLoading: Boolean,
    accent: Color,
    lang: String,
    onCopy: (String) -> Unit,
    onRefresh: () -> Unit,
    onPingSubnet: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val isRtl = lang == "fa" || lang == "ar"
    val isDark = LocalAppSettings.current.theme == "DARK"

    // Derive subnet suggestion (e.g. 192.168.1.1 or 192.168.1.0/24)
    val subnetHint = remember(info?.ipAddress) {
        val ip = info?.ipAddress ?: ""
        if (ip.matches(Regex("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$"))) {
            val parts = ip.split(".")
            "${parts[0]}.${parts[1]}.${parts[2]}.1"
        } else "192.168.1.1"
    }

    GlassCard(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = if (isRtl) 12.dp else 0.dp,
                        end = if (!isRtl) 12.dp else 0.dp
                    )
                    .touchDragScroll(scrollState, isVertical = true)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        CyberBadge(text = "LOCAL", color = SecondaryNeon)
                    }

                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = accent, strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = onRefresh, modifier = Modifier.size(40.dp).springPress()) {
                            Icon(Icons.Default.Refresh, "Refresh", tint = accent, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                info?.let {
                    InfoItem(
                        label = LocalizedStrings.get("internal_ip", lang),
                        value = it.ipAddress,
                        icon = Icons.Default.Lan,
                        color = accent,
                        onClick = { onCopy(it.ipAddress) }
                    )

                    InfoItem(
                        label = LocalizedStrings.get("interface", lang),
                        value = it.interfaceName,
                        icon = Icons.Default.SettingsInputComponent,
                        color = SecondaryNeon,
                        onClick = { onCopy(it.interfaceName) }
                    )

                    InfoItem(
                        label = LocalizedStrings.get("connection", lang),
                        value = if (it.isWifi) LocalizedStrings.get("wifi", lang) else LocalizedStrings.get("wired", lang),
                        icon = if (it.isWifi) Icons.Default.Wifi else Icons.Default.SettingsEthernet,
                        color = TertiaryNeon,
                        onClick = {}
                    )
                }

                publicIp?.let {
                    InfoItem(
                        label = LocalizedStrings.get("public_ip", lang),
                        value = it,
                        icon = Icons.Default.Public,
                        color = Color(0xFF00E5FF),
                        onClick = { onCopy(it) }
                    )
                }

                // Subnet Quick Action Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.02f),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.25f))
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

            PortXScrollStateVerticalScrollbar(
                scrollState = scrollState,
                modifier = Modifier
                    .align(if (isRtl) AbsoluteAlignment.CenterLeft else AbsoluteAlignment.CenterRight)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
fun InfoItem(label: String, value: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) Color.White.copy(alpha = 0.035f) else Color.Black.copy(alpha = 0.025f),
        border = BorderStroke(1.dp, if (isDark) GlassBorder else GlassBorderLight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(color.copy(alpha = 0.12f), CircleShape)
                    .border(1.dp, color.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDark) TextMuted else TextMutedLight,
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    value,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        textDirection = TextDirection.Ltr
                    ),
                    color = if (isDark) Color.White else Color.Black
                )
            }
            Icon(
                Icons.Default.ContentCopy,
                contentDescription = "Copy",
                tint = if (isDark) TextMuted.copy(alpha = 0.6f) else TextMutedLight.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
