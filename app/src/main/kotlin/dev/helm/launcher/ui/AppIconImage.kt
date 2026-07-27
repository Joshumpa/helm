package dev.helm.launcher.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Panorama
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.helm.core.theme.PlaceholderColors
import dev.helm.launcher.LauncherAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val actionIconMap: Map<LauncherAction, ImageVector> = mapOf(
    LauncherAction.RADIO          to Icons.Filled.Radio,
    LauncherAction.BLUETOOTH      to Icons.Filled.Bluetooth,
    LauncherAction.CARPLAY        to Icons.Filled.Cast,
    LauncherAction.REVERSE_CAMERA to Icons.Filled.Camera,
    LauncherAction.RIGHT_CAMERA   to Icons.Filled.CameraAlt,
    LauncherAction.CAMERA_360     to Icons.Filled.Panorama,
    LauncherAction.MUSIC          to Icons.Filled.MusicNote,
    LauncherAction.VIDEO          to Icons.Filled.Movie,
    LauncherAction.DVR            to Icons.Filled.Videocam,
    LauncherAction.EQ             to Icons.Filled.Equalizer,
    LauncherAction.AUX            to Icons.Filled.Headphones,
    LauncherAction.CAR_SETTINGS   to Icons.Filled.DirectionsCar,
    LauncherAction.SETTINGS       to Icons.Filled.Settings,
    LauncherAction.SCREENSAVER    to Icons.Filled.DarkMode,
)

@Composable
fun AppIconImage(
    pkg: String,
    label: String,
    action: LauncherAction? = null,
    modifier: Modifier = Modifier,
) {
    val icon = action?.let { actionIconMap[it] }
    if (icon != null) {
        ActionIcon(icon = icon, modifier = modifier)
    } else {
        PackageIconFallback(pkg = pkg, label = label, modifier = modifier)
    }
}

@Composable
private fun ActionIcon(icon: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            RoundedCornerShape(16.dp),
        ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxSize(0.6f),
        )
    }
}

@Composable
private fun PackageIconFallback(pkg: String, label: String, modifier: Modifier = Modifier) {
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
