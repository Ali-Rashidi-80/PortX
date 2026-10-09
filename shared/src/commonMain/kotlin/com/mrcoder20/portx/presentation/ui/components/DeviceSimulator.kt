package com.mrcoder20.portx.presentation.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import com.mrcoder20.portx.domain.LocalizedStrings
import com.mrcoder20.portx.presentation.ui.CyberBadge
import com.mrcoder20.portx.presentation.ui.theme.*

expect fun getDeviceEmulationPointerIcon(): PointerIcon

enum class DeviceCategory {
    RESPONSIVE,
    PHONE,
    TABLET,
    FOLDABLE,
    DESKTOP
}

data class DevicePreset(
    val id: String,
    val name: String,
    val width: Int, // dp
    val height: Int, // dp
    val category: DeviceCategory
) {
    companion object {
        val RESPONSIVE = DevicePreset("responsive", "Responsive", 0, 0, DeviceCategory.RESPONSIVE)

        val ALL_PRESETS = listOf(
            RESPONSIVE,
            // Apple Phones
            DevicePreset("iphone_se", "iPhone SE", 375, 667, DeviceCategory.PHONE),
            DevicePreset("iphone_xr", "iPhone XR", 414, 896, DeviceCategory.PHONE),
            DevicePreset("iphone_12_pro", "iPhone 12 Pro", 390, 844, DeviceCategory.PHONE),
            DevicePreset("iphone_14_pro_max", "iPhone 14 Pro Max", 430, 932, DeviceCategory.PHONE),
            DevicePreset("iphone_15_pro_max", "iPhone 15 Pro Max", 430, 932, DeviceCategory.PHONE),
            DevicePreset("iphone_16_pro_max", "iPhone 16 Pro Max", 440, 956, DeviceCategory.PHONE),

            // Google Pixel Phones
            DevicePreset("pixel_7", "Pixel 7", 412, 915, DeviceCategory.PHONE),
            DevicePreset("pixel_8", "Pixel 8", 412, 915, DeviceCategory.PHONE),
            DevicePreset("pixel_9", "Pixel 9", 412, 923, DeviceCategory.PHONE),
            DevicePreset("pixel_10", "Pixel 10", 412, 930, DeviceCategory.PHONE),

            // Samsung Phones
            DevicePreset("samsung_s8", "Samsung Galaxy S8+", 360, 740, DeviceCategory.PHONE),
            DevicePreset("samsung_s20_ultra", "Samsung Galaxy S20 Ultra", 412, 915, DeviceCategory.PHONE),
            DevicePreset("samsung_a51", "Samsung Galaxy A51/71", 412, 914, DeviceCategory.PHONE),

            // Foldable Devices
            DevicePreset("galaxy_z_fold_5", "Galaxy Z Fold 5", 344, 882, DeviceCategory.FOLDABLE),
            DevicePreset("surface_duo", "Surface Duo", 540, 720, DeviceCategory.FOLDABLE),
            DevicePreset("asus_zenbook_fold", "Asus Zenbook Fold", 853, 1280, DeviceCategory.FOLDABLE),

            // Tablets & Hubs
            DevicePreset("ipad_mini", "iPad Mini", 768, 1024, DeviceCategory.TABLET),
            DevicePreset("ipad_air", "iPad Air", 820, 1180, DeviceCategory.TABLET),
            DevicePreset("ipad_pro", "iPad Pro", 1024, 1366, DeviceCategory.TABLET),
            DevicePreset("surface_pro_7", "Surface Pro 7", 912, 1368, DeviceCategory.TABLET),
            DevicePreset("nest_hub", "Nest Hub", 1024, 600, DeviceCategory.TABLET),
            DevicePreset("nest_hub_max", "Nest Hub Max", 1280, 800, DeviceCategory.TABLET),

            // Desktop & Laptop Viewports
            DevicePreset("laptop_hd", "Laptop (1366×768)", 1366, 768, DeviceCategory.DESKTOP),
            DevicePreset("desktop_fhd", "Desktop (1920×1080)", 1920, 1080, DeviceCategory.DESKTOP),
            DevicePreset("macbook_air", "MacBook Air (1440×900)", 1440, 900, DeviceCategory.DESKTOP)
        )
    }
}

@Composable
fun DeviceSimulatorToolbar(
    selectedPreset: DevicePreset,
    onSelectPreset: (DevicePreset) -> Unit,
    isLandscape: Boolean,
    onToggleOrientation: () -> Unit,
    showFrame: Boolean,
    onToggleFrame: () -> Unit,
    onClose: () -> Unit,
    onSnapWindow: ((Int, Int) -> Unit)? = null,
    lang: String = "en"
) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    val accent = LocalAccentColor.current
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth().height(44.dp),
        color = if (isDark) SurfaceDark.copy(alpha = 0.95f) else Color.White,
        border = BorderStroke(1.dp, if (isDark) GlassBorder.copy(alpha = 0.4f) else BorderLight)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Start: Close Button (Right in RTL / Persian, Left in LTR) + Model Selector
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Close Device Mode Button
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp).pointerHoverIcon(PointerIcon.Hand)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = LocalizedStrings.get("close", lang),
                        tint = DangerNeon,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(20.dp)
                        .background(if (isDark) GlassBorder.copy(alpha = 0.5f) else BorderLight)
                )

                Icon(
                    imageVector = when (selectedPreset.category) {
                        DeviceCategory.TABLET -> Icons.Default.Tablet
                        DeviceCategory.FOLDABLE -> Icons.Default.ScreenLockLandscape
                        DeviceCategory.RESPONSIVE -> Icons.Default.AspectRatio
                        DeviceCategory.DESKTOP -> Icons.Default.Devices
                        else -> Icons.Default.Smartphone
                    },
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )

                Box {
                    Surface(
                        onClick = { showMenu = true },
                        modifier = Modifier
                            .then(
                                if (isDark) Modifier else Modifier.shadow(
                                    elevation = 1.dp,
                                    shape = RoundedCornerShape(8.dp),
                                    spotColor = Color(0x100F172A),
                                    ambientColor = Color(0x0A0F172A)
                                )
                            )
                            .pointerHoverIcon(PointerIcon.Hand),
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.White,
                        border = BorderStroke(1.dp, if (isDark) accent.copy(alpha = 0.35f) else BorderLight)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (selectedPreset.category == DeviceCategory.RESPONSIVE) LocalizedStrings.get("device_responsive", lang) else selectedPreset.name,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isDark) Color.White else Color.Black
                            )
                            Icon(Icons.Default.ArrowDropDown, null, tint = accent, modifier = Modifier.size(14.dp))
                        }
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier
                            .heightIn(max = 380.dp)
                            .background(if (isDark) SurfaceDark else Color.White)
                            .border(1.dp, if (isDark) GlassBorder else BorderLight, RoundedCornerShape(8.dp))
                    ) {
                        DevicePreset.ALL_PRESETS.forEach { preset ->
                            val isSelected = preset.id == selectedPreset.id
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            if (preset.category == DeviceCategory.RESPONSIVE) LocalizedStrings.get("device_responsive", lang) else preset.name,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) accent else (if (isDark) Color.White else Color.Black),
                                            fontSize = 12.sp
                                        )
                                        if (preset.category != DeviceCategory.RESPONSIVE) {
                                            Text(
                                                "${preset.width} × ${preset.height}",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp,
                                                color = if (isDark) TextMuted else TextMutedLight,
                                                modifier = Modifier.padding(start = 12.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    onSelectPreset(preset)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = when (preset.category) {
                                            DeviceCategory.TABLET -> Icons.Default.Tablet
                                            DeviceCategory.FOLDABLE -> Icons.Default.ScreenLockLandscape
                                            DeviceCategory.RESPONSIVE -> Icons.Default.AspectRatio
                                            DeviceCategory.DESKTOP -> Icons.Default.Devices
                                            else -> Icons.Default.Smartphone
                                        },
                                        contentDescription = null,
                                        tint = if (isSelected) accent else (if (isDark) TextMuted else TextMutedLight),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }

                // Dimension Badge
                if (selectedPreset.category != DeviceCategory.RESPONSIVE) {
                    val effectiveW = if (isLandscape) selectedPreset.height else selectedPreset.width
                    val effectiveH = if (isLandscape) selectedPreset.width else selectedPreset.height
                    Surface(
                        color = SecondaryNeon.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, SecondaryNeon.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "\u200E$effectiveW × $effectiveH dp\u200E",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = SecondaryNeon,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Right: Touch Mode Badge, Orientation, Frame Toggle, Snap Window, Close
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Interactive Touch Emulation Mode Indicator Badge (Only for touch devices)
                if (selectedPreset.category != DeviceCategory.DESKTOP) {
                    CyberBadge(
                        text = LocalizedStrings.get("touch_emulation_active", lang),
                        color = TertiaryNeon,
                        hasPulseDot = true,
                        fontSize = 9f
                    )
                }

                if (selectedPreset.category != DeviceCategory.RESPONSIVE) {
                    // Orientation Toggle Button
                    IconButton(
                        onClick = onToggleOrientation,
                        modifier = Modifier.size(32.dp).pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Icon(
                            Icons.Default.ScreenRotation,
                            contentDescription = LocalizedStrings.get("rotate_device", lang),
                            tint = if (isLandscape) SecondaryNeon else (if (isDark) Color.White else Color.Black),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Device Bezel Frame Toggle
                    IconButton(
                        onClick = onToggleFrame,
                        modifier = Modifier.size(32.dp).pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Icon(
                            if (showFrame) Icons.Default.PhoneIphone else Icons.Default.CropFree,
                            contentDescription = LocalizedStrings.get("device_frame", lang),
                            tint = if (showFrame) TertiaryNeon else (if (isDark) TextMuted else TextMutedLight),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Snap Window Button
                    if (onSnapWindow != null) {
                        val effectiveW = if (isLandscape) selectedPreset.height else selectedPreset.width
                        val effectiveH = if (isLandscape) selectedPreset.width else selectedPreset.height
                        IconButton(
                            onClick = { onSnapWindow(effectiveW, effectiveH) },
                            modifier = Modifier.size(32.dp).pointerHoverIcon(PointerIcon.Hand)
                        ) {
                            Icon(
                                Icons.Default.FitScreen,
                                contentDescription = LocalizedStrings.get("snap_window", lang),
                                tint = accent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceChassis(
    preset: DevicePreset,
    isLandscape: Boolean,
    showFrame: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    val accent = LocalAccentColor.current

    if (preset.category == DeviceCategory.RESPONSIVE) {
        CompositionLocalProvider(LocalTouchEmulation provides true) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .pointerHoverIcon(getDeviceEmulationPointerIcon())
            ) {
                content()
            }
        }
        return
    }

    val targetW = if (isLandscape) preset.height.dp else preset.width.dp
    val targetH = if (isLandscape) preset.width.dp else preset.height.dp
    val cornerRadius = when (preset.category) {
        DeviceCategory.TABLET -> 24.dp
        DeviceCategory.FOLDABLE -> 20.dp
        DeviceCategory.DESKTOP -> 12.dp
        else -> 36.dp
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF030712) else Color(0xFFE2E8F0)),
        contentAlignment = Alignment.Center
    ) {
        val availableW = maxWidth
        val availableH = maxHeight
        val margin = if (showFrame) 36.dp else 16.dp
        val maxAllowedW = (availableW - margin).coerceAtLeast(100.dp)
        val maxAllowedH = (availableH - margin).coerceAtLeast(100.dp)

        val computedScale = if (targetW > maxAllowedW || targetH > maxAllowedH) {
            val scaleW = maxAllowedW / targetW
            val scaleH = maxAllowedH / targetH
            minOf(scaleW, scaleH).coerceIn(0.2f, 1f)
        } else {
            1f
        }

        // Device Chassis Container with drop shadow and border
        Surface(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = computedScale
                    scaleY = computedScale
                    transformOrigin = TransformOrigin.Center
                }
                .width(targetW)
                .height(targetH)
                .shadow(16.dp, RoundedCornerShape(if (showFrame) cornerRadius else 0.dp))
                .clip(RoundedCornerShape(if (showFrame) cornerRadius else 0.dp))
                .border(
                    width = if (showFrame) 4.dp else 1.dp,
                    color = if (showFrame) (if (isDark) Color(0xFF1E293B) else Color(0xFF94A3B8)) else accent.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(if (showFrame) cornerRadius else 0.dp)
                ),
            color = if (isDark) BackgroundDark else BackgroundLight
        ) {
            val isTouchEmulated = preset.category != DeviceCategory.DESKTOP
            CompositionLocalProvider(LocalTouchEmulation provides isTouchEmulated) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (isTouchEmulated) Modifier.pointerHoverIcon(getDeviceEmulationPointerIcon()) else Modifier)
                ) {
                    // Main Application Screen Content
                    content()

                    // Realistic Simulated Bezel Details (Camera / Dynamic Island & Home Indicator)
                    if (showFrame && preset.category == DeviceCategory.PHONE && !isLandscape) {
                        // Top Speaker / Camera Pill (Visual only)
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 8.dp)
                                .width(88.dp)
                                .height(20.dp)
                                .background(Color.Black, CircleShape)
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFF1F2937), CircleShape))
                        }

                        // Bottom Home Indicator Line (Visual only)
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 6.dp)
                                .width(110.dp)
                                .height(4.dp)
                                .background((if (isDark) Color.White else Color.Black).copy(alpha = 0.35f), CircleShape)
                        )
                    }
                }
            }
        }
    }
}
