package dev.helm.launcher.music

import android.app.Application
import android.content.ComponentName
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import dev.helm.audio.HelmMusicService
import dev.helm.audio.MusicRepository
import dev.helm.audio.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MusicViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = MusicRepository(app)

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    init {
        viewModelScope.launch {
            _tracks.value = repo.loadTracks()
            _loading.value = false
        }
        connectToService()
    }

    private fun connectToService() {
        val token = SessionToken(
            getApplication(),
            ComponentName(getApplication(), HelmMusicService::class.java),
        )
        val future = MediaController.Builder(getApplication(), token).buildAsync()
        controllerFuture = future
        future.addListener(
            { controller = runCatching { future.get() }.getOrNull() },
            ContextCompat.getMainExecutor(getApplication()),
        )
    }

    fun play(track: Track) {
        val ctrl = controller ?: return
        ctrl.setMediaItem(MediaItem.fromUri(track.uri))
        ctrl.prepare()
        ctrl.play()
    }

    fun togglePlayPause() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun skipNext() = controller?.seekToNextMediaItem()
    fun skipPrev() = controller?.seekToPreviousMediaItem()

    override fun onCleared() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
}
