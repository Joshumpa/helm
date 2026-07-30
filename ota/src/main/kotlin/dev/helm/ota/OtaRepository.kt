package dev.helm.ota

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

// The release workflow publishes latest.json as a GitHub Release asset.
// GitHub's /releases/latest/download/ redirect always points to the newest release.
// Replace YOUR_GITHUB_USERNAME once the repo is created.
private const val UPDATE_CHECK_URL =
    "https://github.com/YOUR_GITHUB_USERNAME/helm/releases/latest/download/latest.json"

private const val CONNECT_TIMEOUT_MS = 10_000
private const val READ_TIMEOUT_MS = 30_000

class OtaRepository {

    fun fetchLatest(): OtaInfo? {
        val conn = URL(UPDATE_CHECK_URL).openConnection() as HttpURLConnection
        conn.connectTimeout = CONNECT_TIMEOUT_MS
        conn.readTimeout = READ_TIMEOUT_MS
        return try {
            conn.connect()
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return null
            val json = JSONObject(conn.inputStream.bufferedReader().readText())
            OtaInfo(
                versionCode = json.getInt("versionCode"),
                versionName = json.getString("versionName"),
                apkUrl = json.getString("apkUrl"),
                changelog = json.optString("changelog", ""),
            )
        } finally {
            conn.disconnect()
        }
    }

    fun download(apkUrl: String, destFile: File, onProgress: (Float) -> Unit) {
        val conn = URL(apkUrl).openConnection() as HttpURLConnection
        conn.connectTimeout = CONNECT_TIMEOUT_MS
        conn.readTimeout = READ_TIMEOUT_MS
        try {
            conn.connect()
            streamToFile(conn, destFile, onProgress)
        } finally {
            conn.disconnect()
        }
    }

    private fun streamToFile(conn: HttpURLConnection, destFile: File, onProgress: (Float) -> Unit) {
        val total = conn.contentLength.toLong()
        conn.inputStream.use { input ->
            destFile.outputStream().use { output ->
                input.copyTracked(output, total, onProgress)
            }
        }
    }

    private fun InputStream.copyTracked(output: OutputStream, total: Long, onProgress: (Float) -> Unit) {
        val buffer = ByteArray(8 * 1024)
        var written = 0L
        var read: Int
        while (read(buffer).also { read = it } != -1) {
            output.write(buffer, 0, read)
            written += read
            if (total > 0) onProgress(written.toFloat() / total)
        }
    }

    fun install(context: Context, apkFile: File) {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, apkFile)
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            },
        )
    }
}
