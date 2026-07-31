package dev.helm.camera

import android.app.Activity
import android.media.AudioManager
import android.os.Bundle
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import dev.helm.sdk.McuServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

// Task 6.3/6.4/6.5/6.6/6.7/6.8 — full-screen reverse camera. Launched by ReverseCameraManager.
// Back press is a no-op while in reverse; dismissed programmatically when gear changes.
class ReverseCameraActivity : Activity() {

    private val activityScope = MainScope()
    private lateinit var surfaceView: SurfaceView
    private lateinit var radarOverlay: RadarOverlayView
    private lateinit var noSignalBanner: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val root = FrameLayout(this)
        surfaceView = SurfaceView(this)
        radarOverlay = RadarOverlayView(this)
        noSignalBanner = TextView(this).apply {
            text = "Sin señal"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 18f
            gravity = android.view.Gravity.CENTER
            setBackgroundColor(android.graphics.Color.argb(160, 0, 0, 0))
            visibility = View.GONE
        }

        root.addView(surfaceView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        root.addView(radarOverlay, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        root.addView(noSignalBanner, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT).also {
            it.gravity = android.view.Gravity.CENTER
        })

        setContentView(root)
        _instance = this

        if (!McuServiceLocator.isInitialized) {
            showStubMode()
            return
        }

        val service = McuServiceLocator.service

        // Task 6.4 — ack MCU when camera is visible
        activityScope.launch { service.send(0x0304, 1) }

        // Task 6.6 — handle stream lost
        service.cameraStreamActive
            .onEach { active -> noSignalBanner.visibility = if (active) View.GONE else View.VISIBLE }
            .launchIn(activityScope)

        // Task 6.5 — parking radar overlay; alert on critical distance
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        service.parkingRadar
            .onEach { event ->
                radarOverlay.update(event)
                val criticalThreshold = 30
                val allDistances = event.front + event.rear
                if (allDistances.any { it <= criticalThreshold }) {
                    audioManager.playSoundEffect(AudioManager.FX_KEY_CLICK)
                }
            }
            .launchIn(activityScope)
    }

    // Back press is a no-op while in reverse — user cannot dismiss camera manually.
    override fun onBackPressed() {
        if (McuServiceLocator.isInitialized && McuServiceLocator.service.reverseGear.value) return
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }

    override fun onDestroy() {
        if (_instance === this) _instance = null
        if (McuServiceLocator.isInitialized) {
            activityScope.launch { McuServiceLocator.service.send(0x0304, 0) }
        }
        activityScope.cancel()
        super.onDestroy()
    }

    // Task 6.7 — stub mode placeholder
    private fun showStubMode() {
        surfaceView.visibility = View.GONE
        noSignalBanner.text = "Cámara reversa — No disponible (Track B)"
        noSignalBanner.visibility = View.VISIBLE
    }

    companion object {
        @Volatile private var _instance: ReverseCameraActivity? = null

        fun requestDismiss() {
            _instance?.finish()
        }
    }
}
