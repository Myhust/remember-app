package com.example.acurdate.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.acurdate.data.Reminder
import com.example.acurdate.data.ReminderCategory
import java.util.*

@Composable
fun CalendarView(
    reminders: List<Reminder>,
    selectedDate: Calendar,
    onDateSelected: (Calendar) -> Unit,
    onDayDoubleTapped: (Calendar) -> Unit,
    modifier: Modifier = Modifier
) {
    var visibleCalendar by remember { mutableStateOf(selectedDate.clone() as Calendar) }
    
    val monthName = remember(visibleCalendar) {
        visibleCalendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("es", "ES"))
            ?.replaceFirstChar { it.uppercase() } ?: ""
    }
    val year = visibleCalendar.get(Calendar.YEAR)
    
    val daysInMonth = remember(visibleCalendar) {
        visibleCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
    
    val firstDayOfWeek = remember(visibleCalendar) {
        val cal = visibleCalendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val day = cal.get(Calendar.DAY_OF_WEEK)
        if (day == Calendar.SUNDAY) 6 else day - 2
    }
    
    val weekDays = listOf("Lu", "Ma", "Mi", "Ju", "Vi", "Sá", "Do")
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF16181D))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable {
                        val cal = visibleCalendar.clone() as Calendar
                        cal.add(Calendar.MONTH, -1)
                        visibleCalendar = cal
                    },
                contentAlignment = Alignment.Center
            ) {
                ArrowLeftIcon(color = Color.White)
            }
            
            Text(
                text = "$monthName $year",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable {
                        val cal = visibleCalendar.clone() as Calendar
                        cal.add(Calendar.MONTH, 1)
                        visibleCalendar = cal
                    },
                contentAlignment = Alignment.Center
            ) {
                ArrowRightIcon(color = Color.White)
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(modifier = Modifier.fillMaxWidth()) {
            weekDays.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    color = Color.White.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        val totalCells = firstDayOfWeek + daysInMonth
        val rows = (totalCells + 6) / 7
        
        Column(modifier = Modifier.fillMaxWidth()) {
            for (row in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (col in 0 until 7) {
                        val index = row * 7 + col
                        val dayNumber = index - firstDayOfWeek + 1
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (dayNumber in 1..daysInMonth) {
                                val cellCal = visibleCalendar.clone() as Calendar
                                cellCal.set(Calendar.DAY_OF_MONTH, dayNumber)
                                
                                val isSelected = cellCal.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
                                        cellCal.get(Calendar.DAY_OF_YEAR) == selectedDate.get(Calendar.DAY_OF_YEAR)
                                
                                val todayCal = Calendar.getInstance()
                                val isToday = cellCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                                        cellCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
                                
                                val dayReminders = reminders.filter { reminder ->
                                    reminder.dueDate?.let { due ->
                                        val dueCal = Calendar.getInstance().apply { timeInMillis = due }
                                        dueCal.get(Calendar.YEAR) == cellCal.get(Calendar.YEAR) &&
                                                dueCal.get(Calendar.DAY_OF_YEAR) == cellCal.get(Calendar.DAY_OF_YEAR)
                                    } ?: false
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when {
                                                isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = when {
                                                isSelected -> MaterialTheme.colorScheme.primary
                                                isToday -> Color.White.copy(alpha = 0.25f)
                                                else -> Color.Transparent
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .pointerInput(cellCal) {
                                            detectTapGestures(
                                                onTap = { onDateSelected(cellCal) },
                                                onDoubleTap = { onDayDoubleTapped(cellCal) }
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = dayNumber.toString(),
                                            color = when {
                                                isSelected -> Color.White
                                                isToday -> MaterialTheme.colorScheme.primary
                                                else -> Color.White.copy(alpha = 0.85f)
                                            },
                                            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp
                                        )
                                        
                                        if (dayReminders.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                dayReminders.take(3).forEach { reminder ->
                                                    val color = when (reminder.category) {
                                                        ReminderCategory.IMPORTANTE -> Color(0xFFFF4B4B)
                                                        ReminderCategory.HABITO -> Color(0xFF00F0FF)
                                                        ReminderCategory.TAREA -> Color(0xFF39FF14)
                                                        ReminderCategory.IDEA -> Color(0xFFFFEA00)
                                                        ReminderCategory.NOTA -> Color(0xFFD080FF)
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.dp)
                                                            .padding(horizontal = 0.5.dp)
                                                            .clip(CircleShape)
                                                            .background(color)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArrowLeftIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        drawLine(
            color = color,
            start = Offset(w * 0.6f, h * 0.3f),
            end = Offset(w * 0.4f, h * 0.5f),
            strokeWidth = 2.dp.toPx()
        )
        drawLine(
            color = color,
            start = Offset(w * 0.4f, h * 0.5f),
            end = Offset(w * 0.6f, h * 0.7f),
            strokeWidth = 2.dp.toPx()
        )
    }
}

@Composable
fun ArrowRightIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        drawLine(
            color = color,
            start = Offset(w * 0.4f, h * 0.3f),
            end = Offset(w * 0.6f, h * 0.5f),
            strokeWidth = 2.dp.toPx()
        )
        drawLine(
            color = color,
            start = Offset(w * 0.6f, h * 0.5f),
            end = Offset(w * 0.4f, h * 0.7f),
            strokeWidth = 2.dp.toPx()
        )
    }
}
