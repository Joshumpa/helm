package dev.helm.launcher.media

data class LrcLine(val timeMs: Long, val text: String)

sealed interface LyricsState {
    data object Idle : LyricsState
    data object Loading : LyricsState
    data object Unavailable : LyricsState
    data class Plain(val text: String) : LyricsState
    data class Synced(val lines: List<LrcLine>) : LyricsState
}
