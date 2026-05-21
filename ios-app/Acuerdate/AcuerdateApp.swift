import SwiftUI
import SwiftData

@main
struct AcuerdateApp: App {
    var body: some Scene {
        WindowGroup {
            MainScreen()
        }
        .modelContainer(for: [Reminder.self, CompletedReminder.self])
    }
}
