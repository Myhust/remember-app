import SwiftUI

struct CalendarView: View {
    let reminders: [Reminder]
    @Binding var selectedDate: Date
    let onDateSelected: (Date) -> Void
    let onDayDoubleTapped: (Date) -> Void
    
    @State private var visibleDate = Date()
    
    private let calendar = Calendar.current
    private let weekDays = ["Lu", "Ma", "Mi", "Ju", "Vi", "Sá", "Do"]
    
    var body: some View {
        VStack(spacing: 12) {
            // Calendar Header with Month/Year and navigation arrows
            HStack {
                Button(action: {
                    changeMonth(by: -1)
                }) {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(.white)
                        .frame(width: 40, height: 40)
                        .background(Color.white.opacity(0.05))
                        .clipShape(Circle())
                }
                
                Spacer()
                
                Text(monthYearString(from: visibleDate))
                    .font(.system(.body, design: .rounded))
                    .fontWeight(.bold)
                    .foregroundColor(.white)
                
                Spacer()
                
                Button(action: {
                    changeMonth(by: 1)
                }) {
                    Image(systemName: "chevron.right")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(.white)
                        .frame(width: 40, height: 40)
                        .background(Color.white.opacity(0.05))
                        .clipShape(Circle())
                }
            }
            .padding(.horizontal, 4)
            
            // Weekday labels
            HStack(spacing: 0) {
                ForEach(weekDays, id: \.self) { day in
                    Text(day)
                        .frame(maxWidth: .infinity)
                        .font(.system(size: 12, weight: .semibold, design: .rounded))
                        .foregroundColor(.white.opacity(0.4))
                }
            }
            
            // Monthly Days Grid
            let days = generateDaysInMonth()
            let columns = Array(repeating: GridItem(.flexible(), spacing: 4), count: 7)
            
            LazyVGrid(columns: columns, spacing: 4) {
                ForEach(0..<days.count, id: \.self) { index in
                    if let date = days[index] {
                        let isSelected = calendar.isDate(date, inSameDayAs: selectedDate)
                        let isToday = calendar.isDateInToday(date)
                        let dayReminders = reminders.filter { reminder in
                            if let due = reminder.dueDate {
                                return calendar.isDate(due, inSameDayAs: date)
                            }
                            return false
                        }
                        
                        let dayNumber = calendar.component(.day, from: date)
                        
                        VStack(spacing: 2) {
                            Text("\(dayNumber)")
                                .font(.system(size: 13, weight: (isToday || isSelected) ? .bold : .regular, design: .rounded))
                                .foregroundColor(isSelected ? .white : (isToday ? Color(hex: "00F0FF") : .white.opacity(0.85)))
                            
                            // Category dots below number
                            if !dayReminders.isEmpty {
                                HStack(spacing: 2) {
                                    ForEach(Array(dayReminders.prefix(3).enumerated()), id: \.offset) { _, reminder in
                                        Circle()
                                            .fill(categoryColor(for: reminder.category))
                                            .frame(width: 4, height: 4)
                                    }
                                }
                            } else {
                                Spacer()
                                    .frame(height: 4)
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .aspectRatio(1.0, contentMode: .fill)
                        .background(isSelected ? Color(hex: "00F0FF").opacity(0.25) : Color.clear)
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                        .overlay(
                            RoundedRectangle(cornerRadius: 8)
                                .stroke(isSelected ? Color(hex: "00F0FF") : (isToday ? .white.opacity(0.25) : .clear), lineWidth: 1)
                        )
                        .onTapGesture(count: 2) {
                            onDayDoubleTapped(date)
                        }
                        .onTapGesture(count: 1) {
                            selectedDate = date
                            onDateSelected(date)
                        }
                    } else {
                        // Empty cell placeholder for days before first of month
                        Color.clear
                            .aspectRatio(1.0, contentMode: .fill)
                    }
                }
            }
        }
        .padding(12)
        .background(Color(hex: "16181D"))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(Color.white.opacity(0.08), lineWidth: 1)
        )
    }
    
    private func changeMonth(by value: Int) {
        if let newDate = calendar.date(byAdding: .month, value: value, to: visibleDate) {
            visibleDate = newDate
        }
    }
    
    private func monthYearString(from date: Date) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "es_ES")
        formatter.dateFormat = "MMMM yyyy"
        return formatter.string(from: date).capitalized
    }
    
    private func generateDaysInMonth() -> [Date?] {
        guard let monthRange = calendar.range(of: .day, in: .month, for: visibleDate),
              let firstOfMonth = calendar.date(from: calendar.dateComponents([.year, .month], from: visibleDate)) else {
            return []
        }
        
        let weekdayOfFirst = calendar.component(.weekday, from: firstOfMonth)
        // Convert Sunday-first (1-7, Sun=1) to Monday-first (0-6, Mon=0)
        let firstDayOfWeek = weekdayOfFirst == 1 ? 6 : weekdayOfFirst - 2
        
        var days: [Date?] = Array(repeating: nil, count: firstDayOfWeek)
        
        for day in 1...monthRange.count {
            if let date = calendar.date(byAdding: .day, value: day - 1, to: firstOfMonth) {
                days.append(date)
            }
        }
        
        return days
    }
    
    private func categoryColor(for category: ReminderCategory) -> Color {
        switch category {
        case .importante: return Color(hex: "FF4B4B")
        case .habito: return Color(hex: "00F0FF")
        case .tarea: return Color(hex: "39FF14")
        case .idea: return Color(hex: "FFEA00")
        case .nota: return Color(hex: "D080FF")
        }
    }
}
