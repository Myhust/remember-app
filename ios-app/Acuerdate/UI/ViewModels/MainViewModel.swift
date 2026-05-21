import Foundation
import SwiftData
import Combine

class MainViewModel: ObservableObject {
    @Published var reminders: [Reminder] = []
    @Published var completedReminders: [CompletedReminder] = []
    
    @Published var filteredReminders: [Reminder] = []
    @Published var selectedDateReminders: [Reminder] = []
    
    @Published var selectedDate = Date()
    @Published var streak: Int = 0
    @Published var planetState: PlanetState = .inerte
    @Published var activeFilter: ReminderCategory? = nil
    
    private var modelContext: ModelContext?
    private var cancellables = Set<AnyCancellable>()
    private var sosTimer: Timer?
    
    init() {
        // Start checking for SOS alarms periodically
        startSOSAlarmTimer()
    }
    
    func setModelContext(_ context: ModelContext) {
        self.modelContext = context
        fetchData()
    }
    
    func fetchData() {
        guard let context = modelContext else { return }
        
        do {
            let remindersFetch = FetchDescriptor<Reminder>(sortBy: [SortDescriptor(\.createdAt, order: .reverse)])
            let completedFetch = FetchDescriptor<CompletedReminder>(sortBy: [SortDescriptor(\.completedAt, order: .reverse)])
            
            self.reminders = try context.fetch(remindersFetch)
            self.completedReminders = try context.fetch(completedFetch)
            
            updateState()
        } catch {
            print("Failed to fetch data: \(error)")
        }
    }
    
    func updateState() {
        self.streak = calculateStreak(completedReminders)
        
        // Calculate planet state
        let now = Date()
        let hasOverdue = reminders.any { reminder in
            if let due = reminder.dueDate {
                return due < now
            }
            return false
        }
        
        if hasOverdue {
            self.planetState = .peligro
        } else if streak > 3 {
            self.planetState = .radiante
        } else if streak >= 1 {
            self.planetState = .estable
        } else {
            self.planetState = .inerte
        }
        
        // Filter by category
        if let filter = activeFilter {
            self.filteredReminders = reminders.filter { $0.category == filter }
        } else {
            self.filteredReminders = reminders
        }
        
        // Filter bookings on selected day
        let calendar = Calendar.current
        self.selectedDateReminders = reminders.filter { reminder in
            if let due = reminder.dueDate {
                return calendar.isDate(due, inSameDayAs: selectedDate)
            }
            return false
        }
    }
    
    func createReminder(text: String,
                        category: ReminderCategory,
                        priority: ReminderPriority,
                        dueDate: Date?,
                        recurrence: ReminderRecurrence) {
        guard let context = modelContext else { return }
        
        let reminder = Reminder(
            text: text,
            category: category,
            priority: priority,
            dueDate: dueDate,
            recurrence: recurrence
        )
        
        context.insert(reminder)
        saveChanges()

        if let due = dueDate {
            NotificationService.shared.scheduleAlarm(
                reminderId: reminder.id,
                text: text,
                dueDate: due,
                category: category.rawValue
            )
        }

        HapticFeedbackHelper.shared.vibrateTick()
        SoundSynthesizer.shared.playSelectedTone()
    }
    
    func completeReminder(reminderId: String) {
        guard let context = modelContext else { return }
        
        if let idx = reminders.firstIndex(where: { $0.id == reminderId }) {
            let reminder = reminders[idx]
            
            let completed = CompletedReminder(
                reminderId: reminder.id,
                text: reminder.text,
                category: reminder.category,
                completedAt: Date()
            )
            
            context.insert(completed)
            context.delete(reminder)
            saveChanges()

            NotificationService.shared.cancelAlarm(reminderId: reminderId)

            HapticFeedbackHelper.shared.vibrateSuccess()
            SoundSynthesizer.shared.playCelestialChord()
        }
    }
    
    func deleteReminder(reminderId: String) {
        guard let context = modelContext else { return }
        
        if let idx = reminders.firstIndex(where: { $0.id == reminderId }) {
            let reminder = reminders[idx]
            context.delete(reminder)
            saveChanges()

            NotificationService.shared.cancelAlarm(reminderId: reminder.id)
            HapticFeedbackHelper.shared.vibrateDelete()
        }
    }
    
    func clearHistory() {
        guard let context = modelContext else { return }
        
        for completed in completedReminders {
            context.delete(completed)
        }
        saveChanges()
        
        HapticFeedbackHelper.shared.vibrateDelete()
    }
    
    func setSelectedDate(_ date: Date) {
        self.selectedDate = date
        updateState()
    }
    
    func setFilter(_ category: ReminderCategory?) {
        self.activeFilter = category
        updateState()
    }
    
    func checkAndTriggerSOSAlarms() {
        let now = Date()
        if let overdue = reminders.first(where: { reminder in
            if let due = reminder.dueDate {
                return due < now && !reminder.alerted
            }
            return false
        }) {
            // Sound and Haptic SOS coordinated
            HapticFeedbackHelper.shared.vibrateSOS()
            SoundSynthesizer.shared.playSOSAlarm()
            
            // Mark as alerted to prevent looping alarms
            overdue.alerted = true
            saveChanges()
        }
    }
    
    private func saveChanges() {
        guard let context = modelContext else { return }
        do {
            try context.save()
            fetchData()
        } catch {
            print("Failed to save changes: \(error)")
        }
    }
    
    private func startSOSAlarmTimer() {
        sosTimer = Timer.scheduledTimer(withTimeInterval: 5.0, repeats: true) { [weak self] _ in
            self?.checkAndTriggerSOSAlarms()
        }
    }
    
    deinit {
        sosTimer?.invalidate()
    }
    
    private func calculateStreak(_ completed: [CompletedReminder]) -> Int {
        if completed.isEmpty { return 0 }
        let calendar = Calendar.current
        let today = Date()
        
        let completedDates: Set<String> = Set(completed.map {
            let components = calendar.dateComponents([.year, .month, .day], from: $0.completedAt)
            return "\(components.year!)-\(components.month!)-\(components.day!)"
        })
        
        var streak = 0
        var checkDate = today
        
        let todayComponents = calendar.dateComponents([.year, .month, .day], from: checkDate)
        let todayKey = "\(todayComponents.year!)-\(todayComponents.month!)-\(todayComponents.day!)"
        
        let completedToday = completedDates.contains(todayKey)
        
        if !completedToday {
            // Check if yesterday was completed to preserve streak, else 0
            if let yesterday = calendar.date(byAdding: .day, value: -1, to: today) {
                checkDate = yesterday
                let yesterdayComponents = calendar.dateComponents([.year, .month, .day], from: checkDate)
                let yesterdayKey = "\(yesterdayComponents.year!)-\(yesterdayComponents.month!)-\(yesterdayComponents.day!)"
                if !completedDates.contains(yesterdayKey) {
                    return 0
                }
            } else {
                return 0
            }
        }
        
        while true {
            let components = calendar.dateComponents([.year, .month, .day], from: checkDate)
            let dateKey = "\(components.year!)-\(components.month!)-\(components.day!)"
            
            if completedDates.contains(dateKey) {
                streak += 1
                if let prevDate = calendar.date(byAdding: .day, value: -1, to: checkDate) {
                    checkDate = prevDate
                } else {
                    break
                }
            } else {
                break
            }
        }
        
        return streak
    }
}

// Swift Array helper matching Kotlin's `any` function
extension Array {
    func any(matching predicate: (Element) -> Bool) -> Bool {
        return self.first(where: predicate) != nil
    }
}
