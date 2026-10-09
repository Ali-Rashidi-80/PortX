package com.mrcoder20.portx.presentation.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrcoder20.portx.domain.LocalizedStrings
import com.mrcoder20.portx.presentation.ui.components.cyberPulse
import com.mrcoder20.portx.presentation.ui.components.springPress
import com.mrcoder20.portx.presentation.ui.theme.*

@Composable
fun LiquidGlowBackground() {
    val accent = LocalAccentColor.current
    val isDark = LocalAppSettings.current.theme == "DARK"
    var pointerOffset by remember { mutableStateOf(Offset.Zero) }
    var isTouching by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition()
    
    val breathAnim by infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearOutSlowInEasing), RepeatMode.Reverse)
    )

    val auroraShift by infiniteTransition.animateFloat(
        initialValue = -50f, targetValue = 50f,
        animationSpec = infiniteRepeatable(tween(11000, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )

    val ringScale by animateFloatAsState(
        targetValue = if (isTouching) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
    )
    val ringAlpha by animateFloatAsState(
        targetValue = if (isTouching) 0.35f else 0f,
        animationSpec = tween(300)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        isTouching = event.type != PointerEventType.Exit && event.type != PointerEventType.Release
                        if (event.changes.isNotEmpty()) {
                            pointerOffset = event.changes.first().position
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = size
            val centerOffset = Offset(canvasSize.width * 0.5f, canvasSize.height * 0.45f)
            val secondaryCenter = Offset(canvasSize.width * 0.8f + auroraShift, canvasSize.height * 0.2f)

            // In Light Mode: draw an ambient daylight wash from top to anchor spatial orientation
            if (!isDark) {
                drawRect(
                    brush = Brush.verticalGradient(
                        0.0f to Color.White.copy(alpha = 0.65f),
                        0.4f to Color.White.copy(alpha = 0.20f),
                        1.0f to Color.Transparent
                    ),
                    size = canvasSize
                )
            }

            val gridSpacing = 64.dp.toPx()
            val gridAlpha = if (isDark) 0.025f else 0.035f
            val gridColor = if (isDark) Color.White else Color(0xFF0F172A)
            
            // Subtle cybersecurity coordinate grid
            val horizontalLines = (canvasSize.width / gridSpacing).toInt()
            for (x in 0..horizontalLines) {
                drawLine(
                    color = gridColor.copy(alpha = gridAlpha),
                    start = Offset(x * gridSpacing, 0f),
                    end = Offset(x * gridSpacing, canvasSize.height),
                    strokeWidth = 1f
                )
            }
            val verticalLines = (canvasSize.height / gridSpacing).toInt()
            for (y in 0..verticalLines) {
                drawLine(
                    color = gridColor.copy(alpha = gridAlpha),
                    start = Offset(0f, y * gridSpacing),
                    end = Offset(canvasSize.width, y * gridSpacing),
                    strokeWidth = 1f
                )
            }

            // Primary ambient radial beacon (Deep refined hue in light mode)
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to accent.copy(alpha = if (isDark) 0.09f else 0.06f),
                    0.6f to accent.copy(alpha = if (isDark) 0.03f else 0.015f),
                    1.0f to Color.Transparent,
                    center = centerOffset,
                    radius = canvasSize.width * 0.65f * breathAnim
                )
            )

            // Secondary aurora glow (Purple in Dark, Subtle Indigo in Light)
            val secondaryGlowColor = if (isDark) SecondaryNeon else ElectricIndigo
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to secondaryGlowColor.copy(alpha = if (isDark) 0.06f else 0.04f),
                    1.0f to Color.Transparent,
                    center = secondaryCenter,
                    radius = canvasSize.width * 0.45f * breathAnim
                )
            )

            // Interactive pointer tracking crosshair
            if (isTouching || ringAlpha > 0f) {
                val lineLength = 18.dp.toPx()
                val color = accent.copy(alpha = ringAlpha)

                drawLine(color, Offset(pointerOffset.x, pointerOffset.y - lineLength), Offset(pointerOffset.x, pointerOffset.y + lineLength), 1.5f)
                drawLine(color, Offset(pointerOffset.x - lineLength, pointerOffset.y), Offset(pointerOffset.x + lineLength, pointerOffset.y), 1.5f)

                drawCircle(
                    color = accent.copy(alpha = ringAlpha * 0.5f),
                    center = pointerOffset,
                    radius = 80.dp.toPx() * ringScale,
                    style = Stroke(width = 1.5f)
                )
            }
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    shape: RoundedCornerShape = RoundedCornerShape(22.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    val accent = LocalAccentColor.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val borderBrush = if (onClick != null && isHovered) {
        Brush.linearGradient(listOf(accent.copy(alpha = 0.70f), accent.copy(alpha = 0.35f)))
    } else {
        topBorderHighlightBrush(accent, isDark)
    }

    val targetElevation = if (isDark) {
        0.dp
    } else {
        if (onClick != null && isHovered) 6.dp else 2.5.dp
    }
    val animatedElevation by animateDpAsState(
        targetValue = targetElevation,
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )

    val shadowModifier = if (isDark) {
        Modifier
    } else {
        Modifier.shadow(
            elevation = animatedElevation,
            shape = shape,
            ambientColor = Color(0x140F172A),
            spotColor = Color(0x1E0F172A)
        )
    }

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier
                .then(shadowModifier)
                .pointerHoverIcon(PointerIcon.Hand),
            shape = shape,
            border = BorderStroke(1.dp, borderBrush),
            color = if (isDark) Color.Transparent else Color.White,
            interactionSource = interactionSource
        ) {
            Box(
                modifier = Modifier
                    .background(cardGlassBackdropBrush(isDark, accent))
                    .padding(contentPadding)
            ) {
                content()
            }
        }
    } else {
        Surface(
            modifier = modifier
                .then(shadowModifier),
            shape = shape,
            border = BorderStroke(1.dp, borderBrush),
            color = if (isDark) Color.Transparent else Color.White
        ) {
            Box(
                modifier = Modifier
                    .background(cardGlassBackdropBrush(isDark, accent))
                    .padding(contentPadding)
            ) {
                content()
            }
        }
    }
}

@Composable
fun CyberBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    hasPulseDot: Boolean = false,
    hasGlowAura: Boolean = false,
    fontSize: Float = 10f
) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse)
    )

    Surface(
        modifier = modifier.cyberPulse(color, enabled = hasGlowAura || hasPulseDot),
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = if (isDark) 0.12f else 0.10f),
        border = BorderStroke(1.dp, color.copy(alpha = if (isDark) 0.45f else 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (hasPulseDot) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = pulseAlpha))
                )
            }
            Text(
                text = text,
                color = color,
                maxLines = 1,
                softWrap = false,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = fontSize.sp
                )
            )
        }
    }
}

@Composable
fun MetricTile(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    subValue: String? = null,
    isCompact: Boolean = false
) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    val minHeight = if (isCompact) 36.dp else 60.dp
    val verticalPadding = if (isCompact) 3.dp else 8.dp
    val horizontalPadding = if (isCompact) 6.dp else 10.dp
    val valueFontSize = if (isCompact) 11.5.sp else 13.sp
    val titleFontSize = if (isCompact) 8.5.sp else 9.sp

    Surface(
        modifier = modifier
            .heightIn(min = minHeight)
            .then(
                if (isDark) Modifier else Modifier.shadow(
                    elevation = if (isCompact) 1.dp else 1.5.dp,
                    shape = RoundedCornerShape(if (isCompact) 8.dp else 12.dp),
                    spotColor = Color(0x140F172A),
                    ambientColor = Color(0x0C0F172A)
                )
            )
            .springPress(pressedScale = 0.97f),
        shape = RoundedCornerShape(if (isCompact) 8.dp else 12.dp),
        color = if (isDark) Color.White.copy(alpha = 0.035f) else Color.White,
        border = BorderStroke(1.dp, if (isDark) accent.copy(alpha = 0.25f) else BorderLight)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = verticalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 4.dp else 8.dp)
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(if (isCompact) 24.dp else 32.dp)
                        .clip(RoundedCornerShape(if (isCompact) 6.dp else 8.dp))
                        .background(accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = accent, modifier = Modifier.size(if (isCompact) 13.dp else 16.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = titleFontSize,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (isDark) TextMuted else TextMutedLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        fontSize = valueFontSize
                    ),
                    color = if (isDark) Color.White else Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subValue.isNullOrBlank()) {
                    Text(
                        text = subValue,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun CyberSegmentedControl(
    items: List<Pair<String, ImageVector?>>,
    selectedIndex: Int,
    onIndexSelected: (Int) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    BoxWithConstraints(modifier = modifier) {
        val totalWidth = maxWidth
        val count = items.size.coerceAtLeast(1)
        val itemEstimatedWidth = totalWidth / count
        val showAllIcons = itemEstimatedWidth >= 88.dp || count <= 2
        val horizontalPadding = if (itemEstimatedWidth < 80.dp) 3.dp else if (itemEstimatedWidth < 100.dp) 6.dp else 10.dp
        val labelFontSize = if (itemEstimatedWidth < 76.dp) 9.sp else if (itemEstimatedWidth < 95.dp) 10.sp else 11.sp
        val controlHeight = if (itemEstimatedWidth < 80.dp) 38.dp else 44.dp

        Surface(
            modifier = Modifier.fillMaxWidth().height(controlHeight),
            shape = RoundedCornerShape(12.dp),
            color = if (isDark) Color.Black.copy(alpha = 0.35f) else SurfaceInsetLight,
            border = BorderStroke(1.dp, if (isDark) GlassBorder.copy(alpha = 0.3f) else BorderLight)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items.forEachIndexed { index, (label, icon) ->
                    val isSelected = index == selectedIndex
                    Surface(
                        onClick = { onIndexSelected(index) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .springPress(pressedScale = 0.95f)
                            .pointerHoverIcon(PointerIcon.Hand),
                        shape = RoundedCornerShape(9.dp),
                        color = if (isSelected) (if (isDark) accent.copy(alpha = 0.22f) else accent) else Color.Transparent,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) accent.copy(alpha = 0.5f) else Color.Transparent
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = horizontalPadding),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (icon != null && (showAllIcons || isSelected)) {
                                Icon(
                                    icon,
                                    null,
                                    tint = if (isSelected) (if (isDark) accent else Color.White) else (if (isDark) TextMuted else TextMutedLight),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = labelFontSize
                                ),
                                color = if (isSelected) Color.White else (if (isDark) TextMuted else TextMutedLight),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumSnackbar(message: String) {
    val accent = LocalAccentColor.current
    val isDark = LocalAppSettings.current.theme == "DARK"
    val lang = LocalAppSettings.current.language

    val resolvedMessage = remember(message, lang) {
        when {
            message.startsWith("saved_to:") -> {
                val path = message.substringAfter("saved_to:")
                "${LocalizedStrings.get("saved_to", lang)}: $path"
            }
            message.startsWith("download_failed:") -> {
                val err = message.substringAfter("download_failed:")
                if (err.isNotBlank()) "${LocalizedStrings.get("download_failed", lang)}: $err" else LocalizedStrings.get("download_failed", lang)
            }
            message.startsWith("share_failed:") -> {
                val err = message.substringAfter("share_failed:")
                if (err.isNotBlank()) "${LocalizedStrings.get("share_failed", lang)}: $err" else LocalizedStrings.get("share_failed", lang)
            }
            message.startsWith("failed_delete:") -> {
                val err = message.substringAfter("failed_delete:")
                if (err.isNotBlank()) "${LocalizedStrings.get("failed_delete_record", lang)}: $err" else LocalizedStrings.get("failed_delete_record", lang)
            }
            message.startsWith("failed_clear:") -> {
                val err = message.substringAfter("failed_clear:")
                if (err.isNotBlank()) "${LocalizedStrings.get("failed_clear_history", lang)}: $err" else LocalizedStrings.get("failed_clear_history", lang)
            }
            message.startsWith("failed_load:") -> {
                val err = message.substringAfter("failed_load:")
                if (err.isNotBlank()) "${LocalizedStrings.get("failed_load_records", lang)}: $err" else LocalizedStrings.get("failed_load_records", lang)
            }
            message.startsWith("copied:") -> {
                val text = message.substringAfter("copied:")
                "${LocalizedStrings.get("copied", lang)}: $text"
            }
            message == "Scan report deleted" -> LocalizedStrings.get("scan_report_deleted", lang)
            message == "All scan reports cleared" -> LocalizedStrings.get("all_reports_cleared", lang)
            message == "Download failed" -> LocalizedStrings.get("download_failed", lang)
            message == "Report copied to clipboard." -> LocalizedStrings.get("report_copied_clipboard", lang)
            message == "No output to copy." -> LocalizedStrings.get("no_output_to_copy", lang)
            else -> LocalizedStrings.get(message, lang)
        }
    }

    Surface(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(
                1.dp,
                Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.5f))),
                RoundedCornerShape(18.dp)
            ),
        color = if (isDark) Color(0xE60A1018) else Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.CheckCircle, null, tint = TertiaryNeon, modifier = Modifier.size(18.dp))
            Text(
                text = resolvedMessage, 
                color = if (isDark) Color.White else Color.Black,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
