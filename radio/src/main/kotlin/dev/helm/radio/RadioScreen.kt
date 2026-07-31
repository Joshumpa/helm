package dev.helm.radio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.helm.core.Spacing
import dev.helm.sdk.RadioBand
import dev.helm.sdk.SeekDirection

@Composable
fun RadioScreen(
    onBack: () -> Unit,
    vm: RadioViewModel = viewModel(),
) {
    val state by vm.state.collectAsState()
    val presets by vm.presets.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(Spacing.lg),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Atrás",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "Radio",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = Spacing.sm),
            )
        }

        // Task 5.6 — stub banner
        if (state.isStubMode) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(8.dp))
                    .padding(Spacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No disponible (Track B — requiere root)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(Spacing.md))
        }

        // Band selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = state.station.band == RadioBand.FM,
                onClick = { vm.tune(state.station.frequencyMhz, RadioBand.FM) },
                label = { Text("FM") },
                modifier = Modifier.padding(end = Spacing.sm),
            )
            FilterChip(
                selected = state.station.band == RadioBand.AM,
                onClick = { vm.tune(state.station.frequencyMhz, RadioBand.AM) },
                label = { Text("AM") },
            )
        }

        Spacer(Modifier.height(Spacing.lg))

        // Frequency display
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.isSeeking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp,
                )
                Spacer(Modifier.height(Spacing.sm))
            }
            Text(
                text = if (state.station.band == RadioBand.FM) {
                    "%.1f MHz".format(state.station.frequencyMhz)
                } else {
                    "%.0f kHz".format(state.station.frequencyMhz * 1000)
                },
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 48.sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
            state.station.name?.let { name ->
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Spacer(Modifier.height(Spacing.xl))

        // Seek controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { vm.seek(SeekDirection.BACKWARD) },
                modifier = Modifier.size(64.dp),
            ) {
                Icon(
                    Icons.Filled.SkipPrevious,
                    contentDescription = "Buscar anterior",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp),
                )
            }
            Icon(
                imageVector = Icons.Filled.Radio,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier
                    .padding(horizontal = Spacing.xl)
                    .size(56.dp),
            )
            IconButton(
                onClick = { vm.seek(SeekDirection.FORWARD) },
                modifier = Modifier.size(64.dp),
            ) {
                Icon(
                    Icons.Filled.SkipNext,
                    contentDescription = "Buscar siguiente",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp),
                )
            }
        }

        Spacer(Modifier.height(Spacing.lg))

        // Presets grid — max 3 columns (portrait rule)
        Text(
            text = "Presets",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Spacing.sm),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier.fillMaxWidth(),
        ) {
            itemsIndexed(presets) { index, preset ->
                if (preset != null) {
                    Button(
                        onClick = { vm.recallPreset(preset) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (preset.frequencyMhz == state.station.frequencyMhz && preset.band == state.station.band) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        ),
                        modifier = Modifier.height(56.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (preset.band == RadioBand.FM) "%.1f".format(preset.frequencyMhz)
                                else "%.0f".format(preset.frequencyMhz * 1000),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (preset.frequencyMhz == state.station.frequencyMhz) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { vm.savePreset(index) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(56.dp),
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
