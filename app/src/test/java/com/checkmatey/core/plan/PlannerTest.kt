package com.checkmatey.core.plan

import org.junit.Assert.assertEquals
import org.junit.Test

class PlannerTest {

    private fun progress(
        lessonsDone: Int = 12, puzzlesSolved: Int = 10, gamesPlayed: Int = 5,
        weak: Boolean = false, unreviewed: Boolean = false, dueReviews: Int = 0,
    ) = Progress(lessonsDone, 12, puzzlesSolved, gamesPlayed, weak, unreviewed, dueReviews)

    @Test
    fun newUserIsSentToLessonsFirst() {
        assertEquals(StepTarget.LESSON, Planner.next(progress(lessonsDone = 0)).target)
    }

    @Test
    fun dueReviewsTakePriorityOverEverythingElse() {
        // Spaced reviews are time-sensitive: clear them before new lessons or an unreviewed game.
        val step = Planner.next(progress(lessonsDone = 0, unreviewed = true, dueReviews = 3))
        assertEquals(StepTarget.PUZZLE, step.target)
        assertEquals("복습할 시간 (3)", step.title)
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
