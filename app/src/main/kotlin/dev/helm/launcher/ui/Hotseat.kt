package dev.helm.launcher.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
fun Hotseat(
    apps: List<AppEntry>,
    onAppClick: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        apps.forEach { entry ->
            HotseatButton(entry = entry, onClick = { onAppClick(entry) })
        }
    }
}

@Composable
private fun HotseatButton(entry: AppEntry, onClick: () -> Unit) {
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
                cornerRadius = 20.dp,
                elevation = 6.dp,
            )
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
    ) {
        AppIconImage(
            pkg = entry.pkg,
            label = entry.label,
            modifier = Modifier
                .size(48.dp)
                .scale(iconScale.value),
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = entry.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
