package com.example.acurdate.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File

interface DataRepository {
    val reminders: Flow<List<Reminder>>
    val completedReminders: Flow<List<CompletedReminder>>
    
    suspend fun saveReminder(reminder: Reminder)
    suspend fun updateReminder(reminder: Reminder)
    suspend fun deleteReminder(id: String)
    suspend fun completeReminder(reminderId: String, completedAt: Long)
}

class JSONDataRepository(private val context: Context) : DataRepository {
    private val remindersFile = File(context.filesDir, "reminders.json")
    private val completedFile = File(context.filesDir, "completed_reminders.json")
    
    private val _reminders = MutableStateFlow<List<Reminder>>(emptyList())
    override val reminders: Flow<List<Reminder>> = _reminders.asStateFlow()
    
    private val _completedReminders = MutableStateFlow<List<CompletedReminder>>(emptyList())
    override val completedReminders: Flow<List<CompletedReminder>> = _completedReminders.asStateFlow()
    
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    
    init {
        loadData()
    }
    
    private fun loadData() {
        try {
            if (remindersFile.exists()) {
                val text = remindersFile.readText()
                _reminders.value = json.decodeFromString<List<Reminder>>(text)
            } else {
                _reminders.value = emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _reminders.value = emptyList()
        }
        
        try {
            if (completedFile.exists()) {
                val text = completedFile.readText()
                _completedReminders.value = json.decodeFromString<List<CompletedReminder>>(text)
            } else {
                _completedReminders.value = emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _completedReminders.value = emptyList()
        }
    }
    
    private suspend fun saveRemindersToFile(list: List<Reminder>) = withContext(Dispatchers.IO) {
        try {
            val text = json.encodeToString(list)
            remindersFile.writeText(text)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private suspend fun saveCompletedToFile(list: List<CompletedReminder>) = withContext(Dispatchers.IO) {
        try {
            val text = json.encodeToString(list)
            completedFile.writeText(text)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    override suspend fun saveReminder(reminder: Reminder) {
        val current = _reminders.value.toMutableList()
        val index = current.indexOfFirst { it.id == reminder.id }
        if (index >= 0) {
            current[index] = reminder
        } else {
            current.add(reminder)
        }
        _reminders.value = current
        saveRemindersToFile(current)
    }
    
    override suspend fun updateReminder(reminder: Reminder) {
        saveReminder(reminder)
    }
    
    override suspend fun deleteReminder(id: String) {
        val current = _reminders.value.filterNot { it.id == id }
        _reminders.value = current
        saveRemindersToFile(current)
    }
    
    override suspend fun completeReminder(reminderId: String, completedAt: Long) {
        val currentReminders = _reminders.value.toMutableList()
        val index = currentReminders.indexOfFirst { it.id == reminderId }
        if (index >= 0) {
            val reminder = currentReminders[index]
            
            // Log completion in history
            val completed = CompletedReminder(
                id = java.util.UUID.randomUUID().toString(),
                reminderId = reminder.id,
                text = reminder.text,
                category = reminder.category,
                completedAt = completedAt
            )
            val currentCompleted = _completedReminders.value.toMutableList()
            currentCompleted.add(completed)
            _completedReminders.value = currentCompleted
            saveCompletedToFile(currentCompleted)
            
            // Check recurrence
            if (reminder.recurrence == ReminderRecurrence.NONE) {
                currentReminders.removeAt(index)
            } else {
                // Reschedule for next day or week
                val nextDueDate = reminder.dueDate?.let { due ->
                    val offset = if (reminder.recurrence == ReminderRecurrence.DAILY) 24 * 60 * 60 * 1000L else 7 * 24 * 60 * 60 * 1000L
                    due + offset
                }
                currentReminders[index] = reminder.copy(
                    createdAt = completedAt, // reset ticker
                    dueDate = nextDueDate,
                    alerted = false
                )
            }
            _reminders.value = currentReminders
            saveRemindersToFile(currentReminders)
        }
    }
}
