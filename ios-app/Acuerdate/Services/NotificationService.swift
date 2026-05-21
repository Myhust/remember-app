import UserNotifications

class NotificationService {
    static let shared = NotificationService()

    private init() {}

    func requestPermissions() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { _, error in
            if let error { print("Notification permission error: \(error)") }
        }
    }

    func scheduleAlarm(reminderId: String, text: String, dueDate: Date, category: String) {
        let content = UNMutableNotificationContent()
        content.title = categoryTitle(for: category)
        content.body = text
        content.sound = .default
        content.badge = 1
        content.userInfo = ["reminderId": reminderId]

        let components = Calendar.current.dateComponents([.year, .month, .day, .hour, .minute], from: dueDate)
        let trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: false)
        let request = UNNotificationRequest(identifier: reminderId, content: content, trigger: trigger)

        UNUserNotificationCenter.current().add(request) { error in
            if let error { print("Failed to schedule notification: \(error)") }
        }
    }

    func cancelAlarm(reminderId: String) {
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: [reminderId])
    }

    func cancelAllAlarms() {
        UNUserNotificationCenter.current().removeAllPendingNotificationRequests()
    }

    private func categoryTitle(for category: String) -> String {
        switch category {
        case "importante": return "⚠️ Recordatorio Importante"
        case "habito":     return "🔁 Hábito Programado"
        case "tarea":      return "✅ Tarea Pendiente"
        case "idea":       return "💡 Idea Anotada"
        case "nota":       return "📝 Nota Programada"
        default:           return "⏰ Recordatorio"
        }
    }
}
