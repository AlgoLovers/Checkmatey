package com.checkmatey.core.srs

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * How the solver did on a review, mapped to an SM-2 quality score. Kept small on purpose: a
 * beginner tactics trainer wants a snappy loop, so the puzzle outcome drives the grade (missed =
 * [AGAIN], solved = [GOOD]) without a self-rating prompt. [EASY] exists for a future "that was easy"
 * button and for completeness of the scheduling math.
 */
enum class Grade(val quality: Int) {
    AGAIN(2), // failed — relearn from the start
    GOOD(4),  // solved at the expected effort
    EASY(5),  // solved instantly
}

/**
 * One spaced-repetition card: a puzzle the user has struggled with, scheduled to reappear. All the
 * fields the SM-2 algorithm needs, plus the puzzle [id] it points at. [dueDay] is an **epoch day**
 * (millis / 86_400_000) so the whole scheduler is deterministic and Android-free — the UI passes in
 * `today`, and this stays a pure, unit-tested Kotlin type.
 */
data class SrsCard(
    val id: String,
    val ease: Double,
    val intervalDays: Int,
    val reps: Int,
    val dueDay: Long,
) {
    /** Compact single-line form for SharedPreferences: `id:ease*1000:interval:reps:due`. */
    fun encode(): String = "$id:${(ease * 1000).roundToInt()}:$intervalDays:$reps:$dueDay"

    companion object {
        fun decode(s: String): SrsCard? {
            val c = s.split(":")
            if (c.size != 5) return null
            val ease = c[1].toIntOrNull() ?: return null
            val interval = c[2].toIntOrNull() ?: return null
            val reps = c[3].toIntOrNull() ?: return null
            val due = c[4].toLongOrNull() ?: return null
            if (c[0].isBlank()) return null
            return SrsCard(c[0], ease / 1000.0, interval, reps, due)
        }
    }
}

/**
 * Spaced-repetition scheduler — a faithful SM-2 (the SuperMemo 2 algorithm, the vanilla, proven
 * choice for flashcard-style review). Each successful review pushes the next due date further out
 * (1 day → 6 days → interval × ease); a miss resets the card so the pattern is drilled again soon.
 * This is the learning moat: the app remembers *which* tactics you keep missing and resurfaces them
 * exactly when you're about to forget.
 */
object Srs {
    const val MIN_EASE = 1.3
    private const val START_EASE = 2.5

    /** A brand-new card, due immediately (the first encounter counts as its first review). */
    fun new(id: String, today: Long): SrsCard =
        SrsCard(id = id, ease = START_EASE, intervalDays = 0, reps = 0, dueDay = today)

    /** SM-2 step: return the card rescheduled after grading this review on [today]. */
    fun review(card: SrsCard, grade: Grade, today: Long): SrsCard {
        val q = grade.quality
        // Ease adjusts every review; the classic SM-2 curve, floored so cards never collapse to 0.
        val ease = max(MIN_EASE, card.ease + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02)))
        return if (q < 3) {
            // Lapse: relearn from the start, back tomorrow.
            card.copy(ease = ease, intervalDays = 1, reps = 0, dueDay = today + 1)
        } else {
            val interval = when (card.reps) {
                0 -> 1
                1 -> 6
                else -> (card.intervalDays * ease).roundToInt().coerceAtLeast(1)
            }
            card.copy(ease = ease, intervalDays = interval, reps = card.reps + 1, dueDay = today + interval)
        }
    }

    /** Cards due for review on [today] (due date reached), most overdue first. */
    fun due(cards: List<SrsCard>, today: Long): List<SrsCard> =
        cards.filter { it.dueDay <= today }.sortedBy { it.dueDay }

    fun dueCount(cards: List<SrsCard>, today: Long): Int = cards.count { it.dueDay <= today }
}
