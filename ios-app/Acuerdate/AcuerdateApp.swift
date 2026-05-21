import SwiftUI
import SwiftData

@main
struct AcuerdateApp: App {
    init() {
        NotificationService.shared.requestPermissions()
    }

    var body: some Scene {
        WindowGroup {
            MainScreen()
        }
        .modelContainer(for: [Reminder.self, CompletedReminder.self])
    }
}
