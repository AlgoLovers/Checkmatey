package com.checkmatey.core.skilltree

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the learning-map logic: prerequisites gate access, demonstrated skills read DONE, and a
 * finished user lights up the whole tree. Pure data + predicates, so the map can't drift from what
 * the user has actually done.
 */
class SkillTreeTest {

    private val empty = SkillProgress(emptySet(), 0, 0, emptySet())
    private val allLessons = setOf(
        "coords", "pawn", "knight", "brq", "check_escape", "castle", "enpassant",
        "backrank", "kr_mate", "kq_mate", "opening", "pin_skewer",
    )

    @Test
    fun aFreshUserSeesOnlyTheRootUnlocked() {
        val s = SkillTree.statuses(empty)
        assertEquals(NodeStatus.AVAILABLE, s["basics"])
        assertEquals(NodeStatus.LOCKED, s["special"])
        assertEquals(NodeStatus.LOCKED, s["tactics"])
        assertEquals(NodeStatus.LOCKED, s["mates"])
        assertEquals(NodeStatus.LOCKED, s["play"])
        assertEquals(0, SkillTree.progressPercent(empty))
    }

    @Test
    fun finishingBasicsUnlocksItsDependents() {
        val p = empty.copy(completedLessons = setOf("coords", "pawn", "knight", "brq"))
        val s = SkillTree.statuses(p)
        assertEquals(NodeStatus.DONE, s["basics"])
        assertEquals(NodeStatus.AVAILABLE, s["special"]) // prereq basics done
        assertEquals(NodeStatus.AVAILABLE, s["tactics"]) // prereq basics done, condition not yet
        assertEquals(NodeStatus.LOCKED, s["mates"]) // needs special
    }

    @Test
    fun tacticsNeedsBothLessonAndPuzzleVolume() {
        val base = empty.copy(completedLessons = setOf("coords", "pawn", "knight", "brq", "pin_skewer"))
        assertEquals(NodeStatus.AVAILABLE, SkillTree.statuses(base)["tactics"]) // lesson but < 20 puzzles
        val solved = base.copy(puzzlesSolved = 20)
        assertEquals(NodeStatus.DONE, SkillTree.statuses(solved)["tactics"])
    }

    @Test
    fun playRequiresBothTacticsAndOpeningBranches() {
        // Enough to finish tactics + openings but only 2 games -> play still just AVAILABLE.
        val p = SkillProgress(
            completedLessons = allLessons,
            puzzlesSolved = 25,
            gamesPlayed = 2,
            completedDrills = emptySet(),
        )
        assertEquals(NodeStatus.DONE, SkillTree.statuses(p)["tactics"])
        assertEquals(NodeStatus.DONE, SkillTree.statuses(p)["openings"])
        assertEquals(NodeStatus.AVAILABLE, SkillTree.statuses(p)["play"])
        assertEquals(NodeStatus.DONE, SkillTree.statuses(p.copy(gamesPlayed = 3))["play"])
    }

    @Test
    fun aFullyProgressedUserLightsUpEverything() {
        val p = SkillProgress(allLessons, 30, 5, setOf("kq", "kr"))
        val s = SkillTree.statuses(p)
        assertTrue("every node should be DONE: $s", s.values.all { it == NodeStatus.DONE })
        assertEquals(100, SkillTree.progressPercent(p))
    }
}
