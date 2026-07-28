package dev.helm.launcher.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import dev.helm.core.Spacing
import kotlin.math.min
import kotlinx.coroutines.delay

@Composable
fun DashboardSection(speedKmh: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)),
    ) {
        SpeedometerGauge(
            speedKmh = speedKmh,
            modifier = Modifier
                .weight(0.45f)
                .fillMaxHeight()
                .padding(Spacing.md),
        )
        RoadAnimation(
            speedKmh = speedKmh,
            modifier = Modifier
                .weight(0.55f)
                .fillMaxHeight(),
        )
    }
}

@Composable
private fun RoadAnimation(speedKmh: Int, modifier: Modifier = Modifier) {
    val roadColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    val dashColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
    val shoulderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
    val carBodyColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val carOutlineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)

    var dashOffset by remember { mutableFloatStateOf(0f) }
    val speedState = rememberUpdatedState(speedKmh)

    LaunchedEffect(Unit) {
        var lastMs = System.currentTimeMillis()
        while (true) {
            delay(16L)
            val now = System.currentTimeMillis()
            val dtSec = (now - lastMs) / 1000f
            lastMs = now
            val pxPerSec = if (speedState.value == 0) 8f else speedState.value * 2.4f
            dashOffset = (dashOffset + pxPerSec * dtSec) % 80f
        }
    }

    Canvas(modifier = modifier) {
        val roadWidth = size.width * 0.62f
        val roadLeft = (size.width - roadWidth) / 2f
        val dashPeriod = 80f
        val dashLen = 26f
        val dashW = roadWidth * 0.07f
        val cx = size.width / 2f

        drawRect(color = roadColor, topLeft = Offset(roadLeft, 0f), size = Size(roadWidth, size.height))
        drawRect(color = shoulderColor, topLeft = Offset(roadLeft - 10f, 0f), size = Size(10f, size.height))
        drawRect(color = shoulderColor, topLeft = Offset(roadLeft + roadWidth, 0f), size = Size(10f, size.height))

        var y = -dashPeriod + dashOffset
        while (y < size.height) {
            val top = y.coerceAtLeast(0f)
            val bot = (y + dashLen).coerceAtMost(size.height)
            if (bot > top) {
                drawRect(color = dashColor, topLeft = Offset(cx - dashW / 2f, top), size = Size(dashW, bot - top))
            }
            y += dashPeriod
        }

        drawCarSilhouette(cx = cx, bottom = size.height - 14f, width = roadWidth * 0.42f, body = carBodyColor, outline = carOutlineColor)
    }
}

private fun DrawScope.drawCarSilhouette(cx: Float, bottom: Float, width: Float, body: Color, outline: Color) {
    val h = width * 2.1f
    val top = bottom - h
    val hw = width / 2f
    val cabinStart = top + h * 0.27f
    val cabinEnd = top + h * 0.62f

    val path = Path().apply {
        moveTo(cx - hw * 0.78f, bottom)
        lineTo(cx - hw, cabinEnd)
        lineTo(cx - hw * 0.72f, cabinStart)
        lineTo(cx - hw * 0.42f, top)
        lineTo(cx + hw * 0.42f, top)
        lineTo(cx + hw * 0.72f, cabinStart)
        lineTo(cx + hw, cabinEnd)
        lineTo(cx + hw * 0.78f, bottom)
        close()
    }

    drawPath(path, color = body)
    drawPath(path, color = outline, style = Stroke(width = 1.5f, cap = StrokeCap.Round))
}

@Composable
private fun SpeedometerGauge(speedKmh: Int, modifier: Modifier = Modifier) {
    val fraction = (speedKmh.coerceIn(0, 120) / 120f)

    val animFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(600),
        label = "gauge_fraction",
    )
    val gaugeColor by animateColorAsState(
        targetValue = when {
            speedKmh >= 90 -> MaterialTheme.colorScheme.error
            speedKmh >= 50 -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outline
        },
        animationSpec = tween(400),
        label = "gauge_color",
    )
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
    val bgColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.28f)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sz = min(size.width, size.height)
            val r = sz / 2f * 0.88f
            val cx = size.width / 2f
            val cy = size.height / 2f
            val arcTopLeft = Offset(cx - r, cy - r)
            val arcSize = Size(r * 2f, r * 2f)
            val strokeW = sz * 0.07f

            drawCircle(color = bgColor, radius = r * 1.06f, center = Offset(cx, cy))
            drawArc(
                color = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(strokeW, cap = StrokeCap.Round),
            )
            if (animFraction > 0.005f) {
                drawArc(
                    color = gaugeColor,
                    startAngle = 135f,
                    sweepAngle = 270f * animFraction,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(strokeW, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$speedKmh",
                style = MaterialTheme.typography.displaySmall,
                color = gaugeColor,
            )
            Text(
                text = "km/h",
                style = MaterialTheme.typography.labelSmall,
                color = gaugeColor.copy(alpha = 0.65f),
            )
        }
    }
}
