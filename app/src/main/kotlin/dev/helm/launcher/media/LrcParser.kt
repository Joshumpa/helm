package dev.helm.launcher.media

object LrcParser {

    private val LINE_RE = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)""")
    private const val MAX_LINES = 5_000

    fun parse(lrc: String): List<LrcLine> = lrc.lines()
        .take(MAX_LINES)
        .mapNotNull { line ->
            LINE_RE.find(line)?.let { m ->
                val ms = m.groupValues[1].toLong() * 60_000 +
                         m.groupValues[2].toLong() * 1_000 +
                         m.groupValues[3].padEnd(3, '0').toLong()
                val text = m.groupValues[4].trim()
                if (text.isEmpty()) null else LrcLine(ms, text)
            }
        }
        .sortedBy { it.timeMs }
}
