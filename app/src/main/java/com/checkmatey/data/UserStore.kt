package com.checkmatey.data

import android.content.Context
import com.checkmatey.core.puzzle.Rating

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

    private companion object {
        const val KEY_RATING = "puzzleRating"
        const val KEY_SOLVED = "solvedCount"
        const val KEY_STREAK = "streak"
        const val KEY_BEST_STREAK = "bestStreak"
    }
}
