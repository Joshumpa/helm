package dev.helm.launcher.media

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class LyricsRepository {

    suspend fun fetch(title: String, artist: String, durationSec: Int): LyricsState {
        if (title.isBlank()) return LyricsState.Unavailable
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://lrclib.net/api/get" +
                    "?artist_name=${Uri.encode(artist)}" +
                    "&track_name=${Uri.encode(title)}" +
                    "&duration=$durationSec"
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5_000
                    readTimeout = 8_000
                    setRequestProperty("User-Agent", "Helm/1.0 (dev.helm.launcher)")
                }
                if (conn.responseCode != 200) return@withContext LyricsState.Unavailable
                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                val synced = json.optString("syncedLyrics", "")
                val plain  = json.optString("plainLyrics",  "")
                when {
                    synced.isNotEmpty() -> LyricsState.Synced(LrcParser.parse(synced))
                    plain.isNotEmpty()  -> LyricsState.Plain(plain)
                    else                -> LyricsState.Unavailable
                }
            } catch (_: Exception) {
                LyricsState.Unavailable
            }
        }
    }
}
