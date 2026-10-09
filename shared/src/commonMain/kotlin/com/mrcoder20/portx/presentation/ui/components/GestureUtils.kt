package com.mrcoder20.portx.presentation.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitDragOrCancellation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * CompositionLocal indicating whether touch emulation is actively simulated
 * (e.g. mobile/tablet responsive devtools mode or touchscreen mode).
 */
val LocalTouchEmulation = compositionLocalOf { false }

/**
 * Universal Touch & Mouse Drag-to-Scroll Modifier for Compose Multiplatform.
 * Enables natural touch swiping on touchscreen devices and simulated mobile/tablet
 * views while translating vertical mouse wheel events into horizontal scroll for rows.
 *
 * Provides touch slop protection: taps/clicks pass through untouched without interference,
 * while dragging smoothly scrolls the container with inertial momentum on release.
 */
fun Modifier.touchDragScroll(
    scrollState: ScrollState,
    isVertical: Boolean = false,
    isRtl: Boolean = false
): Modifier {
    if (com.mrcoder20.portx.getPlatform().name.startsWith("Android")) {
        return this
    }
    return this.composed {
        val isTouchEmulated = LocalTouchEmulation.current
        val coroutineScope = rememberCoroutineScope()

        this.pointerInput(scrollState, isVertical, isRtl, isTouchEmulated) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                // On desktop when touch emulation is NOT active, let standard mouse wheel handle it.
                // When touch emulation IS active (mobile/tablet presets), mouse behaves as a touch finger.
                if (down.type == PointerType.Mouse && !isTouchEmulated) {
                    return@awaitEachGesture
                }

                var overSlop = 0f
                val slopPassed = awaitTouchSlopOrCancellation(down.id) { change, over ->
                    change.consume()
                    overSlop = if (isVertical) {
                        -over.y
                    } else {
                        if (isRtl) over.x else -over.x
                    }
                }

                if (slopPassed != null) {
                    scrollState.dispatchRawDelta(overSlop)
                    var lastDelta = overSlop
                    var currentDrag: PointerInputChange? = slopPassed

                    while (currentDrag != null) {
                        val change = awaitDragOrCancellation(currentDrag.id)
                        if (change == null) break
                        val delta = if (isVertical) {
                            -change.positionChange().y
                        } else {
                            if (isRtl) change.positionChange().x else -change.positionChange().x
                        }
                        scrollState.dispatchRawDelta(delta)
                        lastDelta = delta
                        change.consume()
                        currentDrag = change
                    }

                    // Inertial momentum fling on rapid release
                    if (abs(lastDelta) > 3f) {
                        val flingDistance = (lastDelta * 12f).coerceIn(-500f, 500f)
                        coroutineScope.launch {
                            scrollState.animateScrollBy(
                                value = flingDistance,
                                animationSpec = tween(durationMillis = 350, easing = LinearOutSlowInEasing)
                            )
                        }
                    }
                }
            }
        }.pointerInput(scrollState, isVertical, isRtl) {
            if (!isVertical) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Scroll) {
                            val deltaY = event.changes.sumOf { it.scrollDelta.y.toDouble() }.toFloat()
                            val deltaX = event.changes.sumOf { it.scrollDelta.x.toDouble() }.toFloat()
                            val effectiveDelta = if (abs(deltaX) > 0.01f) deltaX else deltaY
                            if (abs(effectiveDelta) > 0.01f) {
                                val factor = if (isRtl) -40f else 40f
                                scrollState.dispatchRawDelta(effectiveDelta * factor)
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Universal Touch & Mouse Drag-to-Scroll for LazyListState.
 * Supports natural touch dragging on mobile/tablet presets with touch slop guard
 * and inertial fling deceleration on release.
 */
fun Modifier.touchDragScroll(
    listState: LazyListState,
    isVertical: Boolean = true,
    isRtl: Boolean = false
): Modifier {
    if (com.mrcoder20.portx.getPlatform().name.startsWith("Android")) {
        return this
    }
    return this.composed {
        val isTouchEmulated = LocalTouchEmulation.current
        val coroutineScope = rememberCoroutineScope()

        this.pointerInput(listState, isVertical, isRtl, isTouchEmulated) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                if (down.type == PointerType.Mouse && !isTouchEmulated) {
                    return@awaitEachGesture
                }

                var overSlop = 0f
                val slopPassed = awaitTouchSlopOrCancellation(down.id) { change, over ->
                    change.consume()
                    overSlop = if (isVertical) {
                        -over.y
                    } else {
                        if (isRtl) over.x else -over.x
                    }
                }

                if (slopPassed != null) {
                    listState.dispatchRawDelta(overSlop)
                    var lastDelta = overSlop
                    var currentDrag: PointerInputChange? = slopPassed

                    while (currentDrag != null) {
                        val change = awaitDragOrCancellation(currentDrag.id)
                        if (change == null) break
                        val delta = if (isVertical) {
                            -change.positionChange().y
                        } else {
                            if (isRtl) change.positionChange().x else -change.positionChange().x
                        }
                        listState.dispatchRawDelta(delta)
                        lastDelta = delta
                        change.consume()
                        currentDrag = change
                    }

                    // Inertial momentum fling on rapid release
                    if (abs(lastDelta) > 3f) {
                        val flingDistance = (lastDelta * 12f).coerceIn(-500f, 500f)
                        coroutineScope.launch {
                            listState.animateScrollBy(
                                value = flingDistance,
                                animationSpec = tween(durationMillis = 350, easing = LinearOutSlowInEasing)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Universal horizontal fading edge modifier for ScrollState containers (e.g. chips/presets rows).
 * Softly dissolves overflowing left/right edges with an alpha gradient mask so chips do not clip abruptly.
 * Automatically adapts to RTL (Persian) vs LTR layout directions.
 */
fun Modifier.horizontalFadingEdges(
    scrollState: ScrollState,
    fadeWidth: Dp = 18.dp,
    isRtl: Boolean = false
): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        val widthPx = size.width
        val heightPx = size.height
        val fadePx = fadeWidth.toPx()

        if (fadePx <= 0f || widthPx <= 0f) return@drawWithContent

        val showLeftFade = if (isRtl) scrollState.canScrollForward else scrollState.canScrollBackward
        val showRightFade = if (isRtl) scrollState.canScrollBackward else scrollState.canScrollForward

        if (showLeftFade) {
            val leftEnd = fadePx.coerceAtMost(widthPx)
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startX = 0f,
                    endX = leftEnd
                ),
                topLeft = Offset.Zero,
                size = Size(leftEnd, heightPx),
                blendMode = BlendMode.DstIn
            )
        }

        if (showRightFade) {
            val rightStart = (widthPx - fadePx).coerceAtLeast(0f)
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startX = rightStart,
                    endX = widthPx
                ),
                topLeft = Offset(rightStart, 0f),
                size = Size(widthPx - rightStart, heightPx),
                blendMode = BlendMode.DstIn
            )
        }
    }

/**
 * Universal horizontal fading edge modifier for LazyListState.
 */
fun Modifier.horizontalFadingEdges(
    listState: LazyListState,
    fadeWidth: Dp = 18.dp,
    isRtl: Boolean = false
): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        val widthPx = size.width
        val heightPx = size.height
        val fadePx = fadeWidth.toPx()

        if (fadePx <= 0f || widthPx <= 0f) return@drawWithContent

        val showLeftFade = if (isRtl) listState.canScrollForward else listState.canScrollBackward
        val showRightFade = if (isRtl) listState.canScrollBackward else listState.canScrollForward

        if (showLeftFade) {
            val leftEnd = fadePx.coerceAtMost(widthPx)
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startX = 0f,
                    endX = leftEnd
                ),
                topLeft = Offset.Zero,
                size = Size(leftEnd, heightPx),
                blendMode = BlendMode.DstIn
            )
        }

        if (showRightFade) {
            val rightStart = (widthPx - fadePx).coerceAtLeast(0f)
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startX = rightStart,
                    endX = widthPx
                ),
                topLeft = Offset(rightStart, 0f),
                size = Size(widthPx - rightStart, heightPx),
                blendMode = BlendMode.DstIn
            )
        }
    }

/**
 * Auto-Scroll Peek Hint.
 * Gently peeks a scrollable container forward by a small distance when loaded to hint to the
 * user that additional overflow items are available, then returns to start.
 * If the user interacts or scrolls, it safely yields immediately.
 */
@Composable
fun LaunchedAutoScrollHint(
    scrollState: ScrollState,
    enabled: Boolean = true,
    initialDelayMs: Long = 900L,
    peekDistancePx: Int = 45
) {
    if (!enabled) return
    LaunchedEffect(scrollState.maxValue) {
        if (scrollState.maxValue > 15 && scrollState.value == 0) {
            delay(initialDelayMs)
            if (!scrollState.isScrollInProgress && scrollState.value == 0) {
                val targetScroll = (scrollState.maxValue.coerceAtMost(peekDistancePx))
                if (targetScroll > 0) {
                    scrollState.animateScrollTo(
                        value = targetScroll,
                        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                    )
                    delay(250)
                    if (!scrollState.isScrollInProgress) {
                        scrollState.animateScrollTo(
                            value = 0,
                            animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing)
                        )
                    }
                }
            }
        }
    }
}
