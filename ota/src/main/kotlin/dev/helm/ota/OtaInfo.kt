package dev.helm.ota

data class OtaInfo(
    val versionName: String,
    val apkUrl: String,
    val changelog: String,
)

// Semantic-ish version compare. Tolerates a leading "v", ignores pre-release ("-rc1")
// and build metadata ("+42") suffixes, and pads missing components with 0 so
// "1.2" and "1.2.0" compare equal.
internal fun isNewer(server: String, installed: String): Boolean {
    fun String.components(): List<Int> {
        val core = substringBefore('+').substringBefore('-').trimStart('v')
        val parts = core.split('.').mapNotNull { it.toIntOrNull() }
        return parts.ifEmpty { listOf(0) }
    }
    val s = server.components()
    val c = installed.components()
    for (i in 0 until maxOf(s.size, c.size)) {
        val sv = s.getOrElse(i) { 0 }
        val cv = c.getOrElse(i) { 0 }
        if (sv > cv) return true
        if (sv < cv) return false
    }
    return false
}
