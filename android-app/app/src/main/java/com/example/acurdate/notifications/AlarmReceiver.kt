package com.example.acurdate.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.acurdate.MainActivity

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(NotificationHelper.EXTRA_REMINDER_ID) ?: return
        val text = intent.getStringExtra(NotificationHelper.EXTRA_REMINDER_TEXT) ?: return
        val category = intent.getStringExtra(NotificationHelper.EXTRA_REMINDER_CATEGORY) ?: ""

        val tapPendingIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(categoryTitle(category))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(tapPendingIntent)
            .build()

        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(reminderId.hashCode(), notification)
    }

    private fun categoryTitle(category: String): String = when (category.lowercase()) {
        "importante" -> "⚠️ Recordatorio Importante"
        "habito"     -> "🔁 Hábito Programado"
        "tarea"      -> "✅ Tarea Pendiente"
        "idea"       -> "💡 Idea Anotada"
        "nota"       -> "📝 Nota Programada"
        else         -> "⏰ Recordatorio"
    }
}
