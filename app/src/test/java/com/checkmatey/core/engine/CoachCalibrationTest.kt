package com.checkmatey.core.engine

import com.checkmatey.core.chess.Position
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Calibrates the coach's grading bands against ground truth from real human play (a fixed,
 * committed sample of the Lichess puzzle DB):
 *
 * - **Blunder recall** — each puzzle's *setup move* is the move a real player made that allowed a
 *   winning tactic. A well-calibrated coach should grade most of these INACCURACY or worse.
 *   (Not 100%: some positions were already lost before the setup move.)
 * - **Good-move specificity** — each puzzle's *solution move* is an independently verified best
 *   move. The coach should grade nearly all of these BEST/GOOD; flagging them would mean the
 *   bands are too harsh.
 *
 * The printed distributions are the calibration signal; the assertions are regression floors so
 * future engine/band changes can't silently degrade either side.
 */
class CoachCalibrationTest {

    private data class Row(val fenBefore: String, val setupUci: String, val fenAfter: String, val solutionUci: String)

    @Test
    fun gradingMatchesRealBlundersAndVerifiedBestMoves() {
        // Calibration bench (~3.8s): kept out of the fast gate. Run with `RUN_BENCH=true ./gradlew test…`.
        assumeTrue("calibration bench — set RUN_BENCH=true to run", System.getenv("RUN_BENCH") == "true")
        val rows = load()
        val annotator = Annotator(KotlinMinimaxEngine(), depth = 4)

        val setupGrades = IntArray(MoveQuality.entries.size)
        val solutionGrades = IntArray(MoveQuality.entries.size)
        var setupN = 0
        var solutionN = 0

        for (row in rows) {
            // 1) The human move that allowed the tactic.
            val before = Position.fromFen(row.fenBefore)
            before.findMove(row.setupUci)?.let { move ->
                setupGrades[annotator.annotate(before, move).quality.ordinal]++
                setupN++
            }
            // 2) The verified refutation.
            val after = Position.fromFen(row.fenAfter)
            after.findMove(row.solutionUci)?.let { move ->
                solutionGrades[annotator.annotate(after, move).quality.ordinal]++
                solutionN++
            }
        }
        assertTrue("sample must load", setupN > 80 && solutionN > 80)

        fun pct(counts: IntArray, n: Int, vararg q: MoveQuality) =
            q.sumOf { counts[it.ordinal] } * 100 / n

        val recall = pct(setupGrades, setupN, MoveQuality.INACCURACY, MoveQuality.MISTAKE, MoveQuality.BLUNDER)
        val hardRecall = pct(setupGrades, setupN, MoveQuality.MISTAKE, MoveQuality.BLUNDER)
        val specificity = pct(solutionGrades, solutionN, MoveQuality.BEST, MoveQuality.GOOD)

        println("=== Coach calibration (n=$setupN setup moves / $solutionN solutions, depth 4) ===")
        println("  실제 실수(셋업 수) 채점 분포:")
        for (q in MoveQuality.entries) println("    ${q.label}: ${setupGrades[q.ordinal]}  (${setupGrades[q.ordinal] * 100 / setupN}%)")
        println("  -> 재현율(부정확 이하로 잡음): $recall%   (실수/블런더로: $hardRecall%)")
        println("  검증된 정답 수 채점 분포:")
        for (q in MoveQuality.entries) println("    ${q.label}: ${solutionGrades[q.ordinal]}  (${solutionGrades[q.ordinal] * 100 / solutionN}%)")
        println("  -> 특이도(BEST/GOOD 인정): $specificity%")

        // Regression floors — set ~10pp under first measurement; tighten as calibration improves.
        assertTrue("blunder recall dropped: $recall%", recall >= RECALL_FLOOR)
        assertTrue("good-move specificity dropped: $specificity%", specificity >= SPECIFICITY_FLOOR)
    }

    private fun load(): List<Row> {
        val stream = javaClass.getResourceAsStream("/calibration_sample.csv")
        assertNotNull("calibration_sample.csv missing from test resources", stream)
        return stream!!.bufferedReader().useLines { lines ->
            lines.drop(1).mapNotNull { line ->
                val c = line.split(",")
                if (c.size < 5) null else Row(c[0], c[1], c[2], c[3])
            }.toList()
        }
    }

    private companion object {
        const val RECALL_FLOOR = 70
        const val SPECIFICITY_FLOOR = 90
    }
}
