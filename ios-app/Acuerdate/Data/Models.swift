import Foundation
import SwiftData

enum ReminderCategory: String, Codable, CaseIterable {
    case importante
    case habito
    case tarea
    case idea
    case nota
    
    var displayName: String {
        switch self {
        case .importante: return "Importante"
        case .habito: return "Hábito"
        case .tarea: return "Tarea"
        case .idea: return "Idea"
        case .nota: return "Nota"
        }
    }
}

enum ReminderPriority: String, Codable, CaseIterable {
    case high
    case medium
    case low
    
    var displayName: String {
        switch self {
        case .high: return "Alta"
        case .medium: return "Media"
        case .low: return "Baja"
        }
    }
}

enum ReminderRecurrence: String, Codable, CaseIterable {
    case none
    case daily
    case weekly
    
    var displayName: String {
        switch self {
        case .none: return "Única vez"
        case .daily: return "Diario 🔁"
        case .weekly: return "Semanal 🔁"
        }
    }
}

enum PlanetState: String, Codable {
    case inerte
    case estable
    case radiante
    case peligro
    
    var displayName: String {
        switch self {
        case .inerte: return "Inerte"
        case .estable: return "Estable"
        case .radiante: return "Radiante"
        case .peligro: return "Peligro"
        }
    }
}

@Model
final class Reminder {
    @Attribute(.unique) var id: String
    var text: String
    var categoryRaw: String
    var priorityRaw: String
    var createdAt: Date
    var dueDate: Date?
    var recurrenceRaw: String
    var alerted: BooleanLiteralType
    
    var category: ReminderCategory {
        get { ReminderCategory(rawValue: categoryRaw) ?? .habito }
        set { categoryRaw = newValue.rawValue }
    }
    
    var priority: ReminderPriority {
        get { ReminderPriority(rawValue: priorityRaw) ?? .medium }
        set { priorityRaw = newValue.rawValue }
    }
    
    var recurrence: ReminderRecurrence {
        get { ReminderRecurrence(rawValue: recurrenceRaw) ?? .none }
        set { recurrenceRaw = newValue.rawValue }
    }
    
    init(id: String = UUID().uuidString,
         text: String,
         category: ReminderCategory = .habito,
         priority: ReminderPriority = .medium,
         createdAt: Date = Date(),
         dueDate: Date? = nil,
         recurrence: ReminderRecurrence = .none,
         alerted: Bool = false) {
        self.id = id
        self.text = text
        self.categoryRaw = category.rawValue
        self.priorityRaw = priority.rawValue
        self.createdAt = createdAt
        self.dueDate = dueDate
        self.recurrenceRaw = recurrence.rawValue
        self.alerted = alerted
    }
}

@Model
final class CompletedReminder {
    @Attribute(.unique) var id: String
    var reminderId: String
    var text: String
    var categoryRaw: String
    var completedAt: Date
    
    var category: ReminderCategory {
        get { ReminderCategory(rawValue: categoryRaw) ?? .habito }
        set { categoryRaw = newValue.rawValue }
    }
    
    init(id: String = UUID().uuidString,
         reminderId: String,
         text: String,
         category: ReminderCategory,
         completedAt: Date = Date()) {
        self.id = id
        self.reminderId = reminderId
        self.text = text
        self.categoryRaw = category.rawValue
        self.completedAt = completedAt
    }
}
