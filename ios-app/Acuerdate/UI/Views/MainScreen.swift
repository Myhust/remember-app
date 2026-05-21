import SwiftUI
import SwiftData

struct MainScreen: View {
    @Environment(\.modelContext) private var modelContext
    @StateObject private var viewModel = MainViewModel()
    
    @State private var showingAddSheet = false
    @State private var isCalendarView = false
    @State private var showingSoundSheet = false
    @AppStorage("selectedAlertTone") private var selectedAlertTone = "chime"
    
    // Form fields for adding reminders
    @State private var reminderText = ""
    @State private var selectedCategory = ReminderCategory.habito
    @State private var selectedPriority = ReminderPriority.medium
    @State private var selectedRecurrence = ReminderRecurrence.none
    @State private var enableBooking = false
    @State private var bookingDate = Date()
    @StateObject private var speechService = SpeechRecognitionService()
    
    // Background colors matching the premium dark theme
    private let backgroundColor = Color(hex: "0B0C10")
    private let cardBackground = Color(hex: "12141A")
    private let borderGlass = Color.white.opacity(0.08)
    
    var body: some View {
        ZStack {
            backgroundColor.ignoresSafeArea()
            
            ScrollView(.vertical, showsIndicators: false) {
                VStack(spacing: 20) {
                    
                    // HEADER
                    HStack(alignment: .center) {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Remember")
                                .font(.system(size: 26, weight: .black, design: .rounded))
                                .foregroundColor(.white)
                            Text("Transcribe & Organize")
                                .font(.system(size: 13, weight: .medium, design: .rounded))
                                .foregroundColor(.white.opacity(0.45))
                        }
                        
                        Spacer()
                        
                        // Streak Flame Badge
                        HStack(spacing: 4) {
                            Image(systemName: "flame.fill")
                                .foregroundColor(Color(hex: "FF9F0A"))
                            Text("\(viewModel.streak)")
                                .fontWeight(.bold)
                                .foregroundColor(.white)
                            Text("días")
                                .font(.system(size: 11))
                                .foregroundColor(.white.opacity(0.6))
                        }
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(Color.white.opacity(0.05))
                        .clipShape(Capsule())
                        .overlay(Capsule().stroke(borderGlass, lineWidth: 1))
                        
                        // Choose/Preview Alert Tone Settings
                        Button(action: {
                            HapticFeedbackHelper.shared.vibrateTick()
                            showingSoundSheet = true
                        }) {
                            Image(systemName: "speaker.wave.2.fill")
                                .font(.system(size: 15))
                                .foregroundColor(Color(hex: "00F0FF"))
                                .frame(width: 40, height: 40)
                                .background(Color.white.opacity(0.05))
                                .clipShape(Circle())
                                .overlay(Circle().stroke(borderGlass, lineWidth: 1))
                        }
                        
                        // Floating Plus Add Button
                        Button(action: {
                            HapticFeedbackHelper.shared.vibrateTick()
                            showingAddSheet = true
                        }) {
                            Image(systemName: "plus")
                                .font(.system(size: 18, weight: .bold))
                                .foregroundColor(.black)
                                .frame(width: 40, height: 40)
                                .background(Color(hex: "00F0FF"))
                                .clipShape(Circle())
                        }
                    }
                    .padding(.horizontal)
                    .padding(.top, 10)
                    
                    // PLANET WIDGET AND STATS
                    HStack(spacing: 12) {
                        // 1. Planet Widget
                        VStack {
                            PlanetWidget(state: viewModel.planetState)
                        }
                        .frame(width: 110, height: 130)
                        .background(cardBackground)
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                        .overlay(RoundedRectangle(cornerRadius: 16).stroke(borderGlass, lineWidth: 1))
                        
                        // 2. Stats Grid
                        VStack(spacing: 8) {
                            // Active Reminders
                            HStack {
                                Image(systemName: "bell.fill")
                                    .foregroundColor(Color(hex: "00F0FF"))
                                VStack(alignment: .leading, spacing: 1) {
                                    Text("\(viewModel.reminders.count)")
                                        .font(.system(.title3, design: .rounded))
                                        .fontWeight(.black)
                                        .foregroundColor(.white)
                                    Text("Activos")
                                        .font(.caption2)
                                        .foregroundColor(.white.opacity(0.5))
                                }
                                Spacer()
                            }
                            .padding(10)
                            .frame(maxWidth: .infinity)
                            .background(cardBackground)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .overlay(RoundedRectangle(cornerRadius: 12).stroke(borderGlass, lineWidth: 1))
                            
                            // Completed Reminders
                            HStack {
                                Image(systemName: "checkmark.seal.fill")
                                    .foregroundColor(Color(hex: "34D399"))
                                VStack(alignment: .leading, spacing: 1) {
                                    Text("\(viewModel.completedReminders.count)")
                                        .font(.system(.title3, design: .rounded))
                                        .fontWeight(.black)
                                        .foregroundColor(.white)
                                    Text("Cumplidos")
                                        .font(.caption2)
                                        .foregroundColor(.white.opacity(0.5))
                                }
                                Spacer()
                            }
                            .padding(10)
                            .frame(maxWidth: .infinity)
                            .background(cardBackground)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .overlay(RoundedRectangle(cornerRadius: 12).stroke(borderGlass, lineWidth: 1))
                        }
                    }
                    .padding(.horizontal)
                    
                    // TOGGLE RECORDATORIOS / CALENDARIO
                    HStack(spacing: 0) {
                        Button(action: {
                            HapticFeedbackHelper.shared.vibrateTick()
                            isCalendarView = false
                        }) {
                            HStack(spacing: 6) {
                                Image(systemName: "list.bullet")
                                Text("Recordatorios")
                            }
                            .font(.system(size: 13, weight: .bold, design: .rounded))
                            .foregroundColor(!isCalendarView ? .white : .white.opacity(0.4))
                            .frame(maxWidth: .infinity, minHeight: 40)
                            .background(!isCalendarView ? Color.white.opacity(0.08) : Color.clear)
                        }
                        
                        Button(action: {
                            HapticFeedbackHelper.shared.vibrateTick()
                            isCalendarView = true
                        }) {
                            HStack(spacing: 6) {
                                Image(systemName: "calendar")
                                Text("Calendario")
                            }
                            .font(.system(size: 13, weight: .bold, design: .rounded))
                            .foregroundColor(isCalendarView ? .white : .white.opacity(0.4))
                            .frame(maxWidth: .infinity, minHeight: 40)
                            .background(isCalendarView ? Color.white.opacity(0.08) : Color.clear)
                        }
                    }
                    .background(Color.white.opacity(0.03))
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                    .padding(.horizontal)
                    
                    // FILTER CATEGORY PILLS
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            // "Todos" pill
                            Button(action: {
                                HapticFeedbackHelper.shared.vibrateTick()
                                viewModel.setFilter(nil)
                            }) {
                                Text("Todos")
                                    .font(.system(size: 13, weight: .semibold, design: .rounded))
                                    .padding(.horizontal, 14)
                                    .padding(.vertical, 8)
                                    .background(viewModel.activeFilter == nil ? Color(hex: "00F0FF").opacity(0.2) : cardBackground)
                                    .foregroundColor(viewModel.activeFilter == nil ? Color(hex: "00F0FF") : .white)
                                    .clipShape(Capsule())
                                    .overlay(Capsule().stroke(viewModel.activeFilter == nil ? Color(hex: "00F0FF") : borderGlass, lineWidth: 1))
                            }
                            
                            // Categories pills
                            ForEach(ReminderCategory.allCases, id: \.self) { category in
                                Button(action: {
                                    HapticFeedbackHelper.shared.vibrateTick()
                                    viewModel.setFilter(category)
                                }) {
                                    Text(category.displayName)
                                        .font(.system(size: 13, weight: .semibold, design: .rounded))
                                        .padding(.horizontal, 14)
                                        .padding(.vertical, 8)
                                        .background(viewModel.activeFilter == category ? categoryColor(for: category).opacity(0.2) : cardBackground)
                                        .foregroundColor(viewModel.activeFilter == category ? categoryColor(for: category) : .white)
                                        .clipShape(Capsule())
                                        .overlay(Capsule().stroke(viewModel.activeFilter == category ? categoryColor(for: category) : borderGlass, lineWidth: 1))
                                }
                            }
                        }
                        .padding(.horizontal)
                    }
                    
                    // CONTENT VIEW AREA
                    if isCalendarView {
                        // CALENDAR INTERACTIVE VIEW
                        VStack(spacing: 16) {
                            CalendarView(
                                reminders: viewModel.reminders,
                                selectedDate: $viewModel.selectedDate,
                                onDateSelected: { date in
                                    viewModel.setSelectedDate(date)
                                },
                                onDayDoubleTapped: { date in
                                    viewModel.setSelectedDate(date)
                                    showingAddSheet = true
                                }
                            )
                            
                            // Day Details
                            VStack(alignment: .leading, spacing: 10) {
                                HStack {
                                    Text("Reservas del día")
                                        .font(.system(size: 14, weight: .bold, design: .rounded))
                                        .foregroundColor(.white)
                                    Spacer()
                                    Button(action: {
                                        showingAddSheet = true
                                    }) {
                                        HStack(spacing: 4) {
                                            Image(systemName: "plus")
                                            Text("Nueva")
                                        }
                                        .font(.system(size: 11, weight: .bold))
                                        .foregroundColor(Color(hex: "00F0FF"))
                                    }
                                }
                                
                                if viewModel.selectedDateReminders.isEmpty {
                                    Text("No tienes recordatorios programados para esta fecha.")
                                        .font(.system(size: 12, design: .rounded))
                                        .foregroundColor(.white.opacity(0.4))
                                        .padding(.vertical, 10)
                                } else {
                                    ForEach(viewModel.selectedDateReminders) { reminder in
                                        reminderRow(for: reminder)
                                    }
                                }
                            }
                            .padding()
                            .background(cardBackground)
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                            .overlay(RoundedRectangle(cornerRadius: 16).stroke(borderGlass, lineWidth: 1))
                        }
                        .padding(.horizontal)
                        
                    } else {
                        // LIST VIEW PERSISTENT REMINDERS
                        VStack(spacing: 10) {
                            if viewModel.filteredReminders.isEmpty {
                                VStack(spacing: 12) {
                                    Image(systemName: "questionmark.folder")
                                        .font(.system(size: 32))
                                        .foregroundColor(.white.opacity(0.2))
                                    Text("No tienes recordatorios activos en esta categoría.")
                                        .font(.system(size: 13, design: .rounded))
                                        .foregroundColor(.white.opacity(0.4))
                                        .multilineTextAlignment(.center)
                                }
                                .padding(.vertical, 40)
                            } else {
                                ForEach(viewModel.filteredReminders) { reminder in
                                    reminderRow(for: reminder)
                                        .contextMenu {
                                            Button {
                                                viewModel.completeReminder(reminderId: reminder.id)
                                            } label: {
                                                Label("Cumplir", systemImage: "checkmark.circle")
                                            }
                                            
                                            Button(role: .destructive) {
                                                viewModel.deleteReminder(reminderId: reminder.id)
                                            } label: {
                                                Label("Eliminar", systemImage: "trash")
                                            }
                                        }
                                }
                            }
                        }
                        .padding(.horizontal)
                    }
                    
                    // COMPLETED REMINDERS EXPANDABLE SECTION (HISTORIAL)
                    VStack(alignment: .leading) {
                        DisclosureGroup {
                            VStack(spacing: 12) {
                                HStack {
                                    Text("Registro de recordatorios completados.")
                                        .font(.system(size: 11))
                                        .foregroundColor(.white.opacity(0.4))
                                    Spacer()
                                    Button(action: {
                                        viewModel.clearHistory()
                                    }) {
                                        Text("Limpiar Historial")
                                            .font(.system(size: 11, weight: .bold))
                                            .foregroundColor(.red.opacity(0.8))
                                    }
                                }
                                .padding(.top, 4)
                                
                                if viewModel.completedReminders.isEmpty {
                                    Text("Aún no has cumplido ningún recordatorio. ¡Da el primer paso hoy!")
                                        .font(.system(size: 12, design: .rounded))
                                        .foregroundColor(.white.opacity(0.3))
                                        .padding(.vertical, 20)
                                        .frame(maxWidth: .infinity)
                                } else {
                                    ForEach(viewModel.completedReminders) { completed in
                                        HStack(spacing: 10) {
                                            Circle()
                                                .fill(categoryColor(for: completed.category).opacity(0.2))
                                                .frame(width: 24, height: 24)
                                                .overlay(Image(systemName: "checkmark").font(.system(size: 10)).foregroundColor(categoryColor(for: completed.category)))
                                            
                                            VStack(alignment: .leading, spacing: 2) {
                                                Text(completed.text)
                                                    .font(.system(size: 13, weight: .medium, design: .rounded))
                                                    .foregroundColor(.white.opacity(0.85))
                                                    .strikethrough()
                                                
                                                Text("\(completed.category.displayName) • \(formatDate(completed.completedAt))")
                                                    .font(.system(size: 10))
                                                    .foregroundColor(.white.opacity(0.4))
                                            }
                                            Spacer()
                                        }
                                        .padding(.vertical, 4)
                                    }
                                }
                            }
                        } label: {
                            HStack {
                                Image(systemName: "clock.arrow.circlepath")
                                    .foregroundColor(.white.opacity(0.7))
                                Text("Recuerdos Cumplidos (Historial)")
                                    .foregroundColor(.white.opacity(0.9))
                                    .fontWeight(.bold)
                                    .font(.system(size: 14, design: .rounded))
                                Spacer()
                                Text("\(viewModel.completedReminders.count)")
                                    .font(.system(size: 11, weight: .bold))
                                    .foregroundColor(.white)
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 4)
                                    .background(Color.white.opacity(0.1))
                                    .clipShape(Capsule())
                            }
                        }
                    }
                    .padding()
                    .background(cardBackground)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                    .overlay(RoundedRectangle(cornerRadius: 16).stroke(borderGlass, lineWidth: 1))
                    .padding(.horizontal)
                    
                    Spacer(height: 30)
                }
            }
        }
        .onAppear {
            viewModel.setModelContext(modelContext)
        }
        .sheet(isPresented: $showingAddSheet) {
            addReminderSheet()
        }
        .sheet(isPresented: $showingSoundSheet) {
            soundSettingsSheet()
        }
    }
    
    // View element for a single active reminder card
    @ViewBuilder
    private func reminderRow(for reminder: Reminder) -> some View {
        HStack(spacing: 12) {
            // Category outline badge
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 6) {
                    Text(reminder.category.displayName)
                        .font(.system(size: 10, weight: .bold, design: .rounded))
                        .foregroundColor(categoryColor(for: reminder.category))
                        .padding(.horizontal, 8)
                        .padding(.vertical, 2)
                        .background(categoryColor(for: reminder.category).opacity(0.12))
                        .clipShape(Capsule())
                    
                    Text("P. \(reminder.priority.displayName)")
                        .font(.system(size: 10, weight: .bold, design: .rounded))
                        .foregroundColor(.white.opacity(0.5))
                }
                
                Text(reminder.text)
                    .font(.system(size: 15, weight: .semibold, design: .rounded))
                    .foregroundColor(.white.opacity(0.95))
                    .lineLimit(2)
                
                if let due = reminder.dueDate {
                    HStack(spacing: 4) {
                        Image(systemName: "calendar.badge.clock")
                        Text(formatDateTime(due))
                        
                        if due < Date() {
                            Text("(Atrasado)")
                                .foregroundColor(.red)
                                .fontWeight(.bold)
                        }
                    }
                    .font(.system(size: 10, weight: .medium))
                    .foregroundColor(due < Date() ? .red : .white.opacity(0.45))
                } else {
                    Text("Activo hace \(formatDurationSince(reminder.createdAt))")
                        .font(.system(size: 10))
                        .foregroundColor(.white.opacity(0.4))
                }
            }
            
            Spacer()
            
            // Check button
            Button(action: {
                viewModel.completeReminder(reminderId: reminder.id)
            }) {
                Image(systemName: "circle")
                    .font(.system(size: 22))
                    .foregroundColor(categoryColor(for: reminder.category))
            }
            
            // Trash button
            Button(action: {
                viewModel.deleteReminder(reminderId: reminder.id)
            }) {
                Image(systemName: "trash")
                    .font(.system(size: 18))
                    .foregroundColor(.white.opacity(0.2))
            }
        }
        .padding()
        .background(cardBackground)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(reminder.dueDate != nil && reminder.dueDate! < Date() ? Color.red.opacity(0.3) : borderGlass, lineWidth: 1)
        )
    }
    
    // Bottom Sheet Form View for adding reminders
    @ViewBuilder
    private func addReminderSheet() -> some View {
        NavigationView {
            ZStack {
                Color(hex: "0D0E12").ignoresSafeArea()
                
                VStack(spacing: 16) {
                    ScrollView {
                        VStack(alignment: .leading, spacing: 20) {
                            Text("Crear Recuerdos / Agenda")
                                .font(.system(size: 20, weight: .black, design: .rounded))
                                .foregroundColor(.white)
                                .padding(.top, 10)
                            
                            // Description/Voice text box
                            VStack(alignment: .leading, spacing: 6) {
                                Text("¿Qué quieres recordar?")
                                    .font(.system(size: 13, weight: .bold, design: .rounded))
                                    .foregroundColor(.white.opacity(0.6))
                                
                                HStack(alignment: .top, spacing: 8) {
                                    TextEditor(text: $reminderText)
                                        .scrollContentBackground(.hidden)
                                        .background(Color.white.opacity(0.04))
                                        .foregroundColor(.white)
                                        .frame(height: 90)
                                        .clipShape(RoundedRectangle(cornerRadius: 10))
                                        .overlay(RoundedRectangle(cornerRadius: 10).stroke(borderGlass, lineWidth: 1))
                                    
                                    // Dictation Voice Button
                                    Button(action: {
                                        HapticFeedbackHelper.shared.vibrateTick()
                                        toggleVoiceListening()
                                    }) {
                                        ZStack {
                                            Circle()
                                                .fill(speechService.isListening ? Color.red.opacity(0.2) : Color.white.opacity(0.04))
                                                .frame(width: 44, height: 44)
                                                .overlay(Circle().stroke(speechService.isListening ? Color.red : borderGlass, lineWidth: 1))
                                            
                                            Image(systemName: speechService.isListening ? "mic.fill" : "mic")
                                                .font(.system(size: 16))
                                                .foregroundColor(speechService.isListening ? .red : Color(hex: "00F0FF"))
                                        }
                                    }
                                }
                                
                                // Voice feedback caption
                                HStack(spacing: 6) {
                                    Circle()
                                        .fill(speechService.isListening ? Color.red : Color.white.opacity(0.2))
                                        .frame(width: 6, height: 6)
                                    Text(speechService.statusText)
                                        .font(.system(size: 10))
                                        .foregroundColor(.white.opacity(0.4))
                                }
                                .padding(.top, 2)
                            }
                            
                            // Category picker
                            VStack(alignment: .leading, spacing: 8) {
                                Text("Categoría")
                                    .font(.system(size: 13, weight: .bold, design: .rounded))
                                    .foregroundColor(.white.opacity(0.6))
                                
                                ScrollView(.horizontal, showsIndicators: false) {
                                    HStack(spacing: 8) {
                                        ForEach(ReminderCategory.allCases, id: \.self) { category in
                                            Button(action: {
                                                HapticFeedbackHelper.shared.vibrateTick()
                                                selectedCategory = category
                                            }) {
                                                Text(category.displayName)
                                                    .font(.system(size: 12, weight: .semibold, design: .rounded))
                                                    .padding(.horizontal, 12)
                                                    .padding(.vertical, 6)
                                                    .background(selectedCategory == category ? categoryColor(for: category).opacity(0.2) : Color.white.opacity(0.03))
                                                    .foregroundColor(selectedCategory == category ? categoryColor(for: category) : .white)
                                                    .clipShape(Capsule())
                                                    .overlay(Capsule().stroke(selectedCategory == category ? categoryColor(for: category) : borderGlass, lineWidth: 1))
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // Priority & Recurrence Segmented grids
                            HStack(spacing: 16) {
                                VStack(alignment: .leading, spacing: 8) {
                                    Text("Prioridad")
                                        .font(.system(size: 13, weight: .bold, design: .rounded))
                                        .foregroundColor(.white.opacity(0.6))
                                    
                                    Picker("Prioridad", selection: $selectedPriority) {
                                        ForEach(ReminderPriority.allCases, id: \.self) { priority in
                                            Text(priority.displayName).tag(priority)
                                        }
                                    }
                                    .pickerStyle(.segmented)
                                    .background(Color.white.opacity(0.02))
                                    .clipShape(RoundedRectangle(cornerRadius: 8))
                                }
                                
                                VStack(alignment: .leading, spacing: 8) {
                                    Text("Recurrencia (Hábito)")
                                        .font(.system(size: 13, weight: .bold, design: .rounded))
                                        .foregroundColor(.white.opacity(0.6))
                                    
                                    Picker("Recurrencia", selection: $selectedRecurrence) {
                                        ForEach(ReminderRecurrence.allCases, id: \.self) { recurrence in
                                            Text(recurrence.displayName).tag(recurrence)
                                        }
                                    }
                                    .pickerStyle(.menu)
                                    .frame(maxWidth: .infinity, minHeight: 32)
                                    .background(Color.white.opacity(0.04))
                                    .foregroundColor(.white)
                                    .clipShape(RoundedRectangle(cornerRadius: 8))
                                    .overlay(RoundedRectangle(cornerRadius: 8).stroke(borderGlass, lineWidth: 1))
                                }
                            }
                            
                            // Program date time toggle
                            VStack(spacing: 10) {
                                Toggle(isOn: $enableBooking) {
                                    Text("¿Programar fecha y hora? (Booking)")
                                        .font(.system(size: 13, weight: .bold, design: .rounded))
                                        .foregroundColor(.white.opacity(0.6))
                                }
                                .toggleStyle(SwitchToggleStyle(tint: Color(hex: "00F0FF")))
                                
                                if enableBooking {
                                    DatePicker("Fecha y Hora de la Reserva", selection: $bookingDate, in: Date()..., displayedComponents: [.date, .hourAndMinute])
                                        .datePickerStyle(.compact)
                                        .colorScheme(.dark)
                                        .font(.system(size: 12))
                                        .padding(8)
                                        .background(Color.white.opacity(0.02))
                                        .clipShape(RoundedRectangle(cornerRadius: 8))
                                }
                            }
                        }
                        .padding(.horizontal)
                    }
                    
                    Spacer()
                    
                    // Dialog Actions (Cancelar / Guardar)
                    HStack(spacing: 12) {
                        Button(action: {
                            HapticFeedbackHelper.shared.vibrateTick()
                            showingAddSheet = false
                        }) {
                            Text("Cancelar")
                                .fontWeight(.bold)
                                .foregroundColor(.white.opacity(0.6))
                                .frame(maxWidth: .infinity, minHeight: 44)
                                .background(Color.white.opacity(0.04))
                                .clipShape(RoundedRectangle(cornerRadius: 10))
                                .overlay(RoundedRectangle(cornerRadius: 10).stroke(borderGlass, lineWidth: 1))
                        }
                        
                        Button(action: {
                            if !reminderText.isEmpty {
                                viewModel.createReminder(
                                    text: reminderText,
                                    category: selectedCategory,
                                    priority: selectedPriority,
                                    dueDate: enableBooking ? bookingDate : nil,
                                    recurrence: selectedRecurrence
                                )
                                showingAddSheet = false
                                // Reset form fields
                                reminderText = ""
                                enableBooking = false
                            }
                        }) {
                            Text("Guardar")
                                .fontWeight(.bold)
                                .foregroundColor(.black)
                                .frame(maxWidth: .infinity, minHeight: 44)
                                .background(reminderText.isEmpty ? Color.white.opacity(0.1) : Color(hex: "00F0FF"))
                                .clipShape(RoundedRectangle(cornerRadius: 10))
                        }
                        .disabled(reminderText.isEmpty)
                    }
                    .padding(.horizontal)
                    .padding(.bottom, 10)
                }
            }
            .navigationBarHidden(true)
            .onChange(of: speechService.transcript) { _, newValue in
                if !newValue.isEmpty { reminderText = newValue }
            }
            .onDisappear {
                if speechService.isListening { speechService.stopListening() }
            }
        }
    }

    private func toggleVoiceListening() {
        if speechService.isListening {
            speechService.stopListening()
        } else {
            speechService.transcript = ""
            speechService.startListening()
        }
    }
    
    // Category colors
    private func categoryColor(for category: ReminderCategory) -> Color {
        switch category {
        case .importante: return Color(hex: "FF4B4B")
        case .habito: return Color(hex: "00F0FF")
        case .tarea: return Color(hex: "39FF14")
        case .idea: return Color(hex: "FFEA00")
        case .nota: return Color(hex: "D080FF")
        }
    }
    
    // Format helpers
    private func formatDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "es_ES")
        formatter.dateFormat = "d MMM, HH:mm"
        return formatter.string(from: date)
    }
    
    private func formatDateTime(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "es_ES")
        formatter.dateFormat = "d MMM, HH:mm"
        return formatter.string(from: date)
    }
    
    private func formatDurationSince(_ date: Date) -> String {
        let diff = Date().timeIntervalSince(date)
        let mins = Int(diff / 60)
        if mins < 1 { return "1 minuto" }
        if mins < 60 { return "\(mins) minutos" }
        let hours = mins / 60
        if hours < 24 { return "\(hours) horas" }
        let days = hours / 24
        return "\(days) días"
    }
    
    // Sound Settings Sheet
    @ViewBuilder
    private func soundSettingsSheet() -> some View {
        NavigationView {
            ZStack {
                Color(hex: "0D0E12").ignoresSafeArea()
                
                VStack(spacing: 20) {
                    ScrollView {
                        VStack(alignment: .leading, spacing: 22) {
                            // Title & Description
                            VStack(alignment: .leading, spacing: 6) {
                                Text("Ajustes de Sonido (Aviso)")
                                    .font(.system(size: 20, weight: .black, design: .rounded))
                                    .foregroundColor(.white)
                                    .padding(.top, 10)
                                
                                Text("Selecciona el tono sintetizado por software que deseas escuchar en los avisos del iPhone. Toca un tono para preescuchar su timbre.")
                                    .font(.system(size: 13, design: .rounded))
                                    .foregroundColor(.white.opacity(0.5))
                                    .lineLimit(nil)
                            }
                            .padding(.horizontal)
                            
                            // Tones list card
                            VStack(spacing: 12) {
                                ForEach(AlertTone.allCases) { tone in
                                    let isSelected = selectedAlertTone == tone.rawValue
                                    
                                    Button(action: {
                                        HapticFeedbackHelper.shared.vibrateTick()
                                        selectedAlertTone = tone.rawValue
                                        SoundSynthesizer.shared.playTone(tone)
                                    }) {
                                        HStack(spacing: 16) {
                                            // Icon
                                            ZStack {
                                                Circle()
                                                    .fill(isSelected ? Color(hex: "00F0FF").opacity(0.15) : Color.white.opacity(0.04))
                                                    .frame(width: 44, height: 44)
                                                
                                                Image(systemName: tone.systemIcon)
                                                    .font(.system(size: 18, weight: .bold))
                                                    .foregroundColor(isSelected ? Color(hex: "00F0FF") : .white.opacity(0.6))
                                            }
                                            
                                            // Name
                                            VStack(alignment: .leading, spacing: 2) {
                                                Text(tone.displayName)
                                                    .font(.system(size: 15, weight: .bold, design: .rounded))
                                                    .foregroundColor(isSelected ? .white : .white.opacity(0.85))
                                                
                                                Text(isSelected ? "Tono seleccionado" : "Sintetizador PCM")
                                                    .font(.system(size: 11))
                                                    .foregroundColor(isSelected ? Color(hex: "00F0FF").opacity(0.8) : .white.opacity(0.35))
                                            }
                                            
                                            Spacer()
                                            
                                            // Selection Radio Glow
                                            ZStack {
                                                Circle()
                                                    .stroke(isSelected ? Color(hex: "00F0FF") : Color.white.opacity(0.1), lineWidth: 2)
                                                    .frame(width: 22, height: 22)
                                                
                                                if isSelected {
                                                    Circle()
                                                        .fill(Color(hex: "00F0FF"))
                                                        .frame(width: 12, height: 12)
                                                        .shadow(color: Color(hex: "00F0FF").opacity(0.8), radius: 4)
                                                }
                                            }
                                        }
                                        .padding()
                                        .background(isSelected ? Color(hex: "12141A").opacity(0.8) : Color(hex: "12141A"))
                                        .clipShape(RoundedRectangle(cornerRadius: 16))
                                        .overlay(
                                            RoundedRectangle(cornerRadius: 16)
                                                .stroke(isSelected ? Color(hex: "00F0FF").opacity(0.4) : borderGlass, lineWidth: 1)
                                        )
                                    }
                                    .buttonStyle(PlainButtonStyle())
                                }
                            }
                            .padding(.horizontal)
                        }
                    }
                    
                    Spacer()
                    
                    // Close button
                    Button(action: {
                        HapticFeedbackHelper.shared.vibrateTick()
                        showingSoundSheet = false
                    }) {
                        Text("Cerrar y Aplicar")
                            .fontWeight(.bold)
                            .foregroundColor(.black)
                            .frame(maxWidth: .infinity, minHeight: 46)
                            .background(Color(hex: "00F0FF"))
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .shadow(color: Color(hex: "00F0FF").opacity(0.3), radius: 6, x: 0, y: 3)
                    }
                    .padding(.horizontal)
                    .padding(.bottom, 12)
                }
            }
            .navigationBarHidden(true)
        }
    }
}
