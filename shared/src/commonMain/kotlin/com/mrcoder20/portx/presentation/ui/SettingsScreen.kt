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
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import com.mrcoder20.portx.presentation.ui.components.LaunchedAutoScrollHint
import com.mrcoder20.portx.presentation.ui.components.PortXScrollStateVerticalScrollbar
import com.mrcoder20.portx.presentation.ui.components.horizontalFadingEdges
import com.mrcoder20.portx.presentation.ui.components.springPress
import com.mrcoder20.portx.presentation.ui.components.touchDragScroll
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
    val isRtl = state.language == "fa"


    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .absolutePadding(
                    right = if (isRtl) 14.dp else 0.dp,
                    left = if (!isRtl) 14.dp else 0.dp
                )
                .touchDragScroll(scrollState, isVertical = true)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            // 1. LANGUAGE SELECTOR (3 BUTTONS IN A ROW)
            SettingsSectionTitle(LocalizedStrings.get("language", state.language))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Language.entries.forEach { langItem ->
                    val isSelected = state.language == langItem.code
                    Surface(
                        onClick = { settingsManager.updateLanguage(langItem.code) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) accent.copy(alpha = 0.15f) else (if (isDark) Color.White.copy(alpha = 0.04f) else Color.White),
                        border = BorderStroke(1.dp, if (isSelected) accent else (if (isDark) GlassBorder else BorderLight)),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .then(
                                if (isDark || isSelected) Modifier else Modifier.shadow(
                                    elevation = 1.5.dp,
                                    shape = RoundedCornerShape(12.dp),
                                    ambientColor = Color(0x100F172A),
                                    spotColor = Color(0x140F172A)
                                )
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                color = if (isSelected) accent else (if (isDark) Color.White.copy(alpha = 0.1f) else SurfaceInsetLight),
                                shape = RoundedCornerShape(6.dp),
                                border = if (isSelected) null else BorderStroke(1.dp, if (isDark) Color.Transparent else BorderLight)
                            ) {
                                Text(
                                    langItem.code.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = if (isSelected) Color.Black else (if (isDark) Color.White else Color.Black),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }

                            Spacer(Modifier.width(6.dp))

                            Text(
                                langItem.label,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.5.sp
                                ),
                                color = if (isSelected) (if (isDark) Color.White else Color.Black) else (if (isDark) TextMuted else TextMutedLight),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 2. APPEARANCE (Theme Mode & Accent Color)
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
                            color = if (isDark) accent.copy(alpha = 0.15f) else (if (isDark) Color.White.copy(alpha = 0.04f) else Color.White),
                            border = BorderStroke(1.dp, if (isDark) accent else (if (isDark) GlassBorder else BorderLight)),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .then(
                                    if (isDark) Modifier else Modifier.shadow(
                                        elevation = 1.5.dp,
                                        shape = RoundedCornerShape(12.dp),
                                        ambientColor = Color(0x100F172A),
                                        spotColor = Color(0x140F172A)
                                    )
                                )
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
                            color = if (!isDark) accent.copy(alpha = 0.15f) else (if (isDark) Color.White.copy(alpha = 0.04f) else Color.White),
                            border = BorderStroke(1.dp, if (!isDark) accent else (if (isDark) GlassBorder else BorderLight)),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .then(
                                    if (isDark) Modifier else Modifier.shadow(
                                        elevation = 1.5.dp,
                                        shape = RoundedCornerShape(12.dp),
                                        ambientColor = Color(0x100F172A),
                                        spotColor = Color(0x140F172A)
                                    )
                                )
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

                    HorizontalDivider(color = if (isDark) GlassBorder.copy(alpha = 0.4f) else BorderLight)

                    // Accent Colors
                    Column {
                        Text(
                            LocalizedStrings.get("accent", state.language),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isDark) TextMuted else TextMutedLight
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        val colorsScrollState = rememberScrollState()
                        LaunchedAutoScrollHint(colorsScrollState)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalFadingEdges(colorsScrollState, fadeWidth = 14.dp, isRtl = isRtl)
                                .touchDragScroll(colorsScrollState, isVertical = false, isRtl = isRtl)
                                .horizontalScroll(colorsScrollState),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                            Spacer(Modifier.width(8.dp))
                        }
                    }
                }
            }

            // 3. COMMUNICATION & COMMUNITY
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
                        LocalizedStrings.get("app_footer_build", state.language),
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
                .align(if (isRtl) AbsoluteAlignment.CenterRight else AbsoluteAlignment.CenterLeft)
                .fillMaxHeight()
        )

    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    val isRtl = LocalAppSettings.current.language == "fa"
    Text(
        text = if (isRtl) title else title.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            color = if (isDark) TextMuted else Color.Black.copy(alpha = 0.5f),
            fontWeight = FontWeight.Bold,
            letterSpacing = if (isRtl) 0.sp else 1.sp
        ),
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

@Composable
fun ColorCircle(color: Color, isSelected: Boolean, onClick: () -> Unit) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .springPress(pressedScale = 0.92f, interactionSource = interactionSource)
            .pointerHoverIcon(PointerIcon.Hand),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .border(2.dp, color, CircleShape)
            )
        }
        Box(
            modifier = Modifier
                .size(30.dp)
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
            .then(
                if (isDark) Modifier else Modifier.shadow(
                    elevation = 2.dp,
                    shape = RoundedCornerShape(14.dp),
                    ambientColor = Color(0x100F172A),
                    spotColor = Color(0x140F172A)
                )
            )
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, if (isDark) GlassBorder else BorderLight, RoundedCornerShape(14.dp))
            .springPress(pressedScale = 0.96f)
            .pointerHoverIcon(PointerIcon.Hand),
        color = if (isDark) Color.White.copy(alpha = 0.04f) else Color.White,
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
                color = if (isDark) Color.White else Color.Black,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}
