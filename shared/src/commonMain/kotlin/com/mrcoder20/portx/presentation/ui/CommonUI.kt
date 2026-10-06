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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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

            val gridSpacing = 64.dp.toPx()
            val gridAlpha = if (isDark) 0.025f else 0.045f
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

            // Primary ambient radial beacon
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to accent.copy(alpha = if (isDark) 0.09f else 0.12f),
                    0.6f to accent.copy(alpha = if (isDark) 0.03f else 0.04f),
                    1.0f to Color.Transparent,
                    center = centerOffset,
                    radius = canvasSize.width * 0.65f * breathAnim
                )
            )

            // Secondary aurora glow (Purple in Dark, Subtle Indigo in Light)
            val secondaryGlowColor = if (isDark) SecondaryNeon else ElectricIndigo
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to secondaryGlowColor.copy(alpha = if (isDark) 0.06f else 0.07f),
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

    val animatedScale by animateFloatAsState(
        targetValue = if (onClick != null && isHovered) 1.01f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium)
    )

    val borderBrush = if (onClick != null && isHovered) {
        Brush.linearGradient(listOf(accent.copy(alpha = 0.6f), accent.copy(alpha = 0.2f)))
    } else {
        topBorderHighlightBrush(accent, isDark)
    }

    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .clip(shape)
            .border(1.dp, borderBrush, shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            ),
        color = Color.Transparent,
        shape = shape
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
    subValue: String? = null
) {
    val isDark = LocalAppSettings.current.theme == "DARK"
    Surface(
        modifier = modifier
            .heightIn(min = 60.dp)
            .springPress(pressedScale = 0.97f),
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) Color.White.copy(alpha = 0.035f) else Color.Black.copy(alpha = 0.025f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = accent, modifier = Modifier.size(16.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
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
                        fontSize = 13.sp
                    ),
                    color = if (isDark) Color.White else Color.Black,
                    maxLines = 1
                )
                if (!subValue.isNullOrBlank()) {
                    Text(
                        text = subValue,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = accent
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
    Surface(
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, if (isDark) GlassBorder.copy(alpha = 0.3f) else GlassBorderLight.copy(alpha = 0.3f))
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
                        .springPress(pressedScale = 0.95f),
                    shape = RoundedCornerShape(9.dp),
                    color = if (isSelected) accent.copy(alpha = if (isDark) 0.22f else 0.85f) else Color.Transparent,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) accent.copy(alpha = 0.5f) else Color.Transparent
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (icon != null) {
                            Icon(
                                icon,
                                null,
                                tint = if (isSelected) (if (isDark) accent else Color.White) else (if (isDark) TextMuted else TextMutedLight),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp
                            ),
                            color = if (isSelected) (if (isDark) Color.White else Color.White) else (if (isDark) TextMuted else TextMutedLight),
                            maxLines = 1
                        )
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
        color = if (isDark) Color(0xE60A1018) else Color(0xF2FFFFFF),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.CheckCircle, null, tint = TertiaryNeon, modifier = Modifier.size(18.dp))
            Text(
                text = message, 
                color = if (isDark) Color.White else Color.Black,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
