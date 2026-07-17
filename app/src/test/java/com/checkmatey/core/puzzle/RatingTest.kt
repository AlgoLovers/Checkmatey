package com.checkmatey.core.puzzle

import org.junit.Assert.assertTrue
import org.junit.Test

class RatingTest {

    @Test
    fun solvingRaisesRatingAndHarderRaisesMore() {
        val gainEasy = Rating.update(1000, 800, true) - 1000
        val gainHard = Rating.update(1000, 1200, true) - 1000
        assertTrue("solving should raise rating", gainEasy > 0)
        assertTrue("harder puzzle should raise more", gainHard > gainEasy)
    }

    @Test
    fun failingLowersRating() {
        assertTrue(Rating.update(1000, 800, false) < 1000)
    }

    @Test
    fun anEvenMatchMovesLittle() {
        val delta = Rating.update(1000, 1000, true) - 1000
        assertTrue("expected win nudges ~K/2", delta in 1..20)
    }
}
