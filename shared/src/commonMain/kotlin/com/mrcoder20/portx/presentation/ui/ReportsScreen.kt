package com.mrcoder20.portx.presentation.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrcoder20.portx.domain.LocalizedStrings
import com.mrcoder20.portx.domain.model.ScanResult
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
    val lang = appSettings.language
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var filterQuery by remember { mutableStateOf("") }
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
            Spacer(modifier = Modifier.height(32.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            LocalizedStrings.get("reports", lang).uppercase(), 
                            style = MaterialTheme.typography.labelMedium.copy(color = if (appSettings.theme == "DARK") TextMuted else TextMutedLight, fontWeight = FontWeight.Bold),
                        )
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All Reports", tint = DangerNeon.copy(alpha = 0.7f))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(LocalizedStrings.get("security_trend", lang), style = MaterialTheme.typography.bodySmall, color = if (appSettings.theme == "DARK") TextMuted else TextMutedLight)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    ScanScoreTrendChart(
                        scans = state.scans,
                        accent = accent,
                        modifier = Modifier.fillMaxWidth().height(120.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    if (state.scans.isNotEmpty()) {
                        OutlinedTextField(
                            value = filterQuery,
                            onValueChange = { filterQuery = it },
                            placeholder = { Text(LocalizedStrings.get("search_reports", lang), color = if (appSettings.theme == "DARK") TextMuted else TextMutedLight, fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = accent, modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                if (filterQuery.isNotEmpty()) {
                                    IconButton(onClick = { filterQuery = "" }, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Default.Clear, null, tint = if (appSettings.theme == "DARK") TextMuted else TextMutedLight, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accent,
                                unfocusedBorderColor = (if (appSettings.theme == "DARK") GlassBorder else GlassBorderLight).copy(alpha = 0.4f),
                                focusedTextColor = if (appSettings.theme == "DARK") Color.White else Color.Black,
                                unfocusedTextColor = if (appSettings.theme == "DARK") Color.White else Color.Black
                            ),
                            textStyle = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.ContentOrLtr)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (state.isLoading) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = accent)
                        }
                    } else if (state.scans.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(LocalizedStrings.get("no_reports", lang), color = TextMuted, fontSize = 12.sp)
                        }
                    } else if (filteredScans.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(LocalizedStrings.get("no_matching_reports", lang), color = TextMuted, fontSize = 12.sp)
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
                                        start = if (isRtl) 14.dp else 0.dp,
                                        end = if (!isRtl) 14.dp else 0.dp
                                    ),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredScans, key = { it.id ?: it.timestamp }) { scan ->
                                    ReportHistoryItem(
                                        scan = scan,
                                        accent = accent,
                                        lang = lang,
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

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                LocalizedStrings.get("global_export_format", lang), 
                style = MaterialTheme.typography.labelMedium.copy(color = if (appSettings.theme == "DARK") TextMuted else TextMutedLight, fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val formats = listOf("MD", "CSV", "JSON")
                formats.forEach { format ->
                    GlassExportButton(
                        label = if(format == "MD") "Markdown" else format, 
                        isSelected = state.exportFormat == format,
                        accent = accent,
                        onClick = { viewModel.onFormatChange(format) },
                        modifier = Modifier.weight(1f)
                    )
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

    if (showDeleteConfirm) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val isDark = LocalAppSettings.current.theme == "DARK"
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                containerColor = if (isDark) SurfaceDark else SurfaceLight,
                titleContentColor = if (isDark) Color.White else Color.Black,
                textContentColor = if (isDark) TextSecondary else TextSecondaryLight,
                title = { Text(LocalizedStrings.get("clear_history_title", lang)) },
                text = { Text(LocalizedStrings.get("clear_history_desc", lang)) },
                confirmButton = {
                    TextButton(onClick = { 
                        viewModel.clearAll()
                        showDeleteConfirm = false
                    }) {
                        Text(LocalizedStrings.get("clear_all", lang), color = if (isDark) DangerNeon else DangerLight)
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
}

@Composable
fun ScanScoreTrendChart(scans: List<ScanResult>, accent: Color, modifier: Modifier = Modifier) {
    val scores = scans.sortedBy { it.timestamp }.map { it.securityScore.toFloat() / 100f }.takeLast(10)
    if (scores.size < 2) {
        val lang = LocalAppSettings.current.language
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(LocalizedStrings.get("not_enough_data", lang), color = TextMuted, fontSize = 10.sp)
        }
        return
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val spacing = width / (scores.size - 1)
        val vPadding = 12.dp.toPx()
        val graphHeight = (height - vPadding * 2).coerceAtLeast(1f)
        
        // Subtle baseline grid lines
        val y50 = vPadding + graphHeight * 0.5f
        val y80 = vPadding + graphHeight * 0.2f
        drawLine(
            color = Color.White.copy(alpha = 0.05f),
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
                radius = 7.dp.toPx(),
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
            .heightIn(min = 90.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, if (isDark) GlassBorder else GlassBorderLight, RoundedCornerShape(20.dp)),
        color = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                val isolatedTarget = "\u2066${scan.target}\u2069"
                val title = if (!scan.deviceName.isNullOrBlank()) "$isolatedTarget (${scan.deviceName})" else "${LocalizedStrings.get("target", lang)}: $isolatedTarget"
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDark) Color.White else Color.Black,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    // Security Risk Pill
                    Surface(
                        color = riskColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, riskColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = riskLabel,
                            color = riskColor,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 9.sp),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
                
                // Metadata row: Date/Time + OS Fingerprint + Open Ports Badge
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val isolatedDateTime = "\u2066$formattedDate $formattedTime\u2069"
                    Text(
                        text = if (!scan.osFingerprint.isNullOrBlank()) "$isolatedDateTime • ${scan.osFingerprint}" else isolatedDateTime,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = if (isDark) TextMuted else TextMutedLight,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    // Open Ports Count Badge
                    Surface(
                        color = accent.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(0.5.dp, accent.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "${scan.openPorts.size} ${LocalizedStrings.get("open_ports_count", lang)}",
                            color = accent,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
            }
            
            // Score Display
            Surface(
                color = riskColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, riskColor.copy(alpha = 0.3f)),
                modifier = Modifier.padding(horizontal = 6.dp)
            ) {
                Text(
                    "${scan.securityScore}%", 
                    color = riskColor,
                    fontWeight = FontWeight.Black,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }

            IconButton(
                onClick = onDownload,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = "Download Report", tint = accent, modifier = Modifier.size(18.dp))
            }

            IconButton(
                onClick = onExport,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Export Report", tint = SecondaryNeon, modifier = Modifier.size(18.dp))
            }
            
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Report", tint = if (isDark) TextMuted else TextMutedLight, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun GlassExportButton(
    label: String, 
    isSelected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, if (isSelected) accent else (if (isDark) GlassBorder else GlassBorderLight), RoundedCornerShape(12.dp)),
        color = if (isSelected) accent.copy(alpha = 0.15f) else (if (isDark) GlassBackground else GlassLight)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label, 
                style = MaterialTheme.typography.labelLarge, 
                color = if (isSelected) (if (isDark) Color.White else Color.Black) else (if (isDark) TextMuted else TextMutedLight)
            )
        }
    }
}
