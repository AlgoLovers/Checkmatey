package com.checkmatey.core.chess

/**
 * A board square addressed by [file] (0..7 -> a..h) and [rank] (0..7 -> 1..8).
 * Rank 0 is white's back rank.
 */
data class Square(val file: Int, val rank: Int) {
    init {
        require(file in 0..7) { "file out of range: $file" }
        require(rank in 0..7) { "rank out of range: $rank" }
    }

    /** Algebraic name such as "e4". */
    val name: String
        get() = "${'a' + file}${rank + 1}"

    companion object {
        /** Parses an algebraic square name such as "e4". */
        fun fromName(name: String): Square {
            require(name.length == 2) { "Square name must be 2 chars: $name" }
            val file = name[0].lowercaseChar() - 'a'
            val rank = name[1] - '1'
            return Square(file, rank)
        }
    }
}
