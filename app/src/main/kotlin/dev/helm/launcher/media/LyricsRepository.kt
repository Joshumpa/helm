package dev.helm.launcher.media

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

private const val MAX_RESPONSE_BYTES = 512 * 1024  // 512 KB — generous for any LRC file

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
                    setRequestProperty("User-Agent", "Helm/1.0")
                }
                if (conn.responseCode != 200) return@withContext LyricsState.Unavailable
                val contentType = conn.getHeaderField("Content-Type") ?: ""
                if (!contentType.startsWith("application/json")) return@withContext LyricsState.Unavailable
                val bytes = conn.inputStream.readLimited(MAX_RESPONSE_BYTES)
                    ?: return@withContext LyricsState.Unavailable
                val json = JSONObject(bytes.toString(Charsets.UTF_8))
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

    private fun InputStream.readLimited(maxBytes: Int): ByteArray? {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(8_192)
        var read: Int
        while (read(buf).also { read = it } != -1) {
            out.write(buf, 0, read)
            if (out.size() > maxBytes) return null
        }
        return out.toByteArray()
    }
}
