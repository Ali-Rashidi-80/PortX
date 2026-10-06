package com.mrcoder20.portx.presentation.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun PortXVerticalScrollbar(
    listState: LazyListState,
    modifier: Modifier
) {
    // Android utilizes system touch fling/overscroll dynamics
}

@Composable
actual fun PortXScrollStateVerticalScrollbar(
    scrollState: ScrollState,
    modifier: Modifier
) {
    // Android utilizes system touch fling/overscroll dynamics
}
