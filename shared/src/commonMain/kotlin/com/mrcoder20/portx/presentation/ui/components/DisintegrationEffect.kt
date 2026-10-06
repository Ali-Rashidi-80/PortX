package com.mrcoder20.portx.presentation.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import com.mrcoder20.portx.presentation.ui.theme.DangerNeon
import com.mrcoder20.portx.presentation.ui.theme.SecondaryNeon
import com.mrcoder20.portx.presentation.ui.theme.TertiaryNeon
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Mathematical model for an individual dust spark in the Telegram Thanos-style disintegration effect.
 */
internal data class ParticleDust(
    val relX: Float,
    val relY: Float,
    val vx: Float,
    val vy: Float,
    val baseSize: Float,
    val color: Color,
    val turbulence: Float,
    val phase: Float,
    val delay: Float
)

/**
 * Telegram-Style Dust / Thanos Disintegration Container.
 *
 * When [isDisintegrating] turns true, the wrapped content gracefully dissolves into
 * hundreds of swirling, glowing neon dust particles that drift upward and disperse,
 * invoking [onDisintegrated] upon completion.
 */
@Composable
fun DisintegrationContainer(
    isDisintegrating: Boolean,
    modifier: Modifier = Modifier,
    accent: Color = TertiaryNeon,
    particleCount: Int = 180,
    onDisintegrated: () -> Unit,
    content: @Composable () -> Unit
) {
    val progress = remember { Animatable(0f) }

    // Pre-generate dust particles with deterministic seed per disintegration cycle
    val particles = remember(isDisintegrating) {
        if (!isDisintegrating) emptyList()
        else {
            val colors = listOf(
                accent,
                accent.copy(alpha = 0.9f),
                SecondaryNeon,
                DangerNeon.copy(alpha = 0.85f),
                Color(0xFFFFE082), // Golden spark
                Color.White
            )
            List(particleCount) {
                val relX = Random.nextFloat()
                val relY = Random.nextFloat()
                // Particles drift outward horizontally and upward against gravity
                val angle = Random.nextFloat() * (PI.toFloat() * 0.8f) + (PI.toFloat() * 1.1f)
                val speed = Random.nextFloat() * 140f + 60f
                val vx = kotlin.math.cos(angle) * speed + (relX - 0.5f) * 80f
                val vy = kotlin.math.sin(angle) * speed - (Random.nextFloat() * 120f + 40f)

                ParticleDust(
                    relX = relX,
                    relY = relY,
                    vx = vx,
                    vy = vy,
                    baseSize = Random.nextFloat() * 3.2f + 1.8f,
                    color = colors[Random.nextInt(colors.size)],
                    turbulence = Random.nextFloat() * 28f + 8f,
                    phase = Random.nextFloat() * (PI.toFloat() * 2f),
                    delay = relY * 0.28f + Random.nextFloat() * 0.12f // Disintegrates bottom-to-top progressively
                )
            }
        }
    }

    LaunchedEffect(isDisintegrating) {
        if (isDisintegrating) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 720, easing = FastOutSlowInEasing)
            )
            onDisintegrated()
        }
    }

    Box(modifier = modifier) {
        // Content with dissolve graphicsLayer (alpha wipe + subtle collapse)
        Box(
            modifier = Modifier.graphicsLayer {
                if (isDisintegrating) {
                    val p = progress.value
                    alpha = (1f - p * 1.35f).coerceIn(0f, 1f)
                    scaleX = 1f - p * 0.08f
                    scaleY = 1f - p * 0.08f
                    translationY = -p * 12f
                }
            }
        ) {
            content()
        }

        // Particle Canvas Overlay
        if (isDisintegrating && progress.value in 0.001f..0.999f) {
            val animProgress = progress.value
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height

                particles.forEach { p ->
                    if (animProgress >= p.delay) {
                        val localProgress = ((animProgress - p.delay) / (1f - p.delay)).coerceIn(0f, 1f)
                        val alpha = (1f - localProgress * 1.15f).coerceIn(0f, 1f)

                        if (alpha > 0.01f) {
                            val twinkle = (sin(localProgress * 14f + p.phase) * 0.35f + 0.65f).coerceIn(0.2f, 1f)
                            val effectiveAlpha = alpha * twinkle
                            val curX = p.relX * w + p.vx * localProgress + sin(localProgress * 7f + p.phase) * p.turbulence
                            val curY = p.relY * h + p.vy * localProgress - (localProgress * localProgress * 70f)
                            val curSize = p.baseSize * (1f - localProgress * 0.65f)

                            // 1. Soft atmospheric glow halo
                            drawCircle(
                                color = p.color.copy(alpha = effectiveAlpha * 0.35f),
                                radius = curSize * 2.5f,
                                center = Offset(curX, curY)
                            )
                            // 2. Bright core spark
                            drawCircle(
                                color = p.color.copy(alpha = effectiveAlpha),
                                radius = curSize,
                                center = Offset(curX, curY)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Premium Spring Press Micro-Interaction Modifier.
 * Adds tactile physical spring compression (scale = 0.95f) upon press and elastic release.
 */
fun Modifier.springPress(
    pressedScale: Float = 0.95f,
    interactionSource: MutableInteractionSource? = null
): Modifier = this.composed {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    this.graphicsLayer {
        scaleX = animatedScale
        scaleY = animatedScale
    }
}

/**
 * Ambient Breathing Glow Pulse for live indicators and cyber telemetry badges.
 */
fun Modifier.cyberPulse(
    glowColor: Color,
    enabled: Boolean = true,
    minAlpha: Float = 0.3f,
    maxAlpha: Float = 0.85f,
    durationMs: Int = 1600
): Modifier = this.composed {
    if (!enabled) return@composed this

    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = minAlpha,
        targetValue = maxAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    this.graphicsLayer {
        shadowElevation = 8f * alpha
        ambientShadowColor = glowColor.copy(alpha = alpha)
        spotShadowColor = glowColor.copy(alpha = alpha)
    }
}

/**
 * Kinetic Cyber Shimmer wave sweeping across active controls and hero indicators.
 */
fun Modifier.cyberShimmer(
    highlightColor: Color = Color.White.copy(alpha = 0.22f),
    durationMs: Int = 1800,
    enabled: Boolean = true
): Modifier = this.composed {
    if (!enabled) return@composed this

    val infiniteTransition = rememberInfiniteTransition()
    val translateAnim by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    this.drawWithContent {
        drawContent()
        val w = size.width
        val h = size.height
        val startX = translateAnim * w
        val brush = androidx.compose.ui.graphics.Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                highlightColor,
                Color.Transparent
            ),
            start = Offset(startX - w * 0.35f, 0f),
            end = Offset(startX + w * 0.35f, h)
        )
        drawRect(brush = brush)
    }
}

