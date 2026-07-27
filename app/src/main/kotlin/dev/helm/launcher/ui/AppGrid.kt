package dev.helm.launcher.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.helm.core.Spacing
import dev.helm.core.neumorphicClickable
import dev.helm.launcher.AppEntry
import kotlinx.coroutines.launch

@Composable
fun AppGrid(
    apps: List<AppEntry>,
    onAppClick: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        items(apps) { entry ->
            GridCell(entry = entry, onClick = { onAppClick(entry) })
        }
    }
}

@Composable
private fun GridCell(entry: AppEntry, onClick: () -> Unit) {
    val iconScale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .neumorphicClickable(
                onClick = {
                    onClick()
                    scope.launch {
                        iconScale.animateTo(
                            0.78f,
                            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh),
                        )
                        iconScale.animateTo(
                            1f,
                            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                        )
                    }
                },
                cornerRadius = 16.dp,
                elevation = 6.dp,
            )
            .padding(Spacing.sm),
    ) {
        AppIconImage(
            pkg = entry.pkg,
            label = entry.label,
            action = entry.action,
            modifier = Modifier
                .size(56.dp)
                .scale(iconScale.value),
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = entry.label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
