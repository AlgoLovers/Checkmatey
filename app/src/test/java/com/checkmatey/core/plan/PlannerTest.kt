package com.checkmatey.core.plan

import org.junit.Assert.assertEquals
import org.junit.Test

class PlannerTest {

    private fun progress(
        lessonsDone: Int = 12, puzzlesSolved: Int = 10, gamesPlayed: Int = 5,
        weak: Boolean = false, unreviewed: Boolean = false,
    ) = Progress(lessonsDone, 12, puzzlesSolved, gamesPlayed, weak, unreviewed)

    @Test
    fun newUserIsSentToLessonsFirst() {
        assertEquals(StepTarget.LESSON, Planner.next(progress(lessonsDone = 0)).target)
    }

    @Test
    fun anUnreviewedGameTakesPriorityOverDrilling() {
        assertEquals(StepTarget.REVIEW, Planner.next(progress(unreviewed = true, weak = true)).target)
    }

    @Test
    fun weakThemeLeadsToTargetedPuzzles() {
        assertEquals(StepTarget.PUZZLE, Planner.next(progress(weak = true)).target)
    }

    @Test
    fun aBeginnerWhoFinishedLessonsPlaysAfterSomePuzzles() {
        assertEquals(StepTarget.PUZZLE, Planner.next(progress(puzzlesSolved = 0)).target)
        assertEquals(StepTarget.PLAY, Planner.next(progress(gamesPlayed = 0)).target)
    }

    @Test
    fun aSteadyUserGetsTheDailyRoutine() {
        assertEquals("오늘의 훈련", Planner.next(progress()).title)
    }
}
