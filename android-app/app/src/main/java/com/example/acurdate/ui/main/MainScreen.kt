package com.example.acurdate.ui.main

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import androidx.compose.runtime.rememberCoroutineScope
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.acurdate.data.*
import com.example.acurdate.theme.*
import com.example.acurdate.ui.HapticFeedbackHelper
import com.example.acurdate.ui.SoundSynthesizer
import com.example.acurdate.ui.AlertTone
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel? = null
) {
    val context = LocalContext.current
    val realViewModel = viewModel ?: viewModel {
        val appContext = context.applicationContext
        val sound = SoundSynthesizer(appContext)
        val haptic = HapticFeedbackHelper(appContext)
        MainScreenViewModel(JSONDataRepository(appContext), sound, haptic)
    }
    val viewModel = realViewModel
    val state by viewModel.state.collectAsState()
    
    val soundSynthesizer = remember { SoundSynthesizer(context.applicationContext) }
    
    // Ticker to force recompositions for active time elapsed counters
    var ticker by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while(true) {
            delay(30000L) // update every 30 seconds
            ticker++
            viewModel.checkAndTriggerSOSAlarms()
        }
    }

    var selectedTab by remember { mutableStateOf(0) } // 0 = Recordatorios, 1 = Calendario
    var isCreateDialogOpen by remember { mutableStateOf(false) }
    var isSoundDialogOpen by remember { mutableStateOf(false) }
    var preselectedCalendar = remember { mutableStateOf<Calendar?>(null) }
    
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    preselectedCalendar.value = null
                    isCreateDialogOpen = true 
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 16.dp, end = 8.dp)
            ) {
                AddIcon(color = Color.Black, modifier = Modifier.size(28.dp))
            }
        },
        containerColor = SpaceBackground
    ) { paddingValues ->
        Column(
            modifier = modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Sleek Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Remember",
                        style = Typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Transcribe & Organize",
                        style = Typography.bodySmall.copy(color = TextSecondary)
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Sound Settings Button
                    IconButton(
                        onClick = { isSoundDialogOpen = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E1F26))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                    ) {
                        SpeakerIcon(
                            color = ThemePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Racha / Streak Glowing Badge
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E1F26))
                            .border(1.dp, Color(0xFFFBBF24).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FavoriteIcon(
                            color = Color(0xFFFBBF24),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${state.streak} días",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
            
            // 2. KPI statistics & Habit Planet Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Active Card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SpaceCardBackground)
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Activos", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = state.reminders.size.toString(),
                        color = ThemePrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                
                // Completed Card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SpaceCardBackground)
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Cumplidos", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = state.completedReminders.size.toString(),
                        color = ThemeSecondary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                
                // Habit Planet Card
                Box(
                    modifier = Modifier
                        .weight(1.3f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SpaceCardBackground)
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PlanetWidget(state = state.planetState)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 3. Tab Switches (Recordatorios vs Calendario)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF16181D))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedTab == 0) Color(0xFF262930) else Color.Transparent)
                        .clickable { selectedTab = 0 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Recordatorios",
                        color = if (selectedTab == 0) Color.White else TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedTab == 1) Color(0xFF262930) else Color.Transparent)
                        .clickable { selectedTab = 1 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Calendario",
                        color = if (selectedTab == 1) Color.White else TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 4. Tab Contents
            if (selectedTab == 0) {
                // FILTER BAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryFilterChip(
                        name = "Todos",
                        isSelected = state.activeFilter == null,
                        color = ThemePrimary,
                        onClick = { viewModel.setFilter(null) }
                    )
                    ReminderCategory.values().forEach { category ->
                        val chipColor = when(category) {
                            ReminderCategory.IMPORTANTE -> NeonRed
                            ReminderCategory.HABITO -> NeonCyan
                            ReminderCategory.TAREA -> NeonGreen
                            ReminderCategory.IDEA -> NeonYellow
                            ReminderCategory.NOTA -> NeonPurple
                        }
                        CategoryFilterChip(
                            name = category.displayName,
                            isSelected = state.activeFilter == category,
                            color = chipColor,
                            onClick = { viewModel.setFilter(category) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // ACTIVE REMINDERS LIST
                if (state.filteredReminders.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No tienes recordatorios activos en esta categoría.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Unused 'ticker' references forces compose updates
                        val activeTicker = ticker
                        state.filteredReminders.forEach { reminder ->
                            ReminderCard(
                                reminder = reminder,
                                activeTicker = activeTicker,
                                onComplete = { viewModel.completeReminder(reminder.id) },
                                onDelete = { viewModel.deleteReminder(reminder.id) }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // COLLAPSIBLE HISTORY
                var isHistoryExpanded by remember { mutableStateOf(false) }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SpaceCardBackground)
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isHistoryExpanded = !isHistoryExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recuerdos Cumplidos (Historial)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        if (isHistoryExpanded) {
                            KeyboardArrowUpIcon(color = TextSecondary, modifier = Modifier.size(24.dp))
                        } else {
                            KeyboardArrowDownIcon(color = TextSecondary, modifier = Modifier.size(24.dp))
                        }
                    }
                    
                    if (isHistoryExpanded) {
                        Spacer(modifier = Modifier.height(10.dp))
                        if (state.completedReminders.isEmpty()) {
                            Text(
                                text = "El historial está vacío. ¡Cumple un recordatorio para empezar!",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                state.completedReminders.take(15).forEach { comp ->
                                    val dateStr = SimpleDateFormat("dd MMM, HH:mm", Locale("es", "ES"))
                                        .format(Date(comp.completedAt))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = comp.text,
                                                color = TextPrimary.copy(alpha = 0.7f),
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${comp.category.displayName} • $dateStr",
                                                color = TextSecondary.copy(alpha = 0.6f),
                                                fontSize = 10.sp
                                            )
                                        }
                                        CheckIcon(
                                            color = ThemePrimary.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // CALENDAR TAB CONTENT
                CalendarView(
                    reminders = state.reminders,
                    selectedDate = state.selectedDate,
                    onDateSelected = { viewModel.setSelectedDate(it) },
                    onDayDoubleTapped = { cal ->
                        preselectedCalendar.value = cal
                        isCreateDialogOpen = true
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Day details pane
                val dateLabel = SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "ES"))
                    .format(state.selectedDate.time)
                    .replaceFirstChar { it.uppercase() }
                
                Text(
                    text = "Agenda: $dateLabel",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (state.selectedDateReminders.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SpaceCardBackground)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay reservas programadas para este día.\nHaz doble-tap para agregar una.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.selectedDateReminders.forEach { reminder ->
                            val activeTicker = ticker
                            ReminderCard(
                                reminder = reminder,
                                activeTicker = activeTicker,
                                onComplete = { viewModel.completeReminder(reminder.id) },
                                onDelete = { viewModel.deleteReminder(reminder.id) }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(64.dp))
        }
    }
    
    // CREATION DIALOG
    if (isCreateDialogOpen) {
        CreateReminderDialog(
            preselectedDate = preselectedCalendar.value,
            onDismiss = { isCreateDialogOpen = false },
            onCreate = { text, category, priority, due, rec ->
                viewModel.createReminder(text, category, priority, due, rec)
                isCreateDialogOpen = false
            }
        )
    }
    
    // SOUND SETTINGS DIALOG
    if (isSoundDialogOpen) {
        SoundSettingsDialog(
            soundSynthesizer = soundSynthesizer,
            onDismiss = { isSoundDialogOpen = false }
        )
    }
}

@Composable
fun CategoryFilterChip(
    name: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) color.copy(alpha = 0.15f) else Color(0xFF1E1F26))
            .border(
                width = 1.dp,
                color = if (isSelected) color else Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            color = if (isSelected) color else TextSecondary,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

@Composable
fun ReminderCard(
    reminder: Reminder,
    activeTicker: Int,
    onComplete: () -> Unit,
    onDelete: () -> Unit
) {
    val categoryColor = when(reminder.category) {
        ReminderCategory.IMPORTANTE -> NeonRed
        ReminderCategory.HABITO -> NeonCyan
        ReminderCategory.TAREA -> NeonGreen
        ReminderCategory.IDEA -> NeonYellow
        ReminderCategory.NOTA -> NeonPurple
    }
    
    val priorityLabel = when(reminder.priority) {
        ReminderPriority.ALTA -> "P. Alta"
        ReminderPriority.MEDIA -> "P. Media"
        ReminderPriority.BAJA -> "P. Baja"
    }
    
    val priorityColor = when(reminder.priority) {
        ReminderPriority.ALTA -> NeonRed
        ReminderPriority.MEDIA -> NeonYellow
        ReminderPriority.BAJA -> NeonGreen
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SpaceCardBackground)
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Urgency dot
        UrgencyIndicator(createdAt = reminder.createdAt)
        
        Spacer(modifier = Modifier.width(10.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Category Chip Tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(categoryColor.copy(alpha = 0.12f))
                        .border(0.5.dp, categoryColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = reminder.category.displayName,
                        color = categoryColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.width(6.dp))
                
                // Priority Tag
                Text(
                    text = priorityLabel,
                    color = priorityColor.copy(alpha = 0.8f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold
                )
                
                // Recurrence Tag
                if (reminder.recurrence != ReminderRecurrence.NONE) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = reminder.recurrence.displayName,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // Text Body
            Text(
                text = reminder.text,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // Time ticker & due date
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = getActiveDurationText(reminder.createdAt),
                    color = TextSecondary.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
                
                if (reminder.dueDate != null) {
                    val dateLabel = SimpleDateFormat("dd MMM, HH:mm", Locale("es", "ES"))
                        .format(Date(reminder.dueDate))
                    Text(
                        text = "📅 Límite: $dateLabel",
                        color = if (reminder.dueDate < System.currentTimeMillis()) NeonRed else TextSecondary.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        fontWeight = if (reminder.dueDate < System.currentTimeMillis()) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Actions
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onComplete,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = ThemePrimary.copy(alpha = 0.1f)
                ),
                modifier = Modifier.size(36.dp)
            ) {
                CheckIcon(
                    color = ThemePrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            
            IconButton(
                onClick = onDelete,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = NeonRed.copy(alpha = 0.1f)
                ),
                modifier = Modifier.size(36.dp)
            ) {
                DeleteIcon(
                    color = NeonRed,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun UrgencyIndicator(createdAt: Long) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    val ageMs = System.currentTimeMillis() - createdAt
    val color = when {
        ageMs < 24 * 60 * 60 * 1000L -> Color(0xFF39FF14)
        ageMs < 3 * 24 * 60 * 60 * 1000L -> Color(0xFFFFEA00)
        else -> Color(0xFFFF4B4B)
    }
    
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
            .border(1.dp, color, CircleShape)
    )
}

fun getActiveDurationText(createdAt: Long): String {
    val durationMs = System.currentTimeMillis() - createdAt
    val seconds = durationMs / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    
    return when {
        days > 0 -> "Activo hace $days ${if (days == 1L) "día" else "días"}"
        hours > 0 -> "Activo hace $hours ${if (hours == 1L) "hora" else "horas"}"
        minutes > 0 -> "Activo hace $minutes ${if (minutes == 1L) "minuto" else "minutos"}"
        else -> "Activo hace unos instantes"
    }
}

@Composable
fun MicIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.35f, h * 0.22f),
            size = Size(w * 0.3f, h * 0.44f),
            cornerRadius = CornerRadius(w * 0.15f, w * 0.15f)
        )
        drawArc(
            color = color,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.23f, h * 0.36f),
            size = Size(w * 0.54f, w * 0.54f),
            style = Stroke(width = 2.5.dp.toPx())
        )
        drawLine(
            color = color,
            start = Offset(w * 0.5f, h * 0.74f),
            end = Offset(w * 0.5f, h * 0.9f),
            strokeWidth = 2.5.dp.toPx()
        )
        drawLine(
            color = color,
            start = Offset(w * 0.32f, h * 0.9f),
            end = Offset(w * 0.68f, h * 0.9f),
            strokeWidth = 2.5.dp.toPx()
        )
    }
}

@Composable
fun CreateReminderDialog(
    preselectedDate: Calendar?,
    onDismiss: () -> Unit,
    onCreate: (String, ReminderCategory, ReminderPriority, Long?, ReminderRecurrence) -> Unit
) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ReminderCategory.TAREA) }
    var priority by remember { mutableStateOf(ReminderPriority.MEDIA) }
    var recurrence by remember { mutableStateOf(ReminderRecurrence.NONE) }
    
    var enableDueDate by remember { mutableStateOf(preselectedDate != null) }
    val calendar = remember { 
        mutableStateOf(
            preselectedDate?.clone() as? Calendar ?: Calendar.getInstance().apply {
                add(Calendar.HOUR_OF_DAY, 2) // default 2 hours from now
            }
        ) 
    }
    
    // SPEECH REGISTRATION INTENT
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val results = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val recognizedText = results?.getOrNull(0) ?: ""
            text = recognizedText
        }
    }
    
    val startVoiceRecognition = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla ahora en español...")
        }
        try {
            voiceLauncher.launch(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SpaceCardBackground)
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Crear Recuerdos / Agenda",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Text Input Box
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Escribe o dicta...", color = TextSecondary, fontSize = 13.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E1F26)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1E1F26),
                        unfocusedContainerColor = Color(0xFF1E1F26),
                        disabledContainerColor = Color(0xFF1E1F26),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Mic Button
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(ThemePrimary.copy(alpha = 0.15f))
                        .border(1.dp, ThemePrimary.copy(alpha = 0.4f), CircleShape)
                        .clickable { startVoiceRecognition() },
                    contentAlignment = Alignment.Center
                ) {
                    MicIcon(color = ThemePrimary)
                }
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            // Category Selector
            Text("Categoría", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReminderCategory.values().forEach { cat ->
                    val color = when(cat) {
                        ReminderCategory.IMPORTANTE -> NeonRed
                        ReminderCategory.HABITO -> NeonCyan
                        ReminderCategory.TAREA -> NeonGreen
                        ReminderCategory.IDEA -> NeonYellow
                        ReminderCategory.NOTA -> NeonPurple
                    }
                    val isSelected = category == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) color.copy(alpha = 0.15f) else Color(0xFF1E1F26))
                            .border(
                                width = 1.dp,
                                color = if (isSelected) color else Color.White.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { category = cat }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = cat.displayName,
                            color = if (isSelected) color else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            // Priority & Recurrence Grid
            Row(modifier = Modifier.fillMaxWidth()) {
                // Priority Column
                Column(modifier = Modifier.weight(1f)) {
                    Text("Prioridad", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    ReminderPriority.values().forEach { prio ->
                        val isSelected = priority == prio
                        val color = when(prio) {
                            ReminderPriority.ALTA -> NeonRed
                            ReminderPriority.MEDIA -> NeonYellow
                            ReminderPriority.BAJA -> NeonGreen
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) color.copy(alpha = 0.1f) else Color.Transparent)
                                .clickable { priority = prio }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = prio.displayName,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Recurrence Column
                Column(modifier = Modifier.weight(1.2f)) {
                    Text("Recurrencia (Hábito)", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    ReminderRecurrence.values().forEach { rec ->
                        val isSelected = recurrence == rec
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) ThemeSecondary.copy(alpha = 0.1f) else Color.Transparent)
                                .clickable { recurrence = rec }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSelected) {
                                CheckIcon(
                                    color = ThemeSecondary,
                                    modifier = Modifier.size(12.dp)
                                )
                            } else {
                                NotificationsIcon(
                                    color = TextSecondary.copy(alpha = 0.4f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = rec.displayName,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            // Limit Date / Booking Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Programar Reserva / Fecha Límite",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
                Switch(
                    checked = enableDueDate,
                    onCheckedChange = { enableDueDate = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ThemePrimary,
                        checkedTrackColor = ThemePrimary.copy(alpha = 0.3f),
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = Color(0xFF1E1F26)
                    )
                )
            }
            
            // Limit Date Picker Panel
            AnimatedVisibility(visible = enableDueDate) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E1F26))
                        .padding(10.dp)
                ) {
                    val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES"))
                        .format(calendar.value.time)
                    val timeStr = SimpleDateFormat("HH:mm", Locale("es", "ES"))
                        .format(calendar.value.time)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Date selector button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF262930))
                                .clickable {
                                    val current = calendar.value
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, day ->
                                            val next = calendar.value.clone() as Calendar
                                            next.set(Calendar.YEAR, year)
                                            next.set(Calendar.MONTH, month)
                                            next.set(Calendar.DAY_OF_MONTH, day)
                                            calendar.value = next
                                        },
                                        current.get(Calendar.YEAR),
                                        current.get(Calendar.MONTH),
                                        current.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📅 $dateStr", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Time selector button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF262930))
                                .clickable {
                                    val current = calendar.value
                                    TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            val next = calendar.value.clone() as Calendar
                                            next.set(Calendar.HOUR_OF_DAY, hour)
                                            next.set(Calendar.MINUTE, minute)
                                            calendar.value = next
                                        },
                                        current.get(Calendar.HOUR_OF_DAY),
                                        current.get(Calendar.MINUTE),
                                        true
                                    ).show()
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⏰ $timeStr", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar", color = TextSecondary)
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Button(
                    onClick = {
                        if (text.isNotBlank()) {
                            onCreate(
                                text,
                                category,
                                priority,
                                if (enableDueDate) calendar.value.timeInMillis else null,
                                recurrence
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ThemePrimary,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp),
                    enabled = text.isNotBlank()
                ) {
                    Text("Guardar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AddIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 2.5.dp.toPx()
        drawLine(
            color = color,
            start = Offset(w * 0.25f, h * 0.5f),
            end = Offset(w * 0.75f, h * 0.5f),
            strokeWidth = stroke
        )
        drawLine(
            color = color,
            start = Offset(w * 0.5f, h * 0.25f),
            end = Offset(w * 0.5f, h * 0.75f),
            strokeWidth = stroke
        )
    }
}

@Composable
fun FavoriteIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.5f, h * 0.25f)
            cubicTo(w * 0.5f, h * 0.1f, w * 0.15f, h * 0.1f, w * 0.15f, h * 0.4f)
            cubicTo(w * 0.15f, h * 0.65f, w * 0.5f, h * 0.85f, w * 0.5f, h * 0.9f)
            cubicTo(w * 0.5f, h * 0.85f, w * 0.85f, h * 0.65f, w * 0.85f, h * 0.4f)
            cubicTo(w * 0.85f, h * 0.1f, w * 0.5f, h * 0.1f, w * 0.5f, h * 0.25f)
            close()
        }
        drawPath(path = path, color = color)
    }
}

@Composable
fun KeyboardArrowUpIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 2.dp.toPx()
        drawLine(
            color = color,
            start = Offset(w * 0.3f, h * 0.6f),
            end = Offset(w * 0.5f, h * 0.4f),
            strokeWidth = stroke
        )
        drawLine(
            color = color,
            start = Offset(w * 0.5f, h * 0.4f),
            end = Offset(w * 0.7f, h * 0.6f),
            strokeWidth = stroke
        )
    }
}

@Composable
fun KeyboardArrowDownIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 2.dp.toPx()
        drawLine(
            color = color,
            start = Offset(w * 0.3f, h * 0.4f),
            end = Offset(w * 0.5f, h * 0.6f),
            strokeWidth = stroke
        )
        drawLine(
            color = color,
            start = Offset(w * 0.5f, h * 0.6f),
            end = Offset(w * 0.7f, h * 0.4f),
            strokeWidth = stroke
        )
    }
}

@Composable
fun CheckIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 2.5.dp.toPx()
        drawLine(
            color = color,
            start = Offset(w * 0.25f, h * 0.5f),
            end = Offset(w * 0.45f, h * 0.7f),
            strokeWidth = stroke
        )
        drawLine(
            color = color,
            start = Offset(w * 0.45f, h * 0.7f),
            end = Offset(w * 0.75f, h * 0.3f),
            strokeWidth = stroke
        )
    }
}

@Composable
fun DeleteIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 2.dp.toPx()
        drawLine(
            color = color,
            start = Offset(w * 0.2f, h * 0.25f),
            end = Offset(w * 0.8f, h * 0.25f),
            strokeWidth = stroke
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.4f, h * 0.12f),
            size = Size(w * 0.2f, h * 0.13f),
            cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx()),
            style = Stroke(width = stroke)
        )
        val bodyPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.28f, h * 0.25f)
            lineTo(w * 0.32f, h * 0.82f)
            quadraticTo(w * 0.33f, h * 0.88f, w * 0.4f, h * 0.88f)
            lineTo(w * 0.6f, h * 0.88f)
            quadraticTo(w * 0.67f, h * 0.88f, w * 0.68f, h * 0.82f)
            lineTo(w * 0.72f, h * 0.25f)
        }
        drawPath(path = bodyPath, color = color, style = Stroke(width = stroke))
        drawLine(
            color = color,
            start = Offset(w * 0.43f, h * 0.38f),
            end = Offset(w * 0.43f, h * 0.75f),
            strokeWidth = stroke
        )
        drawLine(
            color = color,
            start = Offset(w * 0.57f, h * 0.38f),
            end = Offset(w * 0.57f, h * 0.75f),
            strokeWidth = stroke
        )
    }
}

@Composable
fun NotificationsIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 2.dp.toPx()
        drawCircle(
            color = color,
            radius = w * 0.08f,
            center = Offset(w * 0.5f, h * 0.15f)
        )
        val bellPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.5f, h * 0.2f)
            cubicTo(w * 0.3f, h * 0.2f, w * 0.3f, h * 0.65f, w * 0.2f, h * 0.7f)
            lineTo(w * 0.8f, h * 0.7f)
            cubicTo(w * 0.7f, h * 0.65f, w * 0.7f, h * 0.2f, w * 0.5f, h * 0.2f)
            close()
        }
        drawPath(path = bellPath, color = color)
        drawArc(
            color = color,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.42f, h * 0.7f),
            size = Size(w * 0.16f, h * 0.12f)
        )
    }
}

@Composable
fun SpeakerIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.25f, h * 0.35f)
            lineTo(w * 0.45f, h * 0.35f)
            lineTo(w * 0.7f, h * 0.15f)
            lineTo(w * 0.7f, h * 0.85f)
            lineTo(w * 0.45f, h * 0.65f)
            lineTo(w * 0.25f, h * 0.65f)
            close()
        }
        drawPath(path = path, color = color)
        
        // Sound wave arcs
        val stroke = 2.dp.toPx()
        drawArc(
            color = color,
            startAngle = -45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(w * 0.55f, h * 0.3f),
            size = Size(w * 0.3f, h * 0.4f),
            style = Stroke(width = stroke)
        )
    }
}

@Composable
fun SoundSettingsDialog(
    soundSynthesizer: SoundSynthesizer,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("acurdate_prefs", android.content.Context.MODE_PRIVATE) }
    var selectedAlertTone by remember {
        mutableStateOf(prefs.getString("selectedAlertTone", "chime") ?: "chime")
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SpaceCardBackground)
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Text(
                text = "Ajustes de Sonido 🔊",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )

            AlertTone.values().forEach { tone ->
                val isSelected = selectedAlertTone == tone.value
                val toneColor = when (tone) {
                    AlertTone.CHIME -> NeonGreen
                    AlertTone.CELESTIAL -> NeonCyan
                    AlertTone.SOS -> NeonRed
                    AlertTone.BELL -> NeonYellow
                    AlertTone.PULSE -> NeonPurple
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) toneColor.copy(alpha = 0.12f) else Color(0xFF1E1F26))
                        .border(
                            width = 1.dp,
                            color = if (isSelected) toneColor else Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            selectedAlertTone = tone.value
                            prefs.edit().putString("selectedAlertTone", tone.value).apply()
                            soundSynthesizer.playTone(tone)
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) toneColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (tone) {
                                AlertTone.CHIME -> "🔔"
                                AlertTone.CELESTIAL -> "✨"
                                AlertTone.SOS -> "🚨"
                                AlertTone.BELL -> "🔔"
                                AlertTone.PULSE -> "⚡"
                            },
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = tone.displayName,
                        color = if (isSelected) Color.White else TextPrimary.copy(alpha = 0.7f),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )

                    RadioButton(
                        selected = isSelected,
                        onClick = {
                            selectedAlertTone = tone.value
                            prefs.edit().putString("selectedAlertTone", tone.value).apply()
                            soundSynthesizer.playTone(tone)
                        },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = toneColor,
                            unselectedColor = TextSecondary.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThemePrimary,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Aceptar",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
