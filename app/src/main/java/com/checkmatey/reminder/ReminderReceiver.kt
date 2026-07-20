package com.checkmatey.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Wakes for two things: the daily alarm (post the reminder) and BOOT_COMPLETED (alarms are cleared
 * on reboot, so re-arm the schedule). Both paths are guarded inside [DailyReminder].
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> DailyReminder.schedule(context)
            else -> DailyReminder.notifyNow(context) // the alarm fired
        }
    }
}
