package com.checkmatey.data

import android.content.Context
import com.checkmatey.core.puzzle.Rating
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

    /** Ids of previously-missed puzzles, queued for spaced-repetition review. */
    var reviewIds: List<String>
        get() = prefs.getString(KEY_REVIEW, "").orEmpty().split(",").filter { it.isNotBlank() }
        set(value) = prefs.edit().putString(KEY_REVIEW, value.distinct().joinToString(",")).apply()

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

    private companion object {
        const val KEY_RATING = "puzzleRating"
        const val KEY_SOLVED = "solvedCount"
        const val KEY_STREAK = "streak"
        const val KEY_BEST_STREAK = "bestStreak"
        const val KEY_REVIEW = "reviewIds"
        const val KEY_THEMES = "themeStats"
    }
}
