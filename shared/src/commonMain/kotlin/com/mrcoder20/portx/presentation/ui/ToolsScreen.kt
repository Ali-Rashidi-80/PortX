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
    GlassCard(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp)) {
        Column {
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
            Spacer(Modifier.height(12.dp))
            Box(modifier = Modifier.weight(1f).fillMaxWidth().background(if (isDark) Color.Black.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.05f), RoundedCornerShape(8.dp)).padding(8.dp)) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        itemsIndexed(results, key = { index, res -> "${index}_${res.sequence}" }) { _, res ->
                            Row(modifier = Modifier.fillMaxWidth().clickable { onCopyItem(res.message) }, verticalAlignment = Alignment.CenterVertically) {
                                Text("> ", color = accent, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(res.message, color = if (res.isSuccess) (if (isDark) Color.White else Color.Black) else DangerNeon, fontFamily = FontFamily.Monospace, fontSize = 12.sp, textAlign = TextAlign.Start)
                            }
                        }
                        if (results.isEmpty() && !isLoading) item { Text(LocalizedStrings.get("engine_ready", lang), color = if (isDark) TextMuted else TextMutedLight, fontSize = 12.sp, fontFamily = FontFamily.Monospace) }
                    }
                }
            }
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
        Column {
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
            }
            Spacer(Modifier.height(12.dp))
            if (isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp), color = accent)
            Box(modifier = Modifier.weight(1f).fillMaxWidth().background(if (isDark) Color.Black.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.05f), RoundedCornerShape(8.dp)).padding(8.dp)) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(results, key = { index, ip -> "${index}_$ip" }) { _, ip ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onCopyItem(ip) }.padding(vertical = 4.dp)) {
                                Icon(Icons.Default.Adjust, null, tint = accent, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(ip, color = if (isDark) Color.White else Color.Black, fontFamily = FontFamily.Monospace, fontSize = 14.sp, textAlign = TextAlign.Start)
                            }
                        }
                        if (results.isEmpty() && !isLoading) item { Text(LocalizedStrings.get("awaiting_dns", lang), color = if (isDark) TextMuted else TextMutedLight, fontSize = 12.sp, fontFamily = FontFamily.Monospace) }
                    }
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
    onCopy: (String) -> Unit = {}
) {
    val accent = LocalAccentColor.current
    val isDark = LocalAppSettings.current.theme == "DARK"
    GlassCard(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp)) {
        Column {
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
            Spacer(Modifier.height(12.dp))
            if (isLoading && result == null) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp), color = accent)
            Box(modifier = Modifier.weight(1f).fillMaxWidth().background(if (isDark) Color.Black.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.05f), RoundedCornerShape(8.dp)).verticalScroll(rememberScrollState()).padding(12.dp)) {
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
    GlassCard(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
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
                InfoItem(LocalizedStrings.get("interface", lang), it.interfaceName, Icons.Default.SettingsInputComponent, accent) { onCopy(it.interfaceName) }
                InfoItem(LocalizedStrings.get("connection", lang), if (it.isWifi) LocalizedStrings.get("wifi", lang) else LocalizedStrings.get("wired", lang), if (it.isWifi) Icons.Default.Wifi else Icons.Default.SettingsEthernet, accent) {}
            }

            publicIp?.let {
                InfoItem(LocalizedStrings.get("public_ip", lang), it, Icons.Default.Public, accent) { onCopy(it) }
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
    }
}

@Composable
fun InfoItem(label: String, value: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    Row(
        modifier = Modifier.fillMaxWidth().background(if (isDark) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.03f), RoundedCornerShape(12.dp)).clickable { onClick() }.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(36.dp).background(color.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = color, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = if (isDark) TextMuted else TextMutedLight, letterSpacing = 1.sp)
            Text(value, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, textDirection = TextDirection.Ltr), color = if (isDark) Color.White else Color.Black)
        }
    }
}
