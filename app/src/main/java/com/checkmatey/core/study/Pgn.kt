package com.checkmatey.core.study

/** A parsed PGN game: header tags plus the movetext as a list of SAN tokens. */
data class PgnGame(val tags: Map<String, String>, val sanMoves: List<String>) {
    val white: String get() = tags["White"] ?: "?"
    val black: String get() = tags["Black"] ?: "?"
    val event: String get() = tags["Event"] ?: ""
    val date: String get() = tags["Date"] ?: ""
    val result: String get() = tags["Result"] ?: "*"

    /** Four-digit year if the Date tag has one, else "". */
    val year: String
        get() = date.take(4).takeIf { it.length == 4 && it.all(Char::isDigit) } ?: ""
}

/** Minimal PGN reader: tags + movetext, tolerant of comments, variations, NAGs and move numbers. */
object PgnParser {

    private val results = setOf("1-0", "0-1", "1/2-1/2", "*")
    private val tagRegex = Regex("""\[(\w+)\s+"([^"]*)"]""")

    fun parse(pgn: String): List<PgnGame> =
        Regex("""(?=\[Event )""").split(pgn)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { parseOne(it) }

    private fun parseOne(chunk: String): PgnGame {
        val tags = tagRegex.findAll(chunk).associate { it.groupValues[1] to it.groupValues[2] }
        val movetext = chunk.lines().filterNot { it.trimStart().startsWith("[") }.joinToString(" ")
        return PgnGame(tags, tokenize(movetext))
    }

    private fun tokenize(movetext: String): List<String> {
        val cleaned = movetext
            .replace(Regex("""\{[^}]*\}"""), " ") // comments (escape } — Android's ICU regex is strict)
            .replace(Regex("""\([^)]*\)"""), " ") // variations (assumes non-nested)
            .replace(Regex("""\$\d+"""), " ") // NAGs
        return cleaned.trim().split(Regex("""\s+""")).mapNotNull { token ->
            // A token may be a bare move number ("12."), a numbered move ("12.e4"), or a move.
            val move = token.replace(Regex("""^\d+\.(\.\.)?"""), "").trim()
            when {
                move.isEmpty() -> null
                move in results -> null
                else -> move
            }
        }
    }
}
