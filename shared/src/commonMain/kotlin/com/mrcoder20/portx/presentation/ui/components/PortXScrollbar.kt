package com.mrcoder20.portx.presentation.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun PortXVerticalScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier
)

@Composable
expect fun PortXScrollStateVerticalScrollbar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier
)
