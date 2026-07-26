package dev.helm.launcher.ui

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.helm.core.Spacing
import dev.helm.core.neumorphicClickable
import dev.helm.core.neumorphicShadow
import dev.helm.launcher.media.NowPlayingViewModel
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

@Composable
fun NowPlayingScreen(
    onBack: () -> Unit,
    vm: NowPlayingViewModel = viewModel(),
) {
    val media  by vm.media.collectAsState()
    val lyrics by vm.lyrics.collectAsState()
    val context = LocalContext.current

    var positionMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(media.isPlaying) {
        while (true) {
            positionMs = vm.livePositionMs()
            delay(500)
        }
    }

    if (media.title.isEmpty()) {
        NoMediaPlaceholder(
            onBack = onBack,
            onOpenSettings = {
                context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
            },
        )
        return
    }

    val bg = MaterialTheme.colorScheme.background

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(bg),
    ) {
        // ── Left column ───────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .width(300.dp)
                .fillMaxHeight()
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            AlbumArtImage(
                bitmap = media.artwork,
                bg = bg,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            )

            Spacer(Modifier.height(Spacing.xs))

            Text(
                text = media.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = media.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (media.durationMs > 0) {
                var seeking by remember { mutableStateOf(false) }
                var seekValue by remember { mutableFloatStateOf(0f) }
                val sliderValue = if (seeking) seekValue
                    else (positionMs.toFloat() / media.durationMs).coerceIn(0f, 1f)

                Slider(
                    value = sliderValue,
                    onValueChange = { seeking = true; seekValue = it },
                    onValueChangeFinished = {
                        seeking = false
                        vm.seekTo((seekValue * media.durationMs).toLong())
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(formatMs(positionMs), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatMs(media.durationMs), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Transport controls — neumorphicClickable replaces ripple with shadow animation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Prev
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .neumorphicClickable(onClick = { vm.skipPrev() }, cornerRadius = 26.dp, elevation = 5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.SkipPrevious, "Previous", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(28.dp))
                }

                // Play / Pause — larger, circular, accent border (v2 accessibility)
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .neumorphicClickable(
                            onClick = { if (media.isPlaying) vm.pause() else vm.play() },
                            cornerRadius = 32.dp,
                            elevation = 8.dp,
                            showAccentBorder = true,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (media.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (media.isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp),
                    )
                }

                // Next
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .neumorphicClickable(onClick = { vm.skipNext() }, cornerRadius = 26.dp, elevation = 5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.SkipNext, "Next", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(28.dp))
                }
            }
        }

        VerticalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

        // ── Right column: lyrics ─────────────────────────────────────────────
        LyricsView(
            lyrics = lyrics,
            positionMs = positionMs,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
    }
}

@Composable
private fun AlbumArtImage(bitmap: Bitmap?, bg: Color, modifier: Modifier = Modifier) {
    if (bitmap != null) {
        Image(
            painter = BitmapPainter(bitmap.asImageBitmap()),
            contentDescription = "Album art",
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(RoundedCornerShape(20.dp)),
        )
    } else {
        // Static card — no interaction, use neumorphicShadow directly
        Box(
            modifier = modifier
                .neumorphicShadow(backgroundColor = bg, cornerRadius = 20.dp, elevation = 8.dp)
                .background(bg, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("♪", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NoMediaPlaceholder(onBack: () -> Unit, onOpenSettings: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(Spacing.md)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text("Nothing playing", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "Grant notification access so Helm can read media sessions",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text("Open notification settings")
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return "%d:%02d".format(minutes, seconds)
}
