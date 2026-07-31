package dev.helm.audio

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dev.helm.sdk.AudioSourceId
import dev.helm.sdk.McuServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@UnstableApi
class HelmMusicService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus= */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        val sessionActivity = PendingIntent.getActivity(
            this, 0,
            packageManager.getLaunchIntentForPackage(packageName) ?: Intent(),
            PendingIntent.FLAG_IMMUTABLE,
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .build()

        // Task 2.7 — pause music when MCU switches audio source away from USB/BT music.
        if (McuServiceLocator.isInitialized) {
            McuServiceLocator.service.audioSource
                .onEach { sourceId ->
                    if (sourceId != AudioSourceId.USB_MUSIC && sourceId != AudioSourceId.BLUETOOTH) {
                        player.pause()
                    }
                }
                .launchIn(serviceScope)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        // isTrusted covers: callers holding MEDIA_CONTENT_CONTROL and system UID.
        // packageName from connectionHints is caller-supplied — not a reliable origin check.
        if (controllerInfo.isTrusted) mediaSession else null

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
