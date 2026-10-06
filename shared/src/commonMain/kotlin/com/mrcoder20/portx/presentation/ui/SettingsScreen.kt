package com.mrcoder20.portx.presentation.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrcoder20.portx.domain.Language
import com.mrcoder20.portx.domain.LocalizedStrings
import com.mrcoder20.portx.domain.SettingsManager
import com.mrcoder20.portx.presentation.ui.components.PortXScrollStateVerticalScrollbar
import com.mrcoder20.portx.presentation.ui.theme.*
import org.koin.compose.koinInject

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(settingsManager: SettingsManager = koinInject()) {
    val state by settingsManager.settings.collectAsState()
    val accent = LocalAccentColor.current
    val isDark = state.theme == "DARK"
    val uriHandler = LocalUriHandler.current
    val githubUrl = "https://github.com/mr-coder20/PortX"
    val scrollState = rememberScrollState()
    val isRtl = state.language == "fa" || state.language == "ar"


    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(
                    start = if (isRtl) 14.dp else 0.dp,
                    end = if (!isRtl) 14.dp else 0.dp
                )
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            // 1. LANGUAGE SELECTOR (RESPONSIVE FLOW GRID)
            SettingsSectionTitle(LocalizedStrings.get("language", state.language))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Language.entries.forEach { langItem ->
                    val isSelected = state.language == langItem.code
                    Surface(
                        onClick = { settingsManager.updateLanguage(langItem.code) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) accent.copy(alpha = 0.15f) else (if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.03f)),
                        border = BorderStroke(1.dp, if (isSelected) accent else (if (isDark) GlassBorder else GlassBorderLight)),
                        modifier = Modifier.widthIn(min = 100.dp, max = 140.dp).height(64.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = if (isSelected) accent else (if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    langItem.code.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = if (isSelected) Color.Black else (if (isDark) Color.White else Color.Black),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    langItem.label,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp
                                    ),
                                    color = if (isSelected) (if (isDark) Color.White else Color.Black) else (if (isDark) TextMuted else TextMutedLight),
                                    maxLines = 1
                                )
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(accent)
                                )
                            }
                        }
                    }
                }
            }

            // 2. SCAN ENGINE PERFORMANCE PRESETS
            SettingsSectionTitle(LocalizedStrings.get("scan_presets", state.language))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val presets = listOf(
                        Triple(LocalizedStrings.get("preset_fast", state.language), "500 Conns • 100ms", SecondaryNeon),
                        Triple(LocalizedStrings.get("preset_balanced", state.language), "100 Conns • 500ms", accent),
                        Triple(LocalizedStrings.get("preset_deep", state.language), "20 Conns • 1500ms", TertiaryNeon)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        presets.forEachIndexed { idx, (title, meta, pColor) ->
                            val isDefault = idx == 1
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.03f),
                                border = BorderStroke(1.dp, if (isDefault) pColor.copy(alpha = 0.5f) else (if (isDark) GlassBorder else GlassBorderLight)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        title,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isDark) Color.White else Color.Black
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        meta,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = pColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. APPEARANCE (Theme Mode & Accent Color)
            SettingsSectionTitle(LocalizedStrings.get("theme", state.language))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Theme Mode Selector Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Dark Mode Card
                        Surface(
                            onClick = { settingsManager.updateTheme("DARK") },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) accent.copy(alpha = 0.15f) else (if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.03f)),
                            border = BorderStroke(1.dp, if (isDark) accent else (if (isDark) GlassBorder else GlassBorderLight)),
                            modifier = Modifier.weight(1f).height(56.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.DarkMode, null, tint = if (isDark) accent else TextMuted, modifier = Modifier.size(18.dp))
                                Text(
                                    LocalizedStrings.get("dark", state.language),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (isDark) FontWeight.Bold else FontWeight.Normal),
                                    color = if (isDark) Color.White else Color.Black
                                )
                            }
                        }

                        // Light Mode Card
                        Surface(
                            onClick = { settingsManager.updateTheme("LIGHT") },
                            shape = RoundedCornerShape(12.dp),
                            color = if (!isDark) accent.copy(alpha = 0.15f) else (if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.03f)),
                            border = BorderStroke(1.dp, if (!isDark) accent else (if (isDark) GlassBorder else GlassBorderLight)),
                            modifier = Modifier.weight(1f).height(56.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.LightMode, null, tint = if (!isDark) accent else TextMuted, modifier = Modifier.size(18.dp))
                                Text(
                                    LocalizedStrings.get("light", state.language),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (!isDark) FontWeight.Bold else FontWeight.Normal),
                                    color = if (isDark) Color.White else Color.Black
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = if (isDark) GlassBorder.copy(alpha = 0.4f) else GlassBorderLight.copy(alpha = 0.4f))

                    // Accent Colors
                    Column {
                        Text(
                            LocalizedStrings.get("accent", state.language),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isDark) TextMuted else TextMutedLight
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val colors = listOf(
                                Color(0xFF00D1FF), // Cyber Blue
                                Color(0xFFBD00FF), // Neon Violet
                                Color(0xFF00FFA3), // Mint Emerald
                                Color(0xFFF0D400), // Electric Amber
                                Color(0xFFFF4B4B), // Crimson Red
                                Color(0xFF00B4D8)  // Deep Cyan
                            )
                            colors.forEach { color ->
                                ColorCircle(
                                    color = color,
                                    isSelected = state.accentColor == color,
                                    onClick = { settingsManager.updateAccentColor(color) }
                                )
                            }
                        }
                    }
                }
            }

            // 4. SYSTEM & CORE DIAGNOSTICS HUD
            SettingsSectionTitle(LocalizedStrings.get("system_info", state.language))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            LocalizedStrings.get("runtime_arch", state.language),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) TextMuted else TextMutedLight
                        )
                        Text(
                            "KMP • Compose Multiplatform 1.7.3",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = accent
                        )
                    }

                    HorizontalDivider(color = if (isDark) GlassBorder.copy(alpha = 0.3f) else GlassBorderLight.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Core Engine",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) TextMuted else TextMutedLight
                        )
                        Text(
                            "Asynchronous NIO Sockets",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = SecondaryNeon
                        )
                    }

                    HorizontalDivider(color = if (isDark) GlassBorder.copy(alpha = 0.3f) else GlassBorderLight.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Threat Intelligence",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) TextMuted else TextMutedLight
                        )
                        Text(
                            "NVD / CVE Embedded v2026.1",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = TertiaryNeon
                        )
                    }

                    HorizontalDivider(color = if (isDark) GlassBorder.copy(alpha = 0.3f) else GlassBorderLight.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Local Persistence",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) TextMuted else TextMutedLight
                        )
                        Text(
                            "Encrypted SQLite Local-First",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = WarningNeon
                        )
                    }
                }
            }

            // 5. COMMUNICATION & COMMUNITY
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CommunicationGlassButton(
                    label = LocalizedStrings.get("about", state.language),
                    icon = Icons.Default.Info,
                    modifier = Modifier.weight(1f)
                ) {
                    try { uriHandler.openUri(githubUrl) } catch (_: Exception) {}
                }
                CommunicationGlassButton(
                    label = LocalizedStrings.get("support", state.language),
                    icon = Icons.Default.HeadsetMic,
                    modifier = Modifier.weight(1f)
                ) {
                    try { uriHandler.openUri("$githubUrl/issues") } catch (_: Exception) {}
                }
                CommunicationGlassButton(
                    label = LocalizedStrings.get("feedback", state.language),
                    icon = Icons.Default.Feedback,
                    modifier = Modifier.weight(1f)
                ) {
                    try { uriHandler.openUri("$githubUrl/discussions") } catch (_: Exception) {}
                }
            }

            // Footer
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "PortX Professional Cyber Suite v5.2.1 • Production Build",
                        color = if (isDark) TextSecondary else Color.Black.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Icon(Icons.Default.Shield, null, tint = accent, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        PortXScrollStateVerticalScrollbar(
            scrollState = scrollState,
            modifier = Modifier
                .align(if (isRtl) AbsoluteAlignment.CenterLeft else AbsoluteAlignment.CenterRight)
                .fillMaxHeight()
        )

    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            color = if (isDark) TextMuted else Color.Black.copy(alpha = 0.5f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        ),
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

@Composable
fun ColorCircle(color: Color, isSelected: Boolean, onClick: () -> Unit) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    Box(
        modifier = Modifier
            .size(42.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(2.dp, color, CircleShape)
            )
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(color)
                .border(
                    if (isSelected) 2.dp else 0.dp,
                    if (isDark) Color.White.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.8f),
                    CircleShape
                )
        )
    }
}

@Composable
fun CommunicationGlassButton(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val accent = LocalAccentColor.current
    val isDark = LocalAppSettings.current.theme == "DARK"
    Surface(
        modifier = modifier
            .height(80.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, if (isDark) GlassBorder else GlassBorderLight, RoundedCornerShape(14.dp)),
        color = if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.02f),
        onClick = onClick
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = if (isDark) Color.White else Color.Black
            )
        }
    }
}
