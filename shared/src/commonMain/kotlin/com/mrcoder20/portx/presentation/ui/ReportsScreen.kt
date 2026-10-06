package com.mrcoder20.portx.presentation.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mrcoder20.portx.domain.LocalizedStrings
import com.mrcoder20.portx.domain.model.ScanResult
import com.mrcoder20.portx.presentation.ui.components.PortXScrollStateVerticalScrollbar
import com.mrcoder20.portx.presentation.ui.components.PortXVerticalScrollbar
import com.mrcoder20.portx.presentation.ui.theme.*
import com.mrcoder20.portx.presentation.viewmodel.ReportsViewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject

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
                                modifier = Modifier.size(38.dp)
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

                    // Security Trend Chart
                    ScanScoreTrendSection(
                        scans = state.scans,
                        accent = accent,
                        lang = lang,
                        isDark = isDark,
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
                        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            LazyColumn(
                                state = reportsListState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(
                                        start = if (isRtl) 12.dp else 0.dp,
                                        end = if (!isRtl) 12.dp else 0.dp
                                    ),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filteredScans, key = { it.id ?: it.timestamp }) { scan ->
                                    ReportHistoryItem(
                                        scan = scan,
                                        accent = accent,
                                        lang = lang,
                                        onClick = { selectedReportForDetail = scan },
                                        onDelete = { scan.id?.let { viewModel.deleteScan(it) } },
                                        onExport = { viewModel.shareScan(scan) },
                                        onDownload = { viewModel.downloadScan(scan) }
                                    )
                                }
                            }
                            PortXVerticalScrollbar(
                                listState = reportsListState,
                                modifier = Modifier
                                    .align(if (isRtl) AbsoluteAlignment.CenterLeft else AbsoluteAlignment.CenterRight)
                                    .fillMaxHeight()
                            )
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
            onExport = { viewModel.shareScan(scan) },
            onDownload = { viewModel.downloadScan(scan) },
            onDelete = {
                scan.id?.let { viewModel.deleteScan(it) }
                selectedReportForDetail = null
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm) {
        val isDark = LocalAppSettings.current.theme == "DARK"
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = if (isDark) SurfaceDark else SurfaceLight,
            titleContentColor = if (isDark) Color.White else Color.Black,
            textContentColor = if (isDark) TextSecondary else TextSecondaryLight,
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
                        viewModel.clearAll()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerNeon),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(LocalizedStrings.get("clear_all", lang), color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(LocalizedStrings.get("cancel", lang), color = if (isDark) Color.White else Color.Black)
                }
            },
            modifier = Modifier.padding(24.dp)
        )
    }
}

@Composable
fun ScanScoreTrendSection(
    scans: List<ScanResult>,
    accent: Color,
    lang: String,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val scores = remember(scans) {
        scans.sortedBy { it.timestamp }.map { it.securityScore }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.02f),
        border = BorderStroke(1.dp, if (isDark) GlassBorder else GlassBorderLight)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    LocalizedStrings.get("security_trend", lang),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isDark) Color.White else Color.Black
                )

                if (scores.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val avg = kotlin.math.round(scores.average()).toInt()
                        CyberBadge(text = "AVG $avg%", color = accent, fontSize = 9f)
                        val latest = scores.last()
                        CyberBadge(text = "LATEST $latest%", color = SecondaryNeon, fontSize = 9f)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            ScanScoreTrendChart(
                scans = scans,
                accent = accent,
                modifier = Modifier.fillMaxWidth().height(100.dp)
            )
        }
    }
}

@Composable
fun ScanScoreTrendChart(scans: List<ScanResult>, accent: Color, modifier: Modifier = Modifier) {
    val scores = scans.sortedBy { it.timestamp }.map { it.securityScore.toFloat() / 100f }.takeLast(12)
    if (scores.size < 2) {
        val lang = LocalAppSettings.current.language
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(LocalizedStrings.get("not_enough_data", lang), color = TextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
        return
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val spacing = width / (scores.size - 1)
        val vPadding = 10.dp.toPx()
        val graphHeight = (height - vPadding * 2).coerceAtLeast(1f)

        // Baseline grid lines at 50% and 80%
        val y50 = vPadding + graphHeight * 0.5f
        val y80 = vPadding + graphHeight * 0.2f
        drawLine(
            color = Color.White.copy(alpha = 0.06f),
            start = Offset(0f, y50),
            end = Offset(width, y50),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = Color.White.copy(alpha = 0.08f),
            start = Offset(0f, y80),
            end = Offset(width, y80),
            strokeWidth = 1.dp.toPx()
        )

        val points = scores.mapIndexed { index, score ->
            Offset(index * spacing, vPadding + graphHeight * (1 - score))
        }

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
                colors = listOf(accent.copy(alpha = 0.25f), Color.Transparent)
            )
        )

        // Curve Stroke
        drawPath(
            path = path,
            brush = Brush.horizontalGradient(listOf(SecondaryNeon, accent)),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Glowing Vertex Points
        points.forEach { pt ->
            drawCircle(
                color = accent.copy(alpha = 0.35f),
                radius = 6.dp.toPx(),
                center = pt
            )
            drawCircle(
                color = Color.White,
                radius = 2.5.dp.toPx(),
                center = pt
            )
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
    onExport: () -> Unit,
    onDownload: () -> Unit
) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    val (formattedDate, formattedTime) = try {
        val dt = kotlin.time.Instant.fromEpochMilliseconds(scan.timestamp)
            .toLocalDateTime(TimeZone.currentSystemDefault())
        val d = dt.date.toString()
        val t = "${dt.time.hour.toString().padStart(2, '0')}:${dt.time.minute.toString().padStart(2, '0')}"
        d to t
    } catch (_: Exception) {
        "N/A" to "N/A"
    }

    val (riskLabel, riskColor) = when {
        scan.securityScore >= 80 -> LocalizedStrings.get("secure", lang) to TertiaryNeon
        scan.securityScore >= 50 -> LocalizedStrings.get("warning", lang) to WarningNeon
        else -> LocalizedStrings.get("high_risk", lang) to DangerNeon
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 86.dp)
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
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    CyberBadge(text = riskLabel, color = riskColor, fontSize = 9f)
                }

                Spacer(Modifier.height(4.dp))

                // Metadata row: Date/Time + OS Fingerprint + Open Ports Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isolatedDateTime = "\u2066$formattedDate $formattedTime\u2069"
                    Text(
                        text = if (!scan.osFingerprint.isNullOrBlank()) "$isolatedDateTime • ${scan.osFingerprint}" else isolatedDateTime,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = if (isDark) TextMuted else TextMutedLight,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
                modifier = Modifier.padding(horizontal = 6.dp)
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

            IconButton(onClick = onDownload, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Download, contentDescription = "Download Report", tint = accent, modifier = Modifier.size(18.dp))
            }

            IconButton(onClick = onExport, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Share, contentDescription = "Export Report", tint = SecondaryNeon, modifier = Modifier.size(18.dp))
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Report", tint = if (isDark) TextMuted else TextMutedLight, modifier = Modifier.size(18.dp))
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
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    val isRtl = lang == "fa" || lang == "ar"
    val scrollState = rememberScrollState()

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
                        Text(
                            scan.target,
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
                                "GRADE ${grade.first}",
                                color = grade.second,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = if (isDark) TextMuted else TextMutedLight)
                        }
                    }
                }

                HorizontalDivider(
                    color = if (isDark) GlassBorder else GlassBorderLight,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // Scrollable Content
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(end = if (!isRtl) 10.dp else 0.dp, start = if (isRtl) 10.dp else 0.dp)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // KPI Metric Tiles
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

                        // System Environment Info
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
                                        Text("OS Fingerprint", style = MaterialTheme.typography.bodySmall, color = if (isDark) TextMuted else TextMutedLight)
                                        Text(it, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = SecondaryNeon)
                                    }
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Scan Protocol", style = MaterialTheme.typography.bodySmall, color = if (isDark) TextMuted else TextMutedLight)
                                    Text("${scan.scanType ?: "TCP"} • ${scan.concurrentScans} conns", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = TertiaryNeon)
                                }
                            }
                        }

                        // Open Ports List
                        Text(
                            LocalizedStrings.get("discovered_ports", lang),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isDark) Color.White else Color.Black
                        )

                        if (scan.openPorts.isEmpty()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = TertiaryNeon.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, TertiaryNeon.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    "No open ports detected. Perimeter is fully sealed.",
                                    color = TertiaryNeon,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        } else {
                            scan.openPorts.sorted().forEach { port ->
                                val banner = scan.portBanners[port]
                                val service = scan.portServices[port] ?: "Service"
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
                        }
                    }

                    PortXScrollStateVerticalScrollbar(
                        scrollState = scrollState,
                        modifier = Modifier
                            .align(if (isRtl) AbsoluteAlignment.CenterLeft else AbsoluteAlignment.CenterRight)
                            .fillMaxHeight()
                    )
                }

                HorizontalDivider(
                    color = if (isDark) GlassBorder else GlassBorderLight,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // Action Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onDownload,
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Download, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Download", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = onExport,
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryNeon),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Share", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
