package com.example.acurdate.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.acurdate.data.JSONDataRepository
import kotlinx.coroutines.*

// Reschedules alarms after the device reboots (AlarmManager is cleared on reboot)
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val helper = NotificationHelper(context)
        val repository = JSONDataRepository(context)
        val now = System.currentTimeMillis()

        CoroutineScope(Dispatchers.IO).launch {
            repository.reminders.collect { reminders ->
                reminders
                    .filter { it.dueDate != null && it.dueDate > now }
                    .forEach { reminder ->
                        helper.scheduleAlarm(
                            reminderId = reminder.id,
                            text = reminder.text,
                            category = reminder.category.name.lowercase(),
                            dueDate = reminder.dueDate!!
                        )
                    }
                cancel()
            }
        }
    }
}
