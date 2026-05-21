package com.example.acurdate.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.acurdate.data.*
import com.example.acurdate.notifications.NotificationHelper
import com.example.acurdate.ui.HapticFeedbackHelper
import com.example.acurdate.ui.SoundSynthesizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.*

data class MainScreenState(
    val reminders: List<Reminder> = emptyList(),
    val filteredReminders: List<Reminder> = emptyList(),
    val completedReminders: List<CompletedReminder> = emptyList(),
    val selectedDateReminders: List<Reminder> = emptyList(),
    val selectedDate: Calendar = Calendar.getInstance(),
    val streak: Int = 0,
    val planetState: PlanetState = PlanetState.INERTE,
    val activeFilter: ReminderCategory? = null
)

class MainScreenViewModel(
    private val repository: DataRepository,
    private val soundSynthesizer: SoundSynthesizer,
    private val hapticHelper: HapticFeedbackHelper,
    private val notificationHelper: NotificationHelper
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(Calendar.getInstance())
    private val _activeFilter = MutableStateFlow<ReminderCategory?>(null)

    private val _state = MutableStateFlow(MainScreenState())
    val state: StateFlow<MainScreenState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.reminders,
                repository.completedReminders,
                _selectedDate,
                _activeFilter
            ) { reminders, completed, selDate, filter ->
                val streak = calculateStreak(completed)
                
                // Calculate planet state
                val now = System.currentTimeMillis()
                val hasOverdue = reminders.any { it.dueDate != null && it.dueDate < now }
                
                val planetState = when {
                    hasOverdue -> PlanetState.PELIGRO
                    streak > 3 -> PlanetState.RADIANTE
                    streak in 1..3 -> PlanetState.ESTABLE
                    else -> PlanetState.INERTE
                }
                
                // Filter reminders
                val filtered = if (filter == null) {
                    reminders
                } else {
                    reminders.filter { it.category == filter }
                }
                
                // Bookings on selected day
                val dayReminders = reminders.filter { reminder ->
                    reminder.dueDate?.let { due ->
                        val dueCal = Calendar.getInstance().apply { timeInMillis = due }
                        dueCal.get(Calendar.YEAR) == selDate.get(Calendar.YEAR) &&
                                dueCal.get(Calendar.DAY_OF_YEAR) == selDate.get(Calendar.DAY_OF_YEAR)
                    } ?: false
                }
                
                MainScreenState(
                    reminders = reminders,
                    filteredReminders = filtered,
                    completedReminders = completed.sortedByDescending { it.completedAt },
                    selectedDateReminders = dayReminders,
                    selectedDate = selDate,
                    streak = streak,
                    planetState = planetState,
                    activeFilter = filter
                )
            }.collect {
                _state.value = it
            }
        }
    }

    fun createReminder(
        text: String,
        category: ReminderCategory,
        priority: ReminderPriority,
        dueDate: Long?,
        recurrence: ReminderRecurrence
    ) {
        viewModelScope.launch {
            val reminder = Reminder(
                id = UUID.randomUUID().toString(),
                text = text,
                category = category,
                priority = priority,
                createdAt = System.currentTimeMillis(),
                dueDate = dueDate,
                recurrence = recurrence
            )
            repository.saveReminder(reminder)

            if (dueDate != null) {
                notificationHelper.scheduleAlarm(
                    reminderId = reminder.id,
                    text = text,
                    category = category.name.lowercase(),
                    dueDate = dueDate
                )
            }

            hapticHelper.vibrateTick()
            soundSynthesizer.playSelectedTone()
        }
    }

    fun completeReminder(reminderId: String) {
        viewModelScope.launch {
            repository.completeReminder(reminderId, System.currentTimeMillis())
            notificationHelper.cancelAlarm(reminderId)

            hapticHelper.vibrateSuccess()
            soundSynthesizer.playCelestialChord()
        }
    }

    fun deleteReminder(reminderId: String) {
        viewModelScope.launch {
            repository.deleteReminder(reminderId)
            notificationHelper.cancelAlarm(reminderId)

            hapticHelper.vibrateDelete()
        }
    }

    fun setSelectedDate(calendar: Calendar) {
        _selectedDate.value = calendar
    }

    fun setFilter(category: ReminderCategory?) {
        _activeFilter.value = category
    }

    fun checkAndTriggerSOSAlarms() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val overdueReminder = _state.value.reminders.firstOrNull { 
                it.dueDate != null && it.dueDate < now && !it.alerted 
            }
            if (overdueReminder != null) {
                // Sound and Haptic SOS coordinated
                hapticHelper.vibrateSOS()
                soundSynthesizer.playSOSAlarm()
                
                // Mark as alerted to prevent looping alarms
                repository.saveReminder(overdueReminder.copy(alerted = true))
            }
        }
    }

    private fun calculateStreak(completed: List<CompletedReminder>): Int {
        if (completed.isEmpty()) return 0
        val today = Calendar.getInstance()
        
        val completedDates = completed.map {
            val cal = Calendar.getInstance().apply { timeInMillis = it.completedAt }
            String.format("%d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
        }.toSet()
        
        var streak = 0
        val checkCal = today.clone() as Calendar
        
        val todayStr = String.format("%d-%02d-%02d", checkCal.get(Calendar.YEAR), checkCal.get(Calendar.MONTH) + 1, checkCal.get(Calendar.DAY_OF_MONTH))
        val completedToday = completedDates.contains(todayStr)
        
        if (!completedToday) {
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayStr = String.format("%d-%02d-%02d", checkCal.get(Calendar.YEAR), checkCal.get(Calendar.MONTH) + 1, checkCal.get(Calendar.DAY_OF_MONTH))
            if (!completedDates.contains(yesterdayStr)) {
                return 0
            }
        }
        
        while (true) {
            val dateStr = String.format("%d-%02d-%02d", checkCal.get(Calendar.YEAR), checkCal.get(Calendar.MONTH) + 1, checkCal.get(Calendar.DAY_OF_MONTH))
            if (completedDates.contains(dateStr)) {
                streak++
                checkCal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        
        return streak
    }
}
