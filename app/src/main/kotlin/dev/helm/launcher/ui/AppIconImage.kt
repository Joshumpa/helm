package dev.helm.launcher.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import dev.helm.core.theme.PlaceholderColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AppIconImage(pkg: String, label: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bmp by produceState<Bitmap?>(initialValue = null, pkg) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val d = context.packageManager.getApplicationIcon(pkg)
                Bitmap.createBitmap(72, 72, Bitmap.Config.ARGB_8888).also { bmp ->
                    val canvas = android.graphics.Canvas(bmp)
                    d.setBounds(0, 0, 72, 72)
                    d.draw(canvas)
                }
            }.getOrNull()
        }
    }

    if (bmp != null) {
        Image(
            painter = BitmapPainter(bmp!!.asImageBitmap()),
            contentDescription = label,
            modifier = modifier,
        )
    } else {
        AppIconPlaceholder(label = label, pkg = pkg, modifier = modifier)
    }
}

@Composable
private fun AppIconPlaceholder(label: String, pkg: String, modifier: Modifier = Modifier) {
    val color = remember(pkg) {
        PlaceholderColors[pkg.hashCode().and(0x7FFFFFFF) % PlaceholderColors.size]
    }
    Box(
        modifier = modifier.background(color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
        )
    }
}
