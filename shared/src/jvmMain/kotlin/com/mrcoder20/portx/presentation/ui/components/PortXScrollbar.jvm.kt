package com.mrcoder20.portx.presentation.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mrcoder20.portx.presentation.ui.theme.LocalAccentColor

@Composable
actual fun PortXVerticalScrollbar(
    listState: LazyListState,
    modifier: Modifier
) {
    val accent = LocalAccentColor.current
    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(listState),
        modifier = modifier.padding(vertical = 4.dp),
        style = defaultScrollbarStyle().copy(
            unhoverColor = accent.copy(alpha = 0.35f),
            hoverColor = accent.copy(alpha = 0.85f),
            shape = RoundedCornerShape(4.dp),
            thickness = 6.dp
        )
    )
}

@Composable
actual fun PortXScrollStateVerticalScrollbar(
    scrollState: ScrollState,
    modifier: Modifier
) {
    val accent = LocalAccentColor.current
    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(scrollState),
        modifier = modifier.padding(vertical = 4.dp),
        style = defaultScrollbarStyle().copy(
            unhoverColor = accent.copy(alpha = 0.35f),
            hoverColor = accent.copy(alpha = 0.85f),
            shape = RoundedCornerShape(4.dp),
            thickness = 6.dp
        )
    )
}
