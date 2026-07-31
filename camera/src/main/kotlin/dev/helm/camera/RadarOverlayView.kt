package dev.helm.camera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import dev.helm.sdk.McuEvent

private const val CRITICAL_DISTANCE_CM = 30

class RadarOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private var frontDistances: List<Int> = List(4) { 255 }
    private var rearDistances: List<Int> = List(4) { 255 }
    private var frontMax = 255
    private var rearMax = 255

    private val safePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GREEN
        style = Paint.Style.FILL
        alpha = 180
    }
    private val warnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.YELLOW
        style = Paint.Style.FILL
        alpha = 180
    }
    private val criticalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        style = Paint.Style.FILL
        alpha = 200
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    fun update(event: McuEvent.ParkingRadarChanged) {
        frontDistances = event.front
        frontMax = event.frontMax
        rearDistances = event.rear
        rearMax = event.rearMax
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val sensorW = w / 4f
        val barH = h * 0.12f

        // Front sensors — top of screen
        frontDistances.forEachIndexed { i, dist ->
            val paint = paintForDistance(dist)
            val rect = RectF(i * sensorW + 4f, 4f, (i + 1) * sensorW - 4f, barH)
            canvas.drawRoundRect(rect, 8f, 8f, paint)
            canvas.drawText("${dist}cm", rect.centerX(), rect.centerY() + labelPaint.textSize / 3, labelPaint)
        }

        // Rear sensors — bottom of screen
        rearDistances.forEachIndexed { i, dist ->
            val paint = paintForDistance(dist)
            val rect = RectF(i * sensorW + 4f, h - barH - 4f, (i + 1) * sensorW - 4f, h - 4f)
            canvas.drawRoundRect(rect, 8f, 8f, paint)
            canvas.drawText("${dist}cm", rect.centerX(), rect.centerY() + labelPaint.textSize / 3, labelPaint)
        }
    }

    private fun paintForDistance(cm: Int): Paint = when {
        cm <= CRITICAL_DISTANCE_CM -> criticalPaint
        cm <= CRITICAL_DISTANCE_CM * 3 -> warnPaint
        else -> safePaint
    }
}
