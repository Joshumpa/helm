package dev.helm.launcher.media

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class NowPlayingViewModel(app: Application) : AndroidViewModel(app) {

    private val mediaRepo  = MediaRepository(app)
    private val lyricsRepo = LyricsRepository()

    val media: StateFlow<NowPlayingState> = mediaRepo.state

    private val _lyrics = MutableStateFlow<LyricsState>(LyricsState.Idle)
    val lyrics: StateFlow<LyricsState> = _lyrics.asStateFlow()

    init {
        viewModelScope.launch {
            HelmNotificationListener.mediaToken
                .filterNotNull()
                .collect { mediaRepo.connectTo(it) }
        }

        viewModelScope.launch {
            media.distinctUntilChangedBy { "${it.title}::${it.artist}" }
                .collect { state ->
                    if (state.title.isBlank()) { _lyrics.value = LyricsState.Idle; return@collect }
                    state.metadataLyrics?.let { _lyrics.value = LyricsState.Plain(it); return@collect }
                    _lyrics.value = LyricsState.Loading
                    _lyrics.value = lyricsRepo.fetch(
                        title       = state.title,
                        artist      = state.artist,
                        durationSec = (state.durationMs / 1000).toInt(),
                    )
                }
        }
    }

    fun livePositionMs(): Long = mediaRepo.livePositionMs()
    fun play()     = mediaRepo.transportControls()?.play()
    fun pause()    = mediaRepo.transportControls()?.pause()
    fun skipNext() = mediaRepo.transportControls()?.skipToNext()
    fun skipPrev() = mediaRepo.transportControls()?.skipToPrevious()
    fun seekTo(ms: Long) = mediaRepo.transportControls()?.seekTo(ms)

    override fun onCleared() {
        mediaRepo.release()
        super.onCleared()
    }
}
