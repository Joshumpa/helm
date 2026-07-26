package dev.helm.launcher.ui

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.helm.core.Spacing
import dev.helm.core.neumorphicClickable
import dev.helm.launcher.AppEntry

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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .neumorphicClickable(onClick = onClick, cornerRadius = 16.dp, elevation = 6.dp)
            .padding(Spacing.sm),
    ) {
        AppIconImage(
            pkg = entry.pkg,
            label = entry.label,
            modifier = Modifier.size(56.dp),
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
