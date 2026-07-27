package dev.helm.launcher.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SplashScreen() {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    val alpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "splash_alpha",
    )
    val wordScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.88f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "splash_scale",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                scaleX = wordScale
                scaleY = wordScale
                this.alpha = alpha
            },
        ) {
            HelmLogo(modifier = Modifier.size(120.dp))
            Spacer(Modifier.height(28.dp))
            Text(
                text = "HELM",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 18.sp,
                ),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "LAUNCHER",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 10.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HelmLogo(modifier: Modifier = Modifier) {
    val blue = MaterialTheme.colorScheme.primary
    val bg = MaterialTheme.colorScheme.background

    Canvas(modifier = modifier) {
        val half = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Radial glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(blue.copy(alpha = 0.18f), Color.Transparent),
                center = center,
                radius = half,
            ),
        )

        // Outer ring
        drawCircle(
            color = blue,
            radius = half * 0.75f,
            center = center,
            style = Stroke(width = half * 0.078f, cap = StrokeCap.Round),
        )

        // 3 spokes at 30°, 180°, 330° — matching helm-logo.svg geometry
        val strokeW = half * 0.070f
        val spokes = listOf(
            Offset(center.x + half * 0.375f, center.y - half * 0.648f),
            Offset(center.x, center.y + half * 0.75f),
            Offset(center.x - half * 0.375f, center.y - half * 0.648f),
        )
        spokes.forEach { end ->
            drawLine(blue, center, end, strokeW, StrokeCap.Round)
        }

        // Hub fill + inner cutout
        drawCircle(blue, half * 0.172f, center)
        drawCircle(bg, half * 0.078f, center)
    }
}
