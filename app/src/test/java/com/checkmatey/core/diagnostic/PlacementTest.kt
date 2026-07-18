package com.checkmatey.core.diagnostic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the adaptive placement math: the estimate must converge in the right direction, stay inside
 * the beginner band, and finish after a fixed number of questions. Deterministic pure logic, so a
 * regression in "start me at the right level" is caught before it ships.
 */
class PlacementTest {

    /** Simulate a whole quiz where every question is served near the estimate and graded [solved]. */
    private fun runQuiz(allCorrect: Boolean): Int {
        var estimate = Placement.START
        var answered = 0
        while (!Placement.isDone(answered)) {
            val q = Placement.target(estimate) // question served at the current estimate
            estimate = Placement.update(estimate, q, allCorrect, answered)
            answered++
        }
        return estimate
    }

    @Test
    fun quizHasFixedLength() {
        assertFalse(Placement.isDone(Placement.LENGTH - 1))
        assertTrue(Placement.isDone(Placement.LENGTH))
    }

    @Test
    fun solvingEverythingRaisesTheEstimateAboveStart() {
        assertTrue(runQuiz(allCorrect = true) > Placement.START)
    }

    @Test
    fun missingEverythingLowersTheEstimateBelowStart() {
        assertTrue(runQuiz(allCorrect = false) < Placement.START)
    }

    @Test
    fun estimateStaysInsideTheBeginnerBand() {
        // Hammer it in both directions far past the quiz length; it must never escape the band.
        var high = Placement.START
        var low = Placement.START
        repeat(40) {
            high = Placement.update(high, high, true, 0)
            low = Placement.update(low, low, false, 0)
        }
        assertTrue(high <= Placement.MAX)
        assertTrue(low >= Placement.MIN)
    }

    @Test
    fun earlyAnswersMoveMoreThanLateOnes() {
        // Same question, same outcome: the swing at answer 0 must exceed the swing at the last one.
        val base = 800
        val q = 800
        val earlySwing = Placement.update(base, q, true, 0) - base
        val lateSwing = Placement.update(base, q, true, Placement.LENGTH - 1) - base
        assertTrue("early swing $earlySwing should exceed late swing $lateSwing", earlySwing > lateSwing)
    }

    @Test
    fun aHardQuestionSolvedIsWorthMoreThanAnEasyOne() {
        // Solving a puzzle above your estimate should raise it more than solving one below it.
        val hard = Placement.update(800, 1100, true, 0)
        val easy = Placement.update(800, 500, true, 0)
        assertTrue("solving a hard puzzle should reward more", hard > easy)
    }
}
