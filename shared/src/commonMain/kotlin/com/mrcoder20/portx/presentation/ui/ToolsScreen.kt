package com.mrcoder20.portx.presentation.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.mrcoder20.portx.domain.LocalizedStrings
import com.mrcoder20.portx.presentation.ui.components.PortXVerticalScrollbar
import com.mrcoder20.portx.presentation.ui.components.PortXScrollStateVerticalScrollbar
import com.mrcoder20.portx.presentation.ui.theme.*
import com.mrcoder20.portx.presentation.viewmodel.ToolsViewModel
import org.koin.compose.koinInject

@Composable
fun ToolsScreen(viewModel: ToolsViewModel = koinInject()) {
    val state by viewModel.uiState.collectAsState()
    val appSettings = LocalAppSettings.current
    val accent = LocalAccentColor.current
    val lang = appSettings.language

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // 1. TOOL SELECTOR (NEON CHIPS)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "LOCAL" to Icons.Default.Lan,
                    "PING" to Icons.Default.NetworkPing,
                    "DNS" to Icons.Default.Dns,
                    "WHOIS" to Icons.Default.Public
                ).forEach { (type, icon) ->
                    ToolChip(
                        label = type,
                        icon = icon,
                        isSelected = state.activeTool == type,
                        accent = accent,
                        modifier = Modifier.width(100.dp)
                    ) {
                        viewModel.selectTool(type)
                    }
                }
            }

            // 2. INTEGRATED ACTION BAR (Hides automatically for LOCAL)
            AnimatedVisibility(
                visible = state.activeTool != "LOCAL",
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (appSettings.theme == "DARK") GlassBackground else GlassLight,
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, if (appSettings.theme == "DARK") GlassBorder else GlassBorderLight)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(64.dp).padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when(state.activeTool) {
                                "DNS" -> Icons.Default.Dns
                                "WHOIS" -> Icons.Default.Public
                                else -> Icons.Default.NetworkPing
                            },
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.padding(start = 12.dp).size(20.dp)
                        )

                        OutlinedTextField(
                            value = state.target,
                            onValueChange = { viewModel.onTargetChange(it) },
                            placeholder = { Text(LocalizedStrings.get("target", lang), color = if (appSettings.theme == "DARK") TextMuted else TextMutedLight) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent,
                                unfocusedTextColor = if (appSettings.theme == "DARK") Color.White else Color.Black,
                                focusedTextColor = if (appSettings.theme == "DARK") Color.White else Color.Black,
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
                                        when(state.activeTool) {
                                            "PING" -> viewModel.runPing()
                                            "DNS" -> viewModel.runDnsLookup()
                                            "WHOIS" -> viewModel.runWhois()
                                        }
                                    }
                                }
                            ),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr)
                        )

                        Row(
                            modifier = Modifier.padding(end = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.copyResultsToClipboard() },
                                modifier = Modifier.size(44.dp).background(if (appSettings.theme == "DARK") Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f), CircleShape)
                            ) {
                                Icon(Icons.Default.ContentCopy, "Copy", tint = if (appSettings.theme == "DARK") Color.White else Color.Black, modifier = Modifier.size(18.dp))
                            }

                            val infiniteTransition = rememberInfiniteTransition()
                            val pulseAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.6f, targetValue = 1f,
                                animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse)
                            )

                            Button(
                                onClick = {
                                    if (state.isLoading) {
                                        viewModel.stopActiveTool()
                                    } else {
                                        when(state.activeTool) {
                                            "PING" -> viewModel.runPing()
                                            "DNS" -> viewModel.runDnsLookup()
                                            "WHOIS" -> viewModel.runWhois()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (state.isLoading) DangerNeon.copy(alpha = pulseAlpha) else accent
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.height(48.dp).width(80.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(
                                    if (state.isLoading) Icons.Default.Stop else Icons.Default.FlashOn,
                                    null,
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
            }

            state.error?.let {
                Text(
                    it, color = DangerNeon, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }

            // 3. RESULTS DISPLAY
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                AnimatedContent(
                    targetState = state.activeTool,
                    transitionSpec = { fadeIn() togetherWith fadeOut() }
                ) { tool ->
                    when (tool) {
                        "PING" -> PingResultPanel(state.pingResults, state.isLoading, accent, lang) { viewModel.copyIndividualResult(it) }
                        "DNS" -> DnsResultPanel(state.dnsResults, state.isLoading, accent, lang) { viewModel.copyIndividualResult(it) }
                        "WHOIS" -> WhoisResultPanel(state.whoisResult, state.isLoading, lang) { viewModel.copyIndividualResult(it) }
                        else -> LocalInfoPanel(
                            state.localIp, state.publicIp, state.isLoading, accent, lang,
                            onCopy = { viewModel.copyIndividualResult(it) },
                            onRefresh = { viewModel.refreshLocalInfo() }
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
fun ToolChip(label: String, icon: ImageVector, isSelected: Boolean, accent: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) accent.copy(alpha = 0.15f) else (if (isDark) GlassBackground else GlassLight),
        border = BorderStroke(1.dp, if (isSelected) accent else (if (isDark) GlassBorder else GlassBorderLight))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = if (isSelected) accent else (if (isDark) TextMuted else TextMutedLight), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, color = if (isSelected) (if (isDark) Color.White else Color.Black) else (if (isDark) TextMuted else TextMutedLight), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
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
    LaunchedEffect(results.size) { if (results.isNotEmpty()) listState.animateScrollToItem(results.size - 1) }

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
    } else {
        "—"
    }

    GlassCard(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp)) {
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
                    Text(LocalizedStrings.get("tactical_icmp_output", lang), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent)
                    Surface(
                        color = accent.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
                    ) {
                        Text(
                            "ICMP",
                            color = accent,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (isLoading) Text(LocalizedStrings.get("querying", lang), style = MaterialTheme.typography.labelSmall, color = accent)
            }

            Spacer(Modifier.height(10.dp))

            // KPI Telemetry Banner (Tx, Rx, Loss %, Avg Latency)
            if (results.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // TX
                    KpiStatCard(
                        title = LocalizedStrings.get("packets_tx", lang),
                        value = "$totalSent",
                        accent = accent,
                        modifier = Modifier.weight(1f)
                    )
                    // RX
                    KpiStatCard(
                        title = LocalizedStrings.get("packets_rx", lang),
                        value = "$totalReceived",
                        accent = SecondaryNeon,
                        modifier = Modifier.weight(1f)
                    )
                    // Loss %
                    val lossColor = when {
                        packetLoss == 0 -> TertiaryNeon
                        packetLoss < 20 -> WarningNeon
                        else -> DangerNeon
                    }
                    KpiStatCard(
                        title = LocalizedStrings.get("packet_loss", lang),
                        value = "$packetLoss%",
                        accent = lossColor,
                        modifier = Modifier.weight(1f)
                    )
                    // Avg Latency
                    KpiStatCard(
                        title = LocalizedStrings.get("avg_latency", lang),
                        value = avgLatencyStr,
                        accent = Color(0xFF00E5FF),
                        modifier = Modifier.weight(1.1f)
                    )
                }
                Spacer(Modifier.height(10.dp))
            }

            // Terminal Log Box
            Box(modifier = Modifier.weight(1f).fillMaxWidth().background(if (isDark) Color.Black.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.05f), RoundedCornerShape(12.dp)).padding(10.dp)) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(end = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        itemsIndexed(results, key = { index, res -> "${index}_${res.sequence}" }) { _, res ->
                            Row(modifier = Modifier.fillMaxWidth().clickable { onCopyItem(res.message) }, verticalAlignment = Alignment.CenterVertically) {
                                Text("> ", color = accent, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(res.message, color = if (res.isSuccess) (if (isDark) Color.White else Color.Black) else DangerNeon, fontFamily = FontFamily.Monospace, fontSize = 12.sp, textAlign = TextAlign.Start)
                            }
                        }
                        if (results.isEmpty() && !isLoading) item { Text(LocalizedStrings.get("engine_ready", lang), color = if (isDark) TextMuted else TextMutedLight, fontSize = 12.sp, fontFamily = FontFamily.Monospace) }
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
fun KpiStatCard(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    Surface(
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.03f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
                color = if (isDark) TextMuted else TextMutedLight,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                ),
                color = accent,
                maxLines = 1
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
    onCopyItem: (String) -> Unit
) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    GlassCard(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp)) {
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
                    Text(LocalizedStrings.get("dns_resolution_records", lang), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent)
                    Surface(
                        color = accent.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
                    ) {
                        Text(
                            "DNS",
                            color = accent,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (results.isNotEmpty()) {
                    Surface(
                        color = SecondaryNeon.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SecondaryNeon.copy(alpha = 0.3f))
                    ) {
                        Text(
                            "${results.size} ${LocalizedStrings.get("dns_records_count", lang)}",
                            color = SecondaryNeon,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            if (isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp), color = accent)
            val dnsListState = rememberLazyListState()
            Box(modifier = Modifier.weight(1f).fillMaxWidth().background(if (isDark) Color.Black.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.05f), RoundedCornerShape(10.dp)).padding(8.dp)) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    LazyColumn(state = dnsListState, modifier = Modifier.fillMaxSize().padding(end = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                modifier = Modifier.fillMaxWidth().clickable { onCopyItem(ip) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDark) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.02f),
                                border = BorderStroke(1.dp, if (isDark) GlassBorder else GlassBorderLight)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Surface(
                                        color = badgeColor.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            recordType,
                                            color = badgeColor,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 9.sp),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        ip,
                                        color = if (isDark) Color.White else Color.Black,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Start
                                    )
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        tint = if (isDark) TextMuted else TextMutedLight,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        if (results.isEmpty() && !isLoading) item { Text(LocalizedStrings.get("awaiting_dns", lang), color = if (isDark) TextMuted else TextMutedLight, fontSize = 12.sp, fontFamily = FontFamily.Monospace) }
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
    
    // Parse WHOIS summary fields if result is present
    val whoisSummary = remember(result) {
        if (result == null) null
        else {
            val registrarMatch = "(?i)Registrar:\\s*([^\\r\\n]+)".toRegex().find(result)?.groupValues?.get(1)?.trim()
            val createdMatch = "(?i)(?:Creation Date|created):\\s*([^\\r\\n]+)".toRegex().find(result)?.groupValues?.get(1)?.trim()
            val expiryMatch = "(?i)(?:Registry Expiry Date|paid-till|Expiration Date):\\s*([^\\r\\n]+)".toRegex().find(result)?.groupValues?.get(1)?.trim()
            Triple(registrarMatch, createdMatch, expiryMatch)
        }
    }

    GlassCard(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp)) {
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
                    Text(LocalizedStrings.get("whois_authority_data", lang), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent)
                    Surface(
                        color = accent.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
                    ) {
                        Text(
                            "WHOIS",
                            color = accent,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (!result.isNullOrBlank()) {
                    IconButton(onClick = { onCopy(result) }, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = LocalizedStrings.get("copy_whois", lang), tint = accent, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Structured WHOIS HUD summary banner if key fields are extracted
            if (whoisSummary != null && (whoisSummary.first != null || whoisSummary.second != null || whoisSummary.third != null)) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.03f),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            LocalizedStrings.get("whois_summary", lang),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            color = accent
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            whoisSummary.first?.let { reg ->
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(LocalizedStrings.get("registrar", lang), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = if (isDark) TextMuted else TextMutedLight)
                                    Text(reg, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp), color = if (isDark) Color.White else Color.Black, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                }
                            }
                            whoisSummary.second?.let { crt ->
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(LocalizedStrings.get("created_date", lang), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = if (isDark) TextMuted else TextMutedLight)
                                    Text(crt, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp), color = if (isDark) Color.White else Color.Black, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                }
                            }
                            whoisSummary.third?.let { exp ->
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(LocalizedStrings.get("expiry_date", lang), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = if (isDark) TextMuted else TextMutedLight)
                                    Text(exp, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp), color = WarningNeon, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            if (isLoading && result == null) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp), color = accent)
            val whoisScrollState = rememberScrollState()
            Box(modifier = Modifier.weight(1f).fillMaxWidth().background(if (isDark) Color.Black.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.05f), RoundedCornerShape(10.dp)).padding(12.dp)) {
                Box(modifier = Modifier.fillMaxSize().verticalScroll(whoisScrollState).padding(end = 10.dp)) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        SelectionContainer {
                            Text(
                                result ?: LocalizedStrings.get("ready_whois", lang),
                                color = if (result != null) (if (isDark) Color.White else Color.Black) else (if (isDark) TextMuted else TextMutedLight),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
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
    onRefresh: () -> Unit
) {
    val scrollState = rememberScrollState()
    val isRtl = lang == "fa" || lang == "ar"
    GlassCard(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = if (isRtl) 12.dp else 0.dp,
                        end = if (!isRtl) 12.dp else 0.dp
                    )
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(LocalizedStrings.get("device_environment", lang), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = accent)
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = accent, strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = onRefresh, modifier = Modifier.size(44.dp)) {
                            Icon(Icons.Default.Refresh, null, tint = accent, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                info?.let {
                    InfoItem(LocalizedStrings.get("internal_ip", lang), it.ipAddress, Icons.Default.Lan, accent) { onCopy(it.ipAddress) }
                    InfoItem(LocalizedStrings.get("interface", lang), it.interfaceName, Icons.Default.SettingsInputComponent, SecondaryNeon) { onCopy(it.interfaceName) }
                    InfoItem(LocalizedStrings.get("connection", lang), if (it.isWifi) LocalizedStrings.get("wifi", lang) else LocalizedStrings.get("wired", lang), if (it.isWifi) Icons.Default.Wifi else Icons.Default.SettingsEthernet, TertiaryNeon) {}
                }

                publicIp?.let {
                    InfoItem(LocalizedStrings.get("public_ip", lang), it, Icons.Default.Public, Color(0xFF00E5FF)) { onCopy(it) }
                }
                
                if (info == null && publicIp == null && !isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Button(
                            onClick = onRefresh,
                            colors = ButtonDefaults.buttonColors(containerColor = accent.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, accent)
                        ) {
                            Icon(Icons.Default.Refresh, null, tint = if (LocalAppSettings.current.theme == "DARK") Color.White else Color.Black)
                            Spacer(Modifier.width(8.dp))
                            Text(LocalizedStrings.get("refresh", lang), color = if (LocalAppSettings.current.theme == "DARK") Color.White else Color.Black)
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
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.03f),
        border = BorderStroke(1.dp, if (isDark) GlassBorder else GlassBorderLight)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(40.dp).background(color.copy(alpha = 0.12f), CircleShape).border(1.dp, color.copy(alpha = 0.3f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = if (isDark) TextMuted else TextMutedLight, letterSpacing = 0.5.sp)
                Text(value, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, textDirection = TextDirection.Ltr), color = if (isDark) Color.White else Color.Black)
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
