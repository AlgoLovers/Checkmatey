package com.checkmatey.reminder

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.checkmatey.MainActivity
import com.checkmatey.R
import com.checkmatey.core.daily.ReminderTime
import com.checkmatey.core.habit.DailyGoal
import com.checkmatey.core.habit.DailyState
import com.checkmatey.data.UserStore
import java.util.Calendar

/**
 * The daily practice reminder — the "come back and keep your streak" trigger a free app lives on.
 * A once-a-day inexact alarm (battery-friendly, needs no exact-alarm permission) wakes
 * [ReminderReceiver], which posts a streak-aware notification. Everything is guarded so a missing
 * permission or a locked-down OEM can never crash the app; the scheduling *maths* is the pure,
 * tested [ReminderTime].
 */
object DailyReminder {
    private const val CHANNEL_ID = "daily_practice"
    private const val NOTIF_ID = 4200
    private const val REQ = 4201

    /** (Re)schedule the daily reminder if it's enabled; a no-op (after cancelling) if it's off. */
    fun schedule(context: Context) {
        val app = context.applicationContext
        val store = UserStore(app)
        val am = app.getSystemService(AlarmManager::class.java) ?: return
        am.cancel(alarmIntent(app)) // never stack duplicates
        if (!store.reminderOn) return
        val delay = ReminderTime.millisUntilHour(millisSinceMidnight(), store.reminderHour)
        runCatching {
            am.setInexactRepeating(
                AlarmManager.RTC,
                System.currentTimeMillis() + delay,
                AlarmManager.INTERVAL_DAY,
                alarmIntent(app),
            )
        }
    }

    fun cancel(context: Context) {
        val app = context.applicationContext
        app.getSystemService(AlarmManager::class.java)?.cancel(alarmIntent(app))
    }

    /** Post the reminder now — called by the receiver when the alarm fires. */
    @SuppressLint("MissingPermission") // guarded by hasPermission() above + runCatching on notify()
    fun notifyNow(context: Context) {
        val app = context.applicationContext
        val store = UserStore(app)
        if (!store.reminderOn || !hasPermission(app)) return
        val today = System.currentTimeMillis() / ReminderTime.MS_PER_DAY
        val daily = store.dailyState(today)
        if (DailyGoal.metToday(daily)) return // already practised enough today — don't nag
        ensureChannel(app)
        val (title, text) = copyFor(daily)
        val open = PendingIntent.getActivity(
            app,
            REQ,
            Intent(app, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(EXTRA_OPEN_DAILY, true),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(app, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(open)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        runCatching { NotificationManagerCompat.from(app).notify(NOTIF_ID, notification) }
    }

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    const val EXTRA_OPEN_DAILY = "openDaily"

    private fun copyFor(daily: DailyState): Pair<String, String> =
        if (daily.dayStreak > 0) {
            "🔥 ${daily.dayStreak}일 연속 학습 중!" to "오늘 퍼즐 한 개면 기록이 이어져요 — 5분이면 충분해요."
        } else {
            "오늘의 체스 한 걸음 ♟️" to "오늘의 퍼즐이 기다리고 있어요. 지금 시작해 연속 기록을 만들어요!"
        }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return
        if (mgr.getNotificationChannel(CHANNEL_ID) != null) return
        mgr.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "일일 학습 알림", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "매일 정해진 시간에 오늘의 퍼즐/연속 기록을 알려줍니다."
            },
        )
    }

    private fun alarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQ,
        Intent(context, ReminderReceiver::class.java).setAction(ACTION_FIRE),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun millisSinceMidnight(): Long {
        val c = Calendar.getInstance()
        return c.get(Calendar.HOUR_OF_DAY) * 3_600_000L +
            c.get(Calendar.MINUTE) * 60_000L +
            c.get(Calendar.SECOND) * 1_000L
    }

    const val ACTION_FIRE = "com.checkmatey.reminder.FIRE"
}
