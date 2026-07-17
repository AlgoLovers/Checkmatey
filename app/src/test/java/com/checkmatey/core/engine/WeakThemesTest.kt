package com.checkmatey.core.engine

import com.checkmatey.core.chess.Position
import org.junit.Assert.assertTrue
import org.junit.Test

class WeakThemesTest {

    private val reviewer = GameReviewer(Annotator(KotlinMinimaxEngine(), depth = 3))

    @Test
    fun blunderingAFreeQueenSuggestsMaterialTheme() {
        // White ignores a free queen (blunder); the better move captured it.
        val pos = Position.fromFen("4k3/8/8/8/3q4/8/8/3RK3 w - - 0 1")
        val quietMove = pos.legalMoves().first { it.from == Square("e1") && it.to == Square("e2") }
        val annotation = reviewer.let { Annotator(KotlinMinimaxEngine(), 3).annotate(pos, quietMove) }
        val themes = reviewer.weakThemes(listOf(annotation))
        assertTrue("expected a material theme: $themes", themes.contains("기물 이득"))
    }

    @Test
    fun goodMovesSuggestNothing() {
        val start = Position.startingPosition()
        val e4 = start.legalMoves().first { it.from == Square("e2") && it.to == Square("e4") }
        val a = Annotator(KotlinMinimaxEngine(), 3).annotate(start, e4)
        assertTrue(reviewer.weakThemes(listOf(a)).isEmpty())
    }
}

private fun Square(name: String) = com.checkmatey.core.chess.Square.fromName(name)
