package com.mrcoder20.portx.presentation.ui

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrcoder20.portx.domain.AppSettings
import com.mrcoder20.portx.domain.LocalizedStrings
import com.mrcoder20.portx.presentation.ui.theme.*

@Composable
fun NavigationWrapper(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    settings: com.mrcoder20.portx.domain.AppSettings,
    onMinimize: () -> Unit = {},
    onMaximize: () -> Unit = {},
    onClose: () -> Unit = {},
    windowDraggableArea: @Composable (@Composable () -> Unit) -> Unit = { it() },
    content: @Composable () -> Unit
) {
    val accent = LocalAccentColor.current
    val lang = settings.language

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpanded = maxWidth > 600.dp

        if (isExpanded) {
            // Desktop UI
            Column(modifier = Modifier.fillMaxSize().background(if(settings.theme == "DARK") BackgroundDark else Color.White)) {
                // --- CUSTOM TOP TITLE BAR (Always LTR for native Windows layout consistency) ---
                CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
                    windowDraggableArea {
                        Surface(
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            color = if(settings.theme == "DARK") SurfaceDark else SurfaceLight,
                            border = BorderStroke(0.5.dp, (if (settings.theme == "DARK") GlassBorder else GlassBorderLight).copy(alpha = 0.2f))
                        ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Radar, null, tint = accent, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "PortX Professional",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (settings.theme == "DARK") Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f)
                                    )
                                )
                            }
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                WindowControlBtn(Icons.Default.Remove, if(settings.theme == "DARK") TextMuted else TextMutedLight, onMinimize)
                                WindowControlBtn(Icons.Default.AspectRatio, if(settings.theme == "DARK") TextMuted else TextMutedLight, onMaximize)
                                WindowControlBtn(Icons.Default.Close, DangerNeon, onClose)
                            }
                        }
                    }
                }
            }

                Box(modifier = Modifier.weight(1f)) {
                    LiquidGlowBackground()
                    Row(modifier = Modifier.fillMaxSize()) {
                        Surface(
                            modifier = Modifier
                                .width(300.dp)
                                .fillMaxHeight()
                                .padding(16.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .border(1.dp, if (settings.theme == "DARK") GlassBorder else GlassBorderLight, RoundedCornerShape(24.dp)),
                            color = if (settings.theme == "DARK") GlassBackground else GlassLight
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    // Brand Header
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.padding(bottom = 14.dp, top = 4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(accent.copy(alpha = 0.15f))
                                                .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Radar, null, tint = accent, modifier = Modifier.size(22.dp))
                                        }
                                        Column {
                                            Text(
                                                "PortX",
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    fontWeight = FontWeight.Black,
                                                    color = if (settings.theme == "DARK") Color.White else Color.Black,
                                                    shadow = Shadow(color = accent.copy(alpha = 0.8f), blurRadius = 12f)
                                                )
                                            )
                                            Text(
                                                "ENTERPRISE SUITE v5.2",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                    fontSize = 9.sp,
                                                    letterSpacing = 1.sp,
                                                    color = if (settings.theme == "DARK") TextMuted else TextMutedLight
                                                )
                                            )
                                        }
                                    }

                                    HorizontalDivider(
                                        color = (if (settings.theme == "DARK") GlassBorder else GlassBorderLight).copy(alpha = 0.4f),
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )

                                    DesktopNavItem(Icons.Default.Home, LocalizedStrings.get("dashboard", lang), selectedTab == 0) { onTabSelected(0) }
                                    DesktopNavItem(Icons.Default.Build, LocalizedStrings.get("tools", lang), selectedTab == 1) { onTabSelected(1) }
                                    DesktopNavItem(Icons.AutoMirrored.Filled.List, LocalizedStrings.get("reports", lang), selectedTab == 2) { onTabSelected(2) }
                                    DesktopNavItem(Icons.Default.Settings, LocalizedStrings.get("settings", lang), selectedTab == 3) { onTabSelected(3) }
                                }

                                // Engine Telemetry Footer
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = (if (settings.theme == "DARK") Color.Black else Color.White).copy(alpha = 0.25f),
                                    border = BorderStroke(1.dp, (if (settings.theme == "DARK") GlassBorder else GlassBorderLight).copy(alpha = 0.4f)),
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
                                                .background(TertiaryNeon.copy(alpha = pulseAlpha))
                                                .border(2.dp, TertiaryNeon.copy(alpha = 0.4f), CircleShape)
                                        )
                                        Column {
                                            Text(
                                                LocalizedStrings.get("engine_standby", lang),
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                                color = if (settings.theme == "DARK") Color.White else Color.Black
                                            )
                                            Text(
                                                "Local Engine • TCP/UDP",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                    fontSize = 9.sp
                                                ),
                                                color = if (settings.theme == "DARK") TextMuted else TextMutedLight
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            content()
                        }
                    }
                }
            }
        } else {
            // Mobile UI
            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    InfiniteBottomBar(
                        selectedTab = selectedTab,
                        onTabSelected = onTabSelected,
                        lang = lang
                    )
                },
                contentWindowInsets = WindowInsets(0, 0, 0, 0)
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if(settings.theme == "DARK") BackgroundDark else Color.White)
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
    }
}

@Composable
fun InfiniteBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    lang: String
) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 12.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(if (isDark) SurfaceDark.copy(alpha = 0.9f) else GlassLight)
            .border(1.dp, (if (isDark) GlassBorder else GlassBorderLight).copy(alpha = 0.4f), RoundedCornerShape(32.dp)),
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
            }
        }
    }
}

@Composable
fun DesktopNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
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
                if (isSelected) accent.copy(alpha = 0.4f)
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
                horizontalArrangement = Arrangement.spacedBy(14.dp)
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
                    )
                )
            }
        }
    }
}
