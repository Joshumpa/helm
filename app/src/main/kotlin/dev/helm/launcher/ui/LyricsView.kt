package dev.helm.launcher.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import dev.helm.core.Spacing
import dev.helm.core.theme.HelmPrimary
import dev.helm.core.theme.HelmSubtext
import dev.helm.launcher.media.LrcLine
import dev.helm.launcher.media.LyricsState

@Composable
fun LyricsView(
    lyrics: LyricsState,
    positionMs: Long,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.padding(Spacing.lg)) {
        when (lyrics) {
            is LyricsState.Idle -> Unit

            is LyricsState.Loading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = HelmPrimary,
            )

            is LyricsState.Unavailable -> Text(
                text = "No lyrics found",
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.bodyLarge,
                color = HelmSubtext,
            )

            is LyricsState.Plain -> Text(
                text = lyrics.text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            )

            is LyricsState.Synced -> SyncedLyricsView(
                lines = lyrics.lines,
                positionMs = positionMs,
            )
        }
    }
}

@Composable
private fun SyncedLyricsView(lines: List<LrcLine>, positionMs: Long) {
    val currentIndex = remember(positionMs) {
        lines.indexOfLast { it.timeMs <= positionMs }.coerceAtLeast(0)
    }
    val listState = rememberLazyListState()

    LaunchedEffect(currentIndex) {
        if (lines.isNotEmpty()) {
            listState.animateScrollToItem(
                index = currentIndex.coerceAtMost(lines.lastIndex),
                scrollOffset = -120,
            )
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
    ) {
        itemsIndexed(lines, key = { i, _ -> i }) { index, line ->
            val current = index == currentIndex
            Text(
                text = line.text,
                style = if (current) MaterialTheme.typography.titleMedium
                        else MaterialTheme.typography.bodyMedium,
                color = if (current) MaterialTheme.colorScheme.onBackground else HelmSubtext,
                fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.xs),
            )
        }
    }
}
