package com.mrcoder20.portx.presentation.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

/**
 * Universal Touch & Mouse Drag-to-Scroll Modifier for Compose Multiplatform.
 * Enables natural touch swiping and mouse click-and-drag scrolling on Desktop,
 * as well as translating vertical mouse wheel events into horizontal scroll for horizontal rows.
 */
fun Modifier.touchDragScroll(
    scrollState: ScrollState,
    isVertical: Boolean = false,
    isRtl: Boolean = false
): Modifier = this.composed {
    this.pointerInput(scrollState, isVertical, isRtl) {
        detectDragGestures(
            onDrag = { change, dragAmount ->
                change.consume()
                val delta = if (isVertical) {
                    -dragAmount.y
                } else {
                    if (isRtl) dragAmount.x else -dragAmount.x
                }
                scrollState.dispatchRawDelta(delta)
            }
        )
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
                            val sign = if (isRtl) -1f else 1f
                            scrollState.dispatchRawDelta(effectiveDelta * 40f * sign)
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Universal Touch & Mouse Drag-to-Scroll for LazyListState.
 * Allows smooth vertical or horizontal dragging of LazyColumn/LazyRow on desktop.
 */
fun Modifier.touchDragScroll(
    listState: LazyListState,
    isVertical: Boolean = true
): Modifier = this.composed {
    this.pointerInput(listState, isVertical) {
        detectDragGestures(
            onDrag = { change, dragAmount ->
                change.consume()
                val delta = if (isVertical) -dragAmount.y else -dragAmount.x
                listState.dispatchRawDelta(delta)
            }
        )
    }
}
