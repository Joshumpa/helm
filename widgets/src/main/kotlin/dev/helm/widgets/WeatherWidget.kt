package dev.helm.widgets

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.helm.sdk.WeatherCondition
import dev.helm.sdk.WeatherDataSource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val SunColor = Color(0xFFFFB300)
private val SunRayColor = Color(0xFFFFCC02)
private val DropColor = Color(0xFF64B5F6)
private val FlakeColor = Color(0xFFB3E5FC)
private val BoltColor = Color(0xFFFFEB3B)

// compact=true → small Row (badge for top bar)
// compact=false → full Column widget
@Composable
fun WeatherWidget(
    source: WeatherDataSource,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val state by source.state.collectAsState()
    val s = state ?: return

    val transition = rememberInfiniteTransition(label = "weather")
    val rotation by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(10_000, easing = LinearEasing)),
        label = "rotation",
    )
    val fall by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2_200, easing = LinearEasing)),
        label = "fall",
    )
    val pulse by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3_000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    val flash by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2_500, easing = LinearEasing)),
        label = "flash",
    )

    val cloudColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
    val onBg = MaterialTheme.colorScheme.onBackground
    val onVariant = MaterialTheme.colorScheme.onSurfaceVariant

    if (compact) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            WeatherCanvas(
                modifier = Modifier.size(36.dp),
                condition = s.condition,
                rotation = rotation, fall = fall, pulse = pulse, flash = flash,
                cloudColor = cloudColor,
            )
            Text(
                text = "${s.temperatureCelsius.toInt()}°",
                style = MaterialTheme.typography.titleMedium,
                color = onBg,
            )
        }
    } else {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WeatherCanvas(
                modifier = Modifier.size(64.dp),
                condition = s.condition,
                rotation = rotation, fall = fall, pulse = pulse, flash = flash,
                cloudColor = cloudColor,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${s.temperatureCelsius.toInt()}°",
                style = MaterialTheme.typography.titleMedium,
                color = onBg,
            )
            Text(
                text = s.condition.label,
                style = MaterialTheme.typography.labelSmall,
                color = onVariant,
            )
        }
    }
}

@Composable
private fun WeatherCanvas(
    modifier: Modifier,
    condition: WeatherCondition,
    rotation: Float,
    fall: Float,
    pulse: Float,
    flash: Float,
    cloudColor: Color,
) {
    Canvas(modifier) {
        when (condition) {
            WeatherCondition.CLEAR       -> drawSun(rotation)
            WeatherCondition.CLOUDY      -> drawCloud(pulse, cloudColor)
            WeatherCondition.RAIN        -> drawRain(fall, pulse, cloudColor)
            WeatherCondition.THUNDERSTORM -> drawThunderstorm(fall, flash, pulse, cloudColor)
            WeatherCondition.SNOW        -> drawSnow(fall, pulse, cloudColor)
            WeatherCondition.HAZE        -> drawHaze(pulse, cloudColor)
        }
    }
}

// ── Canvas drawing — each function is an extension on DrawScope ──────────────

private fun DrawScope.drawSun(rotation: Float) {
    val cx = size.width / 2
    val cy = size.height / 2
    val radius = size.minDimension * 0.24f
    val rayInner = radius + size.minDimension * 0.07f
    val rayOuter = size.minDimension * 0.44f

    drawCircle(SunColor, radius = radius, center = Offset(cx, cy))
    repeat(8) { i ->
        val a = ((i * 45f + rotation) * PI / 180.0).toFloat()
        val ca = cos(a); val sa = sin(a)
        drawLine(
            color = SunRayColor,
            start = Offset(cx + ca * rayInner, cy + sa * rayInner),
            end = Offset(cx + ca * rayOuter, cy + sa * rayOuter),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.cloudShape(cx: Float, cy: Float, color: Color) {
    val r = size.minDimension * 0.17f
    drawCircle(color, radius = r * 0.8f, center = Offset(cx - r * 0.8f, cy + r * 0.2f))
    drawCircle(color, radius = r, center = Offset(cx, cy))
    drawCircle(color, radius = r * 0.75f, center = Offset(cx + r, cy + r * 0.15f))
    drawRect(
        color = color,
        topLeft = Offset(cx - r * 1.6f, cy + r * 0.22f),
        size = Size(r * 3.3f, r * 0.8f),
    )
}

private fun DrawScope.drawCloud(pulse: Float, cloudColor: Color) {
    val dx = (pulse - 0.5f) * 6.dp.toPx()
    cloudShape(size.width / 2 + dx, size.height / 2, cloudColor)
}

private fun DrawScope.drawRain(fall: Float, pulse: Float, cloudColor: Color) {
    val dx = (pulse - 0.5f) * 4.dp.toPx()
    cloudShape(size.width / 2 + dx, size.height * 0.37f, cloudColor)

    val areaTop = size.height * 0.62f
    val areaH = size.height * 0.30f
    repeat(7) { i ->
        val xFrac = 0.15f + i * 0.115f
        val yFrac = (fall + i * 0.143f) % 1f
        val y = areaTop + yFrac * areaH
        val alpha = if (yFrac > 0.75f) (1f - yFrac) / 0.25f else 1f
        drawLine(
            color = DropColor.copy(alpha = (alpha * 0.85f).coerceIn(0f, 1f)),
            start = Offset(size.width * xFrac, y),
            end = Offset(size.width * xFrac - 2.dp.toPx(), y + 5.dp.toPx()),
            strokeWidth = 1.5.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawThunderstorm(
    fall: Float,
    flash: Float,
    pulse: Float,
    cloudColor: Color,
) {
    val dx = (pulse - 0.5f) * 4.dp.toPx()
    cloudShape(size.width / 2 + dx, size.height * 0.32f, cloudColor)

    val areaTop = size.height * 0.54f
    val areaH = size.height * 0.22f
    repeat(5) { i ->
        val xFrac = 0.15f + i * 0.175f
        val yFrac = ((fall * 1.6f) + i * 0.2f) % 1f
        val y = areaTop + yFrac * areaH
        val alpha = if (yFrac > 0.75f) (1f - yFrac) / 0.25f else 1f
        drawLine(
            color = DropColor.copy(alpha = (alpha * 0.8f).coerceIn(0f, 1f)),
            start = Offset(size.width * xFrac, y),
            end = Offset(size.width * xFrac - 2.dp.toPx(), y + 5.dp.toPx()),
            strokeWidth = 1.5.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }

    // Lightning bolt — visible for ~15% of each 2.5s cycle (≈375ms)
    val boltAlpha = if (flash < 0.08f) flash / 0.08f
    else if (flash < 0.15f) 1f
    else if (flash < 0.22f) (0.22f - flash) / 0.07f
    else 0f
    if (boltAlpha > 0f) {
        val bx = size.width / 2
        val bTop = size.height * 0.56f
        val bMid = size.height * 0.72f
        val bBot = size.height * 0.87f
        val path = Path().apply {
            moveTo(bx + 4.dp.toPx(), bTop)
            lineTo(bx - 2.dp.toPx(), bMid)
            lineTo(bx + 2.dp.toPx(), bMid)
            lineTo(bx - 4.dp.toPx(), bBot)
        }
        drawPath(
            path,
            BoltColor.copy(alpha = boltAlpha),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

private fun DrawScope.drawSnow(fall: Float, pulse: Float, cloudColor: Color) {
    val dx = (pulse - 0.5f) * 4.dp.toPx()
    cloudShape(size.width / 2 + dx, size.height * 0.37f, cloudColor)

    val areaTop = size.height * 0.60f
    val areaH = size.height * 0.35f
    repeat(8) { i ->
        val xFrac = 0.12f + i * 0.11f
        val yFrac = (fall + i * 0.125f) % 1f
        val swayX = sin((fall * 2.0 * PI + i).toFloat()) * 4.dp.toPx()
        val y = areaTop + yFrac * areaH
        val alpha = if (yFrac > 0.8f) (1f - yFrac) / 0.2f else 1f
        drawCircle(
            color = FlakeColor.copy(alpha = (alpha * 0.9f).coerceIn(0f, 1f)),
            radius = 2.5.dp.toPx(),
            center = Offset(size.width * xFrac + swayX, y),
        )
    }
}

private fun DrawScope.drawHaze(pulse: Float, bandColor: Color) {
    repeat(4) { i ->
        val y = size.height * (0.18f + i * 0.21f)
        val bandW = size.width * (if (i % 2 == 0) 0.84f else 0.62f)
        val xOff = (size.width - bandW) / 2
        val phase = ((pulse + i * 0.25f) % 1f)
        val alpha = 0.22f + 0.55f * (if (phase < 0.5f) phase * 2 else (1f - phase) * 2)
        drawRoundRect(
            color = bandColor.copy(alpha = alpha.coerceIn(0f, 1f)),
            topLeft = Offset(xOff, y),
            size = Size(bandW, 6.dp.toPx()),
            cornerRadius = CornerRadius(3.dp.toPx()),
        )
    }
}
