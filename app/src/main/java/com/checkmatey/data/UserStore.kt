package com.checkmatey.data

import android.content.Context
import com.checkmatey.core.habit.DailyGoal
import com.checkmatey.core.habit.DailyState
import com.checkmatey.core.puzzle.Rating
import com.checkmatey.core.srs.Grade
import com.checkmatey.core.srs.Srs
import com.checkmatey.core.srs.SrsCard
import org.json.JSONObject

/**
 * Persistent per-user progress (puzzle rating, streaks, solved count) via SharedPreferences.
 * Small and synchronous — fine for these few values; can move to DataStore/Room as it grows.
 */
class UserStore(context: Context) {
    private val prefs = context.getSharedPreferences("checkmatey", Context.MODE_PRIVATE)

    var puzzleRating: Int
        get() = prefs.getInt(KEY_RATING, Rating.DEFAULT)
        set(value) = prefs.edit().putInt(KEY_RATING, value).apply()

    var solvedCount: Int
        get() = prefs.getInt(KEY_SOLVED, 0)
        set(value) = prefs.edit().putInt(KEY_SOLVED, value).apply()

    var streak: Int
        get() = prefs.getInt(KEY_STREAK, 0)
        set(value) = prefs.edit().putInt(KEY_STREAK, value).apply()

    var bestStreak: Int
        get() = prefs.getInt(KEY_BEST_STREAK, 0)
        set(value) = prefs.edit().putInt(KEY_BEST_STREAK, value).apply()

    var soundOn: Boolean
        get() = prefs.getBoolean(KEY_SOUND, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND, value).apply()

    var onboardingSeen: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDED, value).apply()

    /** Whether the user has taken (or dismissed) the diagnostic placement quiz. */
    var placementDone: Boolean
        get() = prefs.getBoolean(KEY_PLACED, false)
        set(value) = prefs.edit().putBoolean(KEY_PLACED, value).apply()

    /**
     * Recent finished games, newest first, as "result|uci,uci,...". Kept small (10) — this is a
     * review aid, not an archive.
     */
    var recentGames: List<String>
        get() = prefs.getString(KEY_GAMES, "").orEmpty().split(";").filter { it.isNotBlank() }
        set(value) = prefs.edit().putString(KEY_GAMES, value.take(10).joinToString(";")).apply()

    fun saveGame(result: String, uciMoves: List<String>) {
        if (uciMoves.isEmpty()) return
        recentGames = listOf("$result|${uciMoves.joinToString(",")}") + recentGames
        reviewedLatestGame = false
    }

    /** Whether the most recent game has been opened in review (drives the home nudge). */
    var reviewedLatestGame: Boolean
        get() = prefs.getBoolean(KEY_REVIEWED, false)
        set(value) = prefs.edit().putBoolean(KEY_REVIEWED, value).apply()

    /** Ids of lessons the user has finished. */
    var completedLessons: Set<String>
        get() = prefs.getString(KEY_LESSONS, "").orEmpty().split(",").filter { it.isNotBlank() }.toSet()
        set(value) = prefs.edit().putString(KEY_LESSONS, value.joinToString(",")).apply()

    /** Ids of endgame drills the user has beaten (delivered mate). */
    var completedDrills: Set<String>
        get() = prefs.getString(KEY_DRILLS, "").orEmpty().split(",").filter { it.isNotBlank() }.toSet()
        set(value) = prefs.edit().putString(KEY_DRILLS, value.joinToString(",")).apply()

    /** Puzzle themes recommended by the last game review (weak spots to drill). */
    var recommendedThemes: List<String>
        get() = prefs.getString(KEY_REC_THEMES, "").orEmpty().split(",").filter { it.isNotBlank() }
        set(value) = prefs.edit().putString(KEY_REC_THEMES, value.joinToString(",")).apply()

    /**
     * Spaced-repetition deck: puzzles the user has missed, each carrying its SM-2 schedule (see
     * [Srs]). Stored compactly as `;`-joined encoded cards. Bounded to the most recent 500 so the
     * prefs blob stays small — a beginner's weak-spot deck is far smaller than that in practice.
     */
    var srsCards: List<SrsCard>
        get() = prefs.getString(KEY_SRS, "").orEmpty().split(";").filter { it.isNotBlank() }.mapNotNull { SrsCard.decode(it) }
        set(value) = prefs.edit().putString(KEY_SRS, value.takeLast(500).joinToString(";") { it.encode() }).apply()

    /** How many review cards are due on [today] (epoch day) — drives the home nudge and the deck badge. */
    fun dueReviewCount(today: Long): Int = Srs.dueCount(srsCards, today)

    /**
     * Record a review of puzzle [id] on [today], rescheduling (or creating) its SM-2 card. Missed
     * puzzles enter the deck; solved ones already in the deck graduate to a longer interval.
     */
    fun scheduleReview(id: String, grade: Grade, today: Long) {
        val cards = srsCards
        val base = cards.firstOrNull { it.id == id } ?: Srs.new(id, today)
        srsCards = cards.filter { it.id != id } + Srs.review(base, grade, today)
    }

    /** Record a solved/missed attempt for a puzzle [theme] (for weakness tracking). */
    fun recordTheme(theme: String, solved: Boolean) {
        val obj = JSONObject(prefs.getString(KEY_THEMES, "{}") ?: "{}")
        val stats = obj.optJSONObject(theme) ?: JSONObject()
        stats.put("att", stats.optInt("att") + 1)
        if (solved) stats.put("ok", stats.optInt("ok") + 1)
        obj.put(theme, stats)
        prefs.edit().putString(KEY_THEMES, obj.toString()).apply()
    }

    /** theme -> (attempts, solved). */
    fun themeStats(): Map<String, Pair<Int, Int>> {
        val obj = JSONObject(prefs.getString(KEY_THEMES, "{}") ?: "{}")
        val out = LinkedHashMap<String, Pair<Int, Int>>()
        for (key in obj.keys()) {
            val s = obj.getJSONObject(key)
            out[key] = s.optInt("att") to s.optInt("ok")
        }
        return out
    }

    /** The theme you're weakest at (lowest success rate), needing at least [minAttempts] tries. */
    fun weakestTheme(minAttempts: Int = 2): String? =
        themeStats().filterValues { it.first >= minAttempts }
            .minByOrNull { (_, v) -> v.second.toDouble() / v.first }?.key

    fun successRate(theme: String): Int {
        val (att, ok) = themeStats()[theme] ?: return 0
        return if (att == 0) 0 else (ok * 100 / att)
    }

    /** Last N puzzle ratings, oldest first — for the progress sparkline. */
    var ratingHistory: List<Int>
        get() = prefs.getString(KEY_RATING_HIST, "").orEmpty().split(",").mapNotNull { it.toIntOrNull() }
        set(value) = prefs.edit().putString(KEY_RATING_HIST, value.takeLast(40).joinToString(",")).apply()

    fun pushRating(rating: Int) {
        ratingHistory = ratingHistory + rating
    }

    // ---- Daily habit loop (see core/habit/DailyGoal) ----

    private fun rawDaily(): DailyState = DailyState(
        dayStreak = prefs.getInt(KEY_DAY_STREAK, 0),
        longestStreak = prefs.getInt(KEY_DAY_LONGEST, 0),
        solvedToday = prefs.getInt(KEY_DAY_SOLVED, 0),
        lastActiveDay = prefs.getLong(KEY_DAY_LAST, 0),
    )

    private fun writeDaily(s: DailyState) = prefs.edit()
        .putInt(KEY_DAY_STREAK, s.dayStreak)
        .putInt(KEY_DAY_LONGEST, s.longestStreak)
        .putInt(KEY_DAY_SOLVED, s.solvedToday)
        .putLong(KEY_DAY_LAST, s.lastActiveDay)
        .apply()

    /** The daily state as it should read on [today] (streak lapses / count resets on a new day). */
    fun dailyState(today: Long): DailyState = DailyGoal.viewOn(rawDaily(), today)

    // ---- Daily reminder + puzzle of the day (see core/daily, feature/reminder) ----

    /** Whether the daily practice reminder notification is enabled. */
    var reminderOn: Boolean
        get() = prefs.getBoolean(KEY_REMIND_ON, true)
        set(value) = prefs.edit().putBoolean(KEY_REMIND_ON, value).apply()

    /** Hour of day (0–23) the reminder fires. */
    var reminderHour: Int
        get() = prefs.getInt(KEY_REMIND_HOUR, 19)
        set(value) = prefs.edit().putInt(KEY_REMIND_HOUR, value.coerceIn(0, 23)).apply()

    /** Epoch day handed from Home to the Puzzles tab so it serves the puzzle of the day first (0 = none). */
    var pendingDailyDay: Long
        get() = prefs.getLong(KEY_DAILY_PENDING, 0)
        set(value) = prefs.edit().putLong(KEY_DAILY_PENDING, value).apply()

    /** Epoch day on which the puzzle of the day was last solved (drives the "done ✓" state). */
    var dailySolvedDay: Long
        get() = prefs.getLong(KEY_DAILY_SOLVED, 0)
        set(value) = prefs.edit().putLong(KEY_DAILY_SOLVED, value).apply()

    /** Record one solved puzzle for [today]'s goal & streak; returns the updated view. */
    fun recordSolvedToday(today: Long): DailyState =
        DailyGoal.onSolved(rawDaily(), today).also { writeDaily(it) }

    private companion object {
        const val KEY_RATING = "puzzleRating"
        const val KEY_SOLVED = "solvedCount"
        const val KEY_STREAK = "streak"
        const val KEY_BEST_STREAK = "bestStreak"
        const val KEY_SRS = "srsCards"
        const val KEY_THEMES = "themeStats"
        const val KEY_SOUND = "soundOn"
        const val KEY_ONBOARDED = "onboardingSeen"
        const val KEY_PLACED = "placementDone"
        const val KEY_LESSONS = "completedLessons"
        const val KEY_GAMES = "recentGames"
        const val KEY_DRILLS = "completedDrills"
        const val KEY_REC_THEMES = "recommendedThemes"
        const val KEY_RATING_HIST = "ratingHistory"
        const val KEY_REVIEWED = "reviewedLatestGame"
        const val KEY_DAY_STREAK = "dayStreak"
        const val KEY_DAY_LONGEST = "dayLongest"
        const val KEY_DAY_SOLVED = "daySolved"
        const val KEY_DAY_LAST = "dayLast"
        const val KEY_REMIND_ON = "reminderOn"
        const val KEY_REMIND_HOUR = "reminderHour"
        const val KEY_DAILY_PENDING = "pendingDailyDay"
        const val KEY_DAILY_SOLVED = "dailySolvedDay"
    }
}
