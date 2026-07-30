package dev.helm.launcher.media

import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// METADATA_KEY_LYRICS is @hide in android.media.MediaMetadata — use the raw key string instead.
private const val METADATA_KEY_LYRICS = "android.media.metadata.LYRICS"

class MediaRepository(private val context: Context) {

    private val _state = MutableStateFlow(NowPlayingState())
    val state: StateFlow<NowPlayingState> = _state.asStateFlow()

    private var controller: MediaController? = null

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = sync()
        override fun onPlaybackStateChanged(state: PlaybackState?) = sync()
    }

    fun connectTo(token: MediaSession.Token) {
        controller?.unregisterCallback(callback)
        controller = MediaController(context, token).also {
            it.registerCallback(callback)
            sync()
        }
    }

    fun release() {
        controller?.unregisterCallback(callback)
        controller = null
        _state.value = NowPlayingState()
    }

    fun livePositionMs(): Long {
        val pb = controller?.playbackState ?: return 0L
        if (pb.state != PlaybackState.STATE_PLAYING) return pb.position
        return pb.position + (SystemClock.elapsedRealtime() - pb.lastPositionUpdateTime)
    }

    fun transportControls(): MediaController.TransportControls? = controller?.transportControls

    private fun sync() {
        val meta = controller?.metadata
        val pb   = controller?.playbackState
        _state.value = NowPlayingState(
            title    = meta?.getString(MediaMetadata.METADATA_KEY_TITLE)  ?: "",
            artist   = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "",
            album    = meta?.getString(MediaMetadata.METADATA_KEY_ALBUM)  ?: "",
            artwork  = meta?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                    ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART),
            durationMs      = meta?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
            positionMs      = pb?.position ?: 0L,
            isPlaying       = pb?.state == PlaybackState.STATE_PLAYING,
            metadataLyrics  = meta?.getString(METADATA_KEY_LYRICS),
        )
    }
}
