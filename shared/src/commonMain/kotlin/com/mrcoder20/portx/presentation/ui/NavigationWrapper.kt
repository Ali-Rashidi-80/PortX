package com.mrcoder20.portx.presentation.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrcoder20.portx.domain.AppSettings
import com.mrcoder20.portx.domain.LocalizedStrings
import com.mrcoder20.portx.domain.ScanManager
import com.mrcoder20.portx.domain.SettingsManager
import com.mrcoder20.portx.presentation.ui.components.springPress
import com.mrcoder20.portx.presentation.ui.theme.*
import org.koin.compose.koinInject

@Composable
fun NavigationWrapper(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    settings: AppSettings,
    onMinimize: () -> Unit = {},
    onMaximize: () -> Unit = {},
    onClose: () -> Unit = {},
    onSnapWindow: ((Int, Int) -> Unit)? = null,
    windowDraggableArea: @Composable (@Composable () -> Unit) -> Unit = { it() },
    content: @Composable () -> Unit
) {
    val accent = LocalAccentColor.current
    val lang = settings.language
    val isDark = settings.theme == "DARK"
    val isScanning by ScanManager.isScanning.collectAsState()
    val scanProgress by ScanManager.progress.collectAsState()
    val settingsManager: SettingsManager = koinInject()

    var deviceModeActive by remember { mutableStateOf(false) }
    var selectedDevicePreset by remember { mutableStateOf(com.mrcoder20.portx.presentation.ui.components.DevicePreset.RESPONSIVE) }
    var isLandscapeOrientation by remember { mutableStateOf(false) }
    var showDeviceFrameBezel by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize().background(if (isDark) BackgroundDark else BackgroundLight)) {
        // --- CUSTOM TOP TITLE BAR (Always LTR for native window consistency) ---
        CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
            windowDraggableArea {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    color = if (isDark) SurfaceDark else SurfaceLight,
                    border = BorderStroke(0.5.dp, (if (isDark) GlassBorder else GlassBorderLight).copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Brand & Engine State Indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Radar, null, tint = accent, modifier = Modifier.size(18.dp))
                            Text(
                                "PortX Professional",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.85f)
                                )
                            )

                            // Dynamic Engine Status Badge in Title Bar
                            if (isScanning) {
                                CyberBadge(
                                    text = "SCANNING $scanProgress%",
                                    color = PrimaryNeon,
                                    hasPulseDot = true,
                                    fontSize = 9f
                                )
                            } else {
                                CyberBadge(
                                    text = "ENGINE ONLINE",
                                    color = TertiaryNeon,
                                    hasPulseDot = true,
                                    fontSize = 9f
                                )
                            }
                        }

                        // Right: Device Mode + Quick Theme Switch + Window Controls
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Device Mode Emulation Switcher (DevTools Mode)
                            IconButton(
                                onClick = { deviceModeActive = !deviceModeActive },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    if (deviceModeActive) Icons.Default.PhoneIphone else Icons.Default.Devices,
                                    contentDescription = LocalizedStrings.get("device_mode", lang),
                                    tint = if (deviceModeActive) accent else (if (isDark) TextMuted else TextMutedLight),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Quick Theme Switch
                            IconButton(
                                onClick = {
                                    settingsManager.updateTheme(if (isDark) "LIGHT" else "DARK")
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Toggle Theme",
                                    tint = if (isDark) WarningNeon else Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Spacer(Modifier.width(4.dp))

                            // Window Control Buttons
                            WindowControlBtn(Icons.Default.Remove, if (isDark) TextMuted else TextMutedLight, onMinimize)
                            WindowControlBtn(Icons.Default.AspectRatio, if (isDark) TextMuted else TextMutedLight, onMaximize)
                            WindowControlBtn(Icons.Default.Close, DangerNeon, onClose)
                        }
                    }
                }
            }
        }

        // --- DEVICE SIMULATION TOOLBAR (Visible when Device Mode is active) ---
        AnimatedVisibility(
            visible = deviceModeActive,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            com.mrcoder20.portx.presentation.ui.components.DeviceSimulatorToolbar(
                selectedPreset = selectedDevicePreset,
                onSelectPreset = { selectedDevicePreset = it },
                isLandscape = isLandscapeOrientation,
                onToggleOrientation = { isLandscapeOrientation = !isLandscapeOrientation },
                showFrame = showDeviceFrameBezel,
                onToggleFrame = { showDeviceFrameBezel = !showDeviceFrameBezel },
                onClose = { deviceModeActive = false },
                onSnapWindow = onSnapWindow,
                lang = lang
            )
        }

        // --- MAIN BODY (Normal Adaptive OR Device Frame Emulated) ---
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (deviceModeActive) {
                com.mrcoder20.portx.presentation.ui.components.DeviceChassis(
                    preset = selectedDevicePreset,
                    isLandscape = isLandscapeOrientation,
                    showFrame = showDeviceFrameBezel
                ) {
                    AppAdaptiveContent(
                        selectedTab = selectedTab,
                        onTabSelected = onTabSelected,
                        settings = settings,
                        isScanning = isScanning,
                        scanProgress = scanProgress,
                        content = content
                    )
                }
            } else {
                AppAdaptiveContent(
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected,
                    settings = settings,
                    isScanning = isScanning,
                    scanProgress = scanProgress,
                    content = content
                )
            }
        }
    }
}

@Composable
fun AppAdaptiveContent(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    settings: AppSettings,
    isScanning: Boolean,
    scanProgress: Int,
    content: @Composable () -> Unit
) {
    val lang = settings.language
    val isDark = settings.theme == "DARK"
    val accent = LocalAccentColor.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val width = maxWidth

        when {
            // 1. MOBILE COMPACT (< 640.dp): Full width + Bottom Nav Bar
            width < 640.dp -> {
                Scaffold(
                    containerColor = Color.Transparent,
                    bottomBar = {
                        InfiniteBottomBar(
                            selectedTab = selectedTab,
                            onTabSelected = onTabSelected,
                            lang = lang,
                            isScanning = isScanning
                        )
                    },
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { padding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(if (isDark) BackgroundDark else BackgroundLight)
                    ) {
                        LiquidGlowBackground()
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = padding.calculateBottomPadding())
                        ) {
                            content()
                        }
                    }
                }
            }

            // 2. TABLET ADAPTIVE RAIL (640.dp .. 920.dp): Compact 76.dp Rail + Spacious Content
            width <= 920.dp -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (isDark) BackgroundDark else BackgroundLight)
                ) {
                    LiquidGlowBackground()
                    Row(modifier = Modifier.fillMaxSize()) {
                        TabletNavRail(
                            selectedTab = selectedTab,
                            onTabSelected = onTabSelected,
                            isScanning = isScanning,
                            accent = accent,
                            isDark = isDark,
                            lang = lang
                        )

                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            content()
                        }
                    }
                }
            }

            // 3. EXPANDED DESKTOP (> 920.dp): Full Desktop Sidebar + Content
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (isDark) BackgroundDark else BackgroundLight)
                ) {
                    LiquidGlowBackground()
                    Row(modifier = Modifier.fillMaxSize()) {
                        DesktopSidebar(
                            selectedTab = selectedTab,
                            onTabSelected = onTabSelected,
                            isScanning = isScanning,
                            scanProgress = scanProgress,
                            accent = accent,
                            isDark = isDark,
                            lang = lang
                        )

                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            content()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TabletNavRail(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    isScanning: Boolean,
    accent: Color,
    isDark: Boolean,
    lang: String
) {
    Surface(
        modifier = Modifier
            .width(76.dp)
            .fillMaxHeight()
            .padding(vertical = 12.dp, horizontal = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, if (isDark) GlassBorder else GlassBorderLight, RoundedCornerShape(20.dp)),
        color = if (isDark) GlassBackground else GlassLight
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Mini Brand Logo
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.15f))
                    .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Radar, null, tint = accent, modifier = Modifier.size(22.dp))
            }

            // Nav Icons
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TabletRailItem(Icons.Default.Home, LocalizedStrings.get("dashboard", lang), selectedTab == 0, isScanning) { onTabSelected(0) }
                TabletRailItem(Icons.Default.Build, LocalizedStrings.get("tools", lang), selectedTab == 1) { onTabSelected(1) }
                TabletRailItem(Icons.AutoMirrored.Filled.List, LocalizedStrings.get("reports", lang), selectedTab == 2) { onTabSelected(2) }
                TabletRailItem(Icons.Default.Settings, LocalizedStrings.get("settings", lang), selectedTab == 3) { onTabSelected(3) }
            }

            // Live Engine Pulse Dot
            val infiniteTransition = rememberInfiniteTransition()
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse)
            )
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background((if (isScanning) PrimaryNeon else TertiaryNeon).copy(alpha = pulseAlpha))
                    .border(1.5.dp, if (isScanning) PrimaryNeon else TertiaryNeon, CircleShape)
            )
        }
    }
}

@Composable
fun TabletRailItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    hasBadge: Boolean = false,
    onClick: () -> Unit
) {
    val accent = LocalAccentColor.current
    val isDark = LocalAppSettings.current.theme == "DARK"
    Surface(
        onClick = onClick,
        modifier = Modifier.size(48.dp).springPress(pressedScale = 0.90f),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) accent.copy(alpha = 0.18f) else Color.Transparent,
        border = BorderStroke(1.dp, if (isSelected) accent else Color.Transparent)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (isSelected) accent else (if (isDark) TextMuted else TextMutedLight),
                modifier = Modifier.size(22.dp)
            )
            if (hasBadge) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .background(PrimaryNeon, CircleShape)
                )
            }
        }
    }
}

@Composable
fun DesktopSidebar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    isScanning: Boolean,
    scanProgress: Int,
    accent: Color,
    isDark: Boolean,
    lang: String
) {
    Surface(
        modifier = Modifier
            .width(260.dp)
            .fillMaxHeight()
            .padding(14.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, if (isDark) GlassBorder else GlassBorderLight, RoundedCornerShape(24.dp)),
        color = if (isDark) GlassBackground else GlassLight
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Brand Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 12.dp, top = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accent.copy(alpha = 0.15f))
                            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Radar, null, tint = accent, modifier = Modifier.size(24.dp))
                    }
                    Column {
                        Text(
                            "PortX",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = if (isDark) Color.White else Color.Black,
                                shadow = Shadow(color = accent.copy(alpha = 0.7f), blurRadius = 10f)
                            )
                        )
                        Text(
                            "SECURITY SUITE v5.2",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                letterSpacing = 1.sp,
                                color = if (isDark) TextMuted else TextMutedLight
                            )
                        )
                    }
                }

                HorizontalDivider(
                    color = (if (isDark) GlassBorder else GlassBorderLight).copy(alpha = 0.35f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                DesktopNavItem(
                    icon = Icons.Default.Home,
                    label = LocalizedStrings.get("dashboard", lang),
                    isSelected = selectedTab == 0,
                    hasBadge = isScanning,
                    badgeText = if (isScanning) "$scanProgress%" else null
                ) { onTabSelected(0) }

                DesktopNavItem(
                    icon = Icons.Default.Build,
                    label = LocalizedStrings.get("tools", lang),
                    isSelected = selectedTab == 1
                ) { onTabSelected(1) }

                DesktopNavItem(
                    icon = Icons.AutoMirrored.Filled.List,
                    label = LocalizedStrings.get("reports", lang),
                    isSelected = selectedTab == 2
                ) { onTabSelected(2) }

                DesktopNavItem(
                    icon = Icons.Default.Settings,
                    label = LocalizedStrings.get("settings", lang),
                    isSelected = selectedTab == 3
                ) { onTabSelected(3) }
            }

            // Engine Telemetry Footer HUD
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = (if (isDark) Color.Black else Color.White).copy(alpha = 0.28f),
                border = BorderStroke(1.dp, (if (isDark) GlassBorder else GlassBorderLight).copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val infiniteTransition = rememberInfiniteTransition()
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse)
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (isScanning) PrimaryNeon.copy(alpha = pulseAlpha)
                                else TertiaryNeon.copy(alpha = pulseAlpha)
                            )
                            .border(
                                2.dp,
                                if (isScanning) PrimaryNeon.copy(alpha = 0.4f)
                                else TertiaryNeon.copy(alpha = 0.4f),
                                CircleShape
                            )
                    )
                    Column {
                        Text(
                            if (isScanning) LocalizedStrings.get("engine_active", lang)
                            else LocalizedStrings.get("engine_standby", lang),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                            color = if (isDark) Color.White else Color.Black
                        )
                        Text(
                            "Async NIO • TCP/UDP",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp
                            ),
                            color = if (isDark) TextMuted else TextMutedLight
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InfiniteBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    lang: String,
    isScanning: Boolean = false
) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(if (isDark) SurfaceDark.copy(alpha = 0.92f) else SurfaceLight.copy(alpha = 0.92f))
            .border(1.dp, (if (isDark) GlassBorder else GlassBorderLight).copy(alpha = 0.45f), RoundedCornerShape(32.dp)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                icon = Icons.Default.Home,
                label = LocalizedStrings.get("dashboard", lang),
                isSelected = selectedTab == 0,
                isDark = isDark,
                hasDot = isScanning,
                onClick = { onTabSelected(0) }
            )
            BottomNavItem(
                icon = Icons.Default.Build,
                label = LocalizedStrings.get("tools", lang),
                isSelected = selectedTab == 1,
                isDark = isDark,
                onClick = { onTabSelected(1) }
            )
            BottomNavItem(
                icon = Icons.AutoMirrored.Filled.List,
                label = LocalizedStrings.get("reports", lang),
                isSelected = selectedTab == 2,
                isDark = isDark,
                onClick = { onTabSelected(2) }
            )
            BottomNavItem(
                icon = Icons.Default.Settings,
                label = LocalizedStrings.get("settings", lang),
                isSelected = selectedTab == 3,
                isDark = isDark,
                onClick = { onTabSelected(3) }
            )
        }
    }
}

@Composable
fun WindowControlBtn(icon: ImageVector, color: Color, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isClose = color == DangerNeon

    val bgBrush = if (isClose && isHovered) {
        Color.Red.copy(alpha = 0.85f)
    } else if (isHovered) {
        color.copy(alpha = 0.2f)
    } else {
        color.copy(alpha = 0.05f)
    }

    val animatedScale by animateFloatAsState(
        targetValue = if (isHovered) 1.08f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
    )

    Box(
        modifier = Modifier
            .size(32.dp)
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .clip(CircleShape)
            .background(bgBrush)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isClose && isHovered) Color.White else color.copy(alpha = if (isHovered) 1f else 0.7f),
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
fun RowScope.BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    isDark: Boolean,
    hasDot: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val accent = LocalAccentColor.current
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .graphicsLayer {
                        scaleX = animatedScale
                        scaleY = animatedScale
                    }
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        brush = if (isSelected) {
                            Brush.linearGradient(listOf(accent.copy(alpha = 0.25f), accent.copy(alpha = 0.1f)))
                        } else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                    )
                    .border(
                        1.dp,
                        if (isSelected) accent.copy(alpha = 0.5f) else Color.Transparent,
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isSelected) accent else (if (isDark) TextMuted else TextMutedLight),
                    modifier = Modifier.size(if (isSelected) 24.dp else 22.dp)
                )

                if (hasDot) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = (-4).dp, y = 4.dp)
                            .clip(CircleShape)
                            .background(PrimaryNeon)
                    )
                }
            }
        }
    }
}

@Composable
fun DesktopNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    hasBadge: Boolean = false,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    val accent = LocalAccentColor.current
    val isDark = LocalAppSettings.current.theme == "DARK"
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val bgBrush = if (isSelected) {
        Brush.horizontalGradient(
            listOf(accent.copy(alpha = 0.18f), accent.copy(alpha = 0.05f))
        )
    } else if (isHovered) {
        Brush.horizontalGradient(
            listOf(
                (if (isDark) Color.White else Color.Black).copy(alpha = 0.08f),
                (if (isDark) Color.White else Color.Black).copy(alpha = 0.02f)
            )
        )
    } else {
        Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
    }

    val animatedScale by animateFloatAsState(
        targetValue = if (isHovered) 1.02f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium)
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .clip(RoundedCornerShape(14.dp))
            .border(
                1.dp,
                if (isSelected) accent.copy(alpha = 0.45f)
                else if (isHovered) (if (isDark) GlassBorder else GlassBorderLight).copy(alpha = 0.6f)
                else Color.Transparent,
                RoundedCornerShape(14.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        color = Color.Transparent,
        shape = RoundedCornerShape(14.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgBrush),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Active indicator pill
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(accent)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) accent.copy(alpha = 0.2f) else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) accent else (if (isDark) TextSecondary else TextSecondaryLight),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = label,
                    color = if (isSelected) (if (isDark) Color.White else Color.Black)
                    else (if (isDark) TextSecondary else TextSecondaryLight),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    modifier = Modifier.weight(1f)
                )

                if (hasBadge && !badgeText.isNullOrBlank()) {
                    CyberBadge(
                        text = badgeText,
                        color = accent,
                        hasPulseDot = true,
                        fontSize = 9f
                    )
                }
            }
        }
    }
}
