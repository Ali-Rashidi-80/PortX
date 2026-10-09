package com.mrcoder20.portx.presentation.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.mrcoder20.portx.domain.PortIntelligence
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mrcoder20.portx.domain.LocalizedStrings
import com.mrcoder20.portx.domain.model.ScanResult
import com.mrcoder20.portx.presentation.ui.components.DisintegrationContainer
import com.mrcoder20.portx.presentation.ui.components.LaunchedAutoScrollHint
import com.mrcoder20.portx.presentation.ui.components.LocalTouchEmulation
import com.mrcoder20.portx.presentation.ui.components.PortXScrollStateVerticalScrollbar
import com.mrcoder20.portx.presentation.ui.components.PortXVerticalScrollbar
import com.mrcoder20.portx.presentation.ui.components.cyberPulse
import com.mrcoder20.portx.presentation.ui.components.horizontalFadingEdges
import com.mrcoder20.portx.presentation.ui.components.springPress
import com.mrcoder20.portx.presentation.ui.components.touchDragScroll
import com.mrcoder20.portx.presentation.ui.theme.*
import com.mrcoder20.portx.domain.DateFormatter
import com.mrcoder20.portx.presentation.viewmodel.ReportsViewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject
import kotlin.math.roundToInt
import androidx.compose.ui.zIndex
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.mrcoder20.portx.presentation.ui.components.LocalTouchEmulation

@Composable
fun ReportsScreen(viewModel: ReportsViewModel = koinInject()) {
    val state by viewModel.uiState.collectAsState()
    val appSettings = LocalAppSettings.current
    val accent = LocalAccentColor.current
    val isDark = appSettings.theme == "DARK"
    val lang = appSettings.language

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var filterQuery by remember { mutableStateOf("") }
    var selectedReportCategory by remember { mutableStateOf("ALL") }
    var selectedReportForDetail by remember { mutableStateOf<ScanResult?>(null) }
    var selectedReportForExport by remember { mutableStateOf<ScanResult?>(null) }
    var deletingScanId by remember { mutableStateOf<Long?>(null) }
    var isClearingAllWithDust by remember { mutableStateOf(false) }
    var pendingDeletedScanIds by remember { mutableStateOf(setOf<Long>()) }

    val activeScans = remember(state.scans, pendingDeletedScanIds) {
        if (pendingDeletedScanIds.isEmpty()) state.scans else state.scans.filter { it.id !in pendingDeletedScanIds }
    }

    val criticalScansCount = remember(activeScans) { activeScans.count { it.securityScore < 50 } }
    val moderateScansCount = remember(activeScans) { activeScans.count { it.securityScore in 50..74 } }
    val secureScansCount = remember(activeScans) { activeScans.count { it.securityScore >= 75 } }

    val filteredScans = remember(activeScans, filterQuery, selectedReportCategory) {
        activeScans.filter { scan ->
            val matchesCategory = when (selectedReportCategory) {
                "CRITICAL" -> scan.securityScore < 50
                "MODERATE" -> scan.securityScore in 50..74
                "SECURE" -> scan.securityScore >= 75
                else -> true
            }
            if (!matchesCategory) return@filter false

            if (filterQuery.isBlank()) true
            else {
                val q = filterQuery.trim().lowercase()
                scan.target.lowercase().contains(q) ||
                (scan.deviceName?.lowercase()?.contains(q) == true) ||
                (scan.osFingerprint?.lowercase()?.contains(q) == true)
            }
        }
    }

    LaunchedEffect(isClearingAllWithDust) {
        if (isClearingAllWithDust) {
            kotlinx.coroutines.delay(1850)
            pendingDeletedScanIds = filteredScans.mapNotNull { it.id }.toSet()
            viewModel.clearAll()
            isClearingAllWithDust = false
        }
    }



    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isMobile = maxWidth < 640.dp

        if (isMobile) {
            val reportsListState = rememberLazyListState()
            val isRtl = lang == "fa"

            LazyColumn(
                state = reportsListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .touchDragScroll(reportsListState, isVertical = true),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(28.dp))
                }

                // Mobile Header Bar
                item {
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
                                    contentDescription = LocalizedStrings.get("clear_all", lang),
                                    tint = DangerNeon.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Security Trend Chart Section
                item {
                    ScanScoreTrendSection(
                        scans = state.scans,
                        accent = accent,
                        lang = lang,
                        isDark = isDark,
                        onScanClick = { selectedReportForDetail = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Search & Filter Box
                if (state.scans.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                modifier = Modifier.fillMaxWidth().height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDark) Color.White.copy(alpha = 0.04f) else SurfaceInsetLight,
                                border = BorderStroke(1.dp, if (filterQuery.isNotEmpty()) accent else (if (isDark) GlassBorder else GlassBorderLight).copy(alpha = 0.6f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null,
                                        tint = accent,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    BasicTextField(
                                        value = filterQuery,
                                        onValueChange = { filterQuery = it },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodySmall.copy(
                                            color = if (isDark) Color.White else Color.Black,
                                            fontSize = 11.5.sp,
                                            textDirection = TextDirection.ContentOrRtl
                                        ),
                                        cursorBrush = SolidColor(accent),
                                        modifier = Modifier.weight(1f),
                                        decorationBox = { innerTextField ->
                                            Box(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                if (filterQuery.isEmpty()) {
                                                    Text(
                                                        LocalizedStrings.get("search_reports", lang),
                                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                        color = if (isDark) TextMuted else TextMutedLight,
                                                        maxLines = 1
                                                    )
                                                }
                                                innerTextField()
                                            }
                                        }
                                    )
                                    if (filterQuery.isNotEmpty()) {
                                        IconButton(
                                            onClick = { filterQuery = "" },
                                            modifier = Modifier.size(24.dp).springPress()
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = LocalizedStrings.get("clear", lang),
                                                tint = if (isDark) TextMuted else TextMutedLight,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Reports Category Filter Buttons / Chips
                            val reportsChipsScrollState = rememberScrollState()
                            LaunchedAutoScrollHint(reportsChipsScrollState)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(28.dp)
                                    .horizontalFadingEdges(reportsChipsScrollState, fadeWidth = 14.dp, isRtl = isRtl)
                                    .touchDragScroll(reportsChipsScrollState, isVertical = false, isRtl = isRtl)
                                    .horizontalScroll(reportsChipsScrollState),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChipMini(
                                    label = LocalizedStrings.get("filter_all", lang),
                                    count = activeScans.size,
                                    isSelected = selectedReportCategory == "ALL",
                                    accent = accent,
                                    onClick = { selectedReportCategory = "ALL" }
                                )
                                if (criticalScansCount > 0) {
                                    FilterChipMini(
                                        label = LocalizedStrings.get("high_risk", lang),
                                        count = criticalScansCount,
                                        isSelected = selectedReportCategory == "CRITICAL",
                                        accent = accent,
                                        isDanger = true,
                                        onClick = { selectedReportCategory = if (selectedReportCategory == "CRITICAL") "ALL" else "CRITICAL" }
                                    )
                                }
                                if (moderateScansCount > 0) {
                                    FilterChipMini(
                                        label = LocalizedStrings.get("warning", lang),
                                        count = moderateScansCount,
                                        isSelected = selectedReportCategory == "MODERATE",
                                        accent = WarningNeon,
                                        onClick = { selectedReportCategory = if (selectedReportCategory == "MODERATE") "ALL" else "MODERATE" }
                                    )
                                }
                                if (secureScansCount > 0) {
                                    FilterChipMini(
                                        label = LocalizedStrings.get("secure", lang),
                                        count = secureScansCount,
                                        isSelected = selectedReportCategory == "SECURE",
                                        accent = TertiaryNeon,
                                        onClick = { selectedReportCategory = if (selectedReportCategory == "SECURE") "ALL" else "SECURE" }
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                            }
                        }
                    }
                }

                // Content States
                if (state.isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = accent)
                        }
                    }
                } else if (state.scans.isEmpty() && !isClearingAllWithDust) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth().height(180.dp)) {
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
                        }
                    }
                } else if (filteredScans.isEmpty() && !isClearingAllWithDust) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        LocalizedStrings.get("no_matching_reports", lang),
                                        color = if (isDark) TextMuted else TextMutedLight,
                                        fontSize = 12.sp
                                    )
                                    if (selectedReportCategory != "ALL" || filterQuery.isNotEmpty()) {
                                        TextButton(
                                            onClick = {
                                                selectedReportCategory = "ALL"
                                                filterQuery = ""
                                            }
                                        ) {
                                            Text(
                                                LocalizedStrings.get("clear_filter", lang),
                                                color = accent,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(filteredScans, key = { it.id ?: it.timestamp }) { scan ->
                        val isDeletingThis = deletingScanId == scan.id || isClearingAllWithDust

                        val itemMaterialColors = remember(scan.securityScore, isDark, accent) {
                            val statusColor = when {
                                scan.securityScore >= 80 -> TertiaryNeon
                                scan.securityScore >= 50 -> WarningNeon
                                else -> DangerNeon
                            }
                            if (isDark) {
                                listOf(
                                    Color(0xFFCBD5E1), Color(0xFF94A3B8), Color(0xFF64748B),
                                    Color(0xFFE2E8F0), Color(0xFF475569), Color(0xFFF1F5F9),
                                    Color.White, statusColor, statusColor.copy(alpha = 0.85f),
                                    accent, Color(0xFFFF7043), Color(0xFFFFCA28)
                                )
                            } else {
                                listOf(
                                    Color(0xFF475569), Color(0xFF1E293B), Color(0xFF64748B),
                                    Color(0xFF334155), Color(0xFF0F172A), statusColor,
                                    accent, Color(0xFFFF5722)
                                )
                            }
                        }

                        DisintegrationContainer(
                            isDisintegrating = isDeletingThis,
                            accent = DangerNeon,
                            materialColors = itemMaterialColors,
                            particleCount = 2200,
                            collapseHeight = true,
                            durationMs = 1800,
                            onDisintegrated = {
                                if (!isClearingAllWithDust) {
                                    scan.id?.let { id ->
                                        pendingDeletedScanIds = pendingDeletedScanIds + id
                                        viewModel.deleteScan(id)
                                    }
                                    if (deletingScanId == scan.id) {
                                        deletingScanId = null
                                    }
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
            }
        } else {
            // Desktop High-Density Layout with Fluid Collapsing Hero Trend Chart
            val reportsListState = rememberLazyListState()
            val isRtl = lang == "fa"

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
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = reportsListState,
                            modifier = Modifier
                                .fillMaxSize()
                                .touchDragScroll(reportsListState, isVertical = true)
                                .absolutePadding(
                                    right = if (isRtl) 12.dp else 0.dp,
                                    left = if (!isRtl) 12.dp else 0.dp
                                ),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // 1. Header Bar (Slides up & gracefully fades out on scroll)
                            item(key = "desktop_header_bar") {
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
                                                contentDescription = LocalizedStrings.get("clear_all", lang),
                                                tint = DangerNeon.copy(alpha = 0.8f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // 2. Security Trend Chart (Interactive, dynamic, slides up on scroll)
                            if (state.scans.isNotEmpty()) {
                                item(key = "desktop_trend_chart") {
                                    ScanScoreTrendSection(
                                        scans = state.scans,
                                        accent = accent,
                                        lang = lang,
                                        isDark = isDark,
                                        onScanClick = { selectedReportForDetail = it },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                // 3. Search & Filter Box
                                item(key = "desktop_search_filter") {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth().height(38.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isDark) Color.White.copy(alpha = 0.04f) else SurfaceInsetLight,
                                            border = BorderStroke(1.dp, if (filterQuery.isNotEmpty()) accent else (if (isDark) GlassBorder else GlassBorderLight).copy(alpha = 0.6f))
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.Search,
                                                    contentDescription = null,
                                                    tint = accent,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                BasicTextField(
                                                    value = filterQuery,
                                                    onValueChange = { filterQuery = it },
                                                    singleLine = true,
                                                    textStyle = MaterialTheme.typography.bodySmall.copy(
                                                        color = if (isDark) Color.White else Color.Black,
                                                        fontSize = 11.5.sp,
                                                        textDirection = TextDirection.ContentOrRtl
                                                    ),
                                                    cursorBrush = SolidColor(accent),
                                                    modifier = Modifier.weight(1f),
                                                    decorationBox = { innerTextField ->
                                                        Box(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            contentAlignment = Alignment.CenterStart
                                                        ) {
                                                            if (filterQuery.isEmpty()) {
                                                                Text(
                                                                    LocalizedStrings.get("search_reports", lang),
                                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                                    color = if (isDark) TextMuted else TextMutedLight,
                                                                    maxLines = 1
                                                                )
                                                            }
                                                            innerTextField()
                                                        }
                                                    }
                                                )
                                                if (filterQuery.isNotEmpty()) {
                                                    IconButton(
                                                        onClick = { filterQuery = "" },
                                                        modifier = Modifier.size(24.dp).springPress()
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Close,
                                                            contentDescription = LocalizedStrings.get("clear", lang),
                                                            tint = if (isDark) TextMuted else TextMutedLight,
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Reports Category Filter Buttons / Chips
                                        val reportsChipsScrollState = rememberScrollState()
                                        LaunchedAutoScrollHint(reportsChipsScrollState)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(28.dp)
                                                .horizontalFadingEdges(reportsChipsScrollState, fadeWidth = 14.dp, isRtl = isRtl)
                                                .touchDragScroll(reportsChipsScrollState, isVertical = false, isRtl = isRtl)
                                                .horizontalScroll(reportsChipsScrollState),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            FilterChipMini(
                                                label = LocalizedStrings.get("filter_all", lang),
                                                count = activeScans.size,
                                                isSelected = selectedReportCategory == "ALL",
                                                accent = accent,
                                                onClick = { selectedReportCategory = "ALL" }
                                            )
                                            if (criticalScansCount > 0) {
                                                FilterChipMini(
                                                    label = LocalizedStrings.get("high_risk", lang),
                                                    count = criticalScansCount,
                                                    isSelected = selectedReportCategory == "CRITICAL",
                                                    accent = accent,
                                                    isDanger = true,
                                                    onClick = { selectedReportCategory = if (selectedReportCategory == "CRITICAL") "ALL" else "CRITICAL" }
                                                )
                                            }
                                            if (moderateScansCount > 0) {
                                                FilterChipMini(
                                                    label = LocalizedStrings.get("warning", lang),
                                                    count = moderateScansCount,
                                                    isSelected = selectedReportCategory == "MODERATE",
                                                    accent = WarningNeon,
                                                    onClick = { selectedReportCategory = if (selectedReportCategory == "MODERATE") "ALL" else "MODERATE" }
                                                )
                                            }
                                            if (secureScansCount > 0) {
                                                FilterChipMini(
                                                    label = LocalizedStrings.get("secure", lang),
                                                    count = secureScansCount,
                                                    isSelected = selectedReportCategory == "SECURE",
                                                    accent = TertiaryNeon,
                                                    onClick = { selectedReportCategory = if (selectedReportCategory == "SECURE") "ALL" else "SECURE" }
                                                )
                                            }
                                            Spacer(Modifier.width(12.dp))
                                        }
                                    }
                                }
                            }

                            // Content State inside LazyColumn
                            if (state.isLoading) {
                                item(key = "loading_indicator") {
                                    Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = accent)
                                    }
                                }
                            } else if (state.scans.isEmpty() && !isClearingAllWithDust) {
                                item(key = "empty_scans_card") {
                                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
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
                                }
                            } else if (filteredScans.isEmpty() && !isClearingAllWithDust) {
                                item(key = "empty_filtered_card") {
                                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                LocalizedStrings.get("no_matching_reports", lang),
                                                color = if (isDark) TextMuted else TextMutedLight,
                                                fontSize = 12.sp
                                            )
                                            if (selectedReportCategory != "ALL" || filterQuery.isNotEmpty()) {
                                                TextButton(
                                                    onClick = {
                                                        selectedReportCategory = "ALL"
                                                        filterQuery = ""
                                                    }
                                                ) {
                                                    Text(
                                                        LocalizedStrings.get("clear_filter", lang),
                                                        color = accent,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                items(filteredScans, key = { it.id ?: it.timestamp }) { scan ->
                                    val isDeletingThis = deletingScanId == scan.id || isClearingAllWithDust

                                    val itemMaterialColors = remember(scan.securityScore, isDark, accent) {
                                        val statusColor = when {
                                            scan.securityScore >= 80 -> TertiaryNeon
                                            scan.securityScore >= 50 -> WarningNeon
                                            else -> DangerNeon
                                        }
                                        if (isDark) {
                                            listOf(
                                                Color(0xFFCBD5E1), // Fine silver ash powder
                                                Color(0xFF94A3B8), // Light slate dust
                                                Color(0xFF64748B), // Slate 500
                                                Color(0xFFE2E8F0), // Titanium ash
                                                Color(0xFF475569), // Slate 600 charcoal
                                                Color(0xFFF1F5F9), // Typography off-white
                                                Color.White,       // Header white
                                                statusColor,       // Security score badge color
                                                statusColor.copy(alpha = 0.85f),
                                                accent,            // Cyber badge accent
                                                Color(0xFFFF7043), // Hot burning ember
                                                Color(0xFFFFCA28)  // Incandescent gold spark
                                            )
                                        } else {
                                            listOf(
                                                Color(0xFF475569),
                                                Color(0xFF1E293B),
                                                Color(0xFF64748B),
                                                Color(0xFF334155),
                                                Color(0xFF0F172A),
                                                statusColor,
                                                accent,
                                                Color(0xFFFF5722)
                                            )
                                        }
                                    }

                                    DisintegrationContainer(
                                        isDisintegrating = isDeletingThis,
                                        accent = DangerNeon,
                                        materialColors = itemMaterialColors,
                                        particleCount = 2200,
                                        collapseHeight = true,
                                        durationMs = 1800,
                                        onDisintegrated = {
                                            if (!isClearingAllWithDust) {
                                                scan.id?.let { id ->
                                                    pendingDeletedScanIds = pendingDeletedScanIds + id
                                                    viewModel.deleteScan(id)
                                                }
                                                if (deletingScanId == scan.id) {
                                                    deletingScanId = null
                                                }
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
                        }

                        PortXVerticalScrollbar(
                            listState = reportsListState,
                            modifier = Modifier
                                .align(if (isRtl) AbsoluteAlignment.CenterRight else AbsoluteAlignment.CenterLeft)
                                .fillMaxHeight()
                                .padding(horizontal = 2.dp)
                        )
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
                        "MD" -> LocalizedStrings.get("format_markdown", lang)
                        "CSV" -> LocalizedStrings.get("format_csv", lang)
                        else -> LocalizedStrings.get("format_json", lang)
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

    // Detail Modal Dialog
    selectedReportForDetail?.let { scan ->
        ReportDetailDialog(
            scan = scan,
            accent = accent,
            lang = lang,
            isMobile = isMobile,
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
            isMobile = isMobile,
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
        modifier = modifier.then(
            if (isDark) Modifier else Modifier.shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color(0x180F172A),
                ambientColor = Color(0x0E0F172A)
            )
        ),
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Color.White.copy(alpha = 0.035f) else Color.White,
        border = BorderStroke(1.dp, if (isDark) GlassBorder else BorderLight)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Title + Dynamic Trend Badge + Filter Chips
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isNarrow = maxWidth < 560.dp
                if (isNarrow) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Line 1: Title + Dynamic Trend Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
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

                        // Line 2: Metric Badges Summary with smooth horizontal scroll
                        if (scores.isNotEmpty()) {
                            val metricBadgesScrollState = rememberScrollState()
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalFadingEdges(metricBadgesScrollState, fadeWidth = 12.dp, isRtl = lang == "fa")
                                    .touchDragScroll(metricBadgesScrollState, isVertical = false, isRtl = lang == "fa")
                                    .horizontalScroll(metricBadgesScrollState)
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
                } else {
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
                            val metricBadgesScrollState = rememberScrollState()
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .horizontalFadingEdges(metricBadgesScrollState, fadeWidth = 12.dp, isRtl = lang == "fa")
                                    .touchDragScroll(metricBadgesScrollState, isVertical = false, isRtl = lang == "fa")
                                    .horizontalScroll(metricBadgesScrollState)
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
                }
            }

            Spacer(Modifier.height(8.dp))

            // Filter Chips Bar (All, Last 10, Last 25, Critical)
            if (sortedScans.size > 5) {
                val filterChipsScrollState = rememberScrollState()
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalFadingEdges(filterChipsScrollState, fadeWidth = 12.dp, isRtl = lang == "fa")
                        .touchDragScroll(filterChipsScrollState, isVertical = false, isRtl = lang == "fa")
                        .horizontalScroll(filterChipsScrollState)
                        .padding(bottom = 6.dp)
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
                            color = if (isSelected) (if (isDark) accent.copy(alpha = 0.25f) else accent) else Color.Transparent,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) accent.copy(alpha = 0.6f) else (if (isDark) GlassBorder.copy(alpha = 0.25f) else GlassBorderLight.copy(alpha = 0.25f))
                            ),
                            modifier = Modifier
                                .springPress(pressedScale = 0.94f)
                                .pointerHoverIcon(PointerIcon.Hand)
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
                .then(
                    if (isDark) Modifier else Modifier.shadow(
                        elevation = 1.5.dp,
                        shape = RoundedCornerShape(12.dp),
                        spotColor = Color(0x140F172A),
                        ambientColor = Color(0x0C0F172A)
                    )
                )
                .clip(RoundedCornerShape(12.dp))
                .springPress(pressedScale = 0.98f)
                .clickable { onScanClick(singleScan) },
            color = if (isDark) Color.White.copy(alpha = 0.035f) else Color.White,
            border = BorderStroke(1.dp, if (isDark) sColor.copy(alpha = 0.35f) else BorderLight),
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
    LaunchedEffect(Unit) {
        entryAnim.animateTo(1f, animationSpec = tween(600, easing = FastOutSlowInEasing))
    }

    val hoveredScore = hoveredIndex?.let { scores.getOrNull(it) } ?: 0f
    val tooltipAlignment = if (hoveredScore >= 0.52f) Alignment.BottomCenter else Alignment.TopCenter

    Box(
        modifier = modifier
            .pointerInput(scans) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull()
                        val position = change?.position

                        when (event.type) {
                            PointerEventType.Move, PointerEventType.Press -> {
                                if (position != null && size.width > 0 && scores.isNotEmpty()) {
                                    val hPadding = 20.dp.toPx()
                                    val usableWidth = (size.width - hPadding * 2).coerceAtLeast(1f)
                                    val spacing = usableWidth / (scores.size - 1).coerceAtLeast(1)

                                    val rawIdx = ((position.x - hPadding) / spacing).roundToInt().coerceIn(0, scores.size - 1)

                                    // Hysteresis deadband to eliminate subpixel hover jitter between adjacent points
                                    val currentIdx = hoveredIndex
                                    val resolvedIdx = if (currentIdx != null && currentIdx in scores.indices) {
                                        val currentX = hPadding + currentIdx * spacing
                                        val candidateX = hPadding + rawIdx * spacing
                                        val distCurrent = kotlin.math.abs(position.x - currentX)
                                        val distCandidate = kotlin.math.abs(position.x - candidateX)
                                        val hysteresisPx = 8.dp.toPx()
                                        if (distCandidate < distCurrent - hysteresisPx) rawIdx else currentIdx
                                    } else {
                                        rawIdx
                                    }

                                    if (position.x in -12f..(size.width + 12f) && position.y in -12f..(size.height + 12f)) {
                                        hoveredIndex = resolvedIdx
                                        isPointerActive = true
                                    } else {
                                        hoveredIndex = null
                                        isPointerActive = false
                                    }
                                }
                            }
                            PointerEventType.Release -> {
                                val activeIdx = hoveredIndex
                                if (activeIdx != null && activeIdx in scans.indices) {
                                    onScanClick(scans[activeIdx])
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
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val hPadding = 20.dp.toPx()
            val vPadding = 18.dp.toPx()
            val usableWidth = (width - hPadding * 2).coerceAtLeast(1f)
            val spacing = usableWidth / (scores.size - 1).coerceAtLeast(1)
            val graphHeight = (height - vPadding * 2).coerceAtLeast(1f)
            val anim = entryAnim.value

            // Grid lines: 25%, 50%, 75%
            val gridAlpha = if (isDark) 0.07f else 0.05f
            listOf(0.25f, 0.5f, 0.75f).forEach { frac ->
                val y = vPadding + graphHeight * (1f - frac)
                drawLine(
                    color = (if (isDark) Color.White else Color.Black).copy(alpha = gridAlpha),
                    start = Offset(hPadding, y),
                    end = Offset(width - hPadding, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )
            }

            val points = scores.mapIndexed { index, score ->
                val animatedScore = score * anim
                Offset(hPadding + index * spacing, vPadding + graphHeight * (1f - animatedScore))
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

        // Floating Live Inspection Tooltip HUD Overlay (Adaptive top/bottom alignment to never cover hovered points)
        AnimatedVisibility(
            visible = hoveredIndex != null && hoveredIndex in scans.indices,
            enter = fadeIn(tween(140)),
            exit = fadeOut(tween(140)),
            modifier = Modifier
                .align(tooltipAlignment)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
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
                            .clickable { onScanClick(scan) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f, fill = false),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(5.dp),
                                    color = riskColor.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, riskColor.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        "${scan.securityScore}%",
                                        color = riskColor,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    val isolatedHost = "\u2066${scan.target}\u2069"
                                    val title = if (!scan.deviceName.isNullOrBlank()) "$isolatedHost (${scan.deviceName})" else isolatedHost
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp),
                                        color = if (isDark) Color.White else Color.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "\u2066$dtDate $dtTime\u2069 \u2022 ${scan.openPorts.size} ${LocalizedStrings.get("open_ports_count", lang)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                        color = if (isDark) TextMuted else TextMutedLight,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(Modifier.width(6.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                CyberBadge(text = riskLabel, color = riskColor, fontSize = 8f)
                                Icon(
                                    Icons.Default.TouchApp,
                                    contentDescription = LocalizedStrings.get("click_to_view", lang),
                                    tint = accent,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
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

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 86.dp)
            .then(
                if (isDark) Modifier else Modifier.shadow(
                    elevation = 1.5.dp,
                    shape = RoundedCornerShape(16.dp),
                    spotColor = Color(0x140F172A),
                    ambientColor = Color(0x0C0F172A)
                )
            )
            .springPress(pressedScale = 0.98f)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, if (isDark) GlassBorder else BorderLight, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = if (isDark) Color.White.copy(alpha = 0.045f) else Color.White
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tier 1: Full Target Title, Risk/Score Badge, and Delete Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val isolatedTarget = "\u2066${scan.target}\u2069"
                val title = if (!scan.deviceName.isNullOrBlank()) "$isolatedTarget (${scan.deviceName})" else "${LocalizedStrings.get("target", lang)}: $isolatedTarget"

                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isDark) Color.White else Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    CyberBadge(
                        text = riskLabel,
                        color = riskColor,
                        hasGlowAura = scan.securityScore < 50,
                        fontSize = 8.5f
                    )
                }

                Spacer(Modifier.width(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Score Badge
                    Surface(
                        color = riskColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, riskColor.copy(alpha = 0.35f)),
                        modifier = Modifier.cyberPulse(glowColor = riskColor, enabled = scan.securityScore < 50)
                    ) {
                        Text(
                            "${scan.securityScore}%",
                            color = riskColor,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }

                    // Delete Button
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(30.dp)
                            .springPress(pressedScale = 0.90f)
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = LocalizedStrings.get("delete", lang),
                            tint = DangerNeon.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Tier 2: Date/Time + OS Fingerprint + Open Ports Badge + Compact Export Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val isolatedDateTime = "\u2066$formattedDate $formattedTime\u2069"
                    val metaText = if (!scan.osFingerprint.isNullOrBlank()) "$isolatedDateTime \u2022 ${scan.osFingerprint}" else isolatedDateTime
                    Text(
                        text = metaText,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                        color = if (isDark) TextMuted else TextMutedLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    CyberBadge(
                        text = "${scan.openPorts.size} ${LocalizedStrings.get("open_ports_count", lang)}",
                        color = accent,
                        fontSize = 8.5f
                    )
                }

                Spacer(Modifier.width(6.dp))

                // Compact Capsule Export Button
                Surface(
                    onClick = onExport,
                    shape = RoundedCornerShape(8.dp),
                    color = accent.copy(alpha = if (isDark) 0.14f else 0.10f),
                    border = BorderStroke(1.dp, accent.copy(alpha = if (isDark) 0.45f else 0.35f)),
                    modifier = Modifier.springPress(pressedScale = 0.94f)
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
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            LocalizedStrings.get("export_report", lang),
                            color = if (isDark) Color.White else Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
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
    isMobile: Boolean = false,
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
    val isRtl = lang == "fa"

    val isTouchEmulated = LocalTouchEmulation.current
    val shouldUseBottomSheet = isMobile || isTouchEmulated

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

    val exportDialogBody: @Composable ColumnScope.() -> Unit = {
                // Header: Close button at Start (Right in Persian, Left in other languages), Actions & Identity at End
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .pointerHoverIcon(PointerIcon.Hand)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = LocalizedStrings.get("close", lang),
                                tint = if (isDark) TextMuted else TextMutedLight
                            )
                        }

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
                    }

                    Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
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
                            "MD" -> LocalizedStrings.get("format_markdown", lang)
                            "CSV" -> LocalizedStrings.get("format_csv", lang)
                            else -> LocalizedStrings.get("format_json", lang)
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
                                .touchDragScroll(previewScrollState, isVertical = true)
                                .verticalScroll(previewScrollState)
                                .touchDragScroll(previewHScrollState, isVertical = false, isRtl = isRtl)
                                .horizontalScroll(previewHScrollState)
                                .padding(12.dp)
                                .absolutePadding(
                                    right = if (isRtl) 10.dp else 0.dp,
                                    left = if (!isRtl) 10.dp else 0.dp
                                )
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
                                .align(if (isRtl) AbsoluteAlignment.CenterRight else AbsoluteAlignment.CenterLeft)
                                .fillMaxHeight()
                                .padding(2.dp)
                        )
                    }
                }

                HorizontalDivider(
                    color = if (isDark) GlassBorder else GlassBorderLight,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // Actions Matrix Footer (Adaptive 2x2 Grid on Mobile / Row on Desktop)
                if (shouldUseBottomSheet) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Row 1: Primary File Actions (Save As + Quick Save)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    onSaveAs(selectedFormat)
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accent),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Icon(Icons.Default.SaveAs, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    LocalizedStrings.get("save_as_file", lang),
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Button(
                                onClick = {
                                    onQuickSave(selectedFormat)
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SecondaryNeon),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Icon(Icons.Default.Download, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    LocalizedStrings.get("quick_save_downloads", lang),
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Row 2: Secondary Content Actions (Copy Content + Share)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                modifier = Modifier.weight(1f).height(44.dp)
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
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    onShare(selectedFormat)
                                    onDismiss()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.White.copy(alpha = 0.05f),
                                    contentColor = SecondaryNeon
                                ),
                                border = BorderStroke(1.dp, if (isDark) GlassBorder else GlassBorderLight),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = null,
                                    tint = SecondaryNeon,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    LocalizedStrings.get("share_file_native", lang),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                } else {
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
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            modifier = Modifier.weight(1.2f).height(42.dp)
                        ) {
                            Icon(Icons.Default.SaveAs, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                LocalizedStrings.get("save_as_file", lang),
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            modifier = Modifier.weight(1.2f).height(42.dp)
                        ) {
                            Icon(Icons.Default.Download, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                LocalizedStrings.get("quick_save_downloads", lang),
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            modifier = Modifier.weight(1f).height(42.dp)
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
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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

    if (shouldUseBottomSheet) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(100f),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.90f)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = if (isDark) SurfaceDark else Color.White,
                shadowElevation = 24.dp,
                border = BorderStroke(1.dp, if (isDark) Brush.linearGradient(listOf(accent.copy(alpha = 0.6f), SecondaryNeon.copy(alpha = 0.4f))) else BorderStroke(1.dp, BorderLight).brush)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(40.dp)
                            .height(4.dp)
                            .background((if (isDark) Color.White else Color.Black).copy(alpha = 0.25f), CircleShape)
                    )

                    Spacer(Modifier.height(8.dp))

                    exportDialogBody()
                }
            }
        }
    } else {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.88f)
                    .then(
                        if (isDark) Modifier else Modifier.shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(22.dp),
                            ambientColor = Color(0x280F172A),
                            spotColor = Color(0x380F172A)
                        )
                    ),
                shape = RoundedCornerShape(22.dp),
                color = if (isDark) SurfaceDark else Color.White,
                border = BorderStroke(1.dp, if (isDark) Brush.linearGradient(listOf(accent.copy(alpha = 0.6f), SecondaryNeon.copy(alpha = 0.4f))) else BorderStroke(1.dp, BorderLight).brush)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    exportDialogBody()
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
    isMobile: Boolean = false,
    onDismiss: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    val isRtl = lang == "fa"
    val isTouchEmulated = LocalTouchEmulation.current
    val shouldUseBottomSheet = isMobile || isTouchEmulated
    val listState = rememberLazyListState()
    var portFilterQuery by remember { mutableStateOf("") }
    var selectedPortCategory by remember { mutableStateOf("ALL") }
    var visiblePortLimit by remember { mutableStateOf(100) }
    var isSummaryExpanded by remember { mutableStateOf(false) }

    var selectedPortForDetail by remember { mutableStateOf<DisplayPort?>(null) }
    var selectedPortBanner by remember { mutableStateOf<String?>(null) }
    var selectedPortService by remember { mutableStateOf<String?>(null) }

    val sortedPorts = remember(scan.openPorts) { scan.openPorts.distinct().sorted() }

    val threatPortsSet = remember(sortedPorts, accent) {
        sortedPorts.filter { getPortColor(it, accent) == DangerNeon }.toSet()
    }
    val safePortsSet = remember(sortedPorts, accent) {
        sortedPorts.filter { getPortColor(it, accent) != DangerNeon }.toSet()
    }
    val identifiedPortsSet = remember(sortedPorts, scan.portServices, scan.portBanners) {
        sortedPorts.filter { isIdentifiedPort(it, scan.portServices[it], scan.portBanners[it]) }.toSet()
    }
    val genericPortsSet = remember(sortedPorts, identifiedPortsSet) {
        sortedPorts.filter { it !in identifiedPortsSet }.toSet()
    }
    val systemPortsSet = remember(sortedPorts) {
        sortedPorts.filter { it in 1..1023 }.toSet()
    }
    val userPortsSet = remember(sortedPorts) {
        sortedPorts.filter { it >= 1024 }.toSet()
    }
    val webPortsSet = remember(sortedPorts) {
        sortedPorts.filter { it in setOf(80, 443, 8000, 8008, 8080, 8081, 8088, 8443, 8888, 9000, 9090, 3000, 5000) }.toSet()
    }
    val dbPortsSet = remember(sortedPorts) {
        sortedPorts.filter { it in setOf(1433, 1521, 3306, 5432, 6379, 27017, 9200, 11211) }.toSet()
    }
    val remotePortsSet = remember(sortedPorts) {
        sortedPorts.filter { it in setOf(21, 22, 23, 3389, 5900, 5985, 5986) }.toSet()
    }
    val industrialPortsSet = remember(sortedPorts) {
        sortedPorts.filter { it in setOf(102, 502, 1883, 4840, 47808, 5683) }.toSet()
    }
    val bannerPortsSet = remember(sortedPorts, scan.portBanners) {
        sortedPorts.filter { !scan.portBanners[it].isNullOrBlank() }.toSet()
    }
    val infraPortsSet = remember(sortedPorts) {
        sortedPorts.filter { it in setOf(53, 67, 68, 123, 161, 389, 636) }.toSet()
    }

    val filteredPorts = remember(sortedPorts, portFilterQuery, selectedPortCategory, lang) {
        sortedPorts.filter { port ->
            val matchesCategory = when (selectedPortCategory) {
                "THREATS" -> port in threatPortsSet
                "SAFE" -> port in safePortsSet
                "IDENTIFIED" -> port in identifiedPortsSet
                "GENERIC" -> port in genericPortsSet
                "SYSTEM" -> port in systemPortsSet
                "USER" -> port in userPortsSet
                "WEB" -> port in webPortsSet
                "DATABASE" -> port in dbPortsSet
                "REMOTE" -> port in remotePortsSet
                "INDUSTRIAL" -> port in industrialPortsSet
                "BANNER" -> port in bannerPortsSet
                "INFRA" -> port in infraPortsSet
                else -> true
            }
            if (!matchesCategory) return@filter false

            val rawQ = portFilterQuery.trim()
            if (rawQ.isEmpty()) true
            else {
                val q = rawQ.lowercase()
                val asciiQ = DateFormatter.toAsciiDigits(q)
                val rawService = scan.portServices[port]?.trim()
                val banner = scan.portBanners[port]?.trim() ?: ""
                val resolvedService = getServiceTitle(port, if (rawService.isNullOrBlank() || rawService.equals("unknown", ignoreCase = true)) null else rawService, lang)
                val portStr = port.toString()
                val portFaStr = DateFormatter.toPersianDigits(portStr)
                val isThreat = port in threatPortsSet
                val isIdent = port in identifiedPortsSet

                portStr.contains(asciiQ) ||
                portFaStr.contains(q) ||
                resolvedService.lowercase().contains(q) ||
                (rawService?.lowercase()?.contains(q) == true) ||
                banner.lowercase().contains(q) ||
                (isThreat && (q in listOf("خطر", "تهدید", "threat", "danger", "cve"))) ||
                (!isThreat && (q in listOf("امن", "safe", "نرمال", "normal", "ok"))) ||
                (isIdent && (q in listOf("شناسایی", "معروف", "identified", "known"))) ||
                (!isIdent && (q in listOf("عمومی", "نامشخص", "بازتابی", "generic", "unknown", "reflexive"))) ||
                (port in 1..1023 && (q in listOf("سیستمی", "سیستم", "system", "privileged"))) ||
                (port >= 1024 && (q in listOf("کاربری", "کاربر", "user", "registered")))
            }
        }
    }
    val displayedPorts = remember(filteredPorts, visiblePortLimit) {
        filteredPorts.take(visiblePortLimit)
    }

    LaunchedEffect(selectedPortCategory, portFilterQuery) {
        visiblePortLimit = 100
        listState.scrollToItem(0)
    }

    val isWafAnomaly = remember(scan) {
        com.mrcoder20.portx.domain.isSuspectedTarpitOrWaf(
            target = scan.target,
            openPorts = scan.openPorts,
            banners = scan.portBanners,
            osFingerprint = scan.osFingerprint
        )
    }

    val grade = when {
        scan.securityScore >= 90 -> "A+" to TertiaryNeon
        scan.securityScore >= 75 -> "A" to TertiaryNeon
        scan.securityScore >= 50 -> "B" to WarningNeon
        scan.securityScore >= 25 -> "C" to Color(0xFFF97316)
        else -> "F" to DangerNeon
    }

    val detailDialogBody: @Composable ColumnScope.() -> Unit = {
        // Top Header: Compact 1-line layout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color.White.copy(0.06f) else Color.Black.copy(0.04f))
                    .pointerHoverIcon(PointerIcon.Hand)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = LocalizedStrings.get("close", lang),
                    tint = if (isDark) TextMuted else TextMutedLight,
                    modifier = Modifier.size(16.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = grade.second.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, grade.second.copy(alpha = 0.4f))
                ) {
                    Text(
                        "${LocalizedStrings.get("grade", lang)} ${grade.first}",
                        color = grade.second,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        LocalizedStrings.get("report_detail_title", lang),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                        color = if (isDark) Color.White else Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val isolatedTarget = "\u2066${scan.target}\u2069"
                    val title = if (!scan.deviceName.isNullOrBlank()) "$isolatedTarget (${scan.deviceName})" else isolatedTarget
                    Text(
                        title,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                        color = accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        HorizontalDivider(
            color = if (isDark) GlassBorder else GlassBorderLight,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // Virtualized Scrollable Content
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .touchDragScroll(listState, isVertical = true)
                    .absolutePadding(
                        right = if (isRtl) 8.dp else 0.dp,
                        left = if (!isRtl) 8.dp else 0.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Ultra-Compact 1-Row Telemetry Ribbon (Saves ~55dp)
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color.White.copy(alpha = 0.03f) else SurfaceInsetLight,
                        border = BorderStroke(1.dp, if (isDark) GlassBorder.copy(alpha = 0.45f) else BorderLight)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val secScoreStr = if (lang == "fa") DateFormatter.toPersianDigits("${scan.securityScore}%") else "${scan.securityScore}%"
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    LocalizedStrings.get("security_score", lang),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = if (isDark) TextMuted else TextMutedLight,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    secScoreStr,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                                    color = grade.second
                                )
                            }

                            Box(modifier = Modifier.width(1.dp).height(20.dp).background(if (isDark) GlassBorder else BorderLight))

                            val threatScore = 100 - scan.securityScore
                            val threatScoreStr = if (lang == "fa") DateFormatter.toPersianDigits("$threatScore%") else "$threatScore%"
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    LocalizedStrings.get("threat_score", lang),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = if (isDark) TextMuted else TextMutedLight,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    threatScoreStr,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                                    color = DangerNeon
                                )
                            }

                            Box(modifier = Modifier.width(1.dp).height(20.dp).background(if (isDark) GlassBorder else BorderLight))

                            val openCountStr = if (lang == "fa") DateFormatter.toPersianDigits("${scan.openPorts.size}") else "${scan.openPorts.size}"
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    LocalizedStrings.get("open_ports_count", lang),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = if (isDark) TextMuted else TextMutedLight,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    openCountStr,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                                    color = accent
                                )
                            }
                        }
                    }
                }

                // Collapsible Accordion Executive Summary (Saves ~80dp when collapsed)
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable { isSummaryExpanded = !isSummaryExpanded },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color.White.copy(alpha = 0.03f) else SurfaceInsetLight,
                        border = BorderStroke(1.dp, if (isDark) GlassBorder.copy(alpha = 0.4f) else BorderLight)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Text(
                                        LocalizedStrings.get("executive_summary", lang),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                        color = accent
                                    )
                                    if (!isSummaryExpanded) {
                                        val previewParts = listOfNotNull(
                                            scan.deviceName,
                                            scan.osFingerprint,
                                            scan.scanType ?: "TCP"
                                        )
                                        if (previewParts.isNotEmpty()) {
                                            Text(
                                                previewParts.joinToString(" \u2022 "),
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace),
                                                color = if (isDark) TextMuted else TextMutedLight,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                Icon(
                                    if (isSummaryExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            AnimatedVisibility(visible = isSummaryExpanded) {
                                Column(
                                    modifier = Modifier.padding(top = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    HorizontalDivider(
                                        color = if (isDark) GlassBorder.copy(alpha = 0.35f) else BorderLight,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                    scan.deviceName?.let {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(LocalizedStrings.get("device_profile", lang), style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), color = if (isDark) TextMuted else TextMutedLight)
                                            Text(it, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp), color = if (isDark) Color.White else Color.Black)
                                        }
                                    }
                                    scan.osFingerprint?.let {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(LocalizedStrings.get("os_fingerprint", lang), style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), color = if (isDark) TextMuted else TextMutedLight)
                                            Text(it, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp), color = SecondaryNeon)
                                        }
                                    }
                                    val connCountStr = if (lang == "fa") DateFormatter.toPersianDigits("${scan.concurrentScans}") else "${scan.concurrentScans}"
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(LocalizedStrings.get("scan_protocol", lang), style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), color = if (isDark) TextMuted else TextMutedLight)
                                        Text("${scan.scanType ?: "TCP"} \u2022 $connCountStr ${LocalizedStrings.get("connections", lang)}", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp), color = TertiaryNeon)
                                    }
                                }
                            }
                        }
                    }
                }

                // WAF Anomaly Alert Notice
                if (isWafAnomaly) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = WarningNeon.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, WarningNeon.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Shield, null, tint = WarningNeon, modifier = Modifier.size(18.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                    Text(
                                        LocalizedStrings.get("waf_anomaly_title", lang),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp),
                                        color = WarningNeon
                                    )
                                    Text(
                                        LocalizedStrings.get("waf_anomaly_desc", lang),
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        color = if (isDark) TextSecondary else TextSecondaryLight
                                    )
                                }
                            }
                        }
                    }
                }

                // Open Ports List Header + Search Filter + Category Chips
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                LocalizedStrings.get("discovered_ports", lang),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
                                color = if (isDark) Color.White else Color.Black
                            )
                            if (sortedPorts.isNotEmpty()) {
                                val countText = if (filteredPorts.size != sortedPorts.size) {
                                    val raw = "${filteredPorts.size}/${sortedPorts.size}"
                                    if (lang == "fa") DateFormatter.toPersianDigits(raw) else raw
                                } else {
                                    val raw = "${sortedPorts.size}"
                                    if (lang == "fa") DateFormatter.toPersianDigits(raw) else raw
                                }
                                Text(
                                    "$countText ${LocalizedStrings.get("open_ports_count", lang)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 9.5.sp),
                                    color = accent,
                                    modifier = Modifier
                                        .background(accent.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (sortedPorts.isNotEmpty()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth().height(34.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDark) Color.White.copy(alpha = 0.04f) else SurfaceInsetLight,
                                border = BorderStroke(1.dp, if (portFilterQuery.isNotEmpty()) accent else (if (isDark) GlassBorder else GlassBorderLight).copy(alpha = 0.6f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null,
                                        tint = accent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    BasicTextField(
                                        value = portFilterQuery,
                                        onValueChange = { portFilterQuery = it },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodySmall.copy(
                                            color = if (isDark) Color.White else Color.Black,
                                            fontSize = 11.sp,
                                            textDirection = TextDirection.ContentOrRtl
                                        ),
                                        cursorBrush = SolidColor(accent),
                                        modifier = Modifier.weight(1f),
                                        decorationBox = { innerTextField ->
                                            Box(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                if (portFilterQuery.isEmpty()) {
                                                    Text(
                                                        LocalizedStrings.get("search_ports_in_report", lang),
                                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                                        color = if (isDark) TextMuted else TextMutedLight,
                                                        maxLines = 1
                                                    )
                                                }
                                                innerTextField()
                                            }
                                        }
                                    )
                                    if (portFilterQuery.isNotEmpty()) {
                                        IconButton(
                                            onClick = { portFilterQuery = "" },
                                            modifier = Modifier.size(22.dp).springPress()
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = LocalizedStrings.get("clear", lang),
                                                tint = if (isDark) TextMuted else TextMutedLight,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Category Filter Buttons / Chips
                            val chipsScrollState = rememberScrollState()
                            LaunchedAutoScrollHint(chipsScrollState)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(26.dp)
                                    .horizontalFadingEdges(chipsScrollState, fadeWidth = 12.dp, isRtl = isRtl)
                                    .touchDragScroll(chipsScrollState, isVertical = false, isRtl = isRtl)
                                    .horizontalScroll(chipsScrollState),
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChipMini(
                                    label = LocalizedStrings.get("filter_all", lang),
                                    count = sortedPorts.size,
                                    isSelected = selectedPortCategory == "ALL",
                                    accent = accent,
                                    onClick = { selectedPortCategory = "ALL" }
                                )
                                if (threatPortsSet.isNotEmpty()) {
                                    FilterChipMini(
                                        label = LocalizedStrings.get("filter_threats", lang),
                                        count = threatPortsSet.size,
                                        isSelected = selectedPortCategory == "THREATS",
                                        accent = DangerNeon,
                                        isDanger = true,
                                        onClick = { selectedPortCategory = if (selectedPortCategory == "THREATS") "ALL" else "THREATS" }
                                    )
                                }
                                if (safePortsSet.isNotEmpty() && threatPortsSet.isNotEmpty()) {
                                    FilterChipMini(
                                        label = LocalizedStrings.get("filter_safe", lang),
                                        count = safePortsSet.size,
                                        isSelected = selectedPortCategory == "SAFE",
                                        accent = TertiaryNeon,
                                        onClick = { selectedPortCategory = if (selectedPortCategory == "SAFE") "ALL" else "SAFE" }
                                    )
                                }
                                if (identifiedPortsSet.isNotEmpty() && genericPortsSet.isNotEmpty()) {
                                    FilterChipMini(
                                        label = LocalizedStrings.get("filter_identified", lang),
                                        count = identifiedPortsSet.size,
                                        isSelected = selectedPortCategory == "IDENTIFIED",
                                        accent = accent,
                                        onClick = { selectedPortCategory = if (selectedPortCategory == "IDENTIFIED") "ALL" else "IDENTIFIED" }
                                    )
                                }
                                if (genericPortsSet.isNotEmpty() && identifiedPortsSet.isNotEmpty()) {
                                    FilterChipMini(
                                        label = LocalizedStrings.get("filter_generic", lang),
                                        count = genericPortsSet.size,
                                        isSelected = selectedPortCategory == "GENERIC",
                                        accent = if (isDark) TextMuted else TextMutedLight,
                                        onClick = { selectedPortCategory = if (selectedPortCategory == "GENERIC") "ALL" else "GENERIC" }
                                    )
                                }
                                if (bannerPortsSet.isNotEmpty()) {
                                    FilterChipMini(
                                        label = LocalizedStrings.get("filter_banner", lang),
                                        count = bannerPortsSet.size,
                                        isSelected = selectedPortCategory == "BANNER",
                                        accent = accent,
                                        onClick = { selectedPortCategory = if (selectedPortCategory == "BANNER") "ALL" else "BANNER" }
                                    )
                                }
                                if (webPortsSet.isNotEmpty()) {
                                    FilterChipMini(
                                        label = LocalizedStrings.get("filter_web", lang),
                                        count = webPortsSet.size,
                                        isSelected = selectedPortCategory == "WEB",
                                        accent = accent,
                                        onClick = { selectedPortCategory = if (selectedPortCategory == "WEB") "ALL" else "WEB" }
                                    )
                                }
                                if (dbPortsSet.isNotEmpty()) {
                                    FilterChipMini(
                                        label = LocalizedStrings.get("filter_db", lang),
                                        count = dbPortsSet.size,
                                        isSelected = selectedPortCategory == "DATABASE",
                                        accent = accent,
                                        onClick = { selectedPortCategory = if (selectedPortCategory == "DATABASE") "ALL" else "DATABASE" }
                                    )
                                }
                                if (remotePortsSet.isNotEmpty()) {
                                    FilterChipMini(
                                        label = LocalizedStrings.get("filter_remote", lang),
                                        count = remotePortsSet.size,
                                        isSelected = selectedPortCategory == "REMOTE",
                                        accent = accent,
                                        onClick = { selectedPortCategory = if (selectedPortCategory == "REMOTE") "ALL" else "REMOTE" }
                                    )
                                }
                                if (industrialPortsSet.isNotEmpty()) {
                                    FilterChipMini(
                                        label = LocalizedStrings.get("filter_industrial", lang),
                                        count = industrialPortsSet.size,
                                        isSelected = selectedPortCategory == "INDUSTRIAL",
                                        accent = accent,
                                        onClick = { selectedPortCategory = if (selectedPortCategory == "INDUSTRIAL") "ALL" else "INDUSTRIAL" }
                                    )
                                }
                                if (infraPortsSet.isNotEmpty()) {
                                    FilterChipMini(
                                        label = LocalizedStrings.get("filter_infra", lang),
                                        count = infraPortsSet.size,
                                        isSelected = selectedPortCategory == "INFRA",
                                        accent = accent,
                                        onClick = { selectedPortCategory = if (selectedPortCategory == "INFRA") "ALL" else "INFRA" }
                                    )
                                }
                                if (systemPortsSet.isNotEmpty() && userPortsSet.isNotEmpty()) {
                                    FilterChipMini(
                                        label = LocalizedStrings.get("filter_system_ports", lang),
                                        count = systemPortsSet.size,
                                        isSelected = selectedPortCategory == "SYSTEM",
                                        accent = accent,
                                        onClick = { selectedPortCategory = if (selectedPortCategory == "SYSTEM") "ALL" else "SYSTEM" }
                                    )
                                    FilterChipMini(
                                        label = LocalizedStrings.get("filter_registered_ports", lang),
                                        count = userPortsSet.size,
                                        isSelected = selectedPortCategory == "USER",
                                        accent = accent,
                                        onClick = { selectedPortCategory = if (selectedPortCategory == "USER") "ALL" else "USER" }
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                            }
                        }
                    }
                }

                if (filteredPorts.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = (if (scan.openPorts.isEmpty()) TertiaryNeon else DangerNeon).copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, (if (scan.openPorts.isEmpty()) TertiaryNeon else DangerNeon).copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    if (scan.openPorts.isEmpty()) LocalizedStrings.get("no_open_ports", lang)
                                    else LocalizedStrings.get("no_ports_found", lang),
                                    color = if (scan.openPorts.isEmpty()) TertiaryNeon else DangerNeon,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                                )
                                if (selectedPortCategory != "ALL" || portFilterQuery.isNotEmpty()) {
                                    TextButton(
                                        onClick = {
                                            selectedPortCategory = "ALL"
                                            portFilterQuery = ""
                                        },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(
                                            LocalizedStrings.get("clear_filter", lang),
                                            color = accent,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(displayedPorts, key = { it }) { port ->
                        val banner = scan.portBanners[port]
                        val rawService = scan.portServices[port]?.trim()
                        val service = getServiceTitle(port, if (rawService.isNullOrBlank() || rawService.equals("unknown", ignoreCase = true)) null else rawService, lang)
                        val isHighRisk = port in listOf(21, 23, 135, 445, 3389)
                        val dossier = remember(port, banner) { PortIntelligence.getDossier(port, banner) }
                        val portColor = getPortColor(port, accent)

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
                                .pointerHoverIcon(PointerIcon.Hand)
                                .clickable {
                                    selectedPortBanner = banner
                                    selectedPortService = rawService
                                    selectedPortForDetail = DisplayPort(
                                        number = port,
                                        title = service,
                                        description = banner ?: (scan.portServices[port] ?: ""),
                                        color = portColor
                                    )
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) Color.White.copy(alpha = 0.03f) else Color.White,
                            border = BorderStroke(1.dp, if (isHighRisk) DangerNeon.copy(alpha = 0.4f) else (if (isDark) GlassBorder else BorderLight))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(portColor.copy(alpha = 0.15f), RoundedCornerShape(7.dp))
                                        .border(1.dp, portColor.copy(alpha = 0.5f), RoundedCornerShape(7.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val portNumStr = if (lang == "fa") DateFormatter.toPersianDigits("$port") else "$port"
                                    Text(
                                        portNumStr,
                                        color = portColor,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        fontSize = if (port > 9999) 9.sp else 10.5.sp
                                    )
                                }

                                Spacer(Modifier.width(8.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            service,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
                                            color = if (isDark) Color.White else Color.Black,
                                            modifier = Modifier.weight(1f, fill = false),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (dossier.rfcStandard.isNotBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = accent.copy(alpha = 0.12f),
                                                border = BorderStroke(1.dp, accent.copy(alpha = 0.3f)),
                                                modifier = Modifier.widthIn(max = 130.dp)
                                            ) {
                                                Text(
                                                    dossier.rfcStandard,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 8.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Medium
                                                    ),
                                                    color = accent,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                    if (!banner.isNullOrBlank()) {
                                        Text(
                                            banner,
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 9.5.sp),
                                            color = if (isDark) TextMuted else TextMutedLight,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(Modifier.width(6.dp))

                                if (isHighRisk) {
                                    CyberBadge(text = "CRITICAL", color = DangerNeon, fontSize = 7.5f)
                                    Spacer(Modifier.width(4.dp))
                                }

                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = "Details",
                                    tint = if (isDark) TextMuted else TextMutedLight,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    if (filteredPorts.size > displayedPorts.size) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDark) Color.White.copy(alpha = 0.04f) else SurfaceInsetLight,
                                border = BorderStroke(1.dp, if (isDark) accent.copy(alpha = 0.3f) else BorderLight)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val showingText = LocalizedStrings.get("showing_ports_count", lang)
                                        .replace("{shown}", "${displayedPorts.size}")
                                        .replace("{total}", "${filteredPorts.size}")
                                    Text(
                                        showingText,
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 9.5.sp),
                                        color = if (isDark) TextMuted else TextMutedLight
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        TextButton(
                                            onClick = { visiblePortLimit += 250 },
                                            colors = ButtonDefaults.textButtonColors(contentColor = accent),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("+250", fontWeight = FontWeight.Bold, fontSize = 10.5.sp)
                                        }
                                        Button(
                                            onClick = { visiblePortLimit = filteredPorts.size },
                                            colors = ButtonDefaults.buttonColors(containerColor = accent),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                LocalizedStrings.get("filter_all", lang),
                                                color = Color.Black,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.5.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!shouldUseBottomSheet) {
                PortXVerticalScrollbar(
                    listState = listState,
                    modifier = Modifier
                        .align(if (isRtl) AbsoluteAlignment.CenterRight else AbsoluteAlignment.CenterLeft)
                        .fillMaxHeight()
                        .padding(horizontal = 2.dp)
                )
            }
        }

        HorizontalDivider(
            color = if (isDark) GlassBorder else GlassBorderLight,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        if (selectedPortForDetail != null) {
            PortDetailDialog(
                port = selectedPortForDetail!!,
                target = scan.target,
                rawBanner = selectedPortBanner,
                rawService = selectedPortService,
                accent = accent,
                lang = lang,
                isMobile = isMobile,
                onDismiss = { selectedPortForDetail = null }
            )
        }

        // Action Footer with Unified Export Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onExport,
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(38.dp)
            ) {
                Icon(Icons.Default.FileDownload, null, tint = Color.Black, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text(LocalizedStrings.get("export_report", lang), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
            }

            OutlinedButton(
                onClick = onDelete,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerNeon),
                border = BorderStroke(1.dp, DangerNeon.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(38.dp)
            ) {
                Icon(Icons.Default.DeleteOutline, null, tint = DangerNeon, modifier = Modifier.size(15.dp))
            }
        }
    }

    if (shouldUseBottomSheet) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(100f),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.96f)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                color = if (isDark) SurfaceDark else Color.White,
                shadowElevation = 24.dp,
                border = BorderStroke(1.dp, if (isDark) accent.copy(alpha = 0.4f) else BorderLight)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(36.dp)
                            .height(4.dp)
                            .background((if (isDark) Color.White else Color.Black).copy(alpha = 0.25f), CircleShape)
                    )

                    Spacer(Modifier.height(6.dp))

                    detailDialogBody()
                }
            }
        }
    } else {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.92f)
                    .then(
                        if (isDark) Modifier else Modifier.shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = Color(0x280F172A),
                            spotColor = Color(0x380F172A)
                        )
                    ),
                shape = RoundedCornerShape(16.dp),
                color = if (isDark) SurfaceDark else Color.White,
                border = BorderStroke(1.dp, if (isDark) accent.copy(alpha = 0.4f) else BorderLight)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    detailDialogBody()
                }
            }
        }
    }
}
