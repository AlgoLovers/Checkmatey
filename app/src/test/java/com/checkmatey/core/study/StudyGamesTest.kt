package com.checkmatey.core.study

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyGamesTest {

    private val games = StudyGames.all()

    @Test
    fun bundlesTheExpectedGames() {
        assertEquals(5, games.size)
        assertTrue(games.any { it.meta.white.contains("Morphy") })
        assertTrue(games.any { it.meta.white.contains("Anderssen") })
        assertTrue(games.any { it.meta.white.contains("Legal") })
        assertTrue(games.any { it.meta.white.contains("Reti") })
    }

    @Test
    fun everyGameParsesFully() {
        // If any SAN token doesn't map to a legal move, the build stops early and this catches it.
        for (game in games) {
            assertEquals(
                "${game.title}: ${game.moves.size}/${game.meta.sanMoves.size} moves parsed",
                game.meta.sanMoves.size,
                game.moves.size,
            )
        }
    }

    @Test
    fun everyGameEndsInCheckmate() {
        // All three classics finish with #, so the final position must be checkmate.
        for (game in games) {
            assertTrue("${game.title} should end in checkmate", game.positions.last().isCheckmate())
        }
    }

    @Test
    fun positionsAlignWithMoves() {
        for (game in games) {
            assertEquals(game.moves.size + 1, game.positions.size)
        }
    }
}
