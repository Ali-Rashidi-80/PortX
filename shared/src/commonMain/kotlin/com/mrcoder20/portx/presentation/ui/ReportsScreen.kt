package com.mrcoder20.portx.presentation.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mrcoder20.portx.domain.LocalizedStrings
import com.mrcoder20.portx.domain.model.ScanResult
import com.mrcoder20.portx.presentation.ui.components.DisintegrationContainer
import com.mrcoder20.portx.presentation.ui.components.PortXScrollStateVerticalScrollbar
import com.mrcoder20.portx.presentation.ui.components.PortXVerticalScrollbar
import com.mrcoder20.portx.presentation.ui.components.cyberPulse
import com.mrcoder20.portx.presentation.ui.components.springPress
import com.mrcoder20.portx.presentation.ui.components.touchDragScroll
import com.mrcoder20.portx.presentation.ui.theme.*
import com.mrcoder20.portx.domain.DateFormatter
import com.mrcoder20.portx.presentation.viewmodel.ReportsViewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject
import kotlin.math.roundToInt

@Composable
fun ReportsScreen(viewModel: ReportsViewModel = koinInject()) {
    val state by viewModel.uiState.collectAsState()
    val appSettings = LocalAppSettings.current
    val accent = LocalAccentColor.current
    val isDark = appSettings.theme == "DARK"
    val lang = appSettings.language

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var filterQuery by remember { mutableStateOf("") }
    var selectedReportForDetail by remember { mutableStateOf<ScanResult?>(null) }
    var selectedReportForExport by remember { mutableStateOf<ScanResult?>(null) }
    var deletingScanId by remember { mutableStateOf<Long?>(null) }
    var isClearingAllWithDust by remember { mutableStateOf(false) }

    val filteredScans = remember(state.scans, filterQuery) {
        if (filterQuery.isBlank()) state.scans
        else state.scans.filter {
            it.target.contains(filterQuery.trim(), ignoreCase = true) ||
            (it.deviceName?.contains(filterQuery.trim(), ignoreCase = true) == true) ||
            (it.osFingerprint?.contains(filterQuery.trim(), ignoreCase = true) == true)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(16.dp)
            ) {
                Column {
                    // Header Bar
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
                                LocalizedStrings.get("reports", lang).uppercase(),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = if (isDark) TextMuted else TextMutedLight,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                            if (state.scans.isNotEmpty()) {
                                CyberBadge(
                                    text = "${state.scans.size}",
                                    color = accent
                                )
                            }
                        }

                        if (state.scans.isNotEmpty()) {
                            IconButton(
                                onClick = { showDeleteConfirm = true },
                                modifier = Modifier.size(38.dp).springPress()
                            ) {
                                Icon(
                                    Icons.Default.DeleteSweep,
                                    contentDescription = "Clear All Reports",
                                    tint = DangerNeon.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Security Trend Chart (Interactive, dynamic, hover HUD)
                    ScanScoreTrendSection(
                        scans = state.scans,
                        accent = accent,
                        lang = lang,
                        isDark = isDark,
                        onScanClick = { selectedReportForDetail = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Search & Filter Box
                    if (state.scans.isNotEmpty()) {
                        OutlinedTextField(
                            value = filterQuery,
                            onValueChange = { filterQuery = it },
                            placeholder = {
                                Text(
                                    LocalizedStrings.get("search_reports", lang),
                                    color = if (isDark) TextMuted else TextMutedLight,
                                    fontSize = 12.sp
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Search, null, tint = accent, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                if (filterQuery.isNotEmpty()) {
                                    IconButton(onClick = { filterQuery = "" }, modifier = Modifier.size(32.dp)) {
                                        Icon(
                                            Icons.Default.Clear,
                                            null,
                                            tint = if (isDark) TextMuted else TextMutedLight,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accent,
                                unfocusedBorderColor = (if (isDark) GlassBorder else GlassBorderLight).copy(alpha = 0.5f),
                                focusedTextColor = if (isDark) Color.White else Color.Black,
                                unfocusedTextColor = if (isDark) Color.White else Color.Black,
                                cursorColor = accent
                            ),
                            textStyle = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.ContentOrLtr)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Content State
                    if (state.isLoading) {
                        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = accent)
                        }
                    } else if (state.scans.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Assessment,
                                    contentDescription = null,
                                    tint = if (isDark) TextMuted else TextMutedLight,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    LocalizedStrings.get("no_reports", lang),
                                    color = if (isDark) TextMuted else TextMutedLight,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    } else if (filteredScans.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                LocalizedStrings.get("no_matching_reports", lang),
                                color = if (isDark) TextMuted else TextMutedLight,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        val reportsListState = rememberLazyListState()
                        val isRtl = lang == "fa" || lang == "ar"
                        DisintegrationContainer(
                            isDisintegrating = isClearingAllWithDust,
                            accent = DangerNeon,
                            particleCount = 260,
                            onDisintegrated = {
                                viewModel.clearAll()
                                isClearingAllWithDust = false
                            },
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                LazyColumn(
                                    state = reportsListState,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .touchDragScroll(reportsListState, isVertical = true)
                                        .padding(
                                            start = if (isRtl) 12.dp else 0.dp,
                                            end = if (!isRtl) 12.dp else 0.dp
                                        ),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(filteredScans, key = { it.id ?: it.timestamp }) { scan ->
                                        val isDeletingThis = deletingScanId == scan.id
                                        DisintegrationContainer(
                                            isDisintegrating = isDeletingThis,
                                            accent = DangerNeon,
                                            particleCount = 180,
                                            onDisintegrated = {
                                                scan.id?.let { viewModel.deleteScan(it) }
                                                if (deletingScanId == scan.id) {
                                                    deletingScanId = null
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            ReportHistoryItem(
                                                scan = scan,
                                                accent = accent,
                                                lang = lang,
                                                onClick = {
                                                    if (deletingScanId == null && !isClearingAllWithDust) {
                                                        selectedReportForDetail = scan
                                                    }
                                                },
                                                onDelete = {
                                                    if (scan.id != null) {
                                                        deletingScanId = scan.id
                                                    } else {
                                                        scan.id?.let { viewModel.deleteScan(it) }
                                                    }
                                                },
                                                onExport = {
                                                    if (deletingScanId == null && !isClearingAllWithDust) {
                                                        selectedReportForExport = scan
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                                PortXVerticalScrollbar(
                                    listState = reportsListState,
                                    modifier = Modifier
                                        .align(if (isRtl) AbsoluteAlignment.CenterLeft else AbsoluteAlignment.CenterRight)
                                        .fillMaxHeight()
                                        .padding(horizontal = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Global Export Format Selector (Segmented)
            Text(
                LocalizedStrings.get("global_export_format", lang),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isDark) TextMuted else TextMutedLight,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val exportFormats = listOf(
                "MD" to Icons.Default.Description,
                "CSV" to Icons.Default.TableChart,
                "JSON" to Icons.Default.DataObject
            )
            val selectedFormatIndex = exportFormats.indexOfFirst { it.first == state.exportFormat }.coerceAtLeast(0)

            CyberSegmentedControl(
                items = exportFormats.map { (fmt, icon) ->
                    val label = when (fmt) {
                        "MD" -> "Markdown (.md)"
                        "CSV" -> "CSV Table (.csv)"
                        else -> "JSON Schema (.json)"
                    }
                    label to icon
                },
                selectedIndex = selectedFormatIndex,
                onIndexSelected = { index ->
                    viewModel.onFormatChange(exportFormats[index].first)
                },
                accent = accent,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Snackbar
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

    // Detail Modal Dialog
    selectedReportForDetail?.let { scan ->
        ReportDetailDialog(
            scan = scan,
            accent = accent,
            lang = lang,
            onDismiss = { selectedReportForDetail = null },
            onExport = {
                selectedReportForDetail = null
                selectedReportForExport = scan
            },
            onDelete = {
                val targetId = scan.id
                selectedReportForDetail = null
                if (targetId != null) {
                    deletingScanId = targetId
                } else {
                    scan.id?.let { viewModel.deleteScan(it) }
                }
            }
        )
    }

    // Upgraded Ultimate Export & Save Dialog
    selectedReportForExport?.let { scan ->
        ReportExportDialog(
            scan = scan,
            initialFormat = state.exportFormat,
            accent = accent,
            lang = lang,
            isDark = isDark,
            viewModel = viewModel,
            onFormatSelected = { format -> viewModel.onFormatChange(format) },
            onSaveAs = { format -> viewModel.saveScanAs(scan, format) },
            onQuickSave = { format -> viewModel.quickSaveScan(scan, format) },
            onShare = { format -> viewModel.shareScan(scan, format) },
            onDismiss = { selectedReportForExport = null }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm) {
        val isDarkTheme = LocalAppSettings.current.theme == "DARK"
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = if (isDarkTheme) SurfaceDark else SurfaceLight,
            titleContentColor = if (isDarkTheme) Color.White else Color.Black,
            textContentColor = if (isDarkTheme) TextSecondary else TextSecondaryLight,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, null, tint = DangerNeon, modifier = Modifier.size(20.dp))
                    Text(LocalizedStrings.get("clear_history_title", lang), fontWeight = FontWeight.Bold)
                }
            },
            text = { Text(LocalizedStrings.get("clear_history_desc", lang)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        isClearingAllWithDust = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerNeon),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.springPress()
                ) {
                    Text(LocalizedStrings.get("clear_all", lang), color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false },
                    modifier = Modifier.springPress()
                ) {
                    Text(LocalizedStrings.get("cancel", lang), color = if (isDarkTheme) Color.White else Color.Black)
                }
            },
            modifier = Modifier.padding(24.dp)
        )
    }
}

private enum class TrendFilterMode {
    ALL, LAST_10, LAST_25, CRITICAL
}

@Composable
fun ScanScoreTrendSection(
    scans: List<ScanResult>,
    accent: Color,
    lang: String,
    isDark: Boolean,
    onScanClick: (ScanResult) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(TrendFilterMode.ALL) }

    val sortedScans = remember(scans) { scans.sortedBy { it.timestamp } }

    val visibleScans = remember(sortedScans, selectedFilter) {
        when (selectedFilter) {
            TrendFilterMode.ALL -> sortedScans
            TrendFilterMode.LAST_10 -> sortedScans.takeLast(10)
            TrendFilterMode.LAST_25 -> sortedScans.takeLast(25)
            TrendFilterMode.CRITICAL -> sortedScans.filter { it.securityScore < 50 }
        }
    }

    val scores = remember(visibleScans) { visibleScans.map { it.securityScore } }

    val avgScore = remember(scores) { if (scores.isNotEmpty()) scores.average().roundToInt() else 0 }
    val peakScore = remember(scores) { scores.maxOrNull() ?: 0 }
    val minScore = remember(scores) { scores.minOrNull() ?: 0 }
    val latestScore = remember(scores) { scores.lastOrNull() ?: 0 }

    // Trend Delta calculation (compare last 3 average vs previous average)
    val trendDelta = remember(scores) {
        if (scores.size >= 4) {
            val recent = scores.takeLast(3).average()
            val prior = scores.dropLast(3).average()
            (recent - prior).roundToInt()
        } else if (scores.size in 2..3) {
            scores.last() - scores.first()
        } else {
            0
        }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Color.White.copy(alpha = 0.035f) else Color.Black.copy(alpha = 0.025f),
        border = BorderStroke(1.dp, if (isDark) GlassBorder else GlassBorderLight)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Title + Dynamic Trend Badge + Filter Chips
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
                        LocalizedStrings.get("security_trend", lang),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isDark) Color.White else Color.Black
                    )

                    if (scores.size >= 2) {
                        val trendColor = when {
                            trendDelta > 0 -> TertiaryNeon
                            trendDelta < 0 -> DangerNeon
                            else -> accent
                        }
                        val trendIcon = when {
                            trendDelta > 0 -> "▲ +"
                            trendDelta < 0 -> "▼ "
                            else -> "● "
                        }
                        val trendLabel = when {
                            trendDelta > 0 -> LocalizedStrings.get("trend_improved", lang)
                            trendDelta < 0 -> LocalizedStrings.get("trend_declined", lang)
                            else -> LocalizedStrings.get("trend_stable", lang)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = trendColor.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, trendColor.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    "$trendIcon$trendDelta%",
                                    color = trendColor,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp
                                )
                                Text(
                                    trendLabel,
                                    color = trendColor,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 8.5.sp
                                )
                            }
                        }
                    }
                }

                // Metric Badges Summary
                if (scores.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        CyberBadge(text = "${LocalizedStrings.get("avg_score", lang)} $avgScore%", color = accent, fontSize = 8.5f)
                        CyberBadge(text = "${LocalizedStrings.get("peak_score", lang)} $peakScore%", color = TertiaryNeon, fontSize = 8.5f)
                        if (minScore < 50) {
                            CyberBadge(text = "${LocalizedStrings.get("min_score", lang)} $minScore%", color = DangerNeon, fontSize = 8.5f)
                        }
                        CyberBadge(text = "${LocalizedStrings.get("latest_score", lang)} $latestScore%", color = SecondaryNeon, fontSize = 8.5f)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Filter Chips Bar (All, Last 10, Last 25, Critical)
            if (sortedScans.size > 5) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 6.dp)
                ) {
                    val filterItems = listOf(
                        TrendFilterMode.ALL to LocalizedStrings.get("trend_all", lang),
                        TrendFilterMode.LAST_10 to LocalizedStrings.get("trend_last_10", lang),
                        TrendFilterMode.LAST_25 to LocalizedStrings.get("trend_last_25", lang),
                        TrendFilterMode.CRITICAL to LocalizedStrings.get("trend_critical", lang)
                    )
                    filterItems.forEach { (mode, label) ->
                        val isSelected = selectedFilter == mode
                        Surface(
                            onClick = { selectedFilter = mode },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) accent.copy(alpha = if (isDark) 0.25f else 0.85f) else Color.Transparent,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) accent.copy(alpha = 0.6f) else (if (isDark) GlassBorder.copy(alpha = 0.25f) else GlassBorderLight.copy(alpha = 0.25f))
                            ),
                            modifier = Modifier.springPress(pressedScale = 0.94f)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected) (if (isDark) Color.White else Color.White) else (if (isDark) TextMuted else TextMutedLight),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // Interactive Dynamic Canvas Chart
            ScanScoreTrendChart(
                scans = visibleScans,
                accent = accent,
                lang = lang,
                isDark = isDark,
                onScanClick = onScanClick,
                modifier = Modifier.fillMaxWidth().height(120.dp)
            )
        }
    }
}

@Composable
fun ScanScoreTrendChart(
    scans: List<ScanResult>,
    accent: Color,
    lang: String = "en",
    isDark: Boolean = true,
    onScanClick: (ScanResult) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (scans.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                LocalizedStrings.get("not_enough_data", lang),
                color = TextMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        return
    }

    if (scans.size == 1) {
        val singleScan = scans.first()
        val (sDate, sTime) = DateFormatter.formatScanTimestamp(singleScan.timestamp, lang)
        val sColor = when {
            singleScan.securityScore >= 80 -> TertiaryNeon
            singleScan.securityScore >= 50 -> WarningNeon
            else -> DangerNeon
        }
        Surface(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .springPress(pressedScale = 0.98f)
                .clickable { onScanClick(singleScan) },
            color = if (isDark) Color.White.copy(alpha = 0.035f) else Color.Black.copy(alpha = 0.025f),
            border = BorderStroke(1.dp, sColor.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(sColor.copy(alpha = 0.15f))
                            .border(1.5.dp, sColor, CircleShape)
                            .cyberPulse(glowColor = sColor, enabled = true),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${singleScan.securityScore}%",
                            color = sColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Column {
                        val isolatedTarget = "\u2066${singleScan.target}\u2069"
                        val title = if (!singleScan.deviceName.isNullOrBlank()) "$isolatedTarget (${singleScan.deviceName})" else isolatedTarget
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                            color = if (isDark) Color.White else Color.Black,
                            maxLines = 1
                        )
                        Text(
                            text = "\u2066$sDate $sTime\u2069 \u2022 ${singleScan.openPorts.size} ${LocalizedStrings.get("open_ports_count", lang)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = if (isDark) TextMuted else TextMutedLight
                        )
                    }
                }

                CyberBadge(
                    text = LocalizedStrings.get("click_to_view", lang),
                    color = accent,
                    fontSize = 8.5f
                )
            }
        }
        return
    }

    val scores = remember(scans) { scans.map { it.securityScore.toFloat() / 100f } }

    var hoveredIndex by remember { mutableStateOf<Int?>(null) }
    var isPointerActive by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition()
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )

    val entryAnim = remember { Animatable(0f) }
    LaunchedEffect(scans.size) {
        entryAnim.snapTo(0f)
        entryAnim.animateTo(1f, animationSpec = tween(600, easing = FastOutSlowInEasing))
    }

    Column(modifier = modifier) {
        // Floating / Docked Live Inspection Tooltip HUD
        AnimatedVisibility(
            visible = hoveredIndex != null && hoveredIndex in scans.indices,
            enter = fadeIn(tween(150)) + expandVertically(),
            exit = fadeOut(tween(150)) + shrinkVertically()
        ) {
            hoveredIndex?.let { idx ->
                if (idx in scans.indices) {
                    val scan = scans[idx]
                    val (dtDate, dtTime) = DateFormatter.formatScanTimestamp(scan.timestamp, lang)
                    val (riskLabel, riskColor) = when {
                        scan.securityScore >= 80 -> LocalizedStrings.get("secure", lang) to TertiaryNeon
                        scan.securityScore >= 50 -> LocalizedStrings.get("warning", lang) to WarningNeon
                        else -> LocalizedStrings.get("high_risk", lang) to DangerNeon
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = (if (isDark) CardDark else SurfaceLight).copy(alpha = 0.96f),
                        border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(accent, SecondaryNeon))),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .clickable { onScanClick(scan) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = riskColor.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, riskColor.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        "${scan.securityScore}%",
                                        color = riskColor,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Column {
                                    val isolatedHost = "\u2066${scan.target}\u2069"
                                    val title = if (!scan.deviceName.isNullOrBlank()) "$isolatedHost (${scan.deviceName})" else isolatedHost
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                        color = if (isDark) Color.White else Color.Black,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "\u2066$dtDate $dtTime\u2069 \u2022 ${scan.openPorts.size} ${LocalizedStrings.get("open_ports_count", lang)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = if (isDark) TextMuted else TextMutedLight
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CyberBadge(text = riskLabel, color = riskColor, fontSize = 8.5f)
                                Icon(
                                    Icons.Default.TouchApp,
                                    contentDescription = LocalizedStrings.get("click_to_view", lang),
                                    tint = accent,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Interactive Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(scans) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val position = event.changes.firstOrNull()?.position
                            when (event.type) {
                                PointerEventType.Move, PointerEventType.Press -> {
                                    if (position != null && size.width > 0) {
                                        val spacing = size.width / (scores.size - 1).coerceAtLeast(1)
                                        val idx = (position.x / spacing).roundToInt().coerceIn(0, scores.size - 1)
                                        hoveredIndex = idx
                                        isPointerActive = true
                                    }
                                }
                                PointerEventType.Release -> {
                                    if (position != null && size.width > 0) {
                                        val spacing = size.width / (scores.size - 1).coerceAtLeast(1)
                                        val idx = (position.x / spacing).roundToInt().coerceIn(0, scores.size - 1)
                                        if (idx in scans.indices) {
                                            onScanClick(scans[idx])
                                        }
                                    }
                                    isPointerActive = false
                                }
                                PointerEventType.Exit -> {
                                    hoveredIndex = null
                                    isPointerActive = false
                                }
                            }
                        }
                    }
                }
                .pointerInput(scans) {
                    detectTapGestures { offset ->
                        val spacing = size.width / (scores.size - 1).coerceAtLeast(1)
                        val idx = (offset.x / spacing).roundToInt().coerceIn(0, scores.size - 1)
                        if (idx in scans.indices) {
                            onScanClick(scans[idx])
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val spacing = width / (scores.size - 1).coerceAtLeast(1)
                val vPadding = 12.dp.toPx()
                val graphHeight = (height - vPadding * 2).coerceAtLeast(1f)
                val anim = entryAnim.value

                // Grid lines: 25%, 50%, 75%, 100%
                val gridAlpha = if (isDark) 0.07f else 0.05f
                listOf(0.25f, 0.5f, 0.75f).forEach { frac ->
                    val y = vPadding + graphHeight * (1f - frac)
                    drawLine(
                        color = (if (isDark) Color.White else Color.Black).copy(alpha = gridAlpha),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )
                }

                val points = scores.mapIndexed { index, score ->
                    val animatedScore = score * anim
                    Offset(index * spacing, vPadding + graphHeight * (1f - animatedScore))
                }

                if (points.size >= 2) {
                    val path = Path().apply {
                        moveTo(points[0].x, points[0].y)
                        for (i in 0 until points.size - 1) {
                            val p1 = points[i]
                            val p2 = points[i + 1]
                            val controlPoint1 = Offset(p1.x + (p2.x - p1.x) / 2f, p1.y)
                            val controlPoint2 = Offset(p1.x + (p2.x - p1.x) / 2f, p2.y)
                            cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p2.x, p2.y)
                        }
                    }

                    val fillPath = Path().apply {
                        addPath(path)
                        lineTo(points.last().x, height)
                        lineTo(points.first().x, height)
                        close()
                    }

                    // Gradient Area Fill
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            listOf(
                                accent.copy(alpha = 0.30f * anim),
                                SecondaryNeon.copy(alpha = 0.10f * anim),
                                Color.Transparent
                            )
                        )
                    )

                    // Curve Stroke with glowing cyber gradient
                    drawPath(
                        path = path,
                        brush = Brush.horizontalGradient(listOf(SecondaryNeon, accent, TertiaryNeon)),
                        style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                // Glowing Laser Guideline at hovered position
                hoveredIndex?.let { hIdx ->
                    if (hIdx in points.indices) {
                        val pt = points[hIdx]
                        drawLine(
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    accent.copy(alpha = 0.8f),
                                    accent.copy(alpha = 0.2f)
                                )
                            ),
                            start = Offset(pt.x, 0f),
                            end = Offset(pt.x, height),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }
                }

                // Data Points Drawing
                points.forEachIndexed { index, pt ->
                    val isHovered = hoveredIndex == index
                    val isLatest = index == points.size - 1

                    if (isHovered) {
                        // Interactive Pulsing Dual Halo
                        drawCircle(
                            color = accent.copy(alpha = 0.25f),
                            radius = 16.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = accent.copy(alpha = 0.6f),
                            radius = 9.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 4.5.dp.toPx(),
                            center = pt
                        )
                    } else if (isLatest) {
                        // Live pulse beacon on latest point
                        drawCircle(
                            color = SecondaryNeon.copy(alpha = 0.35f * pulseGlow),
                            radius = 11.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = SecondaryNeon,
                            radius = 4.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.dp.toPx(),
                            center = pt
                        )
                    } else {
                        drawCircle(
                            color = accent.copy(alpha = 0.30f),
                            radius = 5.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.2.dp.toPx(),
                            center = pt
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReportHistoryItem(
    scan: ScanResult,
    accent: Color,
    lang: String = "en",
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit
) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    val (formattedDate, formattedTime) = DateFormatter.formatScanTimestamp(scan.timestamp, lang)

    val (riskLabel, riskColor) = when {
        scan.securityScore >= 80 -> LocalizedStrings.get("secure", lang) to TertiaryNeon
        scan.securityScore >= 50 -> LocalizedStrings.get("warning", lang) to WarningNeon
        else -> LocalizedStrings.get("high_risk", lang) to DangerNeon
    }

    var isDisintegrating by remember { mutableStateOf(false) }

    DisintegrationContainer(
        isDisintegrating = isDisintegrating,
        accent = DangerNeon,
        particleCount = 180,
        onDisintegrated = onDelete,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 86.dp)
                .springPress(pressedScale = 0.98f)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, if (isDark) GlassBorder else GlassBorderLight, RoundedCornerShape(16.dp))
                .clickable(onClick = onClick),
            color = if (isDark) Color.White.copy(alpha = 0.045f) else Color.Black.copy(alpha = 0.025f)
        ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val isolatedTarget = "\u2066${scan.target}\u2069"
                val title = if (!scan.deviceName.isNullOrBlank()) "$isolatedTarget (${scan.deviceName})" else "${LocalizedStrings.get("target", lang)}: $isolatedTarget"

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDark) Color.White else Color.Black,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    CyberBadge(
                        text = riskLabel,
                        color = riskColor,
                        hasGlowAura = scan.securityScore < 50,
                        fontSize = 9f
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Metadata row: Date/Time + OS Fingerprint + Open Ports Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isolatedDateTime = "\u2066$formattedDate $formattedTime\u2069"
                    Text(
                        text = if (!scan.osFingerprint.isNullOrBlank()) "$isolatedDateTime \u2022 ${scan.osFingerprint}" else isolatedDateTime,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = if (isDark) TextMuted else TextMutedLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    CyberBadge(
                        text = "${scan.openPorts.size} ${LocalizedStrings.get("open_ports_count", lang)}",
                        color = accent,
                        fontSize = 9f
                    )
                }
            }

            // Score Badge
            Surface(
                color = riskColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, riskColor.copy(alpha = 0.35f)),
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .cyberPulse(glowColor = riskColor, enabled = scan.securityScore < 50)
            ) {
                Text(
                    "${scan.securityScore}%",
                    color = riskColor,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }

            // Single Unified Save/Export Button
            Surface(
                onClick = onExport,
                shape = RoundedCornerShape(10.dp),
                color = accent.copy(alpha = if (isDark) 0.14f else 0.10f),
                border = BorderStroke(1.dp, accent.copy(alpha = if (isDark) 0.45f else 0.35f)),
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .springPress(pressedScale = 0.94f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.FileDownload,
                        contentDescription = LocalizedStrings.get("export_report", lang),
                        tint = accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        LocalizedStrings.get("export_report", lang),
                        color = if (isDark) Color.White else Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            // Delete Button with Telegram Dust Disintegration Trigger
            IconButton(
                onClick = { isDisintegrating = true },
                modifier = Modifier
                    .size(36.dp)
                    .springPress(pressedScale = 0.90f)
            ) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = LocalizedStrings.get("delete", lang),
                    tint = DangerNeon.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
    }
}

@Composable
fun ReportExportDialog(
    scan: ScanResult,
    initialFormat: String = "JSON",
    accent: Color,
    lang: String,
    isDark: Boolean,
    viewModel: ReportsViewModel = koinInject(),
    onFormatSelected: (String) -> Unit = { viewModel.onFormatChange(it) },
    onSaveAs: (String) -> Unit = { viewModel.saveScanAs(scan, it) },
    onQuickSave: (String) -> Unit = { viewModel.quickSaveScan(scan, it) },
    onShare: (String) -> Unit = { viewModel.shareScan(scan, it) },
    onDismiss: () -> Unit
) {
    var selectedFormat by remember { mutableStateOf(initialFormat) }
    val clipboardManager = LocalClipboardManager.current
    var isCopied by remember { mutableStateOf(false) }

    val exportContent = remember(scan, selectedFormat) {
        viewModel.getExportContent(scan, selectedFormat)
    }

    val lineCount = remember(exportContent) { exportContent.lines().size }
    val byteSize = remember(exportContent) {
        val bytes = exportContent.encodeToByteArray().size
        if (bytes < 1024) "$bytes B"
        else "${((bytes / 1024.0) * 10).roundToInt() / 10.0} KB"
    }

    val sanitizedTarget = scan.target.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    val extension = if (selectedFormat == "MD") "md" else selectedFormat.lowercase()
    val fileName = "PortX_Report_${sanitizedTarget}_${scan.timestamp}.$extension"

    val (gradeText, gradeColor) = when {
        scan.securityScore >= 90 -> "A+" to TertiaryNeon
        scan.securityScore >= 75 -> "A" to TertiaryNeon
        scan.securityScore >= 50 -> "B" to WarningNeon
        scan.securityScore >= 25 -> "C" to Color(0xFFF97316)
        else -> "F" to DangerNeon
    }

    val previewScrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(22.dp),
            color = if (isDark) SurfaceDark else SurfaceLight,
            border = BorderStroke(1.dp, Brush.linearGradient(listOf(accent.copy(alpha = 0.6f), SecondaryNeon.copy(alpha = 0.4f))))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.FileDownload, null, tint = accent, modifier = Modifier.size(22.dp))
                            Text(
                                LocalizedStrings.get("export_modal_title", lang),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isDark) Color.White else Color.Black
                            )
                        }
                        val isolatedTarget = "\u2066${scan.target}\u2069"
                        val subtitle = if (!scan.deviceName.isNullOrBlank()) "$isolatedTarget (${scan.deviceName})" else isolatedTarget
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = accent
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = gradeColor.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, gradeColor.copy(alpha = 0.4f))
                        ) {
                            Text(
                                "${scan.securityScore}% \u2022 ${LocalizedStrings.get("grade", lang)} $gradeText",
                                color = gradeColor,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.Close, contentDescription = LocalizedStrings.get("close", lang), tint = if (isDark) TextMuted else TextMutedLight)
                        }
                    }
                }

                HorizontalDivider(
                    color = if (isDark) GlassBorder else GlassBorderLight,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // Format Switcher Tabs
                Text(
                    LocalizedStrings.get("export_format_label", lang),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isDark) TextMuted else TextMutedLight,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                val exportFormats = listOf(
                    "JSON" to Icons.Default.DataObject,
                    "CSV" to Icons.Default.TableChart,
                    "MD" to Icons.Default.Description
                )
                val selectedIndex = exportFormats.indexOfFirst { it.first == selectedFormat }.coerceAtLeast(0)

                CyberSegmentedControl(
                    items = exportFormats.map { (fmt, icon) ->
                        val label = when (fmt) {
                            "MD" -> "Markdown (.md)"
                            "CSV" -> "CSV Table (.csv)"
                            else -> "JSON Schema (.json)"
                        }
                        label to icon
                    },
                    selectedIndex = selectedIndex,
                    onIndexSelected = { idx ->
                        val newFmt = exportFormats[idx].first
                        selectedFormat = newFmt
                        onFormatSelected(newFmt)
                    },
                    accent = accent,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                // Live Preview Box Header (Filename + Stats)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Visibility, null, tint = accent, modifier = Modifier.size(14.dp))
                        Text(
                            LocalizedStrings.get("preview", lang),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isDark) Color.White else Color.Black
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CyberBadge(text = fileName, color = SecondaryNeon, fontSize = 9f)
                        CyberBadge(text = "$byteSize \u2022 $lineCount ${LocalizedStrings.get("lines_count", lang)}", color = accent, fontSize = 9f)
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Live Preview Code Container (Bi-directional scroll for wide CSV/JSON)
                val previewHScrollState = rememberScrollState()
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF070B10) else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (isDark) GlassBorder else GlassBorderLight)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(previewScrollState)
                                .horizontalScroll(previewHScrollState)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = exportContent,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.5.sp,
                                    lineHeight = 16.sp
                                ),
                                color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
                            )
                        }

                        PortXScrollStateVerticalScrollbar(
                            scrollState = previewScrollState,
                            modifier = Modifier
                                .align(AbsoluteAlignment.CenterRight)
                                .fillMaxHeight()
                                .padding(2.dp)
                        )
                    }
                }

                HorizontalDivider(
                    color = if (isDark) GlassBorder else GlassBorderLight,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // Actions Matrix Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Save As... (JFileChooser on desktop)
                    Button(
                        onClick = {
                            onSaveAs(selectedFormat)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(Icons.Default.SaveAs, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            LocalizedStrings.get("save_as_file", lang),
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp
                        )
                    }

                    // Quick Save to Downloads
                    Button(
                        onClick = {
                            onQuickSave(selectedFormat)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryNeon),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(Icons.Default.Download, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            LocalizedStrings.get("quick_save_downloads", lang),
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp
                        )
                    }

                    // Copy to Clipboard
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(exportContent))
                            isCopied = true
                            viewModel.showSnackbar(LocalizedStrings.get("copied_to_clipboard", lang))
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isCopied) TertiaryNeon.copy(alpha = 0.15f) else Color.Transparent,
                            contentColor = if (isCopied) TertiaryNeon else accent
                        ),
                        border = BorderStroke(1.dp, if (isCopied) TertiaryNeon else accent.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            null,
                            tint = if (isCopied) TertiaryNeon else accent,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (isCopied) LocalizedStrings.get("copied", lang) else LocalizedStrings.get("copy_content", lang),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    // Native Share
                    IconButton(
                        onClick = {
                            onShare(selectedFormat)
                            onDismiss()
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, if (isDark) GlassBorder else GlassBorderLight, RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = LocalizedStrings.get("share_file_native", lang),
                            tint = SecondaryNeon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReportDetailDialog(
    scan: ScanResult,
    accent: Color,
    lang: String,
    onDismiss: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    val isRtl = lang == "fa" || lang == "ar"
    val listState = rememberLazyListState()
    var portFilterQuery by remember { mutableStateOf("") }
    var visiblePortLimit by remember { mutableStateOf(100) }

    val sortedPorts = remember(scan.openPorts) { scan.openPorts.distinct().sorted() }
    val filteredPorts = remember(sortedPorts, portFilterQuery) {
        if (portFilterQuery.isBlank()) sortedPorts
        else {
            val q = portFilterQuery.trim().lowercase()
            sortedPorts.filter { port ->
                val rawService = scan.portServices[port]?.trim()
                val resolvedService = getServiceTitle(port, if (rawService.isNullOrBlank() || rawService.equals("unknown", ignoreCase = true)) null else rawService)
                port.toString().contains(q) ||
                resolvedService.lowercase().contains(q) ||
                (rawService?.lowercase()?.contains(q) == true) ||
                (scan.portBanners[port]?.lowercase()?.contains(q) == true)
            }
        }
    }
    val displayedPorts = remember(filteredPorts, visiblePortLimit) {
        filteredPorts.take(visiblePortLimit)
    }

    val grade = when {
        scan.securityScore >= 90 -> "A+" to TertiaryNeon
        scan.securityScore >= 75 -> "A" to TertiaryNeon
        scan.securityScore >= 50 -> "B" to WarningNeon
        scan.securityScore >= 25 -> "C" to Color(0xFFF97316)
        else -> "F" to DangerNeon
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            color = if (isDark) SurfaceDark else SurfaceLight,
            border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            LocalizedStrings.get("report_detail_title", lang),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isDark) Color.White else Color.Black
                        )
                        val isolatedTarget = "\u2066${scan.target}\u2069"
                        val title = if (!scan.deviceName.isNullOrBlank()) "$isolatedTarget (${scan.deviceName})" else isolatedTarget
                        Text(
                            title,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = accent
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = grade.second.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, grade.second.copy(alpha = 0.4f))
                        ) {
                            Text(
                                "${LocalizedStrings.get("grade", lang)} ${grade.first}",
                                color = grade.second,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Close, contentDescription = LocalizedStrings.get("close", lang), tint = if (isDark) TextMuted else TextMutedLight)
                        }
                    }
                }

                HorizontalDivider(
                    color = if (isDark) GlassBorder else GlassBorderLight,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // Virtualized Scrollable Content
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(end = if (!isRtl) 10.dp else 0.dp, start = if (isRtl) 10.dp else 0.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // KPI Metric Tiles
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MetricTile(
                                    title = LocalizedStrings.get("security_score", lang),
                                    value = "${scan.securityScore}%",
                                    accent = grade.second,
                                    modifier = Modifier.weight(1f)
                                )
                                MetricTile(
                                    title = LocalizedStrings.get("threat_score", lang),
                                    value = "${100 - scan.securityScore}%",
                                    accent = DangerNeon,
                                    modifier = Modifier.weight(1f)
                                )
                                MetricTile(
                                    title = LocalizedStrings.get("open_ports_count", lang),
                                    value = "${scan.openPorts.size}",
                                    accent = accent,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // System Environment Info
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.02f),
                                border = BorderStroke(1.dp, if (isDark) GlassBorder else GlassBorderLight)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        LocalizedStrings.get("executive_summary", lang),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = accent
                                    )
                                    scan.deviceName?.let {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(LocalizedStrings.get("device_profile", lang), style = MaterialTheme.typography.bodySmall, color = if (isDark) TextMuted else TextMutedLight)
                                            Text(it, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = if (isDark) Color.White else Color.Black)
                                        }
                                    }
                                    scan.osFingerprint?.let {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(LocalizedStrings.get("os_fingerprint", lang), style = MaterialTheme.typography.bodySmall, color = if (isDark) TextMuted else TextMutedLight)
                                            Text(it, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = SecondaryNeon)
                                        }
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(LocalizedStrings.get("scan_protocol", lang), style = MaterialTheme.typography.bodySmall, color = if (isDark) TextMuted else TextMutedLight)
                                        Text("${scan.scanType ?: "TCP"} \u2022 ${scan.concurrentScans} conns", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = TertiaryNeon)
                                    }
                                }
                            }
                        }

                        // Open Ports List Header + Search Filter
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        LocalizedStrings.get("discovered_ports", lang),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isDark) Color.White else Color.Black
                                    )
                                    if (sortedPorts.isNotEmpty()) {
                                        Text(
                                            "${sortedPorts.size} ${LocalizedStrings.get("open_ports_count", lang)}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                            color = accent,
                                            modifier = Modifier
                                                .background(accent.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                if (sortedPorts.size > 15) {
                                    OutlinedTextField(
                                        value = portFilterQuery,
                                        onValueChange = { portFilterQuery = it },
                                        placeholder = {
                                            Text(
                                                LocalizedStrings.get("search_ports_in_report", lang),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isDark) TextMuted else TextMutedLight
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Search, null, tint = accent, modifier = Modifier.size(16.dp))
                                        },
                                        trailingIcon = {
                                            if (portFilterQuery.isNotEmpty()) {
                                                IconButton(onClick = { portFilterQuery = "" }, modifier = Modifier.size(24.dp)) {
                                                    Icon(Icons.Default.Close, null, tint = if (isDark) TextMuted else TextMutedLight, modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            unfocusedBorderColor = if (isDark) GlassBorder else GlassBorderLight,
                                            focusedBorderColor = accent,
                                            unfocusedTextColor = if (isDark) Color.White else Color.Black,
                                            focusedTextColor = if (isDark) Color.White else Color.Black
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                                    )
                                }
                            }
                        }

                        if (filteredPorts.isEmpty()) {
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = (if (scan.openPorts.isEmpty()) TertiaryNeon else DangerNeon).copy(alpha = 0.08f),
                                    border = BorderStroke(1.dp, (if (scan.openPorts.isEmpty()) TertiaryNeon else DangerNeon).copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        if (scan.openPorts.isEmpty()) LocalizedStrings.get("no_open_ports", lang)
                                        else LocalizedStrings.get("no_ports_found", lang),
                                        color = if (scan.openPorts.isEmpty()) TertiaryNeon else DangerNeon,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        } else {
                            items(displayedPorts, key = { it }) { port ->
                                val banner = scan.portBanners[port]
                                val rawService = scan.portServices[port]?.trim()
                                val service = getServiceTitle(port, if (rawService.isNullOrBlank() || rawService.equals("unknown", ignoreCase = true)) null else rawService)
                                val isHighRisk = port in listOf(21, 23, 445, 3389)

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isDark) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.02f),
                                    border = BorderStroke(1.dp, if (isHighRisk) DangerNeon.copy(alpha = 0.4f) else (if (isDark) GlassBorder else GlassBorderLight))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CyberBadge(
                                            text = "$port",
                                            color = if (isHighRisk) DangerNeon else accent
                                        )

                                        Spacer(Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                service,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (isDark) Color.White else Color.Black
                                            )
                                            if (!banner.isNullOrBlank()) {
                                                Text(
                                                    banner,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                                                    color = if (isDark) TextMuted else TextMutedLight,
                                                    maxLines = 2
                                                )
                                            }
                                        }

                                        if (isHighRisk) {
                                            CyberBadge(text = "CRITICAL", color = DangerNeon, fontSize = 8f)
                                        }
                                    }
                                }
                            }

                            if (filteredPorts.size > displayedPorts.size) {
                                item {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.03f),
                                        border = BorderStroke(1.dp, accent.copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val showingText = LocalizedStrings.get("showing_ports_count", lang)
                                                .replace("{shown}", "${displayedPorts.size}")
                                                .replace("{total}", "${filteredPorts.size}")
                                            Text(
                                                showingText,
                                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                                color = if (isDark) TextMuted else TextMutedLight
                                            )

                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                TextButton(
                                                    onClick = { visiblePortLimit += 250 },
                                                    colors = ButtonDefaults.textButtonColors(contentColor = accent)
                                                ) {
                                                    Text("+250", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                }
                                                Button(
                                                    onClick = { visiblePortLimit = filteredPorts.size },
                                                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        LocalizedStrings.get("filter_all", lang),
                                                        color = Color.Black,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    PortXVerticalScrollbar(
                        listState = listState,
                        modifier = Modifier
                            .align(if (isRtl) AbsoluteAlignment.CenterLeft else AbsoluteAlignment.CenterRight)
                            .fillMaxHeight()
                            .padding(horizontal = 2.dp)
                    )
                }

                HorizontalDivider(
                    color = if (isDark) GlassBorder else GlassBorderLight,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // Action Footer with Unified Export Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onExport,
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FileDownload, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(LocalizedStrings.get("export_report", lang), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerNeon),
                        border = BorderStroke(1.dp, DangerNeon.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, null, tint = DangerNeon, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
