package com.example.acurdate.data

import kotlinx.serialization.Serializable

@Serializable
enum class ReminderCategory(val displayName: String) {
    IMPORTANTE("Importante"),
    HABITO("Hábito"),
    TAREA("Tarea"),
    IDEA("Idea"),
    NOTA("Nota")
}

@Serializable
enum class ReminderPriority(val displayName: String) {
    ALTA("Alta"),
    MEDIA("Media"),
    BAJA("Baja")
}

@Serializable
enum class ReminderRecurrence(val displayName: String) {
    NONE("Única vez"),
    DAILY("Diario 🔁"),
    WEEKLY("Semanal 🔁")
}

@Serializable
data class Reminder(
    val id: String,
    val text: String,
    val category: ReminderCategory,
    val priority: ReminderPriority,
    val createdAt: Long,
    val dueDate: Long? = null,
    val recurrence: ReminderRecurrence = ReminderRecurrence.NONE,
    val alerted: Boolean = false
)

@Serializable
data class CompletedReminder(
    val id: String,
    val reminderId: String,
    val text: String,
    val category: ReminderCategory,
    val completedAt: Long
)
