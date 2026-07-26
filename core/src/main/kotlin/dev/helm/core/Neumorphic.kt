package dev.helm.core

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────────────────────
// Static neumorphic shadow — use on non-interactive elements (album art, cards).
// ─────────────────────────────────────────────────────────────────────────────

fun Modifier.neumorphicShadow(
    backgroundColor: Color,
    cornerRadius: Dp = 16.dp,
    elevation: Dp = 7.dp,
): Modifier = drawBehind {
    drawNeumorphicExtruded(
        backgroundColor = backgroundColor,
        cornerPx = cornerRadius.toPx(),
        blurPx = elevation.toPx(),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Interactive neumorphic — v2 with pressed animation.
//
// Normal state  → extruded shadow (element emerges from surface)
// Pressed state → inset shadow (element sinks into surface)
//
// showAccentBorder: thin primary-color border for primary actions (play button,
// important CTAs) — addresses v1 accessibility weakness.
// ─────────────────────────────────────────────────────────────────────────────

fun Modifier.neumorphicClickable(
    onClick: () -> Unit,
    cornerRadius: Dp = 16.dp,
    elevation: Dp = 7.dp,
    showAccentBorder: Boolean = false,
): Modifier = composed {
    val bg      = MaterialTheme.colorScheme.background
    val primary = MaterialTheme.colorScheme.primary

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // depth > 0 → extruded  |  depth < 0 → inset  |  0 → flat
    val depth by animateFloatAsState(
        targetValue = if (isPressed) -(elevation.value * 0.40f) else elevation.value,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness    = Spring.StiffnessMediumLow,
        ),
        label = "neomorph_depth",
    )

    neumorphicDepth(backgroundColor = bg, cornerRadius = cornerRadius, depth = depth)
        .background(bg, RoundedCornerShape(cornerRadius))
        .then(
            if (showAccentBorder)
                Modifier.border(1.dp, primary.copy(alpha = 0.30f), RoundedCornerShape(cornerRadius))
            else Modifier
        )
        .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
}

// ─────────────────────────────────────────────────────────────────────────────
// Internal — handles extruded (depth > 0) and inset (depth < 0) in one modifier.
// ─────────────────────────────────────────────────────────────────────────────

internal fun Modifier.neumorphicDepth(
    backgroundColor: Color,
    cornerRadius: Dp = 16.dp,
    depth: Float = 7f,
): Modifier = drawWithContent {
    val cornerPx = cornerRadius.toPx()
    when {
        depth > 0.2f -> {
            drawNeumorphicExtruded(
                backgroundColor = backgroundColor,
                cornerPx = cornerPx,
                blurPx = depth.dp.toPx(),
            )
            drawContent()
        }
        depth < -0.2f -> {
            drawContent()
            drawNeumorphicInset(
                backgroundColor = backgroundColor,
                cornerPx = cornerPx,
                blurPx = (-depth).dp.toPx(),
            )
        }
        else -> drawContent()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Draw helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNeumorphicExtruded(
    backgroundColor: Color,
    cornerPx: Float,
    blurPx: Float,
) {
    val offsetPx = blurPx * 0.55f
    drawIntoCanvas { canvas ->
        val lightPaint = shadowPaint(backgroundColor.lighten(0.13f), blurPx)
        canvas.drawRoundRect(-offsetPx, -offsetPx, size.width - offsetPx, size.height - offsetPx, cornerPx, cornerPx, lightPaint)
        val darkPaint = shadowPaint(backgroundColor.darken(0.13f), blurPx)
        canvas.drawRoundRect(offsetPx, offsetPx, size.width + offsetPx, size.height + offsetPx, cornerPx, cornerPx, darkPaint)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNeumorphicInset(
    backgroundColor: Color,
    cornerPx: Float,
    blurPx: Float,
) {
    drawIntoCanvas { canvas ->
        // Clip all drawing to inside the shape so shadows don't leak out
        val clipPath = Path().apply {
            addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(cornerPx)))
        }
        canvas.save()
        canvas.clipPath(clipPath, ClipOp.Intersect)

        // Dark inset at top-left — rect starts outside, blur bleeds inward
        val darkPaint = shadowPaint(backgroundColor.darken(0.20f), blurPx)
        canvas.drawRoundRect(-blurPx, -blurPx, size.width * 0.65f, size.height * 0.65f, cornerPx, cornerPx, darkPaint)

        // Light inset at bottom-right
        val lightPaint = shadowPaint(backgroundColor.lighten(0.14f), blurPx)
        canvas.drawRoundRect(size.width * 0.35f, size.height * 0.35f, size.width + blurPx, size.height + blurPx, cornerPx, cornerPx, lightPaint)

        canvas.restore()
    }
}

private fun shadowPaint(color: Color, blurPx: Float): Paint = Paint().apply {
    asFrameworkPaint().apply {
        isAntiAlias = true
        this.color = color.toArgb()
        maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
    }
}

internal fun Color.lighten(factor: Float): Color = copy(
    red   = (red   + factor).coerceIn(0f, 1f),
    green = (green + factor).coerceIn(0f, 1f),
    blue  = (blue  + factor).coerceIn(0f, 1f),
)

internal fun Color.darken(factor: Float): Color = copy(
    red   = (red   - factor).coerceIn(0f, 1f),
    green = (green - factor).coerceIn(0f, 1f),
    blue  = (blue  - factor).coerceIn(0f, 1f),
)
